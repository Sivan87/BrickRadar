package com.sivan.brickradar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Designdokumentet använder 'Space Grotesk' (UI-text/titlar/etiketter) och
// 'JetBrains Mono' (priser/kr-per-del/modellnummer/tidsstämplar). Vi bäddar
// inte in de faktiska Google Fonts-typsnittsfilerna (skulle kräva antingen
// binära .ttf-resurser eller en nedladdningsbar-typsnitt-leverantör med ett
// certifikat-arrayresurs som är för lätt att få subtilt fel utan att kunna
// testköra på enhet) — GrotesqueFont/MonoFont mappar istället till
// enhetens standard sans-serif/monospace, vilket bevarar designens
// avsikt (geometrisk sans för UI, monospace för data) utan den risken.
// Byt ut FontFamily.SansSerif/FontFamily.Monospace mot riktiga
// Font(R.font.space_grotesk_*)/Font(R.font.jetbrains_mono_*)-familjer om/när
// faktiska typsnittsfiler läggs till i res/font.
val GrotesqueFont = FontFamily.SansSerif
val MonoFont = FontFamily.Monospace

val AppTypography = Typography(
    headlineSmall = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = GrotesqueFont, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = 0.6.sp),
)
