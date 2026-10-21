// Archivo: app/src/main/java/com/stiven/sos/viewmodel/PreguntaViewModel.kt

package com.stiven.sos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.models.Pregunta
import com.stiven.sos.models.PreguntasIAResponse
import com.stiven.sos.repository.PreguntaRepository
import com.stiven.sos.repository.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PreguntasUiState(
    val preguntas: List<Pregunta> = emptyList(),
    val preguntaSeleccionada: Pregunta? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    // Guardar el último filtro usado para recargas
    val lastCursoId: String? = null,
    val lastEstado: String? = null
)

class PreguntaViewModel : ViewModel() {

    private val repository = PreguntaRepository()

    private val _uiState = MutableStateFlow(PreguntasUiState())
    val uiState: StateFlow<PreguntasUiState> = _uiState.asStateFlow()

    fun cargarPreguntas(cursoId: String? = null, estado: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                lastCursoId = cursoId,
                lastEstado = estado
            )

            when (val result = repository.obtenerPreguntas(cursoId, estado)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        preguntas = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun recargarConFiltrosActuales() {
        val currentState = _uiState.value
        cargarPreguntas(
            cursoId = currentState.lastCursoId,
            estado = currentState.lastEstado
        )
    }

    fun cargarPreguntaPorId(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.obtenerPreguntaPorId(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        preguntaSeleccionada = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun crearPregunta(pregunta: Pregunta, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.crearPregunta(pregunta)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Pregunta creada correctamente",
                        isLoading = false
                    )
                    onSuccess?.invoke()
                    recargarConFiltrosActuales()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    /**
     * Guarda una pregunta del formulario manual: la crea (idEdicion = null) o actualiza una existente.
     * Ignora toques repetidos mientras guarda, para no crear duplicados.
     * onGuardada recibe el id definitivo de la pregunta.
     */
    fun guardarPreguntaManual(pregunta: Pregunta, idEdicion: String?, onGuardada: (String) -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val resultado = if (idEdicion == null) repository.crearPregunta(pregunta)
                            else repository.actualizarPregunta(idEdicion, pregunta.copy(id = idEdicion)).let { r ->
                                if (r is ApiResult.Success) ApiResult.Success(idEdicion) else r
                            }
            when (resultado) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = if (idEdicion == null) "Pregunta guardada. Ya puedes escribir la siguiente."
                                         else "Cambios guardados."
                    )
                    onGuardada(resultado.data)
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = resultado.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun actualizarPregunta(id: String, pregunta: Pregunta, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.actualizarPregunta(id, pregunta)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data,
                        isLoading = false
                    )
                    onSuccess?.invoke()
                    recargarConFiltrosActuales()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun eliminarPregunta(id: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.eliminarPregunta(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data,
                        isLoading = false
                    )
                    onSuccess?.invoke()
                    recargarConFiltrosActuales()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun actualizarEstadoPregunta(
        id: String,
        estado: String,
        notas: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.actualizarEstadoPregunta(id, estado, notas)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data.message,
                        isLoading = false
                    )
                    onSuccess?.invoke()
                    // CORRECCIÓN: Recargar con los mismos filtros en lugar de sin filtros
                    recargarConFiltrosActuales()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun generarPreguntasIA(
        cursoId: String,
        temaId: String,
        temaTexto: String,
        cantidad: Int = 5,
        onSuccess: ((PreguntasIAResponse) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.generarPreguntasIA(cursoId, temaId, temaTexto, cantidad)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data.message,
                        isLoading = false
                    )
                    onSuccess?.invoke(result.data)
                    // Aquí sí queremos recargar con el cursoId específico
                    cargarPreguntas(cursoId, _uiState.value.lastEstado)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    // ===================== Generación con IA por lotes =====================

    /** Avance de una generación con IA en curso (null = no hay ninguna). */
    data class ProgresoIA(
        val total: Int,
        val hechas: Int,
        val lote: Int,
        val lotes: Int,
        val esperando: Boolean = false,
        val mensaje: String = ""
    )

    /** Preguntas creadas o generadas en esta visita a "Crear preguntas" (sobreviven a girar la pantalla). */
    val preguntasSesion = androidx.compose.runtime.mutableStateListOf<com.stiven.sos.PreguntaSesion>()

    private val _progresoIA = MutableStateFlow<ProgresoIA?>(null)
    val progresoIA: StateFlow<ProgresoIA?> = _progresoIA.asStateFlow()
    private var trabajoIA: kotlinx.coroutines.Job? = null

    companion object {
        /** Tope por solicitud del docente (el servidor acepta hasta 30). */
        const val MAX_PREGUNTAS_IA = 30
        /** Preguntas por llamada: lotes pequeños responden rápido y no saturan la API de la IA. */
        const val LOTE_IA = 10
        /** Pausa entre lotes para respetar el límite de solicitudes por minuto del proveedor. */
        const val PAUSA_ENTRE_LOTES_MS = 4_000L
        /** Si la IA responde "demasiadas solicitudes", se espera esto y se reintenta una vez el mismo lote. */
        const val ESPERA_LIMITE_MS = 25_000L

        fun lotesPara(total: Int): List<Int> {
            val t = total.coerceIn(1, MAX_PREGUNTAS_IA)
            return List((t + LOTE_IA - 1) / LOTE_IA) { i -> minOf(LOTE_IA, t - i * LOTE_IA) }
        }

        /** Segundos aproximados que tarda una generación (para avisar al docente antes de empezar). */
        fun segundosEstimados(total: Int): Int {
            val lotes = lotesPara(total)
            return lotes.sumOf { 8 + it * 2 } + (lotes.size - 1) * (PAUSA_ENTRE_LOTES_MS / 1000).toInt()
        }
    }

    /**
     * Genera [total] preguntas (máximo 30) en lotes de 10 con una pausa entre lotes.
     * Cada lote se guarda en el servidor apenas llega, así que si uno falla no se pierden los anteriores.
     * [onLote] recibe las preguntas de cada lote para mostrarlas sin salir de la pantalla.
     */
    fun generarPreguntasPorLotes(
        cursoId: String,
        temaId: String,
        temaTexto: String,
        total: Int,
        onLote: (List<Pregunta>) -> Unit,
        onFin: (generadas: Int, error: String?) -> Unit
    ) {
        if (trabajoIA?.isActive == true) return
        val lotes = lotesPara(total)
        val pedidas = lotes.sum()
        trabajoIA = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, successMessage = null)
            var hechas = 0
            var error: String? = null
            try {
                for ((i, cantidad) in lotes.withIndex()) {
                    if (i > 0) {
                        _progresoIA.value = ProgresoIA(pedidas, hechas, i + 1, lotes.size, esperando = true,
                            mensaje = "Pausa corta para no saturar la IA…")
                        kotlinx.coroutines.delay(PAUSA_ENTRE_LOTES_MS)
                    }
                    _progresoIA.value = ProgresoIA(pedidas, hechas, i + 1, lotes.size,
                        mensaje = "Generando lote ${i + 1} de ${lotes.size} ($cantidad preguntas)…")
                    var resultado = repository.generarPreguntasIA(cursoId, temaId, temaTexto, cantidad)
                    if (resultado is ApiResult.Error && esLimite(resultado.message)) {
                        _progresoIA.value = ProgresoIA(pedidas, hechas, i + 1, lotes.size, esperando = true,
                            mensaje = "La IA pidió una pausa. Reintentando en ${ESPERA_LIMITE_MS / 1000} s…")
                        kotlinx.coroutines.delay(ESPERA_LIMITE_MS)
                        resultado = repository.generarPreguntasIA(cursoId, temaId, temaTexto, cantidad)
                    }
                    when (resultado) {
                        is ApiResult.Success -> {
                            val nuevas = resultado.data.preguntas.orEmpty()
                            hechas += nuevas.size
                            onLote(nuevas)
                            _progresoIA.value = ProgresoIA(pedidas, hechas, i + 1, lotes.size, mensaje = "Lote ${i + 1} listo")
                        }
                        is ApiResult.Error -> { error = resultado.message; break }
                        is ApiResult.Loading -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                error = if (hechas > 0) "Generación detenida. Se guardaron $hechas preguntas." else "Generación detenida."
            } catch (e: Exception) {
                error = e.message ?: "Error inesperado al generar preguntas"
            } finally {
                _progresoIA.value = null
                val mensajeFinal = when {
                    error == null -> "Se generaron $hechas preguntas. Quedan pendientes de revisión."
                    hechas > 0 -> "Se generaron $hechas de $pedidas preguntas. $error"
                    else -> null
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = mensajeFinal,
                    error = if (hechas == 0) error else null
                )
                onFin(hechas, error)
            }
        }
    }

    fun detenerGeneracion() { trabajoIA?.cancel() }

    private fun esLimite(mensaje: String?): Boolean {
        val m = mensaje?.lowercase() ?: return false
        return "demasiadas solicitudes" in m || "429" in m || "rate limit" in m || "límite" in m
    }

    /** Aprueba de una vez todas las pendientes del curso (o de un tema). */
    fun aprobarPendientes(cursoId: String, temaId: String?, onListo: () -> Unit = {}) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val datos = buildMap { put("cursoId", cursoId); if (!temaId.isNullOrBlank()) put("temaId", temaId) }
                val r = com.stiven.sos.api.ApiClient.apiService.aprobarPendientes(datos)
                if (r.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = r.body()?.get("message") ?: "Preguntas aprobadas")
                    onListo()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = com.stiven.sos.api.mensajeDeError(r))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No se pudo conectar con el servidor.")
            }
        }
    }

    fun limpiarCache() {
        viewModelScope.launch {
            when (val result = repository.limpiarCache()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = result.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun clearSelectedPregunta() {
        _uiState.value = _uiState.value.copy(preguntaSeleccionada = null)
    }
}