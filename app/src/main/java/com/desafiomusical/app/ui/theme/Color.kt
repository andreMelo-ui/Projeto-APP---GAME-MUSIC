package com.desafiomusical.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta de cores do "Desafio Musical" — extraída por inspeção visual das telas
// aprovadas no protótipo Lovable (tema escuro, gamer-premium, neon discreto).
//
// Convenção de nomes: cores de fundo prefixadas com `Bg`, texto com `Text`,
// marca/destaque com `Brand`, semânticas com o próprio significado (Success/Warning/Danger).

// ---------------------------------------------------------------------
// Fundo
// ---------------------------------------------------------------------
val BgBase = Color(0xFF0A0512) // fundo raiz do app, quase preto com matiz roxo
val BgBaseGlowTop = Color(0xFF2A1040) // usado no gradiente radial sutil no topo de algumas telas
val BgSurface = Color(0xFF1A1025) // cards padrão
val BgSurfaceElevated = Color(0xFF241634) // cards em destaque / inputs / itens de lista ativos
val BgSurfaceBorder = Color(0x33FFFFFF) // borda 1dp translúcida usada em cards e pills

// ---------------------------------------------------------------------
// Marca / Gradiente principal (botões primários, avatar ativo, barra de progresso)
// ---------------------------------------------------------------------
val BrandMagenta = Color(0xFFA855F7)
val BrandMagentaStrong = Color(0xFFC026D3)
val BrandBlue = Color(0xFF3B82F6)
val BrandCyan = Color(0xFF22D3EE)

/** Gradiente diagonal padrão usado em botões primários, cronômetro e barras de progresso. */
val BrandGradient =
    Brush.linearGradient(
        colors = listOf(BrandMagentaStrong, BrandMagenta, BrandBlue, BrandCyan),
    )

/** Variante de 2 pontos, para elementos menores (ex.: anel de avatar). */
fun brandGradient2(): Brush = Brush.linearGradient(colors = listOf(BrandMagenta, BrandCyan))

/**
 * Cor de texto pra usar SOBRE [BrandGradient] (nunca [TextPrimary]/branco: contra a ponta
 * ciano do gradiente o contraste cai pra 1.81:1, bem abaixo do mínimo AA de 4.5:1). É o
 * mesmo tom de [BgBase] — quase preto — que mede ≥4.27:1 em todos os 4 pontos do gradiente.
 */
val TextOnGradient = BgBase

// ---------------------------------------------------------------------
// Cores semânticas
// ---------------------------------------------------------------------
val SuccessGreen = Color(0xFF22C55E) // "Pronto" no lobby
val InfoTeal = Color(0xFF14B8A6) // badges "SOMENTE VISUALIZAÇÃO" / "RESPONDENTE"
val DangerRed = Color(0xFFEF4444) // roubo / erro / eliminação
val WarningAmber = Color(0xFFF59E0B) // 1º lugar do pódio, alertas de tempo

/** Gradiente do pódio (1º lugar): laranja -> rosa. */
val PodiumGoldGradient = Brush.linearGradient(colors = listOf(WarningAmber, Color(0xFFEC4899)))

/** Cor da badge de dificuldade "Médio" (marrom/dourado translúcido). */
val DifficultyMediumBg = Color(0xFF4A3319)
val DifficultyMediumText = Color(0xFFD9A441)
val DifficultyEasyBg = Color(0xFF15412B)
val DifficultyEasyText = Color(0xFF4ADE80)
val DifficultyHardBg = Color(0xFF4A1919)
val DifficultyHardText = Color(0xFFF87171)

// ---------------------------------------------------------------------
// Texto
// ---------------------------------------------------------------------
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3D8CCEF) // lilás claro ~70% opacidade
val TextTertiary = Color(0x80D8CCEF) // lilás claro ~50% opacidade (labels pequenos, timestamps)
val TextDisabled = Color(0x4DD8CCEF)

// ---------------------------------------------------------------------
// Glow (sombras neon usadas atrás do cronômetro, avatares ativos, cards de destaque)
// ---------------------------------------------------------------------
val GlowMagenta = Color(0x40C026D3)
val GlowCyan = Color(0x4022D3EE)

// ---------------------------------------------------------------------
// Papéis semânticos de jogo (mapeados para a paleta acima — usados em
// HostLobbyScreen/JoinLobbyScreen e nas telas de rodada).
// ---------------------------------------------------------------------
val ColorChooser = BrandMagenta
val ColorResponder = BrandCyan
val ColorStealer = WarningAmber
val ColorSuccess = SuccessGreen
val ColorDanger = DangerRed
