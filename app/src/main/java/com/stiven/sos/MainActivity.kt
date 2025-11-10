package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.SolicitudViewModel
import java.util.*

class MainActivity : ComponentActivity() {

    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()

        setContent {
            EduRachaTheme {
                MainEstudianteScreen(
                    solicitudViewModel = solicitudViewModel,
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    onNavigateToNotifications = { navigateTo(NotificacionesActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCursos = {
                        startActivity(Intent(this, CursosDisponiblesActivity::class.java))
                    },
                    onNavigateToSolicitudes = {
                        startActivity(Intent(this, MisSolicitudesActivity::class.java))
                    },
                    onNavigateToCursosInscritos = {
                        startActivity(Intent(this, CursosInscritosActivity::class.java))
                    },
                    onNavigateToRanking = {
                        startActivity(Intent(this, RankingDetalleActivity::class.java))
                    }
                )
            }
        }
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_uid", null)

        userId?.let {
            solicitudViewModel.cargarSolicitudesEstudiante(it)
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}

@Composable
fun MainEstudianteScreen(
    solicitudViewModel: SolicitudViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCursos: () -> Unit,
    onNavigateToSolicitudes: () -> Unit,
    onNavigateToCursosInscritos: () -> Unit,
    onNavigateToRanking: () -> Unit
) {
    val context = LocalContext.current
    val greeting = remember { getGreeting() }

    // Leer datos del usuario desde SharedPreferences
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        fullName = prefs.getString("user_name", null) ?: "Estudiante"
        userEmail = prefs.getString("user_email", null) ?: ""
        userRole = prefs.getString("user_role", null) ?: "estudiante"
        userUid = prefs.getString("user_uid", null) ?: ""

        android.util.Log.d("MainActivity", "=== DATOS DEL ESTUDIANTE ===")
        android.util.Log.d("MainActivity", "Nombre: $fullName")
        android.util.Log.d("MainActivity", "Email: $userEmail")
        android.util.Log.d("MainActivity", "UID: $userUid")
    }

    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    // Calcular estadísticas
    val solicitudesPendientes = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count { it.estado == com.stiven.sos.models.EstadoSolicitud.PENDIENTE }
    }

    val solicitudesAceptadas = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count { it.estado == com.stiven.sos.models.EstadoSolicitud.ACEPTADA }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header del usuario
        EstudianteHeader(
            greeting = greeting,
            fullName = fullName,
            userEmail = userEmail,
            userRole = userRole,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToSettings = onNavigateToSettings
        )

        // Tarjetas de estadísticas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactStatCardEstudiante(
                label = "Inscritos",
                value = "$solicitudesAceptadas",
                icon = Icons.Outlined.School,
                iconColor = EduRachaColors.Success,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCursosInscritos
            )
            CompactStatCardEstudiante(
                label = "Pendientes",
                value = "$solicitudesPendientes",
                icon = Icons.Outlined.HourglassTop,
                iconColor = EduRachaColors.Warning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSolicitudes
            )
            CompactStatCardEstudiante(
                label = "Puntos",
                value = "0",
                icon = Icons.Outlined.EmojiEvents,
                iconColor = EduRachaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToRanking
            )
        }

        // Herramientas principales
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Explora y aprende",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MainToolCardEstudiante(
                title = "Cursos Disponibles",
                description = "Explora y únete a nuevos cursos",
                icon = Icons.Outlined.Explore,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.8f)
                    )
                ),
                onClick = onNavigateToCursos
            )

            MainToolCardEstudiante(
                title = "Mis Cursos",
                description = "Continúa tu aprendizaje",
                icon = Icons.Outlined.MenuBook,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(
                        EduRachaColors.Success,
                        EduRachaColors.Success.copy(alpha = 0.8f)
                    )
                ),
                onClick = onNavigateToCursosInscritos
            )

            MainToolCardEstudiante(
                title = "Mis Solicitudes",
                description = "Revisa el estado de tus solicitudes",
                icon = Icons.Outlined.Assignment,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(
                        EduRachaColors.Accent,
                        EduRachaColors.Accent.copy(alpha = 0.8f)
                    )
                ),
                onClick = onNavigateToSolicitudes
            )

            MainToolCardEstudiante(
                title = "Ranking",
                description = "Compite y gana recompensas",
                icon = Icons.Outlined.Leaderboard,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(
                        EduRachaColors.Secondary,
                        EduRachaColors.Secondary.copy(alpha = 0.8f)
                    )
                ),
                onClick = onNavigateToRanking
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun EstudianteHeader(
    greeting: String,
    fullName: String,
    userEmail: String,
    userRole: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(top = 40.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onNavigateToProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.firstOrNull()?.uppercase() ?: "E",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = fullName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row {
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCardEstudiante(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(36.dp),
                tint = iconColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = EduRachaColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainToolCardEstudiante(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(42.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Ir",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

