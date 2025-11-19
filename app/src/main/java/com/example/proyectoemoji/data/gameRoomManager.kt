package com.example.proyectoemoji.data

import com.example.proyectoemoji.models.GameRoom
import com.example.proyectoemoji.models.Player
import com.example.proyectoemoji.models.EmojiGuess
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GameRoomManager(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val emojisPool = listOf(
        "😀","😎","🤡","🐱","🐶","🦊","🐸","🐵",
        "🔥","❄️","⚡","🌈","🍕","🍔","🍩","🌮"
    )

    private fun roomRef(roomId: String) = db.collection("rooms").document(roomId)
    private fun playersRef(roomId: String) = roomRef(roomId).collection("players")

    /**
     * Asigna emojis aleatorios a los jugadores activos.
     * Si hay menos emojis que jugadores, se repiten de forma ciclica.
     */
    suspend fun assignEmojis(roomId: String) {
        val playersSnapshot = playersRef(roomId).get().await()
        val players = playersSnapshot.toObjects(Player::class.java)

        if (players.isEmpty()) return

        // mezclar emojis y asignar en orden
        val shuffled = emojisPool.shuffled()
        val assignments = players.mapIndexed { idx, p ->
            val emoji = shuffled.getOrElse(idx) { shuffled[idx % shuffled.size] }
            p.copy(emoji = emoji, isEliminated = false)
        }

        // actualizar cada jugador
        assignments.forEach { p ->
            playersRef(roomId).document(p.id).set(p).await()
        }

        // resetear estado de ronda en el documento room
        roomRef(roomId).update(
            mapOf(
                "round" to FieldValue.increment(1),
                "isGameStarted" to true,
                "turnEndTime" to System.currentTimeMillis() + 30_000L // 30s por defecto
            )
        ).await()

        // fijar primer turno al primer jugador asignado
        val firstId = assignments.first().id
        roomRef(roomId).update("currentTurnPlayerId", firstId).await()
    }

    /**
     * Devuelve lista de jugadores activos (no eliminados)
     */
    suspend fun getActivePlayers(roomId: String): List<Player> {
        val snaps = playersRef(roomId).get().await()
        return snaps.toObjects(Player::class.java).filter { !it.isEliminated }
    }

    /**
     * Evalúa un intento (guessedEmoji) para el jugador.
     * Retorna true si acertó, false si falló.
     * Si falla, lo marca eliminado y avanza turno.
     */
    suspend fun evaluateGuess(roomId: String, guess: EmojiGuess): Boolean {
        val playerDoc = playersRef(roomId).document(guess.playerId).get().await()
        val player = playerDoc.toObject(Player::class.java) ?: return false

        val correct = player.emoji == guess.guessedEmoji

        if (!correct) {
            // marcar eliminado
            val updated = player.copy(isEliminated = true)
            playersRef(roomId).document(player.id).set(updated).await()
        }

        // Chequear ganador y avanzar turno
        checkAndResolveAfterTurn(roomId)
        return correct
    }

    /**
     * Si queda un solo jugador activo, declara ganador en el documento room.
     * Si quedan >1, reasigna siguiente turno y actualiza turnEndTime.
     */
    private suspend fun checkAndResolveAfterTurn(roomId: String) {
        val active = getActivePlayers(roomId)
        if (active.size == 1) {
            // declarar ganador
            roomRef(roomId).update(
                mapOf(
                    "isGameStarted" to false,
                    "isGameOver" to true,
                    "winnerId" to active.first().id
                )
            ).await()
            return
        }

        // si siguen varios jugadores → pasar turno al siguiente jugador activo
        val roomSnap = roomRef(roomId).get().await()
        val room = roomSnap.toObject(GameRoom::class.java) ?: return

        // busca índice del player actual en la lista completa de jugadores
        val allPlayersSnap = playersRef(roomId).get().await()
        val allPlayers = allPlayersSnap.toObjects(Player::class.java)
        if (allPlayers.isEmpty()) return

        val currentIndex = allPlayers.indexOfFirst { it.id == room.currentTurnPlayerId }
        // buscar el siguiente activo
        var nextIndex = (currentIndex + 1) % allPlayers.size
        var attempts = 0
        while (attempts < allPlayers.size && allPlayers[nextIndex].isEliminated) {
            nextIndex = (nextIndex + 1) % allPlayers.size
            attempts++
        }
        val nextId = allPlayers[nextIndex].id

        // actualizar turno y ajustar timer
        roomRef(roomId).update(
            mapOf(
                "currentTurnPlayerId" to nextId,
                "turnEndTime" to System.currentTimeMillis() + 30_000L // 30s para el siguiente turno
            )
        ).await()

        // reasignar emojis si quieres al final de la ronda (opcional)
    }

    /**
     * Marca al jugador como eliminado por timeout y resuelve estado.
     */
    suspend fun eliminateByTimeout(roomId: String, playerId: String) {
        val playerDoc = playersRef(roomId).document(playerId).get().await()
        val player = playerDoc.toObject(Player::class.java) ?: return
        val updated = player.copy(isEliminated = true)
        playersRef(roomId).document(playerId).set(updated).await()
        checkAndResolveAfterTurn(roomId)
    }

    /**
     * Fuerza pasar al siguiente turno (util para host o Cloud Function)
     */
    suspend fun forceNextTurn(roomId: String) {
        // reutiliza la lógica de resolución después de un turno
        checkAndResolveAfterTurn(roomId)
    }
}
