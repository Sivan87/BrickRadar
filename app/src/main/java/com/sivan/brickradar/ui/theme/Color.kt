package com.sivan.brickradar.ui.theme

import androidx.compose.ui.graphics.Color

// Palett från designdokumentet ("Klonradarn Design Options.dc.html", rond
// t5-t11 där mörka hex-färger konvergerade — se CLAUDE.md/ui-redesign-anteckningar).
val AppBackground = Color(0xFF0D0D0D)
val ScreenBackground = Color(0xFF0B0D0F)
val CardBackground = Color(0xFF0E1113)
val CardBackgroundAlt = Color(0xFF0C0F11)
val PanelBackground = Color(0xFF0D1012)
val HighlightCardBackground = Color(0xFF101316)
val ImagePlaceholder = Color(0xFF1A1E22)

val CardBorder = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
val CardBorderMuted = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
val HighlightBorder = Color(0x8CE8C93F) // rgba(232,201,63,0.55)

val AccentGold = Color(0xFFE8C93F)
val AccentGoldLink = Color(0xFFFFCE45)

val TextPrimary = Color(0xFFFBFAF8)
val TextSecondary = Color(0xFFE9E6E0)
val TextMutedLight = Color(0xFFA2A8B0)
val TextMuted = Color(0xFF8B929C)
val TextMutedMore = Color(0xFF7D838C)
val TextMutedMost = Color(0xFF6E747C)
val TextDim = Color(0xFF5F666E)

val PositiveGreen = Color(0xFF63C98A)
val NegativeRed = Color(0xFFE0705E)

// Samma 5 kr/del-nivåer som webbens --cyan/--green/--amber/--orange/--red
// (static/style.css) — ANVÄND FÖR ALLT som färgar en kr/del-siffra efter
// tröskelmodellen (klon: cyan/grön/gul/orange/röd, LEGO: grön/gul/orange/röd,
// se app.py: classify_value/CLONE_VALUE_LEVELS/LEGO_VALUE_LEVELS). Håll dessa
// i synk med webbens hex-värden — de är inte bara en visuell tycka-smak-sak,
// utan ska vara den ENDA sanningen om vad "bra pris" betyder i appen.
val ValueCyan = Color(0xFF22D3EE)
val ValueGreen = Color(0xFF4ADE80)
val ValueYellow = Color(0xFFFFCE45)
val ValueOrange = Color(0xFFFB923C)
val ValueRed = Color(0xFFF87171)

// Statuslistans "bevakar"-färg i webbens statistikvy (--blue, static/style.css)
// — status-dot/bar-fill för de andra tre lägena återanvänder redan befintliga
// AccentGold/ValueYellow ("sök"), PositiveGreen/ValueGreen ("äger") och
// TextDim ("avslagen"), så bara denna saknades.
val StatusWatchingBlue = Color(0xFF8B93FF)

val HomeIndicator = Color(0x47FFFFFF) // rgba(255,255,255,0.28)
