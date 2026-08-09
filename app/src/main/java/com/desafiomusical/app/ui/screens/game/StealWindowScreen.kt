package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.state.ActiveRoundView
import com.desafiomusical.app.ui.components.BigAnswerButton
import com.desafiomusical.app.ui.components.CompactCountdown
import com.desafiomusical.app.ui.theme.BackgroundDeep
import com.desafiomusical.app.ui.theme.NeonAmber
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun StealWindowScreen(
    round: ActiveRoundView,
    stealSecondsLeft: Int,
    onClaim: (playerId: String) -> Unit
) {
    val eligible = round.eligibleStealers.filterNot { it.id in round.eliminatedIds }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHANCE DE ROUBAR",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonAmber,
            fontWeight = FontWeight.Black
        )
        CompactCountdown(secondsLeft = stealSecondsLeft, modifier = Modifier.padding(vertical = 12.dp))
        Text(
            text = "${round.pointsAvailableNow} pontos disponíveis para quem acertar",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (eligible.isEmpty()) {
            Text(
                text = "Ninguém elegível para roubar nesta rodada.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(eligible, key = { it.id }) { player ->
                    BigAnswerButton(
                        text = "RESPONDER — ${player.name}",
                        onClick = { onClaim(player.id) },
                        containerColor = NeonAmber,
                        contentColor = BackgroundDeep
                    )
                }
            }
        }
    }
}
