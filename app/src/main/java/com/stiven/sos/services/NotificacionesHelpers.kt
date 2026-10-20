package com.stiven.sos.services



import com.stiven.sos.services.NotificacionModel
import com.stiven.sos.services.ServicioNotificaciones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper para enviar notificaciones automáticas sin necesidad de llamadas explícitas
 * Las notificaciones se envían automáticamente cuando ocurren ciertos eventos
 */
object NotificacionesHelper {

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Notificación automática cuando un estudiante inicia un quiz
     */
    fun notificarQuizIniciado(
        estudianteId: String,
        cursoId: String,
        temaId: String,
        tituloTema: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "Quiz iniciado",
                    mensaje = "Has iniciado el quiz del tema: $tituloTema",
                    tipo = "quiz_iniciado",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    temaId = temaId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de quiz iniciado: ${e.message}")
            }
        }
    }

    /**
     * Notificación cuando un estudiante completa un quiz
     * Ya está implementado en el backend (ServicioProgreso.enviarNotificacionQuizCompletado)
     */
    fun notificarQuizCompletado(
        estudianteId: String,
        cursoId: String,
        temaId: String,
        xpGanado: Int,
        racha: Int,
        aprobado: Boolean
    ) {
        scope.launch {
            try {
                val mensaje = when {
                    racha > 1 && aprobado -> "¡Quiz completado! Ganaste $xpGanado XP. Racha de $racha días"
                    aprobado -> "¡Quiz completado! Ganaste $xpGanado XP. ¡Sigue así!"
                    else -> "Quiz completado. Intenta nuevamente para mejorar tu puntuación."
                }

                val notificacion = NotificacionModel(
                    id = "",
                    titulo = if (aprobado) "¡Quiz completado!" else "Quiz finalizado",
                    mensaje = mensaje,
                    tipo = "quiz_completado",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    temaId = temaId,
                    xpGanado = xpGanado,
                    racha = racha
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de quiz completado: ${e.message}")
            }
        }
    }

    /**
     * Notificación cuando un estudiante se inscribe a un curso
     * Ya está implementado en el backend (ServicioProgreso.enviarNotificacionBienvenida)
     */
    fun notificarInscripcion(
        estudianteId: String,
        nombreEstudiante: String,
        cursoId: String,
        tituloCurso: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "¡Bienvenido al curso!",
                    mensaje = "Hola $nombreEstudiante, estás inscrito en el curso '$tituloCurso'. ¡Empieza a aprender!",
                    tipo = "inscripcion",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de inscripción: ${e.message}")
            }
        }
    }

    /**
     * Notificación cuando hay un nuevo quiz disponible
     */
    fun notificarQuizDisponible(
        estudianteId: String,
        cursoId: String,
        temaId: String,
        tituloTema: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "Nuevo quiz disponible",
                    mensaje = "Hay un nuevo quiz disponible para el tema: $tituloTema",
                    tipo = "quiz_disponible",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    temaId = temaId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de quiz disponible: ${e.message}")
            }
        }
    }

    /**
     * Notificación de recordatorio (vidas recuperadas, racha en riesgo, etc.)
     */
    fun notificarRecordatorio(
        estudianteId: String,
        cursoId: String,
        titulo: String,
        mensaje: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = titulo,
                    mensaje = mensaje,
                    tipo = "recordatorio",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de recordatorio: ${e.message}")
            }
        }
    }

    /**
     * Notificación de logro (primera vez que completa un tema, racha récord, etc.)
     */
    fun notificarLogro(
        estudianteId: String,
        cursoId: String,
        temaId: String?,
        titulo: String,
        mensaje: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = titulo,
                    mensaje = mensaje,
                    tipo = "logro",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    temaId = temaId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de logro: ${e.message}")
            }
        }
    }

    /**
     * Notificación para el docente sobre reportes generados
     */
    fun notificarReporteGenerado(
        docenteId: String,
        cursoId: String,
        rutaArchivo: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "Reporte diario generado",
                    mensaje = "El reporte diario del curso está listo para descargar",
                    tipo = "reporte",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    rutaArchivo = rutaArchivo
                )

                ServicioNotificaciones.enviarNotificacion(docenteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de reporte: ${e.message}")
            }
        }
    }

    /**
     * Notificación para el docente sobre solicitudes de estudiantes
     */
    fun notificarSolicitudEstudiante(
        docenteId: String,
        cursoId: String,
        estudianteId: String,
        nombreEstudiante: String,
        tipoSolicitud: String
    ) {
        scope.launch {
            try {
                val mensaje = when (tipoSolicitud) {
                    "mas_preguntas" -> "$nombreEstudiante necesita más preguntas para continuar"
                    "inscripcion" -> "$nombreEstudiante solicita inscribirse al curso"
                    else -> "$nombreEstudiante ha enviado una solicitud"
                }

                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "Nueva solicitud",
                    mensaje = mensaje,
                    tipo = "solicitud",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    estudianteId = estudianteId
                )

                ServicioNotificaciones.enviarNotificacion(docenteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de solicitud: ${e.message}")
            }
        }
    }

    /**
     * Notificación cuando un tema es desbloqueado
     */
    fun notificarTemaDesbloqueado(
        estudianteId: String,
        cursoId: String,
        temaId: String,
        tituloTema: String
    ) {
        scope.launch {
            try {
                val notificacion = NotificacionModel(
                    id = "",
                    titulo = "¡Nuevo tema desbloqueado!",
                    mensaje = "Has desbloqueado el tema: $tituloTema. ¡Comienza a aprender!",
                    tipo = "tema_desbloqueado",
                    fecha = System.currentTimeMillis(),
                    leido = false,
                    cursoId = cursoId,
                    temaId = temaId
                )

                ServicioNotificaciones.enviarNotificacion(estudianteId, notificacion)
            } catch (e: Exception) {
                println("Error enviando notificación de tema desbloqueado: ${e.message}")
            }
        }
    }
}
