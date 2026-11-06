package com.spendwise.app.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentLoginBinding
import com.spendwise.app.utils.snackbar
import com.spendwise.app.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var userPreferences: UserPreferences
    private var failedAttempts = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        (activity as? MainActivity)?.hideNavigation()

        handleArguments()
        setupListeners()
        setupObservers()
        animateEntry()
    }

    private fun handleArguments() {
        val registeredEmail = arguments?.getString("registeredEmail")
        if (!registeredEmail.isNullOrEmpty()) {
            binding.etEmail.setText(registeredEmail)
            binding.etPassword.requestFocus()
            binding.root.snackbar("Account created! Please sign in 🎉")
        }
    }

    private fun setupListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                authViewModel.signIn(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            binding.root.snackbar("Google Sign-In is disabled. Please use email/password.")
        }
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignIn.isEnabled = !isLoading
            binding.btnSignIn.text = if (isLoading) "Authenticating..." else getString(R.string.sign_in)
        }

        authViewModel.authState.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            result.onSuccess { user ->
                failedAttempts = 0
                userPreferences.userEmail = user.email
                userPreferences.userName = user.name
                userPreferences.preferredCurrency = user.preferredCurrency
                userPreferences.monthlyBudget = user.monthlyBudget
                userPreferences.isLoggedIn = true
                
                findNavController().navigate(
                    R.id.action_login_to_dashboard,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
            }.onFailure { error ->
                failedAttempts++
                binding.root.snackbar(error.message ?: "Authentication failed")
                if (failedAttempts >= 5) {
                    lockLogin()
                }
            }
        }
    }

    private fun lockLogin() {
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = "Too many attempts. Wait 30s"
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = getString(R.string.sign_in)
                failedAttempts = 0
            }
        }, 30000)
    }

    private fun validateInputs(email: String, pass: String): Boolean {
        var isValid = true
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (pass.isEmpty() || pass.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        return isValid
    }

    private fun animateEntry() {
        binding.loginContainer.alpha = 0f
        binding.loginContainer.translationY = 50f
        ViewCompat.animate(binding.loginContainer)
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
