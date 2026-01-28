package com.example.personalfinanceapp

import com.example.personalfinanceapp.presentation.home.HomeScreen
import com.example.personalfinanceapp.presentation.recurring.RecurringScreen
import com.example.personalfinanceapp.utils.formatAmount
import com.example.personalfinanceapp.utils.mapFrequency


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalfinanceapp.data.Frequency
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

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp)),
                containerColor = Color.White,
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