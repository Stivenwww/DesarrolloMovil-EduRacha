package com.stiven.sos.repository

import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.ApiResponse
import com.stiven.sos.models.Curso
import com.stiven.sos.models.toRequest

class CursoRepository {

    private val api = ApiClient.apiService

    /**
     * Crear curso - Convierte Curso a CursoRequest (sin ID)
     */
    suspend fun crearCurso(curso: Curso): Result<ApiResponse> {
        return try {
            // Convertir Curso a CursoRequest (elimina el campo id)
            val cursoRequest = curso.toRequest()

            // Log para debug
            Log.d("CursoRepository", "Enviando: ${com.google.gson.Gson().toJson(cursoRequest)}")

            val response = api.crearCurso(cursoRequest)

            if (response.isSuccessful && response.body() != null) {
                Log.d("CursoRepository", "Curso creado exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("CursoRepository", "Error ${response.code()}: $errorBody")
                Result.failure(Exception("Error al crear curso: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "Error al crear curso", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerCursos(): Result<List<Curso>> {
        return try {
            val response = api.obtenerCursos()
            if (response.isSuccessful) {
                val lista = response.body() ?: emptyList()

                if (lista.isEmpty()) {
                    Log.w("CursoRepository", "No se encontraron cursos en el backend todavía.")
                    Result.success(emptyList())
                } else {
                    Result.success(lista)
                }
            } else {
                Result.failure(Exception("Error al obtener cursos: ${response.code()} ${response.message()}"))
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