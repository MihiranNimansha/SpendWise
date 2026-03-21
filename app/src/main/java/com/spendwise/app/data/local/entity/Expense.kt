package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an expense transaction
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)
