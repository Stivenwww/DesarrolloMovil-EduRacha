package com.stiven.sos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import com.stiven.sos.viewmodel.TemaViewModel
import java.time.Instant
import kotlin.math.min

class CrearPreguntaActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val temaViewModel: TemaViewModel by viewModels()
    private val preguntaViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoViewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                CrearPreguntaScreen(
                    cursoViewModel = cursoViewModel,
                    temaViewModel = temaViewModel,
                    preguntaViewModel = preguntaViewModel,
                    onBack = { finish() },
                    onSuccess = {
                        Toast.makeText(this, "✅ Pregunta creada exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
        }
    }
}

enum class ModoCreacion {
    MANUAL, IA
}

data class OpcionEditable(
    val id: Int,
    val texto: String,
    val esCorrecta: Boolean
)

// Clase para dimensiones responsivas
data class ResponsiveDimensions(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val cardPadding: Dp,
    val headerTopPadding: Dp,
    val headerBottomPadding: Dp,
    val titleFontSize: Int,
    val subtitleFontSize: Int,
    val bodyFontSize: Int,
    val buttonHeight: Dp,
    val iconSize: Dp,
    val smallIconSize: Dp,
    val spacing: Dp,
    val cardElevation: Dp,
    val borderRadius: Dp
) {
    companion object {
        @Composable
        fun get(): ResponsiveDimensions {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp

            // Detectar tipo de dispositivo
            val isSmallDevice = screenWidth < 360.dp
            val isMediumDevice = screenWidth in 360.dp..600.dp
            val isLargeDevice = screenWidth > 600.dp

            return when {
                isSmallDevice -> ResponsiveDimensions(
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    horizontalPadding = 12.dp,
                    verticalPadding = 12.dp,
                    cardPadding = 12.dp,
                    headerTopPadding = 32.dp,
                    headerBottomPadding = 16.dp,
                    titleFontSize = 22,
                    subtitleFontSize = 13,
                    bodyFontSize = 13,
                    buttonHeight = 48.dp,
                    iconSize = 20.dp,
                    smallIconSize = 16.dp,
                    spacing = 12.dp,
                    cardElevation = 2.dp,
                    borderRadius = 12.dp
                )
                isMediumDevice -> ResponsiveDimensions(
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    horizontalPadding = 16.dp,
                    verticalPadding = 16.dp,
                    cardPadding = 16.dp,
                    headerTopPadding = 40.dp,
                    headerBottomPadding = 20.dp,
                    titleFontSize = 26,
                    subtitleFontSize = 14,
                    bodyFontSize = 14,
                    buttonHeight = 56.dp,
                    iconSize = 24.dp,
                    smallIconSize = 18.dp,
                    spacing = 16.dp,
                    cardElevation = 3.dp,
                    borderRadius = 16.dp
                )
                else -> ResponsiveDimensions(
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    horizontalPadding = 24.dp,
                    verticalPadding = 20.dp,
                    cardPadding = 20.dp,
                    headerTopPadding = 48.dp,
                    headerBottomPadding = 24.dp,
                    titleFontSize = 28,
                    subtitleFontSize = 15,
                    bodyFontSize = 15,
                    buttonHeight = 60.dp,
                    iconSize = 28.dp,
                    smallIconSize = 20.dp,
                    spacing = 20.dp,
                    cardElevation = 4.dp,
                    borderRadius = 20.dp
                )
            }
        }
    }
}

@Composable
fun CrearPreguntaScreen(
    cursoViewModel: CursoViewModel,
    temaViewModel: TemaViewModel,
    preguntaViewModel: PreguntaViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val dims = ResponsiveDimensions.get()

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val temaUiState by temaViewModel.uiState.collectAsState()
    val preguntaUiState by preguntaViewModel.uiState.collectAsState()

    // Estados de navegación
    var modoCreacion by remember { mutableStateOf<ModoCreacion?>(null) }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var temaSeleccionado by remember { mutableStateOf<Tema?>(null) }

    // Estados para creación manual
    var textoPregunta by remember { mutableStateOf("") }
    var dificultadSeleccionada by remember { mutableStateOf(DificultadPregunta.MEDIO) }
    var opciones by remember { mutableStateOf(listOf(
        OpcionEditable(0, "", false),
        OpcionEditable(1, "", false),
        OpcionEditable(2, "", false)
    )) }
    var explicacionCorrecta by remember { mutableStateOf("") }

    // Estados de validación
    var preguntaError by remember { mutableStateOf(false) }

    // Estados para generación con IA
    var cantidadPreguntas by remember { mutableStateOf(5) }

    // Diálogos
    var showCursoDialog by remember { mutableStateOf(false) }
    var showTemaDialog by remember { mutableStateOf(false) }

    // Cargar temas cuando se selecciona un curso
    LaunchedEffect(cursoSeleccionado) {
        cursoSeleccionado?.let { curso ->
            curso.id?.let { cursoId ->
                temaViewModel.cargarTemasPorCurso(cursoId)
            }
        }
    }

    // Observar éxito
    LaunchedEffect(preguntaUiState.successMessage) {
        preguntaUiState.successMessage?.let {
            onSuccess()
        }
    }

    // Animación
    val infiniteTransition = rememberInfiniteTransition(label = "questionAnimation")
    val questionRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "questionRotation"
    )

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFF8B5CF6).copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = dims.headerTopPadding,
                            bottom = dims.headerBottomPadding,
                            start = dims.horizontalPadding,
                            end = dims.horizontalPadding
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(dims.iconSize + 16.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(dims.iconSize)
                            )
                        }

                        // Badge de progreso
                        if (modoCreacion != null) {
                            Surface(
                                shape = RoundedCornerShape(dims.borderRadius),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = dims.cardPadding * 0.75f,
                                        vertical = dims.cardPadding * 0.4f
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.4f)
                                ) {
                                    Icon(
                                        if (modoCreacion == ModoCreacion.MANUAL)
                                            Icons.Outlined.Edit
                                        else
                                            Icons.Outlined.AutoAwesome,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(dims.smallIconSize)
                                    )
                                    Text(
                                        if (modoCreacion == ModoCreacion.MANUAL) "Manual" else "IA",
                                        color = Color.White,
                                        fontSize = dims.bodyFontSize.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(dims.spacing))

                    // Vista previa
                    AnimatedVisibility(
                        visible = textoPregunta.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = dims.spacing * 0.75f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.75f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(dims.iconSize + 8.dp)
                                    .rotate(questionRotation)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Vista Previa",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = (dims.bodyFontSize - 2).sp
                                )
                                Text(
                                    textoPregunta,
                                    color = Color.White,
                                    fontSize = (dims.subtitleFontSize + 2).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    if (textoPregunta.isEmpty()) {
                        Text(
                            "Crear Pregunta",
                            color = Color.White,
                            fontSize = dims.titleFontSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Elige cómo deseas crear la pregunta",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = dims.subtitleFontSize.sp
                        )
                    }
                }
            }

            // CONTENIDO
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = dims.horizontalPadding,
                        vertical = dims.verticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(dims.spacing)
            ) {
                // PASO 1: Seleccionar modo
                if (modoCreacion == null) {
                    ModoCreacionCards(dims, onModoSelected = { modoCreacion = it })
                } else {
                    // Modo seleccionado
                    ModoSeleccionadoHeader(
                        dims,
                        modo = modoCreacion!!,
                        onCambiar = {
                            modoCreacion = null
                            cursoSeleccionado = null
                            temaSeleccionado = null
                            textoPregunta = ""
                            opciones = listOf(
                                OpcionEditable(0, "", false),
                                OpcionEditable(1, "", false),
                                OpcionEditable(2, "", false)
                            )
                        }
                    )

                    // PASO 2: Selección de curso y tema
                    SelectorsSection(
                        dims,
                        cursoSeleccionado = cursoSeleccionado,
                        temaSeleccionado = temaSeleccionado,
                        isLoadingTemas = temaUiState.isLoading,
                        onCursoClick = { showCursoDialog = true },
                        onTemaClick = { showTemaDialog = true }
                    )

                    // PASO 3: Formulario
                    if (cursoSeleccionado != null && temaSeleccionado != null) {
                        when (modoCreacion) {
                            ModoCreacion.MANUAL -> {
                                FormularioManualSection(
                                    dims,
                                    textoPregunta = textoPregunta,
                                    onTextoPreguntaChange = { textoPregunta = it; preguntaError = false },
                                    preguntaError = preguntaError,
                                    dificultad = dificultadSeleccionada,
                                    onDificultadChange = { dificultadSeleccionada = it },
                                    opciones = opciones,
                                    onOpcionesChange = { opciones = it },
                                    explicacionCorrecta = explicacionCorrecta,
                                    onExplicacionChange = { explicacionCorrecta = it },
                                    onCrear = {
                                        preguntaError = textoPregunta.isBlank()

                                        if (!preguntaError &&
                                            opciones.any { it.esCorrecta } &&
                                            opciones.all { it.texto.isNotEmpty() } &&
                                            explicacionCorrecta.isNotBlank()) {
                                            crearPreguntaManual(
                                                context = context,
                                                preguntaViewModel = preguntaViewModel,
                                                curso = cursoSeleccionado!!,
                                                tema = temaSeleccionado!!,
                                                textoPregunta = textoPregunta,
                                                opciones = opciones,
                                                dificultad = dificultadSeleccionada,
                                                explicacion = explicacionCorrecta
                                            )
                                        } else {
                                            val mensaje = when {
                                                preguntaError -> "⚠️ El texto de la pregunta es obligatorio"
                                                !opciones.any { it.esCorrecta } -> "⚠️ Debes marcar una respuesta correcta"
                                                !opciones.all { it.texto.isNotEmpty() } -> "⚠️ Todas las opciones deben tener texto"
                                                explicacionCorrecta.isBlank() -> "⚠️ La explicación de la respuesta es obligatoria"
                                                else -> "⚠️ Completa todos los campos obligatorios"
                                            }
                                            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    isLoading = preguntaUiState.isLoading
                                )
                            }
                            ModoCreacion.IA -> {
                                FormularioIASection(
                                    dims,
                                    cantidad = cantidadPreguntas,
                                    onCantidadChange = { cantidadPreguntas = it },
                                    onGenerar = {
                                        generarPreguntasIA(
                                            context = context,
                                            preguntaViewModel = preguntaViewModel,
                                            curso = cursoSeleccionado!!,
                                            tema = temaSeleccionado!!,
                                            cantidad = cantidadPreguntas
                                        )
                                    },
                                    isLoading = preguntaUiState.isLoading
                                )
                            }
                            null -> {}
                        }
                    }

                    // Mensaje de error
                    preguntaUiState.error?.let { error ->
                        ErrorMessage(dims, error)
                    }
                }

                Spacer(Modifier.height(dims.spacing))
            }
        }

        // Loading overlay
        if (preguntaUiState.isLoading) {
            LoadingOverlay(dims, modo = modoCreacion)
        }
    }

    // Diálogos
    if (showCursoDialog) {
        SelectorCursoDialogSimple(
            dims,
            cursos = cursoUiState.cursos,
            onCursoSelected = {
                cursoSeleccionado = it
                temaSeleccionado = null
                showCursoDialog = false
            },
            onDismiss = { showCursoDialog = false }
        )
    }

    if (showTemaDialog && cursoSeleccionado != null) {
        SelectorTemaDialogSimple(
            dims,
            temas = temaUiState.temas,
            onTemaSelected = {
                temaSeleccionado = it
                showTemaDialog = false
            },
            onDismiss = { showTemaDialog = false }
        )
    }
}

// ================== COMPONENTES RESPONSIVOS ==================

@Composable
fun ModoCreacionCards(dims: ResponsiveDimensions, onModoSelected: (ModoCreacion) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing)) {
        SectionHeaderSimple(dims, "SELECCIONA EL MÉTODO", Icons.Default.TouchApp)

        ModoCard(
            dims,
            title = "Creación Manual",
            description = "Escribe tu pregunta y opciones personalizadas",
            icon = Icons.Outlined.Edit,
            color = EduRachaColors.Primary,
            onClick = { onModoSelected(ModoCreacion.MANUAL) }
        )

        ModoCard(
            dims,
            title = "Generar con IA",
            description = "La IA generará preguntas automáticamente",
            icon = Icons.Outlined.AutoAwesome,
            color = EduRachaColors.Accent,
            onClick = { onModoSelected(ModoCreacion.IA) }
        )
    }
}

@Composable
fun ModoCard(
    dims: ResponsiveDimensions,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dims.borderRadius),
        elevation = CardDefaults.cardElevation(dims.cardElevation),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(dims.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(dims.spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dims.iconSize * 2.5f)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(dims.iconSize + 4.dp), tint = color)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = (dims.subtitleFontSize + 2).sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    description,
                    fontSize = dims.bodyFontSize.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = (dims.bodyFontSize + 4).sp
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                null,
                modifier = Modifier.size(dims.iconSize),
                tint = EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun ModoSeleccionadoHeader(dims: ResponsiveDimensions, modo: ModoCreacion, onCambiar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(dims.borderRadius * 0.75f),
        color = if (modo == ModoCreacion.MANUAL)
            EduRachaColors.Primary.copy(alpha = 0.1f)
        else
            EduRachaColors.Accent.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(dims.cardPadding * 0.75f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (modo == ModoCreacion.MANUAL) Icons.Outlined.Edit else Icons.Outlined.AutoAwesome,
                    null,
                    modifier = Modifier.size(dims.iconSize),
                    tint = if (modo == ModoCreacion.MANUAL) EduRachaColors.Primary else EduRachaColors.Accent
                )
                Text(
                    if (modo == ModoCreacion.MANUAL) "Creación Manual" else "Generación con IA",
                    fontSize = dims.bodyFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (modo == ModoCreacion.MANUAL) EduRachaColors.Primary else EduRachaColors.Accent
                )
            }
            TextButton(onClick = onCambiar) {
                Text("Cambiar", fontSize = dims.bodyFontSize.sp)
            }
        }
    }
}

@Composable
fun SelectorsSection(
    dims: ResponsiveDimensions,
    cursoSeleccionado: Curso?,
    temaSeleccionado: Tema?,
    isLoadingTemas: Boolean,
    onCursoClick: () -> Unit,
    onTemaClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.75f)) {
        SelectorCard(
            dims,
            title = "Curso",
            value = cursoSeleccionado?.titulo ?: "Seleccionar curso",
            icon = Icons.Outlined.MenuBook,
            isSelected = cursoSeleccionado != null,
            onClick = onCursoClick
        )

        if (cursoSeleccionado != null) {
            SelectorCard(
                dims,
                title = "Tema",
                value = if (isLoadingTemas) "Cargando..." else (temaSeleccionado?.titulo ?: "Seleccionar tema"),
                icon = Icons.Outlined.Topic,
                isSelected = temaSeleccionado != null,
                isLoading = isLoadingTemas,
                onClick = onTemaClick,
                enabled = !isLoadingTemas
            )
        }
    }
}

@Composable
fun SelectorCard(
    dims: ResponsiveDimensions,
    title: String,
    value: String,
    icon: ImageVector,
    isSelected: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(dims.borderRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                EduRachaColors.Success.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(dims.cardElevation)
    ) {
        Row(
            modifier = Modifier.padding(dims.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.75f),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dims.iconSize),
                        color = EduRachaColors.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(dims.iconSize),
                        tint = if (isSelected) EduRachaColors.Success else EduRachaColors.TextSecondary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = (dims.bodyFontSize - 2).sp,
                        color = EduRachaColors.TextSecondary
                    )
                    Text(
                        value,
                        fontSize = dims.subtitleFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) EduRachaColors.Success else EduRachaColors.TextPrimary,
                        maxLines = 2
                    )
                }
            }
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                null,
                modifier = Modifier.size(dims.iconSize),
                tint = if (isSelected) EduRachaColors.Success else EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun FormularioManualSection(
    dims: ResponsiveDimensions,
    textoPregunta: String,
    onTextoPreguntaChange: (String) -> Unit,
    preguntaError: Boolean,
    dificultad: String,
    onDificultadChange: (String) -> Unit,
    opciones: List<OpcionEditable>,
    onOpcionesChange: (List<OpcionEditable>) -> Unit,
    explicacionCorrecta: String,
    onExplicacionChange: (String) -> Unit,
    onCrear: () -> Unit,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing)) {
        SectionHeaderSimple(dims, "DETALLES DE LA PREGUNTA", Icons.Default.Edit)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dims.borderRadius),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(dims.cardElevation)
        ) {
            Column(
                modifier = Modifier.padding(dims.cardPadding),
                verticalArrangement = Arrangement.spacedBy(dims.spacing)
            ) {
                // Pregunta
                OutlinedTextField(
                    value = textoPregunta,
                    onValueChange = onTextoPreguntaChange,
                    label = { Text("Texto de la pregunta *", fontSize = dims.bodyFontSize.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = preguntaError,
                    shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (preguntaError) EduRachaColors.Error else EduRachaColors.Primary,
                        focusedLabelColor = if (preguntaError) EduRachaColors.Error else EduRachaColors.Primary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = dims.bodyFontSize.sp)
                )

                // Dificultad
                DificultadSelector(dims, dificultad = dificultad, onDificultadChange = onDificultadChange)
            }
        }

        // Opciones
        OpcionesSection(dims, opciones = opciones, onOpcionesChange = onOpcionesChange)

        // Explicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dims.borderRadius),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(dims.cardElevation)
        ) {
            Column(
                modifier = Modifier.padding(dims.cardPadding),
                verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Explicación de la respuesta *",
                        fontSize = dims.bodyFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    Surface(
                        shape = RoundedCornerShape(dims.borderRadius * 0.5f),
                        color = EduRachaColors.Error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "OBLIGATORIO",
                            fontSize = (dims.bodyFontSize - 3).sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Error,
                            modifier = Modifier.padding(
                                horizontal = dims.cardPadding * 0.4f,
                                vertical = dims.cardPadding * 0.2f
                            )
                        )
                    }
                }
                Text(
                    "Explica por qué esta es la respuesta correcta. Esta información ayudará a los estudiantes a comprender mejor el tema.",
                    fontSize = (dims.bodyFontSize - 1).sp,
                    color = EduRachaColors.TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = (dims.bodyFontSize + 3).sp
                )
                OutlinedTextField(
                    value = explicacionCorrecta,
                    onValueChange = onExplicacionChange,
                    label = { Text("¿Por qué esta es la respuesta correcta?", fontSize = dims.bodyFontSize.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = explicacionCorrecta.isBlank(),
                    shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(dims.iconSize),
                            tint = if (explicacionCorrecta.isBlank())
                                EduRachaColors.Error
                            else
                                EduRachaColors.Primary
                        )
                    },
                    supportingText = {
                        if (explicacionCorrecta.isBlank()) {
                            Text(
                                "La explicación es obligatoria",
                                fontSize = (dims.bodyFontSize - 2).sp,
                                color = EduRachaColors.Error
                            )
                        } else {
                            Text(
                                "${explicacionCorrecta.length} caracteres",
                                fontSize = (dims.bodyFontSize - 2).sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(fontSize = dims.bodyFontSize.sp)
                )
            }
        }

        // Botón
        Button(
            onClick = onCrear,
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.buttonHeight),
            enabled = !isLoading,
            shape = RoundedCornerShape(dims.borderRadius),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dims.iconSize),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(dims.spacing * 0.75f))
                Text("Creando...", fontSize = dims.subtitleFontSize.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(dims.iconSize))
                Spacer(Modifier.width(dims.spacing * 0.75f))
                Text("Crear Pregunta", fontSize = dims.subtitleFontSize.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DificultadSelector(dims: ResponsiveDimensions, dificultad: String, onDificultadChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)) {
        Text(
            "Dificultad",
            fontSize = dims.bodyFontSize.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = dificultad == DificultadPregunta.FACIL,
                onClick = { onDificultadChange(DificultadPregunta.FACIL) },
                label = { Text("Fácil", fontSize = dims.bodyFontSize.sp) },
                leadingIcon = if (dificultad == DificultadPregunta.FACIL) {
                    { Icon(Icons.Default.Check, null, Modifier.size(dims.smallIconSize)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = dificultad == DificultadPregunta.MEDIO,
                onClick = { onDificultadChange(DificultadPregunta.MEDIO) },
                label = { Text("Medio", fontSize = dims.bodyFontSize.sp) },
                leadingIcon = if (dificultad == DificultadPregunta.MEDIO) {
                    { Icon(Icons.Default.Check, null, Modifier.size(dims.smallIconSize)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = dificultad == DificultadPregunta.DIFICIL,
                onClick = { onDificultadChange(DificultadPregunta.DIFICIL) },
                label = { Text("Difícil", fontSize = dims.bodyFontSize.sp) },
                leadingIcon = if (dificultad == DificultadPregunta.DIFICIL) {
                    { Icon(Icons.Default.Check, null, Modifier.size(dims.smallIconSize)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OpcionesSection(
    dims: ResponsiveDimensions,
    opciones: List<OpcionEditable>,
    onOpcionesChange: (List<OpcionEditable>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.75f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeaderSimple(dims, "OPCIONES DE RESPUESTA", Icons.Default.ListAlt)
            if (opciones.size < 5) {
                IconButton(
                    onClick = {
                        onOpcionesChange(opciones + OpcionEditable(opciones.size, "", false))
                    },
                    modifier = Modifier
                        .size(dims.iconSize + 8.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Success.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Agregar opción",
                        modifier = Modifier.size(dims.iconSize),
                        tint = EduRachaColors.Success
                    )
                }
            }
        }

        opciones.forEachIndexed { index, opcion ->
            OpcionCard(
                dims,
                opcion = opcion,
                onTextoChange = { nuevo ->
                    onOpcionesChange(
                        opciones.mapIndexed { i, o ->
                            if (i == index) o.copy(texto = nuevo) else o
                        }
                    )
                },
                onCorrectaChange = { esCorrecta ->
                    onOpcionesChange(
                        opciones.mapIndexed { i, o ->
                            if (i == index) o.copy(esCorrecta = esCorrecta)
                            else o.copy(esCorrecta = false)
                        }
                    )
                },
                onEliminar = if (opciones.size > 2) {
                    {
                        onOpcionesChange(
                            opciones.filterIndexed { i, _ -> i != index }
                                .mapIndexed { newIndex, o -> o.copy(id = newIndex) }
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun OpcionCard(
    dims: ResponsiveDimensions,
    opcion: OpcionEditable,
    onTextoChange: (String) -> Unit,
    onCorrectaChange: (Boolean) -> Unit,
    onEliminar: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.borderRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (opcion.esCorrecta)
                EduRachaColors.Success.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(dims.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(dims.cardPadding),
            verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Opción ${opcion.id + 1}",
                    fontSize = (dims.bodyFontSize - 1).sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.25f)
                    ) {
                        Checkbox(
                            checked = opcion.esCorrecta,
                            onCheckedChange = onCorrectaChange,
                            modifier = Modifier.size(dims.iconSize + 4.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = EduRachaColors.Success
                            )
                        )
                        Text(
                            "Correcta",
                            fontSize = (dims.bodyFontSize - 1).sp,
                            color = if (opcion.esCorrecta)
                                EduRachaColors.Success
                            else
                                EduRachaColors.TextSecondary
                        )
                    }
                    onEliminar?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier.size(dims.iconSize + 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Eliminar",
                                modifier = Modifier.size(dims.iconSize),
                                tint = EduRachaColors.Error
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = opcion.texto,
                onValueChange = onTextoChange,
                label = { Text("Texto de la opción", fontSize = dims.bodyFontSize.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (opcion.esCorrecta)
                        EduRachaColors.Success
                    else
                        EduRachaColors.Primary
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = dims.bodyFontSize.sp)
            )
        }
    }
}

@Composable
fun FormularioIASection(
    dims: ResponsiveDimensions,
    cantidad: Int,
    onCantidadChange: (Int) -> Unit,
    onGenerar: () -> Unit,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(dims.spacing)) {
        SectionHeaderSimple(dims, "CONFIGURACIÓN DE IA", Icons.Default.AutoAwesome)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dims.borderRadius),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(dims.cardElevation)
        ) {
            Column(
                modifier = Modifier.padding(dims.cardPadding),
                verticalArrangement = Arrangement.spacedBy(dims.spacing)
            ) {
                // Info
                Surface(
                    shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(dims.cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.75f)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            modifier = Modifier.size(dims.iconSize),
                            tint = EduRachaColors.Accent
                        )
                        Text(
                            "La IA generará preguntas basadas en el contenido del tema. Todas pasarán a revisión antes de ser aprobadas.",
                            fontSize = dims.bodyFontSize.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = (dims.bodyFontSize + 4).sp
                        )
                    }
                }

                // Selector de cantidad
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)) {
                    Text(
                        "Cantidad de preguntas",
                        fontSize = dims.bodyFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$cantidad preguntas",
                            fontSize = (dims.titleFontSize - 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Accent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)) {
                            IconButton(
                                onClick = { if (cantidad > 1) onCantidadChange(cantidad - 1) },
                                enabled = cantidad > 1,
                                modifier = Modifier
                                    .size(dims.iconSize + 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (cantidad > 1)
                                            EduRachaColors.Accent.copy(alpha = 0.1f)
                                        else
                                            Color.Gray.copy(alpha = 0.1f)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    "Disminuir",
                                    modifier = Modifier.size(dims.iconSize),
                                    tint = if (cantidad > 1)
                                        EduRachaColors.Accent
                                    else
                                        Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { if (cantidad < 10) onCantidadChange(cantidad + 1) },
                                enabled = cantidad < 10,
                                modifier = Modifier
                                    .size(dims.iconSize + 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (cantidad < 10)
                                            EduRachaColors.Accent.copy(alpha = 0.1f)
                                        else
                                            Color.Gray.copy(alpha = 0.1f)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    "Aumentar",
                                    modifier = Modifier.size(dims.iconSize),
                                    tint = if (cantidad < 10)
                                        EduRachaColors.Accent
                                    else
                                        Color.Gray
                                )
                            }
                        }
                    }

                    Slider(
                        value = cantidad.toFloat(),
                        onValueChange = { onCantidadChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = EduRachaColors.Accent,
                            activeTrackColor = EduRachaColors.Accent
                        )
                    )
                }

                // Advertencia
                Surface(
                    shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(dims.cardPadding * 0.75f),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(dims.iconSize)
                        )
                        Text(
                            "Este proceso puede tomar entre 10-30 segundos.",
                            fontSize = (dims.bodyFontSize - 1).sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = (dims.bodyFontSize + 3).sp
                        )
                    }
                }
            }
        }

        // Botón generar
        Button(
            onClick = onGenerar,
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.buttonHeight),
            enabled = !isLoading,
            shape = RoundedCornerShape(dims.borderRadius),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Accent)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dims.iconSize),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(dims.spacing * 0.75f))
                Text("Generando...", fontSize = dims.subtitleFontSize.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(dims.iconSize))
                Spacer(Modifier.width(dims.spacing * 0.75f))
                Text("Generar con IA", fontSize = dims.subtitleFontSize.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeaderSimple(dims: ResponsiveDimensions, title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
    ) {
        Icon(
            icon,
            null,
            tint = EduRachaColors.Primary,
            modifier = Modifier.size(dims.iconSize)
        )
        Text(
            title,
            fontSize = dims.bodyFontSize.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Primary
        )
    }
}

@Composable
fun ErrorMessage(dims: ResponsiveDimensions, error: String) {
    Surface(
        shape = RoundedCornerShape(dims.borderRadius * 0.75f),
        color = EduRachaColors.Error.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(dims.cardPadding * 0.75f),
            horizontalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
        ) {
            Icon(
                Icons.Outlined.Error,
                null,
                modifier = Modifier.size(dims.iconSize),
                tint = EduRachaColors.Error
            )
            Text(
                error,
                color = EduRachaColors.Error,
                fontSize = dims.bodyFontSize.sp,
                lineHeight = (dims.bodyFontSize + 4).sp
            )
        }
    }
}

@Composable
fun LoadingOverlay(dims: ResponsiveDimensions, modo: ModoCreacion?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(dims.borderRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(dims.horizontalPadding)
        ) {
            Column(
                modifier = Modifier.padding(dims.cardPadding * 1.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dims.spacing)
            ) {
                CircularProgressIndicator(
                    color = EduRachaColors.Primary,
                    modifier = Modifier.size(dims.iconSize + 16.dp)
                )
                Text(
                    text = if (modo == ModoCreacion.IA)
                        "Generando preguntas con IA..."
                    else
                        "Creando pregunta...",
                    fontSize = dims.subtitleFontSize.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// DIÁLOGO DE CURSOS
@Composable
fun SelectorCursoDialogSimple(
    dims: ResponsiveDimensions,
    cursos: List<Curso>,
    onCursoSelected: (Curso) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar Curso",
                fontWeight = FontWeight.Bold,
                fontSize = (dims.subtitleFontSize + 2).sp
            )
        },
        text = {
            if (cursos.isEmpty()) {
                Text(
                    "No hay cursos disponibles",
                    fontSize = dims.bodyFontSize.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = min(dims.screenHeight * 0.6f, 400.dp))
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
                ) {
                    cursos.forEach { curso ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCursoSelected(curso) },
                            shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                            elevation = CardDefaults.cardElevation(dims.cardElevation * 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(dims.cardPadding * 0.75f)) {
                                Text(
                                    curso.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = dims.bodyFontSize.sp
                                )
                                if (curso.codigo.isNotEmpty()) {
                                    Text(
                                        "Código: ${curso.codigo}",
                                        fontSize = (dims.bodyFontSize - 2).sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = dims.bodyFontSize.sp)
            }
        },
        shape = RoundedCornerShape(dims.borderRadius)
    )
}

// DIÁLOGO DE TEMAS CON SCROLL
@Composable
fun SelectorTemaDialogSimple(
    dims: ResponsiveDimensions,
    temas: List<Tema>,
    onTemaSelected: (Tema) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar Tema",
                fontWeight = FontWeight.Bold,
                fontSize = (dims.subtitleFontSize + 2).sp
            )
        },
        text = {
            if (temas.isEmpty()) {
                Text(
                    "No hay temas disponibles en este curso",
                    fontSize = dims.bodyFontSize.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = min(dims.screenHeight * 0.6f, 400.dp))
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dims.spacing * 0.5f)
                ) {
                    temas.forEach { tema ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTemaSelected(tema) },
                            shape = RoundedCornerShape(dims.borderRadius * 0.75f),
                            elevation = CardDefaults.cardElevation(dims.cardElevation * 0.5f)
                        ) {
                            Text(
                                tema.titulo,
                                modifier = Modifier.padding(dims.cardPadding * 0.75f),
                                fontWeight = FontWeight.Bold,
                                fontSize = dims.bodyFontSize.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = dims.bodyFontSize.sp)
            }
        },
        shape = RoundedCornerShape(dims.borderRadius)
    )
}

// ================== FUNCIONES ==================

private fun crearPreguntaManual(
    context: Context,
    preguntaViewModel: PreguntaViewModel,
    curso: Curso,
    tema: Tema,
    textoPregunta: String,
    opciones: List<OpcionEditable>,
    dificultad: String,
    explicacion: String
) {
    val cursoId = curso.id ?: run {
        Toast.makeText(context, "Error: ID de curso no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val temaId = tema.id.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "Error: ID de tema no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val docenteNombre = prefs.getString("user_name", null) ?: "Docente"

    val pregunta = Pregunta(
        id = null,
        cursoId = cursoId,
        temaId = temaId,
        texto = textoPregunta,
        opciones = opciones.map { opcion ->
            Opcion(
                id = opcion.id,
                texto = opcion.texto,
                esCorrecta = opcion.esCorrecta
            )
        },
        fuente = FuentePregunta.DOCENTE,
        estado = EstadoPregunta.PENDIENTE_REVISION,
        dificultad = dificultad,
        creadoPor = docenteNombre,
        fechaCreacion = Instant.now().toString(),
        metadatosIA = null,
        revisadoPor = null,
        fechaRevision = null,
        notasRevision = null,
        modificada = false,
        explicacionCorrecta = explicacion
    )

    preguntaViewModel.crearPregunta(pregunta)
}

private fun generarPreguntasIA(
    context: Context,
    preguntaViewModel: PreguntaViewModel,
    curso: Curso,
    tema: Tema,
    cantidad: Int
) {
    val cursoId = curso.id ?: run {
        Toast.makeText(context, "Error: ID de curso no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val temaId = tema.id.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "Error: ID de tema no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val temaTexto = tema.titulo + if (tema.contenido.isNotEmpty()) {
        "\n\n${tema.contenido}"
    } else ""

    preguntaViewModel.generarPreguntasIA(
        cursoId = cursoId,
        temaId = temaId,
        temaTexto = temaTexto,
        cantidad = cantidad,
        onSuccess = { response ->
            Toast.makeText(
                context,
                "✅ ${response.total} preguntas generadas correctamente",
                Toast.LENGTH_LONG
            ).show()
        }
    )
}