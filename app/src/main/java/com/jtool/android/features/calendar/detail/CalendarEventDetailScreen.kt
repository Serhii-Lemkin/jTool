package com.jtool.android.features.calendar.detail

import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.components.comments.CommentSection
import com.jtool.android.core.components.comments.CommentEvent
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.features.calendar.components.delete_event_confirm_sheet.DeleteEventConfirmSheet
import com.jtool.android.features.calendar.detail.components.event_action_sheet.EventActionSheet
import com.jtool.android.features.calendar.detail.components.rsvp_status_sheet.RsvpStatusSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventDetailScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarEventDetailViewModel,
    groups: Map<String, GroupInfo>
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailToProfile by viewModel.emailToProfile.collectAsState()
    val event = uiState.event
    val extension = uiState.extension
    var showDeleteSheet by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showRsvpSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showDeleteSheet) {
        DeleteEventConfirmSheet(
            isSynced = true,
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

    if (showRsvpSheet && event != null) {
        RsvpStatusSheet(
            currentStatus = event.currentUserStatus,
            onAccept = { viewModel.acceptInvite() },
            onDecline = { viewModel.declineInvite() },
            onDismiss = { showRsvpSheet = false }
        )
    }

    if (showActionSheet && event != null) {
        val myEmail = viewModel.settingsManager.getGoogleCalendarEmail().ifEmpty {
            FirebaseAuth.getInstance().currentUser?.email ?: ""
        }.lowercase().trim()
        val myAuthEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim() ?: ""
        val myUid = viewModel.settingsManager.getUserId()
        val isCreator = (event.organizer != null && event.organizer.lowercase().trim().let { it == myEmail || it == myAuthEmail }) ||
                        (extension != null && extension.ownerUid == myUid) ||
                        event.id.startsWith("virtual_")

        EventActionSheet(
            event = event,
            canEdit = isCreator || !event.id.startsWith("virtual_"),
            canChangeRsvp = !isCreator,
            onEdit = { viewModel.toggleEditDialog(true) },
            onDelete = { showDeleteSheet = true },
            onChangeRsvp = { showRsvpSheet = true },
            onSyncToggle = {},
            onShareToggle = {},
            onDismiss = { showActionSheet = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.isPermissionDenied) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        CalendarLoc.permissionDenied,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        CalendarLoc.permissionDeniedMsg,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text(Loc.General.back)
                    }
                }
            }
        } else if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Loc.Calendar.noEventFound)
            }
        } else {
            val themeColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                val myEmail = viewModel.settingsManager.getGoogleCalendarEmail().ifEmpty {
                    FirebaseAuth.getInstance().currentUser?.email ?: ""
                }.lowercase().trim()
                val myAuthEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim() ?: ""
                val myUid = viewModel.settingsManager.getUserId()
                val myName = viewModel.settingsManager.getUserName()

                val isCreator = (event.organizer != null && event.organizer.lowercase().trim().let { it == myEmail || it == myAuthEmail }) ||
                                (extension != null && extension.ownerUid == myUid) ||
                                event.id.startsWith("virtual_")

                val extStatus = extension?.attendeeStatuses?.get(myEmail) ?: extension?.attendeeStatuses?.get(myAuthEmail)
                val myStatus = extStatus ?: event.currentUserStatus ?: event.attendeeStatuses[myEmail] ?: event.attendeeStatuses[myAuthEmail]

                val isDeclined = myStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                val showRsvpButtons = !isCreator && myStatus != CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED

                val eventColor = getEventColor(event.colorKey)
                val workShiftColorKey = remember { viewModel.settingsManager.getWorkShiftColorKey() }
                val isWork = (event.colorKey != null && event.colorKey == workShiftColorKey) || 
                             (event.colorKey == null && (workShiftColorKey == "7" || workShiftColorKey == "1"))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = eventColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(eventColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWork) Icons.Default.Work else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = event.title.ifEmpty { Loc.Calendar.event },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = eventColor
                                )
                            }
                        }
                    }
                }
                
                if (showRsvpButtons) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { viewModel.acceptInvite() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isResponding,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (uiState.isResponding) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text(CalendarLoc.acceptInvite)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.declineInvite() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isResponding,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (uiState.isResponding) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(8.dp))
                                Text(Loc.General.decline)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = Loc.General.details,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val formattedDate = remember(event.date, Loc.lang) {
                            try {
                                val localDate = LocalDate.parse(event.date)
                                localDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale(Loc.lang)))
                            } catch (_: Exception) { event.date }
                        }
                        
                        DetailRow(Icons.Default.CalendarToday, Loc.Calendar.date, formattedDate)
                        DetailRow(Icons.Default.AccessTime, Loc.Calendar.time, "${event.startTime} - ${event.endTime}")

                        if (event.note.isNotEmpty()) {
                            DetailRow(Icons.Default.Notes, Loc.Calendar.note, event.note)
                        }

                        if (extension != null && extension.sharedGroupIds.isNotEmpty()) {
                            val groupNames = extension.sharedGroupIds.map { gid ->
                                val group = groups[gid]
                                if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                            }
                            DetailRow(Icons.Default.Groups, Loc.Calendar.belongsTo, groupNames.joinToString(", "))
                        } else if (event.attendees.isNotEmpty()) {
                            val attendeeNames = event.attendees.map { email ->
                                val cleanEmail = email.lowercase().trim()
                                val profile = emailToProfile[cleanEmail]
                                when {
                                    (cleanEmail == myEmail || cleanEmail == myAuthEmail) && myName.isNotEmpty() -> myName
                                    profile != null && profile.displayName.isNotEmpty() -> profile.displayName
                                    else -> email
                                }
                            }
                            DetailRow(Icons.Default.Groups, Loc.Calendar.belongsTo, attendeeNames.joinToString(", "))
                        }
                    }
                }

                val allAttendeeEmails = remember(event.attendees) {
                    event.attendees.map { it.lowercase().trim() }.distinct().filter { it.isNotEmpty() }
                }

                if (allAttendeeEmails.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = Loc.Tracker.members,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            allAttendeeEmails.forEach { email ->
                                val profile = emailToProfile[email]
                                val displayName = when {
                                    (email == myEmail || email == myAuthEmail) && myName.isNotEmpty() -> myName
                                    profile != null && profile.displayName.isNotEmpty() -> profile.displayName
                                    else -> email
                                }

                                val extAttendeeStatus = extension?.attendeeStatuses?.get(email)
                                val status = extAttendeeStatus 
                                    ?: (if (email == myEmail || email == myAuthEmail) event.currentUserStatus else null)
                                    ?: event.attendeeStatuses[email] 
                                    ?: event.attendeeStatuses[email.lowercase().trim()]

                                val (statusIcon, statusColor, statusLabel) = when (status) {
                                    CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> Triple(
                                        Icons.Default.CheckCircle,
                                        Color(0xFF4CAF50),
                                        CalendarLoc.accepted
                                    )
                                    CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> Triple(
                                        Icons.Default.Cancel,
                                        MaterialTheme.colorScheme.error,
                                        CalendarLoc.declined
                                    )
                                    else -> Triple(
                                        Icons.Default.HourglassTop,
                                        Color(0xFFFF9800),
                                        CalendarLoc.pending
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (!showRsvpButtons) {
                    Spacer(Modifier.height(24.dp))
                    
                    CommentSection(
                        comments = uiState.comments,
                        currentUserId = viewModel.settingsManager.getUserId(),
                        userNames = uiState.userNames,
                        onEvent = { e ->
                            when (e) {
                                is CommentEvent.Add -> viewModel.addComment(e.content, e.parentId)
                                is CommentEvent.Edit -> {} // Implement if needed
                                is CommentEvent.Delete -> {} // Implement if needed
                            }
                        }
                    )
                }

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
