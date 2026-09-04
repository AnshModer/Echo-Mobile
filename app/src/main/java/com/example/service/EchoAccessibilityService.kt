package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Echo Accessibility Service enables system-level actions like taking global screenshots
 * from any screen, game, browser, or app without root or projection permissions.
 */
class EchoAccessibilityService : AccessibilityService() {

    companion object {
        private var serviceRef: WeakReference<EchoAccessibilityService>? = null

        val isServiceRunning: Boolean
            get() = serviceRef?.get() != null

        fun takeSystemScreenshot(): Boolean {
            val service = serviceRef?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event interception needed
    }

    override fun onInterrupt() {
        // Handle interrupt
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceRef?.get() == this) {
            serviceRef = null
        }
    }
}
