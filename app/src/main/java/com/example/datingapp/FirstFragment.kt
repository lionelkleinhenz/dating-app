package com.example.datingapp

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.navigation.fragment.findNavController
import com.example.datingapp.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(Color.WHITE)
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {

            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            swipeRight()
                        } else {
                            swipeLeft()
                        }
                        return true
                    }
                }
                return false
            }
        })

        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun swipeLeft() {
        animateSwipe(toRight = false)
    }

    private fun swipeRight() {
        animateSwipe(toRight = true)
    }

    private fun animateSwipe(toRight: Boolean) {
        val color = if (toRight) Color.parseColor("#71c174") else Color.parseColor("#e44e44")
        val direction = if (toRight) 1f else -1f
        val screenWidth = requireView().width.toFloat()

        // Color the container behind the fragment (revealed as it slides away)
        (requireView().parent as? View)?.setBackgroundColor(color)

        requireView().animate()
            .translationX(direction * screenWidth)
            .setDuration(250)
            .withEndAction {
                /*if (isAdded) {
                    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                }*/
                requireView().translationX = 0f
                (requireView().parent as? View)?.setBackgroundColor(Color.WHITE)

            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}