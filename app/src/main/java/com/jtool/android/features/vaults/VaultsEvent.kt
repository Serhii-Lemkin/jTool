package com.jtool.android.features.vaults

import android.net.Uri
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.models.vaults.enums.VaultDocFilterEnum

sealed interface VaultsEvent {
    data class SetGroupId(val groupId: String?) : VaultsEvent
    data class CreateVault(
        val name: String,
        val description: String,
        val onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) : VaultsEvent
    data class UpdateVault(val vault: Vault) : VaultsEvent
    data class DeleteVault(val vaultId: String, val onComplete: (Boolean) -> Unit = {}) : VaultsEvent
    data class SelectVault(val vault: Vault?) : VaultsEvent
    data class UploadDoc(
        val vault: Vault,
        val files: List<AttachedFile>,
        val docName: String,
        val tag: String,
        val docDate: Long,
        val description: String,
        val saveOffline: Boolean = false,
        val onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) : VaultsEvent
    data class UpdateDoc(
        val vault: Vault,
        val doc: VaultDocument,
        val newFilesToUpload: List<AttachedFile> = emptyList(),
        val removedDriveFileIds: List<String> = emptyList(),
        val onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) : VaultsEvent
    data class DeleteDoc(val vaultId: String, val docId: String, val onComplete: (Boolean) -> Unit = {}) : VaultsEvent
    data class DeleteSpecificFile(val doc: VaultDocument, val fileIndex: Int) : VaultsEvent
    data class DownloadDocForOffline(val doc: VaultDocument, val fileIndex: Int = 0) : VaultsEvent
    data class SaveToDownloads(val doc: VaultDocument, val fileIndex: Int? = null, val onComplete: (Boolean) -> Unit = {}) : VaultsEvent
    data class DeleteDocOffline(val doc: VaultDocument, val fileIndex: Int = 0) : VaultsEvent
    data class SetFilterType(val filter: VaultDocFilterEnum) : VaultsEvent
    data class UpdateSearchQuery(val query: String) : VaultsEvent
    data class SetAdvancedFilters(
        val name: String,
        val desc: String,
        val tag: String,
        val downloadedOnly: Boolean,
        val docDateFrom: Long?,
        val docDateTo: Long?,
        val uploadDateFrom: Long?,
        val uploadDateTo: Long?
    ) : VaultsEvent
    object ClearAdvancedFilters : VaultsEvent
    object ToggleSearch : VaultsEvent
    object LoadMore : VaultsEvent
    object RetryPendingAction : VaultsEvent
    object Refresh : VaultsEvent
    object ReapplyPermissions : VaultsEvent
    data class ValidateCachedData(val onComplete: (Int) -> Unit = {}) : VaultsEvent
}
