package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.Budget
import com.example.personalfinanceapp.data.BudgetDao
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun setBudget(budget: Budget) {
        budgetDao.setBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
}