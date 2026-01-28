package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.CategoryTuple
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.ExpenseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
        .catch { emit(emptyList()) }

    val totalSpending: Flow<Double?> = expenseDao.getTotalSpending()
        .catch { emit(0.0) }

    val categoryBreakdown: Flow<List<CategoryTuple>> = expenseDao.getCategoryBreakdown()
        .catch { emit(emptyList()) }

    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.insertExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.deleteExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.updateExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryPrediction(title: String): Result<String?> {
        return try {
            val category = expenseDao.getLastCategoryForTitle(title)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}