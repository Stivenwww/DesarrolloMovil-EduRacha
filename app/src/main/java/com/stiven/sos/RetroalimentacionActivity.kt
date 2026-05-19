package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.stiven.sos.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity V2 - Retroalimentación gamificada con diseño moderno
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
                RetroalimentacionScreenV2(
                    quizId = quizId,
                    repository = repository,
                    onCerrar = { finish() }
                )
            }
        }
    }
}

/**
 * Pantalla principal V2 con diseño gamificado
 */
@Composable
fun RetroalimentacionScreenV2(
    quizId: String,
    repository: QuizRepository,
    onCerrar: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var retroalimentacion by remember { mutableStateOf<RetroalimentacionFallosResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(quizId) {
        scope.launch {
            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { data ->
                    delay(300) // Pausa para mejor experiencia
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

    EduRachaV2Container {
        when {
            isLoading -> {
                RetroalimentacionLoadingState()
            }

            error != null -> {
                RetroalimentacionErrorState(
                    error = error ?: "Error desconocido",
                    onCerrar = onCerrar
                )
            }

            retroalimentacion != null -> {
                if (retroalimentacion!!.totalFallos == 0) {
                    PerfectScoreScreenV2(onCerrar = onCerrar)
                } else {
                    RetroalimentacionContent(
                        retroalimentacion = retroalimentacion!!,
                        onCerrar = onCerrar
                    )
                }
            }
        }
    }
}

/**
 * Loading State con animación moderna
 */
@Composable
fun RetroalimentacionLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animación de búsqueda circular
        val infiniteTransition = rememberInfiniteTransition(label = "search")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = rotation }
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = EduRachaV2Colors.Primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Analizando tus respuestas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaV2Colors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Preparando tu retroalimentación...",
            fontSize = 14.sp,
            color = EduRachaV2Colors.TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        EduRachaV2ProgressBar(
            progress = 0.7f,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(horizontal = 24.dp),
            color = EduRachaV2Colors.Success
        )
    }
}

/**
 * Error State con diseño mejorado
 */
@Composable
fun RetroalimentacionErrorState(
    error: String,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(EduRachaV2Colors.Warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = EduRachaV2Colors.Warning
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "¡Ups! Algo salió mal",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaV2Colors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No pudimos cargar tu retroalimentación",
            fontSize = 15.sp,
            color = EduRachaV2Colors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        EduRachaV2Button(
            text = "Cerrar",
            onClick = onCerrar,
            icon = Icons.Outlined.Close,
            gradient = EduRachaV2Gradients.Orange,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

/**
 * Pantalla de puntuación perfecta V2
 */
@Composable
fun PerfectScoreScreenV2(onCerrar: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(tween(600)) + scaleIn(tween(600))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Medalla animada
            MedallaAnimada(
                tipo = TipoMedalla.ORO,
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¡PERFECTO!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaV2Colors.Primary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sin errores en este quiz",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaV2Colors.TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Badges de logros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LogroBadge(
                    icon = Icons.Outlined.EmojiEvents,
                    label = "Experto",
                    color = EduRachaV2Colors.Success
                )
                LogroBadge(
                    icon = Icons.Outlined.Star,
                    label = "100%",
                    color = EduRachaV2Colors.Accent
                )
                LogroBadge(
                    icon = Icons.Outlined.Whatshot,
                    label = "Racha",
                    color = EduRachaV2Colors.Warning
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            EduRachaV2Button(
                text = "Continuar",
                onClick = onCerrar,
                icon = Icons.Outlined.ArrowForward,
                gradient = EduRachaV2Gradients.Green,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Badge de logro
 */
@Composable
fun LogroBadge(
    icon: ImageVector,
    label: String,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(3.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = color
            )
        }

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Contenido principal de retroalimentación
 */
@Composable
fun RetroalimentacionContent(
    retroalimentacion: RetroalimentacionFallosResponse,
    onCerrar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    EduRachaV2Gradients.Blue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
        ) {
            // Burbujas decorativas
            AnimatedBubblesDecoration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                color = Color.White
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 32.dp)
            ) {
                // Botón de cerrar
                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Título principal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )

                    Column {
                        Text(
                            text = "Retroalimentación",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Aprende de tus errores",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Lista de contenido
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Estadísticas de errores
            item {
                EstadisticasErroresCard(totalFallos = retroalimentacion.totalFallos)
            }

            // Título de sección
            item {
                Text(
                    text = "Preguntas a revisar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Preguntas falladas
            itemsIndexed(retroalimentacion.preguntasFalladas) { index, pregunta ->
                PreguntaFalladaCardV2(
                    numero = index + 1,
                    pregunta = pregunta,
                    delay = index * 80
                )
            }

            // Mensaje motivacional
            item {
                MensajeMotivacionalCard()
            }

            // Botón de finalizar - Más grande y visual
            item {
                Spacer(modifier = Modifier.height(16.dp))

                BotonHeAprendido(onClick = onCerrar)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Card de estadísticas de errores
 */
@Composable
fun EstadisticasErroresCard(totalFallos: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                EduRachaV2Colors.Warning.copy(alpha = 0.1f),
                                Color.White
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EduRachaV2Colors.Warning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = EduRachaV2Colors.Warning
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$totalFallos",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = EduRachaV2Colors.Warning
                        )
                        Text(
                            text = if (totalFallos == 1) "error encontrado" else "errores encontrados",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = EduRachaV2Colors.TextSecondary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduRachaV2Badge(
                        text = "REVISAR",
                        color = EduRachaV2Colors.Warning
                    )
                }
            }
        }
    }
}

/**
 * Card de pregunta fallada V2 con diseño moderno
 */
@Composable
fun PreguntaFalladaCardV2(
    numero: Int,
    pregunta: com.stiven.sos.models.RetroalimentacionPregunta,
    delay: Int
) {
    var expandido by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            tween(400),
            initialOffsetY = { it / 3 }
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = if (expandido) 8.dp else 4.dp,
            onClick = { expandido = !expandido }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Número con gradiente
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EduRachaV2Gradients.Pink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$numero",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = EduRachaV2Colors.Pink
                            )
                            Text(
                                text = "Pregunta $numero",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaV2Colors.Pink
                            )
                        }
                        Text(
                            text = if (expandido) "Toca para ocultar" else "Toca para ver detalles",
                            fontSize = 12.sp,
                            color = EduRachaV2Colors.TextSecondary
                        )
                    }

                    // Indicador de expansión
                    Surface(
                        shape = CircleShape,
                        color = EduRachaV2Colors.Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (expandido) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = EduRachaV2Colors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texto de la pregunta
                Text(
                    text = pregunta.texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaV2Colors.TextPrimary,
                    lineHeight = 24.sp
                )

                // Contenido expandible
                AnimatedVisibility(
                    visible = expandido,
                    enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Divider(
                            color = EduRachaV2Colors.SoftGray.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Tu respuesta (incorrecta)
                        RespuestaCardV2(
                            icon = Icons.Outlined.Cancel,
                            titulo = "TU RESPUESTA",
                            texto = pregunta.respuestaUsuarioTexto,
                            color = EduRachaV2Colors.Pink,
                            esCorrecta = false
                        )

                        // Respuesta correcta
                        RespuestaCardV2(
                            icon = Icons.Outlined.CheckCircle,
                            titulo = "RESPUESTA CORRECTA",
                            texto = pregunta.respuestaCorrectaTexto,
                            color = EduRachaV2Colors.Success,
                            esCorrecta = true
                        )

                        // Explicación
                        ExplicacionCardV2(explicacion = pregunta.explicacion)
                    }
                }
            }
        }
    }
}

/**
 * Card de respuesta V2
 */
@Composable
fun RespuestaCardV2(
    icon: ImageVector,
    titulo: String,
    texto: String,
    color: Color,
    esCorrecta: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = titulo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    letterSpacing = 1.sp
                )
                Text(
                    text = texto,
                    fontSize = 15.sp,
                    color = EduRachaV2Colors.TextPrimary,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Card de explicación V2
 */
@Composable
fun ExplicacionCardV2(explicacion: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EduRachaV2Colors.Primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = EduRachaV2Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "EXPLICACIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.Primary,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = explicacion,
                fontSize = 15.sp,
                color = EduRachaV2Colors.TextPrimary,
                lineHeight = 23.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Card con mensaje motivacional
 */
@Composable
fun MensajeMotivacionalCard() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + scaleIn(tween(600))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                EduRachaV2Colors.Success.copy(alpha = 0.1f),
                                EduRachaV2Colors.Primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "motivacion")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rotation"
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Success.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💪",
                        fontSize = 28.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Sigue mejorando!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cada error es una oportunidad de aprendizaje",
                        fontSize = 13.sp,
                        color = EduRachaV2Colors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Botón "He Aprendido" grande y visual sin fondo gris
 */
@Composable
fun BotonHeAprendido(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Button(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EduRachaV2Colors.Success,
                            EduRachaV2Colors.SuccessDark,
                            EduRachaV2Colors.Success
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "He Aprendido",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}