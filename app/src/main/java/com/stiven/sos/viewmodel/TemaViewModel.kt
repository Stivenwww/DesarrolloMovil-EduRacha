package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.models.Tema
import com.stiven.sos.repository.ApiResult
import com.stiven.sos.repository.TemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TemasUiState(
    val temas: List<Tema> = emptyList(),
    val temaSeleccionado: Tema? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class TemaViewModel : ViewModel() {

    private val repository = TemaRepository()

    private val _uiState = MutableStateFlow(TemasUiState())
    val uiState: StateFlow<TemasUiState> = _uiState.asStateFlow()

    fun cargarTemasPorCurso(cursoId: String, estado: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.obtenerTemasPorCurso(cursoId, estado)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        temas = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun cargarTemaPorId(cursoId: String, temaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.obtenerTemaPorId(cursoId, temaId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        temaSeleccionado = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearTemaSeleccionado() {
        _uiState.value = _uiState.value.copy(temaSeleccionado = null)
    }
}