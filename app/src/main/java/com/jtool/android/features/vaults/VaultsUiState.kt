package com.jtool.android.features.vaults

import android.net.Uri
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.models.vaults.enums.VaultDocFilterEnum

data class AttachedFile(
    val uri: Uri,
    var fileName: String,
    val extension: String
)

data class PendingDocUpload(
    val vault: Vault,
    val files: List<AttachedFile>,
    val docName: String,
    val tag: String,
    val docDate: Long,
    val description: String
)

enum class VaultSortEnum {
    UPLOAD_DATE_DESC,
    NAME_ASC,
    DOC_DATE_DESC,
    TAG_ASC
}

data class VaultsUiState(
    val vaults: List<Vault> = emptyList(),
    val selectedVault: Vault? = null,
    val selectedDoc: VaultDocument? = null,
    val docs: List<VaultDocument> = emptyList(),
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isOffline: Boolean = false,
    val isSearchVisible: Boolean = false,
    val pagingLimit: Int = 15,
    val filterType: VaultDocFilterEnum = VaultDocFilterEnum.ALL,
    val sortType: VaultSortEnum = VaultSortEnum.UPLOAD_DATE_DESC,
    val searchQuery: String = "",
    val nameFilter: String = "",
    val descFilter: String = "",
    val tagFilter: String = "",
    val downloadedOnly: Boolean = false,
    val docDateFrom: Long? = null,
    val docDateTo: Long? = null,
    val uploadDateFrom: Long? = null,
    val uploadDateTo: Long? = null,
    val pendingVault: Pair<String, String>? = null,
    val pendingDoc: PendingDocUpload? = null,
    val missingFileIds: Set<String> = emptySet(),
    val downloadingDocIds: Set<String> = emptySet(),
    val deletingDocIds: Set<String> = emptySet(),
    val updatingDocIds: Set<String> = emptySet(),
    val savingToDownloadsDocIds: Set<String> = emptySet(),
    val offlineVersion: Long = 0L
) {
    val filteredVaults: List<Vault>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return vaults
            return vaults.filter { vault ->
                vault.name.lowercase().contains(query) ||
                vault.description.lowercase().contains(query)
            }
        }

    val filteredDocs: List<VaultDocument>
        get() {
            val qName = nameFilter.ifBlank { searchQuery }.trim().lowercase()
            val qDesc = descFilter.trim().lowercase()
            val qTag = tagFilter.trim().lowercase()

            return docs.filter { doc ->
                val matchesName = qName.isEmpty() || doc.name.lowercase().contains(qName)
                val matchesDesc = qDesc.isEmpty() || doc.description.lowercase().contains(qDesc)
                val matchesTag = qTag.isEmpty() || doc.tag.lowercase().contains(qTag)
                val matchesDownloaded = !downloadedOnly || doc.localPath.isNotEmpty()
                
                val docDate = doc.documentTimestamp
                val matchesDocDate = (docDateFrom == null || docDate >= docDateFrom) && (docDateTo == null || docDate <= docDateTo)
                
                val upDate = doc.uploadTimestamp
                val matchesUpDate = (uploadDateFrom == null || upDate >= uploadDateFrom) && (uploadDateTo == null || upDate <= uploadDateTo)

                matchesName && matchesDesc && matchesTag && matchesDownloaded && matchesDocDate && matchesUpDate
            }
        }

    val sortedAndFilteredDocs: List<VaultDocument>
        get() {
            val baseList = filteredDocs
            return when (sortType) {
                VaultSortEnum.UPLOAD_DATE_DESC -> baseList.sortedByDescending { it.uploadTimestamp }
                VaultSortEnum.NAME_ASC -> baseList.sortedBy { it.name.lowercase() }
                VaultSortEnum.DOC_DATE_DESC -> baseList.sortedByDescending { it.documentTimestamp }
                VaultSortEnum.TAG_ASC -> baseList.sortedBy { it.tag.lowercase() }
            }
        }
}
