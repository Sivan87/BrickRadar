package com.sivan.brickradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sivan.brickradar.model.BrandBreakdown
import com.sivan.brickradar.model.CategoryBreakdown
import com.sivan.brickradar.model.StatsModelRef
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.StatusCounts
import com.sivan.brickradar.model.ValueDistribution
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.CardBackground
import com.sivan.brickradar.ui.theme.CardBorder
import com.sivan.brickradar.ui.theme.CardBorderMuted
import com.sivan.brickradar.ui.theme.MonoFont
import com.sivan.brickradar.ui.theme.PanelBackground
import com.sivan.brickradar.ui.theme.PositiveGreen
import com.sivan.brickradar.ui.theme.StatusWatchingBlue
import com.sivan.brickradar.ui.theme.TextDim
import com.sivan.brickradar.ui.theme.TextMuted
import com.sivan.brickradar.ui.theme.TextMutedMore
import com.sivan.brickradar.ui.theme.TextMutedMost
import com.sivan.brickradar.ui.theme.TextPrimary
import com.sivan.brickradar.ui.theme.TextSecondary
import com.sivan.brickradar.util.colorForValueRating
import com.sivan.brickradar.viewmodel.StatistikUiState
import com.sivan.brickradar.viewmodel.StatistikViewModel

// Fas 11 -- full paritet med webbverktygets statistiksida (issue #6 i
// Sivan87/BrickRadar: "statistik sidan innehaller inte mycket info och visar
// inte samma statistik som finns i webbverktyget"). Version 1 (Fas 10) visade
// bara Klon-/LEGO-snittet; denna vy visar nu ALLT renderStats() i
// static/app.js redan visar, i samma ordning: statusfordelning, sex
// snabbstatistik-kort, Klon-/LEGO-snitt (kvar fran version 1 -- finns inte pa
// webbens statistiksida men var redan implementerat och tas inte bort),
// prislage bland bevakade (vardefordelning), basta fynd just nu, fordelning
// per marke och basta kr/del per kategori. Ingen ny server-/DTO-utokning
// behovdes utover att modellera resten av det redan existerande
// GET /api/stats-svaret (se model/Stats.kt).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikScreen(
    onBack: () -> Unit,
    onModelClick: (Int) -> Unit,
    viewModel: StatistikViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Statistik", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBackground),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = TextMuted)
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is StatistikUiState.Loading -> LoadingBox(padding)
            is StatistikUiState.Error -> ErrorBox(padding, state.message, onRetry = viewModel::loadStats)
            is StatistikUiState.Loaded -> StatistikContent(padding, state.stats, onModelClick)
        }
    }
}

@Composable
private fun StatistikContent(
    padding: PaddingValues,
    stats: StatsResponse,
    onModelClick: (Int) -> Unit,
) {
    val counts = stats.counts ?: StatusCounts()
    val allCount = counts.new + counts.watching + counts.owned + counts.rejected

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { StatusDistribution(counts, allCount) }

        item { StatCardsGrid(stats) }

        item {
            SectionTitle("Kr/del i snitt (klon vs LEGO)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Klon-snitt", stats.avgKrPerPieceCloneAll?.let { "%.2f kr".format(it) } ?: "–", modifier = Modifier.weight(1f))
                StatCard("LEGO-snitt", stats.avgKrPerPieceLegoAll?.let { "%.2f kr".format(it) } ?: "–", modifier = Modifier.weight(1f))
            }
        }

        item {
            SectionTitle("Prisläge bland bevakade")
            ValueDistributionRow(stats.valueDistributionWatching ?: ValueDistribution())
        }

        item { SectionTitle("Bästa fynd just nu (bevakade)") }
        if (stats.bestDeals.isEmpty()) {
            item { EmptyNote("Inga bevakade modeller med känt pris ännu.") }
        } else {
            items(stats.bestDeals) { deal -> DealRow(deal, onClick = { onModelClick(deal.id) }) }
        }

        item { SectionTitle("Fördelning per märke") }
        if (stats.brandBreakdown.isEmpty()) {
            item { EmptyNote("Inga modeller ännu.") }
        } else {
            items(stats.brandBreakdown) { brand -> BrandRow(brand) }
        }

        item { SectionTitle("Bästa kr/del per kategori") }
        if (stats.categoryBreakdown.isEmpty()) {
            item { EmptyNote("Inga kategoriserade modeller med känt pris ännu.") }
        } else {
            items(stats.categoryBreakdown) { category ->
                CategoryRow(category, onClick = { category.best?.let { onModelClick(it.id) } })
            }
        }
    }
}

// Fas 12 (design t12) -- rubrikstilen speglar mockupens egen
// (font:700 12.5px, ej versaler, ljusare farg #e7e3da) istallet for den
// tidigare generiska versal-etikett-stilen (labelMedium/TextMutedMore), som
// design t12 anvander konsekvent for alla fem underrubriker pa statistiksidan.
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextMutedMost)
}

private data class StatusBarItem(val label: String, val count: Int, val color: Color)

// Fas 12 (design t12/12a) -- varje status ar tva rader (etikett+antal ovanpa
// en fullbredds-bar) istallet for den tidigare enda raden (etikett | bar |
// antal sida vid sida) -- matchar mockupens exakta struktur. "Sok"-fargen
// bytt fran ValueYellow till AccentGold for att matcha designens #e8c93f
// exakt (ValueYellow, #ffce45, ar en annan gul-nyans anvand for kr/del-
// vardering pa annat hall i appen).
@Composable
private fun StatusDistribution(counts: StatusCounts, allCount: Int) {
    val items = listOf(
        StatusBarItem("Sök", counts.new, AccentGold),
        StatusBarItem("Bevakar", counts.watching, StatusWatchingBlue),
        StatusBarItem("Äger", counts.owned, PositiveGreen),
        StatusBarItem("Avslagen", counts.rejected, TextDim),
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { item ->
            val pct = if (allCount > 0) item.count.toFloat() / allCount else 0f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        text = item.count.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                        color = TextMutedMore,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CardBorderMuted),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(item.color),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCardsGrid(stats: StatsResponse) {
    val counts = stats.counts ?: StatusCounts()
    val cards = listOf(
        "Bevakar" to counts.watching.toString(),
        "Äger" to counts.owned.toString(),
        "Sök" to counts.new.toString(),
        "Delar ägda" to (stats.totalPiecesOwned?.toString() ?: "–"),
        "Samlingsvärde" to (stats.estimatedCollectionValueSek?.let { "%.0f kr".format(it) } ?: "–"),
        "Snitt kr/del (ägda)" to (stats.avgKrPerPieceOwned?.let { "%.2f".format(it) } ?: "–"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) -> StatCard(label, value, modifier = Modifier.weight(1f)) }
                if (row.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.Bold),
            color = AccentGold,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedMost,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ValueDistributionRow(dist: ValueDistribution) {
    if (dist.total == 0) {
        EmptyNote("Inga bevakade modeller med känt pris ännu.")
        return
    }
    val entries = listOf(
        Triple("cyan", dist.cyan, "kanon"),
        Triple("green", dist.green, "bra"),
        Triple("yellow", dist.yellow, "okej"),
        Triple("orange", dist.orange, "högt"),
        Triple("red", dist.red, "mycket högt"),
    )
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            entries.forEach { (rating, count, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorForValueRating(rating)),
                    )
                    Text(text = "$count $label", style = MaterialTheme.typography.bodySmall, color = TextMutedMore)
                }
            }
        }
        Text(
            text = "bland ${dist.total} bevakade med känt pris",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun DealRow(deal: StatsModelRef, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colorForValueRating(deal.bestValueRating)),
        )
        Text(
            text = listOfNotNull(deal.brand, deal.modelNumber?.let { "#$it" }).joinToString(" ") + " — " + (deal.name ?: "Namn ej angivet"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colorForValueRating(deal.bestValueRating).copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "%.2f kr/del".format(deal.bestKrPerPiece),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                color = colorForValueRating(deal.bestValueRating),
            )
        }
    }
}

@Composable
private fun BrandRow(brand: BrandBreakdown) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = brand.brand, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(text = "${brand.count} st", style = MaterialTheme.typography.bodySmall, color = TextMutedMore, modifier = Modifier.padding(end = 10.dp))
        Text(
            text = brand.avgKrPerPiece?.let { "%.2f kr/del snitt".format(it) } ?: "–",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
            color = TextMutedMost,
        )
    }
}

@Composable
private fun CategoryRow(category: CategoryBreakdown, onClick: () -> Unit) {
    val best = category.best
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .then(if (best != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (best != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colorForValueRating(best.bestValueRating)),
            )
        }
        val label = if (best != null) {
            "${category.label} (${category.count} st) — ${listOfNotNull(best.brand, best.modelNumber?.let { "#$it" }).joinToString(" ")}"
        } else {
            "${category.label} (${category.count} st)"
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        if (best != null) {
            Text(
                text = "%.2f kr/del".format(best.bestKrPerPiece),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                color = colorForValueRating(best.bestValueRating),
            )
        } else {
            Text(text = "–", style = MaterialTheme.typography.bodySmall, color = TextMutedMost)
        }
    }
}

@Composable
private fun LoadingBox(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AccentGold)
    }
}

@Composable
private fun ErrorBox(padding: PaddingValues, message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Button(onClick = onRetry) {
                Text("Försök igen")
            }
        }
    }
}
