package com.stiven.sos.api

import com.stiven.sos.models.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody
import retrofit2.http.Streaming
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
        @Body curso: CursoRequest
    ): Response<ApiResponse>
    @DELETE("api/cursos/{id}")
    suspend fun eliminarCurso(@Path("id") id: String): Response<ApiResponse>


    @GET("api/cursos/{id}/estudiantes")
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
    // ENDPOINTS DE QUIZ
    // ============================================

    /**
     * Iniciar un nuevo quiz
     */
    @POST("quiz/iniciar")
    suspend fun iniciarQuiz(
        @Body request: IniciarQuizRequest
    ): Response<IniciarQuizResponse>

    /**
     * Finalizar un quiz y obtener resultados
     */
    @POST("quiz/finalizar")
    suspend fun finalizarQuiz(
        @Body request: FinalizarQuizRequest
    ): Response<FinalizarQuizResponse>

    /**
     * Obtener revisión del quiz
     */
    @GET("quiz/{quizId}/revision")
    suspend fun obtenerRevisionQuiz(
        @Path("quizId") quizId: String
    ): Response<RevisionQuizResponse>

    /**
     * Marcar explicación como vista
     */
    @POST("quiz/explicacion-vista")
    suspend fun marcarExplicacionVista(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    /**
     * Obtener retroalimentación de errores
     */
    @GET("quiz/{quizId}/retroalimentacion")
    suspend fun obtenerRetroalimentacion(
        @Path("quizId") quizId: String
    ): Response<RetroalimentacionFallosResponse>

    /**
     * Obtener vidas actuales con regeneración automática
     */
    @GET("quiz/vidas/{cursoId}")
    suspend fun obtenerVidas(
        @Path("cursoId") cursoId: String
    ): Response<Map<String, Any>>

    //  ENDPOINTS PARA MODOS DE QUIZ

    /**
     * Verificar modos disponibles para un tema
     */
    @GET("api/quiz/modos-disponibles")
    suspend fun verificarModosDisponibles(
        @Query("cursoId") cursoId: String,
        @Query("temaId") temaId: String
    ): Response<ModoQuizDisponibleResponse>

    /**
     * Verificar disponibilidad del quiz final
     */
    @GET("api/quiz/final/disponible")
    suspend fun verificarQuizFinalDisponible(
        @Query("cursoId") cursoId: String
    ): Response<QuizFinalDisponibleResponse>

    /**
     * Iniciar quiz final
     */
    @POST("api/quiz/final/iniciar")
    suspend fun iniciarQuizFinal(
        @Body request: Map<String, String>
    ): Response<IniciarQuizResponse>

    // ============================================
    // ENDPOINTS DE PROGRESO Y RACHA
    // ============================================

    /**
     * Obtener racha del estudiante autenticado
     */
    @GET("api/solicitudes/curso/{cursoId}/racha")
    suspend fun obtenerRacha(
        @Path("cursoId") cursoId: String
    ): Response<RachaResponse>

    /**
     * Obtener experiencia del estudiante autenticado
     */
    @GET("api/solicitudes/curso/{cursoId}/experiencia")
    suspend fun obtenerExperiencia(
        @Path("cursoId") cursoId: String
    ): Response<Map<String, Any>>

    /**
     * Regenerar vidas
     */
    @POST("api/solicitudes/curso/{cursoId}/regenerar-vidas")
    suspend fun regenerarVidas(
        @Path("cursoId") cursoId: String
    ): Response<Map<String, Any>>
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

    //  AGREGAR ESTE ENDPOINT
    @PUT("api/cursos/{cursoId}/temas/{temaId}/validar-explicacion")
    suspend fun actualizarEstadoExplicacion(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String,
        @Body estado: Map<String, String>
    ): Response<Map<String, Any>>


    // ============================================
    // ENDPOINTS DE RANKING (COMPLETOS)
    // ============================================

    /**
     * Obtener ranking por experiencia
     */
    @GET("api/cursos/ranking/{cursoId}")
    suspend fun obtenerRankingPorExperiencia(
        @Path("cursoId") cursoId: String
    ): Response<List<RankingEstudianteAPI>>

    /**
     * Obtener ranking por racha
     */
    @GET("api/cursos/ranking/{cursoId}/racha")
    suspend fun obtenerRankingPorRacha(
        @Path("cursoId") cursoId: String
    ): Response<List<RankingEstudianteAPI>>

    /**
     * Obtener ranking por vidas
     */
    @GET("api/cursos/ranking/{cursoId}/vidas")
    suspend fun obtenerRankingPorVidas(
        @Path("cursoId") cursoId: String
    ): Response<List<RankingEstudianteAPI>>

    /**
     * Ranking general (todos los cursos)
     */
    @GET("api/cursos/ranking/general")
    suspend fun obtenerRankingGeneral(): Response<List<RankingEstudianteAPI>>

    @GET("api/cursos/ranking/general/{filtro}")
    suspend fun obtenerRankingGeneralConFiltro(
        @Path("filtro") filtro: String
    ): Response<List<RankingEstudianteAPI>>

// ============================================
    // ENDPOINTS DE REPORTES EXCEL - VISTA PREVIA (JSON)
    // ============================================

    @GET("reportes/debug/cursos/{cursoId}/diario/fecha/{fecha}")
    suspend fun obtenerVistaPreviewReporteDiario(
        @Path("cursoId") cursoId: String,
        @Path("fecha") fecha: String
    ): Response<DatosReporteDiarioResponse>

    @GET("reportes/debug/cursos/{cursoId}/tema/{temaId}")
    suspend fun obtenerVistaPreviewReporteTema(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<DatosReporteTemaResponse>

    @GET("reportes/debug/cursos/{cursoId}/general")
    suspend fun obtenerVistaPreviewReporteGeneral(
        @Path("cursoId") cursoId: String
    ): Response<DatosReporteGeneralResponse>

    @GET("reportes/debug/cursos/{cursoId}/rango")
    suspend fun obtenerVistaPreviewReporteRango(
        @Path("cursoId") cursoId: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String
    ): Response<DatosReporteRangoResponse>

    // ============================================
    // ENDPOINTS DE REPORTES EXCEL - DESCARGA
    // ============================================

    @GET("reportes/cursos/{cursoId}/diario/fecha/{fecha}")
    @Streaming
    suspend fun descargarReporteDiario(
        @Path("cursoId") cursoId: String,
        @Path("fecha") fecha: String
    ): Response<ResponseBody>

    @GET("reportes/cursos/{cursoId}/tema/{temaId}")
    @Streaming
    suspend fun descargarReporteTema(
        @Path("cursoId") cursoId: String,
        @Path("temaId") temaId: String
    ): Response<ResponseBody>

    @GET("reportes/cursos/{cursoId}/general")
    @Streaming
    suspend fun descargarReporteGeneral(
        @Path("cursoId") cursoId: String
    ): Response<ResponseBody>

    @GET("reportes/cursos/{cursoId}/rango")
    @Streaming
    suspend fun descargarReporteRango(
        @Path("cursoId") cursoId: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String
    ): Response<ResponseBody>
}