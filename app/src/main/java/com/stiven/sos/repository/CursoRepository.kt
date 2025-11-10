package com.stiven.sos.repository

import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.ApiResponse
import com.stiven.sos.models.Curso
import com.stiven.sos.models.toRequest

class CursoRepository {

    private val api = ApiClient.apiService

    /**
     * Crear curso - Convierte Curso a CursoRequest (sin ID y usa TemaRequest)
     */
    suspend fun crearCurso(curso: Curso): Result<ApiResponse> {
        return try {
            // Convertir Curso a CursoRequest (elimina el campo id y convierte Tema a TemaRequest)
            val cursoRequest = curso.toRequest()

            // Log para debug
            Log.d("CursoRepository", "Enviando curso:")
            Log.d("CursoRepository", "- Titulo: ${cursoRequest.titulo}")
            Log.d("CursoRepository", "- Codigo: ${cursoRequest.codigo}")
            Log.d("CursoRepository", "- DocenteId: ${cursoRequest.docenteId}")
            Log.d("CursoRepository", "- Total temas: ${cursoRequest.temas?.size ?: 0}")

            val response = api.crearCurso(cursoRequest)

            if (response.isSuccessful && response.body() != null) {
                Log.d("CursoRepository", "✅ Curso creado exitosamente")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("CursoRepository", "❌ Error ${response.code()}: $errorBody")
                Result.failure(Exception("Error al crear curso: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "❌ Excepción al crear curso", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener todos los cursos
     */
    suspend fun obtenerCursos(): Result<List<Curso>> {
        return try {
            val response = api.obtenerCursos()
            if (response.isSuccessful) {
                val lista = response.body() ?: emptyList()

                if (lista.isEmpty()) {
                    Log.w("CursoRepository", "No se encontraron cursos en el backend todavía.")
                    Result.success(emptyList())
                } else {
                    Log.d("CursoRepository", "✅ ${lista.size} curso(s) obtenido(s)")
                    Result.success(lista)
                }
            } else {
                Log.e("CursoRepository", "❌ Error al obtener cursos: ${response.code()}")
                Result.failure(Exception("Error al obtener cursos: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "❌ Excepción al obtener cursos", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener curso por ID
     */
    suspend fun obtenerCursoPorId(id: String): Result<Curso> {
        return try {
            val response = api.obtenerCursoPorId(id)
            if (response.isSuccessful && response.body() != null) {
                Log.d("CursoRepository", "✅ Curso obtenido: ${response.body()!!.titulo}")
                Result.success(response.body()!!)
            } else {
                Log.e("CursoRepository", "❌ Error al obtener curso: ${response.code()}")
                Result.failure(Exception("Error al obtener curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "❌ Excepción al obtener curso por id", e)
            Result.failure(e)
        }
    }

    /**
     * Actualizar curso existente
     */
    suspend fun actualizarCurso(id: String, curso: Curso): Result<ApiResponse> {
        return try {
            val response = api.actualizarCurso(id, curso)
            if (response.isSuccessful && response.body() != null) {
                Log.d("CursoRepository", "✅ Curso actualizado exitosamente")
                Result.success(response.body()!!)
            } else {
                Log.e("CursoRepository", "❌ Error al actualizar curso: ${response.code()}")
                Result.failure(Exception("Error al actualizar curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "❌ Excepción al actualizar curso", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar curso
     */
    suspend fun eliminarCurso(id: String): Result<ApiResponse> {
        return try {
            val response = api.eliminarCurso(id)
            if (response.isSuccessful && response.body() != null) {
                Log.d("CursoRepository", "✅ Curso eliminado exitosamente")
                Result.success(response.body()!!)
            } else {
                Log.e("CursoRepository", "❌ Error al eliminar curso: ${response.code()}")
                Result.failure(Exception("Error al eliminar curso: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CursoRepository", "❌ Excepción al eliminar curso", e)
            Result.failure(e)
        }
    }
}