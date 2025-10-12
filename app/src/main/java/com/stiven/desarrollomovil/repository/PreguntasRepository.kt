package com.stiven.desarrollomovil.repository

import com.stiven.desarrollomovil.api.ApiClient
import com.stiven.desarrollomovil.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreguntasRepository {

    private val api = ApiClient.apiService

    // Generar preguntas con IA
    suspend fun generarPreguntasIA(
        cursoId: String,
        temaId: String,
        temaTexto: String,
        cantidad: Int = 5
    ): Result<PreguntasIAResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GenerarPreguntasRequest(
                cursoId = cursoId,
                temaId = temaId,
                temaTexto = temaTexto,
                cantidad = cantidad.toString()
            )

            val response = api.generarPreguntasIA(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener preguntas pendientes
    suspend fun obtenerPreguntasPendientes(
        cursoId: String,
        temaId: String
    ): Result<List<PreguntaIA>> = withContext(Dispatchers.IO) {
        try {
            val response = api.obtenerPreguntasPendientes(cursoId, temaId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Aprobar pregunta
    suspend fun aprobarPregunta(
        id: String,
        notas: String = ""
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RevisarPreguntaRequest(estado = "aprobada", notas = notas)
            val response = api.revisarPregunta(id, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al aprobar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Rechazar pregunta
    suspend fun rechazarPregunta(
        id: String,
        motivo: String
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RevisarPreguntaRequest(estado = "rechazada", notas = motivo)
            val response = api.revisarPregunta(id, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al rechazar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar pregunta
    suspend fun actualizarPregunta(
        id: String,
        pregunta: PreguntaIA
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.actualizarPregunta(id, pregunta)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eliminar pregunta
    suspend fun eliminarPregunta(id: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.eliminarPreguntaIA(id)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al eliminar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
