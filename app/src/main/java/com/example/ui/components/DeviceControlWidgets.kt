package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BatteryInfo
import com.example.engine.ContactLookupHelper
import com.example.engine.ContactMatch
import com.example.engine.DeviceController
import com.example.engine.VolumeInfo
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
fun FlashlightControlCard(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (isOn) SolarAmber else DarkNebulaSurface,
        label = "flash_bg"
    )

    GlassmorphicCard(
        modifier = modifier.testTag("flashlight_control_card"),
        borderColor = if (isOn) SolarAmber else NeonCyan.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOn) Brush.radialGradient(listOf(SolarAmber, Color(0xFFFF8800)))
                            else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight Status",
                        tint = if (isOn) Color.Black else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Device Flashlight",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isOn) "Torch Active (Say 'Torch Off')" else "Torch Disabled",
                        color = if (isOn) SolarAmber else TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("flashlight_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = SolarAmber,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
fun BatteryStatusCard(
    batteryInfo: BatteryInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        batteryInfo.level > 50 -> EmeraldGlow
        batteryInfo.level > 20 -> SolarAmber
        else -> RadiantMagenta
    }

    GlassmorphicCard(
        modifier = modifier.testTag("battery_status_card"),
        borderColor = batteryColor.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = batteryColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Battery & Health",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "${batteryInfo.level}%",
                    fontWeight = FontWeight.ExtraBold,
                    color = batteryColor,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { batteryInfo.level / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = batteryColor,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (batteryInfo.isCharging) "Charging (${batteryInfo.chargeType})" else "Discharging",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Temp: ${batteryInfo.temperatureCelsius}°C",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun VolumeControlCard(
    volumeInfo: VolumeInfo,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSliderValue by remember(volumeInfo.mediaPercent) {
        mutableFloatStateOf(volumeInfo.mediaPercent.toFloat())
    }

    GlassmorphicCard(
        modifier = modifier.testTag("volume_control_card"),
        borderColor = ElectricBlue.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (volumeInfo.mediaPercent == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Media Volume",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "${currentSliderValue.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue,
                    fontSize = 15.sp
                )
            }

            Slider(
                value = currentSliderValue,
                onValueChange = { currentSliderValue = it },
                onValueChangeFinished = { onVolumeChange(currentSliderValue.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("media_volume_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = ElectricBlue,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        onMuteToggle(volumeInfo.mediaPercent > 0)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (volumeInfo.mediaPercent == 0) VividViolet else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (volumeInfo.mediaPercent == 0) "Unmute" else "Mute",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onVolumeChange(25) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("25%", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { onVolumeChange(75) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("75%", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { onVolumeChange(100) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("100%", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAppLauncherGrid(
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickApps = listOf(
        Triple("YouTube", Icons.Default.PlayArrow, RadiantMagenta),
        Triple("WhatsApp", Icons.Default.MusicNote, EmeraldGlow),
        Triple("Camera", Icons.Default.CameraAlt, SolarAmber),
        Triple("Maps", Icons.Default.Map, ElectricBlue),
        Triple("Calculator", Icons.Default.Calculate, VividViolet),
        Triple("Settings", Icons.Default.Settings, NeonCyan)
    )

    GlassmorphicCard(
        modifier = modifier.testTag("app_launcher_grid"),
        borderColor = VividViolet.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Voice App Control",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp
            )
            Text(
                text = "Tap or say 'Open [App Name]'",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickApps.take(3).forEach { (name, icon, color) ->
                    AppLauncherItem(
                        name = name,
                        icon = icon,
                        color = color,
                        onClick = { onLaunchApp(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickApps.drop(3).forEach { (name, icon, color) ->
                    AppLauncherItem(
                        name = name,
                        icon = icon,
                        color = color,
                        onClick = { onLaunchApp(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLauncherItem(
    name: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131B2E))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
fun ContactCallCard(
    deviceController: DeviceController,
    onCallRequest: (String) -> Unit,
    onRequestContactPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    var matchingContacts by remember { mutableStateOf<List<ContactMatch>>(emptyList()) }
    val hasPermission = ContactLookupHelper.hasContactsPermission(context)

    LaunchedEffect(queryText, hasPermission) {
        if (hasPermission && queryText.isNotBlank()) {
            matchingContacts = deviceController.searchContacts(queryText)
        } else {
            matchingContacts = emptyList()
        }
    }

    GlassmorphicCard(
        modifier = modifier.testTag("contact_call_card"),
        borderColor = EmeraldGlow.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(Brush.radialGradient(listOf(EmeraldGlow, Color(0xFF059669)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Smart Call",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Contact Call",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Searches matching contact name & dials",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contacts permission needed",
                            color = SolarAmber,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enable to look up contacts by spoken names",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onRequestContactPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("grant_contacts_button")
                    ) {
                        Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                // Interactive Search Input
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            "Type name to search (e.g. Mom, John)...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Contacts",
                            tint = EmeraldGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (queryText.isNotBlank()) {
                            IconButton(
                                onClick = { onCallRequest(queryText) },
                                modifier = Modifier.testTag("direct_call_submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Contact",
                                    tint = EmeraldGlow
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = EmeraldGlow
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_search_field")
                )

                if (matchingContacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Found ${matchingContacts.size} matching contact(s):",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        matchingContacts.take(3).forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF131B2E))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGlow.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.firstOrNull()?.uppercase() ?: "C",
                                            color = EmeraldGlow,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.5.sp
                                        )
                                        Text(
                                            text = "${contact.typeLabel} • ${contact.number}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onCallRequest(contact.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("call_contact_${contact.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${contact.name}",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sample voice call chips
            Text(
                text = "Voice Commands (Tap or Say):",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Call Mom", "Call John", "Call Doctor", "Dial 100").forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { onCallRequest(cmd) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cmd,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppMessageCard(
    deviceController: DeviceController,
    onSendWhatsApp: (target: String, message: String) -> Unit,
    modifier: Modifier = Modifier,
    onRequestContactPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var targetText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var matchingContacts by remember { mutableStateOf<List<ContactMatch>>(emptyList()) }
    val hasContactPermission = ContactLookupHelper.hasContactsPermission(context)

    val quickMessages = listOf(
        "I'll be there in 10 mins",
        "Running late, see you soon!",
        "Please call me back",
        "Reached safely!"
    )

    LaunchedEffect(targetText, hasContactPermission) {
        if (hasContactPermission && targetText.isNotBlank() && !ContactLookupHelper.isDirectPhoneNumber(targetText)) {
            matchingContacts = deviceController.searchContacts(targetText)
        } else {
            matchingContacts = emptyList()
        }
    }

    GlassmorphicCard(
        modifier = modifier.testTag("whatsapp_message_card"),
        borderColor = EmeraldGlow.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(Brush.radialGradient(listOf(Color(0xFF25D366), Color(0xFF128C7E)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WhatsApp Messaging",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Voice message or direct chat compose",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = { deviceController.openWhatsApp() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open App",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", color = EmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!hasContactPermission && onRequestContactPermission != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldGlow.copy(alpha = 0.12f))
                        .clickable { onRequestContactPermission() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable contact matching for names (e.g. Mom)",
                        color = EmeraldGlow,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Grant",
                        color = Color.Black,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldGlow)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact / Target input
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                placeholder = {
                    Text(
                        "Contact name or phone number (e.g. Mom, +1...)",
                        color = TextMuted,
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Recipient",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGlow,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldGlow
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_target_input")
            )

            // Contact match dropdown suggestions
            if (matchingContacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingContacts.take(2).forEach { contact ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF131B2E))
                                .clickable {
                                    targetText = contact.name
                                    matchingContacts = emptyList()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "👤 ${contact.name}",
                                color = EmeraldGlow,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message text input
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = {
                    Text(
                        "Type your WhatsApp message...",
                        color = TextMuted,
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Message",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGlow,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldGlow
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_message_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset message chips
            Text(
                text = "Quick Presets:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickMessages.take(2).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { messageText = preset }
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickMessages.drop(2).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { messageText = preset }
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Send Button
            Button(
                onClick = {
                    onSendWhatsApp(targetText, messageText)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("send_whatsapp_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send WhatsApp",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (targetText.isNotBlank()) "Send WhatsApp to $targetText" else "Send via WhatsApp",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voice command examples
            Text(
                text = "Voice Commands:",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("WhatsApp Mom: I'm late", "Send WhatsApp to Rahul", "Open WhatsApp").forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                onSendWhatsApp(
                                    cmd.substringAfter("WhatsApp ").substringBefore(":").trim(),
                                    cmd.substringAfter(":", "").trim()
                                )
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cmd,
                            color = EmeraldGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotControlCard(
    deviceController: DeviceController,
    onTakeScreenshot: () -> Unit,
    onShareScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recentScreenshots = remember { mutableStateOf(com.example.engine.ScreenshotHelper.getRecentScreenshots(context)) }

    fun refreshList() {
        recentScreenshots.value = com.example.engine.ScreenshotHelper.getRecentScreenshots(context)
    }

    GlassmorphicCard(
        modifier = modifier.testTag("screenshot_control_card"),
        borderColor = NeonCyan.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(Brush.radialGradient(listOf(NeonCyan, Color(0xFF0284C7)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Screenshot",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Screen Capture & Snapshots",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Say 'Take screenshot' or tap capture",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Capture Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onTakeScreenshot()
                        coroutineScope.launch {
                            delay(400)
                            refreshList()
                            delay(1000)
                            refreshList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("take_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Capture Screen",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Capture Screen",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                Button(
                    onClick = {
                        onShareScreenshot()
                        coroutineScope.launch {
                            delay(500)
                            refreshList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("share_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // System-wide accessibility screenshot prompt
            val isAccessibilityActive = com.example.service.EchoAccessibilityService.isServiceRunning
            if (!isAccessibilityActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                        .clickable {
                            com.example.service.EchoAccessibilityService.openAccessibilitySettings(context)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = "Accessibility",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Global Screenshot (Capture any app)",
                            color = NeonCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Enable",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shortcut Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF131B2E))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Hardware Shortcut",
                        tint = SolarAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice: \"Take a screenshot\" | Hardware: Power + Vol Down",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Recent Screenshots Preview
            if (recentScreenshots.value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Screenshots (${recentScreenshots.value.size}):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Saved in Gallery",
                        color = EmeraldGlow,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentScreenshots.value.take(2).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF131B2E))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ElectricBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Screenshot Image",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.sizeBytes / 1024} KB",
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { com.example.engine.ScreenshotHelper.viewScreenshot(context, item.uri) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("view_screenshot_${item.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "View",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { com.example.engine.ScreenshotHelper.shareScreenshot(context, item.uri) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("share_screenshot_${item.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = EmeraldGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sample voice screenshot chips
            Text(
                text = "Voice Commands:",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Take a screenshot", "Capture screen", "Share screenshot").forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                if (cmd.contains("Share")) onShareScreenshot() else onTakeScreenshot()
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cmd,
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
