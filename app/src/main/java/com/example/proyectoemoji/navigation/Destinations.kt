package com.example.proyectoemoji.navigation

sealed class Destinations(val route: String) {
    object Lobby : Destinations("lobby")
    object Game : Destinations("game/{roomId}/{playerId}") {
        fun build(roomId: String, playerId: String) = "game/$roomId/$playerId"
    }
    object Winner : Destinations("winner/{roomId}/{winnerName}") {
        fun build(roomId: String, winnerName: String) = "winner/$roomId/$winnerName"
    }
}

