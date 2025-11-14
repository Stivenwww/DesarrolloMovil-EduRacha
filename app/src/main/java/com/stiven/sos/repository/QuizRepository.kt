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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class QuizRepository(private val application: Application) {

    private val TAG = "QuizRepository"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val prefs = application.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

    // CONSTANTES DE REGENERACIÓN
    companion object {
        const val VIDA_REGEN_MINUTOS = 30
        const val VIDAS_MAX = 5
    }

    suspend fun obtenerCursosInscritos(): Result<List<Curso>> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                Log.e(TAG, " Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, " Buscando cursos inscritos para usuario: $userUid")

            val cursosConProgreso = mutableSetOf<String>()

            try {
                val progresoSnapshot = database.getReference("usuarios/$userUid/cursos").get().await()

                if (progresoSnapshot.exists()) {
                    progresoSnapshot.children.forEach { cursoSnapshot ->
                        val cursoId = cursoSnapshot.key
                        val progresoData = cursoSnapshot.child("progreso").value

                        if (cursoId != null && progresoData != null) {
                            cursosConProgreso.add(cursoId)
                            Log.d(TAG, "✓ Usuario tiene progreso en curso: $cursoId")
                        }
                    }
                }

                Log.d(TAG, "📊 Total cursos con progreso: ${cursosConProgreso.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error leyendo progreso de Firebase", e)
            }

            if (cursosConProgreso.isEmpty()) {
                Log.i(TAG, "ℹ️ Usuario no tiene cursos inscritos")
                return Result.success(emptyList())
            }

            val responseCursos = ApiClient.apiService.obtenerCursos()
            if (!responseCursos.isSuccessful) {
                Log.e(TAG, "❌ Error al obtener cursos: ${responseCursos.code()}")
                return Result.failure(Exception("Error al obtener cursos: ${responseCursos.code()}"))
            }

            val todosCursos = responseCursos.body() ?: emptyList()
            Log.d(TAG, "📚 Total cursos en backend: ${todosCursos.size}")

            val cursosInscritos = todosCursos.filter { curso ->
                val estaInscrito = curso.id in cursosConProgreso
                if (estaInscrito) {
                    Log.d(TAG, " Curso inscrito: ${curso.titulo} (${curso.id})")
                }
                estaInscrito
            }

            Log.d(TAG, "Total cursos inscritos: ${cursosInscritos.size}")

            if (cursosInscritos.isEmpty()) {
                Log.w(TAG, " Usuario tiene progreso pero no coinciden con cursos del backend")
            }

            Result.success(cursosInscritos)

        } catch (e: Exception) {
            Log.e(TAG, " Error en obtenerCursosInscritos", e)
            Result.failure(e)
        }
    }

    suspend fun marcarExplicacionVista(temaId: String): Result<Unit> = withContext(NonCancellable) {
        return@withContext try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, " Usuario no autenticado")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "📱 Marcando explicación como vista para tema: $temaId")
            Log.d(TAG, "👤 Usuario: ${currentUser.uid}")

            try {
                val token = currentUser.getIdToken(true).await()
                if (token.token.isNullOrEmpty()) {
                    Log.e(TAG, " Token vacío")
                    return@withContext Result.failure(Exception("No se pudo obtener el token de autenticación"))
                }
                Log.d(TAG, " Token válido: ${token.token?.take(30)}...")
            } catch (e: Exception) {
                Log.e(TAG, " Error obteniendo token", e)
                return@withContext Result.failure(Exception("Error de autenticación: ${e.message}"))
            }

            val request = mapOf("temaId" to temaId)
            val response = ApiClient.apiService.marcarExplicacionVista(request)

            when (response.code()) {
                200 -> {
                    Log.d(TAG, " Explicación marcada como vista")
                    Result.success(Unit)
                }
                401 -> {
                    Log.e(TAG, " Token rechazado por el backend")
                    Result.failure(Exception("Sesión expirada. Por favor, vuelve a iniciar sesión."))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, " Error 400: $errorBody")
                    Result.failure(Exception(errorBody ?: "Solicitud inválida"))
                }
                else -> {
                    Log.e(TAG, " Error ${response.code()}")
                    Result.failure(Exception("Error al marcar explicación: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, " Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    suspend fun iniciarQuiz(
        cursoId: String,
        temaId: String,
        modo: String = "oficial"
    ): Result<IniciarQuizResponse> = withContext(NonCancellable) {
        return@withContext try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, " Usuario no autenticado")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, " Iniciando quiz - Curso: $cursoId, Tema: $temaId, Modo: $modo")

            try {
                val token = currentUser.getIdToken(true).await()
                if (token.token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No se pudo obtener el token de autenticación"))
                }
                Log.d(TAG, " Token válido para iniciar quiz")
            } catch (e: Exception) {
                Log.e(TAG, " Error obteniendo token", e)
                return@withContext Result.failure(Exception("Error de autenticación: ${e.message}"))
            }

            val request = IniciarQuizRequest(
                cursoId = cursoId,
                temaId = temaId,
                modo = modo
            )

            Log.d(TAG, " Enviando request: cursoId=$cursoId, temaId=$temaId, modo=$modo")

            val response = ApiClient.apiService.iniciarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, " Quiz iniciado: ${it.quizId} con ${it.preguntas.size} preguntas (modo: $modo)")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía del servidor"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, " Validación fallida: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Log.e(TAG, " Token inválido")
                    Result.failure(Exception("Sesión expirada. Por favor, vuelve a iniciar sesión."))
                }
                403 -> {
                    val errorBody = response.errorBody()?.string() ?: "No tienes permiso"
                    Log.e(TAG, " Acceso denegado: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                404 -> {
                    Log.e(TAG, " Recurso no encontrado")
                    Result.failure(Exception("Curso o tema no encontrado"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, " Error ${response.code()}: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error al iniciar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, " Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexión. Verifica tu internet."))
        }
    }

    suspend fun finalizarQuiz(
        quizId: String,
        respuestas: List<RespuestaUsuario>
    ): Result<FinalizarQuizResponse> = withContext(NonCancellable) {
        return@withContext try {
            Log.d(TAG, "🏁 Finalizando quiz: $quizId con ${respuestas.size} respuestas")

            val request = FinalizarQuizRequest(quizId = quizId, respuestas = respuestas)
            val response = ApiClient.apiService.finalizarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, " Quiz finalizado exitosamente")
                        Log.d(TAG, " Correctas: ${it.preguntasCorrectas}, Incorrectas: ${it.preguntasIncorrectas}")
                        Log.d(TAG, " XP: ${it.experienciaGanada}, Vidas: ${it.vidasRestantes}")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacía"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, " Error: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, " Error ${response.code()}: $errorBody")
                    Result.failure(Exception("Error al finalizar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, " Error de red: ${e.message}", e)
            Result.failure(Exception("Error de conexión"))
        }
    }

    suspend fun verificarModosDisponibles(
        cursoId: String,
        temaId: String
    ): Result<ModoQuizDisponibleResponse> {
        return try {
            val response = ApiClient.apiService.verificarModosDisponibles(cursoId, temaId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Log.d(TAG, " Modos disponibles: ${it.modosDisponibles}")
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, " Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerRetroalimentacion(quizId: String): Result<RetroalimentacionFallosResponse> =
        withContext(NonCancellable) {
            return@withContext try {
                Log.d(TAG, " Obteniendo retroalimentación para quiz: $quizId")

                val response = ApiClient.apiService.obtenerRetroalimentacion(quizId)

                when (response.code()) {
                    200 -> {
                        response.body()?.let {
                            Log.d(TAG, " Retroalimentación obtenida: ${it.totalFallos} fallos")
                            Result.success(it)
                        } ?: Result.failure(Exception("Respuesta vacía"))
                    }
                    404 -> {
                        Log.d(TAG, "ℹ Sin retroalimentación disponible")
                        Result.success(RetroalimentacionFallosResponse(
                            quizId = quizId,
                            totalFallos = 0,
                            preguntasFalladas = emptyList()
                        ))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, " Error ${response.code()}: $errorBody")
                        Result.failure(Exception("Error al obtener retroalimentación: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, " Error al obtener retroalimentación: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun obtenerProgresoCurso(cursoId: String): Result<ProgresoCurso> {
        return try {
            val response = ApiClient.apiService.obtenerExperiencia(cursoId)

            if (response.isSuccessful) {
                response.body()?.let { data ->
                    val experiencia = (data["experiencia"] as? Number)?.toInt() ?: 0
                    val diasConsecutivos = (data["diasConsecutivos"] as? Number)?.toInt() ?: 0
                    val vidas = (data["vidas"] as? Number)?.toInt() ?: 5

                    Log.d(TAG, "✅ Progreso: XP=$experiencia, Racha=$diasConsecutivos, Vidas=$vidas")

                    Result.success(ProgresoCurso(
                        experiencia = experiencia,
                        rachaDias = diasConsecutivos,
                        vidas = vidas
                    ))
                } ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observarProgresoTiempoReal(
        cursoId: String,
        onProgresoActualizado: (ProgresoCurso) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/progreso")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.value as? Map<String, Any>
                    val experiencia = (data?.get("experiencia") as? Number)?.toInt() ?: 0
                    val diasConsecutivos = (data?.get("diasConsecutivos") as? Number)?.toInt() ?: 0
                    val vidas = (data?.get("vidas") as? Number)?.toInt() ?: 5

                    onProgresoActualizado(ProgresoCurso(
                        experiencia = experiencia,
                        rachaDias = diasConsecutivos,
                        vidas = vidas
                    ))

                    Log.d(TAG, "📊 Progreso: XP=$experiencia, Racha=$diasConsecutivos, Vidas=$vidas")
                } else {
                    onProgresoActualizado(ProgresoCurso(0, 0, 5))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    // ✅ CORREGIDO: Sistema de regeneración de vidas completamente funcional
    fun observarVidasTiempoReal(
        cursoId: String,
        onVidasActualizadas: (VidasResponse) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val userUid = prefs.getString("user_uid", "") ?: ""
        val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/progreso")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.value as? Map<String, Any>
                    var vidas = (data?.get("vidas") as? Number)?.toInt() ?: VIDAS_MAX
                    val vidasMax = (data?.get("vidasMax") as? Number)?.toInt() ?: VIDAS_MAX
                    var ultimaRegen = (data?.get("ultimaRegen") as? Number)?.toLong() ?: System.currentTimeMillis()

                    val ahora = System.currentTimeMillis()

                    // ✅ REGENERACIÓN AUTOMÁTICA
                    if (vidas < vidasMax) {
                        val tiempoTranscurrido = ahora - ultimaRegen
                        val minutosPasados = (tiempoTranscurrido / (1000 * 60)).toInt()
                        val vidasRecuperadas = minutosPasados / VIDA_REGEN_MINUTOS

                        Log.d(TAG, "🔄 Regeneración: minutos=$minutosPasados, vidas recuperadas=$vidasRecuperadas")

                        if (vidasRecuperadas > 0) {
                            val nuevasVidas = (vidas + vidasRecuperadas).coerceAtMost(vidasMax)
                            val nuevaUltimaRegen = ultimaRegen + (vidasRecuperadas * VIDA_REGEN_MINUTOS * 60 * 1000L)

                            Log.d(TAG, "✅ Regenerando: $vidas -> $nuevasVidas vidas")

                            // Actualizar en Firebase
                            val actualizacion = mapOf(
                                "vidas" to nuevasVidas,
                                "ultimaRegen" to nuevaUltimaRegen
                            )

                            ref.updateChildren(actualizacion).addOnSuccessListener {
                                Log.d(TAG, "💾 Vidas actualizadas en Firebase: $nuevasVidas")
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "❌ Error actualizando vidas: ${e.message}")
                            }

                            vidas = nuevasVidas
                            ultimaRegen = nuevaUltimaRegen
                        }
                    }

                    // Calcular minutos para próxima vida
                    val minutosParaProxima = if (vidas < vidasMax) {
                        val tiempoTranscurrido = ahora - ultimaRegen
                        val minutosPasados = (tiempoTranscurrido / (1000 * 60)).toInt()
                        val minutosEnCicloActual = minutosPasados % VIDA_REGEN_MINUTOS
                        val minutosRestantes = VIDA_REGEN_MINUTOS - minutosEnCicloActual
                        minutosRestantes.coerceIn(0, VIDA_REGEN_MINUTOS)
                    } else {
                        0
                    }

                    onVidasActualizadas(VidasResponse(
                        vidasActuales = vidas,
                        vidasMax = vidasMax,
                        minutosParaProximaVida = minutosParaProxima
                    ))

                    Log.d(TAG, "💚 Vidas: $vidas/$vidasMax | Próxima en: ${minutosParaProxima}min")
                } else {
                    onVidasActualizadas(VidasResponse(VIDAS_MAX, VIDAS_MAX, 0))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    fun detenerObservacion(cursoId: String, listener: ValueEventListener) {
        val userUid = prefs.getString("user_uid", "") ?: ""
        database.getReference("usuarios/$userUid/cursos/$cursoId/progreso")
            .removeEventListener(listener)
        Log.d(TAG, "🛑 Observador detenido")
    }

    // ✅ NUEVO: Verificar si un tema está aprobado
    suspend fun verificarTemaAprobado(cursoId: String, temaId: String): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

            val snapshot = ref.get().await()

            if (snapshot.exists()) {
                val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = snapshot.child("porcentajePromedio").getValue(Int::class.java) ?: 0

                Log.d(TAG, "✅ Tema $temaId: aprobado=$aprobado, porcentaje=$porcentaje%")
                Result.success(aprobado)
            } else {
                Log.d(TAG, "ℹ️ Tema $temaId sin progreso")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando tema aprobado: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✅ NUEVO: Verificar si todos los temas están aprobados
    suspend fun verificarTodosTemasAprobados(cursoId: String, totalTemas: Int): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados")

            val snapshot = ref.get().await()

            var temasAprobados = 0

            snapshot.children.forEach { temaSnap ->
                val aprobado = temaSnap.child("aprobado").getValue(Boolean::class.java) ?: false
                if (aprobado) temasAprobados++
            }

            val todosAprobados = temasAprobados >= totalTemas

            Log.d(TAG, "✅ Progreso temas: $temasAprobados/$totalTemas aprobados")
            Result.success(todosAprobados)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando temas: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class ProgresoCurso(
    val experiencia: Int = 0,
    val rachaDias: Int = 0,
    val vidas: Int = 5
)