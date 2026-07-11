package com.example.jtool.settingsSections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountScreen(
    settingsManager: SettingsManager,
    currentUser: FirebaseUser?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var userName by remember(currentUser) { mutableStateOf(settingsManager.getUserName()) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
        
        if (currentUser != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentUser.displayName ?: "User", style = MaterialTheme.typography.bodyLarge)
                    Text(currentUser.email ?: "", style = MaterialTheme.typography.bodySmall)
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

        if (currentUser != null) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            // User Settings Section
            Text(Loc.userSettings, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text(Loc.displayName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { settingsManager.saveUserName(userName) },
                    enabled = userName.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = Loc.save)
                }
            }
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
}
