package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.AssistantActivity
import com.example.MainActivity
import com.example.R
import com.example.engine.DeviceController

class EchoTriggerNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "echo_quick_access_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_TOGGLE_TORCH = "com.example.action.TOGGLE_TORCH"
        const val ACTION_TOGGLE_MEDIA = "com.example.action.TOGGLE_MEDIA"

        fun startService(context: Context) {
            val intent = Intent(context, EchoTriggerNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EchoTriggerNotificationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_TORCH -> {
                val controller = DeviceController(this)
                controller.toggleFlashlight()
            }
            ACTION_TOGGLE_MEDIA -> {
                val controller = DeviceController(this)
                controller.sendMediaCommand(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
        }

        val notification = buildQuickNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun buildQuickNotification(): Notification {
        // Intent to launch floating Siri overlay
        val assistIntent = Intent(this, AssistantActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_AUTO_START_LISTENING", true)
        }
        val assistPendingIntent = PendingIntent.getActivity(
            this,
            101,
            assistIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to toggle flashlight
        val torchIntent = Intent(this, EchoTriggerNotificationService::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        val torchPendingIntent = PendingIntent.getService(
            this,
            102,
            torchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open Main Dashboard
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            103,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_echo_mic)
            .setContentTitle("Echo Voice Assistant")
            .setContentText("Tap to summon Siri popup overlay anytime")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(assistPendingIntent)
            .addAction(R.drawable.ic_echo_mic, "🎤 Speak", assistPendingIntent)
            .addAction(R.drawable.ic_echo_tile, "⚡ Torch", torchPendingIntent)
            .addAction(R.drawable.ic_echo_tile, "⚙️ Dashboard", mainPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Echo Quick Access Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick trigger notification to summon Echo popup assistant from anywhere"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
