package com.jtool.android.core.crash

import android.util.Log
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.utils.DeviceInfoProvider
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.ui.theme.JToolTheme
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
        val dataManager = DataManager(this)
        Loc.load(this, settings.getLanguage())

        setContent {
            JToolTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = Loc.Crash.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = Loc.Crash.msg,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(32.dp))
                        
                        var isSending by remember { mutableStateOf(false) }
                        var isSuccess by remember { mutableStateOf(false) }
                        var errorMessage by remember { mutableStateOf<String?>(null) }
                        val scope = rememberCoroutineScope()

                        val sendReport = {
                            isSending = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val authUser = FirebaseAuth.getInstance().currentUser
                                    if (authUser == null) {
                                        isSending = false
                                        errorMessage = "User session expired. Please restart and sign in."
                                        return@launch
                                    }
                                    val userId = authUser.uid

                                    val request = SupportRequest(
                                        type = SupportTypeEnum.CRASH,
                                        status = SupportStatusEnum.OPENED,
                                        text = Loc.Crash.automatedReport,
                                        userId = userId,
                                        userName = settings.getUserName(),
                                        timestamp = System.currentTimeMillis(),
                                        deviceInfo = DeviceInfoProvider.getDeviceInfo(this@CrashActivity),
                                        crashLog = crashLog
                                    )
                                    val success = dataManager.submitSupportRequest(request, userId, bypassLimit = true)
                                    if (success) {
                                        isSuccess = true
                                    } else {
                                        errorMessage = "Server rejected the report. Please try again."
                                    }
                                    isSending = false
                                } catch (e: Exception) {
                                    isSending = false
                                    Log.e("CrashActivity", "Failed to send crash report", e)
                                    errorMessage = "Connection error: ${e.localizedMessage}"
                                }
                            }
                        }

                        /* 
                        LaunchedEffect(Unit) {
                            sendReport()
                        }
                        */
                        
                        if (isSuccess) {
                            Text(
                                text = Loc.Crash.sentSuccess, 
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { restartApp() }, modifier = Modifier.fillMaxWidth()) {
                                Text(Loc.Crash.goHome)
                            }
                        } else {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            Button(
                                onClick = { sendReport() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSending
                            ) {
                                if (isSending) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp), 
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(Loc.Crash.reporting)
                                    }
                                } else {
                                    Text(Loc.Crash.reportBug)
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            TextButton(
                                onClick = { restartApp() }, 
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSending
                            ) {
                                Text(Loc.Crash.goHome)
                            }
                        }
                    }
                }
            }
        }
    }
}
