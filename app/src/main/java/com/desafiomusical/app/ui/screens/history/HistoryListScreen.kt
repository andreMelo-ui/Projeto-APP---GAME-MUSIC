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
import androidx.compose.foundation.lazy.LazyRow
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
import com.desafiomusical.app.domain.model.GameHistoryEntry
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.ui.components.ScoreBadge
import com.desafiomusical.app.ui.theme.SuccessGreen
import com.desafiomusical.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Sem `private`: compartilhado com GameDetailScreen.kt, mesmo pacote.
val playedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

fun formatPlayedAt(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(playedAtFormatter)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenGame: (gameId: String) -> Unit,
    onOpenPlayerStats: (playerId: String) -> Unit,
) {
    val viewModel: HistoryListViewModel =
        viewModel(
            factory = viewModelFactory { initializer { HistoryListViewModel(container) } },
        )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            state.entries.isEmpty() -> EmptyHistoryMessage(modifier = Modifier.fillMaxSize().padding(padding))

            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.players.isNotEmpty()) {
                        item {
                            Text(
                                text = "Jogadores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.players, key = { it.id }) { player ->
                                    PlayerChip(player = player, onClick = { onOpenPlayerStats(player.id) })
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Partidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = if (state.players.isNotEmpty()) 8.dp else 0.dp),
                        )
                    }

                    items(state.entries, key = { it.gameId }) { entry ->
                        GameHistoryCard(entry = entry, onClick = { onOpenGame(entry.gameId) })
                    }
                }
        }
    }
}

@Composable
private fun EmptyHistoryMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🎵", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Nenhuma partida ainda",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Jogue uma partida pra ver o histórico aqui.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PlayerChip(
    player: Player,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun GameHistoryCard(
    entry: GameHistoryEntry,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = formatPlayedAt(entry.playedAt), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(text = "${entry.roundCount} rodadas", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Text(
                text = entry.winner?.let { "🏆 ${it.name} venceu" } ?: "Partida sem vencedor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            entry.scoreboard.forEach { playerScore ->
                ScoreBadge(
                    playerName = playerScore.player.name,
                    score = playerScore.totalScore,
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = entry.winner?.id == playerScore.player.id,
                )
            }
        }
    }
}
