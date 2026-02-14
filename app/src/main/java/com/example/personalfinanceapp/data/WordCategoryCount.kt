package com.example.personalfinanceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Stores word frequency per category for Naive Bayes classification
 *
 * Example:
 * - word: "netflix", category: "Bills", count: 5
 * - word: "coffee", category: "Food", count: 12
 */
@Entity(
    tableName = "word_category_count",
    indices = [Index(value = ["word", "category"], unique = true)]
)
data class WordCategoryCount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val word: String,        // Lowercase, cleaned word (e.g., "netflix")
    val category: String,    // Category name (e.g., "Bills")
    val count: Int = 1       // How many times this word appeared in this category
)

@Dao
interface WordCategoryCountDao {

    /**
     * Insert or update word count
     * If word-category pair exists, it will be replaced
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wordCategoryCount: WordCategoryCount)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(wordCategoryCount: WordCategoryCount): Long

    @Query("UPDATE word_category_count SET count = count + 1 WHERE word = :word AND category = :category")
    suspend fun updateExistingCount(word: String, category: String)

    /**
     * Increment count for a specific word-category pair
     * Used during training
     */
    @androidx.room.Transaction
    suspend fun incrementCount(word: String, category: String) {
        val rowId = insertInitial(WordCategoryCount(word = word, category = category, count = 1))
        if (rowId == -1L) {
            // -1 means the row already existed because of the UNIQUE constraint
            updateExistingCount(word, category)
        }
    }

    /**
     * Get count for specific word in specific category
     * Returns 0 if not found
     */
    @Query("SELECT count FROM word_category_count WHERE word = :word AND category = :category")
    suspend fun getCount(word: String, category: String): Int?

    /**
     * Get all counts for a specific word across all categories
     * Used during prediction
     */
    @Query("SELECT * FROM word_category_count WHERE word = :word")
    suspend fun getCountsForWord(word: String): List<WordCategoryCount>

    /**
     * Get total count for a category (all words)
     * Used for probability calculation
     */
    @Query("SELECT SUM(count) FROM word_category_count WHERE category = :category")
    suspend fun getTotalCountForCategory(category: String): Int?

    /**
     * Get all unique categories that have been trained
     */
    @Query("SELECT DISTINCT category FROM word_category_count")
    suspend fun getAllCategories(): List<String>

    /**
     * Get total number of unique words (vocabulary size)
     */
    @Query("SELECT COUNT(DISTINCT word) FROM word_category_count")
    suspend fun getVocabularySize(): Int

    /**
     * Get all word-category pairs (for debugging/stats)
     */
    @Query("SELECT * FROM word_category_count ORDER BY count DESC")
    fun getAllWordCounts(): Flow<List<WordCategoryCount>>

    /**
     * Delete all data (for testing/reset)
     */
    @Query("DELETE FROM word_category_count")
    suspend fun deleteAll()

    /**
     * Get top words for a category (for UI display)
     */
    @Query("""
        SELECT * FROM word_category_count 
        WHERE category = :category 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopWordsForCategory(category: String, limit: Int = 10): List<WordCategoryCount>
}