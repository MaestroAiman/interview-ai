package com.pfa.interview.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pfa.interview.core.utils.AuthEventBus
import com.pfa.interview.ui.admin.AdminDashboardScreen
import com.pfa.interview.ui.admin.AdminUsersScreen
import com.pfa.interview.ui.auth.LoginScreen
import com.pfa.interview.ui.auth.RegisterScreen
import com.pfa.interview.ui.history.HistoryDetailsScreen
import com.pfa.interview.ui.history.HistoryScreen
import com.pfa.interview.ui.home.HomeScreen
import com.pfa.interview.ui.profile.ProfileScreen
import com.pfa.interview.ui.progress.ProgressScreen
import com.pfa.interview.ui.results.ResultsScreen
import com.pfa.interview.ui.session.SessionScreen
import com.pfa.interview.ui.setup.SetupScreen
import com.pfa.interview.ui.splash.OnboardingScreen
import com.pfa.interview.ui.splash.SplashScreen
import com.pfa.interview.ui.theme.*

private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.Home.route, Icons.Default.Home, "Home"),
    BottomNavItem(Routes.Setup.route, Icons.Default.PlayArrow, "Practice"),
    BottomNavItem(Routes.Progress.route, Icons.Default.BarChart, "Progress"),
    BottomNavItem(Routes.Profile.route, Icons.Default.Person, "Profile"),
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun InterviewNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    LaunchedEffect(Unit) {
        AuthEventBus.unauthorizedEvent.collect {
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = DeepNavy,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = NavyCard, tonalElevation = 0.dp) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Routes.Onboarding.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                    onLoginSuccess = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    onStartInterview = { navController.navigate(Routes.Setup.route) },
                    onSessionClick = { sessionId, isCompleted ->
                        if (isCompleted)
                            navController.navigate(Routes.Results.createRoute(sessionId))
                        else
                            navController.navigate(Routes.Session.createRoute(sessionId))
                    },
                    onSeeAllHistory = { navController.navigate(Routes.History.route) }
                )
            }

            composable(Routes.Setup.route) {
                SetupScreen(
                    onSessionStarted = { sessionId ->
                        navController.navigate(Routes.Session.createRoute(sessionId)) {
                            popUpTo(Routes.Setup.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.Session.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                SessionScreen(
                    sessionId = sessionId,
                    onSessionComplete = { id ->
                        navController.navigate(Routes.Results.createRoute(id)) {
                            popUpTo(Routes.Session.route) { inclusive = true }
                        }
                    },
                    onQuit = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Session.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.Results.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                ResultsScreen(
                    sessionId = sessionId,
                    onPracticeAgain = {
                        navController.navigate(Routes.Setup.route) {
                            popUpTo(Routes.Results.route) { inclusive = true }
                        }
                    },
                    onGoHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.History.route) {
                HistoryScreen(
                    onReviewSession = { sessionId ->
                        navController.navigate(Routes.HistoryDetails.createRoute(sessionId))
                    },
                    onResumeSession = { sessionId ->
                        navController.navigate(Routes.Session.createRoute(sessionId))
                    },
                    onStartInterview = { navController.navigate(Routes.Setup.route) }
                )
            }

            composable(
                route = Routes.HistoryDetails.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) {
                HistoryDetailsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.Progress.route) {
                ProgressScreen()
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    onLoggedOut = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onAdminDashboard = {
                        navController.navigate(Routes.AdminDashboard.route)
                    },
                    onAdminUsers = {
                        navController.navigate(Routes.AdminUsers.route)
                    }
                )
            }

            composable(Routes.AdminDashboard.route) {
                AdminDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onReviewSession = { sessionId ->
                        navController.navigate(Routes.HistoryDetails.createRoute(sessionId))
                    }
                )
            }

            composable(Routes.AdminUsers.route) {
                AdminUsersScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
