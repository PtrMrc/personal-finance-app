package com.example.personalfinanceapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_items")
    fun getAllRecurringItemsFlow(): Flow<List<RecurringItem>>

    @Query("SELECT * FROM recurring_items")
    suspend fun getAllRecurringItems(): List<RecurringItem>

    @Insert
    suspend fun insertRecurringItem(item: RecurringItem)

    // Updates the timestamp so we don't pay it twice in one month
    @Query("UPDATE recurring_items SET lastProcessedDate = :date WHERE id = :id")
    suspend fun updateLastProcessed(id: Int, date: Long)

    // Delete if you cancel a subscription
    @Query("DELETE FROM recurring_items WHERE id = :id")
    suspend fun deleteRecurringItem(id: Int)
}