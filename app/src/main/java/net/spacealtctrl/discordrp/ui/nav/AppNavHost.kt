package net.spacealtctrl.discordrp.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.math.sign
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.ui.screens.alerts.AlertsScreen
import net.spacealtctrl.discordrp.ui.screens.home.HomeScreen
import net.spacealtctrl.discordrp.ui.screens.login.LoginScreen
import net.spacealtctrl.discordrp.ui.screens.presence.PresenceScreen
import net.spacealtctrl.discordrp.ui.screens.setup.SetupScreen
import net.spacealtctrl.discordrp.ui.screens.you.YouScreen
import net.spacealtctrl.discordrp.ui.theme.Pace

private const val ROUTE_SETUP = "setup"
private const val ROUTE_LOGIN = "login"

private const val MODAL_DISMISS_MS = 200

private fun dockOrdinal(route: String?): Int? =
    Dock.entries.firstOrNull { it.route == route }?.ordinal

private fun dockDir(initial: NavBackStackEntry, target: NavBackStackEntry): Int? {
    val from = dockOrdinal(initial.destination.route) ?: return null
    val to = dockOrdinal(target.destination.route) ?: return null
    return (to - from).sign
}

@Composable
fun AppNavHost(notificationAccess: State<Boolean>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val stash = Stash.of(context)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentDock = Dock.entries.firstOrNull { it.route == currentRoute }

    val startRoute = if (stash.setupComplete && stash.signedIn) Dock.HOME.route else ROUTE_SETUP

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            var lastDock by remember { mutableStateOf(currentDock) }
            if (currentDock != null) lastDock = currentDock
            AnimatedVisibility(
                visible = currentDock != null,
                enter = slideInVertically(Pace.glidespring(IntOffset.VisibilityThreshold)) { it } +
                    fadeIn(tween(Pace.EASY, easing = Pace.settle)),
                exit = slideOutVertically(tween(Pace.QUICK, easing = Pace.dart)) { it } +
                    fadeOut(tween(Pace.QUICK, easing = Pace.dart)),
            ) {
                TabDock(
                    current = lastDock,
                    onPick = { dock ->
                        navController.navigate(dock.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                enterTransition = {
                    val dir = dockDir(initialState, targetState)
                    if (dir != null) {
                        fadeIn(tween(Pace.EASY, easing = Pace.settle)) +
                            slideInHorizontally(tween(Pace.EASY, easing = Pace.glide)) { dir * it / 10 }
                    } else {
                        slideInVertically(Pace.glidespring(IntOffset.VisibilityThreshold)) { it / 5 } +
                            fadeIn(tween(Pace.EASY, easing = Pace.settle))
                    }
                },
                exitTransition = {
                    val dir = dockDir(initialState, targetState)
                    if (dir != null) {
                        fadeOut(tween(Pace.QUICK, easing = Pace.dart)) +
                            slideOutHorizontally(tween(Pace.QUICK, easing = Pace.dart)) { -dir * it / 14 }
                    } else {
                        fadeOut(tween(Pace.QUICK, easing = Pace.dart))
                    }
                },
                popEnterTransition = {
                    val dir = dockDir(initialState, targetState)
                    if (dir != null) {
                        fadeIn(tween(Pace.EASY, easing = Pace.settle)) +
                            slideInHorizontally(tween(Pace.EASY, easing = Pace.glide)) { -dir * it / 10 }
                    } else {
                        fadeIn(tween(Pace.EASY, easing = Pace.settle))
                    }
                },
                popExitTransition = {
                    val dir = dockDir(initialState, targetState)
                    if (dir != null) {
                        fadeOut(tween(Pace.QUICK, easing = Pace.dart)) +
                            slideOutHorizontally(tween(Pace.QUICK, easing = Pace.dart)) { dir * it / 14 }
                    } else {
                        slideOutVertically(tween(MODAL_DISMISS_MS, easing = Pace.dart)) { it / 5 } +
                            fadeOut(tween(MODAL_DISMISS_MS, easing = Pace.dart))
                    }
                },
            ) {
                composable(ROUTE_SETUP) {
                    SetupScreen(
                        notificationAccess = notificationAccess,
                        onSignIn = { navController.navigate(ROUTE_LOGIN) },
                        onFinished = {
                            stash.setupComplete = true
                            navController.navigate(Dock.HOME.route) {
                                popUpTo(ROUTE_SETUP) { inclusive = true }
                            }
                        },
                    )
                }

                composable(ROUTE_LOGIN) {
                    LoginScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Dock.HOME.route) {
                    HomeScreen(notificationAccess = notificationAccess)
                }
                composable(Dock.PRESENCE.route) {
                    PresenceScreen()
                }
                composable(Dock.ALERTS.route) {
                    AlertsScreen()
                }
                composable(Dock.YOU.route) {
                    YouScreen(onSignIn = { navController.navigate(ROUTE_LOGIN) })
                }
            }
        }
    }
}
