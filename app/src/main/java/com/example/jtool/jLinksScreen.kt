package com.example.jtool

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun JLinksScreen(
    manager: JLinksManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    var links by remember { mutableStateOf(manager.loadLinks()) }
    var searchQuery by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val syncUri = settingsManager.getSyncUri()

    fun triggerSync() {
        if (syncUri != null) {
            scope.launch {
                backupManager.syncPushLinks(syncUri)
            }
        }
    }

    fun updateLinks(newList: List<LinkItem>) {
        links = newList
        manager.saveLinks(newList)
        triggerSync()
    }

    LaunchedEffect(Unit) {
        if (syncUri != null) {
            if (backupManager.syncPullLinks(syncUri)) {
                links = manager.loadLinks()
            }
        }
    }

    val filteredLinks = remember(links, searchQuery) {
        if (searchQuery.isBlank()) {
            links.sortedByDescending { it.timestamp }
        } else {
            links.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.url.contains(searchQuery, ignoreCase = true) 
            }.sortedByDescending { it.timestamp }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Loc.jLinks,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(Loc.searchLinks) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLinks, key = { it.id }) { link ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = link.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = link.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                            }
                            IconButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, Loc.t("Invalid URL", "Невірна адреса"), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                            }
                            IconButton(onClick = {
                                updateLinks(links.filter { it.id != link.id })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            }
                        }
                        Text(
                            text = "${Loc.by} ${link.lastUpdatedBy}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text(Loc.linkTitle) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text(Loc.linkUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://...") }
                )
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                            var url = newUrl.trim()
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            val newLink = LinkItem(
                                id = System.currentTimeMillis(),
                                title = newTitle.trim(),
                                url = url,
                                lastUpdatedBy = settingsManager.getUserName()
                            )
                            updateLinks(links + newLink)
                            newTitle = ""
                            newUrl = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newTitle.isNotBlank() && newUrl.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.addLink)
                }
            }
        }
    }
}
