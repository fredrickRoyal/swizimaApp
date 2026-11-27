package com.swizima.stafftracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swizima.stafftracker.data.AuthRepository
import com.swizima.stafftracker.data.StaffProfile
import com.swizima.stafftracker.ui.screens.LoginScreen
import com.swizima.stafftracker.ui.screens.MainScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var activeProfile by remember { mutableStateOf(AuthRepository.getSavedSession(context)) }
    val startDestination = remember {
        if (activeProfile != null) Screen.Main.route else Screen.Login.route
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { profile ->
                    AuthRepository.saveSession(context, profile)
                    activeProfile = profile
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            val profile = activeProfile ?: StaffProfile("st_default", "Staff Member", "+256700408387", "Field Staff", "Swizima Motors")
            MainScreen(
                profile = profile,
                onLogout = {
                    AuthRepository.clearSession(context)
                    activeProfile = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
