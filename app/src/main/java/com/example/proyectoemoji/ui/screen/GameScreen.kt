package com.example.proyectoemoji.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectoemoji.data.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    roomId: String,
    viewModel: GameViewModel = viewModel()
) {
    // Estado del juego que llega de Firestore
    val gameState by viewModel.gameState.collectAsState()

    // Emojis visibles (menos el propio)
    val otherPlayers = gameState.players.filter { it.id != viewModel.myPlayerId }
    val emojis = otherPlayers.map { it.assignedEmoji }

    // --------------------------------------------------------------------
    // VARIABLES DE TURNO Y TIEMPO
    // --------------------------------------------------------------------
    var remainingTime by remember { mutableIntStateOf(30) }
    var timerRunning by remember { mutableStateOf(true) }

    val currentTurnPlayerId = gameState.currentTurnPlayerId
    val myPlayerId = viewModel.myPlayerId
    val isMyTurn = currentTurnPlayerId == myPlayerId

    // Cuando cambia el turno → reiniciar temporizador
    LaunchedEffect(currentTurnPlayerId) {
        remainingTime = 30
        timerRunning = true
    }

    // Lógica del temporizador
    LaunchedEffect(timerRunning, remainingTime) {
        if (!timerRunning) return@LaunchedEffect

        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
        } else {
            timerRunning = false
            // Si se acaba el tiempo y es mi turno → eliminado
            if (isMyTurn) {
                viewModel.onPlayerTimeout(roomId, myPlayerId)
            }
        }
    }

    // --------------------------------------------------------------------
    // UI
    // --------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emoji Guess - Sala $roomId") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF101010)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            // --------------------------------------------------------------
            // TEMPORIZADOR
            // --------------------------------------------------------------
            Text(
                text = "Tiempo restante: $remainingTime s",
                style = MaterialTheme.typography.headlineSmall,
                color = if (remainingTime <= 10) Color.Red else Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mi emoji secreto (oculto)
            Text(
                text = "Tu emoji secreto está oculto 👀",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --------------------------------------------------------------
            // GRID DE EMOJIS VISTOS (LOS DE OTROS JUGADORES)
            // --------------------------------------------------------------
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .height(300.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(80.dp)
                            .background(Color.DarkGray)
                            .clickable(
                                enabled = isMyTurn && remainingTime > 0
                            ) {
                                viewModel.selectedEmoji = emoji
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Emoji seleccionado
            Text(
                text = "Elegiste: ${viewModel.selectedEmoji ?: "Ninguno"}",
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --------------------------------------------------------------
            // BOTÓN DE ADIVINAR
            // --------------------------------------------------------------
            Button(
                enabled = isMyTurn && remainingTime > 0 && viewModel.selectedEmoji != null,
                onClick = {
                    val selected = viewModel.selectedEmoji ?: return@Button

                    viewModel.guessEmoji(
                        roomId = roomId,
                        playerId = myPlayerId,
                        selectedEmoji = selected,
                        onResult = { correct ->
                            if (!correct) {
                                // Eliminado
                                viewModel.eliminatePlayer(roomId, myPlayerId)
                            } else {
                                // Pasar turno
                                viewModel.goToNextTurn(roomId)
                            }
                        }
                    )
                }
            ) {
                Text("Adivinar")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --------------------------------------------------------------
            // INFORMACIÓN DE TURNO
            // --------------------------------------------------------------
            Text(
                text = if (isMyTurn) "Tu turno 👈"
                else "Turno de: ${gameState.players.find { it.id == currentTurnPlayerId }?.name}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

