package com.example.voice

import android.content.Context
import android.content.Intent
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
import com.example.data.local.AssistantPreferences
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
                tts?.language = Locale.getDefault()
                tts?.setPitch(preferences.speechPitch)
                tts?.setSpeechRate(preferences.speechRate)
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

    fun startListening() {
        mainHandler.post {
            try {
                // Stop any speaking first
                stopSpeaking()
                performHapticFeedback()

                _liveTranscript.value = "Listening... Speak your request"
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
                            _liveTranscript.value = "Listening... (e.g. \"Calculate 25 * 4\", \"Play music\", \"Turn on flashlight\")"
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

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
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

        _assistantState.value = AssistantState.SPEAKING
        tts?.setPitch(preferences.speechPitch)
        tts?.setSpeechRate(preferences.speechRate)

        val utteranceId = "echo_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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

