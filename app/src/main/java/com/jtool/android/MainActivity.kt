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
import com.jtool.android.core.models.common.AppNotification
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
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
    private lateinit var dataManager: DataManager
    
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
        dataManager = DataManager(this)

        connectivityObserver = ConnectivityObserver(this, settingsManager, dataManager, lifecycleScope)
        signInHandler = SignInHandler(this, authManager, dataManager, lifecycleScope)

        AppUtils.logAppSignature(this)
        AppUtils.requestNotificationPermission(this)
        connectivityObserver.start()

        initializeLanguage()

        Loc.load(this, settingsManager.getLanguage())

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    dataManager.fetchUserSettings()
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
            
            var allNotifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
            var showNotificationsSlip by remember { mutableStateOf(false) }

            val invites by remember(currentUserState) {
                dataManager.invitesFlow()
            }.collectAsState(initial = emptyList())

            val joinRequests by remember(currentGroupId) {
                if (currentGroupId != null) dataManager.joinRequestsFlow(currentGroupId!!)
                else kotlinx.coroutines.flow.flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            val personalNotifications by remember(currentUserState) {
                if (currentUserState != null) dataManager.personalNotificationsFlow()
                else kotlinx.coroutines.flow.flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            val groupNotifications by remember(currentGroupId) {
                if (currentGroupId != null) dataManager.groupNotificationsFlow(currentGroupId!!)
                else kotlinx.coroutines.flow.flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            LaunchedEffect(invites, joinRequests, personalNotifications, groupNotifications) {
                val list = mutableListOf<AppNotification>()
                invites.forEach { list.add(AppNotification.GroupInvite(it)) }
                val uid = auth.currentUser?.uid
                joinRequests.filter { it.userId != uid }.forEach { list.add(AppNotification.GroupJoinRequest(it)) }
                personalNotifications.forEach { list.add(AppNotification.SystemResult(it)) }
                groupNotifications.forEach { list.add(AppNotification.SystemResult(it)) }
                allNotifications = list.sortedByDescending { it.timestamp }
            }

            var lastNotifCount by remember { mutableStateOf(0) }
            LaunchedEffect(allNotifications.size) {
                if (allNotifications.size > lastNotifCount) {
                    showNotificationsSlip = true
                }
                lastNotifCount = allNotifications.size
            }

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
                                dataManager = dataManager,
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
                                    lifecycleScope.launch { dataManager.pushUserSettings() }
                                },
                                currentLang = currentLang,
                                onLangToggle = {
                                    currentLang = settingsManager.getLanguage()
                                    Loc.load(this@MainActivity, currentLang)
                                },
                                notificationCount = allNotifications.size,
                                onNotificationsClick = { showNotificationsSlip = true },
                                groups = groups,
                                onGroupsUpdated = { groups = it }
                            )
                            
                            SyncStatusOverlay()

                            if (showNotificationsSlip) {
                                com.jtool.android.ui.components.notifications_slip.NotificationsSlip(
                                    notifications = allNotifications,
                                    onAcceptInvite = { 
                                        lifecycleScope.launch { 
                                            dataManager.respondToInvite(it.invite.inviteId, it.invite.groupId, true) 
                                        }
                                    },
                                    onDeclineInvite = {
                                        lifecycleScope.launch { 
                                            dataManager.respondToInvite(it.invite.inviteId, it.invite.groupId, false) 
                                        }
                                    },
                                    onAcceptJoinRequest = { 
                                        lifecycleScope.launch { dataManager.acceptJoinRequest(it.request) } 
                                    },
                                    onDeclineJoinRequest = { 
                                        lifecycleScope.launch { dataManager.declineJoinRequest(it.request) } 
                                    },
                                    onDismissSystemNotification = {
                                        lifecycleScope.launch { dataManager.markNotificationAsRead(it.notification) }
                                    },
                                    onDismiss = { showNotificationsSlip = false }
                                )
                            }
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

    private fun initializeLanguage() {
        if (!settingsManager.hasLanguageBeenSet()) {
            val systemLocale = java.util.Locale.getDefault()
            val systemLangCode = systemLocale.language
            
            val normalizedCode = when(systemLangCode) {
                "iw" -> "he"
                else -> systemLangCode
            }

            val supportedLanguages = Loc.getAvailableLanguages(this).map { it.locale }
            
            if (supportedLanguages.contains(normalizedCode)) {
                settingsManager.setLanguage(normalizedCode)
            } else {
                lifecycleScope.launch {
                    dataManager.logRequestedLanguage(
                        name = systemLocale.displayLanguage,
                        code = systemLangCode
                    )
                }
                settingsManager.setLanguage("en")
            }
        }
    }
}
