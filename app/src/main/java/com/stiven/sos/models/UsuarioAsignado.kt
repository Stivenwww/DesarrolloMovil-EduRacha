package com.stiven.sos.models

import com.google.gson.annotations.SerializedName


data class UsuarioAsignado(
    @SerializedName("id")
    val uid: String = "",

    val nombre: String = "",

    @SerializedName("email")
    val correo: String = "",

    val rol: String = "estudiante",

    val estado: String = "activo",

    @SerializedName("fechaRegistro")
    val fechaRegistro: String? = null
)

/**
 * Respuesta del endpoint /api/solicitudes/curso/{id}/estudiantes
 */
data class EstudiantesCursoResponse(
    val message: String = "",
    val cursoId: String = "",
    val cursoTitulo: String = "",
    val totalEstudiantes: Int = 0,
    val estudiantes: List<UsuarioAsignado> = emptyList()
)
