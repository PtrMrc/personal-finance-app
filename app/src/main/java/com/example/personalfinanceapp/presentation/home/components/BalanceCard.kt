package com.example.personalfinanceapp.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.BackgroundSimple
import com.example.personalfinanceapp.ui.theme.SurfaceSimple
import com.example.personalfinanceapp.utils.formatAmount

// Two green gradient stops — tweak these hex values to taste
private val GreenStart = Color(0xFF2E7D52)   // deep forest green
private val GreenEnd   = Color(0xFF56C596)   // bright mint

@Composable
fun BalanceCardWithSparkline(total: Double) {
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = if (isDark) {
        // Dark: keep dark surfaces but tint green instead of neutral
        Brush.linearGradient(
            colors = listOf(Color(0xFF1B3A2D), SurfaceSimple),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        // Light: rich green gradient replacing the default blue
        Brush.linearGradient(
            colors = listOf(GreenStart, GreenEnd),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = backgroundBrush, size = size)

                val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)

                // Arc 1 — anchored so the arc body actually falls within the card bounds
                drawArc(
                    color = Color.White.copy(alpha = 0.30f),
                    startAngle = 160f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(-size.width * 0.05f, size.height * 0.1f),
                    size = Size(size.width * 0.90f, size.height * 1.4f),
                    style = stroke
                )

                // Arc 2 — tighter, slightly offset for layered depth
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = 150f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.10f, -size.height * 0.10f),
                    size = Size(size.width * 0.85f, size.height * 1.6f),
                    style = stroke
                )

                // Arc 3 — outermost, most subtle
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 170f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.30f, -size.height * 0.30f),
                    size = Size(size.width * 0.95f, size.height * 1.8f),
                    style = stroke
                )
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Egyenleg",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatAmount(total)} Ft",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "\uD83D\uDCC8",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                fontSize = 48.sp
            )
        }
    }
}