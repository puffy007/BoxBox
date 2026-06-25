package com.boxbox.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.notifications.createNotificationChannels
import com.boxbox.app.ui.home.HomeScreen
import com.boxbox.app.ui.live.LiveScreen
import com.boxbox.app.ui.profile.ProfileScreen
import com.boxbox.app.ui.results.ResultsScreen
import com.boxbox.app.ui.standings.DriverDetailScreen
import com.boxbox.app.ui.standings.StandingsScreen
import com.boxbox.app.ui.standings.TeamDetailScreen
import com.boxbox.app.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Live : Screen("live", "Live", Icons.Default.Flag)
    object Standings : Screen("standings", "Standings", Icons.Default.EmojiEvents)
    object Results : Screen("results", "Results", Icons.Default.FormatListNumbered)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Live, Screen.Standings, Screen.Results, Screen.Profile
)

// Detail routes (not in bottom nav, pushed on top)
const val DRIVER_DETAIL_ROUTE = "driver_detail/{driverId}"
const val TEAM_DETAIL_ROUTE = "team_detail/{teamId}"

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannels(this)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    try {
                        val repo = BoxBoxRepository()
                        val uid = repo.getCurrentUser()?.uid
                        if (uid != null) {
                            val profile = repo.getUserProfile(uid)
                            if (profile != null && profile.favouriteTeam.isNotEmpty()) {
                                ThemeState.favouriteTeam = profile.favouriteTeam
                            }
                        }
                    } catch (e: Exception) { /* stay on default theme */ }
                }
            }

            BoxBoxTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val perm = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                    LaunchedEffect(Unit) { perm.launchPermissionRequest() }
                }
                BoxBoxAppFunction()
            }
        }
    }
}

@Composable
fun BoxBoxAppFunction() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on detail screens for a more native, focused feel
    val showBottomBar = currentRoute == null ||
            bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = AppColors.background,
        bottomBar = {
            if (showBottomBar) BoxBoxBottomNav(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Live.route) { LiveScreen() }
            composable(Screen.Standings.route) {
                StandingsScreen(
                    onDriverClick = { driverId -> navController.navigate("driver_detail/$driverId") },
                    onTeamClick = { teamId -> navController.navigate("team_detail/$teamId") }
                )
            }
            composable(Screen.Results.route) { ResultsScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }

            composable(DRIVER_DETAIL_ROUTE) { backStackEntry ->
                val driverId = backStackEntry.arguments?.getString("driverId") ?: return@composable
                DriverDetailScreen(
                    driverId = driverId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TEAM_DETAIL_ROUTE) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
                TeamDetailScreen(
                    constructorId = teamId,
                    onBack = { navController.popBackStack() },
                    onDriverClick = { driver ->
                        // Navigate using the driver's surname-derived id isn't reliable from OpenF1 data alone,
                        // so for cross-navigation from team -> driver we just pop back; full driver detail
                        // is reached from the Standings list directly.
                    }
                )
            }
        }
    }
}

@Composable
fun BoxBoxBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = AppColors.background,
        tonalElevation = 0.dp,
        modifier = Modifier
            .background(AppColors.background)
            .navigationBarsPadding()
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text(screen.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.primary,
                    selectedTextColor = AppColors.primary,
                    unselectedIconColor = AppColors.onSurfaceVariant,
                    unselectedTextColor = AppColors.onSurfaceVariant,
                    indicatorColor = AppColors.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
