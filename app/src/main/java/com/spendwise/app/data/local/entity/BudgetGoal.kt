package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a monthly budget goal per category
 */
@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val userId: String
)
