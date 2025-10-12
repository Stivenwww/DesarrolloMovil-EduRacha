package com.stiven.desarrollomovil.models

data class ApiResponse(
    val message: String,
    val id: String? = null,
    val estado: String? = null // Agregar para respuestas de revisión
)

// Agregar nuevos modelos para IA
data class PreguntaIA(
    val id: String? = null,
    val cursoId: String = "",
    val temaId: String = "",
    val texto: String = "",
    val opciones: List<OpcionIA> = emptyList(),
    val fuente: String = "",
    val estado: String = "",
    val dificultad: String? = null,
    val creadoPor: String = "",
    val fechaCreacion: String = "",
    val metadatosIA: MetadatosIA? = null,
    val revisadoPor: String? = null,
    val fechaRevision: String? = null,
    val notasRevision: String? = null,
    val modificada: Boolean = false
)

data class OpcionIA(
    val id: Int = 0,
    val texto: String = "",
    val esCorrecta: Boolean = false
)

data class MetadatosIA(
    val generadoPor: String? = null,
    val instruccion: String? = null,
    val loteId: String? = null,
    val versionOriginal: VersionOriginal? = null
)

data class VersionOriginal(
    val texto: String? = null,
    val opciones: List<String>? = null
)

// Request para generar preguntas
data class GenerarPreguntasRequest(
    val cursoId: String,
    val temaId: String,
    val temaTexto: String,
    val cantidad: String = "5"
)

// Response de generación
data class PreguntasIAResponse(
    val message: String,
    val total: Int,
    val preguntas: List<PreguntaIA>
)

// Request para revisar
data class RevisarPreguntaRequest(
    val estado: String, // "aprobada" o "rechazada"
    val notas: String = ""
)
