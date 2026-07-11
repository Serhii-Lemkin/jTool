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

    suspend fun pushSuggestion(text: String, userId: String) {
        try {
            val sanitizedText = text.trim()
            if (sanitizedText.isBlank()) return
            
            val timestamp = System.currentTimeMillis()
            val suggestionData = mapOf(
                "text" to sanitizedText,
                "userId" to userId,
                "timestamp" to timestamp
            )
            
            // For shopping list suggestions (legacy key-based for autocomplete)
            val key = sanitizedText.lowercase().replace(".", "_").replace("$", "_").replace("#", "_").replace("[", "_").replace("]", "_")
            getSuggestionsRef().child(key).setValue(true).await()
            
            // For general requests/suggestions (new structured format)
            database.getReference("all_suggestions").push().setValue(suggestionData).await()
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
        val snapshot = getUserSettingsRef()?.get()?.await() ?: return emptyList()
        val groupIdsObj = snapshot.child("groupIds").value
        return when (groupIdsObj) {
            is List<*> -> groupIdsObj.filterIsInstance<String>()
            is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>()
            else -> snapshot.child("groupIds").children.mapNotNull { it.getValue(String::class.java) }
        }
    }

    suspend fun createGroup(groupName: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        val groupRef = getGroupsRef().push()
        val groupId = groupRef.key ?: return null
        
        val groupData = mapOf(
            "info" to mapOf("name" to groupName, "owner" to uid),
            "members" to mapOf(uid to true),
            "data" to mapOf("shopping" to "", "notes" to "", "links" to "", "cleaning" to "")
        )
        
        return try {
            groupRef.setValue(groupData).await()
            
            // Add to user's groupIds list - robust update
            val userRef = getUserSettingsRef()
            if (userRef != null) {
                val currentGroups = try { getUserGroups().toMutableList() } catch (e: Exception) { mutableListOf() }
                if (!currentGroups.contains(groupId)) {
                    currentGroups.add(groupId)
                    userRef.child("groupIds").setValue(currentGroups).await()
                }
            }
            
            settings.addGroup(JGroupInfo(id = groupId, name = groupName, owner = uid))
            settings.setCurrentGroupId(groupId)
            pushUserSettings()
            groupId
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to create group", e)
            null
        }
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        return try {
            getGroupsRef().child(groupId).child("info").child("name").setValue(newName).await()
            
            // Update local info
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let {
                groups[groupId] = it.copy(name = newName)
                settings.saveGroups(groups)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to update group name", e)
            false
        }
    }

    suspend fun joinGroup(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val groupSnapshot = getGroupsRef().child(groupId).get().await()
            val groupName = groupSnapshot.child("info").child("name").getValue(String::class.java) ?: "Unknown Group"
            val owner = groupSnapshot.child("info").child("owner").getValue(String::class.java)
            
            // 1. Add to group members
            getGroupsRef().child(groupId).child("members").child(uid).setValue(true).await()
            
            // 2. Add to user's group list - robust update
            val userRef = getUserSettingsRef()
            if (userRef != null) {
                val currentGroups = try { getUserGroups().toMutableList() } catch (e: Exception) { mutableListOf() }
                if (!currentGroups.contains(groupId)) {
                    currentGroups.add(groupId)
                    userRef.child("groupIds").setValue(currentGroups).await()
                }
            }
            
            settings.addGroup(JGroupInfo(id = groupId, name = groupName, owner = owner))
            settings.setCurrentGroupId(groupId)
            pushUserSettings()
            pullGroupData()
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to join group", e)
            false
        }
    }

    suspend fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            // 1. Remove from group members list
            getGroupsRef().child(groupId).child("members").child(uid).removeValue().await()
            
            // 2. Remove from user's group list - robust update
            val userRef = getUserSettingsRef()
            if (userRef != null) {
                val currentGroups = try { getUserGroups().toMutableList() } catch (e: Exception) { mutableListOf() }
                if (currentGroups.remove(groupId)) {
                    userRef.child("groupIds").setValue(currentGroups).await()
                }
            }
            
            settings.removeGroup(groupId)
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(null)
            }
            pushUserSettings()
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to leave group", e)
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
        SyncStatusManager.startSync()
        val groupId = settings.getCurrentGroupId() ?: run { SyncStatusManager.endSync(false); return false }
        val dataRef = getGroupsRef().child(groupId).child("data")
        return try {
            val snapshot = dataRef.get().await()
            if (!snapshot.exists()) {
                SyncStatusManager.endSync(true)
                return true
            }

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
            SyncStatusManager.endSync(true)
            true
        } catch (e: Exception) {
            SyncStatusManager.endSync(false)
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
        SyncStatusManager.startSync()
        val groupId = settings.getCurrentGroupId() ?: run { SyncStatusManager.endSync(false); return false }
        val dataRef = getGroupsRef().child(groupId).child("data")
        
        return try {
            val data = mapOf(
                "shopping" to context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).getString("items", ""),
                "notes" to context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).getString("notes", ""),
                "links" to context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).getString("links", ""),
                "cleaning" to context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).getString("tasks", "")
            )
            dataRef.setValue(data).await()
            SyncStatusManager.endSync(true)
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to push group data", e)
            SyncStatusManager.endSync(false)
            false
        }
    }

    suspend fun pushUserSettings(): Boolean {
        val ref = getUserSettingsRef() ?: return false
        val appUserId = settings.getAppUserId()
        val userName = settings.getUserName()
        
        return try {
            val snapshot = ref.get().await()
            val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
            
            // Preserve groupIds from server if local is empty (e.g. after re-login)
            val localGroupIds = settings.getGroupIds()
            val serverGroupIdsObj = snapshot.child("groupIds").value
            val serverGroupIds = when (serverGroupIdsObj) {
                is List<*> -> serverGroupIdsObj.filterIsInstance<String>()
                is Map<*, *> -> serverGroupIdsObj.values.filterIsInstance<String>()
                else -> emptyList()
            }
            val mergedGroupIds = (localGroupIds + serverGroupIds).distinct()

            // Push private settings
            val data = mutableMapOf<String, Any?>(
                "currentGroupId" to (settings.getCurrentGroupId() ?: snapshot.child("currentGroupId").getValue(String::class.java)),
                "groupIds" to mergedGroupIds,
                "appUserId" to appUserId,
                "userName" to userName,
                "isDarkMode" to settings.isDarkMode(),
                "language" to settings.getLanguage(),
                "isAdmin" to isAdmin
            )
            
            // Add workday hours to private settings
            settings.getAllWeeklyHours().forEach { (day, hours) ->
                data["hours_${day.name}"] = hours
            }

            ref.setValue(data).await()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to push user settings", e)
            false
        }
    }

    suspend fun manualSyncMerge(): Boolean {
        SyncStatusManager.startSync()
        val groupId = settings.getCurrentGroupId() ?: run { SyncStatusManager.endSync(false); return false }
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
            SyncStatusManager.endSync(true)
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Manual merge failed", e)
            SyncStatusManager.endSync(false)
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
        val uid = auth.currentUser?.uid ?: return false
        
        return try {
            android.util.Log.d("GroupSync", "Fetching user settings for UID: $uid")
            val snapshot = ref.get().await()
            
            // 1. Get the list of group IDs
            val groupIdsObj = snapshot.child("groupIds").value
            val groupIds = when (groupIdsObj) {
                is List<*> -> groupIdsObj.filterIsInstance<String>()
                is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>()
                else -> {
                    // Try to get children if it's a DataSnapshot representation
                    snapshot.child("groupIds").children.mapNotNull { it.getValue(String::class.java) }
                }
            }

            // 2. Fetch info for each group
            val finalGroupsMap = mutableMapOf<String, JGroupInfo>()
            groupIds.forEach { gid ->
                try {
                    val infoSnapshot = getGroupsRef().child(gid).child("info").get().await()
                    val name = infoSnapshot.child("name").getValue(String::class.java) ?: "Unknown Group"
                    val owner = infoSnapshot.child("owner").getValue(String::class.java)
                    finalGroupsMap[gid] = JGroupInfo(id = gid, name = name, owner = owner)
                } catch (e: Exception) {
                    finalGroupsMap[gid] = JGroupInfo(id = gid, name = gid)
                }
            }
            
            // 3. Persist locally
            settings.saveGroups(finalGroupsMap)
            
            val localCurrentGroupId = settings.getCurrentGroupId()
            
            if (localCurrentGroupId == null && groupIds.isNotEmpty()) {
                val firstGroup = groupIds.first()
                settings.setCurrentGroupId(firstGroup)
                pullGroupData()
            } else if (localCurrentGroupId != null && !groupIds.contains(localCurrentGroupId)) {
                val fallback = groupIds.firstOrNull()
                settings.setCurrentGroupId(fallback)
                if (fallback != null) pullGroupData()
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

    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, JGroupInfo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, JGroupInfo>()
                groupIds.forEach { id ->
                    val infoNode = snapshot.child(id).child("info")
                    if (infoNode.exists()) {
                        val name = infoNode.child("name").getValue(String::class.java) ?: "Unknown"
                        val owner = infoNode.child("owner").getValue(String::class.java)
                        map[id] = JGroupInfo(id = id, name = name, owner = owner)
                    }
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        getGroupsRef().addValueEventListener(listener)
        awaitClose { getGroupsRef().removeEventListener(listener) }
    }

    fun userNamesFlow(): Flow<Map<String, String>> = callbackFlow {
        val ref = database.getReference("user_settings")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val appUserId = child.child("appUserId").getValue(String::class.java)
                    val userName = child.child("userName").getValue(String::class.java)
                    if (appUserId != null && userName != null) {
                        map[appUserId] = userName
                    }
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Admin Features ---

    fun isAdminFlow(): Flow<Boolean> = callbackFlow {
        val ref = getUserSettingsRef() ?: run { trySend(false); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun allUsersFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = database.getReference("user_settings")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val data = child.value as? Map<String, Any> ?: return@mapNotNull null
                    data + ("uid" to child.key!!)
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        database.getReference("user_settings").child(uid).child("isAdmin").setValue(isAdmin).await()
    }

    fun allSuggestionsFlow(): Flow<List<Map<String, String>>> = callbackFlow {
        val ref = database.getReference("all_suggestions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val map = mutableMapOf<String, String>()
                    map["id"] = child.key ?: ""
                    child.children.forEach { subChild ->
                        map[subChild.key!!] = subChild.value?.toString() ?: ""
                    }
                    map
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteSuggestion(id: String) {
        database.getReference("all_suggestions").child(id).removeValue().await()
    }

    suspend fun updateAppInfo(versionCode: Int, downloadUrl: String) {
        val data = mapOf(
            "versionCode" to versionCode,
            "downloadUrl" to downloadUrl,
            "timestamp" to System.currentTimeMillis()
        )
        getAppInfoRef().setValue(data).await()
    }

    suspend fun deleteEmptyGroups() {
        val snapshot = getGroupsRef().get().await()
        snapshot.children.forEach { group ->
            if (!group.child("members").exists() || group.child("members").childrenCount == 0L) {
                group.ref.removeValue().await()
            }
        }
    }
}
