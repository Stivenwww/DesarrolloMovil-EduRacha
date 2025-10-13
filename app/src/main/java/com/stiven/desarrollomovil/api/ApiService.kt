// Archivo: app/src/main/java/com/stiven/desarrollomovil/api/ApiService.kt

package com.stiven.desarrollomovil.api

import com.stiven.desarrollomovil.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define todos los endpoints de la API de EduRacha usando Retrofit.
 */
interface ApiService {

    // ============================================
    // ENDPOINTS DE CURSOS
    // ============================================

    // Dentro de tu interfaz ApiService.kt// Después (La forma correcta)
    @POST("api/cursos")
    suspend fun crearCurso(@Body curso: CursoRequest): Response<ApiResponse>

    @GET("api/cursos")
    suspend fun obtenerCursos(): Response<List<Curso>>

    @GET("api/cursos/{id}")
    suspend fun obtenerCursoPorId(@Path("id") id: String): Response<Curso>

    @PUT("api/cursos/{id}")
    suspend fun actualizarCurso(
        @Path("id") id: String,
        @Body curso: Curso
    ): Response<ApiResponse>

    @DELETE("api/cursos/{id}")
    suspend fun eliminarCurso(@Path("id") id: String): Response<ApiResponse>


    // =================================================================
    // --- ¡AQUÍ ESTÁ LA SECCIÓN QUE SOLUCIONA LOS ERRORES! ---
    // ENDPOINTS DE PREGUNTAS (IA)
    // =================================================================

    /**
     * Solicita a la IA que genere un conjunto de preguntas para un tema específico.
     */
    @POST("api/ia/preguntas/generar")
    suspend fun generarPreguntasIA(
        @Body request: GenerarPreguntasRequest
    ): Response<PreguntasIAResponse>

    /**
     * Obtiene solo las preguntas pendientes de validación para un tema de un curso.
     */
    @GET("api/ia/preguntas/pendientes/{cursoId}/{temaId}")
    suspend fun obtenerPreguntasPendientes(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<List<PreguntaIA>>

    /**
     * Endpoint unificado para revisar una pregunta.
     * Puede ser usada para aprobar, rechazar o actualizar (editar) una pregunta.
     */
    @PUT("api/ia/preguntas/revisar/{id}")
    suspend fun revisarPregunta(
        @Path("id") id: String,
        @Body request: RevisarPreguntaRequest
    ): Response<ApiResponse>

    /**
     * Elimina una pregunta de la base de datos.
     */
    @DELETE("api/ia/preguntas/{id}")
    suspend fun eliminarPreguntaIA(
        @Path("id") id: String
    ): Response<ApiResponse>

    /**
     * Obtiene TODAS las preguntas pendientes de todos los cursos.
     * Útil para el contador en el Panel del Docente.
     */
    @GET("api/ia/preguntas/pendientes/todas")
    suspend fun obtenerTodasLasPreguntasPendientes(): Response<List<PreguntaIA>>
}
