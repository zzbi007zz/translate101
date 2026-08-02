# Phase 04: Scheduler & Background Services

**Priority:** P1 | **Status:** Completed

## Overview

Implement AlarmManager scheduling, BroadcastReceivers, BootReceiver, AutomationForegroundService, and the wake/lock/unlock flow needed to execute automation when the device is locked or asleep.

## Key Insights
- `setExactAndAllowWhileIdle(RTC_WAKEUP)` works through Doze
- Exact alarm is a documented FGS background-start exemption
- SYSTEM_ALERT_WINDOW provides BAL exemption for launching Zalo from background
- Device must be awake and unlocked for accessibility to read Zalo UI
- BootReceiver must re-schedule all PENDING alarms from Room DB

## Architecture

### Flow
```
Room (schedule) → AlarmScheduler → AlarmManager (fires) → AlarmReceiver
    → startForegroundService(AutomationForegroundService)
    → wake screen + dismiss keyguard + launch Zalo
    → signal ZaloAutomationService via AutomationEngine
    → ZaloAutomationService executes steps
    → AutomationForegroundService updates Room + notifies + stopSelf
```

### AlarmScheduler
```kotlin
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun scheduleMessage(messageId: Long, triggerAtMillis: Long): Boolean {
        if (!alarmManager.canScheduleExactAlarms()) return false
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message_id", messageId)
        }
        intent.data = Uri.parse("zalo-auto://msg/$messageId")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )
        return true
    }

    fun cancelMessage(messageId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, messageId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent?.cancel()
    }
}
```

### AlarmReceiver
```kotlin
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra("message_id", -1L)
        val serviceIntent = Intent(context, AutomationForegroundService::class.java).apply {
            putExtra("message_id", messageId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
```

### BootReceiver
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val repo = MessageRepository(AppDatabase.getInstance(context))
            val scheduler = AlarmScheduler(context)
            val pending = repo.getPendingMessages().first()
            // Re-schedule past-due PENDING messages within 5-min grace window
            val now = System.currentTimeMillis()
            pending.forEach { msg ->
                if (msg.scheduledAt > now - 300_000L) {
                    scheduler.scheduleMessage(msg.id, maxOf(msg.scheduledAt, now + 5_000L))
                } else {
                    repo.markFailed(msg.id, "Stale: over 5 min past due after boot")
                }
            }
        }
    }
}
```
BootReceiver starts `enabled="false"` in manifest; enabled when alarms exist, disabled when none.

### AutomationForegroundService
```kotlin
@AndroidEntryPoint // if Hilt; else manual
class AutomationForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val messageId = intent?.getLongExtra("message_id", -1L) ?: -1L
        executeAutomation(messageId)
        return START_NOT_STICKY
    }

    private fun executeAutomation(messageId: Long) {
        // 1. Acquire wake lock
        val pm = getSystemService<POWER_SERVICE>() as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "zalo_auto:send"
        )
        wakeLock.acquire(60_000L) // max 60s

        // 2. Launch Zalo via getLaunchIntentForPackage
        val launchIntent = packageManager.getLaunchIntentForPackage("com.zing.zalo")
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)

        // 3. Signal AutomationEngine to start
        AutomationEngine.start(messageId) { success, error ->
            updateDatabaseAndNotify(messageId, success, error)
            wakeLock.release()
            stopSelf()
        }
    }
}
```

### AutomationEngine (singleton)
```kotlin
object AutomationEngine {
    @Volatile private var isBusy: Boolean = false
    @Volatile private var currentMessageId: Long? = null
    @Volatile private var onComplete: ((Boolean, String?, ErrorCategory) -> Unit)? = null

    fun start(messageId: Long, callback: (Boolean, String?, ErrorCategory) -> Unit): Boolean {
        synchronized(this) {
            if (isBusy) return false
            isBusy = true
            currentMessageId = messageId
            onComplete = callback
        }
        ZaloAutomationService.instance?.let { service ->
            // Service reads messageId from this engine; FGS context holds Room refs
        }
        return true
    }

    fun complete(success: Boolean, error: String?, category: ErrorCategory = ErrorCategory.NONE) {
        synchronized(this) {
            onComplete?.invoke(success, error, category)
            currentMessageId = null
            onComplete = null
            isBusy = false
        }
    }
}
```

### Screen Wake & Keyguard
AccessibilityService needs the screen on and keyguard dismissed to read Zalo UI. From the FGS context:
- `PowerManager.ACQUIRE_CAUSES_WAKEUP` wakes screen (if device supports)
- `KeyguardManager.requestDismissKeyguard(activity, callback)` requires an activity; alternative: use a transparent activity in the FGS or a `FLAG_TURN_SCREEN_ON` activity
- If device has secure lock screen (PIN/pattern), keyguard CANNOT be dismissed programmatically → task will fail
- Document requirement: device should be unlocked or have no secure lock for scheduled sends

## Related Files

| File | Action | Path (under service/) |
|------|--------|------|
| AlarmScheduler.kt | create | service/AlarmScheduler.kt |
| AlarmReceiver.kt | create | service/AlarmReceiver.kt |
| BootReceiver.kt | create | service/BootReceiver.kt |
| AutomationForegroundService.kt | create | service/AutomationForegroundService.kt |
| AutomationEngine.kt | create | service/AutomationEngine.kt |

## Todo List
- [ ] Create AlarmScheduler with schedule/cancel methods
- [ ] Create AlarmReceiver
- [ ] Create BootReceiver (enabled only when alarms exist)
- [ ] Create AutomationForegroundService with wake lock + Zalo launch
- [ ] Create AutomationEngine singleton for cross-component communication
- [ ] Create transparent ScreenWakeActivity for keyguard dismissal on devices without secure lock
- [ ] Wire MessageRepository.updateStatus/insertLog calls

## Success Criteria
- Alarm fires at scheduled time and starts FGS
- FGS appears with ongoing notification in status bar
- FGS launches Zalo if SYSTEM_ALERT_WINDOW permission granted
- BootReceiver reschedules existing alarms after device restart

## Risk Assessment
- Doze 9-min throttle for rapid successive alarms → use setAlarmClock as fallback
- Secure lock screen blocks automation → document limitation; detect and abort gracefully
- OEM FGS kill → suggest user disable battery optimization for the app
- Zalo may take time to load: stepWaitForZaloWindow handles cold start with 15s timeout (moved to Phase 03)

## Red Team Fixes Applied
- **F2 (Critical):** `AutomationEngine` now uses `@Volatile isBusy` + `synchronized` guard; `start()` returns false if already executing
- **F3 (Critical):** BootReceiver uses `.first()` instead of `.collect()` for one-shot emission; adds 5-min grace window for in-flight PENDING messages
- **F7 (High):** PendingIntent uses `intent.data = Uri.parse("zalo-auto://msg/$messageId")` for disambiguation; requestCode set to 0
- **F7b (Low):** `cancelMessage` now calls `alarmManager.cancel()` before `pendingIntent?.cancel()` (correct order)
- **F12 (Medium):** Removed `PermissionChangedReceiver` — permissions checked via ActivityResultLauncher callbacks on Settings screen

## Next Steps
Phase 05: UI Implementation
