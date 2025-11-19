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

    companion object {
        const val VIDA_REGEN_MINUTOS = 30
        const val VIDAS_MAX = 5
        const val PORCENTAJE_APROBACION = 80 // Porcentaje minimo para aprobar
    }

    suspend fun obtenerCursosInscritos(): Result<List<Curso>> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            if (userUid.isEmpty()) {
                Log.e(TAG, "Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "Buscando cursos inscritos para usuario: $userUid")

            val cursosConProgreso = mutableSetOf<String>()

            try {
                val progresoSnapshot = database.getReference("usuarios/$userUid/cursos").get().await()

                if (progresoSnapshot.exists()) {
                    progresoSnapshot.children.forEach { cursoSnapshot ->
                        val cursoId = cursoSnapshot.key
                        val progresoData = cursoSnapshot.child("progreso").value

                        if (cursoId != null && progresoData != null) {
                            cursosConProgreso.add(cursoId)
                            Log.d(TAG, "Usuario tiene progreso en curso: $cursoId")
                        }
                    }
                }

                Log.d(TAG, "Total cursos con progreso: ${cursosConProgreso.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo progreso de Firebase", e)
            }

            if (cursosConProgreso.isEmpty()) {
                Log.i(TAG, "Usuario no tiene cursos inscritos")
                return Result.success(emptyList())
            }

            val responseCursos = ApiClient.apiService.obtenerCursos()
            if (!responseCursos.isSuccessful) {
                Log.e(TAG, "Error al obtener cursos: ${responseCursos.code()}")
                return Result.failure(Exception("Error al obtener cursos: ${responseCursos.code()}"))
            }

            val todosCursos = responseCursos.body() ?: emptyList()
            Log.d(TAG, "Total cursos en backend: ${todosCursos.size}")

            val cursosInscritos = todosCursos.filter { curso ->
                val estaInscrito = curso.id in cursosConProgreso
                if (estaInscrito) {
                    Log.d(TAG, "Curso inscrito: ${curso.titulo} (${curso.id})")
                }
                estaInscrito
            }

            Log.d(TAG, "Total cursos inscritos: ${cursosInscritos.size}")

            if (cursosInscritos.isEmpty()) {
                Log.w(TAG, "Usuario tiene progreso pero no coinciden con cursos del backend")
            }

            Result.success(cursosInscritos)

        } catch (e: Exception) {
            Log.e(TAG, "Error en obtenerCursosInscritos", e)
            Result.failure(e)
        }
    }

    suspend fun marcarExplicacionVista(temaId: String): Result<Unit> = withContext(NonCancellable) {
        return@withContext try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "Usuario no autenticado")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "Marcando explicacion como vista para tema: $temaId")
            Log.d(TAG, "Usuario: ${currentUser.uid}")

            try {
                val token = currentUser.getIdToken(true).await()
                if (token.token.isNullOrEmpty()) {
                    Log.e(TAG, "Token vacio")
                    return@withContext Result.failure(Exception("No se pudo obtener el token de autenticacion"))
                }
                Log.d(TAG, "Token valido: ${token.token?.take(30)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo token", e)
                return@withContext Result.failure(Exception("Error de autenticacion: ${e.message}"))
            }

            val request = mapOf("temaId" to temaId)
            val response = ApiClient.apiService.marcarExplicacionVista(request)

            when (response.code()) {
                200 -> {
                    Log.d(TAG, "Explicacion marcada como vista")
                    Result.success(Unit)
                }
                401 -> {
                    Log.e(TAG, "Token rechazado por el backend")
                    Result.failure(Exception("Sesion expirada. Por favor, vuelve a iniciar sesion."))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error 400: $errorBody")
                    Result.failure(Exception(errorBody ?: "Solicitud invalida"))
                }
                else -> {
                    Log.e(TAG, "Error ${response.code()}")
                    Result.failure(Exception("Error al marcar explicacion: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexion: ${e.message}"))
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
                Log.e(TAG, "Usuario no autenticado")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "Iniciando quiz - Curso: $cursoId, Tema: $temaId, Modo: $modo")

            try {
                val token = currentUser.getIdToken(true).await()
                if (token.token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No se pudo obtener el token de autenticacion"))
                }
                Log.d(TAG, "Token valido para iniciar quiz")
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo token", e)
                return@withContext Result.failure(Exception("Error de autenticacion: ${e.message}"))
            }

            val request = IniciarQuizRequest(
                cursoId = cursoId,
                temaId = temaId,
                modo = modo
            )

            Log.d(TAG, "Enviando request: cursoId=$cursoId, temaId=$temaId, modo=$modo")

            val response = ApiClient.apiService.iniciarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "Quiz iniciado: ${it.quizId} con ${it.preguntas.size} preguntas (modo: $modo)")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacia del servidor"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Validacion fallida: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Log.e(TAG, "Token invalido")
                    Result.failure(Exception("Sesion expirada. Por favor, vuelve a iniciar sesion."))
                }
                403 -> {
                    val errorBody = response.errorBody()?.string() ?: "No tienes permiso"
                    Log.e(TAG, "Acceso denegado: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                404 -> {
                    Log.e(TAG, "Recurso no encontrado")
                    Result.failure(Exception("Curso o tema no encontrado"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error ${response.code()}: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error al iniciar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexion. Verifica tu internet."))
        }
    }

    suspend fun finalizarQuiz(
        quizId: String,
        respuestas: List<RespuestaUsuario>
    ): Result<FinalizarQuizResponse> = withContext(NonCancellable) {
        return@withContext try {
            Log.d(TAG, "Finalizando quiz: $quizId con ${respuestas.size} respuestas")

            val request = FinalizarQuizRequest(quizId = quizId, respuestas = respuestas)
            val response = ApiClient.apiService.finalizarQuiz(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "Quiz finalizado exitosamente")
                        Log.d(TAG, "Correctas: ${it.preguntasCorrectas}, Incorrectas: ${it.preguntasIncorrectas}")
                        Log.d(TAG, "XP: ${it.experienciaGanada}, Vidas: ${it.vidasRestantes}")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacia"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Result.failure(Exception("Sesion expirada"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error ${response.code()}: $errorBody")
                    Result.failure(Exception("Error al finalizar quiz: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red: ${e.message}", e)
            Result.failure(Exception("Error de conexion"))
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
                    Log.d(TAG, "Modos disponibles: ${it.modosDisponibles}")
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacia"))
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerRetroalimentacion(quizId: String): Result<RetroalimentacionFallosResponse> =
        withContext(NonCancellable) {
            return@withContext try {
                Log.d(TAG, "Obteniendo retroalimentacion para quiz: $quizId")

                val response = ApiClient.apiService.obtenerRetroalimentacion(quizId)

                when (response.code()) {
                    200 -> {
                        response.body()?.let {
                            Log.d(TAG, "Retroalimentacion obtenida: ${it.totalFallos} fallos")
                            Result.success(it)
                        } ?: Result.failure(Exception("Respuesta vacia"))
                    }
                    404 -> {
                        Log.d(TAG, "Sin retroalimentacion disponible")
                        Result.success(RetroalimentacionFallosResponse(
                            quizId = quizId,
                            totalFallos = 0,
                            preguntasFalladas = emptyList()
                        ))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error ${response.code()}: $errorBody")
                        Result.failure(Exception("Error al obtener retroalimentacion: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener retroalimentacion: ${e.message}", e)
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

                    Log.d(TAG, "Progreso: XP=$experiencia, Racha=$diasConsecutivos, Vidas=$vidas")

                    Result.success(ProgresoCurso(
                        experiencia = experiencia,
                        rachaDias = diasConsecutivos,
                        vidas = vidas
                    ))
                } ?: Result.failure(Exception("Respuesta vacia"))
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
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

                    Log.d(TAG, "Progreso: XP=$experiencia, Racha=$diasConsecutivos, Vidas=$vidas")
                } else {
                    onProgresoActualizado(ProgresoCurso(0, 0, 5))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    /**
     * SISTEMA DE REGENERACION DE VIDAS
     * Observa en tiempo real las vidas del usuario y las regenera automaticamente
     * cada 30 minutos hasta alcanzar el maximo de 5 vidas
     */
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

                    // Calcular regeneracion automatica de vidas
                    if (vidas < vidasMax) {
                        val tiempoTranscurrido = ahora - ultimaRegen
                        val minutosPasados = (tiempoTranscurrido / (1000 * 60)).toInt()
                        val vidasRecuperadas = minutosPasados / VIDA_REGEN_MINUTOS

                        Log.d(TAG, "Regeneracion: minutos=$minutosPasados, vidas recuperadas=$vidasRecuperadas")

                        if (vidasRecuperadas > 0) {
                            val nuevasVidas = (vidas + vidasRecuperadas).coerceAtMost(vidasMax)
                            val nuevaUltimaRegen = ultimaRegen + (vidasRecuperadas * VIDA_REGEN_MINUTOS * 60 * 1000L)

                            Log.d(TAG, "Regenerando: $vidas -> $nuevasVidas vidas")

                            // Actualizar en Firebase
                            val actualizacion = mapOf(
                                "vidas" to nuevasVidas,
                                "ultimaRegen" to nuevaUltimaRegen
                            )

                            ref.updateChildren(actualizacion).addOnSuccessListener {
                                Log.d(TAG, "Vidas actualizadas en Firebase: $nuevasVidas")
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Error actualizando vidas: ${e.message}")
                            }

                            vidas = nuevasVidas
                            ultimaRegen = nuevaUltimaRegen
                        }
                    }

                    // Calcular minutos para proxima vida
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

                    Log.d(TAG, "Vidas: $vidas/$vidasMax | Proxima en: ${minutosParaProxima}min")
                } else {
                    onVidasActualizadas(VidasResponse(VIDAS_MAX, VIDAS_MAX, 0))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error: ${error.message}")
                onError(Exception(error.message))
            }
        })

        return listener
    }

    fun detenerObservacion(cursoId: String, listener: ValueEventListener) {
        val userUid = prefs.getString("user_uid", "") ?: ""
        database.getReference("usuarios/$userUid/cursos/$cursoId/progreso")
            .removeEventListener(listener)
        Log.d(TAG, "Observador detenido")
    }

    suspend fun verificarTemaAprobado(cursoId: String, temaId: String): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

            val snapshot = ref.get().await()

            if (snapshot.exists()) {
                val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = snapshot.child("porcentajePromedio").getValue(Int::class.java) ?: 0

                Log.d(TAG, "Tema $temaId: aprobado=$aprobado, porcentaje=$porcentaje%")
                Result.success(aprobado)
            } else {
                Log.d(TAG, "Tema $temaId sin progreso")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando tema aprobado: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun validarAccesoQuizPractica(cursoId: String, temaId: String): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/$temaId")

            val snapshot = ref.get().await()

            if (snapshot.exists()) {
                val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false

                if (!aprobado) {
                    Log.d(TAG, "No puede acceder a practica: tema no aprobado")
                    return Result.failure(Exception("Debes aprobar el quiz oficial primero"))
                }

                Log.d(TAG, "Acceso a practica permitido")
                Result.success(true)
            } else {
                Log.d(TAG, "Tema sin progreso")
                Result.failure(Exception("Debes aprobar el quiz oficial primero"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validando acceso a practica: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * VALIDACION CRITICA: INICIAR QUIZ FINAL
     *
     * El Quiz Final solo puede iniciarse si:
     * 1. Todos los temas regulares estan aprobados (80% o mas)
     * 2. El usuario NO ha aprobado previamente el Quiz Final con 80% o mas
     *
     * Si ya aprobo el Quiz Final con 80% o mas, NO puede volver a hacerlo
     */
    suspend fun iniciarQuizFinal(cursoId: String): Result<IniciarQuizResponse> = withContext(NonCancellable) {
        return@withContext try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "Usuario no autenticado")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "======================================")
            Log.d(TAG, "VALIDANDO INICIO DE QUIZ FINAL")
            Log.d(TAG, "Curso: $cursoId")
            Log.d(TAG, "======================================")

            // VALIDACION 1: Verificar si ya aprobo el Quiz Final previamente
            val userUid = prefs.getString("user_uid", "") ?: ""
            val quizFinalRef = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/quiz_final")

            try {
                val quizFinalSnapshot = quizFinalRef.get().await()

                if (quizFinalSnapshot.exists()) {
                    val aprobadoAntes = quizFinalSnapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                    val porcentajeAntes = quizFinalSnapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                    Log.d(TAG, "Estado previo Quiz Final:")
                    Log.d(TAG, "  - Aprobado: $aprobadoAntes")
                    Log.d(TAG, "  - Porcentaje: $porcentajeAntes%")

                    // VALIDACION CRITICA: Si ya aprobo con 80% o mas, BLOQUEAR acceso
                    if (aprobadoAntes && porcentajeAntes >= PORCENTAJE_APROBACION) {
                        Log.e(TAG, "======================================")
                        Log.e(TAG, "ACCESO DENEGADO AL QUIZ FINAL")
                        Log.e(TAG, "Razon: Ya aprobado con $porcentajeAntes%")
                        Log.e(TAG, "======================================")
                        return@withContext Result.failure(
                            Exception("Ya has completado el Quiz Final exitosamente con $porcentajeAntes%. Solo puede realizarse una vez por curso.")
                        )
                    }

                    // Si saco menos de 80%, puede reintentar
                    if (porcentajeAntes < PORCENTAJE_APROBACION) {
                        Log.d(TAG, "Permiso concedido: Puede reintentar (obtuvo $porcentajeAntes%)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando estado previo del Quiz Final", e)
                // En caso de error, permitir continuar para que el backend valide
            }

            // VALIDACION 2: Token de autenticacion
            try {
                val token = currentUser.getIdToken(true).await()
                if (token.token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No se pudo obtener el token de autenticacion"))
                }
                Log.d(TAG, "Token valido para iniciar quiz final")
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo token", e)
                return@withContext Result.failure(Exception("Error de autenticacion: ${e.message}"))
            }

            // Llamar al endpoint del Quiz Final
            val request = mapOf("cursoId" to cursoId)
            Log.d(TAG, "Enviando request al endpoint del quiz final")

            val response = ApiClient.apiService.iniciarQuizFinal(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let {
                        Log.d(TAG, "======================================")
                        Log.d(TAG, "QUIZ FINAL INICIADO EXITOSAMENTE")
                        Log.d(TAG, "Quiz ID: ${it.quizId}")
                        Log.d(TAG, "Preguntas: ${it.preguntas.size}")
                        Log.d(TAG, "======================================")
                        Result.success(it)
                    } ?: Result.failure(Exception("Respuesta vacia del servidor"))
                }
                400 -> {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Validacion fallida: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                401 -> {
                    Log.e(TAG, "Token invalido")
                    Result.failure(Exception("Sesion expirada. Por favor, vuelve a iniciar sesion."))
                }
                403 -> {
                    val errorBody = response.errorBody()?.string() ?: "No tienes permiso"
                    Log.e(TAG, "Acceso denegado: $errorBody")
                    Result.failure(Exception(errorBody))
                }
                404 -> {
                    Log.e(TAG, "Recurso no encontrado en quiz final")
                    Result.failure(Exception("No se encontro el quiz final"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error ${response.code()}: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error al iniciar quiz final: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(Exception("Error de conexion. Verifica tu internet."))
        }
    }

    /**
     * VERIFICAR DISPONIBILIDAD DEL QUIZ FINAL
     *
     * El Quiz Final se habilita cuando:
     * - Todos los temas regulares estan aprobados con 80% o mas
     *
     * Estado de disponibilidad:
     * - disponible = true: Puede iniciar el Quiz Final
     * - disponible = false: Aun faltan temas por aprobar
     * - yaCompletado = true: Ya aprobo el Quiz Final con 80% o mas (solo informativo)
     */
    suspend fun verificarDisponibilidadQuizFinal(cursoId: String): Result<QuizFinalDisponibleResponse> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""

            Log.d(TAG, "======================================")
            Log.d(TAG, "VERIFICANDO DISPONIBILIDAD QUIZ FINAL")
            Log.d(TAG, "Curso: $cursoId")
            Log.d(TAG, "======================================")

            // Obtener total de temas del curso
            val cursosResponse = ApiClient.apiService.obtenerCursos()
            val curso = cursosResponse.body()?.find { it.id == cursoId }
            val totalTemas = curso?.temas?.size ?: 0

            if (totalTemas == 0) {
                Log.e(TAG, "No hay temas en este curso")
                return Result.failure(Exception("No hay temas en este curso"))
            }

            // Contar temas aprobados con 80% o mas
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados")
            val snapshot = ref.get().await()

            var temasAprobados = 0
            snapshot.children.forEach { temaSnap ->
                val temaId = temaSnap.key ?: ""

                // Ignorar el quiz_final en el conteo de temas
                if (temaId == "quiz_final") {
                    return@forEach
                }

                val aprobado = temaSnap.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = temaSnap.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                if (aprobado && porcentaje >= PORCENTAJE_APROBACION) {
                    temasAprobados++
                    Log.d(TAG, "Tema $temaId aprobado con $porcentaje%")
                }
            }

            val disponible = temasAprobados >= totalTemas

            // Verificar si ya completo el Quiz Final previamente
            val quizFinalSnapshot = snapshot.child("quiz_final")
            val yaCompletado = if (quizFinalSnapshot.exists()) {
                val aprobado = quizFinalSnapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = quizFinalSnapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0
                aprobado && porcentaje >= PORCENTAJE_APROBACION
            } else {
                false
            }

            val mensaje = when {
                yaCompletado -> "Ya completaste el Quiz Final exitosamente. Solo puede realizarse una vez."
                disponible -> "Quiz final desbloqueado. Demuestra todo lo aprendido"
                else -> "Completa los $totalTemas temas para desbloquear el quiz final. Llevas $temasAprobados/$totalTemas"
            }

            Log.d(TAG, "Resultado:")
            Log.d(TAG, "  - Disponible: $disponible")
            Log.d(TAG, "  - Temas aprobados: $temasAprobados/$totalTemas")
            Log.d(TAG, "  - Ya completado: $yaCompletado")
            Log.d(TAG, "======================================")

            Result.success(QuizFinalDisponibleResponse(
                disponible = disponible && !yaCompletado, // Solo disponible si no lo ha completado
                temasAprobados = temasAprobados,
                totalTemas = totalTemas,
                mensaje = mensaje
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando quiz final: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * VERIFICAR SI TODOS LOS TEMAS ESTAN APROBADOS
     * Utilizado para habilitar el boton del Quiz Final
     */
    suspend fun verificarTodosTemasAprobados(cursoId: String, totalTemas: Int): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados")

            val snapshot = ref.get().await()

            var temasAprobados = 0

            snapshot.children.forEach { temaSnap ->
                val temaId = temaSnap.key ?: ""

                // Ignorar quiz_final en el conteo
                if (temaId == "quiz_final") {
                    return@forEach
                }

                val aprobado = temaSnap.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = temaSnap.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                if (aprobado && porcentaje >= PORCENTAJE_APROBACION) {
                    temasAprobados++
                }
            }

            val todosAprobados = temasAprobados >= totalTemas

            Log.d(TAG, "Progreso temas: $temasAprobados/$totalTemas aprobados")
            Result.success(todosAprobados)
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando temas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * VERIFICAR SI EL QUIZ FINAL YA FUE COMPLETADO CON EXITO
     * Retorna true si el usuario ya aprobo el Quiz Final con 80% o mas
     * Esto se usa para bloquear el acceso si ya lo completo
     */
    suspend fun verificarQuizFinalCompletado(cursoId: String): Result<Boolean> {
        return try {
            val userUid = prefs.getString("user_uid", "") ?: ""
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/quiz_final")

            val snapshot = ref.get().await()

            if (snapshot.exists()) {
                val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                val porcentaje = snapshot.child("porcentajeObtenido").getValue(Int::class.java) ?: 0

                val yaCompletado = aprobado && porcentaje >= PORCENTAJE_APROBACION

                Log.d(TAG, "Quiz Final completado: $yaCompletado (porcentaje: $porcentaje%)")
                Result.success(yaCompletado)
            } else {
                Log.d(TAG, "Quiz Final nunca intentado")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando Quiz Final: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun procesarRespuestaIndividual(
        quizId: String,
        preguntaId: String,
        respuestaSeleccionada: Int,
        tiempoSeg: Int
    ): Result<ProcesarRespuestaResponse> = withContext(NonCancellable) {
        return@withContext try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "PROCESANDO RESPUESTA EN TIEMPO REAL")
            Log.d(TAG, "Quiz: $quizId")
            Log.d(TAG, "Pregunta: $preguntaId")
            Log.d(TAG, "Respuesta seleccionada: $respuestaSeleccionada")
            Log.d(TAG, "========================================")

            val request = ProcesarRespuestaRequest(
                quizId = quizId,
                preguntaId = preguntaId,
                respuestaSeleccionada = respuestaSeleccionada,
                tiempoSeg = tiempoSeg
            )

            val response = ApiClient.apiService.procesarRespuestaIndividual(request)

            when (response.code()) {
                200 -> {
                    response.body()?.let { resultado ->
                        Log.d(TAG, " Respuesta procesada exitosamente")
                        Log.d(TAG, "   Es correcta: ${resultado.esCorrecta}")
                        Log.d(TAG, "   Vidas restantes: ${resultado.vidasRestantes}")
                        Log.d(TAG, "   Quiz activo: ${resultado.quizActivo}")
                        Log.d(TAG, "   Preguntas respondidas: ${resultado.preguntasRespondidas}")
                        Log.d(TAG, "   Correctas: ${resultado.preguntasCorrectas}")

                        Result.success(resultado)
                    } ?: Result.failure(Exception("Respuesta vacía del servidor"))
                }

                400 -> {
                    // Parsear el error del backend
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }

                    Log.e(TAG, "========================================")
                    Log.e(TAG, " ERROR 400 AL PROCESAR RESPUESTA")
                    Log.e(TAG, "Error body: $errorBody")
                    Log.e(TAG, "========================================")

                    // Detectar si es error de vidas agotadas
                    if (errorBody?.contains("QUIZ_ABANDONADO_POR_VIDAS", ignoreCase = true) == true ||
                        errorBody?.contains("sin vidas", ignoreCase = true) == true ||
                        errorBody?.contains("quedado sin vidas", ignoreCase = true) == true) {

                        Log.e(TAG, " TIPO DE ERROR: QUIZ ABANDONADO POR VIDAS")

                        // Lanzar excepción custom
                        Result.failure(
                            QuizAbandonadoPorVidasException(
                                errorBody ?: "Te has quedado sin vidas. El quiz se ha detenido."
                            )
                        )
                    } else {
                        Result.failure(Exception(errorBody ?: "Error al procesar respuesta"))
                    }
                }

                401 -> {
                    Log.e(TAG, "Token inválido")
                    Result.failure(Exception("Sesión expirada. Por favor, vuelve a iniciar sesión."))
                }

                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error ${response.code()}: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error al procesar respuesta: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception al procesar respuesta: ${e.message}", e)
            Result.failure(Exception("Error de conexión. Verifica tu internet."))
        }
    }
}

data class ProgresoCurso(
    val experiencia: Int = 0,
    val rachaDias: Int = 0,
    val vidas: Int = 5
)