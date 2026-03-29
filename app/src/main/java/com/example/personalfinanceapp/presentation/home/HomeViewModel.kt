package com.example.personalfinanceapp.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Budget
import com.example.personalfinanceapp.data.CategoryCorrectionStat
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.data.repository.BudgetRepository
import com.example.personalfinanceapp.data.repository.CategoryCorrectionRepository
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.data.repository.NaiveBayesRepository
import com.example.personalfinanceapp.data.repository.RecurringRepository
import com.example.personalfinanceapp.ml.AdaptiveEnsembleModel
import com.example.personalfinanceapp.ml.EnsemblePrediction
import com.example.personalfinanceapp.ml.EnsembleStats
import com.example.personalfinanceapp.ml.ExpenseClassifier
import com.example.personalfinanceapp.ml.MLMath
import com.example.personalfinanceapp.ml.PredictionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar

/**
 * UI model for a single budget category's monthly progress.
 * Computed properties (percentage, isWarning, isExceeded) derive automatically
 * from the primary data so they never go stale.
 */
data class BudgetProgress(
    val category: String,
    val limit: Double,
    val spent: Double
) {
    val percentage = if (limit > 0) spent / limit else 0.0
    val isWarning = percentage in 0.8..<1.0   // 80–99 %: amber warning
    val isExceeded = percentage >= 1.0          // 100 %+: red exceeded
}

/**
 * A spending anomaly for one category in the current epoch-week.
 * Shown as a warning card on the Home screen.
 */
data class AnomalyAlert(
    val category: String,
    val currentWeekSpend: Double,
    val historicalAverage: Double,
    val percentageAbove: Double
)

private const val HISTORY_WEEKS = 8
private const val ANOMALY_THRESHOLD = 0.5      // 50 % above historical average
private const val MIN_EXPENSES_THIS_WEEK = 2   // Avoid false alerts early in the week

/**
 * Shared ViewModel for Home, Recurring, Learning, and BudgetSetup screens.
 *
 * Owns all repositories and the ML ensemble. Exposes reactive StateFlows so
 * the UI never needs to manually refresh — Room changes propagate automatically
 * through the Flow pipeline.
 *
 * Uses manual ViewModelProvider.Factory injection (no Hilt/Dagger).
 */
class HomeViewModel(
    application: Application,
    private val budgetRepository: BudgetRepository
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val recurringRepository = RecurringRepository(database.recurringDao())
    private val naiveBayesRepository = NaiveBayesRepository(database.wordCategoryCountDao())
    private val tfliteClassifier = ExpenseClassifier(application)

    private val ensembleModel = AdaptiveEnsembleModel(
        tfliteClassifier = tfliteClassifier,
        naiveBayesRepository = naiveBayesRepository,
        modelPerformanceDao = database.modelPerformanceDao()
    )

    private val correctionRepository = CategoryCorrectionRepository(
        database.categoryCorrectionStatDao()
    )

    // ── Public data streams ───────────────────────────────────────────────────

    val correctionStats: Flow<List<CategoryCorrectionStat>> = correctionRepository.allStats
    val allExpenses: Flow<List<Expense>> = expenseRepository.allExpenses
    val totalSpending: Flow<Double?> = expenseRepository.totalSpending
    val allRecurringItems: Flow<List<RecurringItem>> = recurringRepository.allRecurringItems

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Polls ensemble statistics every 5 seconds.
     * A Room Flow on ModelPerformance would be cleaner, but the DAO does not
     * expose one — polling is a pragmatic workaround.
     */
    val ensembleStats: Flow<EnsembleStats> = flow {
        while (true) {
            try {
                emit(ensembleModel.getEnsembleStats())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting ensemble stats", e)
            }
            delay(5000)
        }
    }

    // ── Budget progress ───────────────────────────────────────────────────────

    /**
     * Reactively combines the full expense list with all budgets to produce
     * per-category progress for the current calendar month.
     *
     * combine() re-executes the lambda whenever either upstream Flow emits,
     * so the UI stays in sync without any manual refresh calls.
     *
     * The special "Összesen" category sums all monthly expenses instead of
     * filtering by category name.
     */
    val budgetProgress: StateFlow<List<BudgetProgress>> = combine(
        expenseRepository.allExpenses,
        budgetRepository.allBudgets
    ) { expenses, budgets ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthlyExpenses = expenses.filter {
            val expCal = Calendar.getInstance().apply { timeInMillis = it.date }
            expCal.get(Calendar.MONTH) == currentMonth &&
                    expCal.get(Calendar.YEAR) == currentYear &&
                    !it.isIncome
        }

        val spentByCategory = monthlyExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.map { budget ->
            val spentAmount = if (budget.category.trim().equals("Összesen", ignoreCase = true)) {
                monthlyExpenses.sumOf { it.amount }
            } else {
                spentByCategory[budget.category] ?: 0.0
            }
            BudgetProgress(
                category = budget.category,
                limit = budget.monthlyLimit,
                spent = spentAmount
            )
        }.sortedByDescending { it.percentage }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Spending forecast ─────────────────────────────────────────────────────

    /**
     * Month-end spending forecast with historical blending.
     *
     * Blends the last 3 months + same month last year as a baseline, weighted
     * against the current month's median-cleaned daily rate using confidence
     * (active days / days in month) as the interpolation parameter.
     *
     * Returns null when the algorithm cannot produce a meaningful forecast
     * (fewer than 3 spending days and no historical data).
     */
    val spendingForecast: StateFlow<PredictionResult?> = expenseRepository.allExpenses
        .map { expList ->
            val today = LocalDate.now()
            val daysInMonth = YearMonth.from(today).lengthOfMonth()
            val startOfMonth = today.withDayOfMonth(1)
            val monthToDateTotals = getDailyTotalsForRange(expList, startOfMonth, today)

            if (monthToDateTotals.isNotEmpty()) {
                val currentYearMonth = YearMonth.from(today)
                val historicalRate = MLMath.computeHistoricalDailyRate(
                    expList, today, currentYearMonth
                )
                val categories = expList.filter { !it.isIncome }.map { it.category }.distinct()
                val categoryDailyData = categories.associateWith { cat ->
                    getDailyTotalsForRangeByCategory(expList, startOfMonth, today, cat)
                }
                val result = MLMath.calculateSmartForecast(
                    monthToDateTotals, daysInMonth, historicalRate, categoryDailyData
                )
                if (result.forecastedTotal == 0.0 && result.daysOfData < 3) null else result
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Budget limits as a flat map for use in the forecast card UI. */
    val budgetLimits: StateFlow<Map<String, Double>> = budgetProgress
        .map { list -> list.associate { it.category to it.limit } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Spending anomaly detection ────────────────────────────────────────────

    /**
     * Detects unusual spending in the current epoch-week per category.
     *
     * Uses epoch-based week numbers (milliseconds / 604_800_000) rather than
     * calendar weeks. This is a known limitation — blocks may not align with
     * Monday–Sunday boundaries.
     *
     * An alert is raised when:
     *   currentWeekSpend > historicalAverage * (1 + ANOMALY_THRESHOLD)
     * with a minimum of MIN_EXPENSES_THIS_WEEK transactions to avoid
     * false positives early in the week.
     *
     * Historical average = sum of the past HISTORY_WEEKS weeks (including
     * zero-spend weeks) divided by HISTORY_WEEKS — intentionally conservative.
     */
    val anomalyAlerts: StateFlow<List<AnomalyAlert>> =
        expenseRepository.weeklySpendByCategory
            .map { rows ->
                val currentWeek = System.currentTimeMillis() / 604_800_000L
                val historyStart = currentWeek - HISTORY_WEEKS

                val byCategory = rows.groupBy { it.category }
                val alerts = mutableListOf<AnomalyAlert>()

                for ((category, weekRows) in byCategory) {
                    val currentRow = weekRows.firstOrNull { it.weekNumber == currentWeek }
                    if ((currentRow?.expenseCount ?: 0) < MIN_EXPENSES_THIS_WEEK) continue

                    val currentSpend = currentRow?.total ?: 0.0
                    val historicRows = weekRows.filter {
                        it.weekNumber in historyStart until currentWeek
                    }
                    val historicalTotal = historicRows.sumOf { it.total }
                    val historicalAverage = historicalTotal / HISTORY_WEEKS

                    if (historicalAverage <= 0.0) continue

                    val fractionAbove = (currentSpend - historicalAverage) / historicalAverage
                    if (fractionAbove > ANOMALY_THRESHOLD) {
                        alerts += AnomalyAlert(
                            category = category,
                            currentWeekSpend = currentSpend,
                            historicalAverage = historicalAverage,
                            percentageAbove = fractionAbove
                        )
                    }
                }

                alerts.sortedByDescending { it.percentageAbove }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Budget CRUD ───────────────────────────────────────────────────────────

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            budgetRepository.setBudget(Budget(category, limit))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(Budget(category, 0.0))
        }
    }

    // ── ML prediction ─────────────────────────────────────────────────────────

    /**
     * Runs the adaptive ensemble (TFLite + Naive Bayes) on [title].
     * Returns a safe fallback prediction on error so the UI never crashes.
     */
    suspend fun predictCategoryEnsemble(title: String): EnsemblePrediction {
        return try {
            ensembleModel.predict(title)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to predict category", e)
            EnsemblePrediction(
                finalCategory = "Egyéb",
                confidence = 0.3,
                tflitePrediction = null,
                naiveBayesPrediction = null,
                weights = com.example.personalfinanceapp.ml.ModelWeights(0.6, 0.4),
                explanation = "Error occurred"
            )
        }
    }

    /**
     * Records the user's final category choice after saving an expense.
     *
     * Triggers two side effects:
     *   1. EMA weight adjustment + Naive Bayes training via [ensembleModel]
     *   2. Correction stat update (AI suggestion vs. user choice) via [correctionRepository]
     */
    fun recordCategoryChoice(
        title: String,
        ensemblePrediction: EnsemblePrediction,
        userChoice: String
    ) {
        viewModelScope.launch {
            try {
                ensembleModel.recordUserChoice(title, ensemblePrediction, userChoice)
                correctionRepository.recordPrediction(
                    aiSuggestedCategory = ensemblePrediction.finalCategory,
                    userChosenCategory = userChoice
                )
                Log.d("HomeViewModel", "Recorded user choice. " +
                        "AI=${ensemblePrediction.finalCategory}, User=$userChoice")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to record user choice", e)
            }
        }
    }

    // ── Expense / recurring CRUD ──────────────────────────────────────────────

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add expense", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete expense", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás frissítésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to update expense", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.addRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add recurring item", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun updateRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.updateRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel frissítésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to update recurring item", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete recurring item", e)
                }
                .onSuccess { _errorMessage.value = null }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns one Double per calendar day in [startDate]..[endDate].
     * Zero-spend days are included so the forecast sees the full month shape.
     * Epoch-millis dates are converted via the system default timezone.
     */
    private fun getDailyTotalsForRange(
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Double> {
        val results = mutableListOf<Double>()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            results.add(expenses.filter {
                Instant.ofEpochMilli(it.date)
                    .atZone(ZoneId.systemDefault()).toLocalDate() == curr
                        && !it.isIncome
            }.sumOf { it.amount })
            curr = curr.plusDays(1)
        }
        return results
    }

    /** Same as [getDailyTotalsForRange] but filtered to a single [category]. */
    private fun getDailyTotalsForRangeByCategory(
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate,
        category: String
    ): List<Double> {
        val results = mutableListOf<Double>()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            results.add(expenses.filter {
                Instant.ofEpochMilli(it.date)
                    .atZone(ZoneId.systemDefault()).toLocalDate() == curr
                        && !it.isIncome
                        && it.category == category
            }.sumOf { it.amount })
            curr = curr.plusDays(1)
        }
        return results
    }
}