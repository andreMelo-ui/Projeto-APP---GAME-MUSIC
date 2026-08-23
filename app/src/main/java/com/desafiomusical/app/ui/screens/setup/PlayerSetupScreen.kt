package com.desafiomusical.app.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.theme.DangerRed
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSetupScreen(
    container: AppContainer,
    onGameStarted: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PlayerSetupViewModel = viewModel(
        factory = viewModelFactory { initializer { PlayerSetupViewModel(container) } }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Partida") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Quantos jogadores?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    (2..4).forEach { count ->
                        FilterChip(
                            selected = state.playerCount == count,
                            onClick = { viewModel.setPlayerCount(count) },
                            label = { Text("$count") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                Text("Nomes dos jogadores", style = MaterialTheme.typography.titleMedium)
            }

            items(state.playerCount) { index ->
                OutlinedTextField(
                    value = state.playerNames.getOrElse(index) { "" },
                    onValueChange = { viewModel.setPlayerName(index, it) },
                    label = { Text("Jogador ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Rodadas", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        FilterChip(
                            selected = state.roundCount == count,
                            onClick = { viewModel.setRoundCount(count) },
                            label = { Text("$count") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Roubo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Quando o respondente erra, outros podem tentar roubar a resposta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(checked = state.stealEnabled, onCheckedChange = viewModel::setStealEnabled)
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(text = state.errorMessage.orEmpty(), color = DangerRed, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                if (state.isStarting) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    PrimaryButton(
                        text = "Começar Partida",
                        onClick = { viewModel.startGame(onGameStarted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}
