package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.domain.state.ChooserRoundView
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.RoleChip
import com.desafiomusical.app.ui.theme.ColorChooser
import com.desafiomusical.app.ui.theme.BgSurfaceElevated
import com.desafiomusical.app.ui.theme.TextSecondary

/**
 * Tela do escolhedor (seção 8 da especificação) — único ponto do fluxo em
 * que título, artista, obra e dificuldade ficam visíveis antes da revelação.
 */
@Composable
fun ChooserScreen(
    chooserView: ChooserRoundView,
    onStartPlayback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoleChip(label = "Só você vê isso", color = ColorChooser)

        Text(
            text = "Rodada ${chooserView.roundNumber} de ${chooserView.totalRounds}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(BgSurfaceElevated, MaterialTheme.shapes.large)
                .padding(24.dp)
        ) {
            LabelValue(label = "Título", value = chooserView.song.title)
            LabelValue(label = "Artista", value = chooserView.song.artist)
            LabelValue(label = "Categoria", value = chooserView.song.category.displayName)
            val work = chooserView.song.work
            if (!work.isNullOrBlank()) {
                LabelValue(label = "Obra", value = work)
            }
            LabelValue(label = "Dificuldade", value = chooserView.song.difficulty.displayName)
        }

        Text(
            text = "${chooserView.mainResponder.name} vai responder primeiro.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 20.dp, bottom = 32.dp)
        )

        PrimaryButton(text = "Iniciar Reprodução", onClick = onStartPlayback, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
