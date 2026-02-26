package com.example.personalfinanceapp

import android.app.Application
import com.example.personalfinanceapp.presentation.home.HomeScreen
import com.example.personalfinanceapp.presentation.recurring.RecurringScreen
import com.example.personalfinanceapp.presentation.navigation.Screen
import com.example.personalfinanceapp.data.SettingsManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.personalfinanceapp.data.repository.BudgetRepository
import com.example.personalfinanceapp.presentation.budget.BudgetSetupScreen
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import com.example.personalfinanceapp.presentation.learning.LearningScreen
import com.example.personalfinanceapp.presentation.settings.SettingsScreen
import com.example.personalfinanceapp.presentation.stats.StatsScreen
import com.example.personalfinanceapp.presentation.stats.StatsViewModel
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
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

        val settingsManager = SettingsManager(this)

        setContent {
            val darkMode by settingsManager.darkModeFlow.collectAsState(initial = false)

            PersonalFinanceAppTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(settingsManager = settingsManager)
                }
            }
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.modelPerformanceDao().initializeDefaultModels()
        }
    }
}

/** Navigation item data class used by the minimal nav bar. */
private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

/**
 * Revolut-style minimal nav bar.
 * - No background card/pill — blends with screen background
 * - Icons only, no labels
 * - Small primary-colored dot under the active icon
 * - No ripple on tap
 */
@Composable
private fun MinimalNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem(Screen.Home,      Icons.Default.Home,        "Főoldal"),
        NavItem(Screen.Stats,     Icons.Default.BarChart,    "Elemzés"),
        NavItem(Screen.Recurring, Icons.Default.DateRange,   "Állandó"),
        NavItem(Screen.Learning,  Icons.Default.AutoAwesome, "Jövő"),
    )

    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        // Subtle top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 10.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.screen.route
                val interactionSource = remember { MutableInteractionSource() }

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navIconScale"
                )

                // Animate tint smoothly between active/inactive
                val iconAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.45f,
                    animationSpec = tween(200),
                    label = "navIconAlpha"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onNavigate(item.screen.route)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )

                    // Active indicator dot
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val activity = context as? ComponentActivity

    val db = AppDatabase.getDatabase(context)
    val budgetRepo = BudgetRepository(db.budgetDao())

    val sharedHomeViewModel = viewModel<HomeViewModel>(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(context.applicationContext as Application, budgetRepo) as T
            }
        }
    )
    // Only show the nav bar on top-level screens
    val topLevelRoutes = setOf(
        Screen.Home.route, Screen.Stats.route,
        Screen.Recurring.route, Screen.Learning.route
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                MinimalNavBar(currentRoute = currentRoute, onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            // Tab switches: smooth crossfade
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
            // ── Top-level tabs: crossfade ──────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = sharedHomeViewModel,
                    onSeeAllClick = { navController.navigate(Screen.History.route) },
                    onBudgetSetupClick = { navController.navigate(Screen.BudgetSetup.route) },
                    onDarkModeToggle = {
                        activity?.lifecycleScope?.launch { settingsManager.toggleDarkMode() }
                    }
                )
            }
            composable(Screen.Stats.route) {
                val repo = ExpenseRepository(db.expenseDao())
                val statsViewModel = viewModel<StatsViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            StatsViewModel(repo) as T
                    }
                )
                StatsScreen(viewModel = statsViewModel)
            }
            composable(Screen.Recurring.route) {
                RecurringScreen(
                    viewModel = sharedHomeViewModel,
                    onBack = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Learning.route) {
                LearningScreen(viewModel = sharedHomeViewModel)
            }

            // ── Drill-down screens: slide in from right ────────────────────
            composable(
                route = Screen.BudgetSetup.route,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(260))
                }
            ) {
                BudgetSetupScreen(
                    viewModel = sharedHomeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.History.route,
                enterTransition = {
                    slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(260))
                }
            ) {
                val repository = ExpenseRepository(db.expenseDao())
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            HistoryViewModel(repository) as T
                    }
                )
                HistoryScreen(viewModel = historyViewModel, onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(settingsManager = settingsManager, onBack = { navController.navigateUp() })
            }
        }
    }
}