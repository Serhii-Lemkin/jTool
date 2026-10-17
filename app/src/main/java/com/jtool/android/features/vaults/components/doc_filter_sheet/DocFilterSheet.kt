package com.jtool.android.features.vaults.components.doc_filter_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet
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
    onApplyFilters: (String, String, String, Boolean, Long?, Long?, Long?, Long?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(nameQuery) }
    var desc by remember { mutableStateOf(descQuery) }
    var tag by remember { mutableStateOf(tagQuery) }
    var isDownloadedOnly by remember { mutableStateOf(downloadedOnly) }
    var ddf by remember { mutableStateOf(docDateFrom) }
    var ddt by remember { mutableStateOf(docDateTo) }
    var udf by remember { mutableStateOf(uploadDateFrom) }
    var udt by remember { mutableStateOf(uploadDateTo) }

    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    
    if (showDatePickerFor != null) {
        val currentMillis = when (showDatePickerFor) {
            "ddf" -> ddf ?: System.currentTimeMillis()
            "ddt" -> ddt ?: System.currentTimeMillis()
            "udf" -> udf ?: System.currentTimeMillis()
            "udt" -> udt ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)
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

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Vaults.filterDocs
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Loc.Vaults.docName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text(Loc.Vaults.description) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text(Loc.Vaults.tag) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Pickers
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onClearFilters()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Loc.Vaults.clearFilters)
                }

                Button(
                    onClick = {
                        onApplyFilters(name.trim(), desc.trim(), tag.trim(), isDownloadedOnly, ddf, ddt, udf, udt)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Loc.Vaults.applyFilters)
                }
            }
        }
    }
}
