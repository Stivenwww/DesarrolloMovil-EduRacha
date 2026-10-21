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

    /**
     * Genera preguntas con IA usando el contenido real de un PDF (lo lee el servidor).
     * Si el PDF no se puede leer, el error del servidor llega tal cual (escaneado, protegido, dañado...).
     */
    fun generarPreguntasDesdePdf(
        archivo: okhttp3.MultipartBody.Part,
        cursoId: String,
        temaId: String,
        cantidad: Int,
        dificultad: String?,
        onListo: (PreguntasIAResponse) -> Unit
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val t = com.stiven.sos.utils.ArchivosPdf::texto
                val resp = com.stiven.sos.api.ApiClient.apiService.generarPreguntasDesdePdf(
                    archivo, t(cursoId), t(temaId), t(cantidad.toString()), t(dificultad ?: "mixta")
                )
                val cuerpo = resp.body()
                if (resp.isSuccessful && cuerpo != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = cuerpo.message)
                    onListo(cuerpo)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = com.stiven.sos.api.mensajeDeError(resp))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se pudo conectar con el servidor. Revisa que el backend esté encendido."
                )
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