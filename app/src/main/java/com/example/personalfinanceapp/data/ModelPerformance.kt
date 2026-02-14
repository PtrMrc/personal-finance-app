package com.example.personalfinanceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Tracks performance metrics for each ML model
 * Used for adaptive weight adjustment in ensemble
 */
@Entity(tableName = "model_performance")
data class ModelPerformance(
    @PrimaryKey
    val modelName: String,      // "TFLite" or "NaiveBayes"

    val correctCount: Int = 0,  // How many predictions were correct
    val totalCount: Int = 0,    // Total predictions made
    val currentWeight: Double = 0.5  // Current weight in ensemble (0.0 to 1.0)
)

@Dao
interface ModelPerformanceDao {

    /**
     * Insert or update model performance
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modelPerformance: ModelPerformance)

    /**
     * Get performance for a specific model
     */
    @Query("SELECT * FROM model_performance WHERE modelName = :modelName")
    suspend fun getPerformance(modelName: String): ModelPerformance?

    /**
     * Get all model performances (for ensemble)
     */
    @Query("SELECT * FROM model_performance")
    suspend fun getAllPerformances(): List<ModelPerformance>

    /**
     * Get all performances as Flow (reactive updates)
     */
    @Query("SELECT * FROM model_performance")
    fun getAllPerformancesFlow(): Flow<List<ModelPerformance>>

    /**
     * Update correct count when model predicts correctly
     */
    @Query("""
        UPDATE model_performance 
        SET correctCount = correctCount + 1, 
            totalCount = totalCount + 1 
        WHERE modelName = :modelName
    """)
    suspend fun incrementCorrect(modelName: String)

    /**
     * Update total count when model makes incorrect prediction
     */
    @Query("""
        UPDATE model_performance 
        SET totalCount = totalCount + 1 
        WHERE modelName = :modelName
    """)
    suspend fun incrementTotal(modelName: String)

    /**
     * Update model weight (for ensemble adjustment)
     */
    @Query("""
        UPDATE model_performance 
        SET currentWeight = :weight 
        WHERE modelName = :modelName
    """)
    suspend fun updateWeight(modelName: String, weight: Double)

    /**
     * Get current accuracy for a model (correctCount / totalCount)
     */
    @Query("""
        SELECT CAST(correctCount AS REAL) / CAST(totalCount AS REAL) 
        FROM model_performance 
        WHERE modelName = :modelName AND totalCount > 0
    """)
    suspend fun getAccuracy(modelName: String): Double?

    /**
     * Reset all statistics (for testing)
     */
    @Query("DELETE FROM model_performance")
    suspend fun deleteAll()

    /**
     * Initialize default models if they don't exist
     */
    suspend fun initializeDefaultModels() {
        // Check if models already exist
        if (getPerformance("TFLite") == null) {
            insert(ModelPerformance(
                modelName = "TFLite",
                correctCount = 0,
                totalCount = 0,
                currentWeight = 0.6  // Start with 60% weight
            ))
        }
        if (getPerformance("NaiveBayes") == null) {
            insert(ModelPerformance(
                modelName = "NaiveBayes",
                correctCount = 0,
                totalCount = 0,
                currentWeight = 0.4  // Start with 40% weight
            ))
        }
    }
}