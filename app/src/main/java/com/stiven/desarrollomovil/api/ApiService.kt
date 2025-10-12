package com.stiven.desarrollomovil.api

import com.stiven.desarrollomovil.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== CURSOS ========== (YA EXISTENTES)

    @POST("api/cursos")
    suspend fun crearCurso(@Body curso: Curso): Response<ApiResponse>

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


    // ========== PREGUNTAS IA ========== (NUEVOS)

    // Generar preguntas con IA
    @POST("api/ia/preguntas/generar")
    suspend fun generarPreguntasIA(
        @Body request: GenerarPreguntasRequest
    ): Response<PreguntasIAResponse>

    // Obtener preguntas pendientes
    @GET("api/ia/preguntas/pendientes/{cursoId}/{temaId}")
    suspend fun obtenerPreguntasPendientes(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<List<PreguntaIA>>

    // Revisar pregunta (aprobar/rechazar)
    @PUT("api/ia/preguntas/revisar/{id}")
    suspend fun revisarPregunta(
        @Path("id") id: String,
        @Body request: RevisarPreguntaRequest
    ): Response<ApiResponse>

    // Eliminar pregunta
    @DELETE("api/ia/preguntas/{id}")
    suspend fun eliminarPreguntaIA(
        @Path("id") id: String
    ): Response<ApiResponse>

    // Actualizar pregunta (para editar)
    @PUT("api/preguntas/{id}")
    suspend fun actualizarPregunta(
        @Path("id") id: String,
        @Body pregunta: PreguntaIA
    ): Response<ApiResponse>
}

