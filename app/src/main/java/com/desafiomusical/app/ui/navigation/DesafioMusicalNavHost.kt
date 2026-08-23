package com.desafiomusical.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.ui.screens.common.ComingSoonScreen
import com.desafiomusical.app.ui.screens.game.GameHostScreen
import com.desafiomusical.app.ui.screens.history.HistoryListScreen
import com.desafiomusical.app.ui.screens.home.HomeScreen
import com.desafiomusical.app.ui.screens.lobby.HostLobbyScreen
import com.desafiomusical.app.ui.screens.lobby.JoinLobbyScreen
import com.desafiomusical.app.ui.screens.setup.PlayerSetupScreen

@Composable
fun DesafioMusicalNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                onNewGame = { navController.navigate(Routes.PlayerSetup.route) },
                onHostGame = { navController.navigate(Routes.HostLobby.route) },
                onJoinGame = { navController.navigate(Routes.JoinLobby.route) },
                onHistory = { navController.navigate(Routes.History.route) },
                onSettings = { navController.navigate(Routes.ComingSoon.build("Configurações")) }
            )
        }
        composable(Routes.HostLobby.route) {
            HostLobbyScreen(
                container = container,
                onGameStarted = {
                    navController.navigate(Routes.Game.route) {
                        popUpTo(Routes.Home.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.JoinLobby.route) {
            JoinLobbyScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PlayerSetup.route) {
            PlayerSetupScreen(
                container = container,
                onGameStarted = {
                    navController.navigate(Routes.Game.route) {
                        popUpTo(Routes.Home.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Game.route) {
            GameHostScreen(
                container = container,
                onExitToHome = {
                    navController.popBackStack(Routes.Home.route, inclusive = false)
                }
            )
        }
        composable(Routes.History.route) {
            HistoryListScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenGame = { /* passo 3: navegar pro detalhe da partida */ }
            )
        }
        composable(
            route = Routes.ComingSoon.route,
            arguments = listOf(navArgument("feature") { type = NavType.StringType })
        ) { backStackEntry ->
            ComingSoonScreen(
                feature = backStackEntry.arguments?.getString("feature").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
