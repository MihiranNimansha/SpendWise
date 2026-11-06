package com.spendwise.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.utils.getCurrentMonthStart
import com.spendwise.app.utils.monthEndOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val userPrefs = UserPreferences(application)
    private var currentUserId: String = userPrefs.userEmail

    val allExpenses: LiveData<List<Expense>> = expenseDao.getAllExpensesByUser(currentUserId).asLiveData()
    val recentFiveExpenses: LiveData<List<Expense>> = expenseDao.getRecentExpenses(currentUserId, 5).asLiveData()

    /** First millisecond of the month being shown. Re-evaluated by [refreshMonth]. */
    private val monthStart = MutableLiveData(getCurrentMonthStart())

    // The range covers the WHOLE calendar month. It used to end at "now" (captured when the
    // ViewModel was created), which silently dropped expenses dated later that day or later this
    // month, and it never rolled over when a new month started while the app stayed alive.
    val totalSpentThisMonth: LiveData<Double> = monthStart.switchMap { start ->
        expenseDao.getTotalSpentInRange(currentUserId, start, monthEndOf(start)).asLiveData()
    }

    val categoryTotalsThisMonth: LiveData<Map<String, Double>> = monthStart.switchMap { start ->
        expenseDao.getSpendingByCategory(currentUserId, start, monthEndOf(start)).asLiveData().map { list ->
            list.associate { it.category to it.total }
        }
    }

    /** Call when a screen becomes visible so the month totals roll over after midnight on the 1st. */
    fun refreshMonth() {
        val current = getCurrentMonthStart()
        if (monthStart.value != current) monthStart.value = current
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.deleteExpense(expense)
        }
    }
    
    fun getExpenseById(id: Int): LiveData<Expense?> {
        return expenseDao.getExpenseById(id).asLiveData()
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.updateExpense(expense)
        }
    }
}
