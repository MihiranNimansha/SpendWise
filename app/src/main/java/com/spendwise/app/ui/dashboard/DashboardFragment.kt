package com.spendwise.app.ui.dashboard

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentDashboardBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.hide
import com.spendwise.app.utils.show
import com.spendwise.app.viewmodel.BudgetViewModel
import com.spendwise.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private val budgetViewModel: BudgetViewModel by activityViewModels()

    private lateinit var userPreferences: UserPreferences
    private lateinit var recentExpenseAdapter: RecentExpenseAdapter

    private var panicShownThisSession = false
    private var isPanicActive = false
    private var tapCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        (activity as? MainActivity)?.showNavigation()

        binding.contentLayout.alpha = 0f

        setupGreetingAndDate()
        setupListeners()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupGreetingAndDate() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        val name = userPreferences.userName.ifEmpty { "User" }
        binding.tvGreeting.text = "$greeting, $name! 👋"

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = sdf.format(Date())
    }

    private fun setupListeners() {
        binding.btnAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addExpense)
        }

        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_expense_history)
        }

        binding.btnDismissPanic.setOnClickListener {
            dismissPanicAlert()
        }

        binding.btnViewExpenses.setOnClickListener {
            dismissPanicAlert()
            findNavController().navigate(R.id.action_dashboard_to_expense_history)
        }

        binding.tvGreeting.setOnClickListener {
            tapCount++
            if (tapCount == 5) {
                tapCount = 0
                seedDemoData()
            }
        }
    }

    private fun setupRecyclerView() {
        recentExpenseAdapter = RecentExpenseAdapter(userPreferences.preferredCurrency) { expense ->
            val bundle = Bundle().apply { putInt("expenseId", expense.id) }
            findNavController().navigate(R.id.action_dashboard_to_addExpense, bundle)
        }
        binding.rvRecentExpenses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentExpenseAdapter
        }
    }

    private fun setupObservers() {
        val currency = userPreferences.preferredCurrency

        // Budget Progress
        expenseViewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { spent ->
            val totalSpent = spent ?: 0.0
            
            budgetViewModel.totalMonthlyBudget.observe(viewLifecycleOwner) { budget ->
                val totalBudget = budget ?: userPreferences.monthlyBudget
                updateBudgetUI(totalSpent, totalBudget, currency)
            }
        }

        // Recent Expenses
        expenseViewModel.recentFiveExpenses.observe(viewLifecycleOwner) { expenses ->
            binding.contentLayout.animate().alpha(1f).setDuration(400).start()
            
            if (expenses.isNullOrEmpty()) {
                binding.rvRecentExpenses.hide()
                binding.layoutEmptyState.show()
            } else {
                binding.layoutEmptyState.hide()
                binding.rvRecentExpenses.show()
                binding.rvRecentExpenses.alpha = 0f
                binding.rvRecentExpenses.animate().alpha(1f).setDuration(500).start()
                
                recentExpenseAdapter.submitList(expenses)
            }
        }

        // Category Totals
        expenseViewModel.categoryTotalsThisMonth.observe(viewLifecycleOwner) { categoryMap ->
            updateCategoryCards(categoryMap ?: emptyMap(), currency)
        }

        // Panic Alert
        budgetViewModel.panicAlert.observe(viewLifecycleOwner) { isAlert ->
            if (isAlert == true && !panicShownThisSession && !isPanicActive) {
                showPanicAlert()
            }
        }
    }

    private fun updateBudgetUI(spent: Double, budget: Double, currency: String) {
        binding.tvTotalSpent.text = CurrencyFormatter.format(spent, currency)
        val left = budget - spent
        binding.tvBudgetLeft.text = if (left >= 0) CurrencyFormatter.format(left, currency) else "Overbudget"

        if (budget > 0) {
            val percent = ((spent / budget) * 100).toInt()
            binding.tvBudgetPercent.text = "${percent.coerceAtMost(100)}% of budget used"

            ObjectAnimator.ofInt(binding.progressBudget, "progress", 0, percent.coerceAtMost(100)).apply {
                duration = 1000
                start()
            }

            val colorRes = when {
                percent < 60 -> R.color.colorSuccess
                percent < 90 -> R.color.colorWarning
                else -> R.color.colorError
            }
            binding.progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), colorRes))
        }
    }

    private fun updateCategoryCards(categoryMap: Map<String, Double>, currency: String) {
        binding.layoutCategories.removeAllViews()
        val categories = listOf(
            Triple("Food", "🍔", "#E8F5E9"),
            Triple("Transport", "🚗", "#E3F2FD"),
            Triple("Fun", "🎉", "#FFF3E0"),
            Triple("Health", "💊", "#FFEBEE"),
            Triple("Bills", "⚡", "#F3E5F5"),
            Triple("Shopping", "🛍️", "#FCE4EC"),
            Triple("Other", "💰", "#ECEFF1")
        )

        categories.forEach { (name, emoji, colorHex) ->
            val amount = categoryMap[name] ?: 0.0
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_category_summary, binding.layoutCategories, false) as MaterialCardView

            cardView.setCardBackgroundColor(android.graphics.Color.parseColor(if (amount > 0) colorHex else "#F5F5F5"))
            if (amount == 0.0) cardView.alpha = 0.5f

            val tvEmoji = cardView.findViewById<TextView>(R.id.tvCatEmoji)
            val tvName = cardView.findViewById<TextView>(R.id.tvCatName)
            val tvAmount = cardView.findViewById<TextView>(R.id.tvCatAmount)

            tvEmoji.text = emoji
            tvName.text = name
            tvAmount.text = CurrencyFormatter.formatShort(amount, currency)

            binding.layoutCategories.addView(cardView)
        }
    }

    private fun showPanicAlert() {
        panicShownThisSession = true
        isPanicActive = true

        binding.layoutPanicAlert.show()
        binding.lottieWarning.playAnimation()

        // Sync Amount text
        val spent = expenseViewModel.totalSpentThisMonth.value ?: 0.0
        val budget = budgetViewModel.totalMonthlyBudget.value ?: userPreferences.monthlyBudget
        val currency = userPreferences.preferredCurrency
        binding.tvPanicAmount.text = "${CurrencyFormatter.format(spent, currency)} spent of ${CurrencyFormatter.format(budget, currency)} budget"

        // Pulse warning
        ObjectAnimator.ofFloat(binding.lottieWarning, "scaleX", 1.0f, 1.1f, 1.0f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofFloat(binding.lottieWarning, "scaleY", 1.0f, 1.1f, 1.0f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }

        // Vibrate
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 500, 200, 500)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        }
    }

    private fun dismissPanicAlert() {
        isPanicActive = false
        binding.layoutPanicAlert.hide()
        binding.lottieWarning.cancelAnimation()
    }

    private fun seedDemoData() {
        val userEmail = userPreferences.userEmail
        if (userEmail.isEmpty()) return
        
        val demoExpenses = listOf(
            Expense(title="Lunch at KFC", amount=850.0, category="Food", date=System.currentTimeMillis(), userId=userEmail, note="Team lunch"),
            Expense(title="Uber to Office", amount=320.0, category="Transport", date=System.currentTimeMillis()-86400000, userId=userEmail, note=""),
            Expense(title="Netflix", amount=1490.0, category="Fun", date=System.currentTimeMillis()-172800000, userId=userEmail, note=""),
            Expense(title="Pharmacy", amount=2200.0, category="Health", date=System.currentTimeMillis()-259200000, userId=userEmail, note=""),
            Expense(title="Electricity Bill", amount=4500.0, category="Bills", date=System.currentTimeMillis()-345600000, userId=userEmail, note=""),
            Expense(title="Grocery Shopping", amount=3200.0, category="Shopping", date=System.currentTimeMillis()-432000000, userId=userEmail, note=""),
            Expense(title="Coffee", amount=450.0, category="Food", date=System.currentTimeMillis()-518400000, userId=userEmail, note="")
        )
        demoExpenses.forEach { expenseViewModel.insertExpense(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
