package com.example.swizima.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.swizima.ui.screens.DashboardScreen
import com.example.swizima.ui.screens.LoginScreen
import com.example.swizima.ui.screens.OtpScreen
import com.example.swizima.ui.screens.ServiceFormScreen
import com.example.swizima.ui.screens.ServiceVehiclesListScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{contactInput}") {
        fun createRoute(contactInput: String): String = "otp/${Uri.encode(contactInput)}"
    }
    object Dashboard : Screen("dashboard/{userContact}") {
        fun createRoute(userContact: String): String = "dashboard/${Uri.encode(userContact)}"
    }
    object VehiclesList : Screen("vehiclesList/{serviceType}") {
        fun createRoute(serviceType: String): String = "vehiclesList/${Uri.encode(serviceType)}"
    }
    object ServiceForm : Screen("serviceForm/{serviceTitle}?recordId={recordId}") {
        fun createRoute(serviceTitle: String, recordId: String? = null): String {
            return if (recordId != null) {
                "serviceForm/${Uri.encode(serviceTitle)}?recordId=$recordId"
            } else {
                "serviceForm/${Uri.encode(serviceTitle)}"
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { contactInput ->
                    navController.navigate(Screen.Otp.createRoute(contactInput))
                }
            )
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("contactInput") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val contactInput = backStackEntry.arguments?.getString("contactInput") ?: ""
            OtpScreen(
                contactInput = contactInput,
                onBackToLogin = {
                    navController.popBackStack()
                },
                onOtpValidated = {
                    navController.navigate(Screen.Dashboard.createRoute(contactInput)) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(
                navArgument("userContact") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val userContact = backStackEntry.arguments?.getString("userContact") ?: ""
            DashboardScreen(
                userContact = userContact,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToVehiclesList = { serviceType ->
                    navController.navigate(Screen.VehiclesList.createRoute(serviceType))
                }
            )
        }

        composable(
            route = Screen.VehiclesList.route,
            arguments = listOf(
                navArgument("serviceType") {
                    type = NavType.StringType
                    defaultValue = "Parking"
                }
            )
        ) { backStackEntry ->
            val serviceType = backStackEntry.arguments?.getString("serviceType") ?: "Parking"
            ServiceVehiclesListScreen(
                serviceType = serviceType,
                onBackToDashboard = {
                    navController.popBackStack()
                },
                onNavigateToAddVehicleForm = { selectedService ->
                    navController.navigate(Screen.ServiceForm.createRoute(selectedService))
                },
                onNavigateToEditForm = { serviceTitle, recordId ->
                    navController.navigate(Screen.ServiceForm.createRoute(serviceTitle, recordId))
                }
            )
        }

        composable(
            route = Screen.ServiceForm.route,
            arguments = listOf(
                navArgument("serviceTitle") {
                    type = NavType.StringType
                    defaultValue = "Parking"
                },
                navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val serviceTitle = backStackEntry.arguments?.getString("serviceTitle") ?: "Parking"
            val recordId = backStackEntry.arguments?.getString("recordId")
            ServiceFormScreen(
                initialService = serviceTitle,
                recordIdToEdit = recordId,
                onBack = {
                    navController.popBackStack()
                },
                onSubmitSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
