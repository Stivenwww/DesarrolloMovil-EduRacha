

package com.stiven.sos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// ============================================
// PALETA DE COLORES EDURACHA PREMIUM
// ============================================




object Uniautonoma {
    val Primary = Color(0xFFD32F2F) // Tonalidad roja
    val Secondary = Color(0xFFFFC107) // Tonalidad ámbar
    val Background = Color(0xFFFAFAFA) // Gris muy claro
    val Surface = Color.White
    val TextPrimary = Color.Black
    val TextSecondary = Color(0xFF757575) // Gris oscuro
    val Warning = Color(0xFFFFA000) // Naranja/ámbar para la racha
}

object EduRachaColors {
    // Todos los colores de interfaz leen la paleta activa (claro/oscuro).
    private val p: NexoPaleta get() = TemaApp.paleta

    // Colores Primarios Institucionales (Azul Profundo)
    val Primary: Color get() = p.marca
    val PrimaryVariant: Color get() = p.marcaSombra
    val PrimaryLight: Color get() = p.marca
    val PrimaryDark: Color get() = p.marcaSombra
    val PrimaryContainer: Color get() = p.marca.copy(alpha = 0.14f)

    // Colores Secundarios (Dorado Académico)
    val Secondary: Color get() = p.racha
    val SecondaryVariant: Color get() = p.rachaSombra
    val SecondaryLight: Color get() = p.racha
    val SecondaryDark: Color get() = p.rachaSombra
    val SecondaryContainer: Color get() = p.racha.copy(alpha = 0.16f)

    // Colores de Acento (Celeste Educativo)
    val Accent: Color get() = p.marca
    val AccentLight: Color get() = p.marca
    val AccentDark: Color get() = p.marcaTexto
    val AccentContainer: Color get() = p.marca.copy(alpha = 0.14f)

    // Verde Institucional
    val Success: Color get() = p.accion
    val SuccessLight: Color get() = p.accion
    val SuccessContainer: Color get() = p.accion.copy(alpha = 0.16f)
    val SuccessDark: Color get() = p.accionTexto

    // Advertencia
    val Warning: Color get() = p.racha
    val WarningLight: Color get() = p.racha
    val WarningContainer: Color get() = p.racha.copy(alpha = 0.16f)
    val WarningDark: Color get() = p.rachaTexto

    // Error
    val Error: Color get() = p.error
    val ErrorLight: Color get() = p.error
    val ErrorContainer: Color get() = p.error.copy(alpha = 0.14f)
    val ErrorDark: Color get() = p.errorTexto

    // Info
    val Info: Color get() = p.marca
    val InfoLight: Color get() = p.marca
    val InfoContainer: Color get() = p.marca.copy(alpha = 0.14f)

    // Fondos y Superficies
    val Background: Color get() = p.fondo
    val BackgroundSecondary: Color get() = p.superficieAlt
    val Surface: Color get() = p.superficie
    val SurfaceVariant: Color get() = p.superficieAlt
    val SurfaceContainer: Color get() = p.superficieAlt
    val SurfaceElevated: Color get() = p.superficie

    // Textos
    val OnPrimary: Color get() = p.sobreMarca
    val OnSecondary: Color get() = p.texto
    val OnBackground: Color get() = p.texto
    val OnSurface: Color get() = p.texto
    val OnSurfaceVariant: Color get() = p.textoSuave
    val TextPrimary: Color get() = p.texto
    val TextSecondary: Color get() = p.textoSuave
    val TextTertiary: Color get() = p.textoTenue
    val TextDisabled: Color get() = p.textoTenue
    val TextHint: Color get() = p.textoTenue

    // Ranking y Gamificación
    val RankingGold = Color(0xFFFFD700)
    val RankingSilver = Color(0xFFC0C0C0)
    val RankingBronze = Color(0xFFCD7F32)
    val StreakFire: Color get() = p.racha
    val XpBar: Color get() = p.ia
    val XpBarBackground: Color get() = p.borde

    // Niveles
    val LevelBronze = Color(0xFFCD7F32)
    val LevelSilver = Color(0xFF90A4AE)
    val LevelGold = Color(0xFFFFB300)
    val LevelPlatinum = Color(0xFF78909C)
    val LevelDiamond = Color(0xFF14C8D6)

    // Quiz
    val QuizCorrect: Color get() = p.accion
    val QuizIncorrect: Color get() = p.error
    val QuizNeutral: Color get() = p.superficieAlt
    val QuizSelected: Color get() = p.marca
    val AIExplanation: Color get() = p.ia.copy(alpha = 0.12f)
    val AIBackground: Color get() = p.ia.copy(alpha = 0.08f)

    // Calendario
    val CalendarToday: Color get() = p.racha
    val CalendarCompleted: Color get() = p.marca
    val CalendarPending: Color get() = p.textoTenue
    val CalendarMissed: Color get() = p.error
    val CalendarExtended: Color get() = p.textoSuave

    // Asignaturas
    val SubjectMath = Color(0xFF1976D2)
    val SubjectScience = Color(0xFF00796B)
    val SubjectLanguage = Color(0xFFC2185B)
    val SubjectHistory = Color(0xFF5D4037)
    val SubjectArt = Color(0xFF7B1FA2)
    val SubjectEngineering = Color(0xFFFF6F00)
    val SubjectBusiness = Color(0xFF388E3C)
    val SubjectHealth = Color(0xFFE53935)
    val SubjectLaw = Color(0xFF424242)
    val SubjectDefault = Color(0xFF546E7A)

    // Bordes y Divisores
    val Border: Color get() = p.borde
    /** Barras superiores con texto blanco: violeta oscuro (contraste AA en claro y oscuro). */
    val BarraSuperior: Color get() = p.iaSombra
    val BorderLight: Color get() = p.borde
    val Divider: Color get() = p.borde
    val DividerLight: Color get() = p.borde

    // Overlays y Sombras
    val OverlayLight = Color(0x0D000000)
    val OverlayMedium = Color(0x1A000000)
    val OverlayDark = Color(0x4D000000)
    val Shadow = Color(0x1A000000)
    val ShadowMedium = Color(0x26000000)
    val Scrim = Color(0x80000000)

    // Gradientes
    val GradientStart: Color get() = p.marcaSombra
    val GradientEnd: Color get() = p.ia
    val GradientAccent: Color get() = p.marca
}

// ============================================
// ESQUEMA DE COLORES LIGHT PREMIUM
// ============================================

// ============================================
// ESQUEMA DARK (Opcional)
// ============================================


// Colores semánticos de la App
val Success = Color(0xFF28A745)
val SuccessContainer = Color(0xFFD1FADF)
val Warning = Color(0xFFFFA901)
val Error = Color(0xFFD92D20)
// ...etc


// ============================================
// TEMA PRINCIPAL PREMIUM
// ============================================
@Composable
fun EduRachaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    TemaApp.inicializar(context)
    val sistemaOscuro = isSystemInDarkTheme()
    val oscuro = when (TemaApp.preferencia) {
        PreferenciaTema.SISTEMA -> sistemaOscuro
        PreferenciaTema.CLARO -> false
        PreferenciaTema.OSCURO -> true
    }
    val paleta = if (oscuro) NexoOscuro else NexoClaro
    SideEffect { TemaApp.sistemaOscuro = sistemaOscuro }

    // Iconos de la barra de estado legibles en ambos modos
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !oscuro
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !oscuro
        }
    }

    CompositionLocalProvider(LocalNexo provides paleta) {
        MaterialTheme(
            colorScheme = esquemaNexo(paleta),
            typography = EduRachaTypography,
            shapes = EduRachaShapes,
            content = content
        )
    }
}

private fun esquemaNexo(p: NexoPaleta) = if (p.esOscuro) darkColorScheme(
    primary = p.marca, onPrimary = p.sobreMarca,
    primaryContainer = p.superficieAlt, onPrimaryContainer = p.texto,
    secondary = p.racha, onSecondary = p.fondo,
    // Chips y filtros seleccionados: tinte de la marca, distinguible del fondo en modo oscuro
    secondaryContainer = p.marca.copy(alpha = 0.22f), onSecondaryContainer = p.texto,
    tertiary = p.ia, onTertiary = Color.White,
    error = p.error, onError = p.fondo,
    errorContainer = p.superficieAlt, onErrorContainer = p.errorTexto,
    background = p.fondo, onBackground = p.texto,
    surface = p.superficie, onSurface = p.texto,
    surfaceVariant = p.superficieAlt, onSurfaceVariant = p.textoSuave,
    surfaceContainer = p.superficie, surfaceContainerHigh = p.superficieAlt,
    surfaceContainerHighest = p.superficieAlt, surfaceContainerLow = p.superficie,
    surfaceContainerLowest = p.fondo,
    outline = p.borde, outlineVariant = p.borde
) else lightColorScheme(
    primary = p.marca, onPrimary = p.sobreMarca,
    primaryContainer = p.marca.copy(alpha = 0.14f), onPrimaryContainer = p.texto,
    secondary = p.racha, onSecondary = p.texto,
    // Chips y filtros seleccionados: mismo tinte de la marca que en modo oscuro
    secondaryContainer = p.marca.copy(alpha = 0.16f), onSecondaryContainer = p.texto,
    tertiary = p.ia, onTertiary = Color.White,
    error = p.error, onError = Color.White,
    errorContainer = p.error.copy(alpha = 0.14f), onErrorContainer = p.errorTexto,
    background = p.fondo, onBackground = p.texto,
    surface = p.superficie, onSurface = p.texto,
    surfaceVariant = p.superficieAlt, onSurfaceVariant = p.textoSuave,
    surfaceContainer = p.superficie, surfaceContainerHigh = p.superficie,
    surfaceContainerHighest = p.superficieAlt, surfaceContainerLow = p.superficie,
    surfaceContainerLowest = p.superficie,
    outline = p.borde, outlineVariant = p.borde
)
