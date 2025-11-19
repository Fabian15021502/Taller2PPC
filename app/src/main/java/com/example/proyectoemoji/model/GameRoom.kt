package com.example.proyectoemoji.models

data class GameRoom(
    val roomId: String = "",
    val currentTurnPlayerId: String = "",
    val round: Int = 1,
    val timeLeft: Int = 30,
    val isGameOver: Boolean = false,
    val winnerId: String? = null
)
