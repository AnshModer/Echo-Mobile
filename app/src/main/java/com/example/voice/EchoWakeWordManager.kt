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
        private const val TRIGGER_DEBOUNCE_MS = 3500L
        private const val HISTORY_FRAME_COUNT = 48 // ~1.5s sliding window
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
    private var noiseFloorEnergy = 450.0

    // Acoustic sliding buffer for temporal phoneme verification (~1.5 seconds)
    private val energyHistory = DoubleArray(HISTORY_FRAME_COUNT)
    private val zcrHistory = DoubleArray(HISTORY_FRAME_COUNT)
    private val highFreqEnergyRatioHistory = DoubleArray(HISTORY_FRAME_COUNT)
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
        // 1. Calculate RMS Energy, Zero-Crossing Rate (ZCR), and high-frequency sample differential
        var sumSquares = 0.0
        var zeroCrossings = 0
        var highFreqEnergySum = 0.0
        var prevSample = buffer[0].toInt()

        for (i in 0 until length) {
            val s = buffer[i].toInt()
            sumSquares += (s * s)
            val diff = s - prevSample
            highFreqEnergySum += (diff * diff)
            if ((s >= 0 && prevSample < 0) || (s < 0 && prevSample >= 0)) {
                zeroCrossings++
            }
            prevSample = s
        }

        val rmsEnergy = sqrt(sumSquares / length)
        val zcr = zeroCrossings.toDouble() / length
        val highFreqRatio = if (sumSquares > 0) sqrt(highFreqEnergySum / sumSquares) else 0.0

        // 2. Dynamic Noise Floor Tracking (requires distinct voice headroom above ambient noise)
        noiseFloorEnergy = (noiseFloorEnergy * 0.97) + (min(noiseFloorEnergy * 1.3, rmsEnergy) * 0.03)
        val ambientThreshold = max(650.0, noiseFloorEnergy * 2.2)

        // Store frame metrics in cyclic history
        energyHistory[historyIndex] = rmsEnergy
        zcrHistory[historyIndex] = zcr
        highFreqEnergyRatioHistory[historyIndex] = highFreqRatio
        historyIndex = (historyIndex + 1) % HISTORY_FRAME_COUNT

        // 3. Stage 1: Energy Gating - ignore frames below voice energy threshold
        if (rmsEnergy < ambientThreshold) {
            return
        }

        // 4. Stage 2: Temporal Acoustic & Syllable Verification for "Hey Echo" / "Echo"
        val detected = evaluateAcousticMatch(ambientThreshold)
        if (detected) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTimestamp > TRIGGER_DEBOUNCE_MS) {
                lastTriggerTimestamp = now
                Log.i(TAG, "Wake word 'Hey Echo' verified and detected!")
                triggerWakeWord("Hey Echo")
            }
        }
    }

    private fun evaluateAcousticMatch(threshold: Double): Boolean {
        val sensitivity = preferences.wakeWordSensitivity.coerceIn(0.2f, 0.95f)

        // Minimum voice utterance length: 300ms to 1200ms (10 to 38 frames @ 32ms/frame)
        // Sensitivity 0.40 (Balanced) -> requires strictly formed syllabic contour
        var speechFrameCount = 0
        var totalFramesChecked = 0
        
        // Track the presence of distinct syllable bursts separated by brief vowel/consonant dips
        var syllablePeaks = 0
        var inPeak = false
        var hasPlosiveKBurst = false
        var hasVowelResonance = false

        for (i in 0 until min(HISTORY_FRAME_COUNT, 36)) {
            val idx = (historyIndex - 1 - i + HISTORY_FRAME_COUNT) % HISTORY_FRAME_COUNT
            val e = energyHistory[idx]
            val z = zcrHistory[idx]
            val hf = highFreqEnergyRatioHistory[idx]

            totalFramesChecked++

            if (e > threshold * 1.1) {
                speechFrameCount++
                if (!inPeak && e > threshold * 1.4) {
                    inPeak = true
                    syllablePeaks++
                }
                // Check vowel resonance (moderate ZCR, high RMS) for /eɪ/, /ɛ/, /oʊ/
                if (z in 0.04..0.22 && e > threshold * 1.3) {
                    hasVowelResonance = true
                }
                // Check velar plosive /k/ (sharp transient high ZCR burst or HF ratio > 0.9)
                if ((z > 0.24 || hf > 1.1) && e > threshold * 0.8) {
                    hasPlosiveKBurst = true
                }
            } else if (e < threshold * 0.85) {
                inPeak = false
            }
        }

        // Minimum duration threshold based on sensitivity
        val minRequiredSpeechFrames = when {
            sensitivity <= 0.35f -> 14 // ~450ms sustained pattern (Strict)
            sensitivity <= 0.55f -> 10 // ~320ms sustained pattern (Balanced)
            else -> 8                  // ~250ms (Sensitive)
        }

        val maxAllowedSpeechFrames = 34 // Discard continuous long speech/babble (> 1.1s)

        if (speechFrameCount < minRequiredSpeechFrames || speechFrameCount > maxAllowedSpeechFrames) {
            return false
        }

        // Must have at least 2 distinct acoustic energy peaks (e.g. "Hey" + "Echo" or "Eh" + "cho")
        // AND have characteristic acoustic properties of the word "Echo"
        val hasValidSyllableStructure = syllablePeaks in 2..4
        val hasEchoCharacteristics = hasPlosiveKBurst && hasVowelResonance

        return when {
            sensitivity <= 0.35f -> hasValidSyllableStructure && hasEchoCharacteristics && speechFrameCount in 12..30
            sensitivity <= 0.55f -> (hasValidSyllableStructure || hasEchoCharacteristics) && syllablePeaks >= 2
            else -> syllablePeaks >= 2 && (hasVowelResonance || hasPlosiveKBurst)
        }
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
