package com.stiven.sos.models

/** Respuesta de GET api/solicitudes/pendientes-docente. */
data class PendientesDocente(
    val preguntas: Int = 0,
    val explicaciones: Int = 0,
    val temasSinExplicacion: Int = 0,
    val aspirantes: Int = 0,
    val total: Int = 0,
    val cursos: List<PendientesCursoDocente> = emptyList()
)

data class PendientesCursoDocente(
    val cursoId: String = "",
    val titulo: String = "",
    val codigo: String = "",
    val preguntas: Int = 0,
    val explicaciones: Int = 0,
    val temasSinExplicacion: Int = 0,
    val aspirantes: Int = 0
) {
    val total: Int get() = preguntas + explicaciones + temasSinExplicacion + aspirantes
}
