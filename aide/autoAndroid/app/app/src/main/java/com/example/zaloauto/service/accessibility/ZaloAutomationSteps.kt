package com.example.zaloauto.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

enum class Step { IDLE, WAITING_ZALO, FINDING_SEARCH, SEARCHING, SELECTING_USER, WAITING_CHAT, TYPING, SENDING, DONE, FAILED }

enum class ErrorCategory { NONE, TRANSIENT, TERMINAL }

class ZaloAutomationSteps(private val service: ZaloAutomationService) {

    private val nodeFinder = ZaloNodeFinder()
    private var currentStep = Step.IDLE
    private var error: String? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    fun execute(
        targetName: String,
        message: String,
        dryRun: Boolean = false,
        onComplete: (Boolean, String?, ErrorCategory) -> Unit
    ) {
        if (job?.isActive == true) {
            onComplete(false, "Automation already in progress", ErrorCategory.TRANSIENT)
            return
        }
        job = scope.launch {
            try {
                withTimeout(60_000L) {
                    stepWaitForZaloWindow()
                    stepFindSearch()
                    stepSearchUser(targetName)
                    stepSelectUser(targetName)
                    stepWaitChat()
                    stepVerifyChat(targetName)
                    stepTypeMessage(message)
                    if (!dryRun) stepTapSend()
                    currentStep = Step.DONE
                    onComplete(true, null, ErrorCategory.NONE)
                }
            } catch (e: TimeoutException) {
                currentStep = Step.FAILED
                onComplete(false, "Timed out at step: $currentStep", ErrorCategory.TRANSIENT)
            } catch (e: CancellationException) {
                currentStep = Step.IDLE
                onComplete(false, "Cancelled", ErrorCategory.NONE)
            } catch (e: Exception) {
                currentStep = Step.FAILED
                error = e.message
                val cat = categorizeError(e)
                onComplete(false, e.message, cat)
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun stepWaitForZaloWindow() {
        currentStep = Step.WAITING_ZALO
        val deadline = SystemClock.uptimeMillis() + 15_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val pkg = root.packageName?.toString()
                root.recycle()
                if (pkg == "com.zing.zalo") return
            }
            SystemClock.sleep(300)
        }
        throw TimeoutException("Zalo did not come to foreground within 15s")
    }

    private fun stepFindSearch() {
        currentStep = Step.FINDING_SEARCH
        val ids = ZaloElementIds.forInstalledVersion(service) ?: throw IllegalStateException("Unsupported Zalo version")
        val root = nodeFinder.getRootSafely(service::rootInActiveWindow) ?: return
        try {
            // Try by content description first (most reliable across versions)
            var node = nodeFinder.findByText(root, ids.searchIconDesc) ?: run {
                // Fallback to resource ID
                ids.searchInputResourceId?.let { nodeFinder.findById(root, it) }
            }
            if (node == null) throw IllegalStateException("Search element not found on Zalo main screen")
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            SystemClock.sleep(500)
        } finally {
            root.recycle()
        }
    }

    private fun stepSearchUser(name: String) {
        currentStep = Step.SEARCHING
        val root = nodeFinder.getRootSafely(service::rootInActiveWindow) ?: throw IllegalStateException("Root window null during search")
        try {
            val editField = nodeFinder.findEditable(root) ?: throw IllegalStateException("Search input not found")
            editField.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            editField.recycle()
            SystemClock.sleep(200)

            // Type the name by setting text on the editable field
            val refreshedRoot = service.rootInActiveWindow ?: return
            val inputField = nodeFinder.findEditable(refreshedRoot)
            if (inputField != null) {
                val arguments = android.os.Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name
                )
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                inputField.recycle()
            }
            refreshedRoot.recycle()
        } finally {
            root.recycle()
        }
    }

    private fun stepSelectUser(name: String) {
        currentStep = Step.SELECTING_USER
        val deadline = SystemClock.uptimeMillis() + 8_000L
        // Poll for search results containing the user name
        while (SystemClock.uptimeMillis() < deadline) {
            val root = service.rootInActiveWindow ?: run {
                SystemClock.sleep(300)
                continue
            }
            try {
                val userNode = nodeFinder.findByText(root, name, exact = true) ?: kotlin.run {
                    // Try first partial match as fallback
                    nodeFinder.findByText(root, name)
                }
                if (userNode != null) {
                    userNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: userNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    userNode.recycle()
                    SystemClock.sleep(500)
                    return
                }
            } finally {
                root.recycle()
                SystemClock.sleep(300)
            }
        }
        throw IllegalStateException("User '$name' not found in search results")
    }

    private fun stepWaitChat() {
        currentStep = Step.WAITING_CHAT
        val ids = ZaloElementIds.forInstalledVersion(service) ?: throw IllegalStateException("Unsupported Zalo version")
        val deadline = SystemClock.uptimeMillis() + 10_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val root = service.rootInActiveWindow ?: run {
                SystemClock.sleep(300)
                continue
            }
            try {
                // Chat is ready when we can find the message input field
                val chatInput = nodeFinder.findByText(root, ids.chatInputHint) ?: ids.chatInputResourceId?.let { nodeFinder.findById(root, it) }
                if (chatInput != null) {
                    chatInput.recycle()
                    return
                }
            } finally {
                root.recycle()
                SystemClock.sleep(300)
            }
        }
        throw TimeoutException("Chat screen did not load within 10s")
    }

    private fun stepVerifyChat(targetName: String) {
        // Read chat title bar to verify we're talking to the right person
        val root = service.rootInActiveWindow ?: return
        try {
            val titleNode = nodeFinder.findByText(root, targetName, exact = true)
            if (titleNode == null) {
                throw IllegalStateException("Chat header does not match target name '$targetName'")
            }
            titleNode.recycle()
        } finally {
            root.recycle()
        }
    }

    private fun stepTypeMessage(message: String) {
        currentStep = Step.TYPING
        val root = nodeFinder.getRootSafely(service::rootInActiveWindow) ?: throw IllegalStateException("Root window null during typing")
        try {
            val inputField = nodeFinder.findEditable(root) ?: throw IllegalStateException("Chat input not found")
            inputField.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            inputField.recycle()
            SystemClock.sleep(200)

            // Re-get root and input field after focus
            val refreshedRoot = service.rootInActiveWindow ?: throw IllegalStateException("Root window null after focus")
            val field = nodeFinder.findEditable(refreshedRoot) ?: throw IllegalStateException("Chat input lost after focus")

            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            val success = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

            // Verify text was set correctly
            val setText = field.text?.toString() ?: ""
            field.recycle()
            refreshedRoot.recycle()

            if (setText != message) {
                throw IllegalStateException("Message text verification failed: expected '${message.length}' chars, got '${setText.length}'")
            }
            if (!success) {
                throw IllegalStateException("Failed to set message text")
            }
            SystemClock.sleep(300)
        } finally {
            root.recycle()
        }
    }

    private fun stepTapSend() {
        currentStep = Step.SENDING
        val ids = ZaloElementIds.forInstalledVersion(service) ?: throw IllegalStateException("Unsupported Zalo version")
        val deadline = SystemClock.uptimeMillis() + 3_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val root = service.rootInActiveWindow ?: run {
                SystemClock.sleep(300)
                continue
            }
            try {
                val sendBtn = nodeFinder.findClickableNear(root, ids.sendIconDesc) ?: ids.sendButtonResourceId?.let { nodeFinder.findById(root, it) }
                if (sendBtn != null) {
                    sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    sendBtn.recycle()
                    SystemClock.sleep(300)
                    return
                }
            } finally {
                root.recycle()
                SystemClock.sleep(200)
            }
        }
        throw TimeoutException("Send button not found after typing")
    }

    private fun categorizeError(e: Exception): ErrorCategory {
        val msg = e.message ?: ""
        return when {
            e is TimeoutException -> ErrorCategory.TRANSIENT
            msg.contains("not found") -> ErrorCategory.TERMINAL
            msg.contains("not installed") -> ErrorCategory.TERMINAL
            msg.contains("login") || msg.contains("\u0110\u0103ng nh\u1eadp") -> ErrorCategory.TERMINAL  // "Dang nhap"
            msg.contains("mismatch") -> ErrorCategory.TERMINAL
            msg.contains("Root window null") -> ErrorCategory.TRANSIENT
            msg.contains("Unsupported Zalo version") -> ErrorCategory.TERMINAL
            else -> ErrorCategory.TRANSIENT
        }
    }
}
