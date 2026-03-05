package com.example.personalfinanceapp.presentation.recurring.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.utils.CategoryMapper
import com.example.personalfinanceapp.utils.formatAmount
import com.example.personalfinanceapp.utils.mapFrequency
import java.time.LocalDate

@Composable
fun ModernRecurringItemCard(
    item: RecurringItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = if (item.isIncome) Color(0xFF10B981) else CategoryMapper.getColor(item.category)
    val categoryIcon = CategoryMapper.getIcon(item.category)
    val nextDue = nextDueDate(item.dayOfMonth, item.frequency)
    val nextDueText = formatNextDue(nextDue)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = accentColor.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + title + meta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${mapFrequency(item.frequency)} • ${item.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Következő: $nextDueText",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Amount + action buttons
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (item.isIncome) "+" else "-"}${formatAmount(item.amount)} Ft",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Szerkesztés",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Törlés",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun nextDueDate(dayOfMonth: Int, frequency: Frequency): LocalDate {
    val today = LocalDate.now()
    val safeDay = dayOfMonth.coerceAtMost(today.month.maxLength())

    return when (frequency) {
        Frequency.MONTHLY -> {
            val candidate = today.withDayOfMonth(safeDay.coerceAtMost(today.lengthOfMonth()))
            if (!candidate.isBefore(today)) candidate else candidate.plusMonths(1)
                .let { it.withDayOfMonth(safeDay.coerceAtMost(it.lengthOfMonth())) }
        }
        Frequency.QUARTERLY -> {
            // Try this month, then +1, +2, +3 months until we find a future occurrence
            var candidate = today.withDayOfMonth(safeDay.coerceAtMost(today.lengthOfMonth()))
            repeat(4) {
                if (candidate.isBefore(today)) {
                    candidate = candidate.plusMonths(1)
                        .let { it.withDayOfMonth(safeDay.coerceAtMost(it.lengthOfMonth())) }
                }
            }
            candidate
        }
        Frequency.YEARLY -> {
            val candidate = today.withDayOfMonth(safeDay.coerceAtMost(today.lengthOfMonth()))
            if (!candidate.isBefore(today)) candidate else candidate.plusYears(1)
                .let { it.withDayOfMonth(safeDay.coerceAtMost(it.lengthOfMonth())) }
        }
    }
}

private fun formatNextDue(date: LocalDate): String {
    val today = LocalDate.now()
    val daysUntil = today.until(date).days +
            today.until(date).months * 30 +
            today.until(date).years * 365

    // Use a simple calculation for display
    val actualDays = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        actualDays == 0L -> "ma"
        actualDays == 1L -> "holnap"
        actualDays < 7L -> "${actualDays} nap múlva"
        actualDays < 31L -> "${actualDays / 7} hét múlva"
        else -> "${date.year}.${date.monthValue.toString().padStart(2, '0')}.${date.dayOfMonth.toString().padStart(2, '0')}"
    }
}