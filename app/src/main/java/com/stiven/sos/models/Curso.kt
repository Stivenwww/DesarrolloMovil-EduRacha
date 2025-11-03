// Archivo: app/src/main/java/com/stiven/desarrollomovil/models/Curso.kt

package com.stiven.sos.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ========================================
// MODELO TEMA (ACTUALIZADO CON EXPLICACIÓN)
// ========================================
@Parcelize
data class Tema(
    val id: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val archivoUrl: String = "",
    val tipo: String = "",
    val fechaCreacion: String = "",
    // NUEVAS PROPIEDADES PARA QUIZZES
    val descripcion: String = "", // Descripción corta del tema
    val explicacion: String = "", // Explicación completa para mostrar antes del quiz
    val orden: Int = 0, // Orden de visualización
    val cursoId: String = "" // ID del curso al que pertenece
) : Parcelable

// ========================================
// MODELO CURSO (Para recibir datos - GET)
// ========================================
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
    // NUEVAS PROPIEDADES OPCIONALES (no rompen código existente)
    val nombre: String = titulo // Alias para compatibilidad con código nuevo
) {
    // Función helper para obtener temas como lista ordenada
    fun getTemasLista(): List<Tema> {
        return temas?.values?.toList()?.sortedBy { it.orden } ?: emptyList()
    }
}

// ========================================
// MODELO CURSO REQUEST (Para enviar datos - POST)
// ========================================
data class CursoRequest(
    val titulo: String,
    val codigo: String,
    val descripcion: String,
    val docenteId: String,
    val duracionDias: Int,
    val temas: Map<String, Tema>? = null,
    val estado: String,
    val fechaCreacion: String
)


// Función de extensión para convertir Curso a CursoRequest
fun Curso.toRequest(): CursoRequest {
    return CursoRequest(
        titulo = this.titulo,
        codigo = this.codigo,
        descripcion = this.descripcion,
        docenteId = this.docenteId,
        duracionDias = this.duracionDias,
        temas = this.temas,
        estado = this.estado,
        fechaCreacion = this.fechaCreacion
    )
}