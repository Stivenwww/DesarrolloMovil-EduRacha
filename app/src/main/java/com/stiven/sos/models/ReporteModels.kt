package com.stiven.sos.models

import com.google.gson.annotations.SerializedName

sealed class ReporteEstado {
    object Idle : ReporteEstado()
    object Descargando : ReporteEstado()
    data class Exito(val nombreArchivo: String) : ReporteEstado()
    data class Error(val mensaje: String) : ReporteEstado()
}

enum class TipoReporte {
    DIARIO,
    TEMA,
    GENERAL,
    RANGO
}

data class DatosReporteDiarioResponse(
    @SerializedName("cursoId") val cursoId: String,
    @SerializedName("cursoNombre") val cursoNombre: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("totalEstudiantes") val totalEstudiantes: Int,
    @SerializedName("estudiantesConDatos") val estudiantesConDatos: Int,
    @SerializedName("estudiantes") val estudiantes: List<EstudianteReporteData>
)

data class EstudianteReporteData(
    @SerializedName("estudianteId") val estudianteId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("quizzesRealizados") val quizzesRealizados: Int,
    @SerializedName("promedioCorrectas") val promedioCorrectas: String,
    @SerializedName("experienciaGanada") val experienciaGanada: Int
)

data class DatosReporteTemaResponse(
    @SerializedName("cursoId") val cursoId: String,
    @SerializedName("cursoNombre") val cursoNombre: String,
    @SerializedName("temaId") val temaId: String,
    @SerializedName("temaNombre") val temaNombre: String,
    @SerializedName("totalEstudiantes") val totalEstudiantes: Int,
    @SerializedName("estudiantesConDatos") val estudiantesConDatos: Int,
    @SerializedName("estudiantes") val estudiantes: List<EstudianteTemaData>
)

data class EstudianteTemaData(
    @SerializedName("estudianteId") val estudianteId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("intentos") val intentos: Int,
    @SerializedName("mejorPuntaje") val mejorPuntaje: Int,
    @SerializedName("promedioTiempo") val promedioTiempo: String,
    @SerializedName("estado") val estado: String
)

data class DatosReporteGeneralResponse(
    @SerializedName("cursoId") val cursoId: String,
    @SerializedName("cursoNombre") val cursoNombre: String,
    @SerializedName("totalEstudiantes") val totalEstudiantes: Int,
    @SerializedName("estudiantesConDatos") val estudiantesConDatos: Int,
    @SerializedName("estudiantes") val estudiantes: List<EstudianteGeneralData>
)

data class EstudianteGeneralData(
    @SerializedName("estudianteId") val estudianteId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("totalQuizzes") val totalQuizzes: Int,
    @SerializedName("promedioGeneral") val promedioGeneral: String,
    @SerializedName("experienciaTotal") val experienciaTotal: Int,
    @SerializedName("temasCompletados") val temasCompletados: Int
)

data class DatosReporteRangoResponse(
    @SerializedName("cursoId") val cursoId: String,
    @SerializedName("cursoNombre") val cursoNombre: String,
    @SerializedName("fechaInicio") val fechaInicio: String,
    @SerializedName("fechaFin") val fechaFin: String,
    @SerializedName("totalEstudiantes") val totalEstudiantes: Int,
    @SerializedName("estudiantesConDatos") val estudiantesConDatos: Int,
    @SerializedName("estudiantes") val estudiantes: List<EstudianteRangoData>
)

data class EstudianteRangoData(
    @SerializedName("estudianteId") val estudianteId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("quizzesEnRango") val quizzesEnRango: Int,
    @SerializedName("promedioCorrectas") val promedioCorrectas: String,
    @SerializedName("experiencia") val experiencia: Int,
    @SerializedName("tiempoPromedio") val tiempoPromedio: String
)

data class DatosReporte(
    val encabezados: List<String>,
    val filas: List<Map<String, String>>,
    val cursoId: String,
    val cursoNombre: String,
    val temaId: String? = null,
    val temaNombre: String? = null,
    val fecha: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val totalRegistros: Int = 0,
    val registrosConDatos: Int = 0,
    val tipoReporte: TipoReporte
)