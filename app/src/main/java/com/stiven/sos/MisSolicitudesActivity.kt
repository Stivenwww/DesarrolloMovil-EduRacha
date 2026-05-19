package com.stiven.sos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.SolicitudViewModel
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
                MisSolicitudesScreenV2(
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
fun MisSolicitudesScreenV2(
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()
    var filtroSeleccionado by remember { mutableStateOf<EstadoSolicitud?>(null) }
    var textoBusqueda by remember { mutableStateOf("") }

    val solicitudesFiltradas = remember(solicitudUiState.solicitudes, filtroSeleccionado, textoBusqueda) {
        var resultado = solicitudUiState.solicitudes

        if (filtroSeleccionado != null) {
            resultado = resultado.filter { it.estado == filtroSeleccionado }
        }

        if (textoBusqueda.isNotBlank()) {
            resultado = resultado.filter { solicitud ->
                solicitud.codigoCurso.contains(textoBusqueda, ignoreCase = true) ||
                        solicitud.nombreCurso.contains(textoBusqueda, ignoreCase = true) ||
                        solicitud.mensajeEstudiante?.contains(textoBusqueda, ignoreCase = true) == true
            }
        }

        resultado
    }

    EduRachaV2Container {
        when {
            solicitudUiState.isLoading -> LoadingSolicitudesV2()
            solicitudUiState.solicitudes.isEmpty() -> EmptySolicitudesV2(onNavigateBack)
            else -> ListaSolicitudesV2(
                solicitudes = solicitudUiState.solicitudes,
                solicitudesFiltradas = solicitudesFiltradas,
                filtroSeleccionado = filtroSeleccionado,
                textoBusqueda = textoBusqueda,
                onTextoBusquedaChange = { textoBusqueda = it },
                onFiltroClick = { nuevoFiltro ->
                    filtroSeleccionado = if (filtroSeleccionado == nuevoFiltro) null else nuevoFiltro
                },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
fun ListaSolicitudesV2(
    solicitudes: List<SolicitudCurso>,
    solicitudesFiltradas: List<SolicitudCurso>,
    filtroSeleccionado: EstadoSolicitud?,
    textoBusqueda: String,
    onTextoBusquedaChange: (String) -> Unit,
    onFiltroClick: (EstadoSolicitud) -> Unit,
    onNavigateBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeaderSolicitudesV2(
                total = solicitudes.size,
                onNavigateBack = onNavigateBack
            )
        }

        item { Spacer(Modifier.height(24.dp)) }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                BuscadorSolicitudes(
                    textoBusqueda = textoBusqueda,
                    onTextoBusquedaChange = onTextoBusquedaChange
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                StatsCardsV2(
                    solicitudes = solicitudes,
                    filtroSeleccionado = filtroSeleccionado,
                    onFiltroClick = onFiltroClick
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (solicitudesFiltradas.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (textoBusqueda.isNotBlank()) {
                        EmptySearchStateV2(textoBusqueda)
                    } else if (filtroSeleccionado != null) {
                        EmptyFilterStateV2(filtroSeleccionado)
                    }
                }
            }
        } else {
            items(items = solicitudesFiltradas) { solicitud ->
                val index = solicitudesFiltradas.indexOf(solicitud)
                var visible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * 80L)
                    visible = true
                }

                val offsetY by animateFloatAsState(
                    targetValue = if (visible) 0f else 50f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )

                val alpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = tween(400)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            translationY = offsetY
                            this.alpha = alpha
                        }
                ) {
                    SolicitudCardV2(solicitud)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun HeaderSolicitudesV2(
    total: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                EduRachaV2Gradients.Purple,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        AnimatedBubblesDecoration(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            color = Color.White
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedSolicitudIcon()

            Spacer(Modifier.height(20.dp))

            Text(
                "Mis Solicitudes",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Seguimiento de inscripciones",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            ContadorSolicitudes(cantidad = total)
        }
    }
}

@Composable
fun AnimatedSolicitudIcon() {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Assignment,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ContadorSolicitudes(cantidad: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                var displayCount by remember { mutableStateOf(0) }

                LaunchedEffect(cantidad) {
                    animate(
                        initialValue = 0f,
                        targetValue = cantidad.toFloat(),
                        animationSpec = tween(1200, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        displayCount = value.toInt()
                    }
                }

                Text(
                    "$displayCount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Column {
                Text(
                    "Solicitudes totales",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Gestiona tus inscripciones",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Outlined.Ballot,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BuscadorSolicitudes(
    textoBusqueda: String,
    onTextoBusquedaChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = EduRachaV2Colors.Primary,
                modifier = Modifier.size(24.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (textoBusqueda.isEmpty()) {
                    Text(
                        "Buscar por código o nombre...",
                        fontSize = 15.sp,
                        color = EduRachaV2Colors.TextSecondary.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = textoBusqueda,
                    onValueChange = onTextoBusquedaChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,
                        color = EduRachaV2Colors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(EduRachaV2Colors.Primary)
                )
            }

            if (textoBusqueda.isNotEmpty()) {
                IconButton(
                    onClick = { onTextoBusquedaChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Limpiar",
                        tint = EduRachaV2Colors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCardsV2(
    solicitudes: List<SolicitudCurso>,
    filtroSeleccionado: EstadoSolicitud?,
    onFiltroClick: (EstadoSolicitud) -> Unit
) {
    val pendientes = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE }
    val aceptadas = solicitudes.count { it.estado == EstadoSolicitud.ACEPTADA }
    val rechazadas = solicitudes.count { it.estado == EstadoSolicitud.RECHAZADA }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChipV2(
            count = pendientes,
            label = "Pendientes",
            icon = Icons.Outlined.HourglassEmpty,
            gradient = EduRachaV2Gradients.Orange,
            color = EduRachaV2Colors.Warning,
            estado = EstadoSolicitud.PENDIENTE,
            isSelected = filtroSeleccionado == EstadoSolicitud.PENDIENTE,
            onClick = { onFiltroClick(EstadoSolicitud.PENDIENTE) },
            modifier = Modifier.weight(1f)
        )
        StatChipV2(
            count = aceptadas,
            label = "Aceptadas",
            icon = Icons.Outlined.CheckCircle,
            gradient = EduRachaV2Gradients.Green,
            color = EduRachaV2Colors.Success,
            estado = EstadoSolicitud.ACEPTADA,
            isSelected = filtroSeleccionado == EstadoSolicitud.ACEPTADA,
            onClick = { onFiltroClick(EstadoSolicitud.ACEPTADA) },
            modifier = Modifier.weight(1f)
        )
        StatChipV2(
            count = rechazadas,
            label = "Rechazadas",
            icon = Icons.Outlined.Cancel,
            gradient = EduRachaV2Gradients.Pink,
            color = EduRachaV2Colors.Pink,
            estado = EstadoSolicitud.RECHAZADA,
            isSelected = filtroSeleccionado == EstadoSolicitud.RECHAZADA,
            onClick = { onFiltroClick(EstadoSolicitud.RECHAZADA) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatChipV2(
    count: Int,
    label: String,
    icon: ImageVector,
    gradient: Brush,
    color: Color,
    estado: EstadoSolicitud,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .background(
                    if (isSelected) gradient
                    else Brush.linearGradient(
                        listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.1f))
                    )
                )
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 52.dp else 48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.25f)
                            else color.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = if (isSelected) Color.White else color,
                        modifier = Modifier.size(if (isSelected) 28.dp else 26.dp)
                    )
                }

                Text(
                    count.toString(),
                    fontSize = if (isSelected) 28.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else color
                )

                Text(
                    label,
                    fontSize = if (isSelected) 13.sp else 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White.copy(alpha = 0.95f) else color,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun SolicitudCardV2(solicitud: SolicitudCurso) {
    data class EstiloSolicitud(
        val gradient: Brush,
        val texto: String,
        val icono: ImageVector,
        val color: Color
    )

    val estilo = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> EstiloSolicitud(
            EduRachaV2Gradients.Orange,
            "En Revisión",
            Icons.Outlined.HourglassEmpty,
            EduRachaV2Colors.Warning
        )
        EstadoSolicitud.ACEPTADA -> EstiloSolicitud(
            EduRachaV2Gradients.Green,
            "Aceptada ✓",
            Icons.Outlined.CheckCircle,
            EduRachaV2Colors.Success
        )
        EstadoSolicitud.RECHAZADA -> EstiloSolicitud(
            EduRachaV2Gradients.Pink,
            "Rechazada",
            Icons.Outlined.Cancel,
            EduRachaV2Colors.Pink
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(estilo.gradient)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedCardIcon()

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                solicitud.codigoCurso,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            solicitud.nombreCurso.ifBlank { "Curso ${solicitud.codigoCurso}" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                estilo.icono,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                estilo.texto,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRowV2(
                    icon = Icons.Outlined.Event,
                    label = "Fecha de Solicitud",
                    value = formatearFecha(solicitud.fechaSolicitud),
                    subValue = formatearHora(solicitud.fechaSolicitud),
                    color = estilo.color
                )

                solicitud.mensajeEstudiante?.takeIf { it.isNotBlank() }?.let { mensaje ->
                    MensajeBoxV2(
                        icon = Icons.Outlined.Person,
                        titulo = "Tu Mensaje",
                        mensaje = mensaje,
                        color = EduRachaV2Colors.Primary
                    )
                }

                if (solicitud.fechaRespuesta != null) {
                    Divider(color = estilo.color.copy(alpha = 0.2f), thickness = 1.dp)

                    InfoRowV2(
                        icon = Icons.Outlined.Schedule,
                        label = "Fecha de Respuesta",
                        value = formatearFecha(solicitud.fechaRespuesta),
                        subValue = formatearHora(solicitud.fechaRespuesta),
                        color = estilo.color
                    )

                    solicitud.mensajeDocente?.takeIf { it.isNotBlank() }?.let { mensaje ->
                        MensajeBoxV2(
                            icon = Icons.Outlined.School,
                            titulo = "Respuesta del Docente",
                            mensaje = mensaje,
                            color = estilo.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCardIcon() {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun InfoRowV2(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextSecondary
                )
                Text(
                    value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary
                )
                Text(
                    subValue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

@Composable
fun MensajeBoxV2(
    icon: ImageVector,
    titulo: String,
    mensaje: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Text(
                mensaje,
                fontSize = 14.sp,
                color = EduRachaV2Colors.TextPrimary,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptySearchStateV2(textoBusqueda: String) {
    EduRachaV2EmptyState(
        icon = Icons.Outlined.SearchOff,
        iconColor = EduRachaV2Colors.Secondary,
        title = "Sin resultados",
        message = "No se encontraron solicitudes con \"$textoBusqueda\". Intenta con otro término de búsqueda 🔍",
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun EmptyFilterStateV2(filtro: EstadoSolicitud) {
    val (texto, color, icono) = when (filtro) {
        EstadoSolicitud.PENDIENTE -> Triple(
            "Sin solicitudes pendientes",
            EduRachaV2Colors.Warning,
            Icons.Outlined.HourglassEmpty
        )
        EstadoSolicitud.ACEPTADA -> Triple(
            "Sin solicitudes aceptadas",
            EduRachaV2Colors.Success,
            Icons.Outlined.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Triple(
            "Sin solicitudes rechazadas",
            EduRachaV2Colors.Pink,
            Icons.Outlined.Cancel
        )
    }

    EduRachaV2EmptyState(
        icon = icono,
        iconColor = color,
        title = texto,
        message = "Toca el filtro nuevamente para ver todas las solicitudes 🔄",
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun LoadingSolicitudesV2() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EduRachaV2Colors.Secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = EduRachaV2Colors.Secondary,
                    strokeWidth = 4.dp
                )
            }

            Text(
                "Cargando solicitudes...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaV2Colors.TextPrimary
            )

            Text(
                "Preparando tu información 📋",
                fontSize = 14.sp,
                color = EduRachaV2Colors.TextSecondary
            )
        }
    }
}

@Composable
fun EmptySolicitudesV2(onNavigateBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        EduRachaV2Gradients.Purple,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AssignmentLate,
                        null,
                        modifier = Modifier.size(60.dp),
                        tint = EduRachaV2Colors.Accent
                    )
                }

                Text(
                    "No tienes solicitudes",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Comienza explorando cursos disponibles y solicita unirte a las clases que te interesen",
                    fontSize = 15.sp,
                    color = EduRachaV2Colors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(8.dp))

                EduRachaV2Button(
                    text = "Explorar Cursos",
                    onClick = { },
                    icon = Icons.Outlined.Explore,
                    gradient = EduRachaV2Gradients.Blue,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = EduRachaV2Colors.Success.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        EduRachaV2Colors.Success.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EduRachaV2Colors.Success.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                null,
                                tint = EduRachaV2Colors.Success,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "¿Cómo funciona?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaV2Colors.TextPrimary
                            )
                            Text(
                                "Solicita unirte a un curso y recibe respuestas del docente en tiempo real",
                                fontSize = 13.sp,
                                color = EduRachaV2Colors.TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}