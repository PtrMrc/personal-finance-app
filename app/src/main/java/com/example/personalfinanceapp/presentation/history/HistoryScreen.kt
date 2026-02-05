package com.example.personalfinanceapp.presentation.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.data.Expense
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()

    // Animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Calculate statistics
    val totalAmount = remember(expenses) {
        expenses.sumOf { it.amount }
    }
    val averageAmount = remember(expenses) {
        if (expenses.isNotEmpty()) expenses.sumOf { it.amount } / expenses.size else 0.0
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            ModernHistoryTopBar(onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FILTER SECTION
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600)) +
                                slideInVertically(animationSpec = tween(600))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Időszak szűrése",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )

                            ModernFilterChips(
                                currentFilter = currentFilter,
                                onFilterSelected = { viewModel.setFilter(it) }
                            )
                        }
                    }
                }

                // STATISTICS CARDS
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 100))
                    ) {
                        HistoryStatsRow(
                            totalAmount = totalAmount,
                            transactionCount = expenses.size,
                            averageAmount = averageAmount
                        )
                    }
                }

                // TRANSACTIONS HEADER
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tranzakciók",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${expenses.size} találat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (expenses.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = getFilterDisplayName(currentFilter),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TRANSACTION LIST
                if (expenses.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 300))
                        ) {
                            HistoryEmptyState(currentFilter = currentFilter)
                        }
                    }
                } else {
                    items(expenses, key = { it.id }) { expense ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) +
                                    slideInVertically(animationSpec = tween(600, delayMillis = 300))
                        ) {
                            HistoryExpenseCard(expense)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHistoryTopBar(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFF1F5F9),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Vissza",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Előzmények",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Összes tranzakció",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFF1F5F9),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Keresés",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernFilterChips(
    currentFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipModern(
                text = "Hét",
                icon = Icons.Default.CalendarViewWeek,
                selected = currentFilter == DateFilter.WEEK,
                onClick = { onFilterSelected(DateFilter.WEEK) },
                modifier = Modifier.weight(1f)
            )
            FilterChipModern(
                text = "Hónap",
                icon = Icons.Default.CalendarMonth,
                selected = currentFilter == DateFilter.MONTH,
                onClick = { onFilterSelected(DateFilter.MONTH) },
                modifier = Modifier.weight(1f)
            )
            FilterChipModern(
                text = "Év",
                icon = Icons.Default.CalendarToday,
                selected = currentFilter == DateFilter.YEAR,
                onClick = { onFilterSelected(DateFilter.YEAR) },
                modifier = Modifier.weight(1f)
            )
            FilterChipModern(
                text = "Mind",
                icon = Icons.Default.AllInclusive,
                selected = currentFilter == DateFilter.ALL,
                onClick = { onFilterSelected(DateFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FilterChipModern(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF6366F1) else Color(0xFFF8F9FA),
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF64748B),
        animationSpec = tween(300)
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HistoryStatsRow(
    totalAmount: Double,
    transactionCount: Int,
    averageAmount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HistoryStatCard(
            title = "Összesen",
            value = "${totalAmount.toInt()} Ft",
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF6366F1),
            modifier = Modifier.weight(1f)
        )

        HistoryStatCard(
            title = "Darabszám",
            value = "$transactionCount db",
            icon = Icons.Default.Receipt,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )

        HistoryStatCard(
            title = "Átlag",
            value = "${averageAmount.toInt()} Ft",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HistoryStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )

            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun HistoryExpenseCard(expense: Expense) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF6366F1).copy(alpha = 0.08f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = getCategoryColor(expense.category).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = null,
                        tint = getCategoryColor(expense.category),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(0xFF64748B), CircleShape)
                        )
                        Text(
                            text = formatDate(expense.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Text(
                text = "${if (expense.isIncome) "+" else "-"}${expense.amount.toInt()} Ft",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (expense.isIncome) Color(0xFF10B981) else Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun HistoryEmptyState(currentFilter: DateFilter) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Color(0xFF6366F1).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Nincs találat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = "Nem található tranzakció ebben az időszakban: ${getFilterDisplayName(currentFilter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}

// Helper functions
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "étel", "food" -> Color(0xFFEF4444)
        "közlekedés", "transport" -> Color(0xFF3B82F6)
        "szórakozás", "entertainment" -> Color(0xFF8B5CF6)
        "lakhatás", "housing" -> Color(0xFFF59E0B)
        "egészség", "health" -> Color(0xFF10B981)
        "oktatás", "education" -> Color(0xFF6366F1)
        "ruházat", "clothing" -> Color(0xFFEC4899)
        "bevétel", "income" -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }
}

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "étel", "food" -> Icons.Default.Restaurant
        "közlekedés", "transport" -> Icons.Default.DirectionsCar
        "szórakozás", "entertainment" -> Icons.Default.MovieCreation
        "lakhatás", "housing" -> Icons.Default.Home
        "egészség", "health" -> Icons.Default.Favorite
        "oktatás", "education" -> Icons.Default.School
        "ruházat", "clothing" -> Icons.Default.Checkroom
        "bevétel", "income" -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.Category
    }
}

fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        days == 0L -> "Ma"
        days == 1L -> "Tegnap"
        days < 7 -> "$days napja"
        else -> {
            val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale("hu")).format(java.util.Date(timestamp))
            date
        }
    }
}

fun getFilterDisplayName(filter: DateFilter): String {
    return when (filter) {
        DateFilter.WEEK -> "Ez a hét"
        DateFilter.MONTH -> "Ez a hónap"
        DateFilter.YEAR -> "Ez az év"
        DateFilter.ALL -> "Összes időszak"
    }
}