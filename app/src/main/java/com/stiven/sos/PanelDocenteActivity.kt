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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.models.UserPreferences
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import java.util.*

class PanelDocenteActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val preguntasViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()

        setContent {
            EduRachaTheme {
                PanelDocenteScreen(
                    cursoViewModel = cursoViewModel,
                    preguntasViewModel = preguntasViewModel,
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    onNavigateToNotifications = { navigateTo(NotificacionesDocenteActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCreateCourse = { navigateTo(CrearCursoActivity::class.java) },
                    onNavigateToValidation = { handleValidationClick() },
                    onNavigateToReports = { navigateTo(RankingDetalleActivity::class.java) },
                    onNavigateToGroups = { navigateTo(GestionGruposActivity::class.java) },
                    onNavigateToStudents = { navigateTo(SeleccionarCursoRankingActivity::class.java) },
                    onNavigateToCourses = { handleCoursesClick() },
                    onNavigateToReviewedQuestions = { navigateTo(PreguntasRevisadasActivity::class.java) },
                    onNavigateToCreateQuestion = { navigateTo(CrearPreguntaActivity::class.java) },
                    onNavigateToExplicaciones = { navigateTo(SeleccionarCursoExplicacionesActivity::class.java) }// ✅ NUEVO
                )
            }
        }
    }

    private fun cargarDatos() {
        cursoViewModel.obtenerCursos()
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
    onNavigateToCourses: () -> Unit,
    onNavigateToReviewedQuestions: () -> Unit,
    onNavigateToCreateQuestion: () -> Unit,
    onNavigateToExplicaciones: () -> Unit// ✅ NUEVO PARÁMETRO
) {
    val context = LocalContext.current
    val greeting = remember { getGreeting() }

    // ✅ Estados para almacenar datos del usuario
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    // ✅ Leer datos directamente desde SharedPreferences cada vez que se compone
    LaunchedEffect(Unit) {
        // Acceso directo a SharedPreferences para asegurar lectura actualizada
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

        fullName = prefs.getString("user_name", null) ?: "Usuario"
        userEmail = prefs.getString("user_email", null) ?: ""
        userRole = prefs.getString("user_role", null) ?: "estudiante"
        userUid = prefs.getString("user_uid", null) ?: ""

        android.util.Log.d("PanelDocente", "=== LEYENDO DATOS DEL USUARIO ===")
        android.util.Log.d("PanelDocente", "Nombre completo: $fullName")
        android.util.Log.d("PanelDocente", "Email: $userEmail")
        android.util.Log.d("PanelDocente", "Rol: $userRole")
        android.util.Log.d("PanelDocente", "UID: $userUid")
    }

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()

    val totalCursos = cursoUiState.cursos.size

    val preguntasPendientes = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count {
            it.estado == EstadoPregunta.PENDIENTE_REVISION
        }
    }

    // ✅ NUEVO: Contar preguntas aprobadas y rechazadas
    val preguntasAprobadas = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count { it.estado == EstadoPregunta.APROBADA }
    }

    val preguntasRechazadas = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count { it.estado == EstadoPregunta.RECHAZADA }
    }

    LaunchedEffect(preguntasUiState.preguntas) {
        android.util.Log.d("PanelDocente", "Total preguntas cargadas: ${preguntasUiState.preguntas.size}")
        val pendientes = preguntasUiState.preguntas.count { it.estado == EstadoPregunta.PENDIENTE_REVISION }
        android.util.Log.d("PanelDocente", "Preguntas realmente pendientes: $pendientes")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        UserHeader(
            greeting = greeting,
            fullName = fullName,
            userEmail = userEmail,
            userRole = userRole,
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
                value = "124",
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
                title = "Validación de preguntas",
                description = "Revisa y aprueba preguntas generadas",
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = EduRachaColors.Accent,
                onClick = onNavigateToValidation
            )

            // ✅ NUEVA CARD: Gestión de Explicaciones
            MainToolCard(
                title = "Gestión de Explicaciones",
                description = "Revisa y valida explicaciones generadas por IA",
                icon = Icons.Outlined.Description,
                backgroundColor = Color(0xFF10B981), // Color verde
                onClick = onNavigateToExplicaciones
            )
            // ✅ NUEVA CARD: Crear Pregunta
            MainToolCard(
                title = "Crear Pregunta",
                description = "Crea preguntas manualmente o genera con IA",
                icon = Icons.Outlined.QuestionAnswer,
                backgroundColor = Color(0xFF8B5CF6), // Color morado
                onClick = onNavigateToCreateQuestion
            )
            // ✅ NUEVA CARD: Preguntas Revisadas
            MainToolCard(
                title = "Preguntas Revisadas",
                description = "Ver historial de preguntas aprobadas y rechazadas ($preguntasAprobadas aprobadas, $preguntasRechazadas rechazadas)",
                icon = Icons.Outlined.History,
                backgroundColor = Color(0xFF6366F1), // Color índigo
                onClick = onNavigateToReviewedQuestions
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
            .background(Color.White)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f))
                    .border(2.dp, EduRachaColors.Primary, CircleShape)
                    .clickable(onClick = onNavigateToProfile),
                contentAlignment = Alignment.Center
            ) {
                // Mostrar inicial del nombre
                Text(
                    text = fullName.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary
                )
                Text(
                    text = fullName,
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
                // Mostrar badge del rol
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (userRole == "docente") EduRachaColors.Primary.copy(alpha = 0.15f)
                    else EduRachaColors.Success.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (userRole == "docente") "👨‍🏫 Docente" else "👨‍🎓 Estudiante",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (userRole == "docente") EduRachaColors.Primary else EduRachaColors.Success,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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