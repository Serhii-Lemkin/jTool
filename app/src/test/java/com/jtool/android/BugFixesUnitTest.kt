package com.jtool.android

import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.user.NotesConfig
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.vaults.common.GoogleDriveTransferHelper
import com.jtool.android.features.vaults.common.VaultsOperationsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BugFixesUnitTest {

    @Test
    fun findExtension_matchesDirectAndContinuationAndPrefixAndICalAndDeepLink() {
        val ext = CalendarEventExtension(
            googleEventId = "g123",
            sharedGroupIds = listOf("grp1"),
            title = "Team Sync",
            dateTimestamp = 1_700_000_000_000L
        )
        val mapByRaw = mapOf("g123" to ext)
        assertEquals(ext, CalendarEventHelper.findExtensionForEvent(CalendarEvent(id = "g123"), mapByRaw))
        assertEquals(ext, CalendarEventHelper.findExtensionForEvent(CalendarEvent(id = "g123_cont_2026-05-01"), mapByRaw))
        assertEquals(ext, CalendarEventHelper.findExtensionForEvent(CalendarEvent(id = "ext_g123"), mapByRaw))
        assertEquals(ext, CalendarEventHelper.findExtensionForEvent(CalendarEvent(id = "local_9", iCalUID = "g123"), mapByRaw))
        assertEquals(
            ext,
            CalendarEventHelper.findExtensionForEvent(
                CalendarEvent(id = "other", note = "Details https://jtool.app/event?id=g123"),
                mapByRaw
            )
        )
    }

    @Test
    fun findExtension_matchesAllDaySameDateAndTitleFallback() {
        val date = LocalDate.of(2026, 6, 15)
        val morningMs = date.atTime(9, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val noonMs = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ext = CalendarEventExtension(
            googleEventId = "remote_99",
            sharedGroupIds = listOf("family"),
            title = "Anniversary",
            dateTimestamp = morningMs
        )
        val extensions = mapOf("remote_99" to ext)
        val allDayEvent = CalendarEvent(
            id = "local_77",
            title = " anniversary ",
            allDay = true,
            dtStart = noonMs
        )
        val found = CalendarEventHelper.findExtensionForEvent(allDayEvent, extensions)
        assertNotNull(found)
        assertEquals(listOf("family"), found?.sharedGroupIds)

        val mismatchedTitle = CalendarEvent(id = "local_78", title = "Other", allDay = true, dtStart = noonMs)
        assertNull(CalendarEventHelper.findExtensionForEvent(mismatchedTitle, extensions))
    }

    @Test
    fun driveUrlExtraction_handlesStandardAndQueryUrls() {
        assertEquals(
            "1AbCdEfGhIjKlMnOpQr",
            GoogleDriveTransferHelper.extractDriveFileIdFromUrl("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQr/view?usp=sharing")
        )
        assertEquals(
            "9XyZ_123-abc",
            GoogleDriveTransferHelper.extractDriveFileIdFromUrl("https://drive.google.com/uc?export=download&id=9XyZ_123-abc")
        )
        assertEquals(
            "456def",
            GoogleDriveTransferHelper.extractDriveFileIdFromUrl("https://drive.google.com/open?id=456def")
        )
        assertEquals("", GoogleDriveTransferHelper.extractDriveFileIdFromUrl(""))
        assertEquals("", GoogleDriveTransferHelper.extractDriveFileIdFromUrl("https://example.com/file"))
    }

    @Test
    fun resolveDriveFileId_skipsPlaceholdersAndFallsBackToUrl() {
        val docWithArrayUrl = VaultDocument(
            id = "d1",
            driveFileId = "local_temp",
            driveFileIds = listOf("jtools_placeholder", "fileIdFromArray"),
            driveUrls = listOf("https://drive.google.com/file/d/UrlFallback0/view", "")
        )
        assertEquals("UrlFallback0", GoogleDriveTransferHelper.resolveDriveFileId(docWithArrayUrl, 0))
        assertEquals("fileIdFromArray", GoogleDriveTransferHelper.resolveDriveFileId(docWithArrayUrl, 1))
    }

    @Test
    fun vaultsOperations_checksOwnershipAndMapsTags() {
        val vault = Vault(id = "v1", name = "Shared", createdBy = "vaultOwnerUid")
        val doc = VaultDocument(id = "d1", vaultId = "v1", lastUpdatedBy = "docOwnerUid")

        assertTrue(VaultsOperationsHelper.isDocOrVaultOwner(doc, vault, "docOwnerUid", ""))
        assertTrue(VaultsOperationsHelper.isDocOrVaultOwner(doc, vault, "otherUser", "vaultOwnerUid"))
        assertFalse(VaultsOperationsHelper.isDocOrVaultOwner(doc, vault, "nonOwnerUser", "nonOwnerAuth"))

        assertEquals("#WARRANTY#", VaultsOperationsHelper.mapTagToStandardKey(" ${VaultsLoc.tagWarranty} "))
        assertEquals("CustomTag", VaultsOperationsHelper.mapTagToStandardKey(" CustomTag "))
    }

    @Test
    fun notesConfig_defaultsAndCopyWorkCleanly() {
        val defaultConfig = NotesConfig()
        assertEquals(2, defaultConfig.previewMaxLines)
        val updated = defaultConfig.copy(previewMaxLines = 5)
        assertEquals(5, updated.previewMaxLines)
    }
}
