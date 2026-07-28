package com.sivan.brickradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.model.Model
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ModelListUiState {
    data object Loading : ModelListUiState()
    data class Loaded(val models: List<Model>, val isRefreshing: Boolean = false) : ModelListUiState()
    data class Error(val message: String) : ModelListUiState()
}

enum class SortOption {
    DEFAULT, KR_ASC, RECENT_DESC, NAME_ASC
}

enum class ListViewMode {
    LIST, GRID
}

data class ModelListFilters(
    val status: String? = null,
    val category: String? = null,
    val sort: SortOption = SortOption.DEFAULT,
)

class ModelListViewModel @JvmOverloads constructor(
    private val repository: ModelRepository = ModelRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModelListUiState>(ModelListUiState.Loading)
    val uiState: StateFlow<ModelListUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _filters = MutableStateFlow(ModelListFilters())
    val filters: StateFlow<ModelListFilters> = _filters.asStateFlow()

    private val _viewMode = MutableStateFlow(ListViewMode.LIST)
    val viewMode: StateFlow<ListViewMode> = _viewMode.asStateFlow()

    // Driver filterchipsens räknare (Alla/Bevakar/Äger m.fl.) — se StatsResponse.
    // Null tills första hämtningen lyckas; UI:t visar då bara etiketten utan siffra.
    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCategories()
        loadStats()
        loadModels()
    }

    fun setViewMode(mode: ListViewMode) {
        _viewMode.value = mode
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _categories.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            when (val result = repository.getStats()) {
                is ApiResult.Success -> _stats.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }

    fun setStatusFilter(status: String?) {
        if (_filters.value.status == status) return
        _filters.value = _filters.value.copy(status = status)
        loadModels()
    }

    fun setCategoryFilter(category: String?) {
        if (_filters.value.category == category) return
        _filters.value = _filters.value.copy(category = category)
        loadModels()
    }

    fun setSortOption(sort: SortOption) {
        if (_filters.value.sort == sort) return
        _filters.value = _filters.value.copy(sort = sort)
        loadModels()
    }

    fun loadModels() {
        loadJob?.cancel()
        val previousModels = (_uiState.value as? ModelListUiState.Loaded)?.models
        _uiState.value = if (previousModels != null) {
            ModelListUiState.Loaded(previousModels, isRefreshing = true)
        } else {
            ModelListUiState.Loading
        }

        val current = _filters.value
        // API:t har inget sort=name_asc (bekräftat mot api.py: bara kr_asc/
        // kr_desc/recent_asc/recent_desc stöds) — sorteras klientsidan istället.
        val sortParam = when (current.sort) {
            SortOption.KR_ASC -> "kr_asc"
            SortOption.RECENT_DESC -> "recent_desc"
            SortOption.DEFAULT, SortOption.NAME_ASC -> null
        }

        loadJob = viewModelScope.launch {
            when (val result = repository.getModels(status = current.status, category = current.category, sort = sortParam)) {
                is ApiResult.Success -> {
                    val models = if (current.sort == SortOption.NAME_ASC) {
                        result.data.sortedBy { (it.name ?: it.modelNumber).lowercase() }
                    } else {
                        result.data
                    }
                    _uiState.value = ModelListUiState.Loaded(models)
                }
                is ApiResult.Error -> _uiState.value = ModelListUiState.Error(result.message)
            }
        }
    }
}
