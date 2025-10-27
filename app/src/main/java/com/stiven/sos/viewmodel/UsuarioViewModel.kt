package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.UsuarioAsignado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _uiState = MutableStateFlow(UsuarioUiState())
    val uiState: StateFlow<UsuarioUiState> = _uiState.asStateFlow()

    /**
     * Carga los estudiantes asignados a un curso
     */
    fun cargarEstudiantesPorCurso(cursoId: String, cursoTitulo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, cursoTitulo = cursoTitulo) }

            try {
                val response = ApiClient.apiService.obtenerEstudiantesPorCurso(cursoId)

                if (response.isSuccessful) {
                    val estudiantes = response.body() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            usuarios = estudiantes,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar estudiantes: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error de conexión: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cambia el estado de un estudiante (activo, inactivo, eliminado)
     */
    fun cambiarEstadoEstudiante(
        cursoId: String,
        estudianteId: String,
        nuevoEstado: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = ApiClient.apiService.cambiarEstadoEstudiante(
                    cursoId = cursoId,
                    estudianteId = estudianteId,
                    estado = mapOf("estado" to nuevoEstado)
                )

                if (response.isSuccessful) {
                    // Recargar la lista de estudiantes después de cambiar el estado
                    val cursoTitulo = _uiState.value.cursoTitulo
                    cargarEstudiantesPorCurso(cursoId, cursoTitulo)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cambiar estado: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Limpia el error actual
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}