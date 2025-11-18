package com.stiven.sos.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.stiven.sos.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ReporteRepository(
    private val apiService: ApiService,
    private val context: Context
) {

    companion object {
        private const val TAG = "ReporteRepository"
        private const val MIME_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    suspend fun descargarReporteDiario(cursoId: String, fecha: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Descargando reporte diario: cursoId=$cursoId, fecha=$fecha")
            val response = apiService.descargarReporteDiario(cursoId, fecha)

            if (!response.isSuccessful) {
                val errorMsg = "Error ${response.code()}: ${response.message()}"
                Log.e(TAG, " $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body == null) {
                Log.e(TAG, " Body vacío en respuesta")
                return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
            }

            val nombreArchivo = "reporte_diario_${cursoId}_${fecha}.xlsx"
            Log.d(TAG, "💾 Guardando archivo: $nombreArchivo")

            val resultado = guardarArchivo(body, nombreArchivo)

            if (resultado) {
                Log.d(TAG, " Reporte guardado exitosamente: $nombreArchivo")
                Result.success(nombreArchivo)
            } else {
                Result.failure(Exception("Error al guardar el archivo"))
            }

        } catch (e: Exception) {
            Log.e(TAG, " Error descargando reporte diario", e)
            Result.failure(e)
        }
    }

    suspend fun descargarReporteTema(cursoId: String, temaId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, " Descargando reporte por tema: cursoId=$cursoId, temaId=$temaId")
            val response = apiService.descargarReporteTema(cursoId, temaId)

            if (!response.isSuccessful) {
                val errorMsg = "Error ${response.code()}: ${response.message()}"
                Log.e(TAG, " $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body == null) {
                Log.e(TAG, " Body vacío en respuesta")
                return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
            }

            val nombreArchivo = "reporte_tema_${temaId}_${System.currentTimeMillis()}.xlsx"
            Log.d(TAG, " Guardando archivo: $nombreArchivo")

            val resultado = guardarArchivo(body, nombreArchivo)

            if (resultado) {
                Log.d(TAG, " Reporte guardado exitosamente: $nombreArchivo")
                Result.success(nombreArchivo)
            } else {
                Result.failure(Exception("Error al guardar el archivo"))
            }

        } catch (e: Exception) {
            Log.e(TAG, " Error descargando reporte por tema", e)
            Result.failure(e)
        }
    }

    private fun guardarArchivo(body: ResponseBody, nombreArchivo: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                guardarConMediaStore(body, nombreArchivo)
            } else {
                guardarEnDownloads(body, nombreArchivo)
            }
        } catch (e: Exception) {
            Log.e(TAG, " Error guardando archivo", e)
            false
        }
    }

    private fun guardarConMediaStore(body: ResponseBody, nombreArchivo: String): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
            put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE_EXCEL)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false

        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                copiarArchivo(body, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Log.d(TAG, " Archivo guardado en MediaStore: $uri")
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Log.e(TAG, " Error en MediaStore", e)
            false
        }
    }

    private fun guardarEnDownloads(body: ResponseBody, nombreArchivo: String): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, nombreArchivo)

        return try {
            FileOutputStream(file).use { outputStream ->
                copiarArchivo(body, outputStream)
            }
            Log.d(TAG, " Archivo guardado en: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, " Error guardando en Downloads", e)
            false
        }
    }

    private fun copiarArchivo(body: ResponseBody, outputStream: OutputStream) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytes = 0L

        body.byteStream().use { inputStream ->
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
        }

        Log.d(TAG, " Total bytes escritos: $totalBytes")
    }
}