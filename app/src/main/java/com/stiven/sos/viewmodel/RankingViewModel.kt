package com.stiven.sos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Curso
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.repository.RankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class TipoRanking {
    EXPERIENCIA,
    RACHA,
    VIDAS
}

data class RankingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cursosInscritos: List<Curso> = emptyList(),
    val rankingEstudiantes: List<RankingEstudiante> = emptyList(),
    val tipoRanking: TipoRanking = TipoRanking.EXPERIENCIA
)

class RankingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val apiService = ApiClient.apiService
    private val rankingRepository = RankingRepository()
    private val auth = FirebaseAuth.getInstance()

    fun obtenerUsuarioActualId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Cargar cursos inscritos (ESTUDIANTES - usando Firebase)
     */
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

                Log.d("RankingViewModel", "Obteniendo cursos para usuario: $userId")
                val responseCursos = apiService.obtenerCursos()

                if (responseCursos.isSuccessful && responseCursos.body() != null) {
                    val todosCursos = responseCursos.body()!!
                    val cursosInscritos = mutableListOf<Curso>()

                    for (curso in todosCursos) {
                        if (curso.id != null) {
                            val tieneProgreso = verificarProgresoEnFirebase(userId, curso.id)
                            if (tieneProgreso) {
                                cursosInscritos.add(curso)
                            }
                        }
                    }

                    Log.d("RankingViewModel", "Cursos inscritos encontrados: ${cursosInscritos.size}")
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

    private suspend fun verificarProgresoEnFirebase(userId: String, cursoId: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            FirebaseDatabase.getInstance()
                .getReference("usuarios/$userId/cursos/$cursoId/progreso")
                .get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.exists())
                }
                .addOnFailureListener {
                    cont.resume(false)
                }
        }
    }

    /**
     * Cargar ranking de un curso específico
     */
    fun cargarRankingCurso(cursoId: String, tipo: TipoRanking = TipoRanking.EXPERIENCIA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                tipoRanking = tipo
            )

            try {
                Log.d("RankingViewModel", "🔍 Cargando ranking: $tipo para curso: $cursoId")

                val result = when (tipo) {
                    TipoRanking.EXPERIENCIA -> rankingRepository.obtenerRankingPorExperiencia(cursoId)
                    TipoRanking.RACHA -> rankingRepository.obtenerRankingPorRacha(cursoId)
                    TipoRanking.VIDAS -> rankingRepository.obtenerRankingPorVidas(cursoId)
                }

                result.fold(
                    onSuccess = { ranking ->
                        Log.d("RankingViewModel", "✅ Ranking cargado: ${ranking.size} estudiantes")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            rankingEstudiantes = ranking,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Log.e("RankingViewModel", "❌ Error: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error al cargar ranking: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("RankingViewModel", "❌ Excepción: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

    /**
     * Cargar ranking general (todos los cursos)
     */
    fun cargarRankingGeneral(tipo: TipoRanking = TipoRanking.EXPERIENCIA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                tipoRanking = tipo
            )

            try {
                Log.d("RankingViewModel", "🌐 Cargando ranking general: $tipo")

                val filtro = when (tipo) {
                    TipoRanking.EXPERIENCIA -> "experiencia"
                    TipoRanking.RACHA -> "racha"
                    TipoRanking.VIDAS -> "vidas"
                }

                val result = rankingRepository.obtenerRankingGeneral(filtro)

                result.fold(
                    onSuccess = { ranking ->
                        Log.d("RankingViewModel", "✅ Ranking general: ${ranking.size} estudiantes")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            rankingEstudiantes = ranking,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Log.e("RankingViewModel", "❌ Error ranking general: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error al cargar ranking general: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("RankingViewModel", "❌ Excepción ranking general: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }
}