package com.desafiomusical.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.SecondaryButton
import com.desafiomusical.app.ui.theme.BrandCyan
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "DESAFIO",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "MUSICAL",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = BrandCyan,
        )
        Text(
            text = "Quem sabe mais de música?",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
        )

        PrimaryButton(text = "Nova Partida", onClick = onNewGame, modifier = Modifier.fillMaxWidth())
        SecondaryButton(
            text = "Criar Sala (Wi-Fi)",
            onClick = onHostGame,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        SecondaryButton(
            text = "Entrar em Partida",
            onClick = onJoinGame,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        SecondaryButton(
            text = "Histórico",
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        SecondaryButton(
            text = "Configurações",
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
