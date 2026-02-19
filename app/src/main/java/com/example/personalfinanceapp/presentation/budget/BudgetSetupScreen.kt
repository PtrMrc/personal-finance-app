package com.example.personalfinanceapp.presentation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.utils.Validation
import com.example.personalfinanceapp.utils.ValidationResult
import com.example.personalfinanceapp.utils.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val budgets by viewModel.budgetProgress.collectAsState()

    // Form State
    var selectedCategory by remember { mutableStateOf("Összesen") }
    var expanded by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }
    var limitError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Összesen", "Élelmiszer", "Utazás", "Szórakozás", "Számlák", "Egészség", "Egyéb")

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vissza", tint = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Költségvetések",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Kiadási limitek beállítása",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. ADD NEW BUDGET FORM ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Új limit hozzáadása",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        // Category Dropdown
                        Box {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                label = { Text("Kategória") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expanded = true })
                                }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = { selectedCategory = cat; expanded = false }
                                    )
                                }
                            }
                        }

                        // Limit Input
                        OutlinedTextField(
                            value = limitInput,
                            onValueChange = { limitInput = it; limitError = null },
                            label = { Text("Havi keret (Ft)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = limitError != null,
                            supportingText = limitError?.let { { Text(it) } }
                        )

                        // Save Button
                        Button(
                            onClick = {
                                val validation = Validation.validateAmount(limitInput)
                                if (validation is ValidationResult.Error) {
                                    limitError = validation.message
                                } else {
                                    viewModel.setBudget(selectedCategory, limitInput.toDouble())
                                    limitInput = ""
                                    // Reset to default or keep it to add more
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("Mentés", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // --- 2. ACTIVE BUDGETS LIST ---
            item {
                Text(
                    text = "Aktív limitek",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Text(
                        text = "Még nincsenek beállítva költségvetési limitek.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(budgets, key = { it.category }) { budget ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = budget.category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Keret: ${formatAmount(budget.limit)} Ft",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteBudget(budget.category) },
                                modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Törlés", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }
}