package com.sivan.brickradar.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Delad, kompakt pill-chip (issue #18, punkt 5) — ersätter de tidigare
// separata men i praktiken identiska DetailPillChip (ModelDetailScreen.kt)
// och PillFilterChip (ModelListScreen.kt), som båda byggde på Material3s
// FilterChip. FilterChip har en fast 32dp-höjd + generös inbyggd padding
// som inte går att krympa via dess publika API — en egen Box+clickable-
// implementation ger full kontroll över padding/höjd/bokstavsavstånd för
// en tightare, mer mobilanpassad känsla, samtidigt som färgspråket
// (AccentGold/AppBackground vid val, TextMuted/CardBorder annars) behålls.
@Composable
fun AppPillChip(
    selected: Boolean,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (selected) AccentGold else Color.Transparent)
            .border(width = 1.dp, color = if (selected) AccentGold else CardBorder, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
            color = if (selected) AppBackground else TextMuted,
        )
    }
}
