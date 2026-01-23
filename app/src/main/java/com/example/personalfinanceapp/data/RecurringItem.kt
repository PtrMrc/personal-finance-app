package com.example.personalfinanceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum to handle different periods
enum class Frequency { MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "recurring_items")
data class RecurringItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,          // e.g. "Netflix" or "Salary"
    val amount: Double,         // e.g. 4500.0
    val category: String,       // e.g. "Szórakozás" or "Bevétel"
    val isIncome: Boolean,      // TRUE for Salary, FALSE for Netflix
    val frequency: Frequency,   // Monthly, Quarterly, etc.
    val dayOfMonth: Int,        // Which day? e.g. 15
    val lastProcessedDate: Long? = null // When did we last add this automatically?
)