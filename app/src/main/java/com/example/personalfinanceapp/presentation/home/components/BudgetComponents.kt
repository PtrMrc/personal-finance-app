package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.home.BudgetProgress
import com.example.personalfinanceapp.presentation.home.getCategoryColor
import com.example.personalfinanceapp.presentation.home.getCategoryIcon
import com.example.personalfinanceapp.utils.formatAmount

@Composable
fun BudgetProgressSection(
    budgets: List<BudgetProgress>,
    onSetupClick: () -> Unit
) {
    if (budgets.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Havi Költségvetés",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Kövesd nyomon a kiadásaid",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            // Edit Button
            IconButton(
                onClick = onSetupClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Beállítások",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            // Empty State Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSetupClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Nincs beállítva limit",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Kattints ide a beállításhoz",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Existing progress bars
            budgets.forEach { progress ->
                BudgetProgressBar(progress)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BudgetProgressBar(progress: BudgetProgress) {
    val progressColor = when {
        progress.isExceeded -> Color(0xFFEF4444) // Red
        progress.isWarning -> Color(0xFFF59E0B)  // Orange
        else -> Color(0xFF10B981)                // Green
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage.toFloat().coerceIn(0f, 1f),
        animationSpec = tween(1000)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon based on category (reusing your helper)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(getCategoryColor(progress.category).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(progress.category),
                        contentDescription = null,
                        tint = getCategoryColor(progress.category),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = progress.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (progress.isWarning || progress.isExceeded) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = "Figyelmeztetés",
                    tint = progressColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(progressColor, RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatAmount(progress.spent)} Ft",
                style = MaterialTheme.typography.labelMedium,
                color = progressColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/ ${formatAmount(progress.limit)} Ft",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}