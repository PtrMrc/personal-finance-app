package com.example.personalfinanceapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class DateFilter { WEEK, MONTH, YEAR, ALL }

class HistoryViewModel(repository: ExpenseRepository) : ViewModel() {

    // 1. Current Filter State (Default: Month)
    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter

    // 2. Stream of ALL expenses
    private val _allExpenses = repository.allExpenses

    // 3. Computed stream: Data + Filter = Result
    val filteredExpenses: StateFlow<List<Expense>> = combine(_allExpenses, _selectedFilter) { expenses, filter ->
        val cal = Calendar.getInstance()

        // Calculate the "Cutoff Date" (Everything after this date is shown)
        val cutoffDate = when (filter) {
            DateFilter.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            DateFilter.MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            DateFilter.YEAR -> {
                cal.add(Calendar.YEAR, -1)
                cal.timeInMillis
            }
            DateFilter.ALL -> 0L // 1970 (Everything)
        }

        // Return only items newer than the cutoff
        expenses.filter { it.date >= cutoffDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }
}