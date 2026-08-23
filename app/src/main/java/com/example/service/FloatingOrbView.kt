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
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.voice.AssistantState

/**
 * Animated hardware-accelerated Siri-style glowing orb supporting multi-state visual indicators:
 * LISTENING, THINKING, SPEAKING, ERROR, and IDLE.
 */
class FloatingOrbView(context: Context) : View(context) {

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
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
    private var rotateAngle = 0f
    private var waveFraction = 0f
    private var rmsLevel = 0.2f
    private var isPressedState = false

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            pulseFraction = it.animatedValue as Float
            rotateAngle = (rotateAngle + 2.5f) % 360f
            waveFraction = (waveFraction + 0.05f) % 1f
            invalidate()
        }
    }

    init {
        pulseAnimator.start()
    }

    fun setState(state: AssistantState) {
        currentState = state
        invalidate()
    }

    fun setRmsAudioLevel(level: Float) {
        rmsLevel = level.coerceIn(0.1f, 1.8f)
        invalidate()
    }

    fun setPressedVisual(pressed: Boolean) {
        isPressedState = pressed
        invalidate()
    }

    fun stopAnimation() {
        pulseAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (Math.min(width, height) / 2f) * 0.70f

        val stateScale = when (currentState) {
            AssistantState.LISTENING -> 1f + (rmsLevel * 0.12f) + (pulseFraction * 0.05f)
            AssistantState.THINKING -> 1.05f + (pulseFraction * 0.06f)
            AssistantState.SPEAKING -> 1.08f + (pulseFraction * 0.10f)
            AssistantState.ERROR -> 0.95f
            AssistantState.IDLE -> 0.92f
        }
        val scale = if (isPressedState) stateScale * 1.1f else stateScale
        val orbRadius = baseRadius * scale

        // 1. Outer Glowing Halo based on state
        drawHalo(canvas, cx, cy, orbRadius)

        // 2. Multi-color Core Sphere
        drawSphereBody(canvas, cx, cy, orbRadius)

        // 3. Inner Specular Highlight / Glass refraction
        drawSpecularHighlight(canvas, cx, cy, orbRadius)

        // 4. State-specific Animated Overlays & Glyphs
        when (currentState) {
            AssistantState.LISTENING -> {
                drawListeningRings(canvas, cx, cy, orbRadius)
                drawMicrophoneIcon(canvas, cx, cy, orbRadius * 0.44f)
            }
            AssistantState.THINKING -> {
                drawThinkingSpinRings(canvas, cx, cy, orbRadius)
                drawSparkleGlyph(canvas, cx, cy, orbRadius * 0.44f)
            }
            AssistantState.SPEAKING -> {
                drawSpeakingWavebars(canvas, cx, cy, orbRadius * 0.45f)
            }
            AssistantState.ERROR -> {
                drawErrorGlyph(canvas, cx, cy, orbRadius * 0.42f)
            }
            AssistantState.IDLE -> {
                drawMicrophoneIcon(canvas, cx, cy, orbRadius * 0.40f)
            }
        }
    }

    private fun drawHalo(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val haloRadius = orbRadius * 1.45f
        val haloColors = when (currentState) {
            AssistantState.LISTENING -> intArrayOf(
                Color.argb((150 + pulseFraction * 60).toInt(), 0, 245, 255), // Neon Cyan
                Color.argb((100 + pulseFraction * 40).toInt(), 139, 92, 246), // Vivid Violet
                Color.argb((40 + pulseFraction * 20).toInt(), 59, 130, 246), // Electric Blue
                Color.TRANSPARENT
            )
            AssistantState.THINKING -> intArrayOf(
                Color.argb(190, 168, 85, 247), // Purple
                Color.argb(130, 236, 72, 153), // Pink
                Color.argb(50, 59, 130, 246),  // Blue
                Color.TRANSPARENT
            )
            AssistantState.SPEAKING -> intArrayOf(
                Color.argb(180, 16, 185, 129), // Emerald
                Color.argb(120, 0, 245, 255),  // Cyan
                Color.argb(60, 245, 158, 11),  // Amber
                Color.TRANSPARENT
            )
            AssistantState.ERROR -> intArrayOf(
                Color.argb(140, 239, 68, 68),  // Red
                Color.argb(70, 245, 158, 11),  // Amber
                Color.TRANSPARENT
            )
            AssistantState.IDLE -> intArrayOf(
                Color.argb(90, 0, 245, 255),
                Color.argb(40, 139, 92, 246),
                Color.TRANSPARENT
            )
        }

        val positions = if (haloColors.size == 4) floatArrayOf(0f, 0.4f, 0.75f, 1f) else floatArrayOf(0f, 0.55f, 1f)
        val haloShader = RadialGradient(cx, cy, haloRadius, haloColors, positions, Shader.TileMode.CLAMP)
        haloPaint.shader = haloShader
        canvas.drawCircle(cx, cy, haloRadius, haloPaint)
    }

    private fun drawSphereBody(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val orbColors = when (currentState) {
            AssistantState.LISTENING -> intArrayOf(
                Color.parseColor("#4DEEEA"), // Cyan
                Color.parseColor("#3B82F6"), // Blue
                Color.parseColor("#7C3AED"), // Deep violet
                Color.parseColor("#0F172A")  // Slate dark
            )
            AssistantState.THINKING -> intArrayOf(
                Color.parseColor("#F472B6"), // Pink
                Color.parseColor("#A855F7"), // Purple
                Color.parseColor("#3B82F6"), // Blue
                Color.parseColor("#1E1B4B")
            )
            AssistantState.SPEAKING -> intArrayOf(
                Color.parseColor("#34D399"), // Emerald
                Color.parseColor("#06B6D4"), // Cyan
                Color.parseColor("#8B5CF6"), // Violet
                Color.parseColor("#0F172A")
            )
            AssistantState.ERROR -> intArrayOf(
                Color.parseColor("#F87171"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#7F1D1D")
            )
            AssistantState.IDLE -> intArrayOf(
                Color.parseColor("#38BDF8"),
                Color.parseColor("#6366F1"),
                Color.parseColor("#0F172A")
            )
        }

        val positions = if (orbColors.size == 4) floatArrayOf(0f, 0.35f, 0.75f, 1f) else floatArrayOf(0f, 0.55f, 1f)
        val orbShader = RadialGradient(
            cx - orbRadius * 0.25f,
            cy - orbRadius * 0.3f,
            orbRadius * 1.25f,
            orbColors,
            positions,
            Shader.TileMode.CLAMP
        )
        orbPaint.shader = orbShader
        canvas.drawCircle(cx, cy, orbRadius, orbPaint)
    }

    private fun drawSpecularHighlight(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val highlightRadius = orbRadius * 0.55f
        val highlightShader = LinearGradient(
            cx, cy - orbRadius * 0.8f, cx, cy,
            Color.argb(170, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        highlightPaint.shader = highlightShader
        canvas.drawCircle(cx, cy - orbRadius * 0.35f, highlightRadius, highlightPaint)
    }

    private fun drawListeningRings(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        val ringRadius = orbRadius * (1.08f + rmsLevel * 0.15f)
        ringPaint.strokeWidth = 3f
        ringPaint.color = Color.argb((120 + pulseFraction * 80).toInt(), 0, 245, 255)
        canvas.drawCircle(cx, cy, ringRadius, ringPaint)
    }

    private fun drawThinkingSpinRings(canvas: Canvas, cx: Float, cy: Float, orbRadius: Float) {
        ringPaint.strokeWidth = 4f
        ringPaint.color = Color.argb(200, 236, 72, 153)
        val rect = RectF(cx - orbRadius * 1.15f, cy - orbRadius * 1.15f, cx + orbRadius * 1.15f, cy + orbRadius * 1.15f)
        canvas.drawArc(rect, rotateAngle, 100f, false, ringPaint)

        ringPaint.color = Color.argb(200, 0, 245, 255)
        canvas.drawArc(rect, rotateAngle + 180f, 100f, false, ringPaint)
    }

    private fun drawSpeakingWavebars(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val barCount = 4
        val barWidth = size * 0.15f
        val spacing = size * 0.10f
        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = cx - totalWidth / 2f + barWidth / 2f

        iconPaint.strokeWidth = barWidth
        iconPaint.color = Color.WHITE

        for (i in 0 until barCount) {
            val x = startX + i * (barWidth + spacing)
            val wave = Math.sin((waveFraction * 2 * Math.PI) + (i * 1.2)).toFloat()
            val heightMultiplier = 0.4f + Math.abs(wave) * 0.6f
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
        val d = size * 0.6f
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, iconPaint)
        canvas.drawLine(cx + d, cy - d, cx - d, cy + d, iconPaint)
    }

    private fun drawMicrophoneIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val strokeWidth = size * 0.16f
        iconPaint.strokeWidth = strokeWidth
        iconPaint.color = Color.WHITE

        val micWidth = size * 0.55f
        val micHeight = size * 0.9f
        val micTop = cy - micHeight * 0.7f
        val micBottom = micTop + micHeight * 0.85f

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
            cy - micHeight * 0.35f,
            cx + micWidth * 0.85f,
            cy + micHeight * 0.55f
        )
        canvas.drawArc(arcRect, 0f, 180f, false, iconPaint)

        val stemTop = cy + micHeight * 0.55f
        val stemBottom = cy + micHeight * 0.85f
        canvas.drawLine(cx, stemTop, cx, stemBottom, iconPaint)

        val baseWidth = micWidth * 0.8f
        canvas.drawLine(cx - baseWidth / 2f, stemBottom, cx + baseWidth / 2f, stemBottom, iconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
