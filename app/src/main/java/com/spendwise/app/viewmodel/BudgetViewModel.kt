package com.spendwise.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.local.entity.BudgetGoal
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.utils.Constants
import com.spendwise.app.utils.getCurrentMonthStart
import com.spendwise.app.utils.monthEndOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetGoalDao = AppDatabase.getDatabase(application).budgetGoalDao()
    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val userPrefs = UserPreferences(application)
    private var currentUserId: String = userPrefs.userEmail

    /** First millisecond of the month being shown. Re-evaluated by [refreshMonth]. */
    private val monthStart = MutableLiveData(getCurrentMonthStart())

    val allGoals: LiveData<List<BudgetGoal>> = budgetGoalDao.getAllGoalsByUser(currentUserId).asLiveData()

    /** Wrapper so "not loaded yet" (no value) can be told apart from "no goals set" (null total). */
    private data class BudgetTotal(val total: Double?)

    private val budgetTotal: LiveData<BudgetTotal> = monthStart.switchMap { start ->
        val month = Calendar.getInstance().apply { timeInMillis = start }
        budgetGoalDao.getTotalBudgetForMonth(
            currentUserId, month.get(Calendar.MONTH) + 1, month.get(Calendar.YEAR)
        ).map { BudgetTotal(it) }.asLiveData()
    }

    val totalMonthlyBudget: LiveData<Double?> = budgetTotal.map { it.total }

    private val totalSpentThisMonth: LiveData<Double> = monthStart.switchMap { start ->
        expenseDao.getTotalSpentInRange(currentUserId, start, monthEndOf(start)).asLiveData()
    }

    // Uses the same budget the dashboard displays (sum of category goals, else the monthly budget
    // from settings). Before, the alert always used the settings budget, so it could disagree with
    // the progress bar the user was looking at.
    val panicAlert: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun recompute() {
            val spent = totalSpentThisMonth.value ?: return
            val goalsTotal = budgetTotal.value ?: return   // wait until the budget has loaded
            val budget = goalsTotal.total ?: userPrefs.monthlyBudget
            value = budget > 0 && spent >= budget * Constants.PANIC_THRESHOLD
        }
        addSource(totalSpentThisMonth) { recompute() }
        addSource(budgetTotal) { recompute() }
    }

    /** Call when a screen becomes visible so the month rolls over after midnight on the 1st. */
    fun refreshMonth() {
        val current = getCurrentMonthStart()
        if (monthStart.value != current) monthStart.value = current
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
