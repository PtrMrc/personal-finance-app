package com.example.personalfinanceapp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.ml.ExpenseClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC5),
                    background = Color(0xFFF5F5F5)
                )
            ) {
                MainScreen()
            }
        }
    }
}

// --- VIEWMODEL: Connects the Database to the UI ---
// We use AndroidViewModel so we can get the Application Context for the Database
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()

    fun addExpense(expense: Expense) {
        // We need a coroutine to run database operations in the background
        viewModelScope.launch {
            db.expenseDao().insertExpense(expense)
        }
    }
}

// --- THE MAIN SCREEN ---
@Composable
fun MainScreen(viewModel: ExpenseViewModel = viewModel()) {
    val context = LocalContext.current
    val classifier = remember { ExpenseClassifier(context) } // The AI Brain
    val expenseList by viewModel.allExpenses.collectAsState(initial = emptyList())

    // UI State
    var textInput by remember { mutableStateOf("") }

    // Dialog State ---
    var showDialog by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftAmount by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Egyéb") }

    // Logic: Prepare the draft, but don't save yet
    fun prepareExpense() {
        if (textInput.isBlank()) return

        val amountVal = extractAmount(textInput)
        // Clean the title: remove numbers
        val titleVal = textInput.replace(amountVal.toInt().toString(), "")
            .replace(amountVal.toString(), "")
            .trim()

        val predictedCategory = classifier.classify(titleVal) ?: "Egyéb"

        // Set draft values
        draftTitle = titleVal.ifBlank { "Ismeretlen" }
        draftAmount = if (amountVal > 0) amountVal.toInt().toString() else ""
        draftCategory = predictedCategory

        // Open the popup
        showDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), // Pushes up when keyboard opens
        bottomBar = {
            // INPUT AREA
            Surface(
                tonalElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("pl. Tesco 3500") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { prepareExpense() },
                        shape = CircleShape,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Draft")
                    }
                }
            }
        }
    ) { innerPadding ->
        // LIST AREA
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            Text(
                text = "Kiadások",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseList) { expense ->
                    ExpenseCard(expense)
                }
            }
            if (showDialog) {
                ExpenseDialog(
                    initialTitle = draftTitle,
                    initialAmount = draftAmount,
                    initialCategory = draftCategory,
                    onDismiss = { showDialog = false },
                    onConfirm = { title, amount, category, desc ->
                        val newExpense = Expense(
                            title = title,
                            amount = amount,
                            category = category,
                            description = desc,
                            date = System.currentTimeMillis(),
                            isIncome = false
                        )
                        viewModel.addExpense(newExpense)
                        showDialog = false
                        textInput = "" // Clear the input field only after success
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    initialTitle: String,
    initialAmount: String,
    initialCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf("") }

    // Category Dropdown State
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Transport", "Entertainment", "Bills", "Health", "Income", "Egyéb")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Kiadás") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Megnevezés") },
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Összeg (Ft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Category Dropdown
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Kategória") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "Select", Modifier.clickable { expanded = true })
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Leírás (Opcionális)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amountDouble > 0) {
                        onConfirm(title, amountDouble, category, description)
                    }
                }
            ) {
                Text("Mentés")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}

// --- CARD ITEM DESIGN ---
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
            // Category Icon/Circle
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

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (!expense.description.isNullOrBlank()) {
                    Text(text = expense.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Amount
            Text(
                text = "-${expense.amount.toInt()} Ft",
                color = Color(0xFFD32F2F), // Red for expense
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// --- HELPERS ---

// 1. Extracts the first number found in a string (e.g. "Pizza 3000" -> 3000.0)
fun extractAmount(text: String): Double {
    val regex = Regex("[0-9]+")
    val match = regex.find(text)
    return match?.value?.toDouble() ?: 0.0
}

// 2. Returns a color based on category name
fun getColorForCategory(category: String): Color {
    return when(category) {
        "Food" -> Color(0xFFFF9800)
        "Transport" -> Color(0xFF2196F3)
        "Entertainment" -> Color(0xFF9C27B0)
        "Bills" -> Color(0xFFF44336)
        "Health" -> Color(0xFF4CAF50)
        "Income" -> Color(0xFF009688)
        else -> Color.Gray
    }
}