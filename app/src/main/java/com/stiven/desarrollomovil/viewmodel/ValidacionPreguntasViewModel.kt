// Archivo: app/src/main/java/com/stiven/desarrollomovil/viewmodel/ValidacionPreguntasViewModel.kt

package com.stiven.desarrollomovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            // Inicia el estado de carga
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.obtenerPreguntasPendientes(cursoId, temaId)
                .onSuccess { preguntasRecibidas ->
                    // En caso de éxito, actualiza el estado con las preguntas
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            preguntas = preguntasRecibidas
                        )
                    }
                }
                .onFailure { exception ->
                    // En caso de fallo, actualiza el estado con el mensaje de error
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = "Error al cargar: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun aprobarPregunta(preguntaId: String, notas: String = "Pregunta aprobada sin modificaciones") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.aprobarPregunta(preguntaId, notas)
                .onSuccess {
                    // Si la API aprueba la pregunta, la filtramos de la lista local
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

    fun actualizarPregunta(pregunta: PreguntaIA, notas: String) {
        viewModelScope.launch {
            val id = pregunta.id ?: return@launch // No hacer nada si no hay ID

            _uiState.update { it.copy(isLoading = true) }

            repository.actualizarPregunta(id, pregunta)
                .onSuccess {
                    // Al actualizar, también la eliminamos de la lista de pendientes
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

    fun generarPreguntasIA(cursoId: String, temaId: String, temaTexto: String, cantidad: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.generarPreguntasIA(cursoId, temaId, temaTexto, cantidad)
                .onSuccess { response ->
                    _uiState.update { it.copy(isLoading = false, successMessage = "✓ ${response.total} preguntas generadas") }
                    // Después de generar, recargamos la lista para ver las nuevas preguntas
                    cargarPreguntasPendientes(cursoId, temaId)
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = "Error al generar: ${exception.message}") }
                }
        }
    }

    /**
     * Limpia los mensajes de error y éxito de la UI para que no se muestren repetidamente.
     */
    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
