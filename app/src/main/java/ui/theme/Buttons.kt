

package com.stiven.sos.ui.theme.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.*

// ============================================
// BOTÓN PRIMARIO PREMIUM
// ============================================
@Composable
fun EduRachaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = EduRachaColors.Primary,
            contentColor = EduRachaColors.OnPrimary,
            disabledContainerColor = EduRachaColors.Primary.copy(alpha = Alpha.disabled),
            disabledContentColor = EduRachaColors.OnPrimary.copy(alpha = Alpha.disabled)
        ),
        shape = CustomShapes.Button,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.medium,
            pressedElevation = Elevation.large,
            disabledElevation = Elevation.none
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.large)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.iconSize),
                color = EduRachaColors.OnPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ============================================
// BOTÓN SECUNDARIO (OUTLINED)
// ============================================
@Composable
fun EduRachaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = EduRachaColors.Primary,
            disabledContentColor = EduRachaColors.Primary.copy(alpha = Alpha.disabled)
        ),
        shape = CustomShapes.Button,
        border = BorderStroke(BorderWidth.medium, EduRachaColors.Primary),
        contentPadding = PaddingValues(horizontal = Spacing.large)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// BOTÓN CON GRADIENTE
// ============================================
@Composable
fun EduRachaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradientColors: List<Color> = listOf(
        EduRachaColors.GradientStart,
        EduRachaColors.GradientEnd
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight)
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors),
                shape = CustomShapes.Button
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = Alpha.disabled)
        ),
        shape = CustomShapes.Button,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.large,
            pressedElevation = Elevation.extraLarge
        ),
        contentPadding = PaddingValues(horizontal = Spacing.large)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// BOTÓN DE TEXTO
// ============================================
@Composable
fun EduRachaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = EduRachaColors.Primary,
            disabledContentColor = EduRachaColors.Primary.copy(alpha = Alpha.disabled)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// BOTÓN CON ICONO (COLOREABLE)
// ============================================
@Composable
fun EduRachaIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = EduRachaColors.Primary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = Alpha.disabled),
            disabledContentColor = contentColor.copy(alpha = Alpha.disabled)
        ),
        shape = CustomShapes.Button,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.medium
        ),
        contentPadding = PaddingValues(horizontal = Spacing.large)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.iconSizeSmall)
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================
// BOTÓN GOOGLE (Con imagen)
// ============================================
@Composable
fun EduRachaGoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imageResId: Int
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF424242)
        ),
        shape = CustomShapes.Button,
        border = BorderStroke(BorderWidth.thin, Color(0xFFE0E0E0)),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.small
        )
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Google",
            modifier = Modifier.size(Dimensions.iconSize)
        )
        Spacer(modifier = Modifier.width(Spacing.medium))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF424242)
        )
    }
}

// ============================================
// BOTÓN PEQUEÑO (Para acciones secundarias)
// ============================================
@Composable
fun EduRachaSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    backgroundColor: Color = EduRachaColors.Primary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(Dimensions.buttonHeightSmall),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = CustomShapes.ButtonSmall,
        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.extraSmall))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================
// FAB (Floating Action Button) PREMIUM
// ============================================
@Composable
fun EduRachaFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    backgroundColor: Color = EduRachaColors.Secondary,
    contentColor: Color = Color.White,
    expanded: Boolean = false,
    text: String? = null
) {
    if (expanded && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = backgroundColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Elevation.large
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimensions.iconSize)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = backgroundColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Elevation.large
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimensions.iconSize)
            )
        }
    }
}