package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.state.ActiveRoundView
import com.desafiomusical.app.domain.usecase.ScoringRules
import com.desafiomusical.app.ui.components.NeonTimer
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.SecondaryButton
import com.desafiomusical.app.ui.theme.ColorResponder
import com.desafiomusical.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayingScreen(
    round: ActiveRoundView,
    elapsedSeconds: Int,
    onRequestHint: () -> Unit,
    onSubmitAnswer: (titleClaimed: Boolean, artistClaimed: Boolean, workClaimed: Boolean) -> Unit,
    audioUnavailable: Boolean = false,
) {
    var titleClaimed by remember(round.roundNumber) { mutableStateOf(false) }
    var artistClaimed by remember(round.roundNumber) { mutableStateOf(false) }
    var workClaimed by remember(round.roundNumber) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Escolhido por ${round.chooser.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = round.mainResponder.name,
            style = MaterialTheme.typography.headlineMedium,
            color = ColorResponder,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        NeonTimer(elapsedSeconds = elapsedSeconds, totalSeconds = ScoringRules.ROUND_DURATION_SECONDS)

        if (audioUnavailable) {
            Text(
                text = "🔇 Áudio indisponível — julgue de ouvido/memória",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            text = "${round.pointsAvailableNow} pontos disponíveis agora",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        if (round.hintsRevealedCount > 0) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                round.maskedSong.hintsRevealed.forEachIndexed { index, hint ->
                    Text(
                        text = "Dica ${index + 1}: $hint",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            FilterChip(
                selected = titleClaimed,
                onClick = { titleClaimed = !titleClaimed },
                label = { Text("Acertei a Música") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
            FilterChip(
                selected = artistClaimed,
                onClick = { artistClaimed = !artistClaimed },
                label = { Text("Acertei o Artista") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
            if (round.maskedSong.hasWork) {
                FilterChip(
                    selected = workClaimed,
                    onClick = { workClaimed = !workClaimed },
                    label = { Text("Acertei a Obra") },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            }
        }

        PrimaryButton(
            text = "Confirmar Resposta",
            onClick = { onSubmitAnswer(titleClaimed, artistClaimed, workClaimed) },
            enabled = titleClaimed || artistClaimed || workClaimed,
            modifier = Modifier.fillMaxWidth(),
        )

        SecondaryButton(
            text =
                if (round.hintsRevealedCount >= ScoringRules.MAX_HINTS) {
                    "Todas as dicas usadas"
                } else {
                    "Pedir Dica ${round.hintsRevealedCount + 1} de ${ScoringRules.MAX_HINTS}"
                },
            onClick = onRequestHint,
            enabled = round.hintsRevealedCount < ScoringRules.MAX_HINTS,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}
