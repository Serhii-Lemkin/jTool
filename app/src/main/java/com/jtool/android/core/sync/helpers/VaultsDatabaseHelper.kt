package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VaultsDatabaseHelper(private val firestore: FirebaseFirestore) {

    fun getVaultsColl(groupId: String) =
        firestore.collection("groups").document(groupId).collection("vaults")

    private fun getVaultDocsColl(groupId: String, vaultId: String) =
        getVaultsColl(groupId).document(vaultId).collection("docs")

    suspend fun getVaultsDirect(groupId: String): List<Vault> {
        val snapshot = getVaultsColl(groupId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Vault::class.java)?.apply { id = doc.id }
        }
    }

    suspend fun getVaultDocsDirect(groupId: String, vaultId: String): List<VaultDocument> {
        val snapshot = getVaultDocsColl(groupId, vaultId)
            .orderBy("uploadTimestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(VaultDocument::class.java)?.apply { id = doc.id }
        }
    }

    fun getVaultsFlow(groupId: String): Flow<List<Vault>> = callbackFlow {
        val listener = getVaultsColl(groupId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Vault::class.java)?.apply { id = doc.id }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getVaultDocsFlow(groupId: String, vaultId: String): Flow<List<VaultDocument>> = callbackFlow {
        val listener = getVaultDocsColl(groupId, vaultId)
            .orderBy("uploadTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(VaultDocument::class.java)?.apply { id = doc.id }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addVault(groupId: String, vault: Vault): String {
        val ref = getVaultsColl(groupId).document()
        val finalVault = vault.copy(id = ref.id)
        ref.set(finalVault).await()
        return ref.id
    }

    suspend fun updateVault(groupId: String, vault: Vault) {
        getVaultsColl(groupId).document(vault.id).set(vault).await()
    }

    suspend fun deleteVault(groupId: String, vaultId: String) {
        val docsSnapshot = getVaultDocsColl(groupId, vaultId).get().await()
        for (doc in docsSnapshot.documents) {
            getVaultDocsColl(groupId, vaultId).document(doc.id).delete().await()
        }
        getVaultsColl(groupId).document(vaultId).delete().await()
    }

    suspend fun addVaultDoc(groupId: String, vaultId: String, doc: VaultDocument): String {
        val ref = getVaultDocsColl(groupId, vaultId).document()
        val finalDoc = doc.copy(id = ref.id, vaultId = vaultId)
        ref.set(finalDoc).await()
        return ref.id
    }

    suspend fun updateVaultDoc(groupId: String, vaultId: String, doc: VaultDocument) {
        getVaultDocsColl(groupId, vaultId).document(doc.id).set(doc).await()
    }

    suspend fun deleteVaultDoc(groupId: String, vaultId: String, docId: String) {
        getVaultDocsColl(groupId, vaultId).document(docId).delete().await()
    }
}
