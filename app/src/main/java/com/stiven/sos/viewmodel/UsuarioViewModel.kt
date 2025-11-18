package com.stiven.sos.viewmodel

import android.util.Log
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

    fun cargarEstudiantesPorCurso(cursoId: String, cursoTitulo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, cursoTitulo = cursoTitulo) }

            Log.d("UsuarioViewModel", "🔍 Cargando estudiantes del curso: $cursoId")

            try {
                val response = ApiClient.apiService.obtenerEstudiantesPorCurso(cursoId)

                Log.d("UsuarioViewModel", "📡 Response code: ${response.code()}")
                Log.d("UsuarioViewModel", "📡 Response successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val estudiantes = response.body() ?: emptyList()

                    Log.d("UsuarioViewModel", "✅ Total estudiantes recibidos: ${estudiantes.size}")
                    estudiantes.forEachIndexed { index, estudiante ->
                        Log.d("UsuarioViewModel", """
                            📋 Estudiante #${index + 1}:
                            - UID: ${estudiante.uid}
                            - Nombre: ${estudiante.nombre}
                            - Email: ${estudiante.correo}
                            - Estado: ${estudiante.estado}
                        """.trimIndent())
                    }

                    _uiState.update {
                        it.copy(
                            usuarios = estudiantes,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UsuarioViewModel", "❌ Error HTTP: ${response.code()}")
                    Log.e("UsuarioViewModel", "❌ Error body: $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar estudiantes: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("UsuarioViewModel", "❌ Exception al cargar estudiantes", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error de conexión: ${e.message}"
                    )
                }
            }
        }
    }

    fun cambiarEstadoEstudiante(
        cursoId: String,
        estudianteId: String,
        nuevoEstado: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d("UsuarioViewModel", """
                 Cambiando estado:
                - Curso: $cursoId
                - Estudiante: $estudianteId
                - Nuevo estado: $nuevoEstado
            """.trimIndent())

            try {
                val response = ApiClient.apiService.cambiarEstadoEstudiante(
                    cursoId = cursoId,
                    estudianteId = estudianteId,
                    estado = mapOf("estado" to nuevoEstado)
                )

                if (response.isSuccessful) {
                    Log.d("UsuarioViewModel", " Estado cambiado exitosamente")
                    // Recargar la lista de estudiantes después de cambiar el estado
                    val cursoTitulo = _uiState.value.cursoTitulo
                    cargarEstudiantesPorCurso(cursoId, cursoTitulo)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UsuarioViewModel", " Error al cambiar estado: ${response.code()}")
                    Log.e("UsuarioViewModel", " Error body: $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cambiar estado: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("UsuarioViewModel", " Exception al cambiar estado", e)
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