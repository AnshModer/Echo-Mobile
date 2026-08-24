package com.example.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.voice.AssistantState

/**
 * Floating system overlay view group housing the interactive FloatingOrbView
 * anchored in the bottom centre with a glassmorphic HUD pill card resting above the orb.
 */
class FloatingAssistantOverlayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val orbView: FloatingOrbView
    private val pillCard: LinearLayout
    private val stateBadgeText: TextView
    private val transcriptText: TextView
    private val closeButton: ImageView

    private val density = resources.displayMetrics.density
    private var onOrbClicked: (() -> Unit)? = null
    private var onCloseClicked: (() -> Unit)? = null
    private var onDragPositionChanged: ((Int, Int) -> Unit)? = null
    private var onSnapToEdge: (() -> Unit)? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false
    private val touchSlop = 10 * density

    init {
        // Root container styling
        clipChildren = false
        clipToPadding = false

        // 1. Response & Transcription HUD Capsule (Anchored ABOVE the Orb)
        pillCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = (16 * density).toInt()
            val padV = (12 * density).toInt()
            setPadding(padH, padV, padH, padV)

            // Glassmorphic dark nebula rounded background
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE0B1120")) // Slate 950 with 93% opacity
                cornerRadius = 20 * density
                setStroke((1.5f * density).toInt(), Color.parseColor("#3300F5FF")) // Neon Cyan border accent
            }

            layoutParams = LayoutParams(
                (310 * density).toInt(),
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = (112 * density).toInt() // Positioned directly above the larger orb
            }
            elevation = 16 * density
        }

        // Header Row in Pill (State Badge + Close button)
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        stateBadgeText = TextView(context).apply {
            text = "🎤 Listening..."
            setTextColor(Color.parseColor("#00F5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        closeButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#94A3B8"))
            val btnSize = (22 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            setPadding((2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt())
            setOnClickListener {
                onCloseClicked?.invoke()
            }
        }

        headerRow.addView(stateBadgeText)
        headerRow.addView(closeButton)

        // Transcription & Response Text Body
        transcriptText = TextView(context).apply {
            text = "Listening... Speak your command"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setLineSpacing(2.5f * density, 1f)
            maxLines = 5
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }

        pillCard.addView(headerRow)
        pillCard.addView(transcriptText)

        // 2. Interactive Animated Large Orb (Anchored at Bottom Center)
        val orbSizePx = (96 * density).toInt()
        orbView = FloatingOrbView(context).apply {
            layoutParams = LayoutParams(orbSizePx, orbSizePx).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = (8 * density).toInt()
            }
        }

        addView(pillCard)
        addView(orbView)

        setupOrbTouchEvents()
    }

    fun setCallbacks(
        onOrbClick: () -> Unit,
        onClose: () -> Unit,
        onDragMove: (Int, Int) -> Unit,
        onSnap: () -> Unit
    ) {
        this.onOrbClicked = onOrbClick
        this.onCloseClicked = onClose
        this.onDragPositionChanged = onDragMove
        this.onSnapToEdge = onSnap
    }

    fun updateState(state: AssistantState, message: String? = null) {
        orbView.setState(state)
        when (state) {
            AssistantState.LISTENING -> {
                stateBadgeText.text = "🎤 Listening..."
                stateBadgeText.setTextColor(Color.parseColor("#00F5FF"))
                (pillCard.background as? GradientDrawable)?.setStroke((1.5f * density).toInt(), Color.parseColor("#4D00F5FF"))
                if (message != null) transcriptText.text = message
                showPill(true)
            }
            AssistantState.THINKING -> {
                stateBadgeText.text = "✨ Processing..."
                stateBadgeText.setTextColor(Color.parseColor("#C084FC"))
                (pillCard.background as? GradientDrawable)?.setStroke((1.5f * density).toInt(), Color.parseColor("#4DC084FC"))
                if (message != null) transcriptText.text = message
                showPill(true)
            }
            AssistantState.SPEAKING -> {
                stateBadgeText.text = "🔊 Echo Assistant"
                stateBadgeText.setTextColor(Color.parseColor("#34D399"))
                (pillCard.background as? GradientDrawable)?.setStroke((1.5f * density).toInt(), Color.parseColor("#4D34D399"))
                if (message != null) transcriptText.text = message
                showPill(true)
            }
            AssistantState.ERROR -> {
                stateBadgeText.text = "⚠️ Notice"
                stateBadgeText.setTextColor(Color.parseColor("#F87171"))
                (pillCard.background as? GradientDrawable)?.setStroke((1.5f * density).toInt(), Color.parseColor("#4DF87171"))
                if (message != null) transcriptText.text = message
                showPill(true)
            }
            AssistantState.IDLE -> {
                stateBadgeText.text = "Echo Assistant"
                stateBadgeText.setTextColor(Color.parseColor("#38BDF8"))
                if (message != null) {
                    transcriptText.text = message
                    showPill(true)
                } else {
                    showPill(false)
                }
            }
        }
    }

    fun showPill(show: Boolean) {
        if (show && pillCard.visibility != View.VISIBLE) {
            pillCard.visibility = View.VISIBLE
            pillCard.alpha = 0f
            pillCard.scaleX = 0.85f
            pillCard.scaleY = 0.85f
            pillCard.translationY = 20 * density
            pillCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(240L)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        } else if (!show && pillCard.visibility == View.VISIBLE) {
            pillCard.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(15 * density)
                .setDuration(180L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        pillCard.visibility = View.GONE
                    }
                })
                .start()
        }
    }

    private fun setupOrbTouchEvents() {
        orbView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    orbView.setPressedVisual(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        onDragPositionChanged?.invoke(dx, dy)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    orbView.setPressedVisual(false)
                    if (!isDragging) {
                        onOrbClicked?.invoke()
                    } else {
                        onSnapToEdge?.invoke()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    orbView.setPressedVisual(false)
                    true
                }
                else -> false
            }
        }
    }
}
