package com.stiven.sos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.SolicitudViewModel
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

class CursosDisponiblesActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val solicitudViewModel: SolicitudViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        cursoViewModel.obtenerCursos()
        quizViewModel.cargarCursosInscritos() // Cargar cursos inscritos

        if (userUid.isNotEmpty()) {
            solicitudViewModel.cargarSolicitudesEstudiante(userUid)
        }

        setContent {
            EduRachaTheme {
                CursosDisponiblesScreen(
                    cursoViewModel = cursoViewModel,
                    solicitudViewModel = solicitudViewModel,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// Enum para los filtros
enum class FiltroTipo {
    DISPONIBLES,
    SOLICITADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosDisponiblesScreen(
    cursoViewModel: CursoViewModel,
    solicitudViewModel: SolicitudViewModel,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()
    val quizUiState by quizViewModel.uiState.collectAsState()

    var showSolicitudDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var mensajeEstudiante by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showMascotaMessage by remember { mutableStateOf(false) }
    var filtroActual by remember { mutableStateOf(FiltroTipo.DISPONIBLES) }

    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val userUid = prefs.getString("user_uid", "") ?: ""
    val userName = prefs.getString("user_name", "") ?: ""
    val userEmail = prefs.getString("user_email", "") ?: ""

    // Obtener códigos de cursos con solicitud pendiente
    val cursosConSolicitud = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .map { it.codigoCurso }
            .toSet()
    }

    // Obtener códigos de cursos donde ya está inscrito
    val cursosInscritos = remember(quizUiState.cursosInscritos) {
        quizUiState.cursosInscritos.map { it.codigo }.toSet()
    }

    LaunchedEffect(solicitudUiState.mensajeExito) {
        solicitudUiState.mensajeExito?.let {
            showSuccessDialog = true
            solicitudViewModel.clearMessages()
        }
    }

    LaunchedEffect(solicitudUiState.error) {
        solicitudUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            solicitudViewModel.clearMessages()
        }
    }

    // FILTRADO COMPLETO
    val cursosFiltrados = remember(cursoUiState.cursos, searchQuery) {
        var filtered = cursoUiState.cursos

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true)
            }
        }

        filtered
    }

    // CURSOS DISPONIBLES: Excluir los que tienen solicitud pendiente Y los que ya está inscrito
    val cursosDisponibles = cursosFiltrados.filterNot {
        cursosConSolicitud.contains(it.codigo) || cursosInscritos.contains(it.codigo)
    }

    // CURSOS SOLICITADOS: Solo los que tienen solicitud pendiente Y no está inscrito
    val cursosSolicitados = cursosFiltrados.filter {
        cursosConSolicitud.contains(it.codigo) && !cursosInscritos.contains(it.codigo)
    }

    // Determinar qué lista mostrar según el filtro
    val cursosAMostrar = when (filtroActual) {
        FiltroTipo.DISPONIBLES -> cursosDisponibles
        FiltroTipo.SOLICITADOS -> cursosSolicitados
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                HeaderFigma(
                    onNavigateBack = onNavigateBack,
                    cursosCount = cursoUiState.cursos.size,
                    pendingCount = cursosConSolicitud.size
                )
            }

            // Buscador
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    BuscadorFigma(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )
                }
            }

            // Filtros/Tabs
            item {
                FiltrosTabs(
                    filtroActual = filtroActual,
                    onFiltroChange = { filtroActual = it },
                    cantidadDisponibles = cursosDisponibles.size,
                    cantidadSolicitados = cursosSolicitados.size
                )
                Spacer(Modifier.height(16.dp))
            }

            // Título de la sección actual
            item {
                val titulo = when (filtroActual) {
                    FiltroTipo.DISPONIBLES -> "Cursos Disponibles"
                    FiltroTipo.SOLICITADOS -> "Solicitudes Enviadas"
                }
                val subtitulo = when (filtroActual) {
                    FiltroTipo.DISPONIBLES -> "Encuentra el curso perfecto para ti"
                    FiltroTipo.SOLICITADOS -> "Estas solicitudes están siendo revisadas"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        titulo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitulo,
                        fontSize = 13.sp,
                        color = Color(0xFF717182)
                    )
                }
            }

            // Lista de cursos
            if (cursosAMostrar.isNotEmpty()) {
                items(cursosAMostrar, key = { it.codigo }) { curso ->
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        CursoCardFigma(
                            curso = curso,
                            tieneSolicitudPendiente = cursosConSolicitud.contains(curso.codigo),
                            onSolicitarClick = {
                                cursoSeleccionado = curso
                                mensajeEstudiante = ""
                                showSolicitudDialog = true
                            }
                        )
                    }
                }
            }

            // Empty state
            if (cursosAMostrar.isEmpty() && !cursoUiState.isLoading) {
                item {
                    EmptyStateFigma(
                        filtroActual = filtroActual,
                        isSearchActive = searchQuery.isNotBlank()
                    )
                }
            }

            // Loading
            if (cursoUiState.isLoading || quizUiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF3C79F5),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // Mascota en esquina inferior derecha
        MascotaFigma(
            onClick = { showMascotaMessage = true }
        )

        // Mensaje de la mascota
        AnimatedVisibility(
            visible = showMascotaMessage,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 110.dp, bottom = 40.dp)
        ) {
            MascotaMessageBubble(
                onDismiss = { showMascotaMessage = false }
            )
        }
    }

    // Diálogos
    if (showSuccessDialog) {
        DialogoExitoSolicitudFigma(
            onDismiss = { showSuccessDialog = false }
        )
    }

    if (showSolicitudDialog && cursoSeleccionado != null) {
        DialogoSolicitudFigma(
            curso = cursoSeleccionado!!,
            mensaje = mensajeEstudiante,
            onMensajeChange = { mensajeEstudiante = it },
            onDismiss = {
                showSolicitudDialog = false
                cursoSeleccionado = null
                mensajeEstudiante = ""
            },
            onConfirm = {
                solicitudViewModel.crearSolicitud(
                    codigoCurso = cursoSeleccionado!!.codigo,
                    estudianteId = userUid,
                    estudianteNombre = userName,
                    estudianteEmail = userEmail,
                    mensaje = mensajeEstudiante.ifBlank { null }
                )
                showSolicitudDialog = false
                cursoSeleccionado = null
                mensajeEstudiante = ""
            }
        )
    }
}

// ============================================================================
// FILTROS/TABS
// ============================================================================

@Composable
fun FiltrosTabs(
    filtroActual: FiltroTipo,
    onFiltroChange: (FiltroTipo) -> Unit,
    cantidadDisponibles: Int,
    cantidadSolicitados: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Disponibles
        FiltroTab(
            text = "Disponibles",
            count = cantidadDisponibles,
            isSelected = filtroActual == FiltroTipo.DISPONIBLES,
            onClick = { onFiltroChange(FiltroTipo.DISPONIBLES) },
            color = Color(0xFF3C79F5),
            modifier = Modifier.weight(1f)
        )

        // Tab Solicitados
        FiltroTab(
            text = "Solicitados",
            count = cantidadSolicitados,
            isSelected = filtroActual == FiltroTipo.SOLICITADOS,
            onClick = { onFiltroChange(FiltroTipo.SOLICITADOS) },
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FiltroTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.White,
        animationSpec = tween(300)
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF717182),
        animationSpec = tween(300)
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 2.dp,
        animationSpec = tween(300)
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = elevation,
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            if (count > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isSelected)
                        Color.White.copy(alpha = 0.3f)
                    else
                        color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// HEADER FIGMA
// ============================================================================

@Composable
fun HeaderFigma(
    onNavigateBack: () -> Unit,
    cursosCount: Int,
    pendingCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF3C79F5),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Botón atrás
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Título
            Text(
                "Explora Cursos",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "$cursosCount cursos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFF9800)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "$pendingCount pendientes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// BUSCADOR FIGMA
// ============================================================================

@Composable
fun BuscadorFigma(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = Color(0xFF717182),
                modifier = Modifier.size(24.dp)
            )

            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFF1C1C1E),
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Buscar cursos...",
                            fontSize = 16.sp,
                            color = Color(0xFFA0A0AB)
                        )
                    }
                    innerTextField()
                }
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Limpiar",
                        tint = Color(0xFF717182),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// CARD DE CURSO FIGMA
// ============================================================================

@Composable
fun CursoCardFigma(
    curso: Curso,
    tieneSolicitudPendiente: Boolean,
    onSolicitarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono del curso
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE8E5F5)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🎓",
                        fontSize = 40.sp
                    )
                }
            }

            // Contenido del curso
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Título
                Text(
                    curso.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E),
                    lineHeight = 24.sp
                )

                // Descripción
                Text(
                    curso.descripcion,
                    fontSize = 14.sp,
                    color = Color(0xFF717182),
                    lineHeight = 20.sp,
                    maxLines = 2
                )

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5FE)
                    ) {
                        Text(
                            curso.codigo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3C79F5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5FE)
                    ) {
                        Text(
                            "Avanzado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3C79F5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Botón Solicitar
                Button(
                    onClick = onSolicitarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !tieneSolicitudPendiente,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tieneSolicitudPendiente)
                            Color(0xFFFF9800)
                        else Color(0xFF3C79F5),
                        disabledContainerColor = Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        if (tieneSolicitudPendiente) Icons.Default.Schedule else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tieneSolicitudPendiente) "Solicitud Enviada" else "Solicitar unirse",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================================
// MASCOTA FIGMA CON INTERACCIÓN
// ============================================================================

@Composable
fun MascotaFigma(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascota")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .offset(y = offsetY.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🦉",
                    fontSize = 48.sp
                )
            }
        }
    }
}

// ============================================================================
// MENSAJE DE LA MASCOTA
// ============================================================================

@Composable
fun MascotaMessageBubble(
    onDismiss: () -> Unit
) {
    val messages = listOf(
        "¡Anímate a estudiar! 📚",
        "¡Tú puedes lograrlo! 💪",
        "¡Sigue aprendiendo! 🎓",
        "¡Eres increíble! ⭐",
        "¡No te rindas! 🚀",
        "¡El éxito te espera! 🏆"
    )

    val randomMessage = remember { messages.random() }

    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.width(200.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3C79F5).copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = randomMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// EMPTY STATE FIGMA
// ============================================================================

@Composable
fun EmptyStateFigma(
    filtroActual: FiltroTipo,
    isSearchActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val icon = when {
            isSearchActive -> Icons.Default.SearchOff
            filtroActual == FiltroTipo.SOLICITADOS -> Icons.Default.Schedule
            else -> Icons.Default.School
        }

        val title = when {
            isSearchActive -> "No se encontraron cursos"
            filtroActual == FiltroTipo.SOLICITADOS -> "No hay solicitudes enviadas"
            else -> "No hay cursos disponibles"
        }

        val message = when {
            isSearchActive -> "Intenta con otros términos"
            filtroActual == FiltroTipo.SOLICITADOS -> "Aún no has enviado solicitudes a ningún curso"
            else -> "Pronto habrá nuevos cursos"
        }

        val iconColor = when (filtroActual) {
            FiltroTipo.SOLICITADOS -> Color(0xFFFF9800)
            else -> Color(0xFF3C79F5)
        }

        // Icono animado
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    iconColor.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = iconColor.copy(alpha = 0.5f)
            )
        }

        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E),
            textAlign = TextAlign.Center
        )

        Text(
            message,
            fontSize = 15.sp,
            color = Color(0xFF717182),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// DIÁLOGO SOLICITUD FIGMA
// ============================================================================

@Composable
fun DialogoSolicitudFigma(
    curso: Curso,
    mensaje: String,
    onMensajeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var selectedPredefined by remember { mutableStateOf<String?>(null) }

    val mensajesPredefinidos = listOf(
        "Estoy muy interesado en aprender sobre este tema y creo que este curso me ayudará en mi formación académica.",
        "Me gustaría unirme a este curso para complementar mis conocimientos en el área.",
        "Este curso es fundamental para mi carrera y me comprometo a participar activamente."
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3C79F5),
                                    Color(0xFF5B93FF)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Solicitar Unirse",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Contenido
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Info del curso
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF7F9FC)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8E5F5)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎓", fontSize = 32.sp)
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    curso.titulo,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    curso.codigo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF3C79F5)
                                )
                            }
                        }
                    }

                    // Mensajes predefinidos
                    Text(
                        "Selecciona un mensaje:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF717182)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        mensajesPredefinidos.forEach { predefinido ->
                            MensajePredefinidoChip(
                                message = predefinido,
                                isSelected = selectedPredefined == predefinido,
                                onClick = {
                                    selectedPredefined = predefinido
                                    onMensajeChange(predefinido)
                                }
                            )
                        }
                    }

                    // Campo de texto personalizado
                    Text(
                        "O escribe tu mensaje:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF717182)
                    )

                    OutlinedTextField(
                        value = mensaje,
                        onValueChange = {
                            onMensajeChange(it)
                            selectedPredefined = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe aquí...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3C79F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                "Cancelar",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF717182)
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3C79F5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Enviar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MensajePredefinidoChip(
    message: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF3C79F5).copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF3C79F5) else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF3C79F5) else Color(0xFFE0E0E0),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = if (isSelected) Color(0xFF3C79F5) else Color(0xFF717182),
                lineHeight = 18.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// ============================================================================
// DIÁLOGO DE ÉXITO SOLICITUD
// ============================================================================

@Composable
fun DialogoExitoSolicitudFigma(
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icono de éxito con animación
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF41C77C).copy(alpha = 0.2f),
                                    Color(0xFF41C77C).copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF41C77C),
                        modifier = Modifier
                            .size(50.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                // Título
                Text(
                    "¡Solicitud Enviada!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E),
                    textAlign = TextAlign.Center
                )

                // Mensaje
                Text(
                    "Tu solicitud ha sido enviada correctamente y está siendo revisada por el docente",
                    fontSize = 15.sp,
                    color = Color(0xFF717182),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // Info adicional
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF41C77C).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF41C77C),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Te notificaremos cuando haya una respuesta",
                            fontSize = 13.sp,
                            color = Color(0xFF1C1C1E),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}