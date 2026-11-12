package com.jtool.android.features.vaults.components.doc_filter_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.utils.rememberCustomDatePickerState
import com.jtool.android.features.vaults.VaultSortEnum
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocFilterSheet(
    nameQuery: String,
    descQuery: String,
    tagQuery: String,
    downloadedOnly: Boolean,
    docDateFrom: Long?,
    docDateTo: Long?,
    uploadDateFrom: Long?,
    uploadDateTo: Long?,
    currentSort: VaultSortEnum,
    onApplyFilters: (String, String, String, Boolean, Long?, Long?, Long?, Long?, VaultSortEnum) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(nameQuery) }
    var desc by remember { mutableStateOf(descQuery) }
    var tag by remember { mutableStateOf(tagQuery) }
    var isDownloadedOnly by remember { mutableStateOf(downloadedOnly) }
    var ddf by remember { mutableStateOf(docDateFrom) }
    var ddt by remember { mutableStateOf(docDateTo) }
    var udf by remember { mutableStateOf(uploadDateFrom) }
    var udt by remember { mutableStateOf(uploadDateTo) }
    var sortType by remember { mutableStateOf(currentSort) }

    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    
    if (showDatePickerFor != null) {
        val currentMillis = when (showDatePickerFor) {
            "ddf" -> ddf ?: System.currentTimeMillis()
            "ddt" -> ddt ?: System.currentTimeMillis()
            "udf" -> udf ?: System.currentTimeMillis()
            "udt" -> udt ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        val datePickerState = rememberCustomDatePickerState(initialSelectedDateMillis = currentMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        when (showDatePickerFor) {
                            "ddf" -> ddf = it
                            "ddt" -> ddt = it
                            "udf" -> udf = it
                            "udt" -> udt = it
                        }
                    }
                    showDatePickerFor = null
                }) { Text(Loc.General.select) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    when (showDatePickerFor) {
                        "ddf" -> ddf = null
                        "ddt" -> ddt = null
                        "udf" -> udf = null
                        "udt" -> udt = null
                    }
                    showDatePickerFor = null 
                }) { Text(Loc.General.reset) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = Loc.s("vaults_filterTab", "Filter"),
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = Loc.s("vaults_sortTab", "Sort"),
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Loc.Vaults.docName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                IconButton(onClick = { name = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(Loc.Vaults.description) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            if (desc.isNotBlank()) {
                                IconButton(onClick = { desc = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val settingsManager = remember { com.jtool.android.core.settings.SettingsManager(context) }
                    val cachedCustomTags = remember { settingsManager.getCachedCustomTags() }
                    
                    val standardTagsMap = remember {
                        mapOf(
                            "#DOCUMENT#" to com.jtool.android.core.localization.domains.VaultsLoc.tagDocument,
                            "#WARRANTY#" to com.jtool.android.core.localization.domains.VaultsLoc.tagWarranty,
                            "#RECEIPT#" to com.jtool.android.core.localization.domains.VaultsLoc.tagReceipt,
                            "#INVOICE#" to com.jtool.android.core.localization.domains.VaultsLoc.tagInvoice,
                            "#AGREEMENT#" to com.jtool.android.core.localization.domains.VaultsLoc.tagAgreement,
                            "#IDENTIFICATION#" to com.jtool.android.core.localization.domains.VaultsLoc.tagId,
                            "#SLIP#" to com.jtool.android.core.localization.domains.VaultsLoc.tagSlip,
                            "#LICENSE#" to com.jtool.android.core.localization.domains.VaultsLoc.tagLicense,
                            "#CONTRACT#" to com.jtool.android.core.localization.domains.VaultsLoc.tagContract,
                            "#INSURANCE#" to com.jtool.android.core.localization.domains.VaultsLoc.tagInsurance,
                            "#MEDICAL#" to com.jtool.android.core.localization.domains.VaultsLoc.tagMedical,
                            "#TAX#" to com.jtool.android.core.localization.domains.VaultsLoc.tagTax,
                            "#VEHICLE#" to com.jtool.android.core.localization.domains.VaultsLoc.tagVehicle,
                            "#PROPERTY#" to com.jtool.android.core.localization.domains.VaultsLoc.tagProperty,
                            "#FINANCIAL#" to com.jtool.android.core.localization.domains.VaultsLoc.tagFinancial,
                            "#CERTIFICATE#" to com.jtool.android.core.localization.domains.VaultsLoc.tagCertificate,
                            "#EDUCATION#" to com.jtool.android.core.localization.domains.VaultsLoc.tagEducation
                        )
                    }

                    val tagsList = remember(cachedCustomTags, standardTagsMap) {
                        val list = mutableListOf<Pair<String, String>>()
                        list.addAll(standardTagsMap.toList())
                        cachedCustomTags.forEach { custom ->
                            if (custom.isNotBlank() && !standardTagsMap.containsKey(custom)) {
                                val label = if (custom.startsWith("#") && custom.endsWith("#")) custom.removeSurrounding("#") else custom
                                list.add(custom to label)
                            }
                        }
                        list
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Loc.Vaults.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tagsList, key = { it.first }) { (key, label) ->
                                val isSelected = tag == key || tag.equals(label, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        tag = if (isSelected) "" else key
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        OutlinedTextField(
                            value = tag,
                            onValueChange = { tag = it },
                            label = { Text(Loc.Vaults.tag) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                            trailingIcon = {
                                if (tag.isNotBlank()) {
                                    IconButton(onClick = { tag = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }

                    // Date Pickers
                    val dateFormat = remember(context) { com.jtool.android.core.utils.getPreferredSimpleDateFormat(context) }
                    
                    Text(Loc.Vaults.docDate, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDatePickerFor = "ddf" }, modifier = Modifier.weight(1f)) {
                            Text("${com.jtool.android.core.localization.domains.VaultsLoc.dateFrom}: ${ddf?.let { dateFormat.format(Date(it)) } ?: "-"}")
                        }
                        OutlinedButton(onClick = { showDatePickerFor = "ddt" }, modifier = Modifier.weight(1f)) {
                            Text("${com.jtool.android.core.localization.domains.VaultsLoc.dateTo}: ${ddt?.let { dateFormat.format(Date(it)) } ?: "-"}")
                        }
                    }
                    
                    Text(Loc.Vaults.uploadDate, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDatePickerFor = "udf" }, modifier = Modifier.weight(1f)) {
                            Text("${com.jtool.android.core.localization.domains.VaultsLoc.uploadDateFrom}: ${udf?.let { dateFormat.format(Date(it)) } ?: "-"}")
                        }
                        OutlinedButton(onClick = { showDatePickerFor = "udt" }, modifier = Modifier.weight(1f)) {
                            Text("${com.jtool.android.core.localization.domains.VaultsLoc.uploadDateTo}: ${udt?.let { dateFormat.format(Date(it)) } ?: "-"}")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Loc.Vaults.downloaded, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isDownloadedOnly,
                            onCheckedChange = { isDownloadedOnly = it }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionListItem(
                            headline = Loc.s("vaults_sortUploadDate", "Upload Date (Newest First)"),
                            supportingText = Loc.s("vaults_sortUploadDateSubtitle", "Default sort by cloud upload timestamp"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = if (sortType == VaultSortEnum.UPLOAD_DATE_DESC) {
                                { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = { sortType = VaultSortEnum.UPLOAD_DATE_DESC }
                        )

                        ActionListItem(
                            headline = Loc.s("vaults_sortName", "Document Name (A - Z)"),
                            supportingText = Loc.s("vaults_sortNameSubtitle", "Alphabetical order by title"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = if (sortType == VaultSortEnum.NAME_ASC) {
                                { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = { sortType = VaultSortEnum.NAME_ASC }
                        )

                        ActionListItem(
                            headline = Loc.s("vaults_sortDocDate", "Document Date (Newest First)"),
                            supportingText = Loc.s("vaults_sortDocDateSubtitle", "Sort by document issue date"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = if (sortType == VaultSortEnum.DOC_DATE_DESC) {
                                { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = { sortType = VaultSortEnum.DOC_DATE_DESC }
                        )

                        ActionListItem(
                            headline = Loc.s("vaults_sortTag", "Tag Name (A - Z)"),
                            supportingText = Loc.s("vaults_sortTagSubtitle", "Alphabetical order by tag category"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = if (sortType == VaultSortEnum.TAG_ASC) {
                                { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = { sortType = VaultSortEnum.TAG_ASC }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        name = ""
                        desc = ""
                        tag = ""
                        isDownloadedOnly = false
                        ddf = null
                        ddt = null
                        udf = null
                        udt = null
                        sortType = VaultSortEnum.UPLOAD_DATE_DESC
                        onClearFilters()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Loc.Vaults.clearFilters)
                }

                Button(
                    onClick = {
                        onApplyFilters(
                            name.trim(),
                            desc.trim(),
                            tag.trim(),
                            isDownloadedOnly,
                            ddf,
                            ddt,
                            udf,
                            udt,
                            sortType
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Loc.Vaults.applyFilters)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
