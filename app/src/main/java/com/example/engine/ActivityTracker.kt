package com.example.engine

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Tracks the current active foreground Activity in the app
 * so background services, voice handlers, and device controllers
 * can capture screen or perform activity-scoped operations safely.
 */
object ActivityTracker {
    private var currentActivityRef: WeakReference<Activity>? = null

    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    fun clearCurrentActivity(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
        }
    }

    fun getCurrentActivity(): Activity? = currentActivityRef?.get()
}
