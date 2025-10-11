package com.stiven.desarrollomovil

data class PreguntaIA(
    val id: String,
    val asignatura: String,
    val pregunta: String,
    val opciones: List<String>,
    val respuestaCorrecta: Int, // Índice de la respuesta correcta (0-3)
    val explicacion: String = "",
    var estado: EstadoValidacion = EstadoValidacion.PENDIENTE,
    val fechaGeneracion: Long = System.currentTimeMillis()
)

enum class EstadoValidacion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    EDITADA
}

// Companion object para datos de ejemplo
object PreguntasIARepository {

    private val preguntasPorAsignatura = mutableMapOf<String, MutableList<PreguntaIA>>()

    init {
        // Generar preguntas de ejemplo
        generarPreguntasEjemplo()
    }

    private fun generarPreguntasEjemplo() {
        // Preguntas para Bases de Datos
        preguntasPorAsignatura["Bases de Datos"] = mutableListOf(
            PreguntaIA(
                id = "1",
                asignatura = "Bases de Datos",
                pregunta = "¿Qué significa SQL en el contexto de bases de datos?",
                opciones = listOf(
                    "Structured Query Language",
                    "Simple Question Language",
                    "System Query Logic",
                    "Standard Quality Language"
                ),
                respuestaCorrecta = 0,
                explicacion = "SQL es el lenguaje estándar para la gestión y manipulación de bases de datos relacionales"
            ),
            PreguntaIA(
                id = "2",
                asignatura = "Bases de Datos",
                pregunta = "¿Cuál es la clave primaria en una tabla?",
                opciones = listOf(
                    "Un campo que puede contener valores duplicados",
                    "Un campo único que identifica cada registro",
                    "Un campo opcional en la tabla",
                    "Un campo que se puede modificar libremente"
                ),
                respuestaCorrecta = 1,
                explicacion = "La clave primaria identifica de manera única cada registro en una tabla"
            ),
            PreguntaIA(
                id = "3",
                asignatura = "Bases de Datos",
                pregunta = "¿Qué operación SQL se usa para recuperar datos?",
                opciones = listOf(
                    "INSERT",
                    "UPDATE",
                    "SELECT",
                    "DELETE"
                ),
                respuestaCorrecta = 2,
                explicacion = "SELECT es la operación que se utiliza para consultar y recuperar datos"
            )
        )

        // Preguntas para Desarrollo Móvil
        preguntasPorAsignatura["Desarrollo Móvil"] = mutableListOf(
            PreguntaIA(
                id = "4",
                asignatura = "Desarrollo Móvil",
                pregunta = "¿Qué patrón arquitectónico es recomendado por Google para apps Android?",
                opciones = listOf(
                    "MVC",
                    "MVVM",
                    "MVP",
                    "VIPER"
                ),
                respuestaCorrecta = 1,
                explicacion = "MVVM (Model-View-ViewModel) es el patrón recomendado por Google para apps Android modernas"
            ),
            PreguntaIA(
                id = "5",
                asignatura = "Desarrollo Móvil",
                pregunta = "¿Cuál es el ciclo de vida correcto de una Activity?",
                opciones = listOf(
                    "onCreate → onStart → onResume",
                    "onStart → onCreate → onResume",
                    "onCreate → onResume → onStart",
                    "onResume → onCreate → onStart"
                ),
                respuestaCorrecta = 0,
                explicacion = "El ciclo de vida de una Activity comienza con onCreate, seguido de onStart y finalmente onResume"
            )
        )

        // Preguntas para Programación
        preguntasPorAsignatura["Programación"] = mutableListOf(
            PreguntaIA(
                id = "6",
                asignatura = "Programación",
                pregunta = "¿Qué es la herencia en POO?",
                opciones = listOf(
                    "Compartir propiedades entre clases",
                    "Crear múltiples objetos",
                    "Eliminar código duplicado",
                    "Optimizar el rendimiento"
                ),
                respuestaCorrecta = 0,
                explicacion = "La herencia permite crear nuevas clases basadas en clases existentes, reutilizando código"
            )
        )
    }

    fun obtenerAsignaturasConPreguntasPendientes(): List<String> {
        return preguntasPorAsignatura.keys.filter { asignatura ->
            contarPreguntasPendientesPorAsignatura(asignatura) > 0
        }
    }

    fun obtenerPreguntasPendientes(asignatura: String): List<PreguntaIA> {
        return preguntasPorAsignatura[asignatura]?.filter {
            it.estado == EstadoValidacion.PENDIENTE
        } ?: emptyList()
    }

    fun obtenerPreguntasAprobadas(asignatura: String): List<PreguntaIA> {
        return preguntasPorAsignatura[asignatura]?.filter {
            it.estado == EstadoValidacion.APROBADA
        } ?: emptyList()
    }

    fun aprobarPregunta(preguntaId: String) {
        preguntasPorAsignatura.values.forEach { lista ->
            lista.find { it.id == preguntaId }?.estado = EstadoValidacion.APROBADA
        }
    }

    fun rechazarPregunta(preguntaId: String) {
        preguntasPorAsignatura.values.forEach { lista ->
            lista.find { it.id == preguntaId }?.estado = EstadoValidacion.RECHAZADA
        }
    }

    fun editarPregunta(preguntaId: String) {
        preguntasPorAsignatura.values.forEach { lista ->
            lista.find { it.id == preguntaId }?.estado = EstadoValidacion.EDITADA
        }
    }

    // ⭐ NUEVA FUNCIÓN: Actualizar pregunta completa
    fun actualizarPregunta(preguntaEditada: PreguntaIA) {
        preguntasPorAsignatura.values.forEach { lista ->
            val index = lista.indexOfFirst { it.id == preguntaEditada.id }
            if (index != -1) {
                lista[index] = preguntaEditada.copy(
                    estado = EstadoValidacion.EDITADA
                )
            }
        }
    }

    fun contarPreguntasPendientesPorAsignatura(asignatura: String): Int {
        return preguntasPorAsignatura[asignatura]?.count {
            it.estado == EstadoValidacion.PENDIENTE
        } ?: 0
    }
}