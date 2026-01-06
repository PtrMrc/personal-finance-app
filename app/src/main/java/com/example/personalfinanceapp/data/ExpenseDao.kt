package com.example.personalfinanceapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // Insert a new transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // Delete an item
    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Get all items sorted by Date (Newest first)
    // We use Flow<> so the UI updates automatically when data changes
    @Query("SELECT * FROM expense_table ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // Get Total Balance (Income - Expense)
    // We get these separately to calculate "Available Balance" in the UI
    @Query("SELECT SUM(amount) FROM expense_table WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expense_table WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>
}