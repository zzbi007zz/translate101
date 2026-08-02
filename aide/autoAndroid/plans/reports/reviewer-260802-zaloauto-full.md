## Code Review: Zalo Auto Sender -- Full Greenfield Review

**Scope:** 38 Kotlin files + 1 XML config + build.gradle + proguard
**LOC:** ~1,900 across all files
**Reviewed:** 2026-08-02

---

### Overall Assessment

Clean architecture with proper layering. KISS/YAGNI compliant -- no DI framework, no unnecessary abstractions. AccessibilityService correctly scoped to `com.zing.zalo` only. The `AutomationEngine` singleton with `@Volatile` + `synchronized` correctly guards concurrent automation. **However, there is one critical alarm-scheduling bug that breaks multi-message scheduling and a release-build crash risk due to missing ProGuard rules.** Both are blockers for production.

---

## CRITICAL (Blocking)

### C1. Single PendingIntent requestCode causes alarm collision -- only one message can be scheduled

**File:** `service/AlarmScheduler.kt`, lines 29-31

```kotlin
val pendingIntent = PendingIntent.getBroadcast(
    context,
    0,  // <-- HARDCODED requestCode
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

All alarms use `requestCode = 0`. With `FLAG_UPDATE_CURRENT`, scheduling message B **silently overwrites** message A's alarm. Only the most recently scheduled message will ever fire. This is a data-loss bug -- user schedules 3 messages, only the last one ever sends.

Additionally, `cancelMessage()` also uses `requestCode = 0` with `FLAG_NO_CREATE`. Canceling any message attempts to cancel the single shared PendingIntent.

**Fix:** Use `messageId.toInt()` as requestCode in both `scheduleMessage()` (line 29) and `cancelMessage()` (line 48):

```kotlin
PendingIntent.getBroadcast(context, messageId.toInt(), intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
```

---

### C2. Release builds will crash -- empty ProGuard rules with R8 enabled

**File:** `app/proguard-rules.pro` (empty), `app/build.gradle.kts` line 22 (`isMinifyEnabled = true`)

With R8 enabled and no keep rules:

| Component | What breaks |
|-----------|-------------|
| Room entities | Fields renamed; Room generated code can't find columns |
| Room DAOs | `@Dao` interfaces stripped; database builder fails |
| kotlinx.serialization | `@Serializable` route classes (`DetailRoute`) params renamed; `toRoute()` throws |
| DataStore preferences | Keys/proto content obfuscated |

**Fix:** Add these minimum keep rules:

```proguard
# Room
-keep class com.example.zaloauto.data.db.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.example.zaloauto.**$$serializer { *; }
-keepclassmembers class com.example.zaloauto.** {
    *** Companion;
}

# AccessibilityService (keeps instance field names for reflection-free access)
-keep class com.example.zaloauto.service.accessibility.** { *; }
```

---

### C3. BootReceiver is `android:enabled="false"` -- pending messages lost after reboot

**File:** `AndroidManifest.xml`, line 66

```xml
<receiver android:name=".service.BootReceiver"
    android:enabled="false">  <!-- always disabled -->
```

BootReceiver contains proper reschedule logic but is never triggered. After device reboot, all pending messages are permanently lost.

**Fix:** Change to `android:enabled="true"` or implement programmatic toggle based on whether pending messages exist.

---

## HIGH Priority

### H1. `runBlocking` on foreground service main thread

**File:** `service/AutomationForegroundService.kt`, line 64

```kotlin
val message = runCatching {
    kotlinx.coroutines.runBlocking { repo.getById(messageId) }
}.getOrNull()
```

`Service.onStartCommand()` runs on main thread. `runBlocking` blocks it. Fragile -- could contribute to ANR under DB contention.

**Fix:** Launch coroutine for full `executeAutomation` flow, post UI operations to `mainLooper`:

```kotlin
private fun executeAutomation(messageId: Long) {
    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        val message = repo.getById(messageId)
        if (message == null) { stopSelf(); return@launch }
        withContext(Dispatchers.Main) {
            // launch Zalo, delegate, etc.
        }
    }
}
```

---

### H2. Duplicate `ZaloNodeFinder` instances -- one unused

**Files:** `ZaloAutomationService.kt:14`, `ZaloAutomationSteps.kt:22`

`ZaloAutomationService` creates `val nodeFinder = ZaloNodeFinder()` (line 14) that is **never used**. `ZaloAutomationSteps` creates its own independent instance (line 22). Misleading and wastes memory.

**Fix:** Delete `nodeFinder` from `ZaloAutomationService` or pass it into `ZaloAutomationSteps` as a constructor parameter.

---

### H3. Recursive tree traversal -- StackOverflow risk on deep Zalo layouts

**File:** `service/accessibility/ZaloNodeFinder.kt`, `collectByText()` (line 40), `findFirst()` (line 126)

Both use recursion to walk AccessibilityNodeInfo tree. Deep Zalo layouts (nested RecyclerViews, ViewPagers) can exceed default stack depth.

**Fix:** Replace with iterative DFS:

```kotlin
private fun findFirst(
    root: AccessibilityNodeInfo,
    predicate: (AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.addLast(root)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (predicate(node)) return node
        for (i in (0 until node.childCount).reversed()) {
            node.getChild(i)?.let { stack.addLast(it) }
        }
    }
    return null
}
```

---

### H4. `notificationTimeout: 100` -- aggressive polling at 10Hz

**File:** `res/xml/accessibility_config.xml`, line 10

100ms polling increases CPU/battery usage. The automation uses polling-based loops, not event callbacks, so the event frequency doesn't help responsiveness.

**Recommendation:** Bump to 200-300ms unless testing shows 100ms is needed.

---

## MEDIUM Priority

### M1. `onAccessibilityEvent` no-op despite subscribing to 4 event types

**File:** `ZaloAutomationService.kt:25` + `accessibility_config.xml:5`

Config subscribes to `typeWindowStateChanged|typeWindowContentChanged|typeViewFocused|typeViewClicked` but `onAccessibilityEvent` is empty. All logic is polling-based. Inconsistent -- wastes event dispatch overhead.

**Fix:** Remove unnecessary event types from config, keeping only what's actually needed for window detection.

---

### M2. `DetailViewModel.loadMessage()` -- isLoading false only after logs arrive

**File:** `ui/screens/detail/DetailViewModel.kt`, lines 30-39

Two parallel coroutines: one loads the message, one collects logs. `isLoading = false` is only set in the logs collector. If message loads faster, UI still shows spinner. If `loadMessage` called repeatedly, multiple flows collect into same state.

**Fix:** Set `isLoading = false` in the message coroutine too. Track load jobs and cancel on re-entry.

---

### M3. `ZaloElementIds.forInstalledVersion()` called 3x per automation run

**File:** `ZaloAutomationSteps.kt` -- `stepFindSearch()`, `stepWaitChat()`, `stepTapSend()`

Each call does a binder IPC (`PackageManager.getPackageInfo()`). Called 3x per send.

**Fix:** Cache the result once at the top of `execute()`.

---

### M4. No max input length validation

**File:** `ui/screens/home/HomeViewModel.kt`

No limits on `recipient` or `messageText`. Zalo has practical limits (~5000 chars). Could cause silent `ACTION_SET_TEXT` failures in accessibility layer.

**Fix:** Add length validation in `scheduleMessage()`.

---

### M5. Hardware "granted" accessibility status chip

**File:** `ui/screens/home/HomeScreen.kt`, line 71

```kotlin
PermissionStatusChip(label = "Accessibility", isGranted = true, ...)
```

Fake status always shows green. Should check `ZaloAutomationService.instance != null`.

---

### M6. Unused import in TemplatesScreen

**File:** `ui/screens/templates/TemplatesScreen.kt`, line 18

`import com.example.zaloauto.ui.components.StatusChip` is unused.

---

### M7. No edge-case scout was run prior to this review

Recommend running `/ck:scout` focused on `AlarmScheduler`, `ZaloAutomationSteps` recycling guarantees, and `AutomationForegroundService` timing on API 34+.

---

## LOW Priority

### L1. Hardcoded `"com.zing.zalo"` in 3 locations

`ZaloElementIds.kt:44`, `AutomationForegroundService.kt:222`, `accessibility_config.xml:4`. Extract to constant.

### L2. `android:allowBackup="true"` in manifest

Room DB included in auto-backup. Non-sensitive data, but worth noting.

### L3. Unused imports in `UserPreferencesRepository.kt`

`longPreferencesKey` and `stringPreferencesKey` imported but unused (lines 8-9).

### L4. Redundant deprecated keyguard check for API < 26

`AutomationForegroundService.kt:109-120`. Since `minSdk = 26`, this deprecated path is dead code. `ScreenWakeActivity` handles keyguard dismissal.

### L5. `private set` on `ZaloAutomationService.instance` var

Cosmetic: `getInstance()` could be a Kotlin property with custom getter instead of explicit function.

---

## Positive Observations

1. **AccessibilityService scoped to `com.zing.zalo`** -- most important security property, correctly set
2. **AutomationEngine concurrency** -- `@Volatile + synchronized` pattern guards concurrent automation correctly
3. **Error categorization** -- `TRANSIENT` vs `TERMINAL` with differentiated retry logic
4. **WakeLock cleanup** -- `onDestroy()` checks `::wakeLock.isInitialized && wakeLock.isHeld` before release
5. **Exact alarm check** -- `canScheduleExactAlarms()` guards against runtime failures
6. **Empty states** -- all screens handle empty data with descriptive messages
7. **Past-time validation** -- `HomeViewModel` rejects scheduled times in the past
8. **Retry logic** -- up to 2 retries with 30s delay for transient failures
9. **Room DB singleton** -- double-checked locking with `@Volatile` is correct
10. **Compose lifecycle** -- `collectAsStateWithLifecycle()` used everywhere
11. **Navigation** -- `launchSingleTop + restoreState` prevents duplicate destinations
12. **Node recycling** -- `ZaloNodeFinder` conscientiously recycles non-matching nodes
13. **Version-aware element IDs** -- `ZaloElementIds` maps Zalo versions to element IDs with graceful fallback

---

## Prioritized Action Items

| # | Issue | Severity |
|---|-------|----------|
| 1 | C1 -- Fix PendingIntent requestCode from `0` to `messageId.toInt()` | CRITICAL |
| 2 | C2 -- Add ProGuard/R8 keep rules for Room, serialization | CRITICAL |
| 3 | C3 -- Enable BootReceiver or implement programmatic toggle | CRITICAL |
| 4 | H1 -- Replace runBlocking with coroutine in AutomationForegroundService | HIGH |
| 5 | H2 -- Remove duplicate ZaloNodeFinder or pass shared reference | HIGH |
| 6 | H3 -- Replace recursive tree traversal with iterative DFS | HIGH |
| 7 | H4 -- Consider bumping notificationTimeout to 200ms | HIGH |
| 8 | M1-M7 -- Address as time allows, starting with M5 (fake accessibility status) | MEDIUM |
| 9 | L1-L5 -- Opportunistic cleanup | LOW |

---

## Metrics

| Metric | Value |
|--------|-------|
| Files reviewed | 38 Kotlin + 3 config |
| Total LOC | ~1,900 |
| Critical issues | 3 |
| High issues | 4 |
| Medium issues | 7 |
| Low issues | 5 |
| Positive observations | 13 |

---

## Unresolved Questions

1. No `RoomDatabase.Callback` for seeding default templates -- intentional?
2. `ZaloAutomationService.onAccessibilityEvent` is a no-op -- is there a plan to add event-driven steps?
3. No test files found. Are tests planned in a separate module?
4. `autoSend` DataStore preference is collected in `SettingsViewModel` but not consumed by any automation flow -- where is the "skip confirmation" behavior?
5. `ScreenWakeActivity` finishes itself immediately after `requestDismissKeyguard` (which is async) -- does this race with keyguard dismissal on some devices?

**Status:** DONE
**Summary:** Comprehensive review of 41 files found 3 critical issues (alarm collision bug, missing ProGuard rules, disabled BootReceiver), 4 high issues, 7 medium, 5 low. 13 positive observations.
