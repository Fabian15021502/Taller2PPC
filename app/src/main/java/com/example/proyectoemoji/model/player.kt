package com.example.emojiguess.model

data class Player(
    val id: String = "",
    val name: String = "",
    val emoji: String = "",
    val isEliminated: Boolean = false,
    val isTurn: Boolean = false
)
