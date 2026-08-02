# Phase 03: Accessibility Automation Engine

**Priority:** P1 | **Status:** Completed

## Overview

Implement the AccessibilityService that drives Zalo UI automation: finding UI elements, clicking, typing, gesture dispatch, and a state machine for the full send-message flow.

## Key Insights
- AccessibilityService CANNOT be enabled programmatically — user must do it via Settings
- Node finding strategy: try resource-id first, then content-description/text, then structural heuristics
- `ACTION_SET_TEXT` on EditText node is most reliable for typing
- Send button may only appear after text is entered — poll with short delay
- Zalo UI element IDs change per version → maintain a version map

## Architecture

### File Structure (under service/accessibility/)
```
ZaloAutomationService.kt      — main service, receives events
ZaloNodeFinder.kt             — finds nodes by text/id/class with fallback chain
ZaloAutomationSteps.kt        — step-by-step automation actions (search, select, type, send)
ZaloElementIds.kt             — per-version resource ID mapping
automation_config.xml         — res/xml/accessibility_config.xml
```

### ZaloAutomationService
```kotlin
class ZaloAutomationService : AccessibilityService() {
    companion object {
        @Volatile var instance: ZaloAutomationService? = null // for FGS communication
    }
    val nodeFinder = ZaloNodeFinder()
    val automator = ZaloAutomationSteps(this)

    override fun onServiceConnected() {
        instance = this
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null }
}
```

### ZaloNodeFinder
Key methods:
- `findByText(root, text)`: matches exact or contains, visible nodes only
- `findById(root, id)`: resource-id lookup
- `findEditable(root, hintText?)`: find first visible EditText node; optional hint-text filtering
- `findClickableNear(root, text, dx, dy)`: find clickable node near a text node (for send button)
- `waitForNode(timeoutMs, predicate)`: poll root every 200ms until predicate matches or timeout
- `getRootSafely(service)`: get rootInActiveWindow, retry up to 3 times if null

All methods recycle non-returned nodes.

### ZaloAutomationSteps (State Machine)
```kotlin
enum class Step { IDLE, LAUNCHING, FINDING_SEARCH, SEARCHING, SELECTING_USER, WAITING_CHAT, TYPING, SENDING, DONE, FAILED }

class ZaloAutomationSteps(private val service: ZaloAutomationService) {
    private var currentStep = Step.IDLE
    private var error: String? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    fun execute(targetName: String, message: String, dryRun: Boolean = false, onComplete: (Boolean, String?, ErrorCategory) -> Unit) {
        if (job?.isActive == true) { onComplete(false, "Automation already in progress", ErrorCategory.TRANSIENT); return }
        job = scope.launch {
            try {
                withTimeout(AUTOMATION_TIMEOUT_MS) {
                    stepWaitForZaloWindow()
                    stepFindSearch()
                    stepSearchUser(targetName)
                    stepSelectUser(targetName)
                    stepWaitChat()
                    stepTypeMessage(message)
                    if (!dryRun) stepTapSend()
                    currentStep = Step.DONE
                    onComplete(true, null, ErrorCategory.NONE)
                }
            } catch (e: TimeoutCancellationException) {
                currentStep = Step.FAILED
                onComplete(false, "Automation timed out: $currentStep", ErrorCategory.TRANSIENT)
            } catch (e: Exception) {
                currentStep = Step.FAILED
                error = e.message
                val cat = categorizeError(e.message)
                onComplete(false, e.message, cat)
            }
        }
    }

    fun cancel() { job?.cancel(); scope.cancel() }

    private fun stepWaitForZaloWindow() {
        // Poll rootInActiveWindow until package matches com.zing.zalo, timeout 15s
        val deadline = SystemClock.uptimeMillis() + 15_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val root = service.rootInActiveWindow
            if (root?.packageName == "com.zing.zalo") return
            SystemClock.sleep(300)
        }
        throw TimeoutException("Zalo did not come to foreground")
    }

    private fun stepLaunchZalo() { ... }
    private fun stepFindSearch() { ... }
    private fun stepSearchUser(name: String) { ... }
    private fun stepSelectUser(name: String) { ... }
    private fun stepWaitChat() { ... }
    private fun stepTypeMessage(message: String) { ... }
    private fun stepTapSend() { ... }
}
```

### ZaloElementIds (Version Map)
```kotlin
object ZaloElementIds {
    data class Ids(val searchInput: String, val chatInput: String, val sendBtn: String, val searchIconDesc: String)
    private val map = mapOf(
        "8.0.0" to Ids("edtSearch", "chat_input", "btn_send", "Tìm kiếm"),
        // Add more versions as discovered via uiautomator dump
    )
    fun forVersion(context: Context): Ids {
        val v = context.packageManager.getPackageInfo("com.zing.zalo", 0).versionName ?: "unknown"
        for ((prefix, ids) in map) {
            if (v.startsWith(prefix.takeWhile { it != '.' })) return ids
        }
        return map.values.first() // fallback
    }
}
```

### Error Classification
```kotlin
enum class ErrorCategory { NONE, TRANSIENT, TERMINAL }
```
- **TRANSIENT:** timeout waiting for UI, root window null, network slow, Zalo loading — RETRY
- **TERMINAL:** user not found, Zalo not installed, app login screen, secure lock screen, FLAG_SECURE — DO NOT RETRY

### Error Handling per Step
| Step | Possible Failure | Category | Handling |
|------|-----------------|----------|----------|
| WaitForZaloWindow | Zalo not installed | TERMINAL | Return "zalo_not_installed" error |
| WaitForZaloWindow | Login screen detected | TERMINAL | Detect "Đăng nhập" text, abort |
| WaitForZaloWindow | Timeout (15s) | TRANSIENT | Zalo slow to load, retry |
| Find Search | Search not found on UI | TERMINAL | Try gesture by coordinate; if fail, abort |
| Search User | No results | TERMINAL | Timeout after 8s, abort "user not found" |
| Select User | User name match but wrong row | TERMINAL | Try first result with exact text match |
| Select User | Chat header doesn't match target name | TERMINAL | After opening chat, read chat title and compare; if mismatch, abort "recipient mismatch" |
| Wait Chat | Chat never loads | TRANSIENT | Timeout after 10s, abort |
| Type | ACTION_SET_TEXT fails | TRANSIENT | Fallback to per-character click + focus |
| Type | Message truncated/empty | TERMINAL | Read back node.text after set; if doesn't match, fail |
| Send | Send button not visible | TRANSIENT | Poll for 3s after typing; if still missing, abort |

### Red Team Fixes Applied
- **F4 (Critical):** Added `stepWaitForZaloWindow()` — polls rootInActiveWindow for Zalo package, 15s timeout
- **F6 (Critical):** Replaced raw `Thread` with `CoroutineScope(Dispatchers.IO + SupervisorJob())` + cancelable `Job`; added `withTimeout` wrapping
- **F9 (High):** Added `ErrorCategory` enum (TRANSIENT vs TERMINAL) for intelligent retry decisions
- **F11 (Medium):** Added `dryRun` flag — skips `stepTapSend()` for safe testing without sending real messages
- Added post-typing validation: read back `node.text` and verify against input

## Zalo Launch (from FGS, not here)
The AccessibilityService itself doesn't launch Zalo — the [AutomationForegroundService](./phase-04-scheduler-services.md) does. The accessibility service just listens and executes when Zalo is in foreground.

## Related Files

| File | Action | Path (under service/accessibility/) |
|------|--------|------|
| ZaloAutomationService.kt | create | service/accessibility/ZaloAutomationService.kt |
| ZaloNodeFinder.kt | create | service/accessibility/ZaloNodeFinder.kt |
| ZaloAutomationSteps.kt | create | service/accessibility/ZaloAutomationSteps.kt |
| ZaloElementIds.kt | create | service/accessibility/ZaloElementIds.kt |

## Todo List
- [ ] Create ZaloElementIds version mapping object
- [ ] Create ZaloNodeFinder with text/id/class/structural fallback chain
- [ ] Create ZaloAutomationSteps state machine
- [ ] Create ZaloAutomationService with singleton pattern
- [ ] Wire accessibility_config.xml (already created in Phase 01)
- [ ] Test node finding logic with a debug activity that logs node tree

## Success Criteria
- AccessibilityService compiles and shows up in Settings → Accessibility after install
- NodeFinder can locate standard EditText/Button nodes in a test app
- Automation state machine transitions through steps without crash

## Risk Assessment
- Zalo element IDs change between versions → version map must be maintained; fallback to text matching
- FLAG_SECURE screens block accessibility reading → detection and graceful abort
- ACTION_SET_TEXT varies in speed → use delays between steps, don't assume instant UI update

## Next Steps
Phase 04: Scheduler & Background Services
