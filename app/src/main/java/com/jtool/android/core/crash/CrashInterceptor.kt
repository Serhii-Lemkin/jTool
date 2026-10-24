package com.jtool.android.core.crash

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashInterceptor(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val causeString = throwable.cause?.toString() ?: ""
        val messageString = throwable.message ?: ""
        
        if (throwable is FirebaseFirestoreException || 
            causeString.contains("FirebaseFirestoreException") || 
            causeString.contains("NOT_FOUND: No document to update") ||
            messageString.contains("NOT_FOUND: No document to update")) {
            Log.e("CrashInterceptor", "Ignored non-fatal Firestore rejected write exception", throwable)
            return
        }

        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("crash_log", stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            
            exitProcess(1)
        } catch (e: Exception) {
            Log.e("CrashInterceptor", "Failed to start CrashActivity", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashInterceptor(context))
        }
    }
}
