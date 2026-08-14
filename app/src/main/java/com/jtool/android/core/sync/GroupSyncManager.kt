package com.jtool.android.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jtool.android.core.models.JGroupInfo
import com.jtool.android.core.settings.SettingsManager
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
            
            // Unified location for all suggestions/requests
            database.getReference("all_suggestions").push().setValue(suggestionData).await()
        } catch (e: Exception) {}
    }

    fun suggestionsFlow(): Flow<List<String>> = callbackFlow {
        val ref = database.getReference("all_suggestions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Return unique text values for autocomplete
                val list = snapshot.children.mapNotNull { it.child("text").getValue(String::class.java) }.distinct()
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
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
            "members" to emptyMap<String, Boolean>(),
            "data" to mapOf("shopping" to "", "notes" to "", "links" to "", "cleaning" to "")
        )
        
        return try {
            groupRef.setValue(groupData).await()
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
        android.util.Log.d("GroupSync", "Attempting to join group: $groupId as UID: $uid")
        
        return try {
            // 1. Add to group members FIRST to gain permissions
            try {
                getGroupsRef().child(groupId).child("members").child(uid).setValue(true).await()
                android.util.Log.d("GroupSync", "Step 1: Added to members list successfully")
            } catch (e: Exception) {
                android.util.Log.e("GroupSync", "Step 1 Failed: Could not add to members list", e)
                return false // Critical failure
            }
            
            // 2. Try to get group info with retries (permissions might take a second to propagate)
            var groupName = "New Group"
            var owner: String? = null
            
            for (i in 1..5) { // Increased retries
                try {
                    val groupSnapshot = getGroupsRef().child(groupId).get().await()
                    val name = groupSnapshot.child("info").child("name").getValue(String::class.java)
                    if (name != null) {
                        groupName = name
                        owner = groupSnapshot.child("info").child("owner").getValue(String::class.java)
                        android.util.Log.d("GroupSync", "Step 2: Fetched group info '$groupName' on attempt $i")
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GroupSync", "Step 2 Retry $i: Could not fetch group info. ${e.message}")
                    kotlinx.coroutines.delay(kotlin.time.Duration.parse("1s")) // Longer wait for propagation
                }
            }
            
            // 3. Add to user's group list - robust update
            try {
                val userRef = getUserSettingsRef()
                if (userRef != null) {
                    val currentGroups = try { getUserGroups().toMutableList() } catch (e: Exception) { mutableListOf() }
                    if (!currentGroups.contains(groupId)) {
                        currentGroups.add(groupId)
                        userRef.child("groupIds").setValue(currentGroups).await()
                        android.util.Log.d("GroupSync", "Step 3: Added to user groupIds successfully")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("GroupSync", "Step 3 Failed: Could not update user group list (Non-critical)", e)
            }
            
            // 4. Update local info
            settings.addGroup(JGroupInfo(id = groupId, name = groupName, owner = owner))
            settings.setCurrentGroupId(groupId)
            
            // 5. Cleanup my info snapshot in group (No longer used, using public_user_names)
            try {
                // Remove legacy per-member metadata if it exists
                getGroupsRef().child(groupId).child("members").child(uid).setValue(true).await()
            } catch (e: Exception) { }

            pushUserSettings()
            
            // Clear local data before pulling new group data to ensure fresh start
            clearLocalCache()
            pullGroupData()
            
            android.util.Log.i("GroupSync", "Successfully joined group: $groupId")
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Unexpected error during joinGroup", e)
            false
        }
    }

    suspend fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        android.util.Log.i("GroupSync", "USER INITIATED LEAVE: User $uid is leaving group $groupId")
        try {
            val groupRef = getGroupsRef().child(groupId)
            val membersSnapshot = groupRef.child("members").get().await()
            val memberIds = membersSnapshot.children.mapNotNull { it.key }
            
            if (memberIds.size <= 1 && memberIds.contains(uid)) {
                // I am the last member, delete the group entirely
                deleteGroupPermanently(groupId)
            } else {
                // If I am the owner, transfer ownership to someone else
                val infoSnapshot = groupRef.child("info").get().await()
                val owner = infoSnapshot.child("owner").getValue(String::class.java)
                if (owner == uid) {
                    val nextOwner = memberIds.filter { it != uid }.random()
                    groupRef.child("info").child("owner").setValue(nextOwner).await()
                    android.util.Log.d("GroupSync", "Ownership transferred to $nextOwner")
                }
                
                // Remove from group members list
                groupRef.child("members").child(uid).removeValue().await()
            }
            
            // 2. Remove from user's group list - robust update
            val userRef = getUserSettingsRef()
            if (userRef != null) {
                val currentGroups = try { getUserGroups().toMutableList() } catch (e: Exception) { 
                    android.util.Log.e("GroupSync", "Step 2 Error: Could not get current user groups", e)
                    mutableListOf() 
                }
                if (currentGroups.remove(groupId)) {
                    userRef.child("groupIds").setValue(currentGroups).await()
                    android.util.Log.d("GroupSync", "Step 2: Removed from user groupIds node. Remaining: $currentGroups")
                } else {
                    android.util.Log.w("GroupSync", "Step 2 Warning: Group $groupId was not found in user's groupIds list")
                }
            }
            
            settings.removeGroup(groupId)
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(null)
            }
            
            pushUserSettings()
            android.util.Log.i("GroupSync", "Leave group $groupId completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "CRITICAL: Failed to leave group $groupId", e)
        }
    }

    suspend fun switchGroup(groupId: String) {
        settings.setCurrentGroupId(groupId)
        pushUserSettings()
        
        // Clear local data before pulling new group data to ensure fresh start
        clearLocalCache()
        
        pullGroupData()
    }

    fun clearLocalCache() {
        android.util.Log.d("GroupSync", "Clearing local cache for shopping, notes, links, and cleaning")
        context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().clear().apply()
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

    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean {
        val email = auth.currentUser?.email ?: return false
        val sanitizedEmail = email.replace(".", "_")
        android.util.Log.d("GroupSync", "Responding to invite $inviteId for group $groupId (Accept: $accept)")
        return try {
            if (accept) {
                val success = joinGroup(groupId)
                if (success) {
                    android.util.Log.d("GroupSync", "Join successful, deleting invite node.")
                    getInvitesRef().child(sanitizedEmail).child(inviteId).removeValue().await()
                } else {
                    android.util.Log.e("GroupSync", "Join failed, keeping invite node for retry.")
                }
                success
            } else {
                android.util.Log.d("GroupSync", "Invite declined, deleting node.")
                getInvitesRef().child(sanitizedEmail).child(inviteId).removeValue().await()
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Error in respondToInvite", e)
            false
        }
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
        val uid = auth.currentUser?.uid ?: "Unknown"
        val appUserId = settings.getAppUserId()
        val globalUserName = settings.getUserName()
        
        android.util.Log.d("GroupSync", "PUSH USER SETTINGS: Starting push for user $uid")
        
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
            
            android.util.Log.d("GroupSync", "Group Merging: Local=$localGroupIds, Server=$serverGroupIds")
            val mergedGroupIds = (localGroupIds + serverGroupIds).distinct()
            
            if (mergedGroupIds.isEmpty() && serverGroupIds.isNotEmpty()) {
                android.util.Log.w("GroupSync", "ATTENTION: Merged group list is empty but server had groups! Aborting group wipe.")
                // Prevent wiping groups if local state seems invalid
                return false 
            }

            // Push private settings (Removed userName as it's now only in public table)
            val data = mutableMapOf(
                "currentGroupId" to (settings.getCurrentGroupId() ?: snapshot.child("currentGroupId").getValue(String::class.java)),
                "groupIds" to mergedGroupIds,
                "appUserId" to appUserId,
                "email" to (auth.currentUser?.email),
                "isDarkMode" to settings.isDarkMode(),
                "language" to settings.getLanguage(),
                "isAdmin" to isAdmin
            )
            
            // Add workday hours to private settings
            settings.getAllWeeklyHours().forEach { (day, hours) ->
                data["hours_${day.name}"] = hours
            }

            ref.setValue(data).await()
            android.util.Log.i("GroupSync", "Push successful. Current groups in DB: $mergedGroupIds")
            
            // Push name to public directory for name resolution (Global source of truth)
            database.getReference("public_user_names").child(appUserId).setValue(globalUserName).await()
            database.getReference("public_user_names").child(uid).setValue(globalUserName).await()
            
            // Update my info in all my groups (Ensure membership node exists)
            mergedGroupIds.forEach { gid ->
                getGroupsRef().child(gid).child("members").child(uid).setValue(true).await()
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "PUSH FAILED for user $uid", e)
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
            val currentGroups = settings.getGroups()
            val finalGroupsMap = mutableMapOf<String, JGroupInfo>()
            groupIds.forEach { gid ->
                try {
                    val infoSnapshot = getGroupsRef().child(gid).child("info").get().await()
                    val name = infoSnapshot.child("name").getValue(String::class.java) ?: "Unknown Group"
                    val owner = infoSnapshot.child("owner").getValue(String::class.java)
                    finalGroupsMap[gid] = JGroupInfo(id = gid, name = name, owner = owner)
                } catch (e: Exception) {
                    // If fetch fails, keep the old name if we had one, otherwise fallback to ID
                    val oldName = currentGroups[gid]?.name ?: gid
                    finalGroupsMap[gid] = JGroupInfo(id = gid, name = oldName)
                }
            }
            
            // 3. Persist locally
            if (finalGroupsMap.isNotEmpty() || groupIds.isEmpty()) {
                settings.saveGroups(finalGroupsMap)
                android.util.Log.d("GroupSync", "Saved ${finalGroupsMap.size} groups to local storage")
            } else {
                android.util.Log.w("GroupSync", "Fetch for group details yielded 0 results despite having ${groupIds.size} IDs. Skipping local save to prevent data loss.")
            }
            
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
            
            // Fetch name from public directory (Cloud Recovery)
            try {
                val publicNameSnapshot = database.getReference("public_user_names").child(uid).get().await()
                publicNameSnapshot.getValue(String::class.java)?.let {
                    settings.saveUserName(it)
                }
            } catch (e: Exception) {
                android.util.Log.w("GroupSync", "Could not fetch name from public table")
            }

            snapshot.child("appUserId").getValue(String::class.java)?.let {
                settings.saveAppUserId(it)
            }

            // Extract workday hours
            DayOfWeek.entries.forEach { day ->
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

                android.util.Log.d("GroupSync", "Cloud data received for group $groupId, local DB updated.")
                trySend(Unit)
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
        if (groupIds.isEmpty()) {
            trySend(emptyMap())
            awaitClose { }
            return@callbackFlow
        }

        val resultMap = mutableMapOf<String, JGroupInfo>()
        val listeners = groupIds.associateWith { id ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val owner = snapshot.child("owner").getValue(String::class.java)
                        resultMap[id] = JGroupInfo(id = id, name = name, owner = owner)
                        trySend(resultMap.toMap())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("GroupSync", "Group info listener cancelled for $id: ${error.message}")
                }
            }
            getGroupsRef().child(id).child("info").addValueEventListener(listener)
            listener
        }

        awaitClose {
            listeners.forEach { (id, listener) ->
                getGroupsRef().child(id).child("info").removeEventListener(listener)
            }
        }
    }

    fun groupMembersFlow(groupId: String): Flow<List<String>> = callbackFlow {
        val membersRef = getGroupsRef().child(groupId).child("members")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.key }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        membersRef.addValueEventListener(listener)
        awaitClose { membersRef.removeEventListener(listener) }
    }

    fun userNamesFlow(): Flow<Map<String, String>> = callbackFlow {
        val ref = database.getReference("public_user_names")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val appUserId = child.key
                    val userName = child.getValue(String::class.java)
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

    fun userGroupsFlow(): Flow<List<String>> = callbackFlow {
        val ref = getUserSettingsRef() ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupIdsObj = snapshot.child("groupIds").value
                val list = when (groupIdsObj) {
                    is List<*> -> groupIdsObj.filterIsInstance<String>()
                    is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>()
                    else -> snapshot.child("groupIds").children.mapNotNull { it.getValue(String::class.java) }
                }
                trySend(list)
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

    suspend fun deleteEmptyGroups() {
        val snapshot = getGroupsRef().get().await()
        snapshot.children.forEach { group ->
            if (!group.child("members").exists() || group.child("members").childrenCount == 0L) {
                group.ref.removeValue().await()
            }
        }
    }

    suspend fun deleteGroupPermanently(groupId: String) {
        android.util.Log.w("GroupSync", "ADMIN ACTION: Deleting group $groupId permanently")
        try {
            // 1. Remove group from all members' lists
            val membersSnapshot = getGroupsRef().child(groupId).child("members").get().await()
            membersSnapshot.children.forEach { member ->
                val uid = member.key ?: return@forEach
                android.util.Log.d("GroupSync", "Removing group $groupId from member $uid")
                val userRef = database.getReference("user_settings").child(uid)
                val snapshot = userRef.get().await()
                val groupIdsObj = snapshot.child("groupIds").value
                val groupIds = when (groupIdsObj) {
                    is List<*> -> groupIdsObj.filterIsInstance<String>().toMutableList()
                    is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>().toMutableList()
                    else -> snapshot.child("groupIds").children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                }
                if (groupIds.remove(groupId)) {
                    userRef.child("groupIds").setValue(groupIds).await()
                }
            }

            // 2. Remove group data
            getGroupsRef().child(groupId).removeValue().await()
            android.util.Log.i("GroupSync", "Group $groupId and all data removed from server")
            
            // 3. Update local state if it's the current user's group
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(null)
            }
            settings.removeGroup(groupId)
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to delete group $groupId permanently", e)
        }
    }

    suspend fun deleteUserPermanently(uid: String) {
        android.util.Log.w("GroupSync", "ADMIN ACTION: Deleting user $uid permanently")
        try {
            val userRef = database.getReference("user_settings").child(uid)
            val snapshot = userRef.get().await()
            
            // 1. Remove user from all groups they belong to
            val groupIdsObj = snapshot.child("groupIds").value
            val groupIds = when (groupIdsObj) {
                is List<*> -> groupIdsObj.filterIsInstance<String>()
                is Map<*, *> -> groupIdsObj.values.filterIsInstance<String>()
                else -> snapshot.child("groupIds").children.mapNotNull { it.getValue(String::class.java) }
            }
            
            groupIds.forEach { gid ->
                android.util.Log.d("GroupSync", "Removing user $uid from group $gid")
                getGroupsRef().child(gid).child("members").child(uid).removeValue().await()
            }

            // 2. Remove from public directory
            val appUserId = snapshot.child("appUserId").getValue(String::class.java)
            if (appUserId != null) {
                database.getReference("public_user_names").child(appUserId).removeValue().await()
            }

            // 3. Remove user settings
            userRef.removeValue().await()
            
            // 4. Remove from public directory using UID too
            database.getReference("public_user_names").child(uid).removeValue().await()
            
            android.util.Log.i("GroupSync", "User $uid deleted from server")
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to delete user $uid permanently", e)
        }
    }

    fun allGroupsFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = getGroupsRef()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val info = child.child("info").value as? Map<String, Any> ?: return@mapNotNull null
                    info + ("id" to child.key!!)
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteMyAccount(): Boolean {
        val user = auth.currentUser ?: return false
        val uid = user.uid
        
        return try {
            // 1. Fetch current groups to leave them properly (transfers ownership or deletes group if last member)
            val groupIds = getUserGroups()
            groupIds.forEach { gid ->
                try {
                    leaveGroup(gid)
                } catch (e: Exception) {
                    android.util.Log.e("GroupSync", "Failed to leave group $gid during account deletion", e)
                }
            }
            
            // 2. Remove from public names
            val appUserId = settings.getAppUserId()
            database.getReference("public_user_names").child(appUserId).removeValue().await()
            database.getReference("public_user_names").child(uid).removeValue().await()
            
            // 3. Remove private settings
            getUserSettingsRef()?.removeValue()?.await()
            
            // 4. Delete the Auth account
            user.delete().await()
            
            // 5. Clear local data
            settings.setCurrentGroupId(null)
            settings.clearAllGroups()
            clearLocalCache()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to delete account", e)
            false
        }
    }
}
