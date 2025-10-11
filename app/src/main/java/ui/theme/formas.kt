

package com.stiven.desarrollomovil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================
// FORMAS ESTÁNDAR DE MATERIAL 3
// ============================================
val EduRachaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Para elementos muy pequeños
    small = RoundedCornerShape(8.dp),        // Para chips pequeños
    medium = RoundedCornerShape(12.dp),      // Para textfields
    large = RoundedCornerShape(16.dp),       // Para cards
    extraLarge = RoundedCornerShape(24.dp)   // Para dialogs
)

// ============================================
// FORMAS PERSONALIZADAS PREMIUM
// ============================================
object CustomShapes {
    // Botones
    val Button = RoundedCornerShape(28.dp)
    val ButtonSmall = RoundedCornerShape(20.dp)
    val ButtonLarge = RoundedCornerShape(32.dp)

    // Cards
    val Card = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(20.dp)
    val CardSmall = RoundedCornerShape(12.dp)

    // TextFields
    val TextField = RoundedCornerShape(12.dp)
    val TextFieldRounded = RoundedCornerShape(24.dp)

    // Diálogos y Modales
    val Dialog = RoundedCornerShape(24.dp)
    val DialogLarge = RoundedCornerShape(28.dp)

    // Bottom Sheets
    val BottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Chips y Badges
    val Chip = RoundedCornerShape(16.dp)
    val Badge = RoundedCornerShape(12.dp)
    val BadgeSmall = RoundedCornerShape(8.dp)

    // Navegación
    val NavigationBar = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Banners y Notificaciones
    val Banner = RoundedCornerShape(12.dp)
    val Snackbar = RoundedCornerShape(8.dp)

    // Imágenes y Avatars
    val Avatar = RoundedCornerShape(50) // Porcentaje para círculo perfecto
    val ImageSmall = RoundedCornerShape(8.dp)
    val ImageMedium = RoundedCornerShape(12.dp)
    val ImageLarge = RoundedCornerShape(16.dp)

    // Progress Bars
    val ProgressBar = RoundedCornerShape(50) // Completamente redondeado
    val ProgressBarSquare = RoundedCornerShape(4.dp)

    // Tabs
    val Tab = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
}