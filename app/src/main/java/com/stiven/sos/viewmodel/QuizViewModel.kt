package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.models.*
import com.stiven.sos.repository.ProgresoCurso
import com.stiven.sos.repository.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*



data class QuizUiState(
    val isLoading: Boolean = false,
    val quizActivo: IniciarQuizResponse? = null,
    val preguntaActual: Int = 0,
    val respuestas: List<RespuestaUsuario> = emptyList(),
    val resultadoQuiz: FinalizarQuizResponse? = null,
    val retroalimentacion: RetroalimentacionFallosResponse? = null,
    val error: String? = null,
    val vidas: VidasResponse? = null,
    val progreso: ProgresoCurso? = null,
    val sinVidas: Boolean = false,
    val finalizando: Boolean = false,
    val mostrarDialogoSinVidas: Boolean = false,
    val temaAprobado: Boolean = false,
    val todosTemasAprobados: Boolean = false,
    val cursosInscritos: List<Curso> = emptyList(),
    val mostrarMensajeFlotante: Boolean = false,
    val mensajeFlotante: String? = null,
    val tipoMensaje: TipoMensaje = TipoMensaje.INFO,
    val modosDisponibles: ModoQuizDisponibleResponse? = null,
    val modoActual: String = "oficial",
    val puedeHacerPractica: Boolean = false,
    val yaResolviHoy: Boolean = false,
    val mostrarCelebracionRacha: Boolean = false,
    val rachaSubio: Boolean = false,
    val horasParaNuevoQuiz: Int = 0,
    val minutosParaNuevoQuiz: Int = 0,
    val respuestaProcesada: Boolean = false,
    val mostrarConfetti: Boolean = false,
    val porcentajeQuizOficial: Int = 0,
    val mostrarDialogoTemaAprobado: Boolean = false,
    val quizFinalYaCompletado: Boolean = false,
    val mostrarDialogoQuizFinalCompletado: Boolean = false,

    // Indica si el quiz fue interrumpido por perdida de vidas
    // Cuando esto es true, el usuario DEBE ser redirigido inmediatamente
    val quizInterrumpidoPorVidas: Boolean = false,
    val mostrarDialogoPeriodoFinalizado: Boolean = false,
    val mostrarDialogoErrorGeneral: Boolean = false,
    val mensajeErrorDetallado: String = "",
    val tituloError: String = ""

)

enum class TipoMensaje {
    EXITO, ERROR, INFO, ADVERTENCIA
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "QuizViewModel"
    private val repository = QuizRepository(application)

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var listenerVidas: com.google.firebase.database.ValueEventListener? = null
    private var listenerProgreso: com.google.firebase.database.ValueEventListener? = null
    private var timerJob: Job? = null
    private var cursoIdActual: String? = null
    private var temaIdActual: String? = null

    private val finalizarMutex = Mutex()
    private val iniciarMutex = Mutex()

    private val prefs = application.getSharedPreferences("EduRachaUserPrefs", android.content.Context.MODE_PRIVATE)

    init {
        verificarAutenticacion()
    }

    private fun verificarAutenticacion() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado en Firebase")
            mostrarMensaje("Debes iniciar sesion para continuar", TipoMensaje.ERROR)
        } else {
            Log.d(TAG, "Usuario autenticado correctamente: ${currentUser.uid}")
        }
    }
    /**
     * INICIAR OBSERVADORES EN TIEMPO REAL
     */

// ============================================================================
// PASO 1: ACTUALIZAR QuizViewModel - Mejorar detección de vidas
// ============================================================================

// En QuizViewModel.kt, actualizar la función iniciarObservadores:

    fun iniciarObservadores(cursoId: String, temaId: String = "") {
        Log.d(TAG, "======================================")
        Log.d(TAG, "INICIANDO OBSERVADORES EN TIEMPO REAL")
        Log.d(TAG, "Curso ID: $cursoId")
        if (temaId.isNotEmpty()) Log.d(TAG, "Tema ID: $temaId")
        Log.d(TAG, "======================================")

        detenerObservadores()

        cursoIdActual = cursoId
        temaIdActual = temaId

        listenerVidas = repository.observarVidasTiempoReal(
            cursoId = cursoId,
            onVidasActualizadas = { vidas ->
                Log.d(TAG, "----------------------------------------")
                Log.d(TAG, "ACTUALIZACION DE VIDAS DETECTADA")
                Log.d(TAG, "Vidas actuales: ${vidas.vidasActuales}")

                val estadoAnterior = _uiState.value
                val vidasAnteriores = estadoAnterior.vidas?.vidasActuales ?: 5
                val vidasNuevas = vidas.vidasActuales
                val quizActivo = estadoAnterior.quizActivo
                val yaInterrumpido = estadoAnterior.quizInterrumpidoPorVidas

                Log.d(TAG, "Estado del sistema:")
                Log.d(TAG, "  - Vidas anteriores: $vidasAnteriores")
                Log.d(TAG, "  - Vidas nuevas: $vidasNuevas")
                Log.d(TAG, "  - Quiz activo: ${quizActivo != null}")
                Log.d(TAG, "  - Ya interrumpido: $yaInterrumpido")

                //  ACTUALIZAR ESTADO DE VIDAS PRIMERO
                _uiState.value = _uiState.value.copy(
                    vidas = vidas,
                    sinVidas = vidas.vidasActuales <= 0
                )

                // DETECCIÓN INMEDIATA: Vidas llegaron a 0 DURANTE el quiz
                if (vidasNuevas == 0 &&
                    vidasAnteriores > 0 &&
                    quizActivo != null &&
                    !yaInterrumpido) {

                    Log.e(TAG, "========================================")
                    Log.e(TAG, " ALERTA CRÍTICA: VIDAS AGOTADAS")
                    Log.e(TAG, "========================================")
                    Log.e(TAG, "Quiz ID: ${quizActivo.quizId}")
                    Log.e(TAG, "Vidas: $vidasAnteriores → $vidasNuevas")
                    Log.e(TAG, "ACCIÓN: Bloqueando quiz INMEDIATAMENTE")
                    Log.e(TAG, "========================================")

                    // BLOQUEO INMEDIATO
                    _uiState.value = _uiState.value.copy(
                        mostrarDialogoSinVidas = true,
                        quizInterrumpidoPorVidas = true,
                        finalizando = false
                    )
                }

                Log.d(TAG, "Vidas: ${vidas.vidasActuales}/${vidas.vidasMax}")
                Log.d(TAG, "----------------------------------------")
            },
            onError = { error ->
                Log.e(TAG, "Error en observador de vidas: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    sinVidas = true,
                    vidas = VidasResponse(0, 5, 0)
                )
            }
        )


        listenerProgreso = repository.observarProgresoTiempoReal(
            cursoId = cursoId,
            onProgresoActualizado = { progreso ->
                _uiState.value = _uiState.value.copy(progreso = progreso)
                Log.d(TAG, "Progreso actualizado: XP=${progreso.experiencia}, Racha=${progreso.rachaDias}")
            },
            onError = { error ->
                Log.e(TAG, "Error en observador de progreso: ${error.message}")
            }
        )

        iniciarTimerRegeneracion()

        if (temaId.isNotEmpty()) {
            verificarTemaAprobado(cursoId, temaId)
            verificarSiResolviHoy(cursoId, temaId)
            cargarPorcentajeQuizOficial(cursoId, temaId)
            iniciarTimerCooldown(cursoId, temaId)
        }

        if (temaId == "quiz_final") {
            verificarQuizFinalCompletado(cursoId)
        }

        Log.d(TAG, "======================================")
        Log.d(TAG, "OBSERVADORES INICIADOS CORRECTAMENTE")
        Log.d(TAG, "======================================")
    }
    // Agregar esta función en QuizViewModel
    fun cargarEstadoTema(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                    val porcentaje = snapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0
                    val timestampUltimoQuiz = snapshot.child("timestampUltimoQuiz").getValue(Long::class.java) ?: 0L

                    var horasRestantes = 0
                    var minutosRestantes = 0
                    var debeEsperar24h = false

                    // Cooldown solo si aprobó con 80% o más
                    if (porcentaje >= 80 && timestampUltimoQuiz > 0) {
                        val ahora = System.currentTimeMillis()
                        val tiempoTranscurrido = ahora - timestampUltimoQuiz
                        val cooldown24h = 24 * 60 * 60 * 1000L

                        if (tiempoTranscurrido < cooldown24h) {
                            debeEsperar24h = true
                            val tiempoRestante = cooldown24h - tiempoTranscurrido
                            horasRestantes = (tiempoRestante / (1000 * 60 * 60)).toInt()
                            minutosRestantes = ((tiempoRestante % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        temaAprobado = aprobado,
                        porcentajeQuizOficial = porcentaje,
                        puedeHacerPractica = porcentaje >= 80,
                        yaResolviHoy = debeEsperar24h,
                        horasParaNuevoQuiz = horasRestantes,
                        minutosParaNuevoQuiz = minutosRestantes
                    )

                    Log.d(TAG, "Estado tema $temaId: aprobado=$aprobado, porcentaje=$porcentaje%, cooldown=$debeEsperar24h")
                } else {
                    _uiState.value = _uiState.value.copy(
                        temaAprobado = false,
                        porcentajeQuizOficial = 0,
                        puedeHacerPractica = false,
                        yaResolviHoy = false,
                        horasParaNuevoQuiz = 0,
                        minutosParaNuevoQuiz = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando estado del tema: ${e.message}")
            }
        }
    }
    /**
     * VERIFICAR SI EL QUIZ FINAL YA FUE COMPLETADO
     *
     * REGLA CRITICA:
     * El Quiz Final solo puede realizarse UNA VEZ por curso
     * Se considera completado si: aprobado == true Y porcentajeObtenido >= 80
     *
     * Si ya se completo con 80% o mas, se activa el flag quizFinalYaCompletado
     * que bloquea cualquier intento de volver a hacerlo
     */
    private fun verificarQuizFinalCompletado(cursoId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/quiz_final")

                Log.d(TAG, "========================================")
                Log.d(TAG, "VERIFICANDO ESTADO DEL QUIZ FINAL")
                Log.d(TAG, "========================================")

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                    val porcentaje = snapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                    Log.d(TAG, "Quiz Final encontrado en Firebase:")
                    Log.d(TAG, "  - Estado aprobado: $aprobado")
                    Log.d(TAG, "  - Porcentaje obtenido: $porcentaje%")

                    /**
                     * VALIDACION CRITICA:
                     * Si aprobo con 80% o mas, NO puede volver a hacerlo
                     * Esto garantiza que cada usuario solo haga el Quiz Final UNA VEZ
                     */
                    if (aprobado && porcentaje >= 80) {
                        _uiState.value = _uiState.value.copy(
                            quizFinalYaCompletado = true
                        )
                        Log.w(TAG, "========================================")
                        Log.w(TAG, "QUIZ FINAL YA COMPLETADO")
                        Log.w(TAG, "Porcentaje: $porcentaje%")
                        Log.w(TAG, "ACCESO BLOQUEADO")
                        Log.w(TAG, "========================================")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            quizFinalYaCompletado = false
                        )
                        if (porcentaje > 0 && porcentaje < 80) {
                            Log.d(TAG, "Quiz Final puede reintentarse (obtuvo $porcentaje%)")
                        } else {
                            Log.d(TAG, "Quiz Final disponible para realizar")
                        }
                    }
                } else {
                    // Nunca se ha intentado
                    _uiState.value = _uiState.value.copy(
                        quizFinalYaCompletado = false
                    )
                    Log.d(TAG, "Quiz Final nunca intentado - Disponible")
                }

                Log.d(TAG, "========================================")
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando Quiz Final: ${e.message}")
            }
        }
    }

    private fun iniciarTimerCooldown(cursoId: String, temaId: String) {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000) // 1 minuto
                if (_uiState.value.yaResolviHoy) {
                    verificarSiResolviHoy(cursoId, temaId)
                }
            }
        }
    }

    private fun iniciarTimerRegeneracion() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // 1 minuto
                cursoIdActual?.let { cursoId ->
                    Log.d(TAG, "Timer: verificando regeneracion de vidas")
                }
            }
        }
    }

    fun detenerObservadores() {
        Log.d(TAG, "Deteniendo observadores")

        timerJob?.cancel()
        timerJob = null

        listenerVidas?.let {
            cursoIdActual?.let { curso ->
                repository.detenerObservacion(curso, it)
            }
            listenerVidas = null
        }

        listenerProgreso?.let {
            cursoIdActual?.let { curso ->
                repository.detenerObservacion(curso, it)
            }
            listenerProgreso = null
        }

        Log.d(TAG, "Observadores detenidos correctamente")
    }

    private fun cargarPorcentajeQuizOficial(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val porcentaje = snapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0
                    val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false

                    _uiState.value = _uiState.value.copy(
                        porcentajeQuizOficial = porcentaje,
                        temaAprobado = aprobado
                    )

                    Log.d(TAG, "Tema cargado: Porcentaje=$porcentaje%, Aprobado=$aprobado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando porcentaje: ${e.message}")
            }
        }
    }

    private fun verificarSiResolviHoy(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val timestampUltimoQuiz = snapshot.child("timestampUltimoQuiz").getValue(Long::class.java) ?: 0L
                    val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                    val porcentajeObtenido = snapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                    var horasRestantes = 0
                    var minutosRestantes = 0
                    var debeEsperar24h = false

                    // Cooldown solo si aprobo con 80% o mas
                    if (porcentajeObtenido >= 80 && timestampUltimoQuiz > 0) {
                        val ahora = System.currentTimeMillis()
                        val tiempoTranscurrido = ahora - timestampUltimoQuiz
                        val cooldown24h = 24 * 60 * 60 * 1000L

                        if (tiempoTranscurrido < cooldown24h) {
                            debeEsperar24h = true
                            val tiempoRestante = cooldown24h - tiempoTranscurrido
                            horasRestantes = (tiempoRestante / (1000 * 60 * 60)).toInt()
                            minutosRestantes = ((tiempoRestante % (1000 * 60 * 60)) / (1000 * 60)).toInt()

                            Log.d(TAG, "Cooldown activo. Restante: ${horasRestantes}h ${minutosRestantes}m")
                        } else {
                            Log.d(TAG, "Cooldown completado")
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        temaAprobado = aprobado,
                        puedeHacerPractica = porcentajeObtenido >= 80,
                        yaResolviHoy = debeEsperar24h,
                        horasParaNuevoQuiz = horasRestantes,
                        minutosParaNuevoQuiz = minutosRestantes
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando cooldown: ${e.message}")
            }
        }
    }

    fun mostrarDialogoTemaAprobado() {
        _uiState.value = _uiState.value.copy(mostrarDialogoTemaAprobado = true)
    }

    fun cerrarDialogoTemaAprobado() {
        _uiState.value = _uiState.value.copy(mostrarDialogoTemaAprobado = false)
    }

    fun mostrarDialogoQuizFinalCompletado() {
        _uiState.value = _uiState.value.copy(mostrarDialogoQuizFinalCompletado = true)
    }

    fun cerrarDialogoQuizFinalCompletado() {
        _uiState.value = _uiState.value.copy(mostrarDialogoQuizFinalCompletado = false)
    }

    fun iniciarQuiz(cursoId: String, temaId: String, modo: String = "oficial") {
        viewModelScope.launch {
            iniciarMutex.withLock {
                if (_uiState.value.isLoading) {
                    Log.w(TAG, "Inicio ya en progreso, ignorando solicitud")
                    return@withLock
                }

                cursoIdActual = cursoId
                temaIdActual = temaId

                Log.d(TAG, "========================================")
                Log.d(TAG, "INICIANDO QUIZ")
                Log.d(TAG, "Curso: $cursoId")
                Log.d(TAG, "Tema: $temaId")
                Log.d(TAG, "Modo: $modo")
                Log.d(TAG, "========================================")

                if (temaId == "quiz_final" && _uiState.value.quizFinalYaCompletado) {
                    Log.w(TAG, "========================================")
                    Log.w(TAG, "INICIO BLOQUEADO")
                    Log.w(TAG, "Razon: Quiz Final ya completado")
                    Log.w(TAG, "========================================")
                    mostrarDialogoQuizFinalCompletado()
                    return@withLock
                }

                if (modo == "oficial" &&
                    temaId != "quiz_final" &&
                    _uiState.value.temaAprobado &&
                    !_uiState.value.yaResolviHoy) {
                    Log.w(TAG, "Tema ya aprobado, mostrando dialogo de confirmacion")
                    mostrarDialogoTemaAprobado()
                    return@withLock
                }

                // VALIDACION: VIDAS DISPONIBLES (aplica a TODOS los modos)
                val vidasActuales = _uiState.value.vidas?.vidasActuales ?: 5
                if (vidasActuales == 0) {
                    Log.w(TAG, "========================================")
                    Log.w(TAG, "INICIO BLOQUEADO")
                    Log.w(TAG, "Razon: Sin vidas disponibles")
                    Log.w(TAG, "Modo: $modo")
                    Log.w(TAG, "========================================")
                    mostrarMensaje("No tienes vidas disponibles", TipoMensaje.ADVERTENCIA)
                    _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
                    return@withLock
                }

                if (_uiState.value.quizActivo != null) {
                    Log.w(TAG, "INICIO BLOQUEADO: Ya hay un quiz activo")
                    return@withLock
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    modoActual = modo,
                    quizInterrumpidoPorVidas = false,
                    // Limpiar estados de error anteriores
                    mostrarDialogoPeriodoFinalizado = false,
                    mostrarDialogoErrorGeneral = false,
                    mensajeErrorDetallado = "",
                    tituloError = ""
                )

                val result = if (temaId == "quiz_final") {
                    Log.d(TAG, "Llamando a iniciarQuizFinal()")
                    repository.iniciarQuizFinal(cursoId)
                } else {
                    Log.d(TAG, "Llamando a iniciarQuiz()")
                    repository.iniciarQuiz(cursoId, temaId, modo)
                }

                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            quizActivo = response,
                            preguntaActual = 0,
                            respuestas = emptyList(),
                            isLoading = false,
                            finalizando = false
                        )

                        val modoTexto = when {
                            temaId == "quiz_final" -> "Quiz Final"
                            modo == "practica" -> "Modo Practica"
                            else -> "Quiz Oficial"
                        }

                        mostrarMensaje("$modoTexto iniciado correctamente", TipoMensaje.EXITO)

                        Log.d(TAG, "========================================")
                        Log.d(TAG, "QUIZ INICIADO EXITOSAMENTE")
                        Log.d(TAG, "Quiz ID: ${response.quizId}")
                        Log.d(TAG, "Preguntas: ${response.preguntas.size}")
                        Log.d(TAG, "Modo: $modo")
                        Log.d(TAG, "========================================")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "========================================")
                        Log.e(TAG, "ERROR AL INICIAR QUIZ")
                        Log.e(TAG, "Mensaje: ${error.message}")
                        Log.e(TAG, "========================================")

                        // NUEVO: Analizar tipo de error y mostrar diálogo apropiado
                        val mensajeError = error.message ?: "Error desconocido al iniciar el quiz"

                        when {
                            // Error: Período finalizado
                            mensajeError.contains("período", ignoreCase = true) &&
                                    mensajeError.contains("finalizó", ignoreCase = true) -> {
                                Log.w(TAG, "Tipo de error detectado: PERIODO_FINALIZADO")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoPeriodoFinalizado = true,
                                    mensajeErrorDetallado = mensajeError,
                                    tituloError = "Período Finalizado"
                                )
                            }

                            // Error: Sin vidas
                            mensajeError.contains("sin vidas", ignoreCase = true) ||
                                    mensajeError.contains("vidas insuficientes", ignoreCase = true) -> {
                                Log.w(TAG, "Tipo de error detectado: SIN_VIDAS")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    sinVidas = true,
                                    mostrarDialogoSinVidas = true
                                )
                            }

                            // Error: Tema no disponible
                            mensajeError.contains("no disponible", ignoreCase = true) ||
                                    mensajeError.contains("no existe", ignoreCase = true) -> {
                                Log.w(TAG, "Tipo de error detectado: TEMA_NO_DISPONIBLE")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoErrorGeneral = true,
                                    mensajeErrorDetallado = mensajeError,
                                    tituloError = "Tema No Disponible"
                                )
                            }

                            // Error: Conexión
                            mensajeError.contains("conexión", ignoreCase = true) ||
                                    mensajeError.contains("red", ignoreCase = true) ||
                                    mensajeError.contains("timeout", ignoreCase = true) -> {
                                Log.w(TAG, "Tipo de error detectado: ERROR_CONEXION")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoErrorGeneral = true,
                                    mensajeErrorDetallado = "No se pudo conectar con el servidor. Verifica tu conexión a internet.",
                                    tituloError = "Error de Conexión"
                                )
                            }

                            // Error genérico
                            else -> {
                                Log.w(TAG, "Tipo de error detectado: ERROR_GENERAL")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoErrorGeneral = true,
                                    mensajeErrorDetallado = mensajeError,
                                    tituloError = "Error al Iniciar Quiz"
                                )
                            }
                        }

                        // También mostrar mensaje flotante para feedback inmediato
                        mostrarMensaje(
                            parsearMensajeErrorCorto(mensajeError),
                            TipoMensaje.ERROR
                        )
                    }
                )
            }
        }
    }

    // Función para parsear mensaje de error corto (para el toast/mensaje flotante)
    private fun parsearMensajeErrorCorto(mensaje: String): String {
        return when {
            mensaje.contains("período", ignoreCase = true) &&
                    mensaje.contains("finalizó", ignoreCase = true) -> "El período de este tema ya finalizó"

            mensaje.contains("sin vidas", ignoreCase = true) -> "No tienes vidas disponibles"

            mensaje.contains("no disponible", ignoreCase = true) -> "Tema no disponible"

            mensaje.contains("conexión", ignoreCase = true) ||
                    mensaje.contains("red", ignoreCase = true) -> "Error de conexión"

            else -> "Error al iniciar quiz"
        }
    }

    //Función para cerrar el diálogo de período finalizado
    fun cerrarDialogoPeriodoFinalizado() {
        _uiState.value = _uiState.value.copy(
            mostrarDialogoPeriodoFinalizado = false,
            mensajeErrorDetallado = "",
            tituloError = ""
        )
    }

    // Función para cerrar el diálogo de error general
    fun cerrarDialogoErrorGeneral() {
        _uiState.value = _uiState.value.copy(
            mostrarDialogoErrorGeneral = false,
            mensajeErrorDetallado = "",
            tituloError = ""
        )
    }
    /**
     * FORZAR INICIO DE QUIZ
     * Se usa cuando el usuario confirma que quiere seguir practicando
     * a pesar de haber aprobado el tema
     */
    fun forzarInicioQuiz(cursoId: String, temaId: String, modo: String = "oficial") {
        viewModelScope.launch {
            iniciarMutex.withLock {
                Log.d(TAG, "FORZANDO INICIO DE QUIZ (tema aprobado confirmado)")
                cerrarDialogoTemaAprobado()

                cursoIdActual = cursoId
                temaIdActual = temaId

                val vidasActuales = _uiState.value.vidas?.vidasActuales ?: 5
                if (vidasActuales == 0 && modo == "oficial") {
                    mostrarMensaje("No tienes vidas disponibles", TipoMensaje.ADVERTENCIA)
                    _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
                    return@withLock
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    modoActual = modo,
                    // Limpiar estados de error
                    mostrarDialogoPeriodoFinalizado = false,
                    mostrarDialogoErrorGeneral = false,
                    mensajeErrorDetallado = "",
                    tituloError = ""
                )

                val result = repository.iniciarQuiz(cursoId, temaId, modo)

                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            quizActivo = response,
                            preguntaActual = 0,
                            respuestas = emptyList(),
                            isLoading = false,
                            finalizando = false
                        )
                        mostrarMensaje("Modo Practica iniciado", TipoMensaje.EXITO)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error forzando inicio: ${error.message}")

                        val mensajeError = error.message ?: "Error desconocido"

                        // Detectar tipo de error
                        when {
                            mensajeError.contains("período", ignoreCase = true) &&
                                    mensajeError.contains("finalizó", ignoreCase = true) -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoPeriodoFinalizado = true,
                                    mensajeErrorDetallado = mensajeError,
                                    tituloError = "Período Finalizado"
                                )
                            }
                            else -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    mostrarDialogoErrorGeneral = true,
                                    mensajeErrorDetallado = mensajeError,
                                    tituloError = "Error"
                                )
                            }
                        }

                        mostrarMensaje(parsearMensajeErrorCorto(mensajeError), TipoMensaje.ERROR)
                    }
                )
            }
        }
    }

    fun responderPregunta(preguntaId: String, respuestaSeleccionada: Int) {
        viewModelScope.launch {
            val vidasActuales = _uiState.value.vidas?.vidasActuales ?: 0

            if (vidasActuales == 0) {
                Log.e(TAG, "========================================")
                Log.e(TAG, "INTENTO DE RESPONDER SIN VIDAS - BLOQUEADO LOCALMENTE")
                Log.e(TAG, "========================================")

                _uiState.value = _uiState.value.copy(
                    mostrarDialogoSinVidas = true,
                    quizInterrumpidoPorVidas = true,
                    finalizando = false
                )
                return@launch
            }

            val quizId = _uiState.value.quizActivo?.quizId ?: return@launch

            Log.d(TAG, "========================================")
            Log.d(TAG, "PROCESANDO RESPUESTA EN TIEMPO REAL")
            Log.d(TAG, "Quiz: $quizId")
            Log.d(TAG, "Pregunta: $preguntaId")
            Log.d(TAG, "Opción: $respuestaSeleccionada")
            Log.d(TAG, "Vidas actuales (local): $vidasActuales")
            Log.d(TAG, "========================================")

            // Mostrar loading mientras procesa
            _uiState.value = _uiState.value.copy(isLoading = true)

            // ENVIAR AL BACKEND INMEDIATAMENTE
            val result = repository.procesarRespuestaIndividual(
                quizId = quizId,
                preguntaId = preguntaId,
                respuestaSeleccionada = respuestaSeleccionada,
                tiempoSeg = 0
            )

            result.fold(
                onSuccess = { respuesta ->
                    Log.d(TAG, " Respuesta procesada por backend")
                    Log.d(TAG, "   Es correcta: ${respuesta.esCorrecta}")
                    Log.d(TAG, "   Vidas restantes: ${respuesta.vidasRestantes}")
                    Log.d(TAG, "   Quiz activo: ${respuesta.quizActivo}")

                    // CTUALIZAR VIDAS LOCALMENTE CON VALOR DEL BACKEND
                    _uiState.value = _uiState.value.copy(
                        vidas = VidasResponse(
                            vidasActuales = respuesta.vidasRestantes,
                            vidasMax = 5,
                            minutosParaProximaVida = 0
                        ),
                        isLoading = false
                    )

                    //  SI BACKEND DICE VIDAS = 0 → INTERRUMPIR
                    if (respuesta.vidasRestantes == 0 || !respuesta.quizActivo) {
                        Log.e(TAG, "========================================")
                        Log.e(TAG, "BACKEND REPORTÓ: VIDAS AGOTADAS")
                        Log.e(TAG, "Activando interrupción inmediata")
                        Log.e(TAG, "========================================")

                        _uiState.value = _uiState.value.copy(
                            mostrarDialogoSinVidas = true,
                            quizInterrumpidoPorVidas = true,
                            sinVidas = true
                        )
                        return@fold
                    }

                    val respuestaLocal = RespuestaUsuario(
                        preguntaId = preguntaId,
                        respuestaSeleccionada = respuestaSeleccionada,
                        tiempoSeg = 0
                    )

                    // Avanzar a siguiente pregunta
                    _uiState.value = _uiState.value.copy(
                        respuestas = _uiState.value.respuestas + respuestaLocal,
                        preguntaActual = _uiState.value.preguntaActual + 1,
                        respuestaProcesada = true
                    )

                    Log.d(TAG, " Pregunta ${_uiState.value.preguntaActual} completada")
                    Log.d(TAG, "   Total respondidas: ${respuesta.preguntasRespondidas}")
                    Log.d(TAG, "   Correctas: ${respuesta.preguntasCorrectas}")
                },
                onFailure = { error ->
                    Log.e(TAG, "========================================")
                    Log.e(TAG, " ERROR AL PROCESAR RESPUESTA")
                    Log.e(TAG, "Mensaje: ${error.message}")
                    Log.e(TAG, "========================================")

                    //  DETECTAR SI ES ERROR DE VIDAS AGOTADAS
                    if (error is QuizAbandonadoPorVidasException) {
                        Log.e(TAG, " TIPO: Quiz abandonado por vidas")

                        _uiState.value = _uiState.value.copy(
                            mostrarDialogoSinVidas = true,
                            quizInterrumpidoPorVidas = true,
                            sinVidas = true,
                            isLoading = false
                        )
                    } else {
                        // Error genérico
                        _uiState.value = _uiState.value.copy(
                            isLoading = false
                        )
                        mostrarMensaje(
                            error.message ?: "Error al procesar respuesta",
                            TipoMensaje.ERROR
                        )
                    }
                }
            )
        }
    }
    fun finalizarQuiz() {
        viewModelScope.launch {
            finalizarMutex.withLock {
                if (_uiState.value.finalizando) {
                    Log.w(TAG, "Quiz ya esta finalizando, ignorando solicitud")
                    return@withLock
                }

                val quizId = _uiState.value.quizActivo?.quizId
                val respuestas = _uiState.value.respuestas

                if (quizId == null || respuestas.isEmpty()) {
                    Log.e(TAG, "No se puede finalizar: datos incompletos")
                    return@withLock
                }

                Log.d(TAG, "========================================")
                Log.d(TAG, "FINALIZANDO QUIZ")
                Log.d(TAG, "Quiz ID: $quizId")
                Log.d(TAG, "Respuestas enviadas: ${respuestas.size}")
                Log.d(TAG, "Modo: ${_uiState.value.modoActual}")
                Log.d(TAG, "========================================")

                _uiState.value = _uiState.value.copy(
                    finalizando = true,
                    isLoading = true
                )

                repository.finalizarQuiz(quizId, respuestas).fold(
                    onSuccess = { resultado ->
                        val porcentaje = if (respuestas.isNotEmpty()) {
                            (resultado.preguntasCorrectas * 100) / respuestas.size
                        } else 0

                        Log.d(TAG, "========================================")
                        Log.d(TAG, "QUIZ FINALIZADO EXITOSAMENTE")
                        Log.d(TAG, "Correctas: ${resultado.preguntasCorrectas}/${respuestas.size}")
                        Log.d(TAG, "Porcentaje: $porcentaje%")
                        Log.d(TAG, "========================================")

                        _uiState.value = _uiState.value.copy(
                            resultadoQuiz = resultado,
                            finalizando = false,
                            isLoading = false
                        )

                        if (_uiState.value.modoActual == "oficial") {
                            guardarPorcentajeQuizOficial(porcentaje)
                        }

                        if (porcentaje >= 80 && _uiState.value.modoActual == "oficial") {
                            _uiState.value = _uiState.value.copy(mostrarConfetti = true)
                            verificarYActualizarRacha()
                        }

                        if (resultado.preguntasIncorrectas > 0) {
                            obtenerRetroalimentacion(quizId)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "========================================")
                        Log.e(TAG, "ERROR FINALIZANDO QUIZ")
                        Log.e(TAG, "Mensaje: ${error.message}")
                        Log.e(TAG, "Modo: ${_uiState.value.modoActual}")
                        Log.e(TAG, "========================================")

                        val mensajeError = error.message ?: ""

                        // VALIDACION CRITICA: Detectar error de vidas agotadas
                        if (mensajeError.contains("sin vidas", ignoreCase = true) ||
                            mensajeError.contains("quedado sin vidas", ignoreCase = true)) {

                            Log.e(TAG, "========================================")
                            Log.e(TAG, "BACKEND DETECTO: VIDAS AGOTADAS")
                            Log.e(TAG, "Activando mecanismo de interrupcion")
                            Log.e(TAG, "========================================")

                            // Activar interrupción inmediata
                            _uiState.value = _uiState.value.copy(
                                mostrarDialogoSinVidas = true,
                                quizInterrumpidoPorVidas = true,
                                finalizando = false,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                finalizando = false,
                                isLoading = false
                            )
                            mostrarMensaje(parsearMensajeError(error.message), TipoMensaje.ERROR)
                        }
                    }
                )
            }
        }
    }

    private fun guardarPorcentajeQuizOficial(porcentaje: Int) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val cursoId = cursoIdActual ?: return@launch
                val temaId = temaIdActual ?: return@launch

                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

                val snapshotActual = ref.child("porcentajeObtenido").get().await()
                val porcentajeActual = snapshotActual.getValue(Int::class.java) ?: 0

                if (porcentaje > porcentajeActual) {
                    ref.child("porcentajeObtenido").setValue(porcentaje).await()
                    _uiState.value = _uiState.value.copy(porcentajeQuizOficial = porcentaje)
                    Log.d(TAG, "Porcentaje guardado: $porcentaje%")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando porcentaje: ${e.message}")
            }
        }
    }

    fun actualizarRacha(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId")

                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val timestampActual = System.currentTimeMillis()

                Log.d(TAG, "========================================")
                Log.d(TAG, "ACTUALIZANDO RACHA")
                Log.d(TAG, "Fecha hoy: $fechaHoy")
                Log.d(TAG, "========================================")

                val progresoSnapshot = ref.child("progreso").get().await()
                val ultimaRachaFecha = progresoSnapshot.child("ultimaRachaFecha").getValue(String::class.java)
                val rachaActual = progresoSnapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0

                Log.d(TAG, "Ultima fecha racha: $ultimaRachaFecha")
                Log.d(TAG, "Racha actual: $rachaActual dias")

                val ayer = Calendar.getInstance()
                ayer.add(Calendar.DAY_OF_YEAR, -1)
                val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ayer.time)

                var nuevaRacha = rachaActual
                var rachaSubio = false

                when {
                    ultimaRachaFecha == null || ultimaRachaFecha.isEmpty() -> {
                        nuevaRacha = 1
                        rachaSubio = true
                        Log.d(TAG, "Primera racha del usuario")
                    }
                    ultimaRachaFecha == fechaAyer -> {
                        nuevaRacha = rachaActual + 1
                        rachaSubio = true
                        Log.d(TAG, "Racha consecutiva: $rachaActual -> $nuevaRacha")
                    }
                    ultimaRachaFecha == fechaHoy -> {
                        rachaSubio = false
                        Log.d(TAG, "Ya completo el quiz hoy")
                    }
                    else -> {
                        nuevaRacha = 1
                        rachaSubio = true
                        Log.d(TAG, "Racha rota, reiniciando")
                    }
                }

                if (rachaSubio) {
                    Log.d(TAG, "Guardando nueva racha: $nuevaRacha dias")

                    ref.child("progreso/diasConsecutivos").setValue(nuevaRacha).await()
                    ref.child("progreso/rachaDias").setValue(nuevaRacha).await()
                    ref.child("progreso/ultimaRachaFecha").setValue(fechaHoy).await()

                    _uiState.value = _uiState.value.copy(
                        mostrarCelebracionRacha = true,
                        rachaSubio = true
                    )

                    Log.d(TAG, "Racha guardada exitosamente")
                }

                ref.child("temasCompletados/$temaId/ultimaFechaQuiz").setValue(fechaHoy).await()
                ref.child("temasCompletados/$temaId/timestampUltimoQuiz").setValue(timestampActual).await()
                ref.child("temasCompletados/$temaId/aprobado").setValue(true).await()

                _uiState.value = _uiState.value.copy(yaResolviHoy = true)

                delay(500)
                verificarSiResolviHoy(cursoId, temaId)

                Log.d(TAG, "========================================")
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando racha: ${e.message}")
            }
        }
    }

    private fun verificarYActualizarRacha() {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val cursoId = cursoIdActual ?: return@launch
                val temaId = temaIdActual ?: return@launch

                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId")

                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val timestampActual = System.currentTimeMillis()

                val progresoSnapshot = ref.child("progreso").get().await()
                val ultimaRachaFecha = progresoSnapshot.child("ultimaRachaFecha").getValue(String::class.java)
                val rachaActual = progresoSnapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0

                val ayer = Calendar.getInstance()
                ayer.add(Calendar.DAY_OF_YEAR, -1)
                val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ayer.time)

                var nuevaRacha = rachaActual
                var rachaSubio = false

                when {
                    ultimaRachaFecha == null || ultimaRachaFecha.isEmpty() -> {
                        nuevaRacha = 1
                        rachaSubio = true
                    }
                    ultimaRachaFecha == fechaAyer -> {
                        nuevaRacha = rachaActual + 1
                        rachaSubio = true
                    }
                    ultimaRachaFecha == fechaHoy -> {
                        rachaSubio = false
                    }
                    else -> {
                        nuevaRacha = 1
                        rachaSubio = true
                    }
                }

                if (rachaSubio) {
                    ref.child("progreso/diasConsecutivos").setValue(nuevaRacha).await()
                    ref.child("progreso/rachaDias").setValue(nuevaRacha).await()
                    ref.child("progreso/ultimaRachaFecha").setValue(fechaHoy).await()

                    _uiState.value = _uiState.value.copy(
                        mostrarCelebracionRacha = true,
                        rachaSubio = true
                    )
                }

                ref.child("temasCompletados/$temaId/ultimaFechaQuiz").setValue(fechaHoy).await()
                ref.child("temasCompletados/$temaId/timestampUltimoQuiz").setValue(timestampActual).await()
                ref.child("temasCompletados/$temaId/aprobado").setValue(true).await()

                _uiState.value = _uiState.value.copy(yaResolviHoy = true)

                delay(500)
                verificarSiResolviHoy(cursoId, temaId)

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
            }
        }
    }

    fun cerrarCelebracionRacha() {
        _uiState.value = _uiState.value.copy(mostrarCelebracionRacha = false)
    }

    fun cerrarConfetti() {
        _uiState.value = _uiState.value.copy(mostrarConfetti = false)
    }

    fun obtenerRetroalimentacion(quizId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { retro ->
                    _uiState.value = _uiState.value.copy(retroalimentacion = retro)
                    Log.d(TAG, "Retroalimentacion obtenida: ${retro.totalFallos} fallos")
                },
                onFailure = {
                    Log.e(TAG, "Error obteniendo retroalimentacion")
                }
            )
        }
    }

    fun marcarExplicacionVista(temaId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.marcarExplicacionVista(temaId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Log.d(TAG, "Explicacion marcada como vista")
                    onComplete()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    mostrarMensaje(parsearMensajeError(error.message), TipoMensaje.ERROR)
                }
            )
        }
    }

    fun cargarCursosInscritos() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.obtenerCursosInscritos().fold(
                onSuccess = { cursos ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cursosInscritos = cursos
                    )
                    Log.d(TAG, "Cursos inscritos cargados: ${cursos.size}")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    mostrarMensaje("Error al cargar cursos", TipoMensaje.ERROR)
                }
            )
        }
    }

    fun verificarModosDisponibles(cursoId: String, temaId: String) {
        viewModelScope.launch {
            repository.verificarModosDisponibles(cursoId, temaId).fold(
                onSuccess = { modos ->
                    _uiState.value = _uiState.value.copy(
                        modosDisponibles = modos,
                        temaAprobado = modos.temaAprobado,
                        puedeHacerPractica = modos.temaAprobado
                    )
                },
                onFailure = { }
            )
        }
    }

    private fun verificarTemaAprobado(cursoId: String, temaId: String) {
        viewModelScope.launch {
            repository.verificarTemaAprobado(cursoId, temaId).fold(
                onSuccess = { aprobado ->
                    _uiState.value = _uiState.value.copy(
                        temaAprobado = aprobado,
                        puedeHacerPractica = aprobado
                    )
                },
                onFailure = { }
            )
        }
    }

    fun verificarTodosTemasAprobados(cursoId: String, totalTemas: Int) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados")

                val snapshot = ref.get().await()

                if (!snapshot.exists()) {
                    _uiState.value = _uiState.value.copy(todosTemasAprobados = false)
                    return@launch
                }

                var temasAprobados = 0

                snapshot.children.forEach { temaSnapshot ->
                    val temaId = temaSnapshot.key ?: ""

                    // Ignorar quiz_final en el conteo de temas regulares
                    if (temaId == "quiz_final") {
                        return@forEach
                    }

                    val aprobado = temaSnapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                    val porcentaje = temaSnapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                    if (aprobado && porcentaje >= 80) {
                        temasAprobados++
                    }
                }

                val todosAprobados = temasAprobados >= totalTemas

                _uiState.value = _uiState.value.copy(todosTemasAprobados = todosAprobados)

                Log.d(TAG, "Temas aprobados: $temasAprobados/$totalTemas")
                Log.d(TAG, "Quiz Final disponible: $todosAprobados")

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando temas: ${e.message}")
                _uiState.value = _uiState.value.copy(todosTemasAprobados = false)
            }
        }
    }

    private fun parsearMensajeError(mensaje: String?): String {
        return when {
            mensaje == null -> "Error desconocido"
            mensaje.contains("vidas", ignoreCase = true) -> "No tienes vidas disponibles"
            mensaje.contains("explicacion", ignoreCase = true) -> "Debes ver la explicacion primero"
            else -> mensaje
        }
    }

    private fun mostrarMensaje(mensaje: String, tipo: TipoMensaje) {
        _uiState.value = _uiState.value.copy(
            mostrarMensajeFlotante = true,
            mensajeFlotante = mensaje,
            tipoMensaje = tipo
        )
    }

    fun ocultarMensaje() {
        _uiState.value = _uiState.value.copy(
            mostrarMensajeFlotante = false,
            mensajeFlotante = null
        )
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun limpiarQuiz() {
        _uiState.value = _uiState.value.copy(
            quizActivo = null,
            preguntaActual = 0,
            respuestas = emptyList(),
            resultadoQuiz = null,
            retroalimentacion = null,
            error = null,
            finalizando = false,
            modoActual = "oficial",
            respuestaProcesada = false,
            mostrarConfetti = false,
            quizInterrumpidoPorVidas = false
        )
    }

    fun mostrarDialogoSinVidas() {
        _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
    }

    fun cerrarDialogoSinVidas() {
        _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = false)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel destruido, limpiando recursos")
        detenerObservadores()
        cursoIdActual = null
        temaIdActual = null
    }
}