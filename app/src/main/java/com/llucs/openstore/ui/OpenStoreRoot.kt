package com.llucs.openstore.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.llucs.openstore.ui.screens.AboutScreen
import com.llucs.openstore.ui.screens.AppDetailsScreen
import com.llucs.openstore.ui.screens.HomeScreen
import com.llucs.openstore.ui.screens.RepositoriesScreen

private sealed class Tab(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Store : Tab("home", "Apps", { Icon(Icons.Outlined.Storefront, contentDescription = null) })
    data object Repos : Tab("repos", "Repos", { Icon(Icons.Outlined.Public, contentDescription = null) })
    data object About : Tab("about", "Sobre", { Icon(Icons.Outlined.Info, contentDescription = null) })
}

@Composable
fun OpenStoreRoot() {
    val navController = rememberNavController()
    val tabs = listOf(Tab.Store, Tab.Repos, Tab.About)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.startsWith("details/") != true

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 8.dp) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Store.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Store.route) { HomeScreen(navController) }
            composable(Tab.Repos.route) { RepositoriesScreen() }
            composable(Tab.About.route) { AboutScreen() }
            composable("details/{packageName}") { backStack ->
                val packageName = backStack.arguments?.getString("packageName").orEmpty()
                AppDetailsScreen(packageName = packageName, navController = navController)
            }
        }
    }
}
