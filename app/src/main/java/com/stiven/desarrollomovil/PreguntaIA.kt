// Archivo: app/src/main/java/com/stiven/desarrollomovil/models/PreguntaModels.kt

package com.stiven.desarrollomovil.models

import kotlinx.serialization.Serializable

// ========================================================================
// ENUMS Y CONSTANTES
// ========================================================================

/**
 * Enum para estados de validación que vienen de la API
 */
@Serializable
enum class EstadoValidacion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    EDITADA
}

// ========================================================================
// MODELOS DE DOMINIO (NÚCLEO)
// ========================================================================

/**
 * Modelo para una opción de respuesta
 */
@Serializable
data class OpcionIA(
    val id: String? = null, // ID opcional para la opción
    val texto: String,
    val esCorrecta: Boolean
)

/**
 * Modelo para el historial de revisiones
 */
@Serializable
data class HistorialRevision(
    val revisadoPor: String,
    val fechaRevision: Long,
    val notas: String,
    val modificada: Boolean
)

/**
 * Modelo para los metadatos generados por la IA
 */
@Serializable
data class MetadatosIA(
    val generadoPor: String,
    val instruccion: String? = null,
    val lotId: String? = null
)

/**
 * Modelo para la versión original de una pregunta editada
 */
@Serializable
data class VersionOriginal(
    val texto: String,
    val opciones: List<OpcionIA>
)

/**
 * Modelo principal y definitivo para una Pregunta de IA
 *
 * ⚠️ IMPORTANTE: Este modelo debe contener TODOS los campos que vienen de la API
 * incluyendo la información del curso y tema para poder filtrar correctamente.
 */
@Serializable
data class PreguntaIA(
    // === CAMPOS PRINCIPALES ===
    val id: String? = null,
    val texto: String,
    val opciones: List<OpcionIA>,
    val fuente: String,
    var estado: EstadoValidacion = EstadoValidacion.PENDIENTE,
    val dificultad: String? = null,
    val creadoPor: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),

    // === METADATOS DE IA ===
    val metadatosIA: MetadatosIA? = null,

    // === HISTORIAL Y VERSIÓN ORIGINAL ===
    val historialRevisiones: List<HistorialRevision> = emptyList(),
    val versionOriginal: VersionOriginal? = null,

    // === INFORMACIÓN DEL CURSO (CRÍTICO PARA FILTRADO) ===
    val cursoId: String? = null,      // ID del curso en Firebase/MongoDB
    val cursoTitulo: String? = null,  // Título del curso
    val cursoCodigo: String? = null,  // Código del curso (ej: "KOT101")

    // === INFORMACIÓN DEL TEMA ===
    val temaId: String? = null,       // ID del tema (ej: "tema1", "t1")
    val temaTitulo: String? = null    // Título del tema
) {
    // ========================================================================
    // PROPIEDADES CALCULADAS (HELPERS)
    // No se serializan pero son útiles en la UI
    // ========================================================================

    /**
     * Índice de la respuesta correcta
     */
    val respuestaCorrectaIndex: Int
        get() = opciones.indexOfFirst { it.esCorrecta }

    /**
     * Indica si la pregunta ha sido revisada al menos una vez
     */
    val fueRevisada: Boolean
        get() = historialRevisiones.isNotEmpty()

    /**
     * Último revisor que modificó la pregunta
     */
    val ultimoRevisor: String?
        get() = historialRevisiones.lastOrNull()?.revisadoPor

    /**
     * Fecha de la última revisión
     */
    val fechaUltimaRevision: Long?
        get() = historialRevisiones.lastOrNull()?.fechaRevision

    /**
     * Indica si la pregunta fue modificada respecto a su versión original
     */
    val fueModificada: Boolean
        get() = versionOriginal != null

    /**
     * Devuelve información del curso formateada para mostrar en UI
     */
    val infoCursoCompleta: String
        get() = buildString {
            cursoCodigo?.let { append("$it - ") }
            cursoTitulo?.let { append(it) }
        }.ifEmpty { "Curso sin especificar" }
}

// ========================================================================
// EXTENSIONES ÚTILES
// ========================================================================

/**
 * Crea una copia de la pregunta marcándola como aprobada
 */
fun PreguntaIA.aprobar(revisadoPor: String, notas: String = "Aprobada sin cambios"): PreguntaIA {
    return this.copy(
        estado = EstadoValidacion.APROBADA,
        historialRevisiones = historialRevisiones + HistorialRevision(
            revisadoPor = revisadoPor,
            fechaRevision = System.currentTimeMillis(),
            notas = notas,
            modificada = false
        )
    )
}

/**
 * Crea una copia de la pregunta marcándola como rechazada
 */
fun PreguntaIA.rechazar(revisadoPor: String, motivo: String): PreguntaIA {
    return this.copy(
        estado = EstadoValidacion.RECHAZADA,
        historialRevisiones = historialRevisiones + HistorialRevision(
            revisadoPor = revisadoPor,
            fechaRevision = System.currentTimeMillis(),
            notas = "Rechazada: $motivo",
            modificada = false
        )
    )
}

/**
 * Crea una copia de la pregunta con ediciones, guardando la versión original
 */
fun PreguntaIA.editar(
    nuevoTexto: String,
    nuevasOpciones: List<OpcionIA>,
    revisadoPor: String,
    notasEdicion: String
): PreguntaIA {
    val versionOriginalGuardada = versionOriginal ?: VersionOriginal(
        texto = this.texto,
        opciones = this.opciones
    )

    return this.copy(
        texto = nuevoTexto,
        opciones = nuevasOpciones,
        estado = EstadoValidacion.EDITADA,
        versionOriginal = versionOriginalGuardada,
        historialRevisiones = historialRevisiones + HistorialRevision(
            revisadoPor = revisadoPor,
            fechaRevision = System.currentTimeMillis(),
            notas = notasEdicion,
            modificada = true
        )
    )
}