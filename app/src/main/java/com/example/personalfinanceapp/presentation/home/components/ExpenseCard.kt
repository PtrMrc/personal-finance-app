package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.utils.formatAmount
import com.example.personalfinanceapp.utils.getColorForCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.take

@Composable
fun ExpenseCard(expense: Expense) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(getColorForCategory(expense.category), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = expense.category.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (!expense.description.isNullOrBlank()) {
                    Text(text = expense.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(
                    text = SimpleDateFormat("yyyy. MM. dd. HH:mm", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(
                text = if (expense.isIncome) "+${formatAmount(expense.amount)} Ft" else "-${formatAmount(expense.amount)} Ft",
                color = if (expense.isIncome) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}