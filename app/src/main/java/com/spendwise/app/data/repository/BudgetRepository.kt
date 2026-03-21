package com.spendwise.app.data.repository

import com.spendwise.app.data.local.dao.BudgetGoalDao
import com.spendwise.app.data.local.entity.BudgetGoal
import kotlinx.coroutines.flow.Flow

/**
 * Repository for budget goal operations — mediates between ViewModel and BudgetGoalDao
 */
class BudgetRepository(private val budgetGoalDao: BudgetGoalDao) {

    fun getGoalsByUserAndMonth(userId: String, month: Int, year: Int): Flow<List<BudgetGoal>> =
        budgetGoalDao.getGoalsByUserAndMonth(userId, month, year)

    fun getTotalBudgetForMonth(userId: String, month: Int, year: Int): Flow<Double?> =
        budgetGoalDao.getTotalBudgetForMonth(userId, month, year)

    fun getAllGoalsByUser(userId: String): Flow<List<BudgetGoal>> =
        budgetGoalDao.getAllGoalsByUser(userId)

    suspend fun getGoalByCategoryAndMonth(userId: String, category: String, month: Int, year: Int): BudgetGoal? =
        budgetGoalDao.getGoalByCategoryAndMonth(userId, category, month, year)

    suspend fun insertGoal(goal: BudgetGoal) =
        budgetGoalDao.insertGoal(goal)

    suspend fun updateGoal(goal: BudgetGoal) =
        budgetGoalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: BudgetGoal) =
        budgetGoalDao.deleteGoal(goal)

    suspend fun getGoalByCategory(userId: String, category: String, month: Int, year: Int): BudgetGoal? =
        budgetGoalDao.getGoalByCategory(userId, category, month, year)

    suspend fun deleteAllGoalsForUser(userId: String) =
        budgetGoalDao.deleteAllGoalsForUser(userId)
}
