package com.example.zaloauto.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScheduledMessageEntity::class,
        MessageLogEntity::class,
        TemplateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zalo_auto.db"
                )
                // Allow destructive migration in debug builds only
                if (com.example.zaloauto.BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }
                builder.build().also { INSTANCE = it }
            }
    }
}
