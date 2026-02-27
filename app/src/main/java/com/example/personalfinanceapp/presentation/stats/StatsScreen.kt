package com.example.personalfinanceapp.presentation.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.components.ScreenHeader
import com.example.personalfinanceapp.presentation.stats.components.ForecastCard
import com.example.personalfinanceapp.utils.CategoryMapper
import com.example.personalfinanceapp.utils.formatAmount
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class TimePeriod(val displayName: String) {
    WEEK("Hét"),
    MONTH("Hónap"),
    YEAR("Év")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val total by viewModel.totalThisWeek.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val averageDaily by viewModel.averageDailySpending.collectAsState()
    val topCategory by viewModel.topCategory.collectAsState()
    val noSpendDays by viewModel.noSpendDaysThisWeek.collectAsState()

    val forecast by viewModel.spendingForecast.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()

    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    var showDetails by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header handles its own status bar inset + 16dp padding
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically()
        ) {
            ScreenHeader(
                title = "Elemzés",
                subtitle = "Részletes statisztikák és trendek"
            )
        }

        // All cards sit in a padded column with consistent 16dp spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 100))
            ) {
                TimePeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = {
                        selectedPeriod = it
                        viewModel.setChartPeriod(it)
                    }
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 200))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Összes költés",
                        value = total,
                        icon = Icons.Default.AccountBalance,
                        color = MaterialTheme.colorScheme.primary,
                        trend = null
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Napi átlag",
                        value = averageDaily,
                        icon = Icons.Default.CalendarToday,
                        color = MaterialTheme.colorScheme.tertiary,
                        trend = null
                    )
                }
            }

            AnimatedVisibility(
                visible = visible && forecast != null && transactionCount > 3,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 250)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 250))
            ) {
                forecast?.let {
                    ForecastCard(prediction = it)
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 300))
            ) {
                ChartCard(viewModel = viewModel, period = selectedPeriod)
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 400))
            ) {
                CategoryBreakdownCard(
                    categories = categoryBreakdown,
                    total = total,
                    expanded = showDetails,
                    onToggleExpand = { showDetails = !showDetails }
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 500)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 500))
            ) {
                val currentTopCategory = topCategory
                if (currentTopCategory != null && currentTopCategory.first != null) {
                    InsightCard(
                        title = "Legnagyobb kiadás",
                        message = "A legtöbbet ${currentTopCategory.first} kategóriában költöttél: ${formatAmount(currentTopCategory.second)} Ft",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            AnimatedVisibility(
                visible = visible && noSpendDays > 0,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 600))
            ) {
                InsightCard(
                    title = "Kiadásmentes Napok",
                    message = "Ezen a héten $noSpendDays napon egyáltalán nem volt kiadásod.",
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimePeriod.entries.forEach { period ->
                PeriodChip(
                    text = period.displayName,
                    selected = period == selectedPeriod,
                    onClick = { onPeriodSelected(period) }
                )
            }
        }
    }
}

@Composable
fun PeriodChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300)
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300)
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    icon: ImageVector,
    color: Color,
    trend: String?
) {
    var animationTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(value) {
        animationTarget = value.toFloat()
    }

    val animatedValue by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutExpo),
        label = "rolling_number"
    )

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = color.copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatAmount(animatedValue.toDouble())} Ft",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (trend != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trend,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun ChartCard(viewModel: StatsViewModel, period: TimePeriod) {
    val chartData by viewModel.chartData.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kiadások trendje", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Elmúlt ${period.displayName.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlowingTrendChart(
                data = chartData,
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
        }
    }
}

@Composable
fun GlowingTrendChart(
    data: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Még nincs adat",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val positiveColor = lineColor
    val negativeColor = Color(0xFFEF4444)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = h * 0.12f

        val dataMin = data.min()
        val dataMax = data.max()
        val absMax = maxOf(abs(dataMin), abs(dataMax)).coerceAtLeast(1.0)
        val rangeMin = -absMax
        val rangeMax = absMax
        val range = absMax * 2.0

        fun xOf(i: Int) = i * (w / (data.size - 1).coerceAtLeast(1))
        fun yOf(v: Double) = (h - pad) - ((v - rangeMin) / range * (h - pad * 2)).toFloat()

        val zeroY = yOf(0.0)

        val linePath = Path()
        if (data.size == 1) {
            linePath.moveTo(0f, yOf(data[0]))
            linePath.lineTo(w, yOf(data[0]))
        } else {
            linePath.moveTo(xOf(0), yOf(data[0]))
            for (i in 1 until data.size) {
                val x0 = xOf(i - 1); val y0 = yOf(data[i - 1])
                val x1 = xOf(i);     val y1 = yOf(data[i])
                val cpX = (x0 + x1) / 2f
                linePath.cubicTo(cpX, y0, cpX, y1, x1, y1)
            }
        }

        val positiveFill = Path().apply {
            addPath(linePath)
            lineTo(xOf(data.size - 1), zeroY)
            lineTo(xOf(0), zeroY)
            close()
        }
        drawPath(
            path = positiveFill,
            brush = Brush.verticalGradient(
                colors = listOf(positiveColor.copy(alpha = 0.30f), positiveColor.copy(alpha = 0f)),
                startY = 0f, endY = zeroY
            )
        )

        val negativeFill = Path().apply {
            addPath(linePath)
            lineTo(xOf(data.size - 1), zeroY)
            lineTo(xOf(0), zeroY)
            close()
        }
        drawPath(
            path = negativeFill,
            brush = Brush.verticalGradient(
                colors = listOf(negativeColor.copy(alpha = 0f), negativeColor.copy(alpha = 0.28f)),
                startY = zeroY, endY = h
            )
        )

        val dashWidth = 6.dp.toPx()
        val gapWidth  = 4.dp.toPx()
        var x = 0f
        while (x < w) {
            drawLine(
                color = lineColor.copy(alpha = 0.25f),
                start = androidx.compose.ui.geometry.Offset(x, zeroY),
                end   = androidx.compose.ui.geometry.Offset(minOf(x + dashWidth, w), zeroY),
                strokeWidth = 1.dp.toPx()
            )
            x += dashWidth + gapWidth
        }

        drawPath(
            path = linePath,
            color = positiveColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (data.size > 1) {
            for (i in 1 until data.size) {
                if (data[i - 1] < 0 || data[i] < 0) {
                    val segPath = Path()
                    val x0 = xOf(i - 1); val y0 = yOf(data[i - 1])
                    val x1 = xOf(i);     val y1 = yOf(data[i])
                    val cpX = (x0 + x1) / 2f
                    segPath.moveTo(x0, y0)
                    segPath.cubicTo(cpX, y0, cpX, y1, x1, y1)
                    drawPath(
                        path = segPath,
                        color = negativeColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        val lastX = xOf(data.size - 1)
        val lastY = yOf(data.last())
        val dotColor = if (data.last() >= 0) positiveColor else negativeColor
        drawCircle(color = dotColor.copy(alpha = 0.22f), radius = 10.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY))
        drawCircle(color = dotColor, radius = 4.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY))
    }
}

@Composable
fun CategoryBreakdownCard(
    categories: Map<String, Double>,
    total: Double,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kategória bontás",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    categories.entries.sortedByDescending { it.value }.take(5).forEach { (category, amount) ->
                        CategoryItem(
                            name = category,
                            amount = amount,
                            percentage = if (total > 0) (amount / total * 100) else 0.0,
                            color = CategoryMapper.getColor(category)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    name: String,
    amount: Double,
    percentage: Double,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${formatAmount(amount)} Ft",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { (percentage / 100).toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    message: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = color.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.05f))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}