package com.spendwise.app.data.repository

import com.spendwise.app.data.local.dao.ExpenseDao
import com.spendwise.app.data.local.dao.CategoryTotal
import com.spendwise.app.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository for expense operations — mediates between ViewModel and ExpenseDao
 */
class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getAllExpensesByUser(userId: String): Flow<List<Expense>> =
        expenseDao.getAllExpensesByUser(userId)

    fun getExpenseById(id: Int): Flow<Expense?> = 
        expenseDao.getExpenseById(id)

    fun getExpensesByCategory(userId: String, category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(userId, category)

    fun getTotalSpentInRange(userId: String, startDate: Long, endDate: Long): Flow<Double> =
        expenseDao.getTotalSpentInRange(userId, startDate, endDate)

    fun getExpensesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(userId, startDate, endDate)

    fun searchExpenses(userId: String, query: String): Flow<List<Expense>> =
        expenseDao.searchExpenses(userId, query)

    fun getRecentExpenses(userId: String, limit: Int = 5): Flow<List<Expense>> =
        expenseDao.getRecentExpenses(userId, limit)

    fun getSpendingByCategory(userId: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        expenseDao.getSpendingByCategory(userId, startDate, endDate)

    suspend fun insertExpense(expense: Expense) =
        expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) =
        expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) =
        expenseDao.deleteExpense(expense)

    suspend fun deleteAllExpensesForUser(userId: String) =
        expenseDao.deleteAllExpensesForUser(userId)
}
