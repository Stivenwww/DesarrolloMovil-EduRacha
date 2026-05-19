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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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

// ============================================================================
// HEADER FIGMA - Vidas, Racha, XP y Progreso con Flecha de Regreso
// ============================================================================

@Composable
fun HeaderQuizFigma(
    vidasActuales: Int,
    vidasMax: Int,
    rachaActual: Int,
    xpActual: Int,
    preguntaActual: Int,
    totalPreguntas: Int,
    onBackClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(
                horizontal = if (isTablet) 32.dp else 20.dp,
                vertical = if (isTablet) 20.dp else 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 14.dp)
    ) {
        // Fila superior: Flecha atrás, Vidas, Racha y XP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flecha de regreso
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(if (isTablet) 44.dp else 40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF7F9FC))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFF3C79F5),
                    modifier = Modifier.size(if (isTablet) 26.dp else 24.dp)
                )
            }

            // Vidas (corazones rosas)
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(vidasMax) { index ->
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (index < vidasActuales) Color(0xFFFF7096) else Color(0xFFEFEFEF),
                        modifier = Modifier.size(if (isTablet) 36.dp else 32.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Racha (fuego naranja)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFF9800),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 16.dp else 14.dp,
                        vertical = if (isTablet) 10.dp else 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Whatshot,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Text(
                        "$rachaActual",
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // XP (estrella amarilla)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFC864),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 16.dp else 14.dp,
                        vertical = if (isTablet) 10.dp else 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                    Text(
                        "$xpActual",
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        // Barra de progreso azul
        Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 10.dp else 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 12.dp else 10.dp)
                    .clip(RoundedCornerShape(if (isTablet) 6.dp else 5.dp))
                    .background(Color(0xFFE8F5FE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((preguntaActual.toFloat() / totalPreguntas.toFloat()))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(if (isTablet) 6.dp else 5.dp))
                        .background(Color(0xFF3C79F5))
                )
            }

            Text(
                "Pregunta $preguntaActual de $totalPreguntas",
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF717182),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// IDENTIFICADOR GAMIFICADO DE MODO
// ============================================================================

@Composable
fun IdentificadorModoQuiz(
    modo: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    data class ModoConfig(
        val colorFondo: Color,
        val colorTexto: Color,
        val icono: ImageVector,
        val titulo: String
    )

    val config = when (modo) {
        "practica" -> ModoConfig(
            colorFondo = Color(0xFF9C27B0),
            colorTexto = Color.White,
            icono = Icons.Default.Lightbulb,
            titulo = "MODO PRÁCTICA"
        )
        "final" -> ModoConfig(
            colorFondo = Color(0xFFFFB300),
            colorTexto = Color.White,
            icono = Icons.Default.EmojiEvents,
            titulo = "QUIZ FINAL"
        )
        else -> ModoConfig(
            colorFondo = Color(0xFF3C79F5),
            colorTexto = Color.White,
            icono = Icons.Default.Quiz,
            titulo = "QUIZ OFICIAL"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "modo_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .padding(horizontal = if (isTablet) 24.dp else 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = config.colorFondo,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTablet) 20.dp else 16.dp,
                    vertical = if (isTablet) 14.dp else 12.dp
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                config.icono,
                contentDescription = null,
                tint = config.colorTexto,
                modifier = Modifier.size(if (isTablet) 26.dp else 22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                config.titulo,
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = config.colorTexto,
                letterSpacing = 1.sp
            )
        }
    }
}

// ============================================================================
// MASCOTA BÚHO ANIMADA (3 Estados: Pensativo, Feliz, Triste)
// ============================================================================

@Composable
fun BuhoMascotaAnimada(
    estado: EstadoBuho,
    mostrarAnimacion: Boolean
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val buhoSize = if (isTablet) 220.dp else 180.dp

    val infiniteTransition = rememberInfiniteTransition(label = "buho")

    // Flotación suave
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isTablet) -15f else -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )

    // Rotación suave
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "rotate"
    )

    // Escala animada para respuestas
    val scale by animateFloatAsState(
        targetValue = if (mostrarAnimacion) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .size(buhoSize)
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        // Confeti animado para respuesta correcta
        if (mostrarAnimacion && estado == EstadoBuho.FELIZ) {
            ConfetiAnimado(isTablet)
        }

        // Signos de interrogación animados para estado pensativo
        if (estado == EstadoBuho.PENSATIVO) {
            SignosInterrogacionAnimados(isTablet)
        }

        // Búho principal
        when (estado) {
            EstadoBuho.PENSATIVO -> {
                // Búho pensativo (amarillo/naranja)
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .rotate(rotation),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "🦉",
                        fontSize = if (isTablet) 140.sp else 110.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFB300).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Pensando...",
                            fontSize = if (isTablet) 14.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            EstadoBuho.FELIZ -> {
                // Búho feliz (verde)
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .rotate(rotation),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Efectos de brillo
                    Box(
                        modifier = Modifier.size(if (isTablet) 150.dp else 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Círculo de brillo
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF58A700).copy(alpha = 0.2f),
                                radius = size.minDimension / 2
                            )
                        }
                        Text(
                            "🦉",
                            fontSize = if (isTablet) 140.sp else 110.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF58A700).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "✨",
                                fontSize = if (isTablet) 14.sp else 12.sp
                            )
                            Text(
                                "Correcto!",
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF58A700)
                            )
                        }
                    }
                }
            }
            EstadoBuho.TRISTE -> {
                // Búho triste (rosa/rojo)
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .rotate(rotation),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "😢",
                        fontSize = if (isTablet) 140.sp else 110.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFF4B4B).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "💪",
                                fontSize = if (isTablet) 14.sp else 12.sp
                            )
                            Text(
                                "Sigue intentando!",
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF4B4B)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class EstadoBuho {
    PENSATIVO, FELIZ, TRISTE
}

@Composable
fun ConfetiAnimado(isTablet: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "confeti")

    val confetis = listOf(
        "🎉" to Offset(-40f, -60f),
        "✨" to Offset(40f, -50f),
        "🌟" to Offset(-50f, 20f),
        "💫" to Offset(50f, 30f),
        "⭐" to Offset(0f, -70f)
    )

    confetis.forEachIndexed { index, (emoji, offset) ->
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((1000 + index * 200), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "confeti_rotation_$index"
        )

        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "confeti_scale_$index"
        )

        Text(
            emoji,
            fontSize = if (isTablet) 32.sp else 28.sp,
            modifier = Modifier
                .offset(x = offset.x.dp, y = offset.y.dp)
                .scale(scale)
                .rotate(rotation)
        )
    }
}

@Composable
fun SignosInterrogacionAnimados(isTablet: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "signos")

    val signos = listOf(
        Offset(-50f, -40f),
        Offset(50f, -30f),
        Offset(-40f, 30f),
        Offset(45f, 35f)
    )

    signos.forEachIndexed { index, offset ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -15f,
            animationSpec = infiniteRepeatable(
                animation = tween((1200 + index * 150), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "signo_offset_$index"
        )

        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "signo_alpha_$index"
        )

        Text(
            "❓",
            fontSize = if (isTablet) 28.sp else 24.sp,
            modifier = Modifier
                .offset(x = offset.x.dp, y = (offset.y + offsetY).dp)
                .alpha(alpha)
        )
    }
}

// ============================================================================
// FEEDBACK MESSAGE FIGMA
// ============================================================================

@Composable
fun MensajeFeedbackFigma(esCorrecta: Boolean?, xpGanado: Int) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    AnimatedVisibility(
        visible = esCorrecta != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 20.dp),
            shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
            color = if (esCorrecta == true) Color(0xFFD7FFD8) else Color(0xFFFFDFE0),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isTablet) 24.dp else 20.dp,
                    vertical = if (isTablet) 18.dp else 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (esCorrecta == true) "¡Excelente! ✨" else "¡Ups! Sigue intentando 💪",
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (esCorrecta == true) Color(0xFF58A700) else Color(0xFFFF4B4B),
                    modifier = Modifier.weight(1f)
                )
                if (esCorrecta == true && xpGanado > 0) {
                    Text(
                        "+$xpGanado XP",
                        fontSize = if (isTablet) 17.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF58A700)
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREGUNTA CARD FIGMA - Diseño exacto de Figma
// ============================================================================

@Composable
fun PreguntaCardFigma(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 20.dp),
        shape = RoundedCornerShape(if (isTablet) 24.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 24.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 20.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número de pregunta (círculo azul) - exacto como Figma
            Surface(
                shape = CircleShape,
                color = Color(0xFF3C79F5),
                modifier = Modifier.size(if (isTablet) 60.dp else 52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$numeroPregunta",
                        fontSize = if (isTablet) 26.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Texto de la pregunta - más grande
            Text(
                pregunta.texto,
                fontSize = if (isTablet) 20.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                lineHeight = if (isTablet) 28.sp else 25.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// OPCIÓN RESPUESTA FIGMA - Diseño exacto de Figma
// ============================================================================

@Composable
fun OpcionRespuestaFigma(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    isSelected: Boolean,
    esRespuestaCorrecta: Boolean?,
    esRespuestaIncorrecta: Boolean?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val backgroundColor = when {
        esRespuestaCorrecta == true -> Color(0xFFD7FFD8)
        esRespuestaIncorrecta == true -> Color(0xFFFFDFE0)
        else -> Color.White
    }

    val borderColor = when {
        esRespuestaCorrecta == true -> Color(0xFF58A700)
        esRespuestaIncorrecta == true -> Color(0xFFFF4B4B)
        isSelected -> Color(0xFF3C79F5)
        else -> Color(0xFFE5E5E5)
    }

    val borderWidth = when {
        esRespuestaCorrecta == true || esRespuestaIncorrecta == true -> 3.dp
        isSelected -> 3.dp
        else -> 2.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 24.dp else 20.dp,
                vertical = if (isTablet) 10.dp else 8.dp
            ),
        shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTablet) 24.dp else 20.dp,
                    vertical = if (isTablet) 22.dp else 18.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                opcion.texto,
                fontSize = if (isTablet) 18.sp else 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3C3C43),
                modifier = Modifier.weight(1f),
                lineHeight = if (isTablet) 26.sp else 24.sp
            )

            when {
                esRespuestaCorrecta == true -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF58A700),
                        modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                    )
                }
                esRespuestaIncorrecta == true -> {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFFF4B4B),
                        modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// BOTÓN SIGUIENTE FIGMA
// ============================================================================

@Composable
fun BotonSiguienteFigma(
    enabled: Boolean,
    esUltimaPregunta: Boolean,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 62.dp else 56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3C79F5),
            disabledContainerColor = Color(0xFFE5E5E5)
        ),
        shape = RoundedCornerShape(if (isTablet) 18.dp else 16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Text(
            if (esUltimaPregunta) "Finalizar Quiz" else "Siguiente pregunta",
            fontSize = if (isTablet) 19.sp else 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Color.White else Color(0xFF9E9E9E)
        )
    }
}

// ============================================================================
// PANTALLA DE PREGUNTA FIGMA
// ============================================================================

@Composable
fun PreguntaScreenFigma(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    temaTitulo: String,
    modo: String,
    sinVidas: Boolean,
    respuestasEstado: Map<Int, Boolean>,
    ultimaRespuestaCorrecta: Boolean?,
    mostrarAnimacionRespuesta: Boolean,
    vidasActuales: Int,
    vidasMax: Int,
    rachaActual: Int,
    xpActual: Int,
    onRespuestaSeleccionada: (Int) -> Unit,
    onAnimacionCompletada: () -> Unit,
    onBackClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    var mostrarExplicacion by remember(pregunta.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Estado del búho
    val estadoBuho = when {
        mostrarAnimacionRespuesta && ultimaRespuestaCorrecta == true -> EstadoBuho.FELIZ
        mostrarAnimacionRespuesta && ultimaRespuestaCorrecta == false -> EstadoBuho.TRISTE
        else -> EstadoBuho.PENSATIVO
    }

    LaunchedEffect(sinVidas) {
        if (sinVidas) {
            android.util.Log.w("PreguntaScreen", "Detección: Usuario sin vidas")
            android.util.Log.w("PreguntaScreen", "Cancelando animación y limpiando selección")
            respuestaSeleccionada = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
        ) {
            // Header con vidas, racha, XP, progreso y flecha de regreso
            HeaderQuizFigma(
                vidasActuales = vidasActuales,
                vidasMax = vidasMax,
                rachaActual = rachaActual,
                xpActual = xpActual,
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = if (isTablet) 28.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 20.dp)
            ) {
                // Identificador del modo de quiz
                IdentificadorModoQuiz(modo = modo)

                // Mascota búho animada
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BuhoMascotaAnimada(
                        estado = estadoBuho,
                        mostrarAnimacion = mostrarAnimacionRespuesta
                    )
                }

                // Mensaje de feedback
                MensajeFeedbackFigma(
                    esCorrecta = ultimaRespuestaCorrecta,
                    xpGanado = 50
                )

                // Card de pregunta
                PreguntaCardFigma(
                    pregunta = pregunta,
                    numeroPregunta = numeroPregunta
                )

                // Opciones de respuesta
                pregunta.opciones.forEachIndexed { index, opcion ->
                    val esCorrecta = mostrarExplicacion && respuestaSeleccionada != null &&
                            ultimaRespuestaCorrecta == true && respuestaSeleccionada == index
                    val esIncorrecta = mostrarExplicacion && respuestaSeleccionada != null &&
                            ultimaRespuestaCorrecta == false && respuestaSeleccionada == index

                    OpcionRespuestaFigma(
                        opcion = opcion,
                        isSelected = respuestaSeleccionada == index,
                        esRespuestaCorrecta = esCorrecta,
                        esRespuestaIncorrecta = esIncorrecta,
                        enabled = !sinVidas && !mostrarExplicacion,
                        onClick = {
                            if (!sinVidas && !mostrarExplicacion) {
                                respuestaSeleccionada = index
                            } else {
                                android.util.Log.w("PreguntaScreen", "Intento bloqueado: Sin vidas")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(if (isTablet) 120.dp else 100.dp))
            }

            // Botón siguiente en la parte inferior
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(if (isTablet) 24.dp else 20.dp)) {
                    BotonSiguienteFigma(
                        enabled = respuestaSeleccionada != null && !sinVidas,
                        esUltimaPregunta = numeroPregunta >= totalPreguntas,
                        onClick = {
                            if (!sinVidas && respuestaSeleccionada != null) {
                                android.util.Log.d("PreguntaScreen", "Respuesta confirmada: índice $respuestaSeleccionada")
                                mostrarExplicacion = true
                                onRespuestaSeleccionada(respuestaSeleccionada!!)
                            } else {
                                android.util.Log.w("PreguntaScreen", "Click bloqueado: Sin vidas")
                            }
                        }
                    )
                }
            }
        }

        // Overlay de sin vidas
        if (sinVidas) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
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
                            color = Color(0xFF717182),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Animaciones de respuesta
    if (mostrarAnimacionRespuesta && !sinVidas) {
        LaunchedEffect(Unit) {
            delay(2000)
            onAnimacionCompletada()
        }
    }
}

// ============================================================================
// QUIZ SCREEN PRINCIPAL
// ============================================================================

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

    // Todos los diálogos (mantienen funcionalidad original)
    if (uiState.mostrarDialogoPeriodoFinalizado) {
        DialogoPeriodoFinalizadoFigma(
            mensajeError = uiState.mensajeErrorDetallado,
            temaTitulo = temaTitulo,
            onAceptar = {
                quizViewModel.cerrarDialogoPeriodoFinalizado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoErrorGeneral) {
        DialogoErrorGeneralFigma(
            titulo = uiState.tituloError,
            mensaje = uiState.mensajeErrorDetallado,
            onAceptar = {
                quizViewModel.cerrarDialogoErrorGeneral()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoTemaAprobado) {
        DialogoTemaYaAprobadoFigma(
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
        DialogoQuizFinalCompletadoFigma(
            onAceptar = {
                quizViewModel.cerrarDialogoQuizFinalCompletado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (mostrarDialogoSalir) {
        DialogoAbandonarQuizFigma(
            onConfirmar = {
                mostrarDialogoSalir = false
                onRegresarATemasDelCurso()
            },
            onCancelar = { mostrarDialogoSalir = false }
        )
    }

    if (uiState.mostrarDialogoSinVidas && uiState.quizActivo != null) {
        DialogoSinVidasDuranteQuizFigma(
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
        else -> Color(0xFF3C79F5)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.quizActivo == null -> {
                PantallaCargaQuizFigma(colorModo = colorModo)
            }

            uiState.finalizando -> {
                PantallaFinalizandoFigma()
            }

            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    key(uiState.preguntaActual) {
                        PreguntaScreenFigma(
                            pregunta = preguntaActual,
                            numeroPregunta = uiState.preguntaActual + 1,
                            totalPreguntas = quiz.preguntas.size,
                            temaTitulo = temaTitulo,
                            modo = modo,
                            sinVidas = uiState.sinVidas,
                            respuestasEstado = uiState.respuestasEstado,
                            ultimaRespuestaCorrecta = uiState.ultimaRespuestaCorrecta,
                            mostrarAnimacionRespuesta = uiState.mostrarAnimacionRespuesta,
                            vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                            vidasMax = uiState.vidas?.vidasMax ?: 5,
                            rachaActual = uiState.progreso?.rachaDias ?: 0,
                            xpActual = uiState.progreso?.experiencia ?: 0,
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
                            },
                            onBackClick = {
                                mostrarDialogoSalir = true
                            }
                        )
                    }
                }
            }
        }
    }
}


// ============================================================================
// DIÁLOGO SIN VIDAS FIGMA
// ============================================================================

@Composable
fun DialogoSinVidasDuranteQuizFigma(
    minutosParaProxima: Int,
    onDismiss: () -> Unit,
    onVolverATemas: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF7096).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.HeartBroken,
                    contentDescription = null,
                    tint = Color(0xFFFF7096),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Te quedaste sin vidas",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFFF4B4B)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "El quiz ha sido bloqueado porque te has quedado sin vidas disponibles",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3C3C43),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF3C79F5).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF3C79F5),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Próxima vida en:",
                                fontSize = 13.sp,
                                color = Color(0xFF3C79F5),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$minutosParaProxima minutos",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3C79F5)
                            )
                        }
                    }
                }

                Text(
                    "Vuelve cuando tengas vidas disponibles para continuar aprendiendo",
                    fontSize = 13.sp,
                    color = Color(0xFF717182),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onVolverATemas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3C79F5)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Volver a Temas del Curso",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF4B4B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Cerrar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4B4B)
                    )
                }
            }
        }
    )
}

// ============================================================================
// DIÁLOGO ABANDONAR QUIZ FIGMA
// ============================================================================

@Composable
fun DialogoAbandonarQuizFigma(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "¿Salir del quiz?",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E),
                lineHeight = 28.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Si abandonas el quiz ahora, perderás todo tu progreso actual.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF717182),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    "Si decides abandonar:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )

                                Text(
                                    "• Perderás las respuestas correctas\n• No ganarás experiencia\n• Tendrás que empezar de nuevo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1C1C1E),
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    "Te recomendamos continuar y dar lo mejor de ti",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3C79F5),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCancelar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3C79F5)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Continuar quiz",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onConfirmar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF4B4B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Abandonar de todas formas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4B4B)
                        )
                    }
                }
            }
        }
    )
}

// ============================================================================
// PANTALLAS DE CARGA Y FINALIZANDO
// ============================================================================

@Composable
fun PantallaCargaQuizFigma(colorModo: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = colorModo,
                strokeWidth = 6.dp
            )

            Text(
                text = "Preparando tu quiz...",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cargando las mejores preguntas para ti",
                fontSize = 15.sp,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PantallaFinalizandoFigma() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF58A700),
                strokeWidth = 6.dp
            )

            Text(
                text = "Finalizando quiz...",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Calculando tu puntuación y recompensas",
                fontSize = 16.sp,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// DIÁLOGOS ADICIONALES
// ============================================================================

@Composable
fun DialogoPeriodoFinalizadoFigma(
    mensajeError: String,
    temaTitulo: String,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
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
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Periodo Finalizado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E)
            )
        },
        text = {
            Text(
                text = "El periodo de este tema ya finalizó. Ya no puedes realizar quizzes para este tema.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Volver a Temas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    )
}

@Composable
fun DialogoErrorGeneralFigma(
    titulo: String,
    mensaje: String,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
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
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = titulo.ifEmpty { "Error" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E)
            )
        },
        text = {
            Text(
                text = mensaje,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4B4B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Regresar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    )
}

@Composable
fun DialogoTemaYaAprobadoFigma(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF58A700).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF58A700),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Tema ya aprobado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E)
            )
        },
        text = {
            Text(
                "Ya has aprobado este tema. Puedes seguir practicando en modo práctica.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onContinuar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF58A700)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Seguir practicando",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF58A700)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Regresar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF58A700)
                    )
                }
            }
        }
    )
}

@Composable
fun DialogoQuizFinalCompletadoFigma(
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFC864).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFC864),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Quiz Final Completado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1C1E)
            )
        },
        text = {
            Text(
                "Ya has completado exitosamente el Quiz Final de este curso",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC864)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Entendido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    )
}