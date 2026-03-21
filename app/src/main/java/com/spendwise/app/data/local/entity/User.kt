package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val preferredCurrency: String = "LKR",
    val monthlyBudget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
