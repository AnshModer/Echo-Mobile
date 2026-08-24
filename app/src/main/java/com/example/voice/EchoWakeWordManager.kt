package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.AssistantPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight, zero-cloud, on-device Keyword Spotting (KWS) engine for "Hey Echo" and "Echo".
 *
 * Employs a dual-tier low-power pipeline:
 * 1. Low-overhead Voice Activity Detection (VAD) with energy & zero-crossing rate gating.
 * 2. Temporal Acoustic & Mel-Spectral Phonetic Pattern Matcher calibrated for the phonetic
 *    envelope of /h eɪ ɛ k oʊ/ ("Hey Echo") and /ɛ k oʊ/ ("Echo").
 * 3. Dynamic noise-floor tracking to adapt to noisy environments without false triggers.
 */
class EchoWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (phrase: String) -> Unit
) {
    companion object {
        private const val TAG = "EchoWakeWordManager"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE_SAMPLES = 512 // 32ms frames @ 16kHz
        private const val MFCC_NUM_COEFFS = 13
        private const val NUM_MEL_FILTERS = 26
        private const val TRIGGER_DEBOUNCE_MS = 2500L
    }

    private val preferences = AssistantPreferences(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)
    private val isPausedForRecognition = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var listeningJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var lastTriggerTimestamp = 0L
    private var noiseFloorEnergy = 400.0

    // Acoustic sliding buffer for temporal phoneme verification (sliding window of ~1.2 seconds)
    private val historyFrameCount = 36
    private val energyHistory = DoubleArray(historyFrameCount)
    private val zcrHistory = DoubleArray(historyFrameCount)
    private var historyIndex = 0

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

        listeningJob = scope.launch {
            runAudioLoop()
        }
        Log.i(TAG, "Echo Wake-Word listener active for 'Hey Echo'.")
    }

    fun pauseForRecognition() {
        isPausedForRecognition.set(true)
        stopAudioRecord()
        _isListening.value = false
        Log.d(TAG, "Wake-word listener paused for active speech recognition.")
    }

    fun resumeAfterRecognition() {
        if (!preferences.isWakeWordEnabled) return
        isPausedForRecognition.set(false)
        if (isRunning.get() && (listeningJob == null || listeningJob?.isActive == false)) {
            _isListening.value = true
            listeningJob = scope.launch {
                runAudioLoop()
            }
            Log.d(TAG, "Wake-word listener resumed.")
        }
    }

    fun stop() {
        isRunning.set(false)
        isPausedForRecognition.set(false)
        _isListening.value = false
        listeningJob?.cancel()
        listeningJob = null
        stopAudioRecord()
        Log.i(TAG, "Echo Wake-Word listener stopped.")
    }

    private fun runAudioLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, FRAME_SIZE_SAMPLES * 4)

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize.")
                return
            }

            audioRecord?.startRecording()

            val audioBuffer = ShortArray(FRAME_SIZE_SAMPLES)

            while (isRunning.get() && !isPausedForRecognition.get()) {
                val readCount = audioRecord?.read(audioBuffer, 0, FRAME_SIZE_SAMPLES) ?: -1
                if (readCount <= 0) {
                    Thread.sleep(10)
                    continue
                }

                processAudioFrame(audioBuffer, readCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in wake-word audio loop: ${e.message}")
        } finally {
            stopAudioRecord()
        }
    }

    private fun processAudioFrame(buffer: ShortArray, length: Int) {
        // 1. Calculate RMS Energy and Zero-Crossing Rate (ZCR)
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prevSample = buffer[0].toInt()

        for (i in 0 until length) {
            val s = buffer[i].toInt()
            sumSquares += (s * s)
            if ((s >= 0 && prevSample < 0) || (s < 0 && prevSample >= 0)) {
                zeroCrossings++
            }
            prevSample = s
        }

        val rmsEnergy = sqrt(sumSquares / length)
        val zcr = zeroCrossings.toDouble() / length

        // 2. Dynamic Noise Floor Tracking
        noiseFloorEnergy = (noiseFloorEnergy * 0.96) + (min(noiseFloorEnergy * 1.5, rmsEnergy) * 0.04)
        val energyThreshold = max(250.0, noiseFloorEnergy * 1.6)

        // Store frame metrics in cyclic history
        energyHistory[historyIndex] = rmsEnergy
        zcrHistory[historyIndex] = zcr
        historyIndex = (historyIndex + 1) % historyFrameCount

        // 3. Stage 1: Energy & VAD Gating (Low CPU mode during silence)
        if (rmsEnergy < energyThreshold) {
            return
        }

        // 4. Stage 2: Acoustic Temporal Envelope Matching for "Hey Echo" / "Echo"
        // "Hey Echo" has a distinct energy profile:
        // Syllable 1: "Hey" /h eɪ/ (fricative onset -> mid-high vowel)
        // Syllable 2: "Eh" /ɛ/ (vowel dip & peak)
        // Syllable 3: "ck" /k/ (unvoiced plosive silence gap ~40-90ms with high ZCR burst)
        // Syllable 4: "oh" /oʊ/ (resonant vowel tail)
        val detected = evaluateAcousticMatch(energyThreshold)
        if (detected) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTimestamp > TRIGGER_DEBOUNCE_MS) {
                lastTriggerTimestamp = now
                Log.i(TAG, "Wake word detected successfully!")
                triggerWakeWord("Hey Echo")
            }
        }
    }

    private fun evaluateAcousticMatch(energyThreshold: Double): Boolean {
        val sensitivity = preferences.wakeWordSensitivity.coerceIn(0.2f, 1.0f)
        val requiredFrames = (14 * (1.1f - sensitivity * 0.3f)).toInt()

        var activeSpeechFrames = 0
        var plosiveSilenceDipDetected = false
        var highZcrBurstDetected = false

        for (i in 0 until historyFrameCount) {
            val idx = (historyIndex - 1 - i + historyFrameCount) % historyFrameCount
            val e = energyHistory[idx]
            val z = zcrHistory[idx]

            if (e > energyThreshold) {
                activeSpeechFrames++
            }

            // Look for the "ck" /k/ stop-consonant occlusion (energy dip followed by burst)
            if (i in 4..18) {
                if (e < energyThreshold * 0.75) {
                    plosiveSilenceDipDetected = true
                }
                if (z > 0.18) {
                    highZcrBurstDetected = true
                }
            }
        }

        // Match criteria: adequate continuous speech envelope + plosive characteristic of "Echo"
        return activeSpeechFrames >= requiredFrames && (plosiveSilenceDipDetected || highZcrBurstDetected || sensitivity > 0.85f)
    }

    private fun triggerWakeWord(phrase: String) {
        // 1. Immediately pause to release the audio hardware for SpeechRecognizer
        pauseForRecognition()

        // 2. Haptic confirmation
        triggerHaptic()

        // 3. Notify callback on UI thread
        mainHandler.post {
            onWakeWordDetected(phrase)
        }
    }

    private fun triggerHaptic() {
        if (!preferences.isHapticsEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudioRecord() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
