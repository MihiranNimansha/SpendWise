package com.spendwise.app.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentRegisterBinding
import com.spendwise.app.utils.snackbar
import com.spendwise.app.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        (activity as? MainActivity)?.hideNavigation()

        setupViews()
        setupListeners()
        setupObservers()
    }

    private fun setupViews() {
        val currencies = arrayOf("LKR", "USD", "EUR", "GBP")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.atvCurrency.setAdapter(adapter)
        binding.atvCurrency.setText("LKR", false)
        updateCurrencyPrefix("LKR")
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.atvCurrency.setOnItemClickListener { parent, _, position, _ ->
            val currency = parent.getItemAtPosition(position).toString()
            updateCurrencyPrefix(currency)
        }

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrength(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                val email = binding.etEmail.text.toString().trim()
                val pass = binding.etPassword.text.toString().trim()
                val name = binding.etName.text.toString().trim()
                val currency = binding.atvCurrency.text.toString()
                val budget = binding.etBudget.text.toString().toDoubleOrNull() ?: 0.0
                
                authViewModel.register(email, pass, name, currency, budget)
            }
        }
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.btnRegister.text = if (isLoading) "" else getString(R.string.register)
        }

        authViewModel.authState.observe(viewLifecycleOwner) { event ->
            val result = event.getContentIfNotHandled() ?: return@observe
            result.onSuccess { user ->
                val bundle = Bundle().apply {
                    putString("registeredEmail", user.email)
                }
                findNavController().navigate(R.id.action_register_to_login, bundle)
            }.onFailure { error ->
                binding.root.snackbar(error.message ?: "Registration failed")
            }
        }
    }

    private fun updateCurrencyPrefix(currency: String) {
        val prefix = when(currency) {
            "LKR" -> "Rs. "
            "USD" -> "$ "
            "EUR" -> "€ "
            "GBP" -> "£ "
            else -> ""
        }
        binding.tilBudget.prefixText = prefix
    }

    private fun updatePasswordStrength(password: String) {
        val indicator = binding.progressPasswordStrength
        if (password.isEmpty()) {
            indicator.progress = 0
            return
        }

        var strengthScore = 0
        if (password.length >= 6) strengthScore++
        if (password.length >= 10) strengthScore++
        if (password.any { it.isDigit() }) strengthScore++
        if (password.any { it.isUpperCase() }) strengthScore++
        if (password.any { !it.isLetterOrDigit() }) strengthScore++

        when (strengthScore) {
            0, 1 -> {
                indicator.progress = 33
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.colorError))
            }
            2, 3 -> {
                indicator.progress = 66
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.colorWarning))
            }
            else -> {
                indicator.progress = 100
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.colorSuccess))
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_empty_field)
            isValid = false
        } else binding.tilName.error = null

        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else binding.tilEmail.error = null

        val pass = binding.etPassword.text.toString()
        if (pass.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        val confirmPass = binding.etConfirmPassword.text.toString()
        if (confirmPass != pass) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else binding.tilConfirmPassword.error = null

        val budgetStr = binding.etBudget.text.toString()
        if (budgetStr.isEmpty() || budgetStr.toFloatOrNull() == null || budgetStr.toFloat() <= 0f) {
            binding.tilBudget.error = getString(R.string.error_invalid_amount)
            isValid = false
        } else binding.tilBudget.error = null

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
