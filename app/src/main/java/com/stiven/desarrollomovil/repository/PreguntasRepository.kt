// Archivo: app/src/main/java/com/stiven/desarrollomovil/repository/PreguntasRepository.kt

package com.stiven.desarrollomovil.repository

import com.stiven.desarrollomovil.api.ApiClient
import com.stiven.desarrollomovil.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar todas las operaciones de datos relacionadas con las preguntas.
 * Actúa como una capa intermedia entre los ViewModels y la fuente de datos (API).
 */
class PreguntasRepository {

    // Instancia única del cliente de la API
    private val api = ApiClient.apiService

    /**
     * Solicita a la API la generación de nuevas preguntas basadas en el contenido de un tema.
     */
    suspend fun generarPreguntasIA(
        cursoId: String,
        temaId: String,
        temaTexto: String,
        cantidad: Int
    ): Result<PreguntasIAResponse> = withContext(Dispatchers.IO) {
        try {
            // --- ¡CORRECCIÓN! ---
            // El modelo 'GenerarPreguntasRequest' espera un Int para 'cantidad', no un String.
            val request = GenerarPreguntasRequest(
                cursoId = cursoId,
                temaId = temaId,
                temaTexto = temaTexto,
                cantidad = cantidad
            )
            val response = api.generarPreguntasIA(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al generar: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene las preguntas pendientes de validación para un tema específico.
     */
    suspend fun obtenerPreguntasPendientes(
        cursoId: String,
        temaId: String
    ): Result<List<PreguntaIA>> = withContext(Dispatchers.IO) {
        try {
            val response = api.obtenerPreguntasPendientes(cursoId, temaId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener pendientes: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * --- ¡NUEVA FUNCIÓN! ---
     * Obtiene TODAS las preguntas pendientes de TODOS los cursos.
     * Ideal para el contador del Panel del Docente.
     */
    suspend fun obtenerTodasLasPreguntasPendientes(): Result<List<PreguntaIA>> = withContext(Dispatchers.IO) {
        try {
            val response = api.obtenerTodasLasPreguntasPendientes()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener todas las preguntas: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca una pregunta como APROBADA.
     */
    suspend fun aprobarPregunta(
        id: String,
        notas: String = "Aprobada sin comentarios."
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            // --- ¡CORRECCIÓN! ---
            // Usamos el enum 'EstadoValidacion' para mayor seguridad y consistencia.
            val request = RevisarPreguntaRequest(estado = EstadoValidacion.APROBADA, notas = notas)
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

    /**
     * Marca una pregunta como RECHAZADA.
     */
    suspend fun rechazarPregunta(
        id: String,
        motivo: String
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            // --- ¡CORRECCIÓN! ---
            // Usamos el enum 'EstadoValidacion' también aquí.
            val request = RevisarPreguntaRequest(estado = EstadoValidacion.RECHAZADA, notas = motivo)
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


    suspend fun actualizarPregunta(
        id: String,
        preguntaEditada: PreguntaIA
    ): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            // --- ¡CORRECCIÓN! ---
            // Usamos el endpoint 'revisarPregunta' que es más robusto y está diseñado para esto.
            // Le pasamos la pregunta editada completa.
            val request = RevisarPreguntaRequest(
                estado = EstadoValidacion.APROBADA,
                notas = "Pregunta actualizada por el docente.",
                preguntaEditada = preguntaEditada
            )
            val response = api.revisarPregunta(id, request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina permanentemente una pregunta.
     */
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
