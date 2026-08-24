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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNebulaSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantMagenta
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

@Composable
fun RedmiSetupGuide(
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean = false,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onOpenDefaultAssistantSettings: () -> Unit,
    onOpenGestureSettings: () -> Unit,
    onTestAssistantOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("redmi_setup_guide_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card for Redmi Note 12
        GlassmorphicCard(
            borderColor = NeonCyan.copy(alpha = 0.5f),
            backgroundColor = DarkNebulaSurface.copy(alpha = 0.95f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, VividViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Redmi",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Redmi Note 12 Assistant Setup",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Long-Press Power Button & Default App Integration",
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Configure your device to trigger Echo's Siri-style animated circle whenever you hold down the power button or trigger the assistant gesture.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTestAssistantOverlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_siri_overlay_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VividViolet
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Test Overlay",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Power Button Assistant Circle",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Step 1: Default Assistant App
        SetupStepItem(
            stepNumber = "1",
            title = "Set Echo as Default Assistant",
            subtitle = "Settings > Apps > Manage Apps / Default apps > Digital Assistant > Echo",
            buttonLabel = "Open Assistant Settings",
            icon = Icons.Default.SettingsSuggest,
            buttonColor = NeonCyan,
            onClick = onOpenDefaultAssistantSettings,
            isCompleted = false
        )

        // Step 2: Redmi / MIUI Gesture Shortcuts
        SetupStepItem(
            stepNumber = "2",
            title = "Redmi Power Button Shortcut",
            subtitle = "Settings > Additional Settings > Gesture Shortcuts > 'Press and hold the Power button for 0.5s' > Enable Assistant",
            buttonLabel = "Open Gesture Settings",
            icon = Icons.Default.PowerSettingsNew,
            buttonColor = SolarAmber,
            onClick = onOpenGestureSettings,
            isCompleted = false
        )

        // Step 3: Permissions
        SetupStepItem(
            stepNumber = "3",
            title = "Microphone & Voice Input",
            subtitle = if (hasMicPermission) "Microphone permission is active for voice listening." else "Required for Echo to hear your voice commands and display wave ripples.",
            buttonLabel = if (hasMicPermission) "Permission Granted" else "Grant Microphone Access",
            icon = Icons.Default.Security,
            buttonColor = if (hasMicPermission) EmeraldGlow else RadiantMagenta,
            onClick = onRequestMicPermission,
            isCompleted = hasMicPermission
        )

        // Step 4: System Floating Orb Overlay
        SetupStepItem(
            stepNumber = "4",
            title = "Display Over Other Apps (Floating Orb)",
            subtitle = if (hasOverlayPermission) "Overlay permission granted! The floating Siri orb can appear anywhere over any app." else "Allow Echo to display a draggable floating orb on top of games, browsers, and other apps.",
            buttonLabel = if (hasOverlayPermission) "Overlay Access Granted" else "Grant Display Over Apps",
            icon = Icons.Default.PhoneAndroid,
            buttonColor = if (hasOverlayPermission) EmeraldGlow else VividViolet,
            onClick = onRequestOverlayPermission,
            isCompleted = hasOverlayPermission
        )

        // Step 5: Summon Anywhere (Notification & Quick Tile)
        GlassmorphicCard(
            borderColor = ElectricBlue.copy(alpha = 0.4f),
            backgroundColor = DarkNebulaSurface.copy(alpha = 0.8f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .border(1.dp, ElectricBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "5",
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "More Ways to Summon Echo Anywhere",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Bottom Center Floating Echo Orb: A glowing, enlarged Siri orb anchored at the bottom center over other apps. Tap to instantly talk.\n• Quick Settings Tile: Swipe down from the top of your screen, edit tiles, and add 'Echo Assistant' for 1-tap popup anywhere.\n• Power Button: Hold power for 0.5s to trigger the Siri circle overlay instantly.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SetupStepItem(
    stepNumber: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonColor: Color,
    onClick: () -> Unit,
    isCompleted: Boolean
) {
    GlassmorphicCard(
        borderColor = if (isCompleted) EmeraldGlow.copy(alpha = 0.4f) else buttonColor.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(buttonColor.copy(alpha = 0.2f))
                            .border(1.dp, buttonColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            fontWeight = FontWeight.Bold,
                            color = buttonColor,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = buttonColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, buttonColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
