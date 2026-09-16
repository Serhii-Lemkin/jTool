package com.jtool.android.features.calendar.common

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.BuildConfig
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.utils.DeepLinkService
import java.util.TimeZone

object GoogleCalendarSyncHelper {

    private fun safeGetColumnIndex(cursor: Cursor, columnName: String): Int {
        return try { cursor.getColumnIndexOrThrow(columnName) } catch (_: Exception) { -1 }
    }

    private fun Cursor.safeGetString(columnName: String): String? {
        val idx = safeGetColumnIndex(this, columnName)
        return if (idx != -1 && !isNull(idx)) getString(idx) else null
    }

    private fun Cursor.safeGetLong(columnName: String, default: Long = 0L): Long {
        val idx = safeGetColumnIndex(this, columnName)
        return if (idx != -1 && !isNull(idx)) getLong(idx) else default
    }

    private fun Cursor.safeGetInt(columnName: String, default: Int = 0): Int {
        val idx = safeGetColumnIndex(this, columnName)
        return if (idx != -1 && !isNull(idx)) getInt(idx) else default
    }

    private fun applySyncParams(context: Context, uri: Uri, calendarId: String): Uri {
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE),
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.safeGetString(CalendarContract.Calendars.ACCOUNT_NAME)
                    val type = c.safeGetString(CalendarContract.Calendars.ACCOUNT_TYPE)
                    if (!name.isNullOrEmpty() && !type.isNullOrEmpty()) {
                        return uri.buildUpon()
                            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, name)
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, type)
                            .appendQueryParameter("sendNotifications", "0")
                            .build()
                    }
                }
            }
        } catch (_: Exception) {}
        return uri.buildUpon().appendQueryParameter("sendNotifications", "0").build()
    }

    fun pullFromMultipleGoogleCalendars(context: Context, calendarIds: Set<String>, startTime: Long, endTime: Long, userEmail: String? = null): List<CalendarEvent> {
        if (calendarIds.isEmpty()) return emptyList()
        val allEvents = mutableListOf<CalendarEvent>()
        calendarIds.forEach { calId ->
            val events = pullFromGoogleCalendar(context, calId, startTime, endTime, userEmail)
            if (events != null) {
                allEvents.addAll(events)
            }
        }
        return allEvents.distinctBy { it.id }
    }

    fun pullFromGoogleCalendar(context: Context, calendarId: String, startTime: Long, endTime: Long, userEmail: String? = null): List<CalendarEvent>? {
        if (calendarId.isEmpty()) return null
        
        val events = mutableListOf<CalendarEvent>()
        
        val baseInstancesUri = CalendarContract.Instances.CONTENT_URI
        val builder = baseInstancesUri.buildUpon()
        ContentUris.appendId(builder, startTime)
        ContentUris.appendId(builder, endTime)
        val pathUri = builder.build()
        
        val uri = applySyncParams(context, pathUri, calendarId)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.EVENT_COLOR_KEY
        )

        val selection = "${CalendarContract.Instances.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId)

        val cursor = try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        } catch (e: Exception) {
            Log.e("GoogleSync", "Instances query failed for $uri", e)
            try {
                context.contentResolver.query(pathUri, projection, selection, selectionArgs, null)
            } catch (e2: Exception) {
                Log.e("GoogleSync", "Final Instances fallback failed", e2)
                null
            }
        } ?: return null
        
        cursor.use { c ->
            while (c.moveToNext()) {
                val gId = c.safeGetLong(CalendarContract.Instances.EVENT_ID)
                val title = c.safeGetString(CalendarContract.Instances.TITLE) ?: ""
                val fullDesc = c.safeGetString(CalendarContract.Instances.DESCRIPTION) ?: ""
                val start = c.safeGetLong(CalendarContract.Instances.BEGIN)
                val end = c.safeGetLong(CalendarContract.Instances.END)
                val allDay = c.safeGetInt(CalendarContract.Instances.ALL_DAY) == 1
                val organizer = c.safeGetString(CalendarContract.Instances.ORGANIZER)
                val colorKey = c.safeGetString(CalendarContract.Events.EVENT_COLOR_KEY)

                val (attendees, status, statuses) = getAttendeesAndStatus(context, gId.toString(), userEmail)

                val event = CalendarEvent(
                    id = gId.toString(),
                    title = title,
                    note = fullDesc,
                    allDay = allDay,
                    dtStart = start,
                    dtEnd = end,
                    attendees = attendees,
                    googleCalendarId = calendarId,
                    currentUserStatus = status,
                    organizer = organizer,
                    attendeeStatuses = statuses,
                    colorKey = colorKey
                )
                events.add(event)
            }
        }

        if (events.isNotEmpty()) enrichWithICalUID(context, events, calendarId)
        return events.distinctBy { it.id + it.dtStart }
    }

    private fun enrichWithICalUID(context: Context, events: MutableList<CalendarEvent>, calendarId: String) {
        val eventIds = events.map { it.id }.distinct()
        if (eventIds.isEmpty()) return

        val baseUri = CalendarContract.Events.CONTENT_URI
        val uri = applySyncParams(context, baseUri, calendarId)

        eventIds.chunked(100).forEach { chunk ->
            val selection = "${CalendarContract.Events._ID} IN (${chunk.joinToString(",")})"
            val projection = arrayOf(CalendarContract.Events._ID, "sync_data1")

            try {
                context.contentResolver.query(uri, projection, selection, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.safeGetLong(CalendarContract.Events._ID).toString()
                        val iCalUID = c.safeGetString("sync_data1")
                        
                        for (i in events.indices) {
                            if (events[i].id == id) {
                                events[i] = events[i].copy(iCalUID = iCalUID ?: events[i].iCalUID)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun getAttendeesAndStatus(context: Context, googleEventId: String, userEmail: String?): Triple<List<String>, Int?, Map<String, Int>> {
        val attendees = mutableListOf<String>()
        val statuses = mutableMapOf<String, Int>()
        var userStatus: Int? = null
        val uri = CalendarContract.Attendees.CONTENT_URI
        val projection = arrayOf(CalendarContract.Attendees.ATTENDEE_EMAIL, CalendarContract.Attendees.ATTENDEE_STATUS)
        val selection = "${CalendarContract.Attendees.EVENT_ID} = ?"
        val selectionArgs = arrayOf(googleEventId)

        val targetEmails = listOfNotNull(
            userEmail?.lowercase()?.trim(),
            FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
        ).filter { it.isNotEmpty() }.toSet()

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val emailIdx = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL)
                val statusIdx = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_STATUS)
                while (cursor.moveToNext()) {
                    val email = cursor.getString(emailIdx)
                    val status = cursor.getInt(statusIdx)
                    if (!email.isNullOrEmpty()) {
                        val cleanEmail = email.lowercase().trim()
                        attendees.add(cleanEmail)
                        statuses[cleanEmail] = status
                        if (targetEmails.contains(cleanEmail)) {
                            userStatus = status
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return Triple(attendees, userStatus, statuses)
    }

    fun updateAttendeeStatus(context: Context, googleEventId: String, userEmail: String, status: Int): Boolean {
        if (googleEventId.isEmpty() || userEmail.isEmpty()) return false
        val cleanId = googleEventId.removePrefix("ext_")
        val localEventId = resolveLocalEventId(context, cleanId) ?: if (cleanId.all { it.isDigit() }) cleanId else null
        if (localEventId == null) return false

        val baseUri = CalendarContract.Attendees.CONTENT_URI
        val silentUri = baseUri.buildUpon().appendQueryParameter("sendNotifications", "0").build()
        val selection = "${CalendarContract.Attendees.EVENT_ID} = ? AND LOWER(${CalendarContract.Attendees.ATTENDEE_EMAIL}) = ?"
        val selectionArgs = arrayOf(localEventId, userEmail.lowercase().trim())

        val values = ContentValues().apply { put(CalendarContract.Attendees.ATTENDEE_STATUS, status) }
        
        val rowsUpdated = try {
            context.contentResolver.update(silentUri, values, selection, selectionArgs)
        } catch (_: Exception) {
            context.contentResolver.update(baseUri, values, selection, selectionArgs)
        }

        if (rowsUpdated > 0) return true

        val insertValues = ContentValues().apply {
            put(CalendarContract.Attendees.EVENT_ID, localEventId)
            put(CalendarContract.Attendees.ATTENDEE_EMAIL, userEmail.trim())
            put(CalendarContract.Attendees.ATTENDEE_STATUS, status)
            put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED)
            put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE)
        }

        return try {
            context.contentResolver.insert(silentUri, insertValues) != null
        } catch (_: Exception) {
            context.contentResolver.insert(baseUri, insertValues) != null
        }
    }

    private fun resolveLocalEventId(context: Context, eventIdOrSyncId: String): String? {
        if (eventIdOrSyncId.isEmpty()) return null
        val baseUri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(CalendarContract.Events._ID)
        val selections = listOf(
            "${CalendarContract.Events._ID} = ?",
            "${CalendarContract.Events._SYNC_ID} = ?",
            "sync_data1 = ?"
        )

        selections.forEach { selection ->
            try {
                context.contentResolver.query(baseUri, projection, selection, arrayOf(eventIdOrSyncId), null)?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.safeGetLong(CalendarContract.Events._ID)
                        if (id > 0) return id.toString()
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun pullEventById(context: Context, googleEventId: String, userEmail: String? = null): CalendarEvent? {
        val baseUri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.ORGANIZER,
            CalendarContract.Events._SYNC_ID,
            CalendarContract.Events.EVENT_COLOR_KEY
        )
        
        val selections = listOf(
            "${CalendarContract.Events._ID} = ?",
            "${CalendarContract.Events._SYNC_ID} = ?",
            "sync_data1 = ?"
        )

        selections.forEach { selection ->
            try {
                context.contentResolver.query(baseUri, projection, selection, arrayOf(googleEventId), null)?.use { c ->
                    if (c.moveToFirst()) {
                        val calId = c.safeGetString(CalendarContract.Events.CALENDAR_ID) ?: ""
                        if (calId.isNotEmpty()) {
                            val syncUri = applySyncParams(context, baseUri, calId)
                            context.contentResolver.query(syncUri, projection, selection, arrayOf(googleEventId), null)?.use { sc ->
                                if (sc.moveToFirst()) return parseEvent(context, sc, userEmail, calId)
                            }
                        }
                        return parseEvent(context, c, userEmail, calId)
                    }
                }
            } catch (e: Exception) {}
        }
        return null
    }

    private fun parseEvent(context: Context, c: Cursor, userEmail: String?, calendarId: String): CalendarEvent {
        val gId = c.safeGetLong(CalendarContract.Events._ID)
        val title = c.safeGetString(CalendarContract.Events.TITLE) ?: ""
        val fullDesc = c.safeGetString(CalendarContract.Events.DESCRIPTION) ?: ""
        val start = c.safeGetLong(CalendarContract.Events.DTSTART)
        val end = c.safeGetLong(CalendarContract.Events.DTEND)
        val allDay = c.safeGetInt(CalendarContract.Events.ALL_DAY) == 1
        val iCalUID = c.safeGetString("sync_data1")
        val organizer = c.safeGetString(CalendarContract.Events.ORGANIZER)
        val colorKey = c.safeGetString(CalendarContract.Events.EVENT_COLOR_KEY)

        val (attendees, status, statuses) = getAttendeesAndStatus(context, gId.toString(), userEmail)

        return CalendarEvent(
            id = gId.toString(),
            title = title,
            note = fullDesc,
            allDay = allDay,
            dtStart = start,
            dtEnd = end,
            attendees = attendees,
            googleCalendarId = calendarId,
            currentUserStatus = status,
            iCalUID = iCalUID,
            organizer = organizer,
            attendeeStatuses = statuses,
            colorKey = colorKey
        )
    }

    fun pushToGoogleCalendar(context: Context, event: CalendarEvent, calendarId: String, ownerEmail: String? = null): String? {
        val targetCalendarId = if (calendarId.isNotEmpty()) calendarId else {
            getAvailableCalendars(context).firstOrNull()?.first ?: ""
        }
        if (targetCalendarId.isEmpty()) return null

        val baseUri = CalendarContract.Events.CONTENT_URI

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.dtStart)
            put(CalendarContract.Events.DTEND, event.dtEnd)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.note)
            put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (event.allDay) "UTC" else TimeZone.getDefault().id)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            if (!event.colorKey.isNullOrEmpty()) {
                put(CalendarContract.Events.EVENT_COLOR_KEY, event.colorKey)
            }
        }

        val cleanId = event.id.removePrefix("ext_").removePrefix("virtual_")
        val resolvedLocalId = if (cleanId.isNotEmpty() && cleanId.all { it.isDigit() }) cleanId else resolveLocalEventId(context, cleanId)

        val localId = if (resolvedLocalId != null) {
            val updateUri = Uri.withAppendedPath(baseUri, resolvedLocalId)
            try {
                context.contentResolver.update(updateUri, values, null, null)
            } catch (e: Exception) {
                Log.e("GoogleSync", "Update failed", e)
            }
            resolvedLocalId
        } else {
            val margin = 60_000L
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val args = arrayOf(targetCalendarId, event.title, (event.dtStart!! - margin).toString(), (event.dtStart + margin).toString())
            val existingId = try {
                context.contentResolver.query(baseUri, arrayOf(CalendarContract.Events._ID), selection, args, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            } catch (_: Exception) { null }

            if (existingId != null) {
                val updateUri = Uri.withAppendedPath(baseUri, existingId)
                try { context.contentResolver.update(updateUri, values, null, null) } catch (_: Exception) {}
                existingId
            } else {
                try {
                    val resultUri = context.contentResolver.insert(baseUri, values)
                    resultUri?.lastPathSegment
                } catch (e: Exception) { Log.e("GoogleSync", "Insert failed", e); null }
            }
        }

        if (localId != null) {
            val globalId = fetchGlobalId(context, localId, targetCalendarId) ?: localId
            val linkText = DeepLinkService.createEventNoteLinkText(globalId)
            
            val descUri = Uri.withAppendedPath(baseUri, localId)
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
                        try { context.contentResolver.update(descUri, updateValues, null, null) } catch (_: Exception) {}
                    }
                }
            }

            val filteredAttendees = if (ownerEmail != null) {
                event.attendees.filter { it.lowercase().trim() != ownerEmail.lowercase().trim() }
            } else event.attendees
            updateAttendees(context, localId, filteredAttendees, ownerEmail)
            
            return globalId
        }
        return null
    }

    private fun fetchGlobalId(context: Context, localId: String, calendarId: String): String? {
        val baseUri = CalendarContract.Events.CONTENT_URI
        val syncUri = applySyncParams(context, baseUri, calendarId)
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events._SYNC_ID, "sync_data1")

        try {
            context.contentResolver.query(syncUri, projection, "${CalendarContract.Events._ID} = ?", arrayOf(localId), null)?.use { c ->
                if (c.moveToFirst()) {
                    val iCalUID = c.safeGetString("sync_data1")
                    if (!iCalUID.isNullOrEmpty()) return iCalUID
                    val syncId = c.safeGetString(CalendarContract.Events._SYNC_ID)
                    if (!syncId.isNullOrEmpty()) return syncId
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun updateAttendees(context: Context, googleEventId: String, attendees: List<String>, ownerEmail: String? = null) {
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
            val syncUri = baseUri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter("sendNotifications", "0")
                .build()

            var rowsDeleted = 0
            
            if (cleanId.all { it.isDigit() }) {
                val deleteUri = ContentUris.withAppendedId(baseUri, cleanId.toLongOrNull() ?: 0L)
                try {
                    rowsDeleted = context.contentResolver.delete(deleteUri, null, null)
                } catch (_: Exception) {}
            }

            if (rowsDeleted == 0) {
                val selection = "${CalendarContract.Events._ID} = ? OR ${CalendarContract.Events._SYNC_ID} = ? OR sync_data1 = ?"
                rowsDeleted = context.contentResolver.delete(baseUri, selection, arrayOf(cleanId, cleanId, cleanId))
            }

            if (rowsDeleted == 0 && cleanId.all { it.isDigit() }) {
                val deleteSyncUri = ContentUris.withAppendedId(syncUri, cleanId.toLongOrNull() ?: 0L)
                try {
                    rowsDeleted = context.contentResolver.delete(deleteSyncUri, null, null)
                } catch (_: Exception) {}
            }

            Log.d("GoogleSync", "deleteFromGoogleCalendar ID $cleanId result: $rowsDeleted rows deleted")
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e("GoogleSync", "Failed to delete event $googleEventId: ${e.message}", e)
            false
        }
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
}
