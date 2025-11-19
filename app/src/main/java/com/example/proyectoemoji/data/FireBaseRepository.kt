package com.example.proyectoemoji.data

import com.example.proyectoemoji.model.
import com.example.proyectoemoji.models.GameRoom
import com.example.proyectoemoji.models.Player
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // ===============================================================
    // CREATE ROOM
    // ===============================================================
    suspend fun createRoom(roomId: String, creator: Player) {
        val roomRef = db.collection("rooms").document(roomId)

        val room = GameRoom(
            id = roomId,
            players = listOf(creator),
            currentTurnPlayerId = creator.id,
            roundActive = false
        )

        roomRef.set(room).await()
    }

    // ===============================================================
    // JOIN ROOM
    // ===============================================================
    suspend fun joinRoom(roomId: String, player: Player): Boolean {
        val roomRef = db.collection("rooms").document(roomId)

        return try {
            roomRef.update("players", FieldValue.arrayUnion(player)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ===============================================================
    // LISTEN TO ROOM UPDATES (Real-time)
    // ===============================================================
    fun listenRoom(roomId: String, onChange: (GameRoom) -> Unit) {
        db.collection("rooms")
            .document(roomId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val room = snapshot.toObject(GameRoom::class.java)
                    if (room != null) onChange(room)
                }
            }
    }

    // ===============================================================
    // ASSIGN RANDOM EMOJIS
    // ===============================================================
    suspend fun assignEmojis(roomId: String, emojis: List<String>) {
        val roomRef = db.collection("rooms").document(roomId)
        val room = roomRef.get().await().toObject(GameRoom::class.java) ?: return

        val shuffled = emojis.shuffled()
        val updatedPlayers = room.players.mapIndexed { index, p ->
            p.copy(assignedEmoji = shuffled[index])
        }

        roomRef.update("players", updatedPlayers).await()
    }

    // ===============================================================
    // CHECK GUESS (TRUE = correct)
    // ===============================================================
    suspend fun checkGuess(
        roomId: String,
        playerId: String,
        selectedEmoji: String,
        callback: (Boolean) -> Unit
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        val room = roomRef.get().await().toObject(GameRoom::class.java) ?: return

        val player = room.players.find { it.id == playerId } ?: return
        val correct = player.assignedEmoji == selectedEmoji

        callback(correct)
    }

    // ===============================================================
    // ELIMINATE PLAYER
    // ===============================================================
    suspend fun eliminatePlayer(roomId: String, playerId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        val room = roomRef.get().await().toObject(GameRoom::class.java) ?: return

        val remaining = room.players.filter { it.id != playerId }

        // Update players list
        roomRef.update("players", remaining).await()

        // If only one left → game finished
        if (remaining.size == 1) {
            roomRef.update("winnerId", remaining.first().id).await()
        }
    }

    // ===============================================================
    // GO TO NEXT TURN (circular)
    // ===============================================================
    suspend fun goToNextTurn(roomId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        val room = roomRef.get().await().toObject(GameRoom::class.java) ?: return

        val players = room.players
        if (players.size <= 1) return

        val currentIndex = players.indexOfFirst { it.id == room.currentTurnPlayerId }
        val nextIndex = (currentIndex + 1) % players.size

        roomRef.update("currentTurnPlayerId", players[nextIndex].id).await()
    }

    // ===============================================================
    // START NEW ROUND (re-assign emojis)
    // ===============================================================
    suspend fun startNewRound(roomId: String, emojis: List<String>) {
        val roomRef = db.collection("rooms").document(roomId)
        val room = roomRef.get().await().toObject(GameRoom::class.java) ?: return

        val shuffled = emojis.shuffled()
        val updatedPlayers = room.players.mapIndexed { index, p ->
            p.copy(assignedEmoji = shuffled[index])
        }

        roomRef.update(
            mapOf(
                "players" to updatedPlayers,
                "roundActive" to true
            )
        ).await()
    }

    // ===============================================================
    // SEND CHAT MESSAGE
    // ===============================================================
    suspend fun sendChatMessage(roomId: String, message: ChatMessage) {
        val chatRef = db.collection("rooms")
            .document(roomId)
            .collection("chat")
            .document()

        chatRef.set(message).await()
    }

}
