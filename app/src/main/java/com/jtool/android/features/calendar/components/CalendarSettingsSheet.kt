package com.jtool.android.features.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.ui.components.common.GenericSheet
import androidx.compose.ui.platform.LocalContext

@Composable
fun CalendarSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    
    var syncEnabled by remember { mutableStateOf(settings.isGoogleCalendarSyncEnabled()) }
    var pullEnabled by remember { mutableStateOf(settings.isGoogleCalendarPullEnabled()) }
    var pushEnabled by remember { mutableStateOf(settings.isGoogleCalendarPushEnabled()) }
    var keywords by remember { mutableStateOf(settings.getAutoWorkKeywords()) }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Launcher.settings,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Google Calendar Sync", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Sync", style = MaterialTheme.typography.bodyLarge)
                    Text("Mirror app events to Google", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = { 
                        syncEnabled = it
                        settings.setGoogleCalendarSyncEnabled(it)
                    }
                )
            }

            if (syncEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pull Events", style = MaterialTheme.typography.bodyLarge)
                    Checkbox(
                        checked = pullEnabled,
                        onCheckedChange = { 
                            pullEnabled = it
                            settings.setGoogleCalendarPullEnabled(it)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Push Events", style = MaterialTheme.typography.bodyLarge)
                    Checkbox(
                        checked = pushEnabled,
                        onCheckedChange = { 
                            pushEnabled = it
                            settings.setGoogleCalendarPushEnabled(it)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(text = "Smart Work Detection", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Events containing these keywords will be recognized as shifts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            OutlinedTextField(
                value = keywords,
                onValueChange = { 
                    keywords = it
                    settings.setAutoWorkKeywords(it)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Code words (e.g. shift, work)") },
                placeholder = { Text("shift,work") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Full Sync mirrors every local action. Partial sync allows only pulling or pushing individual items.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* TODO: Manual Full Pull Logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Manual Full Pull")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
