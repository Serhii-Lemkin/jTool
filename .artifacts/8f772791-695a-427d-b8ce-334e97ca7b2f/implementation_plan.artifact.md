# Implementation Plan: Calendar Event Types, Validation, Location, Meeting Links & Attachments

This plan details the technical architecture and task breakdown for expanding Calendar event creation with type selection sheets, start/end date calculations and validations, collapsed additional settings (location, conference links, attachments), and Google Maps integration.

---

## Proposed Technical Changes

### 1. Plus Button Event Type Selection Sheet
- **Component**: Create `EventTypeSelectionSheet.kt` in `features/calendar/components/event_type_selection_sheet/`.
- **Behavior**: Tapping the Plus FAB on the Calendar screen opens a bottom sheet with 3 event creation presets:
  1. **Standard Event**: General personal calendar event.
  2. **Work Shift**: Pre-fills shift title ("Work Shift"), sets `colorKey` to the work shift color key from settings, and pre-fills shift start/finish times from worktime options.
  3. **Shared Group Event**: Pre-opens group selection and marks shared group context.
- Clicking an event type navigates to `CalendarAddEditScreen` pre-configured with that preset state.

---

### 2. Separate End Date Picker, Auto-Calculation & Strict Logic Validation
- **End Date Picker**: Add a dedicated `endDate` state variable in `CalendarAddEditScreen.kt` alongside `startDate`.
- **Auto-Overnight Calculation**:
  - When `startTime` > `endTime` (e.g. Start 22:00, Finish 03:00) and `endDate == startDate`, automatically advance `endDate = startDate.plusDays(1)`.
  - Allow the user to manually change both Start Date and End Date at will.
- **Validation Rule Engine**:
  - Require non-empty event title.
  - Require `endTimestamp` > `startTimestamp` for timed events (or `endDate` >= `startDate` for all-day events).
  - If validation fails, display inline error banner / validation sheet preventing invalid event creation.

---

### 3. Collapsible Additional Options Section (Location, Conference Link, Attachments)
- **Collapsible UI Container**: Add a collapsible `Card` titled **"Additional Options"** in `CalendarAddEditScreen.kt` (collapsed by default).
- **Location Field & Google Maps Integration**:
  - Save `location` into `CalendarContract.Events.EVENT_LOCATION`.
  - Include a "View on Maps" button next to the input that opens Google Maps (`geo:0,0?q=location` / `https://www.google.com/maps/search/?api=1&query=location`).
  - On `CalendarEventDetailScreen`, display the location with a clickable Google Maps icon.
- **Conference Data / Meeting Link**:
  - Save video meeting URL into `CalendarContract.Events.CONFERENCE_DATA` / note link.
  - Display "Join Video Meeting" button on `CalendarEventDetailScreen` (`Intent.ACTION_VIEW`).
- **File Attachment Link (Point 7)**:
  - Add optional attachment / Google Drive document link field in the Additional Options card.
  - Render attachment link on event detail screen.

---

### 4. Single Instance vs Series Modifications (Point 5)
- **Recurring Instance Action Sheet**:
  - When deleting or editing a recurring event (`rrule` is present), prompt the user:
    - **This event only**: Deletes/modifies single instance via `CalendarContract.Events.EXDATE` or `ORIGINAL_SYNC_ID`.
    - **All events in series**: Deletes/modifies the master recurring event.

---

## Proposed File Changes

### [MODIFY] [CalendarEvent.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/core/models/calendar/CalendarEvent.kt)
- Add `location: String? = null`, `conferenceUrl: String? = null`, `attachmentUrl: String? = null`, `endDate: String? = null`.

### [MODIFY] [GoogleCalendarSyncHelper.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/calendar/common/GoogleCalendarSyncHelper.kt)
- Read and write `CalendarContract.Events.EVENT_LOCATION` and conference data in `parseEvent` and `pushToGoogleCalendar`.

### [NEW] [EventTypeSelectionSheet.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/calendar/components/event_type_selection_sheet/EventTypeSelectionSheet.kt)
- Bottom sheet to select Standard, Work Shift, or Shared Group event creation preset.

### [MODIFY] [CalendarAddEditScreen.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/calendar/edit/CalendarAddEditScreen.kt)
- Integrate Start/End Date Pickers, auto-overnight calculations, logical validation rules, and the collapsible Additional Options card (Location + Maps button, Meeting Link, Attachment Link).

### [MODIFY] [CalendarEventDetailScreen.kt](file:///var/home/serhii/AndroidStudioProjects/jTool/app/src/main/java/com/jtool/android/features/calendar/detail/CalendarEventDetailScreen.kt)
- Render Location (with Google Maps launcher button), Video Meeting Link, and Attachment Link on event details card.

---

## Verification Plan

### Automated Tests
- Run `gradle_build("app:assembleDebug")` to verify zero compilation errors across model changes, sync helper, and UI screens.

### Manual Verification
- Test opening event type selector from Plus FAB and selecting Work Shift vs Standard event presets.
- Test setting start time 22:00 and finish time 02:00, verifying End Date automatically advances to the next day.
- Test setting an invalid end date before start date, verifying validation blocks saving.
- Test expanding Additional Options, entering a location, and tapping "View on Maps" to open Google Maps.
- Test entering a video conference URL and tapping "Join Video Meeting" on the event detail screen.
