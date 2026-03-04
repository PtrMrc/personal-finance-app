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
import java.util.Calendar

data class BudgetProgress(
    val category: String,
    val limit: Double,
    val spent: Double
) {
    val percentage = if (limit > 0) spent / limit else 0.0
    val isWarning = percentage in 0.8..<1.0 // 80% to 99%
    val isExceeded = percentage >= 1.0 // 100%+
}

/**
 * Represents a spending anomaly for a single category.
 *
 * @param category          Hungarian category name as stored in the DB.
 * @param currentWeekSpend  Total spend in the current 7-day bucket.
 * @param historicalAverage Mean weekly spend over the previous 8 complete weeks.
 * @param percentageAbove   How much above average as a fraction (e.g. 0.75 = 75 % above).
 */
data class AnomalyAlert(
    val category: String,
    val currentWeekSpend: Double,
    val historicalAverage: Double,
    val percentageAbove: Double
)

// Number of 7-day buckets (complete weeks) used to build the baseline
private const val HISTORY_WEEKS = 8

// Minimum fraction above the average that triggers an alert (0.5 = 50 %)
private const val ANOMALY_THRESHOLD = 0.5

// Minimum number of individual expenses in the current week before we fire an alert
private const val MIN_EXPENSES_THIS_WEEK = 2

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

    val correctionStats: Flow<List<CategoryCorrectionStat>> = correctionRepository.allStats

    val allExpenses: Flow<List<Expense>> = expenseRepository.allExpenses
    val totalSpending: Flow<Double?> = expenseRepository.totalSpending
    val categoryBreakdown = expenseRepository.categoryBreakdown
    val allRecurringItems: Flow<List<RecurringItem>> = recurringRepository.allRecurringItems

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val ensembleStats: Flow<EnsembleStats> = flow {
        while (true) {
            try {
                val stats = ensembleModel.getEnsembleStats()
                emit(stats)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting ensemble stats", e)
            }
            delay(5000)  // Update every 5 seconds
        }
    }

    // --- BUDGET LOGIC ---
    val budgetProgress: StateFlow<List<BudgetProgress>> = combine(
        expenseRepository.allExpenses,
        budgetRepository.allBudgets
    ) { expenses, budgets ->
        // Get current month and year
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        // Filter expenses for THIS MONTH only
        val monthlyExpenses = expenses.filter {
            val expCal = Calendar.getInstance().apply { timeInMillis = it.date }
            expCal.get(Calendar.MONTH) == currentMonth &&
                    expCal.get(Calendar.YEAR) == currentYear &&
                    !it.isIncome
        }

        // Sum by category
        val spentByCategory = monthlyExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Map to Progress objects
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
        }.sortedByDescending { it.percentage } // Put most urgent at the top

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SPENDING ANOMALY DETECTION ---

    /**
     * Emits a list of [AnomalyAlert]s whenever the weekly spend data changes.
     *
     * Algorithm:
     *  1. Determine the current week bucket  = now / 604_800_000
     *  2. For each category, split rows into "current week" vs. "history" (buckets
     *     [currentWeek-HISTORY_WEEKS .. currentWeek-1]).
     *  3. Compute the historical average over however many of those weeks have data
     *     (weeks with zero spend have no row, so we normalise over HISTORY_WEEKS to
     *     keep the average honest — a week with no spend still counts as 0).
     *  4. Only raise an alert when:
     *     - historicalAverage > 0  (no baseline → no alert)
     *     - currentWeekSpend > historicalAverage * (1 + ANOMALY_THRESHOLD)
     *     - current-week expenseCount >= MIN_EXPENSES_THIS_WEEK
     */
    val anomalyAlerts: StateFlow<List<AnomalyAlert>> =
        expenseRepository.weeklySpendByCategory
            .map { rows ->
                val currentWeek = System.currentTimeMillis() / 604_800_000L
                val historyStart = currentWeek - HISTORY_WEEKS  // inclusive lower bound

                // Group all DB rows by category
                val byCategory = rows.groupBy { it.category }

                val alerts = mutableListOf<AnomalyAlert>()

                for ((category, weekRows) in byCategory) {
                    // Row for the current week (may be absent if no spend yet)
                    val currentRow = weekRows.firstOrNull { it.weekNumber == currentWeek }

                    // Only evaluate if there are at least MIN_EXPENSES_THIS_WEEK this week
                    if ((currentRow?.expenseCount ?: 0) < MIN_EXPENSES_THIS_WEEK) continue

                    val currentSpend = currentRow?.total ?: 0.0

                    // Rows that fall within the historical window (exclude current week)
                    val historicRows = weekRows.filter { it.weekNumber in historyStart until currentWeek }

                    // Sum up all historical spend; divide by HISTORY_WEEKS so weeks with
                    // zero spend (= no row) still drag the average down correctly.
                    val historicalTotal = historicRows.sumOf { it.total }
                    val historicalAverage = historicalTotal / HISTORY_WEEKS

                    // No usable baseline — skip
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

                // Most severe anomaly first
                alerts.sortedByDescending { it.percentageAbove }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            budgetRepository.setBudget(Budget(category, limit))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            // Need the full object to delete
            budgetRepository.deleteBudget(Budget(category, 0.0))
        }
    }

    /**
     * Predict category using adaptive ensemble
     *
     * @param title Expense title
     * @return Ensemble prediction with both model predictions
     */
    suspend fun predictCategoryEnsemble(title: String): EnsemblePrediction {
        return try {
            ensembleModel.predict(title)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to predict category", e)
            // Return fallback prediction
            EnsemblePrediction(
                finalCategory = "Other",
                confidence = 0.3,
                tflitePrediction = null,
                naiveBayesPrediction = null,
                weights = com.example.personalfinanceapp.ml.ModelWeights(0.6, 0.4),
                explanation = "Error occurred"
            )
        }
    }

    /**
     * Record user's category choice and learn from it
     *
     * @param title Expense title
     * @param ensemblePrediction What the ensemble predicted
     * @param userChoice What the user actually chose
     */
    fun recordCategoryChoice(
        title: String,
        ensemblePrediction: EnsemblePrediction,
        userChoice: String
    ) {
        viewModelScope.launch {
            try {
                // Train Naive Bayes and adjust ensemble weights
                ensembleModel.recordUserChoice(title, ensemblePrediction, userChoice)

                // Record whether the AI suggestion was accepted or corrected
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

    // Expense operations with error handling
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
                .onFailure { e ->
                    _errorMessage.value = "Hiba a kiadás frissítésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to update expense", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.addRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel hozzáadásakor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to add recurring item", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringItem(item)
                .onFailure { e ->
                    _errorMessage.value = "Hiba az ismétlődő tétel törlésekor: ${e.message}"
                    Log.e("HomeViewModel", "Failed to delete recurring item", e)
                }
                .onSuccess {
                    _errorMessage.value = null
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}