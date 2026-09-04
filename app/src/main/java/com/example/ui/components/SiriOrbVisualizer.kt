package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.OrbTheme
import com.example.voice.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SiriOrbVisualizer(
    state: AssistantState,
    audioLevel: Float, // 0.0 to 1.5+
    orbTheme: OrbTheme = OrbTheme.SIRI_RAINBOW,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 190.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

    // Slow rotation for the luminous gradient rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for idle state
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Dynamic wave phase for Siri ribbons
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Smooth responsive scale based on state and audio decibel
    val targetScale = when (state) {
        AssistantState.LISTENING -> (1.1f + audioLevel * 0.25f).coerceIn(1.05f, 1.45f)
        AssistantState.SPEAKING -> (1.08f + audioLevel * 0.18f).coerceIn(1.04f, 1.35f)
        AssistantState.THINKING -> 1.05f
        AssistantState.IDLE -> breathingPulse
        AssistantState.ERROR -> 0.96f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "scale"
    )

    val primaryColor = Color(orbTheme.primaryColorHex)
    val secondaryColor = Color(orbTheme.secondaryColorHex)
    val tertiaryColor = Color(orbTheme.tertiaryColorHex)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp)
            .testTag("siri_orb_circle")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = sizeDp / 2),
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.72f * animatedScale

            // 1. Outermost Ambient Halo Glow
            val glowRadius = baseRadius * 1.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (state == AssistantState.LISTENING) 0.45f else 0.22f),
                        secondaryColor.copy(alpha = if (state == AssistantState.SPEAKING) 0.35f else 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Rotating Iridescent Plasma Ring
            val ringAngleRad = Math.toRadians(rotationAngle.toDouble()).toFloat()
            val gradientStart = Offset(
                center.x + baseRadius * cos(ringAngleRad),
                center.y + baseRadius * sin(ringAngleRad)
            )
            val gradientEnd = Offset(
                center.x - baseRadius * cos(ringAngleRad),
                center.y - baseRadius * sin(ringAngleRad)
            )

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor),
                    start = gradientStart,
                    end = gradientEnd
                ),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = if (state == AssistantState.LISTENING) 7.dp.toPx() else 4.5.dp.toPx())
            )

            // 3. Inner Dark Glass Sphere Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    ),
                    center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 4. Harmonic Waveform Ribbons (Siri-style oscillating sound waves)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.THINKING) {
                drawSiriHarmonicWave(
                    center = center,
                    radius = baseRadius * 0.85f,
                    phase = wavePhase,
                    amplitude = if (state == AssistantState.THINKING) 0.25f else (0.4f + audioLevel * 0.6f),
                    color = primaryColor,
                    strokeWidth = 3.5.dp.toPx(),
                    frequency = 2.5f
                )

                drawSiriHarmonicWave(
                    center = center,
                    radius = baseRadius * 0.85f,
                    phase = wavePhase + 1.2f,
                    amplitude = if (state == AssistantState.THINKING) 0.35f else (0.5f + audioLevel * 0.7f),
                    color = secondaryColor,
                    strokeWidth = 3.dp.toPx(),
                    frequency = 3.2f
                )

                drawSiriHarmonicWave(
                    center = center,
                    radius = baseRadius * 0.85f,
                    phase = wavePhase + 2.4f,
                    amplitude = if (state == AssistantState.THINKING) 0.2f else (0.3f + audioLevel * 0.5f),
                    color = tertiaryColor,
                    strokeWidth = 2.5.dp.toPx(),
                    frequency = 1.8f
                )
            } else {
                // Idle gentle harmonic wave
                drawSiriHarmonicWave(
                    center = center,
                    radius = baseRadius * 0.85f,
                    phase = wavePhase,
                    amplitude = 0.15f,
                    color = primaryColor.copy(alpha = 0.6f),
                    strokeWidth = 2.5.dp.toPx(),
                    frequency = 2f
                )
                drawSiriHarmonicWave(
                    center = center,
                    radius = baseRadius * 0.85f,
                    phase = wavePhase + 1.5f,
                    amplitude = 0.12f,
                    color = secondaryColor.copy(alpha = 0.5f),
                    strokeWidth = 2.dp.toPx(),
                    frequency = 2.5f
                )
            }

            // 5. Core Radiant Energy Point
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (state == AssistantState.LISTENING) 0.95f else 0.75f),
                        primaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.4f
                ),
                radius = baseRadius * 0.4f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawSiriHarmonicWave(
    center: Offset,
    radius: Float,
    phase: Float,
    amplitude: Float,
    color: Color,
    strokeWidth: Float,
    frequency: Float
) {
    val path = Path()
    val steps = 60
    val startX = center.x - radius
    val endX = center.x + radius
    val width = endX - startX

    for (i in 0..steps) {
        val progress = i / steps.toFloat() // 0.0 to 1.0
        val x = startX + progress * width

        // Gaussian bell envelope so edges taper smoothly to 0 at the orb boundary
        val envelope = (sin(progress * PI)).toFloat()

        val yOffset = sin((progress * frequency * 2 * PI + phase).toDouble()).toFloat() *
                (radius * 0.55f * amplitude * envelope)
        val y = center.y + yOffset

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Composable
fun SiriVoiceWaveform(
    state: AssistantState,
    audioLevel: Float,
    primaryColor: Color = Color(0xFF00F5D4),
    secondaryColor: Color = Color(0xFF9D4EDD),
    modifier: Modifier = Modifier
) {
    if (state != AssistantState.LISTENING && state != AssistantState.SPEAKING && state != AssistantState.THINKING) {
        return
    }

    val barCount = 17
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth(0.6f),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val offsetProgress = i.toFloat() / barCount
            val waveFraction = (sin(offsetProgress * 3 * PI + phase).toFloat() + 1f) / 2f

            val heightFactor = when (state) {
                AssistantState.LISTENING -> (6f + waveFraction * 20f * (0.35f + audioLevel * 0.9f)).coerceIn(6f, 26f)
                AssistantState.SPEAKING -> (6f + waveFraction * 18f * (0.3f + audioLevel * 0.7f)).coerceIn(6f, 24f)
                AssistantState.THINKING -> (4f + waveFraction * 10f).coerceIn(4f, 14f)
                else -> 4f
            }
            val baseHeight = heightFactor.dp

            val barColor = if (i % 2 == 0) primaryColor else secondaryColor

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
