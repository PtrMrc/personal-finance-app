package com.example.personalfinanceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val amount: Double,
    val title: String,
    val category: String,
    val description: String?,
    val date: Long,
    val isIncome: Boolean
)