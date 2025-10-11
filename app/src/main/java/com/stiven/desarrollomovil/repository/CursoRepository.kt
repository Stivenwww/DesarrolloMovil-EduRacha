package com.stiven.desarrollomovil.repository

import android.util.Log
import com.stiven.desarrollomovil.api.ApiClient
import com.stiven.desarrollomovil.models.ApiResponse
import com.stiven.desarrollomovil.models.Curso

class CursoRepository {

    private val api = ApiClient.apiService

    suspend fun crearCurso(curso: Curso): Result<ApiResponse> {
        return try {
            val response = api.crearCurso(curso)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al crear curso", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerCursos(): Result<List<Curso>> {
        return try {
            val response = api.obtenerCursos()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener cursos: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al obtener cursos", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerCursoPorId(id: String): Result<Curso> {
        return try {
            val response = api.obtenerCursoPorId(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al obtener curso por id", e)
            Result.failure(e)
        }
    }

    suspend fun actualizarCurso(id: String, curso: Curso): Result<ApiResponse> {
        return try {
            val response = api.actualizarCurso(id, curso)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al actualizar curso", e)
            Result.failure(e)
        }
    }

    suspend fun eliminarCurso(id: String): Result<ApiResponse> {
        return try {
            val response = api.eliminarCurso(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al eliminar curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al eliminar curso", e)
            Result.failure(e)
        }
    }
}