package com.stiven.sos.api

import org.json.JSONObject
import retrofit2.Response

/**
 * Convierte una respuesta HTTP fallida en un mensaje entendible para el usuario.
 * Lee el campo "error" / "mensaje" / "message" que envía el backend; si no hay, usa un texto por código.
 * Ojo: consume errorBody(), así que se debe llamar una sola vez por respuesta.
 */
fun mensajeDeError(response: Response<*>): String {
    val cuerpo = try { response.errorBody()?.string() } catch (_: Exception) { null }
    val delServidor = cuerpo?.let { texto ->
        try {
            val json = JSONObject(texto)
            listOf("error", "mensaje", "message")
                .firstNotNullOfOrNull { k -> json.optString(k).takeIf { it.isNotBlank() } }
        } catch (_: Exception) {
            texto.takeIf { it.isNotBlank() && it.length < 300 && !it.trimStart().startsWith("<") }
        }
    }
    return when (response.code()) {
        412 -> delServidor?.let {
            if (it.contains("explicación", ignoreCase = true))
                "Este tema todavía no tiene una explicación aprobada. Genera la explicación con IA, apruébala en Explicaciones y luego genera las preguntas."
            else it
        } ?: "Falta un paso previo para esta acción."
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        403 -> delServidor ?: "No tienes permisos para esta acción."
        404 -> delServidor ?: "No se encontró la información solicitada."
        503 -> delServidor ?: "El servicio no está disponible. Revisa que el backend esté encendido."
        in 500..599 -> delServidor?.let { "Error del servidor: $it" } ?: "Error del servidor (${response.code()})."
        else -> delServidor ?: "Error ${response.code()}: ${response.message()}"
    }
}
