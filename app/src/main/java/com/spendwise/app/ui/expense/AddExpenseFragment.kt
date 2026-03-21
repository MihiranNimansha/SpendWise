package com.spendwise.app.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentAddExpenseBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.snackbar
import com.spendwise.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferences

    private var selectedDate: Long = System.currentTimeMillis()
    private var currentUserId: String = ""
    private var isEditMode = false
    private var existingExpense: Expense? = null
    private var expenseId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPrefs = UserPreferences(requireContext())
        currentUserId = userPrefs.userEmail
        
        expenseId = arguments?.getInt("expenseId") ?: -1

        (activity as? MainActivity)?.hideNavigation()

        setupViews()
        setupListeners()
        checkEditMode()
    }

    private fun setupViews() {
        // Categories
        val categories = arrayOf("Food", "Transport", "Fun", "Health", "Bills", "Shopping", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)

        updateDateDisplay()
        binding.tvCurrencySymbol.text = CurrencyFormatter.getSymbol(userPrefs.preferredCurrency)
    }

    private fun setupListeners() {
        binding.toolbarAddExpense.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.etDate.setOnClickListener { showDatePicker() }

        binding.btnSaveExpense.setOnClickListener {
            if (validateInputs()) {
                saveExpense()
            }
        }

        binding.btnDeleteExpense.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun checkEditMode() {
        if (expenseId != -1) {
            isEditMode = true
            binding.toolbarAddExpense.title = "Edit Expense"
            binding.btnDeleteExpense.visibility = View.VISIBLE
            expenseViewModel.getExpenseById(expenseId).observe(viewLifecycleOwner) { expense ->
                if (expense != null) {
                    existingExpense = expense
                    fillFields(expense)
                }
            }
        }
    }

    private fun fillFields(expense: Expense) {
        binding.etTitle.setText(expense.title)
        binding.etAmount.setText(expense.amount.toString())
        binding.actvCategory.setText(expense.category, false)
        binding.etNote.setText(expense.note)
        selectedDate = expense.date
        updateDateDisplay()
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(selectedDate)
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedDate = it
            updateDateDisplay()
        }
        picker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(Date(selectedDate)))
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.tilTitle.error = "Title is required"
            isValid = false
        } else binding.tilTitle.error = null

        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.root.snackbar("Enter a valid amount")
            isValid = false
        }

        if (binding.actvCategory.text.toString().isEmpty()) {
            binding.tilCategory.error = "Category is required"
            isValid = false
        } else binding.tilCategory.error = null

        return isValid
    }

    private fun saveExpense() {
        val title = binding.etTitle.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDouble()
        val category = binding.actvCategory.text.toString()
        val note = binding.etNote.text.toString().trim()

        val expense = if (isEditMode && existingExpense != null) {
            existingExpense!!.copy(
                title = title,
                amount = amount,
                category = category,
                note = note,
                date = selectedDate
            )
        } else {
            Expense(
                title = title,
                amount = amount,
                category = category,
                note = note,
                date = selectedDate,
                userId = currentUserId
            )
        }

        if (isEditMode) expenseViewModel.updateExpense(expense)
        else expenseViewModel.insertExpense(expense)

        binding.root.snackbar("Expense saved successfully")
        findNavController().navigateUp()
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                existingExpense?.let {
                    expenseViewModel.deleteExpense(it)
                    binding.root.snackbar("Expense deleted")
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
