package com.stiven.sos.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stiven.sos.api.ApiClient
import com.stiven.sos.api.ApiService
import com.stiven.sos.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReporteViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ReporteViewModel"
    private val apiService: ApiService = ApiClient.apiService

    private val _estadoReporteDiario = MutableStateFlow<ReporteEstado>(ReporteEstado.Idle)
    val estadoReporteDiario: StateFlow<ReporteEstado> = _estadoReporteDiario.asStateFlow()

    private val _estadoReporteTema = MutableStateFlow<ReporteEstado>(ReporteEstado.Idle)
    val estadoReporteTema: StateFlow<ReporteEstado> = _estadoReporteTema.asStateFlow()

    private val _estadoReporteGeneral = MutableStateFlow<ReporteEstado>(ReporteEstado.Idle)
    val estadoReporteGeneral: StateFlow<ReporteEstado> = _estadoReporteGeneral.asStateFlow()

    private val _estadoReporteRango = MutableStateFlow<ReporteEstado>(ReporteEstado.Idle)
    val estadoReporteRango: StateFlow<ReporteEstado> = _estadoReporteRango.asStateFlow()

    private val _datosPreview = MutableStateFlow<DatosReporte?>(null)
    val datosPreview: StateFlow<DatosReporte?> = _datosPreview.asStateFlow()

    private val _cargandoPreview = MutableStateFlow(false)
    val cargandoPreview: StateFlow<Boolean> = _cargandoPreview.asStateFlow()

    private val _errorPreview = MutableStateFlow<String?>(null)
    val errorPreview: StateFlow<String?> = _errorPreview.asStateFlow()

    fun obtenerDatosPreviewDiario(cursoId: String, fecha: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, " Obteniendo vista previa - Diario")
                _cargandoPreview.value = true
                _errorPreview.value = null

                val response = apiService.obtenerVistaPreviewReporteDiario(cursoId, fecha)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _datosPreview.value = parsearDatosReporteDiario(data, cursoId, fecha)
                } else {
                    manejarErrorRespuesta(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Excepción: ${e.message}", e)
                _errorPreview.value = e.message ?: "Error al obtener datos"
            } finally {
                _cargandoPreview.value = false
            }
        }
    }

    fun obtenerDatosPreviewTema(cursoId: String, temaId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, " Obteniendo vista previa - Tema")
                _cargandoPreview.value = true
                _errorPreview.value = null

                val response = apiService.obtenerVistaPreviewReporteTema(cursoId, temaId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _datosPreview.value = parsearDatosReporteTema(data, cursoId, temaId)
                } else {
                    manejarErrorRespuesta(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Excepción: ${e.message}", e)
                _errorPreview.value = e.message ?: "Error al obtener datos"
            } finally {
                _cargandoPreview.value = false
            }
        }
    }

    fun obtenerDatosPreviewGeneral(cursoId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, " Obteniendo vista previa - General")
                _cargandoPreview.value = true
                _errorPreview.value = null

                val response = apiService.obtenerVistaPreviewReporteGeneral(cursoId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _datosPreview.value = parsearDatosReporteGeneral(data, cursoId)
                } else {
                    manejarErrorRespuesta(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Excepción: ${e.message}", e)
                _errorPreview.value = e.message ?: "Error al obtener datos"
            } finally {
                _cargandoPreview.value = false
            }
        }
    }

    fun obtenerDatosPreviewRango(cursoId: String, desde: String, hasta: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, " Obteniendo vista previa - Rango")
                _cargandoPreview.value = true
                _errorPreview.value = null

                val response = apiService.obtenerVistaPreviewReporteRango(cursoId, desde, hasta)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _datosPreview.value = parsearDatosReporteRango(data, cursoId, desde, hasta)
                } else {
                    manejarErrorRespuesta(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Excepción: ${e.message}", e)
                _errorPreview.value = e.message ?: "Error al obtener datos"
            } finally {
                _cargandoPreview.value = false
            }
        }
    }

    private fun parsearDatosReporteDiario(data: DatosReporteDiarioResponse, cursoId: String, fecha: String): DatosReporte {
        val encabezados = listOf("Estudiante ID", "Nombre", "Quizzes Realizados", "Promedio Correctas", "Experiencia Ganada")
        val filas = data.estudiantes.map { est ->
            mapOf(
                "Estudiante ID" to est.estudianteId,
                "Nombre" to est.nombre,
                "Quizzes Realizados" to est.quizzesRealizados.toString(),
                "Promedio Correctas" to est.promedioCorrectas,
                "Experiencia Ganada" to est.experienciaGanada.toString()
            )
        }

        return DatosReporte(encabezados, filas, cursoId, data.cursoNombre, null, null, fecha, null, null, data.totalEstudiantes, data.estudiantesConDatos, TipoReporte.DIARIO)
    }

    private fun parsearDatosReporteTema(data: DatosReporteTemaResponse, cursoId: String, temaId: String): DatosReporte {
        val encabezados = listOf("Nombre", "Intentos", "Mejor Puntaje", "Promedio Tiempo (seg)", "Estado")
        val filas = data.estudiantes.map { est ->
            mapOf(
                "Nombre" to est.nombre,
                "Intentos" to est.intentos.toString(),
                "Mejor Puntaje" to est.mejorPuntaje.toString(),
                "Promedio Tiempo (seg)" to est.promedioTiempo,
                "Estado" to est.estado
            )
        }

        return DatosReporte(encabezados, filas, cursoId, data.cursoNombre, temaId, data.temaNombre, null, null, null, data.totalEstudiantes, data.estudiantesConDatos, TipoReporte.TEMA)
    }

    private fun parsearDatosReporteGeneral(data: DatosReporteGeneralResponse, cursoId: String): DatosReporte {
        val encabezados = listOf("Nombre", "Total Quizzes", "Promedio General", "Experiencia Total", "Temas Completados")
        val filas = data.estudiantes.map { est ->
            mapOf(
                "Nombre" to est.nombre,
                "Total Quizzes" to est.totalQuizzes.toString(),
                "Promedio General" to est.promedioGeneral,
                "Experiencia Total" to est.experienciaTotal.toString(),
                "Temas Completados" to est.temasCompletados.toString()
            )
        }

        return DatosReporte(encabezados, filas, cursoId, data.cursoNombre, null, null, null, null, null, data.totalEstudiantes, data.estudiantesConDatos, TipoReporte.GENERAL)
    }

    private fun parsearDatosReporteRango(data: DatosReporteRangoResponse, cursoId: String, desde: String, hasta: String): DatosReporte {
        val encabezados = listOf("Nombre", "Quizzes en Rango", "Promedio Correctas", "Experiencia", "Tiempo Promedio (seg)")
        val filas = data.estudiantes.map { est ->
            mapOf(
                "Nombre" to est.nombre,
                "Quizzes en Rango" to est.quizzesEnRango.toString(),
                "Promedio Correctas" to est.promedioCorrectas,
                "Experiencia" to est.experiencia.toString(),
                "Tiempo Promedio (seg)" to est.tiempoPromedio
            )
        }

        return DatosReporte(encabezados, filas, cursoId, data.cursoNombre, null, null, null, desde, hasta, data.totalEstudiantes, data.estudiantesConDatos, TipoReporte.RANGO)
    }

    fun descargarReporteDiario(cursoId: String, fecha: String) {
        descargarReporte(cursoId, TipoReporte.DIARIO, _estadoReporteDiario) {
            apiService.descargarReporteDiario(cursoId, fecha)
        }
    }

    fun descargarReporteTema(cursoId: String, temaId: String) {
        descargarReporte(cursoId, TipoReporte.TEMA, _estadoReporteTema) {
            apiService.descargarReporteTema(cursoId, temaId)
        }
    }

    fun descargarReporteGeneral(cursoId: String) {
        descargarReporte(cursoId, TipoReporte.GENERAL, _estadoReporteGeneral) {
            apiService.descargarReporteGeneral(cursoId)
        }
    }

    fun descargarReporteRango(cursoId: String, desde: String, hasta: String) {
        descargarReporte(cursoId, TipoReporte.RANGO, _estadoReporteRango) {
            apiService.descargarReporteRango(cursoId, desde, hasta)
        }
    }

    private fun descargarReporte(
        cursoId: String,
        tipo: TipoReporte,
        estadoFlow: MutableStateFlow<ReporteEstado>,
        apiCall: suspend () -> retrofit2.Response<okhttp3.ResponseBody>
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, " Iniciando descarga - Tipo: $tipo")
                estadoFlow.value = ReporteEstado.Descargando

                val response = apiCall()

                if (response.isSuccessful && response.body() != null) {
                    val excelBytes = response.body()!!.byteStream().readBytes()

                    if (excelBytes.isEmpty()) {
                        estadoFlow.value = ReporteEstado.Error("El archivo generado está vacío")
                        return@launch
                    }

                    if (!esArchivoExcelValido(excelBytes)) {
                        estadoFlow.value = ReporteEstado.Error("El archivo no es un Excel válido")
                        return@launch
                    }

                    val nombreArchivo = generarNombreArchivo(tipo, cursoId)

                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        guardarArchivoModerno(excelBytes, nombreArchivo)
                    } else {
                        guardarArchivoLegacy(excelBytes, nombreArchivo)
                    }

                    if (uri != null) {
                        estadoFlow.value = ReporteEstado.Exito(nombreArchivo)
                    } else {
                        estadoFlow.value = ReporteEstado.Error("Error al guardar el archivo")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    val mensajeError = try {
                        JSONObject(errorBody).getString("error")
                    } catch (e: Exception) {
                        when (response.code()) {
                            404 -> "El endpoint no existe"
                            401 -> "No autorizado"
                            403 -> "No tienes permisos"
                            else -> "Error ${response.code()}"
                        }
                    }
                    estadoFlow.value = ReporteEstado.Error(mensajeError)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Excepción al descargar reporte", e)
                estadoFlow.value = ReporteEstado.Error(e.message ?: "Error de conexión")
            }
        }
    }

    private fun generarNombreArchivo(tipo: TipoReporte, cursoId: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        return when (tipo) {
            TipoReporte.DIARIO -> "reporte_diario_${cursoId}_${timestamp}.xlsx"
            TipoReporte.TEMA -> "reporte_tema_${cursoId}_${timestamp}.xlsx"
            TipoReporte.GENERAL -> "reporte_general_${cursoId}_${timestamp}.xlsx"
            TipoReporte.RANGO -> "reporte_rango_${cursoId}_${timestamp}.xlsx"
        }
    }

    private fun esArchivoExcelValido(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    private fun guardarArchivoModerno(bytes: ByteArray, nombreArchivo: String): android.net.Uri? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    output.write(bytes)
                    output.flush()
                }
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando archivo moderno", e)
            null
        }
    }

    private fun guardarArchivoLegacy(bytes: ByteArray, nombreArchivo: String): android.net.Uri? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = java.io.File(downloadsDir, nombreArchivo)
            file.writeBytes(bytes)
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando archivo legacy", e)
            null
        }
    }

    private fun <T> manejarErrorRespuesta(response: retrofit2.Response<T>) {
        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
        val mensajeError = try {
            JSONObject(errorMsg).getString("error")
        } catch (e: Exception) {
            when (response.code()) {
                404 -> "Endpoint no encontrado"
                401 -> "No autorizado"
                403 -> "No tienes permisos"
                else -> "Error ${response.code()}"
            }
        }
        _errorPreview.value = mensajeError
    }

    fun resetearEstadoDiario() { _estadoReporteDiario.value = ReporteEstado.Idle }
    fun resetearEstadoTema() { _estadoReporteTema.value = ReporteEstado.Idle }
    fun resetearEstadoGeneral() { _estadoReporteGeneral.value = ReporteEstado.Idle }
    fun resetearEstadoRango() { _estadoReporteRango.value = ReporteEstado.Idle }
    fun resetearPreview() {
        _datosPreview.value = null
        _errorPreview.value = null
        _cargandoPreview.value = false
    }
}