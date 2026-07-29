package com.watchnav.com

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Master on/off switch for the navigation bridge.
 *
 * When off, WatchNav ignores Maps/Waze activity entirely — nothing is parsed,
 * nothing is posted to the watch — until the user turns it back on.
 * Backed by SharedPreferences so the choice survives reboots, and exposed as a
 * StateFlow so both the dashboard and the running services observe the same value.
 */
object BridgeState {

    private const val PREFS_NAME = "watchnav_prefs"
    private const val KEY_ENABLED = "bridge_enabled"

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    @Volatile
    private var loaded = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Current value, loading it from disk on first access. Services call this on
     * every event, so the read is cached in memory after the first hit.
     */
    fun isEnabledNow(context: Context): Boolean {
        if (!loaded) {
            _isEnabled.value = prefs(context).getBoolean(KEY_ENABLED, true)
            loaded = true
        }
        return _isEnabled.value
    }

    /**
     * Persists the new value and notifies observers. Turning the bridge off also
     * clears any alert already sitting on the watch.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        loaded = true
        _isEnabled.value = enabled
        if (!enabled) {
            NavNotificationHelper.cancel(context)
        }
    }
}
