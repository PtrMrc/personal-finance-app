package com.example.personalfinanceapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Expense::class,
        RecurringItem::class,
        WordCategoryCount::class,      // For Naive Bayes
        ModelPerformance::class,         // For ensemble tracking
        Budget::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringDao(): RecurringDao
    abstract fun wordCategoryCountDao(): WordCategoryCountDao
    abstract fun modelPerformanceDao(): ModelPerformanceDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)

                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 4 to 5
         * Adds budgets table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `category` TEXT NOT NULL, 
                        `monthlyLimit` REAL NOT NULL, 
                        PRIMARY KEY(`category`)
                    )
                """)
            }
        }

        /**
         * Migration from version 3 to 4
         * Adds word_category_count and model_performance tables
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create word_category_count table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS word_category_count (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        category TEXT NOT NULL,
                        count INTEGER NOT NULL
                    )
                """)

                // Create unique index on (word, category)
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_word_category_count_word_category 
                    ON word_category_count(word, category)
                """)

                // Create model_performance table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS model_performance (
                        modelName TEXT PRIMARY KEY NOT NULL,
                        correctCount INTEGER NOT NULL,
                        totalCount INTEGER NOT NULL,
                        currentWeight REAL NOT NULL
                    )
                """)

                // Initialize default model weights
                db.execSQL("""
                    INSERT INTO model_performance (modelName, correctCount, totalCount, currentWeight)
                    VALUES ('TFLite', 0, 0, 0.6)
                """)
                db.execSQL("""
                    INSERT INTO model_performance (modelName, correctCount, totalCount, currentWeight)
                    VALUES ('NaiveBayes', 0, 0, 0.4)
                """)
            }
        }
    }
}