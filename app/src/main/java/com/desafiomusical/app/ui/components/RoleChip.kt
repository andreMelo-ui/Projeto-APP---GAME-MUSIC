package com.desafiomusical.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.ui.theme.BackgroundDeep

/** Selo colorido para indicar visualmente o papel do jogador na rodada. */
@Composable
fun RoleChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = BackgroundDeep,
        modifier = modifier
            .background(color, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
