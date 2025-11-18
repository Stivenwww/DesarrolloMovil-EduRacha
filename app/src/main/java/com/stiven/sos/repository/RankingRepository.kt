package com.stiven.sos.repository


import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.models.toUIModel

/**
 * Repositorio para gestionar las operaciones de ranking
 */
class RankingRepository {

    private val apiService = ApiClient.apiService

    /**
     * Obtener ranking por experiencia (por defecto)
     */
    suspend fun obtenerRankingPorExperiencia(cursoId: String): Result<List<RankingEstudiante>> {
        return try {
            Log.d("RankingRepository", "Obteniendo ranking por experiencia: $cursoId")

            val response = apiService.obtenerRankingPorExperiencia(cursoId)

            if (response.isSuccessful && response.body() != null) {
                val ranking = response.body()!!.map { it.toUIModel() }
                Log.d("RankingRepository", "Ranking obtenido: ${ranking.size} estudiantes")
                Result.success(ranking)
            } else {
                val error = "Error ${response.code()}: ${response.message()}"
                Log.e("RankingRepository", " $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("RankingRepository", " Error al obtener ranking: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking por racha
     */
    suspend fun obtenerRankingPorRacha(cursoId: String): Result<List<RankingEstudiante>> {
        return try {
            Log.d("RankingRepository", " Obteniendo ranking por racha: $cursoId")

            val response = apiService.obtenerRankingPorRacha(cursoId)

            if (response.isSuccessful && response.body() != null) {
                val ranking = response.body()!!.map { it.toUIModel() }
                Log.d("RankingRepository", " Ranking por racha: ${ranking.size} estudiantes")
                Result.success(ranking)
            } else {
                val error = "Error ${response.code()}: ${response.message()}"
                Log.e("RankingRepository", " $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("RankingRepository", "Error al obtener ranking por racha: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking por vidas
     */
    suspend fun obtenerRankingPorVidas(cursoId: String): Result<List<RankingEstudiante>> {
        return try {
            Log.d("RankingRepository", " Obteniendo ranking por vidas: $cursoId")

            val response = apiService.obtenerRankingPorVidas(cursoId)

            if (response.isSuccessful && response.body() != null) {
                val ranking = response.body()!!.map { it.toUIModel() }
                Log.d("RankingRepository", " Ranking por vidas: ${ranking.size} estudiantes")
                Result.success(ranking)
            } else {
                val error = "Error ${response.code()}: ${response.message()}"
                Log.e("RankingRepository", " $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("RankingRepository", " Error al obtener ranking por vidas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking general (todos los cursos)
     */
    suspend fun obtenerRankingGeneral(filtro: String = "experiencia"): Result<List<RankingEstudiante>> {
        return try {
            Log.d("RankingRepository", " Obteniendo ranking general: $filtro")

            val response = if (filtro == "experiencia") {
                apiService.obtenerRankingGeneral()
            } else {
                apiService.obtenerRankingGeneralConFiltro(filtro)
            }

            if (response.isSuccessful && response.body() != null) {
                val ranking = response.body()!!.map { it.toUIModel() }
                Log.d("RankingRepository", " Ranking general: ${ranking.size} estudiantes")
                Result.success(ranking)
            } else {
                val error = "Error ${response.code()}: ${response.message()}"
                Log.e("RankingRepository", " $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("RankingRepository", " Error al obtener ranking general: ${e.message}", e)
            Result.failure(e)
        }
    }
}