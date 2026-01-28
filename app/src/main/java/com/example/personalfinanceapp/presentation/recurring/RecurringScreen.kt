package com.example.personalfinanceapp.presentation.recurring

import com.example.personalfinanceapp.presentation.home.components.RecurringItemCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.recurring.components.AddRecurringDialog
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.presentation.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allRecurringItems.collectAsState(initial = emptyList())
    var _showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Állandó tételek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { _showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                RecurringItemCard(item, onDelete = { viewModel.deleteRecurringItem(item) })
            }
        }
    }

    if (_showAddDialog) {
        AddRecurringDialog(
            onDismiss = { _showAddDialog = false },
            onConfirm = { title, amount, isIncome, freq, day ->
                val newItem = RecurringItem(
                    title = title,
                    amount = amount,
                    category = if (isIncome) "Bevétel" else "Egyéb",
                    isIncome = isIncome,
                    frequency = freq,
                    dayOfMonth = day
                )
                viewModel.addRecurringItem(newItem)
                _showAddDialog = false
            }
        )
    }
}