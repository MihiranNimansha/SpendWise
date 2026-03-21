package com.spendwise.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spendwise.app.data.local.dao.BudgetGoalDao
import com.spendwise.app.data.local.dao.ExpenseDao
import com.spendwise.app.data.local.dao.UserDao
import com.spendwise.app.data.local.entity.BudgetGoal
import com.spendwise.app.data.local.entity.Expense
import com.spendwise.app.data.local.entity.User
import com.spendwise.app.utils.Constants

@Database(
    entities = [Expense::class, BudgetGoal::class, User::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
