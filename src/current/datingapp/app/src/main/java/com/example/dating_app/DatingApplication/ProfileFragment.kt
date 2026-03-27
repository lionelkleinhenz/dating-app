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
import com.chaquo.python.PyObject
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.flexbox.FlexboxLayout
import org.json.JSONObject
import kotlinx.coroutines.coroutineScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

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
    private lateinit var module: PyObject
    private lateinit var pyClass: PyObject
    private lateinit var instance: PyObject

    private var currentIndex = 1 // ik it's not ideal but it improves code readability

    private var rating = 0


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialization of the python integration
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }
        val py = Python.getInstance()
        module = py.getModule("algorithm_script")
        module.callAttr("set_folder_path", "${requireContext().filesDir.absolutePath}/raw" )
        pyClass = module.get("DatingApp")!!
        instance = pyClass.call()
        instance.callAttr("ensure_loaded")

        // gets the profile from the json
        profile = arguments?.getString(ARG_PROFILE)
            ?.let { DatingProfile.fromJson(JSONObject(it)) }
            ?: return  // safety exit if somehow null



        val overlay = view.findViewById<SwipeOverlayView>(R.id.swipeOverlay)

        // The overlay handles all gesture detection
        overlay.listener = object : SwipeListener {
            override fun onLiked() {
                val result = instance.callAttr("like")  // like() should return the result directly
                // Log.d("PYTHON", result.toList().toString())
                handleSwipeResult(result, view)
            }
            override fun onDisliked() {
                val result = instance.callAttr("dislike")  // same
                // Log.d("PYTHON", result.toList().toString())
                handleSwipeResult(result, view)
            }
        }


        // the two action buttons that replace the swipe functionalities
        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            // Log.d("PYTHON", "like clicked")
            val result = instance.callAttr("like") ?: return@setOnClickListener
            // Log.d("PYTHON", "result null: ${result == null}")
            handleSwipeResult(result, view)
        }
        view.findViewById<View>(R.id.btn_pass).setOnClickListener {
            // Log.d("PYTHON", "dislike clicked")
            val result = instance.callAttr("dislike") ?: return@setOnClickListener
            // Log.d("PYTHON", "result null: ${result == null}")
            handleSwipeResult(result, view)
        }

        // load the initial profile
        bindProfile(view)
        animateEntrance(view)
    }

    private fun handleSwipeResult(result: PyObject?, view: View) {
        // first checks if it needs to display any of the dialog boxes
        if (currentIndex == 5) {
            showAlgorithmExplanationDialog()
        } else if (currentIndex == 50) {
            val ideal = instance.callAttr("get_ideal")
            showIdealProfileDialog(ideal, view)
        }

        if (result == null) return

        try {
            val list = result.asList()
            if (list.size < 2) return
            // parses the values returned by the python function
            rating = list[0].toDouble().toInt()
            val userId = list[1].toString()

            // change to a new profile
            val newProfile = MainActivity.loadProfileFromJson(requireContext(), userId.toInt())
            profile = newProfile
            currentIndex++
            bindProfile(view)
            animateEntrance(view)
        } catch (e: Exception) {
            Log.e("SWIPE", "Error handling result: ${e.message}")
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        bindProfile(view)
//        animateEntrance(view)
//    }

    private fun bindProfile(view: View) {
        // ── furry image ────────────────────────────────────────────────────────
        val heroImage = view.findViewById<ImageView>(R.id.iv_hero)
        val resId = resources.getIdentifier(profile.image, "drawable", requireContext().packageName)
        // Log.d("IMAGE", resId.toString())
        if (resId != 0) {
            heroImage.setImageResource(resId)
        } else {
            heroImage.setImageResource(R.drawable.placeholder_profile)
        }

        // ── matching score ───────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_compatibility).text =
            "${rating}% Match"

        // ── name / age / verified ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_name_age).text =
            "${profile.name}, ${profile.age}"
        view.findViewById<View>(R.id.iv_verified).visibility = View.VISIBLE

        // ── species & location ────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_species).text = profile.species
        view.findViewById<TextView>(R.id.tv_location).text =
            "${profile.location}"

        // ── last active ──────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_last_active).text =
            "Active right now"

        // ── about me ──────────────────────────────────────────────────────────────
        view.findViewById<TextView>(R.id.tv_bio).text = profile.bio

        // ── quick-facts ─────────────────────────────────────────────────
        val factsContainer = view.findViewById<FlexboxLayout>(R.id.ll_quick_facts)
        factsContainer.removeAllViews()
        val facts = listOf(
            "⚧" to profile.gender,
            "\uD83D\uDDE8\uFE0F" to profile.pronouns,
            "\uD83D\uDC3E" to profile.furColor,
            "\uD83C\uDF00" to profile.tailType,
        )
        facts.forEach { (icon, label) ->
            addFactCard(factsContainer, icon, label)
        }

        // ── activities ──────────────────────────────────────────────────
        val activitiesGroup = view.findViewById<ChipGroup>(R.id.chip_group_activities)
        activitiesGroup.removeAllViews()
        profile.activities.forEach { activity ->
            activitiesGroup.addView(makeChip(activity, ChipStyle.FILLED))
        }

        // ── personality traits ────────────────────────────────────────────────
        val personalityGroup = view.findViewById<ChipGroup>(R.id.chip_group_personality)
        personalityGroup.removeAllViews()
        profile.personalityTraits.forEach { trait ->
            personalityGroup.addView(makeChip(trait, ChipStyle.OUTLINED))
        }

    }

    // ── Helpers ───────────────────────────────────────────────────────────────


    // small facts in the quick fact section
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

    // small chips in the activities and personality section
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

//    private fun loadDrawableByName(name: String): Bitmap? {
//        return try {
//            val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
//            if (resId != 0) BitmapFactory.decodeResource(resources, resId) else null
//        } catch (e: Exception) { null }
//    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()


    // animation before displaying the new profile
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


    private fun showIdealProfileDialog(ideal: PyObject?, view: View) {
        if (ideal == null) return

        val ctx = requireContext()
        val dialog = android.app.Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_ideal_profile)

        // populate fields
        dialog.findViewById<TextView>(R.id.tv_ideal_species).text =
            ideal.callAttr("get", "species").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_gender).text =
            ideal.callAttr("get", "gender").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_age).text =
            ideal.callAttr("get", "age").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_location).text =
            ideal.callAttr("get", "location").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_fur).text =
            ideal.callAttr("get", "fur_color").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_tail).text =
            ideal.callAttr("get", "tail_type").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_activity).text =
            ideal.callAttr("get", "activities").toString()
        dialog.findViewById<TextView>(R.id.tv_ideal_trait).text =
            ideal.callAttr("get", "personality_traits").toString()

        dialog.findViewById<View>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAlgorithmExplanationDialog() {

        val ctx = requireContext()
        val dialog = android.app.Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_algorithm_explanation)

        dialog.findViewById<View>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        private const val ARG_PROFILE = "dating_profile"

        fun newInstance(profile: DatingProfile): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    // Serialise to JSON string so no Parcelable needed
                    putString(ARG_PROFILE, DatingProfile.toJson(profile).toString())
                }
            }
        }
    }
}


