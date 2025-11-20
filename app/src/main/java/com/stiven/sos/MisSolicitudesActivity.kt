package com.stiven.sos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MisSolicitudesActivity : ComponentActivity() {
    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        solicitudViewModel.cargarSolicitudesEstudiante(userUid)

        setContent {
            EduRachaTheme {
                MisSolicitudesScreen(
                    solicitudViewModel = solicitudViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

fun formatearFecha(fecha: String): String {
    return try {
        val timestamp = fecha.toLongOrNull() ?: return fecha
        val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        fecha
    }
}

fun formatearHora(fecha: String): String {
    return try {
        val timestamp = fecha.toLongOrNull() ?: return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun getDeviceType(): DeviceType {
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp >= 840 -> DeviceType.TABLET
        config.screenWidthDp >= 600 -> DeviceType.LANDSCAPE_PHONE
        else -> DeviceType.PHONE
    }
}

enum class DeviceType {
    PHONE, LANDSCAPE_PHONE, TABLET
}

@Composable
fun getPadding(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 32.dp
    DeviceType.LANDSCAPE_PHONE -> 24.dp
    DeviceType.PHONE -> 20.dp
}

@Composable
fun getGap(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 20.dp
    DeviceType.LANDSCAPE_PHONE -> 16.dp
    DeviceType.PHONE -> 14.dp
}

@Composable
fun getCardPadding(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 28.dp
    DeviceType.LANDSCAPE_PHONE -> 22.dp
    DeviceType.PHONE -> 18.dp
}

@Composable
fun getIconSize(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 40.dp
    DeviceType.LANDSCAPE_PHONE -> 32.dp
    DeviceType.PHONE -> 28.dp
}

@Composable
fun getSmallIconSize(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 28.dp
    DeviceType.LANDSCAPE_PHONE -> 24.dp
    DeviceType.PHONE -> 22.dp
}

@Composable
fun getTitleSize() = when (getDeviceType()) {
    DeviceType.TABLET -> 36.sp
    DeviceType.LANDSCAPE_PHONE -> 30.sp
    DeviceType.PHONE -> 26.sp
}

@Composable
fun getSubtitleSize() = when (getDeviceType()) {
    DeviceType.TABLET -> 20.sp
    DeviceType.LANDSCAPE_PHONE -> 18.sp
    DeviceType.PHONE -> 16.sp
}

@Composable
fun getBodySize() = when (getDeviceType()) {
    DeviceType.TABLET -> 18.sp
    DeviceType.LANDSCAPE_PHONE -> 16.sp
    DeviceType.PHONE -> 15.sp
}

@Composable
fun getSmallBodySize() = when (getDeviceType()) {
    DeviceType.TABLET -> 16.sp
    DeviceType.LANDSCAPE_PHONE -> 14.sp
    DeviceType.PHONE -> 13.sp
}

@Composable
fun getLabelSize() = when (getDeviceType()) {
    DeviceType.TABLET -> 15.sp
    DeviceType.LANDSCAPE_PHONE -> 13.sp
    DeviceType.PHONE -> 12.sp
}

@Composable
fun getCornerRadius(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 28.dp
    DeviceType.LANDSCAPE_PHONE -> 24.dp
    DeviceType.PHONE -> 20.dp
}

@Composable
fun getSmallCornerRadius(): Dp = when (getDeviceType()) {
    DeviceType.TABLET -> 20.dp
    DeviceType.LANDSCAPE_PHONE -> 16.dp
    DeviceType.PHONE -> 14.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesScreen(
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()
    val padding = getPadding()
    val gap = getGap()

    var filtroSeleccionado by remember { mutableStateOf<EstadoSolicitud?>(null) }

    val solicitudesFiltradas = remember(solicitudUiState.solicitudes, filtroSeleccionado) {
        val filtro = filtroSeleccionado
        if (filtro == null) {
            solicitudUiState.solicitudes
        } else {
            solicitudUiState.solicitudes.filter { it.estado == filtro }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
    ) {
        when {
            solicitudUiState.isLoading -> ModernLoadingScreen()
            solicitudUiState.solicitudes.isEmpty() -> ModernEmptyState()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = gap * 2)
            ) {
                item {
                    ModernTopBar(onNavigateBack, solicitudUiState.solicitudes.size)
                }

                item {
                    Spacer(Modifier.height(gap))
                    Box(modifier = Modifier.padding(horizontal = padding)) {
                        StatsCards(
                            solicitudes = solicitudUiState.solicitudes,
                            filtroSeleccionado = filtroSeleccionado,
                            onFiltroClick = { nuevoFiltro: EstadoSolicitud ->
                                filtroSeleccionado = if (filtroSeleccionado == nuevoFiltro) null else nuevoFiltro
                            }
                        )
                    }
                    Spacer(Modifier.height(gap))
                }

                if (solicitudesFiltradas.isEmpty() && filtroSeleccionado != null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = padding)) {
                            EmptyFilterState(filtroSeleccionado!!)
                        }
                    }
                } else {
                    items(solicitudesFiltradas, key = { it.id!! }) { solicitud ->
                        Box(modifier = Modifier.padding(horizontal = padding, vertical = gap * 0.6f)) {
                            ModernSolicitudCard(solicitud)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTopBar(onNavigateBack: () -> Unit, total: Int) {
    val titleSize = getTitleSize()
    val subtitleSize = getSubtitleSize()
    val padding = getPadding()
    val gap = getGap()
    val iconSize = getIconSize()

    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.PrimaryLight
                        )
                    )
                )
                .padding(horizontal = padding, vertical = padding * 1.2f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                var backPressed by remember { mutableStateOf(false) }

                IconButton(
                    onClick = {
                        backPressed = true
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(iconSize * 1.6f)
                        .graphicsLayer {
                            scaleX = if (backPressed) 0.85f else 1f
                            scaleY = if (backPressed) 0.85f else 1f
                        }
                        .background(Color.White.copy(0.25f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(gap * 0.3f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(gap * 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mis Solicitudes",
                            color = Color.White,
                            fontSize = titleSize * 0.95f,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (total > 0) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = badgeScale
                                        scaleY = badgeScale
                                    }
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                EduRachaColors.Accent,
                                                EduRachaColors.AccentLight
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .padding(horizontal = gap * 1.1f, vertical = gap * 0.5f)
                            ) {
                                Text(
                                    text = total.toString(),
                                    color = Color.White,
                                    fontSize = subtitleSize * 0.95f,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Seguimiento académico",
                        color = Color.White.copy(0.95f),
                        fontSize = subtitleSize * 0.85f,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "iconRotation"
                )

                Box(
                    modifier = Modifier
                        .size(iconSize * 1.8f)
                        .graphicsLayer { rotationZ = rotation }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color.White.copy(0.3f),
                                    Color.White.copy(0.12f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(iconSize * 1.1f)
                            .graphicsLayer { rotationZ = -rotation }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCards(
    solicitudes: List<SolicitudCurso>,
    filtroSeleccionado: EstadoSolicitud?,
    onFiltroClick: (EstadoSolicitud) -> Unit
) {
    val pendientes = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE }
    val aceptadas = solicitudes.count { it.estado == EstadoSolicitud.ACEPTADA }
    val rechazadas = solicitudes.count { it.estado == EstadoSolicitud.RECHAZADA }
    val gap = getGap()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap * 1.2f)
    ) {
        FilterableStatChip(
            count = pendientes,
            label = "Pendientes",
            icon = Icons.Default.HourglassTop,
            color = EduRachaColors.Warning,
            estado = EstadoSolicitud.PENDIENTE,
            isSelected = filtroSeleccionado == EstadoSolicitud.PENDIENTE,
            onClick = { onFiltroClick(EstadoSolicitud.PENDIENTE) },
            modifier = Modifier.weight(1f)
        )
        FilterableStatChip(
            count = aceptadas,
            label = "Aceptadas",
            icon = Icons.Default.CheckCircle,
            color = EduRachaColors.Success,
            estado = EstadoSolicitud.ACEPTADA,
            isSelected = filtroSeleccionado == EstadoSolicitud.ACEPTADA,
            onClick = { onFiltroClick(EstadoSolicitud.ACEPTADA) },
            modifier = Modifier.weight(1f)
        )
        FilterableStatChip(
            count = rechazadas,
            label = "Rechazadas",
            icon = Icons.Default.Cancel,
            color = EduRachaColors.Error,
            estado = EstadoSolicitud.RECHAZADA,
            isSelected = filtroSeleccionado == EstadoSolicitud.RECHAZADA,
            onClick = { onFiltroClick(EstadoSolicitud.RECHAZADA) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FilterableStatChip(
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    estado: EstadoSolicitud,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = getGap()
    val smallIconSize = getSmallIconSize()
    val bodySize = getBodySize()
    val labelSize = getLabelSize()

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(0.22f) else color.copy(0.1f),
        animationSpec = tween(350),
        label = "bgColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(350),
        label = "border"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(getSmallCornerRadius() * 1.2f),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, color),
        shadowElevation = 0.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = gap * 1.5f, horizontal = gap * 1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap * 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSelected) smallIconSize * 2.2f else smallIconSize * 2f)
                    .background(
                        if (isSelected) color.copy(0.25f) else color.copy(0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (isSelected) smallIconSize * 1.3f else smallIconSize * 1.2f)
                )
            }

            Text(
                text = count.toString(),
                fontSize = if (isSelected) bodySize * 2f else bodySize * 1.8f,
                fontWeight = FontWeight.Black,
                color = color
            )

            Text(
                text = label,
                fontSize = if (isSelected) labelSize * 1.15f else labelSize * 1.05f,
                color = if (isSelected) color else EduRachaColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(gap * 3f)
                        .height(3.dp)
                        .background(color, RoundedCornerShape(100.dp))
                )
            }
        }
    }
}

@Composable
fun EmptyFilterState(filtro: EstadoSolicitud) {
    val gap = getGap()
    val iconSize = getIconSize()
    val bodySize = getBodySize()
    val titleSize = getTitleSize()

    val (texto, color, icono) = when (filtro) {
        EstadoSolicitud.PENDIENTE -> Triple(
            "No hay solicitudes pendientes",
            EduRachaColors.Warning,
            Icons.Default.HourglassTop
        )
        EstadoSolicitud.ACEPTADA -> Triple(
            "No hay solicitudes aceptadas",
            EduRachaColors.Success,
            Icons.Default.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Triple(
            "No hay solicitudes rechazadas",
            EduRachaColors.Error,
            Icons.Default.Cancel
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = gap * 6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap * 2f)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * 4f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(0.25f),
                            color.copy(0.08f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = color.copy(0.7f),
                modifier = Modifier.size(iconSize * 2.2f)
            )
        }

        Text(
            text = texto,
            fontSize = titleSize * 0.85f,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Toca el filtro nuevamente para ver todas las solicitudes",
            fontSize = bodySize,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = bodySize * 1.4f
        )
    }
}

@Composable
fun ModernSolicitudCard(solicitud: SolicitudCurso) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { it / 3 }
    ) {
        SolicitudCardContent(solicitud)
    }
}

@Composable
fun SolicitudCardContent(solicitud: SolicitudCurso) {
    val (colors, texto, icono) = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> Triple(
            listOf(EduRachaColors.Warning, EduRachaColors.WarningLight),
            "En Revisión",
            Icons.Default.HourglassTop
        )
        EstadoSolicitud.ACEPTADA -> Triple(
            listOf(EduRachaColors.Success, EduRachaColors.SuccessLight),
            "Aceptada",
            Icons.Default.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Triple(
            listOf(EduRachaColors.Error, EduRachaColors.ErrorLight),
            "Rechazada",
            Icons.Default.Cancel
        )
    }

    val cardPadding = getCardPadding()
    val bodySize = getBodySize()
    val smallBodySize = getSmallBodySize()
    val subtitleSize = getSubtitleSize()
    val gap = getGap()
    val iconSize = getIconSize()
    val smallIconSize = getSmallIconSize()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(getCornerRadius()),
        color = EduRachaColors.Surface,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors[0].copy(0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header mejorado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors.map { it.copy(0.15f) }
                        )
                    )
                    .padding(cardPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap * 1.2f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(gap * 1.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(iconSize * 2f)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(colors[0].copy(0.3f), colors[0].copy(0.15f))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = colors[0],
                                    modifier = Modifier.size(iconSize * 1.1f)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(gap * 0.4f)
                            ) {
                                Text(
                                    text = solicitud.codigoCurso,
                                    fontSize = subtitleSize * 1.3f,
                                    fontWeight = FontWeight.Black,
                                    color = colors[0],
                                    maxLines = 1
                                )

                                Text(
                                    text = solicitud.nombreCurso.ifBlank { "Curso ${solicitud.codigoCurso}" },
                                    fontSize = bodySize * 1.05f,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EduRachaColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = bodySize * 1.3f
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(colors),
                                    RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = gap * 1.8f, vertical = gap * 1.2f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(gap * 0.8f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icono,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(smallIconSize)
                                )
                                Text(
                                    text = texto,
                                    fontSize = smallBodySize * 1.15f,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Contenido mejorado
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(gap * 1.5f)
            ) {
                // Fecha de solicitud
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colors[0].copy(0.08f),
                            RoundedCornerShape(getSmallCornerRadius())
                        )
                        .padding(gap * 1.5f),
                    horizontalArrangement = Arrangement.spacedBy(gap * 1.2f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(smallIconSize * 1.8f)
                            .background(colors[0].copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = colors[0],
                            modifier = Modifier.size(smallIconSize)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(gap * 0.3f)) {
                        Text(
                            text = "Fecha de Solicitud",
                            fontSize = smallBodySize,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextSecondary
                        )
                        Text(
                            text = formatearFecha(solicitud.fechaSolicitud),
                            fontSize = bodySize * 1.05f,
                            fontWeight = FontWeight.SemiBold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = formatearHora(solicitud.fechaSolicitud),
                            fontSize = smallBodySize * 0.95f,
                            fontWeight = FontWeight.Medium,
                            color = colors[0]
                        )
                    }
                }

                // Mensaje del estudiante
                solicitud.mensajeEstudiante?.takeIf { it.isNotBlank() }?.let { mensaje ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(getSmallCornerRadius()),
                        color = EduRachaColors.Primary.copy(0.06f),
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EduRachaColors.Primary.copy(0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(gap * 1.5f),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(smallIconSize * 1.5f)
                                        .background(EduRachaColors.Primary.copy(0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EduRachaColors.Primary,
                                        modifier = Modifier.size(smallIconSize * 0.9f)
                                    )
                                }
                                Text(
                                    text = "Tu Mensaje",
                                    fontSize = smallBodySize * 1.1f,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Primary
                                )
                            }
                            Text(
                                text = mensaje,
                                fontSize = bodySize,
                                color = EduRachaColors.TextPrimary,
                                lineHeight = bodySize * 1.5f,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Respuesta del docente
                if (solicitud.fechaRespuesta != null) {
                    Divider(
                        modifier = Modifier.padding(vertical = gap * 0.5f),
                        color = colors[0].copy(0.2f),
                        thickness = 2.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colors[0].copy(0.08f),
                                RoundedCornerShape(getSmallCornerRadius())
                            )
                            .padding(gap * 1.5f),
                        horizontalArrangement = Arrangement.spacedBy(gap * 1.2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(smallIconSize * 1.8f)
                                .background(colors[0].copy(0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = colors[0],
                                modifier = Modifier.size(smallIconSize)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(gap * 0.3f)) {
                            Text(
                                text = "Fecha de Respuesta",
                                fontSize = smallBodySize,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextSecondary
                            )
                            Text(
                                text = formatearFecha(solicitud.fechaRespuesta),
                                fontSize = bodySize * 1.05f,
                                fontWeight = FontWeight.SemiBold,
                                color = EduRachaColors.TextPrimary
                            )
                            Text(
                                text = formatearHora(solicitud.fechaRespuesta),
                                fontSize = smallBodySize * 0.95f,
                                fontWeight = FontWeight.Medium,
                                color = colors[0]
                            )
                        }
                    }

                    solicitud.mensajeDocente?.takeIf { it.isNotBlank() }?.let { mensaje ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(getSmallCornerRadius()),
                            color = colors[0].copy(0.08f),
                            shadowElevation = 0.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors[0].copy(0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(gap * 1.5f),
                                verticalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(gap),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(smallIconSize * 1.5f)
                                            .background(colors[0].copy(0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = colors[0],
                                            modifier = Modifier.size(smallIconSize * 0.9f)
                                        )
                                    }
                                    Text(
                                        text = "Respuesta del Docente",
                                        fontSize = smallBodySize * 1.1f,
                                        fontWeight = FontWeight.Bold,
                                        color = colors[0]
                                    )
                                }
                                Text(
                                    text = mensaje,
                                    fontSize = bodySize,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = bodySize * 1.5f,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernLoadingScreen() {
    val iconSize = getIconSize()
    val titleSize = getTitleSize()
    val subtitleSize = getSubtitleSize()
    val gap = getGap()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        EduRachaColors.Background,
                        EduRachaColors.BackgroundSecondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap * 2.5f)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                0f,
                360f,
                infiniteRepeatable(tween(1200, easing = LinearEasing)),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(iconSize * 3.5f)
                    .graphicsLayer { rotationZ = rotation }
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                EduRachaColors.Primary,
                                EduRachaColors.Accent,
                                EduRachaColors.Primary
                            )
                        ),
                        CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .background(EduRachaColors.Background, CircleShape)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap * 0.8f)
            ) {
                Text(
                    text = "Cargando solicitudes",
                    fontSize = titleSize * 0.85f,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = "Un momento por favor...",
                    fontSize = subtitleSize,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun ModernEmptyState() {
    val padding = getPadding()
    val gap = getGap()
    val iconSize = getIconSize()
    val titleSize = getTitleSize()
    val bodySize = getBodySize()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        EduRachaColors.Background,
                        EduRachaColors.AccentContainer.copy(0.03f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding * 1.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val scale by rememberInfiniteTransition(label = "empty").animateFloat(
                1f,
                1.08f,
                infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(iconSize * 4f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                EduRachaColors.Accent.copy(0.15f),
                                EduRachaColors.Accent.copy(0.05f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AssignmentLate,
                    null,
                    modifier = Modifier.size(iconSize * 2.2f),
                    tint = EduRachaColors.Accent.copy(0.6f)
                )
            }

            Spacer(Modifier.height(gap * 3f))

            Text(
                text = "No tienes solicitudes",
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(gap * 1.2f))

            Text(
                text = "Comienza explorando cursos disponibles\ny solicita unirte a las clases que te interesen",
                fontSize = bodySize * 1.05f,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = bodySize * 1.5f,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(gap * 3.5f))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(gap * 4f),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, EduRachaColors.Primary.copy(0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.PrimaryLight
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap * 1.2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            null,
                            modifier = Modifier.size(gap * 1.8f),
                            tint = Color.White
                        )
                        Text(
                            text = "Explorar Cursos",
                            fontSize = bodySize * 1.15f,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(gap * 2.5f))

            Surface(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(getSmallCornerRadius() * 1.2f),
                color = EduRachaColors.Surface,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, EduRachaColors.Success.copy(0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(gap * 2f),
                    horizontalArrangement = Arrangement.spacedBy(gap * 1.5f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize * 1.5f)
                            .background(EduRachaColors.Success.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(gap * 1.5f)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(gap * 0.5f)) {
                        Text(
                            text = "¿Cómo funciona?",
                            fontSize = bodySize * 1.05f,
                            fontWeight = FontWeight.ExtraBold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = "Solicita unirte a un curso y recibe respuestas del docente en tiempo real",
                            fontSize = bodySize * 0.9f,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = bodySize * 1.3f,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}