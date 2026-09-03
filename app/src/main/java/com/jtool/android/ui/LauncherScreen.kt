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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.ui.navigation.AppProperties
import com.jtool.android.core.models.common.enums.AppScreenEnum

@Composable
fun LauncherScreen(
    onWorkTimeClick: () -> Unit, 
    onTimeCalcClick: () -> Unit, 
    onShopListClick: () -> Unit,
    onNotesClick: () -> Unit,
    onLinksClick: () -> Unit,
    onCleaningClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    settingsManager: SettingsManager,
    currentGroupId: String?,
    groups: Map<String, GroupInfo>,
    onJoinGroupClick: () -> Unit,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onAccountClick: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    val groupName = remember(currentGroupId, groups) {
        currentGroupId?.let { groups[it]?.name ?: it }
    }

    val clickHandlers = mapOf(
        AppScreenEnum.WORK_TIME to onWorkTimeClick,
        AppScreenEnum.TIME_CALC to onTimeCalcClick,
        AppScreenEnum.SHOP_LIST to onShopListClick,
        AppScreenEnum.NOTES to onNotesClick,
        AppScreenEnum.LINKS to onLinksClick,
        AppScreenEnum.CLEANING to onCleaningClick,
        AppScreenEnum.CALENDAR to onCalendarClick
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(top = 0.dp, start = 24.dp, end = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notificationCount > 0) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green 500
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = notificationCount.toString(),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = if (notificationCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (currentUser != null) {
            Text(
                text = "${Loc.launcher_hello}, ${settingsManager.getUserName()}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            if (groupName != null) {
                Text(
                    text = Loc.settings_activeGroupLabel.format(groupName),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            } else {
                TextButton(onClick = onJoinGroupClick) {
                    Text(
                        text = Loc.settings_noActiveGroup,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "${Loc.launcher_hello}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Loc.settings_notSignedIn, fontSize = 14.sp, textAlign = TextAlign.Center)
                TextButton(onClick = onAccountClick) {
                    Text(
                        text = Loc.settings_pleaseSignInLink,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
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
                        name = Loc.launcher_settings,
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick
                    )
                }

                if (currentUser != null && isAdmin) {
                    item {
                        AppIcon(
                            name = Loc.admin_adminPanel,
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
                    AppListRow(name = Loc.launcher_settings, icon = Icons.Default.Settings, onClick = onSettingsClick)
                }

                if (currentUser != null && isAdmin) {
                    item {
                        AppListRow(name = Loc.admin_adminPanel, icon = Icons.Default.AdminPanelSettings, onClick = onAdminClick)
                    }
                }
            }
        }
    }
}

@Composable
fun AppListRow(name: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                fontSize = if (name.length > 12) 14.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Icon(
                imageVector = Icons.Default.ChevronRight, 
                contentDescription = null, 
                modifier = Modifier.scale(if (isRtl) -1f else 1f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AppIcon(name: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
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
        Text(
            text = name,
            fontSize = if (name.length > 10) 10.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
