package com.example.personalfinanceapp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.CategoryTuple
import com.example.personalfinanceapp.data.Expense
import com.example.personalfinanceapp.data.Frequency
import com.example.personalfinanceapp.data.RecurringItem
import com.example.personalfinanceapp.ml.ExpenseClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                MainApp()
            }
        }
    }
}

// --- VIEWMODEL ---
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    val totalSpending: Flow<Double?> = db.expenseDao().getTotalSpending()
    val categoryBreakdown: Flow<List<CategoryTuple>> = db.expenseDao().getCategoryBreakdown()
    val allRecurringItems: Flow<List<RecurringItem>> = db.recurringDao().getAllRecurringItemsFlow()

    fun addExpense(expense: Expense) {
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

    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            db.recurringDao().insertRecurringItem(item)
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            db.recurringDao().deleteRecurringItem(item.id)
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Főoldal") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Stats") },
                    label = { Text("Elemzés") },
                    selected = currentRoute == "stats",
                    onClick = { navController.navigate("stats") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Recurring") },
                    label = { Text("Állandó") },
                    selected = currentRoute == "recurring",
                    onClick = { navController.navigate("recurring") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBox, contentDescription = "Learn") },
                    label = { Text("Jövő") },
                    selected = currentRoute == "learning",
                    onClick = { navController.navigate("learning") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(viewModel = viewModel())
            }
            composable("stats") {
                // Placeholder for Charts (we can add Vico later)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Elemzések hamarosan...")
                }
            }
            composable("recurring") {
                // We reuse your existing RecurringScreen
                RecurringScreen(
                    viewModel = viewModel(),
                    onBack = { navController.navigate("home") }
                )
            }
            composable("learning") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("AI Előrejelzés hamarosan...")
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val classifier = remember { ExpenseClassifier(context) }
    val expenseList by viewModel.allExpenses.collectAsState(initial = emptyList())
    val total by viewModel.totalSpending.collectAsState(initial = 0.0)

    val recentExpenses = expenseList.take(10)

    var textInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
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
            showDialog = true
        }
    }

    fun openEditDraft(expense: Expense) {
        activeExpense = expense
        showDialog = true
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

    if (showDialog && activeExpense != null) {
        ExpenseDialog(
            initialTitle = activeExpense!!.title,
            initialAmount = if (activeExpense!!.amount > 0) activeExpense!!.amount.toInt().toString() else "",
            initialCategory = activeExpense!!.category,
            initialDescription = activeExpense!!.description ?: "",
            onDismiss = { showDialog = false },
            onConfirm = { title, amount, category, desc ->
                val isIncome = (category == "Bevétel")
                val finalExpense = activeExpense!!.copy(
                    title = title, amount = amount, category = category, description = desc, isIncome = isIncome
                )
                if (finalExpense.id == 0) viewModel.addExpense(finalExpense) else viewModel.updateExpense(finalExpense)
                showDialog = false
                textInput = ""
            }
        )
    }
}

@Composable
fun BalanceCardWithSparkline(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Egyenleg",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
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
                text = "📈",
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                fontSize = 48.sp
            )
        }
    }
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
                    category = if (isIncome) "Bevétel" else "Egyéb",
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

// --- HELPERS & DIALOGS  ---

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
                    text = "${mapFrequency(item.frequency)} • Nap: ${item.dayOfMonth}.",
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
    var isIncome by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf("1") }

    var freqExpanded by remember { mutableStateOf(false) }
    var selectedFreq by remember { mutableStateOf(Frequency.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Ismétlődő Tétel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Megnevezés") })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Összeg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = day,
                    onValueChange = { if (it.length <= 2) day = it },
                    label = { Text("Nap (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
                    Text("Bevétel")
                }

                Box {
                    OutlinedTextField(
                        value = mapFrequency(selectedFreq),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gyakoriság") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { freqExpanded = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        Frequency.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(mapFrequency(f)) },
                                onClick = { selectedFreq = f; freqExpanded = false }
                            )
                        }
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

// --- UTILS---

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

// Helper: Translates Frequency enum to Hungarian
fun mapFrequency(frequency: Frequency): String {
    return when(frequency) {
        Frequency.MONTHLY -> "Havonta"
        Frequency.QUARTERLY -> "Negyedévente"
        Frequency.YEARLY -> "Évente"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SwipeToDismissBoxState.reset() {
    snapTo(SwipeToDismissBoxValue.Settled)
}