package com.jtool.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.features.cleaning.CleaningManager
import com.jtool.android.features.links.LinksManager
import com.jtool.android.features.notes.NotesManager
import com.jtool.android.features.shopping.ShoppingListManager
import com.jtool.android.ui.navigation.AppProperties
import com.jtool.android.ui.navigation.AppNames
import com.jtool.android.ui.navigation.NavigationDependencies
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    settingsManager: SettingsManager, 
    shoppingListManager: ShoppingListManager,
    notesManager: NotesManager,
    cleaningManager: CleaningManager,
    backupManager: BackupManager,
    authManager: AuthManager,
    groupSyncManager: GroupSyncManager,
    onSignIn: () -> Unit,
    isDarkMode: Boolean,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    currentGroupId: String?,
    onGroupChanged: (String?) -> Unit,
    groups: Map<String, GroupInfo>,
    onGroupsUpdated: (Map<String, GroupInfo>) -> Unit,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppNames.LAUNCHER) }
    var navigateToUpdates by remember { mutableStateOf(false) }
    var navigateToGroups by remember { mutableStateOf(false) }
    var updateVersionCode by remember { mutableStateOf<Int?>(null) }
    var updateVersionName by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val isAdmin by groupSyncManager.isAdminFlow().collectAsState(initial = false)

    // Automatically sync group list when user memberships change on server
    val userGroupIds by remember(currentUser) { 
        groupSyncManager.userGroupsFlow() 
    }.collectAsState(initial = groups.keys.toList())

    LaunchedEffect(userGroupIds) {
        if (currentUser != null) {
            groupSyncManager.fetchUserSettings()
            onGroupsUpdated(settingsManager.getGroups())
        }
    }

    val allGroupIds = groups.keys.toList()
    val liveGroups by remember(allGroupIds) { 
        groupSyncManager.groupInfoFlow(allGroupIds) 
    }.collectAsState(initial = emptyMap<String, GroupInfo>())
    
    LaunchedEffect(liveGroups) {
        if (liveGroups.isNotEmpty() && liveGroups != groups) {
            onGroupsUpdated(liveGroups)
            settingsManager.saveGroups(liveGroups)
        }
    }

    // Firestore update check removed in favor of Google Play In-App Updates API

    if (currentScreen != AppNames.LAUNCHER) {
        BackHandler {
            currentScreen = AppNames.LAUNCHER
        }
    }

    val deps = NavigationDependencies(
        settingsManager = settingsManager,
        shoppingListManager = shoppingListManager,
        notesManager = notesManager,
        cleaningManager = cleaningManager,
        linksManager = LinksManager(context),
        backupManager = backupManager,
        authManager = authManager,
        groupSyncManager = groupSyncManager,
        onSignIn = onSignIn,
        isDarkMode = isDarkMode,
        currentUser = currentUser,
        currentGroupId = currentGroupId,
        onGroupChanged = onGroupChanged,
        groups = groups,
        onGroupsUpdated = onGroupsUpdated,
        onThemeToggle = onThemeToggle,
        currentLang = currentLang,
        onLangToggle = onLangToggle,
        setScreen = { currentScreen = it },
        navigateToUpdates = updateVersionCode != null,
        navigateToGroups = navigateToGroups,
        setNavigateToGroups = { navigateToGroups = it },
        hasUpdate = updateVersionCode != null,
        isAdmin = isAdmin
    )

    AppProperties.fromId(currentScreen)?.Content(deps)
}
