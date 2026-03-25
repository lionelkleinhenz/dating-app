package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlin.math.abs

class ProfileFragment : Fragment() {

    private lateinit var profile: DatingProfile

    companion object {
        fun newInstance(profile: DatingProfile): ProfileFragment {
            return ProfileFragment().also { it.profile = profile }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProfile(view)
        animateEntrance(view)
        setupSwipeNavigation(view)
    }

    private fun setupSwipeNavigation(view: View) {
        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val startEvent = e1 ?: return false
                    val diffX = e2.x - startEvent.x
                    val diffY = e2.y - startEvent.y

                    if (abs(diffX) > abs(diffY) &&
                        abs(diffX) > SWIPE_THRESHOLD &&
                        abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                    ) {
                        if (diffX < 0) {
                            // Swipe left → next screen
                            findNavController().navigate(R.id.action_ProfileFragment_to_SecondFragment)
                        } else {
                            // Swipe right → back
                            findNavController().popBackStack()
                        }
                        return true
                    }
                    return false
                }
            })

        view.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun bindProfile(view: View) {
        val ctx = requireContext()

        // ── Hero image ────────────────────────────────────────────────────────
        val heroImage = view.findViewById<ImageView>(R.id.iv_hero)
        loadDrawableByName(profile.img)?.let { heroImage.setImageBitmap(it) }
            ?: heroImage.setImageResource(R.drawable.placeholder_profile)

        // ── Compatibility badge ───────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_compatibility).text =
            getString(R.string.compatibility_score, profile.compatibilityScore)

        // ── Name / age / verified ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_name_age).text =
            getString(R.string.name_age, profile.name, profile.age)
        view.findViewById<View>(R.id.iv_verified).visibility =
            if (profile.verified) View.VISIBLE else View.GONE

        // ── Occupation & location ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_occupation).text = profile.occupation
        view.findViewById<TextView>(R.id.tv_location).text =
            getString(R.string.location_distance, profile.location, profile.distanceKm)

        // ── Last active ──────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_last_active).text =
            getString(R.string.last_active, profile.lastActive.lowercase())

        // ── Bio ──────────────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_bio).text = profile.bio

        // ── Quick-fact pills ─────────────────────────────────────────────────
        val factsContainer = view.findViewById<FlexboxLayout>(R.id.ll_quick_facts)
        val facts = listOf(
            "📏" to profile.heightFormatted,
            "👁" to profile.eyeColor,
            "💇" to profile.hairColor,
            "♑" to profile.zodiac,
            "💞" to profile.relationshipGoal,
            "🐾" to if (profile.hasPets) profile.pets.joinToString() else "No pets"
        )
        facts.forEach { (icon, label) -> addFactCard(factsContainer, icon, label) }

        // ── Interests chips ──────────────────────────────────────────────────
        val interestsGroup = view.findViewById<ChipGroup>(R.id.chip_group_interests)
        profile.interests.forEach { interest ->
            interestsGroup.addView(makeChip(interest, ChipStyle.FILLED))
        }

        // ── Personality chips ────────────────────────────────────────────────
        val personalityGroup = view.findViewById<ChipGroup>(R.id.chip_group_personality)
        profile.personalityTags.forEach { tag ->
            personalityGroup.addView(makeChip(tag, ChipStyle.OUTLINED))
        }

        // ── Languages ────────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_languages).text =
            profile.languages.joinToString("  ·  ")

        // ── Education ────────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_education).text = profile.education

        // ── Lifestyle row ─────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_diet).text = profile.diet
        view.findViewById<TextView>(R.id.tv_drinking).text = profile.drinking
        view.findViewById<TextView>(R.id.tv_smoking).text = profile.smoking

        // ── Love languages ────────────────────────────────────────────────────
        val loveGroup = view.findViewById<ChipGroup>(R.id.chip_group_love)
        profile.loveLangauges.forEach { lang ->
            loveGroup.addView(makeChip("❤ $lang", ChipStyle.HEART))
        }

        // ── Gallery row ──────────────────────────────────────────────────────
        val galleryContainer = view.findViewById<LinearLayout>(R.id.ll_gallery)
        profile.gallery.forEach { name ->
            val iv = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(110), dpToPx(140)).also {
                    it.marginEnd = dpToPx(10)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_gallery_placeholder)
                loadDrawableByName(name)?.let { bmp -> setImageBitmap(bmp) }
                    ?: setImageResource(R.drawable.placeholder_gallery)
            }
            galleryContainer.addView(iv)
        }

        // ── Action buttons ────────────────────────────────────────────────────
        view.findViewById<View>(R.id.btn_pass).setOnClickListener {
            Toast.makeText(ctx, R.string.passed, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            Toast.makeText(ctx, R.string.liked, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_super_like).setOnClickListener {
            Toast.makeText(ctx, R.string.super_liked, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun addFactCard(container: FlexboxLayout, icon: String, label: String) {
        val ctx = requireContext()
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
        } catch (_: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun animateEntrance(view: View) {
        val targets = listOf(
            view.findViewById<View>(R.id.card_hero),
            view.findViewById<View>(R.id.card_bio),
            view.findViewById<View>(R.id.card_details),
            view.findViewById<View>(R.id.card_interests),
            view.findViewById<View>(R.id.card_lifestyle),
            view.findViewById<View>(R.id.card_gallery),
            view.findViewById<View>(R.id.layout_actions)
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
}