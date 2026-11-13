package com.jtool.android.ui.components.launcher.components.gesture_action_picker_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.user.enums.LauncherGestureEnum
import com.jtool.android.ui.components.common.GenericSheet
import com.jtool.android.ui.components.launcher.getActionIcon
import com.jtool.android.ui.navigation.AppProperties

@Composable
fun GestureActionPickerSheet(
    gesture: LauncherGestureEnum,
    currentActionId: String,
    onSelectAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val gestureLabel = when (gesture) {
        LauncherGestureEnum.SWIPE_LEFT -> Loc.Launcher.swipeLeft
        LauncherGestureEnum.SWIPE_RIGHT -> Loc.Launcher.swipeRight
        LauncherGestureEnum.DOUBLE_TAP -> Loc.Launcher.doubleTap
    }

    val availableTargets = remember {
        listOf(
            "" to Loc.Launcher.none,
            AppScreenEnum.SETTINGS.id to Loc.Launcher.settings,
            AppScreenEnum.SUPPORT_HUB.id to Loc.Launcher.supportHub
        ) + AppProperties.allApps.map { it.id.id to it.getName() }
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = "${Loc.Launcher.selectAction}: $gestureLabel"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = availableTargets,
                key = { (actionId, _) ->
                    when (actionId) {
                        "" -> "none"
                        AppScreenEnum.SETTINGS.id, AppScreenEnum.SUPPORT_HUB.id -> "screen_$actionId"
                        else -> "action_$actionId"
                    }
                }
            ) { (actionId, label) ->
                val icon = remember(actionId) { getActionIcon(actionId) }
                val isSelected = actionId == currentActionId
                Surface(
                    onClick = { onSelectAction(actionId) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
