package com.example.proyectoemoji.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectoemoji.viewmodel.GameViewModel
import com.example.proyectoemoji.models.Player

@Composable
fun LobbyScreen(
    viewModel: GameViewModel = viewModel(),
    onGameStart: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var playerName by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf("") }

    // Cuando la sala cambie a "iniciada" -> navegar a GameScreen
    LaunchedEffect(uiState.room?.gameStarted) {
        if (uiState.room?.gameStarted == true) {
            uiState.room?.roomId?.let { onGameStart(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Lobby Emoji Guess",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // =============================
        // 1. CREAR SALA
        // =============================
        Text("Crear sala", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (playerName.isNotEmpty()) {
                    viewModel.createRoom(playerName)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear sala")
        }

        Spacer(modifier = Modifier.height(30.dp))

        // =============================
        // 2. UNIRSE A SALA
        // =============================
        Text("Unirse a sala", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = roomCodeInput,
            onValueChange = { roomCodeInput = it },
            label = { Text("Código de sala") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (playerName.isNotEmpty() && roomCodeInput.isNotEmpty()) {
                    viewModel.joinRoom(playerName, roomCodeInput)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unirse")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // =============================
        // 3. SI YA ESTAMOS EN UNA SALA
        // =============================
        uiState.room?.let { room ->
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Código de sala:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                room.roomId,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EE)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Jugadores:",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(room.players) { player ->
                    PlayerItem(player)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Solo el host puede iniciar
            if (uiState.currentPlayerId == room.hostId) {
                Button(
                    onClick = { viewModel.startGame(room.roomId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar partida")
                }
            }
        }
    }
}

@Composable
fun PlayerItem(player: Player) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .background(Color(0xFFEDE7F6))
            .padding(12.dp)
    ) {
        Text(
            text = player.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
