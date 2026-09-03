package com.jtool.android.core.settings

import android.content.Context
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.WorktimeOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
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

    fun getUserName(): String {
        var name = prefs.getString("user_name", null)
        if (name.isNullOrEmpty()) {
            name = "User_${(1000..9999).random()}"
            saveUserName(name)
        }
        return name
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
                    ownerUid = if (groupJson.has("ownerUid")) groupJson.getString("ownerUid") else null
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
}
