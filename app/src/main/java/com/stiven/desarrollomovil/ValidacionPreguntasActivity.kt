package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.*
import kotlinx.coroutines.launch

class ValidacionPreguntasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val asignatura = intent.getStringExtra("ASIGNATURA") ?: ""

        setContent {
            EduRachaTheme {
                ValidacionPreguntasScreen(
                    asignatura = asignatura,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// ============================================
// PANTALLA PRINCIPAL
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionPreguntasScreen(
    asignatura: String,
    onNavigateBack: () -> Unit
) {
    var preguntas by remember {
        mutableStateOf(PreguntasIARepository.obtenerPreguntasPendientes(asignatura))
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var preguntaAEditar by remember { mutableStateOf<PreguntaIA?>(null) }

    // Estados para manejar el Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        // --- CAMBIO: Se usa el Snackbar nativo de Material 3 ---
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ValidacionTopAppBar(
                asignatura = asignatura,
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
                    icon = Icons.Default.Check,
                    title = "¡Todo listo!",
                    description = "No hay preguntas pendientes de validación en $asignatura",
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
                    items(preguntas, key = { it.id }) { pregunta ->
                        PreguntaValidacionCard(
                            pregunta = pregunta,
                            onAprobar = {
                                PreguntasIARepository.aprobarPregunta(pregunta.id)
                                preguntas = PreguntasIARepository.obtenerPreguntasPendientes(asignatura)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Pregunta aprobada correctamente")
                                }
                            },
                            onRechazar = {
                                PreguntasIARepository.rechazarPregunta(pregunta.id)
                                preguntas = PreguntasIARepository.obtenerPreguntasPendientes(asignatura)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Pregunta rechazada")
                                }
                            },
                            onEditar = {
                                preguntaAEditar = pregunta
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
    if (showEditDialog && preguntaAEditar != null) {
        EditarPreguntaDialog(
            pregunta = preguntaAEditar!!,
            onDismiss = {
                showEditDialog = false
                preguntaAEditar = null
            },
            onConfirm = { preguntaEditada ->
                PreguntasIARepository.actualizarPregunta(preguntaEditada)
                preguntas = PreguntasIARepository.obtenerPreguntasPendientes(asignatura)
                showEditDialog = false
                preguntaAEditar = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Pregunta editada y guardada")
                }
            }
        )
    }
}

// ============================================
// TOP APP BAR
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTopAppBar(
    asignatura: String,
    pendingCount: Int,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium),
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
                        Text(
                            text = asignatura,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
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
                                horizontal = Spacing.medium,
                                vertical = Spacing.small
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
// CARD DE PREGUNTA
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
            // Header con ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EduRachaColors.PrimaryContainer,
                    shape = CustomShapes.Badge
                ) {
                    Text(
                        text = "ID: ${pregunta.id}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.padding(
                            horizontal = Spacing.small,
                            vertical = Spacing.extraSmall
                        )
                    )
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
                text = pregunta.pregunta,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Opciones
            pregunta.opciones.forEachIndexed { index, opcion ->
                OpcionRespuesta(
                    letra = ('A' + index).toString(),
                    texto = opcion,
                    esCorrecta = index == pregunta.respuestaCorrecta,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
            }

            // Explicación expandible
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    EduRachaDivider()
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EduRachaColors.Info,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Column {
                            Text(
                                text = "Explicación",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Info
                            )
                            Spacer(modifier = Modifier.height(Spacing.extraSmall))
                            Text(
                                text = pregunta.explicacion.ifEmpty { "Sin explicación" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
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
                    backgroundColor = EduRachaColors.Secondary,
                    modifier = Modifier.weight(1f)
                )

                EduRachaSmallButton(
                    text = "Rechazar",
                    onClick = onRechazar,
                    icon = Icons.Default.Close,
                    backgroundColor = EduRachaColors.Error,
                    modifier = Modifier.weight(1f)
                )

                EduRachaSmallButton(
                    text = "Aprobar",
                    onClick = onAprobar,
                    icon = Icons.Default.Check,
                    backgroundColor = EduRachaColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================
// OPCIÓN DE RESPUESTA
// ============================================
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
// DIÁLOGO DE EDICIÓN
// ============================================
@Composable
fun EditarPreguntaDialog(
    pregunta: PreguntaIA,
    onDismiss: () -> Unit,
    onConfirm: (PreguntaIA) -> Unit
) {
    var textoPregunta by remember { mutableStateOf(pregunta.pregunta) }
    var opciones by remember { mutableStateOf(pregunta.opciones.toMutableList()) }
    var respuestaCorrecta by remember { mutableStateOf(pregunta.respuestaCorrecta) }
    var explicacion by remember { mutableStateOf(pregunta.explicacion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Editar Pregunta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
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

                items(opciones.size) { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        RadioButton(
                            selected = respuestaCorrecta == index,
                            onClick = { respuestaCorrecta = index },
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
                        value = explicacion,
                        onValueChange = { explicacion = it },
                        label = "Explicación (opcional)",
                        placeholder = "Agrega una explicación...",
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            EduRachaPrimaryButton(
                text = "Guardar",
                onClick = {
                    val preguntaEditada = pregunta.copy(
                        pregunta = textoPregunta,
                        opciones = opciones,
                        respuestaCorrecta = respuestaCorrecta,
                        explicacion = explicacion
                    )
                    onConfirm(preguntaEditada)
                },
                enabled = textoPregunta.isNotBlank() && opciones.all { it.isNotBlank() },
                icon = Icons.Default.Save // Se mantiene el ícono para resolver la ambigüedad si es necesario
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
