package com.spendwise.app.ui.expense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.MainActivity
import com.spendwise.app.R
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.FragmentExpenseHistoryBinding
import com.spendwise.app.utils.CurrencyFormatter
import com.spendwise.app.utils.hide
import com.spendwise.app.utils.show
import com.spendwise.app.utils.snackbarWithAction
import com.spendwise.app.viewmodel.ExpenseViewModel

class ExpenseHistoryFragment : Fragment() {

    private var _binding: FragmentExpenseHistoryBinding? = null
    private val binding get() = _binding!!

    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private lateinit var adapter: ExpenseHistoryAdapter
    private lateinit var userPreferences: UserPreferences
    private lateinit var currentUserId: String

    private var currentQuery = ""
    private var currentCategory = "All"
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        currentUserId = userPreferences.userEmail

        (activity as? MainActivity)?.showNavigation()

        setupRecyclerView()
        setupToolbar()
        setupSearch()
        setupFilters()
        setupFab()
        
        observeExpenses()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseHistoryAdapter(userPreferences.preferredCurrency)
        adapter.onItemClick = { expense ->
            val bundle = Bundle().apply { putInt("expenseId", expense.id) }
            findNavController().navigate(R.id.action_history_to_addExpense, bundle)
        }
        
        binding.rvExpenses.layoutManager = LinearLayoutManager(context)
        binding.rvExpenses.adapter = adapter

        // Swipe to Delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val expense = adapter.getExpenseAt(viewHolder.adapterPosition)
                expenseViewModel.deleteExpense(expense)
                
                binding.root.snackbarWithAction(
                    "\"${expense.title}\" deleted",
                    "UNDO"
                ) {
                    expenseViewModel.insertExpense(expense)
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.parseColor("#D32F2F"))
                val icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
                
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                    
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvExpenses)
        
        // FAB Scroll behavior
        binding.rvExpenses.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.fabAddExpense.hide()
                else if (dy < 0) binding.fabAddExpense.show()
            }
        })
    }

    private fun setupToolbar() {
        binding.toolbarHistory.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    if (binding.tilSearch.visibility == View.GONE) {
                        binding.tilSearch.show()
                        binding.etSearch.requestFocus()
                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
                    } else {
                        binding.etSearch.text?.clear()
                        binding.tilSearch.hide()
                        hideKeyboard()
                    }
                    true
                }
                R.id.action_filter -> {
                    if (binding.scrollFilterChips.visibility == View.GONE) {
                        binding.scrollFilterChips.show()
                    } else {
                        binding.scrollFilterChips.hide()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearch() {
        binding.tilSearch.setEndIconOnClickListener {
            binding.etSearch.text?.clear()
            binding.tilSearch.hide()
            hideKeyboard()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    currentQuery = s.toString().trim()
                    applyFilters()
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedIds.first())
                currentCategory = chip.text.toString()
                applyFilters()
            }
        }
    }

    private fun setupFab() {
        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_history_to_addExpense)
        }
    }

    private fun observeExpenses() {
        expenseViewModel.allExpenses.observe(viewLifecycleOwner) { list ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val allExpenses = expenseViewModel.allExpenses.value ?: emptyList()
        
        var filteredList = allExpenses
        
        // Query filter
        if (currentQuery.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.title.contains(currentQuery, ignoreCase = true) || 
                it.note.contains(currentQuery, ignoreCase = true) 
            }
        }
        
        // Category filter
        if (currentCategory != "All") {
            filteredList = filteredList.filter { it.category == currentCategory }
        }

        adapter.submitList(filteredList)
        updateSummaryBar(filteredList)
        
        if (filteredList.isEmpty()) {
            binding.rvExpenses.hide()
            binding.layoutEmptyHistory.show()
        } else {
            binding.layoutEmptyHistory.hide()
            binding.rvExpenses.show()
        }
    }

    private fun updateSummaryBar(list: List<Expense>) {
        val totalAmount = list.sumOf { it.amount }
        val size = list.size
        
        binding.tvTotalCount.text = resources.getQuantityString(R.plurals.expenses_count, size, size)
        binding.tvTotalAmount.text = getString(
            R.string.history_total,
            CurrencyFormatter.format(totalAmount, userPreferences.preferredCurrency)
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}
