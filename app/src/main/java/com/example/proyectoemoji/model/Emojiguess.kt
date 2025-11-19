package com.example.proyectoemoji.models

data class EmojiGuess(
    val playerId: String = "",
    val guessedEmoji: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
