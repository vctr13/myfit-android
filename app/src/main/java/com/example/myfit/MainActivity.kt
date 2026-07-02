package com.example.myfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myfit.ui.main.MainScreen
import com.example.myfit.ui.nav.Screen
import com.example.myfit.ui.onboarding.OnboardingScreen
import com.example.myfit.ui.theme.MyFITTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyFitApp
        val startDestination = if (app.securePrefs.hasApiKey) Screen.Main.route
                               else Screen.Onboarding.route

        setContent {
            MyFITTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onFinished = {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Main.route) {
                        MainScreen()
                    }
                }
            }
        }
    }
}
