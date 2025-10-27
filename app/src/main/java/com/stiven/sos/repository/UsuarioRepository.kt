package com.stiven.sos.repository

import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.UsuarioAsignado

class UsuarioRepository {

    private val api = ApiClient.apiService


    suspend fun obtenerEstudiantesPorCurso(cursoId: String): Result<List<UsuarioAsignado>> {
        return try {
            Log.d("UsuarioRepository", "📡 Obteniendo estudiantes del curso: $cursoId")

            val response = api.obtenerEstudiantesPorCurso(cursoId)

            if (response.isSuccessful) {
                val estudiantes = response.body() ?: emptyList()

                Log.d("UsuarioRepository", " Estudiantes obtenidos exitosamente")
                Log.d("UsuarioRepository", " Total: ${estudiantes.size}")

                // Verificar que los UIDs no estén vacíos
                estudiantes.forEach { estudiante ->
                    Log.d("UsuarioRepository", "👤 ${estudiante.nombre} - UID: ${estudiante.uid} - Correo: ${estudiante.correo}")
                }

                Result.success(estudiantes)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Error ${response.code()}: ${response.message()}\n$errorBody"

                Log.e("UsuarioRepository", " $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("UsuarioRepository", " Excepción: ${e.message}", e)
            Result.failure(e)
        }
    }
}
