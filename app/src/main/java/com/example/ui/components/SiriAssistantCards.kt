package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActionResult
import com.example.engine.DailyBriefingData
import com.example.engine.TranslationData
import com.example.engine.WeatherData
import com.example.ui.theme.DarkNebulaSurface
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantMagenta
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

@Composable
fun AssistantActiveResultCard(
    actionResult: ActionResult,
    onFollowUp: (String) -> Unit,
    onSpeak: (String) -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (actionResult) {
            is ActionResult.WeatherAction -> {
                WeatherResultCard(weather = actionResult.weather, onFollowUp = onFollowUp)
            }

            is ActionResult.DailyBriefingAction -> {
                DailyBriefingCard(briefing = actionResult.briefing, onFollowUp = onFollowUp)
            }

            is ActionResult.TranslateAction -> {
                TranslationResultCard(
                    translation = actionResult.translation,
                    onSpeak = { onSpeak(actionResult.translation.translatedText) },
                    onCopy = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("Translation", actionResult.translation.translatedText))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            is ActionResult.CalendarEventAction -> {
                CalendarEventCard(
                    title = actionResult.eventTitle,
                    timeFormatted = actionResult.timeFormatted,
                    onFollowUp = onFollowUp
                )
            }

            is ActionResult.RingerModeAction -> {
                RingerModeCard(
                    currentMode = actionResult.mode,
                    onSelectMode = { mode -> onFollowUp("Set ringer to $mode") }
                )
            }

            is ActionResult.CoinFlipAction -> {
                CoinFlipResultCard(isHeads = actionResult.isHeads, onFlipAgain = { onFollowUp("Flip a coin") })
            }

            is ActionResult.DiceRollAction -> {
                DiceRollResultCard(roll = actionResult.roll, onRollAgain = { onFollowUp("Roll a dice") })
            }

            is ActionResult.FunAction -> {
                FunResultCard(
                    category = actionResult.category,
                    content = actionResult.content,
                    onAnother = { onFollowUp(if (actionResult.category == "Joke") "Tell me another joke" else "Tell me another fun fact") }
                )
            }

            is ActionResult.UnitConversionAction -> {
                UnitConversionCard(
                    from = actionResult.from,
                    to = actionResult.to,
                    result = actionResult.resultVal,
                    onCopy = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("Conversion", actionResult.resultVal))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            is ActionResult.CalculationAction -> {
                CalculationResultCard(
                    expression = actionResult.expression,
                    result = actionResult.resultValue,
                    onOpenCalculator = { onFollowUp("Open calculator") }
                )
            }

            is ActionResult.ScreenshotAction -> {
                ScreenshotResultCard(isCaptured = actionResult.isCaptured, onShare = { onFollowUp("Share screenshot") })
            }

            is ActionResult.WhatsAppAction -> {
                WhatsAppSentCard(target = actionResult.target, message = actionResult.message)
            }

            is ActionResult.CallAction -> {
                CallInitiatedCard(target = actionResult.target)
            }

            else -> {
                GenericAssistantCard(text = actionResult.responseText, onFollowUp = onFollowUp)
            }
        }

        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
}

// 1. WEATHER RESULT CARD (Google Assistant / Siri Style)
@Composable
fun WeatherResultCard(weather: WeatherData, onFollowUp: (String) -> Unit) {
    GlassmorphicCard(
        borderColor = NeonCyan.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF0F1B2E)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = weather.conditionIcon, fontSize = 28.sp)
                    Column {
                        Text(text = weather.location, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text(text = weather.condition, color = NeonCyan, fontSize = 13.sp)
                    }
                }
                Text(
                    text = "${weather.temperatureC}°C",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weather details pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherBadge("High / Low", "${weather.highC}° / ${weather.lowC}°")
                WeatherBadge("Humidity", "${weather.humidity}%")
                WeatherBadge("Wind", "${weather.windSpeedKmh} km/h")
                WeatherBadge("Fahrenheit", "${weather.temperatureF}°F")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = weather.summary,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onFollowUp("Will it rain tomorrow?") },
                    label = { Text("Rain forecast?", fontSize = 11.sp, color = TextPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E2A44))
                )
                AssistChip(
                    onClick = { onFollowUp("Weather in Paris") },
                    label = { Text("Weather in Paris", fontSize = 11.sp, color = TextPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E2A44))
                )
            }
        }
    }
}

@Composable
fun WeatherBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16233B))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = TextMuted)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

// 2. DAILY BRIEFING CARD
@Composable
fun DailyBriefingCard(briefing: DailyBriefingData, onFollowUp: (String) -> Unit) {
    GlassmorphicCard(
        borderColor = SolarAmber.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF1C1A14)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SolarAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = briefing.greeting, fontWeight = FontWeight.ExtraBold, color = SolarAmber, fontSize = 16.sp)
                        Text(text = "${briefing.dateString} • ${briefing.timeString}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF26221A))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Weather", fontSize = 10.sp, color = TextMuted)
                        Text(briefing.weatherSummary, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF26221A))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Battery", fontSize = 10.sp, color = TextMuted)
                        Text(briefing.batterySummary, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF26221A))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Notes & Memos", fontSize = 10.sp, color = TextMuted)
                        Text("${briefing.notesCount} saved", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
            }

            // Motivational thought
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2519))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "\"${briefing.motivationalQuote}\"",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 3. TRANSLATION RESULT CARD
@Composable
fun TranslationResultCard(
    translation: TranslationData,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    GlassmorphicCard(
        borderColor = VividViolet.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF181329)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = VividViolet, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Echo Interpreter • ${translation.targetLanguage}",
                        fontWeight = FontWeight.Bold,
                        color = VividViolet,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronounce", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Original phrase
            Text(
                text = translation.originalText,
                color = TextSecondary,
                fontSize = 13.sp
            )

            // Translated phrase
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF261D40))
                    .padding(12.dp)
            ) {
                Text(
                    text = translation.translatedText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// 4. CALENDAR SCHEDULING CARD
@Composable
fun CalendarEventCard(title: String, timeFormatted: String, onFollowUp: (String) -> Unit) {
    GlassmorphicCard(
        borderColor = EmeraldGlow.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF0F241E)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                Text("Event Scheduled", fontWeight = FontWeight.Bold, color = EmeraldGlow, fontSize = 14.sp)
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )

            Text(
                text = "Added to your Android Calendar with reminders.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = { onFollowUp("Open calendar") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open in Calendar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// 5. RINGER MODE CARD
@Composable
fun RingerModeCard(currentMode: String, onSelectMode: (String) -> Unit) {
    GlassmorphicCard(
        borderColor = NeonCyan.copy(alpha = 0.4f),
        backgroundColor = DarkNebulaSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Text("Phone Sound & Ringer Mode", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RingerModeChip("Silent", Icons.Default.NotificationsOff, currentMode == "SILENT") { onSelectMode("silent") }
                RingerModeChip("Vibrate", Icons.Default.Vibration, currentMode == "VIBRATE") { onSelectMode("vibrate") }
                RingerModeChip("Normal", Icons.Default.VolumeUp, currentMode == "NORMAL") { onSelectMode("normal") }
            }
        }
    }
}

@Composable
fun RingerModeChip(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0xFF1B2438))
            .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = if (isSelected) NeonCyan else TextSecondary, modifier = Modifier.size(14.dp))
            Text(title, color = if (isSelected) NeonCyan else TextPrimary, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// 6. COIN FLIP RESULT CARD
@Composable
fun CoinFlipResultCard(isHeads: Boolean, onFlipAgain: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart)
    )

    GlassmorphicCard(
        borderColor = SolarAmber.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF1F1A10)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHeads) "H" else "T",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = Color.Black
                )
            }

            Text(
                text = if (isHeads) "It's HEADS!" else "It's TAILS!",
                fontWeight = FontWeight.ExtraBold,
                color = SolarAmber,
                fontSize = 20.sp
            )

            Button(
                onClick = onFlipAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332914)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = SolarAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip Again", color = SolarAmber, fontSize = 12.sp)
            }
        }
    }
}

// 7. DICE ROLL RESULT CARD
@Composable
fun DiceRollResultCard(roll: Int, onRollAgain: () -> Unit) {
    GlassmorphicCard(
        borderColor = RadiantMagenta.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF1F1226)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RadiantMagenta)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$roll",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    color = Color.White
                )
            }

            Text(
                text = "Rolled a $roll",
                fontWeight = FontWeight.Bold,
                color = RadiantMagenta,
                fontSize = 18.sp
            )

            Button(
                onClick = onRollAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381B45)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = RadiantMagenta, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Roll Again", color = RadiantMagenta, fontSize = 12.sp)
            }
        }
    }
}

// 8. FUN & JOKES CARD
@Composable
fun FunResultCard(category: String, content: String, onAnother: () -> Unit) {
    GlassmorphicCard(
        borderColor = VividViolet.copy(alpha = 0.5f),
        backgroundColor = DarkNebulaSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VividViolet, modifier = Modifier.size(18.dp))
                    Text(category, fontWeight = FontWeight.Bold, color = VividViolet, fontSize = 13.sp)
                }

                Button(
                    onClick = onAnother,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D40)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Another", fontSize = 11.sp, color = NeonCyan)
                }
            }

            Text(
                text = content,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// 9. UNIT CONVERSION CARD
@Composable
fun UnitConversionCard(from: String, to: String, result: String, onCopy: () -> Unit) {
    GlassmorphicCard(
        borderColor = NeonCyan.copy(alpha = 0.4f),
        backgroundColor = DarkNebulaSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unit Converter", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 13.sp)
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Text(from, color = TextSecondary, fontSize = 13.sp)
            Text("=", color = TextMuted, fontSize = 16.sp)
            Text(result, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

// 10. CALCULATION RESULT CARD
@Composable
fun CalculationResultCard(expression: String, result: String, onOpenCalculator: () -> Unit) {
    GlassmorphicCard(
        borderColor = NeonCyan.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF0F1B2E)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(expression, color = TextSecondary, fontSize = 14.sp)
                Button(
                    onClick = onOpenCalculator,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A44)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Calculator", fontSize = 11.sp, color = NeonCyan)
                }
            }
            Text("= $result", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        }
    }
}

// 11. SCREENSHOT RESULT CARD
@Composable
fun ScreenshotResultCard(isCaptured: Boolean, onShare: () -> Unit) {
    GlassmorphicCard(
        borderColor = EmeraldGlow.copy(alpha = 0.5f),
        backgroundColor = DarkNebulaSurface
    ) {
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
                        .background(EmeraldGlow.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Screenshot Captured", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Saved to Pictures/Screenshots", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

// 12. WHATSAPP SENT CARD
@Composable
fun WhatsAppSentCard(target: String, message: String) {
    GlassmorphicCard(
        borderColor = Color(0xFF25D366).copy(alpha = 0.5f),
        backgroundColor = Color(0xFF0D2418)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                Text("WhatsApp Ready • $target", fontWeight = FontWeight.Bold, color = Color(0xFF25D366), fontSize = 13.sp)
            }
            Text("\"$message\"", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// 13. CALL INITIATED CARD
@Composable
fun CallInitiatedCard(target: String) {
    GlassmorphicCard(
        borderColor = EmeraldGlow.copy(alpha = 0.5f),
        backgroundColor = Color(0xFF0F241A)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
            Column {
                Text("Connecting Call", fontWeight = FontWeight.Bold, color = EmeraldGlow, fontSize = 14.sp)
                Text(target, color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

// 14. GENERIC ASSISTANT RESPONSE CARD
@Composable
fun GenericAssistantCard(text: String, onFollowUp: (String) -> Unit) {
    GlassmorphicCard(
        borderColor = VividViolet.copy(alpha = 0.4f),
        backgroundColor = DarkNebulaSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Text("Echo Assistant", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)
            }
            Text(text = text, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
