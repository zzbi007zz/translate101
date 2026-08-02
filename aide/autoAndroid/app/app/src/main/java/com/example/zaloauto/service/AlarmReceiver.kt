package com.example.zaloauto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Receives scheduled alarm broadcasts and starts the AutomationForegroundService.
 * Alarms are set by AlarmScheduler when user schedules a message.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(AlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return

        val serviceIntent = Intent(context, AutomationForegroundService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_MESSAGE_ID, messageId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
