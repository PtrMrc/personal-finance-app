package com.example.personalfinanceapp.ml

import android.util.Log
import com.example.personalfinanceapp.data.WordCategoryCountDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln

/**
 * Naive Bayes Classifier for expense category prediction
 *
 * Algorithm:
 * - Training: Count word frequencies per category
 * - Prediction: Calculate P(category|words) using Bayes theorem
 * - Uses log probabilities to avoid numerical underflow
 * - Laplace smoothing for unseen words
 *
 * This is implemented from scratch for thesis purposes.
 */
class NaiveBayesClassifier(
    private val wordCategoryCountDao: WordCategoryCountDao
) {

    companion object {
        private const val TAG = "NaiveBayesClassifier"
        private const val LAPLACE_ALPHA = 1.0  // Smoothing parameter
    }

    /**
     * Train the model with a new expense
     * Tokenizes the title and increments word counts for the given category
     */
    suspend fun train(title: String, category: String) = withContext(Dispatchers.IO) {
        try {
            val words = tokenize(title)

            // Increment count for each word in this category
            for (word in words) {
                wordCategoryCountDao.incrementCount(word, category)
            }

            try {
                Log.d(TAG, "Trained: '$title' → '$category' (${words.size} words)")
            } catch (e: Exception) {
                println("[$TAG] Trained: '$title' → '$category' (${words.size} words)")
            }
        } catch (e: Exception) {
            try {
                Log.e(TAG, "Error training on '$title'", e)
            } catch (logEx: Exception) {
                println("[$TAG] Error training on '$title': ${e.message}")
            }
        }
    }

    /**
     * Predict category probabilities for a given title
     */
    suspend fun predict(title: String): List<CategoryPrediction> = withContext(Dispatchers.IO) {
        try {
            val words = tokenize(title)

            if (words.isEmpty()) {
                try {
                    Log.w(TAG, "No words to predict from: '$title'")
                } catch (e: Exception) {
                    println("[$TAG] No words to predict from: '$title'")
                }
                return@withContext emptyList()
            }

            // Get all categories we've seen during training
            val categories = wordCategoryCountDao.getAllCategories()

            if (categories.isEmpty()) {
                try {
                    Log.w(TAG, "No training data available yet")
                } catch (e: Exception) {
                    println("[$TAG] No training data available yet")
                }
                return@withContext emptyList()
            }

            // Get vocabulary size for Laplace smoothing
            val vocabularySize = wordCategoryCountDao.getVocabularySize()

            // Calculate log probability for each category
            val predictions = categories.map { category ->
                val logProb = calculateLogProbability(words, category, vocabularySize)
                CategoryPrediction(category, logProb)
            }

            // Convert log probabilities to regular probabilities and normalize
            val normalized = normalizeProbabilities(predictions)

            try {
                Log.d(TAG, "Predicted for '$title': ${normalized.take(2)}")
            } catch (e: Exception) {
                println("[$TAG] Predicted for '$title': ${normalized.take(2)}")
            }

            return@withContext normalized

        } catch (e: Exception) {
            try {
                Log.e(TAG, "Error predicting for '$title'", e)
            } catch (logEx: Exception) {
                println("[$TAG] Error predicting for '$title': ${e.message}")
            }
            return@withContext emptyList()
        }
    }

    private suspend fun calculateLogProbability(
        words: List<String>,
        category: String,
        vocabularySize: Int
    ): Double {
        val logPrior = 0.0
        val totalWordsInCategory = (wordCategoryCountDao.getTotalCountForCategory(category) ?: 0).toDouble()
        var logLikelihood = 0.0

        for (word in words) {
            val wordCount = (wordCategoryCountDao.getCount(word, category) ?: 0).toDouble()
            val probability = (wordCount + LAPLACE_ALPHA) /
                    (totalWordsInCategory + LAPLACE_ALPHA * vocabularySize)
            logLikelihood += ln(probability)
        }

        return logPrior + logLikelihood
    }

    private fun normalizeProbabilities(predictions: List<CategoryPrediction>): List<CategoryPrediction> {
        if (predictions.isEmpty()) return emptyList()
        val maxLogProb = predictions.maxOf { it.probability }
        val expProbs = predictions.map {
            it.copy(probability = kotlin.math.exp(it.probability - maxLogProb))
        }
        val sumProbs = expProbs.sumOf { it.probability }

        return expProbs.map {
            it.copy(probability = it.probability / sumProbs)
        }.sortedByDescending { it.probability }
    }

    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-záéíóöőúüű\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .distinct()
    }

    suspend fun getModelStats(): NaiveBayesStats = withContext(Dispatchers.IO) {
        val vocabularySize = wordCategoryCountDao.getVocabularySize()
        val categories = wordCategoryCountDao.getAllCategories()
        val totalEntries = categories.sumOf { category ->
            wordCategoryCountDao.getTotalCountForCategory(category) ?: 0
        }

        NaiveBayesStats(
            vocabularySize = vocabularySize,
            categoryCount = categories.size,
            totalWordCount = totalEntries
        )
    }
}

data class CategoryPrediction(val category: String, val probability: Double)
data class NaiveBayesStats(val vocabularySize: Int, val categoryCount: Int, val totalWordCount: Int)