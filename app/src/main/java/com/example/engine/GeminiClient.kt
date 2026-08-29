package com.example.engine

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AssistantPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiTaskIntent(
    val action: String, // "FLASHLIGHT", "VOLUME", "BATTERY", "APP_LAUNCH", "TIMER", "ALARM", "NOTE", "CALL", "SMS", "WHATSAPP", "SCREENSHOT", "SETTINGS", "WEB_SEARCH", "NAVIGATION", "CALCULATION", "MEDIA", "YOUTUBE", "CHAT"
    val state: Boolean? = null,
    val level: Int? = null,
    val volumeDirection: String? = null, // "UP", "DOWN", "MUTE", "UNMUTE", "SET"
    val appName: String? = null,
    val timerSeconds: Int? = null,
    val alarmHour: Int? = null,
    val alarmMinute: Int? = null,
    val noteContent: String? = null,
    val callTarget: String? = null,
    val smsTarget: String? = null,
    val smsBody: String? = null,
    val whatsAppTarget: String? = null,
    val whatsAppMessage: String? = null,
    val settingsTarget: String? = null, // "WIFI", "BLUETOOTH", "DISPLAY", "SOUND", "BATTERY", "ASSISTANT", "GESTURE"
    val searchQuery: String? = null,
    val destination: String? = null,
    val mediaCommand: String? = null, // "PLAY", "PAUSE", "NEXT", "PREVIOUS", "SPOTIFY"
    val mediaTarget: String? = null,
    val youtubeQuery: String? = null,
    val expression: String? = null,
    val calculationResult: String? = null,
    val spokenResponse: String
)

class GeminiClient(private val context: Context? = null) {

    private val preferences: AssistantPreferences? = context?.let { AssistantPreferences(it) }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getActiveApiKey(): String {
        preferences?.let {
            val key = it.getActiveGeminiApiKey()
            if (key.isNotBlank()) return key
        }
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun hasActiveKey(): Boolean {
        return getActiveApiKey().isNotBlank()
    }

    /**
     * Uses Gemini 3.5 Flash to understand user requests, even if imperfect, slang, casual,
     * misspelled, or indirect, and maps them to concrete device tasks and actions.
     */
    suspend fun interpretAndExecuteTask(rawQuery: String): GeminiTaskIntent? = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) return@withContext null

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstruction = """
                You are Echo, an intelligent Android digital voice assistant.
                Your task is to understand what the user wants to do on their phone, even if they speak imperfectly, slang, informally, indirectly, casually, or in mixed Hindi/English phrases.

                You must categorize the intent into one of the following ACTIONS and provide parameters and a natural spoken voice response:

                ACTIONS:
                1. "FLASHLIGHT": Toggle or set flashlight/torch (e.g. "turn on light", "it's dark", "torch on karo", "turn off the torch", "give me light").
                   Parameters: "state": true/false
                2. "VOLUME": Adjust media volume or mute/unmute (e.g. "it's too loud", "turn it up", "mute", "unmute", "volume 70%", "sound kam karo", "lower volume").
                   Parameters: "volumeDirection": "UP"|"DOWN"|"MUTE"|"UNMUTE"|"SET", "level": 0-100 (integer)
                3. "BATTERY": Battery level and charging status (e.g. "how much juice is left", "battery percent", "is phone charging", "battery kitni hai").
                   Parameters: none
                4. "APP_LAUNCH": Open or start any installed app (e.g. "take a photo" or "selfie" -> camera, "open whatsapp", "launch calculator", "chrome", "insta", "gallery", "clock", "settings", etc.).
                   Parameters: "appName": name of app (e.g. "camera", "whatsapp", "instagram", "calculator", "gallery", "chrome")
                5. "TIMER": Start a countdown timer (e.g. "boil eggs for 5 mins", "timer for 30 seconds", "remind in 10 minutes").
                   Parameters: "timerSeconds": total integer seconds
                6. "ALARM": Set an alarm (e.g. "wake me up at 6:30 tomorrow", "alarm at 7 am", "set alarm for 8:15").
                   Parameters: "alarmHour": 0-23, "alarmMinute": 0-59
                7. "NOTE": Write or save a note / reminder memo (e.g. "note down: meeting with boss tomorrow", "remember to buy milk", "save note").
                   Parameters: "noteContent": text of note
                8. "CALL": Make a phone call (e.g. "call mom", "dial 9876543210", "phone John").
                   Parameters: "callTarget": contact name or phone number
                9. "SMS": Send an SMS text message (e.g. "text mom I am coming home", "sms to Rahul: reached safely").
                   Parameters: "smsTarget": contact or number, "smsBody": message text
                10. "WHATSAPP": Send a WhatsApp message or start WhatsApp chat (e.g. "send whatsapp message to mom I will be late", "whatsapp rahul saying reached home", "send whatsapp to 9876543210: meeting at 4pm", "message sarah on whatsapp", "open whatsapp chat").
                    Parameters: "whatsAppTarget": contact name or phone number, "whatsAppMessage": message text
                11. "SCREENSHOT": Take or capture a screenshot of current screen (e.g. "take a screenshot", "capture screen", "screenshot this", "screen capture", "take screen snap").
                    Parameters: none
                12. "SETTINGS": Open system settings (e.g. "turn on wifi settings", "bluetooth settings", "change display brightness", "assistant settings", "power button shortcuts").
                    Parameters: "settingsTarget": "WIFI"|"BLUETOOTH"|"DISPLAY"|"SOUND"|"BATTERY"|"ASSISTANT"|"GESTURE"
                13. "NAVIGATION": Open map directions (e.g. "take me to central park", "directions to nearest hospital", "navigate to airport").
                    Parameters: "destination": place name or address
                14. "WEB_SEARCH": Search the web / Google (e.g. "search for latest news", "google quantum computing").
                    Parameters: "searchQuery": query string
                15. "YOUTUBE": Search or play video/music on YouTube (e.g. "play coldplay yellow", "watch cat videos on youtube", "youtube arijit singh").
                    Parameters: "youtubeQuery": video or artist query
                16. "MEDIA": Music controls (e.g. "pause music", "stop song", "next song", "skip", "play something good on spotify").
                    Parameters: "mediaCommand": "PLAY"|"PAUSE"|"NEXT"|"PREVIOUS"|"SPOTIFY", "mediaTarget": optional song name
                17. "CALCULATION": Math calculation (e.g. "what is 24 times 15", "calculate 15 percent of 450").
                    Parameters: "expression": "24 * 15", "calculationResult": "360"
                18. "CHAT": General conversational queries, chit-chat, knowledge questions, explanations, advice, jokes, facts (e.g. "who was Albert Einstein", "tell me a joke", "how does photosynthesis work", "hello how are you").
                    Parameters: none

                Return a JSON object with this exact schema:
                {
                  "action": "<ACTION_NAME>",
                  "state": true/false,
                  "level": 0-100,
                  "volumeDirection": "UP"|"DOWN"|"MUTE"|"UNMUTE"|"SET",
                  "appName": "app name",
                  "timerSeconds": 180,
                  "alarmHour": 7,
                  "alarmMinute": 30,
                  "noteContent": "note string",
                  "callTarget": "name or number",
                  "smsTarget": "name or number",
                  "smsBody": "message string",
                  "whatsAppTarget": "name or number",
                  "whatsAppMessage": "message string",
                  "settingsTarget": "WIFI"|"BLUETOOTH"|"DISPLAY"|"SOUND"|"BATTERY"|"ASSISTANT"|"GESTURE",
                  "destination": "place string",
                  "searchQuery": "query string",
                  "youtubeQuery": "query string",
                  "mediaCommand": "PLAY"|"PAUSE"|"NEXT"|"PREVIOUS"|"SPOTIFY",
                  "mediaTarget": "song string",
                  "expression": "math string",
                  "calculationResult": "result string",
                  "spokenResponse": "Short natural voice response (1-2 sentences maximum, never use asterisks, markdown, bullets, or emojis as this is spoken by TTS)."
                }
            """.trimIndent()

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", "User spoken request: \"$rawQuery\"") })
                })
            }

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(userContent))
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 400)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArr = contentObj?.optJSONArray("parts")
            val rawJsonText = partsArr?.optJSONObject(0)?.optString("text")

            if (rawJsonText.isNullOrBlank()) return@withContext null

            val json = JSONObject(rawJsonText.trim())
            val action = json.optString("action", "CHAT").uppercase()
            val spokenResponse = cleanForSpeech(json.optString("spokenResponse", ""))

            GeminiTaskIntent(
                action = action,
                state = if (json.has("state")) json.optBoolean("state") else null,
                level = if (json.has("level")) json.optInt("level") else null,
                volumeDirection = if (json.has("volumeDirection")) json.optString("volumeDirection") else null,
                appName = if (json.has("appName")) json.optString("appName") else null,
                timerSeconds = if (json.has("timerSeconds")) json.optInt("timerSeconds") else null,
                alarmHour = if (json.has("alarmHour")) json.optInt("alarmHour") else null,
                alarmMinute = if (json.has("alarmMinute")) json.optInt("alarmMinute") else null,
                noteContent = if (json.has("noteContent")) json.optString("noteContent") else null,
                callTarget = if (json.has("callTarget")) json.optString("callTarget") else null,
                smsTarget = if (json.has("smsTarget")) json.optString("smsTarget") else null,
                smsBody = if (json.has("smsBody")) json.optString("smsBody") else null,
                whatsAppTarget = if (json.has("whatsAppTarget")) json.optString("whatsAppTarget") else null,
                whatsAppMessage = if (json.has("whatsAppMessage")) json.optString("whatsAppMessage") else null,
                settingsTarget = if (json.has("settingsTarget")) json.optString("settingsTarget") else null,
                searchQuery = if (json.has("searchQuery")) json.optString("searchQuery") else null,
                destination = if (json.has("destination")) json.optString("destination") else null,
                mediaCommand = if (json.has("mediaCommand")) json.optString("mediaCommand") else null,
                mediaTarget = if (json.has("mediaTarget")) json.optString("mediaTarget") else null,
                youtubeQuery = if (json.has("youtubeQuery")) json.optString("youtubeQuery") else null,
                expression = if (json.has("expression")) json.optString("expression") else null,
                calculationResult = if (json.has("calculationResult")) json.optString("calculationResult") else null,
                spokenResponse = if (spokenResponse.isNotBlank()) spokenResponse else "Done."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun askAssistant(prompt: String, conversationContext: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()

        if (apiKey.isBlank()) {
            // Friendly local offline AI fallback response
            return@withContext getOfflineAiResponse(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstruction = """
                You are Echo, an intelligent, sleek, fast, and warm digital voice assistant for Android.
                You understand voice queries, chit-chat, knowledge questions, explanations, and advice.
                Respond with concise, friendly, and natural conversational answers crafted specifically for speech playback (1 to 3 short sentences maximum unless the user explicitly asks for a long detailed explanation).
                CRITICAL: Never output markdown syntax, asterisks, bullet points, hashtags, emojis, or code blocks because this text is read aloud by Text-To-Speech. Speak naturally as a human assistant.
            """.trimIndent()

            val contentsArray = JSONArray()
            
            if (conversationContext.isNotBlank()) {
                val ctxContent = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Context: $conversationContext") })
                    })
                }
                contentsArray.put(ctxContent)
            }

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            contentsArray.put(userContent)

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext getOfflineAiResponse(prompt)
            }

            val responseBody = response.body?.string() ?: return@withContext getOfflineAiResponse(prompt)
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArr = contentObj?.optJSONArray("parts")
            val text = partsArr?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                cleanForSpeech(text.trim())
            } else {
                getOfflineAiResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineAiResponse(prompt)
        }
    }

    private fun cleanForSpeech(rawText: String): String {
        return rawText
            .replace(Regex("[*#_`~]"), "") // Strip markdown symbols
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun getOfflineAiResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I am Echo, your personal digital assistant powered by Gemini. I can control your device, adjust settings, launch apps, set timers, take notes, and answer your questions."
            lower.contains("hello") || lower.contains("hi echo") || lower.contains("hey echo") ->
                "Hello! How can I help you with your device today?"
            lower.contains("how are you") ->
                "I'm running smoothly and ready for your commands! How may I assist you?"
            lower.contains("joke") ->
                "Why did the smartphone need glasses? Because it lost all its contacts!"
            lower.contains("weather") ->
                "You can ask me to open Google Weather or search for local forecasts anytime."
            lower.contains("time") ->
                "The current time is " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            lower.contains("date") ->
                "Today is " + java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            lower.contains("thank") ->
                "You're very welcome! Let me know if you need anything else."
            lower.contains("siri") ->
                "I'm Echo, inspired by modern digital voice assistants, crafted to give you total control over your Android device."
            lower.contains("redmi") || lower.contains("xiaomi") ->
                "Echo is tailored for Android devices like your Redmi Note 12! You can set me as your Default Digital Assistant in Settings."
            else ->
                "I heard: \"$prompt\". Configure your Gemini API key in Settings or AI Studio Secrets for full conversational intelligence."
        }
    }
}
