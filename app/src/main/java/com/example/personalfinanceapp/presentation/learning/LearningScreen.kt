package com.example.personalfinanceapp.presentation.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import java.util.Locale.getDefault

@Composable
fun LearningScreen(viewModel: HomeViewModel) {
    val lessons = listOf(
        FinanceLesson(1, "Az 50/30/20 szabály", "5 perc", Icons.Default.PieChart, Color(0xFF6366F1), LessonLevel.BEGINNER),
        FinanceLesson(2, "Vészhelyzeti tartalék", "8 perc", Icons.Default.Shield, Color(0xFF10B981), LessonLevel.BEGINNER),
        FinanceLesson(3, "Hogyan működik a kamatos kamat?", "12 perc", Icons.Default.TrendingUp, Color(0xFFF59E0B), LessonLevel.INTERMEDIATE),
        FinanceLesson(4, "TBSZ és Állampapír alapok", "15 perc", Icons.Default.AccountBalance, Color(0xFF8B5CF6), LessonLevel.ADVANCED)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // --- Header (Matches ModernHeader pattern) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Tudástár",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Fejleszd a pénzügyi tudatosságod",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Featured Card (Budgeting Rule) ---
            item {
                FeaturedLessonCard()
            }

            item {AILearningCard(viewModel)}

            item {
                Text(
                    text = "Tananyagok",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // --- Lesson List ---
            items(lessons) { lesson ->
                LessonCard(lesson)
            }
        }
    }
}

@Composable
fun FeaturedLessonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Napi Tipp",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Mi az a TBSZ számla?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tanuld meg, hogyan adózhatsz 0%-kal.",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = 20.dp)
            )
        }
    }
}

@Composable
fun LessonCard(lesson: FinanceLesson) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(lesson.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(lesson.icon, contentDescription = null, tint = lesson.color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "${lesson.duration} • ${
                        lesson.level.name.lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}