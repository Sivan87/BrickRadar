package com.sivan.brickradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StatistikUiState {
    data object Loading : StatistikUiState()
    data class Loaded(val stats: StatsResponse) : StatistikUiState()
    data class Error(val message: String) : StatistikUiState()
}

class StatistikViewModel @JvmOverloads constructor(
    private val repository: ModelRepository = ModelRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatistikUiState>(StatistikUiState.Loading)
    val uiState: StateFlow<StatistikUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.value = StatistikUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getStats()) {
                is ApiResult.Success -> _uiState.value = StatistikUiState.Loaded(result.data)
                is ApiResult.Error -> _uiState.value = StatistikUiState.Error(result.message)
            }
        }
    }
}
