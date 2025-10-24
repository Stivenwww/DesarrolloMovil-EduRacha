// Archivo: app/src/main/java/com/stiven/sos/models/Usuario.kt

package com.stiven.sos.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para un usuario asignado a un curso.
 * Representa un estudiante individual.
 */
data class UsuarioAsignado(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "estudiante",
    val estado: String = "activo",
    @SerializedName("fechaRegistro")
    val fechaRegistro: String? = null
)



data class EstudiantesCursoResponse(
    val message: String = "",
    val cursoId: String = "",
    val cursoTitulo: String = "",
    val totalEstudiantes: Int = 0,
    val estudiantes: List<UsuarioAsignado> = emptyList()
)