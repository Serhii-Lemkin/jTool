package com.example.jtool

import android.os.Bundle
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.jtool.ui.theme.JToolTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.jtool.settingsSections.*

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var shoppingListManager: ShoppingListManager
    private lateinit var notesManager: JNotesManager
    private lateinit var cleaningManager: JCleaningManager
    private lateinit var backupManager: BackupManager
    private lateinit var authManager: AuthManager
    private lateinit var groupSyncManager: GroupSyncManager

    private val scope = CoroutineScope(Dispatchers.Default)

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                lifecycleScope.launch {
                    val success = authManager.signInWithGoogle(account)
                    if (success) {
                        groupSyncManager.fetchUserSettings()
                        groupSyncManager.pushUserSettings()
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "Firebase Auth Failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("jTool", "Sign in failed", e)
            val msg = if (e is com.google.android.gms.common.api.ApiException) "Error Code: ${e.statusCode}" else e.message
            android.widget.Toast.makeText(this, "Sign-in Error: $msg", android.widget.Toast.LENGTH_LONG).show()
        }
    }

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
        
        setupConnectivityListener()
        requestNotificationPermission()

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
                            onSignIn = { signInLauncher.launch(authManager.getSignInClient().signInIntent) },
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

    private fun setupConnectivityListener() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (settingsManager.isSyncPending()) {
                        lifecycleScope.launch {
                            val success = groupSyncManager.pushGroupData()
                            settingsManager.setSyncPending(!success)
                        }
                    }
                }
            }
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun SyncStatusOverlay() {
    val status = SyncStatusManager.status
    if (status == SyncStatus.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(16.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                when (status) {
                    SyncStatus.SYNCING -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    SyncStatus.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    SyncStatus.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    settingsManager: SettingsManager, 
    shoppingListManager: ShoppingListManager,
    notesManager: JNotesManager,
    cleaningManager: JCleaningManager,
    backupManager: BackupManager,
    authManager: AuthManager,
    groupSyncManager: GroupSyncManager,
    onSignIn: () -> Unit,
    isDarkMode: Boolean,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    currentGroupId: String?,
    onGroupChanged: (String?) -> Unit,
    groups: Map<String, JGroupInfo>,
    onGroupsUpdated: (Map<String, JGroupInfo>) -> Unit,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("launcher") }
    var navigateToUpdates by remember { mutableStateOf(false) }
    var navigateToGroups by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf<Int?>(null) }
    var hasShownUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isAdmin by groupSyncManager.isAdminFlow().collectAsState(initial = false)

    // Real-time group info listener (Updates names if they change on server)
    val allGroupIds = groups.keys.toList()
    val liveGroups by remember(allGroupIds) { 
        groupSyncManager.groupInfoFlow(allGroupIds) 
    }.collectAsState(initial = groups)
    
    // Sync live updates back to parent state and local storage
    LaunchedEffect(liveGroups) {
        if (liveGroups.isNotEmpty() && liveGroups != groups) {
            onGroupsUpdated(liveGroups)
            settingsManager.saveGroups(liveGroups)
        }
    }

    if (currentScreen != "launcher" && currentScreen != "settings" && currentUser == null) {
        AlertDialog(
            onDismissRequest = { currentScreen = "launcher" },
            title = { Text(Loc.signInRequired) },
            text = { Text(Loc.signInRequiredMsg) },
            confirmButton = {
                Button(onClick = { currentScreen = "settings" }) {
                    Text(Loc.goToSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { currentScreen = "launcher" }) {
                    Text(Loc.later)
                }
            }
        )
    }

    // Version update popup
    if (updateVersion != null && !hasShownUpdateDialog) {
        AlertDialog(
            onDismissRequest = { hasShownUpdateDialog = true },
            title = { Text(Loc.updateAvailableTitle) },
            text = { Text(Loc.updateAvailableMsg.format(backupManager.getCurrentVersionCode(), updateVersion)) },
            confirmButton = {
                Button(onClick = { 
                    navigateToUpdates = true
                    currentScreen = "settings"
                    hasShownUpdateDialog = true
                }) {
                    Text(Loc.goToUpdates)
                }
            },
            dismissButton = {
                TextButton(onClick = { hasShownUpdateDialog = true }) {
                    Text(Loc.later)
                }
            }
        )
    }

    // Invite Listener & Popup
    val invites by groupSyncManager.invitesFlow().collectAsState(initial = emptyList())
    if (invites.isNotEmpty()) {
        val invite = invites.first()
        AlertDialog(
            onDismissRequest = { },
            title = { Text(Loc.invitePopupTitle) },
            text = { Text(Loc.invitePopupMsg.format(invite["groupName"] ?: "")) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val gid = invite["groupId"]!!
                        groupSyncManager.respondToInvite(invite["inviteId"]!!, gid, true)
                        onGroupChanged(gid) // Update state to trigger listener
                        currentScreen = "settings" // Take them to settings as requested
                    }
                }) { Text(Loc.accept) }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        groupSyncManager.respondToInvite(invite["inviteId"]!!, invite["groupId"]!!, false)
                    }
                }) { Text(Loc.decline) }
            }
        )
    }

    // Global sync logic
    var isInitialized by remember { mutableStateOf(false) }
    var lastProcessedGroupId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            android.util.Log.d("jTool", "User detected: ${currentUser.email}. Starting initial sync.")
            
            // 1. Clean local data if it's a fresh start (no active group)
            if (settingsManager.getCurrentGroupId() == null) {
                context.getSharedPreferences("jtool_shopping", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("jtool_notes", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("jtool_links", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("jtool_cleaning", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
            }

            // 2. Fetch all settings and memberships from server
            groupSyncManager.fetchUserSettings()
            
            // 3. Update the UI state with whatever we found
            val freshGroups = settingsManager.getGroups()
            onGroupsUpdated(freshGroups)
            
            val savedGroupId = settingsManager.getCurrentGroupId()
            if (savedGroupId != null) {
                onGroupChanged(savedGroupId)
                groupSyncManager.pullGroupData()
                lastProcessedGroupId = savedGroupId
            } else {
                navigateToGroups = true
                currentScreen = "settings"
            }
            
            isInitialized = true
        }
    }

    // Secondary effect: watch for group changes (switching)
    LaunchedEffect(currentGroupId) {
        if (currentUser != null && isInitialized && currentGroupId != lastProcessedGroupId) {
            android.util.Log.d("jTool", "Group switch detected: $currentGroupId")
            
            // Clear cache for the old group
            context.getSharedPreferences("jtool_shopping", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("jtool_notes", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("jtool_links", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("jtool_cleaning", ComponentActivity.MODE_PRIVATE).edit().clear().apply()
            
            groupSyncManager.pullGroupData()
            lastProcessedGroupId = currentGroupId
        }
    }

    // Real-time group data listener
    LaunchedEffect(currentGroupId) {
        if (currentGroupId != null) {
            groupSyncManager.groupDataFlow(currentGroupId).collect { }
        }
    }

    // App Updates check
    LaunchedEffect(Unit) {
        while(true) {
            val appInfo = groupSyncManager.getLatestAppInfo()
            val latestVersion = when (val v = appInfo?.get("versionCode")) {
                is Long -> v.toInt()
                is Int -> v
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: 0
                else -> 0
            }
            if (latestVersion > backupManager.getCurrentVersionCode()) {
                updateVersion = latestVersion
                settingsManager.setUpdateUrl(appInfo?.get("downloadUrl") as? String)
            }
            kotlinx.coroutines.delay(3600000)
        }
    }

    if (currentScreen != "launcher") {
        BackHandler {
            currentScreen = "launcher"
        }
    }

    when (currentScreen) {
        "launcher" -> {
            navigateToUpdates = false
            navigateToGroups = false
            LauncherScreen(
                onWorkTimeClick = { currentScreen = "WorkTime" },
                onTimeCalcClick = { currentScreen = "TimeCalc" },
                onShopListClick = { currentScreen = "ShopList" },
                onNotesClick = { currentScreen = "Notes" },
                onLinksClick = { currentScreen = "Links" },
                onCleaningClick = { currentScreen = "Cleaning" },
                onSettingsClick = { 
                    navigateToUpdates = false
                    navigateToGroups = false
                    currentScreen = "settings" 
                },
                onAccountClick = {
                    navigateToUpdates = false
                    navigateToGroups = false
                    // Start SettingsScreen but directly on the Account sub-page
                    // I will add startOnAccount parameter to SettingsScreen
                    currentScreen = "settings_account"
                },
                showBadge = updateVersion != null || (currentUser != null && currentGroupId == null),
                settingsManager = settingsManager,
                currentGroupId = currentGroupId,
                groups = groups, // Pass the groups map
                onJoinGroupClick = { 
                    navigateToGroups = true
                    currentScreen = "settings" 
                },
                isAdmin = isAdmin,
                onAdminClick = { currentScreen = "AdminPanel" },
                currentUser = currentUser
            )
        }
        "settings_account" -> SettingsScreen(
            backupManager = backupManager,
            settingsManager = settingsManager,
            authManager = authManager,
            groupSyncManager = groupSyncManager,
            onSignIn = onSignIn,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            currentLang = currentLang,
            onLangToggle = onLangToggle,
            hasUpdate = updateVersion != null,
            startOnUpdates = false,
            startOnGroups = false,
            startOnAccount = true,
            onGroupChanged = onGroupChanged,
            groups = groups,
            onGroupsUpdated = onGroupsUpdated,
            currentGroupId = currentGroupId,
            onBack = { currentScreen = "launcher" }
        )
        "AdminPanel" -> AdminPanelScreen(
            groupSyncManager = groupSyncManager,
            settingsManager = settingsManager,
            onBack = { currentScreen = "launcher" }
        )
        "WorkTime" -> WorkdayCalculatorScreen(
            settingsManager = settingsManager,
            groupSyncManager = groupSyncManager
        )
        "TimeCalc" -> TimeCalculatorScreen()
        "ShopList" -> ShoppingListScreen(
            manager = shoppingListManager,
            backupManager = backupManager,
            settingsManager = settingsManager,
            groupSyncManager = groupSyncManager
        )
        "Notes" -> JNotesScreen(
            manager = notesManager,
            backupManager = backupManager,
            settingsManager = settingsManager,
            groupSyncManager = groupSyncManager
        )
        "Links" -> JLinksScreen(
            manager = JLinksManager(LocalContext.current),
            backupManager = backupManager,
            settingsManager = settingsManager,
            groupSyncManager = groupSyncManager
        )
        "Cleaning" -> JCleaningScreen(
            manager = cleaningManager,
            backupManager = backupManager,
            settingsManager = settingsManager,
            groupSyncManager = groupSyncManager
        )
        "settings" -> SettingsScreen(
            backupManager = backupManager,
            settingsManager = settingsManager,
            authManager = authManager,
            groupSyncManager = groupSyncManager,
            onSignIn = onSignIn,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            currentLang = currentLang,
            onLangToggle = onLangToggle,
            hasUpdate = updateVersion != null,
            startOnUpdates = navigateToUpdates,
            startOnGroups = navigateToGroups,
            onGroupChanged = onGroupChanged,
            groups = groups,
            onGroupsUpdated = onGroupsUpdated,
            currentGroupId = currentGroupId,
            onBack = { currentScreen = "launcher" }
        )
    }
}

@Composable
fun LauncherScreen(
    onWorkTimeClick: () -> Unit, 
    onTimeCalcClick: () -> Unit, 
    onShopListClick: () -> Unit,
    onNotesClick: () -> Unit,
    onLinksClick: () -> Unit,
    onCleaningClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showBadge: Boolean,
    settingsManager: SettingsManager,
    currentGroupId: String?,
    groups: Map<String, JGroupInfo>, // Added this
    onJoinGroupClick: () -> Unit,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onAccountClick: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?
) {
    val groupName = remember(currentGroupId, groups) {
        currentGroupId?.let { groups[it]?.name ?: it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        if (currentUser != null) {
            Text(
                text = "${Loc.hello}, ${settingsManager.getUserName()}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (groupName != null) {
                Text(
                    text = Loc.activeGroupLabel.format(groupName),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                TextButton(onClick = onJoinGroupClick) {
                    Text(
                        text = Loc.noActiveGroup,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        } else {
            Text(
                text = "${Loc.hello}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Loc.notSignedIn, fontSize = 14.sp)
                TextButton(onClick = onAccountClick) {
                    Text(
                        text = Loc.pleaseSignInLink,
                        fontSize = 14.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (settingsManager.isGridLayout()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentUser != null) {
                    if (settingsManager.isAppVisible("WorkTime")) {
                        item {
                            AppIcon(
                                name = Loc.workTime,
                                icon = Icons.Default.Schedule,
                                onClick = onWorkTimeClick
                            )
                        }
                    }
                    if (settingsManager.isAppVisible("TimeCalc")) {
                        item {
                            AppIcon(
                                name = Loc.timeCalc,
                                icon = Icons.Default.Calculate,
                                onClick = onTimeCalcClick
                            )
                        }
                    }
                    if (settingsManager.isAppVisible("ShopList")) {
                        item {
                            AppIcon(
                                name = Loc.shoppingList,
                                icon = Icons.Default.ShoppingCart,
                                onClick = onShopListClick
                            )
                        }
                    }
                    if (settingsManager.isAppVisible("Notes")) {
                        item {
                            AppIcon(
                                name = Loc.notes,
                                icon = Icons.Default.Description,
                                onClick = onNotesClick
                            )
                        }
                    }
                    if (settingsManager.isAppVisible("Links")) {
                        item {
                            AppIcon(
                                name = Loc.links,
                                icon = Icons.Default.Link,
                                onClick = onLinksClick
                            )
                        }
                    }
                    if (settingsManager.isAppVisible("Cleaning")) {
                        item {
                            AppIcon(
                                name = Loc.cleaning,
                                icon = Icons.Default.Brush,
                                onClick = onCleaningClick
                            )
                        }
                    }
                    if (isAdmin) {
                        item {
                            AppIcon(
                                name = Loc.adminPanel,
                                icon = Icons.Default.AdminPanelSettings,
                                onClick = onAdminClick
                            )
                        }
                    }
                }
                
                item {
                    AppIcon(
                        name = Loc.settings,
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick,
                        hasBadge = showBadge
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentUser != null) {
                    if (settingsManager.isAppVisible("WorkTime")) {
                        item {
                            AppListRow(name = Loc.workTime, icon = Icons.Default.Schedule, onClick = onWorkTimeClick)
                        }
                    }
                    if (settingsManager.isAppVisible("TimeCalc")) {
                        item {
                            AppListRow(name = Loc.timeCalc, icon = Icons.Default.Calculate, onClick = onTimeCalcClick)
                        }
                    }
                    if (settingsManager.isAppVisible("ShopList")) {
                        item {
                            AppListRow(name = Loc.shoppingList, icon = Icons.Default.ShoppingCart, onClick = onShopListClick)
                        }
                    }
                    if (settingsManager.isAppVisible("Notes")) {
                        item {
                            AppListRow(name = Loc.notes, icon = Icons.Default.Description, onClick = onNotesClick)
                        }
                    }
                    if (settingsManager.isAppVisible("Links")) {
                        item {
                            AppListRow(name = Loc.links, icon = Icons.Default.Link, onClick = onLinksClick)
                        }
                    }
                    if (settingsManager.isAppVisible("Cleaning")) {
                        item {
                            AppListRow(name = Loc.cleaning, icon = Icons.Default.Brush, onClick = onCleaningClick)
                        }
                    }
                    if (isAdmin) {
                        item {
                            AppListRow(name = Loc.adminPanel, icon = Icons.Default.AdminPanelSettings, onClick = onAdminClick)
                        }
                    }
                }

                item {
                    AppListRow(name = Loc.settings, icon = Icons.Default.Settings, onClick = onSettingsClick, hasBadge = showBadge)
                }
            }
        }
    }
}

@Composable
fun AppListRow(name: String, icon: ImageVector, onClick: () -> Unit, hasBadge: Boolean = false) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgedBox(
                badge = {
                    if (hasBadge) {
                        Badge(containerColor = MaterialTheme.colorScheme.error)
                    }
                }
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun AppIcon(name: String, icon: ImageVector, onClick: () -> Unit, hasBadge: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        BadgedBox(
            badge = {
                if (hasBadge) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.offset(x = (-8).dp, y = 8.dp)
                    )
                }
            }
        ) {
            Card(
                onClick = onClick,
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LauncherPreview() {
    val context = LocalContext.current
    JToolTheme {
        LauncherScreen(
            onWorkTimeClick = {}, 
            onTimeCalcClick = {}, 
            onShopListClick = {}, 
            onNotesClick = {},
            onLinksClick = {},
            onCleaningClick = {},
            onSettingsClick = {}, 
            showBadge = false,
            settingsManager = SettingsManager(context),
            currentGroupId = "Preview ID",
            groups = emptyMap(),
            onJoinGroupClick = {},
            isAdmin = false,
            onAdminClick = {},
            onAccountClick = {},
            currentUser = null
        )
    }
}
