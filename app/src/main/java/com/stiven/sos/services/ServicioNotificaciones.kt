package com.stiven.sos.services

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Servicio para gestionar notificaciones en tiempo real desde Firebase
 */
object ServicioNotificaciones {

    private val database = FirebaseDatabase.getInstance()
    private const val TAG = "ServicioNotificaciones"

    /**
     * Escucha notificaciones en tiempo real para un usuario
     */
    fun escucharNotificaciones(userId: String): Flow<List<NotificacionModel>> = callbackFlow {
        val ref = database.getReference("notificaciones/$userId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificaciones = mutableListOf<NotificacionModel>()

                for (child in snapshot.children) {
                    try {
                        val notif = NotificacionModel(
                            id = child.key ?: "",
                            titulo = child.child("titulo").getValue(String::class.java) ?: "",
                            mensaje = child.child("mensaje").getValue(String::class.java) ?: "",
                            tipo = child.child("tipo").getValue(String::class.java) ?: "sistema",
                            fecha = child.child("fecha").getValue(Long::class.java) ?: 0L,
                            leido = child.child("leido").getValue(Boolean::class.java) ?: false,
                            cursoId = child.child("cursoId").getValue(String::class.java),
                            rutaArchivo = child.child("rutaArchivo").getValue(String::class.java),
                            estudianteId = child.child("estudianteId").getValue(String::class.java),
                            temaId = child.child("temaId").getValue(String::class.java),
                            xpGanado = child.child("xpGanado").getValue(Int::class.java),
                            racha = child.child("racha").getValue(Int::class.java)
                        )
                        notificaciones.add(notif)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando notificación: ${e.message}")
                    }
                }

                // Ordenar por fecha descendente (más recientes primero)
                notificaciones.sortByDescending { it.fecha }
                trySend(notificaciones)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error escuchando notificaciones: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    /**
     * Marca una notificación como leída
     */
    suspend fun marcarComoLeida(userId: String, notifId: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            val ref = database.getReference("notificaciones/$userId/$notifId/leido")

            ref.setValue(true) { error, _ ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.message))
                } else {
                    cont.resume(Unit)
                }
            }
        }

    /**
     * Marca todas las notificaciones como leídas
     */
    suspend fun marcarTodasComoLeidas(userId: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            val ref = database.getReference("notificaciones/$userId")

            ref.get().addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any>()

                for (child in snapshot.children) {
                    updates["${child.key}/leido"] = true
                }

                if (updates.isEmpty()) {
                    cont.resume(Unit)
                } else {
                    ref.updateChildren(updates) { error, _ ->
                        if (error != null) {
                            cont.resumeWithException(Exception(error.message))
                        } else {
                            cont.resume(Unit)
                        }
                    }
                }
            }.addOnFailureListener { error ->
                cont.resumeWithException(error)
            }
        }

    /**
     * Elimina una notificación
     */
    suspend fun eliminarNotificacion(userId: String, notifId: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            val ref = database.getReference("notificaciones/$userId/$notifId")

            ref.removeValue { error, _ ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.message))
                } else {
                    cont.resume(Unit)
                }
            }
        }

    /**
     * Cuenta notificaciones no leídas
     */
    suspend fun contarNoLeidas(userId: String): Int =
        suspendCancellableCoroutine { cont ->
            val ref = database.getReference("notificaciones/$userId")

            ref.get().addOnSuccessListener { snapshot ->
                var count = 0
                for (child in snapshot.children) {
                    val leido = child.child("leido").getValue(Boolean::class.java) ?: false
                    if (!leido) count++
                }
                cont.resume(count)
            }.addOnFailureListener { error ->
                cont.resumeWithException(error)
            }
        }

    /**
     * Envía una notificación (desde el frontend cuando sea necesario)
     */
    suspend fun enviarNotificacion(
        userId: String,
        notificacion: NotificacionModel
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val ref = database.getReference("notificaciones/$userId").push()

        val notifMap = mutableMapOf<String, Any>(
            "titulo" to notificacion.titulo,
            "mensaje" to notificacion.mensaje,
            "tipo" to notificacion.tipo,
            "fecha" to notificacion.fecha,
            "leido" to false
        )

        notificacion.cursoId?.let { notifMap["cursoId"] = it }
        notificacion.estudianteId?.let { notifMap["estudianteId"] = it }
        notificacion.temaId?.let { notifMap["temaId"] = it }
        notificacion.xpGanado?.let { notifMap["xpGanado"] = it }
        notificacion.racha?.let { notifMap["racha"] = it }
        notificacion.rutaArchivo?.let { notifMap["rutaArchivo"] = it }

        ref.setValue(notifMap) { error, _ ->
            if (error != null) {
                cont.resumeWithException(Exception(error.message))
            } else {
                cont.resume(Unit)
            }
        }
    }
}

/**
 * Modelo de notificación
 */
data class NotificacionModel(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val tipo: String, // "bienvenida", "quiz_completado", "solicitud", "reporte", "curso_creado", etc.
    val fecha: Long,
    val leido: Boolean,
    val cursoId: String? = null,
    val rutaArchivo: String? = null,
    val estudianteId: String? = null,
    val temaId: String? = null,
    val xpGanado: Int? = null,
    val racha: Int? = null
)