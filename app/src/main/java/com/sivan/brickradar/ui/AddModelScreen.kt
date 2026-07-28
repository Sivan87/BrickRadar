package com.sivan.brickradar.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sivan.brickradar.model.Brick4SearchResult
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.KNOWN_BRANDS
import com.sivan.brickradar.model.UNCATEGORIZED_KEY
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.CardBackground
import com.sivan.brickradar.ui.theme.CardBorder
import com.sivan.brickradar.ui.theme.PanelBackground
import com.sivan.brickradar.ui.theme.TextMuted
import com.sivan.brickradar.ui.theme.TextMutedMore
import com.sivan.brickradar.ui.theme.TextPrimary
import com.sivan.brickradar.viewmodel.AddModelEvent
import com.sivan.brickradar.viewmodel.AddModelMode
import com.sivan.brickradar.viewmodel.AddModelViewModel
import com.sivan.brickradar.viewmodel.Brick4SearchState

// Auto-läget skickar inget category-fält alls till servern, som då gissar
// utifrån namnet (suggest_category i app.py) — se ModelRepository.addModel.
private const val AUTO_CATEGORY = "__auto__"

private val STATUS_OPTIONS = listOf(
    "new" to "Sök",
    "watching" to "Bevakar",
    "owned" to "Äger",
    "rejected" to "Avslagen",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddModelScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddModelViewModel = viewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val search by viewModel.search.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchFieldText by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var modelNumber by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var pieceCountText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("new") }
    var selectedCategory by remember { mutableStateOf(AUTO_CATEGORY) }
    var imageUrl by remember { mutableStateOf("") }

    // Modellnummer+märke låses in automatiskt när en Brick4-kandidat väljs —
    // resten av fälten (namn/delantal/kategori/status/bild) fylls i av
    // användaren precis som i det manuella formuläret, bara valfritt här.
    LaunchedEffect(search.selected) {
        val candidate = search.selected
        if (candidate != null) {
            modelNumber = search.query
            brand = candidate.brandName
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddModelEvent.Created -> onSaved()
                is AddModelEvent.Failed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    fun switchToManual(prefillModelNumber: String) {
        modelNumber = prefillModelNumber
        viewModel.setMode(AddModelMode.MANUAL)
    }

    val isSearchConfirmStep = mode == AddModelMode.SEARCH && search.selected != null
    val nameRequired = mode == AddModelMode.MANUAL
    val pieceCountRequired = mode == AddModelMode.MANUAL

    val nameError = nameRequired && name.isBlank()
    val brandError = mode == AddModelMode.MANUAL && brand.isBlank()
    val pieceCount = pieceCountText.toIntOrNull()
    val pieceCountBlank = pieceCountText.isBlank()
    val pieceCountError = if (pieceCountRequired) {
        pieceCount == null || pieceCount <= 0
    } else {
        !pieceCountBlank && (pieceCount == null || pieceCount <= 0)
    }
    val canShowForm = mode == AddModelMode.MANUAL || isSearchConfirmStep
    val canSave = canShowForm && !nameError && !brandError && !pieceCountError && !isSaving

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lägg till modell", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBackground),
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !isSaving) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Avbryt", tint = TextMuted)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormPillChip(
                    selected = mode == AddModelMode.SEARCH,
                    label = "Sök modellnummer",
                    onClick = { viewModel.setMode(AddModelMode.SEARCH) },
                )
                FormPillChip(
                    selected = mode == AddModelMode.MANUAL,
                    label = "Fyll i manuellt",
                    onClick = { viewModel.setMode(AddModelMode.MANUAL) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (mode == AddModelMode.SEARCH && search.selected == null) {
                Brick4SearchSection(
                    searchFieldText = searchFieldText,
                    onSearchFieldTextChange = { searchFieldText = it },
                    search = search,
                    onSearch = { viewModel.searchBrick4(searchFieldText.trim()) },
                    onSelect = { candidate -> viewModel.selectBrick4Candidate(candidate) },
                    onUseManualInstead = { switchToManual(searchFieldText.trim()) },
                )
            }

            if (isSearchConfirmStep) {
                val candidate = search.selected
                if (candidate != null) {
                    FormCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Modellnummer ${search.query} · ${candidate.brandName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Bekräftat via Brick4 — justera fälten nedan vid behov",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedMore,
                            )
                            TextButton(onClick = { viewModel.clearBrick4Selection() }) {
                                Text("Ändra märke")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (canShowForm) {
                AddModelFields(
                    name = name,
                    onNameChange = { name = it },
                    nameRequired = nameRequired,
                    nameError = nameError,
                    showModelNumberAndBrandFields = mode == AddModelMode.MANUAL,
                    modelNumber = modelNumber,
                    onModelNumberChange = { modelNumber = it },
                    brand = brand,
                    onBrandChange = { brand = it },
                    brandError = brandError,
                    pieceCountText = pieceCountText,
                    onPieceCountTextChange = { input -> pieceCountText = input.filter { it.isDigit() } },
                    pieceCountRequired = pieceCountRequired,
                    pieceCountError = pieceCountError,
                    status = status,
                    onStatusChange = { status = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    imageUrl = imageUrl,
                    onImageUrlChange = { imageUrl = it },
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.saveModel(
                            name = name.trim().ifBlank { null },
                            modelNumber = modelNumber.trim(),
                            brand = brand.trim(),
                            pieceCount = pieceCountText.toIntOrNull(),
                            status = status,
                            category = if (selectedCategory == AUTO_CATEGORY) null else selectedCategory,
                            imageUrl = imageUrl.trim().ifBlank { null },
                        )
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
            }
        }
    }
}

@Composable
private fun Brick4SearchSection(
    searchFieldText: String,
    onSearchFieldTextChange: (String) -> Unit,
    search: Brick4SearchState,
    onSearch: () -> Unit,
    onSelect: (Brick4SearchResult) -> Unit,
    onUseManualInstead: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = searchFieldText,
            onValueChange = onSearchFieldTextChange,
            label = { Text("Modellnummer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSearch,
            enabled = searchFieldText.isNotBlank() && !search.isSearching,
        ) {
            Icon(imageVector = Icons.Filled.Search, contentDescription = "Sök")
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    if (search.isSearching) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    } else if (search.error != null) {
        Text(text = search.error, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onUseManualInstead) { Text("Fyll i manuellt istället") }
    } else {
        val results = search.results
        if (results != null) {
            if (results.isEmpty()) {
                Text("Inga träffar hos Brick4 för det modellnumret.")
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onUseManualInstead) { Text("Fyll i manuellt istället") }
            } else {
                if (results.size > 1) {
                    Text(
                        text = "${results.size} möjliga märken hittades — välj rätt:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { candidate ->
                        Brick4CandidateCard(candidate = candidate, onSelect = { onSelect(candidate) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Brick4CandidateCard(candidate: Brick4SearchResult, onSelect: () -> Unit) {
    FormCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = candidate.brandName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Button(onClick = onSelect) { Text("Använd denna") }
        }
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormPillChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
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

@Composable
private fun AddModelFields(
    name: String,
    onNameChange: (String) -> Unit,
    nameRequired: Boolean,
    nameError: Boolean,
    showModelNumberAndBrandFields: Boolean,
    modelNumber: String,
    onModelNumberChange: (String) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    brandError: Boolean,
    pieceCountText: String,
    onPieceCountTextChange: (String) -> Unit,
    pieceCountRequired: Boolean,
    pieceCountError: Boolean,
    status: String,
    onStatusChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Namn") },
        isError = nameError,
        supportingText = {
            Text(if (nameRequired) "Namn får inte vara tomt" else "Valfritt — fylls i automatiskt om du lämnar tomt")
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (showModelNumberAndBrandFields) {
        OutlinedTextField(
            value = modelNumber,
            onValueChange = onModelNumberChange,
            label = { Text("Modellnummer") },
            supportingText = { Text("Valfritt — lämna tomt för ett MOC/anpassat set") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = brand,
            onValueChange = onBrandChange,
            label = { Text("Märke") },
            isError = brandError,
            supportingText = { if (brandError) Text("Märke får inte vara tomt") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KNOWN_BRANDS.forEach { known ->
                FormPillChip(selected = brand == known, label = known, onClick = { onBrandChange(known) })
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = pieceCountText,
        onValueChange = onPieceCountTextChange,
        label = { Text("Delantal") },
        isError = pieceCountError,
        supportingText = {
            val text = when {
                pieceCountError -> "Delantal måste vara ett positivt heltal"
                !pieceCountRequired -> "Valfritt — fylls i automatiskt om möjligt"
                else -> null
            }
            if (text != null) Text(text)
        },
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
        FormPillChip(
            selected = selectedCategory == AUTO_CATEGORY,
            label = "Gissa från namn",
            onClick = { onCategoryChange(AUTO_CATEGORY) },
        )
        categories.forEach { category ->
            FormPillChip(
                selected = selectedCategory == category.category,
                label = category.label,
                onClick = { onCategoryChange(category.category) },
            )
        }
        FormPillChip(
            selected = selectedCategory == UNCATEGORIZED_KEY,
            label = "Ingen kategori",
            onClick = { onCategoryChange(UNCATEGORIZED_KEY) },
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Status", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STATUS_OPTIONS.forEach { (key, label) ->
            FormPillChip(selected = status == key, label = label, onClick = { onStatusChange(key) })
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = imageUrl,
        onValueChange = onImageUrlChange,
        label = { Text("Bildlänk") },
        placeholder = { Text("https://...") },
        supportingText = { Text("Valfritt") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
