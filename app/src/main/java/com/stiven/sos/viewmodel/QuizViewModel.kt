package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.models.*
import com.stiven.sos.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class QuizUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val quizActivo: IniciarQuizResponse? = null,
    val preguntaActual: Int = 0,
    val respuestasUsuario: List<RespuestaUsuario> = emptyList(),
    val tiempoInicioPregunta: Long = 0,
    val resultadoQuiz: FinalizarQuizResponse? = null,
    val revisionQuiz: RevisionQuizResponse? = null,
    val vidas: VidasResponse? = null,
    val temaInfo: TemaInfoResponse? = null,
    val cursosInscritos: List<Curso> = emptyList(),
    val finalizando: Boolean = false
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuizRepository(application)
    private val TAG = "QuizViewModel"

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // ✅ Mutex para prevenir llamadas concurrentes
    private val finalizarMutex = Mutex()
    private val iniciarMutex = Mutex()

    // ============================================
    // CURSOS INSCRITOS
    // ============================================

    fun cargarCursosInscritos() {
        // ✅ Prevenir llamadas duplicadas
        if (_uiState.value.isLoading) {
            Log.w(TAG, "Ya se están cargando los cursos, ignorando llamada duplicada")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.obtenerCursosInscritos().fold(
                onSuccess = { cursos ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cursosInscritos = cursos
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar cursos"
                    )
                }
            )
        }
    }

    // ============================================
    // MARCAR EXPLICACIÓN
    // ============================================

    fun marcarExplicacionVista(temaId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.marcarExplicacionVista(temaId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    // ============================================
    // INICIAR QUIZ
    // ============================================

    fun iniciarQuiz(cursoId: String, temaId: String) {
        viewModelScope.launch {
            // ✅ Usar Mutex para prevenir múltiples inicios simultáneos
            iniciarMutex.withLock {
                // Verificar si ya hay un quiz activo
                if (_uiState.value.quizActivo != null) {
                    Log.w(TAG, "Ya hay un quiz activo, ignorando llamada")
                    return@withLock
                }

                if (_uiState.value.isLoading) {
                    Log.w(TAG, "Ya se está iniciando un quiz, ignorando")
                    return@withLock
                }

                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d(TAG, "Iniciando quiz - Curso: $cursoId, Tema: $temaId")

                repository.iniciarQuiz(cursoId, temaId).fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Quiz iniciado exitosamente: ${response.quizId}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            quizActivo = response,
                            preguntaActual = 0,
                            respuestasUsuario = emptyList(),
                            tiempoInicioPregunta = System.currentTimeMillis(),
                            finalizando = false
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error al iniciar quiz", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Error al iniciar quiz"
                        )
                    }
                )
            }
        }
    }

    // ============================================
    // RESPONDER PREGUNTA
    // ============================================

    fun responderPregunta(preguntaId: String, respuestaSeleccionada: Int) {
        val tiempoActual = System.currentTimeMillis()
        val tiempoUsado = ((tiempoActual - _uiState.value.tiempoInicioPregunta) / 1000).toInt()

        val respuesta = RespuestaUsuario(
            preguntaId = preguntaId,
            respuestaSeleccionada = respuestaSeleccionada,
            tiempoSeg = tiempoUsado
        )

        val nuevasRespuestas = _uiState.value.respuestasUsuario + respuesta

        _uiState.value = _uiState.value.copy(
            respuestasUsuario = nuevasRespuestas,
            preguntaActual = _uiState.value.preguntaActual + 1,
            tiempoInicioPregunta = System.currentTimeMillis()
        )

        Log.d(TAG, "Respuesta registrada - Pregunta: $preguntaId, Opción: $respuestaSeleccionada, Tiempo: ${tiempoUsado}s")
    }

    // ============================================
    // FINALIZAR QUIZ
    // ============================================

    fun finalizarQuiz() {
        viewModelScope.launch {
            // ✅ Usar Mutex para garantizar que solo se ejecute una vez
            finalizarMutex.withLock {
                // Verificar estado
                if (_uiState.value.finalizando) {
                    Log.w(TAG, "Ya se está finalizando el quiz, ignorando llamada duplicada")
                    return@withLock
                }

                val quizId = _uiState.value.quizActivo?.quizId
                if (quizId == null) {
                    Log.e(TAG, "No hay quiz activo para finalizar")
                    return@withLock
                }

                val respuestas = _uiState.value.respuestasUsuario
                if (respuestas.isEmpty()) {
                    Log.e(TAG, "No hay respuestas para enviar")
                    _uiState.value = _uiState.value.copy(
                        error = "No hay respuestas registradas"
                    )
                    return@withLock
                }

                // Marcar como finalizando
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    finalizando = true
                )

                Log.d(TAG, "Finalizando quiz: $quizId con ${respuestas.size} respuestas")

                repository.finalizarQuiz(quizId, respuestas).fold(
                    onSuccess = { resultado ->
                        Log.d(TAG, "✅ Quiz finalizado exitosamente")
                        Log.d(TAG, "Correctas: ${resultado.preguntasCorrectas}, Incorrectas: ${resultado.preguntasIncorrectas}")
                        Log.d(TAG, "XP ganada: ${resultado.experienciaGanada}, Vidas restantes: ${resultado.vidasRestantes}")

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            resultadoQuiz = resultado,
                            finalizando = false
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Error al finalizar quiz", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Error al finalizar quiz",
                            finalizando = false
                        )
                    }
                )
            }
        }
    }

    // ============================================
    // OBTENER REVISIÓN
    // ============================================

    fun obtenerRevision(quizId: String) {
        if (_uiState.value.isLoading) {
            Log.w(TAG, "Ya se está cargando la revisión")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.obtenerRevisionQuiz(quizId).fold(
                onSuccess = { revision ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        revisionQuiz = revision
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    // ============================================
    // OBTENER VIDAS
    // ============================================

    fun obtenerVidas(cursoId: String) {
        viewModelScope.launch {
            repository.obtenerVidas(cursoId).fold(
                onSuccess = { vidas ->
                    _uiState.value = _uiState.value.copy(vidas = vidas)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    // ============================================
    // OBTENER INFO DEL TEMA
    // ============================================

    fun obtenerTemaInfo(cursoId: String, temaId: String) {
        if (_uiState.value.isLoading) {
            Log.w(TAG, "Ya se está cargando la información del tema")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.obtenerTemaInfo(cursoId, temaId).fold(
                onSuccess = { info ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        temaInfo = info
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    // ============================================
    // LIMPIAR ESTADO
    // ============================================

    fun limpiarQuiz() {
        _uiState.value = _uiState.value.copy(
            quizActivo = null,
            preguntaActual = 0,
            respuestasUsuario = emptyList(),
            resultadoQuiz = null,
            revisionQuiz = null,
            error = null,
            finalizando = false
        )
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}