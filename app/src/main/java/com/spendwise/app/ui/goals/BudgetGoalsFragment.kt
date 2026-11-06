package com.spendwise.app.ui.goals

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.local.entity.BudgetGoal
import com.spendwise.app.data.model.CategoryGoalItem
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentBudgetGoalsBinding
import com.spendwise.app.utils.Constants
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.snackbar
import com.spendwise.app.utils.toast
import com.spendwise.app.viewmodel.BudgetViewModel
import com.spendwise.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class BudgetGoalsFragment : Fragment() {

    private var _binding: FragmentBudgetGoalsBinding? = null
    private val binding get() = _binding!!

    private val budgetViewModel: BudgetViewModel by activityViewModels()
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    
    private lateinit var adapter: CategoryGoalAdapter
    private lateinit var userPreferences: UserPreferences
    private lateinit var currentUserId: String

    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    private var cachedTotalSpentThisMonth = 0.0
    private var cachedBudgetGoals = listOf<BudgetGoal>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        currentUserId = userPreferences.userEmail

        (activity as? MainActivity)?.showNavigation()

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Set subtitle to current month
        val subtitleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.toolbarGoals.subtitle = subtitleFormat.format(Date())

        binding.toolbarGoals.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_edit_budget) {
                showEditOverallBudgetDialog()
                true
            } else false
        }

        binding.rvCategoryGoals.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryGoalAdapter(userPreferences.preferredCurrency)
        
        adapter.onEditClick = { goalItem ->
            showAddGoalBottomSheet(true, goalItem)
        }
        
        binding.rvCategoryGoals.adapter = adapter

        binding.fabAddGoal.setOnClickListener {
            showAddGoalBottomSheet(false, null)
        }
    }

    private fun observeData() {
        val currency = userPreferences.preferredCurrency
        val monthBudget = userPreferences.monthlyBudget

        binding.tvOverallBudget.text = CurrencyFormatter.format(monthBudget.toDouble(), currency)

        expenseViewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { spent ->
            val totalSpent = spent ?: 0.0
            cachedTotalSpentThisMonth = totalSpent
            updateOverallBudgetCard(totalSpent)
        }

        budgetViewModel.allGoals.observe(viewLifecycleOwner) { goals ->
            cachedBudgetGoals = goals.filter { it.month == currentMonth && it.year == currentYear }
            rebuildCategoryList()
        }

        expenseViewModel.categoryTotalsThisMonth.observe(viewLifecycleOwner) { totalsMap ->
            rebuildCategoryList(totalsMap)
        }
    }

    private fun updateOverallBudgetCard(totalSpent: Double) {
        val monthBudget = userPreferences.monthlyBudget.toDouble()
        val remaining = monthBudget - totalSpent
        val currency = userPreferences.preferredCurrency

        binding.tvOverallSpent.text = getString(R.string.goals_overall_spent, CurrencyFormatter.format(totalSpent, currency))
        binding.tvOverallRemaining.text = getString(R.string.goals_overall_remaining, CurrencyFormatter.format(remaining, currency))
        
        var percent = 0
        if (monthBudget > 0) {
            percent = ((totalSpent / monthBudget) * 100).roundToInt()
        }
        
        binding.tvOverallPercent.text = getString(R.string.goals_percent_used, percent)

        val targetProgress = if (percent > 100) 100 else percent
        val statusColor = when {
            percent < 60 -> Color.parseColor("#4CAF50") // Green
            percent in 60..90 -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Red
        }

        binding.progressOverall.setIndicatorColor(statusColor)

        ObjectAnimator.ofInt(binding.progressOverall, "progress", 0, targetProgress).apply {
            duration = 1000
            start()
        }
    }

    private fun rebuildCategoryList(categoryTotals: Map<String, Double> = expenseViewModel.categoryTotalsThisMonth.value ?: emptyMap()) {
        val goalsMap = cachedBudgetGoals.associateBy { it.category }
        val builtList = mutableListOf<CategoryGoalItem>()

        for (categoryName in Constants.CATEGORIES) {
            val spent = categoryTotals[categoryName] ?: 0.0
            val matchingGoal = goalsMap[categoryName]

            builtList.add(
                CategoryGoalItem(
                    category = categoryName,
                    monthlyLimit = matchingGoal?.monthlyLimit ?: 0.0,
                    spent = spent,
                    hasGoal = matchingGoal != null,
                    goalId = matchingGoal?.id ?: -1,
                    month = currentMonth,
                    year = currentYear
                )
            )
        }

        // Sort built list conditionally: Goals on top, No goals at bottom. Inside, sort Alphabetical
        builtList.sortByDescending { it.hasGoal }
        adapter.submitList(builtList)
    }

    private fun showEditOverallBudgetDialog() {
        val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox)
        val inputEdit = TextInputEditText(inputLayout.context)
        inputEdit.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputEdit.setText(userPreferences.monthlyBudget.toString())
        inputLayout.addView(inputEdit)
        
        val container = FrameLayout(requireContext())
        container.setPadding(48, 24, 48, 0)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Monthly Budget")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTotal = inputEdit.text.toString().toDoubleOrNull()
                if (newTotal != null && newTotal > 0.0) {
                    userPreferences.monthlyBudget = newTotal
                    binding.tvOverallBudget.text = CurrencyFormatter.format(newTotal.toDouble(), userPreferences.preferredCurrency)
                    updateOverallBudgetCard(cachedTotalSpentThisMonth)
                    binding.root.snackbar("Budget updated securely")
                } else {
                    requireContext().toast("Please enter a valid amount > 0")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddGoalBottomSheet(isEditMode: Boolean, existingGoalItem: CategoryGoalItem?) {
        val unassignedCategories = Constants.CATEGORIES.filter { cat ->
            cachedBudgetGoals.none { it.category == cat }
        }

        if (!isEditMode && unassignedCategories.isEmpty()) {
            binding.root.snackbar("All categories already have goals set.")
            return
        }

        val sheet = AddGoalBottomSheet(
            availableCategories = unassignedCategories,
            isEditMode = isEditMode,
            initialCategory = existingGoalItem?.category ?: "",
            initialLimit = existingGoalItem?.monthlyLimit ?: 0.0,
            initialGoalId = existingGoalItem?.goalId ?: -1,
            currency = userPreferences.preferredCurrency
        )

        sheet.onGoalSaved = { category, limit ->
            val newGoal = BudgetGoal(
                category = category,
                monthlyLimit = limit,
                month = currentMonth,
                year = currentYear,
                userId = currentUserId
            )
            // Handle Insert vs Update
            if (isEditMode && existingGoalItem?.goalId != -1) {
                budgetViewModel.updateGoal(newGoal.copy(id = existingGoalItem!!.goalId))
            } else {
                budgetViewModel.insertGoal(newGoal)
            }
            binding.root.snackbar("Goal structured for $category! 🎯")
        }

        sheet.onGoalDeleted = { goalId ->
            if (goalId != -1) {
                val goalToDelete = cachedBudgetGoals.find { it.id == goalId }
                if (goalToDelete != null) budgetViewModel.deleteGoal(goalToDelete)
            }
        }

        sheet.show(parentFragmentManager, "AddGoalSheet")
    }

    override fun onResume() {
        super.onResume()
        expenseViewModel.refreshMonth()
        budgetViewModel.refreshMonth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
