package com.desafiomusical.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DesafioMusicalColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = BackgroundDeep,
    secondary = NeonCyan,
    onSecondary = BackgroundDeep,
    tertiary = NeonPink,
    onTertiary = BackgroundDeep,
    background = BackgroundDeep,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceOutline,
    error = NeonRed,
    onError = BackgroundDeep
)

/**
 * Tema único e sempre escuro (visual "gamer-neon"). Ignora o tema do
 * sistema de propósito — este é um jogo de festa, não um app utilitário.
 */
@Composable
fun DesafioMusicalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesafioMusicalColorScheme,
        typography = DesafioMusicalTypography,
        shapes = DesafioMusicalShapes,
        content = content
    )
}
