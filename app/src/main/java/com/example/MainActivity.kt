package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.example.engine.ActivityTracker
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.engine.ContactLookupHelper
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import com.example.ui.components.AssistantActiveResultCard
import com.example.ui.components.BatteryStatusCard
import com.example.ui.components.ContactCallCard
import com.example.ui.components.FlashlightControlCard
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.NotesAndHistoryView
import com.example.ui.components.QuickAppLauncherGrid
import com.example.ui.components.RedmiSetupGuide
import com.example.ui.components.ScreenshotControlCard
import com.example.ui.components.SiriOrbVisualizer
import com.example.ui.components.SiriVoiceWaveform
import com.example.ui.components.StitchMultimodalSmartCard
import com.example.ui.components.VersionComparisonSheet
import com.example.ui.components.VolumeControlCard
import com.example.ui.components.WhatsAppMessageCard
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
import com.example.ui.theme.StitchObsidianLowest
import com.example.ui.theme.StitchCyanContainer
import com.example.ui.theme.StitchCyanFixed
import com.example.ui.theme.StitchVioletContainer
import com.example.ui.theme.StitchSurfaceLow
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceHigh
import com.example.ui.theme.StitchSurfaceHighest
import com.example.ui.theme.StitchOnSurface
import com.example.ui.theme.StitchOnSurfaceVariant
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

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions updated
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
                    onRequestContactPermission = {
                        contactsPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                            )
                        )
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
        ActivityTracker.setCurrentActivity(this)
        if (preferences.isFloatingBubbleEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                if (!EchoFloatingBubbleService.isRunning) {
                    EchoFloatingBubbleService.start(this)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        ActivityTracker.clearCurrentActivity(this)
    }

    private fun executeCommand(query: String) {
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        coroutineScope.launch {
            voiceManager.setState(AssistantState.THINKING)
            val result = nlpEngine.processQuery(query)
            voiceManager.setLastActionResult(result)
            voiceManager.setLiveTranscript(result.responseText)
            voiceManager.speak(result.responseText)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

enum class DashboardTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    VOICE("Echo", Icons.Default.Mic),
    CONTROLS("Lens", Icons.Default.ViewInAr),
    HISTORY("Timeline", Icons.Default.History),
    SETUP("Skills", Icons.Default.AutoMode)
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
    onRequestContactPermission: () -> Unit,
    onExecuteQuery: (String) -> Unit,
    onOpenAssistantOverlay: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by voiceManager.assistantState.collectAsState()
    val audioLevel by voiceManager.rmsAudioLevel.collectAsState()
    val transcript by voiceManager.liveTranscript.collectAsState()
    val lastActionResult by voiceManager.lastActionResult.collectAsState()

    val historyList by database.assistantDao().getAllHistory().collectAsState(initial = emptyList())
    val notesList by database.assistantDao().getAllNotes().collectAsState(initial = emptyList())

    var currentTab by remember { mutableStateOf(DashboardTab.VOICE) }
    var isTorchOn by remember { mutableStateOf(DeviceController.isFlashlightOn) }
    var batteryInfo by remember { mutableStateOf(deviceController.getBatteryInfo()) }
    var volumeInfo by remember { mutableStateOf(deviceController.getVolumeInfo()) }
    var selectedOrbTheme by remember { mutableStateOf(preferences.orbTheme) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showComparisonSheet by remember { mutableStateOf(false) }
    var showShowcaseWeather by remember { mutableStateOf(true) }

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasOverlayPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    val quickActionChips = listOf(
        Pair("Weather in New York", Icons.Default.WbSunny),
        Pair("Good Morning Briefing", Icons.Default.LightMode),
        Pair("Translate 'Hello' to French", Icons.Default.Translate),
        Pair("Schedule Team Meeting", Icons.Default.CalendarMonth),
        Pair("Silent Mode", Icons.Default.NotificationsOff),
        Pair("Flip a Coin", Icons.Default.AutoAwesome),
        Pair("Roll a Dice", Icons.Default.AutoAwesome),
        Pair("100 Miles in KM", Icons.Default.Calculate),
        Pair("WhatsApp Mom: I'm late", Icons.Default.Chat),
        Pair("Take Screenshot", Icons.Default.AutoAwesome),
        Pair("Call Mom", Icons.Default.Call),
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
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF131728),
                        StitchSurfaceLow,
                        StitchObsidianLowest
                    ),
                    radius = 1600f
                )
            )
            .statusBarsPadding()
            .imePadding()
            .testTag("main_scaffold"),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (currentTab != DashboardTab.VOICE) {
                FloatingActionButton(
                    onClick = {
                        currentTab = DashboardTab.VOICE
                        if (hasMicPermission) {
                            voiceManager.startListening()
                        } else {
                            onRequestMicPermission()
                        }
                    },
                    containerColor = StitchCyanContainer,
                    contentColor = Color(0xFF00363D),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("quick_fab_mic")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Quick Speak",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = StitchObsidianLowest.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(Color(0xFF262D3D), Color.Transparent)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00363D),
                            selectedTextColor = StitchCyanContainer,
                            indicatorColor = StitchCyanContainer,
                            unselectedIconColor = StitchOnSurfaceVariant,
                            unselectedTextColor = StitchOnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stitch Status Bar & Meta
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        fontSize = 12.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(15.dp))
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(15.dp))
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(17.dp))
                    }
                }
            }

            // Stitch Header: Echo Assistant Brand, PRO badge, Controls & Profile
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(StitchCyanContainer, StitchVioletContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Echo Assistant",
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StitchCyanContainer.copy(alpha = 0.2f))
                                        .border(1.dp, StitchCyanContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = StitchCyanFixed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Text(
                                text = "24-bit Neural Audio Engine",
                                color = StitchCyanContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showComparisonSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StitchCyanContainer.copy(alpha = 0.15f))
                                .testTag("open_comparison_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Version Comparison",
                                tint = StitchCyanContainer,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenAssistantOverlay,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StitchVioletContainer.copy(alpha = 0.25f))
                                .testTag("launch_siri_overlay_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Siri Circle Overlay",
                                tint = Color(0xFFD8B4FE),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StitchSurfaceHigh)
                                .testTag("open_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = StitchOnSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StitchCyanFixed)
                                .clickable { showSettingsSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF00363D),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Category Segmented Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StitchSurfaceLow)
                        .border(1.dp, Color(0xFF262D3D), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DashboardTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) StitchCyanContainer.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { currentTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) StitchCyanContainer else StitchOnSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            when (currentTab) {
                DashboardTab.VOICE -> {
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

                                Spacer(modifier = Modifier.height(10.dp))

                                SiriVoiceWaveform(
                                    state = state,
                                    audioLevel = audioLevel,
                                    primaryColor = Color(selectedOrbTheme.primaryColorHex),
                                    secondaryColor = Color(selectedOrbTheme.secondaryColorHex)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val dotAlpha by rememberInfiniteTransition(label = "dot_pulse").animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                    label = "dot"
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(StitchSurfaceHigh.copy(alpha = 0.65f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(StitchCyanContainer.copy(alpha = if (state == AssistantState.LISTENING) dotAlpha else 1f))
                                        )
                                        val chipText = when (state) {
                                            AssistantState.LISTENING -> "LISTENING • 24-BIT NEURAL AUDIO ENGINE"
                                            AssistantState.THINKING -> "PROCESSING • NEURAL REASONING CORE"
                                            AssistantState.SPEAKING -> "SPEAKING • FLUID VOICE SYNTHESIS"
                                            AssistantState.ERROR -> "RETRY • TAP ORB TO SPEAK"
                                            AssistantState.IDLE -> "READY • 24-BIT NEURAL AUDIO ENGINE"
                                        }
                                        Text(
                                            text = chipText,
                                            color = StitchOnSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.6.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stitch Live Conversational Context Stream
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val userQueryText = if (transcript.isNotEmpty()) transcript else "How's the weather in San Francisco today?"
                            // User Pill (aligned right)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(StitchSurfaceHigh.copy(alpha = 0.75f))
                                        .clickable {
                                            if (transcript.isEmpty()) {
                                                onExecuteQuery("How's the weather in San Francisco today?")
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = StitchCyanContainer,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "\"$userQueryText\"",
                                            color = StitchCyanFixed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Assistant Pill (aligned left)
                            val assistantResponseText = when {
                                lastActionResult != null -> lastActionResult!!.responseText
                                state == AssistantState.LISTENING -> "Listening to your voice..."
                                state == AssistantState.THINKING -> "Thinking and executing your command..."
                                else -> "It's currently 68°F and sunny in San Francisco with clear skies and a gentle westerly breeze."
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                        .background(StitchSurfaceLow.copy(alpha = 0.85f))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(StitchCyanContainer.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = StitchCyanContainer,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = assistantResponseText,
                                            color = StitchOnSurface,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Multimodal Smart Action Panel
                    if (lastActionResult != null) {
                        item {
                            AssistantActiveResultCard(
                                actionResult = lastActionResult!!,
                                onFollowUp = onExecuteQuery,
                                onSpeak = { voiceManager.speak(it) },
                                onDismiss = { voiceManager.setLastActionResult(null) }
                            )
                        }
                    } else if (showShowcaseWeather) {
                        item {
                            StitchMultimodalSmartCard(
                                onDismiss = { showShowcaseWeather = false },
                                onForecastClick = { onExecuteQuery("show 5-day forecast") }
                            )
                        }
                    }

                    // Horizontal Scrolling Contextual Assistant Skills Chips
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val stitchChips = listOf(
                                Triple("Turn on Torch", Icons.Default.FlashlightOn, "turn on flashlight"),
                                Triple("Set Timer 15m", Icons.Default.Timer, "set timer for 15 minutes"),
                                Triple("Send Message", Icons.Default.Chat, "send message"),
                                Triple("Play Focus Beats", Icons.Default.Headphones, "play music"),
                                Triple("Summarize Notes", Icons.Default.Psychology, "summarize notes"),
                                Triple("Battery Status", Icons.Default.Refresh, "check battery")
                            )
                            items(stitchChips) { (title, icon, query) ->
                                Box(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(19.dp))
                                        .background(StitchSurfaceHigh.copy(alpha = 0.8f))
                                        .clickable { onExecuteQuery(query) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = StitchCyanContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = title,
                                            color = StitchOnSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Grounded Kinetic Voice Capsule Deck
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(StitchSurface.copy(alpha = 0.95f))
                                .border(1.dp, Color(0xFF283044), RoundedCornerShape(28.dp))
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Multimodal Lens Trigger
                            IconButton(
                                onClick = {
                                    val currentAct = ActivityTracker.getCurrentActivity() ?: (context as? android.app.Activity)
                                    deviceController.captureScreenshot(currentAct) { success, msg, _ ->
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            Toast.makeText(context, if (success) "Multimodal Visual Snapshot Captured!" else msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraEnhance,
                                    contentDescription = "Open Multimodal Lens",
                                    tint = StitchOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Transcribing Input
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = {
                                    Text(
                                        "Ask Echo anything or tap to speak...",
                                        color = StitchOnSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("main_text_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = StitchOnSurface,
                                    unfocusedTextColor = StitchOnSurface
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

                            // Hero Glow Push-To-Talk Mic Pod
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(StitchCyanContainer, StitchVioletContainer)
                                        )
                                    )
                                    .clickable {
                                        if (textInput.isNotBlank()) {
                                            onExecuteQuery(textInput)
                                            textInput = ""
                                            keyboardController?.hide()
                                        } else {
                                            if (state == AssistantState.LISTENING) {
                                                voiceManager.stopListening()
                                            } else {
                                                if (hasMicPermission) voiceManager.startListening() else onRequestMicPermission()
                                            }
                                        }
                                    }
                                    .testTag("mic_action_trigger"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (textInput.isNotBlank()) Icons.Default.Send else if (state == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Action",
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Version Upgrade Comparison Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1B1936), Color(0xFF11172A))
                                    )
                                )
                                .border(1.dp, VividViolet.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable { showComparisonSheet = true }
                                .padding(14.dp)
                                .testTag("open_version_comparison_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(VividViolet.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("Previous vs Pro Version", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                        Text("Compare 28+ Siri & Google Assistant features", color = NeonCyan, fontSize = 11.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VividViolet.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("Compare", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    item {
                        QuickAppLauncherGrid(
                            onLaunchApp = { appName ->
                                deviceController.openApp(appName)
                            }
                        )
                    }
                }

                DashboardTab.CONTROLS -> {
                    item {
                        Column {
                            Text(
                                text = "Device Controls Station",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Hardware switches & direct mobile shortcuts",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
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
                        ContactCallCard(
                            deviceController = deviceController,
                            onCallRequest = { target ->
                                onExecuteQuery(if (target.startsWith("call ", ignoreCase = true) || target.startsWith("dial ", ignoreCase = true)) target else "call $target")
                            },
                            onRequestContactPermission = onRequestContactPermission
                        )
                    }

                    item {
                        WhatsAppMessageCard(
                            deviceController = deviceController,
                            onRequestContactPermission = onRequestContactPermission,
                            onSendWhatsApp = { target, message ->
                                val result = deviceController.sendWhatsAppMessage(target, message)
                                voiceManager.speak(result.second)
                                Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        ScreenshotControlCard(
                            deviceController = deviceController,
                            onTakeScreenshot = {
                                val currentAct = ActivityTracker.getCurrentActivity() ?: (context as? android.app.Activity)
                                val result = deviceController.captureScreenshot(currentAct) { success, msg, uri ->
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        if (success) {
                                            Toast.makeText(context, "Screenshot captured and saved to Gallery!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                voiceManager.speak(result.second)
                                Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                            },
                            onShareScreenshot = {
                                val result = deviceController.shareLastScreenshot()
                                voiceManager.speak(result.second)
                                Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                DashboardTab.HISTORY -> {
                    item {
                        Column {
                            Text(
                                text = "Echo Memory & Notes",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Voice query history and voice-recorded notes",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
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
                }

                DashboardTab.SETUP -> {
                    item {
                        Column {
                            Text(
                                text = "System Integration & Setup",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Floating Orb overlay & Redmi background service",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
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
                        Text("Vibrate on start & command", color = TextSecondary, fontSize = 12.sp)
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

                // Contacts Permission Setting
                val hasContactsPerm = ContactLookupHelper.hasContactsPermission(context)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Contacts & Calling Access", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (hasContactsPerm) "Connected • Can find contacts by spoken name" else "Permission required to look up names before calling",
                            color = if (hasContactsPerm) EmeraldGlow else SolarAmber,
                            fontSize = 12.sp
                        )
                    }
                    if (!hasContactsPerm) {
                        Button(
                            onClick = onRequestContactPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldGlow.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Granted", color = EmeraldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

    if (showComparisonSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        VersionComparisonSheet(
            sheetState = sheetState,
            onDismiss = { showComparisonSheet = false }
        )
    }
}
