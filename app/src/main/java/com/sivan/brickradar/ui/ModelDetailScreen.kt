package com.sivan.brickradar.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sivan.brickradar.model.COUNTRIES
import com.sivan.brickradar.model.CURRENCIES
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.MissingPart
import com.sivan.brickradar.model.MissingPartsResponse
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.Receipt
import com.sivan.brickradar.model.Source
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.model.UNCATEGORIZED_KEY
import com.sivan.brickradar.model.flagForWarehouse
import com.sivan.brickradar.network.ApiConfig
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AccentGoldLink
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.AppPillChip
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
import com.sivan.brickradar.util.BUILD_STATUS_OPTIONS
import com.sivan.brickradar.util.classifyValue
import com.sivan.brickradar.util.colorForValueRating
import com.sivan.brickradar.util.createCameraCaptureUri
import com.sivan.brickradar.util.uriToMultipartPart
import com.sivan.brickradar.util.valueLevelsFor
import com.sivan.brickradar.viewmodel.ModelDetailEvent
import com.sivan.brickradar.viewmodel.ModelDetailUiState
import com.sivan.brickradar.viewmodel.ModelDetailViewModel
import okhttp3.MultipartBody

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
    var pendingDeleteBuildPhoto by remember { mutableStateOf(false) }
    var showMissingPartsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Issue #17 (mirroring mould-king-tracker issue #5) — saknade delar/
    // kvitton är egna StateFlows (se kommentaren i ModelDetailViewModel för
    // varför), inte en del av uiState.
    val missingParts by viewModel.missingParts.collectAsState()
    val isMissingPartsLoading by viewModel.isMissingPartsLoading.collectAsState()
    val isSyncingMissingParts by viewModel.isSyncingMissingParts.collectAsState()
    val isAddingMissingPart by viewModel.isAddingMissingPart.collectAsState()
    val togglingMissingPartId by viewModel.togglingMissingPartId.collectAsState()
    val deletingMissingPartId by viewModel.deletingMissingPartId.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val isUploadingReceipts by viewModel.isUploadingReceipts.collectAsState()
    val deletingReceiptId by viewModel.deletingReceiptId.collectAsState()

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
                            onSave = { name, pieceCount, category, notes -> viewModel.updateModel(name, pieceCount, category, notes) },
                            onCancel = { isEditing = false },
                        )
                    } else {
                        ModelDetail(
                            model = state.model,
                            categories = categories,
                            stats = stats,
                            isUpdatingStatus = state.isUpdatingStatus,
                            deletingSourceId = state.deletingSourceId,
                            isUpdatingBuildStatus = state.isUpdatingBuildStatus,
                            isSavingOrderNumber = state.isSavingOrderNumber,
                            isSavingRebrickableSetNum = state.isSavingRebrickableSetNum,
                            isUploadingBuildPhoto = state.isUploadingBuildPhoto,
                            isDeletingBuildPhoto = state.isDeletingBuildPhoto,
                            missingParts = missingParts,
                            isMissingPartsLoading = isMissingPartsLoading,
                            receipts = receipts,
                            isUploadingReceipts = isUploadingReceipts,
                            deletingReceiptId = deletingReceiptId,
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
                            onBuildStatusSelected = { viewModel.updateBuildStatus(it) },
                            onSaveOrderNumber = { viewModel.updateOrderNumber(it) },
                            onSaveRebrickableSetNum = { viewModel.updateRebrickableSetNum(it) },
                            onUploadBuildPhoto = { viewModel.uploadBuildPhoto(it) },
                            onDeleteBuildPhotoClick = { pendingDeleteBuildPhoto = true },
                            onShowMissingPartsClick = { showMissingPartsDialog = true },
                            onUploadReceipts = { viewModel.uploadReceipts(it) },
                            onDeleteReceiptClick = { viewModel.deleteReceipt(it) },
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
                onSubmit = { name, price, currency, url, inStock, warehouse, deliveryEstimate, shippingAmount, shippingCurrency ->
                    val target = editingSource
                    if (target == null) {
                        viewModel.addSource(name, price, currency, url, inStock, warehouse, deliveryEstimate, shippingAmount, shippingCurrency)
                    } else {
                        viewModel.updateSource(target.id, target.source, price, currency, url, inStock, warehouse, deliveryEstimate, shippingAmount, shippingCurrency)
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

        if (pendingDeleteBuildPhoto) {
            val isDeleting = (uiState as? ModelDetailUiState.Loaded)?.isDeletingBuildPhoto ?: false
            AlertDialog(
                onDismissRequest = { if (!isDeleting) pendingDeleteBuildPhoto = false },
                title = { Text("Ta bort foto") },
                text = { Text("Ta bort det egna byggfotot?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBuildPhoto()
                            pendingDeleteBuildPhoto = false
                        },
                        enabled = !isDeleting,
                    ) { Text("Ta bort", color = NegativeRed) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteBuildPhoto = false }, enabled = !isDeleting) { Text("Avbryt") }
                },
            )
        }

        if (showMissingPartsDialog) {
            loadedState?.model?.let {
                MissingPartsDialog(
                    missingParts = missingParts,
                    isSyncingMissingParts = isSyncingMissingParts,
                    isAddingMissingPart = isAddingMissingPart,
                    togglingMissingPartId = togglingMissingPartId,
                    deletingMissingPartId = deletingMissingPartId,
                    onDismiss = { showMissingPartsDialog = false },
                    onSync = { viewModel.syncMissingParts() },
                    onAdd = { name, partNum, colorName, quantity, sourceNote ->
                        viewModel.addMissingPart(name, partNum, colorName, quantity, sourceNote)
                    },
                    onToggleFound = { partId, found -> viewModel.toggleMissingPartFound(partId, found) },
                    onDelete = { partId -> viewModel.deleteMissingPart(partId) },
                )
            }
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
    isUpdatingBuildStatus: Boolean,
    isSavingOrderNumber: Boolean,
    isSavingRebrickableSetNum: Boolean,
    isUploadingBuildPhoto: Boolean,
    isDeletingBuildPhoto: Boolean,
    missingParts: MissingPartsResponse?,
    isMissingPartsLoading: Boolean,
    receipts: List<Receipt>,
    isUploadingReceipts: Boolean,
    deletingReceiptId: Int?,
    onStatusSelected: (String) -> Unit,
    onAddSourceClick: () -> Unit,
    onEditSourceClick: (Source) -> Unit,
    onDeleteSourceClick: (Source) -> Unit,
    onMoveToWatching: () -> Unit,
    onReject: () -> Unit,
    onDeleteModelClick: () -> Unit,
    onBuildStatusSelected: (String?) -> Unit,
    onSaveOrderNumber: (String) -> Unit,
    onSaveRebrickableSetNum: (String) -> Unit,
    onUploadBuildPhoto: (MultipartBody.Part) -> Unit,
    onDeleteBuildPhotoClick: () -> Unit,
    onShowMissingPartsClick: () -> Unit,
    onUploadReceipts: (List<MultipartBody.Part>) -> Unit,
    onDeleteReceiptClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Fas 12 (design t11c) -- pa breda skarmar (Galaxy Z Fold uppfalld,
        // surfplatta) delas innehallet i tva spalter sida vid sida istallet
        // for att staplas i en enda kolumn: kallor/varde/prishistorik till
        // vanster, verktyg/fakta i en smalare spalt till hoger -- samma
        // brytpunkt (600dp, Material's egen compact/medium-grans) som resten
        // av foldable-stodet i ModelListScreen.kt anvander implicit via
        // GridCells.Adaptive. BottomActionBar forblir en fullbredd rad under
        // bada layouterna, matchar designens egen 11c-footer.
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 600.dp
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                item {
                    HeroSection(model = model)
                    if (isWide) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Column(modifier = Modifier.weight(1.4f)) {
                                StatusChipsRow(currentStatus = model.status, isUpdating = isUpdatingStatus, onStatusSelected = onStatusSelected)
                                ValueScaleSection(model = model, stats = stats)
                                SourcesSection(
                                    model = model,
                                    deletingSourceId = deletingSourceId,
                                    onEdit = onEditSourceClick,
                                    onDeleteRequest = onDeleteSourceClick,
                                )
                                PriceHistorySection()
                                NotesDisplayRow(notes = model.notes)
                                if (model.status == "owned") {
                                    BuildStatusSection(
                                        model = model,
                                        isUpdating = isUpdatingBuildStatus,
                                        onSelect = onBuildStatusSelected,
                                    )
                                    OwnPhotoSection(
                                        model = model,
                                        isUploading = isUploadingBuildPhoto,
                                        isDeleting = isDeletingBuildPhoto,
                                        onUpload = onUploadBuildPhoto,
                                        onDeleteClick = onDeleteBuildPhotoClick,
                                    )
                                    if (model.buildStatus == "pagaende_saknar_delar") {
                                        MissingPartsCompactSection(
                                            model = model,
                                            missingParts = missingParts,
                                            isLoading = isMissingPartsLoading,
                                            isSavingRebrickableSetNum = isSavingRebrickableSetNum,
                                            onSaveRebrickableSetNum = onSaveRebrickableSetNum,
                                            onShowAllClick = onShowMissingPartsClick,
                                        )
                                    }
                                    OrderNumberSection(
                                        model = model,
                                        isSaving = isSavingOrderNumber,
                                        onSave = onSaveOrderNumber,
                                    )
                                    ReceiptsSection(
                                        receipts = receipts,
                                        isUploading = isUploadingReceipts,
                                        deletingReceiptId = deletingReceiptId,
                                        onUpload = onUploadReceipts,
                                        onDeleteClick = onDeleteReceiptClick,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f).widthIn(min = 200.dp, max = 280.dp)) {
                                ToolsSection(onAddSourceClick = onAddSourceClick)
                                FactsSection(model = model, categories = categories)
                                if (model.status == "new") {
                                    DeleteModelRow(onClick = onDeleteModelClick)
                                }
                            }
                        }
                    } else {
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
                            NotesDisplayRow(notes = model.notes)
                            if (model.status == "owned") {
                                BuildStatusSection(
                                    model = model,
                                    isUpdating = isUpdatingBuildStatus,
                                    onSelect = onBuildStatusSelected,
                                )
                                OwnPhotoSection(
                                    model = model,
                                    isUploading = isUploadingBuildPhoto,
                                    isDeleting = isDeletingBuildPhoto,
                                    onUpload = onUploadBuildPhoto,
                                    onDeleteClick = onDeleteBuildPhotoClick,
                                )
                                if (model.buildStatus == "pagaende_saknar_delar") {
                                    MissingPartsCompactSection(
                                        model = model,
                                        missingParts = missingParts,
                                        isLoading = isMissingPartsLoading,
                                        isSavingRebrickableSetNum = isSavingRebrickableSetNum,
                                        onSaveRebrickableSetNum = onSaveRebrickableSetNum,
                                        onShowAllClick = onShowMissingPartsClick,
                                    )
                                }
                                OrderNumberSection(
                                    model = model,
                                    isSaving = isSavingOrderNumber,
                                    onSave = onSaveOrderNumber,
                                )
                                ReceiptsSection(
                                    receipts = receipts,
                                    isUploading = isUploadingReceipts,
                                    deletingReceiptId = deletingReceiptId,
                                    onUpload = onUploadReceipts,
                                    onDeleteClick = onDeleteReceiptClick,
                                )
                            }
                            ToolsSection(onAddSourceClick = onAddSourceClick)
                            FactsSection(model = model, categories = categories)
                            if (model.status == "new") {
                                DeleteModelRow(onClick = onDeleteModelClick)
                            }
                        }
                    }
                }
            }
        }
        if (model.status == "new") {
            BottomActionBar(onMoveToWatching = onMoveToWatching, onReject = onReject)
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
                color = if (model.bestKrPerPiece != null) colorForValueRating(model.bestValueRating) else TextMutedMost,
            )
        }
    }
}

// FlowRow (issue #18) i stallet for en horisontellt skrollbar Row -- pa breda
// skarmar (Fold uppfalld) fick chipparna tidigare plats for de flesta men inte
// alla alternativ, vilket sag ut som att raden klipptes av i kanten istallet
// for att tydligt signalera att den gick att skrolla. Med FlowRow radbryts
// chipparna istallet till en andra rad nar de inte far plats, likt beteendet
// som redan anvands i telefonlaget.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChipsRow(currentStatus: String, isUpdating: Boolean, onStatusSelected: (String) -> Unit) {
    SectionLabel("STATUS")
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun DetailPillChip(selected: Boolean, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    AppPillChip(selected = selected, label = label, enabled = enabled, onClick = onClick)
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
// SCALE_BAR_MAX_KR, se nedan) är ett FAST spann, så bandgränserna alltid
// representerar de RIKTIGA tröskelvärdena i kr/del, inte en andel av bredden
// som råkar bero på vilka tre värden som visas just nu.
// Skalans max är ett FAST tal (SCALE_BAR_MAX_KR), inte beräknat från
// thisValue/cloneAvg/legoAvg (issue #7/#8: när maxV räknades ut dynamiskt
// från de tre värdena (values.max() * 1.15) flyttade sig ALLA tre markörer —
// inklusive Klonsnitt/LEGO-snitt, som är globala snitt och borde ligga still
// oavsett vilken modell som visas — varje gång "Detta set" ändrades. Ett lågt
// thisValue kunde ändå hamna långt in på baren eftersom skalan gick
// 0→(högsta av de tre), inte 0→ett stabilt referensspann). Samma fasta värde
// som webbversionen (static/app.js: SCALE_BAR_MAX_KR), valt med marginal över
// LEGO:s högsta namngivna tröskel (1.30 kr/del, se ValueRating.kt).
private const val SCALE_BAR_MAX_KR = 1.5

@Composable
private fun ValueScaleSection(model: Model, stats: StatsResponse?) {
    val thisValue = model.bestKrPerPiece ?: return
    val cloneAvg = stats?.avgKrPerPieceCloneAll
    val legoAvg = stats?.avgKrPerPieceLegoAll
    val isOfficial = model.isOfficialSet
    val levels = valueLevelsFor(isOfficial)
    val maxV = SCALE_BAR_MAX_KR

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
                isOfficial = model.isOfficialSet,
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
    isOfficial: Boolean,
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
            val context = LocalContext.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                flagForWarehouse(source.warehouse)?.let {
                    Text(text = it, modifier = Modifier.padding(end = 4.dp))
                }
                Text(
                    text = source.source,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (source.url.isNullOrBlank()) TextPrimary else AccentGoldLink,
                    textDecoration = if (source.url.isNullOrBlank()) null else TextDecoration.Underline,
                    modifier = if (source.url.isNullOrBlank()) {
                        Modifier
                    } else {
                        Modifier.clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
                            }
                        }
                    },
                )
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
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
            // Totalpriset är den primära siffran (vad man faktiskt betalar) — större/tyngre
            // stil än kr/del nedanför, se issue #9 i Sivan87/BrickRadar.
            Text(
                text = source.totalPriceSek?.let { "%.2f kr".format(it) }
                    ?: "${source.price?.let { "%.2f".format(it) } ?: "-"} ${source.currency.orEmpty()}".trim(),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                color = if (highlighted) AccentGold else TextPrimary,
            )
            source.krPerPiece?.let {
                Text(
                    text = "%.2f kr/del".format(it),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = colorForValueRating(source.valueRating ?: classifyValue(it, isOfficial)),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
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

// --- Issue #17 (mirroring mould-king-tracker issue #5) --------------------

// Anteckningsfältet är oberoende av byggstatus/status (issue #5, del 2) —
// alltid synligt, till skillnad från byggstatus/eget foto/ordernummer/kvitton
// nedan som alla är gated på status == "owned". Redigeras via samma
// pennikon-formulär som namn/delantal/kategori (EditableModelDetail ovan) —
// ingen egen inline-redigering, för att inte introducera ett andra
// redigeringsläge i en app som redan har ett etablerat mönster för det.
@Composable
private fun NotesDisplayRow(notes: String?) {
    val text = notes?.trim().orEmpty()
    SectionLabel("ANTECKNING")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = CardBorderMuted, shape = RoundedCornerShape(12.dp))
            .background(PanelBackground)
            .padding(12.dp),
    ) {
        Text(
            text = text.ifBlank { "Ingen anteckning — tryck på pennikonen för att lägga till" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isBlank()) TextMutedMost else TextSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildStatusSection(model: Model, isUpdating: Boolean, onSelect: (String?) -> Unit) {
    SectionLabel("BYGGSTATUS")
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BUILD_STATUS_OPTIONS.forEach { (key, label) ->
            DetailPillChip(
                selected = key == model.buildStatus,
                label = label,
                enabled = !isUpdating,
                onClick = { onSelect(if (model.buildStatus == key) null else key) },
            )
        }
        if (isUpdating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentGold)
        }
    }
    if (model.buildStatus == "byggd" && model.buildCompletedAt != null) {
        Text(
            text = "Byggd: ${model.buildCompletedAt}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
            color = TextMutedMore,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    val inactivity = model.missingPartsInactivity
    if (model.buildStatus == "pagaende_saknar_delar" && inactivity?.stale == true) {
        Text(
            text = "⚠ Inga uppdateringar på ${inactivity.days} dagar",
            style = MaterialTheme.typography.bodySmall,
            color = NegativeRed,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OwnPhotoSection(
    model: Model,
    isUploading: Boolean,
    isDeleting: Boolean,
    onUpload: (MultipartBody.Part) -> Unit,
    onDeleteClick: () -> Unit,
) {
    val context = LocalContext.current
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            uriToMultipartPart(context.contentResolver, uri, "photo")?.let(onUpload)
        }
    }

    // Issue #18 (punkt 3) -- kameraalternativ bredvid galleri-väljaren ovan.
    // pendingCameraUri hålls i väntan på launcherns callback (TakePicture
    // skriver till en URI vi själva skapar i förväg, till skillnad från
    // PickVisualMedia där systemet ger tillbaka en URI direkt) -- utan detta
    // state vet callbacken inte vilken fil som faktiskt skrevs.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            uriToMultipartPart(context.contentResolver, uri, "photo")?.let(onUpload)
        }
        pendingCameraUri = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCameraCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    SectionLabel("EGET FOTO")
    if (model.ownPhotoUrl != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ImagePlaceholder),
        ) {
            AsyncImage(
                model = ApiConfig.authenticatedUrl("api/models/${model.id}/build-photo"),
                contentDescription = "Eget foto av byggd modell",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !isUploading,
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentGold)
            } else {
                Text(if (model.ownPhotoUrl != null) "Byt foto" else "Ladda upp foto")
            }
        }
        TextButton(
            onClick = {
                val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (permissionGranted) {
                    val uri = createCameraCaptureUri(context)
                    pendingCameraUri = uri
                    takePictureLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            enabled = !isUploading,
        ) {
            Text("Ta foto")
        }
        if (model.ownPhotoUrl != null) {
            TextButton(onClick = onDeleteClick, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NegativeRed)
                } else {
                    Text("Ta bort", color = NegativeRed)
                }
            }
        }
    }
}

@Composable
private fun OrderNumberSection(model: Model, isSaving: Boolean, onSave: (String) -> Unit) {
    var text by remember(model.id, model.orderNumber) { mutableStateOf(model.orderNumber ?: "") }
    SectionLabel("ORDERNUMMER")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("t.ex. #12345") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        val changed = text.trim() != (model.orderNumber ?: "")
        TextButton(onClick = { onSave(text.trim()) }, enabled = changed && !isSaving) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentGold)
            } else {
                Text("Spara")
            }
        }
    }
}

@Composable
private fun MissingPartsCompactSection(
    model: Model,
    missingParts: MissingPartsResponse?,
    isLoading: Boolean,
    isSavingRebrickableSetNum: Boolean,
    onSaveRebrickableSetNum: (String) -> Unit,
    onShowAllClick: () -> Unit,
) {
    var setNumText by remember(model.id, model.rebrickableSetNum) { mutableStateOf(model.rebrickableSetNum ?: "") }
    SectionLabel("SAKNADE DELAR")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = setNumText,
            onValueChange = { setNumText = it },
            label = { Text("Rebrickable-setnummer") },
            placeholder = { Text("t.ex. 75192-1") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        val changed = setNumText.trim() != (model.rebrickableSetNum ?: "")
        TextButton(onClick = { onSaveRebrickableSetNum(setNumText.trim()) }, enabled = changed && !isSavingRebrickableSetNum) {
            if (isSavingRebrickableSetNum) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentGold)
            } else {
                Text("Spara")
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onShowAllClick)
            .padding(12.dp),
    ) {
        if (isLoading && missingParts == null) {
            Text(text = "Hämtar saknade delar…", style = MaterialTheme.typography.bodyMedium, color = TextMutedMore)
        } else if (missingParts == null) {
            Text(text = "Tryck för att se saknade delar", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        } else {
            Column {
                Text(
                    text = "${missingParts.total - missingParts.foundCount} av ${missingParts.total} delar saknas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "Senast synkad: ${missingParts.syncedAt ?: "aldrig"} · Visa alla ›",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedMore,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiptsSection(
    receipts: List<Receipt>,
    isUploading: Boolean,
    deletingReceiptId: Int?,
    onUpload: (List<MultipartBody.Part>) -> Unit,
    onDeleteClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val parts = uris.mapNotNull { uriToMultipartPart(context.contentResolver, it, "files") }
        if (parts.isNotEmpty()) onUpload(parts)
    }

    SectionLabel("KVITTON")
    if (receipts.isEmpty()) {
        Text(text = "Inga kvitton tillagda ännu", style = MaterialTheme.typography.bodyMedium, color = TextMutedMore)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            receipts.forEach { receipt ->
                ReceiptRow(
                    receipt = receipt,
                    isDeleting = deletingReceiptId == receipt.id,
                    onOpenClick = {
                        val url = ApiConfig.authenticatedUrl("api/receipts/${receipt.id}/file")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onDeleteClick = { onDeleteClick(receipt.id) },
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = !isUploading) { pickFilesLauncher.launch(arrayOf("image/*", "application/pdf")) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentGold)
        } else {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TextMuted)
        }
        Text(
            text = "Lägg till kvitto",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ReceiptRow(receipt: Receipt, isDeleting: Boolean, onOpenClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receipt.originalFilename ?: receipt.filename,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = receipt.uploadedAt,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                color = TextMutedMore,
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentGold)
        } else {
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Ta bort kvitto", tint = TextMutedMore, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MissingPartsDialog(
    missingParts: MissingPartsResponse?,
    isSyncingMissingParts: Boolean,
    isAddingMissingPart: Boolean,
    togglingMissingPartId: Int?,
    deletingMissingPartId: Int?,
    onDismiss: () -> Unit,
    onSync: () -> Unit,
    onAdd: (name: String, partNum: String?, colorName: String?, quantity: Int, sourceNote: String?) -> Unit,
    onToggleFound: (partId: Int, found: Boolean) -> Unit,
    onDelete: (partId: Int) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Saknade delar", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Stäng", tint = TextMuted)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Sök delnamn/nummer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSync, enabled = !isSyncingMissingParts) {
                        if (isSyncingMissingParts) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentGold)
                        } else {
                            Text("⟳ Synka mot Rebrickable")
                        }
                    }
                    TextButton(onClick = { showAddForm = !showAddForm }) {
                        Text(if (showAddForm) "Avbryt" else "+ Lägg till manuellt")
                    }
                }
                if (showAddForm) {
                    AddMissingPartForm(
                        isSaving = isAddingMissingPart,
                        onSubmit = { name, partNum, colorName, quantity, sourceNote ->
                            onAdd(name, partNum, colorName, quantity, sourceNote)
                            showAddForm = false
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val parts = missingParts?.parts.orEmpty().filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.partNum?.contains(query, ignoreCase = true) == true
            }
            val grouped = parts.groupBy { it.colorName ?: "Okänd färg" }.toSortedMap()
            if (parts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (missingParts == null) "Laddar…" else "Inga saknade delar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedMore,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    grouped.forEach { (colorName, partsInGroup) ->
                        item(key = "header-$colorName") {
                            Text(
                                text = colorName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                            )
                        }
                        items(partsInGroup, key = { it.id }) { part ->
                            MissingPartRow(
                                part = part,
                                isToggling = togglingMissingPartId == part.id,
                                isDeleting = deletingMissingPartId == part.id,
                                onToggleFound = { onToggleFound(part.id, !part.isFound) },
                                onDelete = { onDelete(part.id) },
                                onOpenBuyLink = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingPartRow(
    part: MissingPart,
    isToggling: Boolean,
    isDeleting: Boolean,
    onToggleFound: () -> Unit,
    onDelete: () -> Unit,
    onOpenBuyLink: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isToggling) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentGold)
            }
        } else {
            Checkbox(
                checked = part.isFound,
                onCheckedChange = { onToggleFound() },
                colors = CheckboxDefaults.colors(checkedColor = AccentGold, uncheckedColor = TextMuted),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${part.quantity}x ${part.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (part.isFound) TextMutedMore else TextPrimary,
            )
            val meta = listOfNotNull(part.partNum, part.sourceNote).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(text = meta, style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont), color = TextMutedMore)
            }
            val buyLinks = part.buyLinks
            if (buyLinks != null && (buyLinks.bricklink != null || buyLinks.brickowl != null)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    buyLinks.bricklink?.let { url ->
                        Text(
                            text = "BrickLink",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentGold,
                            modifier = Modifier.clickable { onOpenBuyLink(url) },
                        )
                    }
                    buyLinks.brickowl?.let { url ->
                        Text(
                            text = "BrickOwl",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentGold,
                            modifier = Modifier.clickable { onOpenBuyLink(url) },
                        )
                    }
                }
            }
        }
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentGold)
        } else {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Ta bort", tint = TextMutedMore, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddMissingPartForm(
    isSaving: Boolean,
    onSubmit: (name: String, partNum: String?, colorName: String?, quantity: Int, sourceNote: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var partNum by remember { mutableStateOf("") }
    var colorName by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull()
    val canSubmit = name.isNotBlank() && quantity != null && quantity > 0 && !isSaving

    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Delnamn") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = partNum,
                onValueChange = { partNum = it },
                label = { Text("Delnummer") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = quantityText,
                onValueChange = { input -> quantityText = input.filter { it.isDigit() } },
                label = { Text("Antal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(90.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = colorName,
            onValueChange = { colorName = it },
            label = { Text("Färg") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                quantity?.let {
                    onSubmit(name.trim(), partNum.trim().ifBlank { null }, colorName.trim().ifBlank { null }, it, null)
                }
            },
            enabled = canSubmit,
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Lägg till")
            }
        }
    }
}

// Fas 12 (audit av t1-t10, rond 9a/11c) -- den fasta fotraden ar EN
// tva-knapps rad (Avsla + Flytta till bevakning), inte tre -- "Ta bort" bor
// enligt mockupen i den skrollande ytan (se DeleteModelRow nedan), inte i den
// fasta footern.
@Composable
private fun BottomActionBar(onMoveToWatching: () -> Unit, onReject: () -> Unit) {
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
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
                .clickable(onClick = onReject)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Avslå", style = MaterialTheme.typography.titleSmall, color = TextMuted)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PositiveGreen.copy(alpha = 0.08f))
                .border(width = 1.dp, color = PositiveGreen.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                .clickable(onClick = onMoveToWatching),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Flytta till bevakning", style = MaterialTheme.typography.titleSmall, color = PositiveGreen)
        }
    }
}

@Composable
private fun DeleteModelRow(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Ta bort setet", style = MaterialTheme.typography.titleSmall, color = NegativeRed)
    }
}

@Composable
private fun EditableModelDetail(
    model: Model,
    categories: List<Category>,
    isSaving: Boolean,
    onSave: (name: String, pieceCount: Int, category: String, notes: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(model.id) { mutableStateOf(model.name ?: "") }
    var pieceCountText by remember(model.id) { mutableStateOf(model.pieceCount?.toString() ?: "") }
    var selectedCategory by remember(model.id) { mutableStateOf(model.category ?: UNCATEGORIZED_KEY) }
    var notes by remember(model.id) { mutableStateOf(model.notes ?: "") }

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
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Anteckningar") },
            placeholder = { Text("Egna kommentarer om modellen") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { pieceCount?.let { onSave(name.trim(), it, selectedCategory, notes.trim()) } },
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
        shippingAmount: Double?,
        shippingCurrency: String?,
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
    var shippingAmountText by remember(editingSource) { mutableStateOf(editingSource?.shippingOverrideAmount?.toString() ?: "") }
    var shippingCurrency by remember(editingSource) { mutableStateOf(editingSource?.shippingOverrideCurrency ?: "SEK") }

    val price = priceText.toDoubleOrNull()
    val priceError = price == null || price <= 0
    val nameError = editingSource == null && sourceName.isBlank()
    val urlError = url.isBlank()
    val shippingAmount = shippingAmountText.toDoubleOrNull()
    val shippingError = shippingAmountText.isNotBlank() && (shippingAmount == null || shippingAmount < 0)
    val canSave = !priceError && !nameError && !urlError && !shippingError && !isSaving

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
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Fraktkostnad", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CURRENCIES.forEach { c ->
                    DetailPillChip(selected = shippingCurrency == c, label = c, onClick = { shippingCurrency = c })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shippingAmountText,
                onValueChange = { shippingAmountText = it },
                label = { Text("Fraktkostnad") },
                placeholder = { Text("Lämna tomt om okänd") },
                isError = shippingError,
                supportingText = { if (shippingError) Text("Fraktkostnad måste vara ett tal ≥ 0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            shippingAmount, if (shippingAmount != null) shippingCurrency else null,
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
