package com.jtool.android.core.settings

import android.content.Context
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.calendar.CalendarConfig
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import com.jtool.android.core.models.user.PublicProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.Duration

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getWorktimeOptions(): WorktimeOptions {
        val jsonString = prefs.getString("worktime_options", null) ?: return WorktimeOptions()
        return try {
            json.decodeFromString<WorktimeOptions>(jsonString)
        } catch (_: Exception) {
            WorktimeOptions()
        }
    }

    fun saveWorktimeOptions(options: WorktimeOptions) {
        prefs.edit().putString("worktime_options", json.encodeToString(options)).apply()
    }

    fun getAllWeeklyHours(): Map<DayOfWeek, Double> {
        val daysInOrder = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
        return daysInOrder.associateWith { day ->
            val default = when (day) {
                DayOfWeek.THURSDAY -> 8.0
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> 0.0
                else -> 8.5
            }
            prefs.getFloat("hours_${day.name}", default.toFloat()).toDouble()
        }
    }

    fun isSyncPending(): Boolean {
        return prefs.getBoolean("sync_pending", false)
    }

    fun setSyncPending(pending: Boolean) {
        prefs.edit().putBoolean("sync_pending", pending).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", true)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun hasDarkModeBeenSet(): Boolean {
        return prefs.contains("dark_mode")
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun getUserId(): String {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (authUid != null) return authUid
        
        var id = prefs.getString("app_user_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString().take(8)
            saveAppUserId(id)
        }
        return id
    }

    fun getAppUserId(): String = getUserId()

    fun saveAppUserId(id: String) {
        prefs.edit().putString("app_user_id", id).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("language", "en") ?: "en"
    }

    fun hasLanguageBeenSet(): Boolean {
        return prefs.contains("language")
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
    }

    fun isAppVisible(appId: AppScreenEnum): Boolean {
        return prefs.getBoolean("visible_${appId.id}", true)
    }

    fun setAppVisible(appId: AppScreenEnum, visible: Boolean) {
        prefs.edit().putBoolean("visible_${appId.id}", visible).apply()
    }

    fun isGridLayout(): Boolean {
        return prefs.getBoolean("is_grid_layout", true)
    }

    fun setGridLayout(isGrid: Boolean) {
        prefs.edit().putBoolean("is_grid_layout", isGrid).apply()
    }

    fun getCurrentGroupId(): String? {
        return prefs.getString("current_group_id", null)
    }

    fun setCurrentGroupId(groupId: String?) {
        prefs.edit().putString("current_group_id", groupId).apply()
    }

    fun getGroups(): Map<String, GroupInfo> {
        val jsonStr = prefs.getString("remembered_groups", "{}") ?: "{}"
        return try {
            val obj = org.json.JSONObject(jsonStr)
            val map = mutableMapOf<String, GroupInfo>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val groupJson = obj.getJSONObject(id)
                map[id] = GroupInfo(
                    id = id,
                    name = groupJson.getString("name"),
                    ownerUid = if (groupJson.has("ownerUid")) groupJson.getString("ownerUid") else null,
                    isPrivate = if (groupJson.has("isPrivate")) groupJson.getBoolean("isPrivate") else false
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveGroups(groups: Map<String, GroupInfo>) {
        val obj = org.json.JSONObject()
        groups.forEach { (id, info) -> 
            val groupJson = org.json.JSONObject()
            groupJson.put("name", info.name)
            groupJson.put("ownerUid", info.ownerUid)
            groupJson.put("isPrivate", info.isPrivate)
            obj.put(id, groupJson)
        }
        prefs.edit().putString("remembered_groups", obj.toString()).apply()
    }

    fun addGroup(info: GroupInfo) {
        val groups = getGroups().toMutableMap()
        groups[info.id] = info
        saveGroups(groups)
    }

    fun removeGroup(id: String) {
        val groups = getGroups().toMutableMap()
        groups.remove(id)
        saveGroups(groups)
    }

    fun clearAllGroups() {
        prefs.edit().remove("remembered_groups").apply()
    }

    fun getGroupIds(): List<String> = getGroups().keys.toList()

    fun getUpdateUrl(): String? {
        return prefs.getString("update_url", null)
    }

    fun setUpdateUrl(url: String?) {
        prefs.edit().putString("update_url", url).apply()
    }

    fun getSelectedCalendarDate(): String {
        return prefs.getString("selected_calendar_date", java.time.LocalDate.now().toString()) ?: java.time.LocalDate.now().toString()
    }

    fun setSelectedCalendarDate(date: String) {
        prefs.edit().putString("selected_calendar_date", date).commit()
    }

    fun getSelectedCalendarEventId(): String? {
        return prefs.getString("selected_calendar_event_id", null)
    }

    fun setSelectedCalendarEventId(id: String?) {
        prefs.edit().putString("selected_calendar_event_id", id).commit()
    }

    fun shouldAutoOpenCalendarAdd(): Boolean {
        return prefs.getBoolean("auto_open_calendar_add", false)
    }

    fun setAutoOpenCalendarAdd(open: Boolean) {
        prefs.edit().putBoolean("auto_open_calendar_add", open).commit()
    }

    fun getSupportTypeFilters(): Set<String> {
        return prefs.getStringSet("support_type_filters", emptySet()) ?: emptySet()
    }

    fun saveSupportTypeFilters(filters: Set<String>) {
        prefs.edit().putStringSet("support_type_filters", filters).apply()
    }

    fun getSupportStatusFilters(): Set<String> {
        return prefs.getStringSet("support_status_filters", setOf("OPENED", "IN_PROGRESS")) ?: setOf("OPENED", "IN_PROGRESS")
    }

    fun saveSupportStatusFilters(filters: Set<String>) {
        prefs.edit().putStringSet("support_status_filters", filters).apply()
    }

    fun getVisibleGroupIds(): Set<String> {
        return prefs.getStringSet("visible_calendar_groups", emptySet()) ?: emptySet()
    }

    fun saveVisibleGroupIds(groupIds: Set<String>) {
        prefs.edit().putStringSet("visible_calendar_groups", groupIds).apply()
    }

    fun getHiddenGroupIds(): Set<String> {
        return prefs.getStringSet("hidden_calendar_groups", emptySet()) ?: emptySet()
    }

    fun saveHiddenGroupIds(groupIds: Set<String>) {
        prefs.edit().putStringSet("hidden_calendar_groups", groupIds).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_sync_timestamp", 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    // Calendar Config
    fun getCalendarConfig(): CalendarConfig {
        val jsonString = prefs.getString("calendar_config", null) ?: return CalendarConfig()
        return try {
            json.decodeFromString<CalendarConfig>(jsonString)
        } catch (_: Exception) {
            CalendarConfig()
        }
    }

    fun saveCalendarConfig(config: CalendarConfig) {
        prefs.edit().putString("calendar_config", json.encodeToString(config)).apply()
    }

    // Calendar Sync Settings (Legacy Individual Accessors - Now pointing to CalendarConfig)
    fun getGoogleCalendarId(): String = getCalendarConfig().googleCalendarId
    fun setGoogleCalendarId(id: String) = saveCalendarConfig(getCalendarConfig().copy(googleCalendarId = id))

    fun getGoogleCalendarEmail(): String = getCalendarConfig().googleCalendarEmail
    fun setGoogleCalendarEmail(email: String) = saveCalendarConfig(getCalendarConfig().copy(googleCalendarEmail = email))

    fun getGoogleCalendarSharedId(): String = getCalendarConfig().googleCalendarSharedId
    fun setGoogleCalendarSharedId(id: String) = saveCalendarConfig(getCalendarConfig().copy(googleCalendarSharedId = id))

    fun getWorkShiftColorKey(): String = getCalendarConfig().workShiftColorKey.ifEmpty { "7" }
    fun setWorkShiftColorKey(colorKey: String) {
        val config = getCalendarConfig().copy(workShiftColorKey = colorKey)
        saveCalendarConfig(config)
        val options = getWorktimeOptions().copy(workShiftColorKey = colorKey)
        saveWorktimeOptions(options)
    }

    fun getDisplayedCalendarIds(): Set<String> {
        val config = getCalendarConfig()
        return if (config.displayedCalendarIds.isNotEmpty()) {
            config.displayedCalendarIds.toSet()
        } else if (config.googleCalendarId.isNotEmpty()) {
            setOf(config.googleCalendarId)
        } else {
            emptySet()
        }
    }

    fun setDisplayedCalendarIds(ids: Set<String>) {
        saveCalendarConfig(getCalendarConfig().copy(displayedCalendarIds = ids.toList()))
    }

    fun getGridEventStyle(): GridEventStyleEnum = getCalendarConfig().gridEventStyle
    fun setGridEventStyle(style: GridEventStyleEnum) {
        saveCalendarConfig(getCalendarConfig().copy(gridEventStyle = style))
    }

    fun getPublicProfiles(): Map<String, PublicProfile> {
        val jsonString = prefs.getString("public_profiles", null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, PublicProfile>>(jsonString)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun savePublicProfiles(profiles: Map<String, PublicProfile>) {
        prefs.edit().putString("public_profiles", json.encodeToString(profiles)).apply()
    }

    fun getProfilesLastModified(): Long {
        return prefs.getLong("profiles_last_modified", 0L)
    }

    fun saveProfilesLastModified(timestamp: Long) {
        prefs.edit().putLong("profiles_last_modified", timestamp).apply()
    }

    private val contextRef = context

    fun checkAndPurgeIfUserChanged(currentUid: String?): Boolean {
        val lastUid = prefs.getString("last_active_user_uid", null)
        if (lastUid != currentUid) {
            purgeAllUserData()
            prefs.edit().putString("last_active_user_uid", currentUid).apply()
            return true
        }
        return false
    }

    fun purgeAllUserData() {
        val featurePrefixes = listOf("shopping", "notes", "links", "cleaning", "tracker", "calendar", "workday", "support")
        try {
            val dir = File(contextRef.applicationInfo.dataDir, "shared_prefs")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    val name = file.nameWithoutExtension
                    if (featurePrefixes.any { name.startsWith(it) }) {
                        contextRef.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                    }
                }
            }
        } catch (_: Exception) {}

        setCurrentGroupId(null)
        clearAllGroups()
        prefs.edit()
            .remove("group_ids")
            .remove("remembered_groups")
            .remove("user_name")
            .remove("current_group_id")
            .remove("app_user_id")
            .remove("public_profiles")
            .remove("calendar_config")
            .remove("worktime_options")
            .apply()
    }
}
