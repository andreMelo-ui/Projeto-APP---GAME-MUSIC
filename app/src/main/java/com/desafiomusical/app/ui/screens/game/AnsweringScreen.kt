package com.desafiomusical.app.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.ui.components.PrimaryButton
import com.desafiomusical.app.ui.components.RoleChip
import com.desafiomusical.app.ui.theme.NeonGreen
import com.desafiomusical.app.ui.theme.NeonPink
import com.desafiomusical.app.ui.theme.NeonRed
import com.desafiomusical.app.ui.theme.TextSecondary

/**
 * Tela de confirmação, operada por quem sabe a resposta real (o escolhedor,
 * no modo um celular). Compara o que o jogador reivindicou com a resposta
 * verdadeira e registra o resultado — a decisão final é sempre humana (V1
 * não usa reconhecimento de voz, seção 11 da especificação).
 */
@Composable
fun AnsweringScreen(
    respondent: Player,
    song: Song,
    titleClaimed: Boolean,
    artistClaimed: Boolean,
    onConfirm: (titleCorrect: Boolean, artistCorrect: Boolean) -> Unit
) {
    var titleCorrect by remember { mutableStateOf(titleClaimed) }
    var artistCorrect by remember { mutableStateOf(artistClaimed) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoleChip(label = "Confirmação do juiz", color = NeonPink)
        Text(
            text = "${respondent.name} disse:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "Título: ${song.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = "Artista: ${song.artist}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ConfirmToggleRow(
            label = "Acertou o título?",
            value = titleCorrect,
            onChange = { titleCorrect = it }
        )
        ConfirmToggleRow(
            label = "Acertou o artista?",
            value = artistCorrect,
            onChange = { artistCorrect = it }
        )

        PrimaryButton(
            text = "Confirmar",
            onClick = { onConfirm(titleCorrect, artistCorrect) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
    }
}

@Composable
private fun ConfirmToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillToggle(text = "Sim", selected = value, color = NeonGreen, onClick = { onChange(true) })
            PillToggle(text = "Não", selected = !value, color = NeonRed, onClick = { onChange(false) })
        }
    }
}

@Composable
private fun PillToggle(text: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = androidx.compose.ui.graphics.Color.Black
        )
    )
}
