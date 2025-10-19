// Archivo: app/src/main/java/com/stiven/sos/repository/PreguntaRepository.kt

package com.stiven.sos.repository

import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

class PreguntaRepository {

    private val apiService = ApiClient.apiService

    suspend fun obtenerPreguntas(
        cursoId: String? = null,
        estado: String? = null
    ): ApiResult<List<Pregunta>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerPreguntas(cursoId, estado)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al obtener preguntas")
        }
    }

    suspend fun obtenerPreguntaPorId(id: String): ApiResult<Pregunta> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerPreguntaPorId(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Pregunta no encontrada")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al obtener la pregunta")
        }
    }

    suspend fun crearPregunta(pregunta: Pregunta): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.crearPregunta(pregunta)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it.id ?: "")
                } ?: ApiResult.Error("No se recibió ID de la pregunta creada")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al crear la pregunta")
        }
    }

    suspend fun actualizarPregunta(id: String, pregunta: Pregunta): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.actualizarPregunta(id, pregunta)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()?.message ?: "Pregunta actualizada")
                } else {
                    ApiResult.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Error desconocido al actualizar la pregunta")
            }
        }

    suspend fun eliminarPregunta(id: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarPregunta(id)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.message ?: "Pregunta eliminada")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al eliminar la pregunta")
        }
    }

    suspend fun actualizarEstadoPregunta(
        id: String,
        estado: String,
        notas: String? = null
    ): ApiResult<EstadoUpdateResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ActualizarEstadoRequest(estado, notas)
            val response = apiService.actualizarEstadoPregunta(id, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("No se recibió respuesta del servidor")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al actualizar el estado")
        }
    }

    suspend fun generarPreguntasIA(
        cursoId: String,
        temaId: String,
        temaTexto: String,
        cantidad: Int = 5
    ): ApiResult<PreguntasIAResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GenerarPreguntasRequest(cursoId, temaId, temaTexto, cantidad)
            val response = apiService.generarPreguntasIA(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("No se recibieron preguntas generadas")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al generar preguntas con IA")
        }
    }

    suspend fun limpiarCache(): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.limpiarCachePreguntas()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.message ?: "Caché limpiada correctamente")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al limpiar caché")
        }
    }
}