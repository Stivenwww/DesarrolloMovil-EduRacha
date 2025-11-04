package com.stiven.sos.models

import com.google.gson.annotations.SerializedName

// ============================================
// MODELOS DE QUIZ
// ============================================

data class Quiz(
    val id: String? = null,
    val cursoId: String = "",
    val temaId: String = "",
    val estudianteId: String = "",
    val preguntas: List<QuizPregunta> = emptyList(),
    val inicio: String = "",
    val fin: String? = null,
    val tiempoUsadoSeg: Int = 0,
    val estado: String = "en_progreso",
    val intentoNumero: Int = 1,
    val preguntasCorrectas: Int = 0,
    val preguntasIncorrectas: Int = 0,
    val tiempoPromedioPorPregunta: Double = 0.0,
    val recursoVisto: Boolean = false,
    val experienciaGanada: Int = 0,
    val bonificacionRapidez: Int = 0,
    val bonificacionPrimeraVez: Int = 0,
    val bonificacionTodoCorrecto: Int = 0,
    val respuestas: List<RespuestaQuiz> = emptyList()
)

data class QuizPregunta(
    val preguntaId: String = "",
    val orden: Int = 0
)

data class RespuestaQuiz(
    val preguntaId: String = "",
    val respuestaSeleccionada: Int = -1,
    val tiempoSeg: Int = 0,
    val esCorrecta: Boolean = false
)

// ============================================
// REQUESTS
// ============================================

data class IniciarQuizRequest(
    val cursoId: String,
    val temaId: String
)

data class FinalizarQuizRequest(
    val quizId: String,
    val respuestas: List<RespuestaUsuario>
)

data class RespuestaUsuario(
    val preguntaId: String,
    val respuestaSeleccionada: Int,
    val tiempoSeg: Int
)

// ============================================
// RESPONSES
// ============================================

data class IniciarQuizResponse(
    val quizId: String,
    val preguntas: List<PreguntaQuizResponse>
)

data class PreguntaQuizResponse(
    val id: String,
    val orden: Int,
    val texto: String,
    val opciones: List<OpcionQuizResponse>
)

data class OpcionQuizResponse(
    val id: Int,
    val texto: String
)

data class FinalizarQuizResponse(
    val preguntasCorrectas: Int,
    val preguntasIncorrectas: Int,
    val experienciaGanada: Int,
    val vidasRestantes: Int,
    val bonificaciones: BonificacionesResponse
)

data class BonificacionesResponse(
    val rapidez: Int,
    val primeraVez: Int,
    val todoCorrecto: Int
)

data class RevisionQuizResponse(
    val quizId: String,
    val preguntas: List<PreguntaRevisionResponse>
)

data class PreguntaRevisionResponse(
    val preguntaId: String,
    val texto: String,
    val opciones: List<Opcion>,
    val respuestaUsuario: Int,
    val respuestaCorrecta: Int,
    val explicacion: String
)

data class TemaInfoResponse(
    val temaId: String,
    val cursoId: String,
    val preguntasDisponibles: Int,
    val vidasActuales: Int,
    val explicacionVista: Boolean,
    val inscrito: Boolean
)

data class VidasResponse(
    val vidasActuales: Int,
    val vidasMax: Int,
    val minutosParaProximaVida: Int
)

data class HistorialQuizzesResponse(
    val quizzes: List<Quiz>
)

// ============================================
// RETROALIMENTACIÓN DE FALLOS - NUEVO
// ============================================

data class RetroalimentacionFallosResponse(
    val quizId: String,
    val totalFallos: Int,
    val preguntasFalladas: List<RetroalimentacionPregunta>
)

data class RetroalimentacionPregunta(
    val preguntaId: String,
    val texto: String,
    val respuestaUsuarioTexto: String,
    val respuestaCorrectaTexto: String,
    val explicacion: String
)

// ============================================
// MODELO DE INSCRIPCIÓN
// ============================================

data class Inscripcion(
    val userId: String = "",
    val cursoId: String = "",
    val estado: String = "en_progreso",
    val vidasActuales: Int = 5,
    val vidasMax: Int = 5,
    val ultimaRegen: Long = 0,
    val intentosHechos: Int = 0,
    val experiencia: Int = 0,
    val diasConsecutivos: Int = 0,
    val ultimaFecha: Long = 0,
    val vidas: Int = 5
)