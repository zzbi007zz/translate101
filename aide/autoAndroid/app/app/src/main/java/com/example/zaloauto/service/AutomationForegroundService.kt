package com.example.zaloauto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.zaloauto.MainActivity
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.db.ScheduledMessageEntity
import com.example.zaloauto.data.repository.MessageRepository
import com.example.zaloauto.service.accessibility.ErrorCategory
import com.example.zaloauto.service.accessibility.ZaloAutomationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service launched by AlarmReceiver at the scheduled time.
 * Wakes device, launches Zalo, delegates to AccessibilityService for UI automation,
 * and handles the result (notifications, DB update, retry).
 */
class AutomationForegroundService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("Preparing..."))

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "zalo_auto:wakelock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val messageId = intent?.getLongExtra(AlarmScheduler.EXTRA_MESSAGE_ID, -1L) ?: -1L
        if (messageId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }
        executeAutomation(messageId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun executeAutomation(messageId: Long) {
        // 1. Acquire wake lock (120s max, covering cold Zalo start + all step timeouts)
        wakeLock.acquire(120_000L)

        val repo = MessageRepository(
            ZaloAutoApp.getInstance().database.scheduledMessageDao(),
            ZaloAutoApp.getInstance().database.messageLogDao()
        )

        // 2. Load message on IO dispatcher, then continue on main
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val message = kotlinx.coroutines.withContext(Dispatchers.IO) {
                repo.getById(messageId)
            }

            if (message == null) {
                wakeLock.release()
                stopSelf()
                return@launch
            }

            // Post back to main thread for UI operations (launch Zalo, delegate to accessibility)
            android.os.Handler(mainLooper).post {
                onMessageLoaded(message, repo)
            }
        }
    }

    private fun onMessageLoaded(message: ScheduledMessageEntity, repo: MessageRepository) {
        // 3. Update notification
        updateForegroundNotification("Sending to ${message.targetName}...")

        // 4. Launch Zalo
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(ZALO_PACKAGE)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } catch (e: Exception) {
            wakeLock.release()
            repo.markFailed(message.id, "Zalo not installed")
            notifyStatus(message.id, false, "Zalo not installed")
            stopSelf()
            return
        }

        // 5. Short delay for Zalo to start loading, then delegate to accessibility service
        android.os.Handler(mainLooper).postDelayed({
            delegateToAccessibility(message, repo)
        }, 500)
    }

    private fun delegateToAccessibility(
        message: ScheduledMessageEntity,
        repo: MessageRepository
    ) {
        val service = ZaloAutomationService.instance
        if (service == null || !service.isAlive()) {
            wakeLock.release()
            repo.markFailed(message.id, "Accessibility service not running")
            notifyStatus(message.id, false, "Accessibility service not running")
            stopSelf()
            return
        }

        // Check if screen is locked with PIN (can't auto-dismiss)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (km.isDeviceSecure && km.isKeyguardLocked) {
            wakeLock.release()
            repo.markFailed(message.id, "Device locked with secure lock screen")
            notifyStatus(message.id, false, "Device locked — unlock to send")
            stopSelf()
            return
        }

        // Register with automation engine and start
        val started = AutomationEngine.start(message.id) { success, error, category ->
            if (success) {
                repo.markSent(message.id)
                notifyStatus(message.id, true, null)
            } else if (category == ErrorCategory.TRANSIENT && message.retryCount < MAX_RETRIES) {
                // Auto-retry: reschedule alarm 30s later
                val scheduler = AlarmScheduler(this)
                repo.incrementRetry(message.id)
                scheduler.scheduleMessage(message.id, System.currentTimeMillis() + 30_000L)
            } else {
                repo.markFailed(message.id, error ?: "Unknown error")
                notifyStatus(message.id, false, error)
            }
            wakeLock.release()
            stopSelf()
        }

        if (started) {
            service.automator.execute(
                message.targetName, message.messageText
            ) { success, error, category ->
                AutomationEngine.complete(success, error, category)
            }
        } else {
            wakeLock.release()
            stopSelf()
        }
    }

    private fun notifyStatus(messageId: Long, success: Boolean, error: String?) {
        val title = if (success) "Message Sent" else "Message Failed"
        val body = if (success) "Message delivered successfully."
        else "Failed: ${error ?: "Unknown error"}. Tap to view."

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            messageId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ZaloAutoApp.CHANNEL_TASK_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(NOTIFICATION_TASK_ID + messageId.toInt(), notification)
    }

    private fun createNotification(text: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZaloAutoApp.CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle("Zalo Auto Sender")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateForegroundNotification(text: String) {
        val notification = NotificationCompat.Builder(this, ZaloAutoApp.CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle("Zalo Auto Sender")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_TASK_ID = 2001
        private const val MAX_RETRIES = 2
        private const val ZALO_PACKAGE = "com.zing.zalo"
    }
}
