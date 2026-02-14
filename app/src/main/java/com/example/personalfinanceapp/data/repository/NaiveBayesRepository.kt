package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.WordCategoryCountDao
import com.example.personalfinanceapp.ml.CategoryPrediction
import com.example.personalfinanceapp.ml.NaiveBayesClassifier
import com.example.personalfinanceapp.ml.NaiveBayesStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for Naive Bayes model
 * Provides clean interface for ViewModels
 */
class NaiveBayesRepository(
    private val wordCategoryCountDao: WordCategoryCountDao
) {

    private val classifier = NaiveBayesClassifier(wordCategoryCountDao)

    /**
     * Train the model with user's categorization choice
     *
     * @param title Expense title
     * @param category Category chosen by user
     */
    suspend fun trainModel(title: String, category: String): Result<Unit> {
        return try {
            classifier.train(title, category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get category predictions for a title
     *
     * @param title Expense title to classify
     * @return List of category predictions sorted by probability
     */
    suspend fun getPrediction(title: String): Result<List<CategoryPrediction>> {
        return try {
            val predictions = classifier.predict(title)
            Result.success(predictions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the top prediction only
     *
     * @param title Expense title
     * @return Top category prediction or null if no predictions
     */
    suspend fun getTopPrediction(title: String): Result<CategoryPrediction?> {
        return try {
            val predictions = classifier.predict(title)
            Result.success(predictions.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get model statistics for UI display
     */
    suspend fun getModelStats(): Result<NaiveBayesStats> {
        return try {
            val stats = classifier.getModelStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all word counts as Flow (reactive updates)
     */
    fun getAllWordCounts() = wordCategoryCountDao.getAllWordCounts()

    /**
     * Check if model has been trained (has any data)
     */
    suspend fun isTrained(): Boolean {
        return try {
            val stats = classifier.getModelStats()
            stats.vocabularySize > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get top words for a specific category
     * Useful for debugging and UI display
     */
    suspend fun getTopWordsForCategory(category: String, limit: Int = 10): Result<List<Pair<String, Int>>> {
        return try {
            val words = wordCategoryCountDao.getTopWordsForCategory(category, limit)
            val pairs = words.map { it.word to it.count }
            Result.success(pairs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}