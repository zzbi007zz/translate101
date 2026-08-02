---
title: "Zalo Auto Messenger — Kotlin Android App"
description: "Kotlin Android app that auto-selects Zalo at scheduled time finds exact user and sends message via AccessibilityService"
status: completed
priority: P1
effort: 16h
branch: main
tags: [android, kotlin, accessibility, automation, zalo]
blockedBy: []
blocks: []
created: 2026-08-02
---

# Zalo Auto Messenger

## Overview

Android app in Kotlin that:
1. Lets user schedule a time, target Zalo user, and message
2. At exact scheduled time: wakes device, launches Zalo
3. Uses AccessibilityService to find user, type message, send
4. Logs results and shows notifications

Core tech: AccessibilityService (Zalo UI automation) + AlarmManager (exact scheduling) + Foreground Service (execution) + Room (data) + Compose (UI).

## Scope Mode: EXPANSION

In addition to core scheduling, includes:
- Message templates (save/reuse)
- Message history/log with sent/failed tracking
- Auto-retry on failure (max 2 retries)
- Notification per scheduled task status

## Cross-Plan Dependencies

None — greenfield project.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Project Scaffold](./phase-01-project-scaffold.md) | Completed |
| 2 | [Data Layer](./phase-02-data-layer.md) | Completed |
| 3 | [Accessibility Automation Engine](./phase-03-accessibility-automation.md) | Completed |
| 4 | [Scheduler & Background Services](./phase-04-scheduler-services.md) | Completed |
| 5 | [UI Implementation](./phase-05-ui-implementation.md) | Completed |
| 6 | [Integration & Polish](./phase-06-integration-testing.md) | Completed |

## Implementation Summary

All 6 phases completed on 2026-08-02. App builds via `./gradlew assembleDebug` and implements the full flow: Compose UI scheduling → Room persistence → AlarmManager exact alarm → BootReceiver reschedule → AutomationForegroundService (wake lock + Zalo launch) → AccessibilityService automation → status/notification updates with auto-retry (max 2, TRANSIENT only).

**Delivered:** project scaffold (Gradle + manifest + accessibility config), data layer (Room entities/DAOs/repos + DataStore), automation engine (iterative node finder + step state machine), scheduler & background services (AlarmScheduler/Receivers/Engine/FGS), 5-screen Compose UI with navigation, end-to-end integration with error handling and initial setup wizard. All 12 accepted Red Team findings applied; 3 rejected findings documented with rationale above.

### Post-Review Fixes Applied
1. PendingIntent requestCode collision → used `messageId.toInt()` as requestCode in `AlarmScheduler`
2. ProGuard rules added for Room, kotlinx.serialization, AccessibilityService
3. BootReceiver `enabled="true"` in AndroidManifest
4. DB load moved off main thread → `withContext(Dispatchers.IO)` in AutomationForegroundService / `CoroutineScope(Dispatchers.IO)` in BootReceiver
5. Duplicate `ZaloNodeFinder` removed — single class under `service/accessibility/`
6. Recursive traversal converted to iterative stack-based DFS (`ArrayDeque`) to avoid StackOverflowError
7. `notificationTimeout` increased 100 → 200ms in `accessibility_config.xml`

## Red Team Review

### Session — 2026-08-02
**Findings:** 15 deduplicated (12 accepted, 3 rejected)
**Severity breakdown:** 6 Critical, 7 High, 2 Medium

| # | Finding | Severity | Disposition | Applied To |
|---|---------|----------|-------------|------------|
| 1 | Accessibility config unbounded (no packageNames) | Critical | Accept | Phase 01 |
| 2 | AutomationEngine race condition | Critical | Accept | Phase 04 |
| 3 | BootReceiver Flow.collect() infinite leak | Critical | Accept | Phase 04 |
| 4 | No wait between Zalo launch and tree ready | Critical | Accept | Phase 03 |
| 5 | fallbackToDestructiveMigration in production | Critical | Accept | Phase 02, 06 |
| 6 | Raw Thread without lifecycle/cancellation | Critical | Accept | Phase 03 |
| 7 | PendingIntent code truncation + URI fix | High | Accept | Phase 04 |
| 8 | No DI strategy for ViewModels | High | Accept | Phase 05 |
| 9 | Retry on terminal errors wastefully | High | Accept | Phase 03, 06 |
| 10 | Message delete doesn't cancel alarm | High | Accept | Phase 02, 06 |
| 11 | Missing dry-run testing strategy | Medium | Accept | Phase 03 |
| 12 | PermissionChangedReceiver ghost code | Medium | Accept | Phase 01, 04 |
| 13 | Accessibility service exported=true claim | High | **Reject** | — |
| 14 | DataStore/5-VMs/LogEntity over-engineered | High | **Reject** | — |
| 15 | Plaintext DB — SQLCipher encryption | High | **Reject** | — |

**Rejection rationale:**
- F13: Accessibility services MUST use `exported="true"` for system binding; `BIND_ACCESSIBILITY_SERVICE` permission gates access
- F14: Plan is in SCOPE EXPANSION mode; user chose templates, history, settings
- F15: SQLCipher adds native dependency overhead disproportionate to risk (local-only, no-network app)

## Dependencies

- Android SDK (minSdk 26, targetSdk 35)
- Zalo app installed on device (com.zing.zalo)
- SCHEDULE_EXACT_ALARM permission (user-granted)
- SYSTEM_ALERT_WINDOW permission (user-granted, for BAL exemption)
- AccessibilityService enabled by user in Settings
- FOREGROUND_SERVICE_SPECIAL_USE with property explanation
