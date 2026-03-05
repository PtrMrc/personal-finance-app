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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.ml.EnsemblePrediction

/**
 * AI Prediction Card — clean, user-friendly (Hungarian)
 *
 * Replaces both AIPredictionCard (ExpenseDialog.kt) and the old AIPredictionCardCompact.
 * Use this everywhere a prediction needs to be shown.
 *
 * Shows:
 *  - The final predicted category, prominently
 *  - A human-readable confidence level (Biztos / Valószínű / Bizonytalan)
 *  - A short note explaining the source (both models / habits only / pre-trained only)
 *
 * Does NOT show raw percentages or model weights — those are internal details.
 */
@Composable
fun AIPredictionCard(
    prediction: EnsemblePrediction,
    modifier: Modifier = Modifier
) {
    val hasTflite = prediction.tflitePrediction != null
    val hasNaiveBayes = prediction.naiveBayesPrediction != null
    val bothAgree = hasTflite && hasNaiveBayes &&
            prediction.tflitePrediction!!.category == prediction.naiveBayesPrediction!!.category

    val (confidenceLabel, confidenceColor) = when {
        prediction.confidence >= 0.80 -> "Biztos" to Color(0xFF2E7D32)       // green
        prediction.confidence >= 0.55 -> "Valószínű" to Color(0xFFE65100)    // amber
        else -> "Bizonytalan" to Color(0xFF757575)                            // grey
    }

    // Human-readable source note
    val sourceNote = when {
        bothAgree -> "Mindkét modell ugyanezt javasolta"
        hasTflite && hasNaiveBayes -> "A modellek eltértek – legjobb becslés"
        hasNaiveBayes && !hasTflite -> "Szokásaid alapján"
        hasTflite && !hasNaiveBayes -> "Előre betanított modell alapján"
        else -> null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI javaslat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = prediction.finalCategory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                sourceNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence badge
            Surface(
                color = confidenceColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = confidenceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = confidenceColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Learning indicator animation (Hungarian)
 * Shows briefly when AI learns from a user correction.
 */
@Composable
fun LearningIndicator(
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Tanulás",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Köszi, ezt megjegyzem!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}