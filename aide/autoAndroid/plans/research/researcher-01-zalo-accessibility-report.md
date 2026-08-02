# Researcher 01: Zalo AccessibilityService Automation

**Date:** 2026-08-02
**Sources:** developer.android.com (AccessibilityService, AccessibilityNodeInfo, background-starts, BAL exemptions, FGS restrictions, Android 12/13/14 behavior changes), training knowledge

---

## 1. Android AccessibilityService — Verified API

### Manifest Declaration
```xml
<service
    android:name=".ZaloAutomationService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true"
    android:label="Zalo Automation">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_config" />
</service>
```
- `BIND_ACCESSIBILITY_SERVICE` — mandatory; only the system binds this service
- `exported="true"` — mandatory for system binding
- `android:exported` must be explicit on all components with intent filters (Android 12+)

### Config XML (`res/xml/accessibility_config.xml`)
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewClicked"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:notificationTimeout="100"
    android:description="@string/accessibility_description" />
```
- `canRetrieveWindowContent="true"` — REQUIRED to inspect UI nodes
- `canPerformGestures="true"` — REQUIRED for `dispatchGesture()`
- `flagReportViewIds` — enables `viewIdResourceName` property on nodes
- `description` — REQUIRED, shown to user in Settings

### User Enablement
- **Cannot be enabled programmatically** — user must toggle in Settings → Accessibility
- Guide user: `startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))`
- Check: `AccessibilityManager.getEnabledAccessibilityServiceList()`
- Android 13+: sideloaded apps may need "restricted settings" allowlist first

### Node Inspection (Verified)
```kotlin
val root: AccessibilityNodeInfo? = rootInActiveWindow
nodes.findAccessibilityNodeInfosByText("text")  // matches text OR contentDescription
nodes.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/foo")  // API 18+
node.getText() / node.getContentDescription() / node.getViewIdResourceName()
node.getClassName() / node.getPackageName() / node.getBoundsInScreen()
node.isClickable / node.isEditable / node.isVisibleToUser
// Actions
node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)  // API 21+
node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
// Gestures
dispatchGesture(gestureDescription, callback, handler)
// Global actions
performGlobalAction(GLOBAL_ACTION_BACK)
performGlobalAction(GLOBAL_ACTION_HOME)
```

---

## 2. Zalo App Details

| Property | Value |
|---|---|
| Package | `com.zing.zalo` |
| Developer | VNG Corporation |
| Launcher Activity | `com.zing.zalo.ui.ZaloActivity` |
| Category | Messaging |

### Known UI Elements (version-dependent, verify with uiautomator dump)
- Search icon/bar: `edtSearch`, `search_bar`, content-desc="Tìm kiếm"
- Search input: `search_edit_text`, hint="Tìm kiếm"
- User result row: `user_row`, clickable with name `TextView`
- Chat input: EditText with hint "Nhập tin nhắn", possible ID `chat_input`
- Send button: `btn_send`, content-desc="Gửi"/"Send"; appears after typing
- Bottom nav tabs: "Tin nhắn", "Danh bạ", "Khám phá", etc.

### Element Discovery Strategy
```
Priority 1: resource-id (version-mapped via PackageManager.versionName)
Priority 2: content-description text matching (Vietnamese labels stable across versions)
Priority 3: structural heuristics (Find EditText on chat screen, find clickable near bottom-right)
Priority 4: coordinate-based taps via dispatchGesture as last resort
```

### Version Mapping Approach
```kotlin
data class ZaloElementMap(
    val version: String,
    val searchInputId: String,
    val chatInputId: String,
    val sendBtnId: String,
    val searchIconDesc: String = "Tìm kiếm",
)
// Maintain a hardcoded list updated per major Zalo release
```

---

## 3. Background Activity Launch (BAL) — Critical Finding

**Verified from developer.android.com/guide/components/activities/background-starts:**

The BAL exemption list:
1. App has a visible window (activity in foreground)
2. App is current IME
3. System-sent PendingIntent (notification tap, alarm fires getActivity PendingIntent)
4. **`SYSTEM_ALERT_WINDOW` permission** ← This is our path
5. `START_ACTIVITIES_FROM_BACKGROUND` permission
6. App bound to authorized service
7. Launcher-initiated
8. Core OS component (telephony, etc.)

**AccessibilityService is NOT a BAL exemption.** This was confirmed by reading the official docs.

### Mitigation: SYSTEM_ALERT_WINDOW
- Requires user grant via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
- Grants BAL exemption
- Android 15+: FGS bg-start via SYSTEM_ALERT_WINDOW additionally requires a **visible overlay** window
- Check: `Settings.canDrawOverlays(context)`

---

## 4. Android 12+ Restrictions Affecting This App

| Restriction | Impact | Mitigation |
|---|---|---|
| `android:exported` must be explicit | Build error if missing | Declare on all components with intent filters |
| FGS background-start blocked (API 31+) | Cannot start automation service | Exact alarm exemption, or user interaction with notification |
| PendingIntent mutability flag (API 31+) | Missing flag = crash | Use `FLAG_IMMUTABLE` everywhere |
| Notification trampoline ban (API 31+) | Cannot start activity from notification service | Use PendingIntent directly on notification |
| App hibernation (unused 3+ months) | Permissions auto-revoked | Re-prompt flow on detect |
| POST_NOTIFICATIONS runtime (API 33+) | FGS notification invisible | Request at first launch |
| ForegroundServiceType mandatory (API 34+) | Crash without it | Declare `specialUse` with property explanation |
| Exact alarm denied by default (API 33+) | Alarm won't fire | Guide user to "Alarms & reminders" settings |

---

## 5. Automation Flow Design

```
exact_alarm → BroadcastReceiver → startForegroundService(FGS)
    → SYSTEM_ALERT_WINDOW exemption → launchActivity(Zalo)
    → AccessibilityService state machine:
        1. WAIT_ZALO → detect Zalo package in window root
        2. FIND_SEARCH → tap search entry point
        3. SEARCH_USER → type target name, wait results
        4. SELECT_USER → find row with exact name match, click
        5. WAIT_CHAT → detect chat input EditText
        6. TYPE_MESSAGE → set text on EditText
        7. SEND → find/clicksend button
        8. DONE/ERROR → notify user, update Room, stopSelf
```

### Error Detection
- Zalo not installed → PackageManager returns null
- Not logged in → detect "Đăng nhập" text nodes
- User not found → timeout on search results
- Chat not loaded → timeout on chat input
- Internet down → Zalo shows "Không có kết nối mạng"
- FLAG_SECURE screen → accessibility tree empty (rare, mainly login/OTP)

---

## 6. Known Open-Source Projects (from training knowledge, unverified)
- `zing-mp3/zalo-ui` — official Zalo UI design kit (NOT automation)
- AutoJS-based Zalo messaging scripts — various Gists
- `zalo-mcp` — MCP server using official Zalo Open API (OA)
- Official `ZaloSDK` — for login/share integration in Android apps

Recommend: `gh search repos "zalo accessibility"` / `gh search repos "zalo autojs"`

---

## 7. Trade-offs Summary

| Dimension | AccessibilityService | ADB/Appium | Official API |
|---|---|---|---|
| Reliability | Medium (depends on Zalo version) | High | High |
| Autonomy | Full (on-device) | None (needs host) | Full |
| Play Policy Risk | High | None (sideload) | Low |
| Dev Effort | High | Medium | Low-Medium |
| Zalo ToS Risk | Yes (bot account ban) | Yes | Depends on OA |
| User Experience | Needs accessibility enable | Needs USB | Needs OA approval |

---

## 8. Key Risks
1. **Zalo ToS** prohibits bot/automation → account ban risk. Best for personal use, not for distribution.
2. **UI element IDs change** per Zalo version → maintain version map; use text/content-desc as stable alternative.
3. Zalo may add FLAG_SECURE to more screens over time.
4. Google Play may reject accessibility-based automation of third-party apps.
5. Locked screen blocks automation — device must be awake and unlocked.

**Status:** DONE_WITH_CONCERNS
**Concerns:** GitHub search was blocked; Zalo element IDs are from training knowledge (unverified live). BAL exemption confirmed — SYSTEM_ALERT_WINDOW is the path.
