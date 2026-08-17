package com.desafiomusical.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaçamento consistente, inferida do ritmo visual das telas de
 * referência (padding de tela ~20dp, gap entre cards ~12-16dp, gap interno
 * de card ~8dp).
 */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,   // padding padrão de tela
    val xxl: Dp = 28.dp,
    val xxxl: Dp = 40.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
