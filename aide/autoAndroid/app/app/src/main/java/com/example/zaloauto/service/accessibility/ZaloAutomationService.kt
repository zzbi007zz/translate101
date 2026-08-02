package com.example.zaloauto.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service scoped to com.zing.zalo only.
 * Acts as the bridge between the foreground service and Zalo's UI.
 * Singleton instance pattern for cross-component access.
 */
class ZaloAutomationService : AccessibilityService() {

    val automator = ZaloAutomationSteps(this)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are consumed by ZaloAutomationSteps polling, not this callback
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: ZaloAutomationService? = null
            private set
    }
}
