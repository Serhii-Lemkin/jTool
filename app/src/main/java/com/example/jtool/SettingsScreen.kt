package com.example.jtool

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf("main") }
    var linkedUriString by remember { mutableStateOf(settingsManager.getSyncUri()) }
    var availableUpdateVersion by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(linkedUriString) {
        availableUpdateVersion = backupManager.checkForUpdate(linkedUriString)
    }

    if (currentSubScreen != "main") {
        BackHandler { currentSubScreen = "main" }
    }

    when (currentSubScreen) {
        "main" -> SettingsMainScreen(
            onNavigate = { currentSubScreen = it },
            hasUpdate = availableUpdateVersion != null,
            isSyncMissing = linkedUriString.isNullOrEmpty(),
            settingsManager = settingsManager,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            currentLang = currentLang,
            onLangToggle = onLangToggle
        )
        "sync" -> ShoplistSyncScreen(
            backupManager = backupManager,
            settingsManager = settingsManager,
            linkedUri = linkedUriString,
            onUriChanged = { linkedUriString = it },
            onBack = { currentSubScreen = "main" }
        )
        "app_updates" -> AppUpdatesScreen(
            backupManager = backupManager,
            linkedUri = linkedUriString,
            onBack = { currentSubScreen = "main" }
        )
        "visibility" -> AppVisibilityScreen(
            settingsManager = settingsManager,
            onBack = { currentSubScreen = "main" }
        )
    }
}

@Composable
fun SettingsMainScreen(
    onNavigate: (String) -> Unit,
    hasUpdate: Boolean,
    isSyncMissing: Boolean,
    settingsManager: SettingsManager,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit
) {
    var userName by remember { mutableStateOf(settingsManager.getUserName()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Loc.settings,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // User Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Loc.userSettings, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { 
                        userName = it
                        settingsManager.saveUserName(it)
                    },
                    label = { Text(Loc.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance & Language Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Loc.t("Appearance & Language", "Вигляд та Мова"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(Loc.t("Dark Mode", "Темний режим"), fontSize = 16.sp)
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(Loc.t("Language", "Мова"), fontSize = 16.sp)
                    }
                    TextButton(onClick = onLangToggle) {
                        Text(if (currentLang == "en") "English" else "Українська", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                SettingsMenuItem(
                    title = Loc.syncTitle,
                    subtitle = Loc.syncSubtitle,
                    icon = Icons.Default.CloudSync,
                    onClick = { onNavigate("sync") },
                    hasBadge = isSyncMissing
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                SettingsMenuItem(
                    title = Loc.appUpdates,
                    subtitle = Loc.appUpdatesSubtitle,
                    icon = Icons.Default.SystemUpdate,
                    onClick = { onNavigate("app_updates") },
                    hasBadge = hasUpdate
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                SettingsMenuItem(
                    title = Loc.visibilityTitle,
                    subtitle = Loc.visibilitySubtitle,
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate("visibility") }
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    onClick: () -> Unit,
    hasBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (hasBadge) {
                    Badge(containerColor = MaterialTheme.colorScheme.error)
                }
            }
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun ShoplistSyncScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    linkedUri: String?,
    onUriChanged: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, flags)
                    settingsManager.saveSyncUri(it.toString())
                    onUriChanged(it.toString())
                    scope.launch { snackbarHostState.showSnackbar("Folder linked!") }
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Permission denied") }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.use { s -> BufferedReader(InputStreamReader(s)).readText() }
            if (content != null && backupManager.restoreFromJson(content)) scope.launch { snackbarHostState.showSnackbar("Restored!") }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val json = backupManager.createBackupJson()
            context.contentResolver.openOutputStream(it)?.use { s -> s.write(json.toByteArray()) }
            scope.launch { snackbarHostState.showSnackbar("Exported!") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text(Loc.back) }
            }

            Text(Loc.syncTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.cloudFolder, fontWeight = FontWeight.Bold)
                    Text(Loc.selectFolderDesc, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (linkedUri.isNullOrEmpty()) {
                        Button(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.linkSyncFolder)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { 
                                scope.launch { if (backupManager.syncPush(linkedUri)) snackbarHostState.showSnackbar(Loc.t("Pushed!", "Відправлено!")) }
                            }, modifier = Modifier.weight(1f)) { Text(Loc.pushData) }
                            Button(onClick = { 
                                scope.launch { if (backupManager.syncPull(linkedUri)) snackbarHostState.showSnackbar(Loc.t("Pulled!", "Отримано!")) }
                            }, modifier = Modifier.weight(1f)) { Text(Loc.pullData) }
                        }
                        TextButton(onClick = { settingsManager.saveSyncUri(null); onUriChanged(null) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(Loc.unlinkFolder, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.manualBackup, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("jtool_backup.json") }, modifier = Modifier.weight(1f)) { Text(Loc.export) }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) { Text(Loc.import) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdatesScreen(
    backupManager: BackupManager,
    linkedUri: String?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentVersion by remember { mutableStateOf(backupManager.getCurrentVersionCode()) }
    var availableUpdateVersion by remember { mutableStateOf<Int?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    LaunchedEffect(linkedUri) {
        availableUpdateVersion = backupManager.getAvailableVersion(linkedUri)
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text(Loc.back) }
        }

        Text(Loc.appUpdates, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        if (linkedUri.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
                Text(
                    Loc.linkFolderToUpdate,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.versionInfo, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    
                    val statusText = if (availableUpdateVersion != null) {
                        if (availableUpdateVersion!! > currentVersion) {
                            Loc.updateAvailableMsg.format(currentVersion, availableUpdateVersion)
                        } else {
                            Loc.upToDateMsg.format(currentVersion)
                        }
                    } else {
                        Loc.t("I'm on version $currentVersion. Checking for updates...", "У мене версія $currentVersion. Перевірка оновлень...")
                    }

                    Text(
                        text = statusText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (availableUpdateVersion != null && availableUpdateVersion!! > currentVersion) {
                        Button(onClick = { backupManager.downloadAndInstallUpdate(linkedUri) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SystemUpdate, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.updateNow)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { 
                                isChecking = true
                                scope.launch {
                                    availableUpdateVersion = backupManager.getAvailableVersion(linkedUri)
                                    isChecking = false
                                }
                            },
                            enabled = !isChecking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isChecking) Loc.t("Checking...", "Перевірка...") else Loc.checkForUpdates)
                        }
                        
                        if (availableUpdateVersion != null && availableUpdateVersion!! <= currentVersion) {
                            Text(
                                Loc.t("You are on the latest version!", "У вас остання версія!"),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppVisibilityScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val apps = remember {
        listOf(
            "WorkTime" to "WorkTime",
            "TimeCalc" to "TimeCalc",
            "ShopList" to "ShopList",
            "Notes" to "Notes",
            "Links" to "Links",
            "Cleaning" to "Cleaning"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text(Loc.back) }
        }

        Text(Loc.visibilityTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                apps.forEach { (id, name) ->
                    var isVisible by remember { mutableStateOf(settingsManager.isAppVisible(id)) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = name, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isVisible,
                            onCheckedChange = {
                                isVisible = it
                                settingsManager.setAppVisible(id, it)
                            }
                        )
                    }
                    if (id != apps.last().first) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}
