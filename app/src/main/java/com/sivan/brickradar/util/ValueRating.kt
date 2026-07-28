package com.sivan.brickradar.util

import androidx.compose.ui.graphics.Color
import com.sivan.brickradar.ui.theme.ValueCyan
import com.sivan.brickradar.ui.theme.ValueGreen
import com.sivan.brickradar.ui.theme.ValueOrange
import com.sivan.brickradar.ui.theme.ValueRed
import com.sivan.brickradar.ui.theme.ValueYellow

// Speglar app.py: CLONE_VALUE_LEVELS/LEGO_VALUE_LEVELS/classify_value exakt
// (mould-king-tracker, se CLAUDE.md "Value scoring (kr/piece)") — klonmodeller
// och officiella LEGO-set klassas mot separata trösklar eftersom LEGO-priser
// per bit generellt ligger mycket högre. Ordnad lägst tröskel först; sista
// posten saknar tröskel (allt däröver blir "red"). Håll i synk manuellt om
// backend-skalorna någonsin ändras — det finns inget delat schema mellan
// server och app.
private val CLONE_VALUE_LEVELS = listOf(0.35 to "cyan", 0.55 to "green", 0.80 to "yellow", 1.00 to "orange")
private val LEGO_VALUE_LEVELS = listOf(0.70 to "green", 1.00 to "yellow", 1.30 to "orange")

fun valueLevelsFor(isOfficial: Boolean): List<Pair<Double, String>> =
    if (isOfficial) LEGO_VALUE_LEVELS else CLONE_VALUE_LEVELS

fun classifyValue(krPerPiece: Double?, isOfficial: Boolean): String? {
    if (krPerPiece == null) return null
    for ((threshold, tier) in valueLevelsFor(isOfficial)) {
        if (krPerPiece <= threshold) return tier
    }
    return "red"
}

fun colorForValueRating(rating: String?): Color = when (rating) {
    "cyan" -> ValueCyan
    "green" -> ValueGreen
    "yellow" -> ValueYellow
    "orange" -> ValueOrange
    "red" -> ValueRed
    else -> ValueRed
}
