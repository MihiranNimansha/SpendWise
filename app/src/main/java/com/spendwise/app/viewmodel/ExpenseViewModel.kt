package com.spendwise.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val userPrefs = UserPreferences(application)
    private var currentUserId: String = userPrefs.userEmail

    val allExpenses: LiveData<List<Expense>> = expenseDao.getAllExpensesByUser(currentUserId).asLiveData()
    val recentFiveExpenses: LiveData<List<Expense>> = expenseDao.getRecentExpenses(currentUserId, 5).asLiveData()

    private val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
    }
    private val startOfMonth = calendar.timeInMillis
    
    val totalSpentThisMonth: LiveData<Double> = expenseDao.getTotalSpentInRange(currentUserId, startOfMonth, System.currentTimeMillis()).asLiveData()
    
    val categoryTotalsThisMonth: LiveData<Map<String, Double>> = expenseDao.getSpendingByCategory(currentUserId, startOfMonth, System.currentTimeMillis()).asLiveData().map { list ->
        list.associate { it.category to it.total }
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
