package com.example.personalfinanceapp

import com.example.personalfinanceapp.presentation.home.HomeScreen
import com.example.personalfinanceapp.presentation.recurring.RecurringScreen
import com.example.personalfinanceapp.presentation.navigation.Screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalfinanceapp.data.AppDatabase
import com.example.personalfinanceapp.data.repository.ExpenseRepository
import com.example.personalfinanceapp.presentation.history.HistoryViewModel
import java.util.concurrent.TimeUnit
import com.example.personalfinanceapp.presentation.history.HistoryScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.presentation.learning.LearningScreen
import com.example.personalfinanceapp.presentation.stats.StatsScreen
import com.example.personalfinanceapp.presentation.stats.StatsViewModel
import com.example.personalfinanceapp.workers.RecurringWorker
import kotlinx.coroutines.launch

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

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.modelPerformanceDao().initializeDefaultModels()
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
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = currentRoute == Screen.Home.route,
                    onClick = { navController.navigate(Screen.Home.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Stats") },
                    label = { Text(stringResource(R.string.nav_stats)) },
                    selected = currentRoute == Screen.Stats.route,
                    onClick = { navController.navigate(Screen.Stats.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Recurring") },
                    label = { Text(stringResource(R.string.nav_recurring)) },
                    selected = currentRoute == Screen.Recurring.route,
                    onClick = { navController.navigate(Screen.Recurring.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBox, contentDescription = "Learn") },
                    label = { Text(stringResource(R.string.nav_learning)) },
                    selected = currentRoute == Screen.Learning.route,
                    onClick = { navController.navigate(Screen.Learning.route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel(),
                    onSeeAllClick = { navController.navigate(Screen.History.route) }
                )
            }
            composable(Screen.Recurring.route) {
                RecurringScreen(
                    viewModel = viewModel(),
                    onBack = { navController.navigate(Screen.Home.route) }
                )
            }
            composable(Screen.Learning.route) {
                LearningScreen(viewModel = viewModel())
            }
            composable(Screen.History.route) {
                val context = LocalContext.current
                val db = AppDatabase.getDatabase(context)
                val repository = ExpenseRepository(db.expenseDao())
                
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HistoryViewModel(repository) as T
                        }
                    }
                )

                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Stats.route) {
                val context = LocalContext.current
                val db = AppDatabase.getDatabase(context)
                val repo = ExpenseRepository(db.expenseDao())

                val statsViewModel = viewModel<StatsViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return StatsViewModel(repo) as T
                        }
                    }
                )

                StatsScreen(viewModel = statsViewModel)
            }
        }
    }
}