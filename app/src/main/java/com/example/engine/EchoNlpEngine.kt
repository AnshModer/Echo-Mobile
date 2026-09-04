package com.example.engine

import android.content.Context
import com.example.data.local.CommandHistoryItem
import com.example.data.local.EchoDatabase
import com.example.data.local.VoiceNoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

sealed class ActionResult(
    val responseText: String,
    val actionType: String,
    val isDirectDeviceAction: Boolean = false
) {
    class FlashlightAction(text: String, val isTurnedOn: Boolean) : ActionResult(text, "FLASHLIGHT", true)
    class VolumeAction(text: String, val level: Int) : ActionResult(text, "VOLUME", true)
    class BatteryAction(text: String, val batteryInfo: BatteryInfo) : ActionResult(text, "BATTERY", true)
    class AppLaunchAction(text: String, val appName: String, val isSuccess: Boolean) : ActionResult(text, "APP_LAUNCH", true)
    class TimerAction(text: String, val seconds: Int) : ActionResult(text, "TIMER", true)
    class AlarmAction(text: String, val timeFormatted: String) : ActionResult(text, "ALARM", true)
    class NoteAction(text: String, val noteText: String) : ActionResult(text, "NOTE", true)
    class CallAction(text: String, val target: String) : ActionResult(text, "CALL", true)
    class WhatsAppAction(text: String, val target: String, val message: String) : ActionResult(text, "WHATSAPP", true)
    class ScreenshotAction(text: String, val isCaptured: Boolean) : ActionResult(text, "SCREENSHOT", true)
    class SettingsAction(text: String, val settingName: String) : ActionResult(text, "SETTINGS", true)
    class WebAction(text: String, val query: String) : ActionResult(text, "WEB_SEARCH", true)
    class CalculationAction(text: String, val expression: String, val resultValue: String) : ActionResult(text, "CALCULATION", true)
    class MediaAction(text: String, val command: String) : ActionResult(text, "MEDIA", true)
    class YouTubeAction(text: String, val query: String) : ActionResult(text, "YOUTUBE", true)
    class AiResponse(text: String) : ActionResult(text, "AI_CHAT", false)
}

class EchoNlpEngine(
    private val context: Context,
    private val deviceController: DeviceController,
    private val geminiClient: GeminiClient = GeminiClient(context)
) {
    private val database = EchoDatabase.getDatabase(context)

    suspend fun processQuery(rawQuery: String): ActionResult = withContext(Dispatchers.IO) {
        val clean = rawQuery.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // 1. If Gemini API Key is available, use Gemini's deep intent understanding to execute tasks
        // even if the user speaks imperfectly, informally, in slang, or indirectly.
        if (geminiClient.hasActiveKey()) {
            val geminiIntent = geminiClient.interpretAndExecuteTask(clean)
            if (geminiIntent != null) {
                val actionResult = executeGeminiTask(geminiIntent, clean)
                recordHistory(clean, actionResult)
                return@withContext actionResult
            }
        }

        // 2. Fast local rule-based engine (and offline fallback)
        val result: ActionResult = processLocalQuery(clean, lower)
        recordHistory(clean, result)
        result
    }

    private suspend fun executeGeminiTask(intent: GeminiTaskIntent, rawQuery: String): ActionResult {
        val clean = rawQuery.trim()
        return when (intent.action.uppercase(Locale.ROOT)) {
            "FLASHLIGHT" -> {
                val state = intent.state ?: !DeviceController.isFlashlightOn
                deviceController.toggleFlashlight(state)
                val speech = intent.spokenResponse.ifBlank {
                    if (state) "Flashlight turned on." else "Flashlight turned off."
                }
                ActionResult.FlashlightAction(speech, state)
            }

            "VOLUME" -> {
                val direction = intent.volumeDirection?.uppercase(Locale.ROOT)
                when (direction) {
                    "MUTE" -> {
                        val res = deviceController.muteVolume(true)
                        val speech = intent.spokenResponse.ifBlank { res }
                        ActionResult.VolumeAction(speech, 0)
                    }
                    "UNMUTE" -> {
                        val res = deviceController.muteVolume(false)
                        val speech = intent.spokenResponse.ifBlank { res }
                        ActionResult.VolumeAction(speech, 50)
                    }
                    "UP" -> {
                        val res = deviceController.adjustVolume(true)
                        val vol = deviceController.getVolumeInfo().mediaPercent
                        val speech = intent.spokenResponse.ifBlank { res }
                        ActionResult.VolumeAction(speech, vol)
                    }
                    "DOWN" -> {
                        val res = deviceController.adjustVolume(false)
                        val vol = deviceController.getVolumeInfo().mediaPercent
                        val speech = intent.spokenResponse.ifBlank { res }
                        ActionResult.VolumeAction(speech, vol)
                    }
                    else -> {
                        val lvl = (intent.level ?: 50).coerceIn(0, 100)
                        val res = deviceController.setMediaVolumePercent(lvl)
                        val speech = intent.spokenResponse.ifBlank { res }
                        ActionResult.VolumeAction(speech, lvl)
                    }
                }
            }

            "BATTERY" -> {
                val info = deviceController.getBatteryInfo()
                val speech = intent.spokenResponse.ifBlank {
                    val statusText = if (info.isCharging) "charging (${info.chargeType})" else "remaining"
                    "Your battery is at ${info.level}% and currently $statusText. Temperature is ${info.temperatureCelsius}°C."
                }
                ActionResult.BatteryAction(speech, info)
            }

            "APP_LAUNCH" -> {
                val app = intent.appName?.trim()?.ifBlank { "camera" } ?: "camera"
                val res = deviceController.openApp(app)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.AppLaunchAction(speech, app, res.first)
            }

            "TIMER" -> {
                val seconds = (intent.timerSeconds ?: 60).coerceAtLeast(1)
                val res = deviceController.setTimer(seconds)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.TimerAction(speech, seconds)
            }

            "ALARM" -> {
                val hour = (intent.alarmHour ?: 7).coerceIn(0, 23)
                val minute = (intent.alarmMinute ?: 0).coerceIn(0, 59)
                val res = deviceController.setAlarm(hour, minute, "Echo Assistant Alarm")
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.AlarmAction(speech, formatted)
            }

            "NOTE" -> {
                val noteBody = intent.noteContent?.trim()?.ifBlank { clean } ?: clean
                val title = if (noteBody.length > 25) noteBody.substring(0, 25) + "..." else noteBody
                database.assistantDao().insertNote(
                    VoiceNoteItem(title = title, content = noteBody)
                )
                val speech = intent.spokenResponse.ifBlank { "Note saved: \"$noteBody\"" }
                ActionResult.NoteAction(speech, noteBody)
            }

            "CALL" -> {
                val target = intent.callTarget?.trim()?.ifBlank { clean } ?: clean
                val res = deviceController.makeCall(target)
                val speech = res.second
                ActionResult.CallAction(speech, target)
            }

            "SMS" -> {
                val target = intent.smsTarget?.trim() ?: ""
                val body = intent.smsBody?.trim() ?: ""
                val res = deviceController.sendSms(target, body)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.CallAction(speech, target)
            }

            "WHATSAPP" -> {
                val target = intent.whatsAppTarget?.trim() ?: ""
                val body = intent.whatsAppMessage?.trim() ?: ""
                val res = deviceController.sendWhatsAppMessage(target, body)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.WhatsAppAction(speech, target, body)
            }

            "SCREENSHOT" -> {
                val res = deviceController.captureScreenshot()
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.ScreenshotAction(speech, res.first)
            }

            "SETTINGS" -> {
                val target = intent.settingsTarget?.uppercase(Locale.ROOT) ?: "SETTINGS"
                when (target) {
                    "WIFI" -> deviceController.openWifiSettings()
                    "BLUETOOTH" -> deviceController.openBluetoothSettings()
                    "DISPLAY", "BRIGHTNESS" -> deviceController.openDisplaySettings()
                    "SOUND", "VOLUME" -> deviceController.openSoundSettings()
                    "BATTERY" -> deviceController.openBatterySettings()
                    "ASSISTANT" -> deviceController.openDefaultAssistantSettings()
                    "GESTURE", "SHORTCUT" -> deviceController.openRedmiGestureSettings()
                    else -> deviceController.openSettingsScreen(android.provider.Settings.ACTION_SETTINGS)
                }
                val speech = intent.spokenResponse.ifBlank { "Opening $target settings." }
                ActionResult.SettingsAction(speech, target)
            }

            "NAVIGATION" -> {
                val dest = intent.destination?.trim()?.ifBlank { clean } ?: clean
                val res = deviceController.openMapsNavigation(dest)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.WebAction(speech, dest)
            }

            "WEB_SEARCH" -> {
                val query = intent.searchQuery?.trim()?.ifBlank { clean } ?: clean
                val res = deviceController.webSearch(query)
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.WebAction(speech, query)
            }

            "YOUTUBE" -> {
                val ytQ = intent.youtubeQuery?.trim()?.ifBlank { clean } ?: clean
                val res = if (clean.contains("play", ignoreCase = true) || intent.mediaCommand?.equals("PLAY", ignoreCase = true) == true) {
                    deviceController.playOnYouTube(ytQ)
                } else {
                    deviceController.searchYouTube(ytQ)
                }
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.YouTubeAction(speech, ytQ)
            }

            "MEDIA" -> {
                val cmd = intent.mediaCommand?.uppercase(Locale.ROOT) ?: "PLAY"
                val res = when (cmd) {
                    "PAUSE", "STOP" -> deviceController.pauseMusic()
                    "NEXT" -> deviceController.nextTrack()
                    "PREVIOUS" -> deviceController.previousTrack()
                    "SPOTIFY" -> deviceController.playOnSpotify(intent.mediaTarget ?: clean)
                    else -> deviceController.playMusic(intent.mediaTarget)
                }
                val speech = intent.spokenResponse.ifBlank { res.second }
                ActionResult.MediaAction(speech, cmd)
            }

            "CALCULATION" -> {
                val expr = intent.expression ?: clean
                val calcRes = intent.calculationResult ?: ""
                val speech = intent.spokenResponse.ifBlank {
                    if (calcRes.isNotBlank()) "$expr equals $calcRes" else "Calculated $expr"
                }
                ActionResult.CalculationAction(speech, expr, calcRes)
            }

            "CHAT" -> {
                ActionResult.AiResponse(intent.spokenResponse)
            }

            else -> {
                ActionResult.AiResponse(intent.spokenResponse)
            }
        }
    }

    private suspend fun processLocalQuery(clean: String, lower: String): ActionResult {
        return when {
            // --- MATH & CALCULATIONS ---
            isMathCalculationQuery(lower) -> {
                val calcResult = evaluateMathQuery(clean)
                if (calcResult != null) {
                    ActionResult.CalculationAction(calcResult.second, calcResult.first, calcResult.third)
                } else if (lower.contains("calculator")) {
                    val res = deviceController.openCalculator()
                    ActionResult.AppLaunchAction(res.second, "Calculator", res.first)
                } else {
                    val aiResponseText = geminiClient.askAssistant(clean)
                    ActionResult.AiResponse(aiResponseText)
                }
            }

            // --- YOUTUBE SEARCH & PLAY ---
            isYouTubeQuery(lower) -> {
                val ytQuery = extractYouTubeQuery(clean)
                if (ytQuery.isBlank() || ytQuery.equals("youtube", ignoreCase = true)) {
                    val res = deviceController.openApp("youtube")
                    ActionResult.YouTubeAction(res.second, "YouTube")
                } else if (lower.contains("play")) {
                    val res = deviceController.playOnYouTube(ytQuery)
                    ActionResult.YouTubeAction(res.second, ytQuery)
                } else {
                    val res = deviceController.searchYouTube(ytQuery)
                    ActionResult.YouTubeAction(res.second, ytQuery)
                }
            }

            // --- MUSIC & MEDIA CONTROLS ---
            isMusicOrMediaCommand(lower) -> {
                when {
                    lower.contains("pause") || lower.contains("stop music") || lower == "stop" -> {
                        val res = deviceController.pauseMusic()
                        ActionResult.MediaAction(res.second, "PAUSE")
                    }
                    lower.contains("next") || lower.contains("skip") -> {
                        val res = deviceController.nextTrack()
                        ActionResult.MediaAction(res.second, "NEXT")
                    }
                    lower.contains("previous") || lower.contains("prev track") -> {
                        val res = deviceController.previousTrack()
                        ActionResult.MediaAction(res.second, "PREVIOUS")
                    }
                    lower.contains("spotify") -> {
                        val spotQuery = clean.replace(Regex("^(?i)(play|search)\\s+"), "")
                            .replace(Regex("(?i)\\s+on spotify"), "").trim()
                        val res = deviceController.playOnSpotify(spotQuery)
                        ActionResult.MediaAction(res.second, "SPOTIFY")
                    }
                    else -> {
                        // Play music / resume
                        val musicTarget = clean.replace(Regex("^(?i)(play music|play song|play songs|play|resume music)\\s*"), "").trim()
                        val res = deviceController.playMusic(if (musicTarget.isNotEmpty()) musicTarget else null)
                        ActionResult.MediaAction(res.second, "PLAY")
                    }
                }
            }

            // --- FLASHLIGHT / TORCH ---
            isFlashlightOnQuery(lower) -> {
                val success = deviceController.toggleFlashlight(forceState = true)
                val msg = if (success) "Flashlight turned on." else "Flashlight turned on."
                ActionResult.FlashlightAction(msg, true)
            }
            isFlashlightOffQuery(lower) -> {
                val success = deviceController.toggleFlashlight(forceState = false)
                val msg = if (success) "Flashlight turned off." else "Flashlight turned off."
                ActionResult.FlashlightAction(msg, false)
            }
            isFlashlightToggleQuery(lower) -> {
                val newState = !DeviceController.isFlashlightOn
                deviceController.toggleFlashlight(newState)
                val msg = if (newState) "Flashlight turned on." else "Flashlight turned off."
                ActionResult.FlashlightAction(msg, newState)
            }

            // --- VOLUME CONTROLS ---
            lower.contains("mute") && !lower.contains("unmute") -> {
                val res = deviceController.muteVolume(true)
                ActionResult.VolumeAction(res, 0)
            }
            lower.contains("unmute") -> {
                val res = deviceController.muteVolume(false)
                ActionResult.VolumeAction(res, 50)
            }
            lower.contains("volume up") || lower.contains("increase volume") || lower.contains("raise volume") -> {
                val res = deviceController.adjustVolume(true)
                val vol = deviceController.getVolumeInfo().mediaPercent
                ActionResult.VolumeAction(res, vol)
            }
            lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("lower volume") -> {
                val res = deviceController.adjustVolume(false)
                val vol = deviceController.getVolumeInfo().mediaPercent
                ActionResult.VolumeAction(res, vol)
            }
            lower.contains("volume") && (lower.contains("%") || lower.matches(Regex(".*volume.*\\d+.*"))) -> {
                val percent = extractNumber(lower) ?: 50
                val res = deviceController.setMediaVolumePercent(percent)
                ActionResult.VolumeAction(res, percent)
            }

            // --- BATTERY STATUS ---
            lower.contains("battery") || lower.contains("power level") || lower.contains("charge level") -> {
                val info = deviceController.getBatteryInfo()
                val statusText = if (info.isCharging) "charging (${info.chargeType})" else "remaining"
                val speech = "Your battery is at ${info.level}% and currently $statusText. Temperature is ${info.temperatureCelsius}°C."
                ActionResult.BatteryAction(speech, info)
            }

            // --- TAKE NOTES ---
            lower.startsWith("take a note") || lower.startsWith("note down") || lower.startsWith("write down") || lower.startsWith("create note") || lower.startsWith("note:") -> {
                val noteBody = clean
                    .replace(Regex("^(?i)(take a note|note down|write down|create note|note:?\\s*(that)?)\\s*:?"), "")
                    .trim()
                if (noteBody.isNotEmpty()) {
                    val title = if (noteBody.length > 25) noteBody.substring(0, 25) + "..." else noteBody
                    database.assistantDao().insertNote(
                        VoiceNoteItem(title = title, content = noteBody)
                    )
                    ActionResult.NoteAction("Note saved: \"$noteBody\"", noteBody)
                } else {
                    ActionResult.NoteAction("What would you like me to note down?", "")
                }
            }

            // --- TIMERS ---
            lower.contains("timer") -> {
                val seconds = extractTimerSeconds(lower)
                val res = deviceController.setTimer(seconds)
                ActionResult.TimerAction(res.second, seconds)
            }

            // --- ALARMS ---
            lower.contains("alarm") || lower.contains("wake me up") -> {
                val time = extractAlarmTime(lower)
                val res = deviceController.setAlarm(time.first, time.second, "Echo Assistant Alarm")
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", time.first, time.second)
                ActionResult.AlarmAction(res.second, formatted)
            }

            // --- APP LAUNCHER ---
            lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") -> {
                val targetApp = clean.replace(Regex("^(?i)(open|launch|start)\\s+"), "").trim()
                val res = deviceController.openApp(targetApp)
                ActionResult.AppLaunchAction(res.second, targetApp, res.first)
            }

            // --- SCREENSHOT COMMANDS ---
            isScreenshotQuery(lower) -> {
                if (lower.contains("share")) {
                    val res = deviceController.shareLastScreenshot()
                    ActionResult.ScreenshotAction(res.second, res.first)
                } else {
                    val res = deviceController.captureScreenshot()
                    ActionResult.ScreenshotAction(res.second, res.first)
                }
            }

            // --- WHATSAPP MESSAGES ---
            isWhatsAppQuery(lower) -> {
                val (waTarget, waMessage) = extractWhatsAppTargetAndMessage(clean)
                val res = deviceController.sendWhatsAppMessage(waTarget, waMessage)
                ActionResult.WhatsAppAction(res.second, waTarget, waMessage)
            }

            // --- PHONE CALLS ---
            lower == "call" || lower == "dial" || lower == "phone" || lower == "make a call" -> {
                ActionResult.CallAction(
                    "Who would you like to call? Please say a contact name or phone number.",
                    ""
                )
            }
            lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("phone ") ||
                    lower.startsWith("ring ") || lower.startsWith("make a call to ") ||
                    lower.startsWith("place a call to ") || lower.startsWith("can you call ") ||
                    lower.startsWith("please call ") || lower.startsWith("call to ") -> {
                val target = clean
                    .replace(Regex("^(?i)(please\\s+)?(can\\s+you\\s+)?(make\\s+a\\s+call\\s+to|place\\s+a\\s+call\\s+to|call\\s+to|call|dial|phone|ring)\\s+"), "")
                    .trim()
                val res = deviceController.makeCall(target)
                ActionResult.CallAction(res.second, target)
            }

            // --- WEB SEARCH & NAVIGATION ---
            lower.startsWith("search ") || lower.startsWith("google ") -> {
                val query = clean.replace(Regex("^(?i)(search|google|search for)\\s+"), "").trim()
                val res = deviceController.webSearch(query)
                ActionResult.WebAction(res.second, query)
            }
            lower.startsWith("navigate to ") || lower.startsWith("directions to ") -> {
                val dest = clean.replace(Regex("^(?i)(navigate to|directions to)\\s+"), "").trim()
                val res = deviceController.openMapsNavigation(dest)
                ActionResult.WebAction(res.second, dest)
            }

            // --- SETTINGS SHORTCUTS ---
            lower.contains("wifi") || lower.contains("wi-fi") -> {
                val res = deviceController.openWifiSettings()
                ActionResult.SettingsAction("Opening Wi-Fi Settings", "Wi-Fi")
            }
            lower.contains("bluetooth") -> {
                val res = deviceController.openBluetoothSettings()
                ActionResult.SettingsAction("Opening Bluetooth Settings", "Bluetooth")
            }
            lower.contains("display") || lower.contains("brightness") || lower.contains("screen") -> {
                val res = deviceController.openDisplaySettings()
                ActionResult.SettingsAction("Opening Display Settings", "Display")
            }
            lower.contains("sound") || lower.contains("ringtone") -> {
                val res = deviceController.openSoundSettings()
                ActionResult.SettingsAction("Opening Sound Settings", "Sound")
            }
            lower.contains("assistant settings") || lower.contains("default assistant") || lower.contains("default app") -> {
                deviceController.openDefaultAssistantSettings()
                ActionResult.SettingsAction("Opening Default Assistant Settings", "Assistant")
            }
            lower.contains("gesture") || lower.contains("power button") || lower.contains("redmi shortcut") -> {
                deviceController.openRedmiGestureSettings()
                ActionResult.SettingsAction("Opening Gesture Shortcuts Settings", "Gesture Shortcuts")
            }

            // --- GENERAL AI QUERY VIA GEMINI ---
            else -> {
                val aiResponseText = geminiClient.askAssistant(clean)
                ActionResult.AiResponse(aiResponseText)
            }
        }
    }

    private suspend fun recordHistory(query: String, result: ActionResult) {
        try {
            database.assistantDao().insertHistory(
                CommandHistoryItem(
                    queryText = query,
                    responseText = result.responseText,
                    actionType = result.actionType
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isFlashlightOnQuery(query: String): Boolean {
        return (query.contains("flashlight") || query.contains("torch")) &&
                (query.contains("on") || query.contains("enable") || query.contains("activate") || query.contains("start"))
    }

    private fun isFlashlightOffQuery(query: String): Boolean {
        return (query.contains("flashlight") || query.contains("torch")) &&
                (query.contains("off") || query.contains("disable") || query.contains("deactivate") || query.contains("stop"))
    }

    private fun isFlashlightToggleQuery(query: String): Boolean {
        return query == "flashlight" || query == "torch" || query.contains("toggle flashlight") || query.contains("toggle torch")
    }

    private fun extractNumber(text: String): Int? {
        val matcher = Pattern.compile("\\d+").matcher(text)
        return if (matcher.find()) {
            matcher.group().toIntOrNull()
        } else null
    }

    private fun extractTimerSeconds(text: String): Int {
        val minMatcher = Pattern.compile("(\\d+)\\s*(min|minute|minutes)").matcher(text)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toIntOrNull() ?: 1
            val secMatcher = Pattern.compile("(\\d+)\\s*(sec|second|seconds)").matcher(text)
            val secs = if (secMatcher.find()) secMatcher.group(1)?.toIntOrNull() ?: 0 else 0
            return mins * 60 + secs
        }

        val secMatcher = Pattern.compile("(\\d+)\\s*(sec|second|seconds)").matcher(text)
        if (secMatcher.find()) {
            return secMatcher.group(1)?.toIntOrNull() ?: 30
        }

        val plainNumber = extractNumber(text) ?: 5
        return plainNumber * 60 // default to minutes
    }

    private fun extractAlarmTime(text: String): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        val timeMatcher = Pattern.compile("(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE).matcher(text)
        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1)?.toIntOrNull() ?: 7
            val min = timeMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = timeMatcher.group(3)?.lowercase(Locale.ROOT)
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, min)
        }

        val hourOnlyMatcher = Pattern.compile("(\\d{1,2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE).matcher(text)
        if (hourOnlyMatcher.find()) {
            var hour = hourOnlyMatcher.group(1)?.toIntOrNull() ?: 7
            val ampm = hourOnlyMatcher.group(2)?.lowercase(Locale.ROOT)
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, 0)
        }

        // Default to next morning at 7:00 AM
        return Pair(7, 0)
    }

    // --- MATH & CALCULATION HELPERS ---
    private fun isMathCalculationQuery(text: String): Boolean {
        if (text.startsWith("calculate") || text.startsWith("calc ") || text == "calculator" || text.contains("open calculator")) {
            return true
        }
        if (text.contains("percent of") || text.contains("% of") || text.contains("square root") || text.contains("sqrt")) {
            return true
        }
        if (text.startsWith("what is ") && (text.contains("+") || text.contains("-") || text.contains("*") ||
                    text.contains("/") || text.contains("x") || text.contains("times") ||
                    text.contains("plus") || text.contains("minus") || text.contains("divided by") || text.contains("multiplied by"))) {
            return true
        }
        // Direct arithmetic like "25 * 4" or "100 / 2"
        return text.matches(Regex("^[\\d.\\s+\\-*/xX%^()]+$")) && text.any { it in "+-*/xX%^" }
    }

    private fun evaluateMathQuery(raw: String): Triple<String, String, String>? {
        try {
            var expr = raw.lowercase(Locale.ROOT)
                .replace(Regex("^(?i)(calculate|calc|what is|how much is|solve|evaluate)\\s*"), "")
                .replace("?", "")
                .trim()

            // 1. Percentage: "15% of 80" or "15 percent of 80"
            val percentMatcher = Pattern.compile("([\\d.]+)\\s*(%|percent)\\s+of\\s+([\\d.]+)").matcher(expr)
            if (percentMatcher.find()) {
                val pct = percentMatcher.group(1)?.toDoubleOrNull() ?: return null
                val total = percentMatcher.group(3)?.toDoubleOrNull() ?: return null
                val ans = (pct / 100.0) * total
                val formattedAns = if (ans % 1.0 == 0.0) ans.toLong().toString() else String.format(Locale.ROOT, "%.2f", ans)
                val cleanPct = if (pct % 1.0 == 0.0) pct.toLong().toString() else pct.toString()
                val cleanTotal = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                return Triple(
                    "$cleanPct% of $cleanTotal",
                    "$cleanPct percent of $cleanTotal is $formattedAns.",
                    formattedAns
                )
            }

            // 2. Square root: "square root of 144" or "sqrt of 144" or "sqrt 144"
            val sqrtMatcher = Pattern.compile("(square root of|sqrt of|sqrt)\\s+([\\d.]+)").matcher(expr)
            if (sqrtMatcher.find()) {
                val num = sqrtMatcher.group(2)?.toDoubleOrNull() ?: return null
                val ans = Math.sqrt(num)
                val formattedAns = if (ans % 1.0 == 0.0) ans.toLong().toString() else String.format(Locale.ROOT, "%.4f", ans)
                val cleanNum = if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                return Triple(
                    "√$cleanNum",
                    "The square root of $cleanNum is $formattedAns.",
                    formattedAns
                )
            }

            // 3. Power: "2 power 8" or "2 to the power of 8" or "2^8"
            val powerMatcher = Pattern.compile("([\\d.]+)\\s*(to the power of|power of|power|\\^)\\s*([\\d.]+)").matcher(expr)
            if (powerMatcher.find()) {
                val base = powerMatcher.group(1)?.toDoubleOrNull() ?: return null
                val exp = powerMatcher.group(3)?.toDoubleOrNull() ?: return null
                val ans = Math.pow(base, exp)
                val formattedAns = if (ans % 1.0 == 0.0) ans.toLong().toString() else String.format(Locale.ROOT, "%.4f", ans)
                return Triple(
                    "$base ^ $exp",
                    "$base to the power of $exp equals $formattedAns.",
                    formattedAns
                )
            }

            // 4. Standard arithmetic replacement
            expr = expr
                .replace("multiplied by", "*")
                .replace("times", "*")
                .replace("divided by", "/")
                .replace("plus", "+")
                .replace("minus", "-")
                .replace(" x ", " * ")
                .replace("x", "*")
                .replace(" ", "")

            // Parse two-operand arithmetic (e.g. 25 * 4, 1500 / 3, 50 + 75, 100 - 35)
            val binaryMatcher = Pattern.compile("([\\d.]+)\\s*([+\\-*/%])\\s*([\\d.]+)").matcher(expr)
            if (binaryMatcher.find()) {
                val num1 = binaryMatcher.group(1)?.toDoubleOrNull() ?: return null
                val op = binaryMatcher.group(2) ?: "+"
                val num2 = binaryMatcher.group(3)?.toDoubleOrNull() ?: return null

                val ans = when (op) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*" -> num1 * num2
                    "/" -> if (num2 != 0.0) num1 / num2 else Double.NaN
                    "%" -> num1 % num2
                    else -> num1 + num2
                }

                if (ans.isNaN() || ans.isInfinite()) {
                    return Triple(raw, "Cannot divide by zero.", "Error")
                }

                val formattedAns = if (ans % 1.0 == 0.0) ans.toLong().toString() else String.format(Locale.ROOT, "%.2f", ans)
                val opWord = when (op) {
                    "+" -> "plus"
                    "-" -> "minus"
                    "*" -> "multiplied by"
                    "/" -> "divided by"
                    "%" -> "mod"
                    else -> "and"
                }
                val clean1 = if (num1 % 1.0 == 0.0) num1.toLong().toString() else num1.toString()
                val clean2 = if (num2 % 1.0 == 0.0) num2.toLong().toString() else num2.toString()

                return Triple(
                    "$clean1 $op $clean2",
                    "$clean1 $opWord $clean2 equals $formattedAns.",
                    formattedAns
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // --- YOUTUBE HELPERS ---
    private fun isYouTubeQuery(text: String): Boolean {
        return text.contains("youtube") || text.startsWith("play on youtube") ||
                (text.startsWith("play ") && text.contains("video"))
    }

    private fun extractYouTubeQuery(raw: String): String {
        return raw.replace(Regex("^(?i)(open|search|play|find|look up|show me)\\s+"), "")
            .replace(Regex("^(?i)(youtube for|on youtube|youtube)\\s*"), "")
            .replace(Regex("(?i)\\s+on youtube"), "")
            .replace(Regex("(?i)^youtube\\s*"), "")
            .trim()
    }

    // --- MUSIC / MEDIA HELPERS ---
    private fun isMusicOrMediaCommand(text: String): Boolean {
        return text.startsWith("play music") || text.startsWith("play song") || text.startsWith("play songs") ||
                text.startsWith("play audio") || text == "play" || text == "resume" || text == "resume music" ||
                text.startsWith("pause") || text.startsWith("stop music") || text == "stop" ||
                text.contains("next song") || text.contains("skip song") || text.contains("next track") ||
                text.contains("previous song") || text.contains("previous track") ||
                text.contains("on spotify") || text.startsWith("spotify ")
    }

    // --- SCREENSHOT HELPERS ---
    private fun isScreenshotQuery(text: String): Boolean {
        return text.contains("screenshot") || text.contains("screen shot") ||
                text.contains("capture screen") || text.contains("screen capture") ||
                text.contains("snap screen") || text.contains("take a snap") ||
                text == "take snap" || text == "snap of screen" || text.contains("screen snap")
    }

    // --- WHATSAPP HELPERS ---
    private fun isWhatsAppQuery(text: String): Boolean {
        return text.contains("whatsapp") || text.contains("whats app")
    }

    private fun extractWhatsAppTargetAndMessage(raw: String): Pair<String, String> {
        val clean = raw.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // If it's just "open whatsapp" or "whatsapp"
        if (lower == "whatsapp" || lower == "open whatsapp" || lower == "launch whatsapp" || lower == "start whatsapp") {
            return Pair("", "")
        }

        // Pattern: "message [target] on whatsapp ..." or "text [target] on whatsapp ..."
        if (lower.startsWith("message ") || lower.startsWith("text ")) {
            val waIndex = lower.indexOf(" on whatsapp")
            if (waIndex != -1) {
                val target = clean.substring(clean.indexOf(' ') + 1, waIndex).trim()
                val afterWa = clean.substring(waIndex + " on whatsapp".length).trim()
                val msg = afterWa.replace(Regex("^(?i)(saying|that|:|message|with message)?\\s*"), "").trim()
                return Pair(target, msg)
            }
        }

        // Pattern: "send [a] whatsapp [message] to [target] [delimiters] [message]"
        val prefixRegex = Regex("^(?i)(please\\s+)?(can\\s+you\\s+)?(send\\s+(a\\s+)?whatsapp\\s+(message\\s+)?to|whatsapp\\s+message\\s+to|whatsapp\\s+to|send\\s+message\\s+on\\s+whatsapp\\s+to)\\s+")
        val matchPrefix = prefixRegex.find(clean)
        if (matchPrefix != null) {
            val remainder = clean.substring(matchPrefix.range.last + 1).trim()
            val delimRegex = Regex("(?i)\\s*(:|\\s+saying\\s+|\\s+that\\s+|,\\s*|\\s+with\\s+message\\s+|\\s+message\\s+)\\s*")
            val delimMatch = delimRegex.find(remainder)
            if (delimMatch != null) {
                val target = remainder.substring(0, delimMatch.range.first).trim()
                val msg = remainder.substring(delimMatch.range.last + 1).trim()
                return Pair(target, msg)
            } else {
                val spaceIdx = remainder.indexOf(' ')
                return if (spaceIdx > 0) {
                    val firstWord = remainder.substring(0, spaceIdx).trim()
                    val msg = remainder.substring(spaceIdx + 1).trim()
                    Pair(firstWord, msg)
                } else {
                    Pair(remainder, "")
                }
            }
        }

        // Pattern: "whatsapp [target] [message]" or "whatsapp [target]"
        if (lower.startsWith("whatsapp ")) {
            val remainder = clean.substring("whatsapp ".length).trim()
            if (remainder.lowercase(Locale.ROOT).startsWith("message ")) {
                val msg = remainder.substring("message ".length).trim()
                return Pair("", msg)
            }
            val delimRegex = Regex("(?i)\\s*(:|\\s+saying\\s+|\\s+that\\s+|,\\s*|\\s+with\\s+message\\s+|\\s+message\\s+)\\s*")
            val delimMatch = delimRegex.find(remainder)
            if (delimMatch != null) {
                val target = remainder.substring(0, delimMatch.range.first).trim()
                val msg = remainder.substring(delimMatch.range.last + 1).trim()
                return Pair(target, msg)
            } else {
                val spaceIdx = remainder.indexOf(' ')
                return if (spaceIdx > 0) {
                    val firstWord = remainder.substring(0, spaceIdx).trim()
                    val rest = remainder.substring(spaceIdx + 1).trim()
                    Pair(firstWord, rest)
                } else {
                    Pair(remainder, "")
                }
            }
        }

        // Generic fallback:
        val genericMsg = clean.replace(Regex("^(?i)(please\\s+)?(can\\s+you\\s+)?(send\\s+(a\\s+)?whatsapp\\s+(message)?|whatsapp\\s+message|whatsapp)\\s*:?\\s*"), "").trim()
        return Pair("", genericMsg)
    }
}
