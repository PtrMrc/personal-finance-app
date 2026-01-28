package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.CategoryTuple
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.ExpenseDao
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // Expose data streams
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val totalSpending: Flow<Double?> = expenseDao.getTotalSpending()
    val categoryBreakdown: Flow<List<CategoryTuple>> = expenseDao.getCategoryBreakdown()

    // Operations
    suspend fun addExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun getCategoryPrediction(title: String): String? {
        return expenseDao.getLastCategoryForTitle(title)
    }
}