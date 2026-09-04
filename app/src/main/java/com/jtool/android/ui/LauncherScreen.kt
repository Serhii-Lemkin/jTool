package com.jtool.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.jtool.android.features.settings.groups.components.group_selection_sheet.GroupSelectionSheet

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
    onSupportClick: () -> Unit,
    settingsManager: SettingsManager,
    currentGroupId: String?,
    groups: Map<String, GroupInfo>,
    onJoinGroupClick: () -> Unit,
    onGroupSelected: (String) -> Unit,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onAccountClick: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    var showGroupSheet by remember { mutableStateOf(false) }

    val groupName = remember(currentGroupId, groups) {
        currentGroupId?.let { id ->
            val group = groups[id]
            if (group?.isPrivate == true) Loc.Settings.privateGroup else group?.name ?: id
        }
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

    if (showGroupSheet) {
        GroupSelectionSheet(
            groups = groups,
            currentGroupId = currentGroupId,
            onGroupSelected = onGroupSelected,
            onDismiss = { showGroupSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Custom Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSupportClick) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = Loc.Launcher.supportHub,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = Loc.Launcher.settings,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentUser != null && isAdmin) {
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = Loc.Launcher.adminPanel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val hasNotifications = notificationCount > 0
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (hasNotifications) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = Loc.General.notifications,
                    tint = if (hasNotifications) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Greeting & Group Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (currentUser != null) {
                Text(
                    text = "${Loc.Launcher.hello}, ${settingsManager.getUserName()}!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = { if (groups.isNotEmpty()) showGroupSheet = true else onJoinGroupClick() },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
            } else {
                Text(
                    text = "${Loc.Launcher.hello}!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    onClick = onAccountClick,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // App Grid/List
        if (settingsManager.isGridLayout()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
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
        }
    }
}

@Composable
fun AppListRow(name: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
