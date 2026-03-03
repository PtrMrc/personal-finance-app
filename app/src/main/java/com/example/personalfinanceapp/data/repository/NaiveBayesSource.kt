package com.example.personalfinanceapp.data.repository

import com.example.personalfinanceapp.ml.CategoryPrediction

/**
 * Interface for the Naive Bayes prediction source.
 * Allows the real NaiveBayesRepository to be replaced with a fake in unit tests.
 */
interface NaiveBayesSource {
    suspend fun getTopPrediction(title: String): Result<CategoryPrediction?>
    suspend fun trainModel(title: String, category: String): Result<Unit>
}