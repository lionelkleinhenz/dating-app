package com.example.dating_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.flexbox.FlexboxLayout

class ProfileFragment : Fragment() {

    private lateinit var profile: DatingProfile

    companion object {
        private const val ARG_PROFILE = "profile_json"

        fun newInstance(profile: DatingProfile): ProfileFragment {
            // We'll pass data via the activity; keep it simple
            val fragment = ProfileFragment()
            fragment.profile = profile
            return fragment
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
    }

    private fun bindProfile(view: View) {
        // ── Hero image ────────────────────────────────────────────────────────
        val heroImage = view.findViewById<ImageView>(R.id.iv_hero)
        loadDrawableByName(profile.img)?.let { heroImage.setImageBitmap(it) }
            ?: heroImage.setImageResource(R.drawable.pfp_1)

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
}
