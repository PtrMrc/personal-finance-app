package com.example.personalfinanceapp.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.data.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Get database instance
    private val database = AppDatabase.getDatabase(application)

    // Initialize repositories with their DAOs
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val recurringRepository = RecurringRepository(database.recurringDao())

    // Expose data streams from repository
    val allExpenses: Flow<List<Expense>> = expenseRepository.allExpenses
    val totalSpending: Flow<Double?> = expenseRepository.totalSpending
    val categoryBreakdown = expenseRepository.categoryBreakdown
    val allRecurringItems: Flow<List<RecurringItem>> = recurringRepository.allRecurringItems

    // Expense operations
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
        }
    }

    suspend fun getCategoryPrediction(title: String): String? {
        return expenseRepository.getCategoryPrediction(title)
    }

    // Recurring item operations
    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.addRecurringItem(item)
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringItem(item)
        }
    }
}