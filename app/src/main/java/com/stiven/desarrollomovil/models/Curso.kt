// Archivo: app/src/main/java/com/stiven/desarrollomovil/models/Curso.kt

package com.stiven.desarrollomovil.models

// ========================================
// MODELO TEMA
// ========================================
data class Tema(
    val id: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val archivoUrl: String = "",
    val tipo: String = "",
    val fechaCreacion: String = ""
)

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
    val fechaCreacion: String = ""
)

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

// --- 🔥 ESTA SECCIÓN HA SIDO ELIMINADA 🔥 ---
// data class ApiResponse( ... )


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
