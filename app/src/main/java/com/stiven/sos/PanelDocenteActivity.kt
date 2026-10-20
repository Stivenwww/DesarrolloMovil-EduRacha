package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import com.stiven.sos.ui.admin.EstadoChip
import com.stiven.sos.ui.theme.EduRachaColors
import ui.theme.AdminDS
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import java.util.*

class PanelDocenteActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val preguntasViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Trae el tema/sonidos guardados en el perfil (si el usuario cambió de celular)
        com.stiven.sos.services.SincronizadorPreferencias.descargar(this)
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
                    onNavigateToReports = { navigateTo(ReportesActivity::class.java) },
                    onNavigateToGroups = { navigateTo(GestionGruposActivity::class.java) },
                    onNavigateToStudents = { navigateTo(SeleccionarCursoRankingActivity::class.java) },
                    onNavigateToCourses = { handleCoursesClick() },
                    onNavigateToReviewedQuestions = { navigateTo(PreguntasRevisadasActivity::class.java) },
                    onNavigateToCreateQuestion = { navigateTo(CrearPreguntaActivity::class.java) },
                    onNavigateToExplicaciones = { navigateTo(SeleccionarCursoExplicacionesActivity::class.java) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Solo onResume recarga: onCreate ya no dispara carga, se evita la
        // doble solicitud inicial (una de onCreate y otra de onResume).
        cargarDatos()
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
    onNavigateToExplicaciones: () -> Unit
) {
    val context = LocalContext.current
    val greeting = remember { getGreeting() }

    // Estados para almacenar datos del usuario
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    // Leer datos desde SharedPreferences (sin registrar PII en logs)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        fullName = prefs.getString("user_name", null) ?: "Usuario"
        userEmail = prefs.getString("user_email", null) ?: ""
        userRole = prefs.getString("user_role", null) ?: "estudiante"
        userUid = prefs.getString("user_uid", null) ?: ""
    }

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()

    val totalCursos = cursoUiState.cursos.size

    val preguntasPendientes = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count {
            it.estado == EstadoPregunta.PENDIENTE_REVISION
        }
    }

    val preguntasAprobadas = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count { it.estado == EstadoPregunta.APROBADA }
    }

    val preguntasRechazadas = remember(preguntasUiState.preguntas) {
        preguntasUiState.preguntas.count { it.estado == EstadoPregunta.RECHAZADA }
    }

    // Cargar estudiantes totales cuando haya cursos disponibles
    LaunchedEffect(cursoUiState.cursos) {
        if (cursoUiState.cursos.isNotEmpty()) {
            cursoViewModel.cargarEstudiantesTotales()
        }
    }

    //  Calcular el total de estudiantes únicos
    val totalEstudiantes = remember(cursoUiState.estudiantesPorCurso) {
        cursoUiState.estudiantesPorCurso.values.flatten().map { it.uid }.toSet().size
    }

    val cursosError = cursoUiState.error
    val cursosCargando = cursoUiState.isLoading && cursoUiState.cursos.isEmpty()
    val preguntasError = preguntasUiState.error

    fun recargarTodo() {
        cursoViewModel.obtenerCursos()
        preguntasViewModel.cargarPreguntas(
            cursoId = null,
            estado = EstadoPregunta.PENDIENTE_REVISION
        )
    }

    val nx = com.stiven.sos.ui.theme.LocalNexo.current

    com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            UserHeader(
                totalCursos = totalCursos,
                preguntasPendientes = preguntasPendientes,
                greeting = greeting,
                fullName = fullName,
                userEmail = userEmail,
                userRole = userRole,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToSettings = onNavigateToSettings
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Asistente IA (Robi) — acceso directo a crear preguntas con IA
                com.stiven.sos.ui.theme.TarjetaNexo(
                    modifier = Modifier.fillMaxWidth(),
                    acento = nx.ia,
                    padding = 16.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.stiven.sos.ui.theme.RobiNeo(
                            tamano = 76.dp,
                            expresion = if (preguntasPendientes > 0) com.stiven.sos.ui.theme.ExpresionRobi.PIENSA
                            else com.stiven.sos.ui.theme.ExpresionRobi.FELIZ
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "ASISTENTE IA",
                                color = nx.iaTexto,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                when {
                                    preguntasPendientes > 0 -> "Tienes $preguntasPendientes ${if (preguntasPendientes == 1) "pregunta" else "preguntas"} esperando revisión."
                                    totalCursos > 0 -> "Todo al día. ¿Generamos nuevas preguntas para tus cursos?"
                                    else -> "Crea tu primer curso y yo te ayudo con las preguntas."
                                },
                                color = nx.texto,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    com.stiven.sos.ui.theme.BotonNexo(
                        texto = if (preguntasPendientes > 0) "Revisar preguntas" else "Crear pregunta con IA",
                        onClick = if (preguntasPendientes > 0) onNavigateToValidation else onNavigateToCreateQuestion,
                        tipo = com.stiven.sos.ui.theme.TipoBoton.IA,
                        icono = if (preguntasPendientes > 0) Icons.Outlined.FactCheck else Icons.Outlined.AutoAwesome,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        titulo = "Cursos",
                        valor = if (cursosCargando) "…" else "$totalCursos",
                        icon = Icons.Outlined.MenuBook,
                        acento = nx.marca,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCourses
                    )
                    AdminStatCard(
                        titulo = "Pendientes",
                        valor = if (preguntasUiState.isLoading && preguntasUiState.preguntas.isEmpty()) "…"
                        else "$preguntasPendientes",
                        icon = Icons.Outlined.HourglassTop,
                        acento = nx.racha,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToValidation
                    )
                    AdminStatCard(
                        titulo = "Estudiantes",
                        valor = if (cursoUiState.isLoadingEstudiantes) "…" else "$totalEstudiantes",
                        icon = Icons.Outlined.Groups,
                        acento = nx.accion,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToStudents
                    )
                }

                // Estados de error con reintento (solo si no hay datos que mostrar)
                if (cursosError != null && cursoUiState.cursos.isEmpty()) {
                    AdminErrorBanner(
                        mensaje = "No se pudieron cargar los cursos: $cursosError",
                        onRetry = { recargarTodo() }
                    )
                }
                if (preguntasError != null && preguntasUiState.preguntas.isEmpty()) {
                    AdminErrorBanner(
                        mensaje = "No se pudieron cargar las preguntas: $preguntasError",
                        onRetry = { recargarTodo() }
                    )
                }
                if (!cursosCargando && cursoUiState.cursos.isEmpty() && cursosError == null) {
                    AdminEmptyState("Aún no tienes cursos. Crea el primero con “Crear curso”.")
                }

                AdminSeccionTitulo("Contenido académico")
                AdminToolRow(
                    title = "Crear curso",
                    description = "Plan de aula, duración y temas",
                    icon = Icons.Outlined.Add,
                    acento = nx.marca,
                    onClick = onNavigateToCreateCourse
                )
                AdminToolRow(
                    title = "Crear pregunta",
                    description = "Manual o generada con IA",
                    icon = Icons.Outlined.QuestionAnswer,
                    acento = nx.ia,
                    onClick = onNavigateToCreateQuestion
                )
                AdminToolRow(
                    title = "Validar preguntas",
                    description = if (preguntasPendientes > 0) "$preguntasPendientes por revisar"
                    else "Revisa y aprueba preguntas",
                    icon = Icons.Outlined.CheckCircle,
                    acento = nx.accion,
                    badge = if (preguntasPendientes > 0) "$preguntasPendientes" else null,
                    onClick = onNavigateToValidation
                )
                AdminToolRow(
                    title = "Explicaciones",
                    description = "Valida explicaciones generadas por IA",
                    icon = Icons.Outlined.Description,
                    acento = nx.marcaSombra,
                    onClick = onNavigateToExplicaciones
                )
                AdminToolRow(
                    title = "Preguntas revisadas",
                    description = "$preguntasAprobadas aprobadas · $preguntasRechazadas rechazadas",
                    icon = Icons.Outlined.History,
                    acento = nx.error,
                    onClick = onNavigateToReviewedQuestions
                )

                AdminSeccionTitulo("Gestión")
                AdminToolRow(
                    title = "Mis cursos",
                    description = "Ver, editar y gestionar cursos",
                    icon = Icons.Outlined.MenuBook,
                    acento = nx.marca,
                    onClick = onNavigateToCourses
                )
                AdminToolRow(
                    title = "Reportes",
                    description = "Estadísticas de participación",
                    icon = Icons.Outlined.BarChart,
                    acento = nx.racha,
                    onClick = onNavigateToReports
                )
                AdminToolRow(
                    title = "Grupos",
                    description = "Organiza y asigna estudiantes",
                    icon = Icons.Outlined.GroupAdd,
                    acento = nx.iaSombra,
                    onClick = onNavigateToGroups
                )
                Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
            }
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
    onNavigateToSettings: () -> Unit,
    totalCursos: Int = 0,
    preguntasPendientes: Int = 0
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                Text("edu", fontSize = 22.sp, fontWeight = FontWeight.Black, color = nx.texto)
                Text("racha", fontSize = 22.sp, fontWeight = FontWeight.Black, color = nx.marcaTexto)
                Spacer(Modifier.width(8.dp))
                Text(
                    "DOCENTE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = nx.iaTexto,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(nx.ia.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            IconButton(onClick = onNavigateToNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = nx.texto)
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = nx.texto)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.stiven.sos.ui.theme.AvatarNexo(
                nombre = fullName,
                color = nx.ia,
                tamano = 52.dp,
                modifier = Modifier
                    .border(2.dp, nx.marca, CircleShape)
                    .clickable(onClick = onNavigateToProfile)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "$greeting,", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = nx.textoSuave)
                Text(
                    text = fullName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = nx.texto,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (userEmail.isNotEmpty()) {
                    Text(text = userEmail, fontSize = 12.sp, color = nx.textoTenue, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AdminSeccionTitulo(texto: String) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(nx.marca)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = nx.texto
        )
    }
}

@Composable
fun AdminStatCard(
    titulo: String,
    valor: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    acento: Color = AdminDS.Brand,
    onClick: () -> Unit
) {
    com.stiven.sos.ui.theme.StatNexo(
        icono = icon,
        valor = valor,
        etiqueta = titulo,
        color = acento,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun AdminToolRow(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null,
    acento: Color = AdminDS.Brand,
    onClick: () -> Unit
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.FilaNexo(
        icono = icon,
        color = acento,
        titulo = title,
        subtitulo = description,
        onClick = onClick,
        extremo = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) {
                    Text(
                        text = badge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(nx.racha)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        color = nx.sobreAccion,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Abrir $title",
                    tint = acento
                )
            }
        }
    )
}

@Composable
fun AdminErrorBanner(
    mensaje: String,
    onRetry: () -> Unit
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(nx.error.copy(alpha = 0.10f))
            .border(1.dp, nx.error.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = nx.errorTexto)
        Spacer(Modifier.width(8.dp))
        Text(
            text = mensaje,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = nx.texto,
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRetry) {
            Text("Reintentar", color = nx.marcaTexto, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AdminEmptyState(mensaje: String) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = mensaje, color = nx.textoSuave, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}