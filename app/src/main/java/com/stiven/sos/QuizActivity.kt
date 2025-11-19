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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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

@Composable
fun CardPreguntaMejorada(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    colorModo: Color
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((24 * paddingScale).dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = (8 * paddingScale).dp)
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
                .padding((28 * paddingScale).dp),
            verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((16 * paddingScale).dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorModo.copy(alpha = 0.15f),
                    modifier = Modifier.size((54 * paddingScale).dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$numeroPregunta",
                            fontSize = (22 * paddingScale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorModo
                        )
                    }
                }
                Text(
                    text = "Pregunta",
                    fontSize = (20 * paddingScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorModo
                )
            }

            Divider(
                color = colorModo.copy(alpha = 0.2f),
                thickness = (2 * paddingScale).dp
            )

            Text(
                text = pregunta.texto,
                fontSize = (19 * paddingScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary,
                lineHeight = (28 * paddingScale).sp
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) (12 * paddingScale).dp else (4 * paddingScale).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape((20 * paddingScale).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorModo.copy(alpha = 0.12f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) (3 * paddingScale).dp else (2 * paddingScale).dp,
            color = if (isSelected) colorModo else EduRachaColors.Border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((20 * paddingScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((16 * paddingScale).dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) colorModo.copy(alpha = 0.2f) else EduRachaColors.Border.copy(alpha = 0.3f),
                modifier = Modifier.size((42 * paddingScale).dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorModo,
                            modifier = Modifier.size((26 * paddingScale).dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size((26 * paddingScale).dp)
                        )
                    }
                }
            }

            Text(
                text = opcion.texto,
                fontSize = (17 * paddingScale).sp,
                color = if (isSelected) colorModo else EduRachaColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                lineHeight = (25 * paddingScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height((68 * paddingScale).dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorModo,
            disabledContainerColor = EduRachaColors.TextSecondary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape((20 * paddingScale).dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = (8 * paddingScale).dp,
            pressedElevation = (12 * paddingScale).dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (esUltimaPregunta) "Finalizar Quiz" else "Siguiente",
                fontSize = (19 * paddingScale).sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                if (esUltimaPregunta) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size((28 * paddingScale).dp)
            )
        }
    }
}

@Composable
fun DialogoAbandonarQuizConBloqueo(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * paddingScale).dp),
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
                    modifier = Modifier.size((56 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Salida bloqueada durante el quiz",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (24 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (30 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Por politica de integridad academica, no puedes salir de la aplicacion mientras resuelves un quiz.",
                    fontSize = (16 * paddingScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (24 * paddingScale).sp
                )

                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((24 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((18 * paddingScale).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size((28 * paddingScale).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((8 * paddingScale).dp)
                            ) {
                                Text(
                                    "Si decides abandonar:",
                                    fontSize = (16 * paddingScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Perderas 1 vida",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Perderas tu progreso actual",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Text(
                    "Te recomendamos continuar y dar lo mejor de ti",
                    fontSize = (15 * paddingScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.Primary,
                    textAlign = TextAlign.Center,
                    lineHeight = (22 * paddingScale).sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Abandonar de todas formas",
                        fontSize = (16 * paddingScale).sp,
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
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Continuar quiz",
                        fontSize = (17 * paddingScale).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

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
    onRespuestaSeleccionada: (Int) -> Unit
) {
    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    var mostrarAnimacionEstrella by remember(pregunta.id) { mutableStateOf(false) }

    LaunchedEffect(sinVidas) {
        if (sinVidas) {
            android.util.Log.w("PreguntaScreen", "Deteccion: Usuario sin vidas")
            android.util.Log.w("PreguntaScreen", "Cancelando animacion y limpiando seleccion")
            mostrarAnimacionEstrella = false
            respuestaSeleccionada = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            HeaderQuizMejorado(
                temaTitulo = temaTitulo,
                modo = modo,
                colorModo = colorModo,
                tiempoTranscurrido = tiempoTotalQuiz
            )

            BarraProgresoConEstrellasIluminadas(
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                colorModo = colorModo,
                respuestaSeleccionada = respuestaSeleccionada != null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
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

                Spacer(Modifier.height(20.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                BotonConfirmarMejorado(
                    enabled = respuestaSeleccionada != null && !sinVidas,
                    colorModo = colorModo,
                    esUltimaPregunta = numeroPregunta >= totalPreguntas,
                    onClick = {
                        if (!sinVidas) {
                            respuestaSeleccionada?.let {
                                mostrarAnimacionEstrella = true
                            }
                        } else {
                            android.util.Log.w("PreguntaScreen", "Click bloqueado: Sin vidas")
                        }
                    },
                    modifier = Modifier.padding(20.dp)
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
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Quiz Bloqueado",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF4B4B)
                        )
                        Text(
                            "Te has quedado sin vidas",
                            fontSize = 16.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (mostrarAnimacionEstrella && !sinVidas) {
        AnimacionEstrellaExitosa(
            colorModo = colorModo,
            onAnimacionCompleta = {
                if (!sinVidas) {
                    mostrarAnimacionEstrella = false
                    respuestaSeleccionada?.let { opcionSeleccionada ->
                        onRespuestaSeleccionada(opcionSeleccionada)
                    }
                } else {
                    mostrarAnimacionEstrella = false
                    android.util.Log.w("PreguntaScreen", "Animacion cancelada: Vidas agotadas")
                }
            }
        )
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = (4 * paddingScale).dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (20 * paddingScale).dp, vertical = (16 * paddingScale).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = temaTitulo,
                    fontSize = (18 * paddingScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape((8 * paddingScale).dp),
                    color = colorModo.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (modo) {
                            "practica" -> "Modo Practica"
                            "final" -> "Quiz Final"
                            else -> "Modo Oficial"
                        },
                        color = colorModo,
                        fontSize = (12 * paddingScale).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = (10 * paddingScale).dp, vertical = (4 * paddingScale).dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy((8 * paddingScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconoRelojPulsante()

                Text(
                    text = formatearTiempo(tiempoTranscurrido),
                    fontSize = (20 * paddingScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
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
        tint = Color(0xFF4CAF50),
        modifier = Modifier
            .size(24.dp)
            .scale(escala)
    )
}

@Composable
fun BarraProgresoConEstrellasIluminadas(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    respuestaSeleccionada: Boolean
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

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
            modifier = Modifier.padding(horizontal = (20 * paddingScale).dp, vertical = (12 * paddingScale).dp),
            verticalArrangement = Arrangement.spacedBy((12 * paddingScale).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((12 * paddingScale).dp)
                    .clip(RoundedCornerShape((6 * paddingScale).dp))
                    .background(colorModo.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progresoAnimado)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape((6 * paddingScale).dp))
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
                    val estrellaCompletada = i <= preguntaActual
                    EstrellaIluminada(
                        completada = estrellaCompletada,
                        estaActiva = i == preguntaActual && respuestaSeleccionada
                    )
                }
            }
        }
    }
}

@Composable
fun EstrellaIluminada(
    completada: Boolean,
    estaActiva: Boolean
) {
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
        targetValue = if (completada) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escala"
    )

    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        if (completada && estaActiva) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * brillo
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }

        Icon(
            if (completada) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = if (completada) Color(0xFFFFD700) else EduRachaColors.TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier
                .size(24.dp)
                .scale(escala)
        )
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
    var mostrarDialogoInfoEstrellas by remember { mutableStateOf(true) }

    var tiempoTotalQuiz by remember { mutableStateOf(0) }

    LaunchedEffect(mostrarDialogoInfoEstrellas) {
        if (!mostrarDialogoInfoEstrellas) {
            quizViewModel.iniciarObservadores(cursoId, temaId)
            quizViewModel.iniciarQuiz(cursoId, temaId, modo)
        }
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

    if (mostrarDialogoInfoEstrellas) {
        DialogoInfoEstrellas(
            onAceptar = {
                mostrarDialogoInfoEstrellas = false
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
            !mostrarDialogoInfoEstrellas && uiState.isLoading && uiState.quizActivo == null -> {
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
                            onRespuestaSeleccionada = { opcionId ->
                                if (!uiState.sinVidas) {
                                    quizViewModel.responderPregunta(
                                        preguntaId = preguntaActual.id,
                                        respuestaSeleccionada = opcionId
                                    )

                                    if (uiState.preguntaActual + 1 >= quiz.preguntas.size) {
                                        quizViewModel.finalizarQuiz()
                                    }
                                } else {
                                    android.util.Log.w("QuizScreen", "Intento de responder sin vidas - BLOQUEADO")
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp

    val paddingScale = if (isTablet) 1.5f else 1f
    val textScale = if (isTablet) 1.2f else 1f
    val iconScale = if (isTablet) 1.3f else 1f

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * iconScale).dp),
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
                    modifier = Modifier.size((56 * iconScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Te quedaste sin vidas",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (26 * textScale).sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFFF4B4B),
                lineHeight = (32 * textScale).sp,
                modifier = Modifier.padding(horizontal = (8 * paddingScale).dp)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = (4 * paddingScale).dp)
            ) {
                Text(
                    "El quiz ha sido bloqueado porque te has quedado sin vidas disponibles",
                    fontSize = (17 * textScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (24 * textScale).sp
                )

                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((20 * paddingScale).dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size((32 * iconScale).dp)
                        )
                        Spacer(Modifier.width((12 * paddingScale).dp))
                        Column {
                            Text(
                                text = "Proxima vida en:",
                                fontSize = (14 * textScale).sp,
                                color = Color(0xFF1CB0F6),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$minutosParaProxima minutos",
                                fontSize = (24 * textScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1CB0F6)
                            )
                        }
                    }
                }

                Text(
                    "Vuelve cuando tengas vidas disponibles para continuar aprendiendo",
                    fontSize = (15 * textScale).sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (22 * textScale).sp
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy((12 * paddingScale).dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (4 * paddingScale).dp)
            ) {
                Button(
                    onClick = onVolverATemas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((60 * paddingScale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape((18 * paddingScale).dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size((24 * iconScale).dp)
                        )
                        Text(
                            "Volver a Temas del Curso",
                            fontSize = (18 * textScale).sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((56 * paddingScale).dp),
                    border = androidx.compose.foundation.BorderStroke(
                        (2 * paddingScale).dp,
                        Color(0xFFFF4B4B)
                    ),
                    shape = RoundedCornerShape((18 * paddingScale).dp)
                ) {
                    Text(
                        "Cerrar",
                        fontSize = (16 * textScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

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
            verticalArrangement = Arrangement.spacedBy((32 * paddingScale).dp),
            modifier = Modifier.padding((32 * paddingScale).dp)
        ) {
            LoadingAnimation(colorModo = colorModo)

            Text(
                text = "Preparando tu quiz...",
                fontSize = (24 * paddingScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cargando las mejores preguntas para ti",
                fontSize = (16 * paddingScale).sp,
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
        modifier = Modifier.size(120.dp),
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
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                size = size
            )
        }

        Icon(
            Icons.Default.School,
            contentDescription = null,
            tint = colorModo,
            modifier = Modifier
                .size(50.dp)
                .scale(scale)
        )
    }
}

@Composable
fun PantallaFinalizando() {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

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
            verticalArrangement = Arrangement.spacedBy((32 * paddingScale).dp),
            modifier = Modifier.padding((32 * paddingScale).dp)
        ) {
            FinalizandoAnimation()

            Text(
                text = "Finalizando quiz...",
                fontSize = (26 * paddingScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Calculando tu puntuacion y recompensas",
                fontSize = (17 * paddingScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * paddingScale).dp),
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
                    modifier = Modifier.size((56 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Periodo Finalizado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (26 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (32 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "El periodo de este tema ya finalizo",
                    fontSize = (18 * paddingScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    textAlign = TextAlign.Center,
                    lineHeight = (26 * paddingScale).sp
                )

                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((24 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((18 * paddingScale).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size((28 * paddingScale).dp)
                            )
                            Column {
                                Text(
                                    "Tema:",
                                    fontSize = (14 * paddingScale).sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    temaTitulo,
                                    fontSize = (16 * paddingScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFFF9800).copy(alpha = 0.3f),
                            thickness = (1 * paddingScale).dp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size((28 * paddingScale).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((8 * paddingScale).dp)
                            ) {
                                Text(
                                    "Que significa esto?",
                                    fontSize = (16 * paddingScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Este tema tiene un periodo de disponibilidad que ya ha terminado",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = (22 * paddingScale).sp
                                )
                                Text(
                                    "Ya no puedes realizar quizzes para este tema",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = (22 * paddingScale).sp
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape((16 * paddingScale).dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding((16 * paddingScale).dp),
                        horizontalArrangement = Arrangement.spacedBy((12 * paddingScale).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size((24 * paddingScale).dp)
                        )
                        Text(
                            "Consulta con tu profesor sobre otros temas disponibles",
                            fontSize = (14 * paddingScale).sp,
                            color = Color(0xFF1CB0F6),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (20 * paddingScale).sp
                        )
                    }
                }

                if (mensajeError.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape((12 * paddingScale).dp),
                        color = EduRachaColors.TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding((12 * paddingScale).dp),
                            horizontalArrangement = Arrangement.spacedBy((8 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = EduRachaColors.TextSecondary,
                                modifier = Modifier.size((18 * paddingScale).dp)
                            )
                            Text(
                                text = mensajeError.replace("{\"error\":\"", "")
                                    .replace("\"}", "")
                                    .replace("\\", ""),
                                fontSize = (12 * paddingScale).sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = (16 * paddingScale).sp
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
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Volver a Temas",
                        fontSize = (18 * paddingScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * paddingScale).dp),
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
                    modifier = Modifier.size((56 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = titulo.ifEmpty { "Error" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = (24 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (30 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = Color(0xFFFF4B4B).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((20 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((12 * paddingScale).dp)
                    ) {
                        Text(
                            text = mensaje.replace("{\"error\":\"", "")
                                .replace("\"}", "")
                                .replace("\\", ""),
                            fontSize = (16 * paddingScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = (24 * paddingScale).sp
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
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4B4B)
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Regresar",
                        fontSize = (18 * paddingScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * paddingScale).dp),
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
                    modifier = Modifier.size((56 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Tema ya aprobado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (24 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (30 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has aprobado este tema con exito",
                    fontSize = (16 * paddingScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (24 * paddingScale).sp
                )

                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((24 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((18 * paddingScale).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size((28 * paddingScale).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((8 * paddingScale).dp)
                            ) {
                                Text(
                                    "Si deseas seguir practicando:",
                                    fontSize = (16 * paddingScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Podras hacer el quiz en modo practica",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "No perderas vidas",
                                    fontSize = (15 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Seguiras ganando experiencia",
                                    fontSize = (15 * paddingScale).sp,
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
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Seguir practicando",
                        fontSize = (16 * paddingScale).sp,
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
                    .height((60 * paddingScale).dp),
                border = androidx.compose.foundation.BorderStroke((2 * paddingScale).dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Text(
                    "Regresar",
                    fontSize = (17 * paddingScale).sp,
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((100 * paddingScale).dp),
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
                    modifier = Modifier.size((56 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Quiz Final Completado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (24 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (30 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has completado exitosamente el Quiz Final de este curso",
                    fontSize = (16 * paddingScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = (24 * paddingScale).sp
                )

                Surface(
                    shape = RoundedCornerShape((20 * paddingScale).dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((24 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((18 * paddingScale).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size((28 * paddingScale).dp)
                            )
                            Text(
                                "Felicidades por tu logro",
                                fontSize = (16 * paddingScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Text(
                            "El Quiz Final solo puede realizarse una vez por curso. Si deseas mejorar tu conocimiento, puedes practicar en los temas individuales.",
                            fontSize = (15 * paddingScale).sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = (22 * paddingScale).sp,
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
                    .height((60 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300)
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((10 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size((24 * paddingScale).dp)
                    )
                    Text(
                        "Entendido",
                        fontSize = (17 * paddingScale).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

@Composable
fun DialogoInfoEstrellas(
    onAceptar: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val paddingScale = if (isTablet) 1.5f else 1f

    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape((28 * paddingScale).dp),
        icon = {
            Box(
                modifier = Modifier.size((120 * paddingScale).dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "estrella_info")
                val escala by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "escala"
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(escala)
                ) {}

                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size((70 * paddingScale).dp)
                )
            }
        },
        title = {
            Text(
                text = "Informacion Importante",
                fontWeight = FontWeight.ExtraBold,
                fontSize = (26 * paddingScale).sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = (32 * paddingScale).sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((24 * paddingScale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape((24 * paddingScale).dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding((24 * paddingScale).dp),
                        verticalArrangement = Arrangement.spacedBy((20 * paddingScale).dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size((32 * paddingScale).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((8 * paddingScale).dp)
                            ) {
                                Text(
                                    "Que significan las estrellas?",
                                    fontSize = (18 * paddingScale).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Las estrellas solo indican la cantidad de preguntas que has respondido",
                                    fontSize = (16 * paddingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = (24 * paddingScale).sp
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFFFD700).copy(alpha = 0.3f),
                            thickness = (1 * paddingScale).dp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy((14 * paddingScale).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1CB0F6),
                                modifier = Modifier.size((32 * paddingScale).dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy((10 * paddingScale).dp)
                            ) {
                                Text(
                                    "Importante:",
                                    fontSize = (18 * paddingScale).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1CB0F6)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((8 * paddingScale).dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = (20 * paddingScale).sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Las estrellas NO indican si tu respuesta es correcta o incorrecta",
                                        fontSize = (15 * paddingScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = (22 * paddingScale).sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((8 * paddingScale).dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = (20 * paddingScale).sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Solo muestran tu progreso en el quiz",
                                        fontSize = (15 * paddingScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = (22 * paddingScale).sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((8 * paddingScale).dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = (20 * paddingScale).sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Veras tus respuestas correctas e incorrectas al finalizar el quiz",
                                        fontSize = (15 * paddingScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = (22 * paddingScale).sp
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape((16 * paddingScale).dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding((16 * paddingScale).dp),
                        horizontalArrangement = Arrangement.spacedBy((12 * paddingScale).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size((28 * paddingScale).dp)
                        )
                        Text(
                            "Confia en tus conocimientos y da lo mejor de ti",
                            fontSize = (15 * paddingScale).sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            lineHeight = (22 * paddingScale).sp
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
                    .height((64 * paddingScale).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                shape = RoundedCornerShape((18 * paddingScale).dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = (6 * paddingScale).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((12 * paddingScale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size((26 * paddingScale).dp)
                    )
                    Text(
                        "Entendido, comenzar quiz",
                        fontSize = (18 * paddingScale).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}
