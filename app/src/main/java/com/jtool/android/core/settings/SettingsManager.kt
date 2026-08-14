package com.jtool.android.core.settings

import android.content.Context
import com.jtool.android.core.models.JGroupInfo
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
            is Double -> value.toDouble()
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
            saveAppUserId(id)
        }
        return id
    }

    fun saveAppUserId(id: String) {
        prefs.edit().putString("app_user_id", id).apply()
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

    fun getGroups(): Map<String, JGroupInfo> {
        val json = prefs.getString("remembered_groups", "{}") ?: "{}"
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, JGroupInfo>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val groupJson = obj.getJSONObject(id)
                map[id] = JGroupInfo(
                    id = id,
                    name = groupJson.getString("name"),
                    owner = if (groupJson.has("owner")) groupJson.getString("owner") else null
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveGroups(groups: Map<String, JGroupInfo>) {
        val obj = org.json.JSONObject()
        groups.forEach { (id, info) -> 
            val groupJson = org.json.JSONObject()
            groupJson.put("name", info.name)
            groupJson.put("owner", info.owner)
            obj.put(id, groupJson)
        }
        prefs.edit().putString("remembered_groups", obj.toString()).apply()
    }

    fun addGroup(info: JGroupInfo) {
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
}
