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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.jtool.ui.theme.JToolTheme

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var shoppingListManager: ShoppingListManager
    private lateinit var notesManager: JNotesManager
    private lateinit var cleaningManager: JCleaningManager
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        shoppingListManager = ShoppingListManager(this)
        notesManager = JNotesManager(this)
        cleaningManager = JCleaningManager(this)
        backupManager = BackupManager(this)
        
        setupConnectivityListener()
        requestNotificationPermission()

        setContent {
            var isDark by remember { mutableStateOf(settingsManager.isDarkMode()) }
            var currentLang by remember { mutableStateOf(settingsManager.getLanguage()) }
            
            Loc.lang = currentLang

            JToolTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        settingsManager = settingsManager,
                        shoppingListManager = shoppingListManager,
                        notesManager = notesManager,
                        cleaningManager = cleaningManager,
                        backupManager = backupManager,
                        isDarkMode = isDark,
                        onThemeToggle = {
                            isDark = !isDark
                            settingsManager.setDarkMode(isDark)
                        },
                        currentLang = currentLang,
                        onLangToggle = {
                            currentLang = if (currentLang == "en") "uk" else "en"
                            settingsManager.setLanguage(currentLang)
                            Loc.lang = currentLang
                        }
                    )
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
                        val uri = settingsManager.getSyncUri()
                        if (uri != null) {
                            val success = backupManager.syncPush(uri)
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
fun AppNavigation(
    settingsManager: SettingsManager, 
    shoppingListManager: ShoppingListManager,
    notesManager: JNotesManager,
    cleaningManager: JCleaningManager,
    backupManager: BackupManager,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("launcher") }
    var updateVersion by remember { mutableStateOf<Int?>(null) }
    var hasShownUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Global sync logic
    LaunchedEffect(currentScreen) {
        val uri = settingsManager.getSyncUri()
        if (uri != null) {
            // Pull data
            scope.launch {
                backupManager.syncPull(uri)
            }
            // Check for updates independently
            scope.launch {
                updateVersion = backupManager.checkForUpdate(uri)
            }
            // Check if there's a pending push from an offline session
            if (settingsManager.isSyncPending()) {
                scope.launch {
                    val success = backupManager.syncPush(uri)
                    settingsManager.setSyncPending(!success)
                }
            }
        }
    }

    if (updateVersion != null && !hasShownUpdateDialog) {
        AlertDialog(
            onDismissRequest = { hasShownUpdateDialog = true },
            title = { Text(Loc.updateAvailableTitle) },
            text = { Text(Loc.updateAvailableMsg.format(backupManager.getCurrentVersionCode(), updateVersion)) },
            confirmButton = {
                Button(onClick = { 
                    backupManager.downloadAndInstallUpdate(settingsManager.getSyncUri())
                    hasShownUpdateDialog = true
                }) {
                    Text(Loc.install)
                }
            },
            dismissButton = {
                TextButton(onClick = { hasShownUpdateDialog = true }) {
                    Text(Loc.later)
                }
            }
        )
    }

    if (currentScreen != "launcher") {
        BackHandler {
            currentScreen = "launcher"
        }
    }

    when (currentScreen) {
        "launcher" -> LauncherScreen(
            onWorkTimeClick = { currentScreen = "WorkTime" },
            onTimeCalcClick = { currentScreen = "TimeCalc" },
            onShopListClick = { currentScreen = "ShopList" },
            onNotesClick = { currentScreen = "Notes" },
            onLinksClick = { currentScreen = "Links" },
            onCleaningClick = { currentScreen = "Cleaning" },
            onSettingsClick = { currentScreen = "settings" },
            showBadge = updateVersion != null || settingsManager.getSyncUri().isNullOrEmpty(),
            settingsManager = settingsManager
        )
        "WorkTime" -> WorkdayCalculatorScreen(
            settingsManager = settingsManager
        )
        "TimeCalc" -> TimeCalculatorScreen()
        "ShopList" -> ShoppingListScreen(
            manager = shoppingListManager,
            backupManager = backupManager,
            settingsManager = settingsManager
        )
        "Notes" -> JNotesScreen(
            manager = notesManager,
            backupManager = backupManager,
            settingsManager = settingsManager
        )
        "Links" -> JLinksScreen(
            manager = JLinksManager(LocalContext.current),
            backupManager = backupManager,
            settingsManager = settingsManager
        )
        "Cleaning" -> JCleaningScreen(
            manager = cleaningManager,
            backupManager = backupManager,
            settingsManager = settingsManager
        )
        "settings" -> SettingsScreen(
            backupManager = backupManager,
            settingsManager = settingsManager,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            currentLang = currentLang,
            onLangToggle = onLangToggle
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
    settingsManager: SettingsManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "${Loc.hello}, ${settingsManager.getUserName()}!",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (settingsManager.isAppVisible("WorkTime")) {
                item {
                    AppIcon(
                        name = Loc.t("WorkTime", "Робота"),
                        icon = Icons.Default.Schedule,
                        onClick = onWorkTimeClick
                    )
                }
            }
            if (settingsManager.isAppVisible("TimeCalc")) {
                item {
                    AppIcon(
                        name = Loc.t("TimeCalc", "Час"),
                        icon = Icons.Default.Calculate,
                        onClick = onTimeCalcClick
                    )
                }
            }
            if (settingsManager.isAppVisible("ShopList")) {
                item {
                    AppIcon(
                        name = Loc.t("ShopList", "Покупки"),
                        icon = Icons.Default.ShoppingCart,
                        onClick = onShopListClick
                    )
                }
            }
            if (settingsManager.isAppVisible("Notes")) {
                item {
                    AppIcon(
                        name = Loc.t("Notes", "Нотатки"),
                        icon = Icons.Default.Description,
                        onClick = onNotesClick
                    )
                }
            }
            if (settingsManager.isAppVisible("Links")) {
                item {
                    AppIcon(
                        name = Loc.t("Links", "Посилання"),
                        icon = Icons.Default.Link,
                        onClick = onLinksClick
                    )
                }
            }
            if (settingsManager.isAppVisible("Cleaning")) {
                item {
                    AppIcon(
                        name = Loc.t("Cleaning", "Прибирання"),
                        icon = Icons.Default.Brush,
                        onClick = onCleaningClick
                    )
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
            settingsManager = SettingsManager(context)
        )
    }
}
