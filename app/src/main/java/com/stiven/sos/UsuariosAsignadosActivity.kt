package com.stiven.sos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.UsuarioAsignado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.UsuarioViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke

class UsuariosAsignadosActivity : ComponentActivity() {
    private val viewModel: UsuarioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"

        setContent {
            EduRachaTheme {
                UsuariosAsignadosScreen(
                    viewModel = viewModel,
                    cursoId = cursoId,
                    cursoTitulo = cursoTitulo,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosAsignadosScreen(
    viewModel: UsuarioViewModel,
    cursoId: String,
    cursoTitulo: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showActionDialog by remember { mutableStateOf(false) }
    var accionPendiente by remember { mutableStateOf<AccionEstudiante?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var ultimaAccion by remember { mutableStateOf<AccionRealizada?>(null) }

    LaunchedEffect(cursoId) {
        viewModel.cargarEstudiantesPorCurso(cursoId, cursoTitulo)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val usuariosFiltrados = remember(uiState.usuarios, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.usuarios
        } else {
            uiState.usuarios.filter { usuario ->
                usuario.nombre.contains(searchQuery, ignoreCase = true) ||
                        usuario.correo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Estudiantes Asignados",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            cursoTitulo,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState.usuarios.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.usuarios.size}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.usuarios.isNotEmpty()) {
                SearchBarEstudiantes(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                                    "Cargando estudiantes...",
                                    color = EduRachaColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    uiState.error != null && uiState.usuarios.isEmpty() -> {
                        ErrorEstudiantesView(
                            message = uiState.error ?: "Error desconocido",
                            onRetry = {
                                viewModel.cargarEstudiantesPorCurso(cursoId, cursoTitulo)
                            }
                        )
                    }

                    usuariosFiltrados.isEmpty() -> {
                        EmptyEstudiantesView(
                            isSearchActive = searchQuery.isNotBlank()
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                usuariosFiltrados,
                                key = { _, usuario -> usuario.uid }
                            ) { index, usuario ->
                                AnimatedEstudianteCard(
                                    usuario = usuario,
                                    index = index,
                                    onCambiarEstado = { nuevoEstado ->
                                        accionPendiente = AccionEstudiante(
                                            usuario = usuario,
                                            nuevoEstado = nuevoEstado
                                        )
                                        showActionDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de acción
    if (showActionDialog && accionPendiente != null) {
        ConfirmacionAccionDialog(
            accion = accionPendiente!!,
            onConfirm = {
                viewModel.cambiarEstadoEstudiante(
                    cursoId = cursoId,
                    estudianteId = accionPendiente!!.usuario.uid,
                    nuevoEstado = accionPendiente!!.nuevoEstado
                )
                ultimaAccion = AccionRealizada(
                    nombreEstudiante = accionPendiente!!.usuario.nombre,
                    accion = accionPendiente!!.nuevoEstado
                )
                showActionDialog = false
                showSuccessDialog = true
                accionPendiente = null
            },
            onDismiss = {
                showActionDialog = false
                accionPendiente = null
            }
        )
    }

    // Diálogo de éxito
    if (showSuccessDialog && ultimaAccion != null) {
        SuccessDialog(
            accion = ultimaAccion!!,
            onDismiss = {
                showSuccessDialog = false
                ultimaAccion = null
            }
        )
    }
}

@Composable
fun AnimatedEstudianteCard(
    usuario: UsuarioAsignado,
    index: Int,
    onCambiarEstado: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(400))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (usuario.estado.lowercase() == "activo") 3.dp else 1.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Contenido principal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar mejorado con gradiente
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = obtenerGradienteEstado(usuario.estado)
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = obtenerIniciales(usuario.nombre),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Información del estudiante
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = usuario.nombre,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EduRachaColors.TextSecondary
                            )
                            Text(
                                text = usuario.correo,
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Sección de estado y acciones
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge de estado mejorado
                        EstadoBadge(estado = usuario.estado)

                        // Botones de acción
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (usuario.estado.lowercase()) {
                                "activo" -> {
                                    ActionChip(
                                        icon = Icons.Default.PauseCircle,
                                        text = "Desactivar",
                                        color = Color(0xFFFF9800),
                                        onClick = { onCambiarEstado("inactivo") }
                                    )
                                }
                                "inactivo" -> {
                                    ActionChip(
                                        icon = Icons.Default.PlayCircle,
                                        text = "Activar",
                                        color = EduRachaColors.Success,
                                        onClick = { onCambiarEstado("activo") }
                                    )
                                }
                            }

                            if (usuario.estado.lowercase() != "eliminado") {
                                ActionChip(
                                    icon = Icons.Default.DeleteForever,
                                    text = "Eliminar",
                                    color = Color(0xFFE53935),
                                    onClick = { onCambiarEstado("eliminado") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            pressed = true
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.scale(if (pressed) 0.95f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

@Composable
fun EstadoBadge(estado: String) {
    val (icon, color) = when (estado.lowercase()) {
        "activo" -> Icons.Default.CheckCircle to EduRachaColors.Success
        "inactivo" -> Icons.Default.PauseCircle to Color(0xFFFF9800)
        "eliminado" -> Icons.Default.Cancel to Color(0xFF757575)
        else -> Icons.Default.Help to EduRachaColors.Primary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = estado.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ConfirmacionAccionDialog(
    accion: AccionEstudiante,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (titulo, mensaje, icon, color) = when (accion.nuevoEstado.lowercase()) {
        "activo" -> Quadruple(
            "Activar Estudiante",
            "¿Deseas activar a ${accion.usuario.nombre}?\n\nEl estudiante podrá acceder nuevamente al curso y sus contenidos.",
            Icons.Default.PlayCircle,
            EduRachaColors.Success
        )
        "inactivo" -> Quadruple(
            "Desactivar Estudiante",
            "¿Deseas desactivar a ${accion.usuario.nombre}?\n\nEl estudiante no podrá acceder temporalmente al curso.",
            Icons.Default.PauseCircle,
            Color(0xFFFF9800)
        )
        "eliminado" -> Quadruple(
            "Eliminar Estudiante",
            "¿Estás seguro de eliminar a ${accion.usuario.nombre} del curso?\n\n⚠️ Esta acción no se puede deshacer.",
            Icons.Default.DeleteForever,
            Color(0xFFE53935)
        )
        else -> Quadruple(
            "Confirmar Acción",
            "¿Deseas continuar con esta acción?",
            Icons.Default.Help,
            EduRachaColors.Primary
        )
    }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            title = {
                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    mensaje,
                    textAlign = TextAlign.Center,
                    color = EduRachaColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        "Confirmar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(1.5.dp, EduRachaColors.TextSecondary.copy(alpha = 0.3f))
                ) {
                    Text(
                        "Cancelar",
                        color = EduRachaColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun SuccessDialog(
    accion: AccionRealizada,
    onDismiss: () -> Unit
) {
    val (titulo, mensaje, icon, color) = when (accion.accion.lowercase()) {
        "activo" -> Quadruple(
            "¡Estudiante Activado!",
            "${accion.nombreEstudiante} ahora tiene acceso al curso.",
            Icons.Default.CheckCircle,
            EduRachaColors.Success
        )
        "inactivo" -> Quadruple(
            "Estudiante Desactivado",
            "${accion.nombreEstudiante} ha sido desactivado temporalmente.",
            Icons.Default.PauseCircle,
            Color(0xFFFF9800)
        )
        "eliminado" -> Quadruple(
            "Estudiante Eliminado",
            "${accion.nombreEstudiante} ha sido eliminado del curso.",
            Icons.Default.CheckCircle,
            Color(0xFFE53935)
        )
        else -> Quadruple(
            "¡Acción Completada!",
            "La operación se realizó correctamente.",
            Icons.Default.CheckCircle,
            EduRachaColors.Success
        )
    }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2500)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.7f) + slideInVertically(initialOffsetY = { -it / 4 }),
        exit = fadeOut() + scaleOut(targetScale = 0.7f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.2f),
                                    color.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(44.dp)
                    )
                }
            },
            title = {
                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    color = color
                )
            },
            text = {
                Text(
                    mensaje,
                    textAlign = TextAlign.Center,
                    color = EduRachaColors.TextSecondary,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        "Entendido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarEstudiantes(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Buscar estudiante...",
                color = EduRachaColors.TextSecondary.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "Buscar",
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        "Limpiar",
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
fun ErrorEstudiantesView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            null,
            tint = EduRachaColors.Error.copy(alpha = 0.7f),
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Error al cargar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(50.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Reintentar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyEstudiantesView(isSearchActive: Boolean) {
    val (title, subtitle, icon) = if (isSearchActive)
        Triple(
            "Sin resultados",
            "No encontramos estudiantes con ese criterio.\nIntenta con otra búsqueda.",
            Icons.Outlined.SearchOff
        )
    else
        Triple(
            "Sin estudiantes",
            "Aún no hay estudiantes inscritos en este curso.",
            Icons.Outlined.PersonOff
        )

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(120.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            subtitle,
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// Clases de datos auxiliares
data class AccionEstudiante(
    val usuario: UsuarioAsignado,
    val nuevoEstado: String
)

data class AccionRealizada(
    val nombreEstudiante: String,
    val accion: String
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Funciones auxiliares
private fun obtenerIniciales(nombre: String): String {
    val palabras = nombre.trim().split(" ")
    return when {
        palabras.size >= 2 -> "${palabras[0].first()}${palabras[1].first()}".uppercase()
        palabras.isNotEmpty() -> palabras[0].take(2).uppercase()
        else -> "?"
    }
}

private fun obtenerGradienteEstado(estado: String): List<Color> {
    return when (estado.lowercase()) {
        "activo" -> listOf(
            Color(0xFF4CAF50),
            Color(0xFF81C784)
        )
        "inactivo" -> listOf(
            Color(0xFFFF9800),
            Color(0xFFFFB74D)
        )
        "suspendido" -> listOf(
            Color(0xFFE53935),
            Color(0xFFEF5350)
        )
        "eliminado" -> listOf(
            Color(0xFF757575),
            Color(0xFF9E9E9E)
        )
        else -> listOf(
            EduRachaColors.Primary,
            EduRachaColors.Primary.copy(alpha = 0.7f)
        )
    }
}