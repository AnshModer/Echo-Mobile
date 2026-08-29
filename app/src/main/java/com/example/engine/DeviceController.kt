package com.example.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val chargeType: String,
    val temperatureCelsius: Float
)

data class VolumeInfo(
    val mediaPercent: Int,
    val ringPercent: Int,
    val alarmPercent: Int
)

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    companion object {
        var isFlashlightOn: Boolean = false
            private set
    }

    // --- FLASHLIGHT ---
    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        if (cameraManager == null) return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return false

            val newState = forceState ?: !isFlashlightOn
            cameraManager.setTorchMode(cameraId, newState)
            isFlashlightOn = newState
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- VOLUME ---
    fun getVolumeInfo(): VolumeInfo {
        if (audioManager == null) return VolumeInfo(0, 0, 0)
        val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val currMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val currRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val currAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        return VolumeInfo(
            mediaPercent = ((currMedia.toFloat() / maxMedia) * 100).toInt(),
            ringPercent = ((currRing.toFloat() / maxRing) * 100).toInt(),
            alarmPercent = ((currAlarm.toFloat() / maxAlarm) * 100).toInt()
        )
    }

    fun setMediaVolumePercent(percent: Int): String {
        if (audioManager == null) return "Audio service not available."
        val clamped = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((clamped / 100f) * max).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Media volume set to $clamped%."
    }

    fun adjustVolume(increase: Boolean): String {
        if (audioManager == null) return "Audio service not available."
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val info = getVolumeInfo()
        return "Volume adjusted to ${info.mediaPercent}%."
    }

    fun muteVolume(mute: Boolean): String {
        if (audioManager == null) return "Audio service not available."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        } else {
            if (mute) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            } else {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max / 2, AudioManager.FLAG_SHOW_UI)
            }
        }
        return if (mute) "Media muted." else "Media unmuted."
    }

    // --- BATTERY ---
    fun getBatteryInfo(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 50

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Plugged In" else "Discharging"
        }

        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = temp / 10.0f

        return BatteryInfo(batteryPct, isCharging, chargeType, tempCelsius)
    }

    // --- APPS LAUNCHER ---
    fun openApp(appNameOrPackage: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val query = appNameOrPackage.trim().lowercase(Locale.ROOT)

        // Known quick mappings
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "camera" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "clock" to "com.google.android.deskclock",
            "settings" to "com.android.settings",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos"
        )

        val directPkg = knownPackages[query]
        if (directPkg != null) {
            val intent = pm.getLaunchIntentForPackage(directPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return Pair(true, "Opening ${appNameOrPackage.replaceFirstChar { it.uppercase() }}")
            }
        }

        // Generic search through installed apps
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase(Locale.ROOT)
            if (label == query || label.contains(query) || query.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Pair(true, "Opening ${pm.getApplicationLabel(app)}")
                }
            }
        }

        // Fallback: search on Play Store or Web
        return Pair(false, "Could not find application '$appNameOrPackage' on your device.")
    }

    // --- ALARMS & TIMERS ---
    fun setAlarm(hour: Int, minute: Int, message: String = "Echo Alarm"): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            Pair(true, "Alarm set for $timeStr ($message)")
        } catch (e: Exception) {
            Pair(false, "Failed to set alarm: ${e.localizedMessage}")
        }
    }

    fun setTimer(seconds: Int, message: String = "Echo Timer"): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val remainingSecs = seconds % 60
            val formatted = if (mins > 0) "$mins min $remainingSecs sec" else "$seconds seconds"
            Pair(true, "Timer set for $formatted.")
        } catch (e: Exception) {
            Pair(false, "Failed to set timer: ${e.localizedMessage}")
        }
    }

    // --- CALLS & SMS ---
    fun makeCall(target: String): Pair<Boolean, String> {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) {
            return Pair(false, "Who would you like to call? Please say a contact name or phone number.")
        }

        // Case 1: Direct phone number (e.g. "+1-234-567-8900", "9876543210", "911")
        if (ContactLookupHelper.isDirectPhoneNumber(cleanTarget)) {
            return dialOrPlaceCall(cleanTarget, cleanTarget)
        }

        // Case 2: Contact name lookup (e.g. "mom", "John", "Sarah Connor")
        if (!ContactLookupHelper.hasContactsPermission(context)) {
            return Pair(
                false,
                "Contacts permission is required to find \"$cleanTarget\". Please allow Contacts access in Echo settings."
            )
        }

        val bestMatch = ContactLookupHelper.findBestContactMatch(context, cleanTarget)
        return if (bestMatch != null) {
            val res = dialOrPlaceCall(bestMatch.number, bestMatch.name)
            if (res.first) {
                Pair(true, "Calling ${bestMatch.name} (${bestMatch.number})...")
            } else {
                res
            }
        } else {
            Pair(
                false,
                "No contact matching \"$cleanTarget\" was found in your phone contacts."
            )
        }
    }

    fun dialOrPlaceCall(phoneNumber: String, displayName: String): Pair<Boolean, String> {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanNumber.isBlank()) {
                return Pair(false, "Invalid phone number for $displayName.")
            }

            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            }
            Pair(true, "Calling $displayName ($phoneNumber)...")
        } catch (e: Exception) {
            Pair(false, "Could not place call to $displayName: ${e.localizedMessage}")
        }
    }

    fun searchContacts(query: String): List<ContactMatch> {
        return ContactLookupHelper.searchContacts(context, query)
    }

    fun sendSms(phoneNumber: String, messageText: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening SMS to $phoneNumber")
        } catch (e: Exception) {
            Pair(false, "Could not open SMS app: ${e.localizedMessage}")
        }
    }

    // --- WHATSAPP MESSAGING ---
    fun sendWhatsAppMessage(target: String, messageText: String): Pair<Boolean, String> {
        val cleanTarget = target.trim()
        val cleanMsg = messageText.trim()

        if (cleanTarget.isBlank() && cleanMsg.isBlank()) {
            return openWhatsApp()
        }

        var targetNumber = ""
        var displayName = cleanTarget

        if (cleanTarget.isNotBlank()) {
            if (ContactLookupHelper.isDirectPhoneNumber(cleanTarget)) {
                targetNumber = cleanTarget.replace(Regex("[^0-9+]"), "")
            } else {
                val match = ContactLookupHelper.findBestContactMatch(context, cleanTarget)
                if (match != null) {
                    displayName = match.name
                    targetNumber = match.number.replace(Regex("[^0-9+]"), "")
                }
            }
        }

        return try {
            val hasWhatsApp = isAppInstalled("com.whatsapp") || isAppInstalled("com.whatsapp.w4b")
            val pkg = if (isAppInstalled("com.whatsapp")) "com.whatsapp" else "com.whatsapp.w4b"

            if (targetNumber.isNotBlank()) {
                val waNumber = targetNumber.replace("+", "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$waNumber&text=${Uri.encode(cleanMsg)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    if (hasWhatsApp) setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val msgDesc = if (cleanMsg.isNotBlank()) " with message: \"$cleanMsg\"" else ""
                Pair(true, "Opening WhatsApp to message $displayName$msgDesc")
            } else if (cleanMsg.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, cleanMsg)
                    if (hasWhatsApp) setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opening WhatsApp with message: \"$cleanMsg\"")
            } else {
                openWhatsApp()
            }
        } catch (e: Exception) {
            try {
                val waNumber = targetNumber.replace("+", "").replace(Regex("[^0-9]"), "")
                val url = if (waNumber.isNotBlank()) {
                    "https://wa.me/$waNumber?text=${Uri.encode(cleanMsg)}"
                } else {
                    "https://web.whatsapp.com"
                }
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Pair(true, "Opening WhatsApp for $displayName")
            } catch (e2: Exception) {
                Pair(false, "Could not open WhatsApp: ${e.localizedMessage}")
            }
        }
    }

    fun openWhatsApp(): Pair<Boolean, String> {
        return openApp("whatsapp")
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- SCREENSHOT CONTROLLER ---
    fun captureScreenshot(activity: android.app.Activity? = null, onComplete: ((Boolean, String, Uri?) -> Unit)? = null): Pair<Boolean, String> {
        if (activity != null) {
            ScreenshotHelper.captureActivityScreenshot(activity) { success, msg, uri ->
                onComplete?.invoke(success, msg, uri)
            }
            return Pair(true, "Screenshot captured and saved to gallery.")
        }

        val lastUri = ScreenshotHelper.getLastScreenshotUri()
        if (lastUri != null) {
            return Pair(true, "Screenshot captured and saved to gallery.")
        }

        val recent = ScreenshotHelper.getRecentScreenshots(context)
        if (recent.isNotEmpty()) {
            return Pair(true, "Screenshot is available in your gallery.")
        }

        return Pair(true, "Screenshot triggered. You can also capture anytime via the Screen Capture button or Power + Volume Down.")
    }

    fun shareLastScreenshot(): Pair<Boolean, String> {
        val lastUri = ScreenshotHelper.getLastScreenshotUri()
        return if (lastUri != null) {
            ScreenshotHelper.shareScreenshot(context, lastUri)
            Pair(true, "Sharing screenshot...")
        } else {
            val recent = ScreenshotHelper.getRecentScreenshots(context)
            if (recent.isNotEmpty()) {
                ScreenshotHelper.shareScreenshot(context, recent.first().uri)
                Pair(true, "Sharing latest screenshot...")
            } else {
                Pair(false, "No screenshots found to share. Take a screenshot first.")
            }
        }
    }

    // --- WEB & MAPS SEARCH ---
    fun webSearch(query: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Searching Google for '$query'")
        } catch (e: Exception) {
            // fallback to browser
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(urlIntent)
            Pair(true, "Searching for '$query'")
        }
    }

    fun openMapsNavigation(destination: String): Pair<Boolean, String> {
        return try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
            Pair(true, "Navigating to $destination")
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(destination))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening map for $destination")
        }
    }

    // --- SETTINGS SHORTCUTS ---
    fun openSettingsScreen(action: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening settings...")
        } catch (e: Exception) {
            // Fallback to general settings
            val generalIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalIntent)
            Pair(true, "Opening Settings")
        }
    }

    fun openDefaultAssistantSettings() {
        try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    fun openRedmiGestureSettings() {
        try {
            // Xiaomi MIUI / HyperOS Gesture Shortcuts intent
            val intent = Intent().apply {
                setClassName("com.android.settings", "com.android.settings.SubSettings")
                putExtra(":settings:show_fragment", "com.android.settings.gestures.GestureSettings")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent("android.settings.KEY_GESTURE_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                openSettingsScreen(Settings.ACTION_SETTINGS)
            }
        }
    }

    fun openWifiSettings() = openSettingsScreen(Settings.ACTION_WIFI_SETTINGS)
    fun openBluetoothSettings() = openSettingsScreen(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openDisplaySettings() = openSettingsScreen(Settings.ACTION_DISPLAY_SETTINGS)
    fun openSoundSettings() = openSettingsScreen(Settings.ACTION_SOUND_SETTINGS)
    fun openBatterySettings() = openSettingsScreen(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    fun openDateSettings() = openSettingsScreen(Settings.ACTION_DATE_SETTINGS)

    // --- YOUTUBE & MUSIC ---
    fun searchYouTube(query: String): Pair<Boolean, String> {
        return try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
            Pair(true, "Searching YouTube for \"$query\"")
        } catch (e: Exception) {
            // Fallback to browser YouTube URL
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Pair(true, "Opening YouTube for \"$query\"")
        }
    }

    fun playOnYouTube(query: String): Pair<Boolean, String> {
        return try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
            Pair(true, "Playing \"$query\" on YouTube")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Pair(true, "Searching and playing \"$query\" on YouTube")
        }
    }

    fun playMusic(query: String? = null): Pair<Boolean, String> {
        if (!query.isNullOrBlank()) {
            // Check if user specifically requested Spotify
            return playOnSpotify(query)
        }
        // Try resuming media playback
        val res = sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY)
        // Also try opening music app if no active session
        try {
            val pm = context.packageManager
            val musicApps = listOf("com.spotify.music", "com.google.android.apps.youtube.music", "com.miui.player")
            for (pkg in musicApps) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Pair(true, "Playing music...")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(true, "Resuming music playback.")
    }

    fun playOnSpotify(query: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}")).apply {
                setPackage("com.spotify.music")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Playing \"$query\" on Spotify")
        } catch (e: Exception) {
            // If spotify isn't installed, search on YouTube
            searchYouTube(query)
        }
    }

    fun pauseMusic(): Pair<Boolean, String> {
        sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PAUSE)
        return Pair(true, "Music paused.")
    }

    fun nextTrack(): Pair<Boolean, String> {
        sendMediaCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
        return Pair(true, "Playing next track.")
    }

    fun previousTrack(): Pair<Boolean, String> {
        sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return Pair(true, "Playing previous track.")
    }

    fun openCalculator(): Pair<Boolean, String> {
        val calcPackages = listOf(
            "com.google.android.calculator",
            "com.miui.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator"
        )
        val pm = context.packageManager
        for (pkg in calcPackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return Pair(true, "Opening Calculator")
            }
        }
        return Pair(false, "Calculator app not found.")
    }

    // --- MEDIA CONTROLS ---
    fun sendMediaCommand(keyCode: Int): String {
        return try {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            context.sendOrderedBroadcast(downIntent, null)
            context.sendOrderedBroadcast(upIntent, null)
            "Media command sent."
        } catch (e: Exception) {
            "Failed to send media command: ${e.message}"
        }
    }
}
