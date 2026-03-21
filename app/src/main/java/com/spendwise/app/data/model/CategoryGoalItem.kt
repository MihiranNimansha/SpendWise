package com.spendwise.app.data.model

data class CategoryGoalItem(
    val category: String,
    val monthlyLimit: Double,  // 0.0 if no goal set
    val spent: Double,
    val hasGoal: Boolean,
    val goalId: Int = -1,
    val month: Int,
    val year: Int
)
