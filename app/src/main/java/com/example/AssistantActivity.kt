package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.service.EchoFloatingBubbleService

/**
 * Assistant Activity handling OS ASSIST and VOICE_COMMAND intents (such as long-press power button
 * on ROMs that route through Activity intent).
 * Directly launches the Floating Orb Voice Assistant Overlay and finishes immediately without showing any UI.
 */
class AssistantActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Directly trigger the Floating Orb Voice Assistant Overlay
        EchoFloatingBubbleService.startVoiceInteraction(this)

        // Finish immediately with zero animation so no activity UI is ever shown
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        EchoFloatingBubbleService.startVoiceInteraction(this)
        finish()
    }
}
