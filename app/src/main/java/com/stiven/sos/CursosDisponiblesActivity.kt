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
import androidx.compose.ui.graphics.vector.ImageVector
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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.Primary.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ModernTopBar(
                onNavigateBack = onNavigateBack,
                cursosCount = cursoUiState.cursos.size,
                pendingCount = cursosConSolicitud.size
            )

            ModernSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                availableCount = cursoUiState.cursos.size - cursosConSolicitud.size,
                requestedCount = cursosConSolicitud.size
            )

            when {
                cursoUiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = EduRachaColors.Primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Cargando cursos...",
                                color = EduRachaColors.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                cursosFiltrados.isEmpty() -> {
                    ModernEmptyView(
                        isSearchActive = searchQuery.isNotBlank(),
                        filterActive = selectedFilter != "Disponibles"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cursosFiltrados, key = { it.codigo }) { curso ->
                            val tieneSolicitudPendiente = cursosConSolicitud.contains(curso.codigo)

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically()
                            ) {
                                ModernCursoCard(
                                    curso = curso,
                                    tieneSolicitudPendiente = tieneSolicitudPendiente,
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono de error animado
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            EduRachaColors.Warning.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Solicitud ya enviada",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Ya tienes una solicitud pendiente para este curso. El docente revisará tu solicitud pronto.",
                    fontSize = 15.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.05f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = curso.titulo,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Código: ",
                                fontSize = 13.sp,
                                color = EduRachaColors.TextSecondary
                            )
                            Text(
                                text = curso.codigo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, EduRachaColors.Warning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estado: En revisión",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Warning
                            )
                            Text(
                                text = "Recibirás una notificación cuando el docente responda",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        "Entendido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModernTopBar(
    onNavigateBack: () -> Unit,
    cursosCount: Int,
    pendingCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingCount > 0) {
                    StatBadge(
                        icon = Icons.Default.HourglassTop,
                        text = "$pendingCount",
                        backgroundColor = Color.White.copy(alpha = 0.2f)
                    )
                }
                StatBadge(
                    icon = Icons.Default.School,
                    text = "$cursosCount",
                    backgroundColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Column {
            Text(
                text = "Explora Cursos",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Encuentra el curso perfecto para ti",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun StatBadge(
    icon: ImageVector,
    text: String,
    backgroundColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        placeholder = {
            Text(
                "Buscar por nombre, código o descripción...",
                color = EduRachaColors.TextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Limpiar",
                        tint = EduRachaColors.TextSecondary
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
        singleLine = true
    )
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    availableCount: Int,
    requestedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            label = "Disponibles",
            count = availableCount,
            selected = selectedFilter == "Disponibles",
            onClick = { onFilterSelected("Disponibles") },
            icon = Icons.Default.CheckCircle
        )
        FilterChip(
            label = "Solicitados",
            count = requestedCount,
            selected = selectedFilter == "Solicitados",
            onClick = { onFilterSelected("Solicitados") },
            icon = Icons.Default.HourglassTop
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
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EduRachaColors.Primary else Color.White,
        border = if (!selected) BorderStroke(1.5.dp, EduRachaColors.Primary.copy(alpha = 0.3f)) else null,
        modifier = modifier.shadow(if (selected) 6.dp else 0.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else EduRachaColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = if (selected) Color.White else EduRachaColors.TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White.copy(alpha = 0.3f) else EduRachaColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = count.toString(),
                        color = if (selected) Color.White else EduRachaColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCursoCard(
    curso: Curso,
    tieneSolicitudPendiente: Boolean,
    onSolicitarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Primary,
                                EduRachaColors.Primary.copy(alpha = 0.7f)
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = curso.codigo,
                                fontSize = 12.sp,
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
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = curso.titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = curso.descripcion,
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 22.sp,
                    maxLines = 3
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernInfoChip(
                        icon = Icons.Outlined.CalendarToday,
                        text = "${curso.duracionDias} días",
                        modifier = Modifier.weight(1f)
                    )
                    ModernInfoChip(
                        icon = Icons.Outlined.Person,
                        text = "Docente",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tieneSolicitudPendiente) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Warning.copy(alpha = 0.1f),
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
                                text = "Solicitud en revisión por el docente",
                                fontSize = 13.sp,
                                color = EduRachaColors.Warning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onSolicitarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tieneSolicitudPendiente)
                            EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                        else EduRachaColors.Primary,
                        disabledContainerColor = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (tieneSolicitudPendiente) 0.dp else 4.dp
                    )
                ) {
                    Icon(
                        if (tieneSolicitudPendiente) Icons.Default.CheckCircle else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (tieneSolicitudPendiente) "Solicitud Enviada" else "Solicitar Unirse",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = EduRachaColors.Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                fontSize = 13.sp,
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
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header fijo
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        EduRachaColors.Primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = EduRachaColors.TextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Solicitar Unirse",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EduRachaColors.Primary.copy(alpha = 0.05f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = curso.titulo,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Primary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Código:",
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                    Text(
                                        text = curso.codigo,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Contenido scrolleable
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Mensaje para el docente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduRachaColors.TextPrimary
                        )
                    }

                    item {
                        Text(
                            text = "Mensajes sugeridos:",
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    items(mensajesPredefinidos) { predefinido ->
                        PredefinedMessageChip(
                            message = predefinido,
                            isSelected = selectedPredefined == predefinido,
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
                            fontSize = 13.sp,
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
                                    "Escribe aquí tu mensaje personalizado...",
                                    fontSize = 14.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EduRachaColors.Primary,
                                unfocusedBorderColor = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }

                // Footer fijo con botones
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, EduRachaColors.TextSecondary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                "Cancelar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EduRachaColors.Primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Enviar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EduRachaColors.Primary.copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextSecondary,
                lineHeight = 18.sp,
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_animation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    EduRachaColors.Primary.copy(alpha = 0.1f),
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
                modifier = Modifier.size(60.dp),
                tint = EduRachaColors.Primary.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = when {
                isSearchActive -> "No se encontraron cursos"
                filterActive -> "Sin cursos en esta categoría"
                else -> "No hay cursos disponibles por ahora"
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
                filterActive -> "Prueba con otro filtro para ver más cursos"
                else -> "Pronto habrá nuevos cursos disponibles"
            },
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (isSearchActive || filterActive) {
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EduRachaColors.Primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tip: Usa palabras clave más generales",
                        fontSize = 13.sp,
                        color = EduRachaColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}