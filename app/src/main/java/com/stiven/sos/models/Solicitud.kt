package com.stiven.sos.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para solicitud de curso
 */
data class SolicitudCurso(
    val id: String? = null,
    val cursoId: String = "",
    val codigoCurso: String = "",
    val estudianteId: String = "",
    val estudianteNombre: String = "",
    val estudianteEmail: String = "",
    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    val fechaSolicitud: String = "",
    val fechaRespuesta: String? = null,
    val mensaje: String? = null
)

/**
 * Estados posibles de una solicitud
 */
enum class EstadoSolicitud {
    @SerializedName("PENDIENTE")
    PENDIENTE,

    @SerializedName("ACEPTADA")
    ACEPTADA,

    @SerializedName("RECHAZADA")
    RECHAZADA
}

/**
 * Request para crear una solicitud
 */
data class SolicitudRequest(
    val codigoCurso: String,
    val estudianteId: String,
    val estudianteNombre: String,
    val estudianteEmail: String,
    val mensaje: String? = null
)

/**
 * Request para responder una solicitud (usado por el docente)
 */
data class RespuestaSolicitudRequest(
    val aceptar: Boolean,
    val mensaje: String? = null
)

/**
 * Curso inscrito por el estudiante
 */
data class CursoInscrito(
    val cursoId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val codigo: String = "",
    val docenteNombre: String = "",
    val duracionDias: Int = 0,
    val progreso: Int = 0,
    val diasRacha: Int = 0,
    val puntosTotales: Int = 0
)