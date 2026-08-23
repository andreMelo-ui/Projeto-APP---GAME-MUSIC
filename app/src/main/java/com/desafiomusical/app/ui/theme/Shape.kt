package com.desafiomusical.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas do design system. As referências mostram cantos bem arredondados em
 * praticamente todos os elementos — cards (~20dp), botões grandes (~16dp) e
 * elementos "pill" totalmente arredondados (seletores de jogadores/rodadas,
 * badges, bottom nav).
 */

val ShapeCard = RoundedCornerShape(20.dp)
val ShapeCardLarge = RoundedCornerShape(24.dp)   // cards de destaque (Escolhedor, resultado da música)
val ShapeButton = RoundedCornerShape(16.dp)
val ShapePill = RoundedCornerShape(999.dp)       // seletores, badges, bottom nav, código da sala
val ShapeInput = RoundedCornerShape(14.dp)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = ShapeInput,
    medium = ShapeButton,
    large = ShapeCard,
    extraLarge = ShapeCardLarge
)
