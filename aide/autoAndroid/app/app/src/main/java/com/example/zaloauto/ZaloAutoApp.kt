package com.example.zaloauto

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.zaloauto.data.datastore.UserPreferencesRepository
import com.example.zaloauto.data.db.AppDatabase

class ZaloAutoApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepo: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        preferencesRepo = UserPreferencesRepository(dataStore)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val fgsChannel = NotificationChannel(
            CHANNEL_FOREGROUND_SERVICE,
            "Foreground Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows when automation is running" }

        val taskChannel = NotificationChannel(
            CHANNEL_TASK_STATUS,
            "Task Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Scheduled message results" }

        val accessibilityChannel = NotificationChannel(
            CHANNEL_ACCESSIBILITY,
            "Accessibility Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Accessibility service status" }

        manager.createNotificationChannels(
            listOf(fgsChannel, taskChannel, accessibilityChannel)
        )
    }

    companion object {
        const val CHANNEL_FOREGROUND_SERVICE = "channel_fgs"
        const val CHANNEL_TASK_STATUS = "channel_task_status"
        const val CHANNEL_ACCESSIBILITY = "channel_accessibility"

        @Volatile
        private var instance: ZaloAutoApp? = null

        fun getInstance(): ZaloAutoApp =
            instance ?: throw IllegalStateException("ZaloAutoApp not initialized")
    }
}
