package com.stiven.desarrollomovil.api

import com.stiven.desarrollomovil.models.ApiResponse
import com.stiven.desarrollomovil.models.Curso
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== CURSOS ==========

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
}