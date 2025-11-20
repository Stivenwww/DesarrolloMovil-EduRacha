package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.stiven.sos.services.AnimacionEstrellaIncorrecta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class QuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()

    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var userId: String
    private lateinit var modo: String

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Quiz"
        modo = intent.getStringExtra("modo") ?: "oficial"

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        userId = prefs.getString("user_uid", "") ?: ""

        if (userId.isNotEmpty() && cursoId.isNotEmpty() && temaId.isNotEmpty()) {
            com.stiven.sos.services.NotificacionesHelper.notificarQuizIniciado(
                estudianteId = userId,
                cursoId = cursoId,
                temaId = temaId,
                tituloTema = temaTitulo
            )
        }

        setContent {
            EduRachaTheme {
                val uiState by quizViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.quizInterrumpidoPorVidas) {
                    if (uiState.quizInterrumpidoPorVidas) {
                        android.util.Log.e("QuizActivity", "========================================")
                        android.util.Log.e("QuizActivity", "DETECCION CRITICA: Quiz interrumpido por vidas")
                        android.util.Log.e("QuizActivity", "ACCION: Redirigiendo en 2 segundos")
                        android.util.Log.e("QuizActivity", "========================================")

                        kotlinx.coroutines.delay(100000)
                        regresarATemasDelCurso()
                    }
                }

                LaunchedEffect(uiState.sinVidas, uiState.quizActivo) {
                    if (uiState.sinVidas &&
                        uiState.quizActivo != null &&
                        !uiState.quizInterrumpidoPorVidas &&
                        !uiState.finalizando) {

                        android.util.Log.w("QuizActivity", "========================================")
                        android.util.Log.w("QuizActivity", "DETECCION: Usuario sin vidas durante quiz")
                        android.util.Log.w("QuizActivity", "Modo: $modo")
                        android.util.Log.w("QuizActivity", "ACCION: Mostrando dialogo de bloqueo")
                        android.util.Log.w("QuizActivity", "========================================")

                        quizViewModel.mostrarDialogoSinVidas()
                    }
                }

                QuizScreen(
                    cursoId = cursoId,
                    temaId = temaId,
                    temaTitulo = temaTitulo,
                    modo = modo,
                    quizViewModel = quizViewModel,
                    onNavigateToResultado = {
                        val resultado = quizViewModel.uiState.value.resultadoQuiz
                        val quizId = quizViewModel.uiState.value.quizActivo?.quizId

                        val intent = Intent(this, ResultadoQuizActivity::class.java)
                        intent.putExtra("preguntasCorrectas", resultado?.preguntasCorrectas ?: 0)
                        intent.putExtra("preguntasIncorrectas", resultado?.preguntasIncorrectas ?: 0)
                        intent.putExtra("experienciaGanada", resultado?.experienciaGanada ?: 0)
                        intent.putExtra("vidasRestantes", resultado?.vidasRestantes ?: 0)
                        intent.putExtra("bonificacionRapidez", resultado?.bonificaciones?.rapidez ?: 0)
                        intent.putExtra("bonificacionPrimeraVez", resultado?.bonificaciones?.primeraVez ?: 0)
                        intent.putExtra("bonificacionTodoCorrecto", resultado?.bonificaciones?.todoCorrecto ?: 0)
                        intent.putExtra("quizId", quizId ?: "")
                        intent.putExtra("modo", modo)

                        startActivity(intent)
                        finish()
                    },
                    onRegresarATemasDelCurso = {
                        regresarATemasDelCurso()
                    }
                )
            }
        }
    }

    private fun regresarATemasDelCurso() {
        quizViewModel.limpiarQuiz()
        val intent = Intent(this, TemasDelCursoActivity::class.java)
        intent.putExtra("curso_id", cursoId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()

        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            android.util.Log.w("QuizActivity", "Usuario intento salir durante el quiz")
        }
    }

    override fun onResume() {
        super.onResume()

        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            android.util.Log.d("QuizActivity", "App regreso con quiz activo")
        }
    }
}

// Sistema de scaling responsivo mejorado
data class ResponsiveScaling(
    val padding: Float,
    val text: Float,
    val icon: Float,
    val elevation: Float
)

@Composable
fun rememberResponsiveScaling(): ResponsiveScaling {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    return remember(screenWidth, screenHeight) {
        when {
            screenWidth < 360.dp -> ResponsiveScaling(0.8f, 0.85f, 0.85f, 0.8f)
            screenWidth < 400.dp -> ResponsiveScaling(0.9f, 0.95f, 0.95f, 0.9f)
            screenWidth < 600.dp -> ResponsiveScaling(1f, 1f, 1f, 1f)
            screenWidth < 840.dp -> ResponsiveScaling(1.2f, 1.1f, 1.15f, 1.2f)
            else -> ResponsiveScaling(1.4f, 1.2f, 1.3f, 1.4f)
        }
    }
}

@Composable
fun CardPreguntaMejorada(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    colorModo: Color
) {
    val scale = rememberResponsiveScaling()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 * scale.padding).dp),
        shape = RoundedCornerShape((20 * scale.padding).dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = (6 * scale.elevation).dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorModo.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
                .padding((20 * scale.padding).dp),
            verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((12 * scale.padding).dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorModo.copy(alpha = 0.15f),
                    modifier = Modifier.size((46 * scale.icon).dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$numeroPregunta",
                            fontSize = (18 * scale.text).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorModo
                        )
                    }
                }
                Text(
                    text = "Pregunta",
                    fontSize = (17 * scale.text).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorModo
                )
            }

            Divider(
                color = colorModo.copy(alpha = 0.2f),
                thickness = (1.5 * scale.padding).dp
            )

            Text(
                text = pregunta.texto,
                fontSize = (16 * scale.text).sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary,
                lineHeight = (24 * scale.text).sp
            )
        }
    }
}

@Composable
fun OpcionRespuestaMejorada(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    index: Int,
    isSelected: Boolean,
    colorModo: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) (10 * scale.elevation).dp else (3 * scale.elevation).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 * scale.padding).dp, vertical = (6 * scale.padding).dp)
            .scale(scaleAnim)
            .alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape((16 * scale.padding).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorModo.copy(alpha = 0.12f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) (2.5 * scale.padding).dp else (1.5 * scale.padding).dp,
            color = if (isSelected) colorModo else EduRachaColors.Border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scale.padding).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((12 * scale.padding).dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) colorModo.copy(alpha = 0.2f) else EduRachaColors.Border.copy(alpha = 0.3f),
                modifier = Modifier.size((36 * scale.icon).dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorModo,
                            modifier = Modifier.size((22 * scale.icon).dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size((22 * scale.icon).dp)
                        )
                    }
                }
            }

            Text(
                text = opcion.texto,
                fontSize = (15 * scale.text).sp,
                color = if (isSelected) colorModo else EduRachaColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                lineHeight = (22 * scale.text).sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BotonConfirmarMejorado(
    enabled: Boolean,
    colorModo: Color,
    esUltimaPregunta: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = rememberResponsiveScaling()

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height((58 * scale.padding).dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorModo,
            disabledContainerColor = EduRachaColors.TextSecondary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape((16 * scale.padding).dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = (6 * scale.elevation).dp,
            pressedElevation = (10 * scale.elevation).dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (esUltimaPregunta) "Finalizar Quiz" else "Siguiente",
                fontSize = (17 * scale.text).sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                if (esUltimaPregunta) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size((24 * scale.icon).dp)
            )
        }
    }
}

@Composable
fun DialogoAbandonarQuizConBloqueo(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Error.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = "Salida bloqueada durante el quiz",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (20 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (26 * scale.text).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Por política de integridad académica, no puedes salir de la aplicación mientras resuelves un quiz.",
                    fontSize = (14 * scale.text).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (20 * scale.text).sp
                )

                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((18 * scale.padding).dp),
                        verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size((24 * scale.icon).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((5 * scale.padding).dp)
                            ) {
                                Text(
                                    "Si decides abandonar:",
                                    fontSize = (14 * scale.text).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )

                                Text(
                                    "Perderás tu progreso actual",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Text(
                    "Te recomendamos continuar y dar lo mejor de ti",
                    fontSize = (13 * scale.text).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.Primary,
                    textAlign = TextAlign.Center,
                    lineHeight = (18 * scale.text).sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((52 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Abandonar de todas formas",
                        fontSize = (14 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onCancelar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((52 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Continuar quiz",
                        fontSize = (15 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}
// CONTINUACIÓN DE QuizActivity.kt - PARTE 2
// Este código debe ir después de la Parte 1

@Composable
fun PreguntaScreen(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    colorModo: Color,
    temaTitulo: String,
    modo: String,
    tiempoTotalQuiz: Int,
    sinVidas: Boolean,
    respuestasEstado: Map<Int, Boolean>,
    ultimaRespuestaCorrecta: Boolean?,
    mostrarAnimacionRespuesta: Boolean,
    onRespuestaSeleccionada: (Int) -> Unit,
    onAnimacionCompletada: () -> Unit
) {
    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val scale = rememberResponsiveScaling()

    LaunchedEffect(sinVidas) {
        if (sinVidas) {
            android.util.Log.w("PreguntaScreen", "Detección: Usuario sin vidas")
            android.util.Log.w("PreguntaScreen", "Cancelando animación y limpiando selección")
            respuestaSeleccionada = null
        }
    }

    // Box con WindowInsets para evitar solapamiento con barra de estado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorModo.copy(alpha = 0.08f),
                            EduRachaColors.Background,
                            Color.White
                        )
                    )
                )
        ) {
            // Header con padding superior para evitar solapamiento
            HeaderQuizMejorado(
                temaTitulo = temaTitulo,
                modo = modo,
                colorModo = colorModo,
                tiempoTranscurrido = tiempoTotalQuiz
            )

            // Barra de progreso
            BarraProgresoConEstrellasIluminadas(
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                colorModo = colorModo,
                respuestaSeleccionada = respuestaSeleccionada != null,
                respuestasEstado = respuestasEstado
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = (12 * scale.padding).dp),
                verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp)
            ) {
                CardPreguntaMejorada(
                    pregunta = pregunta,
                    numeroPregunta = numeroPregunta,
                    colorModo = colorModo
                )

                pregunta.opciones.forEachIndexed { index, opcion ->
                    OpcionRespuestaMejorada(
                        opcion = opcion,
                        index = index,
                        isSelected = respuestaSeleccionada == index,
                        colorModo = colorModo,
                        enabled = !sinVidas,
                        onClick = {
                            if (!sinVidas) {
                                respuestaSeleccionada = index
                            } else {
                                android.util.Log.w("PreguntaScreen", "Intento bloqueado: Sin vidas")
                            }
                        }
                    )
                }

                Spacer(Modifier.height((16 * scale.padding).dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = (6 * scale.elevation).dp
            ) {
                BotonConfirmarMejorado(
                    enabled = respuestaSeleccionada != null && !sinVidas,
                    colorModo = colorModo,
                    esUltimaPregunta = numeroPregunta >= totalPreguntas,
                    onClick = {
                        if (!sinVidas) {
                            respuestaSeleccionada?.let { opcionSeleccionada ->
                                onRespuestaSeleccionada(opcionSeleccionada)
                            }
                        } else {
                            android.util.Log.w("PreguntaScreen", "Click bloqueado: Sin vidas")
                        }
                    },
                    modifier = Modifier.padding((16 * scale.padding).dp)
                )
            }
        }

        if (sinVidas) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape((18 * scale.padding).dp),
                    color = Color.White,
                    shadowElevation = (10 * scale.elevation).dp
                ) {
                    Column(
                        modifier = Modifier.padding((28 * scale.padding).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size((56 * scale.icon).dp)
                        )
                        Text(
                            "Quiz Bloqueado",
                            fontSize = (22 * scale.text).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF4B4B)
                        )
                        Text(
                            "Te has quedado sin vidas",
                            fontSize = (15 * scale.text).sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Animaciones de respuesta correcta/incorrecta
    if (mostrarAnimacionRespuesta && !sinVidas) {
        if (ultimaRespuestaCorrecta == true) {
            AnimacionEstrellaExitosa(
                colorModo = colorModo,
                onAnimacionCompleta = onAnimacionCompletada
            )
        } else if (ultimaRespuestaCorrecta == false) {
            AnimacionEstrellaIncorrecta(
                onAnimacionCompleta = onAnimacionCompletada
            )
        }
    }
}

@Composable
fun AnimacionEstrellaExitosa(
    colorModo: Color,
    onAnimacionCompleta: () -> Unit
) {
    var animacionIniciada by remember { mutableStateOf(false) }

    val escalaEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "escalaEstrella"
    )

    val rotacionEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 360f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "rotacionEstrella"
    )

    val alphaEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 0f else 1f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 400,
            easing = LinearEasing
        ),
        label = "alphaEstrella"
    )

    LaunchedEffect(Unit) {
        animacionIniciada = true
        delay(500)
        onAnimacionCompleta()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.3f * alphaEstrella)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            ParticulasBrillantes(
                visible = animacionIniciada,
                color = colorModo
            )

            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .size(200.dp)
                    .scale(escalaEstrella)
                    .rotate(rotacionEstrella)
                    .alpha(alphaEstrella)
            )

            Canvas(
                modifier = Modifier
                    .size(250.dp)
                    .alpha(alphaEstrella * 0.6f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.6f),
                            Color(0xFFFFD700).copy(alpha = 0f)
                        )
                    ),
                    radius = size.minDimension / 2 * escalaEstrella
                )
            }
        }
    }
}

@Composable
fun ParticulasBrillantes(
    visible: Boolean,
    color: Color
) {
    val numeroParticulas = 12

    for (i in 0 until numeroParticulas) {
        val angulo = (360f / numeroParticulas) * i

        val offset by animateFloatAsState(
            targetValue = if (visible) 120f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offset_$i"
        )

        val escala by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "escala_$i"
        )

        val alpha by animateFloatAsState(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 300,
                easing = LinearEasing
            ),
            label = "alpha_$i"
        )

        val offsetX = cos(Math.toRadians(angulo.toDouble())).toFloat() * offset
        val offsetY = sin(Math.toRadians(angulo.toDouble())).toFloat() * offset

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .fillMaxSize()
                    .scale(escala)
                    .alpha(alpha)
            )
        }
    }
}

fun formatearTiempo(segundos: Int): String {
    val minutos = segundos / 60
    val segs = segundos % 60
    return String.format("%d:%02d", minutos, segs)
}

@Composable
fun HeaderQuizMejorado(
    temaTitulo: String,
    modo: String,
    colorModo: Color,
    tiempoTranscurrido: Int
) {
    val scale = rememberResponsiveScaling()

    // Configuración de modo
    data class ModoConfig(
        val principal: Color,
        val secundario: Color,
        val terciario: Color,
        val texto: String,
        val icono: androidx.compose.ui.graphics.vector.ImageVector
    )

    val config = when (modo) {
        "practica" -> ModoConfig(
            Color(0xFF9C27B0),
            Color(0xFFBA68C8),
            Color(0xFFE1BEE7),
            "Modo Práctica",
            Icons.Default.FitnessCenter
        )
        "final" -> ModoConfig(
            Color(0xFFFFB300),
            Color(0xFFFFCA28),
            Color(0xFFFFE082),
            "Quiz Final",
            Icons.Default.EmojiEvents
        )
        else -> ModoConfig(
            Color(0xFF1CB0F6),
            Color(0xFF4FC3F7),
            Color(0xFFB3E5FC),
            "Modo Oficial",
            Icons.Default.School
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = (4 * scale.elevation).dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            config.principal,
                            config.principal.copy(alpha = 0.9f),
                            config.secundario.copy(alpha = 0.7f),
                            config.terciario.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            // Patrón decorativo de fondo
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((80 * scale.padding).dp)
                    .alpha(0.12f)
            ) {
                val circleRadius = (40 * scale.padding).dp.toPx()

                drawCircle(
                    color = Color.White,
                    radius = circleRadius,
                    center = Offset(size.width * 0.15f, size.height * 0.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = circleRadius * 0.6f,
                    center = Offset(size.width * 0.85f, size.height * 0.3f)
                )
                drawCircle(
                    color = Color.White,
                    radius = circleRadius * 0.4f,
                    center = Offset(size.width * 0.92f, size.height * 0.75f)
                )

                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height * 0.3f),
                    end = Offset(size.width * 0.3f, size.height * 0.3f),
                    strokeWidth = 2f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = (16 * scale.padding).dp,
                        vertical = (14 * scale.padding).dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy((7 * scale.padding).dp)
                    ) {
                        Text(
                            text = temaTitulo,
                            fontSize = (17 * scale.text).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            lineHeight = (23 * scale.text).sp
                        )

                        Surface(
                            shape = RoundedCornerShape((12 * scale.padding).dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = (12 * scale.padding).dp,
                                    vertical = (6 * scale.padding).dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy((7 * scale.padding).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = config.icono,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size((16 * scale.icon).dp)
                                )
                                Text(
                                    text = config.texto,
                                    color = Color.White,
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape((16 * scale.padding).dp),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.padding(start = (8 * scale.padding).dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = (14 * scale.padding).dp,
                                vertical = (10 * scale.padding).dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconoRelojPulsante()

                            Text(
                                text = formatearTiempo(tiempoTranscurrido),
                                fontSize = (19 * scale.text).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconoRelojPulsante() {
    val infiniteTransition = rememberInfiniteTransition(label = "reloj")

    val escala by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    Icon(
        Icons.Default.Timer,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(22.dp)
            .scale(escala)
    )
}

@Composable
fun BarraProgresoConEstrellasIluminadas(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    respuestaSeleccionada: Boolean,
    respuestasEstado: Map<Int, Boolean>
) {
    val scale = rememberResponsiveScaling()

    val progreso = (preguntaActual.toFloat() / totalPreguntas.toFloat())

    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progreso"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = (16 * scale.padding).dp,
                vertical = (10 * scale.padding).dp
            ),
            verticalArrangement = Arrangement.spacedBy((10 * scale.padding).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((10 * scale.padding).dp)
                    .clip(RoundedCornerShape((5 * scale.padding).dp))
                    .background(colorModo.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progresoAnimado)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape((5 * scale.padding).dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorModo,
                                    colorModo.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 1..totalPreguntas) {
                    val indice = i - 1
                    val estrellaCompletada = i <= preguntaActual
                    val estaActiva = i == preguntaActual && respuestaSeleccionada
                    val esIncorrecta = respuestasEstado[indice] == false

                    EstrellaIluminada(
                        completada = estrellaCompletada,
                        estaActiva = estaActiva,
                        esIncorrecta = esIncorrecta
                    )
                }
            }
        }
    }
}

// CONTINUACIÓN DE QuizActivity.kt - PARTE 3
// Este código debe ir después de la Parte 2

@Composable
fun EstrellaIluminada(
    completada: Boolean,
    estaActiva: Boolean,
    esIncorrecta: Boolean
) {
    val scale = rememberResponsiveScaling()

    val infiniteTransition = rememberInfiniteTransition(label = "estrella")

    val brillo by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brillo"
    )

    val escala by animateFloatAsState(
        targetValue = if (completada) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escala"
    )

    Box(
        modifier = Modifier.size((26 * scale.icon).dp),
        contentAlignment = Alignment.Center
    ) {
        if (completada && estaActiva && !esIncorrecta) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * brillo
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }

        if (esIncorrecta && completada) {
            // Estrella rota con grietas
            Box(
                modifier = Modifier
                    .size((22 * scale.icon).dp)
                    .scale(escala),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.fillMaxSize()
                )

                // Líneas de grieta
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2

                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - size.width * 0.3f, centerY),
                        end = Offset(centerX + size.width * 0.3f, centerY),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = Color.White,
                        start = Offset(centerX, centerY - size.height * 0.3f),
                        end = Offset(centerX, centerY + size.height * 0.3f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - size.width * 0.2f, centerY - size.height * 0.2f),
                        end = Offset(centerX + size.width * 0.2f, centerY + size.height * 0.2f),
                        strokeWidth = 1f,
                        cap = StrokeCap.Round
                    )
                }
            }
        } else {
            // Estrella normal
            Icon(
                if (completada) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (completada) Color(0xFFFFD700) else EduRachaColors.TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier
                    .size((22 * scale.icon).dp)
                    .scale(escala)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    cursoId: String,
    temaId: String,
    temaTitulo: String,
    modo: String,
    quizViewModel: QuizViewModel,
    onNavigateToResultado: () -> Unit,
    onRegresarATemasDelCurso: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    var yaNavego by remember { mutableStateOf(false) }
    var yaCargoRetroalimentacion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    var tiempoTotalQuiz by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        quizViewModel.iniciarObservadores(cursoId, temaId)
        quizViewModel.iniciarQuiz(cursoId, temaId, modo)
    }

    LaunchedEffect(uiState.quizActivo, uiState.finalizando) {
        if (uiState.quizActivo != null && !uiState.finalizando) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tiempoTotalQuiz++
            }
        }
    }

    LaunchedEffect(uiState.sinVidas, uiState.quizActivo) {
        if (uiState.sinVidas &&
            uiState.quizActivo != null &&
            !uiState.finalizando &&
            !uiState.quizInterrumpidoPorVidas) {

            android.util.Log.w("QuizScreen", "Usuario sin vidas durante quiz")
            android.util.Log.w("QuizScreen", "Modo: $modo")
            android.util.Log.w("QuizScreen", "Mostrando dialogo SIN auto-cierre")

            quizViewModel.mostrarDialogoSinVidas()
        }
    }

    BackHandler(enabled = true) {
        if (uiState.mostrarDialogoSinVidas) {
            android.util.Log.w("QuizScreen", "Back button bloqueado: Dialogo de sin vidas activo")
            return@BackHandler
        }

        if (uiState.finalizando) {
            android.util.Log.w("QuizScreen", "Bloqueada salida durante finalizacion")
        } else if (uiState.quizActivo != null) {
            mostrarDialogoSalir = true
        } else {
            onRegresarATemasDelCurso()
        }
    }

    LaunchedEffect(uiState.resultadoQuiz) {
        if (uiState.resultadoQuiz != null && !yaCargoRetroalimentacion) {
            yaCargoRetroalimentacion = true
            val quizId = uiState.quizActivo?.quizId

            if (quizId != null && (uiState.resultadoQuiz?.preguntasIncorrectas ?: 0) > 0) {
                quizViewModel.obtenerRetroalimentacion(quizId)
                kotlinx.coroutines.delay(500)
            }

            if (!yaNavego) {
                yaNavego = true
                onNavigateToResultado()
            }
        }
    }

    if (uiState.mostrarDialogoPeriodoFinalizado) {
        DialogoPeriodoFinalizado(
            mensajeError = uiState.mensajeErrorDetallado,
            temaTitulo = temaTitulo,
            onAceptar = {
                quizViewModel.cerrarDialogoPeriodoFinalizado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoErrorGeneral) {
        DialogoErrorGeneral(
            titulo = uiState.tituloError,
            mensaje = uiState.mensajeErrorDetallado,
            onAceptar = {
                quizViewModel.cerrarDialogoErrorGeneral()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoTemaAprobado) {
        DialogoTemaYaAprobado(
            onContinuar = {
                quizViewModel.forzarInicioQuiz(cursoId, temaId, "practica")
            },
            onCancelar = {
                quizViewModel.cerrarDialogoTemaAprobado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoQuizFinalCompletado) {
        DialogoQuizFinalCompletado(
            onAceptar = {
                quizViewModel.cerrarDialogoQuizFinalCompletado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (mostrarDialogoSalir) {
        DialogoAbandonarQuizConBloqueo(
            onConfirmar = {
                mostrarDialogoSalir = false
                onRegresarATemasDelCurso()
            },
            onCancelar = { mostrarDialogoSalir = false }
        )
    }

    if (uiState.mostrarDialogoSinVidas && uiState.quizActivo != null) {
        DialogoSinVidasDuranteQuizMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = {
                android.util.Log.d("QuizScreen", "Usuario cerro dialogo con boton Cerrar")
                android.util.Log.d("QuizScreen", "Marcando quiz como abandonado")

                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            },
            onVolverATemas = {
                android.util.Log.d("QuizScreen", "Usuario presiono Volver a Temas")
                android.util.Log.d("QuizScreen", "Marcando quiz como abandonado")

                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            }
        )
    }

    val colorModo = when (modo) {
        "practica" -> Color(0xFF9C27B0)
        "final" -> Color(0xFFFFB300)
        else -> EduRachaColors.Primary
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.quizActivo == null -> {
                PantallaCargaQuiz(colorModo = colorModo)
            }

            uiState.finalizando -> {
                PantallaFinalizando()
            }

            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    key(uiState.preguntaActual) {
                        PreguntaScreen(
                            pregunta = preguntaActual,
                            numeroPregunta = uiState.preguntaActual + 1,
                            totalPreguntas = quiz.preguntas.size,
                            colorModo = colorModo,
                            temaTitulo = temaTitulo,
                            modo = modo,
                            tiempoTotalQuiz = tiempoTotalQuiz,
                            sinVidas = uiState.sinVidas,
                            respuestasEstado = uiState.respuestasEstado,
                            ultimaRespuestaCorrecta = uiState.ultimaRespuestaCorrecta,
                            mostrarAnimacionRespuesta = uiState.mostrarAnimacionRespuesta,
                            onRespuestaSeleccionada = { opcionId ->
                                if (!uiState.sinVidas) {
                                    quizViewModel.responderPregunta(
                                        preguntaId = preguntaActual.id,
                                        respuestaSeleccionada = opcionId
                                    )
                                } else {
                                    android.util.Log.w("QuizScreen", "Intento de responder sin vidas - BLOQUEADO")
                                }
                            },
                            onAnimacionCompletada = {
                                quizViewModel.ocultarAnimacionRespuesta()

                                quizViewModel.avanzarSiguientePregunta()

                                if (uiState.preguntaActual + 1 >= quiz.preguntas.size) {
                                    quizViewModel.finalizarQuiz()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoSinVidasDuranteQuizMejorado(
    minutosParaProxima: Int,
    onDismiss: () -> Unit,
    onVolverATemas: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.HeartBroken,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = "Te quedaste sin vidas",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (22 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFFF4B4B),
                lineHeight = (28 * scale.text).sp,
                modifier = Modifier.padding(horizontal = (8 * scale.padding).dp)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = (4 * scale.padding).dp)
            ) {
                Text(
                    "El quiz ha sido bloqueado porque te has quedado sin vidas disponibles",
                    fontSize = (15 * scale.text).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (22 * scale.text).sp
                )

                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((16 * scale.padding).dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size((28 * scale.icon).dp)
                        )
                        Spacer(Modifier.width((10 * scale.padding).dp))
                        Column {
                            Text(
                                text = "Próxima vida en:",
                                fontSize = (12 * scale.text).sp,
                                color = Color(0xFF1CB0F6),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$minutosParaProxima minutos",
                                fontSize = (20 * scale.text).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1CB0F6)
                            )
                        }
                    }
                }

                Text(
                    "Vuelve cuando tengas vidas disponibles para continuar aprendiendo",
                    fontSize = (13 * scale.text).sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (19 * scale.text).sp
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy((9 * scale.padding).dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (4 * scale.padding).dp)
            ) {
                Button(
                    onClick = onVolverATemas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale.padding).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape((14 * scale.padding).dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((7 * scale.padding).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size((20 * scale.icon).dp)
                        )
                        Text(
                            "Volver a Temas del Curso",
                            fontSize = (15 * scale.text).sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((48 * scale.padding).dp),
                    border = androidx.compose.foundation.BorderStroke(
                        (2 * scale.padding).dp,
                        Color(0xFFFF4B4B)
                    ),
                    shape = RoundedCornerShape((14 * scale.padding).dp)
                ) {
                    Text(
                        "Cerrar",
                        fontSize = (14 * scale.text).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4B4B)
                    )
                }
            }
        }
    )
}

@Composable
fun PantallaCargaQuiz(colorModo: Color) {
    val scale = rememberResponsiveScaling()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorModo.copy(alpha = 0.15f),
                        EduRachaColors.Background,
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((24 * scale.padding).dp),
            modifier = Modifier.padding((24 * scale.padding).dp)
        ) {
            LoadingAnimation(colorModo = colorModo)

            Text(
                text = "Preparando tu quiz...",
                fontSize = (20 * scale.text).sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cargando las mejores preguntas para ti",
                fontSize = (14 * scale.text).sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingAnimation(colorModo: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            drawCircle(
                color = colorModo.copy(alpha = 0.2f),
                radius = radius,
                center = Offset(centerX, centerY)
            )

            drawArc(
                color = colorModo,
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                size = size
            )
        }

        Icon(
            Icons.Default.School,
            contentDescription = null,
            tint = colorModo,
            modifier = Modifier
                .size(46.dp)
                .scale(scale)
        )
    }
}

@Composable
fun PantallaFinalizando() {
    val scale = rememberResponsiveScaling()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Success.copy(alpha = 0.15f),
                        EduRachaColors.Background,
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((24 * scale.padding).dp),
            modifier = Modifier.padding((24 * scale.padding).dp)
        ) {
            FinalizandoAnimation()

            Text(
                text = "Finalizando quiz...",
                fontSize = (22 * scale.text).sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Calculando tu puntuación y recompensas",
                fontSize = (15 * scale.text).sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FinalizandoAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "finalizando")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = EduRachaColors.Success.copy(alpha = 0.2f),
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
        ) {}

        Surface(
            shape = CircleShape,
            color = EduRachaColors.Success.copy(alpha = 0.3f),
            modifier = Modifier.size(100.dp)
        ) {}

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EduRachaColors.Success,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
fun DialogoPeriodoFinalizado(
    mensajeError: String,
    temaTitulo: String,
    onAceptar: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = "Periodo Finalizado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (20 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (26 * scale.text).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "El periodo de este tema ya finalizó",
                    fontSize = (15 * scale.text).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    textAlign = TextAlign.Center,
                    lineHeight = (22 * scale.text).sp
                )

                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((18 * scale.padding).dp),
                        verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size((24 * scale.icon).dp)
                            )
                            Column {
                                Text(
                                    "Tema:",
                                    fontSize = (12 * scale.text).sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    temaTitulo,
                                    fontSize = (14 * scale.text).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFFF9800).copy(alpha = 0.3f),
                            thickness = (1 * scale.padding).dp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size((24 * scale.icon).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((5 * scale.padding).dp)
                            ) {
                                Text(
                                    "¿Qué significa esto?",
                                    fontSize = (14 * scale.text).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Este tema tiene un periodo de disponibilidad que ya ha terminado",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = (19 * scale.text).sp
                                )
                                Text(
                                    "Ya no puedes realizar quizzes para este tema",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = (19 * scale.text).sp
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape((12 * scale.padding).dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding((12 * scale.padding).dp),
                        horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size((20 * scale.icon).dp)
                        )
                        Text(
                            "Consulta con tu profesor sobre otros temas disponibles",
                            fontSize = (12 * scale.text).sp,
                            color = Color(0xFF1CB0F6),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (17 * scale.text).sp
                        )
                    }
                }

                if (mensajeError.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape((10 * scale.padding).dp),
                        color = EduRachaColors.TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding((10 * scale.padding).dp),
                            horizontalArrangement = Arrangement.spacedBy((6 * scale.padding).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = EduRachaColors.TextSecondary,
                                modifier = Modifier.size((16 * scale.icon).dp)
                            )
                            Text(
                                text = mensajeError.replace("{\"error\":\"", "")
                                    .replace("\"}", "")
                                    .replace("\\", ""),
                                fontSize = (11 * scale.text).sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = (14 * scale.text).sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((50 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Volver a Temas",
                        fontSize = (15 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

@Composable
fun DialogoErrorGeneral(
    titulo: String,
    mensaje: String,
    onAceptar: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = titulo.ifEmpty { "Error" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = (20 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (26 * scale.text).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((12 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = Color(0xFFFF4B4B).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((16 * scale.padding).dp),
                        verticalArrangement = Arrangement.spacedBy((10 * scale.padding).dp)
                    ) {
                        Text(
                            text = mensaje.replace("{\"error\":\"", "")
                                .replace("\"}", "")
                                .replace("\\", ""),
                            fontSize = (14 * scale.text).sp,
                            fontWeight = FontWeight.Medium,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = (20 * scale.text).sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((50 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4B4B)
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Regresar",
                        fontSize = (15 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

@Composable
fun DialogoTemaYaAprobado(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = "Tema ya aprobado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (20 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (26 * scale.text).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has aprobado este tema con éxito",
                    fontSize = (14 * scale.text).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (20 * scale.text).sp
                )

                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((18 * scale.padding).dp),
                        verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size((24 * scale.icon).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((5 * scale.padding).dp)
                            ) {
                                Text(
                                    "Si deseas seguir practicando:",
                                    fontSize = (14 * scale.text).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Podrás hacer el quiz en modo práctica",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Pierdes vidas",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Seguirás ganando experiencia",
                                    fontSize = (13 * scale.text).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinuar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((50 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Seguir practicando",
                        fontSize = (14 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((50 * scale.padding).dp),
                border = androidx.compose.foundation.BorderStroke((2 * scale.padding).dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Text(
                    "Regresar",
                    fontSize = (15 * scale.text).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    )
}

@Composable
fun DialogoQuizFinalCompletado(
    onAceptar: () -> Unit
) {
    val scale = rememberResponsiveScaling()

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((24 * scale.padding).dp),
        icon = {
            Box(
                modifier = Modifier.size((90 * scale.icon).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFB300).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size((50 * scale.icon).dp)
                )
            }
        },
        title = {
            Text(
                text = "Quiz Final Completado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (20 * scale.text).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (26 * scale.text).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * scale.padding).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has completado exitosamente el Quiz Final de este curso",
                    fontSize = (14 * scale.text).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (20 * scale.text).sp
                )

                Surface(
                    shape = RoundedCornerShape((16 * scale.padding).dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((18 * scale.padding).dp),
                        verticalArrangement = Arrangement.spacedBy((14 * scale.padding).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale.padding).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size((24 * scale.icon).dp)
                            )
                            Text(
                                "Felicidades por tu logro",
                                fontSize = (14 * scale.text).sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Text(
                            "El Quiz Final solo puede realizarse una vez por curso. Si deseas mejorar tu conocimiento, puedes practicar en los temas individuales.",
                            fontSize = (13 * scale.text).sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = (19 * scale.text).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((50 * scale.padding).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300)
                ),
                shape = RoundedCornerShape((14 * scale.padding).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scale.padding).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size((20 * scale.icon).dp)
                    )
                    Text(
                        "Entendido",
                        fontSize = (15 * scale.text).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}