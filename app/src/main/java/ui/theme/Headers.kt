// ============================================
// 📁 ARCHIVO: Headers.kt
// 📂 Ubicación: app/src/main/java/com/stiven/desarrollomovil/ui/theme/components/Headers.kt
// 📱 Cabeceras, AppBars y Componentes de Navegación
// ============================================

package com.stiven.desarrollomovil.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// --- CORRECCIÓN AQUÍ ---
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// --- Y ASEGÚRATE DE TENER ESTA OTRA IMPORTACIÓN ---
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.stiven.desarrollomovil.ui.theme.*

// ============================================
// HEADER DE PERFIL PREMIUM (Como en tu imagen)
// ============================================
@Composable
fun EduRachaProfileHeader(
    name: String,
    email: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onBackClick: (() -> Unit)? = null,
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium, shape = CustomShapes.CardLarge),
        shape = CustomShapes.CardLarge,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botones superiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Row {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Avatar
            Box(
                modifier = Modifier
                    .size(Dimensions.avatarSizeExtraLarge)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .border(BorderWidth.thick, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Nombre
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            // Email
            Text(
                text = email,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// ============================================
// TOP APP BAR SIMPLE
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = EduRachaColors.Surface,
            titleContentColor = EduRachaColors.TextPrimary,
            navigationIconContentColor = EduRachaColors.TextPrimary,
            actionIconContentColor = EduRachaColors.TextPrimary
        )
    )
}

// ============================================
// TOP APP BAR CON GRADIENTE
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRachaGradientTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.topAppBarHeight)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        EduRachaColors.GradientStart,
                        EduRachaColors.GradientEnd
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(content = actions)
        }
    }
}

// ============================================
// HEADER DE SALUDO (Como en tu imagen)
// ============================================
@Composable
fun EduRachaGreetingHeader(
    greeting: String,
    userName: String,
    userEmail: String,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.screenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar y saludo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.avatarSizeMedium)
                    .background(EduRachaColors.Primary, CircleShape)
                    .border(BorderWidth.medium, EduRachaColors.PrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column {
                Text(
                    text = greeting,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = EduRachaColors.TextSecondary
                )
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Botones de acción
        Row {
            IconButton(onClick = onNotificationClick) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = EduRachaColors.TextPrimary
                    )
                    // Badge de notificaciones (opcional)
                    // EduRachaBadge(count = 3, modifier = Modifier.align(Alignment.TopEnd))
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    tint = EduRachaColors.TextPrimary
                )
            }
        }
    }
}

// ============================================
// SECCIÓN CON HEADER
// ============================================
@Composable
fun EduRachaSection(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = CustomTextStyles.SectionTitle,
                color = EduRachaColors.TextPrimary
            )

            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    if (actionIcon != null) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    }
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EduRachaColors.Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        Column(content = content)
    }
}

// ============================================
// BANNER INFORMATIVO
// ============================================
@Composable
fun EduRachaBanner(
    title: String,
    message: String,
    icon: ImageVector,
    backgroundColor: Color = EduRachaColors.InfoContainer,
    iconColor: Color = EduRachaColors.Info,
    textColor: Color = EduRachaColors.TextPrimary,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.small, shape = CustomShapes.Banner),
        shape = CustomShapes.Banner,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(Dimensions.iconSize)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor.copy(alpha = 0.8f)
                )

                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    TextButton(
                        onClick = onActionClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = actionText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
            }

            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// TAB ROW PERSONALIZADO
// ============================================
@Composable
fun EduRachaTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = EduRachaColors.Surface,
        contentColor = EduRachaColors.Primary,
        edgePadding = Spacing.screenPadding,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = EduRachaColors.Primary,
                    height = 3.dp
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selectedContentColor = EduRachaColors.Primary,
                unselectedContentColor = EduRachaColors.TextSecondary
            )
        }
    }
}
