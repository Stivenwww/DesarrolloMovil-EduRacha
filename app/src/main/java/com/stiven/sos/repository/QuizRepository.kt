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
import android.util.Log

class QuizRepository(private val application: Application) {

    private val TAG = "QuizRepository"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val prefs = application.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

    /**
     * ✅ Marcar explicación como vista - CORREGIDO
     */
    suspend fun marcarExplicacionVista(temaId: String): Result<Unit> {
        return try {
            // ✅ IMPORTANTE: Verificar que el usuario esté autenticado
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ Usuario no autenticado en Firebase")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "📱 Marcando explicación como vista para tema: $temaId")
            Log.d(TAG, "👤 Usuario: ${currentUser.uid}")

            // ✅ Verificar que el token sea válido
            val token = currentUser.getIdToken(true).await()
            Log.d(TAG, "✅ Token obtenido: ${token.token?.take(30)}...")

            val request = mapOf("temaId" to temaId)
            val response = ApiClient.apiService.marcarExplicacionVista(request)

            when (response.code()) {
                200 -> {
                    Log.d(TAG, "✅ Explicación marcada como vista correctamente")
                    Result.success(Unit)
                }
                401 -> {
                    Log.e(TAG, "❌ Token rechazado por el backend")
                    Result.failure(Exception("Token inválido. Por favor, vuelve a iniciar sesión."))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Error 400: $errorBody")
                    Result.failure(Exception(errorBody ?: "Solicitud inválida"))
                }
                else -> {
                    Log.e(TAG, "❌ Error ${response.code()}: ${response.message()}")
                    Result.failure(Exception("Error al marcar explicación: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    /**
     * ✅ Iniciar quiz - ACTUALIZADO
     */
    suspend fun iniciarQuiz(cursoId: String, temaId: String): Result<IniciarQuizResponse> {
        return try {
            // Verificar autenticación
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "❌ Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "🚀 Iniciando quiz - Curso: $cursoId, Tema: $temaId")
            Log.d(TAG, "👤 Usuario: ${currentUser.uid}")

            val request = IniciarQuizRequest(cursoId = cursoId, temaId = temaId)
            val response = ApiClient.apiService.iniciarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "✅ Quiz iniciado: ${it.quizId} con ${it.preguntas.size} preguntas")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía del servidor"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "❌ Validación fallida: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Log.e(TAG, "❌ Token inválido o expirado")
                    Result.failure(Exception("Sesión expirada. Por favor, vuelve a iniciar sesión."))
                }
                404 -> {
                    Log.e(TAG, "❌ Curso o tema no encontrado")
                    Result.failure(Exception("Curso o tema no encontrado"))
                }
                else -> {
                    Log.e(TAG, "❌ Error ${response.code()}: ${response.message()}")
                    Result.failure(Exception("Error al iniciar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception al iniciar quiz: ${e.message}", e)
            Result.failure(Exception("Error de conexión. Verifica tu internet."))
        }
    }

    /**
     * ✅ Finalizar quiz
     */
    suspend fun finalizarQuiz(
        quizId: String,
        respuestas: List<RespuestaUsuario>
    ): Result<FinalizarQuizResponse> {
        return try {
            val request = FinalizarQuizRequest(quizId = quizId, respuestas = respuestas)
            val response = ApiClient.apiService.finalizarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "✅ Quiz finalizado")
                        Log.d(TAG, "📊 Correctas: ${it.preguntasCorrectas}, Incorrectas: ${it.preguntasIncorrectas}")
                        Log.d(TAG, "⭐ XP ganada: ${it.experienciaGanada}, Vidas restantes: ${it.vidasRestantes}")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía del servidor"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "❌ Error al finalizar: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Log.e(TAG, "❌ Token inválido")
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Log.e(TAG, "❌ Error ${response.code()}")
                    Result.failure(Exception("Error al finalizar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error de red: ${e.message}", e)
            Result.failure(Exception("Error de conexión"))
        }
    }

    /**
     * ✅ Obtener revisión del quiz
     */
    suspend fun obtenerRevisionQuiz(quizId: String): Result<RevisionQuizResponse> {
        return try {
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
     * ✅ Obtener retroalimentación de errores
     */
    suspend fun obtenerRetroalimentacion(quizId: String): Result<RetroalimentacionFallosResponse> {
        return try {
            val response = ApiClient.apiService.obtenerRetroalimentacion(quizId)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "💡 Retroalimentación: ${it.totalFallos} fallos")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía"))
                }
                404 -> {
                    Log.d(TAG, "ℹ️ Sin retroalimentación (quiz perfecto)")
                    Result.success(RetroalimentacionFallosResponse(
                        quizId = quizId,
                        totalFallos = 0,
                        preguntasFalladas = emptyList()
                    ))
                }
                else -> {
                    Result.failure(Exception("Error ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo retroalimentación", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Obtener cursos inscritos
     */
    suspend fun obtenerCursosInscritos(): Result<List<Curso>> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val inscripcionesSnapshot = database.getReference("inscripciones").get().await()

            if (!inscripcionesSnapshot.exists()) {
                return Result.success(emptyList())
            }

            val cursosAprobadosIds = mutableListOf<String>()

            inscripcionesSnapshot.children.forEach { cursoSnapshot ->
                if (cursoSnapshot.hasChild(userUid)) {
                    val inscripcion = cursoSnapshot.child(userUid)
                    val estado = inscripcion.child("estado").getValue(String::class.java)

                    if (estado == "aprobado") {
                        cursoSnapshot.key?.let { cursosAprobadosIds.add(it) }
                    }
                }
            }

            if (cursosAprobadosIds.isEmpty()) {
                Log.d(TAG, "ℹ️ No hay cursos aprobados")
                return Result.success(emptyList())
            }

            val responseCursos = ApiClient.apiService.obtenerCursos()
            if (!responseCursos.isSuccessful) {
                return Result.failure(Exception("Error al obtener cursos: ${responseCursos.code()}"))
            }

            val todosCursos = responseCursos.body() ?: emptyList()
            val cursosInscritos = todosCursos.filter { it.id in cursosAprobadosIds }

            Log.d(TAG, "✅ ${cursosInscritos.size} cursos inscritos")
            Result.success(cursosInscritos)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en obtenerCursosInscritos", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Obtener progreso del estudiante - ACTUALIZADO para usar Firebase rachas
     */
    suspend fun obtenerProgresoCurso(cursoId: String): Result<ProgresoCurso> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // ✅ Leer directamente desde Firebase rachas
            val rachaSnapshot = database.getReference("rachas")
                .child(cursoId)
                .child(userUid)
                .get()
                .await()

            if (!rachaSnapshot.exists()) {
                Log.d(TAG, "ℹ️ No hay datos de racha, usando valores por defecto")
                return Result.success(ProgresoCurso(0, 0, 5))
            }

            val experiencia = rachaSnapshot.child("experiencia").getValue(Int::class.java) ?: 0
            val rachaDias = rachaSnapshot.child("rachaDias").getValue(Int::class.java)
                ?: rachaSnapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0
            val vidas = rachaSnapshot.child("vidas").getValue(Int::class.java) ?: 5

            Log.d(TAG, "✅ Progreso obtenido: XP=$experiencia, Racha=$rachaDias, Vidas=$vidas")

            Result.success(ProgresoCurso(
                experiencia = experiencia,
                rachaDias = rachaDias,
                vidas = vidas
            ))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo progreso", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Observar vidas en tiempo real desde Firebase - ACTUALIZADO
     */
    fun observarVidasTiempoReal(
        cursoId: String,
        onVidasActualizadas: (VidasResponse) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val rachaRef = database.getReference("rachas").child(cursoId).child(userUid)

        val listener = rachaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val vidas = snapshot.child("vidas").getValue(Int::class.java) ?: 5
                    val vidasMax = 5

                    // Calcular regeneración
                    val ultimaRegen = snapshot.child("ultimaRegen").getValue(Long::class.java)
                        ?: snapshot.child("ultimaFecha").getValue(Long::class.java) ?: 0L
                    val ahora = System.currentTimeMillis()
                    val tiempoTranscurrido = ahora - ultimaRegen
                    val minutosParaProxima = if (vidas < vidasMax && ultimaRegen > 0) {
                        30 - ((tiempoTranscurrido / (1000 * 60)).toInt() % 30)
                    } else 0

                    onVidasActualizadas(VidasResponse(
                        vidasActuales = vidas,
                        vidasMax = vidasMax,
                        minutosParaProximaVida = minutosParaProxima
                    ))

                    Log.d(TAG, "💚 Vidas actualizadas: $vidas/$vidasMax (próxima en ${minutosParaProxima}min)")
                } else {
                    // Valores por defecto si no existe
                    onVidasActualizadas(VidasResponse(5, 5, 0))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observando vidas: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    /**
     * ✅ Observar progreso en tiempo real desde Firebase - ACTUALIZADO
     */
    fun observarProgresoTiempoReal(
        cursoId: String,
        onProgresoActualizado: (ProgresoCurso) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val rachaRef = database.getReference("rachas").child(cursoId).child(userUid)

        val listener = rachaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val experiencia = snapshot.child("experiencia").getValue(Int::class.java) ?: 0
                    val rachaDias = snapshot.child("rachaDias").getValue(Int::class.java)
                        ?: snapshot.child("diasConsecutivos").getValue(Int::class.java) ?: 0
                    val vidas = snapshot.child("vidas").getValue(Int::class.java) ?: 5

                    onProgresoActualizado(ProgresoCurso(
                        experiencia = experiencia,
                        rachaDias = rachaDias,
                        vidas = vidas
                    ))

                    Log.d(TAG, "📊 Progreso: XP=$experiencia, Racha=$rachaDias, Vidas=$vidas")
                } else {
                    onProgresoActualizado(ProgresoCurso(0, 0, 5))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observando progreso: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    /**
     * Detener observación
     */
    fun detenerObservacion(cursoId: String, listener: ValueEventListener, tipo: String) {
        val userUid = prefs.getString("user_uid", "") ?: ""

        when (tipo) {
            "vidas", "progreso" -> {
                database.getReference("rachas")
                    .child(cursoId)
                    .child(userUid)
                    .removeEventListener(listener)
                Log.d(TAG, "🛑 Observador detenido: $tipo")
            }
        }
    }
}

/**
 * ✅ Modelo para progreso del curso
 */
data class ProgresoCurso(
    val experiencia: Int = 0,
    val rachaDias: Int = 0,
    val vidas: Int = 5
)