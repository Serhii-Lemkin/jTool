package com.jtool.android.features.settings.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.account.components.logout_confirm_sheet.LogoutConfirmSheet
import com.jtool.android.features.settings.account.components.delete_account_confirm_sheet.DeleteAccountConfirmSheet
import com.jtool.android.features.settings.account.components.edit_name_sheet.EditNameSheet

@Composable
fun AccountScreen(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AccountViewModel(authManager, settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    
    var showDeleteStep1 by remember { mutableStateOf(false) }
    var emailConfirmationInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Loc.Settings.cloudSyncFirebase, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
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
                    Text(Loc.Settings.logout)
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
                Text(Loc.Settings.signInWithGoogle)
            }
        }

        if (uiState.currentUserEmail != null) {
            var showNameEdit by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(Loc.Settings.userSettings, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            
            ActionListItem(
                headline = Loc.Settings.displayName,
                supportingText = uiState.userName.ifEmpty { "Not set" },
                leadingContent = { Icon(Icons.Default.Person, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                onClick = { showNameEdit = true }
            )

            if (showNameEdit) {
                EditNameSheet(
                    initialName = uiState.userName,
                    onDismiss = { showNameEdit = false },
                    onConfirm = { newName ->
                        viewModel.onEvent(AccountEvent.UpdateUserName(newName))
                        showNameEdit = false
                    }
                )
            }
            
            Spacer(Modifier.height(48.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = Loc.Settings.dangerZone, 
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
                Text(Loc.Settings.deleteAccountData)
            }
        }
        
        if (uiState.isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmSheet(
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                viewModel.onEvent(AccountEvent.SignOut)
                onSignOut()
            }
        )
    }

    if (showDeleteStep1) {
        DeleteAccountConfirmSheet(
            currentUserEmail = uiState.currentUserEmail ?: "",
            onDismiss = { showDeleteStep1 = false },
            onConfirm = {
                showDeleteStep1 = false
                viewModel.onEvent(AccountEvent.DeleteAccount)
                onSignOut()
            }
        )
    }
}
