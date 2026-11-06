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
import com.spendwise.app.utils.CategoryStyles
import com.spendwise.app.utils.Constants
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

    private var latestSpent: Double? = null
    private var latestBudget: Double? = null

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
        val greetingRes = when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        val name = userPreferences.userName.ifEmpty { getString(R.string.default_user_name) }
        binding.tvGreeting.text = getString(greetingRes, name)

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

        // Budget progress: two independent observers feeding one UI update. (Observing the budget
        // inside the spent observer added a new observer on every emission, leaking observers and
        // re-running the progress animation several times.)
        expenseViewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { spent ->
            latestSpent = spent ?: 0.0
            refreshBudgetUI(currency)
        }
        budgetViewModel.totalMonthlyBudget.observe(viewLifecycleOwner) { budget ->
            latestBudget = budget ?: userPreferences.monthlyBudget
            refreshBudgetUI(currency)
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

    private fun refreshBudgetUI(currency: String) {
        val spent = latestSpent ?: return
        val budget = latestBudget ?: return
        updateBudgetUI(spent, budget, currency)
    }

    override fun onResume() {
        super.onResume()
        // Roll the month totals over if the date changed while the app was in the background
        expenseViewModel.refreshMonth()
        budgetViewModel.refreshMonth()
        binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun updateBudgetUI(spent: Double, budget: Double, currency: String) {
        binding.tvTotalSpent.text = CurrencyFormatter.format(spent, currency)
        val left = budget - spent
        binding.tvBudgetLeft.text = if (left >= 0) CurrencyFormatter.format(left, currency) else getString(R.string.budget_overbudget)

        if (budget > 0) {
            val percent = ((spent / budget) * 100).toInt()
            binding.tvBudgetPercent.text = getString(R.string.budget_percent_of_budget_used, percent.coerceAtMost(100))

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
        Constants.CATEGORIES.forEach { name ->
            val style = CategoryStyles.of(name)
            val amount = categoryMap[name] ?: 0.0
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_category_summary, binding.layoutCategories, false) as MaterialCardView

            cardView.setCardBackgroundColor(if (amount > 0) style.pastelColor else android.graphics.Color.parseColor("#F5F5F5"))
            if (amount == 0.0) cardView.alpha = 0.5f

            val tvEmoji = cardView.findViewById<TextView>(R.id.tvCatEmoji)
            val tvName = cardView.findViewById<TextView>(R.id.tvCatName)
            val tvAmount = cardView.findViewById<TextView>(R.id.tvCatAmount)

            tvEmoji.text = style.emoji
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
        binding.tvPanicAmount.text = getString(
            R.string.budget_panic_amount,
            CurrencyFormatter.format(spent, currency),
            CurrencyFormatter.format(budget, currency)
        )

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
