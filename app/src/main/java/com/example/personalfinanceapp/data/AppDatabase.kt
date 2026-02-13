package com.example.personalfinanceapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Expense::class, RecurringItem::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If instance exists, return it; otherwise create it
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true) // Wipes data if the schema changed (good for dev)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}