package com.stiven.sos.models

/** Respuesta de GET reportes/cursos/{cursoId}/estadisticas (calculada por el servidor con los datos de cada estudiante). */
data class EstadisticasCurso(
    val cursoId: String = "",
    val cursoTitulo: String = "",
    val generado: String = "",
    val resumen: ResumenEstadisticas = ResumenEstadisticas(),
    val actividadDiaria: List<PuntoActividad> = emptyList(),
    val distribucionPuntajes: List<RangoPuntaje> = emptyList(),
    val porTema: List<EstadisticaTema> = emptyList(),
    val estudiantes: List<EstadisticaEstudiante> = emptyList(),
    val preguntasDificiles: List<PreguntaDificil> = emptyList()
)

data class ResumenEstadisticas(
    val totalEstudiantes: Int = 0,
    val activosUltimos7Dias: Int = 0,
    val quizzesFinalizados: Int = 0,
    val promedioAciertos: Int = 0,
    val tasaAprobacion: Int = 0,
    val xpTotal: Int = 0,
    val rachaPromedio: Double = 0.0,
    val tiempoPromedioSeg: Int = 0,
    val estudiantesEnRiesgo: Int = 0
)

data class PuntoActividad(val fecha: String = "", val quizzes: Int = 0, val estudiantes: Int = 0)
data class RangoPuntaje(val etiqueta: String = "", val cantidad: Int = 0)
data class EstadisticaTema(
    val temaId: String = "",
    val titulo: String = "",
    val intentos: Int = 0,
    val estudiantes: Int = 0,
    val promedio: Int = 0,
    val tasaAprobacion: Int = 0
)
data class EstadisticaEstudiante(
    val estudianteId: String = "",
    val nombre: String = "",
    val experiencia: Int = 0,
    val racha: Int = 0,
    val vidas: Int = 0,
    val quizzes: Int = 0,
    val promedio: Int = 0,
    val temasAprobados: Int = 0,
    val ultimaActividad: String? = null,
    val diasSinActividad: Int? = null,
    val enRiesgo: Boolean = false,
    val motivoRiesgo: String? = null
)
data class PreguntaDificil(val preguntaId: String = "", val texto: String = "", val respuestas: Int = 0, val aciertos: Int = 0)
