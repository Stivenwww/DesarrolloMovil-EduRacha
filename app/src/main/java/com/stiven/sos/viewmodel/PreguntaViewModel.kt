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

    // Método auxiliar para recargar con los mismos filtros
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