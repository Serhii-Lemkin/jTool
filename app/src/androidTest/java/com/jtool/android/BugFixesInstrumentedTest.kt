package com.jtool.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.user.NotesConfig
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.vaults.common.VaultDocUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BugFixesInstrumentedTest {

    @Test
    fun releaseNotesDesc_usesInstalledAppVersionDynamically() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = pInfo.versionName ?: ""
        @Suppress("DEPRECATION")
        val versionCode = pInfo.longVersionCode.toString()
        assertTrue(versionName.isNotEmpty())
        val formatted = String.format(Loc.Settings.releaseNotesDesc, versionName, versionCode)
        assertTrue(formatted.contains(versionName))
        assertTrue(formatted.contains(versionCode))
        assertFalse(formatted.contains("2.8.0"))
    }

    @Test
    fun settingsManager_persistsAndRestoresNotesConfig() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsManager(context)
        val previous = settings.getNotesConfig()
        try {
            settings.saveNotesConfig(NotesConfig(previewMaxLines = 4))
            assertEquals(4, settings.getNotesConfig().previewMaxLines)
        } finally {
            settings.saveNotesConfig(previous)
        }
    }

    @Test
    fun vaultDocUtils_offlineCacheAndSafeOpenReturnExpectedResults() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("receipt.pdf", VaultDocUtils.ensureExtension("receipt", "pdf"))

        val testDoc = VaultDocument(
            id = "inst_test_doc_999",
            vaultId = "inst_test_vault_999",
            name = "receipt",
            fileType = "pdf",
            fileNames = listOf("receipt"),
            fileTypes = listOf("pdf")
        )
        val vaultTestDir = File(context.filesDir, "vaults/inst_test_vault_999")
        try {
            assertFalse(VaultDocUtils.isDocumentDownloadedLocally(context, testDoc))
            val localFile = VaultDocUtils.getOfflineLocalFile(context, testDoc, 0)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            assertTrue(VaultDocUtils.isDocumentDownloadedLocally(context, testDoc))

            val missingDoc = testDoc.copy(id = "nonexistent_doc_999")
            assertFalse(VaultDocUtils.openLocalFile(context, missingDoc, 0))
            assertFalse(VaultDocUtils.openExternalUrl(context, "jtoolunsupported://invalid-host/file"))
        } finally {
            vaultTestDir.deleteRecursively()
        }
    }

    @Test
    fun localeAssets_containAllNewKeysInAllLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requiredKeys = listOf(
            "settings_releaseNotesDesc",
            "notes_previewLength",
            "notes_previewLines",
            "vaults_openFileError",
            "vaults_shareError"
        )
        listOf("en.json", "uk.json", "he.json").forEach { fileName ->
            val raw = context.assets.open("locales/$fileName").bufferedReader().use { it.readText() }
            val json = JSONObject(raw).getJSONObject("strings")
            requiredKeys.forEach { key ->
                assertTrue("Missing key $key in $fileName", json.has(key) && json.getString(key).isNotBlank())
            }
        }
    }
}
