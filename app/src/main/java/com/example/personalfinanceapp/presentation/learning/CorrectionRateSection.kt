package com.example.personalfinanceapp.presentation.learning

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.CategoryCorrectionStat
import com.example.personalfinanceapp.presentation.home.HomeViewModel

/**
 * Displays a ranked list of AI correction rates per category.
 * Add as an item inside LearningScreen's LazyColumn.
 */
@Composable
fun CorrectionRateSection(viewModel: HomeViewModel) {
    val stats by viewModel.correctionStats.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kategória javítási arány",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hány esetben módosítottad az AI javaslatát",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "Még nincs elegendő adat. Adj hozzá néhány kiadást, hogy az AI tanulhasson!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                stats.forEach { stat ->
                    CorrectionRateRow(stat)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CorrectionRateRow(stat: CategoryCorrectionStat) {
    val rate = stat.correctionRate ?: 0.0

    // Colour shifts from green (low correction) → amber → red (high correction)
    val barColor = when {
        rate >= 0.6 -> MaterialTheme.colorScheme.error
        rate >= 0.3 -> Color(0xFFF59E0B) // amber
        else        -> Color(0xFF22C55E) // green
    }

    val animatedRate by animateFloatAsState(
        targetValue = rate.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "correction_bar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stat.totalCorrections}/${stat.totalSuggestions} javítva (${stat.correctionRatePercent})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRate)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}