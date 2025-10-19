

package com.stiven.sos.ui.theme.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.stiven.sos.ui.theme.*

// ============================================
// CHIP PREMIUM
// ============================================
@Composable
fun EduRachaChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EduRachaColors.Primary,
    textColor: Color = Color.White,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = CustomShapes.Chip,
        color = backgroundColor,
        shadowElevation = Elevation.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.medium,
                vertical = Spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

// ============================================
// CHIP SELECCIONABLE (Para filtros)
// ============================================
@Composable
fun EduRachaSelectableChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val backgroundColor = if (selected) EduRachaColors.Primary else EduRachaColors.SurfaceVariant
    val textColor = if (selected) Color.White else EduRachaColors.TextPrimary

    Surface(
        modifier = modifier.clickable { onSelectedChange(!selected) },
        shape = CustomShapes.Chip,
        color = backgroundColor,
        shadowElevation = if (selected) Elevation.small else Elevation.none,
        border = if (!selected) BorderStroke(BorderWidth.thin, EduRachaColors.Border) else null
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.medium,
                vertical = Spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ============================================
// BADGE DE NOTIFICACIÓN
// ============================================
@Composable
fun EduRachaBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EduRachaColors.Error,
    textColor: Color = Color.White,
    maxCount: Int = 99
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .size(Dimensions.badgeSize)
                .background(backgroundColor, CircleShape)
                .border(BorderWidth.medium, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > maxCount) "$maxCount+" else count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// BADGE DE NIVEL (Gamificación)
// ============================================
@Composable
fun EduRachaLevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val badgeColor = when {
        level < 10 -> EduRachaColors.LevelBronze
        level < 20 -> EduRachaColors.LevelSilver
        level < 30 -> EduRachaColors.LevelGold
        level < 50 -> EduRachaColors.LevelPlatinum
        else -> EduRachaColors.LevelDiamond
    }

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.3f),
                        badgeColor.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
            .border(BorderWidth.thick, badgeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.toString(),
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
}

// ============================================
// PROGRESS BAR DE XP (Animado)
// ============================================
@Composable
fun EduRachaXpProgressBar(
    currentXp: Int,
    targetXp: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    height: Dp = Dimensions.progressBarHeight
) {
    val progress = (currentXp.toFloat() / targetXp.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Column(modifier = modifier) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progreso al siguiente nivel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextSecondary
                )
                Text(
                    text = "$currentXp/$targetXp XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Primary
                )
            }
            Spacer(modifier = Modifier.height(Spacing.small))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(CustomShapes.ProgressBar)
                .background(EduRachaColors.XpBarBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CustomShapes.ProgressBar)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Accent,
                                EduRachaColors.Primary
                            )
                        )
                    )
            )
        }
    }
}

// ============================================
// PROGRESS BAR SIMPLE
// ============================================
@Composable
fun EduRachaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    color: Color = EduRachaColors.Primary,
    backgroundColor: Color = EduRachaColors.SurfaceVariant,
    height: Dp = Dimensions.progressBarHeight,
    showPercentage: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800)
    )

    Column(modifier = modifier) {
        if (label != null || showPercentage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }
                if (showPercentage) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.small))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(CustomShapes.ProgressBar)
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CustomShapes.ProgressBar)
                    .background(color)
            )
        }
    }
}

// ============================================
// DIVIDER PREMIUM
// ============================================
@Composable
fun EduRachaDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = Dimensions.dividerThickness,
    color: Color = EduRachaColors.Divider
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

// ============================================
// DIVIDER CON TEXTO
// ============================================
@Composable
fun EduRachaDividerWithText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = EduRachaColors.Divider,
    textColor: Color = EduRachaColors.TextSecondary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = color
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = Spacing.medium)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = color
        )
    }
}

// ============================================
// STREAK INDICATOR (Racha de días)
// ============================================
@Composable
fun EduRachaStreakIndicator(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CustomShapes.Badge,
        color = EduRachaColors.StreakFire.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.medium,
                vertical = Spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = EduRachaColors.StreakFire,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.extraSmall))
            Text(
                text = "$streakDays días",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.StreakFire
            )
        }
    }
}

// ============================================
// LOADING INDICATOR PERSONALIZADO
// ============================================
@Composable
fun EduRachaLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Cargando...",
    color: Color = EduRachaColors.Primary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = color,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================
// ESTADO VACÍO (Empty State)
// ============================================
@Composable
fun EduRachaEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EduRachaColors.TextTertiary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = description,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.large))
            EduRachaPrimaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
    }
}