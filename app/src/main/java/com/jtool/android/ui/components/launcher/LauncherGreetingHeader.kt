package com.jtool.android.ui.components.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import com.google.firebase.auth.FirebaseUser
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo

@Composable
fun LauncherGreetingHeader(
    currentUser: FirebaseUser?,
    userName: String,
    groupName: String?,
    groups: Map<String, GroupInfo>,
    onGroupClick: () -> Unit,
    onAccountClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (currentUser != null) {
            Text(
                text = if (userName.isNotBlank()) "${Loc.Launcher.hello}, $userName!" else "${Loc.Launcher.hello}!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.align(AbsoluteAlignment.CenterRight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = Loc.Launcher.searchApps,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onGroupClick,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = groupName ?: Loc.Settings.noGroupsMsg,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (groups.size > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
        } else {
            Text(
                text = "${Loc.Launcher.hello}!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.align(AbsoluteAlignment.CenterRight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = Loc.Launcher.searchApps,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Card(
                    onClick = onAccountClick,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(Loc.Settings.notSignedIn, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Loc.Settings.pleaseSignInLink,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}
