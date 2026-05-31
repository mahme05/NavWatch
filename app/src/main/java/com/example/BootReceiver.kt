package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Ensures that the notification service remains reliable by triggering on system boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Initializing NavListenerService.")
            try {
                val serviceIntent = Intent(context, NavListenerService::class.java)
                context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Could not manually start service on boot: ${e.message}")
            }
        }
    }
}
