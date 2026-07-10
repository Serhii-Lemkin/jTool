package com.example.jtool

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.DayOfWeek
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupSyncManager(private val context: Context) {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsManager(context)

    // Database Paths
    private fun getGroupsRef() = database.getReference("groups")
    private fun getUserSettingsRef() = auth.currentUser?.uid?.let { uid ->
        database.getReference("user_settings").child(uid)
    }
    private fun getAppInfoRef() = database.getReference("app_info")
    private fun getSuggestionsRef() = database.getReference("suggestions")
    private fun getUserNamesRef() = database.getReference("user_names")
    private fun getInvitesRef() = database.getReference("invites")

    // --- App Update Logic ---

    suspend fun getLatestAppInfo(): Map<String, Any>? {
        return try {
            val snapshot = getAppInfoRef().get().await()
            if (snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                snapshot.value as? Map<String, Any>
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // --- Suggestions Logic ---

    suspend fun pushSuggestion(itemText: String) {
        try {
            val sanitized = itemText.trim().lowercase()
            getSuggestionsRef().child(sanitized).setValue(true).await()
        } catch (e: Exception) {}
    }

    fun suggestionsFlow(): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.key }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        getSuggestionsRef().addValueEventListener(listener)
        awaitClose { getSuggestionsRef().removeEventListener(listener) }
    }

    // --- Group Management ---

    suspend fun getUserGroups(): List<String> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            android.util.Log.e("GroupSync", "Cannot fetch groups: User is NULL")
            return emptyList()
        }

        return try {
            android.util.Log.d("GroupSync", "Fetching group IDs for user: $uid")

            // Query the database for groups where this user is listed under members
            val query = getGroupsRef()
                .orderByChild("members/$uid")
                .equalTo(true)

            val snapshot = query.get().await()
            val groupIdsList = mutableListOf<String>()

            for (groupSnapshot in snapshot.children) {
                val groupId = groupSnapshot.key ?: continue
                groupIdsList.add(groupId)
            }

            android.util.Log.d("GroupSync", "Successfully fetched ${groupIdsList.size} group IDs")
            groupIdsList
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to fetch user groups", e)
            emptyList()
        }
    }

    suspend fun createGroup(groupName: String): String? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            android.util.Log.e("GroupSync", "Cannot create group: User is NULL")
            return null
        }
        
        val groupRef = getGroupsRef().push()
        val groupId = groupRef.key ?: return null
        
        val groupData = mapOf(
            "info" to mapOf(
                "name" to groupName,
                "owner" to uid
            ),
            "members" to mapOf(uid to true),
            "data" to mapOf(
                "shopping" to "",
                "notes" to "",
                "links" to "",
                "cleaning" to ""
            )
        )
        
        return try {
            android.util.Log.d("GroupSync", "Attempting to create group: $groupId")
            groupRef.setValue(groupData).await()
            settings.addGroupId(groupId)
            settings.setCurrentGroupId(groupId)
            pushUserSettings()
            android.util.Log.d("GroupSync", "Group created successfully: $groupId")
            groupId
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "CRITICAL: Group creation failed!", e)
            null
        }
    }

    suspend fun joinGroup(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            getGroupsRef().child(groupId).child("members").child(uid).setValue(true).await()
            settings.addGroupId(groupId)
            settings.setCurrentGroupId(groupId)
            pushUserSettings()
            pullGroupData()
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to join group", e)
            false
        }
    }

    suspend fun switchGroup(groupId: String) {
        settings.setCurrentGroupId(groupId)
        pushUserSettings()
        
        // Clear local data before pulling new group data to ensure fresh start
        context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().clear().apply()
        
        pullGroupData()
    }

    suspend fun pullGroupData(): Boolean {
        val groupId = settings.getCurrentGroupId() ?: return false
        val dataRef = getGroupsRef().child(groupId).child("data")
        return try {
            val snapshot = dataRef.get().await()
            if (!snapshot.exists()) return true

            snapshot.child("shopping").getValue(String::class.java)?.let {
                context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().putString("items", it).apply()
            }
            snapshot.child("notes").getValue(String::class.java)?.let {
                context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().putString("notes", it).apply()
            }
            snapshot.child("links").getValue(String::class.java)?.let {
                context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().putString("links", it).apply()
            }
            snapshot.child("cleaning").getValue(String::class.java)?.let {
                context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().putString("tasks", it).apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Invite Logic ---

    suspend fun sendInvite(email: String, groupId: String): Boolean {
        val sanitizedEmail = email.replace(".", "_")
        return try {
            val groupInfo = getGroupsRef().child(groupId).child("info").get().await()
            val groupName = groupInfo.child("name").getValue(String::class.java) ?: "Unknown"
            
            val inviteData = mapOf(
                "groupId" to groupId,
                "groupName" to groupName,
                "from" to (auth.currentUser?.email ?: "Unknown")
            )
            getInvitesRef().child(sanitizedEmail).push().setValue(inviteData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun invitesFlow(): Flow<List<Map<String, String>>> = callbackFlow {
        val email = auth.currentUser?.email ?: run { close(); return@callbackFlow }
        val sanitizedEmail = email.replace(".", "_")
        val ref = getInvitesRef().child(sanitizedEmail)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val map = mutableMapOf<String, String>()
                    map["inviteId"] = child.key ?: ""
                    child.children.forEach { map[it.key!!] = it.value.toString() }
                    map
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean) {
        val email = auth.currentUser?.email ?: return
        val sanitizedEmail = email.replace(".", "_")
        try {
            if (accept) {
                joinGroup(groupId)
            }
            getInvitesRef().child(sanitizedEmail).child(inviteId).removeValue().await()
        } catch (e: Exception) {}
    }

    // --- Data Sync ---

    suspend fun pushGroupData(): Boolean {
        val groupId = settings.getCurrentGroupId() ?: return false
        val dataRef = getGroupsRef().child(groupId).child("data")
        
        return try {
            val data = mapOf(
                "shopping" to context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).getString("items", ""),
                "notes" to context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).getString("notes", ""),
                "links" to context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).getString("links", ""),
                "cleaning" to context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).getString("tasks", "")
            )
            dataRef.setValue(data).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to push group data", e)
            false
        }
    }

    suspend fun pushUserSettings(): Boolean {
        val ref = getUserSettingsRef() ?: return false
        val appUserId = settings.getAppUserId()
        val userName = settings.getUserName()
        
        return try {
            // Push private settings
            val data = mutableMapOf<String, Any?>(
                "currentGroupId" to settings.getCurrentGroupId(),
                "groupIds" to settings.getGroupIds(),
                "appUserId" to appUserId,
                "userName" to userName,
                "isDarkMode" to settings.isDarkMode(),
                "language" to settings.getLanguage()
            )
            
            // Add workday hours to private settings
            settings.getAllWeeklyHours().forEach { (day, hours) ->
                data["hours_${day.name}"] = hours
            }

            ref.setValue(data).await()
            
            // Push public ID -> Name mapping
            getUserNamesRef().child(appUserId).setValue(userName).await()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to push user settings", e)
            false
        }
    }

    suspend fun manualSyncMerge(): Boolean {
        val groupId = settings.getCurrentGroupId() ?: return false
        val dataRef = database.getReference("groups").child(groupId).child("data")
        
        return try {
            val snapshot = dataRef.get().await()
            if (!snapshot.exists()) {
                // If server is empty, just push local data
                return pushGroupData()
            }

            // 1. Shopping List Merge
            val serverShop = snapshot.child("shopping").getValue(String::class.java) ?: ""
            val localShop = context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).getString("items", "") ?: ""
            val mergedShop = mergeEncodedData(localShop, serverShop)
            context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().putString("items", mergedShop).apply()

            // 2. Notes Merge
            val serverNotes = snapshot.child("notes").getValue(String::class.java) ?: ""
            val localNotes = context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).getString("notes", "") ?: ""
            val mergedNotes = mergeEncodedData(localNotes, serverNotes)
            context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().putString("notes", mergedNotes).apply()

            // 3. Links Merge
            val serverLinks = snapshot.child("links").getValue(String::class.java) ?: ""
            val localLinks = context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).getString("links", "") ?: ""
            val mergedLinks = mergeEncodedData(localLinks, serverLinks)
            context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().putString("links", mergedLinks).apply()

            // 4. Cleaning Merge
            val serverCleaning = snapshot.child("cleaning").getValue(String::class.java) ?: ""
            val localCleaning = context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).getString("tasks", "") ?: ""
            val mergedCleaning = mergeEncodedData(localCleaning, serverCleaning)
            context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().putString("tasks", mergedCleaning).apply()

            // Push the final merged result back to server
            val finalData = mapOf(
                "shopping" to mergedShop,
                "notes" to mergedNotes,
                "links" to mergedLinks,
                "cleaning" to mergedCleaning
            )
            dataRef.setValue(finalData).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Manual merge failed", e)
            false
        }
    }

    private fun mergeEncodedData(localEncoded: String, serverEncoded: String): String {
        if (localEncoded.isEmpty()) return serverEncoded
        if (serverEncoded.isEmpty()) return localEncoded

        val localItems = localEncoded.split(";").map { it.split("|") }.filter { it.size >= 4 }
        val serverItems = serverEncoded.split(";").map { it.split("|") }.filter { it.size >= 4 }

        val resultMap = mutableMapOf<String, List<String>>() // Key by ID (parts[0])

        // Add all local
        localItems.forEach { parts -> resultMap[parts[0]] = parts }

        // Merge with server based on timestamp (parts[3] usually, or parts[8] for cleaning)
        serverItems.forEach { sParts ->
            val id = sParts[0]
            val lParts = resultMap[id]
            if (lParts == null) {
                resultMap[id] = sParts
            } else {
                val lTs = lParts.last().toLongOrNull() ?: 0L
                val sTs = sParts.last().toLongOrNull() ?: 0L
                
                if (sTs > lTs) {
                    resultMap[id] = sParts
                }
            }
        }

        return resultMap.values.joinToString(";") { it.joinToString("|") }
    }

    suspend fun fetchUserSettings(): Boolean {
        val ref = getUserSettingsRef() ?: return false
        return try {
            android.util.Log.d("GroupSync", "Fetching user settings for UID: ${auth.currentUser?.uid}")
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                android.util.Log.w("GroupSync", "User settings NOT FOUND in Firebase")
                return true
            }

            // Robust group list extraction
            val groupIdsObj = snapshot.child("groupIds").value
            android.util.Log.d("GroupSync", "Raw groupIds from DB: $groupIdsObj")
            
            when (groupIdsObj) {
                is List<*> -> groupIdsObj.filterIsInstance<String>().forEach { settings.addGroupId(it) }
                is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>().forEach { settings.addGroupId(it) }
            }

            snapshot.child("currentGroupId").getValue(String::class.java)?.let {
                android.util.Log.d("GroupSync", "Setting current group to: $it")
                settings.setCurrentGroupId(it)
            }
            
            snapshot.child("userName").getValue(String::class.java)?.let {
                settings.saveUserName(it)
            }

            // Extract workday hours
            DayOfWeek.values().forEach { day ->
                snapshot.child("hours_${day.name}").getValue(Double::class.java)?.let {
                    settings.saveDayHours(day, it)
                }
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to fetch user settings", e)
            false
        }
    }

    fun groupDataFlow(groupId: String): Flow<Unit> = callbackFlow {
        android.util.Log.d("GroupSync", "Starting real-time listener for group: $groupId")
        val ref = getGroupsRef().child(groupId).child("data")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                var changed = false
                snapshot.child("shopping").getValue(String::class.java)?.let {
                    val prefs = context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)
                    if (prefs.getString("items", "") != it) {
                        prefs.edit().putString("items", it).apply()
                        changed = true
                    }
                }
                snapshot.child("notes").getValue(String::class.java)?.let {
                    val prefs = context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)
                    if (prefs.getString("notes", "") != it) {
                        prefs.edit().putString("notes", it).apply()
                        changed = true
                    }
                }
                snapshot.child("links").getValue(String::class.java)?.let {
                    val prefs = context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)
                    if (prefs.getString("links", "") != it) {
                        prefs.edit().putString("links", it).apply()
                        changed = true
                    }
                }
                snapshot.child("cleaning").getValue(String::class.java)?.let {
                    val prefs = context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)
                    if (prefs.getString("tasks", "") != it) {
                        prefs.edit().putString("tasks", it).apply()
                        changed = true
                    }
                }

                if (changed) {
                    android.util.Log.d("GroupSync", "Cloud data changed for group $groupId, local DB updated.")
                    trySend(Unit)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GroupSync", "Group listener cancelled", error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { 
            android.util.Log.d("GroupSync", "Closing listener for group: $groupId")
            ref.removeEventListener(listener) 
        }
    }

    fun groupNamesFlow(groupIds: List<String>): Flow<Map<String, String>> = callbackFlow {
        android.util.Log.d("GroupSync", "Starting groupNamesFlow for IDs: $groupIds")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                groupIds.forEach { id ->
                    val name = snapshot.child(id).child("info").child("name").getValue(String::class.java)
                    if (name != null) map[id] = name
                }
                android.util.Log.d("GroupSync", "groupNamesFlow updated: $map")
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GroupSync", "groupNamesFlow cancelled", error.toException())
            }
        }
        getGroupsRef().addValueEventListener(listener)
        awaitClose { getGroupsRef().removeEventListener(listener) }
    }

    fun userNamesFlow(): Flow<Map<String, String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = snapshot.children.associate { it.key!! to (it.value as? String ?: "Unknown") }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        getUserNamesRef().addValueEventListener(listener)
        awaitClose { getUserNamesRef().removeEventListener(listener) }
    }
}
