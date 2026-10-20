package com.stiven.sos.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.stiven.sos.R
import com.stiven.sos.ui.theme.TemaApp

/**
 * Efectos de sonido cortos (acierto, error, fin de quiz, avance).
 * - Respetan la preferencia del usuario (TemaApp.sonidosActivos).
 * - Usan el volumen multimedia del celular (canal de juego).
 * - Nunca bloquean la interfaz; si algo falla, simplemente no suena.
 * Los audios son sintetizados para EduRacha (sin licencias de terceros).
 */
object GestorSonidos {
    enum class Sonido { CORRECTO, ERROR, FIN, AVANCE }

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Sonido, Int>()
    private val cargados = mutableSetOf<Int>()

    fun inicializar(context: Context) {
        if (pool != null) return
        try {
            val p = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            p.setOnLoadCompleteListener { _, id, estado -> if (estado == 0) cargados.add(id) }
            val app = context.applicationContext
            ids[Sonido.CORRECTO] = p.load(app, R.raw.sonido_correcto, 1)
            ids[Sonido.ERROR] = p.load(app, R.raw.sonido_error, 1)
            ids[Sonido.FIN] = p.load(app, R.raw.sonido_fin, 1)
            ids[Sonido.AVANCE] = p.load(app, R.raw.sonido_avance, 1)
            pool = p
        } catch (e: Exception) {
            Log.w("GestorSonidos", "No se pudieron cargar los sonidos: ${e.message}")
        }
    }

    fun reproducir(context: Context, sonido: Sonido) {
        if (!TemaApp.sonidosActivos) return
        inicializar(context)
        val p = pool ?: return
        val id = ids[sonido] ?: return
        if (id in cargados) p.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }
}
