package com.desafiomusical.app.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.network.multiplayer.HostGameCoordinator
import com.desafiomusical.app.network.multiplayer.KtorHostRoomSession
import com.desafiomusical.app.network.multiplayer.NsdRoomAdvertiser
import com.desafiomusical.app.network.multiplayer.RoomInvite
import com.desafiomusical.app.network.multiplayer.RoomSession
import com.desafiomusical.app.network.multiplayer.findLocalIpv4Address
import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.RoomPlayerInfo
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HostLobbyPhase { SETUP, CREATING, WAITING, STARTING }

data class HostLobbyUiState(
    val phase: HostLobbyPhase = HostLobbyPhase.SETUP,
    val hostName: String = "",
    val roundCount: Int = 10,
    val stealEnabled: Boolean = true,
    val roomCode: String = "",
    val qrPayload: String = "",
    val players: List<RoomPlayerInfo> = emptyList(),
    val errorMessage: String? = null
) {
    val canStart: Boolean get() = phase == HostLobbyPhase.WAITING && players.size in 2..4
}

/**
 * Dono da sala do lado do host: sobe o [KtorHostRoomSession], anuncia via
 * [NsdRoomAdvertiser], monta o convite (QR/manual) e mantém o roster do lobby
 * — [HostGameCoordinator] só entra em cena quando a partida realmente começa
 * ([startGame]), pois seu trabalho é sobre rodadas, não sobre quem está entrando
 * na sala. Usa [AppContainer.applicationScope] (não `viewModelScope`) pro
 * coordinator: a navegação para a tela de jogo troca de tela ANTES da partida
 * acabar, e esta ViewModel é limpa nesse momento — se o coordinator dependesse
 * de `viewModelScope`, seus coletores em segundo plano morreriam junto.
 */
class HostLobbyViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HostLobbyUiState())
    val uiState: StateFlow<HostLobbyUiState> = _uiState.asStateFlow()

    private val advertiser = NsdRoomAdvertiser(container.nsdManager)
    private var roomSession: KtorHostRoomSession? = null
    private var gameStarted = false

    fun setHostName(name: String) = _uiState.update { it.copy(hostName = name, errorMessage = null) }

    fun setRoundCount(count: Int) = _uiState.update { it.copy(roundCount = count) }

    fun setStealEnabled(enabled: Boolean) = _uiState.update { it.copy(stealEnabled = enabled) }

    fun createRoom() {
        val name = _uiState.value.hostName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Digite seu nome.") }
            return
        }
        _uiState.update { it.copy(phase = HostLobbyPhase.CREATING, errorMessage = null) }

        viewModelScope.launch {
            val localAddress = findLocalIpv4Address()
            if (localAddress == null) {
                _uiState.update {
                    it.copy(phase = HostLobbyPhase.SETUP, errorMessage = "Não foi possível detectar a rede Wi-Fi. Verifique se está conectado.")
                }
                return@launch
            }

            val code = generateRoomCode()
            val session = KtorHostRoomSession(roomCode = code)
            session.start()
            roomSession = session
            runCatching { advertiser.advertise(code, session.port) }

            // Reaproveita o id de quem já jogou antes (por nome) — ver KDoc de
            // PlayerRepository.findOrCreatePlayer.
            val hostPlayer = container.playerRepository.findOrCreatePlayer(name)
            val self = RoomPlayerInfo(hostPlayer.id, hostPlayer.name, ready = true)
            val invite = RoomInvite(roomCode = code, hostAddress = localAddress, port = session.port)

            _uiState.update {
                it.copy(
                    phase = HostLobbyPhase.WAITING,
                    roomCode = code,
                    qrPayload = invite.toQrPayload(),
                    players = listOf(self)
                )
            }

            observeIncoming(session)
        }
    }

    private fun observeIncoming(session: RoomSession) {
        viewModelScope.launch {
            session.observeIncoming().collect { event ->
                when (event) {
                    is GameEvent.JoinRoom -> {
                        addOrUpdatePlayer(RoomPlayerInfo(event.playerId, event.playerName, ready = false))
                        broadcastRoster(session)
                    }
                    is GameEvent.PlayerReady -> {
                        updateReady(event.playerId, event.ready)
                        broadcastRoster(session)
                    }
                    else -> Unit
                }
            }
        }
    }

    /** Sala comporta no máximo 4 jogadores ([GameConfig]) — quem chegar depois disso fica conectado, mas fora do roster/da partida. */
    private fun addOrUpdatePlayer(info: RoomPlayerInfo) {
        _uiState.update { state ->
            val alreadyIn = state.players.any { it.playerId == info.playerId }
            if (!alreadyIn && state.players.size >= 4) return@update state
            state.copy(players = state.players.filterNot { it.playerId == info.playerId } + info)
        }
    }

    private fun updateReady(playerId: String, ready: Boolean) {
        _uiState.update { state ->
            state.copy(players = state.players.map { if (it.playerId == playerId) it.copy(ready = ready) else it })
        }
    }

    private suspend fun broadcastRoster(session: RoomSession) {
        val state = _uiState.value
        session.broadcast(
            GameEvent.RoomRoster(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                roundCount = state.roundCount,
                stealEnabled = state.stealEnabled,
                players = state.players
            )
        )
    }

    fun startGame(onGameStarted: () -> Unit) {
        val session = roomSession ?: return
        val state = _uiState.value
        if (!state.canStart) {
            _uiState.update { it.copy(errorMessage = "Espere pelo menos mais um jogador entrar (2 a 4 no total).") }
            return
        }

        _uiState.update { it.copy(phase = HostLobbyPhase.STARTING, errorMessage = null) }
        viewModelScope.launch {
            val catalog = container.songRepository.getCatalog()
            if (catalog.isEmpty()) {
                _uiState.update {
                    it.copy(phase = HostLobbyPhase.WAITING, errorMessage = "Catálogo de músicas ainda não carregado. Tente novamente.")
                }
                return@launch
            }

            val players = state.players.map { Player(id = it.playerId, name = it.playerName, createdAt = System.currentTimeMillis()) }
            val config = GameConfig(players = players, roundCount = state.roundCount, stealEnabled = state.stealEnabled)

            container.gameEngine.setCatalog(catalog)
            val coordinator = HostGameCoordinator(container.gameEngine, session, container.applicationScope)
            coordinator.startGame(config)

            gameStarted = true
            onGameStarted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (gameStarted) return
        advertiser.stop()
        roomSession?.let { session ->
            CoroutineScope(Dispatchers.IO).launch { runCatching { session.stop() } }
        }
    }

    companion object {
        // Sem 0/O nem 1/I: evita ambiguidade pra quem for digitar o código manualmente.
        private const val ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        private fun generateRoomCode(length: Int = 5): String =
            (1..length).map { ROOM_CODE_CHARS.random() }.joinToString("")
    }
}
