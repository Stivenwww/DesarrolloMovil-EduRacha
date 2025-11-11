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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import com.stiven.sos.viewmodel.TemaViewModel
import java.time.Instant

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
                        .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                        }

                        // Badge de progreso
                        if (modoCreacion != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (modoCreacion == ModoCreacion.MANUAL)
                                            Icons.Outlined.Edit
                                        else
                                            Icons.Outlined.AutoAwesome,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        if (modoCreacion == ModoCreacion.MANUAL) "Manual" else "IA",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Vista previa
                    AnimatedVisibility(
                        visible = textoPregunta.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(40.dp)
                                    .rotate(questionRotation)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Vista Previa",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    textoPregunta,
                                    color = Color.White,
                                    fontSize = 18.sp,
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
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Elige cómo deseas crear la pregunta",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // CONTENIDO
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // PASO 1: Seleccionar modo
                if (modoCreacion == null) {
                    ModoCreacionCards(onModoSelected = { modoCreacion = it })
                } else {
                    // Modo seleccionado
                    ModoSeleccionadoHeader(
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
                                            explicacionCorrecta.isNotBlank()) { // ✅ VALIDAR EXPLICACIÓN
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
                        ErrorMessage(error)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        // Loading overlay
        if (preguntaUiState.isLoading) {
            LoadingOverlay(modo = modoCreacion)
        }
    }

    // Diálogos
    if (showCursoDialog) {
        SelectorCursoDialogSimple(
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
            temas = temaUiState.temas,
            onTemaSelected = {
                temaSeleccionado = it
                showTemaDialog = false
            },
            onDismiss = { showTemaDialog = false }
        )
    }
}

// ================== COMPONENTES ==================

@Composable
fun ModoCreacionCards(onModoSelected: (ModoCreacion) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeaderSimple("SELECCIONA EL MÉTODO", Icons.Default.TouchApp)

        ModoCard(
            title = "Creación Manual",
            description = "Escribe tu pregunta y opciones personalizadas",
            icon = Icons.Outlined.Edit,
            color = EduRachaColors.Primary,
            onClick = { onModoSelected(ModoCreacion.MANUAL) }
        )

        ModoCard(
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
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(30.dp), tint = color)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    description,
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                null,
                tint = EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun ModoSeleccionadoHeader(modo: ModoCreacion, onCambiar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (modo == ModoCreacion.MANUAL)
            EduRachaColors.Primary.copy(alpha = 0.1f)
        else
            EduRachaColors.Accent.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (modo == ModoCreacion.MANUAL) Icons.Outlined.Edit else Icons.Outlined.AutoAwesome,
                    null,
                    tint = if (modo == ModoCreacion.MANUAL) EduRachaColors.Primary else EduRachaColors.Accent
                )
                Text(
                    if (modo == ModoCreacion.MANUAL) "Creación Manual" else "Generación con IA",
                    fontWeight = FontWeight.Bold,
                    color = if (modo == ModoCreacion.MANUAL) EduRachaColors.Primary else EduRachaColors.Accent
                )
            }
            TextButton(onClick = onCambiar) {
                Text("Cambiar")
            }
        }
    }
}

@Composable
fun SelectorsSection(
    cursoSeleccionado: Curso?,
    temaSeleccionado: Tema?,
    isLoadingTemas: Boolean,
    onCursoClick: () -> Unit,
    onTemaClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectorCard(
            title = "Curso",
            value = cursoSeleccionado?.titulo ?: "Seleccionar curso",
            icon = Icons.Outlined.MenuBook,
            isSelected = cursoSeleccionado != null,
            onClick = onCursoClick
        )

        if (cursoSeleccionado != null) {
            SelectorCard(
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                EduRachaColors.Success.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = EduRachaColors.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        icon,
                        null,
                        tint = if (isSelected) EduRachaColors.Success else EduRachaColors.TextSecondary
                    )
                }
                Column {
                    Text(
                        title,
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                    Text(
                        value,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) EduRachaColors.Success else EduRachaColors.TextPrimary
                    )
                }
            }
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                null,
                tint = if (isSelected) EduRachaColors.Success else EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun FormularioManualSection(
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeaderSimple("DETALLES DE LA PREGUNTA", Icons.Default.Edit)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pregunta
                OutlinedTextField(
                    value = textoPregunta,
                    onValueChange = onTextoPreguntaChange,
                    label = { Text("Texto de la pregunta *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = preguntaError,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (preguntaError) EduRachaColors.Error else EduRachaColors.Primary,
                        focusedLabelColor = if (preguntaError) EduRachaColors.Error else EduRachaColors.Primary
                    )
                )

                // Dificultad
                DificultadSelector(
                    dificultad = dificultad,
                    onDificultadChange = onDificultadChange
                )
            }
        }

        // Opciones
        OpcionesSection(
            opciones = opciones,
            onOpcionesChange = onOpcionesChange
        )

        // Explicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Explicación de la respuesta *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "OBLIGATORIO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Explica por qué esta es la respuesta correcta. Esta información ayudará a los estudiantes a comprender mejor el tema.",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                OutlinedTextField(
                    value = explicacionCorrecta,
                    onValueChange = onExplicacionChange,
                    label = { Text("¿Por qué esta es la respuesta correcta?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = explicacionCorrecta.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
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
                                color = EduRachaColors.Error
                            )
                        } else {
                            Text(
                                "${explicacionCorrecta.length} caracteres",
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                )
            }
        }

        // Botón
        Button(
            onClick = onCrear,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Creando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(12.dp))
                Text("Crear Pregunta", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DificultadSelector(dificultad: String, onDificultadChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Dificultad",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dificultad == DificultadPregunta.FACIL,
                onClick = { onDificultadChange(DificultadPregunta.FACIL) },
                label = { Text("Fácil") },
                leadingIcon = if (dificultad == DificultadPregunta.FACIL) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = dificultad == DificultadPregunta.MEDIO,
                onClick = { onDificultadChange(DificultadPregunta.MEDIO) },
                label = { Text("Medio") },
                leadingIcon = if (dificultad == DificultadPregunta.MEDIO) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = dificultad == DificultadPregunta.DIFICIL,
                onClick = { onDificultadChange(DificultadPregunta.DIFICIL) },
                label = { Text("Difícil") },
                leadingIcon = if (dificultad == DificultadPregunta.DIFICIL) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun OpcionesSection(
    opciones: List<OpcionEditable>,
    onOpcionesChange: (List<OpcionEditable>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeaderSimple("OPCIONES DE RESPUESTA", Icons.Default.ListAlt)
            if (opciones.size < 5) {
                IconButton(
                    onClick = {
                        onOpcionesChange(opciones + OpcionEditable(opciones.size, "", false))
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EduRachaColors.Success.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Agregar opción",
                        tint = EduRachaColors.Success
                    )
                }
            }
        }

        opciones.forEachIndexed { index, opcion ->
            OpcionCard(
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
    opcion: OpcionEditable,
    onTextoChange: (String) -> Unit,
    onCorrectaChange: (Boolean) -> Unit,
    onEliminar: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (opcion.esCorrecta)
                EduRachaColors.Success.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Opción ${opcion.id + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = opcion.esCorrecta,
                            onCheckedChange = onCorrectaChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = EduRachaColors.Success
                            )
                        )
                        Text(
                            "Correcta",
                            fontSize = 12.sp,
                            color = if (opcion.esCorrecta)
                                EduRachaColors.Success
                            else
                                EduRachaColors.TextSecondary
                        )
                    }
                    onEliminar?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Eliminar",
                                modifier = Modifier.size(20.dp),
                                tint = EduRachaColors.Error
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = opcion.texto,
                onValueChange = onTextoChange,
                label = { Text("Texto de la opción") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (opcion.esCorrecta)
                        EduRachaColors.Success
                    else
                        EduRachaColors.Primary
                )
            )
        }
    }
}

@Composable
fun FormularioIASection(
    cantidad: Int,
    onCantidadChange: (Int) -> Unit,
    onGenerar: () -> Unit,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeaderSimple("CONFIGURACIÓN DE IA", Icons.Default.AutoAwesome)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = EduRachaColors.Accent
                        )
                        Text(
                            "La IA generará preguntas basadas en el contenido del tema. Todas pasarán a revisión antes de ser aprobadas.",
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Selector de cantidad
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cantidad de preguntas",
                        fontSize = 14.sp,
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Accent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { if (cantidad > 1) onCantidadChange(cantidad - 1) },
                                enabled = cantidad > 1,
                                modifier = Modifier
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
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Este proceso puede tomar entre 10-30 segundos.",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary
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
                .height(60.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Accent)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Generando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(12.dp))
                Text("Generar con IA", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeaderSimple(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = EduRachaColors.Primary, modifier = Modifier.size(20.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Primary
        )
    }
}

@Composable
fun ErrorMessage(error: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = EduRachaColors.Error.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Error,
                null,
                tint = EduRachaColors.Error
            )
            Text(
                error,
                color = EduRachaColors.Error,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LoadingOverlay(modo: ModoCreacion?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = EduRachaColors.Primary)
                Text(
                    text = if (modo == ModoCreacion.IA)
                        "Generando preguntas con IA..."
                    else
                        "Creando pregunta...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ✅ DIÁLOGO DE CURSOS CON SCROLL
@Composable
fun SelectorCursoDialogSimple(
    cursos: List<Curso>,
    onCursoSelected: (Curso) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Curso", fontWeight = FontWeight.Bold) },
        text = {
            if (cursos.isEmpty()) {
                Text("No hay cursos disponibles")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // ✅ Altura máxima
                        .verticalScroll(rememberScrollState()), // ✅ SCROLL AGREGADO
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cursos.forEach { curso ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCursoSelected(curso) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    curso.titulo,
                                    fontWeight = FontWeight.Bold
                                )
                                if (curso.codigo.isNotEmpty()) {
                                    Text(
                                        "Código: ${curso.codigo}",
                                        fontSize = 12.sp,
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
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ✅ DIÁLOGO DE TEMAS CON SCROLL
@Composable
fun SelectorTemaDialogSimple(
    temas: List<Tema>,
    onTemaSelected: (Tema) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Tema", fontWeight = FontWeight.Bold) },
        text = {
            if (temas.isEmpty()) {
                Text("No hay temas disponibles en este curso")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // ✅ Altura máxima
                        .verticalScroll(rememberScrollState()), // ✅ SCROLL AGREGADO
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    temas.forEach { tema ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTemaSelected(tema) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                tema.titulo,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
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
        Toast.makeText(context, "❌ Error: ID de curso no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val temaId = tema.id.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "❌ Error: ID de tema no válido", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(context, "❌ Error: ID de curso no válido", Toast.LENGTH_SHORT).show()
        return
    }

    val temaId = tema.id.takeIf { it.isNotEmpty() } ?: run {
        Toast.makeText(context, "❌ Error: ID de tema no válido", Toast.LENGTH_SHORT).show()
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