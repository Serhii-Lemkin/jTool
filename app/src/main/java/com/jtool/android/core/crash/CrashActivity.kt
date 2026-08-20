package com.jtool.android.core.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.utils.DeviceInfoProvider
import com.jtool.android.features.support.logic.SupportRequest
import com.jtool.android.features.support.logic.SupportStatus
import com.jtool.android.features.support.logic.SupportType
import com.jtool.android.ui.theme.JToolTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {
    private fun restartApp() {
        val intent = Intent(this, com.jtool.android.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val crashLog = intent.getStringExtra("crash_log") ?: "No log found"
        val settings = SettingsManager(this)
        val syncManager = GroupSyncManager(this)
        Loc.load(this, settings.getLanguage())

        setContent {
            JToolTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Oops! App Crashed",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            text = "A fatal error occurred. You can report this to the developer.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        text = crashLog,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        var isSending by remember { mutableStateOf(false) }
                        var isSuccess by remember { mutableStateOf(false) }
                        
                        if (isSuccess) {
                            Text("Report sent! Thank you.", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { restartApp() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Go to Home")
                            }
                        } else {
                            Button(
                                onClick = {
                                    isSending = true
                                    val scope = MainScope()
                                    scope.launch {
                                        val request = SupportRequest(
                                            type = SupportType.CRASH,
                                            status = SupportStatus.OPENED,
                                            text = "Automated Crash Report",
                                            userId = settings.getAppUserId(),
                                            userName = settings.getUserName(),
                                            timestamp = System.currentTimeMillis(),
                                            deviceInfo = DeviceInfoProvider.getDeviceInfo(this@CrashActivity),
                                            crashLog = crashLog
                                        )
                                        val success = syncManager.submitSupportRequest(request, settings.getUserId(), bypassLimit = true)
                                        isSending = false
                                        if (success) isSuccess = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSending
                            ) {
                                if (isSending) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                else Text("Report to Admin")
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            TextButton(onClick = { restartApp() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Go to Home")
                            }
                        }
                    }
                }
            }
        }
    }
}
