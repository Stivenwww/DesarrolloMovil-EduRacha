package com.stiven.sos.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Modelo de request para crear/actualizar Tema (coincide con backend)
 */
@Parcelize
@Serializable
data class TemaRequest(
    val titulo: String,
    val contenido: String,
    val archivoUrl: String? = null,
    val tipo: String,
    val fechaCreacion: String,

    val explicacion: String? = null,
    val explicacionFuente: String? = null,
    val explicacionUltimaActualizacion: String? = null,
    val explicacionEstado: String? = null
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
    val explicacionEstado: String? = null
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
    val temas: Map<String, Tema>? = null,
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
    val temas: Map<String, TemaRequest>? = null,
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
        temas = this.temas?.mapValues { (_, tema) ->
            TemaRequest(
                titulo = tema.titulo,
                contenido = tema.contenido,
                archivoUrl = tema.archivoUrl,
                tipo = tema.tipo,
                fechaCreacion = tema.fechaCreacion,
                explicacion = tema.explicacion,
                explicacionFuente = tema.explicacionFuente,
                explicacionUltimaActualizacion = tema.explicacionUltimaActualizacion,
                explicacionEstado = tema.explicacionEstado
            )
        },
        estado = this.estado,
        fechaCreacion = this.fechaCreacion
    )
}
