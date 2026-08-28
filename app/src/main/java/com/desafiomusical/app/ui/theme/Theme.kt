package com.desafiomusical.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Desafio Musical é um app "sempre escuro" por design (visual gamer-neon,
 * seção 23 da especificação) — não oferecemos um ColorScheme claro alternativo,
 * pois alto contraste e legibilidade rápida durante a partida dependem do fundo escuro.
 */
private val DesafioMusicalColorScheme =
    darkColorScheme(
        primary = BrandMagenta,
        onPrimary = TextPrimary,
        secondary = BrandCyan,
        onSecondary = TextPrimary,
        tertiary = InfoTeal,
        background = BgBase,
        onBackground = TextPrimary,
        surface = BgSurface,
        onSurface = TextPrimary,
        surfaceVariant = BgSurfaceElevated,
        onSurfaceVariant = TextSecondary,
        error = DangerRed,
        onError = TextPrimary,
        outline = BgSurfaceBorder,
    )

@Composable
fun DesafioMusicalTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            window.statusBarColor = BgBase.toArgb()
            window.navigationBarColor = BgBase.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DesafioMusicalColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
