package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object CalendarLoc {
    val calendar get() = Loc.s("calendar_calendar", "Calendar")
    val shift get() = Loc.s("calendar_shift", "Shift")
    val addEvent get() = Loc.s("calendar_addEvent", "Add Event")
    val editEvent get() = Loc.s("calendar_editEvent", "Edit Event")
    val noEvents get() = Loc.s("calendar_noEvents", "No events scheduled")
    val event get() = Loc.s("calendar_event", "Event")
    val date get() = Loc.s("calendar_date", "Date")
    val time get() = Loc.s("calendar_time", "Time")
    val type get() = Loc.s("calendar_type", "Type")
    val noEventFound get() = Loc.s("calendar_noEventFound", "No event found")
    
    // Legacy calendar keys
    val eventTitle get() = Loc.s("calendar_eventTitle", "Event Title")
    val common get() = Loc.s("calendar_common", "Common")
    val note get() = Loc.s("calendar_note", "Note")
}
