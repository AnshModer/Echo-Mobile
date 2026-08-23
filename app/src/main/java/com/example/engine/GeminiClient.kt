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
