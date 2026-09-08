package com.jtool.android.features.calendar.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

object GoogleCalendarSyncHelper {

    private const val APP_METADATA_HEADER = "\n\n--- jTool Data ---"

    fun pullFromGoogleCalendar(context: Context, userId: String, calendarId: String, startTime: Long, endTime: Long): List<CalendarEvent>? {
        val events = mutableListOf<CalendarEvent>()
        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(calendarId, startTime.toString(), endTime.toString())

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null) ?: return null
        
        cursor.use { c ->
            val idIdx = c.getColumnIndex(CalendarContract.Events._ID)
            val titleIdx = c.getColumnIndex(CalendarContract.Events.TITLE)
            val descIdx = c.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startIdx = c.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIdx = c.getColumnIndex(CalendarContract.Events.DTEND)
            val allDayIdx = c.getColumnIndex(CalendarContract.Events.ALL_DAY)

            while (c.moveToNext()) {
                val gId = c.getLong(idIdx)
                val title = c.getString(titleIdx) ?: ""
                val fullDesc = c.getString(descIdx) ?: ""
                val start = c.getLong(startIdx)
                val end = c.getLong(endIdx)
                val allDay = c.getInt(allDayIdx) == 1

                val (cleanNote, metadata) = parseMetadata(fullDesc)

                val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())
                val endLdt = if (end > 0) LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()) else startLdt.plusHours(1)

                val type = metadata["Type"]?.let { 
                    try { CalendarEventTypeEnum.valueOf(it.uppercase()) } catch (_: Exception) { CalendarEventTypeEnum.COMMON }
                } ?: CalendarEventTypeEnum.COMMON

                events.add(CalendarEvent(
                    id = "google_$gId",
                    title = title,
                    note = cleanNote,
                    date = startLdt.toLocalDate().toString(),
                    startTime = if (allDay) "00:00" else startLdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime = if (allDay) "23:59" else endLdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    allDay = allDay,
                    googleEventId = gId.toString(),
                    googleCalendarId = calendarId,
                    dtStart = start,
                    dtEnd = end,
                    ownerUid = userId,
                    createdBy = "Google Calendar",
                    origin = metadata["Group"] ?: "Google Calendar",
                    type = type
                ))
            }
        }
        return events
    }

    fun pushToGoogleCalendar(context: Context, event: CalendarEvent, calendarId: String, groupName: String? = null): Long? {
        val metadataString = buildString {
            append(APP_METADATA_HEADER)
            append("\nType: ${event.type.name}")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.dtStart)
            put(CalendarContract.Events.DTEND, event.dtEnd)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, "${event.note}$metadataString")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
        }

        return if (event.googleEventId != null) {
            val updateUri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, event.googleEventId)
            context.contentResolver.update(updateUri, values, null, null)
            event.googleEventId.toLong()
        } else {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong()
        }
    }

    private fun parseMetadata(description: String): Pair<String, Map<String, String>> {
        if (!description.contains(APP_METADATA_HEADER)) return description to emptyMap()
        
        val parts = description.split(APP_METADATA_HEADER)
        val cleanNote = parts[0].trim()
        val metaPart = parts.getOrNull(1) ?: return cleanNote to emptyMap()
        
        val metadata = mutableMapOf<String, String>()
        metaPart.lines().forEach { line ->
            if (line.contains(": ")) {
                val kv = line.split(": ", limit = 2)
                metadata[kv[0].trim()] = kv[1].trim()
            }
        }
        return cleanNote to metadata
    }

    fun deleteFromGoogleCalendar(context: Context, googleEventId: String): Boolean {
        return try {
            val deleteUri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, googleEventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e("GoogleSync", "Delete failed", e)
            false
        }
    }

    fun getAvailableCalendars(context: Context): List<Pair<String, String>> {
        val calendars = mutableListOf<Pair<String, String>>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )
        
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx)
                val account = cursor.getString(accountIdx)
                calendars.add(id to "$name ($account)")
            }
        }
        return calendars
    }
}
