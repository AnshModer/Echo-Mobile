package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AssistantPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Foreground Service for Continuous 'Hey Echo' Wake Word Detection.
 * Operates in the background, listening for wake phrases ("Hey Echo", "OK Echo", "Echo")
 * and summoning the Echo Voice Assistant floating orb or executing direct voice commands immediately.
 */
class EchoWakeWordService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_WAKE_WORD"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_WAKE_WORD"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE_WAKE_WORD"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME_WAKE_WORD"

        const val NOTIFICATION_CHANNEL_ID = "echo_wake_word_channel"
        const val NOTIFICATION_ID = 2003

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, EchoWakeWordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EchoWakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var preferences: AssistantPreferences
    private var speechRecognizer: SpeechRecognizer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isListeningLoopActive = false
    private var isPausedForInteraction = false
    private var monitorJob: Job? = null

    private val wakeWordTriggers = listOf(
        "hey echo",
        "okay echo",
        "ok echo",
        "hi echo",
        "hey eco",
        "ok eco",
        "a echo",
        "echo"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferences = AssistantPreferences(this)
        isRunning = true
        _isRunningFlow.value = true

        createNotificationChannel()
        startForegroundNotification()

        startWakeWordMonitoring()
        startInteractionObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                preferences.isWakeWordEnabled = false
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseMonitoring()
            }
            ACTION_RESUME -> {
                resumeMonitoring()
            }
            ACTION_START -> {
                if (!isListeningLoopActive && !isPausedForInteraction) {
                    startListeningCycle()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val serviceType = if (hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (ignored: Exception) {}
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EchoWakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            202,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Echo Wake Word Active")
            .setContentText("Listening for \"Hey Echo\" • Hands-free assistant ready")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_echo_mic, "Stop Wake Word", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Echo Wake Word Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening for hands-free 'Hey Echo' wake word"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startWakeWordMonitoring() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, stop service gracefully
            stopSelf()
            return
        }
        startListeningCycle()
    }

    private fun startListeningCycle() {
        if (isPausedForInteraction || !preferences.isWakeWordEnabled) return

        mainHandler.post {
            try {
                cleanUpRecognizer()

                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListeningLoopActive = true
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListeningLoopActive = false
                        }

                        override fun onError(error: Int) {
                            isListeningLoopActive = false
                            // Restart monitoring loop after a slight debounce unless paused
                            scheduleRestart(delayMillis = 800L)
                        }

                        override fun onResults(results: Bundle?) {
                            isListeningLoopActive = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                processSpeechMatches(matches)
                            } else {
                                scheduleRestart(delayMillis = 500L)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!partialMatches.isNullOrEmpty()) {
                                for (phrase in partialMatches) {
                                    if (detectWakeWordInPhrase(phrase) != null) {
                                        cleanUpRecognizer()
                                        processSpeechMatches(partialMatches)
                                        return
                                    }
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                        putExtra("android.speech.extra.DICTATION_MODE", true)
                    }

                    startListening(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isListeningLoopActive = false
                scheduleRestart(delayMillis = 1500L)
            }
        }
    }

    private fun processSpeechMatches(matches: List<String>) {
        for (rawMatch in matches) {
            val detection = detectWakeWordInPhrase(rawMatch)
            if (detection != null) {
                onWakeWordDetected(detection.commandQuery)
                return
            }
        }
        // No wake word found in this utterance, keep listening
        scheduleRestart(delayMillis = 400L)
    }

    private data class WakeWordMatch(val matchedKeyword: String, val commandQuery: String)

    private fun detectWakeWordInPhrase(phrase: String): WakeWordMatch? {
        val cleanPhrase = phrase.lowercase(Locale.getDefault()).trim()

        for (trigger in wakeWordTriggers) {
            if (cleanPhrase == trigger) {
                return WakeWordMatch(trigger, "")
            } else if (cleanPhrase.startsWith("$trigger ") || cleanPhrase.startsWith("$trigger,")) {
                val command = cleanPhrase.removePrefix(trigger)
                    .trim(',', ' ', '.', '!', '?')
                    .trim()
                return WakeWordMatch(trigger, command)
            } else if (cleanPhrase.contains(trigger)) {
                val index = cleanPhrase.indexOf(trigger)
                val after = cleanPhrase.substring(index + trigger.length)
                    .trim(',', ' ', '.', '!', '?')
                    .trim()
                return WakeWordMatch(trigger, after)
            }
        }
        return null
    }

    private fun onWakeWordDetected(commandQuery: String) {
        performWakeHaptic()

        // Temporarily pause wake word listener while the voice assistant is interacting
        pauseMonitoring()

        if (commandQuery.isNotBlank() && commandQuery.length > 2) {
            // User spoke both wake word and command: "Hey Echo what's the weather"
            EchoFloatingBubbleService.startVoiceInteractionWithQuery(this, commandQuery)
        } else {
            // User only spoke wake word: "Hey Echo" -> open orb in listening mode
            EchoFloatingBubbleService.startVoiceInteraction(this)
        }

        // Wait until interaction finishes or resume after reasonable period
        serviceScope.launch {
            // Give 8-15 seconds for user voice query & TTS response, or until Echo is no longer active
            var waited = 0
            while (waited < 25 && (EchoFloatingBubbleService.isInteractionActive || waited < 5)) {
                delay(1000L)
                waited++
            }
            resumeMonitoring()
        }
    }

    private fun performWakeHaptic() {
        if (!preferences.isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 90), intArrayOf(0, 180, 0, 255), -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        if (!isRunning || isPausedForInteraction || !preferences.isWakeWordEnabled) return
        mainHandler.postDelayed({
            if (isRunning && !isPausedForInteraction && preferences.isWakeWordEnabled) {
                startListeningCycle()
            }
        }, delayMillis)
    }

    private fun startInteractionObserver() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                delay(2000L)
                if (!preferences.isWakeWordEnabled) {
                    stop(this@EchoWakeWordService)
                    break
                }
                if (!isPausedForInteraction && !isListeningLoopActive) {
                    startListeningCycle()
                }
            }
        }
    }

    private fun pauseMonitoring() {
        isPausedForInteraction = true
        cleanUpRecognizer()
    }

    private fun resumeMonitoring() {
        isPausedForInteraction = false
        if (preferences.isWakeWordEnabled && isRunning) {
            startListeningCycle()
        }
    }

    private fun cleanUpRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            speechRecognizer = null
            isListeningLoopActive = false
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        isPausedForInteraction = true
        cleanUpRecognizer()
    }

    override fun onDestroy() {
        isRunning = false
        _isRunningFlow.value = false
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }
}
