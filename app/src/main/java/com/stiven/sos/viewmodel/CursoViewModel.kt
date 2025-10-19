package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.models.Curso
import com.stiven.sos.repository.CursoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.onSuccess

data class CursoUiState(
    val cursos: List<Curso> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val operationSuccess: String? = null
)

class CursoViewModel : ViewModel() {

    private val repository = CursoRepository()

    private val _uiState = MutableStateFlow(CursoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        obtenerCursos()
    }

    fun obtenerCursos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.obtenerCursos()
                .onSuccess { listaCursos ->
                    _uiState.update { it.copy(isLoading = false, cursos = listaCursos) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al cargar") }
                }
        }
    }

    fun crearCurso(curso: Curso) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.crearCurso(curso)
                .onSuccess { response ->
                    _uiState.update { it.copy(isLoading = false, operationSuccess = "Curso creado exitosamente") }
                    obtenerCursos() // Refrescar la lista
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al crear") }
                }
        }
    }

    /**
     * --- ¡AQUÍ ESTÁ LA CORRECCIÓN CLAVE! ---
     * Se añade una validación robusta para el ID antes de llamar al repositorio.
     */
    fun actualizarCurso(curso: Curso) {
        // 1. Extraer el ID del curso de forma segura.
        val id = curso.id

        // 2. Comprobar que el ID no sea nulo ni esté en blanco.
        if (id.isNullOrBlank()) {
            // Si el ID es inválido, actualiza la UI con un error claro y no continúes.
            _uiState.update { it.copy(isLoading = false, error = "ID del curso no encontrado. No se puede actualizar.") }
            return // Detiene la ejecución de la función aquí.
        }

        // 3. Si el ID es válido, procede con la llamada a la API.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            // Llama al repositorio con el ID validado.
            repository.actualizarCurso(id, curso)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, operationSuccess = "Curso '${curso.titulo}' actualizado") }
                    obtenerCursos() // Refrescar la lista para que la UI muestre los cambios.
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al actualizar") }
                }
        }
    }

    fun eliminarCurso(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.eliminarCurso(id)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, operationSuccess = "Curso eliminado exitosamente.") }
                    obtenerCursos() // Refrescar la lista
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al eliminar") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(operationSuccess = null) }
    }
}
