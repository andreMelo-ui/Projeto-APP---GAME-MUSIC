package com.desafiomusical.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografia do Desafio Musical.
 * As telas de referência usam uma sans-serif bold e bem geométrica nos títulos
 * (ex.: "Desafio Musical" na Home, o número do cronômetro, "+15" no resultado).
 * Usamos a família padrão do sistema (FontFamily.SansSerif) com pesos altos —
 * já cobre o visual sem exigir uma fonte custom nesta fase; trocar por uma
 * fonte de marca (ex. via res/font) é trivial depois, basta alterar FontFamily aqui.
 */

private val AppFontFamily = FontFamily.SansSerif

val Typography =
    Typography(
        // Título principal (ex.: "Desafio Musical" na Home)
        displayLarge =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        // Número gigante do cronômetro / pontuação flutuante ("+15")
        displayMedium =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 56.sp,
                lineHeight = 60.sp,
                letterSpacing = (-1).sp,
            ),
        // Título de tela (ex.: "Configuração", "Histórico", "Placar Final")
        headlineMedium =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        // Título de card (ex.: nome da música "Evidências", nome do jogador no resultado)
        headlineSmall =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        // Texto de botão e labels de destaque (ex.: "Nova Partida", nomes de jogadores em lista)
        titleMedium =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        // Labels de categoria em cards (ex.: "Brasileira", "Games")
        titleSmall =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        // Corpo de texto padrão
        bodyLarge =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        // Texto secundário pequeno (ex.: "Clássico sertanejo", subtítulo do artista)
        bodySmall =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
        // Labels em caixa alta (ex.: "JOGADORES", "TEMPO POR RODADA", "RANKING ATUAL")
        labelLarge =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.8.sp,
            ),
        // Badges pequenas (ex.: "VOCÊ É O ESCOLHEDOR", "SOMENTE VISUALIZAÇÃO", "Médio")
        labelMedium =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
            ),
        // Legendas mínimas (ex.: "SEGUNDOS" abaixo do número do cronômetro)
        labelSmall =
            TextStyle(
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                letterSpacing = 1.sp,
            ),
    )
