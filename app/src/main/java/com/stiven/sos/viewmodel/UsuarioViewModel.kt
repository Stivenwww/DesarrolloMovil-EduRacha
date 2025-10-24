package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.models.UsuarioAsignado
import com.stiven.sos.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuarioUiState(
    val usuarios: List<UsuarioAsignado> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val cursoTitulo: String = ""
)

class UsuarioViewModel : ViewModel() {

    private val repository = UsuarioRepository()

    private val _uiState = MutableStateFlow(UsuarioUiState())
    val uiState = _uiState.asStateFlow()

    fun cargarEstudiantesPorCurso(cursoId: String, cursoTitulo: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    cursoTitulo = cursoTitulo
                )
            }

            repository.obtenerEstudiantesPorCurso(cursoId)
                .onSuccess { listaUsuarios ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            usuarios = listaUsuarios
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error al cargar estudiantes"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}