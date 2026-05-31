package com.watchnav.com

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot completed. Requesting notification listener rebind.")
        // NotificationListenerService is bound by the system, not startable via startService.
        // requestRebind signals the system to reconnect our listener if it was dropped.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cn = ComponentName(context, NavListenerService::class.java)
            NotificationListenerService.requestRebind(cn)
        }
    }
}
