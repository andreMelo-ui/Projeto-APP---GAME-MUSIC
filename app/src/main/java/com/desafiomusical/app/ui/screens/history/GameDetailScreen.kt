package com.desafiomusical.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameHistoryDetail
import com.desafiomusical.app.domain.model.GameHistoryRound
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.ScoreBadge
import com.desafiomusical.app.ui.theme.DangerRed
import com.desafiomusical.app.ui.theme.SuccessGreen
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    container: AppContainer,
    gameId: String,
    onBack: () -> Unit
) {
    val viewModel: GameDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { GameDetailViewModel(container, gameId) } }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhe da Partida") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        val detail = state.detail
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            detail == null -> GameNotFoundMessage(onBack = onBack, modifier = Modifier.fillMaxSize().padding(padding))

            else -> GameDetailContent(detail = detail, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun GameNotFoundMessage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔍", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Partida não encontrada",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Ela pode ter sido removida do histórico.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        PrimaryButton(text = "Voltar", onClick = onBack)
    }
}

@Composable
private fun GameDetailContent(detail: GameHistoryDetail, modifier: Modifier = Modifier) {
    val entry = detail.entry
    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatPlayedAt(entry.playedAt), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(text = "${entry.roundCount} rodadas", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Text(
                    text = entry.winner?.let { "🏆 ${it.name} venceu" } ?: "Partida sem vencedor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                entry.scoreboard.forEach { playerScore ->
                    ScoreBadge(
                        playerName = playerScore.player.name,
                        score = playerScore.totalScore,
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = entry.winner?.id == playerScore.player.id
                    )
                }
            }
        }

        item {
            Text(
                text = "Rodadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        items(detail.rounds, key = { it.roundNumber }) { round ->
            RoundDetailCard(round = round)
        }
    }
}

@Composable
private fun RoundDetailCard(round: GameHistoryRound) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Rodada ${round.roundNumber}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Text(text = round.song.category.displayName, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            Text(text = round.song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = round.song.artist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = round.winner?.let { "${it.name} acertou" } ?: "Ninguém acertou",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (round.winner != null) SuccessGreen else DangerRed
                )
                if (round.winner != null) {
                    Text(
                        text = "+${round.pointsAwarded} pts em ${round.elapsedSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
