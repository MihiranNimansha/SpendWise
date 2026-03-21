package com.spendwise.app.data.local.dao

import androidx.room.*
import com.spendwise.app.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Expense operations
 */
@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    /**
     * Get all expenses for a user, ordered by date descending
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpensesByUser(userId: String): Flow<List<Expense>>

    /**
     * Get expenses filtered by category for a user
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category ORDER BY date DESC")
    fun getExpensesByCategory(userId: String, category: String): Flow<List<Expense>>

    /**
     * Get total amount spent in a date range for a user
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    fun getTotalSpentInRange(userId: String, startDate: Long, endDate: Long): Flow<Double>

    /**
     * Get expenses within a specific date range ordered by date descending
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>>

    /**
     * Search expenses by title or note for a user
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchExpenses(userId: String, query: String): Flow<List<Expense>>

    /**
     * Delete all expenses belonging to a user
     */
    @Query("DELETE FROM expenses WHERE userId = :userId")
    suspend fun deleteAllExpensesForUser(userId: String)

    /**
     * Get total amount grouped by category within a date range
     */
    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate GROUP BY category")
    fun getSpendingByCategory(userId: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    /**
     * Get the most recent N expenses for a user
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(userId: String, limit: Int = 5): Flow<List<Expense>>

    /**
     * Get a specific expense by ID
     */
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Int): Flow<Expense?>
}

/**
 * Helper data class for category-totals aggregate query
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)
