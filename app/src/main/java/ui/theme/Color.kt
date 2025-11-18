

package com.stiven.sos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================
// PALETA DE COLORES EDURACHA PREMIUM
// ============================================




object Uniautonoma {
    val Primary = Color(0xFFD32F2F) // Tonalidad roja, puedes ajustarla
    val Secondary = Color(0xFFFFC107) // Tonalidad ámbar, puedes ajustarla
    val Background = Color(0xFFFAFAFA) // Gris muy claro
    val Surface = Color.White
    val TextPrimary = Color.Black
    val TextSecondary = Color(0xFF757575) // Gris oscuro
    val Warning = Color(0xFFFFA000) // Naranja/ámbar para la racha
}

object EduRachaColors {
    // Colores Primarios Institucionales (Azul Profundo)
    val Primary = Color(0xFF003D82)
    val PrimaryVariant = Color(0xFF002A5C)
    val PrimaryLight = Color(0xFF1565C0)
    val PrimaryDark = Color(0xFF001E3C)
    val PrimaryContainer = Color(0xFFE3F2FD)

    // Colores Secundarios (Dorado Académico)
    val Secondary = Color(0xFFFFB300)
    val SecondaryVariant = Color(0xFFFF8F00)
    val SecondaryLight = Color(0xFFFFC107)
    val SecondaryDark = Color(0xFFFF6F00)
    val SecondaryContainer = Color(0xFFFFF8E1)

    // Colores de Acento (Celeste Educativo)
    val Accent = Color(0xFF42A5F5)
    val AccentLight = Color(0xFF81D4FA)
    val AccentDark = Color(0xFF1976D2)
    val AccentContainer = Color(0xFFE1F5FE)

    // Verde Institucional
    val Success = Color(0xFF2E7D32)
    val SuccessLight = Color(0xFF4CAF50)
    val SuccessContainer = Color(0xFFC8E6C9)
    val SuccessDark = Color(0xFF1B5E20)

    // Advertencia
    val Warning = Color(0xFFFF6F00)
    val WarningLight = Color(0xFFFFB74D)
    val WarningContainer = Color(0xFFFFE0B2)
    val WarningDark = Color(0xFFE65100)

    // Error
    val Error = Color(0xFFE80909)
    val ErrorLight = Color(0xFFEF5350)
    val ErrorContainer = Color(0xFFFFCDD2)
    val ErrorDark = Color(0xFFC62828)

    // Info
    val Info = Color(0xFF0288D1)
    val InfoLight = Color(0xFF4FC3F7)
    val InfoContainer = Color(0xFFE1F5FE)

    // Fondos y Superficies
    val Background = Color(0xFFF8F9FA)
    val BackgroundSecondary = Color(0xFFF1F3F4)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val SurfaceContainer = Color(0xFFFAFAFA)
    val SurfaceElevated = Color(0xFFFFFFFF)

    // Textos
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)
    val OnSurfaceVariant = Color(0xFF5F6368)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF5F6368)
    val TextTertiary = Color(0xFF9AA0A6)
    val TextDisabled = Color(0xFFBDBDBD)
    val TextHint = Color(0xFFAAAAAA)

    // Ranking y Gamificación
    val RankingGold = Color(0xFFFFD700)
    val RankingSilver = Color(0xFFC0C0C0)
    val RankingBronze = Color(0xFFCD7F32)
    val StreakFire = Color(0xFFFF5722)
    val XpBar = Color(0xFF42A5F5)
    val XpBarBackground = Color(0xFFE3F2FD)

    // Niveles
    val LevelBronze = Color(0xFFCD7F32)
    val LevelSilver = Color(0xFF90A4AE)
    val LevelGold = Color(0xFFFFB300)
    val LevelPlatinum = Color(0xFF78909C)
    val LevelDiamond = Color(0xFF42A5F5)

    // Quiz
    val QuizCorrect = Color(0xFF2E7D32)
    val QuizIncorrect = Color(0xFFD32F2F)
    val QuizNeutral = Color(0xFFE3F2FD)
    val QuizSelected = Color(0xFF1976D2)
    val AIExplanation = Color(0xFFE8F4FD)
    val AIBackground = Color(0xFFF0F8FF)

    // Calendario
    val CalendarToday = Color(0xFFFFB300)
    val CalendarCompleted = Color(0xFF42A5F5)
    val CalendarPending = Color(0xFFB0BEC5)
    val CalendarMissed = Color(0xFFEF5350)
    val CalendarExtended = Color(0xFF78909C)

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
    val Border = Color(0xFFE0E0E0)
    val BorderLight = Color(0xFFEEEEEE)
    val Divider = Color(0xFFE0E0E0)
    val DividerLight = Color(0xFFF5F5F5)

    // Overlays y Sombras
    val OverlayLight = Color(0x0D000000)
    val OverlayMedium = Color(0x1A000000)
    val OverlayDark = Color(0x4D000000)
    val Shadow = Color(0x1A000000)
    val ShadowMedium = Color(0x26000000)
    val Scrim = Color(0x80000000)

    // Gradientes
    val GradientStart = Color(0xFF003D82)
    val GradientEnd = Color(0xFF1565C0)
    val GradientAccent = Color(0xFF42A5F5)
}

// ============================================
// ESQUEMA DE COLORES LIGHT PREMIUM
// ============================================
private val LightColorScheme = lightColorScheme(
    primary = EduRachaColors.Primary,
    onPrimary = EduRachaColors.OnPrimary,
    primaryContainer = EduRachaColors.PrimaryContainer,
    onPrimaryContainer = EduRachaColors.PrimaryDark,

    secondary = EduRachaColors.Secondary,
    onSecondary = EduRachaColors.OnSecondary,
    secondaryContainer = EduRachaColors.SecondaryContainer,
    onSecondaryContainer = EduRachaColors.SecondaryDark,

    tertiary = EduRachaColors.Accent,
    onTertiary = EduRachaColors.OnPrimary,
    tertiaryContainer = EduRachaColors.AccentContainer,
    onTertiaryContainer = EduRachaColors.AccentDark,

    error = EduRachaColors.Error,
    onError = EduRachaColors.OnPrimary,
    errorContainer = EduRachaColors.ErrorContainer,
    onErrorContainer = EduRachaColors.ErrorDark,

    background = EduRachaColors.Background,
    onBackground = EduRachaColors.OnBackground,

    surface = EduRachaColors.Surface,
    onSurface = EduRachaColors.OnSurface,
    surfaceVariant = EduRachaColors.SurfaceVariant,
    onSurfaceVariant = EduRachaColors.OnSurfaceVariant,
    surfaceContainer = EduRachaColors.SurfaceContainer,

    outline = EduRachaColors.Border,
    outlineVariant = EduRachaColors.BorderLight,
    scrim = EduRachaColors.Scrim
)

// ============================================
// ESQUEMA DARK (Opcional)
// ============================================
private val DarkColorScheme = darkColorScheme(
    primary = EduRachaColors.PrimaryLight,
    onPrimary = Color(0xFF001E3C),
    primaryContainer = EduRachaColors.Primary,
    onPrimaryContainer = EduRachaColors.PrimaryContainer,

    secondary = EduRachaColors.Secondary,
    onSecondary = Color(0xFF3E2D00),
    secondaryContainer = EduRachaColors.SecondaryDark,
    onSecondaryContainer = EduRachaColors.SecondaryContainer,

    tertiary = EduRachaColors.AccentLight,
    onTertiary = Color(0xFF003258),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E1E1),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCACACA),

    error = EduRachaColors.ErrorLight,
    onError = Color(0xFF690005)
)
// En el archivo Color.kt    // ... (otros colores como Purple80, Pink80, etc.)

// Colores semánticos de la App
val Success = Color(0xFF28A745)
val SuccessContainer = Color(0xFFD1FADF) // <-- AÑADE ESTA LÍNEA (verde claro)
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
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EduRachaTypography,
        shapes = EduRachaShapes,
        content = content
    )
}