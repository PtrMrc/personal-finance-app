package com.example.personalfinanceapp.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // 1. Vico Chart Model Producer
    val modelProducer = CartesianChartModelProducer()

    // 2. Update chart data from database
    init {
        viewModelScope.launch {
            repository.allExpenses.collect { expenses ->
                val today = LocalDate.now()
                val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

                val dailyTotals = last7Days.map { date ->
                    val startOfDay = java.sql.Date.valueOf(date.toString()).time
                    val endOfDay = java.sql.Date.valueOf(date.plusDays(1).toString()).time

                    expenses
                        .filter { it.date in startOfDay until endOfDay }
                        .sumOf { it.amount }
                        .toFloat()
                }

                modelProducer.runTransaction {
                    columnSeries {
                        series(dailyTotals)
                    }
                }
            }
        }
    }

    // 3. Total Spending This Week
    val totalThisWeek: StateFlow<Double> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = java.sql.Date.valueOf(weekAgo.toString()).time

        expenses
            .filter { it.date >= weekAgoMillis }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 4. Average Daily Spending
    val averageDailySpending: StateFlow<Double> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = java.sql.Date.valueOf(weekAgo.toString()).time

        val weekExpenses = expenses.filter { it.date >= weekAgoMillis }
        val total = weekExpenses.sumOf { it.amount }

        if (weekExpenses.isNotEmpty()) total / 7.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 5. Category Breakdown (Map of category name to total amount)
    val categoryBreakdown: StateFlow<Map<String, Double>> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = java.sql.Date.valueOf(weekAgo.toString()).time

        expenses
            .filter { it.date >= weekAgoMillis }
            .groupBy { it.category }
            .mapValues { (_, expenseList) ->
                expenseList.sumOf { it.amount }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 6. Top Spending Category
    val topCategory: StateFlow<Pair<String?, Double>?> = categoryBreakdown.map { breakdown ->
        breakdown.maxByOrNull { it.value }?.let { (category, amount) ->
            category to amount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 7. Monthly Spending Comparison (this month vs last month)
    val monthlyComparison: StateFlow<Pair<Double, Double>> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val firstDayOfMonth = today.withDayOfMonth(1)
        val firstDayOfLastMonth = firstDayOfMonth.minusMonths(1)

        val thisMonthStart = java.sql.Date.valueOf(firstDayOfMonth.toString()).time
        val lastMonthStart = java.sql.Date.valueOf(firstDayOfLastMonth.toString()).time
        val lastMonthEnd = java.sql.Date.valueOf(firstDayOfMonth.toString()).time

        val thisMonthTotal = expenses
            .filter { it.date >= thisMonthStart }
            .sumOf { it.amount }

        val lastMonthTotal = expenses
            .filter { it.date >= lastMonthStart && it.date < lastMonthEnd }
            .sumOf { it.amount }

        Pair(thisMonthTotal, lastMonthTotal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // 8. Spending Trend (increasing, decreasing, or stable)
    val spendingTrend: StateFlow<String> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

        val dailyTotals = last7Days.map { date ->
            val startOfDay = java.sql.Date.valueOf(date.toString()).time
            val endOfDay = java.sql.Date.valueOf(date.plusDays(1).toString()).time

            expenses
                .filter { it.date in startOfDay until endOfDay }
                .sumOf { it.amount }
        }

        if (dailyTotals.size < 2) {
            "Stable"
        } else {
            val firstHalf = dailyTotals.take(dailyTotals.size / 2).average()
            val secondHalf = dailyTotals.drop(dailyTotals.size / 2).average()

            when {
                secondHalf > firstHalf * 1.1 -> "Increasing"
                secondHalf < firstHalf * 0.9 -> "Decreasing"
                else -> "Stable"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stable")

    // 9. Largest Single Expense
    val largestExpense: StateFlow<Pair<String?, Double>?> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = java.sql.Date.valueOf(weekAgo.toString()).time

        expenses
            .filter { it.date >= weekAgoMillis }
            .maxByOrNull { it.amount }
            ?.let { expense ->
                expense.description to expense.amount
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 10. Number of Transactions This Week
    val transactionCount: StateFlow<Int> = repository.allExpenses.map { expenses ->
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = java.sql.Date.valueOf(weekAgo.toString()).time

        expenses.count { it.date >= weekAgoMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Future: Add methods for different time periods (month, year)
    fun updatePeriod(period: TimePeriod) {
        viewModelScope.launch {
            repository.allExpenses.collect { expenses ->
                val today = LocalDate.now()
                val (days, dateRange) = when (period) {
                    TimePeriod.WEEK -> 7 to (0..6)
                    TimePeriod.MONTH -> 30 to (0..29)
                    TimePeriod.YEAR -> 365 to (0..364 step 7) // Weekly data points for year
                }

                val dates = dateRange.map { today.minusDays(it.toLong()) }.reversed()

                val totals = dates.map { date ->
                    val startOfDay = java.sql.Date.valueOf(date.toString()).time
                    val endOfDay = if (period == TimePeriod.YEAR) {
                        java.sql.Date.valueOf(date.plusDays(7).toString()).time
                    } else {
                        java.sql.Date.valueOf(date.plusDays(1).toString()).time
                    }

                    expenses
                        .filter { it.date in startOfDay until endOfDay }
                        .sumOf { it.amount }
                        .toFloat()
                }

                modelProducer.runTransaction {
                    columnSeries {
                        series(totals)
                    }
                }
            }
        }
    }
}