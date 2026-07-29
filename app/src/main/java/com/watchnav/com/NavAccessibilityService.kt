package com.watchnav.com

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavAccessibilityService : AccessibilityService() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    override fun onServiceConnected() {
        _isRunning.value = true
        Log.d("NavAccessibilityService", "Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.google.android.apps.maps") return

        // Master switch off — read nothing, post nothing.
        if (!BridgeState.isEnabledNow(applicationContext)) return

        val root = rootInActiveWindow ?: return
        val stepNodes = mutableListOf<String>()
        traverseForSteps(root, stepNodes)

        if (stepNodes.isNotEmpty()) {
            val instruction = stepNodes.firstOrNull() ?: return
            val distance = stepNodes.getOrNull(1) ?: ""
            val direction = NavDirection(
                instruction = instruction,
                distance = distance,
                street = stepNodes.getOrNull(2) ?: "",
                action = inferAction(instruction)
            )
            NavNotificationHelper.post(applicationContext, direction)
        }
    }

    private fun traverseForSteps(node: AccessibilityNodeInfo?, results: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text.length > 3) {
            results.add(text)
        }
        for (i in 0 until node.childCount) {
            traverseForSteps(node.getChild(i), results)
        }
    }

    private fun inferAction(instruction: String): String {
        val lower = instruction.lowercase()
        return when {
            lower.contains("sharp left") -> "Sharp Left"
            lower.contains("slight left") -> "Slight Left"
            lower.contains("sharp right") -> "Sharp Right"
            lower.contains("slight right") -> "Slight Right"
            lower.contains("left") -> "Left"
            lower.contains("right") -> "Right"
            lower.contains("roundabout") -> "Roundabout"
            else -> ""
        }
    }

    override fun onInterrupt() {
        _isRunning.value = false
        Log.d("NavAccessibilityService", "Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
    }
}
