package com.desafiomusical.app.ui.screens.lobby

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
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
import com.desafiomusical.app.network.payloads.RoomPlayerInfo
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.QrCodeImage
import com.desafiomusical.app.ui.theme.ColorChooser
import com.desafiomusical.app.ui.theme.ColorSuccess
import com.desafiomusical.app.ui.theme.DangerRed
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreen(
    container: AppContainer,
    onGameStarted: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: HostLobbyViewModel = viewModel(
        factory = viewModelFactory { initializer { HostLobbyViewModel(container) } }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Sala") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when (state.phase) {
            HostLobbyPhase.SETUP, HostLobbyPhase.CREATING ->
                HostSetupContent(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            HostLobbyPhase.WAITING, HostLobbyPhase.STARTING ->
                HostWaitingContent(state = state, viewModel = viewModel, onGameStarted = onGameStarted, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun HostSetupContent(state: HostLobbyUiState, viewModel: HostLobbyViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.hostName,
                onValueChange = viewModel::setHostName,
                label = { Text("Seu nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
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
            item { Text(text = state.errorMessage, color = DangerRed, style = MaterialTheme.typography.bodyMedium) }
        }

        item {
            if (state.phase == HostLobbyPhase.CREATING) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                PrimaryButton(
                    text = "Criar Sala",
                    onClick = viewModel::createRoom,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun HostWaitingContent(
    state: HostLobbyUiState,
    viewModel: HostLobbyViewModel,
    onGameStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Text("Código da sala", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                Text(state.roomCode, style = MaterialTheme.typography.displayMedium, color = ColorChooser)
            }
        }

        item {
            QrCodeImage(
                payload = state.qrPayload,
                contentDescription = "QR Code para entrar na sala ${state.roomCode}"
            )
        }

        item {
            Text(
                "Peça pros outros escanearem o QR, digitarem o código na busca automática, ou entrarem manualmente com IP e porta.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            Text(
                "Jogadores (${state.players.size}/4)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        items(state.players, key = RoomPlayerInfo::playerId) { player ->
            PlayerRosterRow(player)
        }

        if (state.errorMessage != null) {
            item { Text(text = state.errorMessage, color = DangerRed, style = MaterialTheme.typography.bodyMedium) }
        }

        item {
            if (state.phase == HostLobbyPhase.STARTING) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp).padding(vertical = 16.dp))
            } else {
                PrimaryButton(
                    text = "Iniciar Partida",
                    onClick = { viewModel.startGame(onGameStarted) },
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerRosterRow(player: RoomPlayerInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(player.playerName, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = if (player.ready) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
            contentDescription = if (player.ready) "Pronto" else "Aguardando",
            tint = if (player.ready) ColorSuccess else TextSecondary
        )
    }
}
