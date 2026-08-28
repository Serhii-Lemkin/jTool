package com.jtool.android.core.localization

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LanguageMetadata(
    val name: String,
    val shortCode: String,
    val locale: String,
    val direction: String = "ltr"
)

@Serializable
data class LocaleFileData(
    val name: String,
    val shortCode: String,
    val locale: String,
    val direction: String = "ltr",
    val strings: Map<String, String>
)

object Loc {
    private val json = Json { ignoreUnknownKeys = true }
    private var loadedStrings by mutableStateOf<Map<String, String>>(emptyMap())
    private var metadata by mutableStateOf<LanguageMetadata?>(null)
    
    var lang by mutableStateOf("en")

    val layoutDirection: LayoutDirection 
        get() = if (metadata?.direction == "rtl") LayoutDirection.Rtl else LayoutDirection.Ltr

    fun load(context: Context, locale: String) {
        try {
            val jsonString = context.assets.open("locales/$locale.json").bufferedReader().use { it.readText() }
            val data = json.decodeFromString<LocaleFileData>(jsonString)
            loadedStrings = data.strings
            metadata = LanguageMetadata(data.name, data.shortCode, data.locale, data.direction)
            lang = locale
        } catch (e: Exception) {
            android.util.Log.e("Loc", "Failed to load locale: $locale", e)
        }
    }

    fun getAvailableLanguages(context: Context): List<LanguageMetadata> {
        val files = context.assets.list("locales") ?: return emptyList()
        return files.mapNotNull { fileName ->
            try {
                val jsonString = context.assets.open("locales/$fileName").bufferedReader().use { it.readText() }
                val data = json.decodeFromString<LocaleFileData>(jsonString)
                LanguageMetadata(data.name, data.shortCode, data.locale, data.direction)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun s(key: String, fallback: String): String {
        return loadedStrings[key] ?: fallback
    }

    // Launcher
    val launcher_hello get() = s("launcher_hello", "Hello")
    val launcher_settings get() = s("launcher_settings", "Settings")

    // Worktime
    val worktime_today get() = s("worktime_today", "Today")
    val worktime_requiredHours get() = s("worktime_requiredHours", "Required Hours")
    val worktime_startTime get() = s("worktime_startTime", "Start Time")
    val worktime_calculate get() = s("worktime_calculate", "Calculate")
    val worktime_remember get() = s("worktime_remember", "Remember")
    val worktime_forgetTitle get() = s("worktime_forgetTitle", "Forget Time?")
    val worktime_forgetConfirm get() = s("worktime_forgetConfirm", "Do you want to delete the saved start time for today?")
    val worktime_finishAt get() = s("worktime_finishAt", "Finish at")
    val worktime_forget get() = s("worktime_forget", "Forget")
    val worktime_workFinished get() = s("worktime_workFinished", "Work day finished!")
    val worktime_timeLeft get() = s("worktime_timeLeft", "Time left")
    val worktime_invalidFormat get() = s("worktime_invalidFormat", "Invalid format. Try 6.13, 613, or 06:13")
    val worktime_noWorkScheduled get() = s("worktime_noWorkScheduled", "No work hours scheduled for today!")
    val worktime_editHours get() = s("worktime_editHours", "Edit Hours")
    val worktime_resetToNow get() = s("worktime_resetToNow", "Reset to Now")
    val worktime_editWorkHours get() = s("worktime_editWorkHours", "Edit Work Hours")
    val worktime_setHoursManually get() = s("worktime_setHoursManually", "Set your work hours manually to start.")
    val worktime_workTime get() = s("worktime_workTime", "Work")

    // Time Calc
    val timecalc_baseDateTime get() = s("timecalc_baseDateTime", "Base Date/Time")
    val timecalc_addSub get() = s("timecalc_addSub", "Add / Subtract")
    val timecalc_sub get() = s("timecalc_sub", "Sub")
    val timecalc_history get() = s("timecalc_history", "History (Tap to restore)")
    val timecalc_added get() = s("timecalc_added", "Added")
    val timecalc_subtracted get() = s("timecalc_subtracted", "Subtracted")
    val timecalc_resultedIn get() = s("timecalc_resultedIn", "Resulted in")
    val timecalc_year get() = s("timecalc_year", "Year")
    val timecalc_month get() = s("timecalc_month", "Mo")
    val timecalc_day get() = s("timecalc_day", "Day")

    fun getDayOfWeekName(day: java.time.DayOfWeek): String {
        if (lang == "uk") {
            return when(day) {
                java.time.DayOfWeek.SUNDAY -> s("timecalc_day_Sunday", "Неділя")
                java.time.DayOfWeek.MONDAY -> s("timecalc_day_Monday", "Понеділок")
                java.time.DayOfWeek.TUESDAY -> s("timecalc_day_Tuesday", "Вівторок")
                java.time.DayOfWeek.WEDNESDAY -> s("timecalc_day_Wednesday", "Середа")
                java.time.DayOfWeek.THURSDAY -> s("timecalc_day_Thursday", "Четвер")
                java.time.DayOfWeek.FRIDAY -> s("timecalc_day_Friday", "П'ятниця")
                java.time.DayOfWeek.SATURDAY -> s("timecalc_day_Saturday", "Субота")
            }
        }
        return day.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag(lang))
    }

    val timecalc_hour get() = s("timecalc_hour", "Hr")
    val timecalc_minute get() = s("timecalc_minute", "Min")
    val timecalc_timeCalc get() = s("timecalc_timeCalc", "Time")

    // ShopList
    val shoplist_shoppingList get() = s("shoplist_shoppingList", "ShopList")
    val shoplist_addItem get() = s("shoplist_addItem", "Add Item")
    val shoplist_recentlyChecked get() = s("shoplist_recentlyChecked", "Recently Checked")
    val shoplist_restore get() = s("shoplist_restore", "Restore")
    val shoplist_itemAlreadyInList get() = s("shoplist_itemAlreadyInList", "Item already in list")
    val shoplist_restoredExisting get() = s("shoplist_restoredExisting", "Restored existing item")
    val shoplist_all get() = s("shoplist_all", "All")
    val shoplist_unactive get() = s("shoplist_unactive", "Unactive")
    val shoplist_selectAisle get() = s("shoplist_selectAisle", "Select Aisle")
    val shoplist_changeAisle get() = s("shoplist_changeAisle", "Change Aisle")
    
    fun getAisleName(aisleKey: String): String {
        return when(aisleKey) {
            "Common" -> s("shoplist_aisle_common", "Common")
            "Veggies" -> s("shoplist_aisle_veggies", "Veggies")
            "Meat" -> s("shoplist_aisle_meat", "Meat")
            "Fish" -> s("shoplist_aisle_fish", "Fish")
            "Dairy" -> s("shoplist_aisle_dairy", "Dairy")
            "Bakery" -> s("shoplist_aisle_bakery", "Bakery")
            "Drinks" -> s("shoplist_aisle_drinks", "Drinks")
            "Frozen" -> s("shoplist_aisle_frozen", "Frozen")
            "Snacks" -> s("shoplist_aisle_snacks", "Snacks")
            "Household" -> s("shoplist_aisle_household", "Household")
            "Personal" -> s("shoplist_aisle_personal", "Personal")
            "Pets" -> s("shoplist_aisle_pets", "Pets")
            else -> aisleKey
        }
    }

    // Notes
    val notes_notes get() = s("notes_notes", "Notes")
    val notes_textNote get() = s("notes_textNote", "Text Note")
    val notes_checklistNote get() = s("notes_checklistNote", "Checklist")
    val notes_noteContentHint get() = s("notes_noteContentHint", "Note content...")
    val notes_noteSaved get() = s("notes_noteSaved", "Note saved!")
    val notes_deleteNoteTitle get() = s("notes_deleteNoteTitle", "Delete Note?")
    val notes_noteTitlePlaceholder get() = s("notes_noteTitlePlaceholder", "Title")
    val notes_untitledNote get() = s("notes_untitledNote", "Untitled")

    // Comments
    val comments_comments get() = s("comments_comments", "Comments")
    val comments_addComment get() = s("comments_addComment", "Add a comment...")
    val comments_reply get() = s("comments_reply", "Reply")
    val comments_edit get() = s("comments_edit", "Edit")
    val comments_edited get() = s("comments_edited", "edited")
    val comments_replying get() = s("comments_replying", "Replying...")

    // Links
    val links_links get() = s("links_links", "Links")
    val links_searchLinks get() = s("links_searchLinks", "Search links...")
    val links_linkTitle get() = s("links_linkTitle", "Title")
    val links_linkUrl get() = s("links_linkUrl", "URL")
    val links_addLink get() = s("links_addLink", "Add Link")
    val links_invalidUrl get() = s("links_invalidUrl", "Invalid URL")
    val links_copy get() = s("links_copy", "Copy")
    val links_copied get() = s("links_copied", "Copied!")
    val links_urlHint get() = s("links_urlHint", "https://...")
    val links_confirmDeleteLinkTitle get() = s("links_confirmDeleteLinkTitle", "Delete Link?")
    val links_confirmDeleteLinkMsg get() = s("links_confirmDeleteLinkMsg", "Are you sure you want to delete this link?")

    // Cleaning
    val cleaning_cleaning get() = s("cleaning_cleaning", "Cleaning")
    val cleaning_taskTitle get() = s("cleaning_taskTitle", "Task Title")
    val cleaning_everyXDays get() = s("cleaning_everyXDays", "Every X Days")
    val cleaning_recurring get() = s("cleaning_recurring", "Recurring")
    val cleaning_addTask get() = s("cleaning_addTask", "Add Task")
    val cleaning_lastDone get() = s("cleaning_lastDone", "Last done")
    val cleaning_oneTime get() = s("cleaning_oneTime", "One-time")
    val cleaning_disable get() = s("cleaning_disable", "Disable")
    val cleaning_restoreTask get() = s("cleaning_restoreTask", "Restore")
    val cleaning_showDisabled get() = s("cleaning_showDisabled", "Show Disabled Tasks")
    val cleaning_activeTasks get() = s("cleaning_activeTasks", "Active Tasks")
    val cleaning_editTask get() = s("cleaning_editTask", "Edit Task")

    val cleaning_recurrence_everyDay get() = s("cleaning_recurrence_everyDay", "Every day")
    val cleaning_recurrence_everyTwoDays get() = s("cleaning_recurrence_everyTwoDays", "Every 2 days")
    val cleaning_recurrence_everyXDays get() = s("cleaning_recurrence_everyXDays", "Every %d days")
    val cleaning_recurrence_uk_1 get() = s("cleaning_recurrence_uk_1", "кожен %d день")
    val cleaning_recurrence_uk_2_4 get() = s("cleaning_recurrence_uk_2_4", "кожні %d дні")
    val cleaning_recurrence_uk_5 get() = s("cleaning_recurrence_uk_5", "кожні %d днів")

    fun getCleaningFrequency(days: Int): String {
        return RecurrenceFormatter.formatEveryXDays(days, lang)
    }

    // Settings
    val settings_displayName get() = s("settings_displayName", "Display Name")
    val settings_userSettings get() = s("settings_userSettings", "User Settings")
    val settings_syncSubtitle get() = s("settings_syncSubtitle", "Cloud sync for your data")
    val settings_appUpdates get() = s("settings_appUpdates", "App Updates")
    val settings_appUpdatesSubtitle get() = s("settings_appUpdatesSubtitle", "Check for new versions on Google Play")
    val settings_visibilityTitle get() = s("settings_visibilityTitle", "App Visibility")
    val settings_visibilitySubtitle get() = s("settings_visibilitySubtitle", "Hide or show apps on main menu")
    val settings_layoutType get() = s("settings_layoutType", "Menu Layout")
    val settings_gridLayout get() = s("settings_gridLayout", "Grid")
    val settings_listLayout get() = s("settings_listLayout", "List")
    val settings_cloudFolder get() = s("settings_cloudFolder", "Cloud Folder")
    val settings_selectFolderDesc get() = s("settings_selectFolderDesc", "Select the shared Google Drive folder for sync.")
    val settings_linkSyncFolder get() = s("settings_linkSyncFolder", "Link Sync Folder")
    val settings_pushData get() = s("settings_pushData", "Push Data")
    val settings_pullData get() = s("settings_pullData", "Pull Data")
    val settings_openInDrive get() = s("settings_openInDrive", "Open in Drive")
    val settings_syncInterval get() = s("settings_syncInterval", "Sync Interval (seconds)")
    val settings_unlinkFolder get() = s("settings_unlinkFolder", "Unlink Folder")
    val settings_manualBackup get() = s("settings_manualBackup", "Manual Backup File")
    val settings_export get() = s("settings_export", "Export")
    val settings_import get() = s("settings_import", "Import")
    val settings_versionInfo get() = s("settings_versionInfo", "Version Info")
    val settings_checkForUpdates get() = s("settings_checkForUpdates", "Check for Updates")
    val settings_updateNow get() = s("settings_updateNow", "Update Now")
    val settings_updateAvailableTitle get() = s("settings_updateAvailableTitle", "Update Available")
    val settings_later get() = s("settings_later", "Later")
    val settings_install get() = s("settings_install", "Install")
    val settings_goToUpdates get() = s("settings_goToUpdates", "Go to Updates")
    val settings_upToDateMsg get() = s("settings_upToDateMsg", "You are on version %s and you are up to date.")
    val settings_updateAvailableMsg get() = s("settings_updateAvailableMsg", "You are on version %s and there is version %s available on Google Play.")
    val settings_linkFolderToUpdate get() = s("settings_linkFolderToUpdate", "You must link a shared folder in 'Sync' first to check for updates.")
    val settings_openSecuritySettings get() = s("settings_openSecuritySettings", "Open Security Settings")
    val settings_checkingUpdates get() = s("settings_checkingUpdates", "Checking for updates...")
    val settings_currentVersionLabel get() = s("currentVersionLabel", "Current version: %s")
    val settings_checking get() = s("settings_checking", "Checking...")
    val settings_latestVersionMsg get() = s("latestVersionMsg", "You are on the latest version!")
    val settings_playStoreUpdates get() = s("playStoreUpdates", "Play Store Updates")
    val settings_playStoreUpdatesDesc get() = s("playStoreUpdatesDesc", "Updates are managed automatically by Google Play. If you encounter issues, you can check the store listing manually.")
    val settings_viewInPlayStore get() = s("viewInPlayStore", "View in Play Store")
    val settings_nameSaved get() = s("settings_nameSaved", "Name saved!")
    val settings_dangerZone get() = s("settings_dangerZone", "Danger Zone")
    val settings_deleteAccountData get() = s("settings_deleteAccountData", "Delete Account & Data")
    val settings_confirmEmailTitle get() = s("settings_confirmEmailTitle", "Confirm Email")
    val settings_confirmEmailMsg get() = s("settings_confirmEmailMsg", "To proceed, please enter your email address: %s")
    val settings_email get() = s("settings_email", "Email")
    val settings_verify get() = s("settings_verify", "Verify")
    val settings_deleteAccountPermanentMsg get() = s("settings_deleteAccountPermanentMsg", "THIS ACTION IS PERMANENT. All your personal settings, group memberships, and public profile will be deleted forever. Do you wish to proceed?")
    val settings_logout get() = s("settings_logout", "Logout")
    val settings_confirmLogoutTitle get() = s("settings_confirmLogoutTitle", "Logout?")
    val settings_confirmLogoutMsg get() = s("settings_confirmLogoutMsg", "Are you sure you want to log out?")
    val settings_signInWithGoogle get() = s("settings_signInWithGoogle", "Sign in with Google")
    val settings_notSignedIn get() = s("settings_notSignedIn", "You are not signed in.")
    val settings_pleaseSignInLink get() = s("settings_pleaseSignInLink", "Please sign in here")
    val settings_cloudSyncFirebase get() = s("settings_cloudSyncFirebase", "Google Account (Firebase)")
    val settings_appearanceLanguage get() = s("settings_appearanceLanguage", "Appearance & Language")
    val settings_darkMode get() = s("settings_darkMode", "Dark Mode")
    val settings_lightMode get() = s("settings_lightMode", "Light Mode")
    val settings_language get() = s("settings_language", "Language")
    val settings_generalSettings get() = s("settings_generalSettings", "General Settings")
    val settings_account get() = s("settings_account", "Account")
    val settings_privacyPolicy get() = s("settings_privacyPolicy", "Privacy Policy")
    val settings_privacySubtitle get() = s("settings_privacySubtitle", "Read how we handle your data")
    val settings_linkedGroupId get() = s("settings_linkedGroupId", "Linked Group")
    val settings_shareIdDesc get() = s("settings_shareIdDesc", "Copy ID and share it with others to get into the group")
    val settings_unlinkGroup get() = s("settings_unlinkGroup", "Leave")
    val settings_activeGroup get() = s("settings_activeGroup", "Active Group:")
    val settings_noneSelected get() = s("settings_noneSelected", "None selected")
    val settings_addNewGroup get() = s("settings_addNewGroup", "Add New Group")
    val settings_groupName get() = s("settings_groupName", "Group Name")
    val settings_createNewGroup get() = s("settings_createNewGroup", "Create New Group")
    val settings_groupIdToJoin get() = s("settings_groupIdToJoin", "Group ID to Join")
    val settings_joinGroup get() = s("settings_joinGroup", "Join Group")
    val settings_noGroupsMsg get() = s("settings_noGroupsMsg", "You are not a member in any group, please create one yourself or ask a friend to invite you in.")
    val settings_activeGroupLabel get() = s("settings_activeGroupLabel", "Group: %s")
    val settings_noActiveGroup get() = s("settings_noActiveGroup", "No group selected. Click here to join one!")
    val settings_manualSync get() = s("settings_manualSync", "Manual Sync (Merge)")
    val settings_groups get() = s("settings_groups", "Groups")
    val settings_groupInvite get() = s("settings_groupInvite", "Invite to Group")
    val settings_enterEmail get() = s("settings_enterEmail", "Enter user email...")
    val settings_confirmUnlinkTitle get() = s("settings_confirmUnlinkTitle", "Leave Group?")
    val settings_confirmUnlinkMsg get() = s("settings_confirmUnlinkMsg", "Are you sure you want to leave this group?")
    val settings_copyId get() = s("settings_copyId", "Copy ID")
    val settings_ownerLabel get() = s("settings_ownerLabel", "Owner")
    val settings_groupMembers get() = s("settings_groupMembers", "Group Members")
    val settings_deleteGroupTitle get() = s("settings_deleteGroupTitle", "Delete Group?")
    val settings_lastMemberLeaveMsg get() = s("settings_lastMemberLeaveMsg", "Because you are the last person to leave the group, on leave the group will be permanently deleted. Are you sure?")
    val settings_groupCreated get() = s("settings_groupCreated", "Group Created!")
    val settings_groupIdLabel get() = s("settings_groupIdLabel", "Group ID: %s")
    val settings_editGroupName get() = s("settings_editGroupName", "Edit Group Name")

    // Support / CRM
    val support_hub_title get() = s("support_hub_title", "Support Hub")
    val support_type_bug get() = s("support_type_bug", "Bug")
    val support_type_crash get() = s("support_type_crash", "Crash")
    val support_type_feature get() = s("support_type_feature", "Feature")
    val support_type_change get() = s("support_type_change", "Change")
    val support_status_opened get() = s("support_status_opened", "Opened")
    val support_status_in_progress get() = s("support_status_in_progress", "In Progress")
    val support_status_closed get() = s("support_status_closed", "Closed")
    val support_status_cut get() = s("support_status_cut", "Cut")
    val support_new_request get() = s("support_new_request", "New Request")
    val support_show_archived get() = s("support_show_archived", "Show Archived")
    val support_submit get() = s("support_submit", "Submit")
    val support_welcome_title get() = s("support_welcome_title", "Welcome to the Support Hub!")
    val support_welcome_msg get() = s("support_welcome_msg", "Here you can open a ticket for the developer to look into any issues you are experiencing. To start, please press the plus icon in the bottom right corner.")
    val support_no_requests get() = s("support_no_requests", "No requests found.")
    val support_request_number get() = s("support_request_number", "#%d")
    val support_device_info get() = s("support_device_info", "Device Info")
    val support_crash_log get() = s("support_crash_log", "Crash Log")
    val support_daily_limit get() = s("support_daily_limit", "Daily limit reached (%d/%d).")
    val support_set_status get() = s("support_set_status", "Set Status")
    val support_enter_details get() = s("support_enter_details", "Enter details here...")
    val support_sent_success get() = s("support_sent_success", "Support request sent!")

    // Admin
    val admin_adminPanel get() = s("admin_adminPanel", "Admin Panel")
    val admin_users get() = s("admin_users", "Users")
    val admin_usersSubtitle get() = s("admin_usersSubtitle", "Manage user permissions")
    val admin_searchUsers get() = s("admin_searchUsers", "Search Users")
    val admin_groupsSubtitle get() = s("admin_groupsSubtitle", "Maintenance and cleanup")
    val admin_deleteUserPermanently get() = s("admin_deleteUserPermanently", "this will permanently delete all user data do you wish to proceed")
    val admin_deleteGroupPermanently get() = s("admin_deleteGroupPermanently", "This will permanently delete the group and all its data. Do you wish to proceed?")
    val admin_deleteGroup get() = s("admin_deleteGroup", "Delete Group")
    val admin_makeAdmin get() = s("admin_makeAdmin", "Make Admin")
    val admin_removeAdmin get() = s("admin_removeAdmin", "Remove Admin")

    // General
    val general_hourSymbol get() = s("general_hourSymbol", "h")
    val general_minuteSymbol get() = s("general_minuteSymbol", "m")
    val general_yearShort get() = s("general_yearShort", "y")
    val general_monthShort get() = s("general_monthShort", "mo")
    val general_dayShort get() = s("general_dayShort", "d")
    val general_zeroUnits get() = s("general_zeroUnits", "0 units")
    val general_add get() = s("general_add", "Add")
    val general_update get() = s("general_update", "Update")
    val general_delete get() = s("general_delete", "Delete")
    val general_cancel get() = s("general_cancel", "Cancel")
    val general_save get() = s("general_save", "Save")
    val general_close get() = s("general_close", "Close")
    val general_back get() = s("general_back", "< Back")
    val general_by get() = s("general_by", "By")
    val general_or get() = s("general_or", "OR")
    val general_confirmDeleteTitle get() = s("general_confirmDeleteTitle", "Delete Item?")
    val general_confirmDeleteMsg get() = s("general_confirmDeleteMsg", "Are you sure you want to delete this item?")
    val general_unknown get() = s("general_unknown", "Unknown")
    val general_search get() = s("general_search", "Search")
    val general_searchGroups get() = s("general_searchGroups", "Search Groups")
    val general_areYouSure get() = s("general_areYouSure", "Are you sure?")
    val general_refreshing get() = s("general_refreshing", "Refreshing...")
    val general_pullToRefresh get() = s("general_pullToRefresh", "Pull to refresh")
    val general_item get() = s("general_item", "item")
    val general_items get() = s("general_items", "items")
}
