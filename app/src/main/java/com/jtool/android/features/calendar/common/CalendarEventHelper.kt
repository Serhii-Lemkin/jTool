package com.jtool.android.features.calendar.common

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import java.time.LocalDate
import java.time.LocalTime

object CalendarEventHelper {
    fun generateVirtualShifts(options: WorktimeOptions, start: LocalDate, end: LocalDate): List<CalendarEvent> {
        val virtualEvents = mutableListOf<CalendarEvent>()
        var current = start
        while (!current.isAfter(end)) {
            val dayOfWeek = current.dayOfWeek
            
            if (options.trackingPreference == WorkTrackingPreferenceEnum.SCHEDULE) {
                // Do not generate virtual shifts for SCHEDULE mode as they are saved as real events
            } else if (options.trackingPreference == WorkTrackingPreferenceEnum.LOG_HOURS) {
                val logGoal = options.logSchedule[dayOfWeek.name]
                if (logGoal != null && !logGoal.isConfirmedOff) {
                    val startTime = if (options.useStableApproximateHours) options.stableApproximateStart else logGoal.approximateStartTime
                    val endTime = if (options.useStableApproximateHours) options.stableApproximateEnd else logGoal.approximateEndTime
                    
                    if (!startTime.isNullOrEmpty()) {
                        virtualEvents.add(CalendarEvent(
                            title = "Approximate Shift",
                            type = CalendarEventTypeEnum.SHIFT,
                            date = current.toString(),
                            startTime = startTime,
                            endTime = endTime,
                            status = CalendarEventStatusEnum.REGULAR,
                            id = "virtual_${current}",
                            note = "(Virtual)"
                        ))
                    }
                }
            }
            current = current.plusDays(1)
        }
        return virtualEvents
    }

    fun isOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        return try {
            val start1 = parseTime(s1)
            var end1 = if (e1.isBlank()) start1.plusHours(1) else parseTime(e1)
            if (end1.isBefore(start1) || end1 == start1) end1 = LocalTime.MAX
            
            val start2 = parseTime(s2)
            var end2 = if (e2.isBlank()) start2.plusHours(1) else parseTime(e2)
            if (end2.isBefore(start2) || end2 == start2) end2 = LocalTime.MAX
            
            start1.isBefore(end2) && start2.isBefore(end1)
        } catch (_: Exception) { false }
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
                // Handle cases like "07" or "7"
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
                if (t.isBefore(start) || t == start) LocalTime.MAX else t
            }
            
            val durationMinutes = java.time.Duration.between(start, end).toMinutes().toInt()
            
            // Coerce to at least 15 minutes for UI interaction, but allow smaller boxes if requested.
            (durationMinutes * hourHeight / 60).coerceAtLeast(hourHeight / 4)
        } catch (_: Exception) { hourHeight }
    }

    /**
     * Groups events into columns to handle side-by-side display of overlaps.
     * Returns a map of event ID to (columnIndex, totalColumnsInGroup)
     */
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
        val columns = mutableListOf<LocalTime?>() // tracks end time of last event in each column
        val eventColumns = mutableMapOf<String, Int>()
        
        for (event in group) {
            val startTime = parseTime(event.startTime)
            var assigned = false
            for (i in columns.indices) {
                if (columns[i] == null || !startTime.isBefore(columns[i])) {
                    columns[i] = if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime)
                    eventColumns[event.id] = i
                    assigned = true
                    break
                }
            }
            if (!assigned) {
                columns.add(if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else parseTime(event.endTime))
                eventColumns[event.id] = columns.size - 1
            }
        }
        
        val totalCols = columns.size
        for (event in group) {
            result[event.id] = Pair(eventColumns[event.id]!!, totalCols)
        }
    }
}
