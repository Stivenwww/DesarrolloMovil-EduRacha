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
import android.util.Log

data class SolicitudDocenteUiState(
    val solicitudes: List<SolicitudCurso> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null
)

class SolicitudDocenteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SolicitudDocenteUiState())
    val uiState: StateFlow<SolicitudDocenteUiState> = _uiState.asStateFlow()

    fun cargarSolicitudesDocente(docenteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            Log.d("SolicitudDocenteVM", " Cargando solicitudes del docente: $docenteId")

            try {
                val response = ApiClient.apiService.obtenerSolicitudesDocente(docenteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    Log.d("SolicitudDocenteVM", " Solicitudes cargadas: ${solicitudes.size}")
                    solicitudes.forEach {
                        Log.d("SolicitudDocenteVM", "  - ${it.estudianteNombre} → Curso ${it.cursoId} (${it.estado})")
                    }

                    _uiState.update {
                        it.copy(
                            solicitudes = solicitudes,
                            isLoading = false
                        )
                    }
                } else if (response.code() == 404) {
                    Log.i("SolicitudDocenteVM", " No hay solicitudes (404)")
                    _uiState.update {
                        it.copy(
                            solicitudes = emptyList(),
                            isLoading = false
                        )
                    }
                } else {
                    Log.e("SolicitudDocenteVM", " Error ${response.code()}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al cargar solicitudes: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocenteVM", " Excepción", e)
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
     * Cargar solicitudes de un curso específico
     */
    fun cargarSolicitudesPorCurso(cursoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = ApiClient.apiService.obtenerSolicitudesPorCurso(cursoId)

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
     * Aceptar solicitud
     */
    fun aceptarSolicitud(solicitudId: String, mensaje: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mensajeExito = null) }
            Log.d("SolicitudDocenteVM", " Aceptando solicitud $solicitudId")

            try {
                val respuesta = RespuestaSolicitudRequest(
                    aceptar = true,
                    mensaje = mensaje
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    Log.d("SolicitudDocenteVM", " Solicitud aceptada correctamente")

                    // Actualizar lista localmente removiendo la solicitud procesada
                    _uiState.update { state ->
                        state.copy(
                            solicitudes = state.solicitudes.map {
                                if (it.id == solicitudId) {
                                    it.copy(estado = EstadoSolicitud.ACEPTADA)
                                } else it
                            },
                            isLoading = false,
                            mensajeExito = "Solicitud aceptada correctamente"
                        )
                    }
                } else {
                    Log.e("SolicitudDocenteVM", " Error al aceptar: ${response.code()}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al aceptar solicitud: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocenteVM", " Excepción al aceptar", e)
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
     * Rechazar solicitud
     */
    fun rechazarSolicitud(solicitudId: String, mensaje: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mensajeExito = null) }
            Log.d("SolicitudDocenteVM", " Rechazando solicitud $solicitudId")

            try {
                val respuesta = RespuestaSolicitudRequest(
                    aceptar = false,
                    mensaje = mensaje
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    Log.d("SolicitudDocenteVM", " Solicitud rechazada correctamente")

                    //  Actualizar lista localmente
                    _uiState.update { state ->
                        state.copy(
                            solicitudes = state.solicitudes.map {
                                if (it.id == solicitudId) {
                                    it.copy(estado = EstadoSolicitud.RECHAZADA)
                                } else it
                            },
                            isLoading = false,
                            mensajeExito = "Solicitud rechazada"
                        )
                    }
                } else {
                    Log.e("SolicitudDocenteVM", " Error al rechazar: ${response.code()}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error al rechazar solicitud: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocenteVM", " Excepción al rechazar", e)
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
     * Limpiar mensajes
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}