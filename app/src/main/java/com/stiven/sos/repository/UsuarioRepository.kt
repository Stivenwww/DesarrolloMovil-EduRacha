

package com.stiven.sos.repository

import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.UsuarioAsignado

class UsuarioRepository {

    private val api = ApiClient.apiService


    suspend fun obtenerEstudiantesPorCurso(cursoId: String): Result<List<UsuarioAsignado>> {
        return try {
            Log.d("UsuarioRepository", " Obteniendo estudiantes del curso: $cursoId")

            // Llamar al endpoint que AHORA devuelve directamente una lista de estudiantes
            val response = api.obtenerEstudiantesPorCurso(cursoId)

            if (response.isSuccessful) {

                val estudiantes = response.body()

                if (estudiantes != null) {
                    Log.d("UsuarioRepository", "Estudiantes obtenidos exitosamente")
                    Log.d("UsuarioRepository", "Total: ${estudiantes.size}")
                    Log.d("UsuarioRepository", "Datos: $estudiantes")

                    Result.success(estudiantes)
                } else {
                    // Esto ocurre si la respuesta es exitosa (código 200) pero el cuerpo está vacío.
                    Log.w("UsuarioRepository", "Respuesta exitosa pero el cuerpo es nulo.")
                    Result.success(emptyList()) // Devolver una lista vacía es un resultado seguro.
                }
            } else {
                // Gestionar respuestas no exitosas (códigos 4xx, 5xx)
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Error ${response.code()}: ${response.message()}\n$errorBody"

                Log.e("UsuarioRepository", " $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Gestionar excepciones de red (sin conexión, timeout, etc.) o de parsing JSON.
            Log.e("UsuarioRepository", " Excepción al obtener estudiantes: ${e.message}", e)
            Result.failure(e)
        }
    }
}
