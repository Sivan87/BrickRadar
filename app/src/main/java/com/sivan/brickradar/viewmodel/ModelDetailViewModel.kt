package com.sivan.brickradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ModelDetailUiState {
    data object Loading : ModelDetailUiState()
    data class Loaded(
        val model: Model,
        val isUpdatingStatus: Boolean = false,
        val isSavingEdit: Boolean = false,
        val isSavingSource: Boolean = false,
        val deletingSourceId: Int? = null,
        val isDeletingModel: Boolean = false,
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

    private var loadedModelId: Int? = null

    fun loadModel(modelId: Int) {
        if (loadedModelId == modelId) return
        loadedModelId = modelId
        _uiState.value = ModelDetailUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getModel(modelId)) {
                is ApiResult.Success -> _uiState.value = ModelDetailUiState.Loaded(result.data)
                is ApiResult.Error -> _uiState.value = ModelDetailUiState.Error(result.message)
            }
        }
        loadCategories()
        loadStats()
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

    fun updateModel(name: String, pieceCount: Int, category: String) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingEdit) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingEdit = true)
        viewModelScope.launch {
            when (val result = repository.updateModel(modelId, name, pieceCount, category)) {
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
    ) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingSource) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingSource = true)
        viewModelScope.launch {
            val result = repository.addSource(
                modelId, source, price, currency, url,
                if (inStock) 1 else 0, warehouse, deliveryEstimate,
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
    ) {
        val current = _uiState.value
        if (current !is ModelDetailUiState.Loaded || current.isSavingSource) return

        val modelId = current.model.id
        _uiState.value = current.copy(isSavingSource = true)
        viewModelScope.launch {
            val result = repository.updateSource(
                modelId, sourceId, sourceName, price, currency, url,
                if (inStock) 1 else 0, warehouse, deliveryEstimate,
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
}
