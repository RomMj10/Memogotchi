package com.example.memogotchi.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)

val LocalAppColors = compositionLocalOf { ColorPresets.default() }

object ColorPresets {
    val bg = listOf(Color(0xFF16171C),
        Color(0xFF0E0F12),
        Color(0xFF1A1625),
        Color(0xFF10151A),
        Color(0xFFE7E2DF),
        Color(0xFFE9E9EC),
    )
    val surface = listOf(Color(0xFF1F2125),
        Color(0xFF17181C),
        Color(0xFF241F30),
        Color(0xFF18222A),
        Color(0xFFD2CCC9),
        Color(0xFFDEDEE1),
    )
    val accent = listOf(Color(0xFF77C59D),
        Color(0xFF8CACCC),
        Color(0xFFE8925A),
        Color(0xFFB08ACB),
        Color(0xFFC46887),
        Color(0xFF915643),

    )
    val textPrimary = listOf(Color(0xFFFFFFFF),
        Color(0xFFE8E6F0),
        Color(0xFFF2F2F2),
        Color(0xFF332F2E),
        Color(0xFF100E0E),
        Color(0xFF282833),
    )
    val textSecondary = listOf(Color(0xFF888888),
        Color(0xFF98ABA0),
        Color(0xFF7A7A85),
        Color(0xFF5D5853),
        Color(0xFF50505B),
        )

    fun default() = AppColors(
        bg = bg[0], surface = surface[0], accent = accent[0],
        accentLight = accent[0].lighten(0.2f), accentDark = accent[0].darken(0.2f),
        textPrimary = textPrimary[0], textSecondary = textSecondary[0],
    )
}

private fun Color.lighten(amt: Float) = Color(
    (red + (1 - red) * amt).coerceIn(0f, 1f),
    (green + (1 - green) * amt).coerceIn(0f, 1f),
    (blue + (1 - blue) * amt).coerceIn(0f, 1f),
)
private fun Color.darken(amt: Float) = Color(
    (red * (1 - amt)).coerceIn(0f, 1f),
    (green * (1 - amt)).coerceIn(0f, 1f),
    (blue * (1 - amt)).coerceIn(0f, 1f),
)
