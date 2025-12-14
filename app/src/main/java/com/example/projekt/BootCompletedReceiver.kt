package com.example.projekt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val coroutineScope = CoroutineScope(Dispatchers.IO)

            coroutineScope.launch {
                try {
                    val themePreferenceManager = ThemePreferenceManager(context)
                    val lastUserId = themePreferenceManager.lastUserId.first()

                    if (lastUserId != null) {
                        val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                            putExtra("USER_ID", lastUserId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } finally {
                    // Always call finish() so the receiver can be recycled.
                    pendingResult.finish()
                }
            }
        }
    }
}