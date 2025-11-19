package com.example.proyectoemoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.proyectoemoji.navigation.AppNavHost
import com.example.proyectoemoji.ui.theme.ProyectoEmojiTheme   // <-- Importa tu theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProyectoEmojiTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
