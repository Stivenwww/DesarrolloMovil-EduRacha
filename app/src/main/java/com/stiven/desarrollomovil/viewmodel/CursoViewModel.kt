package com.stiven.desarrollomovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.desarrollomovil.models.Curso
import com.stiven.desarrollomovil.repository.CursoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CursoViewModel : ViewModel() {

    private val repository = CursoRepository()

    private val _cursos = MutableStateFlow<List<Curso>>(emptyList())
    val cursos: StateFlow<List<Curso>> = _cursos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Obtener todos los cursos
    fun obtenerCursos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.obtenerCursos()
                .onSuccess { listaCursos ->
                    _cursos.value = listaCursos
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                }

            _isLoading.value = false
        }
    }

    // Crear un nuevo curso - CORREGIDO con onError
    fun crearCurso(curso: Curso, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.crearCurso(curso)
                .onSuccess { response ->
                    onSuccess(response.id ?: "")
                    obtenerCursos() // Recargar la lista
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Error al crear curso"
                    _error.value = errorMsg
                    onError(errorMsg) // ESTO FALTABA
                }

            _isLoading.value = false
        }
    }

    // Actualizar un curso
    fun actualizarCurso(id: String, curso: Curso, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.actualizarCurso(id, curso)
                .onSuccess {
                    onSuccess()
                    obtenerCursos() // Recargar la lista
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Error al actualizar curso"
                    _error.value = errorMsg
                    onError(errorMsg)
                }

            _isLoading.value = false
        }
    }

    // Eliminar un curso
    fun eliminarCurso(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.eliminarCurso(id)
                .onSuccess {
                    onSuccess()
                    obtenerCursos() // Recargar la lista
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Error al eliminar curso"
                    _error.value = errorMsg
                    onError(errorMsg)
                }

            _isLoading.value = false
        }
    }
}