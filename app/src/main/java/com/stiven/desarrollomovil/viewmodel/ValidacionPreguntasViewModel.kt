// Archivo: app/src/main/java/com/stiven/desarrollomovil/viewmodel/ValidacionPreguntasViewModel.kt

package com.stiven.desarrollomovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.desarrollomovil.models.OpcionIA
import com.stiven.desarrollomovil.models.PreguntaIA
import com.stiven.desarrollomovil.repository.PreguntasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ValidacionUiState(
    val isLoading: Boolean = false,
    val preguntas: List<PreguntaIA> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class ValidacionPreguntasViewModel : ViewModel() {

    private val repository = PreguntasRepository()

    private val _uiState = MutableStateFlow(ValidacionUiState())
    val uiState = _uiState.asStateFlow()

    fun cargarPreguntasPendientes(cursoId: String, temaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.obtenerPreguntasPendientes(cursoId, temaId)
                .onSuccess { preguntasRecibidas ->
                    _uiState.update { it.copy(isLoading = false, preguntas = preguntasRecibidas) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar: ${exception.message}") }
                }
        }
    }

    fun cargarTodasLasPreguntasPendientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.obtenerTodasLasPreguntasPendientes()
                .onSuccess { listaCompleta ->
                    _uiState.update { it.copy(isLoading = false, preguntas = listaCompleta) }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message, preguntas = emptyList()) }
                }
        }
    }

    fun aprobarPregunta(preguntaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.aprobarPregunta(preguntaId)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            preguntas = currentState.preguntas.filter { it.id != preguntaId },
                            successMessage = "✓ Pregunta aprobada"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun rechazarPregunta(preguntaId: String, motivo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.rechazarPregunta(preguntaId, motivo)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            preguntas = currentState.preguntas.filter { it.id != preguntaId },
                            successMessage = "✗ Pregunta rechazada"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun actualizarPregunta(pregunta: PreguntaIA) {
        viewModelScope.launch {
            val id = pregunta.id ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            repository.actualizarPregunta(id, pregunta)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            preguntas = currentState.preguntas.filter { it.id != id },
                            successMessage = "✓ Pregunta actualizada"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    /**
     * --- ¡FUNCIÓN AÑADIDA! ---
     * Limpia los mensajes de error y éxito para que no se muestren repetidamente.
     */
    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
