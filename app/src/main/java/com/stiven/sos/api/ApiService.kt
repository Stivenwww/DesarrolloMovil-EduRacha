

package com.stiven.sos.api

import com.stiven.sos.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define todos los endpoints de la API de EduRacha usando Retrofit.
 */
interface ApiService {

    // ============================================
    // ENDPOINT DE REGISTRO - NUEVO
    // ============================================

    /**
     * Registra un nuevo usuario (estudiante o docente) en el sistema.
     */
    @POST("registro")
    suspend fun registrarUsuario(
        @Body registro: RegistroRequest
    ): Response<Map<String, String>>

    @PUT("api/usuarios/{uid}")

    suspend fun actualizarPerfil(
        @Path("uid") uid: String,
        @Body perfilUpdate: ActualizarPerfilRequest
    ): Response<Map<String, String>>


// ============================================
    // ENDPOINTS DE CURSOS
    // ============================================


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


    /**
     * Obtener estudiantes asignados a un curso específico.
     * Endpoint: GET /api/solicitudes/curso/{id}/estudiantes
     */


    @GET("api/solicitudes/curso/{id}/estudiantes")
    suspend fun obtenerEstudiantesPorCurso(
        @Path("id") cursoId: String
    ): Response<List<UsuarioAsignado>>

    @POST("api/solicitudes/curso/{cursoId}/estudiante/{estudianteId}/estado")
    suspend fun cambiarEstadoEstudiante(
        @Path("cursoId") cursoId: String,
        @Path("estudianteId") estudianteId: String,
        @Body estado: Map<String, String>
    ): Response<Map<String, String>>


    // ============================================
    // ENDPOINTS DE PREGUNTAS - CRUD
    // ============================================

    @GET("api/preguntas")
    suspend fun obtenerPreguntas(
        @Query("cursoId") cursoId: String? = null,
        @Query("estado") estado: String? = null
    ): Response<List<Pregunta>>

    @GET("api/preguntas/{id}")
    suspend fun obtenerPreguntaPorId(
        @Path("id") id: String
    ): Response<Pregunta>

    @POST("api/preguntas")
    suspend fun crearPregunta(
        @Body pregunta: Pregunta
    ): Response<ApiResponse>

    @PUT("api/preguntas/{id}")
    suspend fun actualizarPregunta(
        @Path("id") id: String,
        @Body pregunta: Pregunta
    ): Response<ApiResponse>

    @DELETE("api/preguntas/{id}")
    suspend fun eliminarPregunta(
        @Path("id") id: String
    ): Response<ApiResponse>

    @PUT("api/preguntas/{id}/estado")
    suspend fun actualizarEstadoPregunta(
        @Path("id") id: String,
        @Body request: ActualizarEstadoRequest
    ): Response<EstadoUpdateResponse>

    @DELETE("api/preguntas/cache")
    suspend fun limpiarCachePreguntas(): Response<ApiResponse>

    // ============================================
    // ENDPOINTS DE IA - GENERACIÓN DE PREGUNTAS
    // ============================================

    @POST("api/preguntas/ia/generar")
    suspend fun generarPreguntasIA(
        @Body request: GenerarPreguntasRequest
    ): Response<PreguntasIAResponse>

// ============================================
// ENDPOINTS DE SOLICITUDES
// ============================================

    /**
     * Crear una solicitud para unirse a un curso
     */
    @POST("api/solicitudes/unirse")
    suspend fun crearSolicitudCurso(
        @Body solicitud: SolicitudRequest
    ): Response<ApiResponse>

    /**
     * Obtener solicitudes de un estudiante específico
     */
    @GET("api/solicitudes/estudiante/{estudianteId}")
    suspend fun obtenerSolicitudesEstudiante(
        @Path("estudianteId") estudianteId: String
    ): Response<List<SolicitudCurso>>

    /**
     * Obtener solicitudes pendientes para un docente
     */
    @GET("api/solicitudes/docente/{docenteId}")
    suspend fun obtenerSolicitudesDocente(
        @Path("docenteId") docenteId: String
    ): Response<List<SolicitudCurso>>

    @GET("api/solicitudes/curso/{cursoId}")
    suspend fun obtenerSolicitudesPorCurso(
        @Path("cursoId") cursoId: String
    ): Response<List<SolicitudCurso>>

    /**
     * Responder a una solicitud (aceptar o rechazar)
     */
    @PUT("api/solicitudes/{id}/responder")
    suspend fun responderSolicitud(
        @Path("id") solicitudId: String,
        @Body respuesta: RespuestaSolicitudRequest
    ): Response<ApiResponse>
}
