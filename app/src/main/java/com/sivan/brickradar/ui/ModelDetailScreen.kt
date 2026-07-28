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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sivan.brickradar.model.COUNTRIES
import com.sivan.brickradar.model.CURRENCIES
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.Source
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.UNCATEGORIZED_KEY
import com.sivan.brickradar.model.flagForWarehouse
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.CardBackground
import com.sivan.brickradar.ui.theme.CardBorder
import com.sivan.brickradar.ui.theme.CardBorderMuted
import com.sivan.brickradar.ui.theme.HighlightBorder
import com.sivan.brickradar.ui.theme.HighlightCardBackground
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
import com.sivan.brickradar.util.classifyValue
import com.sivan.brickradar.util.colorForValueRating
import com.sivan.brickradar.util.valueLevelsFor
import com.sivan.brickradar.viewmodel.ModelDetailEvent
import com.sivan.brickradar.viewmodel.ModelDetailUiState
import com.sivan.brickradar.viewmodel.ModelDetailViewModel

private val STATUS_OPTIONS = listOf(
    "new" to "Sök",
    "watching" to "Bevakar",
    "owned" to "Äger",
    "rejected" to "Avslagen",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailScreen(
    modelId: Int,
    onDeleted: () -> Unit,
    viewModel: ModelDetailViewModel = viewModel(),
) {
    LaunchedEffect(modelId) {
        viewModel.loadModel(modelId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<Source?>(null) }
    var pendingDeleteSource by remember { mutableStateOf<Source?>(null) }
    var pendingDeleteModel by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ModelDetailEvent.Saved -> {
                    if (event.isEditSave) isEditing = false
                    snackbarHostState.showSnackbar(event.message)
                }
                is ModelDetailEvent.SourceSaved -> {
                    showSourceSheet = false
                    editingSource = null
                    snackbarHostState.showSnackbar(event.message)
                }
                is ModelDetailEvent.Failed -> snackbarHostState.showSnackbar(event.message)
                is ModelDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    val loadedState = uiState as? ModelDetailUiState.Loaded

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = loadedState?.model?.let { it.name ?: it.modelNumber } ?: "Modell",
                        color = TextPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBackground),
                actions = {
                    if (loadedState != null) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Avbryt redigering" else "Redigera",
                                tint = TextMuted,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ModelDetailUiState.Loading -> LoadingBox()
                is ModelDetailUiState.Error -> ErrorBox(message = state.message, onRetry = { viewModel.loadModel(modelId) })
                is ModelDetailUiState.Loaded -> {
                    if (isEditing) {
                        EditableModelDetail(
                            model = state.model,
                            categories = categories,
                            isSaving = state.isSavingEdit,
                            onSave = { name, pieceCount, category -> viewModel.updateModel(name, pieceCount, category) },
                            onCancel = { isEditing = false },
                        )
                    } else {
                        ModelDetail(
                            model = state.model,
                            categories = categories,
                            stats = stats,
                            isUpdatingStatus = state.isUpdatingStatus,
                            deletingSourceId = state.deletingSourceId,
                            onStatusSelected = { viewModel.updateStatus(it) },
                            onAddSourceClick = {
                                editingSource = null
                                showSourceSheet = true
                            },
                            onEditSourceClick = {
                                editingSource = it
                                showSourceSheet = true
                            },
                            onDeleteSourceClick = { pendingDeleteSource = it },
                            onMoveToWatching = { viewModel.updateStatus("watching") },
                            onReject = { viewModel.updateStatus("rejected") },
                            onDeleteModelClick = { pendingDeleteModel = true },
                        )
                    }
                }
            }
        }

        if (showSourceSheet) {
            val isSavingSource = (uiState as? ModelDetailUiState.Loaded)?.isSavingSource ?: false
            SourceFormSheet(
                editingSource = editingSource,
                isSaving = isSavingSource,
                onDismiss = {
                    if (!isSavingSource) {
                        showSourceSheet = false
                        editingSource = null
                    }
                },
                onSubmit = { name, price, currency, url, inStock, warehouse, deliveryEstimate ->
                    val target = editingSource
                    if (target == null) {
                        viewModel.addSource(name, price, currency, url, inStock, warehouse, deliveryEstimate)
                    } else {
                        viewModel.updateSource(target.id, target.source, price, currency, url, inStock, warehouse, deliveryEstimate)
                    }
                },
            )
        }

        pendingDeleteSource?.let { source ->
            DeleteSourceDialog(
                source = source,
                onConfirm = {
                    viewModel.deleteSource(source.id)
                    pendingDeleteSource = null
                },
                onDismiss = { pendingDeleteSource = null },
            )
        }

        if (pendingDeleteModel) {
            val isDeleting = (uiState as? ModelDetailUiState.Loaded)?.isDeletingModel ?: false
            DeleteModelDialog(
                isDeleting = isDeleting,
                onConfirm = { viewModel.deleteModel() },
                onDismiss = { if (!isDeleting) pendingDeleteModel = false },
            )
        }
    }
}

@Composable
private fun ModelDetail(
    model: Model,
    categories: List<Category>,
    stats: StatsResponse?,
    isUpdatingStatus: Boolean,
    deletingSourceId: Int?,
    onStatusSelected: (String) -> Unit,
    onAddSourceClick: () -> Unit,
    onEditSourceClick: (Source) -> Unit,
    onDeleteSourceClick: (Source) -> Unit,
    onMoveToWatching: () -> Unit,
    onReject: () -> Unit,
    onDeleteModelClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            item {
                HeroSection(model = model)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatusChipsRow(currentStatus = model.status, isUpdating = isUpdatingStatus, onStatusSelected = onStatusSelected)
                    ValueScaleSection(model = model, stats = stats)
                    SourcesSection(
                        model = model,
                        deletingSourceId = deletingSourceId,
                        onEdit = onEditSourceClick,
                        onDeleteRequest = onDeleteSourceClick,
                    )
                    PriceHistorySection()
                    ToolsSection(onAddSourceClick = onAddSourceClick)
                    FactsSection(model = model, categories = categories)
                }
            }
        }
        if (model.status == "new") {
            BottomActionBar(onMoveToWatching = onMoveToWatching, onReject = onReject, onDelete = onDeleteModelClick)
        }
    }
}

@Composable
private fun HeroSection(model: Model) {
    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ImagePlaceholder),
            contentAlignment = Alignment.Center,
        ) {
            if (model.imageUrl != null) {
                AsyncImage(
                    model = model.imageUrl,
                    contentDescription = model.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = listOfNotNull(model.brand, model.modelNumber.ifBlank { null }, model.releaseYear?.toString())
                .joinToString(" · "),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
            color = TextMutedMore,
        )
        Text(
            text = model.name ?: model.modelNumber,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        model.pieceCount?.let {
            Text(
                text = "$it bitar",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedMore,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 14.dp)) {
            Text(text = "VÄRDE / BIT", style = MaterialTheme.typography.labelSmall, color = TextMutedMore)
            Text(
                text = model.bestKrPerPiece?.let { "%.2f kr".format(it) } ?: "— kr",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFont),
                color = if (model.bestKrPerPiece != null) AccentGold else TextMutedMost,
            )
        }
    }
}

@Composable
private fun StatusChipsRow(currentStatus: String, isUpdating: Boolean, onStatusSelected: (String) -> Unit) {
    SectionLabel("STATUS")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        STATUS_OPTIONS.forEach { (key, label) ->
            DetailPillChip(
                selected = key == currentStatus,
                label = label,
                enabled = !isUpdating,
                onClick = { onStatusSelected(key) },
            )
        }
        if (isUpdating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentGold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailPillChip(selected: Boolean, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        enabled = enabled,
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
            enabled = enabled,
            selected = selected,
            borderColor = CardBorder,
            selectedBorderColor = AccentGold,
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMutedMore,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

// Bakgrundens färgband speglar de FASTA trösklarna (cyan/grön/gul/orange/röd
// för klon, grön/gul/orange/röd för LEGO — se util/ValueRating.kt) istället
// för en generisk 3-stopps grön→guld→röd-gradient utsträckt mellan vilka tre
// värden som råkar finnas (det gamla beteendet, se issue #4 i Sivan87/
// BrickRadar: "Snitt baren ... matchar inte färgen brick per kr enligt
// modellen" — samma set kunde visuellt hamna i en annan färgzon beroende på
// vad Klonsnitt/LEGO-snitt just då var, trots att kr/del-chippen på källrader/
// listkort redan alltid färgas efter samma fasta trösklar). Domänen (0 till
// maxV) skalas för att rymma både de faktiska värdena OCH hela den tillämpliga
// stegen, så bandgränserna alltid representerar de RIKTIGA tröskelvärdena i
// kr/del, inte en godtycklig andel av bredden.
@Composable
private fun ValueScaleSection(model: Model, stats: StatsResponse?) {
    val thisValue = model.bestKrPerPiece ?: return
    val cloneAvg = stats?.avgKrPerPieceCloneAll
    val legoAvg = stats?.avgKrPerPieceLegoAll
    val isOfficial = model.isOfficialSet
    val levels = valueLevelsFor(isOfficial)
    val values = listOfNotNull(thisValue, cloneAvg, legoAvg)
    val maxV = maxOf(values.max() * 1.15, levels.last().first * 1.15)

    fun position(value: Double): Float = (value / maxV).coerceIn(0.0, 1.0).toFloat()

    val tierBoundaries = levels.map { it.first } + listOf(maxV)
    val tierColors = levels.map { colorForValueRating(it.second) } + listOf(colorForValueRating("red"))
    val colorStops = mutableListOf<Pair<Float, Color>>()
    var prevBoundary = 0.0
    tierBoundaries.forEachIndexed { i, boundary ->
        val color = tierColors[i]
        colorStops.add((prevBoundary / maxV).toFloat() to color)
        colorStops.add((boundary / maxV).toFloat() to color)
        prevBoundary = boundary
    }
    val thisValueColor = colorForValueRating(model.bestValueRating ?: classifyValue(thisValue, isOfficial))

    Column {
        SectionLabel("VÄRDESKALA")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(26.dp)) {
            val barWidth = maxWidth
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(colorStops = colorStops.toTypedArray())),
            )
            cloneAvg?.let { v ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = barWidth * position(v) - 1.dp)
                        .width(2.dp)
                        .height(21.dp)
                        .background(Color.White.copy(alpha = 0.7f)),
                )
            }
            legoAvg?.let { v ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = barWidth * position(v) - 1.dp)
                        .width(2.dp)
                        .height(21.dp)
                        .background(Color.White.copy(alpha = 0.38f)),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = barWidth * position(thisValue) - 6.dp)
                    .width(12.dp)
                    .height(25.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(thisValueColor),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Klonsnitt ${cloneAvg?.let { "%.2f".format(it) } ?: "–"}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
            )
            Text(
                text = "Detta set %.2f".format(thisValue),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = thisValueColor,
            )
            Text(
                text = "LEGO-snitt ${legoAvg?.let { "%.2f".format(it) } ?: "–"}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
            )
        }
    }
}

@Composable
private fun SourcesSection(
    model: Model,
    deletingSourceId: Int?,
    onEdit: (Source) -> Unit,
    onDeleteRequest: (Source) -> Unit,
) {
    SectionLabel("${model.prices.size} KÄLLOR")
    if (model.prices.isEmpty()) {
        Text(text = "Inga källor ännu", style = MaterialTheme.typography.bodyMedium, color = TextMutedMore)
        return
    }
    val cheapestId = model.prices.minByOrNull { it.totalPriceSek ?: it.price ?: Double.MAX_VALUE }?.id
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.prices.forEach { source ->
            SourceRow(
                source = source,
                highlighted = source.id == cheapestId,
                isDeleting = deletingSourceId == source.id,
                onEdit = { onEdit(source) },
                onDeleteRequest = { onDeleteRequest(source) },
            )
        }
    }
}

@Composable
private fun SourceRow(
    source: Source,
    highlighted: Boolean,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) HighlightCardBackground else CardBackground)
            .border(
                width = 1.dp,
                color = if (highlighted) HighlightBorder else CardBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                flagForWarehouse(source.warehouse)?.let {
                    Text(text = it, modifier = Modifier.padding(end = 4.dp))
                }
                Text(text = source.source, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }
            val stockText = when (source.inStock) {
                1 -> "i lager"
                0 -> "slut i lager"
                else -> "lager okänd"
            }
            Text(
                text = listOfNotNull(stockText, source.deliveryEstimate).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = if (source.inStock == 1) PositiveGreen else TextMutedMost,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Text(
            text = source.totalPriceSek?.let { "%.2f kr".format(it) }
                ?: "${source.price?.let { "%.2f".format(it) } ?: "-"} ${source.currency.orEmpty()}".trim(),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFont),
            color = if (highlighted) AccentGold else TextSecondary,
            modifier = Modifier.padding(end = 10.dp),
        )
        source.krPerPiece?.let {
            Text(
                text = "%.2f".format(it),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFont),
                color = if (highlighted) AccentGold else TextMutedMore,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentGold)
        } else {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Redigera källa", tint = TextMutedMore, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDeleteRequest, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Ta bort källa", tint = TextMutedMore, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PriceHistorySection() {
    SectionLabel("PRISHISTORIK")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = CardBorderMuted, shape = RoundedCornerShape(12.dp))
            .background(PanelBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Ingen prishistorik ännu", style = MaterialTheme.typography.bodyMedium, color = TextMutedMost)
    }
}

@Composable
private fun ToolsSection(onAddSourceClick: () -> Unit) {
    SectionLabel("VERKTYG")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onAddSourceClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TextMuted)
        Text(
            text = "Lägg till pris manuellt",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun FactsSection(model: Model, categories: List<Category>) {
    SectionLabel("FAKTA")
    val categoryLabel = categories.firstOrNull { it.category == model.category }?.label ?: model.category ?: "Okategoriserad"
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        FactRow(label = "Bitar", value = model.pieceCount?.toString() ?: "-")
        FactRow(label = "Årsmodell", value = model.releaseYear?.toString() ?: "-")
        FactRow(label = "Kategori", value = categoryLabel)
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMutedMore)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun BottomActionBar(onMoveToWatching: () -> Unit, onReject: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBackground)
            .border(width = 1.dp, color = CardBorderMuted)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PositiveGreen.copy(alpha = 0.08f))
                .border(width = 1.dp, color = PositiveGreen.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                .clickable(onClick = onMoveToWatching),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Flytta till bevakning", style = MaterialTheme.typography.titleSmall, color = PositiveGreen)
        }
        TextButton(onClick = onReject) {
            Text(text = "Avslå", color = TextMuted)
        }
        TextButton(onClick = onDelete) {
            Text(text = "Ta bort", color = NegativeRed)
        }
    }
}

@Composable
private fun EditableModelDetail(
    model: Model,
    categories: List<Category>,
    isSaving: Boolean,
    onSave: (name: String, pieceCount: Int, category: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(model.id) { mutableStateOf(model.name ?: "") }
    var pieceCountText by remember(model.id) { mutableStateOf(model.pieceCount?.toString() ?: "") }
    var selectedCategory by remember(model.id) { mutableStateOf(model.category ?: UNCATEGORIZED_KEY) }

    val nameError = name.isBlank()
    val pieceCount = pieceCountText.toIntOrNull()
    val pieceCountError = pieceCount == null || pieceCount <= 0
    val canSave = !nameError && !pieceCountError && !isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Namn") },
            isError = nameError,
            supportingText = { if (nameError) Text("Namn får inte vara tomt") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pieceCountText,
            onValueChange = { input -> pieceCountText = input.filter { it.isDigit() } },
            label = { Text("Delantal") },
            isError = pieceCountError,
            supportingText = { if (pieceCountError) Text("Delantal måste vara ett positivt heltal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Kategori", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                DetailPillChip(
                    selected = selectedCategory == category.category,
                    label = category.label,
                    onClick = { selectedCategory = category.category },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { pieceCount?.let { onSave(name.trim(), it, selectedCategory) } },
                enabled = canSave,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Spara")
                }
            }
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text("Avbryt")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceFormSheet(
    editingSource: Source?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        source: String,
        price: Double,
        currency: String,
        url: String,
        inStock: Boolean,
        warehouse: String?,
        deliveryEstimate: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sourceName by remember(editingSource) { mutableStateOf(editingSource?.source ?: "") }
    var priceText by remember(editingSource) { mutableStateOf(editingSource?.price?.toString() ?: "") }
    var currency by remember(editingSource) { mutableStateOf(editingSource?.currency ?: "SEK") }
    var url by remember(editingSource) { mutableStateOf(editingSource?.url ?: "") }
    var inStock by remember(editingSource) { mutableStateOf(editingSource?.inStock != 0) }
    var warehouse by remember(editingSource) { mutableStateOf(editingSource?.warehouse) }
    var deliveryEstimate by remember(editingSource) { mutableStateOf(editingSource?.deliveryEstimate ?: "") }

    val price = priceText.toDoubleOrNull()
    val priceError = price == null || price <= 0
    val nameError = editingSource == null && sourceName.isBlank()
    val urlError = url.isBlank()
    val canSave = !priceError && !nameError && !urlError && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = PanelBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (editingSource == null) "Lägg till källa" else "Redigera källa",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (editingSource == null) {
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("Butik/källa") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Butik/källa får inte vara tomt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text(text = sourceName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                isError = urlError,
                supportingText = { if (urlError) Text("URL får inte vara tom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Pris") },
                isError = priceError,
                supportingText = { if (priceError) Text("Pris måste vara ett positivt tal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Valuta", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CURRENCIES.forEach { c ->
                    DetailPillChip(selected = currency == c, label = c, onClick = { currency = c })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "I lager", style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = inStock, onCheckedChange = { inStock = it })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Lagerland", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                COUNTRIES.forEach { country ->
                    DetailPillChip(
                        selected = warehouse == country.code,
                        label = "${country.flagEmoji} ${country.displayName}",
                        onClick = { warehouse = if (warehouse == country.code) null else country.code },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = deliveryEstimate,
                onValueChange = { deliveryEstimate = it },
                label = { Text("Leveranstid") },
                placeholder = { Text("t.ex. 2-3 veckor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val currentPrice = price
                    if (currentPrice != null) {
                        onSubmit(
                            sourceName.trim(), currentPrice, currency, url.trim(), inStock,
                            warehouse, deliveryEstimate.trim().ifBlank { null },
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Spara")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeleteSourceDialog(
    source: Source,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ta bort källa") },
        text = { Text("Ta bort källan från ${source.source}?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ta bort", color = NegativeRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}

@Composable
private fun DeleteModelDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ta bort modell") },
        text = { Text("Ta bort modellen permanent, inklusive alla dess källor och prishistorik? Det går inte att ångra.") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NegativeRed)
                } else {
                    Text("Ta bort", color = NegativeRed)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Avbryt") }
        },
    )
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentGold)
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Button(onClick = onRetry) {
                Text("Försök igen")
            }
        }
    }
}
