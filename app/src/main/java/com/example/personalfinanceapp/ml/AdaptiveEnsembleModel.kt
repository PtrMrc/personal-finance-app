package com.example.personalfinanceapp.ml

import android.content.Context
import android.util.Log
import com.example.personalfinanceapp.data.ModelPerformanceDao
import com.example.personalfinanceapp.data.repository.NaiveBayesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Adaptive Ensemble Model - Meta-Learning System
 *
 * Combines TFLite and Naive Bayes predictions using adaptive weights.
 * Learns over time which model to trust more based on user corrections.
 *
 * Architecture:
 * 1. Run both models in parallel
 * 2. Combine predictions using current weights
 * 3. Track which model was correct
 * 4. Adjust weights accordingly
 *
 */
class AdaptiveEnsembleModel(
    private val context: Context,
    private val tfliteClassifier: ExpenseClassifier,
    private val naiveBayesRepository: NaiveBayesRepository,
    private val modelPerformanceDao: ModelPerformanceDao
) {

    companion object {
        private const val TAG = "AdaptiveEnsemble"
        private const val LEARNING_RATE = 0.1  // How fast weights adapt (α)
        private const val MIN_WEIGHT = 0.15    // Minimum weight for any model (keep diversity)
        private const val MAX_WEIGHT = 0.85    // Maximum weight for any model (prevent over-reliance)

        const val MODEL_TFLITE = "TFLite"
        const val MODEL_NAIVE_BAYES = "NaiveBayes"
    }

    /**
     * Predict category using weighted ensemble
     *
     * @param title Expense title to classify
     * @return Ensemble prediction with breakdown
     */
    suspend fun predict(title: String): EnsemblePrediction = withContext(Dispatchers.IO) {
        try {
            // Get current model weights
            val weights = getCurrentWeights()

            // Run both models in parallel for speed
            val tfliteDeferred = async { predictWithTFLite(title) }
            val naiveBayesDeferred = async { predictWithNaiveBayes(title) }

            val tflitePred = tfliteDeferred.await()
            val naiveBayesPred = naiveBayesDeferred.await()

            // Combine predictions using weighted voting
            val finalPrediction = combineePredictions(
                tflitePred,
                naiveBayesPred,
                weights
            )

            logPrediction(title, finalPrediction)

            return@withContext finalPrediction

        } catch (e: Exception) {
            logError("Error predicting for '$title'", e)

            // Fallback: return TFLite only
            return@withContext EnsemblePrediction(
                finalCategory = "Other",
                confidence = 0.3,
                tflitePrediction = ModelPrediction(MODEL_TFLITE, "Other", 0.5, 1.0),
                naiveBayesPrediction = null,
                weights = ModelWeights(1.0, 0.0),
                explanation = "Error occurred, using fallback"
            )
        }
    }

    /**
     * Record user's final choice and update model weights
     * This is where the learning happens!
     *
     * @param title Original expense title
     * @param ensemblePrediction What the ensemble predicted
     * @param userChoice What the user actually chose
     */
    suspend fun recordUserChoice(
        title: String,
        ensemblePrediction: EnsemblePrediction,
        userChoice: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Determine which models were correct
            val tfliteCorrect = ensemblePrediction.tflitePrediction?.category == userChoice
            val naiveBayesCorrect = ensemblePrediction.naiveBayesPrediction?.category == userChoice

            // Update performance tracking
            updateModelPerformance(MODEL_TFLITE, tfliteCorrect)
            updateModelPerformance(MODEL_NAIVE_BAYES, naiveBayesCorrect)

            // Adjust weights based on which model was right
            adjustWeights(tfliteCorrect, naiveBayesCorrect)

            // Train Naive Bayes with user's choice
            naiveBayesRepository.trainModel(title, userChoice)

            logLearning(title, tfliteCorrect, naiveBayesCorrect, userChoice)

        } catch (e: Exception) {
            logError("Error recording user choice for '$title'", e)
        }
    }

    /**
     * Get current model weights from database
     */
    private suspend fun getCurrentWeights(): ModelWeights {
        val tflitePerf = modelPerformanceDao.getPerformance(MODEL_TFLITE)
        val naiveBayesPerf = modelPerformanceDao.getPerformance(MODEL_NAIVE_BAYES)

        // If no data yet, use defaults
        val tfliteWeight = tflitePerf?.currentWeight ?: 0.6
        val naiveBayesWeight = naiveBayesPerf?.currentWeight ?: 0.4

        return ModelWeights(tfliteWeight, naiveBayesWeight)
    }

    /**
     * Predict using TFLite model
     */
    private fun predictWithTFLite(title: String): ModelPrediction? {
        return try {
            val category = tfliteClassifier.classify(title)
            if (category != null) {
                ModelPrediction(
                    modelName = MODEL_TFLITE,
                    category = category,
                    confidence = 0.8,  // TFLite doesn't return confidence, use default
                    weight = 0.0  // Will be filled in later
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logError("TFLite prediction failed", e)
            null
        }
    }

    /**
     * Predict using Naive Bayes model
     */
    private suspend fun predictWithNaiveBayes(title: String): ModelPrediction? {
        return try {
            val result = naiveBayesRepository.getTopPrediction(title)
            result.getOrNull()?.let { pred ->
                ModelPrediction(
                    modelName = MODEL_NAIVE_BAYES,
                    category = pred.category,
                    confidence = pred.probability,
                    weight = 0.0  // Will be filled in later
                )
            }
        } catch (e: Exception) {
            logError("Naive Bayes prediction failed", e)
            null
        }
    }

    /**
     * Combine predictions from both models using weighted voting
     *
     * Algorithm:
     * - If both models agree: high confidence
     * - If they disagree: use weighted average to decide
     * - Weight × Confidence = contribution to final decision
     */
    private fun combineePredictions(
        tflitePred: ModelPrediction?,
        naiveBayesPred: ModelPrediction?,
        weights: ModelWeights
    ): EnsemblePrediction {

        // Case 1: Both models have predictions
        if (tflitePred != null && naiveBayesPred != null) {

            // Add weights to predictions
            val tfliteWeighted = tflitePred.copy(weight = weights.tfliteWeight)
            val naiveBayesWeighted = naiveBayesPred.copy(weight = weights.naiveBayesWeight)

            // If both agree, high confidence!
            if (tflitePred.category == naiveBayesPred.category) {
                return EnsemblePrediction(
                    finalCategory = tflitePred.category,
                    confidence = 0.95,  // Very confident when models agree
                    tflitePrediction = tfliteWeighted,
                    naiveBayesPrediction = naiveBayesWeighted,
                    weights = weights,
                    explanation = "Both models agree"
                )
            }

            // Models disagree - use weighted voting
            val tfliteScore = tflitePred.confidence * weights.tfliteWeight
            val naiveBayesScore = naiveBayesPred.confidence * weights.naiveBayesWeight

            return if (tfliteScore > naiveBayesScore) {
                EnsemblePrediction(
                    finalCategory = tflitePred.category,
                    confidence = tfliteScore / (tfliteScore + naiveBayesScore),
                    tflitePrediction = tfliteWeighted,
                    naiveBayesPrediction = naiveBayesWeighted,
                    weights = weights,
                    explanation = "TFLite has higher weighted score"
                )
            } else {
                EnsemblePrediction(
                    finalCategory = naiveBayesPred.category,
                    confidence = naiveBayesScore / (tfliteScore + naiveBayesScore),
                    tflitePrediction = tfliteWeighted,
                    naiveBayesPrediction = naiveBayesWeighted,
                    weights = weights,
                    explanation = "Your history has higher weighted score"
                )
            }
        }

        // Case 2: Only TFLite has prediction
        if (tflitePred != null) {
            return EnsemblePrediction(
                finalCategory = tflitePred.category,
                confidence = tflitePred.confidence * 0.7,  // Lower confidence (only one model)
                tflitePrediction = tflitePred.copy(weight = 1.0),
                naiveBayesPrediction = null,
                weights = weights,
                explanation = "Only TFLite available (Naive Bayes needs more training)"
            )
        }

        // Case 3: Only Naive Bayes has prediction
        if (naiveBayesPred != null) {
            return EnsemblePrediction(
                finalCategory = naiveBayesPred.category,
                confidence = naiveBayesPred.confidence * 0.7,
                tflitePrediction = null,
                naiveBayesPrediction = naiveBayesPred.copy(weight = 1.0),
                weights = weights,
                explanation = "Only your history available"
            )
        }

        // Case 4: Neither model has prediction (shouldn't happen, but handle it)
        return EnsemblePrediction(
            finalCategory = "Other",
            confidence = 0.3,
            tflitePrediction = null,
            naiveBayesPrediction = null,
            weights = weights,
            explanation = "No predictions available"
        )
    }

    /**
     * Update model performance statistics
     */
    private suspend fun updateModelPerformance(modelName: String, wasCorrect: Boolean) {
        if (wasCorrect) {
            modelPerformanceDao.incrementCorrect(modelName)
        } else {
            modelPerformanceDao.incrementTotal(modelName)
        }
    }

    /**
     * Adjust model weights based on performance
     *
     * Algorithm: Exponential Moving Average
     * - If model correct: weight += α × (1 - weight)
     * - If model wrong: weight -= α × weight
     * - Then normalize to sum to 1.0
     * - Enforce min/max bounds
     */
    private suspend fun adjustWeights(tfliteCorrect: Boolean, naiveBayesCorrect: Boolean) {
        val currentWeights = getCurrentWeights()

        var newTfliteWeight = currentWeights.tfliteWeight
        var newNaiveBayesWeight = currentWeights.naiveBayesWeight

        // Update TFLite weight
        if (tfliteCorrect) {
            newTfliteWeight += LEARNING_RATE * (1.0 - newTfliteWeight)
        } else {
            newTfliteWeight -= LEARNING_RATE * newTfliteWeight
        }

        // Update Naive Bayes weight
        if (naiveBayesCorrect) {
            newNaiveBayesWeight += LEARNING_RATE * (1.0 - newNaiveBayesWeight)
        } else {
            newNaiveBayesWeight -= LEARNING_RATE * newNaiveBayesWeight
        }

        // Normalize to sum to 1.0
        val sum = newTfliteWeight + newNaiveBayesWeight
        newTfliteWeight /= sum
        newNaiveBayesWeight /= sum

        // Enforce bounds (prevent any model from dominating completely)
        newTfliteWeight = newTfliteWeight.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
        newNaiveBayesWeight = newNaiveBayesWeight.coerceIn(MIN_WEIGHT, MAX_WEIGHT)

        // Re-normalize after bounds
        val boundedSum = newTfliteWeight + newNaiveBayesWeight
        newTfliteWeight /= boundedSum
        newNaiveBayesWeight /= boundedSum

        // Save to database
        modelPerformanceDao.updateWeight(MODEL_TFLITE, newTfliteWeight)
        modelPerformanceDao.updateWeight(MODEL_NAIVE_BAYES, newNaiveBayesWeight)

        logWeightUpdate(currentWeights, ModelWeights(newTfliteWeight, newNaiveBayesWeight))
    }

    /**
     * Get ensemble statistics for UI display
     */
    suspend fun getEnsembleStats(): EnsembleStats = withContext(Dispatchers.IO) {
        val tflitePerf = modelPerformanceDao.getPerformance(MODEL_TFLITE)
        val naiveBayesPerf = modelPerformanceDao.getPerformance(MODEL_NAIVE_BAYES)

        val tfliteAccuracy = if (tflitePerf != null && tflitePerf.totalCount > 0) {
            tflitePerf.correctCount.toDouble() / tflitePerf.totalCount.toDouble()
        } else 0.0

        val naiveBayesAccuracy = if (naiveBayesPerf != null && naiveBayesPerf.totalCount > 0) {
            naiveBayesPerf.correctCount.toDouble() / naiveBayesPerf.totalCount.toDouble()
        } else 0.0

        EnsembleStats(
            tfliteWeight = tflitePerf?.currentWeight ?: 0.6,
            naiveBayesWeight = naiveBayesPerf?.currentWeight ?: 0.4,
            tfliteAccuracy = tfliteAccuracy,
            naiveBayesAccuracy = naiveBayesAccuracy,
            tflitePredictions = tflitePerf?.totalCount ?: 0,
            naiveBayesPredictions = naiveBayesPerf?.totalCount ?: 0
        )
    }

    // Logging helpers
    private fun logPrediction(title: String, prediction: EnsemblePrediction) {
        try {
            Log.d(TAG, "Predicted '$title' → ${prediction.finalCategory} " +
                    "(confidence: ${String.format("%.2f", prediction.confidence)}, " +
                    "weights: T=${String.format("%.2f", prediction.weights.tfliteWeight)}, " +
                    "NB=${String.format("%.2f", prediction.weights.naiveBayesWeight)})")
        } catch (e: Exception) {
            println("[$TAG] Predicted '$title' → ${prediction.finalCategory}")
        }
    }

    private fun logLearning(title: String, tfliteCorrect: Boolean, naiveBayesCorrect: Boolean, userChoice: String) {
        try {
            Log.d(TAG, "Learning from '$title' → '$userChoice' " +
                    "(TFLite: ${if (tfliteCorrect) "✓" else "✗"}, " +
                    "NB: ${if (naiveBayesCorrect) "✓" else "✗"})")
        } catch (e: Exception) {
            println("[$TAG] Learning from '$title' → '$userChoice'")
        }
    }

    private fun logWeightUpdate(old: ModelWeights, new: ModelWeights) {
        try {
            Log.d(TAG, "Weights updated: TFLite ${String.format("%.2f", old.tfliteWeight)} → " +
                    "${String.format("%.2f", new.tfliteWeight)}, " +
                    "NB ${String.format("%.2f", old.naiveBayesWeight)} → " +
                    String.format("%.2f", new.naiveBayesWeight)
            )
        } catch (e: Exception) {
            println("[$TAG] Weights updated")
        }
    }

    private fun logError(message: String, e: Exception) {
        try {
            Log.e(TAG, message, e)
        } catch (logEx: Exception) {
            println("[$TAG] ERROR: $message - ${e.message}")
        }
    }
}

/**
 * Ensemble prediction result
 * Contains predictions from both models and the final weighted decision
 */
data class EnsemblePrediction(
    val finalCategory: String,          // The final category chosen by ensemble
    val confidence: Double,              // Overall confidence (0.0 to 1.0)
    val tflitePrediction: ModelPrediction?,     // TFLite's prediction
    val naiveBayesPrediction: ModelPrediction?, // Naive Bayes prediction
    val weights: ModelWeights,           // Current model weights
    val explanation: String              // Why this decision was made
)

/**
 * Prediction from a single model
 */
data class ModelPrediction(
    val modelName: String,    // "TFLite" or "NaiveBayes"
    val category: String,     // Predicted category
    val confidence: Double,   // Model's confidence (0.0 to 1.0)
    val weight: Double        // Model's current weight in ensemble
)

/**
 * Current model weights
 */
data class ModelWeights(
    val tfliteWeight: Double,      // Weight for TFLite (0.0 to 1.0)
    val naiveBayesWeight: Double   // Weight for Naive Bayes (0.0 to 1.0)
    // Note: tfliteWeight + naiveBayesWeight should equal 1.0
)

/**
 * Ensemble statistics for UI display
 */
data class EnsembleStats(
    val tfliteWeight: Double,           // Current TFLite weight
    val naiveBayesWeight: Double,       // Current Naive Bayes weight
    val tfliteAccuracy: Double,         // TFLite accuracy (0.0 to 1.0)
    val naiveBayesAccuracy: Double,     // Naive Bayes accuracy (0.0 to 1.0)
    val tflitePredictions: Int,         // Total predictions from TFLite
    val naiveBayesPredictions: Int      // Total predictions from Naive Bayes
)