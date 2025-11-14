package com.stiven.sos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.google.firebase.auth.FirebaseAuth
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
    // ✅ NUEVOS ESTADOS PARA RACHA Y CONTROL DIARIO
    val yaResolviHoy: Boolean = false,
    val mostrarCelebracionRacha: Boolean = false,
    val rachaSubio: Boolean = false,
    val horasParaNuevoQuiz: Int = 0,
    val minutosParaNuevoQuiz: Int = 0
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
            Log.e(TAG, "Usuario no autenticado")
            mostrarMensaje("Debes iniciar sesión para continuar", TipoMensaje.ERROR)
        } else {
            Log.d(TAG, " Usuario autenticado: ${currentUser.uid}")
        }
    }

    fun iniciarObservadores(cursoId: String, temaId: String = "") {
        Log.d(TAG, " Iniciando observadores - Curso: $cursoId")

        detenerObservadores()

        cursoIdActual = cursoId
        temaIdActual = temaId

        // Observador de vidas
        listenerVidas = repository.observarVidasTiempoReal(
            cursoId = cursoId,
            onVidasActualizadas = { vidas ->
                _uiState.value = _uiState.value.copy(
                    vidas = vidas,
                    sinVidas = vidas.vidasActuales <= 0
                )
                Log.d(TAG, " Vidas: ${vidas.vidasActuales}/${vidas.vidasMax}")

                //  Si se acaban las vidas durante quiz activo
                if (vidas.vidasActuales == 0 && _uiState.value.quizActivo != null) {
                    _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
                }
            },
            onError = { error ->
                Log.e(TAG, " Error en observador de vidas: ${error.message}")
            }
        )

        // Observador de progreso
        listenerProgreso = repository.observarProgresoTiempoReal(
            cursoId = cursoId,
            onProgresoActualizado = { progreso ->
                _uiState.value = _uiState.value.copy(progreso = progreso)
                Log.d(TAG, " XP: ${progreso.experiencia}, Racha: ${progreso.rachaDias} días")
            },
            onError = { error ->
                Log.e(TAG, " Error en observador de progreso: ${error.message}")
            }
        )

        iniciarTimerRegeneracion()

        // Verificar si ya resolvió hoy (solo para temas normales)
        if (temaId.isNotEmpty() && temaId != "quiz_final") {
            verificarTemaAprobado(cursoId, temaId)
            verificarSiResolviHoy(cursoId, temaId)
        }
    }

    private fun iniciarTimerRegeneracion() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // 1 minuto
                cursoIdActual?.let { cursoId ->
                    repository.observarVidasTiempoReal(cursoId, { }, { })
                }
            }
        }
    }

    fun detenerObservadores() {
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
    }

    //  VERIFICAR SI YA RESOLVIÓ EL QUIZ OFICIAL HOY
    private fun verificarSiResolviHoy(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val ultimaFecha = snapshot.child("ultimaFechaQuiz").getValue(String::class.java)
                    val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false

                    val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val yaResolviHoy = ultimaFecha == fechaHoy

                    //  Calcular tiempo restante si ya resolvió
                    var horasRestantes = 0
                    var minutosRestantes = 0

                    if (yaResolviHoy && ultimaFecha != null) {
                        val calendar = Calendar.getInstance()
                        val horaActual = calendar.timeInMillis

                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        val medianoche = calendar.timeInMillis

                        val diferenciaMilis = medianoche - horaActual
                        horasRestantes = (diferenciaMilis / (1000 * 60 * 60)).toInt()
                        minutosRestantes = ((diferenciaMilis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                    }

                    _uiState.value = _uiState.value.copy(
                        temaAprobado = aprobado,
                        puedeHacerPractica = aprobado,
                        yaResolviHoy = yaResolviHoy,
                        horasParaNuevoQuiz = horasRestantes,
                        minutosParaNuevoQuiz = minutosRestantes
                    )

                    Log.d(TAG, " Ya resolvió hoy: $yaResolviHoy, Aprobado: $aprobado")
                }
            } catch (e: Exception) {
                Log.e(TAG, " Error verificando si resolvió hoy: ${e.message}")
            }
        }
    }

    fun iniciarQuiz(cursoId: String, temaId: String, modo: String = "oficial") {
        viewModelScope.launch {
            iniciarMutex.withLock {
                if (_uiState.value.isLoading) return@withLock

                // ✅ VALIDACIÓN: No permitir quiz oficial si ya resolvió hoy
                if (modo == "oficial" && _uiState.value.yaResolviHoy && temaId != "quiz_final") {
                    mostrarMensaje(
                        "Ya resolviste el quiz oficial hoy. Vuelve en ${_uiState.value.horasParaNuevoQuiz}h ${_uiState.value.minutosParaNuevoQuiz}m",
                        TipoMensaje.ADVERTENCIA
                    )
                    return@withLock
                }

                cursoIdActual = cursoId
                temaIdActual = temaId

                // ✅ MODO PRÁCTICA Y OFICIAL quitan vidas
                if (modo == "oficial" || modo == "practica") {
                    val vidasActuales = _uiState.value.vidas?.vidasActuales ?: 5
                    if (vidasActuales == 0) {
                        mostrarMensaje("No tienes vidas disponibles", TipoMensaje.ADVERTENCIA)
                        _uiState.value = _uiState.value.copy(mostrarDialogoSinVidas = true)
                        return@withLock
                    }
                }

                if (_uiState.value.quizActivo != null) return@withLock

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    modoActual = modo
                )

                repository.iniciarQuiz(cursoId, temaId, modo).fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            quizActivo = response,
                            preguntaActual = 0,
                            respuestas = emptyList(),
                            isLoading = false,
                            finalizando = false
                        )

                        val modoTexto = when (modo) {
                            "practica" -> "Modo Práctica 🎯"
                            "final" -> "Quiz Final 🏆"
                            else -> "Quiz Oficial ⭐"
                        }
                        mostrarMensaje("¡$modoTexto iniciado!", TipoMensaje.EXITO)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        mostrarMensaje(parsearMensajeError(error.message), TipoMensaje.ERROR)
                    }
                )
            }
        }
    }

    fun responderPregunta(preguntaId: String, respuestaSeleccionada: Int) {
        val respuesta = RespuestaUsuario(
            preguntaId = preguntaId,
            respuestaSeleccionada = respuestaSeleccionada,
            tiempoSeg = 0
        )

        _uiState.value = _uiState.value.copy(
            respuestas = _uiState.value.respuestas + respuesta,
            preguntaActual = _uiState.value.preguntaActual + 1
        )
    }

    fun finalizarQuiz() {
        viewModelScope.launch {
            finalizarMutex.withLock {
                if (_uiState.value.finalizando) return@withLock

                val quizId = _uiState.value.quizActivo?.quizId
                val respuestas = _uiState.value.respuestas

                if (quizId == null || respuestas.isEmpty()) return@withLock

                _uiState.value = _uiState.value.copy(
                    finalizando = true,
                    isLoading = true
                )

                repository.finalizarQuiz(quizId, respuestas).fold(
                    onSuccess = { resultado ->
                        _uiState.value = _uiState.value.copy(
                            resultadoQuiz = resultado,
                            finalizando = false,
                            isLoading = false
                        )

                        val porcentaje = if (respuestas.isNotEmpty()) {
                            (resultado.preguntasCorrectas * 100) / respuestas.size
                        } else 0

                        // ✅ SOLO modo oficial cuenta para racha (práctica NO)
                        if (porcentaje >= 80 && _uiState.value.modoActual == "oficial") {
                            verificarYActualizarRacha()
                        }

                        if (resultado.preguntasIncorrectas > 0) {
                            obtenerRetroalimentacion(quizId)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            finalizando = false,
                            isLoading = false
                        )
                        mostrarMensaje(parsearMensajeError(error.message), TipoMensaje.ERROR)
                    }
                )
            }
        }
    }

    // ✅ VERIFICAR Y ACTUALIZAR RACHA (solo modo oficial)
    private fun verificarYActualizarRacha() {
        viewModelScope.launch {
            try {
                val userUid = prefs.getString("user_uid", "") ?: return@launch
                val cursoId = cursoIdActual ?: return@launch
                val temaId = temaIdActual ?: return@launch

                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("usuarios/$userUid/cursos/$cursoId")

                // Marcar que ya resolvió hoy
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                ref.child("temasCompletados/$temaId/ultimaFechaQuiz").setValue(fechaHoy).await()
                ref.child("temasCompletados/$temaId/aprobado").setValue(true).await()

                // Actualizar racha
                val progresoSnapshot = ref.child("progreso").get().await()
                val ultimaRachaFecha = progresoSnapshot.child("ultimaRachaFecha").getValue(String::class.java)
                val rachaActual = progresoSnapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0

                val ayer = Calendar.getInstance()
                ayer.add(Calendar.DAY_OF_YEAR, -1)
                val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ayer.time)

                var nuevaRacha = rachaActual
                var rachaSubio = false

                when {
                    ultimaRachaFecha == null || ultimaRachaFecha == "" -> {
                        // Primera vez
                        nuevaRacha = 1
                        rachaSubio = true
                    }
                    ultimaRachaFecha == fechaAyer -> {
                        // Continúa la racha
                        nuevaRacha = rachaActual + 1
                        rachaSubio = true
                    }
                    ultimaRachaFecha == fechaHoy -> {
                        // Ya había resuelto hoy
                        rachaSubio = false
                    }
                    else -> {
                        // Se rompió la racha
                        nuevaRacha = 1
                        rachaSubio = true
                    }
                }

                if (rachaSubio) {
                    ref.child("progreso/diasConsecutivos").setValue(nuevaRacha).await()
                    ref.child("progreso/ultimaRachaFecha").setValue(fechaHoy).await()

                    _uiState.value = _uiState.value.copy(
                        mostrarCelebracionRacha = true,
                        rachaSubio = true,
                        yaResolviHoy = true
                    )

                    Log.d(TAG, "🔥 ¡Racha actualizada! Nueva racha: $nuevaRacha días")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error actualizando racha: ${e.message}")
            }
        }
    }

    fun cerrarCelebracionRacha() {
        _uiState.value = _uiState.value.copy(mostrarCelebracionRacha = false)
    }

    fun obtenerRetroalimentacion(quizId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { retro ->
                    _uiState.value = _uiState.value.copy(retroalimentacion = retro)
                },
                onFailure = { }
            )
        }
    }

    fun marcarExplicacionVista(temaId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.marcarExplicacionVista(temaId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
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
            repository.verificarTodosTemasAprobados(cursoId, totalTemas).fold(
                onSuccess = { todosAprobados ->
                    _uiState.value = _uiState.value.copy(todosTemasAprobados = todosAprobados)
                },
                onFailure = { }
            )
        }
    }

    private fun parsearMensajeError(mensaje: String?): String {
        return when {
            mensaje == null -> "Error desconocido"
            mensaje.contains("vidas", ignoreCase = true) ->
                "No tienes vidas disponibles. Espera 30 minutos."
            mensaje.contains("explicación", ignoreCase = true) ->
                "Debes ver la explicación primero"
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
            modoActual = "oficial"
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
        detenerObservadores()
        cursoIdActual = null
        temaIdActual = null
    }
}