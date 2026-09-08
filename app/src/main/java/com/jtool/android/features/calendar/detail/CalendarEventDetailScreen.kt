package com.jtool.android.features.calendar.detail

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.components.delete_event_confirm_sheet.DeleteEventConfirmSheet
import com.jtool.android.core.components.comments.CommentSection
import com.jtool.android.core.components.comments.CommentEvent
import com.jtool.android.features.calendar.detail.components.event_action_sheet.EventActionSheet
import com.jtool.android.features.calendar.detail.components.group_sharing_sheet.GroupSharingSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import com.jtool.android.ui.components.sheets.CalendarWarningSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventDetailScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarEventDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val event = uiState.event
    var showDeleteSheet by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showSharingSheet by remember { mutableStateOf(false) }
    var showCalendarWarning by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    
    val currentUserId = viewModel.settingsManager.getUserId()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.syncWithGoogle(context)
        } else {
            infoMessage = "Calendar permissions are required for sync" to MessageType.ERROR
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showDeleteSheet) {
        DeleteEventConfirmSheet(
            isSynced = event?.googleEventId != null,
            onDismiss = { showDeleteSheet = false },
            onConfirm = {
                viewModel.deleteEvent()
                onBack()
            }
        )
    }
    
    if (uiState.showEditDialog && event != null) {
        SideEffect {
            viewModel.toggleEditDialog(false)
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
        }
    }

    if (showActionSheet && event != null) {
        val isOwner = event.ownerUid == currentUserId
        
        EventActionSheet(
            event = event,
            canEdit = isOwner,
            onEdit = { viewModel.toggleEditDialog(true) },
            onDelete = { showDeleteSheet = true },
            onSyncToggle = {
                val calendarId = viewModel.settingsManager.getGoogleCalendarId()
                if (calendarId.isEmpty() || calendarId == "primary") {
                    showCalendarWarning = true
                } else if (event.googleEventId != null) {
                    viewModel.removeFromGoogle(context)
                } else {
                    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.syncWithGoogle(context)
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    }
                }
            },
            onShareToggle = { showSharingSheet = true },
            onDismiss = { showActionSheet = false }
        )
    }

    if (showSharingSheet && event != null) {
        val groups = viewModel.settingsManager.getGroups()
        
        GroupSharingSheet(
            groups = groups,
            sharedGroupIds = event.sharedGroupIds,
            onToggleGroup = { gid ->
                val currentGroups = event.sharedGroupIds.toMutableList()
                if (currentGroups.contains(gid)) {
                    currentGroups.remove(gid)
                } else {
                    currentGroups.add(gid)
                }
                viewModel.updateEvent(context, event, currentGroups)
            },
            onDismiss = { showSharingSheet = false }
        )
    }

    if (showCalendarWarning) {
        CalendarWarningSheet(
            onDismiss = { showCalendarWarning = false },
            onNavigateToSettings = { 
                showCalendarWarning = false
                onNavigate(AppScreenEnum.CALENDAR_SETTINGS)
            }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            GenericTitle(
                title = Loc.Calendar.event,
                onBack = onBack,
                actions = {
                    if (event != null && !event.id.startsWith("virtual_")) {
                        IconButton(onClick = { showActionSheet = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Loc.Calendar.noEventFound)
            }
        } else {
            val isShift = event.type == CalendarEventTypeEnum.SHIFT
            val themeColor = if (isShift) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(themeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isShift) Icons.Default.Work else Icons.Default.Event,
                                    null,
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = event.title.ifEmpty { Loc.Calendar.event },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeColor
                                )
                                Text(
                                    text = if (isShift) Loc.Calendar.shift else Loc.Calendar.common,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = themeColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Info Section
                Text(
                    text = Loc.General.details,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val formattedDate = remember(event.date, Loc.lang) {
                            try {
                                val localDate = LocalDate.parse(event.date)
                                localDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale(Loc.lang)))
                            } catch (_: Exception) { event.date }
                        }
                        
                        DetailRow(Icons.Default.CalendarToday, Loc.Calendar.date, formattedDate)
                        
                        val timeValue = if (event.allDay) Loc.Calendar.allDay else "${event.startTime}${event.endTime?.let { " - $it" } ?: ""}"
                        DetailRow(Icons.Default.AccessTime, Loc.Calendar.time, timeValue)
                        
                        val belongsTo = if (event.sharedGroupIds.isEmpty()) {
                            Loc.Calendar.private
                        } else {
                            val groups = viewModel.settingsManager.getGroups()
                            event.sharedGroupIds.map { id -> groups[id]?.name ?: id }.joinToString(", ")
                        }
                        
                        DetailRow(Icons.Default.Groups, Loc.Calendar.belongsTo, belongsTo)

                        if (event.note.isNotEmpty()) {
                            DetailRow(Icons.Default.Notes, Loc.Calendar.note, event.note)
                        }

                        val creatorName = if (event.ownerUid == currentUserId) Loc.Calendar.me else event.createdBy
                        DetailRow(Icons.Default.Person, Loc.Calendar.owner, creatorName)
                        
                        if (event.googleEventId != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Icon(Icons.Default.Sync, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(Loc.Calendar.synced, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (event.sharedGroupIds.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = Loc.Calendar.shareWithGroups,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    
                    val groupsMap = viewModel.settingsManager.getGroups()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        event.sharedGroupIds.forEach { gid ->
                            val group = groupsMap[gid]
                            if (group != null) {
                                Surface(
                                    color = themeColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Groups, null, tint = themeColor, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(group.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = themeColor)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                // Comments Section
                val comments by viewModel.comments.collectAsState(initial = emptyList())
                val userNames by viewModel.userNames.collectAsState(initial = emptyMap())
                
                CommentSection(
                    comments = comments,
                    currentUserId = currentUserId,
                    userNames = userNames,
                    onEvent = { viewModel.onCommentEvent(it) },
                    initiallyExpanded = true
                )
                
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            icon, 
            null, 
            tint = MaterialTheme.colorScheme.primary, 
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
