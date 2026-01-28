package com.example.personalfinanceapp.presentation.home

import com.example.personalfinanceapp.presentation.home.components.ExpenseCard
import com.example.personalfinanceapp.presentation.home.components.RecurringItemCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.presentation.home.components.BalanceCardWithSparkline
import com.example.personalfinanceapp.presentation.home.components.ExpenseDialog
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.utils.extractAmount
import com.example.personalfinanceapp.utils.mapToHungarian
import com.example.personalfinanceapp.ml.ExpenseClassifier
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val classifier = remember { ExpenseClassifier(context) }
    val expenseList by viewModel.allExpenses.collectAsState(initial = emptyList())
    val total by viewModel.totalSpending.collectAsState(initial = 0.0)

    val recentExpenses = expenseList.take(10)

    var textInput by remember { mutableStateOf("") }
    var _showDialog by remember { mutableStateOf(false) }
    var activeExpense by remember { mutableStateOf<Expense?>(null) }
    val scope = rememberCoroutineScope()

    fun openNewDraft() {
        if (textInput.isBlank()) return
        val amountVal = extractAmount(textInput)
        val titleVal = textInput.replace(amountVal.toInt().toString(), "")
            .replace(amountVal.toString(), "").trim().ifBlank { "Ismeretlen" }

        scope.launch {
            val historyCategory = viewModel.getCategoryPrediction(titleVal)
            val finalCategory = historyCategory ?: mapToHungarian(classifier.classify(titleVal) ?: "Other")

            activeExpense = Expense(
                id = 0, title = titleVal, amount = if (amountVal > 0) amountVal else 0.0,
                category = finalCategory, description = "", date = System.currentTimeMillis(),
                isIncome = (finalCategory == "Bevétel")
            )
            _showDialog = true
        }
    }

    fun openEditDraft(expense: Expense) {
        activeExpense = expense
        _showDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                "Profile",
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )

            Surface(
                color = Color(0xFFF0F0F0),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .height(40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keresés...", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Icon(
                Icons.Default.Settings,
                "Settings",
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BalanceCardWithSparkline(total = total ?: 0.0)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Legutóbbi tranzakciók",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(recentExpenses, key = { it.id }) { expense ->
                val dismissScope = rememberCoroutineScope()
                var showDeleteDialog by remember { mutableStateOf(false) }
                val dismissState = rememberSwipeToDismissBoxState()

                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    LaunchedEffect(Unit) { showDeleteDialog = true }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false; dismissScope.launch { dismissState.reset() }
                        },
                        title = { Text("Törlés") },
                        text = { Text("Törlöd: ${expense.title}?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteExpense(expense)
                                showDeleteDialog = false
                            }) { Text("Törlés", color = Color.Red) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog =
                                    false; dismissScope.launch { dismissState.reset() }
                            }) { Text("Mégse") }
                        }
                    )
                }

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color =
                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(12.dp))
                                .padding(end = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
                    }
                ) {
                    Box(Modifier.clickable { openEditDraft(expense) }) {
                        ExpenseCard(expense)
                    }
                }
            }

            item {
                TextButton(
                    onClick = { /* Navigate to History Screen TODO */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Összes megtekintése", textDecoration = TextDecoration.Underline)
                }
            }
        }

        Surface(
            tonalElevation = 12.dp,
            shadowElevation = 12.dp,
            color = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Mit vettél?") },
                    placeholder = { Text("pl. Tesco 3500", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { openNewDraft() },
                    shape = CircleShape,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Rögzítés")
                }
            }
        }
    }

    if (_showDialog && activeExpense != null) {
        ExpenseDialog(
            initialTitle = activeExpense!!.title,
            initialAmount = if (activeExpense!!.amount > 0) activeExpense!!.amount.toInt().toString() else "",
            initialCategory = activeExpense!!.category,
            initialDescription = activeExpense!!.description ?: "",
            onDismiss = { _showDialog = false },
            onConfirm = { title, amount, category, desc ->
                val isIncome = (category == "Bevétel")
                val finalExpense = activeExpense!!.copy(
                    title = title, amount = amount, category = category, description = desc, isIncome = isIncome
                )
                if (finalExpense.id == 0) viewModel.addExpense(finalExpense) else viewModel.updateExpense(finalExpense)
                _showDialog = false
                textInput = ""
            }
        )
    }
}