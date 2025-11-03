package com.stiven.sos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.*
import com.stiven.sos.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SolicitudDocenteUiState(
    val solicitudes: List<SolicitudCurso> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null,
    val solicitudEnProceso: String? = null
)

class SolicitudDocenteViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(SolicitudDocenteUiState())
    val uiState: StateFlow<SolicitudDocenteUiState> = _uiState.asStateFlow()

    /**
     * Cargar solicitudes filtradas por curso
     */
    fun cargarSolicitudesPorCurso(cursoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = ApiClient.apiService.obtenerSolicitudesPorCurso(cursoId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    _uiState.update { it.copy(solicitudes = solicitudes, isLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Error al cargar solicitudes: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error de conexión: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * ✅ Aceptar solicitud con mensaje opcional
     */
    fun aceptarSolicitud(solicitudId: String, mensaje: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(solicitudEnProceso = solicitudId, error = null, mensajeExito = null) }

            try {
                val respuesta = RespuestaSolicitudRequest(
                    aceptar = true,
                    mensaje = mensaje
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            mensajeExito = "Solicitud aceptada correctamente",
                            solicitudes = it.solicitudes.map { sol ->
                                if (sol.id == solicitudId) {
                                    // ✅ Crear nueva solicitud con el mensaje actualizado
                                    sol.copy(
                                        estado = EstadoSolicitud.ACEPTADA,
                                        mensaje = mensaje,  // Backend usa "mensaje" para ambos
                                        fechaRespuesta = System.currentTimeMillis().toString()
                                    )
                                } else sol
                            }
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            error = "Error al aceptar solicitud: ${response.code()} - ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(solicitudEnProceso = null, error = "Error al aceptar: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * ✅ Rechazar una solicitud con mensaje
     */
    fun rechazarSolicitud(solicitudId: String, mensaje: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(solicitudEnProceso = solicitudId, error = null, mensajeExito = null) }

            try {
                val respuesta = RespuestaSolicitudRequest(
                    aceptar = false,
                    mensaje = mensaje
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            mensajeExito = "Solicitud rechazada",
                            solicitudes = it.solicitudes.map { sol ->
                                if (sol.id == solicitudId) {
                                    sol.copy(
                                        estado = EstadoSolicitud.RECHAZADA,
                                        mensaje = mensaje,  // Backend usa "mensaje" para ambos
                                        fechaRespuesta = System.currentTimeMillis().toString()
                                    )
                                } else sol
                            }
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            error = "Error al rechazar solicitud: ${response.code()} - ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(solicitudEnProceso = null, error = "Error al rechazar: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Reset de mensajes UI
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}