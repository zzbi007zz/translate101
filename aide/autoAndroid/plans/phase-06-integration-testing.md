# Phase 06: Integration & Polish

**Priority:** P1 | **Status:** Completed

## Overview

Wire all components together end-to-end. Add database update logic from automation results, implement retry mechanism, add proper error screens, and do final polish.

## Key Insights
- The full automation flow crosses 3 layers: UI (schedule) → Alarm/FGS (trigger) → Accessibility (execute) → DB & Notifications (result)
- All errors must be surfaced via Room + notification, since automation runs in background
- Retry: max 2 retries on failure (re-schedule alarm 30s later)

## Integration Tasks

### 1. End-to-End Wiring
- `HomeViewModel.schedule()` → `MessageRepository.scheduleMessage()` → returns messageId → `AlarmScheduler.scheduleMessage(id, time)`
- `AlarmReceiver` → `AutomationForegroundService.onStartCommand` → `AutomationEngine.start(id)` → `ZaloAutomationService.automator.execute(target, msg)` → `AutomationEngine.complete(success, error)`
- `AutomationForegroundService` callback → `MessageRepository.markSent(id)` or `markFailed(id, error)` → `AlarmScheduler.cancelMessage(id)` + status notification

### 2. Notification Updates
- PENDING → "Scheduled: message to [name] at [time]" (IMPORTANCE_LOW)
- SENT → "Sent: message to [name]" (IMPORTANCE_HIGH, auto-cancel)
- FAILED → "Failed: message to [name] — [error]" (IMPORTANCE_HIGH with tap-to-view-detail PendingIntent)

### 3. Auto-Retry on Failure (with Error Classification)
```kotlin
// In AutomationForegroundService callback
if (!success && category == ErrorCategory.TRANSIENT && retryCount < MAX_RETRIES) {
    alarmScheduler.scheduleMessage(messageId, System.currentTimeMillis() + 30_000)
    repository.incrementRetry(messageId)
} else {
    repository.markFailed(messageId, error ?: "Unknown error")
}
// TERMINAL errors (USER_NOT_FOUND, NOT_INSTALLED, NOT_LOGGED_IN, RECIPIENT_MISMATCH) skip retry
```

### 4. Initial Setup Wizard
When app first launches:
1. Step 1: "Enable Accessibility Service" → opens Settings, check enabled on return
2. Step 2: "Grant Display Over Other Apps" → opens overlay settings
3. Step 3: "Grant Exact Alarm Permission" → opens alarm settings
Only proceed to Home after all 3 granted.

### 5. Error States
- "Zalo not installed" → redirect to Play Store
- "Accessibility service disabled" → prompt to enable
- "Exact alarm permission denied" → prompt to grant
- "Overlay permission denied" → prompt to grant
- "Device has secure lock screen" → warning dialog

### 6. Edge Cases
- Scheduled time in the past → reject with toast
- Empty message text → disable Send button
- Empty recipient name → disable Send button
- Multiple messages to same person at same time → allow (different IDs)
- App process killed before alarm fires → BootReceiver handles re-schedule
- Message deleted from list while alarm pending → `MessageRepository.deleteAndCancelAlarm(id)` (cancels alarm BEFORE Room delete)

### 7. Final Polish
- Splash screen (Android 12+ SplashScreen API)
- App icon + name "Zalo Auto Sender"
- Minimum Compose animations (enter/exit transitions in NavHost)
- About screen with version
- Keep all files under 200 lines (per dev rules)

## Related Files (modified)

| File | Action |
|------|--------|
| AutomationForegroundService.kt | modify — add retry logic, DB update, notification dispatch |
| AlarmScheduler.kt | modify — add reschedule method |
| HomeViewModel.kt | modify — add validation, permission checks |
| HomeScreen.kt | modify — add setup wizard overlay |
| MainActivity.kt | modify — add SplashScreen API |
| string resources | create — all UI strings |

## Todo List
- [ ] Wire Home → Repository → Scheduler chain
- [ ] Wire Alarm → FGS → Engine → Automation chain
- [ ] Implement notification dispatch for all status changes
- [ ] Implement auto-retry (max 2 retries)
- [ ] Create initial setup wizard flow
- [ ] Add all error-state screens and permission checks
- [ ] Handle edge cases (past time, empty fields, duplicate, process kill)
- [ ] Add SplashScreen API
- [ ] Test full flow on device with Zalo installed
- [ ] Verify BootReceiver reschedules after reboot
- [ ] Verify all notification channels deliver correctly

## Success Criteria
- Full flow: schedule → alarm → launch Zalo → type → send → notification
- Failed sends retry automatically up to 2 times
- All permissions requested and handled gracefully
- Boot receiver restores alarms correctly
- No crashes in any edge case path

## Risk Assessment
- Zalo UI changes may require element ID updates → version map extensibility
- Secure lock screen limits automation → clear documentation for user
- Battery optimization kills FGS on some OEMs → guide user to whitelist

## Red Team Fixes Applied
- **F9 (High):** Retry only on TRANSIENT errors; TERMINAL errors skip retry completely
- **F10 (High):** Added `deleteAndCancelAlarm()` method — cancels alarm before Room delete
- **F5 (Critical):** Added Room Migration task: create `Migration(1, 2)` support — schema versioning uses auto-migration for column additions, manual migration blocks for breaking changes

## Next Steps
Release: sideload APK, test on real device with Zalo, iterate on element IDs.
