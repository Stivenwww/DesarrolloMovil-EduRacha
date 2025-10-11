package com.stiven.desarrollomovil

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.stiven.desarrollomovil.models.Curso
import com.stiven.desarrollomovil.viewmodel.CursoViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

class ListaCursosActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EduRachaTheme {
                ListaCursosScreen(
                    viewModel = cursoViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCursosScreen(
    viewModel: CursoViewModel,
    onNavigateBack: () -> Unit
) {
    // Observar los estados del ViewModel
    val cursos by viewModel.cursos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()

    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }

    // Cargar cursos cuando se inicia
    LaunchedEffect(Unit) {
        viewModel.obtenerCursos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cursos Creados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(EduRachaColors.Background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EduRachaColors.Primary
                    )
                }

                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage ?: "Error desconocido",
                        onRetry = { viewModel.obtenerCursos() }
                    )
                }

                cursos.isEmpty() -> {
                    EmptyCursosState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(cursos) { index, curso ->
                            AnimatedCursoCard(
                                curso = curso,
                                index = index,
                                onClick = { cursoSeleccionado = curso }
                            )
                        }
                    }
                }
            }

            // Mostrar detalles del curso si se selecciona
            cursoSeleccionado?.let { curso ->
                CursoDetailDialog(
                    curso = curso,
                    viewModel = viewModel,
                    onDismiss = { cursoSeleccionado = null }
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = EduRachaColors.Error,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Oops, algo salió mal",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = EduRachaColors.Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun AnimatedCursoCard(
    curso: Curso,
    index: Int,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(400)
        ) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header con color de estado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(obtenerColorEstado(curso.estado))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono del curso
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(obtenerColorEstado(curso.estado).copy(alpha = 0.1f))
                            .border(
                                2.dp,
                                obtenerColorEstado(curso.estado).copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = obtenerColorEstado(curso.estado),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Información
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = curso.titulo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = curso.codigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = obtenerColorEstado(curso.estado)
                            )

                            Text(
                                text = " • ",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )

                            Text(
                                text = "${curso.duracionDias} días",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chip de estado
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = obtenerColorEstado(curso.estado).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = obtenerIconoEstado(curso.estado),
                                    contentDescription = null,
                                    tint = obtenerColorEstado(curso.estado),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = curso.estado.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = obtenerColorEstado(curso.estado)
                                )
                            }
                        }
                    }

                    // Flecha
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = EduRachaColors.TextSecondary
                    )
                }

                // Preview de la descripción
                if (curso.descripcion.isNotEmpty()) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = EduRachaColors.Background
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (curso.descripcion.length > 120) {
                                curso.descripcion.substring(0, 120) + "..."
                            } else {
                                curso.descripcion
                            },
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CursoDetailDialog(
    curso: Curso,
    viewModel: CursoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Estados para edición
    var modoEdicion by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf(curso.titulo) }
    var descripcion by remember { mutableStateOf(curso.descripcion) }
    var duracionDias by remember { mutableStateOf(curso.duracionDias.toString()) }
    var estado by remember { mutableStateOf(curso.estado) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = obtenerColorEstado(estado),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (modoEdicion) "Editar curso" else curso.titulo,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (modoEdicion) {
                // Modo edición
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título del curso") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = duracionDias,
                        onValueChange = { duracionDias = it.filter { c -> c.isDigit() } },
                        label = { Text("Duración (días)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de estado
                    Text("Estado", fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownMenuEstado(
                        estadoSeleccionado = estado,
                        onEstadoSeleccionado = { estado = it }
                    )
                }

            } else {
                // Vista de detalles normal
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DetailRow(
                        label = "Código",
                        value = curso.codigo,
                        icon = Icons.Outlined.Tag,
                        color = EduRachaColors.Primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        label = "Duración",
                        value = "${curso.duracionDias} días",
                        icon = Icons.Outlined.CalendarMonth,
                        color = EduRachaColors.Accent
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        label = "Estado",
                        value = curso.estado.replaceFirstChar { it.uppercase() },
                        icon = obtenerIconoEstado(curso.estado),
                        color = obtenerColorEstado(curso.estado)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        label = "ID Docente",
                        value = curso.docenteId,
                        icon = Icons.Outlined.Person,
                        color = EduRachaColors.Secondary
                    )

                    if (curso.descripcion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = EduRachaColors.Background)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = EduRachaColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Descripción",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = curso.descripcion,
                                    fontSize = 13.sp,
                                    color = EduRachaColors.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (modoEdicion) {
                    // Botón Guardar cambios
                    Button(
                        onClick = {
                            val cursoActualizado = curso.copy(
                                titulo = titulo,
                                descripcion = descripcion,
                                duracionDias = duracionDias.toIntOrNull() ?: curso.duracionDias,
                                estado = estado
                            )

                            viewModel.actualizarCurso(
                                curso.id!!,
                                cursoActualizado,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Curso actualizado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    modoEdicion = false
                                    onDismiss()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar")
                    }
                } else {
                    // Botón Editar
                    Button(
                        onClick = { modoEdicion = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }
                }

                // Botón Cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = obtenerColorEstado(estado)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun DropdownMenuEstado(
    estadoSeleccionado: String,
    onEstadoSeleccionado: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val opciones = listOf("activo", "inactivo", "borrador", "archivado")

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = estadoSeleccionado.replaceFirstChar { it.uppercase() },
                modifier = Modifier.weight(1f),
                color = EduRachaColors.TextPrimary
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onEstadoSeleccionado(opcion)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = EduRachaColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

@Composable
fun EmptyCursosState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = EduRachaColors.Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No tienes cursos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )

        Text(
            text = "Crea tu primer curso para comenzar",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = EduRachaColors.Accent.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = EduRachaColors.Accent,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Los cursos te ayudan a organizar tu contenido educativo",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Funciones auxiliares
fun obtenerColorEstado(estado: String): Color {
    return when (estado.lowercase()) {
        "activo" -> EduRachaColors.Success
        "inactivo" -> EduRachaColors.TextSecondary
        "borrador" -> EduRachaColors.Warning
        "archivado" -> EduRachaColors.Info
        else -> EduRachaColors.Primary
    }
}

fun obtenerIconoEstado(estado: String): ImageVector {
    return when (estado.lowercase()) {
        "activo" -> Icons.Default.CheckCircle
        "inactivo" -> Icons.Default.Cancel
        "borrador" -> Icons.Default.Edit
        "archivado" -> Icons.Default.Archive
        else -> Icons.Default.Circle
    }
}