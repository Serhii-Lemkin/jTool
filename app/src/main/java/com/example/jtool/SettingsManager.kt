package com.example.jtool

import android.content.Context
import java.time.DayOfWeek

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_settings", Context.MODE_PRIVATE)

    fun saveDayHours(day: DayOfWeek, hours: Double) {
        prefs.edit().putFloat("hours_${day.name}", hours.toFloat()).apply()
    }

    fun getDayHours(day: DayOfWeek): Double {
        val default = when (day) {
            DayOfWeek.THURSDAY -> 8.0
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> 0.0
            else -> 8.5
        }
        val key = "hours_${day.name}"
        val value = prefs.all[key]
        return when (value) {
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Double -> value
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }
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
        return daysInOrder.associateWith { getDayHours(it) }
    }

    fun saveRememberedStart(time: String, date: String) {
        prefs.edit()
            .putString("remembered_time", time)
            .putString("remembered_date", date)
            .apply()
    }

    fun getRememberedStart(currentDate: String): String? {
        val savedDate = prefs.getString("remembered_date", "")
        return if (savedDate == currentDate) {
            prefs.getString("remembered_time", null)
        } else {
            null
        }
    }

    fun saveSyncUri(uri: String?) {
        if (uri.isNullOrEmpty()) {
            prefs.edit().remove("sync_uri").apply()
        } else {
            prefs.edit().putString("sync_uri", uri).apply()
        }
    }

    fun getSyncUri(): String? {
        return prefs.getString("sync_uri", null)
    }

    fun setSyncPending(pending: Boolean) {
        prefs.edit().putBoolean("sync_pending", pending).apply()
    }

    fun isSyncPending(): Boolean {
        return prefs.getBoolean("sync_pending", false)
    }

    fun clearRememberedStart() {
        prefs.edit()
            .remove("remembered_time")
            .remove("remembered_date")
            .apply()
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

    fun getAppUserId(): String {
        var id = prefs.getString("app_user_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString("app_user_id", id).apply()
        }
        return id
    }

    fun getLanguage(): String {
        return prefs.getString("language", "en") ?: "en"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
    }

    fun isAppVisible(appId: String): Boolean {
        return prefs.getBoolean("visible_$appId", true)
    }

    fun setAppVisible(appId: String, visible: Boolean) {
        prefs.edit().putBoolean("visible_$appId", visible).apply()
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

    fun getGroupIds(): List<String> {
        return prefs.getString("group_ids", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun addGroupId(groupId: String) {
        val groups = getGroupIds().toMutableList()
        if (!groups.contains(groupId)) {
            groups.add(groupId)
            prefs.edit().putString("group_ids", groups.joinToString(",")).apply()
        }
    }

    fun removeGroupId(groupId: String) {
        val groups = getGroupIds().toMutableList()
        groups.remove(groupId)
        prefs.edit().putString("group_ids", groups.joinToString(",")).apply()
    }

    fun getUpdateUrl(): String? {
        return prefs.getString("update_url", null)
    }

    fun setUpdateUrl(url: String?) {
        prefs.edit().putString("update_url", url).apply()
    }

    fun getSyncInterval(): Int {
        return prefs.getInt("sync_interval", 10)
    }

    fun setSyncInterval(seconds: Int) {
        prefs.edit().putInt("sync_interval", seconds.coerceAtLeast(5)).apply()
    }
}
