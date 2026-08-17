package com.desafiomusical.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Tela de validação visual do design system — NÃO faz parte do fluxo do app.
 * Serve apenas para conferência rápida no painel de Preview do Android Studio
 * antes de seguirmos para componentes e telas reais.
 */
@Preview(showBackground = true, backgroundColor = 0xFF0A0512, widthDp = 400, heightDp = 1400)
@Composable
private fun ThemeValidationPreview() {
    DesafioMusicalTheme {
        Surface(color = BgBase) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LocalSpacing.current.xl),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xl)
            ) {
                SectionLabel("Tipografia")
                Text("Desafio Musical", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
                Text("18", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                Text("Configuração", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Evidências", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Nova Partida", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Brasileira", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Texto de corpo padrão para descrições.", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text("Clássico sertanejo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("JOGADORES", style = MaterialTheme.typography.labelLarge, color = TextTertiary)
                Text("SOMENTE VISUALIZAÇÃO", style = MaterialTheme.typography.labelMedium, color = InfoTeal)

                SectionLabel("Cores de fundo e superfície")
                Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                    ColorSwatch(BgBase, "BgBase")
                    ColorSwatch(BgSurface, "BgSurface")
                    ColorSwatch(BgSurfaceElevated, "BgSurfaceElevated")
                }

                SectionLabel("Gradiente de marca")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(ShapeButton)
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nova Partida", style = MaterialTheme.typography.titleMedium, color = TextOnGradient)
                }

                SectionLabel("Cores semânticas")
                Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                    ColorSwatch(SuccessGreen, "Success")
                    ColorSwatch(InfoTeal, "Info")
                    ColorSwatch(DangerRed, "Danger")
                    ColorSwatch(WarningAmber, "Warning")
                }

                SectionLabel("Badges de dificuldade")
                Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
                    DifficultyChip("Fácil", DifficultyEasyBg, DifficultyEasyText)
                    DifficultyChip("Médio", DifficultyMediumBg, DifficultyMediumText)
                    DifficultyChip("Difícil", DifficultyHardBg, DifficultyHardText)
                }

                SectionLabel("Formas")
                Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                    Box(Modifier.size(56.dp).clip(ShapeCard).background(BgSurfaceElevated))
                    Box(Modifier.size(56.dp).clip(ShapeButton).background(BgSurfaceElevated))
                    Box(Modifier.size(56.dp).clip(ShapePill).background(BgSurfaceElevated))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = BrandCyan,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ColorSwatch(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun DifficultyChip(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(ShapePill)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
