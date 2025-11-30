package com.petitbear.catuplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.views.HomeScreen
import com.petitbear.catuplayer.views.Meme
import com.petitbear.catuplayer.views.NowPlayingScreen
import com.petitbear.catuplayer.views.PlaylistScreen
import com.petitbear.catuplayer.views.SearchPage
import com.petitbear.catuplayer.views.settingsView.ClearCachePage

@Composable
fun CatuApp(viewModel: AudioPlayerViewModel) {
    CatuPlayerTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStackEntry?.destination

// 定义哪些页面需要显示底部导航栏
        val shouldShowBottomBar = when (currentDestination?.route) {
            Screen.Home.route -> true
            Screen.SearchPage.route -> true
            Screen.Playlist.route -> true
            Screen.NowPlaying.route -> true
            Screen.Mine.route -> true
            else -> false // 其他页面（如 ClearCache）不显示底部栏
        }


        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar) {
                    BottomNavigationBar(navController,currentRoute)
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController, viewModel)
                }
                composable(Screen.SearchPage.route) {
                    SearchPage(navController, viewModel)
                }
                composable(Screen.Playlist.route) {
                    PlaylistScreen(navController, viewModel)
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(navController, viewModel)
                }
                composable(Screen.Mine.route) {
                    Meme(navController, viewModel)
                }

                    composable(Screen.ClearCache.route) {
                        ClearCachePage()

                }
            }
        }
    }
}

// 底部导航栏组件
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String? // 可能为null
) {
    NavigationBar {
        val navItems = Screen.bottomNavScreens.filterNotNull()

        navItems.forEach { screen ->
            val icon = when (screen) {
                is Screen.Home -> Icons.Default.Home
                is Screen.Playlist -> Icons.AutoMirrored.Filled.List
                is Screen.NowPlaying -> Icons.Default.PlayArrow
                is Screen.Mine -> Icons.Default.Person
                is Screen.SearchPage -> Icons.Default.Search
                else -> Icons.Default.Home
            }

            NavigationBarItem(
                icon = {
                    Icon(icon, contentDescription = screen.title)
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}