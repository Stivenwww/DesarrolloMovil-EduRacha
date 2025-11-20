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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.SolicitudViewModel

class CursosDisponiblesActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        cursoViewModel.obtenerCursos()
        if (userUid.isNotEmpty()) {
            solicitudViewModel.cargarSolicitudesEstudiante(userUid)
        }

        setContent {
            EduRachaTheme {
                CursosDisponiblesScreen(
                    cursoViewModel = cursoViewModel,
                    solicitudViewModel = solicitudViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosDisponiblesScreen(
    cursoViewModel: CursoViewModel,
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    var showSolicitudDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var mensajeEstudiante by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Disponibles") }

    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val userUid = prefs.getString("user_uid", "") ?: ""
    val userName = prefs.getString("user_name", "") ?: ""
    val userEmail = prefs.getString("user_email", "") ?: ""

    val cursosConSolicitud = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .map { it.codigoCurso }
            .toSet()
    }

    LaunchedEffect(solicitudUiState.mensajeExito) {
        solicitudUiState.mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            solicitudViewModel.clearMessages()
        }
    }

    LaunchedEffect(solicitudUiState.error) {
        solicitudUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            solicitudViewModel.clearMessages()
        }
    }

    val cursosFiltrados = remember(cursoUiState.cursos, searchQuery, selectedFilter) {
        var filtered = cursoUiState.cursos

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedFilter) {
            "Disponibles" -> filtered.filterNot { cursosConSolicitud.contains(it.codigo) }
            "Solicitados" -> filtered.filter { cursosConSolicitud.contains(it.codigo) }
            else -> filtered.filterNot { cursosConSolicitud.contains(it.codigo) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // TopBar como primer item del LazyColumn
            item {
                ResponsiveTopBar(
                    onNavigateBack = onNavigateBack,
                    cursosCount = cursoUiState.cursos.size,
                    pendingCount = cursosConSolicitud.size,
                    isTablet = isTablet
                )
            }

            // Contenido principal
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 32.dp else 16.dp)
                ) {
                    Spacer(Modifier.height(if (isTablet) 20.dp else 16.dp))

                    ModernSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isTablet = isTablet
                    )

                    Spacer(Modifier.height(16.dp))

                    FilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        availableCount = cursoUiState.cursos.size - cursosConSolicitud.size,
                        requestedCount = cursosConSolicitud.size,
                        isTablet = isTablet
                    )

                    Spacer(Modifier.height(20.dp))
                }
            }

            // Estados de carga/vacío/contenido
            when {
                cursoUiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = EduRachaColors.Primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Cargando cursos...",
                                    color = EduRachaColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                cursosFiltrados.isEmpty() -> {
                    item {
                        ModernEmptyView(
                            isSearchActive = searchQuery.isNotBlank(),
                            filterActive = selectedFilter != "Disponibles"
                        )
                    }
                }

                else -> {
                    // Lista de cursos
                    items(cursosFiltrados, key = { it.codigo }) { curso ->
                        val tieneSolicitudPendiente = cursosConSolicitud.contains(curso.codigo)

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = if (isTablet) 32.dp else 16.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                ModernCursoCard(
                                    curso = curso,
                                    tieneSolicitudPendiente = tieneSolicitudPendiente,
                                    isTablet = isTablet,
                                    onSolicitarClick = {
                                        if (tieneSolicitudPendiente) {
                                            cursoSeleccionado = curso
                                            showErrorDialog = true
                                        } else {
                                            cursoSeleccionado = curso
                                            mensajeEstudiante = ""
                                            showSolicitudDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSolicitudDialog && cursoSeleccionado != null) {
        ModernSolicitudDialog(
            curso = cursoSeleccionado!!,
            mensaje = mensajeEstudiante,
            onMensajeChange = { mensajeEstudiante = it },
            isTablet = isTablet,
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

    if (showErrorDialog && cursoSeleccionado != null) {
        ErrorSolicitudDialog(
            curso = cursoSeleccionado!!,
            isTablet = isTablet,
            onDismiss = {
                showErrorDialog = false
                cursoSeleccionado = null
            }
        )
    }
}

@Composable
fun ErrorSolicitudDialog(
    curso: Curso,
    isTablet: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 32.dp else 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 32.dp else 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 90.dp else 80.dp)
                        .background(
                            EduRachaColors.Warning.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(if (isTablet) 46.dp else 42.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Solicitud Pendiente",
                        fontSize = if (isTablet) 26.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tu solicitud está siendo revisada",
                        fontSize = if (isTablet) 16.sp else 15.sp,
                        color = EduRachaColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8F9FA)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = curso.titulo,
                            fontSize = if (isTablet) 17.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = curso.codigo,
                                    fontSize = if (isTablet) 14.sp else 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EduRachaColors.Primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Te notificaremos cuando el docente revise tu solicitud",
                            fontSize = if (isTablet) 14.sp else 13.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTablet) 56.dp else 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "Entendido",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isTablet) 17.sp else 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ResponsiveTopBar(
    onNavigateBack: () -> Unit,
    cursosCount: Int,
    pendingCount: Int,
    isTablet: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EduRachaColors.Primary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = if (isTablet) 32.dp else 16.dp,
                    vertical = if (isTablet) 24.dp else 20.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(if (isTablet) 48.dp else 44.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 24.dp else 22.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pendingCount > 0) {
                        StatBadge(
                            icon = Icons.Default.Schedule,
                            text = "$pendingCount",
                            backgroundColor = EduRachaColors.Warning,
                            iconTint = Color.White,
                            isTablet = isTablet
                        )
                    }
                    StatBadge(
                        icon = Icons.Default.LocalLibrary,
                        text = "$cursosCount",
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        iconTint = Color.White,
                        isTablet = isTablet
                    )
                }
            }

            Spacer(Modifier.height(if (isTablet) 20.dp else 16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Explora Cursos",
                    fontSize = if (isTablet) 30.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Encuentra tu próximo curso",
                    fontSize = if (isTablet) 16.sp else 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    iconTint: Color,
    isTablet: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 14.dp else 12.dp,
                vertical = if (isTablet) 10.dp else 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (isTablet) 20.dp else 18.dp)
            )
            Text(
                text = text,
                color = iconTint,
                fontWeight = FontWeight.Bold,
                fontSize = if (isTablet) 15.sp else 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isTablet: Boolean
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        placeholder = {
            Text(
                "Buscar cursos...",
                color = EduRachaColors.TextSecondary.copy(alpha = 0.5f),
                fontSize = if (isTablet) 16.sp else 15.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(if (isTablet) 26.dp else 24.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Limpiar",
                        tint = EduRachaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = if (isTablet) 16.sp else 15.sp)
    )
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    availableCount: Int,
    requestedCount: Int,
    isTablet: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            label = "Disponibles",
            count = availableCount,
            selected = selectedFilter == "Disponibles",
            onClick = { onFilterSelected("Disponibles") },
            icon = Icons.Default.CheckCircle,
            isTablet = isTablet,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            label = "Solicitados",
            count = requestedCount,
            selected = selectedFilter == "Solicitados",
            onClick = { onFilterSelected("Solicitados") },
            icon = Icons.Default.Schedule,
            isTablet = isTablet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val height = if (isTablet) 100.dp else 86.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EduRachaColors.Primary else Color.White,
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = modifier.height(height)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 18.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else EduRachaColors.Primary,
                modifier = Modifier.size(if (isTablet) 28.dp else 26.dp)
            )

            Text(
                text = label,
                color = if (selected) Color.White else EduRachaColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (isTablet) 15.sp else 14.sp,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) Color.White.copy(alpha = 0.25f) else EduRachaColors.Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = count.toString(),
                    color = if (selected) Color.White else EduRachaColors.Primary,
                    fontSize = if (isTablet) 14.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ModernCursoCard(
    curso: Curso,
    tieneSolicitudPendiente: Boolean,
    isTablet: Boolean,
    onSolicitarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 110.dp else 100.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Primary,
                                EduRachaColors.Primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = curso.codigo,
                                fontSize = if (isTablet) 13.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        if (tieneSolicitudPendiente) {
                            Surface(
                                shape = CircleShape,
                                color = EduRachaColors.Warning
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = curso.titulo,
                        fontSize = if (isTablet) 18.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = 22.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = curso.descripcion,
                    fontSize = if (isTablet) 15.sp else 14.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 20.sp,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernInfoChip(
                        icon = Icons.Outlined.CalendarToday,
                        text = "${curso.duracionDias} días",
                        isTablet = isTablet,
                        modifier = Modifier.weight(1f)
                    )
                    ModernInfoChip(
                        icon = Icons.Outlined.Person,
                        text = "Docente",
                        isTablet = isTablet,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tieneSolicitudPendiente) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Warning.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, EduRachaColors.Warning.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "En revisión",
                                fontSize = if (isTablet) 14.sp else 13.sp,
                                color = EduRachaColors.Warning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = onSolicitarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTablet) 54.dp else 50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tieneSolicitudPendiente)
                            EduRachaColors.TextSecondary.copy(alpha = 0.2f)
                        else EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (tieneSolicitudPendiente) 0.dp else 0.dp
                    )
                ) {
                    Icon(
                        if (tieneSolicitudPendiente) Icons.Default.CheckCircle else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (tieneSolicitudPendiente) "Solicitud Enviada" else "Solicitar Unirse",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isTablet) 16.sp else 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernInfoChip(
    icon: ImageVector,
    text: String,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(if (isTablet) 44.dp else 42.dp),
        shape = RoundedCornerShape(12.dp),
        color = EduRachaColors.Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = if (isTablet) 14.sp else 13.sp,
                color = EduRachaColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernSolicitudDialog(
    curso: Curso,
    mensaje: String,
    onMensajeChange: (String) -> Unit,
    isTablet: Boolean,
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
                .padding(horizontal = if (isTablet) 40.dp else 20.dp)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(if (isTablet) 28.dp else 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 52.dp else 48.dp)
                                    .background(
                                        EduRachaColors.Primary.copy(alpha = 0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(if (isTablet) 28.dp else 26.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(if (isTablet) 44.dp else 40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = EduRachaColors.TextSecondary
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Solicitar Unirse",
                                fontSize = if (isTablet) 28.sp else 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8F9FA)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = curso.titulo,
                                        fontSize = if (isTablet) 17.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EduRachaColors.Primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = curso.codigo,
                                            fontSize = if (isTablet) 14.sp else 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EduRachaColors.Primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = if (isTablet) 28.dp else 24.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Mensaje para el docente",
                            fontSize = if (isTablet) 17.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduRachaColors.TextPrimary
                        )
                    }

                    item {
                        Text(
                            text = "Selecciona un mensaje sugerido:",
                            fontSize = if (isTablet) 14.sp else 13.sp,
                            color = EduRachaColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    items(mensajesPredefinidos) { predefinido ->
                        PredefinedMessageChip(
                            message = predefinido,
                            isSelected = selectedPredefined == predefinido,
                            isTablet = isTablet,
                            onClick = {
                                selectedPredefined = predefinido
                                onMensajeChange(predefinido)
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "O escribe tu propio mensaje:",
                            fontSize = if (isTablet) 14.sp else 13.sp,
                            color = EduRachaColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = mensaje,
                            onValueChange = {
                                onMensajeChange(it)
                                selectedPredefined = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Escribe aquí tu mensaje...",
                                    fontSize = if (isTablet) 15.sp else 14.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduRachaColors.Primary,
                                unfocusedBorderColor = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = if (isTablet) 15.sp else 14.sp)
                        )
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isTablet) 28.dp else 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isTablet) 56.dp else 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, EduRachaColors.TextSecondary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "Cancelar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isTablet) 16.sp else 15.sp,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isTablet) 56.dp else 52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EduRachaColors.Primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Enviar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isTablet) 16.sp else 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredefinedMessageChip(
    message: String,
    isSelected: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) EduRachaColors.Primary.copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.5.dp,
            color = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 16.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Text(
                text = message,
                fontSize = if (isTablet) 14.sp else 13.sp,
                color = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary,
                lineHeight = 19.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ModernEmptyView(isSearchActive: Boolean, filterActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_animation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    EduRachaColors.Primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    isSearchActive -> Icons.Default.SearchOff
                    filterActive -> Icons.Default.FilterAltOff
                    else -> Icons.Default.School
                },
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = EduRachaColors.Primary.copy(alpha = 0.5f)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = when {
                isSearchActive -> "No se encontraron cursos"
                filterActive -> "Sin cursos en esta categoría"
                else -> "No hay cursos disponibles"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = when {
                isSearchActive -> "Intenta con otros términos de búsqueda"
                filterActive -> "Prueba con otro filtro"
                else -> "Pronto habrá nuevos cursos"
            },
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (isSearchActive || filterActive) {
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EduRachaColors.Primary.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Usa palabras clave más generales",
                        fontSize = 13.sp,
                        color = EduRachaColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}