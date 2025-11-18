package com.stiven.sos.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Modelo de Explicacion
 */
data class Explicacion(
    val id: String = "",
    val temaId: String = "",
    val cursoId: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val orden: Int = 0,
    val tipo: String = "texto", // texto, video, imagen, etc.
    val urlRecurso: String? = null,
    val duracionEstimada: Int = 0, // en minutos
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val activo: Boolean = true,
    val vistas: Int = 0
)

/**
 * Repository para CRUD de Explicaciones
 * Implementa operaciones con Firebase Realtime Database
 */
class ExplicacionRepository {

    private val TAG = "ExplicacionRepository"
    private val database = FirebaseDatabase.getInstance()
    private val explicacionesRef = database.getReference("explicaciones")

    /**
     * Crea una nueva explicación
     * @param explicacion Objeto Explicacion a crear
     * @return Result con el ID generado o error
     */
    suspend fun crearExplicacion(explicacion: Explicacion): Result<String> {
        return try {
            Log.d(TAG, "Creando explicación para tema: ${explicacion.temaId}")

            // Generar ID único
            val nuevoId = explicacionesRef.push().key
                ?: return Result.failure(Exception("Error al generar ID"))

            // Crear objeto con ID
            val explicacionConId = explicacion.copy(
                id = nuevoId,
                fechaCreacion = System.currentTimeMillis(),
                fechaActualizacion = System.currentTimeMillis()
            )

            // Guardar en Firebase
            explicacionesRef.child(nuevoId).setValue(explicacionConId).await()

            // Crear índice por tema para búsqueda rápida
            database.getReference("explicaciones_por_tema")
                .child(explicacion.temaId)
                .child(nuevoId)
                .setValue(true)
                .await()

            // Crear índice por curso
            database.getReference("explicaciones_por_curso")
                .child(explicacion.cursoId)
                .child(nuevoId)
                .setValue(true)
                .await()

            Log.d(TAG, "Explicación creada exitosamente: $nuevoId")
            Result.success(nuevoId)

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear explicación", e)
            Result.failure(Exception("Error al crear explicación: ${e.message}"))
        }
    }

    /**
     * Obtiene una explicación por su ID
     * @param explicacionId ID de la explicación
     * @return Result con la Explicacion o error
     */
    suspend fun obtenerExplicacion(explicacionId: String): Result<Explicacion> {
        return try {
            Log.d(TAG, "Obteniendo explicación: $explicacionId")

            val snapshot = explicacionesRef.child(explicacionId).get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Explicación no encontrada"))
            }

            val explicacion = snapshot.getValue(Explicacion::class.java)
                ?: return Result.failure(Exception("Error al parsear explicación"))

            Log.d(TAG, "Explicación obtenida: ${explicacion.titulo}")
            Result.success(explicacion)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener explicación", e)
            Result.failure(Exception("Error al obtener explicación: ${e.message}"))
        }
    }

    /**
     * Obtiene todas las explicaciones de un tema
     * @param temaId ID del tema
     * @return Result con lista de Explicaciones o error
     */
    suspend fun obtenerExplicacionesPorTema(temaId: String): Result<List<Explicacion>> {
        return try {
            Log.d(TAG, "Obteniendo explicaciones del tema: $temaId")

            // Obtener IDs de explicaciones del tema
            val idsSnapshot = database.getReference("explicaciones_por_tema")
                .child(temaId)
                .get()
                .await()

            if (!idsSnapshot.exists()) {
                Log.d(TAG, "No hay explicaciones para el tema: $temaId")
                return Result.success(emptyList())
            }

            val explicaciones = mutableListOf<Explicacion>()

            // Obtener cada explicación
            for (childSnapshot in idsSnapshot.children) {
                val explicacionId = childSnapshot.key ?: continue
                val explicacionSnapshot = explicacionesRef.child(explicacionId).get().await()

                explicacionSnapshot.getValue(Explicacion::class.java)?.let { explicacion ->
                    if (explicacion.activo) {
                        explicaciones.add(explicacion)
                    }
                }
            }

            // Ordenar por orden
            val explicacionesOrdenadas = explicaciones.sortedBy { it.orden }

            Log.d(TAG, "Explicaciones obtenidas: ${explicacionesOrdenadas.size}")
            Result.success(explicacionesOrdenadas)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener explicaciones por tema", e)
            Result.failure(Exception("Error al obtener explicaciones: ${e.message}"))
        }
    }

    /**
     * Obtiene todas las explicaciones de un curso
     * @param cursoId ID del curso
     * @return Result con lista de Explicaciones o error
     */
    suspend fun obtenerExplicacionesPorCurso(cursoId: String): Result<List<Explicacion>> {
        return try {
            Log.d(TAG, "Obteniendo explicaciones del curso: $cursoId")

            val idsSnapshot = database.getReference("explicaciones_por_curso")
                .child(cursoId)
                .get()
                .await()

            if (!idsSnapshot.exists()) {
                return Result.success(emptyList())
            }

            val explicaciones = mutableListOf<Explicacion>()

            for (childSnapshot in idsSnapshot.children) {
                val explicacionId = childSnapshot.key ?: continue
                val explicacionSnapshot = explicacionesRef.child(explicacionId).get().await()

                explicacionSnapshot.getValue(Explicacion::class.java)?.let { explicacion ->
                    if (explicacion.activo) {
                        explicaciones.add(explicacion)
                    }
                }
            }

            Log.d(TAG, "Explicaciones del curso: ${explicaciones.size}")
            Result.success(explicaciones)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener explicaciones por curso", e)
            Result.failure(Exception("Error al obtener explicaciones: ${e.message}"))
        }
    }

    /**
     * Actualiza una explicación existente
     * @param explicacionId ID de la explicación
     * @param actualizaciones Mapa con los campos a actualizar
     * @return Result con éxito o error
     */
    suspend fun actualizarExplicacion(
        explicacionId: String,
        actualizaciones: Map<String, Any>
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Actualizando explicación: $explicacionId")

            // Verificar que la explicación existe
            val snapshot = explicacionesRef.child(explicacionId).get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Explicación no encontrada"))
            }

            // Agregar fecha de actualización
            val actualizacionesCompletas = actualizaciones.toMutableMap()
            actualizacionesCompletas["fechaActualizacion"] = System.currentTimeMillis()

            // Actualizar en Firebase
            explicacionesRef.child(explicacionId)
                .updateChildren(actualizacionesCompletas)
                .await()

            Log.d(TAG, "Explicación actualizada exitosamente")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar explicación", e)
            Result.failure(Exception("Error al actualizar explicación: ${e.message}"))
        }
    }

    /**
     * Elimina una explicación (soft delete)
     * @param explicacionId ID de la explicación
     * @return Result con éxito o error
     */
    suspend fun eliminarExplicacion(explicacionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando explicación: $explicacionId")

            // Soft delete - marcar como inactivo
            val actualizaciones = mapOf(
                "activo" to false,
                "fechaActualizacion" to System.currentTimeMillis()
            )

            explicacionesRef.child(explicacionId)
                .updateChildren(actualizaciones)
                .await()

            Log.d(TAG, "Explicación eliminada exitosamente")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar explicación", e)
            Result.failure(Exception("Error al eliminar explicación: ${e.message}"))
        }
    }

    /**
     * Elimina permanentemente una explicación (hard delete)
     * @param explicacionId ID de la explicación
     * @return Result con éxito o error
     */
    suspend fun eliminarExplicacionPermanente(explicacionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando permanentemente explicación: $explicacionId")

            // Obtener datos de la explicación antes de eliminar
            val snapshot = explicacionesRef.child(explicacionId).get().await()
            val explicacion = snapshot.getValue(Explicacion::class.java)
                ?: return Result.failure(Exception("Explicación no encontrada"))

            // Eliminar de Firebase
            explicacionesRef.child(explicacionId).removeValue().await()

            // Eliminar índices
            database.getReference("explicaciones_por_tema")
                .child(explicacion.temaId)
                .child(explicacionId)
                .removeValue()
                .await()

            database.getReference("explicaciones_por_curso")
                .child(explicacion.cursoId)
                .child(explicacionId)
                .removeValue()
                .await()

            Log.d(TAG, "Explicación eliminada permanentemente")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar permanentemente", e)
            Result.failure(Exception("Error al eliminar: ${e.message}"))
        }
    }

    /**
     * Incrementa el contador de vistas de una explicación
     * @param explicacionId ID de la explicación
     * @return Result con éxito o error
     */
    suspend fun incrementarVistas(explicacionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Incrementando vistas: $explicacionId")

            explicacionesRef.child(explicacionId)
                .child("vistas")
                .runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val vistas = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = vistas + 1
                        return com.google.firebase.database.Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            Log.e(TAG, "Error en transacción de vistas: ${error.message}")
                        } else {
                            Log.d(TAG, "Vistas incrementadas")
                        }
                    }
                })

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al incrementar vistas", e)
            Result.failure(Exception("Error al incrementar vistas: ${e.message}"))
        }
    }

    /**
     * Observa cambios en las explicaciones de un tema en tiempo real
     * @param temaId ID del tema
     * @param onExplicacionesActualizadas Callback con la lista actualizada
     * @param onError Callback de error
     * @return ValueEventListener para poder removerlo después
     */
    fun observarExplicacionesTema(
        temaId: String,
        onExplicacionesActualizadas: (List<Explicacion>) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        Log.d(TAG, "Iniciando observador de explicaciones para tema: $temaId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val explicaciones = mutableListOf<Explicacion>()

                snapshot.children.forEach { childSnapshot ->
                    childSnapshot.getValue(Explicacion::class.java)?.let { explicacion ->
                        if (explicacion.temaId == temaId && explicacion.activo) {
                            explicaciones.add(explicacion)
                        }
                    }
                }

                val explicacionesOrdenadas = explicaciones.sortedBy { it.orden }
                onExplicacionesActualizadas(explicacionesOrdenadas)
                Log.d(TAG, "Explicaciones actualizadas: ${explicacionesOrdenadas.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error en observador: ${error.message}")
                onError(Exception(error.message))
            }
        }

        explicacionesRef.addValueEventListener(listener)
        return listener
    }

    /**
     * Detiene la observación de explicaciones
     * @param listener ValueEventListener a remover
     */
    fun detenerObservacion(listener: ValueEventListener) {
        explicacionesRef.removeEventListener(listener)
        Log.d(TAG, "Observador detenido")
    }

    /**
     * Busca explicaciones por título
     * @param query Texto a buscar
     * @return Result con lista de Explicaciones coincidentes
     */
    suspend fun buscarExplicaciones(query: String): Result<List<Explicacion>> {
        return try {
            Log.d(TAG, "Buscando explicaciones: $query")

            val snapshot = explicacionesRef.get().await()
            val explicaciones = mutableListOf<Explicacion>()

            snapshot.children.forEach { childSnapshot ->
                childSnapshot.getValue(Explicacion::class.java)?.let { explicacion ->
                    if (explicacion.activo &&
                        (explicacion.titulo.contains(query, ignoreCase = true) ||
                                explicacion.contenido.contains(query, ignoreCase = true))) {
                        explicaciones.add(explicacion)
                    }
                }
            }

            Log.d(TAG, "Resultados de búsqueda: ${explicaciones.size}")
            Result.success(explicaciones)

        } catch (e: Exception) {
            Log.e(TAG, "Error en búsqueda", e)
            Result.failure(Exception("Error en búsqueda: ${e.message}"))
        }
    }

    /**
     * Obtiene estadísticas de una explicación
     * @param explicacionId ID de la explicación
     * @return Result con mapa de estadísticas
     */
    suspend fun obtenerEstadisticas(explicacionId: String): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Obteniendo estadísticas: $explicacionId")

            val snapshot = explicacionesRef.child(explicacionId).get().await()
            val explicacion = snapshot.getValue(Explicacion::class.java)
                ?: return Result.failure(Exception("Explicación no encontrada"))

            val estadisticas = mapOf(
                "vistas" to explicacion.vistas,
                "duracionEstimada" to explicacion.duracionEstimada,
                "fechaCreacion" to explicacion.fechaCreacion,
                "ultimaActualizacion" to explicacion.fechaActualizacion,
                "tipo" to explicacion.tipo
            )

            Log.d(TAG, "Estadísticas obtenidas")
            Result.success(estadisticas)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas", e)
            Result.failure(Exception("Error al obtener estadísticas: ${e.message}"))
        }
    }
}