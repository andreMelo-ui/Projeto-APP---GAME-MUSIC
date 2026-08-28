package com.desafiomusical.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.ui.theme.BgSurfaceElevated
import com.desafiomusical.app.ui.theme.TextPrimary
import com.desafiomusical.app.ui.theme.TextSecondary

@Composable
fun ScoreBadge(
    playerName: String,
    score: Int,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val background = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else BgSurfaceElevated
    Row(
        modifier =
            modifier
                .background(background, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else TextPrimary,
        )
        Text(
            text = "$score pts",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
        )
    }
}
