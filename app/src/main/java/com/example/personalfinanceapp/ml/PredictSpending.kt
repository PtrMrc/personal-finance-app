package com.example.personalfinanceapp.ml

/**
 * Result of the smart spending forecast.
 *
 * @param forecastedTotal       Projected month-end total spend (HUF).
 * @param dailyRate             Blended daily rate used for the projection.
 * @param outlierCount          Days excluded from current-month rate (spend > 3x median).
 * @param daysOfData            Days this month that actually had spending recorded.
 * @param confidence            0.0-1.0 — daysOfData / daysInMonth.
 * @param usedHistoricalData    True when past-month history contributed to the rate.
 * @param categoryForecasts     Per-category projected month-end totals.
 */
data class PredictionResult(
    val forecastedTotal: Double,
    val dailyRate: Double,
    val outlierCount: Int = 0,
    val daysOfData: Int = 0,
    val confidence: Double = 0.0,
    val usedHistoricalData: Boolean = false,
    val categoryForecasts: Map<String, Double> = emptyMap()
)

object MLMath {

    /**
     * Calculates a blended month-end spending forecast.
     *
     * Blending strategy (thesis-defensible):
     *   historicalRate  = weighted average of:
     *                       - last 3 complete months' daily averages (60 % weight)
     *                       - same month last year daily average    (40 % weight)
     *   currentRate     = median-cleaned daily rate from this month so far
     *   blendedRate     = historicalRate * (1 - confidence) + currentRate * confidence
     *
     *   At day 3  → confidence ≈ 0.10 → 90 % history, 10 % current month
     *   At day 15 → confidence ≈ 0.50 → 50 / 50 blend
     *   At day 25 → confidence ≈ 0.83 → 17 % history, 83 % current month
     *
     * When there is no historical data (new user) it falls back to current-month only.
     *
     * Outlier detection uses the median (not mean+2σ).  One large purchase (e.g. a
     * plane ticket) raises mean+σ and survives the old filter; it cannot raise the
     * median, so median*3 correctly excludes it.
     *
     * @param dailyData           Spend per calendar day from month-start to today
     *                            (zeros for no-spend days are fine).
     * @param totalDaysInMonth    Calendar length of the current month.
     * @param historicalDailyRate Pre-computed baseline from past months (0.0 = no history).
     *                            Call [computeHistoricalDailyRate] in the ViewModel to get this.
     * @param categoryDailyData   Optional map of category to per-day amounts for the same
     *                            date range. Pass empty map to skip per-category forecasts.
     */
    fun calculateSmartForecast(
        dailyData: List<Double>,
        totalDaysInMonth: Int,
        historicalDailyRate: Double = 0.0,
        categoryDailyData: Map<String, List<Double>> = emptyMap()
    ): PredictionResult {
        if (dailyData.isEmpty()) return PredictionResult(0.0, 0.0)

        val currentTotal = dailyData.sum()
        val daysRemaining = (totalDaysInMonth - dailyData.size).coerceAtLeast(0)

        val spendingDays = dailyData.filter { it > 0.0 }
        val daysOfData = spendingDays.size
        val confidence = (daysOfData.toDouble() / totalDaysInMonth.toDouble()).coerceIn(0.0, 1.0)

        // Guard: with fewer than 3 spending days we cannot do meaningful outlier
        // detection — the outlier IS the only data point (e.g. one day with a plane
        // ticket sums everything and sets an absurd daily rate).
        // If historical data exists the blend already handles this safely (confidence
        // is near 0 so history dominates). If there is no history either, we return
        // null-equivalent so the UI hides the card rather than show a nonsense number.
        if (daysOfData < 3 && historicalDailyRate <= 0.0) {
            return PredictionResult(
                forecastedTotal = 0.0,
                dailyRate = 0.0,
                daysOfData = daysOfData,
                confidence = confidence
            )
        }

        val (currentMonthRate, outlierCount) = medianCleanedRate(spendingDays)

        // Blend current-month rate with historical baseline.
        // With daysOfData < 3 but history available, confidence is near 0 so the
        // blend already heavily favours history — no special case needed.
        val (dailyRate, usedHistory) = if (historicalDailyRate > 0.0) {
            val blended = historicalDailyRate * (1.0 - confidence) + currentMonthRate * confidence
            Pair(blended, true)
        } else {
            Pair(currentMonthRate, false)
        }

        val forecastedTotal = currentTotal + (dailyRate * daysRemaining)

        // Per-category forecasts use current-month median-cleaned rate only.
        // Historical per-category blending is not implemented — the overall blend
        // already captures the baseline and adding per-category history would
        // require significantly more data to be reliable.
        val categoryForecasts: Map<String, Double> = categoryDailyData
            .mapValues { (_, catData) ->
                val catSpendingDays = catData.filter { it > 0.0 }
                if (catSpendingDays.isEmpty()) return@mapValues 0.0
                val (catRate, _) = medianCleanedRate(catSpendingDays)
                catData.sum() + (catRate * daysRemaining)
            }
            .filter { (_, v) -> v > 0.0 }

        return PredictionResult(
            forecastedTotal = forecastedTotal,
            dailyRate = dailyRate,
            outlierCount = outlierCount,
            daysOfData = daysOfData,
            confidence = confidence,
            usedHistoricalData = usedHistory,
            categoryForecasts = categoryForecasts
        )
    }

    /**
     * Computes a historical daily spending baseline from two sources:
     *   - Last 3 complete months' average daily spend (60 % weight)
     *   - Same calendar month last year average daily spend (40 % weight)
     *
     * Only months that actually have data contribute. Returns 0.0 if there is no
     * historical data at all (i.e. user is in their first months of using the app).
     *
     * Call this from the ViewModel and pass the result to [calculateSmartForecast].
     *
     * @param expenses       Full expense list from the repository.
     * @param today          Reference date (pass LocalDate.now() from the ViewModel).
     * @param currentYearMonth The month being forecast — excluded from history.
     */
    fun computeHistoricalDailyRate(
        expenses: List<com.example.personalfinanceapp.data.Expense>,
        today: java.time.LocalDate,
        currentYearMonth: java.time.YearMonth
    ): Double {
        val zone = java.time.ZoneId.systemDefault()

        fun dailyRateForMonth(ym: java.time.YearMonth): Double? {
            val total = expenses.filter { expense ->
                if (expense.isIncome) return@filter false
                val d = java.time.Instant.ofEpochMilli(expense.date)
                    .atZone(zone).toLocalDate()
                java.time.YearMonth.from(d) == ym
            }.sumOf { it.amount }
            return if (total > 0.0) total / ym.lengthOfMonth() else null
        }

        // Last 3 complete months (not including the month being forecast)
        val last3Rates = (1..3).mapNotNull { monthsAgo ->
            val ym = currentYearMonth.minusMonths(monthsAgo.toLong())
            dailyRateForMonth(ym)
        }
        val last3Avg = if (last3Rates.isNotEmpty()) last3Rates.average() else 0.0

        // Same month last year
        val sameMonthLastYear = currentYearMonth.minusYears(1)
        val sameMonthRate = dailyRateForMonth(sameMonthLastYear) ?: 0.0

        return when {
            last3Avg > 0.0 && sameMonthRate > 0.0 ->
                last3Avg * 0.6 + sameMonthRate * 0.4
            last3Avg > 0.0 -> last3Avg
            sameMonthRate > 0.0 -> sameMonthRate
            else -> 0.0  // New user — no historical data yet
        }
    }

    /**
     * Returns (cleanedMeanRate, outlierCount) for a list of non-zero daily spend values.
     * Outlier threshold = median x 3. Falls back to mean x 2 when median is zero.
     */
    private fun medianCleanedRate(spendingDays: List<Double>): Pair<Double, Int> {
        if (spendingDays.isEmpty()) return Pair(0.0, 0)

        val sorted = spendingDays.sorted()
        val median = when {
            sorted.size == 1 -> sorted[0]
            sorted.size % 2 == 0 ->
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            else -> sorted[sorted.size / 2]
        }

        val threshold = if (median > 0.0) median * 3.0 else spendingDays.average() * 2.0
        val cleanDays = spendingDays.filter { it <= threshold }
        val outlierCount = spendingDays.size - cleanDays.size
        val rate = if (cleanDays.isNotEmpty()) cleanDays.average() else spendingDays.average()

        return Pair(rate, outlierCount)
    }
}