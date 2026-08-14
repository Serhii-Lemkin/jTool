package com.jtool.android.core.sync.helpers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.Comment
import kotlinx.coroutines.tasks.await

class RTDBMigrationHelper(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getInvitesColl() = firestore.collection("invites")
    private fun getSuggestionsColl() = firestore.collection("suggestions")
    private fun getPublicProfilesColl() = firestore.collection("public_profiles")

    suspend fun migrateFromRTDB(onStatusUpdate: (String) -> Unit): Boolean {
        val user = auth.currentUser
        if (user == null) {
            onStatusUpdate("Error: User not authenticated.")
            return false
        }

        return try {
            onStatusUpdate("Deleting existing Firestore data for clean start...")
            clearAllFirestoreData(onStatusUpdate)

            onStatusUpdate("Fetching RTDB snapshot...")
            val rtdbSnapshot = try {
                database.reference.get().await()
            } catch (e: Exception) {
                android.util.Log.e("GroupSync", "RTDB Read failed", e)
                onStatusUpdate("Error reading Realtime DB: ${e.message}. Check RTDB Rules!")
                return false
            }

            if (!rtdbSnapshot.exists()) {
                onStatusUpdate("RTDB is empty.")
                return false
            }

            // 1. Migrate Users
            onStatusUpdate("Migrating Users...")
            rtdbSnapshot.child("user_settings").children.forEach { userSnap ->
                val uid = userSnap.key ?: return@forEach
                val data = userSnap.value as? Map<String, Any> ?: return@forEach
                try {
                    getUsersColl().document(uid).set(data).await()
                } catch (e: Exception) {
                    onStatusUpdate("Error writing User $uid: ${e.message}")
                    throw e
                }
            }

            // 2. Migrate Public Profiles
            onStatusUpdate("Migrating Public Profiles...")
            rtdbSnapshot.child("public_user_names").children.forEach { profileSnap ->
                val id = profileSnap.key ?: return@forEach
                val name = profileSnap.value as? String ?: return@forEach
                try {
                    getPublicProfilesColl().document(id).set(mapOf("displayName" to name)).await()
                } catch (e: Exception) {
                    onStatusUpdate("Error writing Profile $id: ${e.message}")
                    throw e
                }
            }

            // 3. Migrate Suggestions
            onStatusUpdate("Migrating Suggestions...")
            rtdbSnapshot.child("all_suggestions").children.forEach { sugSnap ->
                val id = sugSnap.key ?: return@forEach
                val data = sugSnap.value as? Map<String, Any> ?: return@forEach
                try {
                    getSuggestionsColl().document(id).set(data).await()
                } catch (e: Exception) {
                    onStatusUpdate("Error writing Suggestion $id: ${e.message}")
                    throw e
                }
            }

            // 4. Migrate Invites
            onStatusUpdate("Migrating Invites...")
            rtdbSnapshot.child("invites").children.forEach { emailSnap ->
                val email = emailSnap.key ?: return@forEach
                emailSnap.children.forEach { inviteSnap ->
                    val id = inviteSnap.key ?: return@forEach
                    val data = inviteSnap.value as? Map<String, Any> ?: return@forEach
                    try {
                        getInvitesColl().document(email).collection("items").document(id).set(data).await()
                    } catch (e: Exception) {
                        onStatusUpdate("Error writing Invite $id: ${e.message}")
                        throw e
                    }
                }
            }

            // 5. Migrate Groups & Data
            onStatusUpdate("Migrating Groups & Data...")
            rtdbSnapshot.child("groups").children.forEach { groupSnap ->
                val groupId = groupSnap.key ?: return@forEach
                val info = groupSnap.child("info").value as? Map<String, Any> ?: emptyMap()
                
                try {
                    val groupData = mapOf(
                        "name" to (info["name"] ?: "Unknown Group"),
                        "ownerUid" to (info["owner"] ?: ""),
                        "createdAt" to System.currentTimeMillis()
                    )
                    getGroupsColl().document(groupId).set(groupData).await()

                    // Members
                    groupSnap.child("members").children.forEach { memberSnap ->
                        val mUid = memberSnap.key ?: return@forEach
                        getGroupsColl().document(groupId).collection("members").document(mUid)
                            .set(mapOf("joinedAt" to System.currentTimeMillis())).await()
                    }

                    val dataNode = groupSnap.child("data")
                    
                    // Shopping
                    val shoppingStr = dataNode.child("shopping").getValue(String::class.java) ?: ""
                    DataParser.parseShopping(shoppingStr).forEach { item ->
                        getGroupsColl().document(groupId).collection("shopping")
                            .document(item.id).set(item).await()
                    }

                    // Links
                    val linksStr = dataNode.child("links").getValue(String::class.java) ?: ""
                    DataParser.parseLinks(linksStr).forEach { link ->
                        getGroupsColl().document(groupId).collection("links")
                            .document(link.id).set(link).await()
                    }

                    // Cleaning
                    val cleaningStr = dataNode.child("cleaning").getValue(String::class.java) ?: ""
                    DataParser.parseCleaning(cleaningStr).forEach { task ->
                        getGroupsColl().document(groupId).collection("cleaning")
                            .document(task.id).set(task).await()
                    }

                    // Notes
                    val notesJson = dataNode.child("notes").getValue(String::class.java) ?: ""
                    DataParser.parseNotes(notesJson).forEach { note ->
                        val noteDoc = getGroupsColl().document(groupId).collection("notes").document(note.id)
                        
                        val noteMap = mapOf(
                            "title" to note.title,
                            "content" to note.content,
                            "type" to note.type.name,
                            "timestamp" to note.timestamp,
                            "lastUpdatedBy" to note.lastUpdatedBy
                        )
                        noteDoc.set(noteMap).await()

                        note.checklist.forEach { cItem ->
                            noteDoc.collection("checklist").document(cItem.id).set(cItem).await()
                        }
                        
                        migrateComments(noteDoc.collection("comments"), note.comments)
                    }
                } catch (e: Exception) {
                    onStatusUpdate("Error migrating Group $groupId: ${e.message}")
                    throw e
                }
            }

            onStatusUpdate("Migration completed!")
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Migration failed", e)
            false
        }
    }

    private suspend fun clearAllFirestoreData(onStatusUpdate: (String) -> Unit) {
        val collections = listOf("users", "groups", "invites", "suggestions", "public_profiles")
        for (collName in collections) {
            onStatusUpdate("Clearing Firestore collection: $collName...")
            val coll = firestore.collection(collName)
            val snapshot = coll.get().await()
            for (doc in snapshot.documents) {
                if (collName == "groups") {
                    clearSubCollections(doc.reference)
                }
                if (collName == "invites") {
                    val items = doc.reference.collection("items").get().await()
                    items.documents.forEach { it.reference.delete().await() }
                }
                doc.reference.delete().await()
            }
        }
    }

    private suspend fun clearSubCollections(docRef: DocumentReference) {
        val subColls = listOf("members", "shopping", "links", "cleaning", "notes")
        for (subName in subColls) {
            val subColl = docRef.collection(subName)
            val snapshot = subColl.get().await()
            for (doc in snapshot.documents) {
                if (subName == "notes") {
                    val checklist = doc.reference.collection("checklist").get().await()
                    checklist.documents.forEach { it.reference.delete().await() }
                    val comments = doc.reference.collection("comments").get().await()
                    comments.documents.forEach { 
                        clearCommentReplies(it.reference)
                        it.reference.delete().await() 
                    }
                }
                doc.reference.delete().await()
            }
        }
    }

    private suspend fun clearCommentReplies(commentRef: DocumentReference) {
        val replies = commentRef.collection("replies").get().await()
        for (reply in replies.documents) {
            clearCommentReplies(reply.reference)
            reply.reference.delete().await()
        }
    }

    private suspend fun migrateComments(coll: CollectionReference, comments: List<Comment>) {
        comments.forEach { comment ->
            val commentDoc = coll.document(comment.id)
            val commentMap = mapOf(
                "id" to comment.id,
                "content" to comment.content,
                "userId" to comment.userId,
                "timestamp" to comment.timestamp,
                "isEdited" to comment.isEdited,
                "parentId" to comment.parentId
            )
            commentDoc.set(commentMap).await()
            if (comment.replies.isNotEmpty()) {
                migrateComments(commentDoc.collection("replies"), comment.replies)
            }
        }
    }
}
