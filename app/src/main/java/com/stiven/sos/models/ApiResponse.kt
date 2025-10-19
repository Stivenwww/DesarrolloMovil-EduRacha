package com.stiven.sos.models

import kotlinx.serialization.Serializable

// ========================================================================
// MODELOS DE "SOBRE" (PETICIONES Y RESPUESTAS)
// ========================================================================

/**
 * SOBRE GENÉRICO: Respuesta simple de la API con un mensaje.
 * Útil para operaciones de crear, actualizar o eliminar.
 */
@Serializable
data class ApiResponse(
    val message: String,
    val id: String? = null
)


