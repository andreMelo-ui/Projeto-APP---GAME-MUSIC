package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.state.GameResultView
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.ScoreBadge
import com.desafiomusical.app.ui.theme.NeonGreen
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun GameResultScreen(
    result: GameResultView,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🏆", style = MaterialTheme.typography.displayLarge)
        Text(
            text = result.winner?.let { "${it.name} venceu!" } ?: "Empate!",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonGreen,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Text(text = "Placar final", style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            result.finalScoreboard.forEachIndexed { index, entry ->
                ScoreBadge(
                    playerName = "${index + 1}. ${entry.player.name}",
                    score = entry.totalScore,
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = index == 0
                )
            }
        }

        Text(text = "Estatísticas", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(result.stats, key = { it.player.id }) { stats ->
                Column {
                    Text(text = stats.player.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Acertos: ${stats.correctAnswers} • Erros: ${stats.wrongAnswers} • " +
                            "Média: ${"%.1f".format(stats.averagePoints)} • Melhor rodada: ${stats.bestRoundPoints}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Dicas usadas: ${stats.hintsUsed} • Roubos vencidos: ${stats.stealsWon}/${stats.stealsAttempted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        PrimaryButton(
            text = "Menu Principal",
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}
