package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.models.OpcionIA
import com.stiven.desarrollomovil.models.PreguntaIA
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import com.stiven.desarrollomovil.viewmodel.ValidacionPreguntasViewModel

class ValidacionPreguntasActivity : ComponentActivity() {

    private val viewModel: ValidacionPreguntasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val temaId = intent.getStringExtra("TEMA_ID") ?: ""

        setContent {
            EduRachaTheme {
                ValidacionPreguntasScreen(
                    cursoTitulo = cursoTitulo,
                    cursoId = cursoId,
                    temaId = temaId,
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun ValidacionPreguntasScreen(
    cursoTitulo: String,
    cursoId: String,
    temaId: String,
    viewModel: ValidacionPreguntasViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(cursoId, temaId) {
        if (cursoId.isNotEmpty() && temaId.isNotEmpty()) {
            viewModel.cargarPreguntasPendientes(cursoId, temaId)
        }
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ValidacionTopAppBar(
                cursoTitulo = cursoTitulo,
                temaId = temaId,
                pendingCount = uiState.preguntas.size,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.preguntas.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = EduRachaColors.Primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando preguntas...", color = EduRachaColors.TextSecondary)
                    }
                }

                uiState.preguntas.isEmpty() && !uiState.isLoading -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Todo validado",
                            Modifier.size(80.dp),
                            EduRachaColors.Success
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "¡Todo validado!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "No hay preguntas pendientes para este tema.",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.preguntas, key = { it.id ?: it.texto }) { pregunta ->
                            PreguntaCard(
                                pregunta = pregunta,
                                onAprobar = { viewModel.aprobarPregunta(pregunta.id ?: "") },
                                onRechazar = { motivo ->
                                    viewModel.rechazarPregunta(pregunta.id ?: "", motivo)
                                },
                                onEditar = { preguntaEditada ->
                                    viewModel.actualizarPregunta(preguntaEditada)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTopAppBar(
    cursoTitulo: String,
    temaId: String,
    pendingCount: Int,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Validar: $cursoTitulo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "$pendingCount preguntas pendientes | Tema: $temaId",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = EduRachaColors.Primary),
        modifier = Modifier.shadow(4.dp)
    )
}

@Composable
fun PreguntaCard(
    pregunta: PreguntaIA,
    onAprobar: () -> Unit,
    onRechazar: (String) -> Unit,
    onEditar: (PreguntaIA) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con ID y Dificultad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val idPregunta = pregunta.id?.takeLast(4) ?: "N/A"
                    Surface(
                        color = EduRachaColors.Primary.copy(0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "ID: $idPregunta",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = EduRachaColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    val dificultad = pregunta.dificultad?.lowercase() ?: "media"
                    val colorDificultad = when (dificultad) {
                        "facil", "bajo" -> EduRachaColors.Success
                        "media", "medio" -> EduRachaColors.Warning
                        "dificil", "alto" -> EduRachaColors.Error
                        else -> EduRachaColors.TextSecondary
                    }
                    Surface(
                        color = colorDificultad.copy(0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            dificultad.replaceFirstChar { it.uppercase() },
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = colorDificultad,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expandir"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                pregunta.texto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Opciones
            pregunta.opciones.forEachIndexed { index, opcion ->
                OpcionItem(('A' + index).toString(), opcion.texto, opcion.esCorrecta)
                if (index < pregunta.opciones.size - 1) Spacer(Modifier.height(8.dp))
            }

            // Sección Expandible
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(16.dp))
                InfoRow(
                    Icons.Default.AutoAwesome,
                    "Generado por",
                    pregunta.metadatosIA?.generadoPor ?: "No especificado"
                )
                Spacer(Modifier.height(8.dp))
                val fuente = pregunta.fuente ?: "No especificada"
                if(fuente.isNotBlank()) InfoRow(Icons.Default.Bookmark, "Fuente", fuente)
            }

            // Botones de Acción
            Spacer(Modifier.height(20.dp))
            // --- CÓDIGO COMPLETADO ---
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EduRachaColors.Error
                    )
                ) {
                    Icon(Icons.Default.ThumbDown, contentDescription = "Rechazar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Rechazar")
                }
                Button(
                    onClick = onAprobar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Success
                    )
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Aprobar", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aprobar")
                }
            }
        }
    }

    if (showEditDialog) {
        var textoEditado by remember { mutableStateOf(pregunta.texto) }
        val opcionesOriginales = pregunta.opciones
        var opcionesEditadas by remember {
            mutableStateOf(opcionesOriginales.map { it.texto })
        }
        var respuestaCorrectaIndex by remember {
            mutableStateOf(opcionesOriginales.indexOfFirst { it.esCorrecta })
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar pregunta") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        OutlinedTextField(
                            value = textoEditado,
                            onValueChange = { textoEditado = it },
                            label = { Text("Texto de la pregunta") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                    items(opcionesEditadas.size) { index ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = respuestaCorrectaIndex == index,
                                onClick = { respuestaCorrectaIndex = index }
                            )
                            TextField(
                                value = opcionesEditadas[index],
                                onValueChange = {
                                    opcionesEditadas = opcionesEditadas.toMutableList()
                                        .also { list -> list[index] = it }
                                },
                                label = { Text("Opción ${'A' + index}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val preguntaEditada = pregunta.copy(
                        texto = textoEditado,
                        opciones = opcionesEditadas.mapIndexed { index, textoOpcion ->
                            val idOriginal = opcionesOriginales.getOrNull(index)?.id ?: "op${System.currentTimeMillis()}"
                            OpcionIA(
                                id = idOriginal,
                                texto = textoOpcion,
                                esCorrecta = index == respuestaCorrectaIndex
                            )
                        }
                    )
                    onEditar(preguntaEditada)
                    showEditDialog = false
                }) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton({ showEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showRejectDialog) {
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rechazar pregunta") },
            text = {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo del rechazo") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivo.isNotBlank()) {
                            onRechazar(motivo)
                            showRejectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    )
                ) { Text("Confirmar Rechazo") }
            },
            dismissButton = {
                TextButton({ showRejectDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun OpcionItem(letra: String, texto: String, esCorrecta: Boolean) {
    val backgroundColor = if (esCorrecta) {
        EduRachaColors.Success.copy(0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (esCorrecta) EduRachaColors.Success else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (esCorrecta) EduRachaColors.Success else MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(letra, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            Modifier.size(16.dp),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
