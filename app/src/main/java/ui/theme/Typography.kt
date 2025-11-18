
package com.stiven.sos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp



// Usando fuentes del sistema optimizadas
private val SystemFontFamily = FontFamily.Default

// ============================================
// SISTEMA DE TIPOGRAFÍA PREMIUM
// ============================================
val EduRachaTypography = Typography(
    // ============================================
    // DISPLAY - Títulos Grandes
    // Para pantallas de bienvenida, splash, títulos principales
    // ============================================
    displayLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // ============================================
    // HEADLINE - Encabezados
    // Para títulos de secciones importantes
    // ============================================
    headlineLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // ============================================
    // TITLE - Títulos de Sección
    // Para nombres de cards, títulos de diálogos
    // ============================================
    titleLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ============================================
    // BODY - Cuerpo de Texto
    // Para párrafos, descripciones, contenido
    // ============================================
    bodyLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ============================================
    // LABEL - Etiquetas y Botones
    // Para texto de botones, chips, labels
    // ============================================
    labelLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================
// ESTILOS PERSONALIZADOS ADICIONALES
// ============================================
object CustomTextStyles {
    // Para estadísticas grandes
    val StatValue = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    // Para subtítulos de cards
    val CardSubtitle = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )

    // Para badges y números pequeños
    val BadgeText = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    // Para títulos de sección con estilo
    val SectionTitle = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
}