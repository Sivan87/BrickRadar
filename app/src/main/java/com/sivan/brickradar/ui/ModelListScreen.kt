package com.sivan.brickradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.Source
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.CardBackground
import com.sivan.brickradar.ui.theme.CardBorder
import com.sivan.brickradar.ui.theme.CardBorderMuted
import com.sivan.brickradar.ui.theme.HighlightBorder
import com.sivan.brickradar.ui.theme.HighlightCardBackground
import com.sivan.brickradar.ui.theme.HomeIndicator
import com.sivan.brickradar.ui.theme.ImagePlaceholder
import com.sivan.brickradar.ui.theme.MonoFont
import com.sivan.brickradar.ui.theme.NegativeRed
import com.sivan.brickradar.ui.theme.PanelBackground
import com.sivan.brickradar.ui.theme.PositiveGreen
import com.sivan.brickradar.ui.theme.ScreenBackground
import com.sivan.brickradar.ui.theme.TextMuted
import com.sivan.brickradar.ui.theme.TextMutedMore
import com.sivan.brickradar.ui.theme.TextMutedMost
import com.sivan.brickradar.ui.theme.TextPrimary
import com.sivan.brickradar.ui.theme.TextSecondary
import com.sivan.brickradar.util.colorForValueRating
import com.sivan.brickradar.viewmodel.ListViewMode
import com.sivan.brickradar.viewmodel.ModelListFilters
import com.sivan.brickradar.viewmodel.ModelListUiState
import com.sivan.brickradar.viewmodel.ModelListViewModel
import com.sivan.brickradar.viewmodel.SortOption
import kotlinx.coroutines.flow.StateFlow

private val STATUS_FILTER_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Alla",
    "new" to "Sök",
    "watching" to "Bevakar",
    "owned" to "Äger",
    "rejected" to "Avslagen",
)

private val SORT_OPTIONS: List<Pair<SortOption, String>> = listOf(
    SortOption.DEFAULT to "Standard",
    SortOption.KR_ASC to "Kr/del (lägst-högst)",
    SortOption.RECENT_DESC to "Senast ändrad (nyast)",
    SortOption.NAME_ASC to "Namn (A-Ö)",
)

@Composable
fun ModelListScreen(
    onModelClick: (Int) -> Unit,
    onAddModelClick: () -> Unit,
    onStatistikClick: () -> Unit,
    // Signal från "Lägg till modell" (Fas 6) via NavBackStackEntry.savedStateHandle
    // (satt av MainActivity) — true precis efter en lyckad skapelse. Konsumeras
    // (sätts tillbaka till false) av onModelCreatedConsumed så den inte triggar
    // om vid t.ex. skärmrotation.
    modelCreatedFlow: StateFlow<Boolean>,
    onModelCreatedConsumed: () -> Unit,
    viewModel: ModelListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val modelCreated by modelCreatedFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(modelCreated) {
        if (modelCreated) {
            onModelCreatedConsumed()
            viewModel.loadModels()
            viewModel.loadStats()
            snackbarHostState.showSnackbar("Modellen har lagts till")
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNavBar(onAddModelClick = onAddModelClick, onStatistikClick = onStatistikClick) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListHeader(viewMode = viewMode, onViewModeChange = viewModel::setViewMode)
            FilterBar(
                filters = filters,
                categories = categories,
                stats = stats,
                onStatusSelected = viewModel::setStatusFilter,
                onCategorySelected = viewModel::setCategoryFilter,
                onSortSelected = viewModel::setSortOption,
            )
            when (val state = uiState) {
                is ModelListUiState.Loading -> LoadingView()
                is ModelListUiState.Error -> ErrorView(message = state.message, onRetry = viewModel::loadModels)
                is ModelListUiState.Loaded -> {
                    if (state.isRefreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentGold)
                    }
                    if (state.models.isEmpty()) {
                        EmptyResultView()
                    } else if (viewMode == ListViewMode.GRID) {
                        ModelGrid(models = state.models, onModelClick = onModelClick)
                    } else {
                        ModelList(models = state.models, onModelClick = onModelClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(viewMode: ListViewMode, onViewModeChange: (ListViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "BrickRadar", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardBorder)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ViewModeToggleButton(
                selected = viewMode == ListViewMode.LIST,
                glyph = "≡",
                description = "Listvy",
                onClick = { onViewModeChange(ListViewMode.LIST) },
            )
            ViewModeToggleButton(
                selected = viewMode == ListViewMode.GRID,
                glyph = "▦",
                description = "Rutvy",
                onClick = { onViewModeChange(ListViewMode.GRID) },
            )
        }
    }
}

@Composable
private fun ViewModeToggleButton(selected: Boolean, glyph: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) AccentGold else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) AppBackground else TextMuted,
        )
    }
}

@Composable
private fun FilterBar(
    filters: ModelListFilters,
    categories: List<Category>,
    stats: StatsResponse?,
    onStatusSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (SortOption) -> Unit,
) {
    val counts = stats?.counts
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                STATUS_FILTER_OPTIONS.forEach { (key, label) ->
                    val count = statusCount(key, counts)
                    PillFilterChip(
                        selected = filters.status == key,
                        label = if (count != null) "$label $count" else label,
                        onClick = { onStatusSelected(key) },
                    )
                }
            }
            SortMenuButton(selectedSort = filters.sort, onSortSelected = onSortSelected)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PillFilterChip(
                selected = filters.category == null,
                label = "Alla kategorier",
                onClick = { onCategorySelected(null) },
            )
            categories.forEach { category ->
                PillFilterChip(
                    selected = filters.category == category.category,
                    label = category.label,
                    onClick = { onCategorySelected(category.category) },
                )
            }
        }
    }
}

private fun statusCount(key: String?, counts: com.sivan.brickradar.model.StatusCounts?): Int? {
    if (counts == null) return null
    return when (key) {
        null -> counts.new + counts.watching + counts.owned + counts.rejected
        "new" -> counts.new
        "watching" -> counts.watching
        "owned" -> counts.owned
        "rejected" -> counts.rejected
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = TextMuted,
            selectedContainerColor = AccentGold,
            selectedLabelColor = AppBackground,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CardBorder,
            selectedBorderColor = AccentGold,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenuButton(selectedSort: SortOption, onSortSelected: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Sortera", tint = TextMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SORT_OPTIONS.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selectedSort) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun ModelList(models: List<Model>, onModelClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(models, key = { it.id }) { model ->
            ModelListRow(model = model, onClick = { onModelClick(model.id) })
        }
    }
}

// Fas 12 (design t11b/t11d) hade auto-fill (GridCells.Adaptive) har, sa
// kolumnantalet vaxte obegransat med bredden -- det strackte korten/bilderna
// pa breda skarmar/surfplattor istallet for att bara visa fler fasta kolumner.
// Ersatt (issue #10) med BoxWithConstraints + GridCells.Fixed nedan, samma
// GRID_CARD_MIN_WIDTH men kolumnantalet klamras nu till max 4.
private val GRID_CARD_MIN_WIDTH = 160.dp

@Composable
private fun ModelGrid(models: List<Model>, onModelClick: (Int) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = (maxWidth / GRID_CARD_MIN_WIDTH).toInt().coerceIn(1, 4)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(models, key = { it.id }) { model ->
                ModelGridCard(model = model, onClick = { onModelClick(model.id) })
            }
        }
    }
}

@Composable
private fun ModelListRow(model: Model, onClick: () -> Unit) {
    val hasPrice = model.bestKrPerPiece != null
    val highlighted = model.bestValueRating == "green" || model.bestValueRating == "cyan"
    val cheapest = cheapestSource(model.prices)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) HighlightCardBackground else CardBackground)
            .border(
                width = 1.dp,
                color = if (highlighted) HighlightBorder else if (hasPrice) CardBorder else CardBorderMuted,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .alpha(if (hasPrice) 1f else 0.72f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))) {
            if (model.imageUrl != null) {
                AsyncImage(
                    model = model.imageUrl,
                    contentDescription = model.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(ImagePlaceholder))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = listOfNotNull(model.modelNumber.ifBlank { null }, model.pieceCount?.let { "$it bitar" })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
            )
            Text(
                text = model.name ?: model.modelNumber,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = contextNote(model),
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedMore,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (hasPrice) "%.2f kr".format(model.bestKrPerPiece) else "— kr",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                color = if (hasPrice) AccentGold else TextMutedMost,
            )
            if (cheapest != null) {
                Text(
                    text = "%.0f kr · %s".format(cheapest.totalPriceSek ?: cheapest.price ?: 0.0, cheapest.source),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = if (highlighted) PositiveGreen else TextMutedMore,
                )
            }
        }
        Text(text = "›", style = MaterialTheme.typography.titleLarge, color = TextMutedMost, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ModelGridCard(model: Model, onClick: () -> Unit) {
    val hasPrice = model.bestKrPerPiece != null
    val highlighted = model.bestValueRating == "green" || model.bestValueRating == "cyan"
    // Fas 12 (audit av t1-t10, rond 10b) -- rutkortets botten-rad visar
    // billigaste kallans pris+namn ("399 kr · YWOBB"), inte market -- market
    // visas redan pa listkortet (meta-raden) sa det ar ingen forlorad info,
    // bara ratt falt pa ratt kort.
    val cheapest = cheapestSource(model.prices)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) HighlightCardBackground else CardBackground)
            .border(
                width = 1.dp,
                color = if (highlighted) HighlightBorder else if (hasPrice) CardBorder else CardBorderMuted,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .alpha(if (hasPrice) 1f else 0.72f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
        ) {
            if (model.imageUrl != null) {
                AsyncImage(
                    model = model.imageUrl,
                    contentDescription = model.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(ImagePlaceholder))
            }
            model.priceTrend?.pct?.let { pct ->
                TrendBadge(pct = pct, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            if (hasPrice) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colorForValueRating(model.bestValueRating ?: "red")),
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (hasPrice) "%.2f kr".format(model.bestKrPerPiece) else "— kr",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFont),
                color = if (hasPrice) AccentGold else TextMutedMost,
            )
            Text(
                text = "PER BIT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedMore,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = model.name ?: model.modelNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = if (cheapest != null) {
                    "%.0f kr · %s".format(cheapest.totalPriceSek ?: cheapest.price ?: 0.0, cheapest.source)
                } else {
                    "Inga priser hittade · söker"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// Fas 12 -- ersatter tidigare DeltaPill, som lag inline i prisscrimmet.
// Design (rond 7a/10a/10b) visar trenden som en egen halvgenomskinlig pill
// uppe till vanster over kortbilden, aldrig bredvid sjalva priset.
@Composable
private fun TrendBadge(pct: Double, modifier: Modifier = Modifier) {
    val (color, arrow) = when {
        pct < -0.5 -> PositiveGreen to "▼"
        pct > 0.5 -> NegativeRed to "▲"
        else -> TextMuted to "±"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ScreenBackground.copy(alpha = 0.78f))
            .border(width = 1.dp, color = color.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "$arrow ${"%.0f".format(kotlin.math.abs(pct))} %",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private fun cheapestSource(sources: List<Source>): Source? =
    sources.minByOrNull { it.totalPriceSek ?: it.price ?: Double.MAX_VALUE }

private fun contextNote(model: Model): String {
    val trend = model.priceTrend
    return when {
        model.bestKrPerPiece == null -> "Inga priser hittade · söker"
        trend?.isAllTimeLow == true && model.targetPrice != null ->
            "Bevakningspris ${model.targetPrice.toInt()} kr · lägsta någonsin"
        trend?.isAllTimeLow == true -> "Lägsta pris någonsin"
        model.targetPrice != null -> "Bevakningspris ${model.targetPrice.toInt()} kr"
        trend?.daysSinceChange != null -> "Bevakar · inget prisfall sedan ${trend.daysSinceChange} dagar"
        else -> "Bevakar"
    }
}

@Composable
private fun BottomNavBar(onAddModelClick: () -> Unit, onStatistikClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBackground)
            .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            BottomNavItem(glyph = "◎", label = "Sets", active = true, enabled = true, onClick = {})
            BottomNavItem(glyph = "+", label = "Lägg till", active = false, enabled = true, onClick = onAddModelClick)
            BottomNavItem(glyph = "⟳", label = "Statistik", active = false, enabled = true, onClick = onStatistikClick)
            BottomNavItem(glyph = "≡", label = "Mer", active = false, enabled = false, onClick = {})
        }
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 112.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(HomeIndicator),
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    glyph: String,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = when {
        active -> AccentGold
        enabled -> TextMutedMore
        else -> TextMutedMost.copy(alpha = 0.5f)
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.titleMedium, color = tint)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentGold)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Button(onClick = onRetry) {
                Text("Försök igen")
            }
        }
    }
}

@Composable
private fun EmptyResultView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Inga modeller matchar valda filter",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMutedMore,
            modifier = Modifier.padding(24.dp),
        )
    }
}
