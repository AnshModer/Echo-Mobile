package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AssistantPreferences
import com.example.engine.ActionResult
import com.example.engine.DeviceController
import com.example.engine.EchoNlpEngine
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.SiriOrbVisualizer
import com.example.ui.theme.DarkNebulaSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantMagenta
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import com.example.voice.AssistantState
import com.example.voice.EchoVoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AssistantActivity : ComponentActivity() {

    private lateinit var deviceController: DeviceController
    private lateinit var nlpEngine: EchoNlpEngine
    private lateinit var voiceManager: EchoVoiceManager
    private lateinit var preferences: AssistantPreferences

    private var currentActionResult by mutableStateOf<ActionResult?>(null)

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        } else {
            voiceManager.setLiveTranscript("Microphone permission required for voice assistant. You can also type commands.")
            voiceManager.setState(AssistantState.ERROR)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = AssistantPreferences(this)
        deviceController = DeviceController(this)
        nlpEngine = EchoNlpEngine(this, deviceController)

        voiceManager = EchoVoiceManager(this) { spokenText ->
            processQuery(spokenText)
        }

        setContent {
            MyApplicationTheme {
                AssistantOverlayScreen(
                    voiceManager = voiceManager,
                    lastResult = currentActionResult,
                    onDismiss = { finish() },
                    onQuerySubmitted = { query -> processQuery(query) },
                    onToggleVoice = {
                        toggleVoiceListening()
                    }
                )
            }
        }

        startListeningWithPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startListeningWithPermission()
    }

    private fun startListeningWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun toggleVoiceListening() {
        if (voiceManager.assistantState.value == AssistantState.LISTENING) {
            voiceManager.stopListening()
        } else {
            startListeningWithPermission()
        }
    }

    private fun processQuery(query: String) {
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        coroutineScope.launch {
            voiceManager.setState(AssistantState.THINKING)
            val actionResult = nlpEngine.processQuery(query)
            currentActionResult = actionResult
            voiceManager.setLiveTranscript(actionResult.responseText)
            voiceManager.speak(actionResult.responseText)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

@Composable
fun AssistantOverlayScreen(
    voiceManager: EchoVoiceManager,
    lastResult: ActionResult? = null,
    onDismiss: () -> Unit,
    onQuerySubmitted: (String) -> Unit,
    onToggleVoice: () -> Unit
) {
    val state by voiceManager.assistantState.collectAsState()
    val audioLevel by voiceManager.rmsAudioLevel.collectAsState()
    val transcript by voiceManager.liveTranscript.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val quickCommands = listOf(
        Pair("Calculate 25 * 4", Icons.Default.Calculate),
        Pair("Play music", Icons.Default.MusicNote),
        Pair("Search YouTube", Icons.Default.PlayArrow),
        Pair("Turn on Flashlight", Icons.Default.FlashlightOn),
        Pair("Set Volume 80%", Icons.Default.VolumeUp),
        Pair("Battery level", Icons.Default.GraphicEq),
        Pair("Set timer 5 min", Icons.Default.Timer),
        Pair("Tell me a joke", Icons.Default.Mic)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("assistant_overlay_screen"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating bottom sheet containing the Siri Orb and Assistant Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10172A).copy(alpha = 0.96f),
                            Color(0xFF090D1A).copy(alpha = 0.99f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // prevent closing when tapping sheet
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle & Dismiss Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_assistant_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Siri Glowing Circle Animation
            SiriOrbVisualizer(
                state = state,
                audioLevel = audioLevel,
                sizeDp = 175.dp,
                onClick = onToggleVoice,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Status Indicator Text
            val statusText = when (state) {
                AssistantState.LISTENING -> "Listening... Speak your request"
                AssistantState.THINKING -> "Echo is processing..."
                AssistantState.SPEAKING -> "Echo is responding..."
                AssistantState.ERROR -> "Couldn't hear clearly. Tap orb to retry."
                AssistantState.IDLE -> "Tap glowing orb or speak a command"
            }

            val statusColor = when (state) {
                AssistantState.LISTENING -> NeonCyan
                AssistantState.THINKING -> VividViolet
                AssistantState.SPEAKING -> RadiantMagenta
                AssistantState.ERROR -> SolarAmber
                AssistantState.IDLE -> TextSecondary
            }

            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Calculation Result Special Display Card
            if (lastResult is ActionResult.CalculationAction) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    borderColor = NeonCyan.copy(alpha = 0.5f),
                    backgroundColor = DarkNebulaSurface.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = lastResult.expression,
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "= ${lastResult.resultValue}",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calculator",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (transcript.isNotEmpty()) {
                // Live Transcript Box
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    borderColor = NeonCyan.copy(alpha = 0.3f),
                    backgroundColor = DarkNebulaSurface.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = transcript,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Suggestions Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { (cmd, icon) ->
                    AssistChip(
                        onClick = {
                            onQuerySubmitted(cmd)
                        },
                        label = { Text(cmd, color = TextPrimary, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Silent Text Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask or control anything (e.g. 50 * 4)...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_text_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = DarkNebulaSurface,
                        unfocusedContainerColor = DarkNebulaSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                onQuerySubmitted(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onQuerySubmitted(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        } else {
                            onToggleVoice()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (textInput.isNotBlank()) NeonCyan else VividViolet)
                        .testTag("assistant_send_btn")
                ) {
                    Icon(
                        imageVector = if (textInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                        contentDescription = "Submit",
                        tint = if (textInput.isNotBlank()) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

