package com.stiven.sos

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.PreguntaViewModel

class ValidacionPreguntasActivity : ComponentActivity() {

    private val viewModel: PreguntaViewModel by viewModels()

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
    viewModel: PreguntaViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(cursoId, temaId) {
        if (cursoId.isNotEmpty() && temaId.isNotEmpty()) {
            viewModel.cargarPreguntas(
                cursoId = cursoId,
                estado = EstadoPregunta.PENDIENTE_REVISION
            )
        }
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val preguntasFiltradas = remember(uiState.preguntas, temaId) {
        uiState.preguntas.filter { it.temaId == temaId }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ValidacionTopAppBar(
                cursoTitulo = cursoTitulo,
                temaId = temaId,
                pendingCount = preguntasFiltradas.size,
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
                uiState.isLoading && preguntasFiltradas.isEmpty() -> {
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

                preguntasFiltradas.isEmpty() && !uiState.isLoading -> {
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
                        items(preguntasFiltradas, key = { it.id ?: it.texto }) { pregunta ->
                            PreguntaCard(
                                pregunta = pregunta,
                                onAprobar = {
                                    viewModel.actualizarEstadoPregunta(
                                        id = pregunta.id ?: "",
                                        estado = EstadoPregunta.APROBADA,
                                        notas = "Aprobada por el docente",
                                        onSuccess = {
                                            viewModel.cargarPreguntas(
                                                cursoId = cursoId,
                                                estado = EstadoPregunta.PENDIENTE_REVISION
                                            )
                                        }
                                    )
                                },
                                onRechazar = { motivo ->
                                    viewModel.actualizarEstadoPregunta(
                                        id = pregunta.id ?: "",
                                        estado = EstadoPregunta.RECHAZADA,
                                        notas = motivo,
                                        onSuccess = {
                                            viewModel.cargarPreguntas(
                                                cursoId = cursoId,
                                                estado = EstadoPregunta.PENDIENTE_REVISION
                                            )
                                        }
                                    )
                                },
                                onEditar = { preguntaEditada ->
                                    viewModel.actualizarPregunta(
                                        id = preguntaEditada.id ?: "",
                                        pregunta = preguntaEditada,
                                        onSuccess = {
                                            viewModel.cargarPreguntas(
                                                cursoId = cursoId,
                                                estado = EstadoPregunta.PENDIENTE_REVISION
                                            )
                                        }
                                    )
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
    pregunta: Pregunta,
    onAprobar: () -> Unit,
    onRechazar: (String) -> Unit,
    onEditar: (Pregunta) -> Unit
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

            // Texto de la pregunta
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

            // ✅ MOSTRAR EXPLICACIÓN (OBLIGATORIA)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (!pregunta.explicacionCorrecta.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = EduRachaColors.Primary
                            )
                            Text(
                                "Explicación de la respuesta *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Primary
                            )
                        }
                        Text(
                            text = pregunta.explicacionCorrecta,
                            fontSize = 14.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Error.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        EduRachaColors.Error
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = EduRachaColors.Error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "⚠️ Explicación obligatoria faltante",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Error
                            )
                            Text(
                                "Debes agregar una explicación antes de aprobar",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Sección Expandible (Metadatos)
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(16.dp))

                Text(
                    "Información adicional",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))

                InfoRow(
                    Icons.Default.AutoAwesome,
                    "Generado por",
                    pregunta.metadatosIA?.generadoPor ?: "No especificado"
                )
                Spacer(Modifier.height(8.dp))
                val fuente = pregunta.fuente
                if(fuente.isNotBlank()) {
                    InfoRow(Icons.Default.Bookmark, "Fuente", fuente)
                    Spacer(Modifier.height(8.dp))
                }
                InfoRow(Icons.Default.Person, "Creado por", pregunta.creadoPor)
                Spacer(Modifier.height(8.dp))
                InfoRow(Icons.Default.CalendarMonth, "Fecha", pregunta.fechaCreacion)
            }

            // Botones de Acción
            Spacer(Modifier.height(20.dp))
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
                    enabled = !pregunta.explicacionCorrecta.isNullOrBlank(),
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

    // ✅ DIÁLOGO DE EDICIÓN ACTUALIZADO CON EXPLICACIÓN
    if (showEditDialog) {
        var textoEditado by remember { mutableStateOf(pregunta.texto) }
        var explicacionEditada by remember { mutableStateOf(pregunta.explicacionCorrecta ?: "") }
        val opcionesOriginales = pregunta.opciones
        var opcionesEditadas by remember {
            mutableStateOf(opcionesOriginales.map { it.texto })
        }
        var respuestaCorrectaIndex by remember {
            mutableStateOf(opcionesOriginales.indexOfFirst { it.esCorrecta })
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Editar pregunta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Texto de la pregunta
                    Text(
                        "Pregunta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    OutlinedTextField(
                        value = textoEditado,
                        onValueChange = { textoEditado = it },
                        label = { Text("Texto de la pregunta") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider()

                    // Opciones
                    Text(
                        "Opciones de respuesta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    opcionesEditadas.forEachIndexed { index, textoOpcion ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (respuestaCorrectaIndex == index)
                                    EduRachaColors.Success.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = respuestaCorrectaIndex == index,
                                    onClick = { respuestaCorrectaIndex = index },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EduRachaColors.Success
                                    )
                                )
                                OutlinedTextField(
                                    value = textoOpcion,
                                    onValueChange = {
                                        opcionesEditadas = opcionesEditadas.toMutableList()
                                            .also { list -> list[index] = it }
                                    },
                                    label = { Text("Opción ${'A' + index}") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // ✅ CAMPO PARA EDITAR EXPLICACIÓN
                    Text(
                        "Explicación (Obligatoria) *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    OutlinedTextField(
                        value = explicacionEditada,
                        onValueChange = { explicacionEditada = it },
                        label = { Text("¿Por qué esta es la respuesta correcta?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        isError = explicacionEditada.isBlank(),
                        supportingText = {
                            if (explicacionEditada.isBlank()) {
                                Text(
                                    "La explicación es obligatoria",
                                    color = EduRachaColors.Error
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = if (explicacionEditada.isBlank())
                                    EduRachaColors.Error
                                else
                                    EduRachaColors.Primary
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val preguntaEditada = pregunta.copy(
                            texto = textoEditado,
                            opciones = opcionesEditadas.mapIndexed { index, textoOpcion ->
                                Opcion(
                                    id = opcionesOriginales.getOrNull(index)?.id ?: index,
                                    texto = textoOpcion,
                                    esCorrecta = index == respuestaCorrectaIndex
                                )
                            },
                            explicacionCorrecta = explicacionEditada.takeIf { it.isNotBlank() },
                            modificada = true
                        )
                        onEditar(preguntaEditada)
                        showEditDialog = false
                    },
                    enabled = explicacionEditada.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    )
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo de Rechazo
    if (showRejectDialog) {
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Text(
                    "Rechazar pregunta",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Indica el motivo por el cual rechazas esta pregunta:",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo del rechazo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Comment,
                                contentDescription = null,
                                tint = EduRachaColors.Error
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivo.isNotBlank()) {
                            onRechazar(motivo)
                            showRejectDialog = false
                        }
                    },
                    enabled = motivo.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    )
                ) {
                    Icon(Icons.Default.ThumbDown, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
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
        if (esCorrecta) {
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.CheckCircle,
                "Correcta",
                modifier = Modifier.size(20.dp),
                tint = EduRachaColors.Success
            )
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            null,
            Modifier.size(16.dp),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$label:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}