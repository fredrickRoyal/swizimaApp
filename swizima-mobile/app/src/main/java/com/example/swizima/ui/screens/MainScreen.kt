package com.example.swizima.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(
    userContact: String,
    onLogout: () -> Unit,
    onNavigateToVehiclesList: (serviceType: String) -> Unit = {},
    onNavigateToAddVehicleForm: (selectedService: String) -> Unit,
    onNavigateToEditForm: (serviceTitle: String, recordId: String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Transactions") },
                    label = { Text("Transactions") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text("Wallet") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    userContact = userContact,
                    onLogout = onLogout,
                    onNavigateToVehiclesList = onNavigateToVehiclesList
                )
                1 -> ServiceVehiclesListScreen(
                    serviceType = "All",
                    onBackToDashboard = { selectedTab = 0 },
                    onNavigateToAddVehicleForm = onNavigateToAddVehicleForm,
                    onNavigateToEditForm = onNavigateToEditForm,
                    isGlobalList = true
                )
                2 -> ReportsScreen()
                3 -> WalletScreen()
            }
        }
    }
}
