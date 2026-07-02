package com.example.myfit.ui.nav

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object Home : Screen("home")
    data object Fitness : Screen("fitness")
    data object Diet : Screen("diet")
    data object Chat : Screen("chat")
    data object Pedometer : Screen("pedometer")
    data object Settings : Screen("settings")
    data object MyProducts : Screen("my_products")
    data object Archive : Screen("archive")
    data object DayDetail : Screen("day_detail/{date}") {
        fun createRoute(date: String) = "day_detail/$date"
        const val ARG = "date"
    }
}
