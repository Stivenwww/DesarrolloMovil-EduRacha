package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SolicitudUiState(
    val solicitudes: List<SolicitudCurso> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null
)

class SolicitudViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SolicitudUiState())
    val uiState: StateFlow<SolicitudUiState> = _uiState.asStateFlow()

    /**
     * Crear una solicitud para unirse a un curso
     */
    fun crearSolicitud(
        codigoCurso: String,
        estudianteId: String,
        estudianteNombre: String,
        estudianteEmail: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mensajeExito = null) }

            try {
                val request = SolicitudRequest(
                    codigoCurso = codigoCurso,
                    estudianteId = estudianteId,
                    estudianteNombre = estudianteNombre,
                    estudianteEmail = estudianteEmail
                )

                val response = ApiClient.apiService.crearSolicitudCurso(request)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mensajeExito = "Solicitud enviada correctamente"
                        )
                    }
                    // Recargar solicitudes
                    cargarSolicitudesEstudiante(estudianteId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al enviar solicitud: ${response.code()}"
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
     * Cargar solicitudes de un estudiante
     */
    fun cargarSolicitudesEstudiante(estudianteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = ApiClient.apiService.obtenerSolicitudesEstudiante(estudianteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            solicitudes = solicitudes,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar solicitudes: ${response.code()}"
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
     * Cargar solicitudes de un docente
     */
    fun cargarSolicitudesDocente(docenteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = ApiClient.apiService.obtenerSolicitudesDocente(docenteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            solicitudes = solicitudes,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar solicitudes: ${response.code()}"
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
     * Responder a una solicitud (aceptar o rechazar)
     */
    fun responderSolicitud(
        solicitudId: String,
        aceptar: Boolean,
        mensaje: String? = null,
        docenteId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mensajeExito = null) }

            try {
                val respuesta = RespuestaSolicitudRequest(
                    aceptar = aceptar,
                    mensaje = mensaje
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    val mensajeExito = if (aceptar) "Solicitud aceptada" else "Solicitud rechazada"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mensajeExito = mensajeExito
                        )
                    }
                    // Recargar solicitudes
                    cargarSolicitudesDocente(docenteId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al responder solicitud: ${response.code()}"
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
     * Limpiar mensajes de error y éxito
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}