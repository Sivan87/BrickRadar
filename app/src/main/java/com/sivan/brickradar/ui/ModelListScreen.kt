package com.sivan.brickradar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.Source
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.UNCATEGORIZED_KEY
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.AppPillChip
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
import com.sivan.brickradar.ui.theme.StatusWatchingBlue
import com.sivan.brickradar.ui.theme.TextDim
import com.sivan.brickradar.ui.theme.TextMuted
import com.sivan.brickradar.ui.theme.TextMutedMore
import com.sivan.brickradar.ui.theme.TextMutedMost
import com.sivan.brickradar.ui.theme.TextPrimary
import com.sivan.brickradar.ui.theme.TextSecondary
import com.sivan.brickradar.util.buildStatusColor
import com.sivan.brickradar.util.buildStatusLabel
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val modelCreated by modelCreatedFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Issue #22 i Sivan87/BrickRadar — sökfältet ska vara en expanderbar ikon,
    // inte alltid synligt. Expand/collapse är rent UI-tillstånd (samma mönster
    // som SortMenuButtons lokala `expanded`-flagga nedan) — själva söktexten
    // (searchQuery) lever kvar i ViewModel:en som innan, oförändrat.
    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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
            ListHeader(
                viewMode = viewMode,
                onViewModeChange = viewModel::setViewMode,
                searchExpanded = searchExpanded,
                onToggleSearch = {
                    if (searchExpanded) {
                        // Stänger via samma ikon (nu ett X) — rensar sökningen
                        // vid stängning för ett konsekvent, förutsägbart läge
                        // varje gång fältet öppnas på nytt.
                        searchExpanded = false
                        viewModel.setSearchQuery("")
                    } else {
                        searchExpanded = true
                    }
                },
            )
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    focusRequester = searchFocusRequester,
                )
            }
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
                    val displayedModels = state.models.filter { matchesSearch(it, searchQuery, categories) }
                    if (displayedModels.isEmpty()) {
                        EmptyResultView()
                    } else if (viewMode == ListViewMode.GRID) {
                        ModelGrid(models = displayedModels, onModelClick = onModelClick)
                    } else {
                        ModelList(models = displayedModels, categories = categories, onModelClick = onModelClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    viewMode: ListViewMode,
    onViewModeChange: (ListViewMode) -> Unit,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "BrickRadar",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searchExpanded) "Stäng sökning" else "Sök",
                    tint = TextMuted,
                )
            }
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

// Issue #21 i Sivan87/BrickRadar (mirroring mould-king-tracker issue #16) —
// centrerat sökfält högst upp i listvyn, ovanför status-/kategorifiltren.
// Issue #22: fältet visas numera bara när ListHeaders sökikon expanderat det
// (se ModelListScreen ovan) — `focusRequester` kopplar in den auto-fokusering
// som redan triggas därifrån.
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, focusRequester: FocusRequester) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
        placeholder = { Text("Sök på namn, märke, modellnummer eller kategori...") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
    )
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

@Composable
private fun PillFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    AppPillChip(selected = selected, label = label, onClick = onClick)
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
private fun ModelList(models: List<Model>, categories: List<Category>, onModelClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(models, key = { it.id }) { model ->
            ModelListRow(model = model, categories = categories, onClick = { onModelClick(model.id) })
        }
    }
}

// Fas 15 (issue #12): rutvyn ar nu ALLTID max 2 kolumner per rad, samma
// fasta grid-template-columns: repeat(2, 1fr) som webbens motsvarande fix --
// ersatter Fas 12/13:s breddberoende GridCells.Fixed(1..4) (BoxWithConstraints
// + GRID_CARD_MIN_WIDTH), som lat kolumnantalet vaxa pa surfplattor/breda
// skarmar. Medvetet forenklat: ingen bredddetektering behovs langre.
@Composable
private fun ModelGrid(models: List<Model>, onModelClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
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

// Design t13 (rond 13, Fas 13): 13b (telefon) hade redan en enda info-kolumn
// (namn/modellnummer/status/pris klumpade i mitten) -- 13a (surfplatta/bred
// skarm) breder istallet ut kategori/status i en EGEN kolumn, skild fran
// namn-kolumnen. BoxWithConstraints + samma 600dp-troskel som
// ModelDetailScreen.kt:s tvaspalts-lage vaxlar mellan de tva.
private val WIDE_LIST_ROW_MIN_WIDTH = 600.dp

// Fas 15 (issue #11): status visas nu som en fargad vansterkant pa raden --
// samma statusfarger som redan etablerats i StatistikScreen.kt:s StatusDistribution
// (Sok/AccentGold, Bevakar/StatusWatchingBlue, Ager/PositiveGreen, Avslagen/TextDim),
// har atervanda for konsekvens istallet for nya farger. Ritas som en smal
// fargad stripe langst till vanster i raden (Row+IntrinsicSize.Min sa stripen
// stracker sig till radens fulla hojd), inte en badge -- Android-listraden har
// aldrig haft nagon status-badge att ersatta, bara en ny indikator som saknades.
private fun statusStripeColor(status: String): Color = when (status) {
    "watching" -> StatusWatchingBlue
    "owned" -> PositiveGreen
    "rejected" -> TextDim
    else -> AccentGold
}

private fun statusLabel(status: String): String = when (status) {
    "watching" -> "Bevakar"
    "owned" -> "Äger"
    "rejected" -> "Avslagen"
    else -> "Sök"
}

@Composable
private fun ModelListRow(model: Model, categories: List<Category>, onClick: () -> Unit) {
    val hasPrice = model.bestKrPerPiece != null
    val highlighted = model.bestValueRating == "green" || model.bestValueRating == "cyan"
    val cheapest = cheapestSource(model.prices)
    BoxWithConstraints(
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
            .alpha(if (hasPrice) 1f else 0.72f),
    ) {
        val isWide = maxWidth >= WIDE_LIST_ROW_MIN_WIDTH
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(statusStripeColor(model.status)),
            )
            Box(modifier = Modifier.weight(1f).padding(10.dp)) {
                if (isWide) {
                    WideModelListRowContent(model = model, categories = categories, hasPrice = hasPrice, cheapest = cheapest)
                } else {
                    CompactModelListRowContent(model = model, hasPrice = hasPrice, cheapest = cheapest)
                }
            }
        }
    }
}

@Composable
private fun CompactModelListRowContent(model: Model, hasPrice: Boolean, cheapest: Source?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            BuildStatusInlineBadge(model = model)
        }
        PriceColumn(model = model, hasPrice = hasPrice, cheapest = cheapest)
        Text(text = "›", style = MaterialTheme.typography.titleLarge, color = TextMutedMost, modifier = Modifier.padding(start = 6.dp))
    }
}

// Issue #17 (mirroring mould-king-tracker issue #5) — kompakt byggstatus-rad
// för listvyn (bild+namn/kontext-layouten har ingen egen badge-yta som
// rutvyns bild-overlay, se ModelGridCard ovan), bara synlig när ett
// byggstatus-värde faktiskt är satt.
@Composable
private fun BuildStatusInlineBadge(model: Model) {
    val buildStatus = model.buildStatus ?: return
    Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildStatusLabel(buildStatus) ?: buildStatus,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = buildStatusColor(buildStatus),
        )
        if (model.missingPartsInactivity?.stale == true) {
            Text(
                text = " ⚠",
                style = MaterialTheme.typography.labelSmall,
                color = NegativeRed,
            )
        }
    }
}

// Design t13a: bild, "Modell" (namn+modellnummer/bitar) och "Kategori / status"
// far varsin kolumn -- till skillnad fran den smala varianten klumpas
// status-notisen inte langre ihop med namnet, den star under kategorietiketten.
@Composable
private fun WideModelListRowContent(model: Model, categories: List<Category>, hasPrice: Boolean, cheapest: Source?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
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
        Column(modifier = Modifier.weight(2.3f).padding(horizontal = 16.dp)) {
            Text(
                text = model.name ?: model.modelNumber,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(model.modelNumber.ifBlank { null }, model.pieceCount?.let { "$it bitar" })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Column(modifier = Modifier.weight(1.3f).padding(horizontal = 16.dp)) {
            Text(
                text = categoryLabel(model.category, categories),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contextNote(model),
                style = MaterialTheme.typography.bodySmall,
                color = if (model.priceTrend?.isAllTimeLow == true) PositiveGreen else TextMutedMore,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            BuildStatusInlineBadge(model = model)
        }
        PriceColumn(
            modifier = Modifier.weight(1f),
            model = model,
            hasPrice = hasPrice,
            cheapest = cheapest,
        )
        Text(text = "›", style = MaterialTheme.typography.titleLarge, color = TextMutedMost, modifier = Modifier.padding(start = 12.dp))
    }
}

// Totalpris ar det primara fokuset (stort), kr/del en liten sekundar PILL,
// fargad efter VALUE-RATING (cyan/gron/gul/orange/rod, colorForValueRating)
// -- ett explicit anvandarbeslut (2026-07-30): kr/del ska ALLTID visa
// prisniva-fargen i listvyn/detaljvyn/rutvyn, inte statusfargen. En tidigare
// version av denna pill anvande statusStripeColor (samma farg som radens
// vansterkant) for att matcha {{ it.statusColor }} i prototypfilen
// BrickRadar Listvy.dc.html -- men anvandaren rattade uttryckligen till att
// kr/del-fargen ska foljas prisniva-skalan overallt, inte prototypens
// statuskopplade pill. Vansterkanten (statusStripeColor) forblir statusfargad.
@Composable
private fun PriceColumn(model: Model, hasPrice: Boolean, cheapest: Source?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = if (cheapest != null) "%.0f kr".format(cheapest.totalPriceSek ?: cheapest.price ?: 0.0) else "— kr",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
            color = if (cheapest != null) TextPrimary else TextMutedMost,
        )
        if (hasPrice) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorForValueRating(model.bestValueRating))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "%.2f kr/del".format(model.bestKrPerPiece),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont, fontWeight = FontWeight.Bold),
                    color = AppBackground,
                )
            }
        }
    }
}

private fun categoryLabel(category: String?, categories: List<Category>): String {
    val key = category ?: UNCATEGORIZED_KEY
    return categories.firstOrNull { it.category == key }?.label
        ?: key.replaceFirstChar { it.uppercase() }
}

// Matchar mot namn, märke, modellnummer och kategorietikett — case-
// insensitive delsträngsmatchning, samma fyra fält som webbens getDisplayModels
// (mould-king-tracker/static/app.js).
private fun matchesSearch(model: Model, query: String, categories: List<Category>): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    val haystack = listOfNotNull(model.name, model.brand, model.modelNumber, categoryLabel(model.category, categories))
        .joinToString(" ")
        .lowercase()
    return haystack.contains(q)
}

// Fas 16 (issue #12, uppfoljning): jamfort direkt mot BrickRadar Rutvy
// (nuvarande).dc.html -- foljande strukturella element saknades helt i den
// forra versionen av detta kort och ar tillagda har for att verkligen matcha
// prototypen, inte bara prisfokus-bytet:
// - Statuspill uppe till vanster OVER bilden (samma statusfarg som listradens
//   vansterkant, se statusStripeColor/statusLabel).
// - Totalpris ligger nu i en gradient-scrim OVANPA bilden (som prototypens
//   .rv-media-overlay), fargad AccentGold (prototypens #ffce45) -- INTE under
//   bilden i vitt som forra versionen gjorde.
// - Marke (litet, guld, versaler) mellan bild och namn.
// - Antal delar som egen rad.
// - Kontextrad (contextNote, samma text/logik som redan anvands i listvyn).
// - En riktig footer-rad ("Billigast av N" + kallnamn vanster, kr/del hogern,
//   fargad efter value-rating) skild fran resten av kortet med en tunn linje,
//   matchar prototypens .rv-footer exakt.
@Composable
private fun ModelGridCard(model: Model, onClick: () -> Unit) {
    val hasPrice = model.bestKrPerPiece != null
    val highlighted = model.bestValueRating == "green" || model.bestValueRating == "cyan"
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
            Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusStripeColor(model.status))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = statusLabel(model.status).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppBackground,
                    )
                }
                // Issue #17 (mirroring mould-king-tracker issue #5) — byggstatus-
                // badge, bara synlig när ett värde faktiskt är satt (kräver
                // status == "owned" server-side, se util/BuildStatus.kt).
                model.buildStatus?.let { buildStatus ->
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(buildStatusColor(buildStatus))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buildStatusLabel(buildStatus)?.uppercase() ?: buildStatus.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppBackground,
                            )
                            if (model.missingPartsInactivity?.stale == true) {
                                Text(text = " ⚠", style = MaterialTheme.typography.labelSmall, color = AppBackground)
                            }
                        }
                    }
                }
            }
            model.priceTrend?.pct?.let { pct ->
                TrendBadge(pct = pct, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ScreenBackground.copy(alpha = 0.92f))))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = "TOTALPRIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = if (cheapest != null) "%.0f kr".format(cheapest.totalPriceSek ?: cheapest.price ?: 0.0) else "— kr",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFont),
                        color = if (cheapest != null) AccentGold else TextMutedMost,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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
                text = (model.brand ?: "Mould King").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentGold,
            )
            Text(
                text = model.name ?: model.modelNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            model.pieceCount?.let {
                Text(
                    text = "$it delar",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = TextMutedMore,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = contextNote(model),
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedMore,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = CardBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (cheapest != null) {
                    Column {
                        Text(
                            text = "Billigast av ${model.prices.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedMore,
                        )
                        Text(
                            text = cheapest.source,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = "Inga priser hittade · söker",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                        color = TextMutedMore,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hasPrice) {
                    Text(
                        text = "%.2f kr/del".format(model.bestKrPerPiece),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.Bold),
                        color = colorForValueRating(model.bestValueRating),
                    )
                }
            }
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
