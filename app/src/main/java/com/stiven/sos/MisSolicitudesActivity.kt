package com.stiven.sos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.SolicitudViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesScreen(
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Solicitudes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                solicitudUiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EduRachaColors.Primary
                    )
                }

                solicitudUiState.solicitudes.isEmpty() -> {
                    EmptySolicitudesView()
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(solicitudUiState.solicitudes) { solicitud ->
                            SolicitudCard(solicitud = solicitud)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolicitudCard(solicitud: SolicitudCurso) {
    val (estadoColor, estadoTexto, estadoIcono) = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> Triple(
            EduRachaColors.Warning,
            "Pendiente",
            Icons.Default.HourglassTop
        )
        EstadoSolicitud.ACEPTADA -> Triple(
            EduRachaColors.Success,
            "Aceptada",
            Icons.Default.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Triple(
            EduRachaColors.Error,
            "Rechazada",
            Icons.Default.Cancel
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Encabezado con estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = solicitud.codigoCurso,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = estadoColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(estadoColor)
                        )
                        Text(
                            text = estadoTexto,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = estadoColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fecha de solicitud
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = EduRachaColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Solicitado: ${solicitud.fechaSolicitud}",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            // Fecha de respuesta (si existe)
            if (solicitud.fechaRespuesta != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        estadoIcono,
                        contentDescription = null,
                        tint = estadoColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Respondido: ${solicitud.fechaRespuesta}",
                        fontSize = 13.sp,
                        color = estadoColor
                    )
                }
            }

            // Mensaje del docente (si existe)
            if (!solicitud.mensaje.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.SurfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Mensaje del docente:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = solicitud.mensaje,
                            fontSize = 14.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySolicitudesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Assignment,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No tienes solicitudes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            text = "Explora cursos disponibles y solicita unirte",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}