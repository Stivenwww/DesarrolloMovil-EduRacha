package com.stiven.sos.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Prepara un PDF elegido por el docente para enviarlo al servidor.
 * La lectura del texto se hace en el backend (PDFBox); aquí solo se valida tamaño y tipo.
 */
object ArchivosPdf {
    const val TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024

    fun nombre(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i != -1 && c.moveToFirst()) c.getString(i) else null
        } ?: uri.lastPathSegment ?: "documento.pdf"

    /** Devuelve la parte multipart lista para enviar, o un mensaje de error claro. */
    suspend fun prepararParte(context: Context, uri: Uri, campo: String = "archivo"): Result<MultipartBody.Part> =
        withContext(Dispatchers.IO) {
            try {
                val nombreArchivo = nombre(context, uri)
                val tipo = context.contentResolver.getType(uri)
                if (tipo != null && tipo != "application/pdf" && !nombreArchivo.endsWith(".pdf", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("El archivo elegido no es un PDF."))
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo."))
                if (bytes.isEmpty()) return@withContext Result.failure(Exception("El archivo está vacío."))
                if (bytes.size > TAMANO_MAXIMO_BYTES) {
                    return@withContext Result.failure(Exception("El PDF supera el tamaño máximo de 20 MB."))
                }
                val cuerpo = bytes.toRequestBody("application/pdf".toMediaType())
                Result.success(MultipartBody.Part.createFormData(campo, nombreArchivo, cuerpo))
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo leer el archivo: ${e.message}"))
            }
        }

    fun texto(valor: String) = valor.toRequestBody("text/plain".toMediaType())
}
