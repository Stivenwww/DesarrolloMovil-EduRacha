package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.api.mensajeDeError
import com.stiven.sos.models.GenerarExplicacionRequest
import com.stiven.sos.models.Tema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la explicación de un tema, como lo ve el docente. */
enum class EstadoExplicacion { SIN_EXPLICACION, PENDIENTE, APROBADA, RECHAZADA }

fun Tema.estadoExplicacion(): EstadoExplicacion = when {
    explicacion.isNullOrBlank() -> EstadoExplicacion.SIN_EXPLICACION
    explicacionEstado == "aprobada" -> EstadoExplicacion.APROBADA
    explicacionEstado == "rechazada" -> EstadoExplicacion.RECHAZADA
    else -> EstadoExplicacion.PENDIENTE
}

/** Acción en curso sobre un tema (para mostrar el botón cargando y bloquear dobles toques). */
enum class AccionExplicacion { APROBAR, RECHAZAR, GUARDAR, ELIMINAR, GENERAR_IA, GENERAR_PDF }

data class ExplicacionesUiState(
    val temas: List<Tema> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    /** temaId -> acción que se está ejecutando */
    val enCurso: Map<String, AccionExplicacion> = emptyMap(),
    val mensaje: String? = null,
    /** Error de la última acción (por ejemplo, la IA falló): se muestra dentro del detalle del tema. */
    val errorAccion: Pair<String, String>? = null
)

/**
 * Gestión completa de las explicaciones de un curso (docente):
 * ver todas, crear o editar a mano, generar con IA o desde un PDF, aprobar, rechazar y eliminar.
 */
class ExplicacionesViewModel : ViewModel() {

    private val api = ApiClient.apiService
    private val _ui = MutableStateFlow(ExplicacionesUiState())
    val ui: StateFlow<ExplicacionesUiState> = _ui.asStateFlow()

    private var cursoId: String = ""

    fun iniciar(curso: String) {
        if (cursoId == curso && _ui.value.temas.isNotEmpty()) return
        cursoId = curso
        cargar()
    }

    fun cargar(silencioso: Boolean = false) {
        if (cursoId.isBlank()) return
        viewModelScope.launch {
            if (!silencioso) _ui.update { it.copy(cargando = true, error = null) }
            try {
                val r = api.obtenerTemasPorCurso(cursoId, "todos")
                if (r.isSuccessful) {
                    val temas = r.body().orEmpty().filter { !it.id.isNullOrBlank() }.sortedBy { it.orden }
                    _ui.update { it.copy(temas = temas, cargando = false, error = null) }
                } else {
                    _ui.update { it.copy(cargando = false, error = mensajeDeError(r)) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = "No se pudo conectar con el servidor. Revisa que el backend esté encendido.") }
            }
        }
    }

    private fun ejecutar(
        temaId: String,
        accion: AccionExplicacion,
        exito: String,
        llamada: suspend () -> retrofit2.Response<*>
    ) {
        if (_ui.value.enCurso.containsKey(temaId)) return
        viewModelScope.launch {
            _ui.update { it.copy(enCurso = it.enCurso + (temaId to accion), errorAccion = null) }
            try {
                val r = llamada()
                if (r.isSuccessful) {
                    _ui.update { it.copy(mensaje = exito) }
                    cargar(silencioso = true)
                } else {
                    _ui.update { it.copy(errorAccion = temaId to mensajeDeError(r)) }
                }
            } catch (e: Exception) {
                val texto = if (accion == AccionExplicacion.GENERAR_IA || accion == AccionExplicacion.GENERAR_PDF)
                    "La IA tardó demasiado o no respondió. Puedes intentarlo de nuevo o escribir la explicación manualmente."
                else "No se pudo conectar con el servidor."
                _ui.update { it.copy(errorAccion = temaId to texto) }
            } finally {
                _ui.update { it.copy(enCurso = it.enCurso - temaId) }
            }
        }
    }

    fun aprobar(temaId: String) = ejecutar(temaId, AccionExplicacion.APROBAR, "Explicación aprobada. Ya se pueden generar preguntas del tema.") {
        api.actualizarEstadoExplicacion(cursoId, temaId, mapOf("estado" to "aprobada"))
    }

    fun rechazar(temaId: String) = ejecutar(temaId, AccionExplicacion.RECHAZAR, "Explicación rechazada. Edítala o genera una nueva.") {
        api.actualizarEstadoExplicacion(cursoId, temaId, mapOf("estado" to "rechazada"))
    }

    fun guardar(temaId: String, texto: String, aprobar: Boolean) = ejecutar(
        temaId, AccionExplicacion.GUARDAR,
        if (aprobar) "Explicación guardada y aprobada." else "Explicación guardada. Queda pendiente de aprobación."
    ) {
        api.guardarExplicacion(cursoId, temaId, mapOf("explicacion" to texto.trim(), "estado" to if (aprobar) "aprobada" else "pendiente"))
    }

    fun eliminar(temaId: String) = ejecutar(temaId, AccionExplicacion.ELIMINAR, "Explicación eliminada.") {
        api.eliminarExplicacion(cursoId, temaId)
    }

    fun generarConIA(tema: Tema) = ejecutar(tema.id, AccionExplicacion.GENERAR_IA, "La IA escribió una nueva explicación. Revísala y apruébala.") {
        api.generarExplicacionIA(
            cursoId, tema.id,
            GenerarExplicacionRequest(cursoId = cursoId, temaId = tema.id, tituloTema = tema.titulo ?: "", contenidoTema = tema.contenido?.ifBlank { null })
        )
    }

    fun generarDesdePdf(temaId: String, archivo: okhttp3.MultipartBody.Part) =
        ejecutar(temaId, AccionExplicacion.GENERAR_PDF, "La IA escribió la explicación con el PDF. Revísala y apruébala.") {
            api.generarExplicacionDesdePdf(cursoId, temaId, archivo)
        }

    fun errorLocal(temaId: String, texto: String) = _ui.update { it.copy(errorAccion = temaId to texto) }
    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null) }
    fun limpiarErrorAccion() = _ui.update { it.copy(errorAccion = null) }
}
