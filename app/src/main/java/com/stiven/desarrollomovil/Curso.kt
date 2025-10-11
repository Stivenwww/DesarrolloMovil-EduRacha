package com.stiven.desarrollomovil

import kotlinx.serialization.Serializable

@Serializable
data class Tema(
    val id: String? = null,
    val titulo: String = "",
    val contenido: String = "",
    val archivoUrl: String = "",
    val tipo: String = "",
    val fechaCreacion: String = ""
)

@Serializable
data class Curso(
    val id: String? = null,
    val titulo: String = "",
    val codigo: String = "",
    val descripcion: String = "",
    val docenteId: String = "",
    val duracionDias: Int = 0,
    val temas: Map<String, Tema>? = null,
    val estado: String = "activo",
    val fechaCreacion: String = System.currentTimeMillis().toString(),
    val imagenUrl: String = "" // Extra para compatibilidad con tu UI
)