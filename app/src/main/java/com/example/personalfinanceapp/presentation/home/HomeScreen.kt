package com.example.personalfinanceapp.presentation.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.presentation.home.components.ExpenseDialog
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.utils.extractAmount
import com.example.personalfinanceapp.utils.mapToHungarian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSeeAllClick: () -> Unit
) {
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    val expenseList by viewModel.allExpenses.collectAsState(initial = emptyList())
    val total by viewModel.totalSpending.collectAsState(initial = 0.0)

    // Calculate category breakdown from expenses
    val categoryBreakdown = remember(expenseList) {
        expenseList.groupBy { it.category }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }

    val recentExpenses = expenseList.take(10)

    var textInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var activeExpense by remember { mutableStateOf<Expense?>(null) }

    fun openNewDraft() {
        if (textInput.isBlank()) return
        val amountVal = extractAmount(textInput)
        val titleVal = textInput.replace(amountVal.toInt().toString(), "")
            .replace(amountVal.toString(), "").trim().ifBlank { "Ismeretlen" }

        scope.launch {
            activeExpense = Expense(
                id = 0,
                title = titleVal,
                amount = if (amountVal > 0) amountVal else 0.0,
                category = "",
                description = "",
                date = System.currentTimeMillis(),
                isIncome = (false)
            )
            showDialog = true
        }
    }

    fun openEditDraft(expense: Expense) {
        activeExpense = expense
        showDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MODERN HEADER
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically()
            ) {
                ModernHeader()
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BALANCE CARD
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 100))
                    ) {
                        EnhancedBalanceCard(
                            total = total ?: 0.0,
                            categoryBreakdown = categoryBreakdown
                        )
                    }
                }

                // QUICK STATS ROW
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        QuickStatsRow(
                            transactionCount = recentExpenses.size,
                            averageSpending = if (recentExpenses.isNotEmpty())
                                (total ?: 0.0) / recentExpenses.size else 0.0
                        )
                    }
                }

                // RECENT TRANSACTIONS HEADER
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Legutóbbi tranzakciók",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${recentExpenses.size} tranzakció",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (recentExpenses.isNotEmpty()) {
                                TextButton(onClick = onSeeAllClick) {
                                    Text(
                                        text = "Mind",
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // TRANSACTION LIST
                items(recentExpenses, key = { it.id }) { expense ->
                    val dismissScope = rememberCoroutineScope()
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState()

                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        LaunchedEffect(Unit) { showDeleteDialog = true }
                    }

                    if (showDeleteDialog) {
                        ModernDeleteDialog(
                            expenseTitle = expense.title,
                            onConfirm = {
                                viewModel.deleteExpense(expense)
                                showDeleteDialog = false
                            },
                            onDismiss = {
                                showDeleteDialog = false
                                dismissScope.launch { dismissState.reset() }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 400))
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    Color(0xFFEF4444) else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(20.dp))
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            "Törlés",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        ) {
                            Box(Modifier.clickable { openEditDraft(expense) }) {
                                EnhancedExpenseCard(expense)
                            }
                        }
                    }
                }

                // EMPTY STATE
                if (recentExpenses.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
                        ) {
                            EmptyStateCard()
                        }
                    }
                }
            }

            // MODERN INPUT AREA
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 500)) +
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(600, delayMillis = 500)
                        )
            ) {
                ModernInputArea(
                    textInput = textInput,
                    onTextChange = { textInput = it },
                    onSend = { openNewDraft() }
                )
            }
        }
    }

    if (showDialog && activeExpense != null) {
        ExpenseDialog(
            initialTitle = activeExpense!!.title,
            initialAmount = if (activeExpense!!.amount > 0) activeExpense!!.amount.toInt().toString() else "",
            initialCategory = activeExpense!!.category,
            initialDescription = activeExpense!!.description ?: "",
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onConfirm = { title, amount, category, desc ->
                val isIncome = (category == "Bevétel")
                val finalExpense = activeExpense!!.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    description = desc,
                    isIncome = isIncome
                )
                if (finalExpense.id == 0) viewModel.addExpense(finalExpense)
                else viewModel.updateExpense(finalExpense)
                showDialog = false
                textInput = ""
            }
        )
    }
}

@Composable
fun ModernHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Áttekintés",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Pénzügyi aktivitásod",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
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
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedBalanceCard(
    total: Double,
    categoryBreakdown: Map<String, Double>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF6366F1).copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Összes kiadás",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${total.toInt()} Ft",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick category indicators
                if (categoryBreakdown.isNotEmpty()) {
                    val topCategories = categoryBreakdown.entries
                        .sortedByDescending { it.value }
                        .take(3)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topCategories.forEach { (category, amount) ->
                            CategoryPill(
                                category = category,
                                amount = amount,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPill(
    category: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = "${amount.toInt()} Ft",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickStatsRow(
    transactionCount: Int,
    averageSpending: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Tranzakciók",
            value = "$transactionCount db",
            icon = Icons.Default.Receipt,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )

        QuickStatCard(
            title = "Átlag",
            value = "${averageSpending.toInt()} Ft",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickStatCard(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

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
        }
    }
}

@Composable
fun EnhancedExpenseCard(expense: Expense) {
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
fun EmptyStateCard() {
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
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Még nincs tranzakció",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = "Kezdd el az első kiadásod rögzítését!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun ModernInputArea(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChange,
                label = {
                    Text(
                        "Mit vettél?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                placeholder = {
                    Text(
                        "pl. Tesco 3500",
                        color = Color(0xFF94A3B8)
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Button(
                onClick = onSend,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Rögzítés",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ModernDeleteDialog(
    expenseTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Törlés megerősítése",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        },
        text = {
            Text(
                text = "Biztosan törlöd: \"$expenseTitle\"?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Törlés", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Mégse",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
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