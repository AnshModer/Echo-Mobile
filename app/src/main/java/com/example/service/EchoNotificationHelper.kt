package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.AssistantActivity
import com.example.MainActivity
import com.example.R

object EchoNotificationHelper {

    const val CHANNEL_ID = "echo_quick_access_channel"
    const val NOTIFICATION_ID = 2001

    fun showNotification(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Intent to launch floating Siri overlay
            val assistIntent = Intent(context, AssistantActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_AUTO_START_LISTENING", true)
            }
            val assistPendingIntent = PendingIntent.getActivity(
                context,
                101,
                assistIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to toggle flashlight via BroadcastReceiver
            val torchIntent = Intent(context, EchoNotificationActionReceiver::class.java).apply {
                action = EchoNotificationActionReceiver.ACTION_TOGGLE_TORCH
            }
            val torchPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                torchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to open Main Dashboard
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                103,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_echo_mic)
                .setContentTitle("Echo Voice Assistant")
                .setContentText("Tap to speak or summon overlay")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(assistPendingIntent)
                .addAction(R.drawable.ic_echo_mic, "🎤 Speak", assistPendingIntent)
                .addAction(R.drawable.ic_echo_tile, "⚡ Torch", torchPendingIntent)
                .addAction(R.drawable.ic_echo_tile, "⚙️ Dashboard", mainPendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun hideNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Echo Quick Assistant",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Quick trigger notification to summon Echo popup assistant from anywhere"
                    setShowBadge(false)
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
