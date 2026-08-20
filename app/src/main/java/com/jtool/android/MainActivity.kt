package com.jtool.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.crash.CrashInterceptor
import com.jtool.android.core.connectivity.ConnectivityObserver
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.sync.SyncStatusOverlay
import com.jtool.android.core.utils.AppUtils
import com.jtool.android.features.auth.SignInHandler
import com.jtool.android.features.cleaning.CleaningManager
import com.jtool.android.features.notes.NotesManager
import com.jtool.android.features.shopping.ShoppingListManager
import com.jtool.android.ui.AppNavigation
import com.jtool.android.ui.theme.JToolTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var shoppingListManager: ShoppingListManager
    private lateinit var notesManager: NotesManager
    private lateinit var cleaningManager: CleaningManager
    private lateinit var backupManager: BackupManager
    private lateinit var authManager: AuthManager
    private lateinit var groupSyncManager: GroupSyncManager
    
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var signInHandler: SignInHandler
    private lateinit var appUpdateManager: AppUpdateManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            android.util.Log.e("AppUpdate", "Update flow failed! Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashInterceptor.install(applicationContext)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        settingsManager = SettingsManager(this)
        shoppingListManager = ShoppingListManager(this)
        notesManager = NotesManager(this)
        cleaningManager = CleaningManager(this)
        backupManager = BackupManager(this)
        authManager = AuthManager(this)
        groupSyncManager = GroupSyncManager(this)

        connectivityObserver = ConnectivityObserver(this, settingsManager, groupSyncManager, lifecycleScope)
        signInHandler = SignInHandler(this, authManager, groupSyncManager, lifecycleScope)

        AppUtils.logAppSignature(this)
        AppUtils.requestNotificationPermission(this)
        connectivityObserver.start()

        Loc.load(this, settingsManager.getLanguage())

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    groupSyncManager.fetchUserSettings()
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

            JToolTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalLayoutDirection provides Loc.layoutDirection) {
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
                                    currentLang = settingsManager.getLanguage()
                                    Loc.load(this@MainActivity, currentLang)
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

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppUpdate", "Failed to resume update", e)
                }
            }
        }
    }

    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if ((appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppUpdate", "Failed to start update flow", e)
                }
            }
        }
    }
}
