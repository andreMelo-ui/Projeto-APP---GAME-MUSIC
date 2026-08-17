package com.desafiomusical.app.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.network.multiplayer.DiscoveredRoom
import com.desafiomusical.app.network.multiplayer.KtorClientRoomSession
import com.desafiomusical.app.network.multiplayer.NsdRoomDiscoverer
import com.desafiomusical.app.network.multiplayer.RoomInvite
import com.desafiomusical.app.network.multiplayer.RoomSession
import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.RoomPlayerInfo
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JoinLobbyPhase { NAME_INPUT, CHOOSING, CONNECTING, CONNECTED, GAME_STARTED }

data class JoinLobbyUiState(
    val phase: JoinLobbyPhase = JoinLobbyPhase.NAME_INPUT,
    val playerName: String = "",
    val discoveredRooms: List<DiscoveredRoom> = emptyList(),
    val manualRoomCode: String = "",
    val manualHostAddress: String = "",
    val manualPort: String = "",
    val roundCount: Int = 0,
    val stealEnabled: Boolean = true,
    val players: List<RoomPlayerInfo> = emptyList(),
    val isReady: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Lado de quem entra numa sala: descoberta NSD, QR Code ou IP:porta manual —
 * os três convergem no mesmo [join], que sobe um [KtorClientRoomSession]. Ao
 * contrário do host, aqui não existe [com.desafiomusical.app.network.multiplayer.HostGameCoordinator]
 * nem autoridade nenhuma: só encaminha o que a UI pede e reflete o que chega
 * do host (roster do lobby, início de partida).
 */
class JoinLobbyViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinLobbyUiState())
    val uiState: StateFlow<JoinLobbyUiState> = _uiState.asStateFlow()

    private val playerId = UUID.randomUUID().toString()
    private val discoverer = NsdRoomDiscoverer(container.nsdManager)
    private var roomSession: KtorClientRoomSession? = null
    private var discoveryJob: Job? = null

    fun setPlayerName(name: String) = _uiState.update { it.copy(playerName = name, errorMessage = null) }

    fun setManualRoomCode(value: String) = _uiState.update { it.copy(manualRoomCode = value.uppercase(), errorMessage = null) }

    fun setManualHostAddress(value: String) = _uiState.update { it.copy(manualHostAddress = value, errorMessage = null) }

    fun setManualPort(value: String) = _uiState.update { it.copy(manualPort = value.filter(Char::isDigit), errorMessage = null) }

    /** Nome confirmado — avança pra escolha de como entrar (rede automática, QR ou manual). Não inicia nada de rede sozinho. */
    fun confirmName() {
        if (!ensureNameFilled()) return
        _uiState.update { it.copy(phase = JoinLobbyPhase.CHOOSING) }
    }

    /** Chamado quando a aba de busca automática entra em composição — para de buscar sozinho ao sair dela (ver [stopDiscovering]). */
    fun startDiscovering() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = viewModelScope.launch {
            discoverer.discoverRooms().collect { rooms ->
                _uiState.update { it.copy(discoveredRooms = rooms) }
            }
        }
    }

    fun stopDiscovering() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    fun joinDiscoveredRoom(room: DiscoveredRoom) = join(room.roomCode, room.hostAddress, room.port)

    fun joinScannedInvite(invite: RoomInvite) = join(invite.roomCode, invite.hostAddress, invite.port)

    fun joinManualEntry() {
        val state = _uiState.value
        val port = state.manualPort.toIntOrNull()
        if (state.manualRoomCode.isBlank() || state.manualHostAddress.isBlank() || port == null) {
            _uiState.update { it.copy(errorMessage = "Preencha código da sala, IP e porta corretamente.") }
            return
        }
        join(state.manualRoomCode.trim(), state.manualHostAddress.trim(), port)
    }

    private fun ensureNameFilled(): Boolean {
        if (_uiState.value.playerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Digite seu nome primeiro.") }
            return false
        }
        return true
    }

    private fun join(roomCode: String, hostAddress: String, port: Int) {
        discoveryJob?.cancel()
        _uiState.update { it.copy(phase = JoinLobbyPhase.CONNECTING, errorMessage = null) }

        viewModelScope.launch {
            val name = _uiState.value.playerName.trim()
            val session = KtorClientRoomSession(
                roomCode = roomCode,
                playerId = playerId,
                playerName = name,
                hostAddress = hostAddress,
                hostPort = port
            )
            val connected = runCatching { session.start() }.isSuccess
            if (!connected) {
                _uiState.update {
                    it.copy(phase = JoinLobbyPhase.NAME_INPUT, errorMessage = "Não foi possível conectar. Confira o código, o IP e a porta.")
                }
                return@launch
            }

            roomSession = session
            observeIncoming(session)
            _uiState.update { it.copy(phase = JoinLobbyPhase.CONNECTED) }
        }
    }

    private fun observeIncoming(session: RoomSession) {
        viewModelScope.launch {
            session.observeIncoming().collect { event ->
                when (event) {
                    is GameEvent.RoomRoster -> _uiState.update {
                        it.copy(players = event.players, roundCount = event.roundCount, stealEnabled = event.stealEnabled)
                    }
                    is GameEvent.GameStart -> _uiState.update { it.copy(phase = JoinLobbyPhase.GAME_STARTED) }
                    else -> Unit
                }
            }
        }
    }

    fun toggleReady() {
        val session = roomSession ?: return
        val newReady = !_uiState.value.isReady
        _uiState.update { it.copy(isReady = newReady) }
        viewModelScope.launch {
            session.broadcast(GameEvent.PlayerReady(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, newReady))
        }
    }

    /** Sai da sala (voltar da tela) — distinto de [onCleared], que também cobre a saída pelo botão de voltar do sistema. */
    fun leaveRoom() {
        discoveryJob?.cancel()
        val session = roomSession
        roomSession = null
        if (session != null) viewModelScope.launch { runCatching { session.stop() } }
        _uiState.value = JoinLobbyUiState(playerName = _uiState.value.playerName)
    }

    override fun onCleared() {
        super.onCleared()
        discoveryJob?.cancel()
        roomSession?.let { session ->
            CoroutineScope(Dispatchers.IO).launch { runCatching { session.stop() } }
        }
        roomSession = null
    }
}
