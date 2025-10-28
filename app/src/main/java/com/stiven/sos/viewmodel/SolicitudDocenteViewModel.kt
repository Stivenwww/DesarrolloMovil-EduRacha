package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
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
     * ✅ Cargar solicitudes usando el endpoint específico de curso
     */
    fun cargarSolicitudesPorCurso(cursoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("SolicitudDocente", "════════════════════════════════")
                Log.d("SolicitudDocente", "🔍 Buscando solicitudes para cursoId: $cursoId")

                // ✅ Usar el endpoint específico del curso
                val response = ApiClient.apiService.obtenerSolicitudesPorCurso(cursoId)

                when {
                    response.isSuccessful -> {
                        val solicitudes = response.body() ?: emptyList()

                        Log.d("SolicitudDocente", "✅ Solicitudes encontradas: ${solicitudes.size}")
                        solicitudes.forEach {
                            Log.d("SolicitudDocente", "  - ${it.estudianteNombre} (${it.estado})")
                        }
                        Log.d("SolicitudDocente", "════════════════════════════════")

                        _uiState.update {
                            it.copy(
                                solicitudes = solicitudes,
                                isLoading = false
                            )
                        }
                    }

                    response.code() == 404 -> {
                        Log.w("SolicitudDocente", "⚠️ 404: No hay solicitudes para este curso")
                        _uiState.update {
                            it.copy(
                                solicitudes = emptyList(),
                                isLoading = false
                            )
                        }
                    }

                    else -> {
                        Log.e("SolicitudDocente", "❌ Error HTTP: ${response.code()}")
                        val errorBody = response.errorBody()?.string()
                        Log.e("SolicitudDocente", "Error body: $errorBody")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Error al cargar solicitudes: ${response.code()}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocente", "❌ Excepción:", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error de conexión: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Aceptar una solicitud
     */
    fun aceptarSolicitud(solicitudId: String, mensaje: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    solicitudEnProceso = solicitudId,
                    error = null,
                    mensajeExito = null
                )
            }

            try {
                Log.d("SolicitudDocente", "✅ Aceptando solicitud: $solicitudId")

                val respuesta = RespuestaSolicitudRequest(
                    aceptar = true,
                    mensaje = mensaje ?: "¡Bienvenido al curso! Tu solicitud ha sido aceptada."
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    Log.d("SolicitudDocente", "✅ Solicitud aceptada correctamente")

                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            mensajeExito = "Solicitud aceptada correctamente",
                            solicitudes = it.solicitudes.map { sol ->
                                if (sol.id == solicitudId) {
                                    sol.copy(estado = EstadoSolicitud.ACEPTADA)
                                } else sol
                            }
                        )
                    }
                } else {
                    Log.e("SolicitudDocente", "❌ Error al aceptar: ${response.code()}")

                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            error = "Error al aceptar solicitud: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocente", "❌ Excepción al aceptar:", e)

                _uiState.update {
                    it.copy(
                        solicitudEnProceso = null,
                        error = "Error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Rechazar una solicitud
     */
    fun rechazarSolicitud(solicitudId: String, mensaje: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    solicitudEnProceso = solicitudId,
                    error = null,
                    mensajeExito = null
                )
            }

            try {
                Log.d("SolicitudDocente", "⛔ Rechazando solicitud: $solicitudId")

                val respuesta = RespuestaSolicitudRequest(
                    aceptar = false,
                    mensaje = mensaje ?: "Lo sentimos, tu solicitud no pudo ser aceptada en este momento."
                )

                val response = ApiClient.apiService.responderSolicitud(solicitudId, respuesta)

                if (response.isSuccessful) {
                    Log.d("SolicitudDocente", "✅ Solicitud rechazada correctamente")

                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            mensajeExito = "Solicitud rechazada",
                            solicitudes = it.solicitudes.map { sol ->
                                if (sol.id == solicitudId) {
                                    sol.copy(estado = EstadoSolicitud.RECHAZADA)
                                } else sol
                            }
                        )
                    }
                } else {
                    Log.e("SolicitudDocente", "❌ Error al rechazar: ${response.code()}")

                    _uiState.update {
                        it.copy(
                            solicitudEnProceso = null,
                            error = "Error al rechazar solicitud: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SolicitudDocente", "❌ Excepción al rechazar:", e)

                _uiState.update {
                    it.copy(
                        solicitudEnProceso = null,
                        error = "Error: ${e.localizedMessage}"
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