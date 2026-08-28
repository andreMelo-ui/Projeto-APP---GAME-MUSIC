package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.domain.GameEngine
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.domain.state.GameUiState
import com.desafiomusical.app.network.payloads.GameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID

/**
 * Ponte entre o [GameEngine] (autoridade de estado, alheia à rede) e a
 * [RoomSession] do host. Toda intenção de jogador — local (host jogando) ou
 * remota (evento de rede recebido via [RoomSession.observeIncoming]) — deve
 * passar pelos métodos públicos desta classe, nunca diretamente pelo
 * [GameEngine]: é aqui, e só aqui, que ficam (a) a validação de que o
 * remetente de um evento de rede é realmente quem tem autoridade para aquela
 * ação na rodada atual, e (b) a decisão do que pode ir por
 * [RoomSession.broadcast] (todos) e do que só pode ir por
 * [RoomSession.sendTo] ao escolhedor. Título, artista e obra da música da
 * rodada corrente NUNCA aparecem num evento de broadcast — só em
 * [GameEvent.SongOptions] (privado ao escolhedor, antes de a rodada começar)
 * ou em [GameEvent.RoundEnd] (depois de julgada, quando revelar é seguro).
 */
class HostGameCoordinator(
    private val gameEngine: GameEngine,
    private val roomSession: RoomSession,
    scope: CoroutineScope,
) {
    private var currentRoundId: String = ""

    init {
        roomSession.observeIncoming()
            .onEach { handleIncoming(it) }
            .launchIn(scope)

        gameEngine.elapsedSeconds
            .onEach { seconds ->
                if (gameEngine.uiState.value is GameUiState.Playing) {
                    roomSession.broadcast(GameEvent.TimerSync(newId(), now(), currentRoundId, seconds))
                }
            }
            .launchIn(scope)
    }

    // ---- API pública: chamada pela UI local do host E pelo roteador de rede abaixo ----

    suspend fun startGame(config: GameConfig) {
        gameEngine.startGame(config)
        roomSession.broadcast(GameEvent.GameStart(newId(), now()))
        announceRoundStart()
    }

    /** Combina seleção de categoria (derivada de [song]) + música + início da reprodução, num só passo de rede. */
    suspend fun selectSongForRound(song: Song) {
        gameEngine.selectCategory(song.category)
        gameEngine.selectSongForRound(song)
        gameEngine.startPlayback()
        val masked = (gameEngine.uiState.value as GameUiState.Playing).round.maskedSong
        roomSession.broadcast(
            GameEvent.SongPlaying(
                eventId = newId(),
                timestamp = now(),
                roundId = currentRoundId,
                category = masked.category.name,
                difficulty = masked.difficulty.name,
                youtubeVideoId = masked.youtubeVideoId,
                hasWork = masked.hasWork,
            ),
        )
    }

    suspend fun requestHint() {
        gameEngine.requestHint()
        val playing = gameEngine.uiState.value as? GameUiState.Playing ?: return
        val level = playing.round.hintsRevealedCount
        if (level <= 0) return
        val song = gameEngine.songById(playing.round.maskedSong.id) ?: return
        roomSession.broadcast(GameEvent.HintUsed(newId(), now(), currentRoundId, level, song.hintFor(level)))
    }

    suspend fun submitMainAnswer(
        titleClaimed: Boolean,
        artistClaimed: Boolean,
        workClaimed: Boolean = false,
    ) {
        gameEngine.submitMainAnswer(titleClaimed, artistClaimed, workClaimed)
        // Answering não é uma tela pública (mostra a música completa) — nada é
        // transportado aqui; o julgamento em si chega por confirmMainAnswer.
    }

    suspend fun confirmMainAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean = false,
    ) {
        val setup = gameEngine.currentRoundSetup ?: return
        gameEngine.confirmMainAnswer(titleCorrect, artistCorrect, workCorrect)
        val pointsAwarded = pointsAwardedIfSettled(titleCorrect || artistCorrect || workCorrect)
        roomSession.broadcast(
            GameEvent.AnswerResult(
                newId(),
                now(),
                currentRoundId,
                setup.mainResponderId,
                titleCorrect,
                artistCorrect,
                pointsAwarded,
                workCorrect,
            ),
        )
        afterJudgement()
    }

    suspend fun claimSteal(playerId: String) {
        runCatching { gameEngine.claimSteal(playerId) }
    }

    suspend fun confirmStealAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean = false,
    ) {
        val stealerId = (gameEngine.uiState.value as? GameUiState.StealAnswer)?.stealerId ?: return
        gameEngine.confirmStealAnswer(titleCorrect, artistCorrect, workCorrect)
        val correct = titleCorrect || artistCorrect || workCorrect
        val pointsAwarded = pointsAwardedIfSettled(correct)
        roomSession.broadcast(GameEvent.StealResult(newId(), now(), currentRoundId, stealerId, correct, pointsAwarded))
        afterJudgement()
    }

    suspend fun goToNextRound() {
        gameEngine.goToNextRound()
        when (val state = gameEngine.uiState.value) {
            is GameUiState.CategorySelection -> announceRoundStart()
            is GameUiState.GameResult -> roomSession.broadcast(GameEvent.GameEnd(newId(), now(), state.result.winner?.id))
            else -> Unit
        }
    }

    // ---- Roteamento de eventos de rede: valida autoria antes de aplicar ----

    private suspend fun handleIncoming(event: GameEvent) {
        val setup = gameEngine.currentRoundSetup
        when (event) {
            is GameEvent.SongSelected -> {
                if (setup == null || event.playerId != setup.chooserId || event.roundId != currentRoundId) return
                val song = gameEngine.songById(event.songId) ?: return
                runCatching { selectSongForRound(song) }
            }

            is GameEvent.HintUsed -> {
                if (event.roundId != currentRoundId) return
                runCatching { requestHint() }
            }

            is GameEvent.AnswerAttempt -> {
                if (setup == null || event.playerId != setup.mainResponderId || event.roundId != currentRoundId) return
                runCatching { submitMainAnswer(event.titleClaimed, event.artistClaimed) }
            }

            is GameEvent.AnswerJudged -> {
                if (setup == null || event.playerId != setup.chooserId || event.roundId != currentRoundId) return
                runCatching { confirmMainAnswer(event.titleCorrect, event.artistCorrect, event.workCorrect) }
            }

            is GameEvent.StealClaim -> {
                if (event.roundId != currentRoundId) return
                runCatching { claimSteal(event.playerId) }
            }

            is GameEvent.StealJudged -> {
                if (setup == null || event.playerId != setup.chooserId || event.roundId != currentRoundId) return
                runCatching { confirmStealAnswer(event.titleCorrect, event.artistCorrect, event.workCorrect) }
            }

            is GameEvent.JoinRoom -> resyncPlayer(event.playerId)

            // CreateRoom/PlayerReady/GameStart/SongOptions/SongPlaying/TimerSync/AnswerResult/
            // StealOpen/StealResult/RoundEnd/GameEnd são saída do host, não entrada.
            else -> Unit
        }
    }

    /**
     * Um [GameEvent.JoinRoom] chegando DEPOIS da partida já ter começado só
     * pode ser reconexão (etapa 7) — [KtorClientRoomSession] manda de novo com
     * o MESMO `playerId` quando a conexão volta sozinha. Reenvia o suficiente
     * pra reconstruir a tela atual: sempre de quem é a vez, e o estado
     * mascarado da rodada quando ela está em andamento — nunca a música
     * completa (essa regra vale igual pra resync).
     */
    private suspend fun resyncPlayer(playerId: String) {
        val setup = gameEngine.currentRoundSetup ?: return
        val knownPlayerIds = setup.eligibleStealerIds + setup.chooserId + setup.mainResponderId
        if (playerId !in knownPlayerIds) return

        roomSession.sendTo(
            playerId,
            GameEvent.RoundStart(newId(), now(), currentRoundId, setup.roundNumber, setup.chooserId, setup.mainResponderId),
        )
        when (val state = gameEngine.uiState.value) {
            is GameUiState.Playing -> {
                val masked = state.round.maskedSong
                roomSession.sendTo(
                    playerId,
                    GameEvent.SongPlaying(
                        newId(),
                        now(),
                        currentRoundId,
                        masked.category.name,
                        masked.difficulty.name,
                        masked.youtubeVideoId,
                        masked.hasWork,
                    ),
                )
                roomSession.sendTo(playerId, GameEvent.TimerSync(newId(), now(), currentRoundId, state.elapsedSeconds))
            }
            is GameUiState.StealWindow ->
                roomSession.sendTo(
                    playerId,
                    GameEvent.StealOpen(newId(), now(), currentRoundId, state.round.eligibleStealers.map { it.id }, state.stealSecondsLeft),
                )
            else -> Unit // CategorySelection/SongSelection/Ready/Answering/StealAnswer/RoundResult/GameResult:
            // estados transitórios ou já exclusivos do julgador local — o RoundStart acima já basta.
        }
    }

    // ---- Auxiliares ----

    private suspend fun announceRoundStart() {
        val setup = gameEngine.currentRoundSetup ?: return
        currentRoundId = UUID.randomUUID().toString()
        roomSession.broadcast(
            GameEvent.RoundStart(newId(), now(), currentRoundId, setup.roundNumber, setup.chooserId, setup.mainResponderId),
        )
        sendSongOptions(setup.chooserId)
    }

    private suspend fun sendSongOptions(chooserId: String) {
        val categories = Category.concrete.filter { gameEngine.candidatesNow(it).isNotEmpty() }
        val candidatesByCategory =
            categories.associate { category ->
                category.name to gameEngine.candidatesNow(category).map { it.id }
            }
        roomSession.sendTo(
            chooserId,
            GameEvent.SongOptions(newId(), now(), currentRoundId, chooserId, categories.map { it.name }, candidatesByCategory),
        )
    }

    /** Depois de um julgamento (resposta principal ou roubo), decide se abre a próxima janela de roubo ou encerra a rodada. */
    private suspend fun afterJudgement() {
        when (val state = gameEngine.uiState.value) {
            is GameUiState.StealWindow ->
                roomSession.broadcast(
                    GameEvent.StealOpen(newId(), now(), currentRoundId, state.round.eligibleStealers.map { it.id }, state.stealSecondsLeft),
                )
            is GameUiState.RoundResult ->
                roomSession.broadcast(
                    GameEvent.RoundEnd(newId(), now(), currentRoundId, state.result.winner?.id, state.result.song.id),
                )
            else -> Unit
        }
    }

    private fun pointsAwardedIfSettled(settled: Boolean): Int =
        if (settled) (gameEngine.uiState.value as GameUiState.RoundResult).result.pointsAwarded else 0

    private fun newId() = UUID.randomUUID().toString()

    private fun now() = System.currentTimeMillis()
}
