package com.example.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Vibrator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Custom hardware-accelerated animated Siri-style glowing orb view for WindowManager overlay.
 */
class FloatingOrbView(context: Context) : View(context) {

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

    private var pulseFraction = 0f
    private var rotateAngle = 0f
    private var isPressedState = false

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            pulseFraction = it.animatedValue as Float
            rotateAngle = (rotateAngle + 1.5f) % 360f
            invalidate()
        }
    }

    init {
        pulseAnimator.start()
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
        val baseRadius = (Math.min(width, height) / 2f) * 0.72f
        val scale = if (isPressedState) 1.1f else (1f + pulseFraction * 0.08f)
        val orbRadius = baseRadius * scale

        // 1. Draw Outer Glowing Halo (Electric Cyan / Vivid Violet / Deep Indigo)
        val haloRadius = orbRadius * 1.35f
        val haloShader = RadialGradient(
            cx, cy, haloRadius,
            intArrayOf(
                Color.argb((140 + pulseFraction * 50).toInt(), 0, 245, 255),  // Neon Cyan
                Color.argb((90 + pulseFraction * 30).toInt(), 139, 92, 246),  // Vivid Violet
                Color.argb((40 + pulseFraction * 20).toInt(), 59, 130, 246),  // Electric Blue
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        haloPaint.shader = haloShader
        canvas.drawCircle(cx, cy, haloRadius, haloPaint)

        // 2. Draw Sphere Body with Multi-color Linear/Radial Gradient
        val orbShader = RadialGradient(
            cx - orbRadius * 0.25f, cy - orbRadius * 0.3f, orbRadius * 1.2f,
            intArrayOf(
                Color.parseColor("#4DEEEA"), // Bright cyan light
                Color.parseColor("#3B82F6"), // Electric blue
                Color.parseColor("#7C3AED"), // Deep violet
                Color.parseColor("#1E1B4B")  // Dark obsidian edge
            ),
            floatArrayOf(0f, 0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        orbPaint.shader = orbShader
        canvas.drawCircle(cx, cy, orbRadius, orbPaint)

        // 3. Draw Inner Specular Highlight (Glass refraction effect)
        val highlightRadius = orbRadius * 0.55f
        val highlightShader = LinearGradient(
            cx, cy - orbRadius * 0.8f, cx, cy,
            Color.argb(160, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        highlightPaint.shader = highlightShader
        canvas.drawCircle(cx, cy - orbRadius * 0.35f, highlightRadius, highlightPaint)

        // 4. Draw Center Microphone / Soundwave Glyph
        drawMicrophoneIcon(canvas, cx, cy, orbRadius * 0.42f)
    }

    private fun drawMicrophoneIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val strokeWidth = size * 0.16f
        iconPaint.strokeWidth = strokeWidth

        val micWidth = size * 0.55f
        val micHeight = size * 0.9f
        val micTop = cy - micHeight * 0.7f
        val micBottom = micTop + micHeight * 0.85f

        // Mic capsule body
        val capsuleRect = android.graphics.RectF(
            cx - micWidth / 2f,
            micTop,
            cx + micWidth / 2f,
            micBottom
        )
        val cornerRadius = micWidth / 2f
        canvas.drawRoundRect(capsuleRect, cornerRadius, cornerRadius, iconFillPaint)

        // Mic cradle arc
        val arcRect = android.graphics.RectF(
            cx - micWidth * 0.85f,
            cy - micHeight * 0.35f,
            cx + micWidth * 0.85f,
            cy + micHeight * 0.55f
        )
        canvas.drawArc(arcRect, 0f, 180f, false, iconPaint)

        // Mic stem & base
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
