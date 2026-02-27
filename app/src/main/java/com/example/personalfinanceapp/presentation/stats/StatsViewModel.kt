package com.example.personalfinanceapp.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.ml.MLMath
import com.example.personalfinanceapp.ml.PredictionResult
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val expenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val modelProducer = CartesianChartModelProducer()

    private val _chartPeriod = MutableStateFlow(TimePeriod.WEEK)

    val chartData: StateFlow<List<Double>> = combine(
        expenses, _chartPeriod
    ) { expList, period ->
        val today = LocalDate.now()
        var running = 0.0
        when (period) {
            TimePeriod.WEEK, TimePeriod.MONTH -> {
                val startDate = if (period == TimePeriod.WEEK) today.minusDays(6) else today.minusDays(29)
                val points = mutableListOf<Double>()
                var curr = startDate
                while (!curr.isAfter(today)) {
                    val dayEntries = expList.filter {
                        Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == curr
                    }
                    running += dayEntries.filter { it.isIncome }.sumOf { it.amount }
                    running -= dayEntries.filter { !it.isIncome }.sumOf { it.amount }
                    points.add(running)
                    curr = curr.plusDays(1)
                }
                points
            }
            TimePeriod.YEAR -> {
                (11 downTo 0).map { monthsAgo ->
                    val ym = YearMonth.from(today).minusMonths(monthsAgo.toLong())
                    val monthEntries = expList.filter {
                        val d = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                        YearMonth.from(d) == ym
                    }
                    running += monthEntries.filter { it.isIncome }.sumOf { it.amount }
                    running -= monthEntries.filter { !it.isIncome }.sumOf { it.amount }
                    running
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setChartPeriod(period: TimePeriod) { _chartPeriod.value = period }

    init {
        viewModelScope.launch {
            expenses.collect { expList ->
                val today = LocalDate.now()
                val dailyTotals = getDailyTotalsForRange(expList, today.minusDays(6), today)
                modelProducer.runTransaction {
                    columnSeries { series(dailyTotals.map { it.toFloat() }) }
                }
            }
        }
    }

    val spendingForecast: StateFlow<PredictionResult?> = expenses.map { expList ->
        val today = LocalDate.now()
        val daysInMonth = YearMonth.from(today).lengthOfMonth()
        val startOfMonth = today.withDayOfMonth(1)
        val monthToDateTotals = getDailyTotalsForRange(expList, startOfMonth, today)
        if (monthToDateTotals.isNotEmpty()) MLMath.calculateSmartForecast(monthToDateTotals, daysInMonth)
        else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val totalThisWeek: StateFlow<Double> = expenses.map { expList ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        expList.filter { it.date >= weekAgo && !it.isIncome }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val averageDailySpending: StateFlow<Double> = totalThisWeek.map { total ->
        total / 7.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val categoryBreakdown: StateFlow<Map<String, Double>> = expenses.map { expList ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        expList
            .filter { it.date >= weekAgo && !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val topCategory: StateFlow<Pair<String?, Double>?> = categoryBreakdown.map { breakdown ->
        breakdown.maxByOrNull { it.value }?.let { (category, amount) -> category to amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val monthlyComparison: StateFlow<Pair<Double, Double>> = expenses.map { expList ->
        val today = LocalDate.now()
        val firstDayOfMonth = today.withDayOfMonth(1)
        val firstDayOfLastMonth = firstDayOfMonth.minusMonths(1)
        val thisMonthStart = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val lastMonthStart = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val lastMonthEnd = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val thisMonthTotal = expList.filter { it.date >= thisMonthStart && !it.isIncome }.sumOf { it.amount }
        val lastMonthTotal = expList.filter { it.date in lastMonthStart..<lastMonthEnd && !it.isIncome }.sumOf { it.amount }
        Pair(thisMonthTotal, lastMonthTotal)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0.0, 0.0))

    val spendingTrend: StateFlow<String> = expenses.map { expList ->
        val today = LocalDate.now()
        val dailyTotals = getDailyTotalsForRange(expList, today.minusDays(6), today)
        if (dailyTotals.size < 2) "Stabil"
        else {
            val firstHalf = dailyTotals.take(3).average()
            val secondHalf = dailyTotals.takeLast(3).average()
            when {
                secondHalf > firstHalf * 1.1 -> "Növekvő"
                secondHalf < firstHalf * 0.9 -> "Csökkenő"
                else -> "Stabil"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Stabil")

    val transactionCount: StateFlow<Int> = expenses.map { expList ->
        val weekAgo = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        expList.count { it.date >= weekAgo && !it.isIncome }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val noSpendDaysThisWeek: StateFlow<Int> = expenses.map { expList ->
        val today = LocalDate.now()
        var noSpendCount = 0
        for (i in 0..6) {
            val targetDate = today.minusDays(i.toLong())
            val dailyExpense = expList.filter {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == targetDate && !it.isIncome
            }.sumOf { it.amount }
            if (dailyExpense <= 0.0) {
                noSpendCount++
            }
        }
        noSpendCount
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private fun getDailyTotalsForRange(
        expenses: List<com.example.personalfinanceapp.data.Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Double> {
        val results = mutableListOf<Double>()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            results.add(expenses.filter {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == curr && !it.isIncome
            }.sumOf { it.amount })
            curr = curr.plusDays(1)
        }
        return results
    }

    fun updatePeriod(period: TimePeriod) {
        viewModelScope.launch {
            expenses.collect { expList ->
                val today = LocalDate.now()
                val dateRange = when (period) {
                    TimePeriod.WEEK  -> (0..6)
                    TimePeriod.MONTH -> (0..29)
                    TimePeriod.YEAR  -> (0..364 step 7)
                }
                val totals = dateRange.map { daysAgo ->
                    val targetDate = today.minusDays(daysAgo.toLong())
                    expList.filter {
                        Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == targetDate && !it.isIncome
                    }.sumOf { it.amount }.toFloat()
                }.reversed()
                modelProducer.runTransaction { columnSeries { series(totals) } }
            }
        }
    }
}