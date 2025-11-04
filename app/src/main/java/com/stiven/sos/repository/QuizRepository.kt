package com.stiven.sos.repository

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class QuizRepository(private val application: Application) {

    private val TAG = "QuizRepository"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val prefs = application.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

    /**
     * Obtener token de autenticación
     */
    private suspend fun getAuthToken(): String {
        val currentUser = auth.currentUser
            ?: throw Exception("Usuario no autenticado")

        return currentUser.getIdToken(false).await().token
            ?: throw Exception("No se pudo obtener el token")
    }

    /**
     * Marcar explicación como vista
     */
    suspend fun marcarExplicacionVista(temaId: String): Result<Unit> {
        return try {
            val token = getAuthToken()
            val request = mapOf("temaId" to temaId)
            val response = ApiClient.apiService.marcarExplicacionVista(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al marcar explicación: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Iniciar quiz
     */
    suspend fun iniciarQuiz(cursoId: String, temaId: String): Result<IniciarQuizResponse> {
        return try {
            val token = getAuthToken()
            val request = IniciarQuizRequest(cursoId = cursoId, temaId = temaId)
            val response = ApiClient.apiService.iniciarQuiz(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "No tienes vidas disponibles o no estás inscrito"
                    404 -> "Curso o tema no encontrado"
                    else -> "Error al iniciar quiz: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finalizar quiz
     */
    suspend fun finalizarQuiz(
        quizId: String,
        respuestas: List<RespuestaUsuario>
    ): Result<FinalizarQuizResponse> {
        return try {
            val token = getAuthToken()
            val request = FinalizarQuizRequest(quizId = quizId, respuestas = respuestas)
            val response = ApiClient.apiService.finalizarQuiz(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error al finalizar quiz: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener revisión del quiz
     */
    suspend fun obtenerRevisionQuiz(quizId: String): Result<RevisionQuizResponse> {
        return try {
            val token = getAuthToken()
            val response = ApiClient.apiService.obtenerRevisionQuiz(quizId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Revisión no encontrada"))
            } else {
                Result.failure(Exception("Error al obtener revisión: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ✅ NUEVO: Sincronizar progreso con inscripción
     * Actualiza experiencia y racha en Firebase después de completar un quiz
     */
    suspend fun sincronizarProgresoConInscripcion(
        cursoId: String,
        experienciaGanada: Int,
        quizAprobado: Boolean
    ): Result<Unit> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val inscripcionRef = database.getReference("inscripciones")
                .child(cursoId)
                .child(userUid)

            // Obtener inscripción actual
            val snapshot = inscripcionRef.get().await()

            if (!snapshot.exists()) {
                Log.w(TAG, "⚠️ Inscripción no existe, creando campos de progreso")
                // Crear campos iniciales si no existen
                val updates = hashMapOf<String, Any>(
                    "experiencia" to experienciaGanada,
                    "diasConsecutivos" to if (quizAprobado) 1 else 0,
                    "ultimaFecha" to System.currentTimeMillis()
                )
                inscripcionRef.updateChildren(updates).await()
                Log.d(TAG, "✅ Progreso inicial creado: XP=$experienciaGanada, Racha=${if (quizAprobado) 1 else 0}")
                return Result.success(Unit)
            }

            // Obtener valores actuales
            val expActual = snapshot.child("experiencia").getValue(Int::class.java) ?: 0
            val rachaActual = snapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0
            val ultimaFecha = snapshot.child("ultimaFecha").getValue(Long::class.java) ?: 0L

            // Calcular nueva experiencia (SIEMPRE se suma)
            val nuevaExp = expActual + experienciaGanada

            // Calcular nueva racha (solo si aprobó con 70% o más)
            val ahora = System.currentTimeMillis()
            val unDiaEnMilis = 24 * 60 * 60 * 1000L
            val diferenciaDias = if (ultimaFecha > 0) {
                (ahora - ultimaFecha) / unDiaEnMilis
            } else {
                0L
            }

            val nuevaRacha = if (quizAprobado) {
                when {
                    ultimaFecha == 0L -> {
                        Log.d(TAG, "Primera vez completando quiz aprobado")
                        1
                    }
                    diferenciaDias == 0L -> {
                        Log.d(TAG, "Mismo día, racha se mantiene")
                        rachaActual // Mismo día, no incrementar
                    }
                    diferenciaDias == 1L -> {
                        Log.d(TAG, "Día consecutivo, incrementando racha")
                        rachaActual + 1
                    }
                    else -> {
                        Log.d(TAG, "Se rompió la racha (${diferenciaDias} días), reiniciando")
                        1
                    }
                }
            } else {
                Log.d(TAG, "Quiz no aprobado, racha se mantiene")
                rachaActual // No cambiar racha si no aprobó
            }

            // Actualizar Firebase
            val updates = hashMapOf<String, Any>(
                "experiencia" to nuevaExp,
                "diasConsecutivos" to nuevaRacha,
                "ultimaFecha" to ahora
            )

            inscripcionRef.updateChildren(updates).await()

            Log.d(TAG, "✅ Progreso sincronizado correctamente:")
            Log.d(TAG, "   • XP: $expActual → $nuevaExp (+$experienciaGanada)")
            Log.d(TAG, "   • Racha: $rachaActual → $nuevaRacha")
            Log.d(TAG, "   • Aprobado: $quizAprobado")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando progreso con inscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener retroalimentación
     */
    suspend fun obtenerRetroalimentacion(quizId: String): Result<RetroalimentacionFallosResponse> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // Primero verificar en Firebase si el quiz tiene errores
            val quizSnapshot = database.getReference("quizzes").child(quizId).get().await()

            if (!quizSnapshot.exists()) {
                return Result.failure(Exception("Quiz no encontrado"))
            }

            val preguntasIncorrectas = quizSnapshot.child("preguntasIncorrectas").getValue(Int::class.java) ?: 0

            Log.d(TAG, "Quiz $quizId - Preguntas incorrectas en Firebase: $preguntasIncorrectas")

            // Si no hay errores, devolver retroalimentación vacía
            if (preguntasIncorrectas == 0) {
                Log.d(TAG, "Quiz perfecto - No hay errores")
                return Result.success(RetroalimentacionFallosResponse(
                    quizId = quizId,
                    totalFallos = 0,
                    preguntasFalladas = emptyList()
                ))
            }

            // Si hay errores, intentar obtener la retroalimentación del backend
            val token = getAuthToken()
            val response = ApiClient.apiService.obtenerRetroalimentacion(quizId)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "Retroalimentación obtenida: ${it.totalFallos} fallos")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía del servidor"))
                }
                404 -> {
                    Log.w(TAG, "Backend no tiene retroalimentación, construyendo desde Firebase")
                    construirRetroalimentacionDesdeFirebase(quizId, userUid)
                }
                else -> {
                    Result.failure(Exception("Error al obtener retroalimentación: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo retroalimentación", e)
            Result.failure(e)
        }
    }

    /**
     * Construir retroalimentación desde Firebase
     */
    private suspend fun construirRetroalimentacionDesdeFirebase(
        quizId: String,
        userId: String
    ): Result<RetroalimentacionFallosResponse> = withContext(Dispatchers.IO) {
        try {
            val quizRef = database.getReference("quizzes").child(quizId)
            val snapshot = quizRef.get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Quiz no encontrado en Firebase"))
            }

            val respuestasSnapshot = snapshot.child("respuestas")
            val preguntasFalladas = mutableListOf<RetroalimentacionPregunta>()

            respuestasSnapshot.children.forEach { respuestaSnapshot ->
                try {
                    val esCorrecta = respuestaSnapshot.child("esCorrecta").getValue(Boolean::class.java) ?: true

                    if (!esCorrecta) {
                        val preguntaId = respuestaSnapshot.child("preguntaId").getValue(String::class.java) ?: ""

                        if (preguntaId.isNotEmpty()) {
                            val preguntaSnapshot = database.getReference("preguntas").child(preguntaId).get().await()

                            if (preguntaSnapshot.exists()) {
                                val textoPregunta = preguntaSnapshot.child("texto").getValue(String::class.java) ?: "Pregunta sin texto"
                                val respuestaUsuario = respuestaSnapshot.child("respuestaSeleccionada").getValue(Int::class.java) ?: -1
                                val respuestaCorrecta = preguntaSnapshot.child("respuestaCorrecta").getValue(Int::class.java) ?: 0

                                val explicacion = preguntaSnapshot.child("explicacionCorrecta").getValue(String::class.java)
                                    ?: preguntaSnapshot.child("explicacion").getValue(String::class.java)
                                    ?: "No hay explicación disponible para esta pregunta"

                                val opcionesSnapshot = preguntaSnapshot.child("opciones")
                                var respuestaUsuarioTexto = "Opción no encontrada"
                                var respuestaCorrectaTexto = "Opción no encontrada"

                                opcionesSnapshot.children.forEachIndexed { index, opcion ->
                                    val textoOpcion = opcion.child("texto").getValue(String::class.java) ?: ""
                                    if (index == respuestaUsuario) {
                                        respuestaUsuarioTexto = textoOpcion
                                    }
                                    if (index == respuestaCorrecta) {
                                        respuestaCorrectaTexto = textoOpcion
                                    }
                                }

                                preguntasFalladas.add(RetroalimentacionPregunta(
                                    preguntaId = preguntaId,
                                    texto = textoPregunta,
                                    respuestaUsuarioTexto = respuestaUsuarioTexto,
                                    respuestaCorrectaTexto = respuestaCorrectaTexto,
                                    explicacion = explicacion
                                ))

                                Log.d(TAG, "Pregunta fallada construida: $preguntaId")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando respuesta individual", e)
                }
            }

            Log.d(TAG, "✅ Retroalimentación construida desde Firebase: ${preguntasFalladas.size} fallos")

            Result.success(RetroalimentacionFallosResponse(
                quizId = quizId,
                totalFallos = preguntasFalladas.size,
                preguntasFalladas = preguntasFalladas
            ))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error construyendo retroalimentación desde Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener vidas del curso
     */
    suspend fun obtenerVidas(cursoId: String): Result<VidasResponse> {
        return try {
            val token = getAuthToken()
            val response = ApiClient.apiService.obtenerVidas(cursoId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No se pudieron obtener las vidas"))
            } else {
                Result.failure(Exception("Error al obtener vidas: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener información del tema
     */
    suspend fun obtenerTemaInfo(cursoId: String, temaId: String): Result<TemaInfoResponse> {
        return try {
            val token = getAuthToken()
            val response = ApiClient.apiService.obtenerTemaInfo(cursoId, temaId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Información no encontrada"))
            } else {
                Result.failure(Exception("Error al obtener información: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener cursos inscritos
     */
    suspend fun obtenerCursosInscritos(): Result<List<Curso>> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val responseCursos = ApiClient.apiService.obtenerCursos()

            if (!responseCursos.isSuccessful) {
                return Result.failure(Exception("Error al obtener cursos: ${responseCursos.code()}"))
            }

            val todosCursos = responseCursos.body() ?: emptyList()
            val inscripcionesRef = database.getReference("inscripciones")
            val cursosInscritos = mutableListOf<Curso>()

            for (curso in todosCursos) {
                try {
                    val snapshot = inscripcionesRef
                        .child(curso.id!!)
                        .child(userUid)
                        .get()
                        .await()

                    if (snapshot.exists()) {
                        val estado = snapshot.child("estado").getValue(String::class.java)
                        if (estado == "aprobado") {
                            cursosInscritos.add(curso)
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            Result.success(cursosInscritos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener progreso del estudiante en el curso
     */
    suspend fun obtenerProgresoCurso(cursoId: String): Result<ProgresoCurso> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val inscripcionRef = database.getReference("inscripciones")
                .child(cursoId)
                .child(userUid)

            val snapshot = inscripcionRef.get().await()

            if (snapshot.exists()) {
                val experiencia = snapshot.child("experiencia").getValue(Int::class.java) ?: 0
                val racha = snapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0
                val practicasCompletadas = 0

                Result.success(ProgresoCurso(
                    experiencia = experiencia,
                    rachaDias = racha,
                    practicasCompletadas = practicasCompletadas
                ))
            } else {
                Result.success(ProgresoCurso(0, 0, 0))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observar vidas en tiempo real
     */
    fun observarVidasTiempoReal(
        cursoId: String,
        onVidasActualizadas: (VidasResponse) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val inscripcionRef = database.getReference("inscripciones").child(cursoId).child(userUid)

        val listener = inscripcionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val vidasActuales = snapshot.child("vidasActuales").getValue(Int::class.java) ?: 5
                    val vidasMax = snapshot.child("vidasMax").getValue(Int::class.java) ?: 5
                    val ultimaRegen = snapshot.child("ultimaRegen").getValue(Long::class.java) ?: 0L

                    val ahora = System.currentTimeMillis()
                    val VIDA_REGEN_MINUTOS = 30
                    val minutosParaProximaVida = if (vidasActuales < vidasMax) {
                        val minutosTranscurridos = ((ahora - ultimaRegen) / (1000 * 60)).toInt()
                        (VIDA_REGEN_MINUTOS - minutosTranscurridos).coerceAtLeast(0)
                    } else {
                        0
                    }

                    onVidasActualizadas(VidasResponse(
                        vidasActuales = vidasActuales,
                        vidasMax = vidasMax,
                        minutosParaProximaVida = minutosParaProximaVida
                    ))

                    Log.d(TAG, "✅ Vidas actualizadas: $vidasActuales/$vidasMax")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observando vidas: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    /**
     * Observar progreso en tiempo real (DESDE INSCRIPCIONES)
     */
    fun observarProgresoTiempoReal(
        cursoId: String,
        onProgresoActualizado: (ProgresoCurso) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val inscripcionRef = database.getReference("inscripciones")
            .child(cursoId)
            .child(userUid)

        val listener = inscripcionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val experiencia = snapshot.child("experiencia").getValue(Int::class.java) ?: 0
                    val racha = snapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0
                    val practicas = 0

                    onProgresoActualizado(ProgresoCurso(
                        experiencia = experiencia,
                        rachaDias = racha,
                        practicasCompletadas = practicas
                    ))

                    Log.d(TAG, "✅ Progreso actualizado: XP=$experiencia, Racha=$racha")
                } else {
                    Log.w(TAG, "⚠️ No existe inscripción, progreso en 0")
                    onProgresoActualizado(ProgresoCurso(0, 0, 0))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observando progreso: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    /**
     * Detener observación de un listener
     */
    fun detenerObservacion(cursoId: String, listener: ValueEventListener, tipo: String) {
        val userUid = prefs.getString("user_uid", "") ?: ""

        when (tipo) {
            "vidas" -> {
                database.getReference("inscripciones")
                    .child(cursoId)
                    .child(userUid)
                    .removeEventListener(listener)
            }
            "progreso" -> {
                database.getReference("inscripciones")
                    .child(cursoId)
                    .child(userUid)
                    .removeEventListener(listener)
            }
        }
    }
}

/**
 * Modelo para progreso del curso
 */
data class ProgresoCurso(
    val experiencia: Int = 0,
    val rachaDias: Int = 0,
    val practicasCompletadas: Int = 0
)