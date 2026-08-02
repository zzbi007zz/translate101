# Red Team Review — Adjudicated

**Date:** 2026-08-02
**Total Findings:** 15 (deduplicated from ~35 raw across 4 reviewers)
**Severity:** 6 Critical, 7 High, 2 Medium
**Accepted:** 12 / **Rejected:** 3

---

## Accepted Findings

### Finding 1: Accessibility config unbounded — no packageNames scoping
- **Severity:** Critical | **Reviewers:** Security, Assumptions
- **Location:** Phase 01 + 03
- **Flaw:** accessibility_config.xml never specifies `android:packageNames="com.zing.zalo"` — service observes ALL apps on device
- **Fix accepted:** Add packageNames to config XML; document in Phase 01 step 7

### Finding 2: AutomationEngine race condition — no concurrency safety
- **Severity:** Critical | **Reviewers:** Security, Failure Mode, Assumptions
- **Flaw:** Mutable singleton state (currentMessageId, onComplete) without @Volatile or synchronized
- **Fix accepted:** Replace with boolean isBusy guard in start(); or use AtomicReference

### Finding 3: BootReceiver Flow.collect() infinite leak
- **Severity:** Critical | **Reviewers:** Failure Mode, Assumptions, Security
- **Flaw:** `repo.getPendingMessages().collect { }` never terminates; coroutine alive forever
- **Fix accepted:** Replace with `.first()` one-shot emission

### Finding 4: No wait between Zalo launch and accessibility tree ready
- **Severity:** Critical | **Reviewers:** Failure Mode, Assumptions
- **Flaw:** FGS launches Zalo then immediately starts automation; root is null during cold start
- **Fix accepted:** Add stepWaitForZaloWindow() with 15s timeout polling rootInActiveWindow

### Finding 5: fallbackToDestructiveMigration in production
- **Severity:** Critical | **Reviewers:** Security, Failure Mode
- **Flaw:** Schema bump will destroy ALL user data with no migration
- **Fix accepted:** Gate behind `if (BuildConfig.DEBUG)` in Phase 02; add Phase 06 migration task

### Finding 6: Raw Thread with no lifecycle or cancellation
- **Severity:** Critical | **Reviewers:** Failure Mode, Assumptions
- **Flaw:** `Thread { }.start()` with no cancellation; zombie thread on service kill
- **Fix accepted:** Use CoroutineScope with Job; cancel in onDestroy; add withTimeout wrapper

### Finding 7: PendingIntent requestCode Long-to-Int truncation
- **Severity:** High | **Reviewers:** Security, Assumptions
- **Flaw:** `messageId.toInt()` collides on Longs > Int.MAX_VALUE
- **Fix accepted:** Use `messageId.hashCode()` or put id in intent data URI

### Finding 8: No DI strategy for ViewModels
- **Severity:** High | **Reviewers:** Assumptions
- **Flaw:** ViewModels take repositories as constructor params but no ViewModelFactory/DI
- **Fix accepted:** Add AndroidViewModel pattern with manual AppDatabase.getInstance() in each

### Finding 9: Retry retries terminal errors wastefully
- **Severity:** High | **Reviewer:** Failure Mode
- **Flaw:** USER_NOT_FOUND and NOT_INSTALLED retry identically to network timeouts
- **Fix accepted:** Add TRANSIENT vs TERMINAL error classification; only retry TRANSIENT

### Finding 10: Message delete doesn't cancel alarm
- **Severity:** High | **Reviewer:** Failure Mode
- **Flaw:** Deleting from Room leaves orphaned AlarmManager alarm
- **Fix accepted:** Add `deleteAndCancelAlarm()` method in MessageRepository

### Finding 11: Missing dry-run testing strategy
- **Severity:** Medium | **Reviewers:** Assumptions
- **Flaw:** Every test sends real Zalo messages
- **Fix accepted:** Add dryRun flag to ZaloAutomationSteps; skip tapSend in dry mode

### Finding 12: PermissionChangedReceiver is ghost code
- **Severity:** Medium | **Reviewers:** Failure Mode, Complexity Critic
- **Flaw:** Declared in manifest but no broadcast action or implementation specified
- **Fix accepted:** Remove from manifest and phase files

---

## Rejected Findings

### Finding 13 (Rejected): Accessibility service exported=true
- **Reviewer:** Assumptions
- **Reason:** The claim that Android 12+ rejects accessibility services with exported=true is **incorrect**. Accessibility services MUST have `exported="true"` because the system (a different UID) binds to them. The `BIND_ACCESSIBILITY_SERVICE` permission ensures only the system binds. Rejecting.

### Finding 14 (Rejected): MessageLogEntity / DataStore / 5 ViewModels over-engineered
- **Reviewer:** Complexity Critic
- **Reason:** The plan is in SCOPE EXPANSION mode. Templates, history logs, and 5 navigation screens were explicitly chosen by the user in the scope challenge. A DataStore-backed config is standard Android recommended practice. The complexity reductions are valid for MVP but counter to the chosen scope. Document as optional simplification path but do not apply to plan.

### Finding 15 (Rejected): Plaintext message data at rest
- **Reviewer:** Security
- **Reason:** SQLCipher/encryption adds significant complexity (native libs, keystore, backup restrictions) for a personal utility app without network calls or backend. PII concern for a single-user, local-only scheduler is minimal. Document in risk assessment but do not add encryption dependency.

---

## Applied Fixes Summary

| Phase | Changes Applied |
|-------|----------------|
| Phase 01 | Add `android:packageNames="com.zing.zalo"` to config XML spec; remove PermissionChangedReceiver |
| Phase 02 | Gate fallbackToDestructiveMigration behind BuildConfig.DEBUG; add migration task reference |
| Phase 03 | Add stepWaitForZaloWindow(); CoroutineScope+Job instead of raw Thread; dryRun flag; error classification |
| Phase 04 | Fix BootReceiver .first(); fix PendingIntent hashCode; isBusy guard on AutomationEngine; add deleteAndCancelAlarm |
| Phase 05 | Explicit DI strategy (AndroidViewModel + manual AppDatabase) |
| Phase 06 | Terminal vs Transient error classification for retry; Room migration task |
