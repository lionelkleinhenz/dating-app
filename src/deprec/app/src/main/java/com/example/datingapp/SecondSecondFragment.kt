package com.example.datingapp

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.datingapp.databinding.FragmentSecondSecondBinding  // ← fixed

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondSecondFragment : Fragment() {

    private var _binding: FragmentSecondSecondBinding? = null  // ← fixed
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondSecondBinding.inflate(inflater, container, false)  // ← fixed
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.parent as? View)?.setBackgroundColor(Color.WHITE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}