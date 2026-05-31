package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NavNotificationHelper {
    private const val CHANNEL_ID = "watchnav_alerts_v1"
    private const val CHANNEL_NAME = "WatchNav Alerts"
    private const val SERVICE_NOTIFICATION_ID = 1001
    private const val ALERT_NOTIFICATION_ID = 1002

    private val _currentDirection = MutableStateFlow<NavDirection?>(null)
    val currentDirection = _currentDirection.asStateFlow()

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getNotificationManager(context)
            // Check if channel already exists
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Active turn-by-turn alerts routed to your smartwatch"
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Builds a persistent, quiet background service keep-alive notification.
     */
    fun buildServiceNotification(context: Context): Notification {
        createNotificationChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle("WatchNav Bridge Active")
            .setContentText("Ready to forward navigation instructions to your watch")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Builds the notification object suited for foreground service and smartwatch delivery.
     */
    fun buildNotification(context: Context, direction: NavDirection): Notification {
        createNotificationChannel(context)

        val title = if (direction.distance.isNotBlank()) {
            "${direction.distance} → ${direction.instruction}"
        } else {
            direction.instruction
        }

        // Truncate street name to 40 characters
        val text = direction.street.take(40)

        // Return builder matching user specification
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)             // MUST be false so wearable companions forward it
            .setOnlyAlertOnce(false)       // Alert watch for every turn update
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // CATEGORY_MESSAGE is highly prioritized by smartwatch links
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    /**
     * Posts a navigation notification. Also updates the in-app state flow for UI monitoring.
     */
    fun post(context: Context, direction: NavDirection) {
        _currentDirection.value = direction
        val notificationManager = getNotificationManager(context)
        
        // Post the real alert notification that maps to the watch (ID 1002)
        val notification = buildNotification(context, direction)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)

        // Also post the quiet persistent keep-alive service notification (ID 1001)
        val serviceNotif = buildServiceNotification(context)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, serviceNotif)
    }

    /**
     * Cancels the active persistent navigation notification and clears UI state.
     */
    fun cancel(context: Context) {
        _currentDirection.value = null
        val notificationManager = getNotificationManager(context)
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        notificationManager.cancel(SERVICE_NOTIFICATION_ID)
    }
}
