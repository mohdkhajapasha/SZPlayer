package com.shaaztechno.videoplayer.presentation.whatsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaaztechno.videoplayer.domain.model.WhatsAppStatus
import com.shaaztechno.videoplayer.domain.repository.WhatsAppStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WhatsAppStatusViewModel(
    private val repository: WhatsAppStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppStatusUiState())
    val uiState: StateFlow<WhatsAppStatusUiState> = _uiState.asStateFlow()

    init {
        refreshStatuses()
        viewModelScope.launch {
            repository.getStatuses().collect { statuses ->
                _uiState.value = _uiState.value.copy(
                    statuses = statuses,
                    isLoading = false
                )
            }
        }
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchStatuses()
        }
    }

    fun saveStatus(status: WhatsAppStatus, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveStatus(status)
            if (result.isSuccess) {
                onResult(true, "Status saved successfully")
            } else {
                onResult(false, "Failed to save status: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun shareStatus(status: WhatsAppStatus) {
        viewModelScope.launch {
            repository.shareStatus(status)
        }
    }

    class Factory(private val repository: WhatsAppStatusRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WhatsAppStatusViewModel(repository) as T
        }
    }
}

data class WhatsAppStatusUiState(
    val statuses: List<WhatsAppStatus> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
