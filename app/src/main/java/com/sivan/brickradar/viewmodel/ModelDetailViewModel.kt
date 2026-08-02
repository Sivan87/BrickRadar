package com.sivan.brickradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.MissingPartsResponse
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.Receipt
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

// Byggstatus är bara meningsfull när status == "owned" (issue #17/#5) —
// samma konstant som _apply_model_fields (api.py) kontrollerar server-side.
private const val MISSING_PARTS_BUILD_STATUS = "pagaende_saknar_delar"

sealed class ModelDetailUiState {
    data object Loading : ModelDetailUiState()
    data class Loaded(
        val model: Model,
        val isUpdatingStatus: Boolean = false,
        val isSavingEdit: Boolean = false,
        val isSavingSource: Boolean = false,
        val deletingSourceId: Int? = null,
        val isDeletingModel: Boolean = false,
        // Issue #17 (mirroring mould-king-tracker issue #5) — byggstatus/
        // ordernummer/rebrickable-nummer/eget foto muterar Model direkt, så de
        // hör hemma här (samma mönster som isUpdatingStatus/isSavingEdit ovan).
        // Saknade delar/kvitton är EGNA StateFlows (se nedan), inte en del av
        // Loaded, eftersom de INTE ingår i Model-svaret och annars skulle
        // nollställas varje gång någon annan del av modellen muteras (t.ex.
        // en källändring skulle annars tömma missing-parts-listan igen).
        val isUpdatingBuildStatus: Boolean = false,
        val isSavingOrderNumber: Boolean = false,
        val isSavingRebrickableSetNum: Boolean = false,
        val isUploadingBuildPhoto: Boolean = false,
        val isDeletingBuildPhoto: Boolean = false,
    ) : ModelDetailUiState()
    data class Error(val message: String) : ModelDetailUiState()
}

sealed class ModelDetailEvent {
    data class Saved(val message: String, val isEditSave: Boolean) : ModelDetailEvent()
    data class Failed(val message: String) : ModelDetailEvent()
    // Egen händelse (istället för att återanvända Saved/isEditSave) eftersom
    // UI:t måste stänga käll-bottensheeten på lyckad spara/redigera, vilket är
    // ett helt separat tillstånd från isEditing (modellens namn/delantal/kategori).
    data class SourceSaved(val message: String) : ModelDetailEvent()
    // Egen händelse (inte Saved) så UI:t kan navigera tillbaka till listvyn —
    // modellen finns inte längre, till skillnad från alla andra Saved-fall
    // där samma skärm visas kvar.
    data object Deleted : ModelDetailEvent()
}

class ModelDetailViewModel @JvmOverloads constructor(
    private val repository: ModelRepository = ModelRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModelDetailUiState>(ModelDetailUiState.Loading)
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // Referenspunkter för värde-skalan (Klonsnitt/LEGO-snitt) — se StatsResponse.
    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats.asStateFlow()

    private val _events = MutableSharedFlow<ModelDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ModelDetailEvent> = _events

    // Saknade delar (issue #17/#5) — egna StateFlows, se kommentaren vid
    // Loaded ovan för varför de inte ligger inne i uiState.
    private val _missingParts = MutableStateFlow<MissingPartsResponse?>(null)
    val missingParts: StateFlow<MissingPartsResponse?> = _missingParts.asStateFlow()
    private val _isMissingPartsLoading = MutableStateFlow(false)
    val isMissingPartsLoading: StateFlow<Boolean> = _isMissingPartsLoading.asStateFlow()
    private val _isSyncingMissingParts = MutableStateFlow(false)
    val isSyncingMissingParts: StateFlow<Boolean> = _isSyncingMissingParts.asStateFlow()
    private val _isAddingMissingPart = MutableStateFlow(false)
    val isAddingMissingPart: StateFlow<Boolean> = _isAddingMissingPart.asStateFlow()
    private val _togglingMissingPartId = MutableStateFlow<Int?>(null)
    val togglingMissingPartId: StateFlow<Int?> = _togglingMissingPartId.asStateFlow()
    private val _deletingMissingPartId = MutableStateFlow<Int?>(null)
    val deletingMissingPartId: StateFlow<Int?> = _deletingMissingPartId.asStateFlow()

    // Kvitton (issue #17/#5) — samma "eget StateFlow"-mönster som ovan.
    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()
    private val _isReceiptsLoading = MutableStateFlow(false)
    val isReceiptsLoading: StateFlow<Boolean> = _isReceiptsLoading.asStateFlow()
    private val _isUploadingReceipts = MutableStateFlow(false)
    val isUploadingReceipts: StateFlow<Boolean> = _isUploadingReceipts.asStateFlow()
    private val _deletingReceiptId = MutableStateFlow<Int?>(null)
    val deletingReceiptId: StateFlow<Int?> = _deletingReceiptId.asStateFlow()

    private var loadedModelId: Int? = null

    fun loadModel(modelId: Int) {
        if (loadedModelId == modelId) return
        loadedModelId = modelId
        _uiState.value = ModelDetailUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getModel(modelId)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    loadExtrasIfRelevant(result.data)
                }
                is ApiResult.Error -> _uiState.value = ModelDetailUiState.Error(result.message)
            }
        }
        loadCategories()
        loadStats()
    }

    // Saknade delar/kvitton hämtas bara när de faktiskt kan visa något (samma
    // gating som webbens buildDetailHtml/fetchDetailExtras, se CLAUDE.md i
    // mould-king-tracker) — en modell som inte är "owned" (eller vars
    // byggstatus inte är "pagaende_saknar_delar" för saknade delar
    // specifikt) triggar aldrig ett extra API-anrop (eller en möjlig
    // Rebrickable-synk) i onödan.
    private fun loadExtrasIfRelevant(model: Model) {
        if (model.status == "owned") {
            loadReceipts(model.id)
            if (model.buildStatus == MISSING_PARTS_BUILD_STATUS) {
                loadMissingParts(model.id)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _categories.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            when (val result = repository.getStats()) {
                is ApiResult.Success -> _stats.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }

    fun updateStatus(newStatus: String) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isUpdatingStatus) return
        if (current.model.status == newStatus) return

        val modelId = current.model.id
        _uiState.value = current.copy(isUpdatingStatus = true)
        viewModelScope.launch {
            when (val result = repository.updateStatus(modelId, newStatus)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Status uppdaterad", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isUpdatingStatus = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun updateModel(name: String, pieceCount: Int, category: String, notes: String) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingEdit) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingEdit = true)
        viewModelScope.launch {
            when (val result = repository.updateModel(modelId, name, pieceCount, category, notes)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Sparat", isEditSave = true))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isSavingEdit = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun addSource(
        source: String,
        price: Double,
        currency: String,
        url: String,
        inStock: Boolean,
        warehouse: String?,
        deliveryEstimate: String?,
        shippingAmount: Double?,
        shippingCurrency: String?,
    ) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingSource) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingSource = true)
        viewModelScope.launch {
            val result = repository.addSource(
                modelId, source, price, currency, url,
                if (inStock) 1 else 0, warehouse, deliveryEstimate,
                shippingAmount, shippingCurrency,
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.SourceSaved("Källa tillagd"))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isSavingSource = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun updateSource(
        sourceId: Int,
        sourceName: String,
        price: Double,
        currency: String,
        url: String,
        inStock: Boolean,
        warehouse: String?,
        deliveryEstimate: String?,
        shippingAmount: Double?,
        shippingCurrency: String?,
    ) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingSource) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingSource = true)
        viewModelScope.launch {
            val result = repository.updateSource(
                modelId, sourceId, sourceName, price, currency, url,
                if (inStock) 1 else 0, warehouse, deliveryEstimate,
                shippingAmount, shippingCurrency,
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.SourceSaved("Källa uppdaterad"))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isSavingSource = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun deleteModel() {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isDeletingModel) return

        val modelId = current.model.id
        _uiState.value = current.copy(isDeletingModel = true)
        viewModelScope.launch {
            when (val result = repository.deleteModel(modelId)) {
                is ApiResult.Success -> _events.emit(ModelDetailEvent.Deleted)
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isDeletingModel = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun deleteSource(sourceId: Int) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.deletingSourceId != null) return

        val modelId = current.model.id
        _uiState.value = current.copy(deletingSourceId = sourceId)
        viewModelScope.launch {
            when (val result = repository.deleteSource(modelId, sourceId)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Källa borttagen", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(deletingSourceId = null)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    // --- Issue #17 (mirroring mould-king-tracker issue #5) ---------------

    fun updateBuildStatus(newBuildStatus: String?) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isUpdatingBuildStatus) return
        if (current.model.buildStatus == newBuildStatus) return

        val modelId = current.model.id
        _uiState.value = current.copy(isUpdatingBuildStatus = true)
        viewModelScope.launch {
            when (val result = repository.updateBuildStatus(modelId, newBuildStatus)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Byggstatus uppdaterad", isEditSave = false))
                    loadExtrasIfRelevant(result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isUpdatingBuildStatus = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun updateOrderNumber(orderNumber: String) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingOrderNumber) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingOrderNumber = true)
        viewModelScope.launch {
            when (val result = repository.updateOrderNumber(modelId, orderNumber)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Ordernummer sparat", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isSavingOrderNumber = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun updateRebrickableSetNum(rebrickableSetNum: String) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingRebrickableSetNum) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingRebrickableSetNum = true)
        viewModelScope.launch {
            when (val result = repository.updateRebrickableSetNum(modelId, rebrickableSetNum)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Rebrickable-setnummer sparat", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isSavingRebrickableSetNum = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun uploadBuildPhoto(photo: MultipartBody.Part) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isUploadingBuildPhoto) return

        val modelId = current.model.id
        _uiState.value = current.copy(isUploadingBuildPhoto = true)
        viewModelScope.launch {
            when (val result = repository.uploadBuildPhoto(modelId, photo)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Foto uppladdat", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isUploadingBuildPhoto = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    fun deleteBuildPhoto() {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isDeletingBuildPhoto) return

        val modelId = current.model.id
        _uiState.value = current.copy(isDeletingBuildPhoto = true)
        viewModelScope.launch {
            when (val result = repository.deleteBuildPhoto(modelId)) {
                is ApiResult.Success -> {
                    _uiState.value = ModelDetailUiState.Loaded(result.data)
                    _events.emit(ModelDetailEvent.Saved("Foto borttaget", isEditSave = false))
                }
                is ApiResult.Error -> {
                    _uiState.value = current.copy(isDeletingBuildPhoto = false)
                    _events.emit(ModelDetailEvent.Failed(result.message))
                }
            }
        }
    }

    private fun loadMissingParts(modelId: Int) {
        _isMissingPartsLoading.value = true
        viewModelScope.launch {
            when (val result = repository.getMissingParts(modelId)) {
                is ApiResult.Success -> _missingParts.value = result.data
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _isMissingPartsLoading.value = false
        }
    }

    // Anropas explicit av "Visa alla"-dialogen/den kompakta sammanfattningen
    // (t.ex. en manuell uppdatera-knapp) — loadModel triggar redan detta en
    // gång automatiskt när modellen initialt laddas (se loadExtrasIfRelevant).
    fun refreshMissingParts() {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        loadMissingParts(modelId)
    }

    fun addMissingPart(name: String, partNum: String?, colorName: String?, quantity: Int, sourceNote: String?) {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_isAddingMissingPart.value) return
        _isAddingMissingPart.value = true
        viewModelScope.launch {
            when (val result = repository.addMissingPart(modelId, name, partNum, colorName, quantity, sourceNote)) {
                is ApiResult.Success -> {
                    _missingParts.value = result.data
                    _events.emit(ModelDetailEvent.Saved("Del tillagd", isEditSave = false))
                }
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _isAddingMissingPart.value = false
        }
    }

    fun toggleMissingPartFound(partId: Int, found: Boolean) {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_togglingMissingPartId.value != null) return
        _togglingMissingPartId.value = partId
        viewModelScope.launch {
            when (val result = repository.toggleMissingPartFound(modelId, partId, found)) {
                is ApiResult.Success -> _missingParts.value = result.data
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _togglingMissingPartId.value = null
        }
    }

    fun deleteMissingPart(partId: Int) {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_deletingMissingPartId.value != null) return
        _deletingMissingPartId.value = partId
        viewModelScope.launch {
            when (val result = repository.deleteMissingPart(modelId, partId)) {
                is ApiResult.Success -> _missingParts.value = result.data
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _deletingMissingPartId.value = null
        }
    }

    fun syncMissingParts() {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_isSyncingMissingParts.value) return
        _isSyncingMissingParts.value = true
        viewModelScope.launch {
            when (val result = repository.syncMissingParts(modelId)) {
                is ApiResult.Success -> {
                    _missingParts.value = result.data
                    _events.emit(ModelDetailEvent.Saved("Synkat mot Rebrickable", isEditSave = false))
                }
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _isSyncingMissingParts.value = false
        }
    }

    private fun loadReceipts(modelId: Int) {
        _isReceiptsLoading.value = true
        viewModelScope.launch {
            when (val result = repository.getReceipts(modelId)) {
                is ApiResult.Success -> _receipts.value = result.data
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _isReceiptsLoading.value = false
        }
    }

    fun uploadReceipts(files: List<MultipartBody.Part>) {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_isUploadingReceipts.value || files.isEmpty()) return
        _isUploadingReceipts.value = true
        viewModelScope.launch {
            when (val result = repository.uploadReceipts(modelId, files)) {
                is ApiResult.Success -> {
                    _receipts.value = result.data
                    _events.emit(ModelDetailEvent.Saved("Kvitto tillagt", isEditSave = false))
                }
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _isUploadingReceipts.value = false
        }
    }

    fun deleteReceipt(receiptId: Int) {
        val modelId = (_uiState.value as? ModelDetailUiState.Loaded)?.model?.id ?: return
        if (_deletingReceiptId.value != null) return
        _deletingReceiptId.value = receiptId
        viewModelScope.launch {
            when (val result = repository.deleteReceipt(modelId, receiptId)) {
                is ApiResult.Success -> _receipts.value = result.data
                is ApiResult.Error -> _events.emit(ModelDetailEvent.Failed(result.message))
            }
            _deletingReceiptId.value = null
        }
    }
}
