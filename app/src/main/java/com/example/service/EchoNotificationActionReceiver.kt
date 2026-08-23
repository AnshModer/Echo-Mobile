package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.example.engine.DeviceController

class EchoNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.example.action.TOGGLE_TORCH"
        const val ACTION_TOGGLE_MEDIA = "com.example.action.TOGGLE_MEDIA"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_TOGGLE_TORCH -> {
                val controller = DeviceController(context)
                controller.toggleFlashlight()
            }
            ACTION_TOGGLE_MEDIA -> {
                val controller = DeviceController(context)
                controller.sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
        }
    }
}
