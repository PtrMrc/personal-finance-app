package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ml.PredictionResult
import com.example.personalfinanceapp.utils.formatAmount

// Confidence colours (kept local to avoid cross-module coupling)
private val ConfidenceLow  = Color(0xFFF59E0B)
private val ConfidenceMid  = Color(0xFF3B82F6)
private val ConfidenceHigh = Color(0xFF10B981)
private val BudgetWarn     = Color(0xFFEF4444)

private fun confidenceColor(c: Double) = when {
    c < 0.25 -> ConfidenceLow
    c < 0.60 -> ConfidenceMid
    else     -> ConfidenceHigh
}

private fun confidenceShortLabel(c: Double, days: Int, usedHistory: Boolean): String {
    val hist = if (usedHistory) " · hist." else ""
    return when {
        c < 0.25 -> "Alacsony – $days nap$hist"
        c < 0.60 -> "Közepes – $days nap$hist"
        else     -> "Magas – $days nap$hist"
    }
}

/**
 * Compact forecast card for the HomeScreen.
 *
 * Shows:
 * - Month-end total forecast + confidence badge
 * - One-line budget warning if any category projection exceeds its limit
 * - "Részletek" arrow that navigates to StatsScreen for the full breakdown
 *
 * @param prediction      Output of [MLMath.calculateSmartForecast]; null means no data yet.
 * @param budgetLimits    Category → monthly limit map from [HomeViewModel.budgetProgress].
 * @param onViewDetails   Called when the user taps the card (navigate to StatsScreen).
 */
@Composable
fun ForecastSummaryCard(
    prediction: PredictionResult,
    budgetLimits: Map<String, Double> = emptyMap(),
    onViewDetails: () -> Unit = {}
) {
    // Find worst over-budget category (largest absolute overshoot)
    val worstOverBudget: Pair<String, Double>? = prediction.categoryForecasts
        .entries
        .mapNotNull { (cat, proj) ->
            val limit = budgetLimits[cat] ?: return@mapNotNull null
            if (proj > limit) cat to (proj - limit) else null
        }
        .maxByOrNull { it.second }

    val confidenceColor = confidenceColor(prediction.confidence)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            .clickable(onClick = onViewDetails),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Havi előrejelzés",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Részletek",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Forecast total + confidence badge ─────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${formatAmount(prediction.forecastedTotal)} Ft",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Confidence pill
                    Surface(
                        color = confidenceColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = confidenceShortLabel(prediction.confidence, prediction.daysOfData, prediction.usedHistoricalData),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = confidenceColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }

                // ── Budget warning (one line, most severe category) ───────────
                if (worstOverBudget != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BudgetWarn,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${worstOverBudget.first}: várható keret-túllépés " +
                                    "+${formatAmount(worstOverBudget.second)} Ft",
                            style = MaterialTheme.typography.labelSmall,
                            color = BudgetWarn,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}