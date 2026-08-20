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
        Text(Loc.settings_cloudSyncFirebase, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
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
                    Text(Loc.settings_logout)
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
                Text(Loc.settings_signInWithGoogle)
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
            Text(Loc.settings_userSettings, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    label = { Text(Loc.settings_displayName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { 
                        viewModel.onEvent(AccountEvent.UpdateUserName(userNameInput))
                        android.widget.Toast.makeText(context, Loc.settings_nameSaved, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = userNameInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = Loc.general_save)
                }
            }
            
            Spacer(Modifier.height(48.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = Loc.settings_dangerZone, 
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
                Text(Loc.settings_deleteAccountData)
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
            title = { Text(Loc.settings_confirmLogoutTitle) },
            text = { Text(Loc.settings_confirmLogoutMsg) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.onEvent(AccountEvent.SignOut)
                    onSignOut()
                }) {
                    Text(Loc.settings_logout, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    // Deletion Flow: Step 1 (Email verification)
    if (showDeleteStep1) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1 = false },
            title = { Text(Loc.settings_confirmEmailTitle) },
            text = {
                Column {
                    Text(Loc.settings_confirmEmailMsg.format(uiState.currentUserEmail))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = emailConfirmationInput,
                        onValueChange = { emailConfirmationInput = it },
                        label = { Text(Loc.settings_email) },
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
                    Text(Loc.settings_verify)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1 = false }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    // Deletion Flow: Step 2 (Final Warning)
    if (showDeleteStep2) {
        AlertDialog(
            onDismissRequest = { showDeleteStep2 = false },
            title = { Text(Loc.general_areYouSure) },
            text = { Text(Loc.settings_deleteAccountPermanentMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStep2 = false
                        viewModel.onEvent(AccountEvent.DeleteAccount)
                        onSignOut() // Trigger UI transition
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep2 = false }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }
}
