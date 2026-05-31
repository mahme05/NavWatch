# WatchNav

Bridge turn-by-turn navigation from Google Maps or Waze to your **CMF Watch 3 Pro** (or any Nothing smartwatch) — no root, no ADB, no subscriptions.

## How It Works

WatchNav runs a background `NotificationListenerService` that intercepts navigation notifications from Maps/Waze, parses the turn instruction and distance, then re-posts a high-priority notification that the **Nothing X** companion app forwards to your watch over Bluetooth.

```
Maps/Waze notification
  → WatchNav parses: instruction + distance + street
    → Re-posts notification
      → Nothing X forwards → watch vibrates with turn info
```

## Features

- Supports Google Maps and Waze
- Parses turn instruction, distance (`m`, `km`, `ft`, `mi`), and street name
- Live in-app monitor to preview parsed navigation data
- Test mode — simulate a turn without opening Maps
- Survives device reboot via `BootReceiver`
- Runs as foreground service during active navigation for reliability

## Requirements

| Requirement | Why |
|---|---|
| Android 8.0+ | Notification channels |
| Google Maps or Waze | Navigation source |
| Nothing X app | Delivers notifications to watch over BT |
| CMF Watch 3 Pro / Nothing Watch | Receives the alerts |

## Setup

1. Install and open WatchNav
2. Tap **Grant Permissions** → enable WatchNav in Notification Access settings
3. Open **Nothing X** → Watch Settings → Notification Options → enable **WatchNav**
4. Set Google Maps battery optimization to **Unrestricted** (Settings → Apps → Maps → Battery)
5. *(Recommended)* Set WatchNav battery optimization to **Unrestricted** too
6. Start navigation in Maps or Waze — directions appear on your wrist

## Build

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (Hedgehog or newer)

```bash
git clone https://github.com/your-username/watchnav.git
```

1. Open Android Studio → **Open** → select the project folder
2. Let Gradle sync finish
3. Run on a physical device (notification listener services don't work on emulators)

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt          # Compose UI dashboard
├── NavListenerService.kt    # Notification interception + parsing
├── NavNotificationHelper.kt # Notification builder + StateFlow state
├── NavDirection.kt          # Data class: instruction, distance, street
└── BootReceiver.kt          # Restarts service on device boot
```

## Permissions

| Permission | Purpose |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read navigation notifications |
| `POST_NOTIFICATIONS` | Post alerts for watch forwarding |
| `FOREGROUND_SERVICE` | Keep service alive during navigation |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |

## License

MIT
