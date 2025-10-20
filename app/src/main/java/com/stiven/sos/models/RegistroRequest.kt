package com.stiven.sos.models

/**
 * Modelo de datos para la solicitud de registro de usuario.
 *
 * @param nombreCompleto Nombre completo del usuario
 * @param apodo Nombre de usuario/apodo
 * @param correo Correo electrónico del usuario
 * @param contrasena Contraseña del usuario
 * @param rol Rol del usuario: "estudiante" o "docente"
 */
data class RegistroRequest(
    val nombreCompleto: String,
    val apodo: String,
    val correo: String,
    val contrasena: String,
    val rol: String
)
data class ActualizarPerfilRequest(
    val nombreCompleto: String,
    val apodo: String,
    val correo: String,
    val rol: String
)