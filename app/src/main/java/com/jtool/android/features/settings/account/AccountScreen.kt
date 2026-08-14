package com.jtool.android.features.settings.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager

@Composable
fun AccountScreen(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AccountViewModel(authManager, settingsManager, groupSyncManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    
    var showDeleteStep1 by remember { mutableStateOf(false) }
    var showDeleteStep2 by remember { mutableStateOf(false) }
    var emailConfirmationInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Firebase Auth Section
        Text(Loc.cloudSyncFirebase, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(12.dp))
        
        if (uiState.currentUserEmail != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiState.userName, style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.currentUserEmail ?: "", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.logout)
                }
            }
        } else {
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.signInWithGoogle)
            }
        }

        if (uiState.currentUserEmail != null) {
            var userNameInput by remember { mutableStateOf(uiState.userName) }
            
            // Sync input with state if it changes externally
            LaunchedEffect(uiState.userName) {
                userNameInput = uiState.userName
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            // User Settings Section
            Text(Loc.userSettings, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    label = { Text(Loc.displayName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { 
                        viewModel.onEvent(AccountEvent.UpdateUserName(userNameInput))
                        android.widget.Toast.makeText(context, Loc.t("Name saved!", "Ім'я збережено!"), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = userNameInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = Loc.save)
                }
            }
            
            Spacer(Modifier.height(48.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = Loc.t("Danger Zone", "Небезпечна зона"), 
                style = MaterialTheme.typography.titleSmall, 
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    emailConfirmationInput = ""
                    showDeleteStep1 = true 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.t("Delete Account & Data", "Видалити акаунт та дані"))
            }
        }
        
        if (uiState.isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(Loc.confirmLogoutTitle) },
            text = { Text(Loc.confirmLogoutMsg) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.onEvent(AccountEvent.SignOut)
                    onSignOut()
                }) {
                    Text(Loc.logout, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    // Deletion Flow: Step 1 (Email verification)
    if (showDeleteStep1) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1 = false },
            title = { Text(Loc.t("Confirm Email", "Підтвердіть Email")) },
            text = {
                Column {
                    Text(Loc.t("To proceed, please enter your email address: ${uiState.currentUserEmail}", 
                               "Щоб продовжити, введіть вашу адресу електронної пошти: ${uiState.currentUserEmail}"))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = emailConfirmationInput,
                        onValueChange = { emailConfirmationInput = it },
                        label = { Text(Loc.t("Email", "Електронна пошта")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStep1 = false
                        showDeleteStep2 = true
                    },
                    enabled = emailConfirmationInput.equals(uiState.currentUserEmail, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.t("Verify", "Підтвердити"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1 = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    // Deletion Flow: Step 2 (Final Warning)
    if (showDeleteStep2) {
        AlertDialog(
            onDismissRequest = { showDeleteStep2 = false },
            title = { Text(Loc.areYouSure) },
            text = { Text(Loc.t("THIS ACTION IS PERMANENT. All your personal settings, group memberships, and public profile will be deleted forever. Do you wish to proceed?", 
                               "ЦЯ ДІЯ Є ОСТАТОЧНОЮ. Всі ваші особисті налаштування, членство в групах та публічний профіль будуть видалені назавжди. Бажаєте продовжити?")) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStep2 = false
                        viewModel.onEvent(AccountEvent.DeleteAccount)
                        onSignOut() // Trigger UI transition
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep2 = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }
}
