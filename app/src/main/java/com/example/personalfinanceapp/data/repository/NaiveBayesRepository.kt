package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.data.WordCategoryCountDao
import com.example.personalfinanceapp.ml.CategoryPrediction
import com.example.personalfinanceapp.ml.NaiveBayesClassifier
import com.example.personalfinanceapp.ml.NaiveBayesStats

/**
 * Repository for Naive Bayes model.
 * Provides clean interface for ViewModels.
 * Implements NaiveBayesSource to allow test faking without subclassing.
 */
class NaiveBayesRepository(
    private val wordCategoryCountDao: WordCategoryCountDao
) : NaiveBayesSource {

    private val classifier = NaiveBayesClassifier(wordCategoryCountDao)

    override suspend fun trainModel(title: String, category: String): Result<Unit> {
        return try {
            classifier.train(title, category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrediction(title: String): Result<List<CategoryPrediction>> {
        return try {
            val predictions = classifier.predict(title)
            Result.success(predictions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopPrediction(title: String): Result<CategoryPrediction?> {
        return try {
            val predictions = classifier.predict(title)
            Result.success(predictions.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModelStats(): Result<NaiveBayesStats> {
        return try {
            val stats = classifier.getModelStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllWordCounts() = wordCategoryCountDao.getAllWordCounts()

    suspend fun isTrained(): Boolean {
        return try {
            val stats = classifier.getModelStats()
            stats.vocabularySize > 0
        } catch (e: Exception) {
            false
        }
    }

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