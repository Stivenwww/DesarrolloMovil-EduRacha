package com.stiven.sos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay

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

// Colores institucionales mejorados
object InstitutionalColors {
    val PrimaryGradientStart = Color(0xFF1E3A8A) // Azul institucional profundo
    val PrimaryGradientEnd = Color(0xFF3B82F6) // Azul brillante
    val SecondaryAccent = Color(0xFF8B5CF6) // Púrpura elegante
    val SuccessGreen = Color(0xFF10B981) // Verde esmeralda
    val WarningAmber = Color(0xFFF59E0B) // Ámbar cálido
    val ErrorRed = Color(0xFFEF4444) // Rojo coral
    val BackgroundLight = Color(0xFFF8FAFC) // Gris muy claro
    val CardBackground = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val DividerColor = Color(0xFFE2E8F0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesScreen(
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            EnhancedTopBar(
                onNavigateBack = onNavigateBack,
                totalSolicitudes = solicitudUiState.solicitudes.size
            )
        },
        containerColor = InstitutionalColors.BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                solicitudUiState.isLoading -> {
                    LoadingScreen()
                }

                solicitudUiState.solicitudes.isEmpty() -> {
                    EnhancedEmptySolicitudesView()
                }

                else -> {
                    Column {
                        // Header con estadísticas
                        SolicitudesHeader(solicitudUiState.solicitudes)

                        // Lista de solicitudes con animación
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = solicitudUiState.solicitudes,
                                key = { it.id!! }
                            ) { solicitud ->
                                AnimatedSolicitudCard(solicitud = solicitud)
                            }

                            // Espaciado final
                            item {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopBar(onNavigateBack: () -> Unit, totalSolicitudes: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            InstitutionalColors.PrimaryGradientStart,
                            InstitutionalColors.PrimaryGradientEnd
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Mis Solicitudes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 0.5.sp
                        )

                        // Contador de solicitudes totales
                        if (totalSolicitudes > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = totalSolicitudes.toString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        "Gestión académica",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun SolicitudesHeader(solicitudes: List<SolicitudCurso>) {
    val pendientes = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE }
    val aceptadas = solicitudes.count { it.estado == EstadoSolicitud.ACEPTADA }
    val rechazadas = solicitudes.count { it.estado == EstadoSolicitud.RECHAZADA }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                count = pendientes,
                label = "Pendientes",
                icon = Icons.Default.HourglassTop,
                color = InstitutionalColors.WarningAmber
            )

            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = InstitutionalColors.DividerColor
            )

            StatCard(
                count = aceptadas,
                label = "Aceptadas",
                icon = Icons.Default.CheckCircle,
                color = InstitutionalColors.SuccessGreen
            )

            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = InstitutionalColors.DividerColor
            )

            StatCard(
                count = rechazadas,
                label = "Rechazadas",
                icon = Icons.Default.Cancel,
                color = InstitutionalColors.ErrorRed
            )
        }
    }
}

@Composable
fun StatCard(count: Int, label: String, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = InstitutionalColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AnimatedSolicitudCard(solicitud: SolicitudCurso) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(500)
                )
    ) {
        EnhancedSolicitudCard(solicitud)
    }
}

@Composable
fun EnhancedSolicitudCard(solicitud: SolicitudCurso) {
    val (estadoColor, estadoTexto, estadoIcono) = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> Triple(
            InstitutionalColors.WarningAmber,
            "En Revisión",
            Icons.Default.HourglassTop
        )
        EstadoSolicitud.ACEPTADA -> Triple(
            InstitutionalColors.SuccessGreen,
            "Aceptada",
            Icons.Default.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Triple(
            InstitutionalColors.ErrorRed,
            "Rechazada",
            Icons.Default.Cancel
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = InstitutionalColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                estadoColor.copy(alpha = 0.15f),
                                estadoColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(estadoColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = estadoColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = solicitud.codigoCurso,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = InstitutionalColors.TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Código del curso",
                                fontSize = 12.sp,
                                color = InstitutionalColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = estadoColor,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                estadoIcono,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = estadoTexto,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fecha de solicitud
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Fecha de solicitud",
                    value = solicitud.fechaSolicitud,
                    color = InstitutionalColors.PrimaryGradientEnd
                )

                // Mensaje del estudiante - usando la propiedad computada del modelo original
                solicitud.mensajeEstudiante?.let { mensaje ->
                    if (mensaje.isNotBlank()) {
                        MessageBox(
                            title = "Tu mensaje",
                            message = mensaje,
                            icon = Icons.Default.Person,
                            backgroundColor = InstitutionalColors.PrimaryGradientEnd.copy(alpha = 0.1f),
                            borderColor = InstitutionalColors.PrimaryGradientEnd
                        )
                    }
                }

                // Fecha de respuesta
                solicitud.fechaRespuesta?.let { fecha ->
                    InfoRow(
                        icon = estadoIcono,
                        label = "Fecha de respuesta",
                        value = fecha,
                        color = estadoColor
                    )
                }

                // Mensaje del docente - usando la propiedad computada del modelo original
                solicitud.mensajeDocente?.let { mensaje ->
                    if (mensaje.isNotBlank()) {
                        MessageBox(
                            title = "Respuesta del docente",
                            message = mensaje,
                            icon = Icons.Default.School,
                            backgroundColor = estadoColor.copy(alpha = 0.1f),
                            borderColor = estadoColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = InstitutionalColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InstitutionalColors.TextPrimary
            )
        }
    }
}

@Composable
fun MessageBox(
    title: String,
    message: String,
    icon: ImageVector,
    backgroundColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(borderColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor,
                    letterSpacing = 0.3.sp
                )
            }

            Text(
                text = message,
                fontSize = 14.sp,
                color = InstitutionalColors.TextPrimary,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = InstitutionalColors.PrimaryGradientEnd,
                strokeWidth = 4.dp
            )
            Text(
                text = "Cargando solicitudes...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = InstitutionalColors.TextSecondary
            )
        }
    }
}

@Composable
fun EnhancedEmptySolicitudesView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        InstitutionalColors.BackgroundLight,
                        InstitutionalColors.PrimaryGradientEnd.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        InstitutionalColors.PrimaryGradientEnd.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = InstitutionalColors.PrimaryGradientEnd.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "No tienes solicitudes aún",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InstitutionalColors.TextPrimary,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Explora los cursos disponibles y solicita\nunirte a las clases que te interesen",
                fontSize = 15.sp,
                color = InstitutionalColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { /* Navegar a explorar cursos */ },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InstitutionalColors.PrimaryGradientEnd
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Explorar Cursos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}