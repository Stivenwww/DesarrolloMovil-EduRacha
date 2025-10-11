package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.*
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.models.Curso
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class ValidacionPreguntasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: ""

        setContent {
            EduRachaTheme {
                ValidacionPreguntasScreen(
                    cursoTitulo = cursoTitulo,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionPreguntasScreen(
    cursoTitulo: String,
    onNavigateBack: () -> Unit
) {
    // Buscar el curso por título
    val curso = remember(cursoTitulo) {
        CrearCursoObject.cursosGuardados.find { it.titulo == cursoTitulo }
    }

    var preguntas by remember {
        mutableStateOf(PreguntasIARepository.obtenerPreguntasPendientes(cursoTitulo))
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var preguntaSeleccionada by remember { mutableStateOf<PreguntaIA?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val nombreRevisor = currentUser?.displayName ?: "Docente"

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ValidacionTopAppBar(
                cursoTitulo = cursoTitulo,
                cursoCodigo = curso?.codigo ?: "",
                pendingCount = preguntas.size,
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
            if (preguntas.isEmpty()) {
                EduRachaEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "¡Todo validado!",
                    description = "No hay preguntas pendientes en $cursoTitulo",
                    actionText = "Volver",
                    onActionClick = onNavigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Header con información del curso
                    item {
                        CursoInfoCard(curso = curso)
                    }

                    items(preguntas, key = { it.id }) { pregunta ->
                        PreguntaValidacionCard(
                            pregunta = pregunta,
                            onAprobar = {
                                PreguntasIARepository.aprobarPregunta(
                                    preguntaId = pregunta.id,
                                    revisadoPor = nombreRevisor,
                                    notas = "Pregunta aprobada sin modificaciones"
                                )
                                preguntas = preguntas.filter { it.id != pregunta.id }
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("✓ Pregunta aprobada")
                                }
                            },
                            onRechazar = {
                                preguntaSeleccionada = pregunta
                                showDeleteDialog = true
                            },
                            onEditar = {
                                preguntaSeleccionada = pregunta
                                showEditDialog = true
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Spacing.large))
                    }
                }
            }
        }
    }

    // Diálogo de edición
    if (showEditDialog && preguntaSeleccionada != null) {
        EditarPreguntaDialog(
            pregunta = preguntaSeleccionada!!,
            nombreRevisor = nombreRevisor,
            onDismiss = {
                showEditDialog = false
                preguntaSeleccionada = null
            },
            onConfirm = { preguntaEditada, notas ->
                PreguntasIARepository.actualizarPregunta(
                    preguntaEditada = preguntaEditada,
                    revisadoPor = nombreRevisor,
                    notas = notas
                )
                preguntas = preguntas.filter { it.id != preguntaEditada.id }
                showEditDialog = false
                preguntaSeleccionada = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ Pregunta editada y guardada")
                }
            }
        )
    }

    // Diálogo de confirmación de rechazo
    if (showDeleteDialog && preguntaSeleccionada != null) {
        ConfirmarRechazoDialog(
            pregunta = preguntaSeleccionada!!,
            onDismiss = {
                showDeleteDialog = false
                preguntaSeleccionada = null
            },
            onConfirm = { motivo ->
                PreguntasIARepository.rechazarPregunta(
                    preguntaId = preguntaSeleccionada!!.id,
                    revisadoPor = nombreRevisor,
                    motivo = motivo
                )
                preguntas = preguntas.filter { it.id != preguntaSeleccionada!!.id }
                showDeleteDialog = false
                preguntaSeleccionada = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ Pregunta rechazada")
                }
            }
        )
    }
}

// ============================================
// CARD DE INFORMACIÓN DEL CURSO
// ============================================
@Composable
fun CursoInfoCard(curso: Curso?) {
    if (curso == null) return

    EduRachaCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(EduRachaColors.PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = curso.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = curso.codigo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.Primary
                    )
                    Text(
                        text = "•",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                    Text(
                        text = "${curso.duracionDias} días",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            // Badge de estado
            Surface(
                color = when (curso.estado) {
                    "activo" -> EduRachaColors.Success
                    "borrador" -> EduRachaColors.Warning
                    "inactivo" -> EduRachaColors.TextSecondary
                    else -> EduRachaColors.Primary
                }.copy(alpha = 0.15f),
                shape = CustomShapes.Badge
            ) {
                Text(
                    text = curso.estado.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (curso.estado) {
                        "activo" -> EduRachaColors.Success
                        "borrador" -> EduRachaColors.Warning
                        "inactivo" -> EduRachaColors.TextSecondary
                        else -> EduRachaColors.Primary
                    },
                    modifier = Modifier.padding(
                        horizontal = Spacing.medium,
                        vertical = Spacing.small
                    )
                )
            }
        }
    }
}

// ============================================
// TOP APP BAR
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTopAppBar(
    cursoTitulo: String,
    cursoCodigo: String,
    pendingCount: Int,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        color = EduRachaColors.Primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            EduRachaColors.GradientStart,
                            EduRachaColors.GradientEnd
                        )
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Validar Preguntas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cursoTitulo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            if (cursoCodigo.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = cursoCodigo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        color = EduRachaColors.Secondary,
                        shape = CustomShapes.Badge
                    ) {
                        Text(
                            text = "$pendingCount pendientes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

// ============================================
// CARD DE PREGUNTA CON INFORMACIÓN COMPLETA
// ============================================
@Composable
fun PreguntaValidacionCard(
    pregunta: PreguntaIA,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium, shape = CustomShapes.Card)
            .animateContentSize(),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding)
        ) {
            // Header con metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ID Badge
                    Surface(
                        color = EduRachaColors.PrimaryContainer,
                        shape = CustomShapes.Badge
                    ) {
                        Text(
                            text = "ID: ${pregunta.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Dificultad Badge
                    Surface(
                        color = when (pregunta.dificultad) {
                            DificultadPregunta.FACIL -> EduRachaColors.Success.copy(alpha = 0.15f)
                            DificultadPregunta.MEDIA -> EduRachaColors.Warning.copy(alpha = 0.15f)
                            DificultadPregunta.DIFICIL -> EduRachaColors.Error.copy(alpha = 0.15f)
                        },
                        shape = CustomShapes.Badge
                    ) {
                        Text(
                            text = when (pregunta.dificultad) {
                                DificultadPregunta.FACIL -> "Fácil"
                                DificultadPregunta.MEDIA -> "Media"
                                DificultadPregunta.DIFICIL -> "Difícil"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (pregunta.dificultad) {
                                DificultadPregunta.FACIL -> EduRachaColors.Success
                                DificultadPregunta.MEDIA -> EduRachaColors.Warning
                                DificultadPregunta.DIFICIL -> EduRachaColors.Error
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Pregunta
            Text(
                text = pregunta.texto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            // Fuente/Tema
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = EduRachaColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = pregunta.fuente,
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Opciones
            pregunta.opciones.forEachIndexed { index, opcion ->
                OpcionRespuesta(
                    letra = ('A' + index).toString(),
                    texto = opcion.texto,
                    esCorrecta = opcion.esCorrecta,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
            }

            // Información expandible
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    EduRachaDivider()
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Generado por IA
                    InfoRow(
                        icon = Icons.Default.AutoAwesome,
                        label = "Generado por",
                        value = pregunta.metadatos.generadoPor,
                        color = EduRachaColors.Info
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    // Fecha de creación
                    InfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Fecha de creación",
                        value = formatDate(pregunta.fechaCreacion),
                        color = EduRachaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    // Creado por
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Creado por",
                        value = pregunta.creadoPor,
                        color = EduRachaColors.TextSecondary
                    )

                    // Historial de revisiones
                    if (pregunta.fueRevisada) {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                        EduRachaDivider()
                        Spacer(modifier = Modifier.height(Spacing.medium))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Column {
                                Text(
                                    text = "Historial de Revisiones",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Success
                                )
                                Spacer(modifier = Modifier.height(Spacing.extraSmall))

                                pregunta.historialRevisiones.forEach { revision ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        color = EduRachaColors.SuccessContainer,
                                        shape = CustomShapes.CardSmall
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(Spacing.medium)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Revisado por: ${revision.revisadoPor}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EduRachaColors.Success
                                                )
                                                if (revision.modificada) {
                                                    Surface(
                                                        color = EduRachaColors.Warning.copy(alpha = 0.2f),
                                                        shape = CustomShapes.Badge
                                                    ) {
                                                        Text(
                                                            text = "EDITADA",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = EduRachaColors.Warning,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = formatDate(revision.fechaRevision),
                                                fontSize = 11.sp,
                                                color = EduRachaColors.TextSecondary
                                            )
                                            if (revision.notasRevision.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = revision.notasRevision,
                                                    fontSize = 12.sp,
                                                    color = EduRachaColors.TextPrimary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lote ID
                    Spacer(modifier = Modifier.height(Spacing.small))
                    InfoRow(
                        icon = Icons.Default.Inventory,
                        label = "Lote ID",
                        value = pregunta.metadatos.lotId,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))
            EduRachaDivider()
            Spacer(modifier = Modifier.height(Spacing.medium))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                EduRachaSmallButton(
                    text = "Editar",
                    onClick = onEditar,
                    icon = Icons.Default.Edit,
                    backgroundColor = EduRachaColors.Info,
                    modifier = Modifier.weight(1f)
                )

                EduRachaSmallButton(
                    text = "Rechazar",
                    onClick = onRechazar,
                    icon = Icons.Default.Delete,
                    backgroundColor = EduRachaColors.Error,
                    modifier = Modifier.weight(1f)
                )

                EduRachaSmallButton(
                    text = "Aprobar",
                    onClick = onAprobar,
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = EduRachaColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================
// COMPONENTES AUXILIARES
// ============================================

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = EduRachaColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = EduRachaColors.TextPrimary
        )
    }
}

@Composable
fun OpcionRespuesta(
    letra: String,
    texto: String,
    esCorrecta: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (esCorrecta)
        EduRachaColors.SuccessContainer
    else
        EduRachaColors.SurfaceVariant

    val borderColor = if (esCorrecta)
        EduRachaColors.Success
    else
        Color.Transparent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (esCorrecta) BorderWidth.medium else BorderWidth.none,
                color = borderColor,
                shape = CustomShapes.CardSmall
            ),
        shape = CustomShapes.CardSmall,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (esCorrecta) EduRachaColors.Success else EduRachaColors.Primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letra,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Text(
                text = texto,
                fontSize = 14.sp,
                fontWeight = if (esCorrecta) FontWeight.SemiBold else FontWeight.Normal,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            if (esCorrecta) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Respuesta correcta",
                    tint = EduRachaColors.Success,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================
// DIÁLOGOS
// ============================================

@Composable
fun EditarPreguntaDialog(
    pregunta: PreguntaIA,
    nombreRevisor: String,
    onDismiss: () -> Unit,
    onConfirm: (PreguntaIA, String) -> Unit
) {
    var textoPregunta by remember { mutableStateOf(pregunta.texto) }
    var opciones by remember {
        mutableStateOf(pregunta.opciones.map { it.texto }.toMutableList())
    }
    var respuestaCorrectaIndex by remember {
        mutableStateOf(pregunta.opciones.indexOfFirst { it.esCorrecta })
    }
    var notasRevision by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = EduRachaColors.Info,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Column {
                    Text(
                        text = "Editar Pregunta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${pregunta.id}",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                item {
                    EduRachaTextArea(
                        value = textoPregunta,
                        onValueChange = { textoPregunta = it },
                        label = "Pregunta",
                        placeholder = "Escribe la pregunta...",
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item {
                    Text(
                        text = "Opciones (selecciona la correcta)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                }

                items(opciones.size) { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        RadioButton(
                            selected = respuestaCorrectaIndex == index,
                            onClick = { respuestaCorrectaIndex = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = EduRachaColors.Success
                            )
                        )

                        EduRachaTextField(
                            value = opciones[index],
                            onValueChange = { opciones[index] = it },
                            label = "Opción ${('A' + index)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    EduRachaTextArea(
                        value = notasRevision,
                        onValueChange = { notasRevision = it },
                        label = "Notas de revisión",
                        placeholder = "Describe los cambios realizados...",
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            EduRachaPrimaryButton(
                text = "Guardar Cambios",
                onClick = {
                    val preguntaEditada = pregunta.copy(
                        texto = textoPregunta,
                        opciones = opciones.mapIndexed { index, texto ->
                            OpcionRespuesta(texto, index == respuestaCorrectaIndex)
                        }
                    )
                    onConfirm(
                        preguntaEditada,
                        notasRevision.ifEmpty { "Pregunta editada por $nombreRevisor" }
                    )
                },
                enabled = textoPregunta.isNotBlank() &&
                        opciones.all { it.isNotBlank() } &&
                        respuestaCorrectaIndex >= 0,
                icon = Icons.Default.Save
            )
        },
        dismissButton = {
            EduRachaTextButton(
                text = "Cancelar",
                onClick = onDismiss
            )
        },
        shape = CustomShapes.Dialog,
        containerColor = EduRachaColors.Surface
    )
}

@Composable
fun ConfirmarRechazoDialog(
    pregunta: PreguntaIA,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = EduRachaColors.Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "¿Rechazar pregunta?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Esta acción no se puede deshacer. La pregunta será marcada como rechazada.",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                Surface(
                    color = EduRachaColors.ErrorContainer,
                    shape = CustomShapes.CardSmall
                ) {
                    Column(modifier = Modifier.padding(Spacing.medium)) {
                        Text(
                            text = "Pregunta a rechazar:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pregunta.texto,
                            fontSize = 13.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                EduRachaTextArea(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = "Motivo del rechazo (requerido)",
                    placeholder = "Explica por qué rechazas esta pregunta...",
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(motivo) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                enabled = motivo.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Rechazar")
            }
        },
        dismissButton = {
            EduRachaTextButton(
                text = "Cancelar",
                onClick = onDismiss
            )
        },
        shape = CustomShapes.Dialog,
        containerColor = EduRachaColors.Surface
    )
}

// ============================================
// FUNCIÓN AUXILIAR PARA FORMATEAR FECHAS
// ============================================
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
    return sdf.format(Date(timestamp))
}
