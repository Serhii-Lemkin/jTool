package com.jtool.android.ui.navigation

import com.google.firebase.auth.FirebaseUser
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.cleaning.CleaningManager
import com.jtool.android.features.links.LinksManager
import com.jtool.android.features.notes.NotesManager
import com.jtool.android.features.shopping.ShoppingListManager

data class NavigationDependencies(
    val settingsManager: SettingsManager,
    val shoppingListManager: ShoppingListManager,
    val notesManager: NotesManager,
    val cleaningManager: CleaningManager,
    val linksManager: LinksManager,
    val backupManager: BackupManager,
    val authManager: AuthManager,
    val dataManager: DataManager,
    val onSignIn: () -> Unit,
    val isDarkMode: Boolean,
    val currentUser: FirebaseUser?,
    val currentGroupId: String?,
    val onGroupChanged: (String?) -> Unit,
    val groups: Map<String, GroupInfo>,
    val onGroupsUpdated: (Map<String, GroupInfo>) -> Unit,
    val onThemeToggle: () -> Unit,
    val currentLang: String,
    val onLangToggle: () -> Unit,
    val setScreen: (AppScreenEnum) -> Unit,
    val navigateToUpdates: Boolean,
    val navigateToGroups: Boolean,
    val setNavigateToGroups: (Boolean) -> Unit,
    val isAdmin: Boolean,
    val notificationCount: Int,
    val onNotificationsClick: () -> Unit,
    val onBack: () -> Unit
)
