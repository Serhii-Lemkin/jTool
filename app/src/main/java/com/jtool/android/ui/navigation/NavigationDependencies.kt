package com.jtool.android.ui.navigation

import com.google.firebase.auth.FirebaseUser
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.models.JGroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.features.cleaning.JCleaningManager
import com.jtool.android.features.links.JLinksManager
import com.jtool.android.features.notes.JNotesManager
import com.jtool.android.features.shopping.ShoppingListManager

data class NavigationDependencies(
    val settingsManager: SettingsManager,
    val shoppingListManager: ShoppingListManager,
    val notesManager: JNotesManager,
    val cleaningManager: JCleaningManager,
    val linksManager: JLinksManager,
    val backupManager: BackupManager,
    val authManager: AuthManager,
    val groupSyncManager: GroupSyncManager,
    val onSignIn: () -> Unit,
    val isDarkMode: Boolean,
    val currentUser: FirebaseUser?,
    val currentGroupId: String?,
    val onGroupChanged: (String?) -> Unit,
    val groups: Map<String, JGroupInfo>,
    val onGroupsUpdated: (Map<String, JGroupInfo>) -> Unit,
    val onThemeToggle: () -> Unit,
    val currentLang: String,
    val onLangToggle: () -> Unit,
    val setScreen: (String) -> Unit,
    val navigateToUpdates: Boolean,
    val navigateToGroups: Boolean,
    val setNavigateToGroups: (Boolean) -> Unit,
    val hasUpdate: Boolean,
    val isAdmin: Boolean
)
