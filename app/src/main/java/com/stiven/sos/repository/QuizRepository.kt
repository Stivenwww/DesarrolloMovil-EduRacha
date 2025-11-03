package com.stiven.sos.repository

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.*
import kotlinx.coroutines.tasks.await

class QuizRepository(private val application: Application) {

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

            val request = IniciarQuizRequest(
                cursoId = cursoId,
                temaId = temaId
            )

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

            val request = FinalizarQuizRequest(
                quizId = quizId,
                respuestas = respuestas
            )

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

            // Obtener todos los cursos
            val responseCursos = ApiClient.apiService.obtenerCursos()

            if (!responseCursos.isSuccessful) {
                return Result.failure(Exception("Error al obtener cursos: ${responseCursos.code()}"))
            }

            val todosCursos = responseCursos.body() ?: emptyList()

            // Filtrar solo los cursos donde el usuario está inscrito
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
                    // Continuar con el siguiente curso
                    continue
                }
            }

            Result.success(cursosInscritos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}