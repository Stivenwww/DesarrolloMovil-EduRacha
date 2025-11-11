package com.stiven.sos.api

import com.stiven.sos.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz que define todos los endpoints de la API de EduRacha usando Retrofit.
 */
interface ApiService {

    // ============================================
    // ENDPOINT DE REGISTRO
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


    @GET("api/usuarios/{uid}")
    suspend fun obtenerUsuarioPorUid(
        @Path("uid") uid: String
    ): Response<Map<String, Any>>

    @PUT("usuario/me")
    suspend fun actualizarUsuario(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>


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

    /**
     * Obtener solicitudes por curso
     */
    @GET("api/solicitudes/curso/{cursoId}")
    suspend fun obtenerSolicitudesPorCurso(
        @Path("cursoId") cursoId: String
    ): Response<List<SolicitudCurso>>

    /**
     *  Responder a una solicitud (aceptar o rechazar)
     */
    @POST("api/solicitudes/responder/{solicitudId}")
    suspend fun responderSolicitud(
        @Path("solicitudId") solicitudId: String,
        @Body request: RespuestaSolicitudRequest
    ): Response<Unit>


    // ============================================
    // QUIZ - CON AUTENTICACIÓN
    // ============================================

    @POST("quiz/explicacion/marcar-vista")
    suspend fun marcarExplicacionVista(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @POST("quiz/iniciar")
    suspend fun iniciarQuiz(
        @Body request: IniciarQuizRequest
    ): Response<IniciarQuizResponse>

    @POST("quiz/finalizar")
    suspend fun finalizarQuiz(
        @Body request: FinalizarQuizRequest
    ): Response<FinalizarQuizResponse>

    @GET("quiz/revision/{quizId}")
    suspend fun obtenerRevisionQuiz(
        @Path("quizId") quizId: String
    ): Response<RevisionQuizResponse>

    @GET("quiz/historial")
    suspend fun obtenerHistorialQuizzes(
        @Query("cursoId") cursoId: String? = null
    ): Response<HistorialQuizzesResponse>

    @GET("quiz/curso/{cursoId}/vidas")
    suspend fun obtenerVidas(
        @Path("cursoId") cursoId: String
    ): Response<VidasResponse>

    @GET("quiz/curso/{cursoId}/tema/{temaId}/info")
    suspend fun obtenerTemaInfo(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<TemaInfoResponse>

    @GET("quiz/{quizId}/retroalimentacion")
    suspend fun obtenerRetroalimentacion(
        @Path("quizId") quizId: String
    ): Response<RetroalimentacionFallosResponse>


    // ============================================
// ENDPOINTS DE TEMAS
// ============================================

    @GET("api/cursos/{id}/temas")
    suspend fun obtenerTemasPorCurso(
        @Path("id") cursoId: String,
        @Query("estado") estado: String? = null
    ): Response<List<Tema>>

    @GET("api/cursos/{id}/temas/{temaId}")
    suspend fun obtenerTemaPorId(
        @Path("id") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<Tema>

    /**
     * Obtener ranking completo de un curso con toda la información de Firebase
     * Endpoint: GET /api/solicitudes/curso/{cursoId}/ranking
     */
    @GET("api/cursos/{cursoId}/progreso/{estudianteId}")
    suspend fun obtenerRacha(
        @Path("cursoId") cursoId: String,
        @Path("estudianteId") estudianteId: String
    ): Response<Map<String, Any>>

    // ============================================
// ENDPOINTS DE EXPLICACIONES CON IA
// ============================================

    @POST("api/cursos/{cursoId}/temas/{temaId}/generar-explicacion")
    suspend fun generarExplicacionIA(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String,
        @Body request: GenerarExplicacionRequest
    ): Response<Map<String, Any>>

    // ✅ AGREGAR ESTE ENDPOINT
    @PUT("api/cursos/{cursoId}/temas/{temaId}/validar-explicacion")
    suspend fun actualizarEstadoExplicacion(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String,
        @Body estado: Map<String, String>
    ): Response<Map<String, Any>>


}