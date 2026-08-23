package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AssistantPreferences
import com.example.data.local.EchoDatabase
import com.example.data.local.OrbTheme
import com.example.engine.ActionResult
import com.example.engine.BatteryInfo
import com.example.engine.DeviceController
import com.example.engine.EchoNlpEngine
import com.example.engine.VolumeInfo
import com.example.service.EchoFloatingBubbleService
import com.example.service.EchoNotificationHelper
import com.example.ui.components.BatteryStatusCard
import com.example.ui.components.FlashlightControlCard
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.NotesAndHistoryView
import com.example.ui.components.QuickAppLauncherGrid
import com.example.ui.components.RedmiSetupGuide
import com.example.ui.components.SiriOrbVisualizer
import com.example.ui.components.VolumeControlCard
import com.example.ui.theme.DarkNebulaSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadiantMagenta
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import com.example.voice.AssistantState
import com.example.voice.EchoVoiceManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AssistantPreferences
    private lateinit var deviceController: DeviceController
    private lateinit var nlpEngine: EchoNlpEngine
    private lateinit var voiceManager: EchoVoiceManager
    private lateinit var database: EchoDatabase

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && preferences.isQuickNotificationEnabled) {
            EchoNotificationHelper.showNotification(this)
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            if (preferences.isFloatingBubbleEnabled) {
                EchoFloatingBubbleService.start(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AssistantPreferences(this)
        deviceController = DeviceController(this)
        nlpEngine = EchoNlpEngine(this, deviceController)
        database = EchoDatabase.getDatabase(this)

        voiceManager = EchoVoiceManager(this) { spokenQuery ->
            executeCommand(spokenQuery)
        }

        // Check & request microphone permission on initial launch so background/long-press services have access
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Show persistent quick-trigger notification if enabled
        if (preferences.isQuickNotificationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    EchoNotificationHelper.showNotification(this)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                EchoNotificationHelper.showNotification(this)
            }
        }

        // Start floating orb overlay if enabled
        if (preferences.isFloatingBubbleEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                EchoFloatingBubbleService.start(this)
            }
        }

        setContent {
            MyApplicationTheme {
                MainAssistantDashboard(
                    voiceManager = voiceManager,
                    deviceController = deviceController,
                    database = database,
                    preferences = preferences,
                    onRequestMicPermission = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRequestOverlayPermission = {
                        requestOverlayPermission()
                    },
                    onExecuteQuery = { query ->
                        executeCommand(query)
                    },
                    onOpenAssistantOverlay = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            requestOverlayPermission()
                        } else {
                            EchoFloatingBubbleService.startVoiceInteraction(this)
                        }
                    }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (preferences.isFloatingBubbleEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                if (!EchoFloatingBubbleService.isRunning) {
                    EchoFloatingBubbleService.start(this)
                }
            }
        }
    }

    private fun executeCommand(query: String) {
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        coroutineScope.launch {
            voiceManager.setState(AssistantState.THINKING)
            val result = nlpEngine.processQuery(query)
            voiceManager.setLiveTranscript(result.responseText)
            voiceManager.speak(result.responseText)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAssistantDashboard(
    voiceManager: EchoVoiceManager,
    deviceController: DeviceController,
    database: EchoDatabase,
    preferences: AssistantPreferences,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onExecuteQuery: (String) -> Unit,
    onOpenAssistantOverlay: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by voiceManager.assistantState.collectAsState()
    val audioLevel by voiceManager.rmsAudioLevel.collectAsState()
    val transcript by voiceManager.liveTranscript.collectAsState()

    val historyList by database.assistantDao().getAllHistory().collectAsState(initial = emptyList())
    val notesList by database.assistantDao().getAllNotes().collectAsState(initial = emptyList())

    var isTorchOn by remember { mutableStateOf(DeviceController.isFlashlightOn) }
    var batteryInfo by remember { mutableStateOf(deviceController.getBatteryInfo()) }
    var volumeInfo by remember { mutableStateOf(deviceController.getVolumeInfo()) }
    var selectedOrbTheme by remember { mutableStateOf(preferences.orbTheme) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasOverlayPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    val quickActionChips = listOf(
        Pair("Explain Quantum Physics", Icons.Default.AutoAwesome),
        Pair("Tell me a fun science fact", Icons.Default.AutoAwesome),
        Pair("Calculate 25 * 4", Icons.Default.Calculate),
        Pair("Turn on Flashlight", Icons.Default.FlashlightOn),
        Pair("Play Music", Icons.Default.MusicNote),
        Pair("Volume to 80%", Icons.Default.VolumeUp),
        Pair("Search YouTube", Icons.Default.PlayArrow),
        Pair("Set 5m Timer", Icons.Default.Timer),
        Pair("Take Note", Icons.Default.Chat),
        Pair("Tell me a Joke", Icons.Default.AutoAwesome),
        Pair("Battery Status", Icons.Default.Refresh)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("main_scaffold"),
        containerColor = ObsidianBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Echo Logo, Title, and Settings button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(selectedOrbTheme.primaryColorHex), Color(selectedOrbTheme.secondaryColorHex))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Echo Assistant",
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Default Voice & Device Controller",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onOpenAssistantOverlay,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VividViolet.copy(alpha = 0.2f))
                                .testTag("launch_siri_overlay_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Siri Circle Overlay",
                                tint = VividViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .testTag("open_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Siri Dynamic Animated Orb Section
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("siri_orb_hero_card"),
                    borderColor = Color(selectedOrbTheme.primaryColorHex).copy(alpha = 0.35f),
                    backgroundColor = DarkNebulaSurface.copy(alpha = 0.9f),
                    contentPadding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SiriOrbVisualizer(
                            state = state,
                            audioLevel = audioLevel,
                            orbTheme = selectedOrbTheme,
                            sizeDp = 200.dp,
                            onClick = {
                                if (state == AssistantState.LISTENING) {
                                    voiceManager.stopListening()
                                } else {
                                    if (hasMicPermission) {
                                        voiceManager.startListening()
                                    } else {
                                        onRequestMicPermission()
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val stateMessage = when (state) {
                            AssistantState.LISTENING -> "Listening to your voice..."
                            AssistantState.THINKING -> "Processing command..."
                            AssistantState.SPEAKING -> "Speaking response..."
                            AssistantState.ERROR -> "Couldn't understand. Tap to retry."
                            AssistantState.IDLE -> "Tap orb to speak or type below"
                        }

                        Text(
                            text = stateMessage,
                            fontWeight = FontWeight.Bold,
                            color = when (state) {
                                AssistantState.LISTENING -> NeonCyan
                                AssistantState.THINKING -> VividViolet
                                AssistantState.SPEAKING -> RadiantMagenta
                                else -> TextSecondary
                            },
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        if (transcript.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131B2E))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = transcript,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Quick Query Suggestions
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickActionChips) { (query, icon) ->
                        AssistChip(
                            onClick = { onExecuteQuery(query) },
                            label = { Text(query, color = TextPrimary, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF131C30)
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = Color(0xFF27354E)
                            )
                        )
                    }
                }
            }

            // Silent Query Input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask Echo or type command...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("main_text_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF27354E),
                            focusedContainerColor = DarkNebulaSurface,
                            unfocusedContainerColor = DarkNebulaSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    onExecuteQuery(textInput)
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
                                onExecuteQuery(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            } else {
                                if (hasMicPermission) voiceManager.startListening() else onRequestMicPermission()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (textInput.isNotBlank()) NeonCyan else VividViolet)
                            .testTag("main_send_btn")
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

            // Section 1: Mobile Device Controls
            item {
                Text(
                    text = "Device Controls Station",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }

            item {
                FlashlightControlCard(
                    isOn = isTorchOn,
                    onToggle = { enable ->
                        deviceController.toggleFlashlight(enable)
                        isTorchOn = DeviceController.isFlashlightOn
                    }
                )
            }

            item {
                BatteryStatusCard(
                    batteryInfo = batteryInfo,
                    onRefresh = {
                        batteryInfo = deviceController.getBatteryInfo()
                    }
                )
            }

            item {
                VolumeControlCard(
                    volumeInfo = volumeInfo,
                    onVolumeChange = { pct ->
                        deviceController.setMediaVolumePercent(pct)
                        volumeInfo = deviceController.getVolumeInfo()
                    },
                    onMuteToggle = { mute ->
                        deviceController.muteVolume(mute)
                        volumeInfo = deviceController.getVolumeInfo()
                    }
                )
            }

            item {
                QuickAppLauncherGrid(
                    onLaunchApp = { appName ->
                        deviceController.openApp(appName)
                    }
                )
            }

            // Section 2: Floating Orb & Redmi Assistant Setup
            item {
                Text(
                    text = "Floating Orb & System Shortcuts",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }

            // Dedicated System Floating Orb Card
            item {
                var isOrbActive by remember { mutableStateOf(preferences.isFloatingBubbleEnabled) }
                GlassmorphicCard(
                    borderColor = if (isOrbActive) VividViolet.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.3f),
                    backgroundColor = DarkNebulaSurface.copy(alpha = 0.95f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    NeonCyan,
                                                    VividViolet
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Floating Orb",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "System Floating Orb Overlay",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (isOrbActive) "Active on screen • Drag & tap to speak" else "Stays visible over games & other apps",
                                        color = if (isOrbActive) NeonCyan else TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isOrbActive,
                                onCheckedChange = { enable ->
                                    if (enable && !hasOverlayPermission) {
                                        onRequestOverlayPermission()
                                    } else {
                                        isOrbActive = enable
                                        preferences.isFloatingBubbleEnabled = enable
                                        if (enable) {
                                            EchoFloatingBubbleService.start(context)
                                        } else {
                                            EchoFloatingBubbleService.stop(context)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = VividViolet
                                )
                            )
                        }

                        if (!hasOverlayPermission) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRequestOverlayPermission,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant 'Display Over Other Apps' Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                RedmiSetupGuide(
                    hasMicPermission = hasMicPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestMicPermission = onRequestMicPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onOpenDefaultAssistantSettings = {
                        deviceController.openDefaultAssistantSettings()
                    },
                    onOpenGestureSettings = {
                        deviceController.openRedmiGestureSettings()
                    },
                    onTestAssistantOverlay = onOpenAssistantOverlay
                )
            }

            // Section 3: Notes & History
            item {
                Text(
                    text = "Echo Memory & Notes",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }

            item {
                NotesAndHistoryView(
                    notes = notesList,
                    history = historyList,
                    onDeleteNote = { note ->
                        coroutineScope.launch {
                            database.assistantDao().deleteNote(note)
                        }
                    },
                    onClearHistory = {
                        coroutineScope.launch {
                            database.assistantDao().clearHistory()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkNebulaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Echo Assistant Settings",
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )

                // Orb Theme Selector
                Text(
                    text = "Siri Circle Theme",
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 14.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(OrbTheme.values()) { theme ->
                        val isSelected = selectedOrbTheme == theme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(theme.primaryColorHex).copy(alpha = 0.25f)
                                    else Color(0xFF1E293B)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.primaryColorHex) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedOrbTheme = theme
                                    preferences.orbTheme = theme
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.displayName,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Persistent Quick Notification Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Persistent Quick Trigger", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Swipe down from any app to summon assistant", color = TextSecondary, fontSize = 12.sp)
                    }
                    var notifEnabled by remember { mutableStateOf(preferences.isQuickNotificationEnabled) }
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = {
                            notifEnabled = it
                            preferences.isQuickNotificationEnabled = it
                            if (it) {
                                EchoNotificationHelper.showNotification(context)
                            } else {
                                EchoNotificationHelper.hideNotification(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonCyan)
                    )
                }

                // Floating Screen Bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Assist Orb", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Glowing floating orb on screen over other apps", color = TextSecondary, fontSize = 12.sp)
                    }
                    var bubbleEnabled by remember { mutableStateOf(preferences.isFloatingBubbleEnabled) }
                    Switch(
                        checked = bubbleEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !hasOverlayPermission) {
                                onRequestOverlayPermission()
                            } else {
                                bubbleEnabled = enable
                                preferences.isFloatingBubbleEnabled = enable
                                if (enable) {
                                    EchoFloatingBubbleService.start(context)
                                } else {
                                    EchoFloatingBubbleService.stop(context)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = VividViolet)
                    )
                }

                // Voice Response Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Voice Response (TTS)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Echo speaks answers aloud", color = TextSecondary, fontSize = 12.sp)
                    }
                    var ttsEnabled by remember { mutableStateOf(preferences.isTtsEnabled) }
                    Switch(
                        checked = ttsEnabled,
                        onCheckedChange = {
                            ttsEnabled = it
                            preferences.isTtsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonCyan)
                    )
                }

                // Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Haptic Feedback", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Vibrate on wake & command", color = TextSecondary, fontSize = 12.sp)
                    }
                    var hapticsEnabled by remember { mutableStateOf(preferences.isHapticsEnabled) }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            preferences.isHapticsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = VividViolet)
                    )
                }

                // Auto-listen on launch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Listen on Launch", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Start listening when power button held", color = TextSecondary, fontSize = 12.sp)
                    }
                    var autoListen by remember { mutableStateOf(preferences.autoListenOnLaunch) }
                    Switch(
                        checked = autoListen,
                        onCheckedChange = {
                            autoListen = it
                            preferences.autoListenOnLaunch = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = SolarAmber)
                    )
                }

                // Gemini 3.5 Flash AI Intelligence Configuration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131A2A))
                        .border(1.dp, Color(0xFF25334E), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gemini 3.5 Flash Intelligence",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        val hasKey = preferences.hasValidGeminiApiKey()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasKey) EmeraldGlow.copy(alpha = 0.2f) else SolarAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hasKey) "ACTIVE & TALKING" else "READY",
                                color = if (hasKey) EmeraldGlow else SolarAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Text(
                        text = "Powered by Google Gemini 3.5 Flash for natural conversations, answering questions, giving advice, and spoken voice intelligence. Auto-configured via AI Studio Secrets.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    var apiKeyInput by remember { mutableStateOf(preferences.customGeminiApiKey) }
                    var showKeyPassword by remember { mutableStateOf(false) }
                    var keySavedConfirmation by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keySavedConfirmation = false
                        },
                        label = { Text("Custom Gemini API Key (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("Enter key or leave blank to use build secret", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        singleLine = true,
                        visualTransformation = if (showKeyPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = "API Key", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showKeyPassword = !showKeyPassword }) {
                                Icon(
                                    if (showKeyPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key Visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF25334E),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                preferences.customGeminiApiKey = apiKeyInput
                                keySavedConfirmation = true
                            },
                            modifier = Modifier.weight(1f).testTag("save_api_key_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (keySavedConfirmation) "Saved!" else "Save Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (apiKeyInput.isNotBlank()) {
                            Button(
                                onClick = {
                                    apiKeyInput = ""
                                    preferences.customGeminiApiKey = ""
                                    keySavedConfirmation = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
