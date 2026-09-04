package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNebulaSurface
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadiantMagenta
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

data class FeatureComparison(
    val featureName: String,
    val category: String,
    val previousVersion: String,
    val hasPrevious: Boolean,
    val currentVersion: String,
    val hasCurrent: Boolean = true,
    val highlightBadge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionComparisonSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val comparisons = listOf(
        FeatureComparison(
            featureName = "Live Weather & Forecasts",
            category = "Assistant Intelligence",
            previousVersion = "Not supported (plain search fallback)",
            hasPrevious = false,
            currentVersion = "Real-time weather data with temp, high/low, humidity, wind & Siri sky card",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Daily Briefing ('Good Morning')",
            category = "Assistant Intelligence",
            previousVersion = "Generic conversational reply",
            hasPrevious = false,
            currentVersion = "Synchronized briefing with weather, battery status, notes count & daily quote",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Language Interpreter & Translation",
            category = "Language & Global",
            previousVersion = "Text-only reply without audio or clipboard actions",
            hasPrevious = false,
            currentVersion = "Instant multi-lingual translation cards with pronunciation & 1-tap copy",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Calendar Scheduling",
            category = "Productivity",
            previousVersion = "Manual calendar app opening",
            hasPrevious = false,
            currentVersion = "Natural voice scheduling with calendar provider intent & reminder prep",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Sound & Ringer Mode",
            category = "Hardware Controls",
            previousVersion = "Media volume slider only",
            hasPrevious = false,
            currentVersion = "Silent, Vibrate, Normal & DND modes with visual switcher chips",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Quick Utilities (Coin & Dice)",
            category = "Decision & Fun",
            previousVersion = "Not supported",
            hasPrevious = false,
            currentVersion = "Animated gold coin flip & 3D dice rolling cards with instant re-roll",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Unit & Currency Conversions",
            category = "Calculations & Utilities",
            previousVersion = "Basic 4-function arithmetic",
            hasPrevious = false,
            currentVersion = "Miles/km, °C/°F, kg/lbs, USD/EUR/INR live conversion cards",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Hardware Toggles & Hotspot",
            category = "System Controls",
            previousVersion = "Flashlight, Volume, Settings screen",
            hasPrevious = true,
            currentVersion = "Hotspot, Airplane mode, Auto-rotate, Bluetooth & Wi-Fi deep links",
            highlightBadge = "EXPANDED"
        ),
        FeatureComparison(
            featureName = "System Screenshot Capture",
            category = "System Controls",
            previousVersion = "Failed when assistant in background",
            hasPrevious = false,
            currentVersion = "Global Accessibility screenshot capture with 1-tap sharing card",
            highlightBadge = "FIXED & POLISHED"
        ),
        FeatureComparison(
            featureName = "WhatsApp Direct Messaging",
            category = "Messaging & Comms",
            previousVersion = "Hit-or-miss contact lookup",
            hasPrevious = false,
            currentVersion = "Direct deep-linking with pre-filled message & phone number resolver",
            highlightBadge = "FIXED & POLISHED"
        ),
        FeatureComparison(
            featureName = "Jokes & Science Trivia",
            category = "Chit-chat & Fun",
            previousVersion = "Generic web response",
            hasPrevious = false,
            currentVersion = "Dynamic punchline cards with 'Tell me another' quick action",
            highlightBadge = "NEW"
        ),
        FeatureComparison(
            featureName = "Siri Dynamic Visual Cards",
            category = "UI / UX Polish",
            previousVersion = "Plain text chat bubble",
            hasPrevious = false,
            currentVersion = "14 specialized Material 3 glassmorphic response cards with follow-up chips",
            highlightBadge = "PRO LEVEL"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VividViolet.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Previous Version vs Pro Version",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Siri & Google Assistant Level Capabilities",
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // Summary banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1F36), Color(0xFF151829))
                        )
                    )
                    .border(1.dp, VividViolet.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PREVIOUS", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("12 Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text("Basic rule engine", fontSize = 10.sp, color = TextMuted)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFF2E3856))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEW PRO VERSION", fontSize = 10.sp, color = SolarAmber, fontWeight = FontWeight.Bold)
                        Text("28+ Actions", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                        Text("Siri & Google Level", fontSize = 10.sp, color = EmeraldGlow)
                    }
                }
            }

            // Feature list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(comparisons) { item ->
                    ComparisonCard(item)
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(item: FeatureComparison) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNebulaSurface)
            .border(1.dp, Color(0xFF222D42), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.featureName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                if (item.highlightBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (item.highlightBadge) {
                                    "NEW" -> EmeraldGlow.copy(alpha = 0.2f)
                                    "PRO LEVEL" -> VividViolet.copy(alpha = 0.25f)
                                    else -> NeonCyan.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.highlightBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (item.highlightBadge) {
                                "NEW" -> EmeraldGlow
                                "PRO LEVEL" -> RadiantMagenta
                                else -> NeonCyan
                            }
                        )
                    }
                }
            }

            // Comparison side-by-side or stacked
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Previous
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E2633)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(10.dp))
                    }
                    Text(
                        text = "Previous: ${item.previousVersion}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }

                // Current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(EmeraldGlow.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(10.dp))
                    }
                    Text(
                        text = "Now: ${item.currentVersion}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
