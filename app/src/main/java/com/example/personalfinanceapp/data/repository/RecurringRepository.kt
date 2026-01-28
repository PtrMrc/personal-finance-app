package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.RecurringDao
import com.example.personalfinanceapp.data.RecurringItem
import kotlinx.coroutines.flow.Flow

class RecurringRepository(private val recurringDao: RecurringDao) {

    // Expose data streams
    val allRecurringItems: Flow<List<RecurringItem>> = recurringDao.getAllRecurringItemsFlow()

    // Operations
    suspend fun addRecurringItem(item: RecurringItem) {
        recurringDao.insertRecurringItem(item)
    }

    suspend fun deleteRecurringItem(item: RecurringItem) {
        recurringDao.deleteRecurringItem(item.id)
    }
}