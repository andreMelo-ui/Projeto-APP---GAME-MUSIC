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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.CategoryStats
import com.desafiomusical.app.domain.model.PlayerAggregateStats
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    container: AppContainer,
    playerId: String,
    onBack: () -> Unit,
) {
    val viewModel: PlayerStatsViewModel =
        viewModel(
            factory = viewModelFactory { initializer { PlayerStatsViewModel(container, playerId) } },
        )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playerName.ifBlank { "Estatísticas" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        val stats = state.stats
        if (stats == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            PlayerStatsContent(stats = stats, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun PlayerStatsContent(
    stats: PlayerAggregateStats,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatRow("Partidas jogadas", "${stats.gamesPlayed}")
                    StatRow("Vitórias", "${stats.wins}")
                    StatRow("Derrotas", "${stats.losses}")
                    StatRow("Taxa de vitória", "%.0f%%".format(stats.winRate * 100))
                    StatRow("Músicas respondidas", "${stats.songsAnswered}")
                    StatRow("Acertos", "${stats.correctAnswers}")
                    StatRow("Média de pontos", "%.1f".format(stats.averagePoints))
                    StatRow("Melhor pontuação", "${stats.bestScore}")
                    StatRow("Melhor tempo", stats.bestTimeSeconds?.let { "${it}s" } ?: "—")
                    StatRow("Dicas usadas", "${stats.hintsUsed}")
                    StatRow("Roubos vencidos", "${stats.stealsWon}/${stats.stealsAttempted}")
                }
            }
        }

        item {
            Text(
                text = "Por categoria",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(stats.categoryBreakdown, key = { it.category }) { categoryStats ->
            CategoryStatsRow(categoryStats)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryStatsRow(categoryStats: CategoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = categoryStats.category.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${categoryStats.correctAnswers}/${categoryStats.songsAnswered} acertos",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
