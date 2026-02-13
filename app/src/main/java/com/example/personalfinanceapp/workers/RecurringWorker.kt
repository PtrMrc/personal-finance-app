package com.example.personalfinanceapp.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.data.RecurringItem
import java.util.Calendar

class RecurringWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val today = Calendar.getInstance()

        // 1. Get all rules (Netflix, Salary, Rent...)
        val items = db.recurringDao().getAllRecurringItems()

        items.forEach { item ->
            // 2. Check logic: Is it time to run this item?
            if (shouldProcess(item, today)) {

                // 3. CREATE THE REAL TRANSACTION
                val newExpense = Expense(
                    title = item.title, // "Netflix"
                    amount = item.amount,
                    category = item.category,
                    description = "Automatikus tétel (${item.frequency})",
                    date = System.currentTimeMillis(),
                    isIncome = item.isIncome // Pass this correctly!
                )

                // Insert into the main list so it shows up on your screen
                db.expenseDao().insertExpense(newExpense)

                // 4. Mark as done so it doesn't run again tomorrow
                db.recurringDao().updateLastProcessed(item.id, System.currentTimeMillis())
            }
        }

        return Result.success()
    }

    // --- LOGIC HELPER ---
    private fun shouldProcess(item: RecurringItem, today: Calendar): Boolean {
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val lastDayOfThisMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Rule A: Is today the correct day? (e.g., 15th)
        // (Handle edge case: if user wants 31st but it's Feb 28th, run it on the last day)
        val isTargetDay = (currentDay == item.dayOfMonth) ||
                (item.dayOfMonth > lastDayOfThisMonth && currentDay == lastDayOfThisMonth)

        if (!isTargetDay) return false

        // Rule B: Has enough time passed since the last run?
        if (item.lastProcessedDate == null) return true // First time ever

        val lastRun = Calendar.getInstance().apply { timeInMillis = item.lastProcessedDate }
        val monthsDiff = diffMonths(lastRun, today)

        return when (item.frequency) {
            Frequency.MONTHLY -> monthsDiff >= 1
            Frequency.QUARTERLY -> monthsDiff >= 3
            Frequency.YEARLY -> monthsDiff >= 12
        }
    }

    private fun diffMonths(start: Calendar, end: Calendar): Int {
        val diffYear = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        return diffYear * 12 + end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
    }
}