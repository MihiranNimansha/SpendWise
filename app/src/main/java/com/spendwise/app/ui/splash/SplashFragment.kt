package com.spendwise.app.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.spendwise.app.R
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lottieAnimationView.playAnimation()

        lifecycleScope.launch {
            delay(2500)
            if (isAdded) {
                navigateNext()
            }
        }
    }

    private fun navigateNext() {
        val prefs = UserPreferences(requireContext())
        
        if (!prefs.isOnboardingShown) {
            findNavController().navigate(R.id.action_splash_to_onboarding)
            return
        }

        // Use the new isLoggedIn flag to ensure users see the new Auth flow at least once
        val destination = if (prefs.isLoggedIn && prefs.userEmail.isNotEmpty()) {
            R.id.action_splash_to_dashboard
        } else {
            R.id.action_splash_to_login
        }
        findNavController().navigate(destination)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
