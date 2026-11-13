package com.jtool.android.features.calendar.common

import android.content.Context
import android.provider.CalendarContract
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object CalendarEventHelper {
    var pendingPresetEvent: CalendarEvent? = null

    suspend fun pushAndShareShiftOrEvent(
        context: Context,
        settingsManager: SettingsManager,
        dataManager: DataManager,
        event: CalendarEvent,
        calendarId: String,
        sharedGroupIds: List<String>,
        sendNotification: Boolean = false
    ): String? {
        val attendees = mutableSetOf<String>()
        attendees.addAll(event.attendees)
        sharedGroupIds.forEach { gid ->
            attendees.addAll(dataManager.getGroupMemberEmails(gid))
        }
        val eventToSave = event.copy(attendees = attendees.toList())
        val googleEventId = GoogleCalendarSyncHelper.pushToGoogleCalendar(
            context,
            eventToSave,
            calendarId,
            settingsManager.getGoogleCalendarEmail()
        )
        if (googleEventId != null) {
            val userEmail = settingsManager.getGoogleCalendarEmail()
                .ifEmpty { FirebaseAuth.getInstance().currentUser?.email ?: "" }
                .lowercase().trim()
            val statuses = event.attendeeStatuses.toMutableMap()
            if (userEmail.isNotEmpty()) {
                statuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
            }
            val localId = if (googleEventId.all { it.isDigit() }) googleEventId else ""
            dataManager.saveCalendarExtension(
                CalendarEventExtension(
                    googleEventId = googleEventId,
                    sharedGroupIds = sharedGroupIds,
                    dateTimestamp = event.dtStart ?: 0L,
                    endTimestamp = event.dtEnd ?: 0L,
                    localEventId = localId,
                    ownerUid = settingsManager.getUserId(),
                    attendeeStatuses = statuses,
                    colorKey = event.colorKey,
                    title = event.title
                )
            )
            if (sendNotification) {
                sharedGroupIds.forEach { gid ->
                    dataManager.sendEventNotification(gid, event.title, googleEventId)
                }
            }
        }
        return googleEventId
    }
    fun generateVirtualShifts(options: WorktimeOptions, start: LocalDate, end: LocalDate): List<CalendarEvent> {
        val virtualEvents = mutableListOf<CalendarEvent>()
        val now = LocalDate.now()
        val startOfWeek = now.with(DayOfWeek.valueOf(options.weeklyStartDay)).let {
            if (it.isAfter(now)) it.minusDays(7) else it
        }
        val endOfWeek = startOfWeek.plusDays(6)

        var current = start
        while (!current.isAfter(end)) {
            val dayOfWeek = current.dayOfWeek
            
            val isVisible = !options.showVirtualShiftsOnlyCurrentWeek || (!current.isBefore(startOfWeek) && !current.isAfter(endOfWeek))

            if (isVisible && options.trackingPreference == WorkTrackingPreferenceEnum.LOG_HOURS) {
                val logGoal = options.logSchedule[dayOfWeek.name]
                if (logGoal != null && !logGoal.isConfirmedOff) {
                    val startTime = if (options.useStableApproximateHours) options.stableApproximateStart else logGoal.approximateStartTime
                    val endTime = if (options.useStableApproximateHours) options.stableApproximateEnd else logGoal.approximateEndTime
                    
                    if (!startTime.isNullOrEmpty()) {
                        val startLdt = current.atTime(parseTime(startTime))
                        val endLdt = if (endTime.isNullOrEmpty()) startLdt.plusHours(1) else current.atTime(parseTime(endTime))
                        
                        virtualEvents.add(CalendarEvent(
                            title = Loc.Calendar.shift,
                            id = "virtual_${current}",
                            note = "(${Loc.Calendar.private})",
                            dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        ))
                    }
                }
            }
            current = current.plusDays(1)
        }
        return virtualEvents
    }

    fun parseTime(timeStr: String?): LocalTime {
        if (timeStr.isNullOrBlank()) return LocalTime.MIDNIGHT
        return try {
            val sanitized = timeStr.replace(".", ":")
            if (sanitized.contains(":")) {
                val parts = sanitized.split(":")
                val hour = parts[0].trim().toIntOrNull() ?: 0
                val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
                LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } else {
                val hour = sanitized.trim().toIntOrNull() ?: 0
                LocalTime.of(hour.coerceIn(0, 23), 0)
            }
        } catch (_: Exception) {
            LocalTime.MIDNIGHT
        }
    }

    fun getEventTop(startTime: String, hourHeight: Int): Int {
        return try {
            val time = parseTime(startTime)
            (time.hour * hourHeight) + (time.minute * hourHeight / 60)
        } catch (_: Exception) { 0 }
    }

    fun getEventHeight(startTime: String, endTime: String?, hourHeight: Int): Int {
        return try {
            val start = parseTime(startTime)
            val end = if (endTime.isNullOrBlank()) {
                start.plusHours(1) 
            } else {
                val t = parseTime(endTime)
                if (t.isBefore(start)) LocalTime.MAX else t
            }
            
            val durationMinutes = Duration.between(start, end).toMinutes().toInt()
            (durationMinutes * hourHeight / 60).coerceAtLeast(hourHeight / 4)
        } catch (_: Exception) { hourHeight }
    }

    fun getEventUniqueKey(event: CalendarEvent, index: Int = 0): String {
        return "${event.id}_${event.title}_${event.dtStart}_$index"
    }

    fun calculateOverlapGroups(events: List<CalendarEvent>): Map<String, Pair<Int, Int>> {
        if (events.isEmpty()) return emptyMap()
        
        val sorted = events.sortedBy { it.startTime }
        val result = mutableMapOf<String, Pair<Int, Int>>()
        
        var currentGroup = mutableListOf<CalendarEvent>()
        var groupEndTime = LocalTime.MIN

        for (event in sorted) {
            val startTime = parseTime(event.startTime)
            if (startTime.isBefore(groupEndTime)) {
                currentGroup.add(event)
                val endTime = if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime)
                if (endTime.isAfter(groupEndTime)) groupEndTime = endTime
            } else {
                if (currentGroup.isNotEmpty()) {
                    assignColumns(currentGroup, result)
                }
                currentGroup = mutableListOf(event)
                groupEndTime = if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime)
            }
        }
        if (currentGroup.isNotEmpty()) assignColumns(currentGroup, result)
        
        return result
    }

    private fun assignColumns(group: List<CalendarEvent>, result: MutableMap<String, Pair<Int, Int>>) {
        val columns = mutableListOf<LocalTime?>() 
        val eventKeys = group.mapIndexed { idx, evt -> getEventUniqueKey(evt, idx) }
        val eventColumns = mutableMapOf<String, Int>()
        
        for (i in group.indices) {
            val event = group[i]
            val key = eventKeys[i]
            val startTime = parseTime(event.startTime)
            var assigned = false
            for (c in columns.indices) {
                if (columns[c] == null || !startTime.isBefore(columns[c])) {
                    columns[c] = if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime)
                    eventColumns[key] = c
                    assigned = true
                    break
                }
            }
            if (!assigned) {
                columns.add(if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime))
                eventColumns[key] = columns.size - 1
            }
        }
        
        val totalCols = columns.size
        for (i in group.indices) {
            val key = eventKeys[i]
            val colIndex = eventColumns[key] ?: 0
            result[key] = Pair(colIndex, totalCols)
        }
    }

    fun mergeEvents(firestoreVer: CalendarEvent?, googleVer: CalendarEvent?): CalendarEvent {
        return googleVer ?: firestoreVer ?: CalendarEvent()
    }

    fun mergeEventLists(firestoreEvents: List<CalendarEvent>, googleEvents: List<CalendarEvent>): List<CalendarEvent> {
        return googleEvents
    }

    fun isWorkShift(event: CalendarEvent, workShiftColorKey: String): Boolean {
        if (event.allDay) return false
        val target = workShiftColorKey.ifEmpty { "7" }
        return event.colorKey == target || (event.colorKey == null && (target == "7" || target == "1"))
    }

    fun findExtensionForEvent(
        event: CalendarEvent,
        extensions: Map<String, CalendarEventExtension>
    ): CalendarEventExtension? {
        if (extensions.isEmpty()) return null
        val baseId = event.id.substringBefore("_cont_")
        val cleanId = baseId.removePrefix("ext_")

        extensions[event.id]?.let { return it }
        if (baseId != event.id) extensions[baseId]?.let { return it }
        extensions[cleanId]?.let { return it }
        extensions["ext_$cleanId"]?.let { return it }

        val iCal = event.iCalUID
        if (!iCal.isNullOrEmpty()) {
            extensions[iCal]?.let { return it }
            extensions["ext_$iCal"]?.let { return it }
        }

        val noteText = event.note ?: ""
        val deepLinkId = Regex("[?&]id=([^&\\s]+)").find(noteText)?.groupValues?.getOrNull(1)
            ?: Regex("/event/([^\\s&?/#]+)").find(noteText)?.groupValues?.getOrNull(1)
        if (!deepLinkId.isNullOrEmpty()) {
            extensions[deepLinkId]?.let { return it }
            extensions[deepLinkId.removePrefix("ext_")]?.let { return it }
        }

        val dtStart = event.dtStart
        return extensions.values.find { ext ->
            val extClean = ext.googleEventId.removePrefix("ext_")
            val matchesId = ext.googleEventId == event.id ||
                ext.googleEventId == baseId ||
                extClean == cleanId ||
                (ext.localEventId.isNotEmpty() && (ext.localEventId == event.id || ext.localEventId == baseId || ext.localEventId == cleanId)) ||
                (!iCal.isNullOrEmpty() && (ext.googleEventId == iCal || extClean == iCal)) ||
                (!deepLinkId.isNullOrEmpty() && (ext.googleEventId == deepLinkId || extClean == deepLinkId.removePrefix("ext_") || ext.localEventId == deepLinkId.removePrefix("ext_")))
            if (matchesId) return@find true

            val titleMatch = !ext.title.isNullOrBlank() && ext.title.trim().equals(event.title.trim(), ignoreCase = true)
            if (!titleMatch || ext.dateTimestamp == 0L || dtStart == null) return@find false
            if (kotlin.math.abs(ext.dateTimestamp - dtStart) < 300_000L) return@find true

            if (event.allDay) {
                val zone = ZoneId.systemDefault()
                val extDate = java.time.Instant.ofEpochMilli(ext.dateTimestamp).atZone(zone).toLocalDate()
                val eventDate = java.time.Instant.ofEpochMilli(dtStart).atZone(zone).toLocalDate()
                extDate == eventDate
            } else {
                false
            }
        }
    }
}
