package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.RetroalimentacionFallosResponse
import com.stiven.sos.repository.QuizRepository
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.launch

/**
 * Activity que muestra la retroalimentación de las preguntas incorrectas
 * Ayuda al estudiante a aprender de sus errores con elementos gamificados
 */
class RetroalimentacionActivity : ComponentActivity() {

    private lateinit var quizId: String
    private lateinit var repository: QuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        quizId = intent.getStringExtra("quizId") ?: ""
        repository = QuizRepository(application)

        setContent {
            EduRachaTheme {
                RetroalimentacionScreen(
                    quizId = quizId,
                    repository = repository,
                    onCerrar = { finish() }
                )
            }
        }
    }
}

/**
 * Pantalla principal de retroalimentación mejorada
 * Muestra las preguntas falladas con animaciones y elementos gamificados
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroalimentacionScreen(
    quizId: String,
    repository: QuizRepository,
    onCerrar: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var retroalimentacion by remember { mutableStateOf<RetroalimentacionFallosResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Animación de aparición gradual
    val alphaAnimation by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(quizId) {
        scope.launch {
            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { data ->
                    retroalimentacion = data
                    isLoading = false
                },
                onFailure = { e ->
                    error = e.message
                    isLoading = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            // TopBar con gradiente mejorado
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.Primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Retroalimentación",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onCerrar) {
                                Icon(
                                    Icons.Default.Close,
                                    "Cerrar",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        when {
            // Estado de carga con animación mejorada
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Indicador de carga pulsante
                        PulsingLoadingIndicator()
                        Text(
                            text = "Analizando tus respuestas",
                            color = EduRachaColors.TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Estado de error mejorado
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateCard(onCerrar = onCerrar)
                }
            }

            // Contenido principal
            retroalimentacion != null -> {
                if (retroalimentacion!!.totalFallos == 0) {
                    // Pantalla de éxito perfecta con animaciones
                    PerfectScoreScreen(onCerrar = onCerrar)
                } else {
                    // Lista de preguntas falladas con gamificación
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header con estadísticas gamificadas
                            item {
                                Spacer(Modifier.height(8.dp))
                                AnimatedStatsHeader(
                                    totalFallos = retroalimentacion!!.totalFallos,
                                    alphaAnimation = alphaAnimation
                                )
                            }

                            // Tarjetas de preguntas con animación escalonada
                            itemsIndexed(retroalimentacion!!.preguntasFalladas) { index, pregunta ->
                                val delay = index * 100
                                AnimatedPreguntaFalladaCard(
                                    numero = index + 1,
                                    pregunta = pregunta,
                                    delay = delay
                                )
                            }

                            // Botón de finalización mejorado
                            item {
                                Spacer(Modifier.height(8.dp))
                                GradientButton(
                                    text = "Entendido",
                                    onClick = onCerrar,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Indicador de carga con animación pulsante
 */
@Composable
fun PulsingLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = EduRachaColors.Primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(64.dp)
        )
    }
}

/**
 * Card de estado de error mejorada
 */
@Composable
fun ErrorStateCard(onCerrar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        EduRachaColors.Error.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = EduRachaColors.Error
                )
            }
            Text(
                text = "Algo salió mal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "No pudimos cargar la retroalimentación. Por favor, intenta nuevamente.",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCerrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cerrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Pantalla de puntuación perfecta con animaciones celebratorias
 */
@Composable
fun PerfectScoreScreen(onCerrar: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                // Trofeo animado con fondo circular
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        EduRachaColors.Success.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = EduRachaColors.Success
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Puntuación Perfecta",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "No tuviste errores en este quiz",
                        fontSize = 16.sp,
                        color = EduRachaColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Badge de logro
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Success.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Dominio Total",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Success
                        )
                    }
                }

                GradientButton(
                    text = "Continuar",
                    onClick = onCerrar,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Header con estadísticas y barra de progreso gamificada
 */
@Composable
fun AnimatedStatsHeader(totalFallos: Int, alphaAnimation: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = alphaAnimation },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Warning,
                                    EduRachaColors.Warning.copy(alpha = 0.8f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aprende de tus errores",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Revisa cada respuesta con atención",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Divider(
                color = EduRachaColors.TextSecondary.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Stats badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBadge(
                    icon = Icons.Default.QuestionMark,
                    value = "$totalFallos",
                    label = if (totalFallos == 1) "pregunta" else "preguntas",
                    color = EduRachaColors.Error,
                    modifier = Modifier.weight(1f)
                )

                StatBadge(
                    icon = Icons.Default.TipsAndUpdates,
                    value = "$totalFallos",
                    label = if (totalFallos == 1) "lección" else "lecciones",
                    color = EduRachaColors.Info,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Badge de estadística individual
 */
@Composable
fun StatBadge(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = EduRachaColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Card de pregunta fallada con animación de entrada y expandible
 */
@Composable
fun AnimatedPreguntaFalladaCard(
    numero: Int,
    pregunta: com.stiven.sos.models.RetroalimentacionPregunta,
    delay: Int
) {
    var expandido by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    // Animación de entrada escalonada
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { it / 2 }
                )
    ) {
        PreguntaFalladaCardContent(
            numero = numero,
            pregunta = pregunta,
            expandido = expandido,
            onToggleExpand = { expandido = !expandido }
        )
    }
}

/**
 * Contenido de la card de pregunta fallada
 */
@Composable
fun PreguntaFalladaCardContent(
    numero: Int,
    pregunta: com.stiven.sos.models.RetroalimentacionPregunta,
    expandido: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expandido) 8.dp else 4.dp
        ),
        onClick = onToggleExpand
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header de la pregunta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge con número
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Error,
                                    EduRachaColors.Error.copy(alpha = 0.8f)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$numero",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = EduRachaColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pregunta $numero",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Error
                        )
                    }
                    Text(
                        text = if (expandido) "Toca para ocultar" else "Toca para ver detalles",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                // Indicador de expansión animado
                AnimatedContent(
                    targetState = expandido,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    },
                    label = "expand_icon"
                ) { isExpanded ->
                    Surface(
                        shape = CircleShape,
                        color = EduRachaColors.Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = EduRachaColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Texto de la pregunta
            Text(
                text = pregunta.texto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary,
                lineHeight = 24.sp
            )

            // Contenido expandible con animación
            AnimatedVisibility(
                visible = expandido,
                enter = expandVertically(
                    animationSpec = tween(300),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    animationSpec = tween(300),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(
                        color = EduRachaColors.TextSecondary.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    // Tu respuesta (incorrecta)
                    RespuestaCard(
                        icon = Icons.Default.Cancel,
                        titulo = "Tu respuesta",
                        texto = pregunta.respuestaUsuarioTexto,
                        color = EduRachaColors.Error,
                        esCorrecta = false
                    )

                    // Respuesta correcta
                    RespuestaCard(
                        icon = Icons.Default.CheckCircle,
                        titulo = "Respuesta correcta",
                        texto = pregunta.respuestaCorrectaTexto,
                        color = EduRachaColors.Success,
                        esCorrecta = true
                    )

                    // Explicación
                    ExplicacionCard(explicacion = pregunta.explicacion)
                }
            }
        }
    }
}

/**
 * Card de respuesta (correcta o incorrecta)
 */
@Composable
fun RespuestaCard(
    icon: ImageVector,
    titulo: String,
    texto: String,
    color: Color,
    esCorrecta: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = if (esCorrecta) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 13.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = texto,
                    fontSize = 15.sp,
                    color = EduRachaColors.TextPrimary,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Card de explicación educativa
 */
@Composable
fun ExplicacionCard(explicacion: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EduRachaColors.Info.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(EduRachaColors.Info.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = EduRachaColors.Info,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Explicación",
                    fontSize = 13.sp,
                    color = EduRachaColors.Info,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = explicacion,
                    fontSize = 15.sp,
                    color = EduRachaColors.TextPrimary,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Botón con gradiente mejorado y animación
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    Button(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.Primary.copy(alpha = 0.85f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}