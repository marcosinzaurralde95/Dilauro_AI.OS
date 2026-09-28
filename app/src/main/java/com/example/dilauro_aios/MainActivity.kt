package com.example.dilauro_aios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.ui.screens.*
import com.example.dilauro_aios.ui.theme.CardSurface
import com.example.dilauro_aios.ui.theme.DeepBackground
import com.example.dilauro_aios.ui.theme.DilauroAIOSTheme
import com.example.dilauro_aios.ui.theme.NeonCyan
import com.example.dilauro_aios.ui.theme.TextSecondary
import com.example.dilauro_aios.viewmodel.AiosViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AiosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DilauroAIOSTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                // Handle system back button to always return to dashboard if on sub-screen
                if (currentScreen != "dashboard") {
                    BackHandler {
                        currentScreen = "dashboard"
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DeepBackground,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == "dashboard",
                                onClick = { currentScreen = "dashboard" },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Overview", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_overview")
                            )

                            NavigationBarItem(
                                selected = currentScreen == "scheduler",
                                onClick = { currentScreen = "scheduler" },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "D-Kernel") },
                                label = { Text("Kernel", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_kernel")
                            )

                            NavigationBarItem(
                                selected = currentScreen == "synapse",
                                onClick = { currentScreen = "synapse" },
                                icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "SynapseFS") },
                                label = { Text("SynapseFS", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_synapse")
                            )

                            NavigationBarItem(
                                selected = currentScreen == "capsules",
                                onClick = { currentScreen = "capsules" },
                                icon = { Icon(Icons.Default.Apps, contentDescription = "Capsules") },
                                label = { Text("Capsules", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_capsules")
                            )

                            NavigationBarItem(
                                selected = currentScreen == "security",
                                onClick = { currentScreen = "security" },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Neural Shield") },
                                label = { Text("Security", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_security")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { target -> currentScreen = target }
                            )
                            "scheduler" -> SchedulerScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            "memory" -> MemoryScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            "synapse" -> SynapseFsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            "capsules" -> CapsulesScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            "security" -> NeuralShieldScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            "entanglement" -> EntanglementScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                            else -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { target -> currentScreen = target }
                            )
                        }
                    }
                }
            }
        }
    }
}
