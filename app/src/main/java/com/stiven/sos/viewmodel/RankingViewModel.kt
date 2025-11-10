package com.stiven.sos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Curso
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.models.UsuarioAsignado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RankingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cursosInscritos: List<Curso> = emptyList(),
    val rankingEstudiantes: List<RankingEstudiante> = emptyList()
)

class RankingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val apiService = ApiClient.apiService
    private val auth = FirebaseAuth.getInstance()

    fun obtenerUsuarioActualId(): String? {
        return auth.currentUser?.uid
    }

    fun cargarCursosInscritos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuario no autenticado"
                    )
                    return@launch
                }

                val responseCursos = apiService.obtenerCursos()

                if (responseCursos.isSuccessful && responseCursos.body() != null) {
                    val todosCursos: List<Curso> = responseCursos.body()!!
                    val cursosInscritos = mutableListOf<Curso>()

                    for (curso in todosCursos) {
                        try {
                            if (curso.id != null) {
                                val responseEstudiantes = apiService.obtenerEstudiantesPorCurso(curso.id)
                                if (responseEstudiantes.isSuccessful) {
                                    val estudiantes: List<UsuarioAsignado> = responseEstudiantes.body() ?: emptyList()
                                    // ✅ Usando .uid explícitamente (el campo real)
                                    val estaInscrito = estudiantes.any { estudiante ->
                                        estudiante.uid == userId && estudiante.estado == "activo"
                                    }
                                    if (estaInscrito) {
                                        cursosInscritos.add(curso)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RankingViewModel", "Error checking curso ${curso.id}", e)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cursosInscritos = cursosInscritos
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar cursos: ${responseCursos.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error loading cursos", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun cargarRankingCurso(cursoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d("RankingViewModel", "🔍 Cargando ranking para curso: $cursoId")

                val responseEstudiantes = apiService.obtenerEstudiantesPorCurso(cursoId)

                if (!responseEstudiantes.isSuccessful) {
                    val errorMsg = "Error al obtener estudiantes: ${responseEstudiantes.code()} - ${responseEstudiantes.message()}"
                    Log.e("RankingViewModel", errorMsg)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                    return@launch
                }

                val todosEstudiantes: List<UsuarioAsignado> = responseEstudiantes.body() ?: emptyList()
                Log.d("RankingViewModel", "📚 Total estudiantes recibidos: ${todosEstudiantes.size}")

                val estudiantes = todosEstudiantes.filter { est ->
                    est.estado == "activo"
                }
                Log.d("RankingViewModel", "✅ Estudiantes activos: ${estudiantes.size}")

                if (estudiantes.isEmpty()) {
                    Log.w("RankingViewModel", "⚠️ No hay estudiantes activos en el curso")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        rankingEstudiantes = emptyList()
                    )
                    return@launch
                }

                val ranking = mutableListOf<RankingEstudiante>()

                for (estudiante in estudiantes) {
                    try {
                        // ✅ Usando .uid explícitamente para el endpoint
                        Log.d("RankingViewModel", "🔄 Obteniendo racha de: ${estudiante.nombre} (${estudiante.uid})")
                        val responseRacha = apiService.obtenerRacha(cursoId, estudiante.uid)

                        if (responseRacha.isSuccessful && responseRacha.body() != null) {
                            val racha: Map<String, Any> = responseRacha.body()!!
                            val experiencia = (racha["experiencia"] as? Number)?.toInt() ?: 0
                            val diasConsecutivos = (racha["diasConsecutivos"] as? Number)?.toInt() ?: 0

                            Log.d("RankingViewModel", "   ✅ ${estudiante.nombre}: $experiencia XP, $diasConsecutivos días")

                            ranking.add(
                                RankingEstudiante(
                                    id = estudiante.uid, // ✅ Usar uid para el ID en el ranking
                                    nombre = estudiante.nombre,
                                    experiencia = experiencia,
                                    diasConsecutivos = diasConsecutivos
                                )
                            )
                        } else {
                            Log.w("RankingViewModel", "   ⚠️ No se encontró racha para ${estudiante.nombre}, usando valores por defecto")
                            ranking.add(
                                RankingEstudiante(
                                    id = estudiante.uid,
                                    nombre = estudiante.nombre,
                                    experiencia = 0,
                                    diasConsecutivos = 0
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("RankingViewModel", "❌ Error getting racha for ${estudiante.nombre} (${estudiante.uid})", e)
                        ranking.add(
                            RankingEstudiante(
                                id = estudiante.uid,
                                nombre = estudiante.nombre,
                                experiencia = 0,
                                diasConsecutivos = 0
                            )
                        )
                    }
                }

                val rankingOrdenado: List<RankingEstudiante> = ranking.sortedWith(
                    compareByDescending<RankingEstudiante> { it.experiencia }
                        .thenByDescending { it.diasConsecutivos }
                )

                Log.d("RankingViewModel", "🏆 Ranking final: ${rankingOrdenado.size} estudiantes")
                rankingOrdenado.forEachIndexed { index, est ->
                    Log.d("RankingViewModel", "   ${index + 1}. ${est.nombre}: ${est.experiencia} XP")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    rankingEstudiantes = rankingOrdenado
                )

            } catch (e: Exception) {
                val errorMsg = "Error al cargar ranking: ${e.message}"
                Log.e("RankingViewModel", "❌ $errorMsg", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }
}