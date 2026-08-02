# Zalo Auto Sender

Android app that schedules and automatically sends Zalo messages at a chosen time. The app drives Zalo's UI through an `AccessibilityService`, so no Zalo API or credentials are required — it performs the same taps and text entry a human would.

**Scope note:** The accessibility service is scoped to `com.zing.zalo` only (see `accessibility_config.xml`). It reads and interacts with Zalo's UI exclusively.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Navigation | Navigation Compose (type-safe routes) | 2.8.5 |
| Persistence | Room | 2.6.1 |
| Preferences | DataStore Preferences | 1.1.1 |
| Scheduling | AlarmManager (`setExactAndAllowWhileIdle`) | SDK built-in |
| Automation | AccessibilityService + `AccessibilityNodeInfo` | SDK built-in |
| Background work | Foreground Service (`specialUse`) | SDK built-in |
| Serialization | kotlinx-serialization | 1.7.3 |
| Build | AGP / KSP | 8.7.3 / 2.0.21-1.0.28 |

## Architecture Summary

Single-module app (`:app`), package root `com.example.zaloauto`, organized by concern:

```
com.example.zaloauto/
├── data/          # Room DB, DAOs, entities, repositories, DataStore prefs
├── service/       # Alarm scheduling, FGS, boot recovery, automation engine
│   └── accessibility/   # Accessibility service, node finder, step machine
├── ui/            # Compose screens, navigation, theme, components
├── MainActivity.kt      # Single-activity entry, bottom navigation
├── ZaloAutoApp.kt       # Application: DB + DataStore + notification channels
└── ScreenWakeActivity.kt  # Keyguard-dismiss activity (declared, not yet wired)
```

Flow: the user schedules a message in the UI → it is persisted in Room and an exact alarm is registered → when the alarm fires it starts a foreground service → the service wakes the device and launches Zalo → the accessibility service performs the send steps → the result is written back to Room and surfaced as a notification.

See [system-architecture.md](./system-architecture.md) for the full diagram and class roles.

## Build Instructions

1. Open the project folder (`app/`) in **Android Studio** (Ladybug or newer).
2. Wait for Gradle sync to complete.
3. Select the **app** run configuration and a device/emulator.
4. Run. The project uses Kotlin 2.0.21 and Java 17 (set in `build.gradle.kts`).

Key configuration (`app/build.gradle.kts`):

| Property | Value |
|----------|-------|
| `applicationId` | `com.example.zaloauto` |
| `compileSdk` | 35 |
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` | 35 |
| `versionName` | 1.0.0 |
| JVM target | 17 |

> Release builds enable R8 (`isMinifyEnabled = true`) and use the default optimize ProGuard file.

## Required Permissions

These are declared in `AndroidManifest.xml` and granted at runtime on Android 13+ or via Settings:

| Permission | Purpose |
|-----------|---------|
| `SCHEDULE_EXACT_ALARM` | Fire automation at the exact scheduled time |
| `POST_NOTIFICATIONS` | Show send-result notifications (runtime, Android 13+) |
| `FOREGROUND_SERVICE` | Run the automation foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Declare the service as `specialUse` (`automated_message_sending`) |
| `SYSTEM_ALERT_WINDOW` | Overlay capability (checked in Settings, not currently used at runtime) |
| `WAKE_LOCK` | Keep the screen on while automation runs |
| `DISABLE_KEYGUARD` | Dismiss the non-secure lock screen |
| `RECEIVE_BOOT_COMPLETED` | Reschedule pending alarms after reboot |

### Required setup steps

1. **Accessibility service** — enable "Zalo Auto Sender" in Accessibility settings. Required for the app to drive Zalo's UI.
2. **Exact alarm permission** — grant "Alarms & reminders" for the app (needed for precise scheduling).
3. **Notification permission** — allow on Android 13+ to receive send results.
4. **Don't kill the app** — for reliable background firing, exclude the app from battery optimization if the OS kills it.

The Settings screen (`SettingsScreen` / `SettingsViewModel`) shows live status for accessibility, overlay, exact-alarm, and notification permissions, and deep-links to the matching system settings.

## How It Works

1. **Schedule** — `HomeViewModel.scheduleMessage()` validates input and calls `MessageRepository.scheduleMessage()`, which inserts a `ScheduledMessageEntity` (status `PENDING`) into Room.
2. **Alarm** — `AlarmScheduler.scheduleMessage()` registers an exact alarm (`setExactAndAllowWhileIdle`) keyed by message ID. The `PendingIntent` targets `AlarmReceiver` with the message ID as extra and a `zalo-auto://msg/<id>` URI.
3. **FGS** — When the alarm fires, `AlarmReceiver` starts `AutomationForegroundService` (type `specialUse`). The service acquires a wake lock (60s max), loads the message from Room, and updates its ongoing notification.
4. **Launch Zalo** — The service launches Zalo (`com.zing.zalo`) via `PackageManager.getLaunchIntentForPackage()`, then waits ~500ms.
5. **Accessibility** — `AutomationEngine.start()` reserves the run, then `ZaloAutomationSteps.execute()` runs the step machine on the accessibility service: wait for Zalo window → open search → search the recipient → select the user → wait for the chat to load → verify the chat header → type the message → verify text → tap send.
6. **Result** — On success the message is marked `SENT`; on failure it is marked `FAILED` (or retried up to 2 times for transient errors by rescheduling 30s later). A log row is inserted into `message_logs` and a high-priority notification is posted.
7. **Cleanup** — Wake lock released, foreground service stopped.

### Boot recovery

`BootReceiver` re-registers all `PENDING` alarms after reboot. Messages more than 5 minutes past due are marked `FAILED` ("Stale: over 5 min past due after boot") instead of firing.

### Retry policy

- `ErrorCategory.TRANSIENT` errors (timeouts, root-window null) with `retryCount < 2` → reschedule alarm for +30s and increment retry count.
- `ErrorCategory.TERMINAL` errors (user not found, login required, header mismatch, Zalo not installed) → mark `FAILED` immediately.

### Failure handling

Common failure paths handled explicitly: Zalo not installed, accessibility service not enabled, device locked with a secure lock screen (PIN/pattern — cannot be auto-dismissed), user not found, chat header mismatch, message-text verification failure.

## Key Files

| File | Role |
|------|------|
| `service/AlarmScheduler.kt` | Exact-alarm scheduling/cancellation |
| `service/AlarmReceiver.kt` | Alarm broadcast → start FGS |
| `service/AutomationForegroundService.kt` | Orchestrates wake, launch, delegate, result |
| `service/AutomationEngine.kt` | Singleton bridge, guards concurrent runs |
| `service/BootReceiver.kt` | Re-registers alarms after reboot |
| `service/accessibility/ZaloAutomationService.kt` | Accessibility service instance |
| `service/accessibility/ZaloAutomationSteps.kt` | Send step machine + error categorization |
| `service/accessibility/ZaloNodeFinder.kt` | Node-tree traversal helpers |
| `service/accessibility/ZaloElementIds.kt` | Per-version UI element ID mapping |

## Related Documentation

- [system-architecture.md](./system-architecture.md) — flow diagram, package structure, class roles
- [code-standards.md](./code-standards.md) — naming, package organization, Kotlin conventions
