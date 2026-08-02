package com.example.zaloauto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zaloauto.ui.navigation.HomeRoute
import com.example.zaloauto.ui.navigation.ListRoute
import com.example.zaloauto.ui.navigation.NavGraph
import com.example.zaloauto.ui.navigation.SettingsRoute
import com.example.zaloauto.ui.navigation.TemplatesRoute
import com.example.zaloauto.ui.theme.ZaloAutoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZaloAutoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val navItems = listOf(
                    NavItem("Home", Icons.Outlined.Home, HomeRoute),
                    NavItem("History", Icons.Outlined.DateRange, ListRoute),
                    NavItem("Templates", Icons.Outlined.TextSnippet, TemplatesRoute),
                    NavItem("Settings", Icons.Outlined.Settings, SettingsRoute)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            navItems.forEach { item ->
                                val selected = when (item.route) {
                                    is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    is ListRoute -> currentDestination?.hasRoute<ListRoute>() == true
                                    is TemplatesRoute -> currentDestination?.hasRoute<TemplatesRoute>() == true
                                    is SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            navController.navigate(item.route) {
                                                popUpTo(HomeRoute) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)
