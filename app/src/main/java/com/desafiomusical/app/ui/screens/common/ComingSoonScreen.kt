package com.desafiomusical.app.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.theme.TextSecondary

/**
 * Tela honesta para funcionalidades ainda não implementadas nesta fase
 * (multiplayer, histórico, configurações) — evita telas mortas ou crashes.
 */
@Composable
fun ComingSoonScreen(feature: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🚧", style = MaterialTheme.typography.displayLarge)
        Text(
            text = feature,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = "Essa funcionalidade chega em uma próxima fase do Desafio Musical.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
        )
        PrimaryButton(text = "Voltar", onClick = onBack)
    }
}
