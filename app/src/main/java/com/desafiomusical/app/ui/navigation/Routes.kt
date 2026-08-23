package com.desafiomusical.app.ui.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object PlayerSetup : Routes("player_setup")
    data object Game : Routes("game")
    data object HostLobby : Routes("host_lobby")
    data object JoinLobby : Routes("join_lobby")
    data object History : Routes("history")
    data object GameDetail : Routes("game_detail/{gameId}") {
        fun build(gameId: String) = "game_detail/$gameId"
    }
    data object ComingSoon : Routes("coming_soon/{feature}") {
        fun build(feature: String) = "coming_soon/$feature"
    }
}
