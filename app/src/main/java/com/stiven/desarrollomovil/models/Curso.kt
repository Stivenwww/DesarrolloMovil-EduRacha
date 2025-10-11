package com.stiven.desarrollomovil.models

data class Tema(
    val id: String? = null,
    val titulo: String = "",
    val contenido: String = "",
    val archivoUrl: String = "",
    val tipo: String = "",
    val fechaCreacion: String = ""
)

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