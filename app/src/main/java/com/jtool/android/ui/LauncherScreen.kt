package com.jtool.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.ui.navigation.AppNames
import com.jtool.android.ui.navigation.AppProperties

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
    groups: Map<String, GroupInfo>,
    onJoinGroupClick: () -> Unit,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onAccountClick: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?
) {
    val groupName = remember(currentGroupId, groups) {
        currentGroupId?.let { groups[it]?.name ?: it }
    }

    val clickHandlers = mapOf(
        AppNames.WORK_TIME to onWorkTimeClick,
        AppNames.TIME_CALC to onTimeCalcClick,
        AppNames.SHOP_LIST to onShopListClick,
        AppNames.NOTES to onNotesClick,
        AppNames.LINKS to onLinksClick,
        AppNames.CLEANING to onCleaningClick
    )

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
                        textDecoration = TextDecoration.Underline
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
                        textDecoration = TextDecoration.Underline
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
                    AppProperties.allApps.forEach { app ->
                        if (settingsManager.isAppVisible(app.id)) {
                            item {
                                AppIcon(
                                    name = app.getName(),
                                    icon = app.icon!!,
                                    onClick = clickHandlers[app.id] ?: {}
                                )
                            }
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

                if (currentUser != null && isAdmin) {
                    item {
                        AppIcon(
                            name = Loc.adminPanel,
                            icon = Icons.Default.AdminPanelSettings,
                            onClick = onAdminClick
                        )
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentUser != null) {
                    AppProperties.allApps.forEach { app ->
                        if (settingsManager.isAppVisible(app.id)) {
                            item {
                                AppListRow(
                                    name = app.getName(),
                                    icon = app.icon!!,
                                    onClick = clickHandlers[app.id] ?: {}
                                )
                            }
                        }
                    }
                }

                item {
                    AppListRow(name = Loc.settings, icon = Icons.Default.Settings, onClick = onSettingsClick, hasBadge = showBadge)
                }

                if (currentUser != null && isAdmin) {
                    item {
                        AppListRow(name = Loc.adminPanel, icon = Icons.Default.AdminPanelSettings, onClick = onAdminClick)
                    }
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
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
