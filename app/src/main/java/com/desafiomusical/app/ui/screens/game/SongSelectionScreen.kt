package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.theme.NeonPink
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun SongSelectionScreen(
    chooser: Player,
    roundNumber: Int,
    totalRounds: Int,
    category: Category,
    candidates: List<Song>,
    onSongSelected: (Song) -> Unit,
    onRandomSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = "Rodada $roundNumber de $totalRounds • ${category.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = "${chooser.name}, escolha a música",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonPink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        PrimaryButton(
            text = "Sortear música aleatória",
            onClick = onRandomSelected,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (candidates.isEmpty()) {
            Text(
                text = "Não há músicas restantes nesta categoria — use o sorteio ou volte e escolha outra categoria.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(candidates, key = { it.id }) { song ->
                    Card(
                        onClick = { onSongSelected(song) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${song.artist} • ${song.difficulty.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
