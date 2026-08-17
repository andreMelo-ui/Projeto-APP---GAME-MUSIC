package com.desafiomusical.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.ui.theme.BgSurfaceBorder
import com.desafiomusical.app.ui.theme.DangerRed
import com.desafiomusical.app.ui.theme.SuccessGreen
import com.desafiomusical.app.ui.theme.WarningAmber

/**
 * Cronômetro circular de alto contraste. A cor muda com o tempo restante,
 * mas o número sempre fica visível — a informação nunca depende só da cor
 * (requisito de acessibilidade da seção 32).
 */
@Composable
fun NeonTimer(
    elapsedSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 140.dp
) {
    val remaining = (totalSeconds - elapsedSeconds).coerceIn(0, totalSeconds)
    val progress = if (totalSeconds > 0) elapsedSeconds.toFloat() / totalSeconds else 0f
    val ratioLeft = if (totalSeconds > 0) remaining.toFloat() / totalSeconds else 0f
    val color = when {
        ratioLeft > 0.5f -> SuccessGreen
        ratioLeft > 0.2f -> WarningAmber
        else -> DangerRed
    }

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = 12.dp.toPx()
            drawArc(
                color = BgSurfaceBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            )
        }
        Text(
            text = remaining.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = color
        )
    }
}

/** Contador regressivo compacto, usado na janela de roubo de 5s. */
@Composable
fun CompactCountdown(secondsLeft: Int, modifier: Modifier = Modifier) {
    val color = if (secondsLeft > 2) WarningAmber else DangerRed
    Text(
        text = "$secondsLeft s",
        style = MaterialTheme.typography.headlineLarge,
        color = color,
        modifier = modifier
    )
}
