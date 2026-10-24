package com.jtool.android.core.crash

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class AnrInterceptor(private val context: Context) : Thread() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastTick = 0L

    override fun run() {
        while (!isInterrupted) {
            val currentTick = System.currentTimeMillis()
            lastTick = currentTick
            handler.post { lastTick = 0L }

            try {
                sleep(15000)
            } catch (e: InterruptedException) {
                return
            }

            if (lastTick != 0L) {
                // ANR detected
                val stackTrace = StringBuilder()
                val mainThread = Looper.getMainLooper().thread
                stackTrace.append("ANR Detected (Application Not Responding)\n")
                stackTrace.append("Main Thread Stack Trace:\n")
                mainThread.stackTrace.forEach {
                    stackTrace.append("    at $it\n")
                }

                val intent = Intent(context, CrashActivity::class.java).apply {
                    putExtra("crash_log", stackTrace.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
                return
            }
        }
    }

    companion object {
        fun install(context: Context) {
            AnrInterceptor(context).start()
        }
    }
}
