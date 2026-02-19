package com.example.personalfinanceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val monthlyLimit: Double
)