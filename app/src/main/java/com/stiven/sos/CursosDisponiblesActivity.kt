package com.stiven.sos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // Cargar cursos y solicitudes
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
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var mensajeEstudiante by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Obtener datos del usuario
    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val userUid = prefs.getString("user_uid", "") ?: ""
    val userName = prefs.getString("user_name", "") ?: ""
    val userEmail = prefs.getString("user_email", "") ?: ""

    // ✅ Crear un mapa de cursos con solicitudes existentes
    val cursosConSolicitud = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .map { it.codigoCurso }
            .toSet()
    }

    // Mostrar mensajes
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

    // Filtrar cursos
    val cursosFiltrados = remember(cursoUiState.cursos, searchQuery) {
        if (searchQuery.isBlank()) {
            cursoUiState.cursos
        } else {
            cursoUiState.cursos.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cursos Disponibles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda
            SearchBarCursos(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier.padding(16.dp)
            )

            // Contenido
            when {
                cursoUiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EduRachaColors.Primary)
                    }
                }

                cursosFiltrados.isEmpty() -> {
                    EmptyCursosView(isSearchActive = searchQuery.isNotBlank())
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cursosFiltrados) { curso ->
                            // ✅ Verificar si ya tiene solicitud pendiente
                            val tieneSolicitudPendiente = cursosConSolicitud.contains(curso.codigo)

                            CursoDisponibleCard(
                                curso = curso,
                                tieneSolicitudPendiente = tieneSolicitudPendiente,
                                onSolicitarClick = {
                                    if (tieneSolicitudPendiente) {
                                        Toast.makeText(
                                            context,
                                            "Ya tienes una solicitud pendiente para este curso",
                                            Toast.LENGTH_SHORT
                                        ).show()
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

    // ✅ Diálogo con campo de mensaje
    if (showSolicitudDialog && cursoSeleccionado != null) {
        AlertDialog(
            onDismissRequest = {
                showSolicitudDialog = false
                cursoSeleccionado = null
                mensajeEstudiante = ""
            },
            icon = {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Solicitar unirse al curso",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("¿Deseas enviar una solicitud para unirte a:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        cursoSeleccionado?.titulo ?: "",
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Código: ${cursoSeleccionado?.codigo}",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )

                    Spacer(Modifier.height(16.dp))

                    // ✅ Campo de mensaje
                    OutlinedTextField(
                        value = mensajeEstudiante,
                        onValueChange = { mensajeEstudiante = it },
                        label = { Text("Mensaje para el docente (opcional)") },
                        placeholder = { Text("¿Por qué quieres unirte a este curso?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cursoSeleccionado?.let { curso ->
                            solicitudViewModel.crearSolicitud(
                                codigoCurso = curso.codigo,
                                estudianteId = userUid,
                                estudianteNombre = userName,
                                estudianteEmail = userEmail,
                                mensaje = mensajeEstudiante.ifBlank { null }
                            )
                        }
                        showSolicitudDialog = false
                        cursoSeleccionado = null
                        mensajeEstudiante = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    )
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSolicitudDialog = false
                    cursoSeleccionado = null
                    mensajeEstudiante = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarCursos(
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
                "Buscar cursos...",
                color = EduRachaColors.TextSecondary.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "Buscar",
                tint = EduRachaColors.Primary
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
        shape = RoundedCornerShape(50),
        singleLine = true
    )
}

@Composable
fun CursoDisponibleCard(
    curso: Curso,
    tieneSolicitudPendiente: Boolean,
    onSolicitarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tieneSolicitudPendiente)
                EduRachaColors.Warning.copy(alpha = 0.05f)
            else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = curso.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = curso.codigo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = curso.descripcion,
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.CalendarToday,
                    text = "${curso.duracionDias} días"
                )
                InfoChip(
                    icon = Icons.Default.Person,
                    text = curso.docenteId
                )
            }

            // ✅ Badge si ya tiene solicitud pendiente
            if (tieneSolicitudPendiente) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Solicitud pendiente - Esperando respuesta del docente",
                            fontSize = 12.sp,
                            color = EduRachaColors.Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSolicitarClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tieneSolicitudPendiente)
                        EduRachaColors.TextSecondary
                    else EduRachaColors.Primary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !tieneSolicitudPendiente
            ) {
                Icon(
                    if (tieneSolicitudPendiente) Icons.Default.CheckCircle else Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tieneSolicitudPendiente) "Solicitud enviada" else "Solicitar unirse",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EduRachaColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun EmptyCursosView(isSearchActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearchActive) Icons.Default.SearchOff else Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearchActive) "No se encontraron cursos" else "No hay cursos disponibles",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            text = if (isSearchActive) "Intenta con otra búsqueda" else "Pronto habrá nuevos cursos",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}