package com.stiven.sos.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.stiven.sos.models.*
import com.stiven.sos.repository.ProgresoCurso
import com.stiven.sos.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.firebase.auth.FirebaseAuth



data class QuizUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val quizActivo: IniciarQuizResponse? = null,
    val preguntaActual: Int = 0,
    val respuestasUsuario: List<RespuestaUsuario> = emptyList(),
    val tiempoInicioPregunta: Long = 0,
    val resultadoQuiz: FinalizarQuizResponse? = null,
    val revisionQuiz: RevisionQuizResponse? = null,
    val retroalimentacion: RetroalimentacionFallosResponse? = null,
    val vidas: VidasResponse? = null,
    val progreso: ProgresoCurso? = null,
    val temaInfo: TemaInfoResponse? = null,
    val cursosInscritos: List<Curso> = emptyList(),
    val finalizando: Boolean = false,
    val sinVidas: Boolean = false,
    val mostrarDialogoSinVidas: Boolean = false
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuizRepository(application)
    private val TAG = "QuizViewModel"
    private val database = FirebaseDatabase.getInstance()
    private val prefs = application.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

    private var vidasListener: ValueEventListener? = null
    private var progresoListener: ValueEventListener? = null

    private var currentCursoId: String? = null
    private var currentTemaId: String? = null

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val finalizarMutex = Mutex()
    private val iniciarMutex = Mutex()

    // Agregar al inicio de QuizViewModel
    init {
        verificarAutenticacion()
    }

    private fun verificarAutenticacion() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, " Usuario no autenticado en Firebase")
            _uiState.value = _uiState.value.copy(
                error = "Debes iniciar sesión para continuar"
            )
        } else {
            Log.d(TAG, "✅ Usuario autenticado: ${currentUser.uid}")
        }
    }


    // ============================================
    // OBSERVADORES EN TIEMPO REAL
    // ============================================

    fun iniciarObservadores(cursoId: String) {
        currentCursoId = cursoId
        val userId = prefs.getString("user_uid", "") ?: return

        Log.d(TAG, "🔍 Iniciando observadores para curso: $cursoId")

        observarVidas(cursoId, userId)
        observarProgreso(cursoId, userId)
    }

    private fun observarVidas(cursoId: String, userId: String) {
        vidasListener = repository.observarVidasTiempoReal(
            cursoId = cursoId,
            onVidasActualizadas = { vidas ->
                _uiState.value = _uiState.value.copy(
                    vidas = vidas,
                    sinVidas = vidas.vidasActuales == 0
                )
                Log.d(TAG, "💚 Vidas actualizadas: ${vidas.vidasActuales}/${vidas.vidasMax}")
            },
            onError = { error ->
                Log.e(TAG, "❌ Error al observar vidas", error)
            }
        )
    }

    private fun observarProgreso(cursoId: String, userId: String) {
        progresoListener = repository.observarProgresoTiempoReal(
            cursoId = cursoId,
            onProgresoActualizado = { progreso ->
                _uiState.value = _uiState.value.copy(progreso = progreso)
                Log.d(TAG, " Progreso actualizado: XP=${progreso.experiencia}, Racha=${progreso.rachaDias}, Vidas=${progreso.vidas}")
            },
            onError = { error ->
                Log.e(TAG, " Error al observar progreso", error)
            }
        )
    }

    fun detenerObservadores() {
        currentCursoId?.let { cursoId ->
            vidasListener?.let {
                repository.detenerObservacion(cursoId, it, "vidas")
            }

            progresoListener?.let {
                repository.detenerObservacion(cursoId, it, "progreso")
            }
        }

        vidasListener = null
        progresoListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerObservadores()
        currentCursoId = null
        currentTemaId = null
    }

    // ============================================
    // CURSOS INSCRITOS
    // ============================================

    fun cargarCursosInscritos() {
        if (_uiState.value.isLoading) {
            Log.w(TAG, "⏳ Ya se están cargando los cursos")
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
                    Log.d(TAG, "✅ ${cursos.size} cursos cargados")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar cursos"
                    )
                    Log.e(TAG, "❌ Error al cargar cursos", e)
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
                    Log.d(TAG, "✅ Explicación marcada como vista")
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                    Log.e(TAG, "❌ Error al marcar explicación", e)
                }
            )
        }
    }

    // ============================================
    // INICIAR QUIZ
    // ============================================

    fun iniciarQuiz(cursoId: String, temaId: String) {
        viewModelScope.launch {
            iniciarMutex.withLock {
                currentCursoId = cursoId
                currentTemaId = temaId

                Log.d(TAG, "🎯 IDs guardados - Curso: $cursoId, Tema: $temaId")

                val vidasActuales = _uiState.value.vidas?.vidasActuales ?: 5
                if (vidasActuales == 0) {
                    _uiState.value = _uiState.value.copy(
                        error = "No tienes vidas disponibles",
                        mostrarDialogoSinVidas = true
                    )
                    Log.w(TAG, "⚠️ Sin vidas disponibles")
                    return@withLock
                }

                if (_uiState.value.quizActivo != null) {
                    Log.w(TAG, "⚠️ Ya hay un quiz activo")
                    return@withLock
                }

                if (_uiState.value.isLoading) {
                    Log.w(TAG, "⚠️ Ya se está iniciando un quiz")
                    return@withLock
                }

                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d(TAG, "🚀 Iniciando quiz - Curso: $cursoId, Tema: $temaId")

                repository.iniciarQuiz(cursoId, temaId).fold(
                    onSuccess = { response ->
                        Log.d(TAG, "✅ Quiz iniciado exitosamente: ${response.quizId}")

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
                        Log.e(TAG, "❌ Error al iniciar quiz", e)
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

        Log.d(TAG, "✍️ Respuesta registrada - Pregunta: $preguntaId, Opción: $respuestaSeleccionada, Tiempo: ${tiempoUsado}s")
    }

    // ============================================
    // FINALIZAR QUIZ
    // ============================================

    fun finalizarQuiz() {
        viewModelScope.launch {
            finalizarMutex.withLock {
                if (_uiState.value.finalizando) {
                    Log.w(TAG, "⚠️ Ya se está finalizando el quiz")
                    return@withLock
                }

                val quizId = _uiState.value.quizActivo?.quizId
                if (quizId == null) {
                    Log.e(TAG, "❌ No hay quiz activo para finalizar")
                    return@withLock
                }

                val respuestas = _uiState.value.respuestasUsuario
                if (respuestas.isEmpty()) {
                    Log.e(TAG, "❌ No hay respuestas para enviar")
                    _uiState.value = _uiState.value.copy(error = "No hay respuestas registradas")
                    return@withLock
                }

                _uiState.value = _uiState.value.copy(isLoading = true, error = null, finalizando = true)

                Log.d(TAG, "🏁 Finalizando quiz: $quizId con ${respuestas.size} respuestas")

                repository.finalizarQuiz(quizId, respuestas).fold(
                    onSuccess = { resultado ->
                        Log.d(TAG, "✅ Quiz finalizado exitosamente")
                        Log.d(TAG, "📊 Correctas: ${resultado.preguntasCorrectas}, Incorrectas: ${resultado.preguntasIncorrectas}")
                        Log.d(TAG, "⭐ XP ganada: ${resultado.experienciaGanada}, Vidas restantes: ${resultado.vidasRestantes}")

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            resultadoQuiz = resultado,
                            finalizando = false
                        )

                        // Si hubo fallos, cargar retroalimentación
                        if (resultado.preguntasIncorrectas > 0) {
                            obtenerRetroalimentacion(quizId)
                        }
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
    // OBTENER RETROALIMENTACIÓN
    // ============================================

    fun obtenerRetroalimentacion(quizId: String) {
        if (_uiState.value.isLoading) {
            Log.w(TAG, "⚠️ Ya se está cargando la retroalimentación")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { retroalimentacion ->
                    Log.d(TAG, "💡 Retroalimentación cargada: ${retroalimentacion.totalFallos} fallos")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        retroalimentacion = retroalimentacion
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "❌ Error al obtener retroalimentación", e)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
            retroalimentacion = null,
            error = null,
            finalizando = false
        )
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun cerrarDialogoSinVidas() {
        _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = false)
    }

    fun mostrarDialogoSinVidas() {
        _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
    }
}