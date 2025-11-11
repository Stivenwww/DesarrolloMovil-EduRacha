package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stiven.sos.models.Tema
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.TemaViewModel

class GestionExplicacionesActivity : ComponentActivity() {

    private val temaViewModel: TemaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"

        if (cursoId.isEmpty()) {
            finish()
            return
        }

        temaViewModel.cargarTemasPorCurso(cursoId, "pendiente")

        setContent {
            EduRachaTheme {
                GestionExplicacionesScreen(
                    cursoId = cursoId,
                    cursoTitulo = cursoTitulo,
                    viewModel = temaViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionExplicacionesScreen(
    cursoId: String,
    cursoTitulo: String,
    viewModel: TemaViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var temaSeleccionado by remember { mutableStateOf<Tema?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensaje de éxito
    LaunchedEffect(uiState.operationSuccess) {
        uiState.operationSuccess?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Explicaciones Pendientes",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = cursoTitulo,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(EduRachaColors.Background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = {
                            viewModel.clearError()
                            viewModel.cargarTemasPorCurso(cursoId, "pendiente")
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.temas.isEmpty() -> {
                    EmptyStateView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.temas) { tema ->
                            TemaExplicacionCard(
                                tema = tema,
                                onVerDetalle = { temaSeleccionado = tema },
                                onAprobar = {
                                    viewModel.actualizarEstadoExplicacion(
                                        cursoId = cursoId,
                                        temaId = tema.id,
                                        nuevoEstado = "aprobada"
                                    )
                                },
                                onRechazar = {
                                    viewModel.actualizarEstadoExplicacion(
                                        cursoId = cursoId,
                                        temaId = tema.id,
                                        nuevoEstado = "rechazada"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de detalle
    temaSeleccionado?.let { tema ->
        DetalleExplicacionDialog(
            tema = tema,
            onDismiss = { temaSeleccionado = null },
            onAprobar = {
                viewModel.actualizarEstadoExplicacion(
                    cursoId = cursoId,
                    temaId = tema.id,
                    nuevoEstado = "aprobada"
                )
                temaSeleccionado = null
            },
            onRechazar = {
                viewModel.actualizarEstadoExplicacion(
                    cursoId = cursoId,
                    temaId = tema.id,
                    nuevoEstado = "rechazada"
                )
                temaSeleccionado = null
            }
        )
    }
}

@Composable
fun TemaExplicacionCard(
    tema: Tema,
    onVerDetalle: () -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tema.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FuenteChip(fuente = tema.explicacionFuente ?: "desconocida")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview de la explicación
            Text(
                text = tema.explicacion ?: "Sin explicación",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = EduRachaColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerDetalle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver")
                }

                Button(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }

                Button(
                    onClick = onAprobar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Success
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}

@Composable
fun FuenteChip(fuente: String) {
    val (color, icon, texto) = when (fuente.lowercase()) {
        "ia" -> Triple(
            Color(0xFFE3F2FD),
            Icons.Default.AutoAwesome,
            "Generada por IA"
        )
        "docente" -> Triple(
            Color(0xFFFFF3E0),
            Icons.Default.Person,
            "Creada por docente"
        )
        else -> Triple(
            EduRachaColors.Surface,
            Icons.Default.HelpOutline,
            "Fuente desconocida"
        )
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = EduRachaColors.TextSecondary
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                color = EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun DetalleExplicacionDialog(
    tema: Tema,
    onDismiss: () -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = tema.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FuenteChip(fuente = tema.explicacionFuente ?: "desconocida")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Contenido del tema:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tema.contenido.ifBlank { "Sin contenido" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = EduRachaColors.TextSecondary
                    )
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Text(
                        text = "Explicación generada:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tema.explicacion ?: "Sin explicación",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAprobar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Success
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aprobar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onRechazar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }
            }
        }
    )
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = EduRachaColors.Success
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Todo al día!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No hay explicaciones pendientes de revisión",
            style = MaterialTheme.typography.bodyMedium,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = EduRachaColors.Error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error al cargar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = EduRachaColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reintentar")
        }
    }
}

