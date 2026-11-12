package com.jtool.android.features.vaults.components.incoming_share_wizard_sheet

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.LinksLoc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.sync.SharedTextContent

enum class ShareTextWizardStep {
    GROUP_SELECT,
    APP_SELECT,
    LINK_FORM,
    NOTE_FORM
}

data class ExtractedLinkData(
    val url: String,
    val title: String,
    val tag: String = "#COMMON#"
)

data class ExtractedNoteData(
    val title: String,
    val content: String
)

fun extractLinkData(text: String, subject: String): ExtractedLinkData {
    val trimmedText = text.trim()
    val trimmedSubject = subject.trim()

    val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
    val match = urlRegex.find(trimmedText)

    val extractedUrl = if (match != null) {
        match.value
    } else if (trimmedText.startsWith("www.", ignoreCase = true) || trimmedText.contains(".com") || trimmedText.contains(".org") || trimmedText.contains(".net") || trimmedText.contains(".io") || trimmedText.contains(".co")) {
        if (!trimmedText.startsWith("http://") && !trimmedText.startsWith("https://")) {
            "https://$trimmedText"
        } else trimmedText
    } else {
        trimmedText
    }

    val extraTextWithoutUrl = if (match != null) {
        trimmedText.replace(match.value, "").trim()
    } else ""

    val lowerUrl = extractedUrl.lowercase()
    val detectedTag = when {
        lowerUrl.contains("instagram.com") || lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") || lowerUrl.contains("facebook.com") || lowerUrl.contains("linkedin.com") || lowerUrl.contains("tiktok.com") -> "#SOCIAL#"
        lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("vimeo.com") -> "#VIDEO#"
        lowerUrl.contains("spotify.com") || lowerUrl.contains("soundcloud.com") || lowerUrl.contains("apple.com/music") -> "#MUSIC#"
        lowerUrl.contains("amazon.com") || lowerUrl.contains("ebay.com") || lowerUrl.contains("etsy.com") || lowerUrl.contains("shop") -> "#SHOPPING#"
        lowerUrl.contains("recipe") || lowerUrl.contains("food") || lowerUrl.contains("cook") -> "#RECIPE#"
        lowerUrl.contains("maps") || lowerUrl.contains("location") || lowerUrl.contains("place") -> "#PLACES#"
        else -> "#COMMON#"
    }

    val title = when {
        trimmedSubject.isNotBlank() -> trimmedSubject
        extraTextWithoutUrl.isNotBlank() -> extraTextWithoutUrl.take(60)
        lowerUrl.contains("instagram.com") -> "Instagram Link"
        lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> "YouTube Link"
        lowerUrl.contains("tiktok.com") -> "TikTok Link"
        lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") -> "X / Twitter Link"
        lowerUrl.contains("facebook.com") -> "Facebook Link"
        lowerUrl.contains("linkedin.com") -> "LinkedIn Link"
        else -> {
            try {
                val host = Uri.parse(extractedUrl).host
                if (!host.isNullOrBlank()) host.removePrefix("www.") else "Shared Link"
            } catch (_: Exception) {
                "Shared Link"
            }
        }
    }

    return ExtractedLinkData(url = extractedUrl, title = title, tag = detectedTag)
}

fun extractNoteData(text: String, subject: String): ExtractedNoteData {
    val trimmedText = text.trim()
    val trimmedSubject = subject.trim()

    val linkData = extractLinkData(trimmedText, trimmedSubject)

    val title = when {
        trimmedSubject.isNotBlank() -> trimmedSubject
        trimmedText.contains('\n') -> trimmedText.substringBefore('\n').trim().take(50)
        trimmedText.startsWith("http://") || trimmedText.startsWith("https://") -> linkData.title
        else -> trimmedText.take(40)
    }

    return ExtractedNoteData(title = title, content = trimmedText)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingShareTextWizardSheet(
    sharedContent: SharedTextContent,
    groups: Map<String, GroupInfo>,
    currentGroupId: String?,
    onSaveLink: (groupId: String, title: String, url: String, tag: String) -> Unit,
    onSaveNote: (groupId: String, title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(ShareTextWizardStep.GROUP_SELECT) }
    var selectedGroupId by remember { mutableStateOf(currentGroupId ?: groups.keys.firstOrNull() ?: "") }

    val initialLinkData = remember(sharedContent) { extractLinkData(sharedContent.text, sharedContent.subject) }
    val initialNoteData = remember(sharedContent) { extractNoteData(sharedContent.text, sharedContent.subject) }

    var linkTitleInput by remember(initialLinkData) { mutableStateOf(initialLinkData.title) }
    var linkUrlInput by remember(initialLinkData) { mutableStateOf(initialLinkData.url) }
    var linkTagInput by remember(initialLinkData) { mutableStateOf(initialLinkData.tag) }

    var noteTitleInput by remember(initialNoteData) { mutableStateOf(initialNoteData.title) }
    var noteContentInput by remember(initialNoteData) { mutableStateOf(initialNoteData.content) }

    val allLinkTags = remember { LinksLoc.allStandardLinkTags }
    var tagSearchInput by remember(linkTagInput) { mutableStateOf(LinksLoc.getLocalizedLinkTag(linkTagInput)) }

    val filteredLinkTags = remember(tagSearchInput, allLinkTags) {
        if (tagSearchInput.isBlank()) {
            allLinkTags
        } else {
            val lower = tagSearchInput.trim().lowercase()
            allLinkTags.filter { it.second.lowercase().contains(lower) || it.first.lowercase().contains(lower) }
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentStep != ShareTextWizardStep.GROUP_SELECT) {
        currentStep = when (currentStep) {
            ShareTextWizardStep.APP_SELECT -> ShareTextWizardStep.GROUP_SELECT
            ShareTextWizardStep.LINK_FORM, ShareTextWizardStep.NOTE_FORM -> ShareTextWizardStep.APP_SELECT
            else -> ShareTextWizardStep.GROUP_SELECT
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep != ShareTextWizardStep.GROUP_SELECT) {
                    IconButton(
                        onClick = {
                            currentStep = when (currentStep) {
                                ShareTextWizardStep.APP_SELECT -> ShareTextWizardStep.GROUP_SELECT
                                ShareTextWizardStep.LINK_FORM, ShareTextWizardStep.NOTE_FORM -> ShareTextWizardStep.APP_SELECT
                                else -> ShareTextWizardStep.GROUP_SELECT
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                val titleText = when (currentStep) {
                    ShareTextWizardStep.GROUP_SELECT -> Loc.s("vaults_wizardGroupTitle", "Step 1: Select Group")
                    ShareTextWizardStep.APP_SELECT -> Loc.s("vaults_wizardAppTitle", "Step 2: Select Feature App")
                    ShareTextWizardStep.LINK_FORM -> Loc.s("links_saveSharedLink", "Step 3: Save Shared Link")
                    ShareTextWizardStep.NOTE_FORM -> Loc.s("notes_saveSharedNote", "Step 3: Save Shared Note")
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            when (currentStep) {
                ShareTextWizardStep.GROUP_SELECT -> {
                    Text(
                        text = Loc.s("vaults_wizardSelectGroupSubtitle", "Choose which group to share this content with:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(groups.values.toList(), key = { it.id }) { group ->
                            val isSelected = group.id == selectedGroupId
                            ActionListItem(
                                headline = if (group.isPrivate) Loc.s("calendar_private", "Private / Me") else group.name,
                                leadingContent = {
                                    Icon(
                                        imageVector = if (group.isPrivate) Icons.Default.Person else Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    selectedGroupId = group.id
                                    currentStep = ShareTextWizardStep.APP_SELECT
                                }
                            )
                        }
                    }
                }

                ShareTextWizardStep.APP_SELECT -> {
                    Text(
                        text = Loc.s("vaults_wizardSelectAppSubtitle", "Select destination feature app:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ActionListItem(
                        headline = Loc.s("links_title", "Links"),
                        supportingText = Loc.s("links_appSubtitle", "Save web link to Links app"),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            currentStep = ShareTextWizardStep.LINK_FORM
                        }
                    )

                    ActionListItem(
                        headline = Loc.s("notes_title", "Notes"),
                        supportingText = Loc.s("notes_appSubtitle", "Save text note to Notes app"),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            currentStep = ShareTextWizardStep.NOTE_FORM
                        }
                    )
                }

                ShareTextWizardStep.LINK_FORM -> {
                    OutlinedTextField(
                        value = linkTitleInput,
                        onValueChange = { linkTitleInput = it },
                        label = { Text(Loc.s("links_linkTitle", "Title")) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            if (linkTitleInput.isNotBlank()) {
                                IconButton(onClick = { linkTitleInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
                        label = { Text(Loc.s("links_linkUrl", "URL / Link")) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredLinkTags.isNotEmpty()) {
                            Text(
                                text = LinksLoc.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredLinkTags, key = { it.first }) { (key, label) ->
                                    val isSelected = linkTagInput.equals(key, ignoreCase = true) || tagSearchInput.equals(label, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            linkTagInput = key
                                            tagSearchInput = label
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tagSearchInput,
                            onValueChange = { input ->
                                tagSearchInput = input
                                val matchedKey = allLinkTags.firstOrNull { it.second.equals(input.trim(), ignoreCase = true) }?.first
                                linkTagInput = matchedKey ?: input.trim()
                            },
                            label = { Text(LinksLoc.tag) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                            trailingIcon = {
                                if (tagSearchInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            tagSearchInput = ""
                                            linkTagInput = "#COMMON#"
                                        }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (linkTitleInput.isNotBlank() && linkUrlInput.isNotBlank()) {
                                onSaveLink(selectedGroupId, linkTitleInput.trim(), linkUrlInput.trim(), linkTagInput)
                            }
                        },
                        enabled = linkTitleInput.isNotBlank() && linkUrlInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(Loc.s("links_addLink", "Save Link"), fontWeight = FontWeight.Bold)
                    }
                }

                ShareTextWizardStep.NOTE_FORM -> {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text(Loc.s("notes_noteTitle", "Title")) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            if (noteTitleInput.isNotBlank()) {
                                IconButton(onClick = { noteTitleInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text(Loc.s("notes_noteContent", "Note Content")) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (noteTitleInput.isNotBlank() && noteContentInput.isNotBlank()) {
                                onSaveNote(selectedGroupId, noteTitleInput.trim(), noteContentInput.trim())
                            }
                        },
                        enabled = noteTitleInput.isNotBlank() && noteContentInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(Loc.s("notes_addNote", "Save Note"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
