package com.sivan.brickradar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Designdokumentet utforskade både ljusa och mörka paletter (t1-t4) men
// konvergerade på ett mörkt tema från t5 och framåt (se ui-redesign-
// anteckningar) — appen har därför bara ett mörkt tema, oavsett
// systeminställning, precis som samtliga mobil-mockups i dokumentet.
private val BrickRadarColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = AppBackground,
    secondary = AccentGoldLink,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = HighlightCardBackground,
    onSurfaceVariant = TextMutedMore,
    outline = TextMutedMost,
    error = NegativeRed,
    onError = TextPrimary,
)

@Composable
fun BrickRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrickRadarColorScheme,
        typography = AppTypography,
        content = content,
    )
}
