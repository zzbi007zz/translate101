package com.example.zaloauto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules all PENDING alarms after device reboot.
 * Messages more than 5 minutes past due are marked as failed.
 * Enabled only when PENDING messages exist; disabled otherwise.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val app = context.applicationContext as ZaloAutoApp
                val db = app.database
                val repo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())
                val scheduler = AlarmScheduler(context)
                val now = System.currentTimeMillis()

                val pending = repo.getPendingMessages()
                pending.forEach { msg ->
                    val fiveMinAgo = now - 300_000L
                    if (msg.scheduledAt > fiveMinAgo) {
                        scheduler.scheduleMessage(msg.id, maxOf(msg.scheduledAt, now + 5_000L))
                    } else {
                        repo.markFailed(msg.id, "Stale: over 5 min past due after boot")
                        scheduler.cancelMessage(msg.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
