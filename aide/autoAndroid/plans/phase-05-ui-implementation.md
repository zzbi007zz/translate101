# Phase 05: UI Implementation

**Priority:** P1 | **Status:** Completed

## Overview

Build Compose UI with Navigation component, ViewModels, and screens for scheduling messages, viewing history/list, managing templates, and app settings.

## Key Insights
- Single-activity Compose Navigation architecture
- Type-safe routes via `@Serializable` + Kotlin serialization plugin
- `StateFlow<UiState>` pattern in ViewModels
- DataStore for preferences; Room Flow for list reactivity

### DI Strategy
```kotlin
// All ViewModels extend AndroidViewModel for Application context access
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())
    // ... use repo directly
}
// Note: compose viewModel() with AndroidViewModelFactory works out of the box.
// No Hilt/Koin needed for this scope.
```

## Screens

### 1. Home Screen (Schedule)
- Input fields: Target user name (EditText), Message (EditText multiline), Scheduled date/time (DateTimePicker)
- Template selector dropdown (optional)
- "Schedule Send" button → calls MessageRepository.scheduleMessage + AlarmScheduler.scheduleMessage
- Permission status indicators: SCHEDULE_EXACT_ALARM, SYSTEM_ALERT_WINDOW, accessibility enabled
- Tap indicator → opens relevant settings page

### 2. Scheduled List Screen
- LazyColumn of scheduled messages with status chips (PENDING/SENT/FAILED/CANCELED)
- Swipe-to-cancel on PENDING items
- Tap to view details (time, target, content, logs)
- Empty state: "No scheduled messages yet"
- Pull-to-refresh from Flow

### 3. Templates Screen
- LazyColumn of saved templates
- FAB to add new template (AlertDialog with name+content)
- Swipe-to-delete
- Tap to select → navigates back to Home with template pre-filled

### 4. Settings Screen
- Default recipient name
- Auto-send toggle (skip confirmation)
- Permissions status dashboard with links to grant
- "Enable Accessibility Service" button → opens Settings.ACTION_ACCESSIBILITY_SETTINGS
- "Grant Overlay Permission" → Settings.ACTION_MANAGE_OVERLAY_PERMISSION
- "Grant Exact Alarm" → Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
- About / version

### 5. Message Detail Screen
- Route: `/detail/{messageId}`
- Shows: target name, message text, scheduled time, status, sent time (if sent), error (if failed)
- Logs list (from MessageLog)
- "Resend now" button for FAILED messages

## Navigation Graph
```kotlin
@Serializable object HomeRoute
@Serializable object ListRoute
@Serializable object TemplatesRoute
@Serializable object SettingsRoute
@Serializable data class DetailRoute(val messageId: Long)

@Composable fun NavGraph(navController: NavHostController) {
    NavHost(navController, HomeRoute) {
        composable<HomeRoute> { HomeScreen(navController) }
        composable<ListRoute> { ListScreen(navController) }
        composable<TemplatesRoute> { TemplatesScreen(navController) }
        composable<SettingsRoute> { SettingsScreen(navController) }
        composable<DetailRoute> { DetailScreen(navController) }
    }
}
```

## ViewModels
- `HomeViewModel`: recipient, message, time, templates list, schedule action
- `ListViewModel`: scheduled messages Flow, cancel action
- `TemplatesViewModel`: templates Flow, add/delete/select
- `SettingsViewModel`: permissions state, preferences
- `DetailViewModel`: single message + logs

Each ViewModel exposes a sealed `UiState` data class with loading/loaded/error substates.

## UI Components
- `PermissionStatusChip`: green/red chip showing permission status with onClick to settings
- `DateTimePickerDialog`: Material3 DatePicker + TimePicker
- `StatusChip`: colored chip for PENDING(yellow)/SENT(green)/FAILED(red)/CANCELED(gray)
- `ConfirmDialog`: "Schedule message to [name] at [time]?" before confirming
- `EmptyState`: illustration + text for empty lists

## Related Files

| File | Action | Path (under ui/) |
|------|--------|------|
| NavGraph.kt | create | ui/navigation/NavGraph.kt |
| Routes.kt | create | ui/navigation/Routes.kt |
| HomeScreen.kt | create | ui/screens/home/HomeScreen.kt |
| HomeViewModel.kt | create | ui/screens/home/HomeViewModel.kt |
| ListScreen.kt | create | ui/screens/list/ListScreen.kt |
| ListViewModel.kt | create | ui/screens/list/ListViewModel.kt |
| TemplatesScreen.kt | create | ui/screens/templates/TemplatesScreen.kt |
| TemplatesViewModel.kt | create | ui/screens/templates/TemplatesViewModel.kt |
| SettingsScreen.kt | create | ui/screens/settings/SettingsScreen.kt |
| SettingsViewModel.kt | create | ui/screens/settings/SettingsViewModel.kt |
| DetailScreen.kt | create | ui/screens/detail/DetailScreen.kt |
| DetailViewModel.kt | create | ui/screens/detail/DetailViewModel.kt |
| Theme.kt | create | ui/theme/Theme.kt |
| PermissionStatusChip.kt | create | ui/components/PermissionStatusChip.kt |
| DateTimePickerDialog.kt | create | ui/components/DateTimePickerDialog.kt |
| Theme.kt | create | ui/theme/Theme.kt |
| MainActivity.kt | create | ✅ root package (see below) |

### MainActivity
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZaloAutoTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { /* BottomNavigation: Home, List, Templates, Settings */ }
                ) { padding ->
                    NavGraph(navController, Modifier.padding(padding))
                }
            }
        }
    }
}
```

## Todo List
- [ ] Create MainActivity with Compose + bottom nav
- [ ] Create NavGraph with 5 routes
- [ ] Create Theme.kt (Material3, dark/light)
- [ ] Create HomeScreen + HomeViewModel
- [ ] Create ListScreen + ListViewModel
- [ ] Create TemplatesScreen + TemplatesViewModel
- [ ] Create SettingsScreen + SettingsViewModel
- [ ] Create DetailScreen + DetailViewModel
- [ ] Create reusable UI components (PermissionStatusChip, DateTimePickerDialog, StatusChip)
- [ ] Handle empty states and loading states

## Success Criteria
- All 5 screens navigate correctly
- Scheduling a message writes to Room and triggers AlarmScheduler
- Permission status indicators update reactively
- Template selection pre-fills home screen fields

## Next Steps
Phase 06: Integration & Final Polish
