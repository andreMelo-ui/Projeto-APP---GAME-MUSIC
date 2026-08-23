package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.state.RoundResultView
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.ScoreBadge
import com.desafiomusical.app.ui.theme.SuccessGreen
import com.desafiomusical.app.ui.theme.DangerRed
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun RoundResultScreen(
    result: RoundResultView,
    onNextRound: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (result.winner != null) "${result.winner.name} acertou!" else "Ninguém acertou",
            style = MaterialTheme.typography.headlineMedium,
            color = if (result.winner != null) SuccessGreen else DangerRed,
            fontWeight = FontWeight.Black
        )
        if (result.winner != null) {
            Text(
                text = "+${result.pointsAwarded} pontos em ${result.elapsedSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            text = result.song.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = result.song.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        val work = result.song.work
        if (!work.isNullOrBlank()) {
            Text(
                text = work,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Text(
            text = "Placar",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(result.scoreboard, key = { it.player.id }) { entry ->
                ScoreBadge(
                    playerName = entry.player.name,
                    score = entry.totalScore,
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = result.winner?.id == entry.player.id
                )
            }
        }

        PrimaryButton(
            text = if (result.roundNumber >= result.totalRounds) "Ver Resultado Final" else "Próxima Rodada",
            onClick = onNextRound,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}
