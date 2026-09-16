package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object AdminLoc {
    val cleanupLegacyWorkHours get() = Loc.s("admin_cleanupLegacyWorkHours", "Cleanup Legacy Work Hours")
    val cleanupLegacyWorkHoursConfirm get() = Loc.s("admin_cleanupLegacyWorkHoursConfirm", "This will delete the 'hours' field from all UserData documents in Firestore. Proceed?")
    val sendNotification get() = Loc.s("admin_sendNotification", "Send Notification")
    val toTarget get() = Loc.s("admin_toTarget", "To: %s")
    val notificationTitle get() = Loc.s("admin_notificationTitle", "Title")
    val notificationMessage get() = Loc.s("admin_notificationMessage", "Message")
    val adminPanel get() = Loc.s("admin_adminPanel", "Admin Panel")
    val users get() = Loc.s("admin_users", "Users")
    val usersSubtitle get() = Loc.s("admin_usersSubtitle", "Manage user permissions")
    val searchUsers get() = Loc.s("admin_searchUsers", "Search Users")
    val groupsSubtitle get() = Loc.s("admin_groupsSubtitle", "Maintenance and cleanup")
    val deleteUserPermanently get() = Loc.s("admin_deleteUserPermanently", "this will permanently delete all user data do you wish to proceed")
    val deleteGroupPermanently get() = Loc.s("admin_deleteGroupPermanently", "This will permanently delete the group and all its data. Do you wish to proceed?")
    val deleteGroup get() = Loc.s("admin_deleteGroup", "Delete Group")
    val makeAdmin get() = Loc.s("admin_makeAdmin", "Make Admin")
    val removeAdmin get() = Loc.s("admin_removeAdmin", "Remove Admin")
    val makeReviewer get() = Loc.s("admin_makeReviewer", "Make Reviewer")
    val removeReviewer get() = Loc.s("admin_removeReviewer", "Remove Reviewer")
    val migrateCalendarEvents get() = Loc.s("admin_migrateCalendarEvents", "Migrate Calendar Events")
    val migrateCalendarEventsSubtitle get() = Loc.s("admin_migrateCalendarEventsSubtitle", "Convert legacy date/time strings to timestamps")
    val simulateCrash get() = Loc.s("admin_simulateCrash", "Simulate Crash")
    val simulateCrashSubtitle get() = Loc.s("admin_simulateCrashSubtitle", "Triggers a fatal exception to test crash reporting")
    val sendNotifSubtitle get() = Loc.s("admin_sendNotifSubtitle", "Send messages to users or groups")
}
