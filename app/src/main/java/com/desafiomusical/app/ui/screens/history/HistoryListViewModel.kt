package com.desafiomusical.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryListUiState(
    val isLoading: Boolean = true,
    val entries: List<GameHistoryEntry> = emptyList()
)

class HistoryListViewModel(container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryListUiState())
    val uiState: StateFlow<HistoryListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.gameHistoryRepository.observeHistory().collect { entries ->
                _uiState.value = HistoryListUiState(isLoading = false, entries = entries)
            }
        }
    }
}
