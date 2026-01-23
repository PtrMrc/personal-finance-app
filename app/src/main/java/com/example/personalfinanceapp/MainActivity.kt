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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.personalfinanceapp.data.CategoryTuple
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.ml.ExpenseClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.NumberFormat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.data.RecurringItem
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workRequest = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySubscriptionCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

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

    val totalSpending: Flow<Double?> = db.expenseDao().getTotalSpending()

    val categoryBreakdown: Flow<List<CategoryTuple>> = db.expenseDao().getCategoryBreakdown()

    fun addExpense(expense: Expense) {
        // We need a coroutine to run database operations in the background
        viewModelScope.launch {
            db.expenseDao().insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            db.expenseDao().deleteExpense(expense)
        }
    }

    suspend fun getCategoryPrediction(title: String): String? {
        return db.expenseDao().getLastCategoryForTitle(title)
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            db.expenseDao().updateExpense(expense)
        }
    }

    // --- RECURRING / SUBSCRIPTION LOGIC ---
    // 1. Get the list
    val allRecurringItems: Flow<List<RecurringItem>> = db.recurringDao().getAllRecurringItemsFlow()

    // 2. Add new rule
    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            db.recurringDao().insertRecurringItem(item)
        }
    }

    // 3. Delete rule
    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            db.recurringDao().deleteRecurringItem(item.id)
        }
    }
}

// --- THE MAIN SCREEN ---
@Composable
fun MainScreen(viewModel: ExpenseViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf("Home") } // "Home" or "Recurring"

    val context = LocalContext.current
    val classifier = remember { ExpenseClassifier(context) } // The AI Brain
    val expenseList by viewModel.allExpenses.collectAsState(initial = emptyList())

    // UI State
    var textInput by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var activeExpense by remember { mutableStateOf<Expense?>(null) }

    val total by viewModel.totalSpending.collectAsState(initial = 0.0)
    val breakdown by viewModel.categoryBreakdown.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // 1. Logic for NEW Item
    fun openNewDraft() {
        if (textInput.isBlank()) return

        val amountVal = extractAmount(textInput)
        val titleVal = textInput.replace(amountVal.toInt().toString(), "")
            .replace(amountVal.toString(), "")
            .trim()
            .ifBlank { "Ismeretlen" }

        scope.launch {
            // Check Memory
            val historyCategory = viewModel.getCategoryPrediction(titleVal)

            // Decide Category
            val finalCategory = if (historyCategory != null) {
                historyCategory
            } else {
                val englishPrediction = classifier.classify(titleVal) ?: "Other"
                mapToHungarian(englishPrediction)
            }

            // Create the "Draft" Object (ID is 0 because it's new)
            activeExpense = Expense(
                id = 0,
                title = titleVal,
                amount = if (amountVal > 0) amountVal else 0.0,
                category = finalCategory,
                description = "",
                date = System.currentTimeMillis(),
                isIncome = (finalCategory == "Bevétel")
            )
            showDialog = true
        }
    }

    // 2. Logic for EDITING an Existing Item
    fun openEditDraft(expense: Expense) {
        activeExpense = expense // Load the existing item
        showDialog = true
    }
    if (currentScreen == "Recurring") {
        RecurringScreen(viewModel = viewModel, onBack = { currentScreen = "Home" })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(), // Pushes up when keyboard opens
            bottomBar = {
                // INPUT AREA
                Surface(
                    tonalElevation = 12.dp,
                    shadowElevation = 12.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp
                    ) // Rounded top corners looks modern
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        // 1. Small Header Title
                        Text(
                            text = "Új tétel rögzítése",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 2. The Smart Input Field
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                // Label floats up when you type
                                label = { Text("Mit vettél?") },
                                // Placeholder stays until you type
                                placeholder = { Text("pl. Tesco 3500", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // 3. The Send Button
                            Button(
                                onClick = { openNewDraft() },
                                shape = CircleShape,
                                contentPadding = PaddingValues(16.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Rögzítés"
                                )
                            }
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kiadások",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // The Button to open Recurring Screen
                    IconButton(onClick = { currentScreen = "Recurring" }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Subscriptions")
                    }
                }

                SummaryCard(total = total, breakdown = breakdown)

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenseList, key = { it.id }) { expense ->
                        // 1. Setup Scope for manual animation (Resetting the swipe)
                        val scope = rememberCoroutineScope()
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        // 2. The State
                        val dismissState = rememberSwipeToDismissBoxState()

                        // 3. OBSERVER: Watch the state. If it swipes, trigger the dialog.
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            LaunchedEffect(Unit) {
                                showDeleteDialog = true
                            }
                        }

                        // 4. The Dialog
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    // If they click outside, close dialog AND reset swipe
                                    showDeleteDialog = false
                                    scope.launch { dismissState.reset() }
                                },
                                title = { Text("Törlés") },
                                text = { Text("Biztosan törölni szeretnéd ezt a tételt: ${expense.title}?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteExpense(expense)
                                            // No need to reset state, the item is gone!
                                            showDeleteDialog = false
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                    ) { Text("Törlés") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        // CANCEL: Close dialog AND slide back to normal
                                        showDeleteDialog = false
                                        scope.launch { dismissState.reset() }
                                    }) { Text("Mégse") }
                                }
                            )
                        }

                        // 5. The Swipe Box
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false, // Only swipe right-to-left
                            backgroundContent = {
                                val color =
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openEditDraft(expense) }
                            ) {
                                ExpenseCard(expense)
                            }
                        }
                    }
                }

                if (showDialog && activeExpense != null) {
                    ExpenseDialog(
                        initialTitle = activeExpense!!.title,
                        initialAmount = if (activeExpense!!.amount > 0) activeExpense!!.amount.toInt()
                            .toString() else "",
                        initialCategory = activeExpense!!.category,
                        initialDescription = activeExpense!!.description ?: "",
                        onDismiss = { showDialog = false },
                        onConfirm = { title, amount, category, desc ->
                            val isIncome = (category == "Bevétel")

                            // Update the active object with the new text from the inputs
                            val finalExpense = activeExpense!!.copy(
                                title = title,
                                amount = amount,
                                category = category,
                                description = desc,
                                isIncome = isIncome
                                // We keep the original ID and Date!
                            )

                            if (finalExpense.id == 0) {
                                // ID is 0, so it's NEW -> Insert
                                viewModel.addExpense(finalExpense)
                            } else {
                                // ID is not 0, so it EXISTS -> Update
                                viewModel.updateExpense(finalExpense)
                            }

                            showDialog = false
                            textInput = ""
                        }
                    )
                }
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
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }

    var showError by remember { mutableStateOf(false) }

    // Category Dropdown State
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Élelmiszer", "Utazás", "Szórakozás", "Számlák", "Egészség", "Bevétel", "Egyéb")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Tétel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Megnevezés") },
                    singleLine = true,
                    isError = showError && title.isBlank()
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Összeg (Ft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError && amount.toDoubleOrNull() == null
                )

                if (showError) {
                    Text(
                        text = "Kérlek add meg a nevet és az összeget!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                    } else {
                        showError = true
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
                    text = SimpleDateFormat("yyyy. MM. dd. HH:mm", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Amount
            Text(
                text = if (expense.isIncome) "+${formatAmount(expense.amount)} Ft" else "-${formatAmount(expense.amount)} Ft",
                color = if (expense.isIncome) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SummaryCard(total: Double?, breakdown: List<CategoryTuple>) {
    val balance = total ?: 0.0
    val balanceColor = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Grand Total
            Text(text = "Egyenleg", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${formatAmount(total ?: 0.0)} Ft",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Breakdown
            breakdown.forEach { item ->
                val isIncome = (item.category == "Bevétel")
                val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFD32F2F)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item.category, fontWeight = FontWeight.SemiBold)
                    Text(text = "${formatAmount(item.total)} Ft", color = amountColor)
                }
            }
        }
    }
}

// --- HELPERS ---

// Extracts the first number found in a string (e.g. "Pizza 3000" -> 3000.0)
fun extractAmount(text: String): Double {
    val regex = Regex("[0-9]+")
    val match = regex.find(text)
    return match?.value?.toDouble() ?: 0.0
}

// Logic: Translates AI (English) -> UI (Hungarian)
fun mapToHungarian(englishCategory: String): String {
    return when(englishCategory) {
        "Food" -> "Élelmiszer"
        "Transport" -> "Utazás"
        "Entertainment" -> "Szórakozás"
        "Bills" -> "Számlák"
        "Health" -> "Egészség"
        "Income" -> "Bevétel"
        else -> "Egyéb"
    }
}

// Returns color based on category names
fun getColorForCategory(category: String): Color {
    return when(category) {
        "Food", "Élelmiszer" -> Color(0xFFFF9800)      // Orange
        "Transport", "Utazás" -> Color(0xFF2196F3)     // Blue
        "Entertainment", "Szórakozás" -> Color(0xFF9C27B0) // Purple
        "Bills", "Számlák" -> Color(0xFFF44336)        // Red
        "Health", "Egészség" -> Color(0xFF4CAF50)      // Green
        "Income", "Bevétel" -> Color(0xFF009688)       // Teal
        else -> Color.Gray
    }
}

// Helper: Formats 12345.0 -> "12 345"
fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("hu", "HU")) // Hungarian format uses spaces/dots
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SwipeToDismissBoxState.reset() {
    snapTo(SwipeToDismissBoxValue.Settled)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allRecurringItems.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Állandó tételek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                RecurringItemCard(item, onDelete = { viewModel.deleteRecurringItem(item) })
            }
        }
    }

    if (showAddDialog) {
        AddRecurringDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, isIncome, freq, day ->
                val newItem = RecurringItem(
                    title = title,
                    amount = amount,
                    category = if (isIncome) "Bevétel" else "Egyéb", // Simplified category logic
                    isIncome = isIncome,
                    frequency = freq,
                    dayOfMonth = day
                )
                viewModel.addRecurringItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RecurringItemCard(item: RecurringItem, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = item.title, fontWeight = FontWeight.Bold)
                Text(
                    text = "${item.frequency} • Nap: ${item.dayOfMonth}.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatAmount(item.amount)} Ft",
                    fontWeight = FontWeight.Bold,
                    color = if (item.isIncome) Color(0xFF4CAF50) else Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Boolean, Frequency, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) } // Default to Expense
    var day by remember { mutableStateOf("1") }

    // Frequency Dropdown
    var freqExpanded by remember { mutableStateOf(false) }
    var selectedFreq by remember { mutableStateOf(Frequency.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Ismétlődő Tétel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Megnevezés (pl. Netflix)") })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Összeg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Day Input (Simple number field 1-31)
                OutlinedTextField(
                    value = day,
                    onValueChange = { if (it.length <= 2) day = it },
                    label = { Text("Nap (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Income Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
                    Text("Ez Bevétel (pl. Fizetés)?")
                }

                // Frequency Dropdown
                Box {
                    OutlinedTextField(
                        value = when(selectedFreq) {
                            Frequency.MONTHLY -> "Havonta"
                            Frequency.QUARTERLY -> "Negyedévente"
                            Frequency.YEARLY -> "Évente"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gyakoriság") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { freqExpanded = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        DropdownMenuItem(text = { Text("Havonta") }, onClick = { selectedFreq = Frequency.MONTHLY; freqExpanded = false })
                        DropdownMenuItem(text = { Text("Negyedévente") }, onClick = { selectedFreq = Frequency.QUARTERLY; freqExpanded = false })
                        DropdownMenuItem(text = { Text("Évente") }, onClick = { selectedFreq = Frequency.YEARLY; freqExpanded = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val dayInt = day.toIntOrNull() ?: 1
                if (title.isNotBlank() && amt > 0) {
                    onConfirm(title, amt, isIncome, selectedFreq, dayInt)
                }
            }) { Text("Mentés") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } }
    )
}