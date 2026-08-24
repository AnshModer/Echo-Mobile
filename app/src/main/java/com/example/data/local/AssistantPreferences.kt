package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

enum class OrbTheme(val displayName: String, val primaryColorHex: Long, val secondaryColorHex: Long, val tertiaryColorHex: Long) {
    SIRI_RAINBOW("Siri Classic", 0xFF00E5FF, 0xFFE040FB, 0xFF7C4DFF),
    COSMIC_NEON("Cosmic Neon", 0xFF00F5D4, 0xFF7B2CBF, 0xFFF72585),
    DEEP_AMETHYST("Deep Amethyst", 0xFF9D4EDD, 0xFF5A189A, 0xFF3C096C),
    SOLAR_FLARE("Solar Flare", 0xFFFF9E00, 0xFFFF0054, 0xFFFF5400),
    CYBER_PULSE("Cyber Pulse", 0xFF00F0FF, 0xFF0038FF, 0xFF7000FF)
}

class AssistantPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("echo_settings", Context.MODE_PRIVATE)

    var orbTheme: OrbTheme
        get() {
            val name = prefs.getString("orb_theme", OrbTheme.SIRI_RAINBOW.name)
            return try {
                OrbTheme.valueOf(name ?: OrbTheme.SIRI_RAINBOW.name)
            } catch (e: Exception) {
                OrbTheme.SIRI_RAINBOW
            }
        }
        set(value) = prefs.edit().putString("orb_theme", value.name).apply()

    var isTtsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", true)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speech_rate", value).apply()

    var speechPitch: Float
        get() = prefs.getFloat("speech_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speech_pitch", value).apply()

    var autoListenOnLaunch: Boolean
        get() = prefs.getBoolean("auto_listen_launch", true)
        set(value) = prefs.edit().putBoolean("auto_listen_launch", value).apply()

    var keepScreenOnDuringAssistant: Boolean
        get() = prefs.getBoolean("keep_screen_on", true)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    var isQuickNotificationEnabled: Boolean
        get() = prefs.getBoolean("quick_notification_enabled", true)
        set(value) = prefs.edit().putBoolean("quick_notification_enabled", value).apply()

    var isFloatingBubbleEnabled: Boolean
        get() = prefs.getBoolean("floating_bubble_enabled", false)
        set(value) = prefs.edit().putBoolean("floating_bubble_enabled", value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word_enabled", true)
        set(value) = prefs.edit().putBoolean("wake_word_enabled", value).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat("wake_word_sensitivity", 0.70f)
        set(value) = prefs.edit().putFloat("wake_word_sensitivity", value).apply()

    var wakeWordPhrase: String
        get() = prefs.getString("wake_word_phrase", "Hey Echo") ?: "Hey Echo"
        set(value) = prefs.edit().putString("wake_word_phrase", value).apply()

    var customGeminiApiKey: String
        get() = prefs.getString("custom_gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("custom_gemini_api_key", value.trim()).apply()

    fun getActiveGeminiApiKey(): String {
        val custom = customGeminiApiKey.trim()
        if (custom.isNotBlank()) return custom

        return try {
            val buildKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun hasValidGeminiApiKey(): Boolean {
        return getActiveGeminiApiKey().isNotBlank()
    }
}
