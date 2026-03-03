package com.example.personalfinanceapp.ml

/**
 * Interface for the TFLite expense classifier.
 * Allows the real ExpenseClassifier to be replaced with a fake in unit tests.
 */
interface TFLiteClassifierInterface {
    fun classify(text: String): String?
}