package com.example.service

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Android VoiceInteractionSessionService triggered on long-press power button
 * when Echo is selected as the default digital assistant.
 * Directly activates the Echo Floating Orb Overlay without opening any Activity UI.
 */
class EchoVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return EchoVoiceSession(this)
    }
}

class EchoVoiceSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Directly summon the Floating Orb Voice Assistant Overlay without opening any Activity
        EchoFloatingBubbleService.startVoiceInteraction(context)
        hide()
    }
}
