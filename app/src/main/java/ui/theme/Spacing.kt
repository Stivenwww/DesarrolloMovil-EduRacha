

package com.stiven.desarrollomovil.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// ESPACIADO ESTÁNDAR (Sistema de 4dp)
// ============================================
object Spacing {
    // Espaciado base
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp      // 1x
    val small: Dp = 8.dp           // 2x
    val medium: Dp = 16.dp         // 4x
    val large: Dp = 24.dp          // 6x
    val extraLarge: Dp = 32.dp     // 8x
    val huge: Dp = 48.dp           // 12x
    val massive: Dp = 64.dp        // 16x

    // Padding específico para componentes
    val cardPadding: Dp = 20.dp
    val cardPaddingSmall: Dp = 16.dp
    val cardPaddingLarge: Dp = 24.dp

    val screenPadding: Dp = 20.dp
    val screenPaddingSmall: Dp = 16.dp
    val screenPaddingLarge: Dp = 24.dp

    val listItemPadding: Dp = 16.dp
    val dialogPadding: Dp = 24.dp

    // Espaciado entre elementos
    val spaceBetweenSmall: Dp = 8.dp
    val spaceBetweenMedium: Dp = 16.dp
    val spaceBetweenLarge: Dp = 24.dp
}

// ============================================
// DIMENSIONES DE COMPONENTES
// ============================================
object Dimensions {
    // Alturas de botones
    val buttonHeight: Dp = 56.dp
    val buttonHeightSmall: Dp = 48.dp
    val buttonHeightLarge: Dp = 64.dp

    // Iconos
    val iconSize: Dp = 24.dp
    val iconSizeSmall: Dp = 20.dp
    val iconSizeMedium: Dp = 28.dp
    val iconSizeLarge: Dp = 32.dp
    val iconSizeExtraLarge: Dp = 48.dp

    // Avatares
    val avatarSizeSmall: Dp = 32.dp
    val avatarSizeMedium: Dp = 48.dp
    val avatarSizeLarge: Dp = 64.dp
    val avatarSizeExtraLarge: Dp = 100.dp

    // Cards
    val cardMinHeight: Dp = 100.dp
    val cardMinWidth: Dp = 160.dp

    // Progress Bars
    val progressBarHeight: Dp = 12.dp
    val progressBarHeightSmall: Dp = 8.dp
    val progressBarHeightLarge: Dp = 16.dp

    // Dividers
    val dividerThickness: Dp = 1.dp
    val dividerThicknessBold: Dp = 2.dp

    // Bottom Navigation
    val bottomNavHeight: Dp = 80.dp

    // Top App Bar
    val topAppBarHeight: Dp = 64.dp

    // TextFields
    val textFieldHeight: Dp = 56.dp

    // Chips
    val chipHeight: Dp = 32.dp
    val chipMinWidth: Dp = 48.dp

    // Badges
    val badgeSize: Dp = 20.dp
    val badgeSizeSmall: Dp = 16.dp
}

// ============================================
// ELEVACIONES Y SOMBRAS
// ============================================
object Elevation {
    val none: Dp = 0.dp
    val extraSmall: Dp = 1.dp
    val small: Dp = 2.dp
    val medium: Dp = 4.dp
    val large: Dp = 8.dp
    val extraLarge: Dp = 12.dp
    val huge: Dp = 16.dp
}

// ============================================
// BORDES
// ============================================
object BorderWidth {
    val none: Dp = 0.dp
    val thin: Dp = 1.dp
    val medium: Dp = 2.dp
    val thick: Dp = 3.dp
    val extraThick: Dp = 4.dp
}

// ============================================
// OPACIDADES
// ============================================
object Alpha {
    const val disabled = 0.38f
    const val medium = 0.6f
    const val high = 0.87f
    const val overlay = 0.5f
    const val overlayLight = 0.12f
    const val overlayDark = 0.56f
}