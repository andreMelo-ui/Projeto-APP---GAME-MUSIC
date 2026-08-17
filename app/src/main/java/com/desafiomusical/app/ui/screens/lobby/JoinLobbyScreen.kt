package com.desafiomusical.app.ui.screens.lobby

import android.Manifest
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.network.multiplayer.DiscoveredRoom
import com.desafiomusical.app.network.payloads.RoomPlayerInfo
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.QrScannerView
import com.desafiomusical.app.ui.components.rememberPermissionState
import com.desafiomusical.app.ui.theme.ColorSuccess
import com.desafiomusical.app.ui.theme.NeonRed
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinLobbyScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: JoinLobbyViewModel = viewModel(
        factory = viewModelFactory { initializer { JoinLobbyViewModel(container) } }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrar em Partida") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.leaveRoom(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state.phase) {
            JoinLobbyPhase.NAME_INPUT -> NameInputContent(state, viewModel, contentModifier)
            JoinLobbyPhase.CHOOSING -> ChoosingContent(state, viewModel, contentModifier)
            JoinLobbyPhase.CONNECTING -> CenteredMessage("Conectando...", contentModifier, showSpinner = true)
            JoinLobbyPhase.CONNECTED -> ConnectedContent(state, viewModel, contentModifier)
            JoinLobbyPhase.GAME_STARTED -> CenteredMessage(
                "A partida começou! Acompanhe pelo aparelho do host — a tela de jogo em rede chega numa próxima atualização.",
                contentModifier
            )
        }
    }
}

@Composable
private fun NameInputContent(state: JoinLobbyUiState, viewModel: JoinLobbyViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = state.playerName,
            onValueChange = viewModel::setPlayerName,
            label = { Text("Seu nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = NeonRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        PrimaryButton(text = "Continuar", onClick = viewModel::confirmName, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable
private fun ChoosingContent(state: JoinLobbyUiState, viewModel: JoinLobbyViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rede", "QR Code", "Manual")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> NetworkTabContent(state, viewModel)
                1 -> QrTabContent(viewModel)
                else -> ManualTabContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun NetworkTabContent(state: JoinLobbyUiState, viewModel: JoinLobbyViewModel) {
    val permission = rememberPermissionState(Manifest.permission.NEARBY_WIFI_DEVICES)
    if (!permission.granted) {
        PermissionRequestContent(
            message = "Precisamos da permissão de dispositivos próximos pra buscar salas na rede Wi-Fi.",
            onRequest = permission.request
        )
        return
    }

    DisposableEffect(Unit) {
        viewModel.startDiscovering()
        onDispose { viewModel.stopDiscovering() }
    }

    if (state.discoveredRooms.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text("Procurando salas...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 12.dp))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.discoveredRooms, key = DiscoveredRoom::serviceName) { room ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.joinDiscoveredRoom(room) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sala ${room.roomCode}", style = MaterialTheme.typography.bodyLarge)
                    Text(room.hostAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun QrTabContent(viewModel: JoinLobbyViewModel) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    if (!permission.granted) {
        PermissionRequestContent(message = "Precisamos da câmera pra escanear o QR Code da sala.", onRequest = permission.request)
        return
    }
    QrScannerView(modifier = Modifier.fillMaxSize(), onInviteFound = viewModel::joinScannedInvite)
}

@Composable
private fun ManualTabContent(state: JoinLobbyUiState, viewModel: JoinLobbyViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.manualRoomCode,
            onValueChange = viewModel::setManualRoomCode,
            label = { Text("Código da sala") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.manualHostAddress,
            onValueChange = viewModel::setManualHostAddress,
            label = { Text("IP do host") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.manualPort,
            onValueChange = viewModel::setManualPort,
            label = { Text("Porta") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = NeonRed, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(text = "Conectar", onClick = viewModel::joinManualEntry, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PermissionRequestContent(message: String, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        PrimaryButton(text = "Permitir", onClick = onRequest, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable
private fun ConnectedContent(state: JoinLobbyUiState, viewModel: JoinLobbyViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "${state.roundCount} rodadas · roubo ${if (state.stealEnabled) "ativado" else "desativado"}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text("Jogadores (${state.players.size}/4)", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.players, key = RoomPlayerInfo::playerId) { player ->
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
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Estou pronto", style = MaterialTheme.typography.titleMedium)
            Switch(checked = state.isReady, onCheckedChange = { viewModel.toggleReady() })
        }

        Text(
            "Aguardando o host iniciar a partida...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun CenteredMessage(message: String, modifier: Modifier = Modifier, showSpinner: Boolean = false) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showSpinner) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp).padding(bottom = 16.dp))
        }
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
