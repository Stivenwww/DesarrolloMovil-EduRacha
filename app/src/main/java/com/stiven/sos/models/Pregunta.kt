package com.stiven.sos.models

import com.google.gson.annotations.SerializedName

// ========================================================================
// MODELOS PRINCIPALES DE PREGUNTAS
// ========================================================================

data class Pregunta(
    val id: String? = null,
    val cursoId: String = "",
    val temaId: String = "",
    val texto: String = "",
    val opciones: List<Opcion> = emptyList(),
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

data class Opcion(
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

// ========================================================================
// MODELOS DE REQUEST/RESPONSE PARA PREGUNTAS
// ========================================================================

data class GenerarPreguntasRequest(
    val cursoId: String,
    val temaId: String,
    val temaTexto: String,
    val cantidad: Int = 5
)

data class PreguntasIAResponse(
    val message: String,
    val total: Int,
    val preguntas: List<Pregunta>
)

data class ActualizarEstadoRequest(
    val estado: String,
    val notas: String? = null
)

data class EstadoUpdateResponse(
    val message: String,
    val nuevoEstado: String
)

// ========================================================================
// ESTADOS Y CONSTANTES
// ========================================================================

object EstadoPregunta {
    const val PENDIENTE = "pendiente"
    const val PENDIENTE_REVISION = "pendiente_revision"
    const val APROBADA = "aprobada"
    const val RECHAZADA = "rechazada"
    const val TODOS = "todos"
}

object FuentePregunta {
    const val IA = "ia"
    const val DOCENTE = "docente"
    const val IMPORTADA = "importada"
}

object DificultadPregunta {
    const val FACIL = "facil"
    const val MEDIO = "medio"
    const val DIFICIL = "dificil"
}