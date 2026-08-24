package com.example.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.voice.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance hardware-accelerated Apple Intelligence / Siri & Gemini style
 * glowing animated voice orb with dynamic harmonic soundwaves, multi-chromatic plasma body,
 * specular 3D refraction, and audio-reactive decibel pulsing.
 */
class FloatingOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var currentState: AssistantState = AssistantState.LISTENING
    private var pulseFraction = 0f
    private var rotationAngle = 0f
    private var wavePhase = 0f
    private var targetRmsLevel = 0.2f
    private var currentRmsLevel = 0.2f
    private var isPressedState = false

    private val continuousAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val fraction = it.animatedValue as Float
            // Smooth pulse breathing
            pulseFraction = (sin(fraction * 2 * PI).toFloat() + 1f) / 2f
            // Continuous rotation for chromatic rings
            rotationAngle = (rotationAngle + 3f) % 360f
            // Harmonic wave phase
            wavePhase = (wavePhase + 0.08f) % (2 * PI.toFloat())
            // Smooth RMS interpolation
            currentRmsLevel += (targetRmsLevel - currentRmsLevel) * 0.25f
            invalidate()
        }
    }

    init {
        continuousAnimator.start()
    }

    fun setState(state: AssistantState) {
        currentState = state
        invalidate()
    }

    fun setRmsAudioLevel(level: Float) {
        targetRmsLevel = level.coerceIn(0.1f, 1.8f)
    }

    fun setPressedVisual(pressed: Boolean) {
        isPressedState = pressed
        invalidate()
    }

    fun stopAnimation() {
        continuousAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (Math.min(width, height) / 2f) * 0.68f

        // Dynamic scale computation based on state and audio level
        val stateScale = when (currentState) {
            AssistantState.LISTENING -> 1.0f + (currentRmsLevel * 0.18f) + (pulseFraction * 0.06f)
            AssistantState.THINKING -> 1.04f + (pulseFraction * 0.08f)
            AssistantState.SPEAKING -> 1.06f + (currentRmsLevel * 0.14f) + (pulseFraction * 0.08f)
            AssistantState.ERROR -> 0.95f
            AssistantState.IDLE -> 0.94f + (pulseFraction * 0.04f)
        }
        val finalScale = if (isPressedState) stateScale * 0.92f else stateScale
        val orbRadius = baseRadius * finalScale

        // 1. Radiant Atmospheric Blooming Halo
        drawAtmosphericHalo(canvas, cx, cy, orbRadius)

        // 2. Rotating Chromatic Plasma Ring
        drawChromaticRing(canvas, cx, cy, orbRadius)

        // 3. Deep 3D Nebula Core Sphere Body
        drawCoreSphere(canvas, cx, cy, orbRadius)

        // 4. Harmonic Siri Waveform Ribbons inside the orb
        drawHarmonicWaves(canvas, cx, cy, orbRadius)

        // 5. Specular 3D Glass Surface Refraction Highlight
        drawSpecularHighlight(canvas, cx, cy, orbRadius)

        // 6. Central State Glyph / Icon
        drawStateGlyph(canvas, cx, cy, orbRadius)
    }

    private fun drawAtmosphericHalo(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val haloRadius = orbRadius * 1.55f
        val alphaMultiplier = if (currentState == AssistantState.LISTENING || currentState == AssistantState.SPEAKING) 1f else 0.75f

        val haloColors = when (currentState) {
            AssistantState.LISTENING -> intArrayOf(
                Color.argb(((160 + pulseFraction * 60) * alphaMultiplier).toInt(), 0, 245, 255), // Cyan
                Color.argb(((110 + pulseFraction * 40) * alphaMultiplier).toInt(), 139, 92, 246), // Purple
                Color.argb((50 * alphaMultiplier).toInt(), 59, 130, 246), // Blue
                Color.TRANSPARENT
            )
            AssistantState.THINKING -> intArrayOf(
                Color.argb((180 * alphaMultiplier).toInt(), 236, 72, 153), // Pink
                Color.argb((120 * alphaMultiplier).toInt(), 168, 85, 247), // Purple
                Color.argb((40 * alphaMultiplier).toInt(), 59, 130, 246),
                Color.TRANSPARENT
            )
            AssistantState.SPEAKING -> intArrayOf(
                Color.argb((170 * alphaMultiplier).toInt(), 52, 211, 153), // Emerald
                Color.argb((110 * alphaMultiplier).toInt(), 6, 182, 212), // Cyan
                Color.argb((40 * alphaMultiplier).toInt(), 139, 92, 246),
                Color.TRANSPARENT
            )
            AssistantState.ERROR -> intArrayOf(
                Color.argb(160, 239, 68, 68),
                Color.argb(80, 245, 158, 11),
                Color.TRANSPARENT
            )
            AssistantState.IDLE -> intArrayOf(
                Color.argb((100 + pulseFraction * 40).toInt(), 0, 229, 255),
                Color.argb((50 + pulseFraction * 20).toInt(), 124, 77, 255),
                Color.TRANSPARENT
            )
        }

        val positions = if (haloColors.size == 4) floatArrayOf(0f, 0.35f, 0.72f, 1f) else floatArrayOf(0f, 0.55f, 1f)
        val shader = RadialGradient(cx, cy, haloRadius, haloColors, positions, Shader.TileMode.CLAMP)
        haloPaint.shader = shader
        canvas.drawCircle(cx, cy, haloRadius, haloPaint)
    }

    private fun drawChromaticRing(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val ringRadius = orbRadius * 1.05f
        val ringColors = when (currentState) {
            AssistantState.LISTENING -> intArrayOf(
                Color.parseColor("#00F5FF"),
                Color.parseColor("#8B5CF6"),
                Color.parseColor("#EC4899"),
                Color.parseColor("#3B82F6"),
                Color.parseColor("#00F5FF")
            )
            AssistantState.THINKING -> intArrayOf(
                Color.parseColor("#F472B6"),
                Color.parseColor("#C084FC"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#F472B6")
            )
            AssistantState.SPEAKING -> intArrayOf(
                Color.parseColor("#34D399"),
                Color.parseColor("#00F5FF"),
                Color.parseColor("#A855F7"),
                Color.parseColor("#34D399")
            )
            AssistantState.ERROR -> intArrayOf(
                Color.parseColor("#EF4444"),
                Color.parseColor("#F59E0B"),
                Color.parseColor("#EF4444")
            )
            AssistantState.IDLE -> intArrayOf(
                Color.parseColor("#00E5FF"),
                Color.parseColor("#7C4DFF"),
                Color.parseColor("#E040FB"),
                Color.parseColor("#00E5FF")
            )
        }

        ringPaint.strokeWidth = if (currentState == AssistantState.LISTENING) 6f else 4f
        val sweepShader = SweepGradient(cx, cy, ringColors, null)

        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        ringPaint.shader = sweepShader
        canvas.drawCircle(cx, cy, ringRadius, ringPaint)
        canvas.restore()
    }

    private fun drawCoreSphere(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val sphereColors = when (currentState) {
            AssistantState.LISTENING -> intArrayOf(
                Color.parseColor("#4DEEEA"), // Vivid neon cyan top highlight
                Color.parseColor("#3B82F6"), // Rich electric blue
                Color.parseColor("#7C3AED"), // Deep violet
                Color.parseColor("#090D16")  // Deep obsidian core
            )
            AssistantState.THINKING -> intArrayOf(
                Color.parseColor("#F472B6"), // Pink
                Color.parseColor("#A855F7"), // Purple
                Color.parseColor("#3B82F6"), // Blue
                Color.parseColor("#090D16")
            )
            AssistantState.SPEAKING -> intArrayOf(
                Color.parseColor("#34D399"), // Emerald
                Color.parseColor("#06B6D4"), // Cyan
                Color.parseColor("#8B5CF6"), // Violet
                Color.parseColor("#090D16")
            )
            AssistantState.ERROR -> intArrayOf(
                Color.parseColor("#F87171"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#450A0A")
            )
            AssistantState.IDLE -> intArrayOf(
                Color.parseColor("#38BDF8"),
                Color.parseColor("#6366F1"),
                Color.parseColor("#090D16")
            )
        }

        val positions = if (sphereColors.size == 4) floatArrayOf(0f, 0.32f, 0.70f, 1f) else floatArrayOf(0f, 0.55f, 1f)
        val shader = RadialGradient(
            cx - orbRadius * 0.22f,
            cy - orbRadius * 0.28f,
            orbRadius * 1.25f,
            sphereColors,
            positions,
            Shader.TileMode.CLAMP
        )
        bodyPaint.shader = shader
        canvas.drawCircle(cx, cy, orbRadius, bodyPaint)
    }

    private fun drawHarmonicWaves(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val amplitude = when (currentState) {
            AssistantState.LISTENING -> (0.35f + currentRmsLevel * 0.65f).coerceIn(0.2f, 1.2f)
            AssistantState.SPEAKING -> (0.40f + currentRmsLevel * 0.55f).coerceIn(0.2f, 1.0f)
            AssistantState.THINKING -> 0.35f
            AssistantState.IDLE -> 0.15f
            AssistantState.ERROR -> 0.1f
        }

        // Wave 1: Primary Cyan harmonic ribbon
        drawSingleSineWave(
            canvas, cx, cy, orbRadius * 0.85f,
            phase = wavePhase,
            frequency = 2.4f,
            amplitude = amplitude,
            color = Color.argb(190, 0, 245, 255),
            strokeWidth = 5f
        )

        // Wave 2: Secondary Violet harmonic ribbon
        drawSingleSineWave(
            canvas, cx, cy, orbRadius * 0.85f,
            phase = wavePhase + 1.4f,
            frequency = 3.1f,
            amplitude = amplitude * 0.85f,
            color = Color.argb(170, 236, 72, 153),
            strokeWidth = 4f
        )

        // Wave 3: Tertiary Gold/Emerald accent ribbon
        drawSingleSineWave(
            canvas, cx, cy, orbRadius * 0.85f,
            phase = wavePhase + 2.8f,
            frequency = 1.9f,
            amplitude = amplitude * 0.7f,
            color = Color.argb(140, 52, 211, 153),
            strokeWidth = 3f
        )
    }

    private fun drawSingleSineWave(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        phase: Float,
        frequency: Float,
        amplitude: Float,
        color: Int,
        strokeWidth: Float
    ) {
        val path = Path()
        val steps = 48
        val startX = cx - radius
        val endX = cx + radius
        val width = endX - startX

        for (i in 0..steps) {
            val progress = i / steps.toFloat() // 0.0 to 1.0
            val x = startX + progress * width

            // Gaussian bell taper so waveform ends smoothly at sphere boundaries
            val envelope = sin(progress * PI).toFloat()
            val yOffset = sin((progress * frequency * 2 * PI + phase).toDouble()).toFloat() * (radius * 0.45f * amplitude * envelope)
            val y = cy + yOffset

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        wavePaint.color = color
        wavePaint.strokeWidth = strokeWidth
        canvas.drawPath(path, wavePaint)
    }

    private fun drawSpecularHighlight(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val highlightRadius = orbRadius * 0.58f
        val highlightShader = LinearGradient(
            cx, cy - orbRadius * 0.85f,
            cx, cy - orbRadius * 0.10f,
            intArrayOf(Color.argb(190, 255, 255, 255), Color.argb(60, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        highlightPaint.shader = highlightShader
        canvas.drawCircle(cx, cy - orbRadius * 0.38f, highlightRadius, highlightPaint)
    }

    private fun drawStateGlyph(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val glyphSize = orbRadius * 0.46f

        when (currentState) {
            AssistantState.LISTENING -> {
                drawMicrophoneIcon(canvas, cx, cy, glyphSize)
            }
            AssistantState.THINKING -> {
                drawSparkleGlyph(canvas, cx, cy, glyphSize)
            }
            AssistantState.SPEAKING -> {
                drawEqualizerBars(canvas, cx, cy, glyphSize)
            }
            AssistantState.ERROR -> {
                drawErrorGlyph(canvas, cx, cy, glyphSize)
            }
            AssistantState.IDLE -> {
                drawMicrophoneIcon(canvas, cx, cy, glyphSize * 0.9f)
            }
        }
    }

    private fun drawEqualizerBars(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val barCount = 4
        val barWidth = size * 0.16f
        val spacing = size * 0.12f
        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = cx - totalWidth / 2f + barWidth / 2f

        iconPaint.strokeWidth = barWidth
        iconPaint.color = Color.WHITE

        for (i in 0 until barCount) {
            val x = startX + i * (barWidth + spacing)
            val wave = sin((wavePhase * 1.5) + (i * 1.3)).toFloat()
            val heightMultiplier = 0.35f + Math.abs(wave) * 0.65f
            val barHeight = size * heightMultiplier
            canvas.drawLine(x, cy - barHeight / 2f, x, cy + barHeight / 2f, iconPaint)
        }
    }

    private fun drawSparkleGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconFillPaint.color = Color.WHITE
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx, cy, cx + size, cy)
            quadTo(cx, cy, cx, cy + size)
            quadTo(cx, cy, cx - size, cy)
            quadTo(cx, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, iconFillPaint)
    }

    private fun drawErrorGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.strokeWidth = size * 0.18f
        iconPaint.color = Color.WHITE
        val d = size * 0.55f
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, iconPaint)
        canvas.drawLine(cx + d, cy - d, cx - d, cy + d, iconPaint)
    }

    private fun drawMicrophoneIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val strokeWidth = size * 0.16f
        iconPaint.strokeWidth = strokeWidth
        iconPaint.color = Color.WHITE

        val micWidth = size * 0.52f
        val micHeight = size * 0.88f
        val micTop = cy - micHeight * 0.65f
        val micBottom = micTop + micHeight * 0.80f

        val capsuleRect = RectF(
            cx - micWidth / 2f,
            micTop,
            cx + micWidth / 2f,
            micBottom
        )
        val cornerRadius = micWidth / 2f
        canvas.drawRoundRect(capsuleRect, cornerRadius, cornerRadius, iconFillPaint)

        val arcRect = RectF(
            cx - micWidth * 0.85f,
            cy - micHeight * 0.30f,
            cx + micWidth * 0.85f,
            cy + micHeight * 0.52f
        )
        canvas.drawArc(arcRect, 0f, 180f, false, iconPaint)

        val stemTop = cy + micHeight * 0.52f
        val stemBottom = cy + micHeight * 0.82f
        canvas.drawLine(cx, stemTop, cx, stemBottom, iconPaint)

        val baseWidth = micWidth * 0.8f
        canvas.drawLine(cx - baseWidth / 2f, stemBottom, cx + baseWidth / 2f, stemBottom, iconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
