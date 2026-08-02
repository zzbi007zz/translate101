package com.example.zaloauto.service

import com.example.zaloauto.service.accessibility.ErrorCategory

/**
 * Singleton bridge between AutomationForegroundService and ZaloAutomationService.
 * Coordinates message execution and prevents concurrent automation runs.
 */
object AutomationEngine {

    @Volatile
    private var isBusy: Boolean = false

    @Volatile
    private var currentMessageId: Long? = null

    @Volatile
    private var onComplete: ((Boolean, String?, ErrorCategory) -> Unit)? = null

    /**
     * Start automation. Returns false if another automation is already running.
     */
    fun start(
        messageId: Long,
        callback: (Boolean, String?, ErrorCategory) -> Unit
    ): Boolean {
        synchronized(this) {
            if (isBusy) return false
            isBusy = true
            currentMessageId = messageId
            onComplete = callback
        }
        return true
    }

    /**
     * Called by AutomationForegroundService after the accessibility automation completes.
     */
    fun complete(success: Boolean, error: String?, category: ErrorCategory) {
        synchronized(this) {
            onComplete?.invoke(success, error, category)
            currentMessageId = null
            onComplete = null
            isBusy = false
        }
    }

    /**
     * Returns the currently executing message ID, or null if idle.
     */
    fun getCurrentMessageId(): Long? = currentMessageId
}
