package com.example.personalfinanceapp.presentation.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.recurring.components.AddRecurringDialog
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.presentation.home.EmptyStateCard
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.presentation.recurring.components.ModernRecurringItemCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allRecurringItems.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    // Animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            // Modernized FAB to match the Indigo/Violet theme
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // --- Modern Header ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Állandó tételek",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Automatikus havi tranzakciók",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        // Reuse the EmptyStateCard logic but themed for recurring
                        Box(Modifier.padding(top = 40.dp)) {
                            EmptyStateCard()
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            ModernRecurringItemCard(
                                item = item,
                                onDelete = { viewModel.deleteRecurringItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringDialog(
            onDismiss = { showAddDialog = false },
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
                showAddDialog = false
            }
        )
    }
}