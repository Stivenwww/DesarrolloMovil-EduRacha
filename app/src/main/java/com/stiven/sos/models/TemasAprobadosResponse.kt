package com.stiven.sos.models

/** Respuesta de GET /quiz/aprobados/{cursoId}. */
data class TemasAprobadosResponse(
    val temasAprobados: List<String> = emptyList(),
    val quizFinalAprobado: Boolean = false
)
