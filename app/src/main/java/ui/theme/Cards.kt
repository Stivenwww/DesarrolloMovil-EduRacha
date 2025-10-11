

package com.stiven.desarrollomovil.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.*

// ============================================
// CARD BÁSICA PREMIUM
// ============================================
@Composable
fun EduRachaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = Elevation.medium,
    backgroundColor: Color = EduRachaColors.Surface,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = CustomShapes.Card,
                clip = false
            ),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = borderColor?.let { BorderStroke(BorderWidth.thin, it) },
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            content = content
        )
    }
}

// ============================================
// CARD DE ESTADÍSTICAS (Como en tu imagen)
// ============================================
@Composable
fun EduRachaStatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color = Color.White,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trend: String? = null,
    trendPositive: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .width(Dimensions.cardMinWidth)
            .shadow(
                elevation = Elevation.medium,
                shape = CustomShapes.Card
            ),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        ),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.cardPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono con fondo circular
            Box(
                modifier = Modifier
                    .size(Dimensions.iconSizeExtraLarge + 8.dp)
                    .background(
                        color = iconBackgroundColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Dimensions.iconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Valor principal
            Text(
                text = value,
                style = CustomTextStyles.StatValue,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            // Título
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            // Subtítulo opcional
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = EduRachaColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            // Tendencia opcional
            if (trend != null) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Surface(
                    color = if (trendPositive) EduRachaColors.SuccessContainer else EduRachaColors.ErrorContainer,
                    shape = CustomShapes.Chip
                ) {
                    Text(
                        text = trend,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (trendPositive) EduRachaColors.Success else EduRachaColors.Error,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
                    )
                }
            }
        }
    }
}

// ============================================
// CARD DE RANKING (Como en tu imagen)
// ============================================
@Composable
fun EduRachaRankingCard(
    position: Int,
    userName: String,
    subtitle: String,
    badgeIcon: ImageVector,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onClick: (() -> Unit)? = null
) {
    val rankingColor = when (position) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.Accent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.small, shape = CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        ),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición con badge
            Box(
                modifier = Modifier
                    .size(Dimensions.avatarSizeMedium)
                    .background(
                        color = rankingColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankingColor
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Información del usuario
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = EduRachaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Badge de premio
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = rankingColor,
                modifier = Modifier.size(Dimensions.iconSizeLarge)
            )
        }
    }
}

// ============================================
// CARD DE ACCIÓN PRINCIPAL (Como en tu imagen)
// ============================================
@Composable
fun EduRachaActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.cardMinHeight)
            .shadow(elevation = Elevation.large, shape = CustomShapes.CardLarge),
        shape = CustomShapes.CardLarge,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(Dimensions.iconSize)
                    )
                    Spacer(modifier = Modifier.width(Spacing.medium))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = contentColor.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(Dimensions.iconSize)
            )
        }
    }
}

// ============================================
// CARD DE ASIGNATURA
// ============================================
@Composable
fun EduRachaSubjectCard(
    subjectName: String,
    teacherName: String,
    studentsCount: Int,
    pendingTasks: Int,
    subjectColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium, shape = CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con color de asignatura
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(subjectColor)
            )

            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                // Título de la asignatura
                Text(
                    text = subjectName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                // Profesor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = EduRachaColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text(
                        text = teacherName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Estadísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Estudiantes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                        Text(
                            text = "$studentsCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                        Text(
                            text = "estudiantes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = EduRachaColors.TextSecondary
                        )
                    }

                    // Pendientes
                    if (pendingTasks > 0) {
                        Surface(
                            color = EduRachaColors.WarningContainer,
                            shape = CustomShapes.Badge
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Spacing.small,
                                    vertical = Spacing.extraSmall
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                                Text(
                                    text = "$pendingTasks pendientes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EduRachaColors.Warning
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// CARD COMPACTA (Para listas)
// ============================================
@Composable
fun EduRachaCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = EduRachaColors.Primary,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.small, shape = CustomShapes.CardSmall),
        shape = CustomShapes.CardSmall,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Contenido
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = EduRachaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Contenido adicional (opcional)
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(Spacing.small))
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = EduRachaColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================
// CARD CON IMAGEN
// ============================================
@Composable
fun EduRachaImageCard(
    title: String,
    description: String,
    imageResId: Int? = null,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = EduRachaColors.Secondary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium, shape = CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        ),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Imagen placeholder o real
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(EduRachaColors.PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = EduRachaColors.Primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )

                // Badge opcional
                if (badge != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.small),
                        color = badgeColor,
                        shape = CustomShapes.Badge
                    ) {
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = Spacing.small,
                                vertical = Spacing.extraSmall
                            )
                        )
                    }
                }
            }

            // Contenido
            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = EduRachaColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}