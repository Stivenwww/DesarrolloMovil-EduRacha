// MainActivity.kt
package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay

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
                    onNavigateToNotifications = { navigateTo(NotificacionesEstudianteActivity::class.java) },
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
                        startActivity(Intent(this, RankingEstudianteActivity::class.java))
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

    // Leer datos del usuario
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        fullName = prefs.getString("user_name", null) ?: "Estudiante"
        userEmail = prefs.getString("user_email", null) ?: ""
        userUid = prefs.getString("user_uid", null) ?: ""
    }

    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    // ✅ DATOS REALES - No hardcoded
    val solicitudesPendientes = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count {
            it.estado == com.stiven.sos.models.EstadoSolicitud.PENDIENTE
        }
    }

    val solicitudesAceptadas = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count {
            it.estado == com.stiven.sos.models.EstadoSolicitud.ACEPTADA
        }
    }

    // Calcular puntos reales basados en cursos
    val puntosReales = remember(solicitudesAceptadas) {
        solicitudesAceptadas * 50 // 50 puntos por curso inscrito
    }

    val userInitial = fullName.firstOrNull()?.uppercase() ?: "E"

    // Animación de entrada para toda la pantalla
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        screenVisible = true
    }

    AnimatedVisibility(
        visible = screenVisible,
        enter = fadeIn(animationSpec = tween(600))
    ) {
        EduRachaV2Container(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Header con animación
            EduRachaV2Header(
                userName = fullName,
                userEmail = userEmail,
                userInitial = userInitial,
                onNotificationClick = onNavigateToNotifications,
                onSettingsClick = onNavigateToSettings,
                onInfoClick = onNavigateToProfile,
                hasNotificationBadge = solicitudesPendientes > 0
            )

            // Stats Cards con animación escalonada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedStatCard(
                    icon = Icons.Outlined.School,
                    value = "$solicitudesAceptadas",
                    label = "Inscritos",
                    color = EduRachaV2Colors.Success,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCursosInscritos,
                    delay = 150
                )

                AnimatedStatCard(
                    icon = Icons.Outlined.HourglassTop,
                    value = "$solicitudesPendientes",
                    label = "Pendientes",
                    color = EduRachaV2Colors.Warning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSolicitudes,
                    delay = 300
                )

                AnimatedStatCard(
                    icon = Icons.Outlined.EmojiEvents,
                    value = "$puntosReales",
                    label = "Puntos",
                    color = EduRachaV2Colors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRanking,
                    delay = 450
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sección de módulos con animación
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedTitle(text = "Explora y aprende ⭐")

                Spacer(modifier = Modifier.height(8.dp))

                // Module Cards con animación escalonada
                AnimatedModuleCard(
                    title = "Cursos Disponibles",
                    subtitle = "Explora y únete a nuevos cursos",
                    icon = Icons.Outlined.Explore,
                    gradient = EduRachaV2Gradients.Blue,
                    onClick = onNavigateToCursos,
                    delay = 100
                )

                AnimatedModuleCard(
                    title = "Mis Cursos",
                    subtitle = "Continúa tu aprendizaje",
                    icon = Icons.Outlined.MenuBook,
                    gradient = EduRachaV2Gradients.Green,
                    onClick = onNavigateToCursosInscritos,
                    delay = 200
                )

                AnimatedModuleCard(
                    title = "Mis Solicitudes",
                    subtitle = "Revisa el estado de tus solicitudes",
                    icon = Icons.Outlined.Assignment,
                    gradient = EduRachaV2Gradients.Purple,
                    onClick = onNavigateToSolicitudes,
                    delay = 300
                )

                AnimatedModuleCard(
                    title = "Ranking",
                    subtitle = "Compite y gana recompensas",
                    icon = Icons.Outlined.Leaderboard,
                    gradient = EduRachaV2Gradients.Yellow,
                    onClick = onNavigateToRanking,
                    delay = 400
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AnimatedStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.toPx()
                this.alpha = alpha
            }
    ) {
        EduRachaV2StatCard(
            icon = icon,
            value = value,
            label = label,
            color = color,
            onClick = onClick
        )
    }
}

@Composable
private fun AnimatedModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: androidx.compose.ui.graphics.Brush,
    onClick: () -> Unit,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX.toPx()
            this.alpha = alpha
        }
    ) {
        EduRachaV2ModuleCard(
            title = title,
            subtitle = subtitle,
            icon = icon,
            gradient = gradient,
            onClick = onClick
        )
    }
}

@Composable
private fun AnimatedTitle(text: String) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = EduRachaV2Colors.TextPrimary
        )
    }
}