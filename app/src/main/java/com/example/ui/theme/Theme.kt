package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EchoDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF70F7D7),
    secondary = VividViolet,
    onSecondary = Color(0xFF38006B),
    secondaryContainer = Color(0xFF5A189A),
    onSecondaryContainer = Color(0xFFE0BBFF),
    tertiary = RadiantMagenta,
    onTertiary = Color(0xFF490023),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = DarkNebulaSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF161F36),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF334155),
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EchoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
