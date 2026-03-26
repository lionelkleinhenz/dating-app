package com.example.dating_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.flexbox.FlexboxLayout
import org.json.JSONObject
import kotlinx.coroutines.coroutineScope

// ─────────────────────────────────────────────
// CONSTANTS
// ─────────────────────────────────────────────

private const val SWIPE_THRESHOLD_FRACTION = 0.35f   // fraction of screen width to trigger swipe
private const val ROTATION_FACTOR           = 0.08f   // degrees per px of horizontal drag
private const val LABEL_ALPHA_FACTOR        = 0.008f  // how fast the LIKE/NOPE label fades in
private const val PARTICLE_COUNT            = 18
private const val MAX_PARTICLE_RADIUS       = 80f
private val LIKE_COLOR  = Color(0xFF4CAF50)
private val NOPE_COLOR  = Color(0xFFF44336)
private val CARD_RADIUS = 20.dp

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profile: DatingProfile

    private var currentIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read profile from arguments bundle
        profile = arguments?.getString(ARG_PROFILE)
            ?.let { DatingProfile.fromJson(JSONObject(it)) }
            ?: return  // safety exit if somehow null

        Log.d("PROFILE", "onViewCreated called, profile=${profile.name}")

        val overlay = view.findViewById<SwipeOverlayView>(R.id.swipeOverlay)

        // The overlay handles all gesture detection — just give it a listener
        overlay.listener = object : SwipeListener {
            override fun onLiked() {
                // Called at midpoint of fly-out while overlay still covers screen
                // LIKED
            }
            override fun onDisliked() {
                // DISLIKED
            }
        }

        // Wire your existing buttons — they trigger the overlay's own animation
        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            overlay.triggerSwipeRight()
        }
        view.findViewById<View>(R.id.btn_pass).setOnClickListener {
            overlay.triggerSwipeLeft()
        }

        // Load initial profile
        bindProfile(view)
        animateEntrance(view)
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        bindProfile(view)
//        animateEntrance(view)
//    }

    private fun bindProfile(view: View) {
        // ── Hero image ────────────────────────────────────────────────────────
        val heroImage = view.findViewById<ImageView>(R.id.iv_hero)
        val resId = resources.getIdentifier(profile.img, "drawable", requireContext().packageName)

        if (resId != 0) {
            heroImage.setImageResource(resId)
        } else {
            heroImage.setImageResource(R.drawable.placeholder_profile)
        }

        // ── Compatibility badge ───────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_compatibility).text =
            "${profile.compatibilityScore}% Match"

        // ── Name / age / verified ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_name_age).text =
            "${profile.name}, ${profile.age}"
        view.findViewById<View>(R.id.iv_verified).visibility = View.VISIBLE

        // ── Occupation & location ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_species).text = profile.species
        view.findViewById<TextView>(R.id.tv_location).text =
            "${profile.location}"

        // ── Last active ──────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_last_active).text =
            "Active right now"

        // ── Bio ──────────────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_bio).text = profile.bio

        // ── Quick-fact pills ─────────────────────────────────────────────────
        val factsContainer = view.findViewById<FlexboxLayout>(R.id.ll_quick_facts)
        val facts = listOf(
            "⚧" to profile.gender,
            "\uD83D\uDDE8\uFE0F" to profile.pronouns,
            "\uD83D\uDC3E" to profile.furColor,
            "\uD83C\uDF00" to profile.tailType,
        )
        facts.forEach { (icon, label) ->
            addFactCard(factsContainer, icon, label)
        }

        // ── Interests chips ──────────────────────────────────────────────────
        val activitiesGroup = view.findViewById<ChipGroup>(R.id.chip_group_activities)
        profile.activities.forEach { activity ->
            activitiesGroup.addView(makeChip(activity, ChipStyle.FILLED))
        }

        // ── Personality chips ────────────────────────────────────────────────
        val personalityGroup = view.findViewById<ChipGroup>(R.id.chip_group_personality)
        profile.personalityTraits.forEach { trait ->
            personalityGroup.addView(makeChip(trait, ChipStyle.OUTLINED))
        }

        // ── Action buttons ────────────────────────────────────────────────────
        view.findViewById<View>(R.id.btn_pass).setOnClickListener {
            Toast.makeText(context, "Passed", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            Toast.makeText(context, "❤ Liked!", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun addFactCard(container: FlexboxLayout, icon: String, label: String) {
        val ctx = container.context
        val card = CardView(ctx).apply {
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginEnd = dpToPx(8)
                it.bottomMargin = dpToPx(8)
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_bg))
            useCompatPadding = true
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
        }

        inner.addView(TextView(ctx).apply {
            text = icon
            textSize = 16f
        })

        inner.addView(TextView(ctx).apply {
            text = "  $label"
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        })

        card.addView(inner)
        container.addView(card)
    }

    private enum class ChipStyle { FILLED, OUTLINED, HEART }

    private fun makeChip(text: String, style: ChipStyle): Chip {
        val ctx = requireContext()
        return Chip(ctx).apply {
            this.text = text
            isClickable = false
            isCheckable = false
            textSize = 12f
            when (style) {
                ChipStyle.FILLED -> {
                    setChipBackgroundColorResource(R.color.accent_soft)
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent))
                }
                ChipStyle.OUTLINED -> {
                    setChipBackgroundColorResource(android.R.color.transparent)
                    chipStrokeWidth = dpToPx(1).toFloat()
                    setChipStrokeColorResource(R.color.outline)
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                }
                ChipStyle.HEART -> {
                    setChipBackgroundColorResource(R.color.love_soft)
                    setTextColor(ContextCompat.getColor(ctx, R.color.love))
                }
            }
        }
    }

    private fun loadDrawableByName(name: String): Bitmap? {
        return try {
            val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
            if (resId != 0) BitmapFactory.decodeResource(resources, resId) else null
        } catch (e: Exception) { null }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun animateEntrance(view: View) {
        val targets = listOf<View>(
            view.findViewById(R.id.card_hero),
            view.findViewById(R.id.card_bio),
            view.findViewById(R.id.card_details),
            view.findViewById(R.id.card_interests),
            view.findViewById(R.id.layout_actions)
        )
        targets.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 60f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((i * 80).toLong())
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    companion object {
        private const val ARG_PROFILE = "dating_profile"

        fun newInstance(profile: DatingProfile): ProfileFragment {
            Log.d("PROFILE", "companion, profile=$profile")
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    // Serialise to JSON string so no Parcelable needed
                    putString(ARG_PROFILE, DatingProfile.toJson(profile).toString())
                }
            }
        }
    }
}


