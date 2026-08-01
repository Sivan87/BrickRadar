package com.sivan.brickradar.util

import androidx.compose.ui.graphics.Color
import com.sivan.brickradar.ui.theme.NegativeRed
import com.sivan.brickradar.ui.theme.PositiveGreen
import com.sivan.brickradar.ui.theme.StatusWatchingBlue
import com.sivan.brickradar.ui.theme.TextDim
import com.sivan.brickradar.ui.theme.TextMutedMore

// Speglar BUILD_STATUS_LABELS i static/app.js (mould-king-tracker) och
// BUILD_STATUSES i api.py exakt — samma "begränsad, känd uppsättning"-princip
// som STATUS_OPTIONS i ModelDetailScreen.kt. Ordningen här är samma ordning
// som visas i chipraden/dropdownen.
val BUILD_STATUS_OPTIONS: List<Pair<String, String>> = listOf(
    "obyggd" to "Obyggd",
    "pagaende" to "Pågående",
    "pagaende_saknar_delar" to "Saknar delar",
    "nedmonterad" to "Nedmonterad",
    "byggd" to "Byggd",
)

fun buildStatusLabel(buildStatus: String?): String? =
    BUILD_STATUS_OPTIONS.firstOrNull { it.first == buildStatus }?.second

fun buildStatusColor(buildStatus: String?): Color = when (buildStatus) {
    "obyggd" -> TextMutedMore
    "pagaende" -> StatusWatchingBlue
    "pagaende_saknar_delar" -> NegativeRed
    "nedmonterad" -> TextDim
    "byggd" -> PositiveGreen
    else -> TextMutedMore
}
