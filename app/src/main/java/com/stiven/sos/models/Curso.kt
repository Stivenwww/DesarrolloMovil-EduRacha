package com.stiven.sos.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Programación pedagógica de un tema
 */
@Parcelize
@Serializable
data class ProgramacionTema(
    val objetivos: String? = null,
    val conceptosClave: String? = null,
    val ejemplos: String? = null,
    val pseudocodigo: String? = null,
    val codigo: String? = null
) : Parcelable

/**
 * Programación temporal del curso
 */
@Parcelize
@Serializable
data class ProgramacionCurso(
    val temasOrdenados: List<String> = emptyList(),
    val distribucionTemporal: Map<String, RangoTema> = emptyMap()
) : Parcelable

/**
 * Rango temporal de un tema en el curso
 */
@Parcelize
@Serializable
data class RangoTema(
    val temaId: String = "",
    val titulo: String = "",
    val fechaInicio: Long = 0,
    val fechaFin: Long = 0,
    val quizzesRequeridos: Int = 0,
    val diasAsignados: Int = 0
) : Parcelable

/**
 * Modelo de request para crear/actualizar Tema (coincide con backend)
 */
@Parcelize
@Serializable
data class TemaRequest(
    val id: String? = null,
    val titulo: String,
    val contenido: String,
    val archivoUrl: String? = null,
    val tipo: String,
    val fechaCreacion: String,
    val explicacion: String? = null,
    val explicacionFuente: String? = null,
    val explicacionUltimaActualizacion: String? = null,
    val explicacionEstado: String? = null,
    val programacion: ProgramacionTema? = null
) : Parcelable

/**
 * Modelo Tema completo que viene desde el backend
 */
@Parcelize
@Serializable
data class Tema(
    val id: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val archivoUrl: String? = null,
    val tipo: String = "",
    val fechaCreacion: String = "",
    val descripcion: String = "",
    val orden: Int = 0,
    val cursoId: String = "",
    val explicacion: String? = null,
    val explicacionFuente: String? = null,
    val explicacionUltimaActualizacion: String? = null,
    val explicacionEstado: String? = null,
    val programacion: ProgramacionTema? = null
) : Parcelable

/**
 * Modelo de Curso desde backend
 */
@Parcelize
@Serializable
data class Curso(
    val id: String? = null,
    val titulo: String = "",
    val codigo: String = "",
    val descripcion: String = "",
    val docenteId: String = "",
    val duracionDias: Int = 0,
    val fechaInicio: Long = 0,
    val fechaFin: Long = 0,
    val temas: Map<String, Tema>? = null,
    val programacion: ProgramacionCurso? = null,
    val estado: String = "",
    val fechaCreacion: String = "",
    val nombre: String = titulo
) : Parcelable {
    fun getTemasLista(): List<Tema> {
        return temas?.values?.toList()?.sortedBy { it.orden } ?: emptyList()
    }
}

/**
 * Modelo Request para Curso
 */
@Serializable
data class CursoRequest(
    val titulo: String,
    val codigo: String,
    val descripcion: String,
    val docenteId: String,
    val duracionDias: Int,
    val fechaInicio: Long,
    val fechaFin: Long,
    val temas: Map<String, TemaRequest>? = null,
    val programacion: ProgramacionCurso? = null,
    val estado: String,
    val fechaCreacion: String
)

/**
 * Request para generación de explicación con IA
 */
@Serializable
data class GenerarExplicacionRequest(
    val cursoId: String,
    val temaId: String,
    val tituloTema: String,
    val contenidoTema: String? = null
)

/**
 * Convertir Curso a CursoRequest para guardar en backend
 */
fun Curso.toRequest(): CursoRequest {
    return CursoRequest(
        titulo = this.titulo,
        codigo = this.codigo,
        descripcion = this.descripcion,
        docenteId = this.docenteId,
        duracionDias = this.duracionDias,
        fechaInicio = this.fechaInicio,
        fechaFin = this.fechaFin,
        temas = this.temas?.mapValues { (key, tema) ->
            TemaRequest(
                id = tema.id ?: key,
                titulo = tema.titulo,
                contenido = tema.contenido,
                archivoUrl = tema.archivoUrl,
                tipo = tema.tipo,
                fechaCreacion = tema.fechaCreacion,
                explicacion = tema.explicacion,
                explicacionFuente = tema.explicacionFuente,
                explicacionUltimaActualizacion = tema.explicacionUltimaActualizacion,
                explicacionEstado = tema.explicacionEstado,
                programacion = tema.programacion
            )
        },
        programacion = this.programacion,
        estado = this.estado,
        fechaCreacion = this.fechaCreacion
    )
}