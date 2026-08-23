package com.example.engine

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askAssistant(prompt: String, conversationContext: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Friendly local offline AI fallback response
            return@withContext getOfflineAiResponse(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstruction = "You are Echo, an intelligent, sleek, fast, and helpful digital voice assistant. Respond with concise, friendly, and natural answers suitable for voice playback (1-3 sentences maximum unless asked for detailed explanations)."

            val contentsArray = JSONArray()
            val userContent = JSONObject()
            userContent.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", prompt)
            parts.put(part)
            userContent.put("parts", parts)
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
                    put("maxOutputTokens", 250)
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
                text.trim()
            } else {
                getOfflineAiResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineAiResponse(prompt)
        }
    }

    private fun getOfflineAiResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I am Echo, your personal digital assistant. I can control your device, adjust settings, launch apps, set timers, take notes, and answer your questions."
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
                "I heard: \"$prompt\". You can control flashlight, volume, alarms, timers, apps, or configure my digital assistant shortcut in settings."
        }
    }
}
