package com.stiven.sos.services

import android.content.Context
import android.util.Log
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.PreferenciasUsuario
import com.stiven.sos.ui.theme.TemaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sincroniza el tema y los sonidos con el perfil del usuario en el backend.
 * Es "best-effort": la app siempre funciona con la copia local (SharedPreferences);
 * si no hay red o el servidor falla, simplemente se reintenta en la próxima ocasión.
 */
object SincronizadorPreferencias {
    private const val TAG = "SyncPreferencias"
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var envioPendiente: Job? = null

    /** Descarga las preferencias guardadas en el servidor y las aplica (por ejemplo, tras iniciar sesión). */
    fun descargar(context: Context) {
        val app = context.applicationContext
        alcance.launch {
            try {
                val respuesta = ApiClient.apiService.obtenerPreferencias()
                val prefs = respuesta.body()
                if (respuesta.isSuccessful && prefs != null) {
                    withContext(Dispatchers.Main) {
                        TemaApp.aplicarDesdeServidor(app, prefs.tema, prefs.sonidos)
                    }
                } else if (respuesta.code() == 204) {
                    // Aún no hay nada guardado en el servidor: se sube la elección local
                    subir()
                } else {
                    Log.d(TAG, "Sin preferencias remotas (HTTP ${respuesta.code()})")
                }
            } catch (e: Exception) {
                Log.d(TAG, "No se pudieron descargar preferencias: ${e.message}")
            }
        }
    }

    /** Sube las preferencias actuales. Agrupa cambios rápidos (espera 800 ms). */
    fun subir() {
        envioPendiente?.cancel()
        envioPendiente = alcance.launch {
            delay(800)
            try {
                val cuerpo = PreferenciasUsuario(
                    tema = TemaApp.preferencia.name,
                    sonidos = TemaApp.sonidosActivos
                )
                val respuesta = ApiClient.apiService.guardarPreferencias(cuerpo)
                if (!respuesta.isSuccessful) Log.d(TAG, "El servidor rechazó las preferencias (HTTP ${respuesta.code()})")
            } catch (e: Exception) {
                Log.d(TAG, "No se pudieron subir preferencias: ${e.message}")
            }
        }
    }
}
