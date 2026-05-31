package com.watchnav.com

import android.app.Notification
import android.os.Build
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

        // Regex reused across calls — compiled once
        private val DISTANCE_DETECT_REGEX = Regex("""\d+\.?\d*\s?(?:m\b|km|ft|mi)""", RegexOption.IGNORE_CASE)
        private val DISTANCE_PREFIX_REGEX = Regex("""^In \d+(?:\.\d+)?\s?(?:m\b|km|ft|mi)[,\s]*""", RegexOption.IGNORE_CASE)
        private val DISTANCE_EXTRACT_REGEX = Regex("""In (\d+(?:\.\d+)?\s?(?:m\b|km|ft|mi))""", RegexOption.IGNORE_CASE)
        private val DISTANCE_FALLBACK_REGEX = Regex("""(\d+(?:\.\d+)?\s?(?:m\b|km|ft|mi))""", RegexOption.IGNORE_CASE)
        private val STREET_CLEAN_REGEX = Regex(
            """^(?:""" +
            """Turn\s+(?:left|right|slight\s+left|slight\s+right|sharp\s+left|sharp\s+right)\s+(?:onto|on)\s+|""" +
            """Head\s+(?:north|south|east|west|northeast|northwest|southeast|southwest)\s+(?:on|towards|toward)\s+|""" +
            """Continue\s+onto\s+|""" +
            """Merge\s+onto\s+|""" +
            """Keep\s+(?:left|right)\s+(?:to\s+stay\s+on\s+|onto\s+)?|""" +
            """Take\s+the\s+[^\s]+\s+exit\s+(?:onto\s+)?|""" +
            """Exit\s+onto\s+|""" +
            """Slight\s+(?:left|right)\s+(?:onto\s+)?|""" +
            """Sharp\s+(?:left|right)\s+(?:onto\s+)?""" +
            """)(.+)""",
            RegexOption.IGNORE_CASE
        )
    }

    private var isForegroundStarted = false

    // Debounce: skip re-posting the same instruction within 5 seconds
    private var lastInstruction = ""
    private var lastPostedMs = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "Notification listener connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        isForegroundStarted = false
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

        Log.d(TAG, "Notification from $packageName — Title='$title', Text='$text'")

        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()
        val combined = "$lowerTitle $lowerText"

        // 1. Filter: must be a navigation notification
        val keywords = listOf(
            "turn", "head", "continue", "merge", "exit",
            "arrive", "arrived", "destination", "roundabout", "keep", "take"
        )
        val isNavNotification = keywords.any { combined.contains(it) }
                || DISTANCE_DETECT_REGEX.containsMatchIn(combined)

        if (!isNavNotification) {
            Log.d(TAG, "Filtered out: no nav keywords or distance pattern.")
            return
        }

        // 2. Arrival detection — clear state and stop
        if (combined.contains("arrived") || combined.contains("you have arrived")) {
            Log.d(TAG, "Arrived at destination. Clearing navigation.")
            NavNotificationHelper.cancel(this)
            if (isForegroundStarted) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundStarted = false
            }
            return
        }

        // 3. Parse distance
        var distance = ""
        val distanceMatch = DISTANCE_EXTRACT_REGEX.find(title) ?: DISTANCE_EXTRACT_REGEX.find(text)
        if (distanceMatch != null) {
            distance = distanceMatch.groupValues[1]
        } else {
            val fallbackMatch = DISTANCE_FALLBACK_REGEX.find(title) ?: DISTANCE_FALLBACK_REGEX.find(text)
            if (fallbackMatch != null) distance = fallbackMatch.groupValues[1]
        }

        // 4. Extract instruction — strip "In 300 m," prefix from title
        var instruction = title.replace(DISTANCE_PREFIX_REGEX, "").trim()
        if (instruction.isEmpty()) {
            instruction = if (text.contains("·")) text.split("·").first().trim() else text.trim()
        }
        if (instruction.isBlank()) instruction = "Continue"

        // 5. Extract street name — strip leading direction verb
        val rawStreet = if (text.contains("·")) text.split("·").first().trim() else text.trim()
        var street = rawStreet
        val cleanMatch = STREET_CLEAN_REGEX.find(rawStreet)
        if (cleanMatch != null && cleanMatch.groupValues[1].isNotBlank()) {
            street = cleanMatch.groupValues[1].trim()
        }
        if (street.isBlank()) street = text

        val lowerInst = instruction.lowercase()
        val action = when {
            lowerInst.contains("sharp left") -> "Sharp Left"
            lowerInst.contains("slight left") -> "Slight Left"
            lowerInst.contains("sharp right") -> "Sharp Right"
            lowerInst.contains("slight right") -> "Slight Right"
            lowerInst.contains("left") -> "Left"
            lowerInst.contains("right") -> "Right"
            else -> ""
        }

        val direction = NavDirection(
            instruction = instruction,
            distance = distance,
            street = street,
            action = action
        )

        // 6. Debounce — skip identical instruction within 5 s
        val now = System.currentTimeMillis()
        if (direction.instruction == lastInstruction && now - lastPostedMs < 5_000L) {
            Log.d(TAG, "Debounced: same instruction '${direction.instruction}' within 5s.")
            return
        }
        lastInstruction = direction.instruction
        lastPostedMs = now

        Log.d(TAG, "Parsed: $direction")
        NavNotificationHelper.post(this, direction)

        // 7. Start foreground once — keeps service alive during active navigation
        if (!isForegroundStarted) {
            try {
                val foregroundNotif = NavNotificationHelper.buildServiceNotification(this)
                startForeground(NavNotificationHelper.SERVICE_NOTIFICATION_ID, foregroundNotif)
                isForegroundStarted = true
            } catch (e: Exception) {
                Log.e(TAG, "startForeground failed: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        if (MAPS_PACKAGES.contains(sbn.packageName)) {
            Log.d(TAG, "Navigation notification removed for ${sbn.packageName}.")
            NavNotificationHelper.cancel(this)
            if (isForegroundStarted) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundStarted = false
            }
        }
    }
}
