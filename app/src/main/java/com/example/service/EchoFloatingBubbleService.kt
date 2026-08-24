package com.example.service

import android.Manifest
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AssistantPreferences
import com.example.engine.ActionResult
import com.example.engine.DeviceController
import com.example.engine.EchoNlpEngine
import com.example.voice.AssistantState
import com.example.voice.EchoVoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service managing the System Floating Orb Voice Assistant Overlay.
 * Directly listens, processes queries via Echo NLP / DeviceController / Gemini AI,
 * speaks responses via TTS, and displays animated state indicators over any app.
 */
class EchoFloatingBubbleService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_ORB"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_ORB"
        const val ACTION_TRIGGER_VOICE_INTERACTION = "com.example.service.ACTION_TRIGGER_VOICE_INTERACTION"
        const val NOTIFICATION_CHANNEL_ID = "echo_floating_assistant_channel"
        const val NOTIFICATION_ID = 2002

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, EchoFloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startVoiceInteraction(context: Context) {
            val intent = Intent(context, EchoFloatingBubbleService::class.java).apply {
                action = ACTION_TRIGGER_VOICE_INTERACTION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EchoFloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayLayout: FloatingAssistantOverlayLayout? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private lateinit var preferences: AssistantPreferences
    private lateinit var deviceController: DeviceController
    private lateinit var nlpEngine: EchoNlpEngine
    private lateinit var voiceManager: EchoVoiceManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null
    private var currentProcessingJob: Job? = null

    private var initialX = 0
    private var initialY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        preferences = AssistantPreferences(this)
        deviceController = DeviceController(this)
        nlpEngine = EchoNlpEngine(this, deviceController)

        voiceManager = EchoVoiceManager(this) { spokenText ->
            handleSpokenCommand(spokenText)
        }

        createNotificationChannel()
        updateForegroundState(isMicrophoneActive = false)

        observeVoiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeOverlayView()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_VOICE_INTERACTION -> {
                showOverlayAndStartListening()
            }
            ACTION_START -> {
                if (preferences.isFloatingBubbleEnabled) {
                    ensureOverlayAttached(startListeningImmediately = false)
                }
            }
            else -> {
                showOverlayAndStartListening()
            }
        }
        return START_STICKY
    }

    private fun updateForegroundState(isMicrophoneActive: Boolean) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notification = buildForegroundNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val serviceType = if (isMicrophoneActive && hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (isMicrophoneActive && hasMicPermission) {
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

    private fun observeVoiceState() {
        serviceScope.launch {
            voiceManager.rmsAudioLevel.collectLatest { level ->
                overlayLayout?.orbView?.setRmsAudioLevel(level)
            }
        }

        serviceScope.launch {
            voiceManager.liveTranscript.collectLatest { transcript ->
                if (transcript.isNotBlank()) {
                    val currentState = voiceManager.assistantState.value
                    overlayLayout?.updateState(currentState, transcript)
                }
            }
        }
    }

    private fun showOverlayAndStartListening() {
        cancelAutoDismiss()

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            updateForegroundState(isMicrophoneActive = false)
            ensureOverlayAttached(startListeningImmediately = false)
            voiceManager.stopSpeaking()
            overlayLayout?.updateState(
                AssistantState.ERROR,
                "Microphone permission required. Please open Echo once to allow microphone access."
            )
            scheduleAutoDismiss(delayMillis = 4500L)
            return
        }

        // Ensure foreground service is designated with MICROPHONE foreground type before starting audio capture
        updateForegroundState(isMicrophoneActive = true)
        ensureOverlayAttached(startListeningImmediately = true)
        voiceManager.stopSpeaking()
        overlayLayout?.updateState(AssistantState.LISTENING, "Listening... Speak your request")
        voiceManager.startListening()
    }

    private fun ensureOverlayAttached(startListeningImmediately: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        if (windowManager == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        if (overlayLayout == null) {
            val screenSize = Point()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getSize(screenSize)

            val initialPosX = (screenSize.x - (280 * resources.displayMetrics.density).toInt()) / 2
            val initialPosY = (screenSize.y * 0.55f).toInt()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val bottomMarginPx = (18 * density).toInt()

            windowLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = 0
                y = bottomMarginPx
            }

            overlayLayout = FloatingAssistantOverlayLayout(this).apply {
                setCallbacks(
                    onOrbClick = {
                        handleOrbClick()
                    },
                    onClose = {
                        dismissInteraction()
                    },
                    onDragMove = { dx, dy ->
                        this@EchoFloatingBubbleService.windowLayoutParams?.let { lp ->
                            lp.x = initialX + dx
                            lp.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(this@apply, lp)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onSnap = {
                        snapOverlayToEdge()
                    }
                )
            }

            try {
                windowManager?.addView(overlayLayout, windowLayoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            overlayLayout?.visibility = View.VISIBLE
        }

        if (startListeningImmediately) {
            overlayLayout?.updateState(AssistantState.LISTENING, "Listening... Speak your command")
        } else {
            overlayLayout?.updateState(AssistantState.IDLE, null)
        }
    }

    private fun handleOrbClick() {
        val currentState = voiceManager.assistantState.value
        when (currentState) {
            AssistantState.LISTENING -> {
                // User tapped while listening -> cancel or dismiss
                voiceManager.stopListening()
                dismissInteraction()
            }
            AssistantState.THINKING, AssistantState.SPEAKING -> {
                // User tapped while speaking -> stop and dismiss
                voiceManager.stopSpeaking()
                currentProcessingJob?.cancel()
                dismissInteraction()
            }
            AssistantState.IDLE, AssistantState.ERROR -> {
                // User tapped idle orb -> trigger voice interaction right here
                showOverlayAndStartListening()
            }
        }
    }

    private fun handleSpokenCommand(query: String) {
        cancelAutoDismiss()
        updateForegroundState(isMicrophoneActive = false)
        voiceManager.setState(AssistantState.THINKING)
        overlayLayout?.updateState(AssistantState.THINKING, "\"$query\"")

        currentProcessingJob?.cancel()
        currentProcessingJob = serviceScope.launch {
            try {
                // Process the command via NLP Engine / Device Controller / Gemini AI
                val actionResult: ActionResult = nlpEngine.processQuery(query)

                voiceManager.setState(AssistantState.SPEAKING)
                overlayLayout?.updateState(AssistantState.SPEAKING, actionResult.responseText)

                // Speak response with TTS
                voiceManager.speak(actionResult.responseText) {
                    scheduleAutoDismiss(delayMillis = 2000L)
                }

                // Fallback auto dismiss in case TTS is disabled or silent
                if (!preferences.isTtsEnabled) {
                    scheduleAutoDismiss(delayMillis = 3500L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                overlayLayout?.updateState(AssistantState.ERROR, "Error executing command: ${e.localizedMessage}")
                scheduleAutoDismiss(delayMillis = 3000L)
            }
        }
    }

    private fun snapOverlayToEdge() {
        windowLayoutParams?.let { lp ->
            initialX = lp.x
            initialY = lp.y
        }
    }

    private fun scheduleAutoDismiss(delayMillis: Long) {
        cancelAutoDismiss()
        autoDismissRunnable = Runnable {
            dismissInteraction()
        }
        mainHandler.postDelayed(autoDismissRunnable!!, delayMillis)
    }

    private fun cancelAutoDismiss() {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null
    }

    private fun dismissInteraction() {
        voiceManager.stopSpeaking()
        voiceManager.stopListening()

        if (preferences.isFloatingBubbleEnabled) {
            // Keep minimal idle orb on screen
            overlayLayout?.updateState(AssistantState.IDLE, null)
        } else {
            // Completely hide overlay
            removeOverlayView()
        }
    }

    private fun removeOverlayView() {
        overlayLayout?.let { layout ->
            try {
                windowManager?.removeView(layout)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayLayout = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Echo Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice assistant floating overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Echo Voice Assistant")
            .setContentText("Ready for voice commands • Long-press power to speak")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        cancelAutoDismiss()
        currentProcessingJob?.cancel()
        serviceScope.cancel()
        voiceManager.destroy()
        removeOverlayView()
        super.onDestroy()
    }
}
