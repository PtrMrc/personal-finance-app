package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.RecurringDao
import com.example.personalfinanceapp.data.RecurringItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class RecurringRepository(private val recurringDao: RecurringDao) {

    val allRecurringItems: Flow<List<RecurringItem>> = recurringDao.getAllRecurringItemsFlow()
        .catch { emit(emptyList()) }

    suspend fun addRecurringItem(item: RecurringItem): Result<Unit> {
        return try {
            recurringDao.insertRecurringItem(item)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringItem(item: RecurringItem): Result<Unit> {
        return try {
            recurringDao.deleteRecurringItem(item.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}