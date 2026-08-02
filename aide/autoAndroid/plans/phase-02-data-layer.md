# Phase 02: Data Layer

**Priority:** P1 | **Status:** Completed

## Overview

Implement Room database with entities, DAOs, and repositories. Add DataStore for user preferences.

## Key Insights
- Room 2.6+ requires KSP2 (Kotlin 2.x)
- DAOs return `Flow<List<>>` for reactive UI; `suspend fun` for writes
- DataStore for simple key-value config; Room for structured/queryable data
- No domain/use-case layer (YAGNI); ViewModels call repositories directly

## Entities

### ScheduledMessageEntity
```kotlin
@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetName: String,         // Zalo user's display name
    val messageText: String,        // message to send
    val scheduledAt: Long,          // epoch millis
    val status: String,             // PENDING | SENT | FAILED | CANCELED
    val templateId: Long? = null,   // optional reference to template
    val createdAt: Long,
    val retryCount: Int = 0,
)
```

### MessageLogEntity
```kotlin
@Entity(tableName = "message_logs", foreignKeys = [...])
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledMessageId: Long,
    val status: String,             // SENT | FAILED
    val sentAt: Long?,
    val error: String?,
    val retryCount: Int = 0,
)
```

### TemplateEntity
```kotlin
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String,
    val createdAt: Long,
)
```

## DAOs
- `ScheduledMessageDao`: insert, updateStatus, delete, getById, getAllFlow, getPendingFlow
- `MessageLogDao`: insert, getByScheduledMessageIdFlow
- `TemplateDao`: insert, update, delete, getAllFlow, getById

## Database
```kotlin
@Database(entities = [...], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context, AppDatabase::class.java, "zalo_auto.db")
                .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() }
                .build().also { INSTANCE = it }
        }
    }
}
```

## Repository
- `MessageRepository`: wraps ScheduledMessageDao + MessageLogDao + AlarmScheduler
  - `scheduleMessage(name, text, time)`: insert PENDING + schedule alarm
  - `deleteAndCancelAlarm(id)`: cancel alarm FIRST, then delete from Room
  - `markSent(id)`: update status + insert log
  - `markFailed(id, error)`: update status, increment retry, insert log
  - `getPendingMessagesFlow()`: for BootReceiver rescheduling
  - `getAllMessagesFlow()`: for history list
- `TemplateRepository`: wraps TemplateDao, CRUD

## DataStore Config
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val KEY_DEFAULT_RECIPIENT = stringPreferencesKey("default_recipient")
        val KEY_DEFAULT_TEMPLATE_ID = longPreferencesKey("default_template_id")
        val KEY_AUTO_SEND = booleanPreferencesKey("auto_send")
    }
    val defaultRecipient: Flow<String> = dataStore.data.map { it[KEY_DEFAULT_RECIPIENT] ?: "" }
    suspend fun setDefaultRecipient(name: String) { ... }
}
```

## Related Files

| File | Action | Path (under app/src/main/java/com/example/zaloauto/) |
|------|--------|------|
| AppDatabase.kt | create | data/db/AppDatabase.kt |
| ScheduledMessageEntity.kt | create | data/db/ScheduledMessageEntity.kt |
| MessageLogEntity.kt | create | data/db/MessageLogEntity.kt |
| TemplateEntity.kt | create | data/db/TemplateEntity.kt |
| ScheduledMessageDao.kt | create | data/db/ScheduledMessageDao.kt |
| MessageLogDao.kt | create | data/db/MessageLogDao.kt |
| TemplateDao.kt | create | data/db/TemplateDao.kt |
| MessageRepository.kt | create | data/repository/MessageRepository.kt |
| TemplateRepository.kt | create | data/repository/TemplateRepository.kt |
| UserPreferencesRepository.kt | create | data/datastore/UserPreferencesRepository.kt |

## Todo List
- [ ] Create ScheduledMessageEntity, MessageLogEntity, TemplateEntity
- [ ] Create ScheduledMessageDao, MessageLogDao, TemplateDao
- [ ] Create AppDatabase singleton
- [ ] Create MessageRepository
- [ ] Create TemplateRepository
- [ ] Create UserPreferencesRepository with DataStore
- [ ] Initialize in ZaloAutoApp.onCreate()

## Success Criteria
- Room entities compile with KSP generating proper DAOs
- Repository methods wrapped in try-catch, errors logged
- DataStore reads fall back to defaults on corruption

## Risk Assessment
- Room schema versioning: `fallbackToDestructiveMigration()` gated behind `BuildConfig.DEBUG` — release builds crash on unhandled schema change forcing dev to add proper migrations. Phase 06 includes Room Migration task.
- KSP not running: verify `ksp()` dependency in build.gradle.kts

## Red Team Fixes Applied
- **F5 (Critical):** Gated `fallbackToDestructiveMigration()` behind BuildConfig.DEBUG — prevents accidental data destruction in production builds

## Next Steps
Phase 03: Accessibility Automation Engine — ZaloAutomationService + node finder + state machine
