package com.stiven.desarrollomovil.models

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

/**
 * SOBRE DE PETICIÓN: Datos necesarios para solicitar la generación de preguntas.
 */
@Serializable
data class GenerarPreguntasRequest(
    val cursoId: String,
    val temaId: String,
    val temaTexto: String,
    val cantidad: Int = 5 // Usamos Int, es más correcto para una cantidad.
)

/**
 * SOBRE DE RESPUESTA: Lo que la API devuelve al generar preguntas.
 * Fíjate que USA la clase `PreguntaIA`, pero NO la define aquí.
 */
@Serializable
data class PreguntasIAResponse(
    val message: String,
    val total: Int,
    val preguntas: List<PreguntaIA> // Usa el modelo de dominio central
)

/**
 * SOBRE DE PETICIÓN: Datos para revisar una pregunta (aprobar, rechazar o editar).
 */
@Serializable
data class RevisarPreguntaRequest(
    val estado: EstadoValidacion, // Usamos el enum para evitar errores de texto
    val notas: String = "",
    val preguntaEditada: PreguntaIA? = null // Opcional, si se edita
)

// ========================================================================
// ¡IMPORTANTE!
//
// Las siguientes clases FUERON ELIMINADAS de este archivo porque son
// MODELOS DE DOMINIO y deben vivir en su propio archivo (ej: PreguntaModels.kt)
// para evitar el error de "Redeclaration".
//
// - data class PreguntaIA(...)
// - data class OpcionIA(...)
// - data class MetadatosIA(...)
// - data class VersionOriginal(...)
//
// ========================================================================
