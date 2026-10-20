// ui/theme/Nexo.kt
// Sistema de diseño "Nexo" de EduRacha: paletas claro/oscuro y preferencia de tema del usuario.
package com.stiven.sos.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Paleta semántica. Las pantallas deben usar estos nombres, nunca colores fijos. */
@Immutable
data class NexoPaleta(
    val esOscuro: Boolean,
    val fondo: Color,
    val fondoCentro: Color,
    val superficie: Color,
    val superficieAlt: Color,
    val borde: Color,
    val texto: Color,
    val textoSuave: Color,
    val textoTenue: Color,
    // Marca (cian / mar)
    val marca: Color,
    val marcaSombra: Color,
    val marcaTexto: Color,
    val sobreMarca: Color,
    // IA (violeta Robi)
    val ia: Color,
    val iaSombra: Color,
    val iaTexto: Color,
    // Acción principal / correcto (verde)
    val accion: Color,
    val accionSombra: Color,
    val accionTexto: Color,
    val sobreAccion: Color,
    // Racha / XP (ámbar / fogata)
    val racha: Color,
    val rachaSombra: Color,
    val rachaTexto: Color,
    // Error / vidas (rosa / coral)
    val error: Color,
    val errorSombra: Color,
    val errorTexto: Color,
    // Varios
    val arena: Color,
    val islaRoca: Color,
    val visor: Color,
    val robotCuerpo: Color,
    val robotSombra: Color,
    val robotContorno: Color,
    val bloqueado: Color,
    val bloqueadoSombra: Color,
    val decoracion: List<Color>
)

/** Modo oscuro: "Nexo — islas flotantes en la era de la IA". */
val NexoOscuro = NexoPaleta(
    esOscuro = true,
    fondo = Color(0xFF070B1F),
    fondoCentro = Color(0xFF1A1F5C),
    superficie = Color(0xFF0E1533),
    superficieAlt = Color(0xFF121C45),
    borde = Color(0xFF1E2A5A),
    texto = Color(0xFFEEF2FF),
    textoSuave = Color(0xFF8E9BC7),
    textoTenue = Color(0xFF6B78A8),
    marca = Color(0xFF3AF2FF),
    marcaSombra = Color(0xFF12B8C9),
    marcaTexto = Color(0xFF3AF2FF),
    sobreMarca = Color(0xFF070B1F),
    ia = Color(0xFFA06BFF),
    iaSombra = Color(0xFF7B45F0),
    iaTexto = Color(0xFFC7B2FF),
    accion = Color(0xFF5CF29A),
    accionSombra = Color(0xFF2DBE6C),
    accionTexto = Color(0xFF5CF29A),
    sobreAccion = Color(0xFF070B1F),
    racha = Color(0xFFFFC14D),
    rachaSombra = Color(0xFFE09A12),
    rachaTexto = Color(0xFFFFC14D),
    error = Color(0xFFFF5FA2),
    errorSombra = Color(0xFFD93C80),
    errorTexto = Color(0xFFFF8FC0),
    arena = Color(0xFF2A3470),
    islaRoca = Color(0xFF2A3470),
    visor = Color(0xFF0A0F2A),
    robotCuerpo = Color(0xFFEEF2FF),
    robotSombra = Color(0xFFC9D2F5),
    robotContorno = Color(0x00000000),
    bloqueado = Color(0xFF2A5A5E),
    bloqueadoSombra = Color(0xFF1A3A3F),
    decoracion = listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
)

/** Modo claro: "Nexo claro con los colores de la opción D". */
val NexoClaro = NexoPaleta(
    esOscuro = false,
    fondo = Color(0xFFF2FBFF),
    fondoCentro = Color(0xFFFFFFFF),
    superficie = Color(0xFFFFFFFF),
    superficieAlt = Color(0xFFF2FBFF),
    borde = Color(0xFFE1EAF2),
    texto = Color(0xFF233042),
    textoSuave = Color(0xFF6B7A90),
    textoTenue = Color(0xFF93A1B5),
    marca = Color(0xFF12B5CB),
    marcaSombra = Color(0xFF0B8FA1),
    marcaTexto = Color(0xFF087C8C),
    sobreMarca = Color(0xFF233042),
    ia = Color(0xFF7C5CFF),
    iaSombra = Color(0xFF5B3FE0),
    iaTexto = Color(0xFF5B3FE0),
    accion = Color(0xFF3DCB6C),
    accionSombra = Color(0xFF27A352),
    accionTexto = Color(0xFF1E7D3D),
    sobreAccion = Color(0xFF233042),
    racha = Color(0xFFFF9F1C),
    rachaSombra = Color(0xFFE07F00),
    rachaTexto = Color(0xFFB35F00),
    error = Color(0xFFFF5A6E),
    errorSombra = Color(0xFFD93A4E),
    errorTexto = Color(0xFFC42A3F),
    arena = Color(0xFFFFE3A3),
    islaRoca = Color(0xFFEED9A6),
    visor = Color(0xFF233042),
    robotCuerpo = Color(0xFFFFFFFF),
    robotSombra = Color(0xFFDDE1FB),
    robotContorno = Color(0xFFD6DCF5),
    bloqueado = Color(0xFFDCE5EC),
    bloqueadoSombra = Color(0xFFC7D3DC),
    decoracion = listOf(Color(0xFF12B5CB), Color(0xFFFFE3A3), Color(0xFF3DCB6C))
)

enum class PreferenciaTema { SISTEMA, CLARO, OSCURO }

/**
 * Preferencias visuales de la app (tema y sonidos).
 * Es estado observable de Compose: al cambiarlo, toda la interfaz se redibuja.
 * Se guarda en SharedPreferences y se sincroniza con el perfil del backend (best-effort).
 */
object TemaApp {
    private const val PREFS = "EduRachaUserPrefs"
    private const val KEY_TEMA = "tema_app"
    private const val KEY_SONIDOS = "sonidos_activos"

    private var inicializado = false

    var preferencia by mutableStateOf(PreferenciaTema.SISTEMA)
        private set
    var sistemaOscuro by mutableStateOf(false)
    var sonidosActivos by mutableStateOf(true)
        private set

    val esOscuro: Boolean
        get() = when (preferencia) {
            PreferenciaTema.SISTEMA -> sistemaOscuro
            PreferenciaTema.CLARO -> false
            PreferenciaTema.OSCURO -> true
        }

    val paleta: NexoPaleta
        get() = if (esOscuro) NexoOscuro else NexoClaro

    fun inicializar(context: Context) {
        if (inicializado) return
        inicializado = true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        preferencia = runCatching {
            PreferenciaTema.valueOf(prefs.getString(KEY_TEMA, PreferenciaTema.SISTEMA.name)!!)
        }.getOrDefault(PreferenciaTema.SISTEMA)
        sonidosActivos = prefs.getBoolean(KEY_SONIDOS, true)
        val modo = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        sistemaOscuro = modo == Configuration.UI_MODE_NIGHT_YES
    }

    fun cambiarTema(context: Context, nueva: PreferenciaTema) {
        preferencia = nueva
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TEMA, nueva.name).apply()
    }

    fun cambiarSonidos(context: Context, activos: Boolean) {
        sonidosActivos = activos
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SONIDOS, activos).apply()
    }

    /** Aplica valores recibidos del servidor (por ejemplo al iniciar sesión en otro dispositivo). */
    fun aplicarDesdeServidor(context: Context, tema: String?, sonidos: Boolean?) {
        tema?.let { t -> runCatching { PreferenciaTema.valueOf(t.uppercase()) }.getOrNull()?.let { cambiarTema(context, it) } }
        sonidos?.let { cambiarSonidos(context, it) }
    }
}

/** Paleta activa dentro de la composición. */
val LocalNexo = staticCompositionLocalOf { NexoClaro }
