package com.jtool.android.features.calendar.edit.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getGoogleCalendarColorPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarAddEditFormContent(
    modifier: Modifier = Modifier,
    themeColor: Color,
    title: String,
    onTitleChange: (String) -> Unit,
    colorKey: String,
    onShowColorPicker: () -> Unit,
    startDate: LocalDate,
    onShowStartDatePicker: () -> Unit,
    endDate: LocalDate,
    onShowEndDatePicker: () -> Unit,
    startTime: String,
    onPickStartTime: () -> Unit,
    endTime: String,
    onPickEndTime: () -> Unit,
    allDay: Boolean,
    onAllDayChange: (Boolean) -> Unit,
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    rrule: String,
    onShowRepeatFrequencySheet: () -> Unit,
    isAdditionalExpanded: Boolean,
    onToggleAdditionalExpanded: () -> Unit,
    locationInput: String,
    onLocationChange: (String) -> Unit,
    conferenceUrlInput: String,
    onConferenceUrlChange: (String) -> Unit,
    attachmentUrlInput: String,
    onAttachmentUrlChange: (String) -> Unit,
    selectedGroupIds: List<String>,
    groups: Map<String, GroupInfo>,
    onShowGroupPicker: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Loc.Calendar.eventTitle)
                            Text(" *", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                    ),
                    trailingIcon = {
                        if (title.isNotBlank()) {
                            IconButton(onClick = { onTitleChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            val colorPalette = getGoogleCalendarColorPalette()
            val currentColorOption = colorPalette.find { it.key == colorKey } ?: colorPalette.first()
            ActionListItem(
                headline = Loc.Calendar.eventColor,
                supportingText = currentColorOption.name,
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentColorOption.hexColor)
                    )
                },
                onClick = onShowColorPicker
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = Loc.Calendar.time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPickerButton(
                        label = Loc.s("calendar_startDate", "Start Date"),
                        value = startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        icon = Icons.Default.CalendarToday,
                        onClick = onShowStartDatePicker,
                        modifier = Modifier.weight(1.1f)
                    )
                    if (!allDay) {
                        CompactPickerButton(
                            label = Loc.Workday.start,
                            value = startTime,
                            icon = Icons.Default.AccessTime,
                            onClick = onPickStartTime,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.alpha(0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPickerButton(
                        label = Loc.s("calendar_endDate", "End Date"),
                        value = endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        icon = Icons.Default.Event,
                        onClick = onShowEndDatePicker,
                        modifier = Modifier.weight(1.1f)
                    )
                    if (!allDay) {
                        CompactPickerButton(
                            label = Loc.Workday.finish,
                            value = if (endTime.isEmpty()) Loc.General.none else endTime,
                            icon = Icons.Default.AccessTime,
                            onClick = onPickEndTime,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.alpha(0.1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Loc.Calendar.allDay, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(
                        checked = allDay,
                        onCheckedChange = onAllDayChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeColor,
                            checkedTrackColor = themeColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = com.jtool.android.core.localization.domains.CalendarLoc.repeatEvent,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = onIsRecurringChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeColor,
                            checkedTrackColor = themeColor.copy(alpha = 0.5f)
                        )
                    )
                }

                if (isRecurring) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.1f))

                    val frequencyLabel = when {
                        rrule.contains("FREQ=DAILY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryDay
                        rrule.contains("FREQ=WEEKLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryWeek
                        rrule.contains("FREQ=MONTHLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryMonth
                        rrule.contains("FREQ=YEARLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryYear
                        else -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryDay
                    }

                    ActionListItem(
                        headline = com.jtool.android.core.localization.domains.CalendarLoc.repeatFrequency,
                        supportingText = frequencyLabel,
                        leadingContent = { Icon(Icons.Default.Repeat, null, tint = themeColor) },
                        trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                        onClick = onShowRepeatFrequencySheet
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleAdditionalExpanded)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, null, tint = themeColor)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Loc.s("calendar_additionalOptions", "Additional Options"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isAdditionalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                if (isAdditionalExpanded) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.1f))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = locationInput,
                                onValueChange = onLocationChange,
                                label = { Text(Loc.s("calendar_eventLocation", "Location / Address")) },
                                leadingIcon = { Icon(Icons.Default.Place, null, tint = themeColor) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                                ),
                                trailingIcon = {
                                    if (locationInput.isNotBlank()) {
                                        IconButton(onClick = { onLocationChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            if (locationInput.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(locationInput)}")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        val chooser = Intent.createChooser(intent, Loc.s("calendar_openLocationWith", "Open Location With"))
                                        try {
                                            context.startActivity(chooser)
                                        } catch (_: Exception) {
                                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationInput)}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Map, null, tint = themeColor)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = conferenceUrlInput,
                            onValueChange = onConferenceUrlChange,
                            label = { Text(Loc.s("calendar_conferenceUrl", "Video Meeting Link (Google Meet / Zoom)")) },
                            leadingIcon = { Icon(Icons.Default.VideoCall, null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (conferenceUrlInput.isNotBlank()) {
                                    IconButton(onClick = { onConferenceUrlChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )

                        OutlinedTextField(
                            value = attachmentUrlInput,
                            onValueChange = onAttachmentUrlChange,
                            label = { Text(Loc.s("calendar_attachmentUrl", "Attachment / Document Link")) },
                            leadingIcon = { Icon(Icons.Default.AttachFile, null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (attachmentUrlInput.isNotBlank()) {
                                    IconButton(onClick = { onAttachmentUrlChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = Loc.Calendar.belongsTo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ActionListItem(
                    headline = Loc.Calendar.shareWithGroups,
                    supportingText = if (selectedGroupIds.isEmpty()) {
                        Loc.Calendar.private
                    } else {
                        selectedGroupIds.map { id ->
                            val group = groups[id]
                            if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: id
                        }.joinToString(", ")
                    },
                    leadingContent = { Icon(Icons.Default.Groups, null, tint = themeColor) },
                    trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                    onClick = onShowGroupPicker,
                    supportingMaxLines = 1
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = Loc.Calendar.note,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
        )

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text(Loc.Calendar.note) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
            ),
            trailingIcon = {
                if (note.isNotBlank()) {
                    IconButton(onClick = { onNoteChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                focusedLabelColor = themeColor
            )
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CompactPickerButton(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
