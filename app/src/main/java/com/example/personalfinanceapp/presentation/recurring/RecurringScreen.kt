package com.example.personalfinanceapp.presentation.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.components.ScreenHeader
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),  // ScreenHeader handles status bar inset itself
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Flat header — matches Home and Stats screen style
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn()
            ) {
                ScreenHeader(
                    title = "Állandó tételek",
                    subtitle = "Automatikus havi tranzakciók"
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, delayMillis = 150))
                        ) {
                            Box(Modifier.padding(top = 40.dp)) {
                                EmptyStateCard()
                            }
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, delayMillis = 200)) +
                                    slideInVertically(tween(600, delayMillis = 200))
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