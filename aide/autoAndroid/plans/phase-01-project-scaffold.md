# Phase 01: Project Scaffold

**Priority:** P1 | **Status:** Completed

## Overview

Create the Android project with Gradle build config, manifest permissions, Application class, and notification channels.

## Key Insights
- minSdk 26 for notification channels, Doze, requestDismissKeyguard
- targetSdk 35 triggers SCHEDULE_EXACT_ALARM + foregroundServiceType requirements
- Kotlin 2.0.x with KSP2 for Room; AGP 8.7+
- Zalo package: com.zing.zalo

## Requirements
- Gradle project with proper Android plugin, Kotlin, KSP, serialization
- AndroidManifest with all services, receivers, permissions declared
- Application class creating notification channels
- Accessibility service config XML in res/xml/

## Architecture
Single `:app` module, package `com.example.zaloauto`

## Related Files

| File | Action | Path |
|------|--------|------|
| settings.gradle.kts | create | `/app/settings.gradle.kts` |
| build.gradle.kts (project) | create | `/app/build.gradle.kts` |
| build.gradle.kts (app) | create | `/app/app/build.gradle.kts` |
| gradle.properties | create | `/app/gradle.properties` |
| AndroidManifest.xml | create | `/app/app/src/main/AndroidManifest.xml` |
| ZaloAutoApp.kt | create | `/app/app/src/main/java/com/example/zaloauto/ZaloAutoApp.kt` |
| accessibility_config.xml | create | `/app/app/src/main/res/xml/accessibility_config.xml` |

## Implementation Steps

1. Create project directory structure (app/, app/app/, app/app/src/main/java/..., app/app/src/main/res/...)
2. Write `settings.gradle.kts` with plugin management, repository, project name
3. Write root `build.gradle.kts` with AGP + Kotlin plugins (no apply)
4. Write app `build.gradle.kts` with:
   - Plugins: android application, kotlin-android, kotlin-serialization, ksp
   - android block: namespace, compileSdk 35, minSdk 26, targetSdk 35
   - Dependencies: compose-bom, navigation-compose, room (runtime/ktx/compiler via ksp), datastore-preferences, lifecycle-viewmodel-compose, kotlinx-serialization-json, core-ktx, activity-compose
5. Write `gradle.properties` with AndroidX, R8, non-transitive R classes
6. Write `AndroidManifest.xml` declaring:
   - Permissions: SCHEDULE_EXACT_ALARM, SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, RECEIVE_BOOT_COMPLETED
   - Application: android:name=".ZaloAutoApp"
   - Services: ZaloAutomationService (BIND_ACCESSIBILITY_SERVICE, exported=true, meta-data), AutomationForegroundService (foregroundServiceType="specialUse", property), SendMessageForegroundService
   - Receivers: AlarmReceiver, BootReceiver
7. Write `accessibility_config.xml` with content:
   ```xml
   <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
       android:packageNames="com.zing.zalo"
       android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewFocused|typeViewClicked"
       android:accessibilityFeedbackType="feedbackGeneric"
       android:accessibilityFlags="flagDefault|flagReportViewIds|flagRetrieveInteractiveWindows"
       android:canRetrieveWindowContent="true"
       android:canPerformGestures="true"
       android:notificationTimeout="100"
       android:description="@string/accessibility_description" />
   ```
   **Critical:** `packageNames="com.zing.zalo"` scopes the service to Zalo only — prevents system-wide UI observation.
9. Write `ZaloAutoApp.kt` extending Application:
   - Create notification channels (channel_fgs, channel_task_status, channel_accessibility) in onCreate
   - Initialize Room database singleton
   - Initialize DataStore singleton

## Todo List
- [ ] Create directory structure
- [ ] Write Gradle build files
- [ ] Write AndroidManifest.xml with all declarations
- [ ] Write ZaloAutoApp.kt with notification channels
- [ ] Write accessibility_config.xml
- [ ] Verify project syncs without errors

## Success Criteria
- Project compiles with `./gradlew assembleDebug`
- AndroidManifest has all required services/receivers/permissions
- Accessibility config XML correct per Android docs

## Risk Assessment
- AGP/Kotlin version incompatibility → pin AGP 8.7+ and Kotlin 2.0.x
- Missing KSP plugin declaration → Room annotation processing fails

## Next Steps
Phase 02: Data Layer — Room database, entities, DAOs, repository
