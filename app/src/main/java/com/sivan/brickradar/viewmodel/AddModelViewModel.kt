package com.sivan.brickradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.model.Brick4SearchResult
import com.sivan.brickradar.model.Category
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddModelEvent {
    data object Created : AddModelEvent()
    data class Failed(val message: String) : AddModelEvent()
}

// SEARCH (default, Fas 7) — modellnummer söks mot Brick4 för att slå fast
// märket innan modellen skapas. MANUAL (Fas 6, oförändrat) — alla fält
// fylls i för hand, ingen Brick4-inblandning alls.
enum class AddModelMode {
    SEARCH,
    MANUAL,
}

// Mirrorar webbens "+ Ny modell"-formulär: en Brick4-sökning på bara
// modellnumret disambiguerar märket, den avslöjar INTE namn/bild/delantal
// (se Brick4SearchResult) — de fälten fylls i av samma asynkrona
// bakgrundshämtning som redan körs för alla nya modeller med känt
// modellnummer. `selected` != null betyder att användaren valt en
// märkeskandidat och nu ser bekräftelseformuläret (namn/delantal valfria där).
data class Brick4SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Brick4SearchResult>? = null,
    val error: String? = null,
    val selected: Brick4SearchResult? = null,
)

class AddModelViewModel @JvmOverloads constructor(
    private val repository: ModelRepository = ModelRepository(),
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _mode = MutableStateFlow(AddModelMode.SEARCH)
    val mode: StateFlow<AddModelMode> = _mode.asStateFlow()

    private val _search = MutableStateFlow(Brick4SearchState())
    val search: StateFlow<Brick4SearchState> = _search.asStateFlow()

    private val _events = MutableSharedFlow<AddModelEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddModelEvent> = _events

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _categories.value = result.data
                is ApiResult.Error -> Unit
            }
        }
    }

    fun setMode(newMode: AddModelMode) {
        _mode.value = newMode
    }

    fun searchBrick4(modelNumber: String) {
        _search.value = Brick4SearchState(query = modelNumber, isSearching = true)
        viewModelScope.launch {
            when (val result = repository.searchBrick4ByNumber(modelNumber)) {
                is ApiResult.Success -> _search.value = _search.value.copy(
                    isSearching = false,
                    results = result.data,
                )
                is ApiResult.Error -> _search.value = _search.value.copy(
                    isSearching = false,
                    error = result.message,
                )
            }
        }
    }

    fun selectBrick4Candidate(candidate: Brick4SearchResult) {
        _search.value = _search.value.copy(selected = candidate)
    }

    // Tillbaka till kandidatlistan (t.ex. användaren valde fel märke).
    fun clearBrick4Selection() {
        _search.value = _search.value.copy(selected = null)
    }

    // Ny sökning görs, eller läget byts bort från sök — nollställ helt så
    // gamla resultat/fel inte dyker upp igen vid nästa sökning.
    fun resetBrick4Search() {
        _search.value = Brick4SearchState()
    }

    fun saveModel(
        name: String?,
        modelNumber: String,
        brand: String,
        pieceCount: Int?,
        status: String,
        category: String?,
        imageUrl: String?,
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = repository.addModel(modelNumber, brand, name, pieceCount, status, imageUrl, category)
            _isSaving.value = false
            when (result) {
                is ApiResult.Success -> _events.emit(AddModelEvent.Created)
                is ApiResult.Error -> _events.emit(AddModelEvent.Failed(result.message))
            }
        }
    }
}
