package com.example.personalfinanceapp.presentation.stats.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ml.PredictionResult
import com.example.personalfinanceapp.utils.CategoryMapper
import com.example.personalfinanceapp.utils.formatAmount

// ── Confidence colours ────────────────────────────────────────────────────────

private val ConfidenceLow    = Color(0xFFF59E0B)   // amber
private val ConfidenceMid    = Color(0xFF3B82F6)   // blue
private val ConfidenceHigh   = Color(0xFF10B981)   // green
private val BudgetExceeded   = Color(0xFFEF4444)   // red

/**
 * Returns a human-readable confidence label and badge colour for the given [confidence] value.
 * Labels are in Hungarian.
 */
private fun confidenceInfo(confidence: Double, daysOfData: Int, usedHistory: Boolean): Pair<String, Color> {
    val historySuffix = if (usedHistory) " · historikus adat" else ""
    return when {
        confidence < 0.25 -> "Alacsony biztonság – $daysOfData nap$historySuffix" to ConfidenceLow
        confidence < 0.60 -> "Közepes biztonság – $daysOfData nap$historySuffix" to ConfidenceMid
        else              -> "Magas biztonság – $daysOfData nap$historySuffix" to ConfidenceHigh
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full Forecast Card  (StatsScreen)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rich forecast card for StatsScreen.
 *
 * Displays:
 * - Month-end total forecast with a confidence badge
 * - Meta row: daily rate · days of data · outliers excluded
 * - Per-category breakdown (top 4 categories), flagged red when the projection
 *   exceeds the category's budget limit
 *
 * @param prediction   Result from [MLMath.calculateSmartForecast].
 * @param budgetLimits Map of category → monthly limit (HUF).  Pass empty map if
 *                     no budgets are configured.
 */
@Composable
fun ForecastCard(
    prediction: PredictionResult,
    budgetLimits: Map<String, Double> = emptyMap()
) {
    val (confidenceLabel, confidenceColor) = confidenceInfo(prediction.confidence, prediction.daysOfData, prediction.usedHistoricalData)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Decorative background icon
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Hónap végi előrejelzés",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Total forecast amount ─────────────────────────────────────
                Text(
                    text = "${formatAmount(prediction.forecastedTotal)} Ft",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Confidence badge ──────────────────────────────────────────
                ConfidenceBadge(label = confidenceLabel, color = confidenceColor)

                Spacer(modifier = Modifier.height(14.dp))

                // ── Meta row: daily rate · days used · outliers ───────────────
                ForecastMetaRow(prediction = prediction)

                if (prediction.categoryForecasts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Per-category breakdown (top 4) ────────────────────────
                    Text(
                        text = "Kategóriánkénti becslés",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val topCategories = prediction.categoryForecasts.entries
                        .sortedByDescending { it.value }
                        .take(4)

                    topCategories.forEach { (category, projected) ->
                        val budgetLimit = budgetLimits[category]
                        val overBudget = budgetLimit != null && projected > budgetLimit
                        CategoryForecastRow(
                            category = category,
                            projected = projected,
                            budgetLimit = budgetLimit,
                            overBudget = overBudget
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfidenceBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ForecastMetaRow(prediction: PredictionResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetaChip(
            label = "Napi átlag",
            value = "${formatAmount(prediction.dailyRate)} Ft",
            modifier = Modifier.weight(1f)
        )
        MetaChip(
            label = "Felhasznált napok",
            value = "${prediction.daysOfData} nap",
            modifier = Modifier.weight(1f)
        )
        MetaChip(
            label = "Kizárt kiugró",
            value = "${prediction.outlierCount} nap",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryForecastRow(
    category: String,
    projected: Double,
    budgetLimit: Double?,
    overBudget: Boolean
) {
    val rowColor = if (overBudget) BudgetExceeded else MaterialTheme.colorScheme.onSurface
    val bgColor  = if (overBudget) BudgetExceeded.copy(alpha = 0.06f) else Color.Transparent

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category colour dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(CategoryMapper.getColor(category), androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Category name
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = rowColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Projected amount
            Text(
                text = "${formatAmount(projected)} Ft",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = rowColor
            )

            // Budget warning icon
            if (overBudget && budgetLimit != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Keret túllépve",
                    tint = BudgetExceeded,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    // Budget limit sub-label when over budget
    if (overBudget && budgetLimit != null) {
        Text(
            text = "  Keret: ${formatAmount(budgetLimit)} Ft — ${formatAmount(projected - budgetLimit)} Ft túllépés",
            style = MaterialTheme.typography.labelSmall,
            color = BudgetExceeded.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 18.dp, bottom = 2.dp)
        )
    }
}