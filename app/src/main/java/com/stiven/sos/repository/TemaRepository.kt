package com.stiven.sos.repository

import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.Tema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TemaRepository {

    private val apiService = ApiClient.apiService

    suspend fun obtenerTemasPorCurso(
        cursoId: String,
        estado: String? = null
    ): ApiResult<List<Tema>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerTemasPorCurso(cursoId, estado)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al obtener temas")
        }
    }

    suspend fun obtenerTemaPorId(
        cursoId: String,
        temaId: String
    ): ApiResult<Tema> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerTemaPorId(cursoId, temaId)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Tema no encontrado")
            } else {
                ApiResult.Error("Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error desconocido al obtener el tema")
        }
    }
}