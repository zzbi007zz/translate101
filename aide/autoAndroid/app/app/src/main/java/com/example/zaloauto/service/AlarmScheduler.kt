package com.example.zaloauto.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Schedules and cancels exact alarms for message automation.
 * Uses setExactAndAllowWhileIdle for Doze-compatible scheduling.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        alarmManager.canScheduleExactAlarms()

    fun scheduleMessage(messageId: Long, triggerAtMillis: Long): Boolean {
        if (!canScheduleExactAlarms()) return false

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MESSAGE_ID, messageId)
            data = Uri.parse("zalo-auto://msg/$messageId")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        return true
    }

    fun cancelMessage(messageId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            data = Uri.parse("zalo-auto://msg/$messageId")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "message_id"
    }
}
