package com.spendwise.app.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentProfileBinding
import com.spendwise.app.utils.CsvExporter
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.snackbar
import com.spendwise.app.viewmodel.AuthViewModel
import com.spendwise.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private lateinit var userPreferences: UserPreferences

    // LiveData only holds a value while observed, so keep the latest list for the export button
    private var latestExpenses: List<Expense> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        (activity as? MainActivity)?.showNavigation()

        setupUI()
        setupListeners()
        observeData()
    }

    private fun setupUI() {
        val name = userPreferences.userName
        val email = userPreferences.userEmail
        
        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.etFullName.setText(name)
        binding.etEmail.setText(email)
        
        binding.tvAvatarInitial.text = name.firstOrNull()?.uppercase() ?: "U"
        
        binding.switchDarkMode.isChecked = userPreferences.isDarkMode

        val sdf = SimpleDateFormat("'Member since' MMMM yyyy", Locale.getDefault())
        // For demo, using current time if not stored. Ideally store user creation date in Room.
        binding.tvMemberSince.text = sdf.format(Date()) 
    }

    private fun setupListeners() {
        binding.toolbarProfile.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etFullName.text.toString().trim()
            if (newName.isEmpty()) {
                binding.tilFullName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            authViewModel.updateProfile(newName)
            userPreferences.userName = newName
            binding.tvProfileName.text = newName
            binding.tvAvatarInitial.text = newName.firstOrNull()?.uppercase() ?: "U"
            (activity as? MainActivity)?.updateNavHeader()
            binding.root.snackbar("Profile updated! ✅")
        }

        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrength(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnChangePassword.setOnClickListener {
            val current = binding.etCurrentPassword.text.toString()
            val newPass = binding.etNewPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (current.isEmpty() || newPass.length < 6 || newPass != confirm) {
                binding.root.snackbar("Please check password requirements")
                return@setOnClickListener
            }

            authViewModel.changePassword(current, newPass).observe(viewLifecycleOwner) { result ->
                result.onSuccess {
                    binding.root.snackbar("Password changed! 🔒")
                    binding.etCurrentPassword.text?.clear()
                    binding.etNewPassword.text?.clear()
                    binding.etConfirmPassword.text?.clear()
                }.onFailure {
                    binding.root.snackbar(it.message ?: "Failed to change password")
                }
            }
        }

        binding.rowExportData.setOnClickListener {
            val expenses = latestExpenses
            if (expenses.isEmpty()) {
                binding.root.snackbar("No expenses to export")
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export to CSV")
                .setMessage("Export ${expenses.size} expenses to a CSV file?")
                .setPositiveButton("Export") { _, _ ->
                    val uri = CsvExporter.exportExpenses(requireContext(), expenses, userPreferences.preferredCurrency)
                    if (uri != null) {
                        CsvExporter.shareCSV(requireContext(), uri)
                    } else {
                        binding.root.snackbar("Export failed")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            userPreferences.isDarkMode = isChecked
            // setDefaultNightMode recreates the activity so the new theme is applied immediately
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    (requireActivity() as MainActivity).resetSession()
                    findNavController().navigate(
                        R.id.action_profile_to_login,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Delete Account")
                .setMessage("This will permanently delete your account and ALL your data. This cannot be undone.")
                .setPositiveButton("Delete Forever") { _, _ ->
                    // Wait until the data is really deleted before leaving, then wipe the session
                    authViewModel.deleteAccount {
                        (activity as? MainActivity)?.resetSession()
                        if (isAdded) {
                            findNavController().navigate(
                                R.id.action_profile_to_login,
                                null,
                                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                            )
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeData() {
        expenseViewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            if (expenses != null) {
                latestExpenses = expenses
                binding.tvTotalExpenses.text = expenses.size.toString()
                val total = expenses.sumOf { it.amount }
                binding.tvTotalSpent.text = CurrencyFormatter.formatShort(total, userPreferences.preferredCurrency)
                
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val activeDays = expenses.map { sdf.format(Date(it.date)) }.distinct().size
                binding.tvActiveDays.text = activeDays.toString()
            }
        }
    }

    private fun updatePasswordStrength(password: String) {
        var strength = 0
        if (password.length >= 6) strength += 20
        if (password.any { it.isDigit() }) strength += 20
        if (password.any { it.isUpperCase() }) strength += 20
        if (password.any { it.isLowerCase() }) strength += 20
        if (password.any { !it.isLetterOrDigit() }) strength += 20
        
        binding.progressPasswordStrength.progress = strength
        val color = when {
            strength < 40 -> R.color.colorError
            strength < 80 -> R.color.colorWarning
            else -> R.color.colorSuccess
        }
        binding.progressPasswordStrength.setIndicatorColor(ContextCompat.getColor(requireContext(), color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
