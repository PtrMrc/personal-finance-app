package com.example.personalfinanceapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Tracks how often the AI's suggestion for a given category is corrected by the user.
 * A "correction" means the user changed the AI's suggested category before saving.
 *
 * Academically useful: reveals which categories the ensemble model struggles with most.
 */
@Entity(tableName = "category_correction_stats")
data class CategoryCorrectionStat(
    @PrimaryKey
    val category: String,           // The AI-suggested category being tracked
    val totalSuggestions: Int = 0,  // Times the AI suggested this category
    val totalCorrections: Int = 0   // Times the user changed away from this suggestion
) {
    val correctionRate: Double?
        get() = if (totalSuggestions > 0) totalCorrections.toDouble() / totalSuggestions else null

    val correctionRatePercent: String
        get() = correctionRate?.let { "${(it * 100).toInt()}%" } ?: "N/A"
}

@Dao
interface CategoryCorrectionStatDao {

    /**
     * Ensures a row exists for the category without overwriting existing data.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: CategoryCorrectionStat)

    /**
     * Called whenever an expense is saved and we had an AI prediction.
     * Always increments suggestions for the AI-suggested category.
     */
    @Query("""
        UPDATE category_correction_stats
        SET totalSuggestions = totalSuggestions + 1
        WHERE category = :category
    """)
    suspend fun incrementSuggestions(category: String)

    /**
     * Called only when the user changed the AI's suggestion.
     * Increments corrections for the category the AI got wrong.
     */
    @Query("""
        UPDATE category_correction_stats
        SET totalCorrections = totalCorrections + 1
        WHERE category = :category
    """)
    suspend fun incrementCorrections(category: String)

    /**
     * Live list ordered by correction rate descending (worst-performing first).
     */
    @Query("""
        SELECT * FROM category_correction_stats
        WHERE totalSuggestions > 0
        ORDER BY CAST(totalCorrections AS REAL) / CAST(totalSuggestions AS REAL) DESC
    """)
    fun getAllStatsFlow(): Flow<List<CategoryCorrectionStat>>

    /** One-shot snapshot for export or testing. */
    @Query("SELECT * FROM category_correction_stats WHERE totalSuggestions > 0")
    suspend fun getAllStatsSnapshot(): List<CategoryCorrectionStat>

    @Query("DELETE FROM category_correction_stats")
    suspend fun deleteAll()

    @Query("SELECT * FROM category_correction_stats ORDER BY category ASC")
    suspend fun getAll(): List<CategoryCorrectionStat>
}