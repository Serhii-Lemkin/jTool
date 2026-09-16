package com.jtool.android

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.BuildConfig
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.crash.CrashInterceptor
import com.jtool.android.core.crash.AnrInterceptor
import com.jtool.android.core.connectivity.ConnectivityObserver
import com.jtool.android.core.config.Config
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.SyncStatusOverlay
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.utils.AppUtils
import com.jtool.android.features.auth.SignInHandler
import com.jtool.android.features.cleaning.CleaningManager
import com.jtool.android.features.notes.NotesManager
import com.jtool.android.features.shopping.ShoppingListManager
import com.jtool.android.ui.AppNavigation
import com.jtool.android.ui.components.notifications_sheet.NotificationsSheet
import com.jtool.android.ui.theme.JToolTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.view.WindowCompat
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var shoppingListManager: ShoppingListManager
    private lateinit var notesManager: NotesManager
    private lateinit var cleaningManager: CleaningManager
    private lateinit var backupManager: BackupManager
    private lateinit var authManager: AuthManager
    private lateinit var dataManager: DataManager
    
    private var pendingScreen by mutableStateOf<AppScreenEnum?>(null)
    
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var signInHandler: SignInHandler
    private lateinit var appUpdateManager: AppUpdateManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e("AppUpdate", "Update flow failed! Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashInterceptor.install(applicationContext)
        AnrInterceptor.install(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        settingsManager = SettingsManager(this)
        if (!settingsManager.hasDarkModeBeenSet()) {
            val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            settingsManager.setDarkMode(isSystemDark)
        }
        settingsManager.checkAndPurgeIfUserChanged(FirebaseAuth.getInstance().currentUser?.uid)
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

        handleIntent(intent)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch {
                    dataManager.fetchUserSettings()
                }
            }
        })

        setContent {
            val auth = remember { FirebaseAuth.getInstance() }
            val currentUserState by produceState(initialValue = auth.currentUser) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
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
            var initialLoadDone by remember { mutableStateOf(false) }
            var isAuthLoading by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }
            
            var allNotifications by remember { mutableStateOf<List<UserNotification>>(emptyList()) }
            var showNotificationsSheet by remember { mutableStateOf(false) }
            var showNotificationPopup by remember { mutableStateOf(false) }
            var showPrivateGroupSheet by remember { mutableStateOf(false) }
            var showNameSetupSheet by remember { mutableStateOf(false) }

            SideEffect {
                signInHandler.onLoadingChanged = { isAuthLoading = it }
                signInHandler.onError = { authError = it }
            }

            LaunchedEffect(currentUserState) {
                if (currentUserState != null) {
                    dataManager.fetchUserSettings(force = true)
                    groups = settingsManager.getGroups()
                    initialLoadDone = true
                    
                    Loc.load(this@MainActivity, settingsManager.getLanguage())
                    isDark = settingsManager.isDarkMode()
                    currentLang = settingsManager.getLanguage()

                    if (settingsManager.getUserName().isEmpty()) {
                        showNameSetupSheet = true
                    }
                }
            }

            LaunchedEffect(currentUserState, groups, currentGroupId) {
                if (currentUserState != null && initialLoadDone) {
                    if (dataManager.ensurePrivateGroupExists()) {
                        showPrivateGroupSheet = true
                        currentGroupId = settingsManager.getCurrentGroupId()
                        groups = settingsManager.getGroups()
                    } else if (currentGroupId == null && groups.isNotEmpty()) {
                        // Auto-select first available group if none is selected
                        val privateGroupId = groups.values.find { it.isPrivate }?.id
                        val targetGroupId = privateGroupId ?: groups.keys.first()
                        lifecycleScope.launch {
                            dataManager.switchGroup(targetGroupId)
                        }
                    }
                }
            }

            val invites by remember(currentUserState) {
                dataManager.invitesFlow()
            }.collectAsState(initial = emptyList())

            val joinRequests by remember(currentGroupId) {
                if (currentGroupId != null) dataManager.joinRequestsFlow(currentGroupId!!)
                else flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            val personalNotifications by remember(currentUserState) {
                if (currentUserState != null) dataManager.personalNotificationsFlow()
                else flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            val groupNotifications by remember(groups) {
                if (groups.isNotEmpty()) {
                    val flows = groups.keys.map { dataManager.groupNotificationsFlow(it) }
                    combine(flows) { it.flatMap { list -> list } }
                } else flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            LaunchedEffect(invites, joinRequests, personalNotifications, groupNotifications) {
                val list = mutableListOf<UserNotification>()
                
                invites.forEach { invite -> 
                    list.add(UserNotification(
                        id = invite.inviteId,
                        groupId = invite.groupId,
                        timestamp = invite.timestamp,
                        type = NotificationTypeEnum.INVITE,
                        targetName = invite.groupName,
                        fromEmail = invite.fromEmail,
                        titleKey = "settings_groupInvitation",
                        messageKey = "settings_invitedToJoin",
                        messageArgs = listOf(invite.groupName)
                    ))
                }
                
                val uid = auth.currentUser?.uid
                joinRequests.filter { it.userId != uid }.forEach { req -> 
                    list.add(UserNotification(
                        id = req.userId,
                        groupId = req.groupId,
                        timestamp = req.timestamp,
                        type = NotificationTypeEnum.JOIN_REQUEST,
                        targetUid = req.userId,
                        targetName = req.userName,
                        targetEmail = req.userEmail,
                        titleKey = "settings_joinRequests",
                        messageKey = "settings_wantsToJoin",
                        messageArgs = listOf(req.userName)
                    ))
                }
                
                list.addAll(personalNotifications)
                list.addAll(groupNotifications)
                
                allNotifications = list.sortedByDescending { it.timestamp }
            }

            var lastNotifCount by remember { mutableStateOf(0) }
            LaunchedEffect(allNotifications.size) {
                if (allNotifications.size > lastNotifCount) {
                    showNotificationPopup = true
                    kotlinx.coroutines.delay(3000)
                    showNotificationPopup = false
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
                                onNotificationsClick = { 
                                    showNotificationsSheet = true 
                                },
                                groups = groups,
                                onGroupsUpdated = { groups = it },
                                pendingScreen = pendingScreen,
                                onPendingScreenHandled = { pendingScreen = null }
                            )
                            
                            if (isAuthLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable(enabled = false) { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            
                            SyncStatusOverlay()

                            if (authError != null) {
                                MessageSheet(
                                    message = authError!!,
                                    type = MessageType.ERROR,
                                    onDismiss = { authError = null }
                                )
                            }

                            AnimatedVisibility(
                                visible = showNotificationPopup,
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 48.dp)
                                    .systemBarsPadding()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            showNotificationsSheet = true
                                            showNotificationPopup = false
                                        },
                                    color = MaterialTheme.colorScheme.error,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    Box(
                                        modifier = Modifier.padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            if (showPrivateGroupSheet) {
                                com.jtool.android.features.settings.groups.components.private_group_created_sheet.PrivateGroupCreatedSheet(
                                    onDismiss = { showPrivateGroupSheet = false }
                                )
                            }

                            if (showNameSetupSheet) {
                                com.jtool.android.features.settings.general.components.name_setup_sheet.NameSetupSheet(
                                    onDismiss = { /* Forced entry, no dismiss */ },
                                    onConfirm = { newName ->
                                        settingsManager.saveUserName(newName)
                                        lifecycleScope.launch {
                                            dataManager.pushUserSettings()
                                            showNameSetupSheet = false
                                        }
                                    }
                                )
                            }

                            if (showNotificationsSheet) {
                                NotificationsSheet(
                                    notifications = allNotifications,
                                    onAcceptInvite = { notif ->
                                        lifecycleScope.launch { 
                                            dataManager.respondToInvite(notif.id, notif.groupId ?: "", true) 
                                        }
                                    },
                                    onDeclineInvite = { notif ->
                                        lifecycleScope.launch { 
                                            dataManager.respondToInvite(notif.id, notif.groupId ?: "", false) 
                                        }
                                    },
                                    onAcceptJoinRequest = { notif ->
                                        lifecycleScope.launch { 
                                            val request = JoinRequest(
                                                groupId = notif.groupId ?: "",
                                                userId = notif.targetUid ?: "",
                                                userName = notif.targetName ?: "",
                                                userEmail = notif.targetEmail ?: ""
                                            )
                                            dataManager.acceptJoinRequest(request) 
                                        } 
                                    },
                                    onDeclineJoinRequest = { notif ->
                                        lifecycleScope.launch { 
                                            val request = JoinRequest(
                                                groupId = notif.groupId ?: "",
                                                userId = notif.targetUid ?: ""
                                            )
                                            dataManager.declineJoinRequest(request) 
                                        } 
                                    },
                                    onDismissNotification = { notif ->
                                        lifecycleScope.launch { dataManager.markNotificationAsRead(notif) }
                                    },
                                    onLinkClick = { link ->
                                        val uri = Uri.parse(link)
                                        if (uri.host == BuildConfig.DEEP_LINK_HOST) {
                                            val path = uri.path ?: ""
                                            if (path.startsWith("/event/")) {
                                                val eventId = path.removePrefix("/event/")
                                                if (eventId.isNotEmpty()) {
                                                    settingsManager.setSelectedCalendarEventId(eventId)
                                                    pendingScreen = AppScreenEnum.CALENDAR_EVENT_DETAIL
                                                    showNotificationsSheet = false
                                                }
                                            }
                                        }
                                    },
                                    onShowError = { authError = it },
                                    onDismiss = { showNotificationsSheet = false }
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
                    Log.e("AppUpdate", "Failed to resume update", e)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if ((uri.scheme == "https" || uri.scheme == "http") && uri.host == BuildConfig.DEEP_LINK_HOST) {
                val path = uri.path ?: ""
                if (path.startsWith("/event/")) {
                    val eventId = path.removePrefix("/event/")
                    if (eventId.isNotEmpty()) {
                        settingsManager.setSelectedCalendarEventId(eventId)
                        pendingScreen = AppScreenEnum.CALENDAR_EVENT_DETAIL
                    }
                }
            } else if (uri.scheme == "jtool" && uri.host == "event") {
                val eventId = uri.lastPathSegment
                if (!eventId.isNullOrEmpty()) {
                    settingsManager.setSelectedCalendarEventId(eventId)
                    pendingScreen = AppScreenEnum.CALENDAR_EVENT_DETAIL
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
                    Log.e("AppUpdate", "Failed to start update flow", e)
                }
            }
        }
    }

    private fun initializeLanguage() {
        if (!settingsManager.hasLanguageBeenSet()) {
            val systemLocale = Locale.getDefault()
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
