package com.example.personalfinanceapp.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.personalfinanceapp.data.AppTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.presentation.components.ScreenHeader
import com.example.personalfinanceapp.presentation.home.components.ExpenseDialog
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.presentation.home.components.BudgetProgressSection
import com.example.personalfinanceapp.presentation.home.components.ForecastSummaryCard
import com.example.personalfinanceapp.utils.extractAmount
import com.example.personalfinanceapp.utils.CategoryMapper
import com.example.personalfinanceapp.utils.formatAmount
import com.example.personalfinanceapp.utils.formatDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Per-theme balance card base gradients
private val CardBaseSimple1 = Color(0xFF0D2A3D)
private val CardBaseSimple2 = Color(0xFF0A1E2E)
private val CardBaseOled1   = Color(0xFF000000)
private val CardBaseOled2   = Color(0xFF050D15)
private val CardBaseLight1  = Color(0xFF1E40AF)
private val CardBaseLight2  = Color(0xFF1E3A8A)

// Blob accent colors
private val BlobBlue   = Color(0xFF3B82F6)
private val BlobGreen  = Color(0xFF10B981)
private val BlobIndigo = Color(0xFF1D4ED8)

// Amber palette for anomaly cards — warm but not alarming
private val AnomalyAmber     = Color(0xFFF59E0B)
private val AnomalyAmberBg   = Color(0xFFFFFBEB)
private val AnomalyAmberDark = Color(0xFF92400E)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSeeAllClick: () -> Unit,
    onBudgetSetupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit = {},
    appTheme: AppTheme = AppTheme.SIMPLE
) {
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val categoryBreakdown = remember(expenseList) {
        expenseList.groupBy { it.category }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }

    val recentExpenses = expenseList.take(10)

    var textInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var activeExpense by remember { mutableStateOf<Expense?>(null) }

    val budgets by viewModel.budgetProgress.collectAsState()
    val anomalies by viewModel.anomalyAlerts.collectAsState()

    // Forecast data
    val spendingForecast by viewModel.spendingForecast.collectAsState()
    val budgetLimits by viewModel.budgetLimits.collectAsState()

    val dismissedCategories = remember { mutableStateSetOf<String>() }
    val visibleAnomalies = anomalies.filter { it.category !in dismissedCategories }

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
                isIncome = false
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically()
            ) {
                ModernHeader(
                    onSettingsClick = onSettingsClick,
                    onBudgetClick = onBudgetSetupClick
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Balance card ──────────────────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 100))
                    ) {
                        EnhancedBalanceCard(
                            total = total ?: 0.0,
                            categoryBreakdown = categoryBreakdown,
                            appTheme = appTheme
                        )
                    }
                }

                // ── Compact forecast card ─────────────────────────────────────
                // Shown once there are at least a few days of data (confidence > 0)
                val monthlyExpenseCount = expenseList.count { !it.isIncome }
                if (spendingForecast != null && monthlyExpenseCount > 3) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 160)) +
                                    slideInVertically(animationSpec = tween(600, delayMillis = 160))
                        ) {
                            ForecastSummaryCard(
                                prediction = spendingForecast!!,
                                budgetLimits = budgetLimits,
                                onViewDetails = onStatsClick
                            )
                        }
                    }
                }

                // ── Budget progress ───────────────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 250)) +
                                slideInVertically(animationSpec = tween(600, delayMillis = 250))
                    ) {
                        BudgetProgressSection(budgets)
                    }
                }

                // ── Anomaly alert cards ───────────────────────────────────────
                if (visibleAnomalies.isNotEmpty()) {
                    items(visibleAnomalies, key = { "anomaly_${it.category}" }) { alert ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) +
                                    expandVertically(animationSpec = tween(400)),
                            exit  = fadeOut(animationSpec = tween(300)) +
                                    shrinkVertically(animationSpec = tween(300))
                        ) {
                            AnomalyAlertCard(
                                alert = alert,
                                onDismiss = { dismissedCategories.add(alert.category) }
                            )
                        }
                    }
                }

                // ── Quick stats ───────────────────────────────────────────────
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

                // ── Transactions header ───────────────────────────────────────
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${recentExpenses.size} tranzakció",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (recentExpenses.isNotEmpty()) {
                                TextButton(onClick = onSeeAllClick) {
                                    Text(
                                        text = "Mind",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Transaction rows ──────────────────────────────────────────
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
                                    MaterialTheme.colorScheme.error else Color.Transparent
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
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            "Törlés",
                                            color = MaterialTheme.colorScheme.onError,
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
            isEditing = activeExpense!!.id != 0,
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

// ---------------------------------------------------------------------------
// Anomaly alert card
// ---------------------------------------------------------------------------

@Composable
fun AnomalyAlertCard(
    alert: AnomalyAlert,
    onDismiss: () -> Unit
) {
    val percentInt = (alert.percentageAbove * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnomalyAmberBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, AnomalyAmber.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Warning icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AnomalyAmber.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AnomalyAmber,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Szokatlanul magas kiadás",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnomalyAmberDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${alert.category}: +${percentInt}% az átlaghoz képest " +
                            "(${formatAmount(alert.currentWeekSpend)} Ft vs. " +
                            "${formatAmount(alert.historicalAverage)} Ft/hét)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnomalyAmberDark.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }

            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Bezárás",
                    tint = AnomalyAmberDark.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ModernHeader(
    onSettingsClick: () -> Unit,
    onBudgetClick: () -> Unit
) {
    ScreenHeader(
        title = "Áttekintés",
        subtitle = "Pénzügyi aktivitásod"
    ) {
        IconButton(onClick = onBudgetClick) {
            Icon(
                Icons.Default.Wallet,
                contentDescription = "Költségvetés",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Beállítások",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun EnhancedBalanceCard(
    total: Double,
    categoryBreakdown: Map<String, Double>,
    appTheme: AppTheme = AppTheme.SIMPLE
) {
    var animationTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(total) {
        animationTarget = total.toFloat()
    }

    val animatedTotal by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutExpo),
        label = "rolling_balance"
    )

    val (base1, base2) = when (appTheme) {
        AppTheme.OLED  -> CardBaseOled1  to CardBaseOled2
        AppTheme.LIGHT -> CardBaseLight1 to CardBaseLight2
        else           -> CardBaseSimple1 to CardBaseSimple2
    }
    val blobAlpha = if (appTheme == AppTheme.OLED) 0.22f else 0.55f
    val shadowColor = if (appTheme == AppTheme.LIGHT) CardBaseLight1 else BlobBlue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (appTheme == AppTheme.OLED) 6.dp else 14.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = shadowColor.copy(alpha = if (appTheme == AppTheme.OLED) 0.12f else 0.28f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(base1, base2),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                fun drawBlob(cx: Float, cy: Float, r: Float, color: Color, alpha: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = alpha),
                                color.copy(alpha = alpha * 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
                // Large primary blob — top-right
                drawBlob(size.width * 0.82f, size.height * 0.10f, size.width * 0.55f, BlobBlue,   blobAlpha)
                // Medium secondary blob — bottom-left
                drawBlob(size.width * 0.08f, size.height * 0.92f, size.width * 0.40f, BlobGreen,  blobAlpha * 0.85f)
                // Small accent blob — centre-right
                drawBlob(size.width * 0.62f, size.height * 0.68f, size.width * 0.28f, BlobIndigo, blobAlpha * 0.70f)
                // Corner vignette to anchor text area
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.32f
                    ),
                    radius = size.width * 0.32f,
                    center = Offset(0f, 0f)
                )
            }

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
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Egyenleg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${formatAmount(animatedTotal.toDouble())} Ft",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (total < 0) Color(0xFFFF6B6B) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatAmount(amount)} Ft",
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
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )

        QuickStatCard(
            title = "Átlag",
            value = "${formatAmount(averageSpending)} Ft",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            color = MaterialTheme.colorScheme.tertiary,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = CategoryMapper.getColor(expense.category).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryMapper.getIcon(expense.category),
                        contentDescription = null,
                        tint = CategoryMapper.getColor(expense.category),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                        )
                        Text(
                            text = formatDate(expense.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "${if (expense.isIncome) "+" else "-"}${formatAmount(expense.amount)} Ft",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (expense.isIncome) MaterialTheme.colorScheme.secondary else Color(0xFFEF4444)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Még nincs tranzakció",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Kezdd el az első kiadásod rögzítését!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            value = textInput,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    "Mit vettél? (pl. Tesco 3500)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Button(
            onClick = onSend,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Rögzítés",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
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
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Törlés megerősítése",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "Biztosan törlöd: \"$expenseTitle\"?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Törlés", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Mégse", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    )
}