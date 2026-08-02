package com.example.zaloauto.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.zaloauto.ui.screens.detail.DetailScreen
import com.example.zaloauto.ui.screens.home.HomeScreen
import com.example.zaloauto.ui.screens.list.ListScreen
import com.example.zaloauto.ui.screens.settings.SettingsScreen
import com.example.zaloauto.ui.screens.templates.TemplatesScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> { HomeScreen(navController) }
        composable<ListRoute> { ListScreen(navController) }
        composable<TemplatesRoute> { TemplatesScreen(navController) }
        composable<SettingsRoute> { SettingsScreen(navController) }
        composable<DetailRoute> { backStackEntry ->
            val route: DetailRoute = backStackEntry.toRoute()
            DetailScreen(navController, route.messageId)
        }
    }
}
