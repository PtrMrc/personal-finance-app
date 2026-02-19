package com.example.personalfinanceapp.presentation.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Budget
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.data.repository.BudgetRepository
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
        context = application,
        tfliteClassifier = tfliteClassifier,
        naiveBayesRepository = naiveBayesRepository,
        modelPerformanceDao = database.modelPerformanceDao()
    )

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
                ensembleModel.recordUserChoice(title, ensemblePrediction, userChoice)
                Log.d("HomeViewModel", "Recorded user choice and updated weights")
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