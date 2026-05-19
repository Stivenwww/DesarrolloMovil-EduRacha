package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.services.VentanaRachaDuolingo
import com.stiven.sos.services.VentanaRachaPerdida
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * ========================================
 * ACTIVITY PRINCIPAL DE RESULTADOS V2
 * ========================================
 */
class ResultadoQuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()

    private var preguntasCorrectas: Int = 0
    private var preguntasIncorrectas: Int = 0
    private var experienciaGanada: Int = 0
    private var vidasRestantes: Int = 0
    private var bonificacionRapidez: Int = 0
    private var bonificacionPrimeraVez: Int = 0
    private var bonificacionTodoCorrecto: Int = 0
    private var quizId: String = ""
    private lateinit var modo: String
    private lateinit var cursoId: String
    private lateinit var temaId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preguntasCorrectas = intent.getIntExtra("preguntasCorrectas", 0)
        preguntasIncorrectas = intent.getIntExtra("preguntasIncorrectas", 0)
        experienciaGanada = intent.getIntExtra("experienciaGanada", 0)
        vidasRestantes = intent.getIntExtra("vidasRestantes", 0)
        bonificacionRapidez = intent.getIntExtra("bonificacionRapidez", 0)
        bonificacionPrimeraVez = intent.getIntExtra("bonificacionPrimeraVez", 0)
        bonificacionTodoCorrecto = intent.getIntExtra("bonificacionTodoCorrecto", 0)
        quizId = intent.getStringExtra("quizId") ?: ""
        modo = intent.getStringExtra("modo") ?: "oficial"

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        cursoId = prefs.getString("last_curso_id", "") ?: ""
        temaId = prefs.getString("last_tema_id", "") ?: ""

        setContent {
            EduRachaTheme {
                ResultadoQuizScreenV2(
                    preguntasCorrectas = preguntasCorrectas,
                    preguntasIncorrectas = preguntasIncorrectas,
                    experienciaGanada = experienciaGanada,
                    vidasRestantes = vidasRestantes,
                    bonificacionRapidez = bonificacionRapidez,
                    bonificacionPrimeraVez = bonificacionPrimeraVez,
                    bonificacionTodoCorrecto = bonificacionTodoCorrecto,
                    quizId = quizId,
                    modo = modo,
                    cursoId = cursoId,
                    temaId = temaId,
                    quizViewModel = quizViewModel,
                    onVerRetroalimentacion = {
                        val intent = Intent(this, RetroalimentacionActivity::class.java)
                        intent.putExtra("quizId", quizId)
                        startActivity(intent)
                    },
                    onVolverACursos = { finish() },
                    onIniciarPractica = {
                        val intent = Intent(this, ExplicacionTemaActivity::class.java)
                        intent.putExtra("curso_id", cursoId)
                        intent.putExtra("tema_id", temaId)
                        intent.putExtra("tema_titulo", "Tema")
                        intent.putExtra("tema_explicacion", "Explicación")
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float
)

/**
 * ========================================
 * PANTALLA PRINCIPAL DE RESULTADOS V2
 * ========================================
 */
@Composable
fun ResultadoQuizScreenV2(
    preguntasCorrectas: Int,
    preguntasIncorrectas: Int,
    experienciaGanada: Int,
    vidasRestantes: Int,
    bonificacionRapidez: Int,
    bonificacionPrimeraVez: Int,
    bonificacionTodoCorrecto: Int,
    quizId: String,
    modo: String,
    cursoId: String,
    temaId: String,
    quizViewModel: QuizViewModel,
    onVerRetroalimentacion: () -> Unit,
    onVolverACursos: () -> Unit,
    onIniciarPractica: () -> Unit
) {
    val totalPreguntas = preguntasCorrectas + preguntasIncorrectas
    val porcentaje = if (totalPreguntas > 0) (preguntasCorrectas * 100) / totalPreguntas else 0
    val aprobo = porcentaje >= 80

    var mostrarVentanaRacha by remember { mutableStateOf(false) }
    var mostrarVentanaRachaPerdida by remember { mutableStateOf(false) }
    var mostrarConfetti by remember { mutableStateOf(false) }
    var diasRacha by remember { mutableStateOf(0) }
    var rachaSubida by remember { mutableStateOf(false) }

    val uiState by quizViewModel.uiState.collectAsState()

    // Determinar gradiente y mensaje según resultado
    val (gradiente, mensaje, emoji) = when {
        porcentaje >= 90 -> Triple(
            EduRachaV2Gradients.Green,
            "¡EXCELENTE TRABAJO!",
            "🎉"
        )
        porcentaje >= 80 -> Triple(
            EduRachaV2Gradients.Purple,
            "¡MUY BIEN HECHO!",
            "🌟"
        )
        porcentaje >= 70 -> Triple(
            EduRachaV2Gradients.Yellow,
            "¡APROBADO!",
            "👏"
        )
        else -> Triple(
            EduRachaV2Gradients.Pink,
            "SIGUE INTENTÁNDOLO",
            "💪"
        )
    }

    val colorResultado = when {
        porcentaje >= 90 -> EduRachaV2Colors.Success
        porcentaje >= 80 -> EduRachaV2Colors.Secondary
        porcentaje >= 70 -> EduRachaV2Colors.Accent
        else -> EduRachaV2Colors.Pink
    }

    LaunchedEffect(Unit) {
        if (cursoId.isNotEmpty()) {
            quizViewModel.iniciarObservadores(cursoId, temaId)
        }
    }

    LaunchedEffect(uiState.progreso) {
        uiState.progreso?.let {
            diasRacha = it.rachaDias
        }
    }

    LaunchedEffect(aprobo, modo) {
        if (aprobo && modo == "oficial" && cursoId.isNotEmpty() && !rachaSubida) {
            rachaSubida = true
            quizViewModel.actualizarRacha(cursoId, temaId)
            delay(500)
            quizViewModel.iniciarObservadores(cursoId, temaId)
            delay(400)
            mostrarConfetti = true
            delay(800)
            mostrarVentanaRacha = true
            delay(4000)
            mostrarVentanaRacha = false
            delay(500)
            mostrarConfetti = false
        } else if (!aprobo && modo == "oficial") {
            delay(600)
            mostrarVentanaRachaPerdida = true
            delay(3500)
            mostrarVentanaRachaPerdida = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mostrarVentanaRacha && diasRacha > 0) {
            VentanaRachaDuolingoV2(
                diasRacha = diasRacha,
                onDismiss = { mostrarVentanaRacha = false }
            )
        }

        if (mostrarVentanaRachaPerdida) {
            VentanaRachaPerdidaV2(
                porcentaje = porcentaje,
                onDismiss = { mostrarVentanaRachaPerdida = false }
            )
        }

        // Todo hace scroll incluyendo el header
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(EduRachaV2Colors.Background),
            contentPadding = PaddingValues(bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con celebración como primer item
            item {
                HeaderCelebracionV2(
                    gradiente = gradiente,
                    mensaje = mensaje,
                    emoji = emoji,
                    porcentaje = porcentaje
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TarjetaCirculoProgresoV2(
                        porcentaje = porcentaje,
                        preguntasCorrectas = preguntasCorrectas,
                        totalPreguntas = totalPreguntas,
                        colorResultado = colorResultado
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaEstadisticaCompactaV2(
                        icon = Icons.Outlined.CheckCircle,
                        valor = "$preguntasCorrectas",
                        label = "Correctas",
                        color = EduRachaV2Colors.Success,
                        modifier = Modifier.weight(1f)
                    )

                    TarjetaEstadisticaCompactaV2(
                        icon = Icons.Outlined.Cancel,
                        valor = "$preguntasIncorrectas",
                        label = "Incorrectas",
                        color = EduRachaV2Colors.Pink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TarjetaExperienciaV2(
                        experienciaGanada = experienciaGanada,
                        bonificacionRapidez = bonificacionRapidez,
                        bonificacionPrimeraVez = bonificacionPrimeraVez,
                        bonificacionTodoCorrecto = bonificacionTodoCorrecto
                    )
                }
            }

            if (diasRacha > 0) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TarjetaRachaV2(
                            diasRacha = diasRacha,
                            aprobo = aprobo
                        )
                    }
                }
            }

            if (modo == "oficial") {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EduRachaV2VidasCard(
                            vidasActuales = vidasRestantes,
                            vidasMax = 5,
                            minutosParaProxima = 30
                        )
                    }
                }
            }

            if (aprobo && modo == "oficial") {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TarjetaModoPracticaV2(
                            onIniciarPractica = onIniciarPractica
                        )
                    }
                }
            }

            // Botones de acción - sin padding horizontal para que ocupen todo el ancho
            item {
                BotonesAccionV2(
                    preguntasIncorrectas = preguntasIncorrectas,
                    onVerRetroalimentacion = onVerRetroalimentacion,
                    onVolverACursos = onVolverACursos
                )
            }
        }

        // Confetti sobre todo
        if (mostrarConfetti) {
            ConfettiAnimacionV2()
        }
    }
}

/**
 * ========================================
 * HEADER DE CELEBRACIÓN V2
 * ========================================
 */
@Composable
fun HeaderCelebracionV2(
    gradiente: Brush,
    mensaje: String,
    emoji: String,
    porcentaje: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    gradiente,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            // Burbujas decorativas animadas (limitadas al header)
            Box(modifier = Modifier.matchParentSize()) {
                AnimatedBubblesDecoration(color = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emoji animado grande
                EmojiAnimadoGrande(emoji)

                // Mensaje principal
                Text(
                    text = mensaje,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                // Píldora con mensaje motivador
                PildoraMensajeMotivador(porcentaje)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun EmojiAnimadoGrande(emoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "emoji")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Text(
        text = emoji,
        fontSize = 64.sp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
    )
}

@Composable
fun PildoraMensajeMotivador(porcentaje: Int) {
    val mensaje = when {
        porcentaje >= 90 -> "¡Eres increíble! Sigue así"
        porcentaje >= 80 -> "Gran trabajo, lo estás logrando"
        porcentaje >= 70 -> "Buen esfuerzo, sigue mejorando"
        else -> "No te rindas, tú puedes"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.25f)
    ) {
        Text(
            text = mensaje,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

/**
 * ========================================
 * TARJETA CÍRCULO DE PROGRESO V2
 * ========================================
 */
@Composable
fun TarjetaCirculoProgresoV2(
    porcentaje: Int,
    preguntasCorrectas: Int,
    totalPreguntas: Int,
    colorResultado: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            CirculoProgresoV2(
                porcentaje = porcentaje,
                colorResultado = colorResultado
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$preguntasCorrectas de $totalPreguntas",
                    fontSize = 20.sp,
                    color = EduRachaV2Colors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "respuestas correctas",
                    fontSize = 14.sp,
                    color = EduRachaV2Colors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun CirculoProgresoV2(
    porcentaje: Int,
    colorResultado: Color
) {
    val porcentajeAnimado by animateFloatAsState(
        targetValue = porcentaje.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "porcentaje"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Fondo del círculo
            drawCircle(
                color = EduRachaV2Colors.SoftGray.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Arco de progreso con gradiente
            drawArc(
                color = colorResultado,
                startAngle = -90f,
                sweepAngle = (porcentajeAnimado / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = size
            )
        }

        // Porcentaje animado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${porcentajeAnimado.toInt()}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = colorResultado
            )
        }
    }
}

/**
 * ========================================
 * TARJETAS DE ESTADÍSTICAS COMPACTAS V2
 * ========================================
 */
@Composable
fun TarjetaEstadisticaCompactaV2(
    icon: ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.12f),
                            Color.White
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = valor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = color
            )

            Text(
                text = label,
                fontSize = 13.sp,
                color = EduRachaV2Colors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * ========================================
 * TARJETA DE EXPERIENCIA V2
 * ========================================
 */
@Composable
fun TarjetaExperienciaV2(
    experienciaGanada: Int,
    bonificacionRapidez: Int,
    bonificacionPrimeraVez: Int,
    bonificacionTodoCorrecto: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EduRachaV2Gradients.Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Experiencia Ganada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary
                )
            }

            // XP Total
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EduRachaV2Colors.Accent.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "star")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = EduRachaV2Colors.Accent,
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                        Text(
                            text = "Total XP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaV2Colors.TextPrimary
                        )
                    }
                    Text(
                        text = "+$experienciaGanada",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = EduRachaV2Colors.Accent
                    )
                }
            }

            // Bonificaciones
            if (bonificacionRapidez > 0 || bonificacionPrimeraVez > 0 || bonificacionTodoCorrecto > 0) {
                Divider(
                    color = EduRachaV2Colors.SoftGray.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                Text(
                    text = "🎁 Bonificaciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (bonificacionRapidez > 0) {
                        FilaBonificacionV2(
                            icono = Icons.Outlined.Speed,
                            texto = "Velocidad relámpago",
                            puntos = bonificacionRapidez,
                            color = EduRachaV2Colors.Primary
                        )
                    }
                    if (bonificacionPrimeraVez > 0) {
                        FilaBonificacionV2(
                            icono = Icons.Outlined.Celebration,
                            texto = "Primera vez perfecto",
                            puntos = bonificacionPrimeraVez,
                            color = EduRachaV2Colors.Secondary
                        )
                    }
                    if (bonificacionTodoCorrecto > 0) {
                        FilaBonificacionV2(
                            icono = Icons.Outlined.EmojiEvents,
                            texto = "100% sin errores",
                            puntos = bonificacionTodoCorrecto,
                            color = EduRachaV2Colors.Accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilaBonificacionV2(
    icono: ImageVector,
    texto: String,
    puntos: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
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
                Icon(
                    icono,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = texto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaV2Colors.TextPrimary
                )
            }
            Text(
                text = "+$puntos XP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

/**
 * ========================================
 * TARJETA DE RACHA V2
 * ========================================
 */
@Composable
fun TarjetaRachaV2(
    diasRacha: Int,
    aprobo: Boolean
) {
    val gradiente = if (aprobo) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFF6B35),
                Color(0xFFFF9600)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                EduRachaV2Colors.SoftGray,
                EduRachaV2Colors.Background
            )
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = if (aprobo) 6.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradiente)
        ) {
            // Burbujas decorativas
            if (aprobo) {
                AnimatedBubblesDecoration(color = Color.White)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconoLlamaAnimadoV2(aprobo = aprobo)

                    Column {
                        Text(
                            text = "🔥 Racha actual",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aprobo) Color.White else EduRachaV2Colors.TextPrimary
                        )
                        Text(
                            text = if (aprobo) "¡Racha aumentada!" else "Mantén tu racha",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (aprobo) Color.White.copy(0.9f) else EduRachaV2Colors.TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (aprobo) Color.White.copy(alpha = 0.25f) else Color.White
                ) {
                    Text(
                        text = "$diasRacha días",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (aprobo) Color.White else Color(0xFFFF9600),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IconoLlamaAnimadoV2(aprobo: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "llama")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (aprobo) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (aprobo) rotation else 0f
            }
            .clip(CircleShape)
            .background(if (aprobo) Color.White.copy(alpha = 0.2f) else EduRachaV2Colors.SoftGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔥",
            fontSize = 28.sp
        )
    }
}

/**
 * ========================================
 * TARJETA MODO PRÁCTICA V2
 * ========================================
 */
@Composable
fun TarjetaModoPracticaV2(
    onIniciarPractica: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EduRachaV2Gradients.Purple)
        ) {
            AnimatedBubblesDecoration(color = Color.White)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💪",
                            fontSize = 32.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Modo Práctica",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "¡Desbloqueado!",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Sigue practicando este tema para dominar todos los conceptos y ganar más experiencia",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )

                EduRachaV2Button(
                    text = "Seguir Practicando",
                    onClick = onIniciarPractica,
                    icon = Icons.Outlined.PlayArrow,
                    variant = EduRachaV2ButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * ========================================
 * BOTONES DE ACCIÓN V2
 * ========================================
 */
@Composable
fun BotonesAccionV2(
    preguntasIncorrectas: Int,
    onVerRetroalimentacion: () -> Unit,
    onVolverACursos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (preguntasIncorrectas > 0) {
            Button(
                onClick = onVerRetroalimentacion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            EduRachaV2Gradients.Blue,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = Color.White
                        )
                        Text(
                            "Ver Retroalimentación",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Button(
            onClick = onVolverACursos,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 3.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        EduRachaV2Gradients.Green,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                    Text(
                        "CONTINUAR",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

/**
 * ========================================
 * VENTANAS EMERGENTES V2
 * ========================================
 */
@Composable
fun VentanaRachaDuolingoV2(
    diasRacha: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF6B35),
                                Color(0xFFFF9600)
                            )
                        )
                    )
            ) {
                AnimatedBubblesDecoration(color = Color.White)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 80.sp,
                        modifier = Modifier.scale(1.2f)
                    )

                    Text(
                        text = "¡RACHA AUMENTADA!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "$diasRacha días consecutivos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }

                    Text(
                        text = "¡Sigue así! 🎉",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun VentanaRachaPerdidaV2(
    porcentaje: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💪",
                        fontSize = 48.sp
                    )
                }

                Text(
                    text = "¡Sigue Intentándolo!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = EduRachaV2Colors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaV2Colors.Warning.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tu puntuación: $porcentaje%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaV2Colors.TextPrimary
                        )
                        Text(
                            text = "Necesitas 80% para aprobar",
                            fontSize = 14.sp,
                            color = EduRachaV2Colors.TextSecondary
                        )
                    }
                }

                Text(
                    text = "Tu racha se mantiene. ¡No te rindas!",
                    fontSize = 14.sp,
                    color = EduRachaV2Colors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * ========================================
 * ANIMACIÓN DE CONFETTI V2
 * ========================================
 */
@Composable
fun ConfettiAnimacionV2() {
    val colores = listOf(
        EduRachaV2Colors.Success,
        EduRachaV2Colors.Primary,
        EduRachaV2Colors.Accent,
        EduRachaV2Colors.Secondary,
        EduRachaV2Colors.Pink,
        EduRachaV2Colors.Warning
    )

    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat() * 1200,
                y = -Random.nextFloat() * 1000,
                color = colores.random(),
                size = Random.nextFloat() * 14 + 8,
                velocityX = Random.nextFloat() * 5 - 2.5f,
                velocityY = Random.nextFloat() * 7 + 4,
                rotation = Random.nextFloat() * 360,
                rotationSpeed = Random.nextFloat() * 10 + 5
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            particle.y += particle.velocityY
            particle.x += particle.velocityX
            particle.rotation += particle.rotationSpeed

            if (particle.y > size.height + 150) {
                particle.y = -150f
                particle.x = Random.nextFloat() * size.width
            }

            if (particle.x < -100) particle.x = size.width + 100
            if (particle.x > size.width + 100) particle.x = -100f

            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(particle.x, particle.y),
                alpha = 0.9f
            )
        }
    }
}