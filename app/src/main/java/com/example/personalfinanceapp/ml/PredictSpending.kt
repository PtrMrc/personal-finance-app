package com.example.personalfinanceapp.ml

import kotlin.math.pow
import kotlin.math.sqrt

data class PredictionResult(val forecastedTotal: Double, val dailyRate: Double)

object MLMath {
    /**
     * Calculates forecast while ignoring extreme outliers
     */
    fun calculateSmartForecast(dailyData: List<Double>, totalDaysInMonth: Int): PredictionResult {
        if (dailyData.isEmpty()) return PredictionResult(0.0, 0.0)

        // 1. Calculate Average and Standard Deviation to find outliers
        val average = dailyData.average()
        val stdDev = sqrt(dailyData.sumOf { (it - average).pow(2.0) } / dailyData.size)

        // 2. Filter data: only keep spendings that aren't "crazy high" (e.g. within 2 standard deviations)
        val cleanData = dailyData.filter { it <= average + (2 * stdDev) }

        // 3. Simple Linear Regression on the "Clean" data
        // w (slope) is just the average daily spending of clean days
        val dailyRate = if (cleanData.isNotEmpty()) cleanData.average() else average

        val currentTotal = dailyData.sum()
        val daysRemaining = totalDaysInMonth - dailyData.size

        val forecastedTotal = currentTotal + (dailyRate * daysRemaining)

        return PredictionResult(forecastedTotal, dailyRate)
    }
}