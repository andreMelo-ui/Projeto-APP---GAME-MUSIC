package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.state.GameUiState
import com.desafiomusical.app.ui.components.YoutubeAudioPlayer

/**
 * Ponto único de observação do [com.desafiomusical.app.domain.GameEngine]:
 * despacha para a tela correspondente ao estado atual. A navegação entre
 * fases do jogo é toda dirigida pelo GameEngine, não pelo NavHost — o
 * NavHost só entra/sai deste destino "game".
 */
@Composable
fun GameHostScreen(
    container: AppContainer,
    onExitToHome: () -> Unit,
) {
    val viewModel: GameViewModel =
        viewModel(
            factory = viewModelFactory { initializer { GameViewModel(container) } },
        )
    val state by viewModel.uiState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val stealSecondsLeft by viewModel.stealSecondsLeft.collectAsState()

    val videoId = (state as? GameUiState.Playing)?.round?.maskedSong?.youtubeVideoId
    var audioUnavailable by remember(videoId) { mutableStateOf(false) }

    Box {
        YoutubeAudioPlayer(
            videoId = videoId,
            isPlaying = state is GameUiState.Playing,
            onError = { audioUnavailable = true },
        )

        // Fundo explícito e opaco antes de qualquer composable filho: nunca depende do
        // que está por trás (WebView do player, frame de transição do NavHost, etc.).
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            when (val current = state) {
                is GameUiState.CategorySelection ->
                    CategorySelectionScreen(
                        chooser = current.chooser,
                        roundNumber = current.roundNumber,
                        totalRounds = current.totalRounds,
                        categories = current.categories,
                        onCategorySelected = viewModel::selectCategory,
                    )

                is GameUiState.SongSelection ->
                    SongSelectionScreen(
                        chooser = current.chooser,
                        roundNumber = current.roundNumber,
                        totalRounds = current.totalRounds,
                        category = current.category,
                        candidates = current.candidates,
                        onSongSelected = viewModel::selectSong,
                        onRandomSelected = viewModel::selectRandomSong,
                    )

                is GameUiState.Ready ->
                    ChooserScreen(
                        chooserView = current.chooserView,
                        onStartPlayback = viewModel::startPlayback,
                    )

                is GameUiState.Playing ->
                    PlayingScreen(
                        round = current.round,
                        elapsedSeconds = elapsedSeconds,
                        audioUnavailable = audioUnavailable,
                        onRequestHint = viewModel::requestHint,
                        onSubmitAnswer = viewModel::submitMainAnswer,
                    )

                is GameUiState.Answering ->
                    AnsweringScreen(
                        respondent = current.round.mainResponder,
                        song = current.song,
                        titleClaimed = current.titleClaimed,
                        artistClaimed = current.artistClaimed,
                        workClaimed = current.workClaimed,
                        onConfirm = viewModel::confirmMainAnswer,
                    )

                is GameUiState.StealWindow ->
                    StealWindowScreen(
                        round = current.round,
                        stealSecondsLeft = stealSecondsLeft,
                        onClaim = viewModel::claimSteal,
                    )

                is GameUiState.StealAnswer -> {
                    val stealer = current.round.eligibleStealers.firstOrNull { it.id == current.stealerId }
                    if (stealer != null) {
                        AnsweringScreen(
                            respondent = stealer,
                            song = current.song,
                            titleClaimed = false,
                            artistClaimed = false,
                            onConfirm = viewModel::confirmStealAnswer,
                        )
                    }
                }

                is GameUiState.RoundResult ->
                    RoundResultScreen(
                        result = current.result,
                        onNextRound = viewModel::goToNextRound,
                    )

                is GameUiState.GameResult -> {
                    LaunchedEffect(current) { viewModel.persistHistoryIfNeeded() }
                    GameResultScreen(result = current.result, onBackToHome = onExitToHome)
                }

                else -> Text("Preparando partida…")
            }
        }
    }
}
