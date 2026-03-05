package com.example.personalfinanceapp.presentation.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.presentation.components.ScreenHeader
import com.example.personalfinanceapp.presentation.home.EmptyStateCard
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.presentation.recurring.components.AddRecurringDialog
import com.example.personalfinanceapp.presentation.recurring.components.ModernRecurringItemCard
import com.example.personalfinanceapp.utils.formatAmount
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allRecurringItems.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringItem?>(null) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val incomeItems = items.filter { it.isIncome }
    val expenseItems = items.filter { !it.isIncome }

    // Monthly-equivalent totals
    val monthlyIncome = incomeItems.sumOf { it.monthlyEquivalent() }
    val monthlyExpenses = expenseItems.sumOf { it.monthlyEquivalent() }
    val monthlyNet = monthlyIncome - monthlyExpenses

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Hozzáadás")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = visible, enter = fadeIn()) {
                ScreenHeader(
                    title = "Állandó tételek",
                    subtitle = "Automatikus havi tranzakciók"
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Summary card ──────────────────────────────────────────────
                if (items.isNotEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -20 }
                        ) {
                            RecurringSummaryCard(
                                monthlyIncome = monthlyIncome,
                                monthlyExpenses = monthlyExpenses,
                                monthlyNet = monthlyNet
                            )
                        }
                    }
                }

                // ── Income section ────────────────────────────────────────────
                if (incomeItems.isNotEmpty()) {
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(tween(500, delayMillis = 100))) {
                            SectionHeader(
                                title = "Bevételek",
                                color = Color(0xFF10B981),
                                icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    items(incomeItems, key = { "income_${it.id}" }) { item ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, delayMillis = 150)) +
                                    slideInVertically(tween(600, delayMillis = 150))
                        ) {
                            ModernRecurringItemCard(
                                item = item,
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.deleteRecurringItem(item) }
                            )
                        }
                    }
                }

                // ── Expense section ───────────────────────────────────────────
                if (expenseItems.isNotEmpty()) {
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(tween(500, delayMillis = 200))) {
                            SectionHeader(
                                title = "Kiadások",
                                color = Color(0xFFEF4444),
                                icon = { Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    items(expenseItems, key = { "expense_${it.id}" }) { item ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, delayMillis = 200)) +
                                    slideInVertically(tween(600, delayMillis = 200))
                        ) {
                            ModernRecurringItemCard(
                                item = item,
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.deleteRecurringItem(item) }
                            )
                        }
                    }
                }

                // ── Empty state ───────────────────────────────────────────────
                if (items.isEmpty()) {
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = 150))) {
                            Box(Modifier.padding(top = 40.dp)) {
                                EmptyStateCard()
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddRecurringDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, isIncome, freq, day, category ->
                viewModel.addRecurringItem(
                    RecurringItem(
                        title = title,
                        amount = amount,
                        category = category,
                        isIncome = isIncome,
                        frequency = freq,
                        dayOfMonth = day
                    )
                )
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingItem?.let { item ->
        AddRecurringDialog(
            existingItem = item,
            onDismiss = { editingItem = null },
            onConfirm = { title, amount, isIncome, freq, day, category ->
                viewModel.updateRecurringItem(
                    item.copy(
                        title = title,
                        amount = amount,
                        isIncome = isIncome,
                        frequency = freq,
                        dayOfMonth = day,
                        category = category
                    )
                )
                editingItem = null
            }
        )
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun RecurringSummaryCard(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    monthlyNet: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Havi összesítő",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryColumn(
                    label = "Bevétel",
                    value = "+${formatAmount(monthlyIncome)} Ft",
                    color = Color(0xFF10B981)
                )
                SummaryColumn(
                    label = "Kiadás",
                    value = "-${formatAmount(monthlyExpenses)} Ft",
                    color = Color(0xFFEF4444)
                )
                SummaryColumn(
                    label = "Egyenleg",
                    value = "${if (monthlyNet >= 0) "+" else ""}${formatAmount(monthlyNet)} Ft",
                    color = if (monthlyNet >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
            if (monthlyExpenses > 0 && monthlyIncome > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "* Negyedéves/éves tételek havi egyenértéken számítva",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    color: Color,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Extension: monthly-equivalent amount ─────────────────────────────────────

private fun RecurringItem.monthlyEquivalent(): Double = when (frequency) {
    Frequency.MONTHLY -> amount
    Frequency.QUARTERLY -> amount / 3.0
    Frequency.YEARLY -> amount / 12.0
}