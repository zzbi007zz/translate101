# Code Standards — Zalo Auto Sender

Conventions observed in the codebase. These reflect the **actual** implementation (verified against source), not aspirational rules. One intentional deviation from the generic guideline is called out below.

## File Naming

**Note on convention:** the generic "kebab-case for file names" guideline does **not** apply here. Every Kotlin source file uses **PascalCase** matching the primary class it declares. This is the standard Android/Kotlin convention, and the project follows it consistently. No kebab-case or snake_case Kotlin filenames exist in the tree.

| Artifact | Convention | Examples |
|----------|-----------|----------|
| Kotlin source file | PascalCase, same name as primary class | `HomeViewModel.kt`, `ZaloAutomationSteps.kt`, `AutomationForegroundService.kt` |
| Compose screen file | `{Feature}Screen.kt` + `{Feature}ViewModel.kt` pair | `ListScreen.kt` / `ListViewModel.kt` |
| XML resources | lowercase snake_case | `accessibility_config.xml`, `strings.xml`, `themes.xml` |
| Navigation routes | `@Serializable object/class`, `XRoute` | `HomeRoute`, `DetailRoute` |

Each file is named so an LLM or developer can infer its purpose from the name alone (e.g., `MessageRepository.kt`, `ZaloNodeFinder.kt`, `UserPreferencesRepository.kt`).

## Package Organization

Root package: `com.example.zaloauto`. Packages are organized by **concern/layer**, not by feature:

```
com.example.zaloauto
├── data            # persistence layer (pure Android + Room)
│   ├── db          # Room entities, DAOs, AppDatabase
│   ├── repository  # business facades over DAOs
│   └── datastore   # DataStore preferences repository
├── service         # scheduling + background execution
│   └── accessibility   # accessibility service + UI automation
├── ui              # presentation layer (Compose)
│   ├── navigation  # route definitions + NavHost
│   ├── screens     # one sub-package per screen (screen + viewmodel)
│   ├── components  # shared Composables
│   └── theme       # Material 3 theme
```

Rules:
- **Data never references UI or service.** `data/*` imports only Room, DataStore, and Kotlin coroutines.
- **`service/accessibility`** is separated from the rest of `service/` because it owns the Zalo-specific automation logic.
- **Screens are self-contained sub-packages** (`home`, `list`, `detail`, `templates`, `settings`), each holding `{X}Screen.kt` and `{X}ViewModel.kt`.
- One class per file (except small enum/`data class` declarations co-located with their primary usage, e.g. `HomeUiState` next to `HomeViewModel`).

## Kotlin Conventions

### Language & toolchain
- Kotlin **2.0.21**, JVM target **17**, compiled with AGP 8.7.3 and KSP (used by Room).
- `kotlin.plugin.serialization` is applied; navigation routes are `@Serializable` for type-safe navigation.

### UI state pattern
- Every ViewModel extends **`AndroidViewModel`** (needs `Application` for services/repos).
- One immutable `data class {X}UiState` per screen, exposed as `StateFlow`:
  ```kotlin
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
  ```
- Updates go through `_uiState.update { it.copy(...) }` — never mutate the public flow.
- ViewModels collect DAO `Flow`s with `viewModelScope.launch { ...collect { } }`.

### Dependency style
- **No DI framework.** Repositories are constructed directly from `ZaloAutoApp.getInstance().database`:
  ```kotlin
  private val db = ZaloAutoApp.getInstance().database
  private val repo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())
  ```
- Dependencies are passed by **constructor injection** where feasible (`MessageRepository(dao, logDao)`, `AlarmScheduler(context)`).

### Repository layer
- Repositories expose `Flow`-returning methods for observation (`getAllMessagesFlow()`) and `suspend` methods for one-shot operations (`scheduleMessage()`, `markSent()`).
- Multi-table writes (message status + log row) live in the repository, not the ViewModel.

### Room
- Entities are `data class` with `@PrimaryKey(autoGenerate = true) val id: Long = 0`.
- DAO interfaces: `@Insert`/`@Update`/`@Query`; reads returning live data use `Flow<T>`, one-shot reads use `suspend fun`.
- SQL uses the `'STATUS'` string literals matching entity companion constants (e.g., `status = 'PENDING'`).
- Schema is exported (`exportSchema = true`); migrations fall back to destructive in debug builds only.

### Background execution
- **Alarm scheduling:** `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)`; `PendingIntent` uses `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.
- **Service orchestration:** coroutines on `Dispatchers.IO` for DB work, then `Handler(mainLooper).post { }` for UI/launch work, `Handler(mainLooper).postDelayed(..., 500)` for settle delays.
- **Accessibility automation:** `CoroutineScope(Dispatchers.IO + SupervisorJob())`, with `withTimeout(60_000L)` around the whole step sequence.

### Singletons
- `object` for stateless/coordinator singletons (`AutomationEngine`, `ZaloElementIds`).
- `@Volatile` fields for cross-thread singleton references (`ZaloAutomationService.instance`, `ZaloAutoApp.getInstance()`, Room `INSTANCE`).
- `private set` on exposed fields the class owns (`lateinit var database: AppDatabase; private set`).

### Constants
- Named constants in `companion object` blocks (status strings, notification IDs, package names, max retries):
  ```kotlin
  companion object {
      const val STATUS_PENDING = "PENDING"
      private const val MAX_RETRIES = 2
      private const val ZALO_PACKAGE = "com.zing.zalo"
  }
  ```
- Notification channel IDs are `const val` on `ZaloAutoApp` and referenced from services.

### Error handling
- `try/catch` around all external calls (package lookups, Room writes, automation steps).
- A two-level error model for automation: `ErrorCategory.TRANSIENT` (retryable) vs `TERMINAL` (permanent), categorized centrally in `ZaloAutomationSteps.categorizeError()`.
- Failure strings use Vietnamese-aware matching (e.g., `"Đăng nhập"` for login prompts).

### Resource management
- `AccessibilityNodeInfo` references are **explicitly recycled** (`node.recycle()`, `root.recycle()`) in `finally` blocks to avoid leaks.
- Wake locks acquired with a timeout (`wakeLock.acquire(60_000L)`) and released in every completion path plus `onDestroy()`.

### Naming rules observed
- Classes: PascalCase noun phrases (`AlarmScheduler`, `ZaloNodeFinder`).
- Functions: camelCase verbs (`scheduleMessage`, `markSent`, `stepWaitForChat`).
- Private step methods in `ZaloAutomationSteps` are prefixed `step` + step name.
- `_uiState` underscore prefix marks the private `MutableStateFlow` backing the public `uiState`.
