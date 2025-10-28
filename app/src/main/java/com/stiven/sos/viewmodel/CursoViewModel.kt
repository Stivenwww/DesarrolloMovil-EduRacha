package com.stiven.sos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Curso
import com.stiven.sos.repository.CursoRepository
import com.stiven.sos.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CursoUiState(
    val cursos: List<Curso> = emptyList(),
    val solicitudesPorCurso: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val operationSuccess: String? = null
)

class CursoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CursoRepository()
    private val sessionManager = SessionManager.getInstance(application)

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
                    cargarSolicitudesPendientes()
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al cargar") }
                }
        }
    }

    /**
     * Cargar el conteo de solicitudes pendientes para cada curso
     */
    private fun cargarSolicitudesPendientes() {
        viewModelScope.launch {
            try {
                val docenteId = obtenerDocenteId()

                val response = ApiClient.apiService.obtenerSolicitudesDocente(docenteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()

                    val solicitudesPorCurso = solicitudes
                        .filter { it.estado.name == "PENDIENTE" }
                        .groupBy { it.cursoId }
                        .mapValues { it.value.size }

                    _uiState.update { it.copy(solicitudesPorCurso = solicitudesPorCurso) }
                } else if (response.code() == 404) {
                    // No hay solicitudes pendientes
                    _uiState.update { it.copy(solicitudesPorCurso = emptyMap()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Obtener el ID del docente actual
     */
    private fun obtenerDocenteId(): String {
        return sessionManager.getUserId()
            ?: throw IllegalStateException("No hay sesión de docente activa")
    }

    fun crearCurso(curso: Curso) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.crearCurso(curso)
                .onSuccess { response ->
                    _uiState.update { it.copy(isLoading = false, operationSuccess = "Curso creado exitosamente") }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Error desconocido al crear") }
                }
        }
    }

    fun actualizarCurso(curso: Curso) {
        val id = curso.id

        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "ID del curso no encontrado. No se puede actualizar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.actualizarCurso(id, curso)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, operationSuccess = "Curso '${curso.titulo}' actualizado") }
                    obtenerCursos()
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
                    obtenerCursos()
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