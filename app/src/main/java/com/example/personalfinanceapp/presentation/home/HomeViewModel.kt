package com.example.personalfinanceapp.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.data.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val recurringRepository = RecurringRepository(database.recurringDao())

    val allExpenses: Flow<List<Expense>> = expenseRepository.allExpenses
    val totalSpending: Flow<Double?> = expenseRepository.totalSpending
    val categoryBreakdown = expenseRepository.categoryBreakdown
    val allRecurringItems: Flow<List<RecurringItem>> = recurringRepository.allRecurringItems

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás frissítésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to update expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    suspend fun getCategoryPrediction(title: String): String? {
        return expenseRepository.getCategoryPrediction(title)
            .onFailure { e ->
                Log.e("HomeViewModel", "Failed to get category prediction", e)
            }
            .getOrNull()
    }

    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.addRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add recurring item", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete recurring item", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}