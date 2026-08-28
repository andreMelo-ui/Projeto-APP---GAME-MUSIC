package com.desafiomusical.app.domain

import com.desafiomusical.app.domain.model.AttemptOutcome
import com.desafiomusical.app.domain.model.AttemptType
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.GameSnapshot
import com.desafiomusical.app.domain.model.MaskedSong
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.PlayerGameStats
import com.desafiomusical.app.domain.model.PlayerScore
import com.desafiomusical.app.domain.model.RoundOutcome
import com.desafiomusical.app.domain.model.RoundSetup
import com.desafiomusical.app.domain.model.ScoreBreakdown
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.domain.model.mask
import com.desafiomusical.app.domain.state.ActiveRoundView
import com.desafiomusical.app.domain.state.ChooserRoundView
import com.desafiomusical.app.domain.state.GamePhase
import com.desafiomusical.app.domain.state.GameResultView
import com.desafiomusical.app.domain.state.GameStateMachine
import com.desafiomusical.app.domain.state.GameUiState
import com.desafiomusical.app.domain.state.RoundResultView
import com.desafiomusical.app.domain.usecase.CalculateScoreUseCase
import com.desafiomusical.app.domain.usecase.DistributeRolesUseCase
import com.desafiomusical.app.domain.usecase.ScoringRules
import com.desafiomusical.app.domain.usecase.SelectSongUseCase
import com.desafiomusical.app.domain.usecase.TieBreakUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class AttemptRecord(
    val playerId: String,
    val attemptType: AttemptType,
    val outcome: AttemptOutcome,
    val timestamp: Long,
)

/**
 * Autoridade única do estado da partida (papel de "host"). Mesmo no modo de
 * um celular, é o GameEngine quem decide pontuação, papéis, cronômetro e
 * vencedores — a UI apenas reflete o [uiState] e envia intenções do jogador.
 *
 * Uma instância vive por partida e é reaproveitada por todas as telas do
 * fluxo, por isso [scope] deve ser um CoroutineScope de vida longa (ligado à
 * Application), não o de uma tela específica.
 */
class GameEngine(
    private val scope: CoroutineScope,
    private val calculateScore: CalculateScoreUseCase = CalculateScoreUseCase(),
    private val distributeRoles: DistributeRolesUseCase = DistributeRolesUseCase(),
    private val selectSong: SelectSongUseCase = SelectSongUseCase(),
    private val tieBreak: TieBreakUseCase = TieBreakUseCase(),
    private val random: Random = Random.Default,
) {
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Lobby)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _stealSecondsLeft = MutableStateFlow(0)
    val stealSecondsLeft: StateFlow<Int> = _stealSecondsLeft.asStateFlow()

    private var phase: GamePhase = GamePhase.LOBBY

    private var gameId: String = ""
    private var config: GameConfig? = null
    private var catalog: List<Song> = emptyList()
    private var roundSetups: List<RoundSetup> = emptyList()
    private var currentRoundIndex = 0
    private var usedSongIds = mutableSetOf<String>()

    private var currentCategory: Category? = null
    private var currentSong: Song? = null
    private var roundStartedAtMs: Long = 0L
    private var hintsUsedThisRound = 0
    private var eliminatedThisRound = mutableSetOf<String>()
    private var pendingStealerId: String? = null
    private var attempts = mutableListOf<AttemptRecord>()
    private var roundAttemptLog = mutableListOf<List<AttemptRecord>>()

    private val cumulativeScore = mutableMapOf<String, Int>()
    private val roundResults = mutableListOf<RoundOutcome>()

    private var globalTimerJob: Job? = null
    private var stealTimerJob: Job? = null

    // ---- Fluxo de configuração ----

    /**
     * Inicia uma partida nova. Reseta a máquina de estados para [GamePhase.LOBBY]
     * de propósito — isso cobre tanto o primeiro jogo quanto "jogar novamente"
     * a partir de [GamePhase.GAME_RESULT], que não tem transição de saída na
     * tabela normal (ela só governa transições *dentro* de uma partida).
     */
    fun startGame(newConfig: GameConfig) {
        phase = GamePhase.LOBBY
        gameId = java.util.UUID.randomUUID().toString()
        config = newConfig
        cumulativeScore.clear()
        newConfig.players.forEach { cumulativeScore[it.id] = 0 }
        roundSetups = distributeRoles(newConfig.players, newConfig.roundCount)
        currentRoundIndex = 0
        usedSongIds = mutableSetOf()
        roundResults.clear()
        roundAttemptLog.clear()
        transitionTo(GamePhase.PLAYER_SETUP)
        transitionTo(GamePhase.CATEGORY_SELECTION)
        emitCategorySelection()
    }

    fun setCatalog(songs: List<Song>) {
        catalog = songs
    }

    /**
     * Papéis já sorteados da rodada atual — leitura pura, sem efeito colateral.
     * Existe para quem precisa saber quem é o escolhedor/respondente antes da
     * rodada expor isso via [uiState] (ex.: a ponte de rede da Fase 3, que
     * precisa anunciar a rodada a todos antes do escolhedor ver a categoria).
     */
    val currentRoundSetup: RoundSetup?
        get() = roundSetups.getOrNull(currentRoundIndex)

    /** Candidatas disponíveis para [category] neste momento — mesma lógica usada ao montar [GameUiState.SongSelection]. */
    fun candidatesNow(category: Category): List<Song> = selectSong.candidatesFor(catalog, category, usedSongIds, random)

    /** Busca no catálogo carregado — leitura pura, para quem só recebeu um `songId` (ex.: a ponte de rede). */
    fun songById(id: String): Song? = catalog.firstOrNull { it.id == id }

    // ---- Seleção de categoria e música ----

    private fun emitCategorySelection() {
        val setup = currentRoundSetup()
        val chooser = playerById(setup.chooserId)
        // Só oferece categorias que ainda têm música não usada nesta partida —
        // evitar que o escolhedor caia numa tela de música sem nenhum candidato.
        val available = selectSong.availableCategories(catalog, usedSongIds)
        _uiState.value =
            GameUiState.CategorySelection(
                chooser = chooser,
                roundNumber = setup.roundNumber,
                totalRounds = roundSetups.size,
                categories = if (available.isEmpty()) emptyList() else available + Category.ALEATORIO,
            )
    }

    fun selectCategory(category: Category) {
        check(phase == GamePhase.CATEGORY_SELECTION) { "Categoria só pode ser escolhida em CATEGORY_SELECTION." }
        val resolvedCategory =
            if (category == Category.ALEATORIO) {
                val available = selectSong.availableCategories(catalog, usedSongIds)
                selectSong.randomCategory(available, random)
            } else {
                category
            }
        currentCategory = resolvedCategory
        transitionTo(GamePhase.SONG_SELECTION)

        val setup = currentRoundSetup()
        _uiState.value =
            GameUiState.SongSelection(
                chooser = playerById(setup.chooserId),
                roundNumber = setup.roundNumber,
                totalRounds = roundSetups.size,
                category = resolvedCategory,
                candidates = selectSong.candidatesFor(catalog, resolvedCategory, usedSongIds, random),
            )
    }

    fun selectSongForRound(song: Song) {
        check(phase == GamePhase.SONG_SELECTION) { "Música só pode ser escolhida em SONG_SELECTION." }
        beginReadyPhase(song)
    }

    fun selectRandomSongForRound() {
        check(phase == GamePhase.SONG_SELECTION) { "Música só pode ser escolhida em SONG_SELECTION." }
        val category = currentCategory ?: error("Categoria não definida.")
        val song =
            selectSong.randomPick(catalog, category, usedSongIds, random)
                ?: error("Não há músicas disponíveis nessa categoria.")
        beginReadyPhase(song)
    }

    private fun beginReadyPhase(song: Song) {
        currentSong = song
        usedSongIds.add(song.id)
        hintsUsedThisRound = 0
        eliminatedThisRound = mutableSetOf()
        pendingStealerId = null
        attempts = mutableListOf()

        transitionTo(GamePhase.READY)
        val setup = currentRoundSetup()
        _uiState.value =
            GameUiState.Ready(
                chooserView =
                    ChooserRoundView(
                        roundNumber = setup.roundNumber,
                        totalRounds = roundSetups.size,
                        song = song,
                        mainResponder = playerById(setup.mainResponderId),
                    ),
            )
    }

    // ---- Reprodução e cronômetro ----

    fun startPlayback() {
        check(phase == GamePhase.READY) { "A reprodução só pode iniciar a partir de READY." }
        transitionTo(GamePhase.PLAYING)
        roundStartedAtMs = System.currentTimeMillis()
        _elapsedSeconds.value = 0
        startGlobalTimer()
        emitPlayingState()
    }

    private fun startGlobalTimer() {
        globalTimerJob?.cancel()
        globalTimerJob =
            scope.launch {
                while (isActive) {
                    delay(1000)
                    val elapsed =
                        ((System.currentTimeMillis() - roundStartedAtMs) / 1000).toInt()
                            .coerceAtMost(ScoringRules.ROUND_DURATION_SECONDS)
                    _elapsedSeconds.value = elapsed
                    if (elapsed >= ScoringRules.ROUND_DURATION_SECONDS) {
                        onRoundTimeout()
                        break
                    }
                }
            }
    }

    private fun stopGlobalTimer() {
        globalTimerJob?.cancel()
        globalTimerJob = null
    }

    private fun onRoundTimeout() {
        if (phase == GamePhase.ROUND_RESULT || phase == GamePhase.GAME_RESULT) return
        cancelStealTimer()
        finalizeRound(winnerId = null, breakdown = null)
    }

    private fun emitPlayingState() {
        _uiState.value = GameUiState.Playing(round = buildActiveRoundView(), elapsedSeconds = _elapsedSeconds.value)
    }

    fun requestHint() {
        check(phase == GamePhase.PLAYING) { "Dicas só podem ser pedidas durante PLAYING." }
        if (hintsUsedThisRound >= ScoringRules.MAX_HINTS) return
        hintsUsedThisRound += 1
        attempts.add(AttemptRecord(currentRoundSetup().mainResponderId, AttemptType.HINT_REQUEST, AttemptOutcome.WRONG, now()))
        emitPlayingState()
    }

    // ---- Resposta do respondente principal ----

    fun submitMainAnswer(
        titleClaimed: Boolean,
        artistClaimed: Boolean,
        workClaimed: Boolean = false,
    ) {
        check(phase == GamePhase.PLAYING) { "Só é possível responder durante PLAYING." }
        transitionTo(GamePhase.ANSWERING)
        _uiState.value =
            GameUiState.Answering(
                round = buildActiveRoundView(),
                song = currentSong ?: error("Música da rodada não definida."),
                elapsedSeconds = _elapsedSeconds.value,
                titleClaimed = titleClaimed,
                artistClaimed = artistClaimed,
                workClaimed = workClaimed,
            )
    }

    /** Confirmação feita pelo host/controlador local, com base na música secreta. */
    fun confirmMainAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean = false,
    ) {
        check(phase == GamePhase.ANSWERING) { "Confirmação só é válida durante ANSWERING." }
        val setup = currentRoundSetup()
        val outcome = outcomeFor(titleCorrect, artistCorrect, workCorrect)
        attempts.add(AttemptRecord(setup.mainResponderId, AttemptType.MAIN_ANSWER, outcome, now()))

        if (titleCorrect || artistCorrect || workCorrect) {
            val breakdown =
                calculateScore(
                    playerId = setup.mainResponderId,
                    elapsedSeconds = _elapsedSeconds.value,
                    titleCorrect = titleCorrect,
                    artistCorrect = artistCorrect,
                    workCorrect = workCorrect,
                    hintsUsed = hintsUsedThisRound,
                )
            finalizeRound(winnerId = setup.mainResponderId, breakdown = breakdown)
        } else {
            eliminatedThisRound.add(setup.mainResponderId)
            openStealWindowIfPossible()
        }
    }

    // ---- Roubo ----

    private fun openStealWindowIfPossible() {
        val stealEnabled = config?.stealEnabled == true
        val nextStealer = nextEligibleStealer()
        if (!stealEnabled || nextStealer == null) {
            finalizeRound(winnerId = null, breakdown = null)
            return
        }
        transitionTo(GamePhase.STEAL_WINDOW)
        startStealTimer()
        _uiState.value =
            GameUiState.StealWindow(
                round = buildActiveRoundView(),
                elapsedSeconds = _elapsedSeconds.value,
                stealSecondsLeft = _stealSecondsLeft.value,
            )
    }

    private fun nextEligibleStealer(): Player? {
        val setup = currentRoundSetup()
        return setup.eligibleStealerIds
            .firstOrNull { it !in eliminatedThisRound }
            ?.let { playerById(it) }
    }

    private fun startStealTimer() {
        stealTimerJob?.cancel()
        _stealSecondsLeft.value = ScoringRules.STEAL_WINDOW_SECONDS
        stealTimerJob =
            scope.launch {
                var remaining = ScoringRules.STEAL_WINDOW_SECONDS
                while (isActive && remaining > 0) {
                    delay(1000)
                    remaining -= 1
                    _stealSecondsLeft.value = remaining
                }
                if (isActive && remaining <= 0) {
                    onStealWindowExpired()
                }
            }
    }

    private fun cancelStealTimer() {
        stealTimerJob?.cancel()
        stealTimerJob = null
    }

    private fun onStealWindowExpired() {
        if (phase != GamePhase.STEAL_WINDOW) return
        finalizeRound(winnerId = null, breakdown = null)
    }

    fun claimSteal(playerId: String) {
        check(phase == GamePhase.STEAL_WINDOW) { "Roubo só pode ser reivindicado durante STEAL_WINDOW." }
        val setup = currentRoundSetup()
        require(playerId in setup.eligibleStealerIds) { "Jogador não elegível para roubar." }
        require(playerId !in eliminatedThisRound) { "Jogador já eliminado nesta rodada." }
        cancelStealTimer()
        pendingStealerId = playerId
        transitionTo(GamePhase.STEAL_ANSWER)
        _uiState.value =
            GameUiState.StealAnswer(
                round = buildActiveRoundView(),
                song = currentSong ?: error("Música da rodada não definida."),
                elapsedSeconds = _elapsedSeconds.value,
                stealerId = playerId,
            )
    }

    fun confirmStealAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean = false,
    ) {
        check(phase == GamePhase.STEAL_ANSWER) { "Confirmação de roubo só é válida durante STEAL_ANSWER." }
        val stealerId = pendingStealerId ?: error("Nenhum roubo pendente.")
        val outcome = outcomeFor(titleCorrect, artistCorrect, workCorrect)
        attempts.add(AttemptRecord(stealerId, AttemptType.STEAL_ANSWER, outcome, now()))

        if (titleCorrect || artistCorrect || workCorrect) {
            val breakdown =
                calculateScore(
                    playerId = stealerId,
                    elapsedSeconds = _elapsedSeconds.value,
                    titleCorrect = titleCorrect,
                    artistCorrect = artistCorrect,
                    workCorrect = workCorrect,
                    hintsUsed = hintsUsedThisRound,
                )
            finalizeRound(winnerId = stealerId, breakdown = breakdown)
        } else {
            eliminatedThisRound.add(stealerId)
            pendingStealerId = null
            openStealWindowIfPossible()
        }
    }

    private fun outcomeFor(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean,
    ): AttemptOutcome =
        when {
            titleCorrect && artistCorrect -> AttemptOutcome.CORRECT_BOTH
            titleCorrect -> AttemptOutcome.CORRECT_TITLE
            artistCorrect -> AttemptOutcome.CORRECT_ARTIST
            workCorrect -> AttemptOutcome.CORRECT_WORK
            else -> AttemptOutcome.WRONG
        }

    // ---- Encerramento de rodada ----

    private fun finalizeRound(
        winnerId: String?,
        breakdown: ScoreBreakdown?,
    ) {
        stopGlobalTimer()
        cancelStealTimer()

        val setup = currentRoundSetup()
        val song = currentSong ?: error("Música da rodada não definida.")

        if (breakdown != null && winnerId != null) {
            cumulativeScore[winnerId] = (cumulativeScore[winnerId] ?: 0) + breakdown.totalPoints
        }

        roundResults.add(
            RoundOutcome(
                roundNumber = setup.roundNumber,
                chooserId = setup.chooserId,
                mainResponderId = setup.mainResponderId,
                songId = song.id,
                winnerId = winnerId,
                startedAt = roundStartedAtMs,
                endedAt = now(),
                scores = listOfNotNull(breakdown),
                hintsUsed = hintsUsedThisRound,
                eliminatedIds = eliminatedThisRound.toList(),
            ),
        )
        roundAttemptLog.add(attempts.toList())

        transitionTo(GamePhase.ROUND_RESULT)
        _uiState.value =
            GameUiState.RoundResult(
                RoundResultView(
                    roundNumber = setup.roundNumber,
                    totalRounds = roundSetups.size,
                    song = song,
                    winner = winnerId?.let { playerById(it) },
                    pointsAwarded = breakdown?.totalPoints ?: 0,
                    elapsedSeconds = _elapsedSeconds.value,
                    scoreboard = currentScoreboard(),
                ),
            )
    }

    fun goToNextRound() {
        check(phase == GamePhase.ROUND_RESULT) { "Só é possível avançar a partir de ROUND_RESULT." }
        transitionTo(GamePhase.NEXT_ROUND)
        currentRoundIndex += 1
        if (currentRoundIndex >= roundSetups.size) {
            finishGame()
        } else {
            transitionTo(GamePhase.CATEGORY_SELECTION)
            emitCategorySelection()
        }
    }

    private fun finishGame() {
        transitionTo(GamePhase.GAME_RESULT)
        val scoreboard = currentScoreboard()
        _uiState.value =
            GameUiState.GameResult(
                GameResultView(
                    finalScoreboard = scoreboard,
                    winner = scoreboard.maxByOrNull { it.totalScore }?.player,
                    stats = buildStats(),
                ),
            )
    }

    fun detectFinalTie(): List<PlayerScore> = tieBreak(currentScoreboard())

    /** Retrato da partida encerrada, pronto para ser persistido no histórico. */
    fun snapshotForPersistence(): GameSnapshot? {
        val cfg = config ?: return null
        if (phase != GamePhase.GAME_RESULT) return null
        val scoreboard = currentScoreboard()
        return GameSnapshot(
            gameId = gameId,
            config = cfg,
            rounds = roundResults.toList(),
            finalScoreboard = scoreboard,
            winner = scoreboard.maxByOrNull { it.totalScore }?.player,
        )
    }

    private fun buildStats(): List<PlayerGameStats> {
        val allPlayers = config?.players.orEmpty()
        return allPlayers.map { player ->
            val playerAttempts = roundAttemptLog.flatten().filter { it.playerId == player.id }
            val mainOrSteal = playerAttempts.filter { it.attemptType != AttemptType.HINT_REQUEST }
            val correct = mainOrSteal.count { it.outcome != AttemptOutcome.WRONG }
            val wrong = mainOrSteal.count { it.outcome == AttemptOutcome.WRONG }
            val pointsPerRound =
                roundResults.mapNotNull { outcome ->
                    outcome.scores.firstOrNull { it.playerId == player.id }?.totalPoints
                }
            PlayerGameStats(
                player = player,
                correctAnswers = correct,
                wrongAnswers = wrong,
                averagePoints =
                    if (roundResults.isNotEmpty()) {
                        (cumulativeScore[player.id] ?: 0).toDouble() / roundResults.size
                    } else {
                        0.0
                    },
                bestRoundPoints = pointsPerRound.maxOrNull() ?: 0,
                hintsUsed = roundAttemptLog.flatten().count { it.playerId == player.id && it.attemptType == AttemptType.HINT_REQUEST },
                stealsAttempted = playerAttempts.count { it.attemptType == AttemptType.STEAL_ANSWER },
                stealsWon = playerAttempts.count { it.attemptType == AttemptType.STEAL_ANSWER && it.outcome != AttemptOutcome.WRONG },
            )
        }
    }

    // ---- Auxiliares ----

    private fun currentRoundSetup(): RoundSetup =
        roundSetups.getOrElse(currentRoundIndex) {
            error("Índice de rodada inválido: $currentRoundIndex")
        }

    private fun playerById(id: String): Player =
        config?.players?.firstOrNull { it.id == id }
            ?: error("Jogador não encontrado: $id")

    private fun currentScoreboard(): List<PlayerScore> =
        config?.players.orEmpty()
            .map { PlayerScore(player = it, totalScore = cumulativeScore[it.id] ?: 0) }
            .sortedByDescending { it.totalScore }

    private fun buildActiveRoundView(): ActiveRoundView {
        val setup = currentRoundSetup()
        val song = currentSong ?: error("Música da rodada não definida.")
        val maskedSong: MaskedSong = song.mask(hintsUsedThisRound)
        return ActiveRoundView(
            roundNumber = setup.roundNumber,
            totalRounds = roundSetups.size,
            chooser = playerById(setup.chooserId),
            mainResponder = playerById(setup.mainResponderId),
            maskedSong = maskedSong,
            eligibleStealers = setup.eligibleStealerIds.map { playerById(it) },
            eliminatedIds = eliminatedThisRound.toSet(),
            hintsRevealedCount = hintsUsedThisRound,
            pointsAvailableNow =
                calculateScore.pointsAvailableNow(
                    _elapsedSeconds.value,
                    hintsUsedThisRound,
                    workAvailable = !song.work.isNullOrBlank(),
                ),
        )
    }

    private fun transitionTo(next: GamePhase) {
        GameStateMachine.requireTransition(phase, next)
        phase = next
    }

    private fun now(): Long = System.currentTimeMillis()
}
