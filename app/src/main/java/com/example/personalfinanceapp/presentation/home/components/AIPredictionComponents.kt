package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.ml.EnsemblePrediction

/**
 * COMPACT AI Prediction Card (Hungarian)
 * Shows ensemble prediction in a clean, minimal way
 *
 * Add this to your ExpenseDialog when prediction is available
 */
@Composable
fun AIPredictionCardCompact(
    prediction: EnsemblePrediction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI javaslat",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(prediction.confidence * 100).toInt()}% biztos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Final prediction (main)
            Text(
                text = "→ ${prediction.finalCategory}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Show model breakdown if they disagree
            if (prediction.tflitePrediction?.category != prediction.naiveBayesPrediction?.category) {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prediction.tflitePrediction?.let { tflite ->
                        ModelChip(
                            label = "🤖 ${tflite.category}",
                            weight = tflite.weight,
                            isWinner = tflite.category == prediction.finalCategory
                        )
                    }
                    prediction.naiveBayesPrediction?.let { nb ->
                        ModelChip(
                            label = "📊 ${nb.category}",
                            weight = nb.weight,
                            isWinner = nb.category == prediction.finalCategory
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small chip showing individual model prediction
 */
@Composable
private fun ModelChip(
    label: String,
    weight: Double,
    isWinner: Boolean
) {
    Surface(
        color = if (isWinner) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${(weight * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Learning indicator animation (Hungarian)
 * Shows briefly when AI learns from user correction
 */
@Composable
fun LearningIndicatorCompact(
    show: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + expandVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Tanulás",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "✨ Az AI tanulja a választásodat...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * USAGE EXAMPLE IN YOUR EXISTING EXPENSEDIALOG:
 *
 * // Add these states at the top:
 * var currentPrediction by remember { mutableStateOf<EnsemblePrediction?>(null) }
 * var showLearning by remember { mutableStateOf(false) }
 *
 * // Predict when title changes:
 * LaunchedEffect(title) {
 *     if (title.length >= 3) {
 *         delay(500)  // Debounce
 *         currentPrediction = viewModel.predictCategoryEnsemble(title)
 *         selectedCategory = currentPrediction?.finalCategory ?: ""
 *     }
 * }
 *
 * // In your dialog content, add this:
 * AnimatedVisibility(visible = currentPrediction != null) {
 *     Column {
 *         currentPrediction?.let { AIPredictionCardCompact(it) }
 *         Spacer(modifier = Modifier.height(8.dp))
 *     }
 * }
 *
 * // After category dropdown:
 * LearningIndicatorCompact(show = showLearning)
 *
 * // When user changes category:
 * if (currentPrediction != null && newCategory != selectedCategory) {
 *     viewModel.recordCategoryChoice(title, currentPrediction!!, newCategory)
 *     scope.launch {
 *         showLearning = true
 *         delay(2000)
 *         showLearning = false
 *     }
 * }
 *
 * // On save:
 * if (currentPrediction != null) {
 *     viewModel.recordCategoryChoice(title, currentPrediction!!, selectedCategory)
 * }
 */