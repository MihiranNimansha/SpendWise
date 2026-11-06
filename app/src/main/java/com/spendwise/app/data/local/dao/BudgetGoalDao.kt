package com.spendwise.app.data.local.dao

import androidx.room.*
import com.spendwise.app.data.local.entity.BudgetGoal
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BudgetGoal operations
 */
@Dao
interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: BudgetGoal)

    @Update
    suspend fun updateGoal(goal: BudgetGoal)

    @Delete
    suspend fun deleteGoal(goal: BudgetGoal)

    /**
     * Get all goals for a user in a specific month/year
     */
    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND month = :month AND year = :year")
    fun getGoalsByUserAndMonth(userId: String, month: Int, year: Int): Flow<List<BudgetGoal>>

    /**
     * Get total budget set for a user in a specific month/year
     */
    @Query("SELECT SUM(monthlyLimit) FROM budget_goals WHERE userId = :userId AND month = :month AND year = :year")
    fun getTotalBudgetForMonth(userId: String, month: Int, year: Int): Flow<Double?>

    /**
     * Get a specific goal by category, month, year, and user
     */
    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getGoalByCategoryAndMonth(userId: String, category: String, month: Int, year: Int): BudgetGoal?

    /**
     * Delete all goals for a user
     */
    @Query("DELETE FROM budget_goals WHERE userId = :userId")
    suspend fun deleteAllGoalsForUser(userId: String)

    /**
     * Get all goals for a user (all months)
     */
    @Query("SELECT * FROM budget_goals WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllGoalsByUser(userId: String): Flow<List<BudgetGoal>>
}
