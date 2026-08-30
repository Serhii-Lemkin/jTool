package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MigrationResult(
    val updatedCount: Int = 0,
    val missedCount: Int = 0,
    val fieldMissingCount: Int = 0,
    val updatedPaths: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

class MigrationHelper(private val firestore: FirebaseFirestore) {

    suspend fun migrateDataIds(onProgress: (String) -> Unit): MigrationResult {
        var updatedCount = 0
        var missedCount = 0
        var fieldMissingCount = 0
        val updatedPaths = mutableListOf<String>()
        val errors = mutableListOf<String>()

        try {
            onProgress("Scanning global collections...")
            
            val globalSupport = migrateCollection(firestore.collection("support_requests"), "Support Requests", enumFields = mapOf("status" to "OPENED", "type" to "BUG"))
            updatedCount += globalSupport.updatedCount
            missedCount += globalSupport.missedCount
            fieldMissingCount += globalSupport.fieldMissingCount
            updatedPaths.addAll(globalSupport.updatedPaths)
            errors.addAll(globalSupport.errors)

            val globalUsers = migrateCollection(firestore.collection("users"), "Users", idFieldName = "uid")
            updatedCount += globalUsers.updatedCount
            missedCount += globalUsers.missedCount
            fieldMissingCount += globalUsers.fieldMissingCount
            updatedPaths.addAll(globalUsers.updatedPaths)
            errors.addAll(globalUsers.errors)

            val globalUserData = migrateCollection(firestore.collection("user_data"), "User Data")
            updatedCount += globalUserData.updatedCount
            missedCount += globalUserData.missedCount
            fieldMissingCount += globalUserData.fieldMissingCount
            updatedPaths.addAll(globalUserData.updatedPaths)
            errors.addAll(globalUserData.errors)

            val globalInvites = migrateCollection(firestore.collection("invites"), "Invites", idFieldName = "inviteId")
            updatedCount += globalInvites.updatedCount
            missedCount += globalInvites.missedCount
            fieldMissingCount += globalInvites.fieldMissingCount
            updatedPaths.addAll(globalInvites.updatedPaths)
            errors.addAll(globalInvites.errors)

            val groupsSnapshot = firestore.collection("groups").get().await()
            val totalGroups = groupsSnapshot.size()
            
            groupsSnapshot.documents.forEachIndexed { gIndex, gDoc ->
                val groupId = gDoc.id
                onProgress("Migrating Group $groupId (${gIndex + 1}/$totalGroups)...")
                
                val resultShopping = migrateCollection(gDoc.reference.collection("shopping"), "Shopping", enumFields = mapOf("aisle" to "COMMON"))
                val resultCleaning = migrateCollection(gDoc.reference.collection("cleaning"), "Cleaning")
                val resultLinks = migrateCollection(gDoc.reference.collection("links"), "Links")
                
                updatedCount += resultShopping.updatedCount + resultCleaning.updatedCount + resultLinks.updatedCount
                missedCount += resultShopping.missedCount + resultCleaning.missedCount + resultLinks.missedCount
                fieldMissingCount += resultShopping.fieldMissingCount + resultCleaning.fieldMissingCount + resultLinks.fieldMissingCount
                updatedPaths.addAll(resultShopping.updatedPaths + resultCleaning.updatedPaths + resultLinks.updatedPaths)
                errors.addAll(resultShopping.errors + resultCleaning.errors + resultLinks.errors)

                val notesColl = gDoc.reference.collection("notes")
                val notesSnapshot = notesColl.get().await()
                notesSnapshot.documents.forEach { nDoc ->
                    var noteUpdated = false
                    if (migrateDocField(nDoc, "id")) noteUpdated = true
                    if (migrateDocField(nDoc, "type", isEnum = true)) noteUpdated = true
                    
                    if (noteUpdated) {
                        updatedCount++
                        updatedPaths.add(nDoc.reference.path)
                    } else {
                        if (nDoc.contains("id") || nDoc.contains("type")) missedCount++ else fieldMissingCount++
                    }
                    
                    val resultChecklist = migrateCollection(nDoc.reference.collection("checklist"), "Checklist")
                    updatedCount += resultChecklist.updatedCount
                    missedCount += resultChecklist.missedCount
                    fieldMissingCount += resultChecklist.fieldMissingCount
                    updatedPaths.addAll(resultChecklist.updatedPaths)
                    errors.addAll(resultChecklist.errors)
                    
                    val resultComments = migrateCollection(nDoc.reference.collection("comments"), "Comments")
                    updatedCount += resultComments.updatedCount
                    missedCount += resultComments.missedCount
                    fieldMissingCount += resultComments.fieldMissingCount
                    updatedPaths.addAll(resultComments.updatedPaths)
                    errors.addAll(resultComments.errors)
                }
            }

            onProgress("Migration Finished!")
        } catch (e: Exception) {
            val msg = "Fatal migration error: ${e.localizedMessage}"
            android.util.Log.e("MigrationHelper", msg, e)
            errors.add(msg)
        }
        
        return MigrationResult(updatedCount, missedCount, fieldMissingCount, updatedPaths, errors)
    }

    private suspend fun migrateCollection(
        collection: com.google.firebase.firestore.CollectionReference,
        label: String,
        idFieldName: String = "id",
        enumFields: Map<String, String> = emptyMap()
    ): MigrationResult {
        var updated = 0
        var missed = 0
        var missing = 0
        val updatedPaths = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        try {
            val snapshot = collection.get().await()
            snapshot.documents.forEach { doc ->
                try {
                    var docUpdated = false
                    if (migrateDocField(doc, idFieldName)) docUpdated = true
                    
                    enumFields.forEach { (field, default) ->
                        if (migrateDocField(doc, field, isEnum = true)) docUpdated = true
                    }

                    if (docUpdated) {
                        updated++
                        updatedPaths.add(doc.reference.path)
                        android.util.Log.d("MigrationHelper", "Successfully updated $label document at path: ${doc.reference.path}")
                    } else {
                        if (doc.contains(idFieldName)) {
                            missed++
                        } else {
                            missing++
                        }
                    }
                } catch (e: Exception) {
                    val msg = "Failed to migrate $label document ${doc.id}: ${e.localizedMessage}"
                    android.util.Log.e("MigrationHelper", msg, e)
                    errors.add(msg)
                }
            }
        } catch (e: Exception) {
            val msg = "Failed to fetch $label collection: ${e.localizedMessage}"
            android.util.Log.e("MigrationHelper", msg, e)
            errors.add(msg)
        }
        
        return MigrationResult(updated, missed, missing, updatedPaths, errors)
    }

    private suspend fun migrateDocField(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        fieldName: String,
        isEnum: Boolean = false
    ): Boolean {
        val value = doc.get(fieldName)
        if (value != null) {
            if (isEnum && value is String) {
                val upperValue = value.uppercase()
                if (value != upperValue) {
                    doc.reference.update(fieldName, upperValue).await()
                    return true
                }
            } else if (value !is String) {
                doc.reference.update(fieldName, value.toString()).await()
                return true
            }
        }
        return false
    }
}
