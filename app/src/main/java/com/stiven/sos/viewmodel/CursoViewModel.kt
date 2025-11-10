package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
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

    //  Flag para evitar múltiples llamadas simultáneas
    private var isLoadingCursos = false
    private var isLoadingSolicitudes = false



    fun obtenerCursos() {
        // ✅ Prevenir llamadas duplicadas
        if (isLoadingCursos) {
            Log.w("CursoViewModel", "⚠️ Ya se están cargando cursos, ignorando llamada duplicada")
            return
        }

        viewModelScope.launch {
            isLoadingCursos = true
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.obtenerCursos()
                .onSuccess { listaCursos ->
                    Log.d("CursoViewModel", "✅ Cursos obtenidos: ${listaCursos.size}")
                    _uiState.update { it.copy(isLoading = false, cursos = listaCursos) }
                    cargarSolicitudesPendientes()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error obteniendo cursos", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al cargar"
                        )
                    }
                }
                .also {
                    isLoadingCursos = false
                }
        }
    }

    private fun cargarSolicitudesPendientes() {
        // ✅ Prevenir llamadas duplicadas
        if (isLoadingSolicitudes) {
            Log.w("CursoViewModel", "⚠️ Ya se están cargando solicitudes, ignorando")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingSolicitudes = true

                val docenteId = obtenerDocenteId()
                Log.d("CursoViewModel", "🔍 Cargando solicitudes para docente: $docenteId")

                val response = ApiClient.apiService.obtenerSolicitudesDocente(docenteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    Log.d("CursoViewModel", "📨 Total solicitudes recibidas: ${solicitudes.size}")

                    // Filtrar solicitudes pendientes
                    val solicitudesPendientes = solicitudes.filter {
                        it.estado == EstadoSolicitud.PENDIENTE
                    }

                    Log.d("CursoViewModel", "📊 Solicitudes PENDIENTES: ${solicitudesPendientes.size}")

                    val solicitudesPorCurso = solicitudesPendientes
                        .groupBy { it.cursoId }
                        .mapValues { (cursoId, lista) ->
                            val count = lista.size
                            Log.d("CursoViewModel", "   - Curso $cursoId: $count pendientes")
                            count
                        }

                    Log.d("CursoViewModel", "✅ Total cursos con solicitudes: ${solicitudesPorCurso.size}")
                    _uiState.update { it.copy(solicitudesPorCurso = solicitudesPorCurso) }

                } else if (response.code() == 404) {
                    Log.i("CursoViewModel", "ℹ️ No hay solicitudes pendientes (404)")
                    _uiState.update { it.copy(solicitudesPorCurso = emptyMap()) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CursoViewModel", "❌ Error ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("CursoViewModel", "❌ Excepción al cargar solicitudes", e)
            } finally {
                isLoadingSolicitudes = false
            }
        }
    }

    private fun obtenerDocenteId(): String {
        val id = sessionManager.getUserId()
        if (id.isNullOrBlank()) {
            throw IllegalStateException("No hay sesión de docente activa")
        }
        return id
    }

    fun crearCurso(curso: Curso) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.crearCurso(curso)
                .onSuccess { response ->
                    Log.d("CursoViewModel", "✅ Curso creado exitosamente")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso creado exitosamente"
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error creando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al crear"
                        )
                    }
                }
        }
    }

    fun actualizarCurso(curso: Curso) {
        val id = curso.id

        if (id.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "ID del curso no encontrado. No se puede actualizar."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.actualizarCurso(id, curso)
                .onSuccess {
                    Log.d("CursoViewModel", "✅ Curso ${curso.titulo} actualizado")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso '${curso.titulo}' actualizado"
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error actualizando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al actualizar"
                        )
                    }
                }
        }
    }

    fun eliminarCurso(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.eliminarCurso(id)
                .onSuccess {
                    Log.d("CursoViewModel", " Curso eliminado")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso eliminado exitosamente."
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", " Error eliminando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al eliminar"
                        )
                    }
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