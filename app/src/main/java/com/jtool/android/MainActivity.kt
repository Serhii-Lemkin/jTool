package com.jtool.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.connectivity.ConnectivityObserver
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.sync.SyncStatusOverlay
import com.jtool.android.core.utils.AppUtils
import com.jtool.android.features.auth.SignInHandler
import com.jtool.android.features.cleaning.JCleaningManager
import com.jtool.android.features.notes.JNotesManager
import com.jtool.android.features.shopping.ShoppingListManager
import com.jtool.android.ui.AppNavigation
import com.jtool.android.ui.theme.JToolTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var shoppingListManager: ShoppingListManager
    private lateinit var notesManager: JNotesManager
    private lateinit var cleaningManager: JCleaningManager
    private lateinit var backupManager: BackupManager
    private lateinit var authManager: AuthManager
    private lateinit var groupSyncManager: GroupSyncManager
    
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var signInHandler: SignInHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        settingsManager = SettingsManager(this)
        shoppingListManager = ShoppingListManager(this)
        notesManager = JNotesManager(this)
        cleaningManager = JCleaningManager(this)
        backupManager = BackupManager(this)
        authManager = AuthManager(this)
        groupSyncManager = GroupSyncManager(this)

        connectivityObserver = ConnectivityObserver(this, settingsManager, groupSyncManager, lifecycleScope)
        signInHandler = SignInHandler(this, authManager, groupSyncManager, lifecycleScope)

        AppUtils.logAppSignature(this)
        AppUtils.requestNotificationPermission(this)
        connectivityObserver.start()

        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    groupSyncManager.fetchUserSettings()
                    if (authManager.getCurrentUser() != null) {
                        groupSyncManager.pullGroupData()
                    }
                }
            }
        })

        setContent {
            val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
            val currentUserState by produceState(initialValue = auth.currentUser) {
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                    value = firebaseAuth.currentUser
                }
                auth.addAuthStateListener(listener)
                awaitDispose {
                    auth.removeAuthStateListener(listener)
                }
            }

            var isDark by remember { mutableStateOf(settingsManager.isDarkMode()) }
            var currentLang by remember { mutableStateOf(settingsManager.getLanguage()) }
            var currentGroupId by remember { mutableStateOf(settingsManager.getCurrentGroupId()) }
            var groups by remember { mutableStateOf(settingsManager.getGroups()) }

            Loc.lang = currentLang

            JToolTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            settingsManager = settingsManager,
                            shoppingListManager = shoppingListManager,
                            notesManager = notesManager,
                            cleaningManager = cleaningManager,
                            backupManager = backupManager,
                            authManager = authManager,
                            groupSyncManager = groupSyncManager,
                            onSignIn = { signInHandler.launchSignIn() },
                            isDarkMode = isDark,
                            currentUser = currentUserState,
                            currentGroupId = currentGroupId,
                            onGroupChanged = { 
                                currentGroupId = it
                                groups = settingsManager.getGroups()
                            },
                            onThemeToggle = {
                                isDark = !isDark
                                settingsManager.setDarkMode(isDark)
                                lifecycleScope.launch { groupSyncManager.pushUserSettings() }
                            },
                            currentLang = currentLang,
                            onLangToggle = {
                                currentLang = if (currentLang == "en") "uk" else "en"
                                settingsManager.setLanguage(currentLang)
                                Loc.lang = currentLang
                                lifecycleScope.launch { groupSyncManager.pushUserSettings() }
                            },
                            groups = groups,
                            onGroupsUpdated = { groups = it }
                        )
                        
                        SyncStatusOverlay()
                    }
                }
            }
        }
    }
}
