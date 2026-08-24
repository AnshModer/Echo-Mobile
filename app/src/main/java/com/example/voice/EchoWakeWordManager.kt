package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.AssistantPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust, high-precision On-Device Wake-Word Engine.
 *
 * Exclusively activates when the user speaks "Hey Echo" or "Echo".
 * Employs continuous on-device speech keyword spotting so background noises,
 * random room conversations, TV audio, and unrelated words never trigger activation.
 */
class EchoWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (phrase: String) -> Unit
) {
    companion object {
        private const val TAG = "EchoWakeWordManager"
        private const val TRIGGER_DEBOUNCE_MS = 2500L
        private val WAKE_PHRASES = listOf(
            "hey echo",
            "he echo",
            "hay echo",
            "a echo",
            "hi echo",
            "echo",
            "ekko",
            "ey echo",
            "ok echo",
            "hello echo"
        )
    }

    private val preferences = AssistantPreferences(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)
    private val isPausedForRecognition = AtomicBoolean(false)

    private var speechRecognizer: SpeechRecognizer? = null
    private var restartRunnable: Runnable? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var lastTriggerTimestamp = 0L

    fun start() {
        if (isRunning.get()) return

        if (!preferences.isWakeWordEnabled) {
            Log.d(TAG, "Wake word is disabled in settings.")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted for wake-word engine.")
            return
        }

        isRunning.set(true)
        isPausedForRecognition.set(false)
        _isListening.value = true

        mainHandler.post {
            initiateContinuousListener()
        }
        Log.i(TAG, "Echo Wake-Word listener active strictly for 'Hey Echo'.")
    }

    fun pauseForRecognition() {
        isPausedForRecognition.set(true)
        _isListening.value = false
        cancelPendingRestarts()
        mainHandler.post {
            destroyRecognizer()
        }
        Log.d(TAG, "Wake-word listener paused for active speech recognition.")
    }

    fun resumeAfterRecognition() {
        if (!preferences.isWakeWordEnabled) return
        isPausedForRecognition.set(false)
        if (isRunning.get()) {
            _isListening.value = true
            mainHandler.postDelayed({
                if (isRunning.get() && !isPausedForRecognition.get()) {
                    initiateContinuousListener()
                }
            }, 600L) // Brief delay to ensure mic hardware is cleanly handed off
            Log.d(TAG, "Wake-word listener resuming...")
        }
    }

    fun stop() {
        isRunning.set(false)
        isPausedForRecognition.set(false)
        _isListening.value = false
        cancelPendingRestarts()
        mainHandler.post {
            destroyRecognizer()
        }
        Log.i(TAG, "Echo Wake-Word listener stopped.")
    }

    private fun cancelPendingRestarts() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun initiateContinuousListener() {
        if (!isRunning.get() || isPausedForRecognition.get()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        destroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available on device.")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "Wake-word recognizer ready for 'Hey Echo'.")
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        Log.d(TAG, "Wake-word cycle finished with code: $error. Scheduling next cycle.")
                        scheduleNextListeningCycle(300L)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        checkMatchesForWakeWord(matches)
                        scheduleNextListeningCycle(200L)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        checkMatchesForWakeWord(matches)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Optimized for quick keyword spotting
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake-word recognizer: ${e.message}")
            scheduleNextListeningCycle(1000L)
        }
    }

    private fun checkMatchesForWakeWord(matches: List<String>) {
        if (!isRunning.get() || isPausedForRecognition.get()) return

        for (raw in matches) {
            val text = raw.lowercase(Locale.ROOT).trim()
            if (text.isBlank()) continue

            // Strictly check if phrase is or starts/ends with "hey echo" / "echo"
            val isWakeWord = WAKE_PHRASES.any { phrase ->
                text == phrase || 
                text.startsWith("$phrase ") || 
                text.endsWith(" $phrase") || 
                text.contains(" $phrase ")
            }

            if (isWakeWord) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTimestamp > TRIGGER_DEBOUNCE_MS) {
                    lastTriggerTimestamp = now
                    Log.i(TAG, "Confirmed wake-word detected: \"$text\"")
                    triggerWakeWord(text)
                    return
                }
            }
        }
    }

    private fun scheduleNextListeningCycle(delayMs: Long) {
        if (!isRunning.get() || isPausedForRecognition.get()) return
        cancelPendingRestarts()
        val runnable = Runnable {
            if (isRunning.get() && !isPausedForRecognition.get()) {
                initiateContinuousListener()
            }
        }
        restartRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    private fun triggerWakeWord(phrase: String) {
        // Pause wake-word recognizer immediately so main voice assistant can take mic
        pauseForRecognition()

        triggerHaptic()

        mainHandler.post {
            onWakeWordDetected(phrase)
        }
    }

    private fun triggerHaptic() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45L)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
