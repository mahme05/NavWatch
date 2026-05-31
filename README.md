<div align="center">
  <img src="app/src/main/res/drawable/watchnav_logo.png" width="120" alt="WatchNav Logo" />
  <h1>WatchNav</h1>
  <p>Turn-by-turn navigation on your Nothing / CMF Watch — no root, no ADB, no subscriptions.</p>
  <p><em>Vibe coded with Claude Code.</em></p>
</div>

---

## What It Does

WatchNav bridges navigation directions from your phone to your **CMF Watch 3 Pro** (or any Nothing smartwatch). Choose how it gets directions:

| Mode | How |
|------|-----|
| **Notification Listener** | Intercepts Google Maps / Waze notifications silently. No API key needed. |
| **Directions API** | Fetches the full route from Google and auto-advances steps via GPS. |
| **Accessibility Service** | Reads the Maps directions panel directly from your screen. |

Whichever mode you pick, directions are forwarded to your watch via the **Nothing X** companion app over Bluetooth.

## How It Works (Notification mode)

```
Maps/Waze notification
  → WatchNav parses: instruction + distance + street
    → Re-posts as high-priority notification
      → Nothing X forwards → watch vibrates with turn info
```

## Features

- First-launch source picker with pros/cons breakdown per mode
- Notification Listener: debounced parsing, arrival detection, foreground keepalive
- Directions API: GPS step tracking, 30 m auto-advance threshold, mode chips (driving/walking/cycling)
- Accessibility Service: reads Maps view tree, no API key needed
- Live in-app monitor — see parsed directions in real time
- Trigger Test button — simulate a turn without opening Maps
- Survives device reboot via `BootReceiver`

## Requirements

| Requirement | Why |
|---|---|
| Android 8.0+ (API 26) | Notification channels |
| Google Maps or Waze | Navigation source |
| Nothing X app | Delivers notifications to watch over BT |
| CMF Watch 3 Pro / Nothing Watch | Receives the alerts |

## Setup (Notification mode — simplest)

1. Install and open WatchNav → pick **Notification Listener**
2. Tap **Grant Permissions** → enable WatchNav in Notification Access settings
3. Open **Nothing X** → Watch Settings → Notification Options → enable **WatchNav**
4. Set Google Maps battery to **Unrestricted** (Settings → Apps → Maps → Battery)
5. *(Recommended)* Set WatchNav battery to **Unrestricted** too
6. Start navigation in Maps or Waze — directions appear on your wrist

## Setup (Directions API mode)

1. Pick **Google Maps Directions API** on first launch
2. Follow the in-app guide to get a free API key from Google Cloud Console
3. Grant location permission when prompted
4. Enter a destination and tap **Go**

## Build

**Prerequisites:** [Android Studio](https://developer.android.com/studio) Hedgehog or newer

```bash
git clone https://github.com/mahme05/NavWatch.git
```

1. Open Android Studio → **Open** → select the project folder
2. Let Gradle sync finish
3. Run on a physical device (notification listener services don't work on emulators)

## Project Structure

```
app/src/main/java/com/watchnav/com/
├── MainActivity.kt             # Screen router + Dashboard UI
├── SourceSelectionScreen.kt    # First-launch mode picker
├── ApiKeySetupScreen.kt        # Google API key guided setup
├── ApiNavigationScreen.kt      # GPS-tracked Directions API navigation
├── AccessibilityGuideScreen.kt # One-time accessibility mode guide
├── NavListenerService.kt       # Notification interception + parsing
├── NavAccessibilityService.kt  # Accessibility service (Maps view tree reader)
├── NavNotificationHelper.kt    # Notification builder + StateFlow state
├── NavDirection.kt             # Data class: instruction, distance, street, action
├── DirectionsModels.kt         # Moshi data classes for Directions API
├── DirectionsApiService.kt     # Retrofit service + haversine + helpers
└── BootReceiver.kt             # Restarts listener on device boot
```

## Permissions

| Permission | Purpose |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read navigation notifications |
| `POST_NOTIFICATIONS` | Post alerts for watch forwarding |
| `FOREGROUND_SERVICE` | Keep service alive during navigation |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |
| `ACCESS_FINE_LOCATION` | GPS tracking for API mode |
| `BIND_ACCESSIBILITY_SERVICE` | Read Maps UI for accessibility mode |

## License

MIT
