package com.example.proyectoemoji.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.proyectoemoji.ui.screens.GameScreen
import com.example.proyectoemoji.ui.screens.LobbyScreen
import com.example.proyectoemoji.ui.screens.WinnerScreen
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.Lobby.route) {

        composable(Destinations.Lobby.route) {
            LobbyScreen(onGameStart = { roomId ->
                // asume que la ViewModel ya puso current player id en algún lugar
                // navega a game (debes construir playerId desde ViewModel)
                val playerId = "" // reemplaza por tu fuente real
                navController.navigate(Destinations.Game.build(roomId, playerId))
            })
        }

        composable(Destinations.Game.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            GameScreen(navController = navController, roomId = roomId, playerId = playerId)
        }

        composable(Destinations.Winner.route) { backStackEntry ->
            val winnerName = backStackEntry.arguments?.getString("winnerName") ?: "Ganador"
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            WinnerScreen(winnerName = winnerName) {
                navController.navigate(Destinations.Lobby.route) {
                    popUpTo(Destinations.Lobby.route)
                }
            }
        }
    }
}

