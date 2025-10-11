package com.stiven.desarrollomovil

object GruposRepository {
    // Almacenamiento temporal: Map de Asignatura -> Lista de Estudiantes
    private val gruposMap = mutableMapOf<String, MutableList<Estudiante>>()

    fun asignarEstudiantes(asignatura: String, estudiantes: List<Estudiante>) {
        if (!gruposMap.containsKey(asignatura)) {
            gruposMap[asignatura] = mutableListOf()
        }

        // Agregar solo los estudiantes que no estén ya asignados
        estudiantes.forEach { estudiante ->
            if (!gruposMap[asignatura]!!.any { it.id == estudiante.id }) {
                gruposMap[asignatura]!!.add(estudiante)
            }
        }
    }

    fun obtenerEstudiantesPorAsignatura(asignatura: String): List<Estudiante> {
        return gruposMap[asignatura] ?: emptyList()
    }

    fun obtenerTodasLasAsignaturasConEstudiantes(): Map<String, List<Estudiante>> {
        return gruposMap.toMap()
    }

    fun eliminarEstudianteDeAsignatura(asignatura: String, estudiante: Estudiante) {
        gruposMap[asignatura]?.removeAll { it.id == estudiante.id }
    }

    fun limpiarTodo() {
        gruposMap.clear()
    }
}