// Archivo: app/src/main/java/com/stiven/sos/PanelDocenteActivity.kt

package com.stiven.sos

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import java.util.*

class PanelDocenteActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val preguntasViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar datos iniciales
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cada vez que volvemos a esta pantalla
        cargarDatos()

        setContent {
            EduRachaTheme {
                PanelDocenteScreen(
                    cursoViewModel = cursoViewModel,
                    preguntasViewModel = preguntasViewModel,
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    onNavigateToNotifications = { navigateTo(NotificacionesActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCreateCourse = { navigateTo(CrearCursoActivity::class.java) },
                    onNavigateToValidation = { handleValidationClick() },
                    onNavigateToReports = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToGroups = { navigateTo(GestionGruposActivity::class.java) },
                    onNavigateToStudents = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToCourses = { handleCoursesClick() }
                )
            }
        }
    }

    private fun cargarDatos() {
        // Cargar cursos
        cursoViewModel.obtenerCursos()

        // Cargar SOLO preguntas pendientes de revisión
        preguntasViewModel.cargarPreguntas(
            cursoId = null,
            estado = EstadoPregunta.PENDIENTE_REVISION
        )
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }

    private fun handleValidationClick() {
        val intent = Intent(this, ListaCursosActivity::class.java).apply {
            putExtra("PROPOSITO", "VALIDAR_PREGUNTAS")
        }
        startActivity(intent)
    }

    private fun handleCoursesClick() {
        navigateTo(ListaCursosActivity::class.java)
    }
}

@Composable
fun PanelDocenteScreen(
    cursoViewModel: CursoViewModel,
    preguntasViewModel: PreguntaViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCourse: () -> Unit,
    onNavigateToValidation: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToCourses: () -> Unit
) {
    val greeting = remember { getGreeting() }
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName?.split(" ")?.firstOrNull() ?: "Docente"
    val userEmail = user?.email ?: "docente@uniautonoma.edu.co"

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()

    val totalCursos = cursoUiState.cursos.size

    // CORRECCIÓN TEMPORAL: Filtrar en el cliente porque el backend no filtra correctamente
    val preguntasPendientes = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count {
            it.estado == EstadoPregunta.PENDIENTE_REVISION
        }
    }

    // Debug log para verificar
    LaunchedEffect(preguntasUiState.preguntas) {
        android.util.Log.d("PanelDocente", "Total preguntas cargadas: ${preguntasUiState.preguntas.size}")
        val pendientes = preguntasUiState.preguntas.count { it.estado == EstadoPregunta.PENDIENTE_REVISION }
        android.util.Log.d("PanelDocente", "Preguntas realmente pendientes: $pendientes")
        preguntasUiState.preguntas.forEach { pregunta ->
            android.util.Log.d("PanelDocente", "Pregunta ID: ${pregunta.id}, Estado: ${pregunta.estado}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        UserHeader(
            greeting = greeting,
            userName = userName,
            userEmail = userEmail,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToSettings = onNavigateToSettings
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactStatCard(
                label = "Cursos",
                value = "$totalCursos",
                icon = Icons.Outlined.MenuBook,
                iconColor = EduRachaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCourses
            )
            CompactStatCard(
                label = "Pendientes",
                value = "$preguntasPendientes",
                icon = Icons.Outlined.HourglassTop,
                iconColor = EduRachaColors.Warning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToValidation
            )
            CompactStatCard(
                label = "Estudiantes",
                value = "124", // Simulado - puedes conectar esto a un ViewModel cuando esté disponible
                icon = Icons.Outlined.Groups,
                iconColor = EduRachaColors.Success,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToStudents
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Herramientas principales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            MainToolCard(
                title = "Crear Curso",
                description = "Sube tu plan de aula y configura el curso",
                icon = Icons.Outlined.Add,
                backgroundColor = EduRachaColors.Primary,
                onClick = onNavigateToCreateCourse
            )
            MainToolCard(
                title = "Validación IA",
                description = "Revisa y aprueba preguntas generadas",
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = EduRachaColors.Accent,
                onClick = onNavigateToValidation
            )
            MainToolCard(
                title = "Reportes",
                description = "Visualiza estadísticas de participación",
                icon = Icons.Outlined.BarChart,
                backgroundColor = EduRachaColors.Secondary,
                onClick = onNavigateToReports
            )
            MainToolCard(
                title = "Gestión de Grupos",
                description = "Organiza y asigna estudiantes a cursos",
                icon = Icons.Outlined.GroupAdd,
                backgroundColor = EduRachaColors.Success,
                onClick = onNavigateToGroups
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun UserHeader(
    greeting: String,
    userName: String,
    userEmail: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, EduRachaColors.Primary, CircleShape)
                    .clickable(onClick = onNavigateToProfile),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Perfil",
                    modifier = Modifier.size(32.dp),
                    tint = EduRachaColors.Primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    fontSize = 14.sp,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row {
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = EduRachaColors.TextSecondary
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Ajustes",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = EduRachaColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
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
                tint = Color.White
            )
        }
    }
}

fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}