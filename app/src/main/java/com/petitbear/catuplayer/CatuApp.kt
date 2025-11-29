package com.petitbear.catuplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.views.HomeScreen
import com.petitbear.catuplayer.views.Meme
import com.petitbear.catuplayer.views.NowPlayingScreen
import com.petitbear.catuplayer.views.PlaylistScreen
import com.petitbear.catuplayer.views.SearchPage

@Composable
fun CatuApp(viewModel: AudioPlayerViewModel) {
    CatuPlayerTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
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
                composable(Screen.SearchPage.route){
                    SearchPage(navController, viewModel)
                }
                composable(Screen.Playlist.route) {
                    PlaylistScreen(navController, viewModel)
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(navController, viewModel)
                }
                composable(Screen.Mine.route){
                    Meme(navController,viewModel)
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