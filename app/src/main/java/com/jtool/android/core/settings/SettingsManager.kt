package com.jtool.android.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.calendar.CalendarConfig
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import com.jtool.android.core.localization.Loc
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

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun getWorktimeOptions(): WorktimeOptions {
        val jsonString = prefs.getString("worktime_options", null) ?: return WorktimeOptions()
        return try {
            json.decodeFromString<WorktimeOptions>(jsonString)
        } catch (_: Exception) {
            WorktimeOptions()
        }
    }

    fun saveWorktimeOptions(options: WorktimeOptions) = prefs.edit().putString("worktime_options", json.encodeToString(options)).apply()

    fun getAllWeeklyHours(): Map<DayOfWeek, Double> {
        val daysInOrder = listOf(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
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

    fun isSyncPending(): Boolean = prefs.getBoolean("sync_pending", false)
    fun setSyncPending(pending: Boolean) = prefs.edit().putBoolean("sync_pending", pending).apply()
    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)
    fun setDarkMode(enabled: Boolean) { prefs.edit().putBoolean("dark_mode", enabled).commit() }
    fun hasDarkModeBeenSet(): Boolean = prefs.contains("dark_mode")
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun saveUserName(name: String) { prefs.edit().putString("user_name", name).commit() }

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
    fun saveAppUserId(id: String) = prefs.edit().putString("app_user_id", id).apply()
    fun getLanguage(): String = prefs.getString("language", "en") ?: "en"
    fun getSeenVaultsInfo(): Boolean = prefs.getBoolean("seen_vaults_info", false)
    fun setSeenVaultsInfo(seen: Boolean) = prefs.edit().putBoolean("seen_vaults_info", seen).apply()
    fun getSelectedVaultId(): String = prefs.getString("selected_vault_id", "") ?: ""
    fun setSelectedVaultId(id: String) = prefs.edit().putString("selected_vault_id", id).apply()
    fun getJtoolsFolderId(): String = prefs.getString("jtools_folder_id", "") ?: ""
    fun setJtoolsFolderId(id: String) = prefs.edit().putString("jtools_folder_id", id).apply()
    fun getVaultsFolderId(): String = prefs.getString("vaults_folder_id", "") ?: ""
    fun setVaultsFolderId(id: String) = prefs.edit().putString("vaults_folder_id", id).apply()
    fun hasLanguageBeenSet(): Boolean = prefs.contains("language")
    fun setLanguage(lang: String) = prefs.edit().putString("language", lang).apply()
    fun getDateFormat(): String = prefs.getString("date_format", "DD/MM/YYYY") ?: "DD/MM/YYYY"
    fun setDateFormat(format: String) = prefs.edit().putString("date_format", format).apply()

    fun getPinnedVaultIds(): List<String> {
        val jsonStr = prefs.getString("pinned_vault_ids", null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    fun savePinnedVaultIds(ids: List<String>) = prefs.edit().putString("pinned_vault_ids", json.encodeToString(ids)).apply()

    fun togglePinVault(vaultId: String) {
        val current = getPinnedVaultIds().toMutableList()
        if (current.contains(vaultId)) current.remove(vaultId) else current.add(vaultId)
        savePinnedVaultIds(current)
    }

    fun isVaultPinned(vaultId: String): Boolean = getPinnedVaultIds().contains(vaultId)
    fun getCachedWebsiteUrl(): String = prefs.getString("cached_website_url", "") ?: ""
    fun setCachedWebsiteUrl(url: String) = prefs.edit().putString("cached_website_url", url).apply()

    fun getLauncherConfig(): com.jtool.android.core.models.user.LauncherConfig {
        val jsonStr = prefs.getString("launcher_config", null)
        if (jsonStr != null) {
            return try {
                json.decodeFromString<com.jtool.android.core.models.user.LauncherConfig>(jsonStr)
            } catch (_: Exception) {
                com.jtool.android.core.models.user.LauncherConfig(
                    appOrder = getAppOrder(),
                    hiddenApps = getHiddenApps(),
                    gridColumnCount = getGridColumnCount()
                )
            }
        }
        return com.jtool.android.core.models.user.LauncherConfig(
            appOrder = getAppOrder(),
            hiddenApps = getHiddenApps(),
            gridColumnCount = getGridColumnCount()
        )
    }

    fun saveLauncherConfig(config: com.jtool.android.core.models.user.LauncherConfig) {
        prefs.edit().putString("launcher_config", json.encodeToString(config)).apply()
        if (config.appOrder.isNotEmpty()) saveAppOrder(config.appOrder)
        saveHiddenApps(config.hiddenApps)
        if (config.gridColumnCount in 1..4) setGridColumnCount(config.gridColumnCount)
    }

    fun getNotesConfig(): com.jtool.android.core.models.user.NotesConfig {
        val jsonStr = prefs.getString("notes_config", null) ?: return com.jtool.android.core.models.user.NotesConfig()
        return try {
            json.decodeFromString<com.jtool.android.core.models.user.NotesConfig>(jsonStr)
        } catch (_: Exception) {
            com.jtool.android.core.models.user.NotesConfig()
        }
    }

    fun saveNotesConfig(config: com.jtool.android.core.models.user.NotesConfig) =
        prefs.edit().putString("notes_config", json.encodeToString(config)).apply()

    fun getLauncherGestures(): Map<String, String> = getLauncherConfig().gestures

    fun saveLauncherGesture(gestureId: String, actionId: String) {
        val config = getLauncherConfig()
        val newGestures = config.gestures.toMutableMap()
        if (actionId.isEmpty()) newGestures.remove(gestureId) else newGestures[gestureId] = actionId
        saveLauncherConfig(config.copy(gestures = newGestures))
    }

    fun getAppOrder(): List<String> {
        val jsonStr = prefs.getString("app_order", null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    fun saveAppOrder(order: List<String>) = prefs.edit().putString("app_order", json.encodeToString(order)).apply()

    fun getHiddenApps(): List<String> {
        val jsonStr = prefs.getString("hidden_apps", null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    fun saveHiddenApps(hidden: List<String>) = prefs.edit().putString("hidden_apps", json.encodeToString(hidden)).apply()
    fun isAppVisible(appId: String): Boolean = !getHiddenApps().contains(appId)
    fun isAppVisible(appId: AppScreenEnum): Boolean = isAppVisible(appId.id)

    fun setAppVisible(appId: String, visible: Boolean) {
        val hidden = getHiddenApps().toMutableList()
        if (visible) hidden.remove(appId) else if (!hidden.contains(appId)) hidden.add(appId)
        saveHiddenApps(hidden)
    }

    fun setAppVisible(appId: AppScreenEnum, visible: Boolean) = setAppVisible(appId.id, visible)
    fun getGridColumnCount(): Int = prefs.getInt("grid_column_count", 3).coerceIn(1, 4)
    fun setGridColumnCount(count: Int) = prefs.edit().putInt("grid_column_count", count.coerceIn(1, 4)).apply()
    fun isGridLayout(): Boolean = prefs.getBoolean("is_grid_layout", true)
    fun setGridLayout(isGrid: Boolean) = prefs.edit().putBoolean("is_grid_layout", isGrid).apply()
    fun getCurrentGroupId(): String? = prefs.getString("current_group_id", null)
    fun setCurrentGroupId(groupId: String?) { prefs.edit().putString("current_group_id", groupId).commit() }

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
                    isPrivate = if (groupJson.has("isPrivate")) groupJson.getBoolean("isPrivate") else false,
                    isInviteOnly = if (groupJson.has("isInviteOnly")) groupJson.getBoolean("isInviteOnly") else false
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
            groupJson.put("isInviteOnly", info.isInviteOnly)
            obj.put(id, groupJson)
        }
        prefs.edit().putString("remembered_groups", obj.toString()).commit()
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

    fun clearAllGroups() = prefs.edit().remove("remembered_groups").apply()
    fun getGroupIds(): List<String> = getGroups().keys.toList()
    fun getUpdateUrl(): String? = prefs.getString("update_url", null)
    fun setUpdateUrl(url: String?) = prefs.edit().putString("update_url", url).apply()
    fun getSelectedCalendarDate(): String = prefs.getString("selected_calendar_date", java.time.LocalDate.now().toString()) ?: java.time.LocalDate.now().toString()
    fun setSelectedCalendarDate(date: String) { prefs.edit().putString("selected_calendar_date", date).commit() }
    fun getSelectedCalendarEventId(): String? = prefs.getString("selected_calendar_event_id", null)
    fun setSelectedCalendarEventId(id: String?) { prefs.edit().putString("selected_calendar_event_id", id).commit() }
    fun getSelectedSupportRequestId(): String? = prefs.getString("selected_support_request_id", null)
    fun setSelectedSupportRequestId(id: String?) { prefs.edit().putString("selected_support_request_id", id).commit() }
    fun shouldAutoOpenCalendarAdd(): Boolean = prefs.getBoolean("auto_open_calendar_add", false)
    fun setAutoOpenCalendarAdd(open: Boolean) { prefs.edit().putBoolean("auto_open_calendar_add", open).commit() }
    fun getSupportTypeFilters(): Set<String> = prefs.getStringSet("support_type_filters", emptySet()) ?: emptySet()
    fun saveSupportTypeFilters(filters: Set<String>) = prefs.edit().putStringSet("support_type_filters", filters).apply()
    fun getSupportStatusFilters(): Set<String> = prefs.getStringSet("support_status_filters", setOf("OPENED", "IN_PROGRESS")) ?: setOf("OPENED", "IN_PROGRESS")
    fun saveSupportStatusFilters(filters: Set<String>) = prefs.edit().putStringSet("support_status_filters", filters).apply()
    fun getVisibleGroupIds(): Set<String> = prefs.getStringSet("visible_calendar_groups", emptySet()) ?: emptySet()
    fun saveVisibleGroupIds(groupIds: Set<String>) = prefs.edit().putStringSet("visible_calendar_groups", groupIds).apply()
    fun getHiddenGroupIds(): Set<String> = prefs.getStringSet("hidden_calendar_groups", emptySet()) ?: emptySet()
    fun saveHiddenGroupIds(groupIds: Set<String>) = prefs.edit().putStringSet("hidden_calendar_groups", groupIds).apply()
    fun getLastSyncTimestamp(): Long = prefs.getLong("last_sync_timestamp", 0L)
    fun setLastSyncTimestamp(timestamp: Long) = prefs.edit().putLong("last_sync_timestamp", timestamp).apply()

    fun getCalendarConfig(): CalendarConfig {
        val jsonString = prefs.getString("calendar_config", null) ?: return CalendarConfig()
        return try { json.decodeFromString<CalendarConfig>(jsonString) } catch (_: Exception) { CalendarConfig() }
    }

    fun saveCalendarConfig(config: CalendarConfig) = prefs.edit().putString("calendar_config", json.encodeToString(config)).apply()
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

    fun setDisplayedCalendarIds(ids: Set<String>) = saveCalendarConfig(getCalendarConfig().copy(displayedCalendarIds = ids.toList()))
    fun isCalendarGridOpened(): Boolean = prefs.getBoolean("calendar_grid_opened", false)
    fun setCalendarGridOpened(opened: Boolean) = prefs.edit().putBoolean("calendar_grid_opened", opened).apply()

    fun getCustomWorkShiftTitle(): String {
        val saved = prefs.getString("custom_work_shift_title", null)
        return if (!saved.isNullOrBlank()) saved else Loc.s("calendar_shift", "Work Shift")
    }

    fun setCustomWorkShiftTitle(title: String) = prefs.edit().putString("custom_work_shift_title", title.trim()).apply()
    fun isShoppingAddBoxCompact(): Boolean = prefs.getBoolean("shopping_add_box_compact", false)
    fun setShoppingAddBoxCompact(compact: Boolean) = prefs.edit().putBoolean("shopping_add_box_compact", compact).apply()

    fun getPublicProfiles(): Map<String, PublicProfile> {
        val jsonString = prefs.getString("public_profiles", null) ?: return emptyMap()
        return try { json.decodeFromString<Map<String, PublicProfile>>(jsonString) } catch (_: Exception) { emptyMap() }
    }

    fun savePublicProfiles(profiles: Map<String, PublicProfile>) = prefs.edit().putString("public_profiles", json.encodeToString(profiles)).apply()
    fun getProfilesLastModified(): Long = prefs.getLong("profiles_last_modified", 0L)
    fun saveProfilesLastModified(timestamp: Long) = prefs.edit().putLong("profiles_last_modified", timestamp).apply()
    fun getLightPrimaryColor(): String = prefs.getString("light_primary", "0xFF059669") ?: "0xFF059669"
    fun setLightPrimaryColor(hex: String) { prefs.edit().putString("light_primary", hex).commit() }
    fun getLightBgColor(): String = prefs.getString("light_bg", "0xFFF0FDF4") ?: "0xFFF0FDF4"
    fun setLightBgColor(hex: String) { prefs.edit().putString("light_bg", hex).commit() }
    fun getDarkPrimaryColor(): String = prefs.getString("dark_primary", "0xFF10B981") ?: "0xFF10B981"
    fun setDarkPrimaryColor(hex: String) { prefs.edit().putString("dark_primary", hex).commit() }
    fun getDarkBgColor(): String = prefs.getString("dark_bg", "0xFF09090B") ?: "0xFF09090B"
    fun setDarkBgColor(hex: String) { prefs.edit().putString("dark_bg", hex).commit() }
    fun getFloatingContainerSide(): String = prefs.getString("floating_container_side", "RIGHT") ?: "RIGHT"
    fun setFloatingContainerSide(side: String) = prefs.edit().putString("floating_container_side", side).apply()
    fun isFloatingContainerCollapsed(): Boolean = prefs.getBoolean("floating_container_collapsed", false)
    fun setFloatingContainerCollapsed(collapsed: Boolean) = prefs.edit().putBoolean("floating_container_collapsed", collapsed).apply()
    fun isLargeFileWarningEnabled(): Boolean = prefs.getBoolean("vault_enable_large_file_warning", true)
    fun setLargeFileWarningEnabled(enabled: Boolean) = prefs.edit().putBoolean("vault_enable_large_file_warning", enabled).apply()
    fun getMaxFileSizeWithoutWarningMb(): Int = prefs.getInt("vault_max_file_size_mb", 100)
    fun setMaxFileSizeWithoutWarningMb(mb: Int) = prefs.edit().putInt("vault_max_file_size_mb", mb).apply()
    fun isShowDriveButtonsEnabled(): Boolean = prefs.getBoolean("vault_show_drive_buttons", false)
    fun setShowDriveButtonsEnabled(enabled: Boolean) = prefs.edit().putBoolean("vault_show_drive_buttons", enabled).apply()
    fun getCachedCustomTags(): Set<String> = prefs.getStringSet("cached_custom_tags", emptySet()) ?: emptySet()

    fun saveCustomTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        val current = getCachedCustomTags().toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet("cached_custom_tags", current).apply()
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
