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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import com.example.data.local.AssistantLanguage
import com.example.data.local.AssistantPreferences
import com.example.data.local.VoicePersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

class EchoVoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) {
    private val preferences = AssistantPreferences(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyHumanLikeVoice()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _assistantState.value = AssistantState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _assistantState.value = AssistantState.IDLE
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _assistantState.value = AssistantState.IDLE
                    }
                })
                isTtsInitialized = true
            }
        }
    }

    /**
     * Finds and applies the most natural, human-sounding voice profile
     * matching the user's selected persona and language.
     */
    fun applyHumanLikeVoice() {
        val ttsInstance = tts ?: return
        try {
            val persona = preferences.voicePersona
            val availableVoices = try {
                ttsInstance.voices ?: emptySet()
            } catch (e: Exception) {
                emptySet()
            }

            // 1. Target locale based on persona & language
            val targetLocale = when (persona) {
                VoicePersona.NATURAL_INDIAN_FEMALE, VoicePersona.NATURAL_INDIAN_MALE -> Locale("en", "IN")
                VoicePersona.NATURAL_US_FEMALE, VoicePersona.NATURAL_US_MALE -> Locale.US
                VoicePersona.SYSTEM_DEFAULT -> {
                    when (preferences.assistantLanguage) {
                        AssistantLanguage.HINDI_NATIVE -> Locale("hi", "IN")
                        AssistantLanguage.ENGLISH_US -> Locale.US
                        AssistantLanguage.HINGLISH_AUTO -> Locale("en", "IN")
                    }
                }
            }

            ttsInstance.language = targetLocale

            // 2. Select highest-quality human natural voice from available voices
            if (availableVoices.isNotEmpty()) {
                val candidateVoice = findBestHumanVoice(availableVoices, persona, targetLocale)
                if (candidateVoice != null) {
                    ttsInstance.voice = candidateVoice
                }
            }

            // 3. Set natural pitch and speed (avoid robotic high-pitch or monotone drag)
            val naturalPitch = preferences.speechPitch.coerceIn(0.85f, 1.25f)
            val naturalRate = preferences.speechRate.coerceIn(0.85f, 1.20f)
            ttsInstance.setPitch(naturalPitch)
            ttsInstance.setSpeechRate(naturalRate)

        } catch (e: Exception) {
            e.printStackTrace()
            ttsInstance.language = Locale("en", "IN")
        }
    }

    private fun findBestHumanVoice(voices: Set<Voice>, persona: VoicePersona, locale: Locale): Voice? {
        val localeVoices = voices.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
        if (localeVoices.isEmpty()) return null

        val isMaleTarget = persona == VoicePersona.NATURAL_INDIAN_MALE || persona == VoicePersona.NATURAL_US_MALE
        val isFemaleTarget = persona == VoicePersona.NATURAL_INDIAN_FEMALE || persona == VoicePersona.NATURAL_US_FEMALE

        // Priority 1: High/Very High Quality neural voices matching gender
        val matchingQuality = localeVoices.filter { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            val matchesGender = when {
                isMaleTarget -> name.contains("male") || name.contains("-c-") || name.contains("-d-") || name.contains("-m")
                isFemaleTarget -> name.contains("female") || name.contains("-a-") || name.contains("-b-") || name.contains("-f")
                else -> true
            }
            val isHighQuality = voice.quality >= Voice.QUALITY_HIGH || name.contains("network") || name.contains("neural") || name.contains("wavenet")
            matchesGender && isHighQuality
        }

        if (matchingQuality.isNotEmpty()) {
            return matchingQuality.first()
        }

        // Priority 2: Any matching gender in that locale
        val genderMatching = localeVoices.filter { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            if (isMaleTarget) name.contains("male") || name.contains("-c-") || name.contains("-d-")
            else if (isFemaleTarget) name.contains("female") || name.contains("-a-") || name.contains("-b-")
            else true
        }

        if (genderMatching.isNotEmpty()) {
            return genderMatching.first()
        }

        // Priority 3: Fallback to any high quality voice for the locale
        return localeVoices.maxByOrNull { it.quality } ?: localeVoices.firstOrNull()
    }

    fun startListening() {
        mainHandler.post {
            try {
                // Stop any speaking first
                stopSpeaking()
                performHapticFeedback()

                // Verify microphone permission before initializing SpeechRecognizer
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    _liveTranscript.value = "Microphone permission required. Please grant mic permission."
                    _assistantState.value = AssistantState.ERROR
                    return@post
                }

                _liveTranscript.value = "Listening... Speak in English or Hinglish"
                _assistantState.value = AssistantState.LISTENING
                _rmsAudioLevel.value = 0.2f

                if (speechRecognizer != null) {
                    try {
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    speechRecognizer = null
                }

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _liveTranscript.value = "Speech recognition service not ready on this device. You can also type commands."
                    _assistantState.value = AssistantState.ERROR
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _assistantState.value = AssistantState.LISTENING
                            _liveTranscript.value = "Listening... (e.g. \"Torch on karo\", \"Calculate 25 * 4\", \"Volume badhao\")"
                        }

                        override fun onBeginningOfSpeech() {
                            _assistantState.value = AssistantState.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize RMS dB (usually -2 to 10) to 0.1 - 1.5
                            val normalized = ((rmsdB + 2f) / 10f).coerceIn(0.1f, 1.5f)
                            _rmsAudioLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _assistantState.value = AssistantState.THINKING
                            _rmsAudioLevel.value = 0f
                        }

                        override fun onError(error: Int) {
                            _rmsAudioLevel.value = 0f
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch any speech. Tap orb to retry."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap orb to speak."
                                SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Check mic permission."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue for voice recognition."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice service network timeout."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mic busy. Tap to restart."
                                SpeechRecognizer.ERROR_CLIENT -> "Voice service ready. Tap to speak."
                                else -> "Mic ready. Tap orb to speak again."
                            }
                            _liveTranscript.value = errorMsg
                            _assistantState.value = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                AssistantState.IDLE
                            } else {
                                AssistantState.ERROR
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _rmsAudioLevel.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                _liveTranscript.value = "\"$text\""
                                _assistantState.value = AssistantState.THINKING
                                performHapticFeedback()
                                onSpeechRecognized(text)
                            } else {
                                _liveTranscript.value = "Didn't hear any words. Tap orb to retry."
                                _assistantState.value = AssistantState.IDLE
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                _liveTranscript.value = "\"$text...\""
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                // Setup multi-lingual recognition supporting English, Hinglish, and Hindi seamlessly
                val primaryLanguage = when (preferences.assistantLanguage) {
                    AssistantLanguage.HINGLISH_AUTO -> "en-IN" // Native support for Hinglish & English
                    AssistantLanguage.ENGLISH_US -> "en-US"
                    AssistantLanguage.HINDI_NATIVE -> "hi-IN"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLanguage)
                    // Enable multilingual recognition for mixed speech
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                _liveTranscript.value = "Speech recognition start failed: ${e.message}"
                _assistantState.value = AssistantState.ERROR
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _assistantState.value = AssistantState.IDLE
                _rmsAudioLevel.value = 0f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!preferences.isTtsEnabled || !isTtsInitialized) {
            _assistantState.value = AssistantState.IDLE
            onFinished?.invoke()
            return
        }

        val cleanedSpeechText = cleanTextForNaturalSpeech(text)
        if (cleanedSpeechText.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            onFinished?.invoke()
            return
        }

        _assistantState.value = AssistantState.SPEAKING
        applyHumanLikeVoice()

        val utteranceId = "echo_${System.currentTimeMillis()}"
        tts?.speak(cleanedSpeechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Cleans text to make TTS sound warm, natural, and human-like by removing
     * formatting symbols, markdown asterisks, URLs, bullet points, and robotic syntax.
     */
    private fun cleanTextForNaturalSpeech(raw: String): String {
        return raw
            // Remove markdown headings, bold, italics, code blocks
            .replace(Regex("[*#_`~]"), "")
            // Remove bracketed citations or notes e.g. [1], [action]
            .replace(Regex("\\[.*?\\]"), "")
            // Remove emojis & special symbols that make TTS pronounce raw names
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Remove URLs
            .replace(Regex("https?://\\S+"), "")
            // Clean excessive punctuation
            .replace(Regex("\\.{2,}"), ".")
            .replace(Regex("-{2,}"), " - ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setState(state: AssistantState) {
        _assistantState.value = state
    }

    fun setLiveTranscript(text: String) {
        _liveTranscript.value = text
    }

    private fun performHapticFeedback() {
        if (!preferences.isHapticsEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
