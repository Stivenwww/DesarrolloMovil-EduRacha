package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.ProgramacionCurso
import com.stiven.sos.models.RangoTema
import com.stiven.sos.models.UsuarioAsignado
import com.stiven.sos.repository.CursoRepository
import com.stiven.sos.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class CursoUiState(
    val cursos: List<Curso> = emptyList(),
    val solicitudesPorCurso: Map<String, Int> = emptyMap(),
    val estudiantesPorCurso: Map<String, List<UsuarioAsignado>> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingEstudiantes: Boolean = false,
    val error: String? = null,
    val operationSuccess: String? = null
)

class CursoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CursoRepository()
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(CursoUiState())
    val uiState = _uiState.asStateFlow()

    // Flag para evitar múltiples llamadas simultáneas
    private var isLoadingCursos = false
    private var isLoadingSolicitudes = false

    fun obtenerCursos() {
        // Prevenir llamadas duplicadas
        if (isLoadingCursos) {
            Log.w("CursoViewModel", "⚠️ Ya se están cargando cursos, ignorando llamada duplicada")
            return
        }

        viewModelScope.launch {
            isLoadingCursos = true
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.obtenerCursos()
                .onSuccess { listaCursos ->
                    Log.d("CursoViewModel", "✅ Cursos obtenidos: ${listaCursos.size}")
                    _uiState.update { it.copy(isLoading = false, cursos = listaCursos) }
                    cargarSolicitudesPendientes()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error obteniendo cursos", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al cargar"
                        )
                    }
                }
                .also {
                    isLoadingCursos = false
                }
        }
    }

    fun cargarEstudiantesTotales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEstudiantes = true) }

            try {
                val cursosIds = _uiState.value.cursos.mapNotNull { it.id }

                if (cursosIds.isEmpty()) {
                    Log.w("CursoViewModel", "⚠️ No hay cursos para cargar estudiantes")
                    _uiState.update {
                        it.copy(
                            isLoadingEstudiantes = false,
                            estudiantesPorCurso = emptyMap()
                        )
                    }
                    return@launch
                }

                Log.d("CursoViewModel", "🔍 Cargando estudiantes de ${cursosIds.size} cursos")

                val estudiantesPorCurso = mutableMapOf<String, List<UsuarioAsignado>>()

                // Cargar estudiantes de cada curso
                cursosIds.forEach { cursoId ->
                    try {
                        val response = ApiClient.apiService.obtenerEstudiantesPorCurso(cursoId)

                        if (response.isSuccessful) {
                            val estudiantes = response.body() ?: emptyList()
                            estudiantesPorCurso[cursoId] = estudiantes

                            Log.d("CursoViewModel", "✅ Curso $cursoId: ${estudiantes.size} estudiantes")
                        } else {
                            Log.w("CursoViewModel", "⚠️ Error al cargar estudiantes del curso $cursoId: ${response.code()}")
                            estudiantesPorCurso[cursoId] = emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("CursoViewModel", "❌ Error cargando estudiantes del curso $cursoId", e)
                        estudiantesPorCurso[cursoId] = emptyList()
                    }
                }

                // Calcular totales
                val totalEstudiantes = estudiantesPorCurso.values.flatten()
                val estudiantesUnicos = totalEstudiantes.map { it.uid }.toSet()

                Log.d("CursoViewModel", "📊 RESUMEN DE ESTUDIANTES:")
                Log.d("CursoViewModel", "   - Total asignaciones: ${totalEstudiantes.size}")
                Log.d("CursoViewModel", "   - Estudiantes únicos: ${estudiantesUnicos.size}")
                Log.d("CursoViewModel", "   - Cursos con estudiantes: ${estudiantesPorCurso.filter { it.value.isNotEmpty() }.size}")

                _uiState.update {
                    it.copy(
                        estudiantesPorCurso = estudiantesPorCurso,
                        isLoadingEstudiantes = false
                    )
                }

            } catch (e: Exception) {
                Log.e("CursoViewModel", "❌ Error general al cargar estudiantes", e)
                _uiState.update {
                    it.copy(
                        isLoadingEstudiantes = false,
                        estudiantesPorCurso = emptyMap()
                    )
                }
            }
        }
    }

    private fun cargarSolicitudesPendientes() {
        // Prevenir llamadas duplicadas
        if (isLoadingSolicitudes) {
            Log.w("CursoViewModel", "⚠️ Ya se están cargando solicitudes, ignorando")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingSolicitudes = true

                val docenteId = obtenerDocenteId()
                Log.d("CursoViewModel", "🔍 Cargando solicitudes para docente: $docenteId")

                val response = ApiClient.apiService.obtenerSolicitudesDocente(docenteId)

                if (response.isSuccessful) {
                    val solicitudes = response.body() ?: emptyList()
                    Log.d("CursoViewModel", "📨 Total solicitudes recibidas: ${solicitudes.size}")

                    // Filtrar solicitudes pendientes
                    val solicitudesPendientes = solicitudes.filter {
                        it.estado == EstadoSolicitud.PENDIENTE
                    }

                    Log.d("CursoViewModel", "📊 Solicitudes PENDIENTES: ${solicitudesPendientes.size}")

                    val solicitudesPorCurso = solicitudesPendientes
                        .groupBy { it.cursoId }
                        .mapValues { (cursoId, lista) ->
                            val count = lista.size
                            Log.d("CursoViewModel", "   - Curso $cursoId: $count pendientes")
                            count
                        }

                    Log.d("CursoViewModel", "✅ Total cursos con solicitudes: ${solicitudesPorCurso.size}")
                    _uiState.update { it.copy(solicitudesPorCurso = solicitudesPorCurso) }

                } else if (response.code() == 404) {
                    Log.i("CursoViewModel", "ℹ️ No hay solicitudes pendientes (404)")
                    _uiState.update { it.copy(solicitudesPorCurso = emptyMap()) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CursoViewModel", "❌ Error ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("CursoViewModel", "❌ Excepción al cargar solicitudes", e)
            } finally {
                isLoadingSolicitudes = false
            }
        }
    }

    private fun obtenerDocenteId(): String {
        val id = sessionManager.getUserId()
        if (id.isNullOrBlank()) {
            throw IllegalStateException("No hay sesión de docente activa")
        }
        return id
    }

    fun crearCurso(curso: Curso) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.crearCurso(curso)
                .onSuccess { response ->
                    Log.d("CursoViewModel", "✅ Curso creado exitosamente")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso creado exitosamente"
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error creando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al crear"
                        )
                    }
                }
        }
    }

    fun actualizarCurso(curso: Curso) {
        val id = curso.id

        if (id.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "ID del curso no encontrado. No se puede actualizar."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }

            // Validar que el curso tenga las fechas necesarias
            if (curso.fechaInicio == 0L || curso.fechaFin == 0L) {
                Log.w("CursoViewModel", "⚠️ Curso sin fechas, se guardará pero sin distribución temporal")
            }

            // Si el curso tiene programación, validar su consistencia
            curso.programacion?.let { prog ->
                val temasIds = curso.temas?.keys ?: emptySet()
                val temasOrdenados = prog.temasOrdenados.toSet()

                if (temasOrdenados != temasIds) {
                    Log.w("CursoViewModel", "⚠️ La programación no coincide con los temas del curso")
                }

                Log.d("CursoViewModel", "📅 Programación incluida:")
                Log.d("CursoViewModel", "   - Temas ordenados: ${prog.temasOrdenados.size}")
                Log.d("CursoViewModel", "   - Distribución temporal: ${prog.distribucionTemporal.size}")
            }

            repository.actualizarCurso(id, curso)
                .onSuccess {
                    Log.d("CursoViewModel", "✅ Curso '${curso.titulo}' actualizado con éxito")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso '${curso.titulo}' actualizado correctamente"
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error actualizando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al actualizar"
                        )
                    }
                }
        }
    }

    fun generarProgramacionAutomatica(curso: Curso): ProgramacionCurso? {
        val temas = curso.temas ?: return null
        if (temas.isEmpty()) return null

        // Validar que el curso tenga fechas
        if (curso.fechaInicio == 0L || curso.fechaFin == 0L) {
            Log.e("CursoViewModel", "❌ No se puede generar programación sin fechas de inicio/fin")
            return null
        }

        val temasLista = temas.values.toList()
        val cantidadTemas = temasLista.size
        val duracionTotal = curso.fechaFin - curso.fechaInicio
        val diasPorTema = TimeUnit.MILLISECONDS.toDays(duracionTotal).toInt() / cantidadTemas

        if (diasPorTema <= 0) {
            Log.e("CursoViewModel", "❌ Duración inválida para generar programación")
            return null
        }

        val temasOrdenados = mutableListOf<String>()
        val distribucionTemporal = mutableMapOf<String, RangoTema>()

        var fechaActual = curso.fechaInicio

        temasLista.forEachIndexed { index, tema ->
            val temaId = tema.id ?: return@forEachIndexed
            temasOrdenados.add(temaId)

            val fechaFinalTema = if (index == temasLista.size - 1) {
                curso.fechaFin // Último tema llega hasta el final
            } else {
                fechaActual + TimeUnit.DAYS.toMillis(diasPorTema.toLong())
            }

            distribucionTemporal[temaId] = RangoTema(
                temaId = temaId,
                titulo = tema.titulo,
                fechaInicio = fechaActual,
                fechaFin = fechaFinalTema,
                quizzesRequeridos = diasPorTema, // 1 quiz por día
                diasAsignados = diasPorTema
            )

            fechaActual = fechaFinalTema
        }

        Log.d("CursoViewModel", "✅ Programación generada automáticamente:")
        Log.d("CursoViewModel", "   - ${temasOrdenados.size} temas")
        Log.d("CursoViewModel", "   - $diasPorTema días por tema")

        return ProgramacionCurso(
            temasOrdenados = temasOrdenados,
            distribucionTemporal = distribucionTemporal
        )
    }

    fun eliminarCurso(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, operationSuccess = null) }
            repository.eliminarCurso(id)
                .onSuccess {
                    Log.d("CursoViewModel", "✅ Curso eliminado")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = "Curso eliminado exitosamente."
                        )
                    }
                    obtenerCursos()
                }
                .onFailure { exception ->
                    Log.e("CursoViewModel", "❌ Error eliminando curso", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido al eliminar"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(operationSuccess = null) }
    }
}