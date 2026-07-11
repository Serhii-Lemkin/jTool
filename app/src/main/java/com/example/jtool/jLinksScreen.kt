package com.example.jtool

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JLinksScreen(
    manager: JLinksManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager // Added this
) {
    val context = LocalContext.current
    var links by remember { mutableStateOf(manager.loadLinks()) }
    var searchQuery by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    val currentGroupId = remember { settingsManager.getCurrentGroupId() }
    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())

    fun triggerSync() {
        settingsManager.setSyncPending(true)
        scope.launch {
            groupSyncManager.pushGroupData()
        }
    }

    fun updateLinks(newList: List<LinkItem>) {
        links = newList
        manager.saveLinks(newList)
        triggerSync()
    }

    // Real-time Firebase Sync
    LaunchedEffect(currentGroupId) {
        if (currentGroupId != null) {
            groupSyncManager.groupDataFlow(currentGroupId).collect {
                links = manager.loadLinks()
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        links = manager.loadLinks()
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
            text = Loc.links,
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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(filteredLinks, key = { it.id }) { link ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = link.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = link.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(link.url))
                                Toast.makeText(context, Loc.t("Copied!", "Скопійовано!"), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, Loc.t("Invalid URL", "Невірна адреса"), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                updateLinks(links.filter { it.id != link.id })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "${Loc.by} ${userNames[link.lastUpdatedBy] ?: link.lastUpdatedBy}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
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
                                lastUpdatedBy = settingsManager.getAppUserId() // Use ID
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LinksPreview() {
    val context = LocalContext.current
    MaterialTheme {
        JLinksScreen(
            manager = JLinksManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
