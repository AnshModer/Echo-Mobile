package com.example.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.AssistantActivity
import com.example.MainActivity
import com.example.R
import com.example.data.local.AssistantPreferences

/**
 * System-level Foreground Service that manages a floating Siri-style glowing Echo Orb overlay
 * using WindowManager and TYPE_APPLICATION_OVERLAY.
 */
class EchoFloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var orbView: FloatingOrbView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private lateinit var preferences: AssistantPreferences

    companion object {
        const val CHANNEL_ID = "echo_floating_orb_service_channel"
        const val NOTIFICATION_ID = 3001
        const val ACTION_START_SERVICE = "com.example.action.START_FLOATING_ORB"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_FLOATING_ORB"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, EchoFloatingBubbleService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EchoFloatingBubbleService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferences = AssistantPreferences(this)
        isRunning = true
        createNotificationChannel()
        startAsForeground()
        createFloatingOrbOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            preferences.isFloatingBubbleEnabled = false
            stopForegroundAndRemove()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (API 34)
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(): Notification {
        // Tap notification to summon assistant
        val assistIntent = Intent(this, AssistantActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_AUTO_START_LISTENING", true)
        }
        val assistPendingIntent = PendingIntent.getActivity(
            this,
            101,
            assistIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to stop and remove floating orb
        val stopIntent = Intent(this, EchoFloatingBubbleService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to open Main Dashboard
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            103,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_echo_mic)
            .setContentTitle("Echo Floating Orb Active")
            .setContentText("Tap floating orb on screen to speak • Drag to move")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(assistPendingIntent)
            .addAction(R.drawable.ic_echo_mic, "🎤 Speak", assistPendingIntent)
            .addAction(R.drawable.ic_echo_tile, "⚙️ Dashboard", mainPendingIntent)
            .addAction(R.drawable.ic_echo_tile, "❌ Hide Orb", stopPendingIntent)
            .build()
    }

    private fun createFloatingOrbOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Standard Orb Size (64dp converted to pixels)
            val density = resources.displayMetrics.density
            val orbSizePx = (66 * density).toInt()

            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val initialX = screenWidth - orbSizePx - (16 * density).toInt()
            val initialY = (displayMetrics.heightPixels * 0.4f).toInt()

            val params = WindowManager.LayoutParams(
                orbSizePx,
                orbSizePx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX
                y = initialY
            }
            windowLayoutParams = params

            val orb = FloatingOrbView(this)
            orbView = orb

            setupTouchAndDragListener(orb, params)

            windowManager?.addView(orb, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupTouchAndDragListener(orb: FloatingOrbView, params: WindowManager.LayoutParams) {
        orb.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var hasMoved = false
            private val touchSlop = 12 * resources.displayMetrics.density

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        hasMoved = false
                        orb.setPressedVisual(true)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX)
                        val dy = (event.rawY - initialTouchY)

                        if (!hasMoved && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            hasMoved = true
                        }

                        if (hasMoved) {
                            params.x = (initialX + dx).toInt()
                            params.y = (initialY + dy).toInt()
                            try {
                                windowManager?.updateViewLayout(orbView, params)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        orb.setPressedVisual(false)
                        if (!hasMoved) {
                            // User Tapped the Orb!
                            performHapticFeedback()
                            launchVoiceAssistant()
                        } else {
                            // Snap to nearest screen edge (left or right)
                            snapToEdge(params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        orb.setPressedVisual(false)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val margin = (12 * density).toInt()
        val orbWidth = params.width

        val targetX = if (params.x + orbWidth / 2 < screenWidth / 2) {
            margin
        } else {
            screenWidth - orbWidth - margin
        }

        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 250L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                params.x = it.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(orbView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animator.start()
    }

    private fun launchVoiceAssistant() {
        try {
            val intent = Intent(this, AssistantActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_AUTO_START_LISTENING", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performHapticFeedback() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundAndRemove() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopForegroundAndRemove()
        orbView?.let {
            it.stopAnimation()
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        orbView = null
        windowManager = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Echo Floating Orb Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps the Echo Floating Orb active on screen for instant access across any app"
                    setShowBadge(false)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
