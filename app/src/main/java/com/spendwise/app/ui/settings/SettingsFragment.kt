package com.spendwise.app.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentSettingsBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.snackbar
import com.spendwise.app.utils.toast
import com.spendwise.app.viewmodel.AuthViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPrefs = UserPreferences(requireContext())

        (activity as? MainActivity)?.showNavigation()

        loadPreferences()
        setupListeners()
        playEntranceAnimations()
    }

    private fun loadPreferences() {
        binding.tvProfileName.text = userPrefs.userName.ifEmpty { "User" }
        binding.tvProfileEmail.text = userPrefs.userEmail.ifEmpty { "No email" }

        binding.tvCurrentCurrency.text = when (userPrefs.preferredCurrency) {
            "LKR" -> getString(R.string.pref_currency_lkr)
            "USD" -> getString(R.string.pref_currency_usd)
            "EUR" -> getString(R.string.pref_currency_eur)
            "GBP" -> getString(R.string.pref_currency_gbp)
            else -> userPrefs.preferredCurrency
        }

        binding.tvCurrentBudget.text = CurrencyFormatter.format(
            userPrefs.monthlyBudget.toDouble(), userPrefs.preferredCurrency
        )

        // Set state without triggering the listener
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = userPrefs.isDarkMode

        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = userPrefs.isNotificationsEnabled

        binding.tvAppVersion.text = getString(R.string.version_name)
    }

    private fun setupListeners() {
        val editProfileClick = View.OnClickListener { showEditProfileDialog() }
        binding.btnEditProfile.setOnClickListener(editProfileClick)
        binding.cardProfile.setOnClickListener(editProfileClick)

        binding.rowCurrency.setOnClickListener { showCurrencySelectorDialog() }

        binding.rowBudget.setOnClickListener { showMonthlyBudgetDialog() }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            userPrefs.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            userPrefs.isNotificationsEnabled = isChecked
            val msg = if (isChecked) "Notifications enabled 🔔" else "Notifications disabled"
            binding.root.snackbar(msg)
        }

        binding.rowChangePassword.setOnClickListener { showChangePasswordDialog() }

        binding.rowExportData.setOnClickListener { showExportDataDialog() }

        binding.btnSignOut.setOnClickListener { showSignOutDialog() }
    }

    private fun showEditProfileDialog() {
        val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox)
        val inputEdit = TextInputEditText(inputLayout.context)
        inputEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        inputEdit.setText(userPrefs.userName)
        inputEdit.hint = "Full Name"
        inputLayout.addView(inputEdit)
        
        val container = FrameLayout(requireContext())
        container.setPadding(48, 24, 48, 0)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_profile))
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = inputEdit.text.toString().trim()
                if (newName.isNotEmpty()) {
                    userPrefs.userName = newName
                    binding.tvProfileName.text = newName
                    binding.root.snackbar("Profile updated successfully")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCurrencySelectorDialog() {
        val currencies = arrayOf(
            getString(R.string.pref_currency_lkr),
            getString(R.string.pref_currency_usd),
            getString(R.string.pref_currency_eur),
            getString(R.string.pref_currency_gbp)
        )
        val currencyCodes = arrayOf("LKR", "USD", "EUR", "GBP")
        var checkedItem = currencyCodes.indexOf(userPrefs.preferredCurrency)
        if (checkedItem == -1) checkedItem = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencies, checkedItem) { dialog, which ->
                val selectedCode = currencyCodes[which]
                userPrefs.preferredCurrency = selectedCode
                binding.tvCurrentCurrency.text = currencies[which]
                
                // Refresh budget constraint format
                binding.tvCurrentBudget.text = CurrencyFormatter.format(
                    userPrefs.monthlyBudget.toDouble(), selectedCode
                )
                
                binding.root.snackbar("Currency updated to $selectedCode")
                dialog.dismiss()
            }
            .show()
    }

    private fun showMonthlyBudgetDialog() {
        val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox)
        val inputEdit = TextInputEditText(inputLayout.context)
        inputEdit.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputEdit.setText(userPrefs.monthlyBudget.toString())
        
        inputLayout.prefixText = when(userPrefs.preferredCurrency) {
            "LKR" -> "Rs. "
            "USD" -> "$ "
            "EUR" -> "€ "
            "GBP" -> "£ "
            else -> ""
        }
        
        inputLayout.addView(inputEdit)
        
        val container = FrameLayout(requireContext())
        container.setPadding(48, 24, 48, 0)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Monthly Budget")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTotal = inputEdit.text.toString().toDoubleOrNull()
                if (newTotal != null && newTotal > 0.0) {
                    userPrefs.monthlyBudget = newTotal
                    binding.tvCurrentBudget.text = CurrencyFormatter.format(
                        newTotal, userPrefs.preferredCurrency
                    )
                    binding.root.snackbar("Budget updated!")
                } else {
                    requireContext().toast("Please enter a valid amount > 0")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setMessage("Password reset is currently disabled in Room Auth mode.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showExportDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Data")
            .setMessage("Export feature generates a summary of all your expenses. This feature will be available in the next update.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage(getString(R.string.sign_out_confirm))
            .setPositiveButton("Sign Out") { _, _ ->
                // Clear preferences manually
                userPrefs.clearAll()
                
                // Navigate and clear stack
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playEntranceAnimations() {
        // Profile Card
        binding.cardProfile.translationY = -150f
        binding.cardProfile.alpha = 0f
        binding.cardProfile.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .start()

        // Rows
        val rows = listOf(
            binding.rowCurrency, binding.rowBudget, binding.rowDarkMode,
            binding.rowNotifications, binding.rowChangePassword, binding.rowExportData
        )
        
        for ((index, row) in rows.withIndex()) {
            row.alpha = 0f
            row.animate()
                .alpha(1f)
                .setStartDelay((index * 80).toLong())
                .setDuration(300)
                .start()
        }

        // Sign Out Button
        binding.btnSignOut.alpha = 0f
        binding.btnSignOut.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(400)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
