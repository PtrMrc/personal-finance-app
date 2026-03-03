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
        WordCategoryCount::class,
        ModelPerformance::class,
        Budget::class,
        CategoryCorrectionStat::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringDao(): RecurringDao
    abstract fun wordCategoryCountDao(): WordCategoryCountDao
    abstract fun modelPerformanceDao(): ModelPerformanceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryCorrectionStatDao(): CategoryCorrectionStatDao

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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /** Migration 5 → 6: adds category_correction_stats table */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_correction_stats` (
                        `category` TEXT NOT NULL,
                        `totalSuggestions` INTEGER NOT NULL DEFAULT 0,
                        `totalCorrections` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`category`)
                    )
                """)
            }
        }

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS word_category_count (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        word TEXT NOT NULL,
                        category TEXT NOT NULL,
                        count INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_word_category_count_word_category
                    ON word_category_count(word, category)
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS model_performance (
                        modelName TEXT PRIMARY KEY NOT NULL,
                        correctCount INTEGER NOT NULL,
                        totalCount INTEGER NOT NULL,
                        currentWeight REAL NOT NULL
                    )
                """)
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