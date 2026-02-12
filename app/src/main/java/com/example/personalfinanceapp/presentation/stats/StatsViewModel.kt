package com.example.personalfinanceapp.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.ml.MLMath
import com.example.personalfinanceapp.ml.PredictionResult
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Vico Chart Model Producer
    val modelProducer = CartesianChartModelProducer()

    // Update chart data from database (Last 7 Days)
    init {
        viewModelScope.launch {
            repository.allExpenses.collect { expenses ->
                val today = LocalDate.now()
                val startDate = today.minusDays(6)

                val dailyTotals = getDailyTotalsForRange(expenses, startDate, today)

                modelProducer.runTransaction {
                    columnSeries {
                        series(dailyTotals.map { it.toFloat() })
                    }
                }
            }
        }
    }

    // ML Spending Forecast (Current Month)
    val spendingForecast: StateFlow<PredictionResult?> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val daysInMonth = YearMonth.from(today).lengthOfMonth()

        // Start from the 1st of current month until today
        val startOfMonth = today.withDayOfMonth(1)
        val monthToDateTotals = getDailyTotalsForRange(expenses, startOfMonth, today)

        if (monthToDateTotals.isNotEmpty()) {
            MLMath.calculateSmartForecast(monthToDateTotals, daysInMonth)
        } else null

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Total Spending This Week
    val totalThisWeek: StateFlow<Double> = repository.allExpenses.map { expenses ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        expenses.filter { it.date >= weekAgo && !it.isIncome }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Average Daily Spending (Last 7 days)
    val averageDailySpending: StateFlow<Double> = totalThisWeek.map { total ->
        total / 7.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Breakdown (Last 7 days)
    val categoryBreakdown: StateFlow<Map<String, Double>> = repository.allExpenses.map { expenses ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        expenses
            .filter { it.date >= weekAgo && !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, expenseList) ->
                expenseList.sumOf { it.amount }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Top Spending Category
    val topCategory: StateFlow<Pair<String?, Double>?> = categoryBreakdown.map { breakdown ->
        breakdown.maxByOrNull { it.value }?.let { (category, amount) ->
            category to amount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Monthly Spending Comparison (This month vs Last month)
    val monthlyComparison: StateFlow<Pair<Double, Double>> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val firstDayOfMonth = today.withDayOfMonth(1)
        val firstDayOfLastMonth = firstDayOfMonth.minusMonths(1)

        val thisMonthStart = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val lastMonthStart = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val lastMonthEnd = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val thisMonthTotal = expenses
            .filter { it.date >= thisMonthStart && !it.isIncome }
            .sumOf { it.amount }

        val lastMonthTotal = expenses
            .filter { it.date in lastMonthStart..<lastMonthEnd && !it.isIncome }
            .sumOf { it.amount }

        Pair(thisMonthTotal, lastMonthTotal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Spending Trend
    val spendingTrend: StateFlow<String> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val dailyTotals = getDailyTotalsForRange(expenses, today.minusDays(6), today)

        if (dailyTotals.size < 2) {
            "Stabil"
        } else {
            val firstHalf = dailyTotals.take(3).average()
            val secondHalf = dailyTotals.takeLast(3).average()

            when {
                secondHalf > firstHalf * 1.1 -> "Növekvő"
                secondHalf < firstHalf * 0.9 -> "Csökkenő"
                else -> "Stabil"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stabil")

    // Number of Transactions This Week
    val transactionCount: StateFlow<Int> = repository.allExpenses.map { expenses ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        expenses.count { it.date >= weekAgo && !it.isIncome }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Helper: Logic for grouping expenses by day accurately
    private fun getDailyTotalsForRange(
        expenses: List<com.example.personalfinanceapp.data.Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Double> {
        val results = mutableListOf<Double>()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            val daySum = expenses
                .filter {
                    val expenseDate = Instant.ofEpochMilli(it.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    expenseDate == curr && !it.isIncome
                }
                .sumOf { it.amount }
            results.add(daySum)
            curr = curr.plusDays(1)
        }
        return results
    }

    // Future: Method for different time periods
    fun updatePeriod(period: TimePeriod) {
        viewModelScope.launch {
            repository.allExpenses.collect { expenses ->
                val today = LocalDate.now()
                val dateRange = when (period) {
                    TimePeriod.WEEK -> (0..6)
                    TimePeriod.MONTH -> (0..29)
                    TimePeriod.YEAR -> (0..364 step 7)
                }

                val totals = dateRange.map { daysAgo ->
                    val targetDate = today.minusDays(daysAgo.toLong())
                    expenses.filter {
                        val d = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                        d == targetDate && !it.isIncome
                    }.sumOf { it.amount }.toFloat()
                }.reversed()

                modelProducer.runTransaction {
                    columnSeries { series(totals) }
                }
            }
        }
    }
}