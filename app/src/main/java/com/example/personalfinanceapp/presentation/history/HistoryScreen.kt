package com.example.personalfinanceapp.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.R
import com.example.personalfinanceapp.presentation.home.components.ExpenseCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // 1. FILTER CHIPS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // We use a helper map or simple if/else to pick the right string resource for the Enum
                FilterChipItem(
                    label = stringResource(R.string.filter_week),
                    selected = currentFilter == DateFilter.WEEK,
                    onClick = { viewModel.setFilter(DateFilter.WEEK) }
                )
                FilterChipItem(
                    label = stringResource(R.string.filter_month),
                    selected = currentFilter == DateFilter.MONTH,
                    onClick = { viewModel.setFilter(DateFilter.MONTH) }
                )
                FilterChipItem(
                    label = stringResource(R.string.filter_year),
                    selected = currentFilter == DateFilter.YEAR,
                    onClick = { viewModel.setFilter(DateFilter.YEAR) }
                )
                FilterChipItem(
                    label = stringResource(R.string.filter_all),
                    selected = currentFilter == DateFilter.ALL,
                    onClick = { viewModel.setFilter(DateFilter.ALL) }
                )
            }

            // 2. THE CONTENT
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_data_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseCard(expense)
                    }
                }
            }
        }
    }
}

// Small helper composable to keep code clean
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}