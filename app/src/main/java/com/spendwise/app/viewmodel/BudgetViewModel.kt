package com.spendwise.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.local.entity.BudgetGoal
import com.spendwise.app.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetGoalDao = AppDatabase.getDatabase(application).budgetGoalDao()
    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val userPrefs = UserPreferences(application)
    private var currentUserId: String = userPrefs.userEmail

    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH) + 1
    private val currentYear = calendar.get(Calendar.YEAR)

    val allGoals: LiveData<List<BudgetGoal>> = budgetGoalDao.getAllGoalsByUser(currentUserId).asLiveData()
    
    val totalMonthlyBudget: LiveData<Double?> = budgetGoalDao.getTotalBudgetForMonth(currentUserId, currentMonth, currentYear).asLiveData()

    private val startOfMonth = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis
    
    val panicAlert: LiveData<Boolean> = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        val totalSpent = expenseDao.getTotalSpentInRange(currentUserId, startOfMonth, System.currentTimeMillis()).asLiveData()
        
        addSource(totalSpent) { spent ->
            val budget = userPrefs.monthlyBudget
            value = spent != null && budget > 0 && spent >= budget * 0.9
        }
    }

    fun insertGoal(goal: BudgetGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetGoalDao.insertGoal(goal)
        }
    }

    fun updateGoal(goal: BudgetGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetGoalDao.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: BudgetGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetGoalDao.deleteGoal(goal)
        }
    }
}
