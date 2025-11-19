package com.example.proyectoemoji.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectoemoji.data.FirebaseRepository
import com.example.proyectoemoji.models.GameRoom
import com.example.proyectoemoji.models.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val repo = FirebaseRepository()

    private val _gameState = MutableStateFlow(GameRoom())
    val gameState: StateFlow<GameRoom> = _gameState

    var remainingSeconds by mutableStateOf(0)

    val emojiList = listOf("😀", "😂", "😍", "🤔", "😎", "😭", "😡", "🤯", "👀", "😴")

    fun startListening(roomId: String) {
        repo.listenRoom(roomId) { room ->
            _gameState.value = room
            startTimer(room.turnEndTime)
        }
    }

    private fun startTimer(endTime: Long) {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = endTime - now
                remainingSeconds = (diff / 1000).toInt()

                if (remainingSeconds <= 0) {
                    forceEliminatePlayer()
                    break
                }
                delay(1000)
            }
        }
    }

    fun guessEmoji(emoji: String) {
        val room = _gameState.value
        val correct = emoji == room.emojiToGuess
        repo.sendGuess(room.roomId, correct)
    }

    private fun forceEliminatePlayer() {
        val room = _gameState.value
        repo.eliminatePlayer(room.roomId, room.currentTurnPlayerId)
    }

    fun sendChatMessage(playerId: String, message: String) {
        val room = _gameState.value
        val player = room.players[playerId] ?: return

        val msg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            playerId = playerId,
            playerName = player.name,
            message = message
        )

        repo.sendChat(room.roomId, msg)
    }
}

