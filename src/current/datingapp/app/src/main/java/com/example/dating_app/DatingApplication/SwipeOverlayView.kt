package com.example.dating_app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.*

/**
 * Extends FrameLayout so it can wrap the ScrollView and use onInterceptTouchEvent
 * to cleanly share gestures — horizontal drags are intercepted for swipe feedback,
 * vertical drags are passed straight through to the ScrollView child.
 *
 * Layout structure:
 *   <com.example.dating_app.SwipeOverlayView
 *       android:id="@+id/swipeOverlay"
 *       android:layout_width="match_parent"
 *       android:layout_height="match_parent">
 *
 *       <ScrollView ...>  ← your existing ScrollView goes INSIDE here
 *           ...
 *       </ScrollView>
 *
 *   </com.example.dating_app.SwipeOverlayView>
 */
class SwipeOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    init {
        // Required for onDraw to be called on a ViewGroup
        setWillNotDraw(false)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    var listener: SwipeListener? = null

    // ── Tuning ────────────────────────────────────────────────────────────────

    companion object {
        private const val THRESHOLD_FRACTION  = 0.36f
        private const val PARTICLE_COUNT      = 22
        private const val FLY_OUT_DURATION_MS = 340L
    }

    // ── Colors ────────────────────────────────────────────────────────────────

    private val likeColor = Color.parseColor("#4CAF50")
    private val nopeColor = Color.parseColor("#F44336")

    // ── Gesture state ─────────────────────────────────────────────────────────

    private var downRawX        = 0f
    private var downRawY        = 0f
    private var dragX           = 0f
    private var dragY           = 0f
    private var isDragging      = false
    private var isHorizontal    = false   // true once we've committed to a horizontal drag
    private var gestureDecided  = false   // true once direction has been determined

    // ── Visual state ──────────────────────────────────────────────────────────

    private val progress: Float
        get() = (abs(dragX) / (width.coerceAtLeast(1) * THRESHOLD_FRACTION)).coerceIn(0f, 1f)

    private val isRight: Boolean get() = dragX >= 0f

    private var overlayAlpha = 0f

    // ── Springs ───────────────────────────────────────────────────────────────

    private var snapSpringX: SpringAnimation? = null
    private var snapSpringY: SpringAnimation? = null

    // ── Paints ────────────────────────────────────────────────────────────────

    private val particlePaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val washPaint         = Paint()
    private val textBounds        = Rect()

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize  = 92f
        textAlign = Paint.Align.LEFT
    }

    private val labelStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface    = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize    = 92f
        textAlign   = Paint.Align.LEFT
        style       = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    // ── Particle seeds ────────────────────────────────────────────────────────

    private data class Particle(
        val normY: Float, val radiusScale: Float, val phase: Float,
        val driftX: Float, val driftY: Float
    )

    private val particles = List(PARTICLE_COUNT) { i ->
        val s = i * 137.508f
        Particle(
            normY       = sin(s.toDouble()).toFloat() * 0.5f + 0.5f,
            radiusScale = cos((s * 2.3).toDouble()).toFloat() * 0.5f + 0.5f,
            phase       = sin((s * 0.7).toDouble()).toFloat() * 0.5f + 0.5f,
            driftX      = sin((s * 3.1).toDouble()).toFloat() * 30f,
            driftY      = cos((s * 1.9).toDouble()).toFloat() * 24f
        )
    }

    // ── Programmatic triggers ─────────────────────────────────────────────────

    fun triggerSwipeRight() { flyOut(toRight = true) }
    fun triggerSwipeLeft()  { flyOut(toRight = false) }

    // ── Gesture interception ──────────────────────────────────────────────────

    /**
     * Called before child views (ScrollView) receive touch events.
     * Returns true only when we've confirmed a horizontal drag —
     * at that point we steal the gesture from the ScrollView.
     * Vertical movements return false immediately so ScrollView scrolls normally.
     */
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX       = event.rawX
                downRawY       = event.rawY
                isDragging     = false
                isHorizontal   = false
                gestureDecided = false
                cancelSprings()
                return false   // never intercept on DOWN — let child see it first
            }
            MotionEvent.ACTION_MOVE -> {
                if (gestureDecided) return isHorizontal
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (dx * dx + dy * dy < 25f) return false   // still within slop
                isHorizontal   = abs(dx) > abs(dy)
                gestureDecided = true
                return isHorizontal   // intercept only if horizontal
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> return false
        }
        return false
    }

    /**
     * Only called after onInterceptTouchEvent returns true (i.e. horizontal drag confirmed).
     * Handles the drag, fly-out, and spring-back from here.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                isDragging   = true
                dragX        = dx
                dragY        = dy * 0.28f
                overlayAlpha = (progress * 1.5f).coerceIn(0f, 1f)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) return false
                val threshold = width * THRESHOLD_FRACTION
                when {
                    dragX >  threshold -> flyOut(toRight = true)
                    dragX < -threshold -> flyOut(toRight = false)
                    else               -> springBack()
                }
                return true
            }
        }
        return false
    }

    // ── Fly-out ───────────────────────────────────────────────────────────────

    private fun flyOut(toRight: Boolean) {
        val targetX  = if (toRight) width * 1.15f else -width * 1.15f
        val startX   = dragX
        val startY   = dragY
        var notified = false

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration     = FLY_OUT_DURATION_MS
            interpolator = AccelerateInterpolator(1.4f)
            addUpdateListener { anim ->
                val f        = anim.animatedFraction
                dragX        = lerp(startX, targetX.toFloat(), f)
                dragY        = lerp(startY, startY + 50f, f)
                overlayAlpha = lerp(1f, 0f, (f / 0.65f).coerceIn(0f, 1f))
                if (!notified && f >= 0.5f) {
                    notified = true
                    if (toRight) listener?.onLiked() else listener?.onDisliked()
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { resetState() }
            })
            start()
        }
    }

    // ── Spring-back ───────────────────────────────────────────────────────────

    private fun springBack() {
        translationX = dragX
        translationY = dragY

        snapSpringX = SpringAnimation(this, DynamicAnimation.TRANSLATION_X, 0f).apply {
            spring.stiffness    = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            addUpdateListener { _, value, _ ->
                dragX        = value
                overlayAlpha = (progress * 1.5f).coerceIn(0f, 1f)
                invalidate()
            }
            addEndListener { _, _, _, _ -> resetState() }
            start()
        }
        snapSpringY = SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring.stiffness    = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            addUpdateListener { _, value, _ -> dragY = value; invalidate() }
            start()
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)   // draws ScrollView first
        if (overlayAlpha <= 0.01f || progress <= 0.01f) return
        drawEdgeWash(canvas)
//        drawParticles(canvas)
        drawLabel(canvas)
    }

    private fun drawEdgeWash(canvas: Canvas) {
        val color    = if (isRight) likeColor else nopeColor
        val maxReach = width * 0.62f
        val reach    = maxReach * progress
        washPaint.color = color

        val bandWidth = (reach * 0.18f).coerceAtLeast(1f)
        washPaint.alpha = (overlayAlpha * progress * 60).toInt().coerceIn(0, 60)
        if (isRight) canvas.drawRect(width - bandWidth, 0f, width.toFloat(), height.toFloat(), washPaint)
        else         canvas.drawRect(0f, 0f, bandWidth, height.toFloat(), washPaint)

        repeat(6) { i ->
            val falloff   = (6 - i).toFloat() / 6f
            val stepReach = reach * (i + 1f) / 6f
            washPaint.alpha = (overlayAlpha * progress * falloff * 35).toInt().coerceIn(0, 35)
            if (isRight) canvas.drawRect(width - stepReach, 0f, width.toFloat(), height.toFloat(), washPaint)
            else         canvas.drawRect(0f, 0f, stepReach, height.toFloat(), washPaint)
        }
    }

    private fun drawLabel(canvas: Canvas) {
        val text  = if (isRight) "LIKE" else "NOPE"
        val color = if (isRight) likeColor else nopeColor
        val alpha = ((progress / 0.5f) * overlayAlpha * 255).toInt().coerceIn(0, 255)
        labelPaint.color       = color;  labelPaint.alpha       = alpha
        labelStrokePaint.color = color;  labelStrokePaint.alpha = alpha
        labelPaint.getTextBounds(text, 0, text.length, textBounds)
        val tw = textBounds.width().toFloat()
        val th = textBounds.height().toFloat()
        val x  = if (isRight) width * 0.08f else width - tw - width * 0.08f
        val y  = height * 0.17f + th
        canvas.save()
        canvas.rotate(if (isRight) -14f else 14f, x + tw / 2f, y - th / 2f)
        canvas.drawText(text, x, y, labelPaint)
        canvas.drawText(text, x, y, labelStrokePaint)
        canvas.restore()
    }

    private fun drawParticles(canvas: Canvas) {
        val color     = if (isRight) likeColor else nopeColor
        val edgeX     = if (isRight) width.toFloat() else 0f
        val maxInward = width * 0.26f
        particles.forEach { p ->
            val pp = ((progress - p.phase * 0.25f) / 0.75f).coerceIn(0f, 1f)
            if (pp <= 0f) return@forEach
            val alpha  = (pp * 0.5f * overlayAlpha * progress * 255).toInt().coerceIn(0, 130)
            val radius = (5f + p.radiusScale * 13f) * pp
            val inward = if (isRight) -(p.radiusScale * maxInward + 18f) else (p.radiusScale * maxInward + 18f)
            particlePaint.color = color
            particlePaint.alpha = alpha
            canvas.drawCircle(edgeX + inward + p.driftX * pp, height * p.normY + p.driftY * pp, radius, particlePaint)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun cancelSprings() {
        snapSpringX?.cancel(); snapSpringY?.cancel()
        translationX = 0f;     translationY = 0f
    }

    private fun resetState() {
        dragX = 0f; dragY = 0f; overlayAlpha = 0f
        translationX = 0f; translationY = 0f
        invalidate()
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
