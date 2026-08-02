# Researcher 02: Android Scheduling & Background Work Patterns

**Date:** 2026-08-02
**Sources:** 15+ official developer.android.com pages (alarms, FGS, notification channels, Room, DataStore, Navigation, ViewModel, accessibility, BAL, WorkManager, Android 14/15 changes)

---

## 1. Scheduling: AlarmManager (Recommended)

### Verdict
**`AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` + BroadcastReceiver + Foreground Service.** WorkManager is NOT for exact-time triggering.

### AlarmManager vs WorkManager

| Dimension | AlarmManager (exact) | WorkManager |
|---|---|---|
| Exact firing | Yes (permission-gated) | No (batching, 15-min min periodic) |
| Fires in Doze | `setExactAndAllowWhileIdle` / `setAlarmClock` fire; `setExact` deferred | Deferred until maintenance window |
| Persists across reboot | No — must re-register via BootReceiver | Yes — auto-rescheduled |
| Permission | `SCHEDULE_EXACT_ALARM` (API 31+) | None |
| FGS bg-start exemption | Yes | No |

### Doze Throttling
- `setExactAndAllowWhileIdle` limited to **1 fire per 9 min per app** while in Doze
- Once first alarm fires and wakes device, Doze is interrupted — subsequent alarms fire normally
- Set `typeAlarmClock` if multiple messages within 9 min needed (shows alarm icon)

---

## 2. Exact Alarm Permissions (Android 12+)

| Permission | Granted | Revocable | Play Policy | Fit |
|---|---|---|---|---|
| `SCHEDULE_EXACT_ALARM` | By user via Settings; denied by default on API 33+ | Yes | Standard special-access | **Use this** |
| `USE_EXACT_ALARM` | Auto at install | No | Alarm-clock/calendar apps only | Not eligible |

### Flow
```kotlin
if (!alarmManager.canScheduleExactAlarms()) {
    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
}
```
Listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` to reschedule.

---

## 3. Persisting Alarms Across Reboot (BootReceiver)
- `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>`
- `ACTION_BOOT_COMPLETED` is exempt from implicit broadcast restrictions
- Enable receiver only when alarms exist; disable when none remaining
- In `onReceive`, re-schedule all PENDING alarms from Room DB

---

## 4. Foreground Service Architecture

### Required: Separate FGS for automation execution (not just AccessibilityService)
- `AccessibilityService` is system-bound — not controllable by alarm directly
- FGS started by `AlarmReceiver` via exact-alarm exemption
- FGS type: `specialUse` with `FOREGROUND_SERVICE_SPECIAL_USE` permission and property explanation

```xml
<service android:name=".SendMessageForegroundService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Schedules automated Zalo message delivery via AccessibilityService" />
</service>
```

### Android 14+ Requirements
- All FGS must declare `foregroundServiceType` + matching `FOREGROUND_SERVICE_*` permission
- Must call `startForeground()` within 5 seconds of start or crash
- `dataSync`/`mediaProcessing` types capped at 6h/24h background; `specialUse` is NOT capped but should be short-lived

---

## 5. Notification Channels

| Channel ID | Name | Importance | Purpose |
|---|---|---|---|
| `channel_fgs` | "Automation running" | `IMPORTANCE_LOW` | Ongoing FGS notification |
| `channel_task_status` | "Scheduled messages" | `IMPORTANCE_HIGH` | Per-task status |
| `channel_accessibility` | "Service needed" | `IMPORTANCE_DEFAULT` | Reminder to enable accessibility |

Created in `Application.onCreate()`. API 26+ mandatory.

---

## 6. Data Storage

### Room (for structured, queryable data)
Entities: `ScheduledMessage`, `MessageLog`, `Template`
DAOs with `suspend fun` for writes, `Flow<List<>>` for reactive reads
KSP2 for Room compiler (Kotlin 2.x)

### DataStore (for config)
Preferences DataStore for: default recipient, last-used options, onboarding flags
Expose via repository → ViewModel StateFlow pattern

---

## 7. UI Architecture

### Compose Navigation (single activity)
Screens: Home/Schedule, Scheduled List, Templates, Settings
Type-safe routes via `@Serializable` + Kotlin serialization plugin

### ViewModel + StateFlow
```kotlin
class ScheduleViewModel(
    private val repo: MessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()
}
```
Consume via `collectAsStateWithLifecycle()`

---

## 8. Project Structure (Light MVVM)

```
app/src/main/java/com/example/zaloauto/
├── ZaloAutoApp.kt              // Application: channels, DI, Room init
├── data/
│   ├── db/                     // AppDatabase, DAOs, Entities
│   ├── repository/             // MessageRepository, ConfigRepository
│   ├── datastore/              // UserPreferencesRepository
│   └── scheduler/              // AlarmScheduler
├── service/
│   ├── AlarmReceiver.kt
│   ├── BootReceiver.kt
│   ├── PermissionChangedReceiver.kt
│   ├── AutomationForegroundService.kt
│   └── accessibility/
│       ├── ZaloAutomationService.kt
│       └── accessibility_config.xml
├── ui/
│   ├── navigation/             // NavGraph, routes
│   ├── screens/                // home, list, templates, settings + ViewModels
│   └── theme/
└── util/
```

---

## 9. Build Configuration

- **minSdk:** 26 (notification channels, Doze, requestDismissKeyguard)
- **targetSdk:** 35 (triggers SCHEDULE_EXACT_ALARM flow; Play requirement)
- **compileSdk:** 35
- **AGP:** 8.7+, **Gradle:** 8.9+, **JDK:** 17
- **Kotlin:** 2.0.x, KSP2 for Room
- Dependencies: Room, DataStore, Compose Navigation, kotlinx-serialization-json

---

## 10. End-to-End Flow

```
1. User schedules → Room insert + AlarmScheduler.setExactAndAllowWhileIdle
2. Alarm fires → AlarmReceiver.onReceive (fast path)
3. Receiver → startForegroundService(AutomationForegroundService)
4. FGS: startForeground → wake lock + screen wake → launch Zalo (SYSTEM_ALERT_WINDOW BAL exemption)
5. ZaloAutomationService: find recipient → type → send
6. FGS: update Room + post notification + stopSelf
7. Reboot → BootReceiver re-schedules from Room
```

---

## 11. Key Risks

| Risk | Severity | Mitigation |
|---|---|---|
| SCHEDULE_EXACT_ALARM denied by default | High | Guided settings flow |
| BAL blocking Zalo launch | High | SYSTEM_ALERT_WINDOW permission required |
| Play policy: accessibility automation | High (distribution) | Personal-use only; accessibility purpose declaration |
| Doze 9-min throttle | Medium | setAlarmClock for clusters |
| OEM FGS kill | Medium | Battery optimization exemption request |

**Status:** DONE_WITH_CONCERNS
**Concerns:** BAL for launching Zalo requires SYSTEM_ALERT_WINDOW — must validate on target devices. Accessibility-based automation faces Play policy scrutiny.
