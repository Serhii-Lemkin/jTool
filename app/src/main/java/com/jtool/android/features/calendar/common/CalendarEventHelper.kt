package com.jtool.android.features.calendar.common

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.core.localization.Loc
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object CalendarEventHelper {
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
        return googleEvents // For pure Google integration, we only care about Google events
    }

    fun isWorkShift(event: CalendarEvent, workShiftColorKey: String): Boolean {
        if (event.allDay) return false
        val target = workShiftColorKey.ifEmpty { "7" }
        return event.colorKey == target || (event.colorKey == null && (target == "7" || target == "1"))
    }
}
