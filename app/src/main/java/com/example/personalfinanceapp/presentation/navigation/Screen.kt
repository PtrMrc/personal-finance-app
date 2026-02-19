package com.example.personalfinanceapp.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Recurring : Screen("recurring")
    object Learning : Screen("learning")
    object History : Screen("history")
    object BudgetSetup : Screen("budget_setup")
}