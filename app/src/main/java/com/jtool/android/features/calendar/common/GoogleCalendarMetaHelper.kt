package com.jtool.android.features.calendar.common

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log

object GoogleCalendarMetaHelper {

    fun fetchGlobalId(context: Context, localId: String, syncUri: Uri): String? {
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events._SYNC_ID, "sync_data1")
        return try {
            context.contentResolver.query(syncUri, projection, "${CalendarContract.Events._ID} = ?", arrayOf(localId), null)?.use { c ->
                if (c.moveToFirst()) {
                    val syncIdx = try { c.getColumnIndexOrThrow("sync_data1") } catch (_: Exception) { -1 }
                    val iCalUID = if (syncIdx != -1 && !c.isNull(syncIdx)) c.getString(syncIdx) else null
                    if (!iCalUID.isNullOrEmpty()) return iCalUID
                    val sidIdx = try { c.getColumnIndexOrThrow(CalendarContract.Events._SYNC_ID) } catch (_: Exception) { -1 }
                    val syncId = if (sidIdx != -1 && !c.isNull(sidIdx)) c.getString(sidIdx) else null
                    if (!syncId.isNullOrEmpty()) return syncId
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun updateAttendees(context: Context, googleEventId: String, attendees: List<String>, ownerEmail: String? = null) {
        val baseAttendeesUri = CalendarContract.Attendees.CONTENT_URI
        val silentAttendeesUri = baseAttendeesUri.buildUpon().appendQueryParameter("sendNotifications", "0").build()

        try { context.contentResolver.delete(silentAttendeesUri, "${CalendarContract.Attendees.EVENT_ID} = ?", arrayOf(googleEventId)) } catch (_: Exception) {}

        if (!ownerEmail.isNullOrEmpty()) {
            val ownerValues = ContentValues().apply {
                put(CalendarContract.Attendees.ATTENDEE_EMAIL, ownerEmail.trim())
                put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ORGANIZER)
                put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED)
                put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED)
                put(CalendarContract.Attendees.EVENT_ID, googleEventId)
            }
            try {
                context.contentResolver.insert(silentAttendeesUri, ownerValues)
            } catch (_: Exception) {}
        }

        attendees.forEach { email ->
            val cleanEmail = email.lowercase().trim()
            if (ownerEmail.isNullOrEmpty() || cleanEmail != ownerEmail.lowercase().trim()) {
                val values = ContentValues().apply {
                    put(CalendarContract.Attendees.ATTENDEE_EMAIL, email.trim())
                    put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE)
                    put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED)
                    put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarContract.Attendees.ATTENDEE_STATUS_INVITED)
                    put(CalendarContract.Attendees.EVENT_ID, googleEventId)
                }
                try {
                    context.contentResolver.insert(silentAttendeesUri, values)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteFromGoogleCalendar(context: Context, googleEventId: String): Boolean {
        if (googleEventId.isEmpty()) return false
        val cleanId = googleEventId.removePrefix("ext_").removePrefix("virtual_")
        return try {
            val baseUri = CalendarContract.Events.CONTENT_URI
            val resolvedId = if (cleanId.all { it.isDigit() }) {
                cleanId
            } else {
                GoogleCalendarSyncHelper.resolveLocalEventId(context, cleanId)
            }
            var rowsDeleted = 0

            val targetLocalId = resolvedId ?: if (cleanId.all { it.isDigit() }) cleanId else null
            if (!targetLocalId.isNullOrEmpty()) {
                val deleteUri = ContentUris.withAppendedId(baseUri, targetLocalId.toLongOrNull() ?: 0L)
                try {
                    rowsDeleted = context.contentResolver.delete(deleteUri, null, null)
                } catch (_: Exception) {}

                if (rowsDeleted == 0) {
                    val accountInfo = getEventAccountInfo(context, targetLocalId)
                    val syncUri = if (accountInfo != null) {
                        baseUri.buildUpon()
                            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountInfo.first)
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountInfo.second)
                            .appendQueryParameter("sendNotifications", "0")
                            .build()
                    } else {
                        baseUri.buildUpon()
                            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                            .appendQueryParameter("sendNotifications", "0")
                            .build()
                    }
                    val deleteSyncUri = ContentUris.withAppendedId(syncUri, targetLocalId.toLongOrNull() ?: 0L)
                    try {
                        rowsDeleted = context.contentResolver.delete(deleteSyncUri, null, null)
                    } catch (_: Exception) {}
                }
            }

            if (rowsDeleted == 0) {
                val selection = "${CalendarContract.Events._ID} = ? OR ${CalendarContract.Events._SYNC_ID} = ? OR sync_data1 = ?"
                try {
                    rowsDeleted = context.contentResolver.delete(baseUri, selection, arrayOf(cleanId, cleanId, cleanId))
                } catch (_: Exception) {}
            }

            rowsDeleted > 0 || isEventDeleted(context, cleanId, targetLocalId ?: "")
        } catch (e: Exception) {
            Log.e("GoogleSync", "Failed to delete event $googleEventId: ${e.message}", e)
            false
        }
    }

    private fun getEventAccountInfo(context: Context, localEventId: String): Pair<String, String>? {
        val proj = arrayOf(CalendarContract.Events.CALENDAR_ID)
        val calId = try {
            context.contentResolver.query(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, localEventId.toLongOrNull() ?: 0L),
                proj, null, null, null
            )?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (_: Exception) { null } ?: return null

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE),
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calId),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    val type = c.getString(1)
                    if (!name.isNullOrEmpty() && !type.isNullOrEmpty()) name to type else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    fun isEventDeleted(context: Context, googleEventId: String, localEventId: String = ""): Boolean {
        val cleanId = googleEventId.removePrefix("ext_").removePrefix("virtual_")
        val ids = listOf(cleanId, localEventId).filter { it.isNotEmpty() }.distinct()
        if (ids.isEmpty()) return true
        val placeholders = ids.joinToString(",") { "?" }
        val clauses = mutableListOf(
            "${CalendarContract.Events._SYNC_ID} IN ($placeholders)",
            "sync_data1 IN ($placeholders)"
        )
        val args = mutableListOf<String>()
        args.addAll(ids)
        args.addAll(ids)
        val numericIds = ids.filter { it.all { ch -> ch.isDigit() } }
        if (numericIds.isNotEmpty()) {
            val numPlaceholders = numericIds.joinToString(",") { "?" }
            clauses.add("${CalendarContract.Events._ID} IN ($numPlaceholders)")
            args.addAll(numericIds)
        }
        val selection = "(${clauses.joinToString(" OR ")}) AND ${CalendarContract.Events.DELETED} != 1"
        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                selection,
                args.toTypedArray(),
                null
            )?.use { !it.moveToFirst() } ?: true
        } catch (_: Exception) {
            true
        }
    }

    fun resolveEffectiveCalendarIds(
        context: Context,
        configuredDisplayedIds: Set<String>,
        primaryCalId: String,
        sharedCalId: String,
        userEmail: String?
    ): Set<String> {
        val available = getAvailableCalendars(context)
        if (available.isEmpty()) {
            return (configuredDisplayedIds + primaryCalId + sharedCalId).filter { it.isNotEmpty() }.toSet()
        }
        val validIds = available.map { it.first }.toSet()
        val validConfigured = (configuredDisplayedIds + primaryCalId + sharedCalId)
            .filter { it.isNotEmpty() && validIds.contains(it) }
            .toSet()

        val emails = listOfNotNull(
            userEmail?.lowercase()?.trim(),
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
        ).filter { it.isNotEmpty() }.toSet()

        val emailMatched = if (emails.isNotEmpty()) {
            available.filter { emails.contains(it.third.lowercase().trim()) }.map { it.first }.toSet()
        } else {
            emptySet()
        }

        val combined = (validConfigured + emailMatched).filter { it.isNotEmpty() }.toSet()
        return combined.ifEmpty { validIds }
    }

    fun getAvailableCalendars(context: Context): List<Triple<String, String, String>> {
        val calendars = mutableListOf<Triple<String, String, String>>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME)
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) calendars.add(Triple(cursor.getString(idIdx), cursor.getString(nameIdx), cursor.getString(accountIdx)))
        }
        return calendars
    }

    fun calculateRfcDuration(startMs: Long?, endMs: Long?, allDay: Boolean): String {
        if (allDay) return "P1D"
        val diffSeconds = if (startMs != null && endMs != null && endMs > startMs) {
            (endMs - startMs) / 1000L
        } else {
            3600L
        }
        return "P${diffSeconds}S"
    }

    fun getColorPalette(context: Context): List<Pair<Int, String>> {
        val colors = mutableListOf<Pair<Int, String>>()
        val uri = CalendarContract.Colors.CONTENT_URI
        val projection = arrayOf(CalendarContract.Colors.COLOR, CalendarContract.Colors.COLOR_KEY, CalendarContract.Colors.COLOR_TYPE)
        try {
            context.contentResolver.query(uri, projection, "${CalendarContract.Colors.COLOR_TYPE} = ?", arrayOf("1"), null)?.use { cursor ->
                val colorIdx = cursor.getColumnIndex(CalendarContract.Colors.COLOR)
                val keyIdx = cursor.getColumnIndex(CalendarContract.Colors.COLOR_KEY)
                while (cursor.moveToNext()) colors.add(cursor.getInt(colorIdx) to cursor.getString(keyIdx))
            }
        } catch (_: Exception) {}
        return colors
    }

    fun getEventReminderMinutes(context: Context, eventId: String): Int? {
        if (eventId.isEmpty()) return null
        val uri = CalendarContract.Reminders.CONTENT_URI
        val projection = arrayOf(CalendarContract.Reminders.MINUTES)
        val selection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        try {
            context.contentResolver.query(uri, projection, selection, arrayOf(eventId), null)?.use { c ->
                if (c.moveToFirst()) {
                    return c.getInt(0)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun updateEventReminder(context: Context, localEventId: String, minutes: Int?) {
        if (localEventId.isEmpty()) return
        val uri = CalendarContract.Reminders.CONTENT_URI
        try {
            context.contentResolver.delete(uri, "${CalendarContract.Reminders.EVENT_ID} = ?", arrayOf(localEventId))
            if (minutes != null && minutes >= 0) {
                val values = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, localEventId)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(uri, values)
            }
        } catch (_: Exception) {}
    }

    fun updateEventDescriptionLink(context: Context, localId: String, canonicalId: String) {
        if (localId.isEmpty() || canonicalId.isEmpty()) return
        val baseUri = CalendarContract.Events.CONTENT_URI
        val descUri = Uri.withAppendedPath(baseUri, localId)
        val linkText = com.jtool.android.core.utils.DeepLinkService.createEventNoteLinkText(canonicalId)
        try {
            context.contentResolver.query(descUri, arrayOf(CalendarContract.Events.DESCRIPTION), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val currentDesc = cursor.getString(0) ?: ""
                    val newDesc = if (currentDesc.contains("jTool: ")) {
                        currentDesc.replace(Regex("jTool: https://[^\\s\\n]+"), linkText)
                    } else {
                        if (currentDesc.isEmpty()) linkText else "$currentDesc\n\n$linkText"
                    }
                    if (newDesc != currentDesc) {
                        val updateValues = ContentValues().apply { put(CalendarContract.Events.DESCRIPTION, newDesc) }
                        context.contentResolver.update(descUri, updateValues, null, null)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
