package com.example

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NavListenerService"
        private val MAPS_PACKAGES = setOf("com.google.android.apps.maps", "com.waze")

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "Notification listener connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "Notification listener disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (!MAPS_PACKAGES.contains(packageName)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d(TAG, "Notification received from $packageName: Title='$title', Text='$text'")

        // 1. Detect if this is a navigation turn-by-turn notification using specified keywords
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()
        val combined = "$lowerTitle $lowerText"

        val keywords = listOf(
            "turn", "head", "continue", "merge", "exit", 
            "arrive", "destination", "roundabout", "keep", "take"
        )
        val isDistanceKeyword = lowerTitle.contains("in ") || lowerText.contains("in ") || 
                                lowerTitle.contains(" m") || lowerText.contains(" m") || 
                                lowerTitle.contains("ft") || lowerText.contains("ft") ||
                                lowerTitle.contains("km") || lowerText.contains("km") ||
                                lowerTitle.contains("mi") || lowerText.contains("mi")

        val isNavNotification = keywords.any { combined.contains(it) } || isDistanceKeyword

        if (!isNavNotification) {
            Log.d(TAG, "Notification filtered out: does not match navigation keywords.")
            return
        }

        // 2. Parse Distance
        // Distance regex specified by user: "In (\d+\s?(?:m|km|ft|mi))"
        // Let's add optional decimal/space matching to make it highly robust
        val distanceRegex = "In (\\d+(?:\\.\\d+)?\\s?(?:m|km|ft|mi))".toRegex(RegexOption.IGNORE_CASE)
        var distance = ""
        val matchResult = distanceRegex.find(title) ?: distanceRegex.find(text)

        if (matchResult != null) {
            distance = matchResult.groupValues[1]
        } else {
            // Fallback: match standalone distance without "In"
            val fallbackRegex = "(\\d+(?:\\.\\d+)?\\s?(?:m|km|ft|mi))".toRegex(RegexOption.IGNORE_CASE)
            val fallbackMatch = fallbackRegex.find(title) ?: fallbackRegex.find(text)
            if (fallbackMatch != null) {
                distance = fallbackMatch.groupValues[1]
            }
        }

        // 3. Clean "In 300 m," prefix from Title
        val prefixRegex = "^In \\d+(?:\\.\\d+)?\\s?(?:m|km|ft|mi)[,\\s]*".toRegex(RegexOption.IGNORE_CASE)
        var instruction = title.replace(prefixRegex, "").trim()

        if (instruction.isEmpty()) {
            // If title only contained the distance, try to extract direction from the first part of the text
            val textBeforeDot = if (text.contains("·")) text.split("·").first().trim() else text.trim()
            instruction = textBeforeDot
        }

        // 4. Extract street name from text before "·" character
        val rawStreet = if (text.contains("·")) {
            text.split("·").first().trim()
        } else {
            text.trim()
        }

        // Clean redundant instructions from street name to make it look clean (e.g. "Turn right onto Elm St" -> "Elm St")
        var street = rawStreet
        val cleanStreetRegex = "^(?:Turn\\s+(?:left|right|slight left|slight right|sharp left|sharp right)\\s+(?:onto|on)\\s+|Head\\s+(?:north|south|east|west|northeast|northwest|southeast|southwest)\\s+(?:on|towards)\\s+|Continue\\s+onto\\s+|Merge\\s+onto\\s+)(.*)".toRegex(RegexOption.IGNORE_CASE)
        val cleanMatch = cleanStreetRegex.find(rawStreet)
        if (cleanMatch != null && cleanMatch.groupValues[1].isNotBlank()) {
            street = cleanMatch.groupValues[1].trim()
        }

        // Final sanitation check
        if (instruction.isBlank()) {
            instruction = "Continue"
        }
        if (street.isBlank()) {
            street = text
        }

        val direction = NavDirection(
            instruction = instruction,
            distance = distance,
            street = street
        )

        Log.d(TAG, "Parsed navigation: $direction")

        // 5. Post notification updates via Helper
        NavNotificationHelper.post(this, direction)

        // Enhance survivability by running this service in foreground when actively navigating
        try {
            val foregroundNotif = NavNotificationHelper.buildServiceNotification(this)
            startForeground(1001, foregroundNotif)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName == "com.google.android.apps.maps" || packageName == "com.waze") {
            Log.d(TAG, "Navigation notification removed for $packageName.")
            NavNotificationHelper.cancel(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
}
