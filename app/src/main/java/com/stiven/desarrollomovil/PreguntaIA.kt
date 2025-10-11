package com.stiven.desarrollomovil

import kotlinx.serialization.Serializable

// Enum para estados de validación
@Serializable
enum class EstadoValidacion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    EDITADA
}

@Serializable
enum class DificultadPregunta {
    FACIL,
    MEDIA,
    DIFICIL
}

@Serializable
data class OpcionRespuesta(
    val texto: String,
    val esCorrecta: Boolean
)

@Serializable
data class HistorialRevision(
    val revisadoPor: String,
    val fechaRevision: Long,
    val notasRevision: String,
    val modificada: Boolean
)

@Serializable
data class VersionOriginal(
    val texto: String,
    val opciones: List<OpcionRespuesta>
)

@Serializable
data class MetadatosPregunta(
    val generadoPor: String, // ej: "IA", "Docente", "Sistema"
    val instruccion: String,
    val lotId: String
)

@Serializable
data class PreguntaIA(
    val id: String, // _id de MongoDB
    val texto: String,
    val opciones: List<OpcionRespuesta>,
    val fuente: String, // ej: "Tema 1: Introducción"
    var estado: EstadoValidacion,
    val dificultad: DificultadPregunta,
    val creadoPor: String,
    val fechaCreacion: Long,

    // Metadatos
    val metadatos: MetadatosPregunta,

    // Historial de Revisiones
    val historialRevisiones: MutableList<HistorialRevision> = mutableListOf(),

    // Versión Original
    val versionOriginal: VersionOriginal?,

    // Información adicional para UI - AHORA USA CURSO
    val cursoTitulo: String,
    val cursoCodigo: String,
    val tema: String
) {
    // Propiedad calculada para obtener la respuesta correcta
    val respuestaCorrecta: Int
        get() = opciones.indexOfFirst { it.esCorrecta }

    // Propiedad para verificar si fue revisada
    val fueRevisada: Boolean
        get() = historialRevisiones.isNotEmpty()

    // Último revisor
    val ultimoRevisor: String?
        get() = historialRevisiones.lastOrNull()?.revisadoPor

    // Fecha de última revisión
    val fechaUltimaRevision: Long?
        get() = historialRevisiones.lastOrNull()?.fechaRevision
}

// Repositorio simulado (deberá conectarse a MongoDB)
object PreguntasIARepository {

    private val preguntasPorCurso = mutableMapOf<String, MutableList<PreguntaIA>>()

    init {
        generarPreguntasEjemplo()
    }

    private fun generarPreguntasEjemplo() {
        // Obtener cursos existentes y generar preguntas para cada uno
        CrearCursoObject.cursosGuardados.forEach { curso ->
            if (curso.estado == "activo" || curso.estado == "borrador") {
                val preguntasCurso = mutableListOf<PreguntaIA>()

                // Generar 2-5 preguntas de ejemplo por curso
                val cantidad = if (curso.estado == "activo") 3 else 5

                repeat(cantidad) { index ->
                    preguntasCurso.add(
                        PreguntaIA(
                            id = "${curso.codigo}_Q${index + 1}",
                            texto = "Pregunta de ejemplo ${index + 1} para el curso ${curso.titulo}",
                            opciones = listOf(
                                OpcionRespuesta("Opción A - Respuesta incorrecta", false),
                                OpcionRespuesta("Opción B - Respuesta correcta", true),
                                OpcionRespuesta("Opción C - Respuesta incorrecta", false),
                                OpcionRespuesta("Opción D - Respuesta incorrecta", false)
                            ),
                            fuente = curso.temas?.keys?.firstOrNull() ?: "Tema general",
                            estado = EstadoValidacion.PENDIENTE,
                            dificultad = when (index % 3) {
                                0 -> DificultadPregunta.FACIL
                                1 -> DificultadPregunta.MEDIA
                                else -> DificultadPregunta.DIFICIL
                            },
                            creadoPor = "sistema_ia",
                            fechaCreacion = System.currentTimeMillis() - (index * 86400000L),
                            metadatos = MetadatosPregunta(
                                generadoPor = "IA - GPT-4",
                                instruccion = "Generar pregunta basada en ${curso.titulo}",
                                lotId = "LOTE_${curso.codigo}_001"
                            ),
                            versionOriginal = null,
                            cursoTitulo = curso.titulo,
                            cursoCodigo = curso.codigo,
                            tema = curso.temas?.keys?.firstOrNull() ?: "Tema general"
                        )
                    )
                }

                preguntasPorCurso[curso.titulo] = preguntasCurso
            }
        }
    }

    fun obtenerCursosConPreguntasPendientes(): List<Curso> {
        return CrearCursoObject.cursosGuardados.filter { curso ->
            contarPreguntasPendientesPorCurso(curso.titulo) > 0
        }
    }

    fun obtenerPreguntasPendientes(cursoTitulo: String): List<PreguntaIA> {
        return preguntasPorCurso[cursoTitulo]?.filter {
            it.estado == EstadoValidacion.PENDIENTE
        } ?: emptyList()
    }

    fun obtenerTodasLasPreguntas(cursoTitulo: String): List<PreguntaIA> {
        return preguntasPorCurso[cursoTitulo] ?: emptyList()
    }

    fun aprobarPregunta(preguntaId: String, revisadoPor: String, notas: String = "") {
        preguntasPorCurso.values.forEach { lista ->
            lista.find { it.id == preguntaId }?.let { pregunta ->
                pregunta.estado = EstadoValidacion.APROBADA
                pregunta.historialRevisiones.add(
                    HistorialRevision(
                        revisadoPor = revisadoPor,
                        fechaRevision = System.currentTimeMillis(),
                        notasRevision = notas.ifEmpty { "Pregunta aprobada" },
                        modificada = false
                    )
                )
            }
        }
    }

    fun rechazarPregunta(preguntaId: String, revisadoPor: String, motivo: String) {
        preguntasPorCurso.values.forEach { lista ->
            lista.find { it.id == preguntaId }?.let { pregunta ->
                pregunta.estado = EstadoValidacion.RECHAZADA
                pregunta.historialRevisiones.add(
                    HistorialRevision(
                        revisadoPor = revisadoPor,
                        fechaRevision = System.currentTimeMillis(),
                        notasRevision = "Rechazada: $motivo",
                        modificada = false
                    )
                )
            }
        }
    }

    fun eliminarPregunta(preguntaId: String): Boolean {
        preguntasPorCurso.values.forEach { lista ->
            val index = lista.indexOfFirst { it.id == preguntaId }
            if (index != -1) {
                lista.removeAt(index)
                return true
            }
        }
        return false
    }

    fun actualizarPregunta(preguntaEditada: PreguntaIA, revisadoPor: String, notas: String) {
        preguntasPorCurso[preguntaEditada.cursoTitulo]?.let { lista ->
            val index = lista.indexOfFirst { it.id == preguntaEditada.id }
            if (index != -1) {
                val preguntaAnterior = lista[index]

                // Guardar versión original si no existe
                val versionOriginal = preguntaAnterior.versionOriginal ?: VersionOriginal(
                    texto = preguntaAnterior.texto,
                    opciones = preguntaAnterior.opciones
                )

                // Agregar historial de revisión
                preguntaEditada.historialRevisiones.add(
                    HistorialRevision(
                        revisadoPor = revisadoPor,
                        fechaRevision = System.currentTimeMillis(),
                        notasRevision = notas,
                        modificada = true
                    )
                )

                lista[index] = preguntaEditada.copy(
                    estado = EstadoValidacion.EDITADA,
                    versionOriginal = versionOriginal
                )
            }
        }
    }

    fun contarPreguntasPendientesPorCurso(cursoTitulo: String): Int {
        return preguntasPorCurso[cursoTitulo]?.count {
            it.estado == EstadoValidacion.PENDIENTE
        } ?: 0
    }

    fun contarTotalPreguntasPendientes(): Int {
        return preguntasPorCurso.values.sumOf { lista ->
            lista.count { it.estado == EstadoValidacion.PENDIENTE }
        }
    }

    // Función para actualizar preguntas cuando se creen nuevos cursos
    fun actualizarPreguntasParaNuevosCursos() {
        CrearCursoObject.cursosGuardados.forEach { curso ->
            if (!preguntasPorCurso.containsKey(curso.titulo) &&
                (curso.estado == "activo" || curso.estado == "borrador")) {
                generarPreguntasEjemplo()
            }
        }
    }
}