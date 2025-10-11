package com.stiven.desarrollomovil

/**
 * Repositorio para gestionar la asignación de estudiantes a cursos.
 * Almacenamiento temporal en memoria (se perderá al cerrar la app).
 * Se combinan funcionalidades del repositorio original y el corregido.
 * NOTA: Se mantiene la validación de estudiantes por 'id' del repositorio original.
 */
object GruposRepository {
    // Map de Curso (título o ID) -> Lista de Estudiantes asignados
    private val gruposMap = mutableMapOf<String, MutableList<Estudiante>>()

    // ===============================================
    // FUNCIONALIDAD DE ASIGNACIÓN
    // ===============================================

    /**
     * Asigna una lista de estudiantes a un curso.
     * No permite duplicados (valida por ID de estudiante, asumiendo 'id' es el identificador único).
     *
     * @param cursoTitulo Título del curso al que se asignarán los estudiantes
     * @param estudiantes Lista de estudiantes a asignar
     */
    fun asignarEstudiantes(cursoTitulo: String, estudiantes: List<Estudiante>) {
        if (!gruposMap.containsKey(cursoTitulo)) {
            // Inicializar la lista si el curso es nuevo
            gruposMap[cursoTitulo] = mutableListOf()
        }

        // Agregar solo los estudiantes que no estén ya asignados (usando 'id')
        estudiantes.forEach { estudiante ->
            if (!gruposMap[cursoTitulo]!!.any { it.id == estudiante.id }) {
                gruposMap[cursoTitulo]!!.add(estudiante)
            }
        }
    }

    /**
     * Asigna un solo estudiante a un curso.
     *
     * @param cursoTitulo Título del curso
     * @param estudiante Estudiante a asignar
     * @return true si se asignó correctamente, false si ya estaba asignado
     */
    fun asignarEstudiante(cursoTitulo: String, estudiante: Estudiante): Boolean {
        if (!gruposMap.containsKey(cursoTitulo)) {
            gruposMap[cursoTitulo] = mutableListOf()
        }

        // Verificar si ya está asignado (usando 'id')
        val yaAsignado = gruposMap[cursoTitulo]!!.any { it.id == estudiante.id }

        return if (!yaAsignado) {
            gruposMap[cursoTitulo]!!.add(estudiante)
            true
        } else {
            false
        }
    }

    // ===============================================
    // FUNCIONALIDAD DE OBTENCIÓN Y CONSULTA
    // ===============================================

    /**
     * Obtiene todos los estudiantes asignados a un curso específico.
     * (Función presente en ambos archivos)
     *
     * @param cursoTitulo Título del curso
     * @return Lista de estudiantes asignados (vacía si no hay ninguno)
     */
    fun obtenerEstudiantesPorCurso(cursoTitulo: String): List<Estudiante> {
        return gruposMap[cursoTitulo] ?: emptyList()
    }

    /**
     * Obtiene el mapa completo de todos los cursos con sus estudiantes.
     * (Función presente en ambos archivos)
     *
     * @return Map inmutable de curso -> lista de estudiantes
     */
    fun obtenerTodosLosCursosConEstudiantes(): Map<String, List<Estudiante>> {
        return gruposMap.toMap()
    }

    /**
     * Cuenta el número de estudiantes asignados a un curso.
     *
     * @param cursoTitulo Título del curso
     * @return Cantidad de estudiantes asignados
     */
    fun contarEstudiantesPorCurso(cursoTitulo: String): Int {
        return gruposMap[cursoTitulo]?.size ?: 0
    }

    /**
     * Verifica si un estudiante ya está asignado a un curso.
     *
     * @param cursoTitulo Título del curso
     * @param estudianteId ID del estudiante
     * @return true si ya está asignado, false en caso contrario
     */
    fun estaAsignado(cursoTitulo: String, estudianteId: String): Boolean {
        return gruposMap[cursoTitulo]?.any { it.id == estudianteId } ?: false
    }

    /**
     * Obtiene todos los cursos a los que está asignado un estudiante.
     *
     * @param estudianteId ID del estudiante
     * @return Lista de títulos de cursos
     */
    fun obtenerCursosPorEstudiante(estudianteId: String): List<String> {
        return gruposMap.filter { (_, estudiantes) ->
            estudiantes.any { it.id == estudianteId }
        }.keys.toList()
    }

    /**
     * Obtiene la lista de estudiantes NO asignados a un curso específico.
     *
     * @param cursoTitulo Título del curso
     * @param todosLosEstudiantes Lista completa de estudiantes del sistema
     * @return Lista de estudiantes no asignados al curso
     */
    fun obtenerEstudiantesNoAsignados(
        cursoTitulo: String,
        todosLosEstudiantes: List<Estudiante>
    ): List<Estudiante> {
        val asignados = obtenerEstudiantesPorCurso(cursoTitulo)
        return todosLosEstudiantes.filter { estudiante ->
            !asignados.any { it.id == estudiante.id }
        }
    }

    /**
     * Obtiene el total de estudiantes en todos los cursos.
     * (Nueva función del archivo corregido)
     */
    fun obtenerTotalEstudiantes(): Int {
        return gruposMap.values.sumOf { it.size }
    }

    /**
     * Obtiene el total de cursos con estudiantes.
     * (Nueva función del archivo corregido, similar a obtenerEstadisticas()["totalCursos"])
     */
    fun obtenerTotalCursos(): Int {
        return gruposMap.keys.size
    }

    /**
     * Obtiene todos los nombres de cursos registrados
     */
    fun obtenerNombresCursos(): List<String> {
        return gruposMap.keys.toList()
    }

    /**
     * Verifica si hay cursos registrados
     */
    fun tieneGrupos(): Boolean {
        return gruposMap.isNotEmpty()
    }

    /**
     * Verifica si un curso tiene estudiantes asignados
     */
    fun cursoTieneEstudiantes(cursoTitulo: String): Boolean {
        return gruposMap[cursoTitulo]?.isNotEmpty() ?: false
    }


    // ===============================================
    // FUNCIONALIDAD DE MODIFICACIÓN Y ELIMINACIÓN
    // ===============================================

    /**
     * Elimina un estudiante específico de un curso.
     * Se usa el 'id' del estudiante.
     *
     * @param cursoTitulo Título del curso
     * @param estudiante Estudiante a eliminar (se usa su ID)
     * @return true si se eliminó correctamente, false si no estaba asignado
     */
    fun eliminarEstudianteDeCurso(cursoTitulo: String, estudiante: Estudiante): Boolean {
        val lista = gruposMap[cursoTitulo] ?: return false
        return lista.removeAll { it.id == estudiante.id }
    }

    /**
     * Elimina un estudiante específico de un curso usando su email.
     * Se mantiene esta función del archivo corregido, asumiendo que `email` puede ser otra clave de búsqueda.
     */
    fun eliminarEstudianteDeCursoPorEmail(cursoTitulo: String, emailEstudiante: String): Boolean {
        val estudiantes = gruposMap[cursoTitulo]
        return if (estudiantes != null) {
            // Se usa el email, como se especificaba en el archivo 2.
            estudiantes.removeIf { it.email == emailEstudiante }
        } else {
            false
        }
    }

    /**
     * Elimina un estudiante de todos los cursos.
     * Útil cuando se elimina un estudiante del sistema.
     *
     * @param estudiante Estudiante a eliminar (se usa su ID)
     * @return Cantidad de cursos de los que fue eliminado
     */
    fun eliminarEstudianteDeTodosLosCursos(estudiante: Estudiante): Int {
        var contador = 0
        gruposMap.values.forEach { lista ->
            // Usamos removeAll porque devuelve true si hubo un cambio (eliminación)
            if (lista.removeAll { it.id == estudiante.id }) {
                contador++
            }
        }
        return contador
    }

    /**
     * Elimina todos los estudiantes de un curso específico.
     *
     * @param cursoTitulo Título del curso
     */
    fun limpiarCurso(cursoTitulo: String) {
        gruposMap[cursoTitulo]?.clear()
    }

    /**
     * Elimina un curso completo del repositorio (el curso y su lista de estudiantes).
     */
    fun eliminarCurso(cursoTitulo: String) {
        gruposMap.remove(cursoTitulo)
    }

    /**
     * Elimina toda la información de grupos.
     * Útil para testing o reset completo.
     */
    fun limpiarTodo() {
        gruposMap.clear()
    }

    /**
     * Transfiere un estudiante de un curso a otro.
     *
     * @param estudianteId ID del estudiante
     * @param cursoOrigen Título del curso origen
     * @param cursoDestino Título del curso destino
     * @return true si la transferencia fue exitosa
     */
    fun transferirEstudiante(estudianteId: String, cursoOrigen: String, cursoDestino: String): Boolean {
        // 1. Encontrar el estudiante
        val estudiante = gruposMap[cursoOrigen]?.find { it.id == estudianteId } ?: return false

        // 2. Eliminar del curso origen
        return if (eliminarEstudianteDeCurso(cursoOrigen, estudiante)) {
            // 3. Asignar al curso destino (reusa asignarEstudiante que valida duplicados)
            asignarEstudiante(cursoDestino, estudiante)
        } else {
            false
        }
    }

    // ===============================================
    // FUNCIONALIDAD DE ESTADÍSTICAS
    // ===============================================

    /**
     * Obtiene estadísticas generales de grupos.
     *
     * @return Map con estadísticas (totalCursos, totalEstudiantes, promedioEstudiantesPorCurso)
     */
    fun obtenerEstadisticas(): Map<String, Int> {
        val totalCursos = gruposMap.size
        val totalEstudiantes = gruposMap.values.sumOf { it.size }
        val promedio = if (totalCursos > 0) totalEstudiantes / totalCursos else 0

        return mapOf(
            "totalCursos" to totalCursos,
            "totalEstudiantes" to totalEstudiantes,
            "promedioEstudiantesPorCurso" to promedio
        )
    }
}