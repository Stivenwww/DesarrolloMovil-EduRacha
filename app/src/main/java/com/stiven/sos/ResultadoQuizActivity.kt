package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stiven.sos.services.VentanaRachaDuolingo
import com.stiven.sos.services.VentanaRachaPerdida
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * ========================================
 * ACTIVITY PRINCIPAL DE RESULTADOS
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
                ResultadoQuizScreen(
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
 * PANTALLA PRINCIPAL DE RESULTADOS
 * ========================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoQuizScreen(
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
            VentanaRachaDuolingo(
                diasRacha = diasRacha,
                onDismiss = { mostrarVentanaRacha = false }
            )
        }

        if (mostrarVentanaRachaPerdida) {
            VentanaRachaPerdida(
                porcentaje = porcentaje,
                onDismiss = { mostrarVentanaRachaPerdida = false }
            )
        }

        val colorResultado = when {
            porcentaje >= 90 -> Color(0xFF58CC02)
            porcentaje >= 70 -> Color(0xFFFFC800)
            else -> Color(0xFFFF4B4B)
        }

        val mensajeResultado = when {
            porcentaje >= 90 -> "EXCELENTE"
            porcentaje >= 80 -> "MUY BIEN"
            porcentaje >= 70 -> "APROBADO"
            else -> "SIGUE PRACTICANDO"
        }

        Column(modifier = Modifier.fillMaxSize()) {
            HeaderResultadoDuolingo(
                colorResultado = colorResultado,
                mensajeResultado = mensajeResultado,
                porcentaje = porcentaje
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F7F7)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TarjetaCirculoProgreso(
                            porcentaje = porcentaje,
                            preguntasCorrectas = preguntasCorrectas,
                            totalPreguntas = totalPreguntas,
                            colorResultado = colorResultado
                        )
                    }

                    item {
                        TarjetaEstadisticasDuolingo(
                            preguntasCorrectas = preguntasCorrectas,
                            preguntasIncorrectas = preguntasIncorrectas
                        )
                    }

                    item {
                        TarjetaExperienciaDuolingo(
                            experienciaGanada = experienciaGanada,
                            bonificacionRapidez = bonificacionRapidez,
                            bonificacionPrimeraVez = bonificacionPrimeraVez,
                            bonificacionTodoCorrecto = bonificacionTodoCorrecto
                        )
                    }

                    if (diasRacha > 0) {
                        item {
                            TarjetaRachaActual(
                                diasRacha = diasRacha,
                                aprobo = aprobo
                            )
                        }
                    }

                    if (modo == "oficial") {
                        item {
                            TarjetaVidasDuolingo(
                                vidasRestantes = vidasRestantes
                            )
                        }
                    }

                    if (aprobo && modo == "oficial") {
                        item {
                            TarjetaModoPracticaDuolingo(
                                onIniciarPractica = onIniciarPractica
                            )
                        }
                    }

                    item {
                        BotonesAccionDuolingo(
                            preguntasIncorrectas = preguntasIncorrectas,
                            onVerRetroalimentacion = onVerRetroalimentacion,
                            onVolverACursos = onVolverACursos
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }

                if (mostrarConfetti) {
                    ConfettiAnimacionDuolingo()
                }
            }
        }
    }
}

@Composable
fun HeaderResultadoDuolingo(
    colorResultado: Color,
    mensajeResultado: String,
    porcentaje: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorResultado,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (porcentaje >= 80)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = mensajeResultado,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TarjetaCirculoProgreso(
    porcentaje: Int,
    preguntasCorrectas: Int,
    totalPreguntas: Int,
    colorResultado: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CirculoProgresoDuolingo(
                porcentaje = porcentaje,
                colorResultado = colorResultado
            )

            Text(
                text = "$preguntasCorrectas de $totalPreguntas correctas",
                fontSize = 18.sp,
                color = Color(0xFF4B4B4B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CirculoProgresoDuolingo(
    porcentaje: Int,
    colorResultado: Color
) {
    val porcentajeAnimado by animateFloatAsState(
        targetValue = porcentaje.toFloat(),
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "porcentaje"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            drawCircle(
                color = Color(0xFFE5E5E5),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = colorResultado,
                startAngle = -90f,
                sweepAngle = (porcentajeAnimado / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = size
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${porcentajeAnimado.toInt()}%",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = colorResultado
            )
        }
    }
}

@Composable
fun TarjetaEstadisticasDuolingo(
    preguntasCorrectas: Int,
    preguntasIncorrectas: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Resumen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3C3C3C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = Color(0xFF58CC02).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF58CC02),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "$preguntasCorrectas",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF58CC02)
                    )
                    Text(
                        text = "Correctas",
                        fontSize = 14.sp,
                        color = Color(0xFF4B4B4B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = Color(0xFFFF4B4B).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFFF4B4B),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "$preguntasIncorrectas",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF4B4B)
                    )
                    Text(
                        text = "Incorrectas",
                        fontSize = 14.sp,
                        color = Color(0xFF4B4B4B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaExperienciaDuolingo(
    experienciaGanada: Int,
    bonificacionRapidez: Int,
    bonificacionPrimeraVez: Int,
    bonificacionTodoCorrecto: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Experiencia Ganada",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3C3C3C)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFC800).copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC800),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Total XP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3C3C3C)
                        )
                    }
                    Text(
                        text = "+$experienciaGanada",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC800)
                    )
                }
            }

            if (bonificacionRapidez > 0 || bonificacionPrimeraVez > 0 || bonificacionTodoCorrecto > 0) {
                Divider(color = Color(0xFFE5E5E5))

                Text(
                    text = "Bonificaciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B4B4B)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bonificacionRapidez > 0) {
                        FilaBonificacion(
                            icono = Icons.Default.Speed,
                            texto = "Velocidad",
                            puntos = bonificacionRapidez,
                            color = Color(0xFF1CB0F6)
                        )
                    }
                    if (bonificacionPrimeraVez > 0) {
                        FilaBonificacion(
                            icono = Icons.Default.Celebration,
                            texto = "Primera vez",
                            puntos = bonificacionPrimeraVez,
                            color = Color(0xFFCE82FF)
                        )
                    }
                    if (bonificacionTodoCorrecto > 0) {
                        FilaBonificacion(
                            icono = Icons.Default.EmojiEvents,
                            texto = "Perfecto",
                            puntos = bonificacionTodoCorrecto,
                            color = Color(0xFFFFC800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilaBonificacion(
    icono: ImageVector,
    texto: String,
    puntos: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
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
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = texto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3C3C3C)
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

@Composable
fun TarjetaRachaActual(
    diasRacha: Int,
    aprobo: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (aprobo) Color(0xFFFF9600) else Color(0xFFE5E5E5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                IconoLlamaAnimado(aprobo = aprobo)

                Column {
                    Text(
                        text = "Racha actual",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (aprobo) Color.White else Color(0xFF4B4B4B)
                    )
                    Text(
                        text = if (aprobo) "Subiste tu racha" else "Mantén tu racha",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (aprobo) Color.White.copy(0.9f) else Color(0xFF777777)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (aprobo) Color.White.copy(alpha = 0.25f) else Color.White
            ) {
                Text(
                    text = "$diasRacha días",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (aprobo) Color.White else Color(0xFFFF9600),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun IconoLlamaAnimado(aprobo: Boolean) {
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

    Icon(
        Icons.Default.Whatshot,
        contentDescription = null,
        tint = if (aprobo) Color.White else Color(0xFF777777),
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
    )
}

@Composable
fun TarjetaVidasDuolingo(
    vidasRestantes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "Vidas restantes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3C3C3C)
                    )
                    Text(
                        text = "Regeneración cada 30 min",
                        fontSize = 13.sp,
                        color = Color(0xFF777777)
                    )
                }
            }

            Text(
                text = "$vidasRestantes / 5",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF4B4B)
            )
        }
    }
}

@Composable
fun TarjetaModoPracticaDuolingo(
    onIniciarPractica: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCE82FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = "Modo Práctica Desbloqueado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Text(
                text = "Sigue practicando para ganar más experiencia ",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )

            Button(
                onClick = onIniciarPractica,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Text(
                    "Seguir Practicando",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFCE82FF)
                )
            }
        }
    }
}

@Composable
fun BotonesAccionDuolingo(
    preguntasIncorrectas: Int,
    onVerRetroalimentacion: () -> Unit,
    onVolverACursos: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (preguntasIncorrectas > 0) {
            Button(
                onClick = onVerRetroalimentacion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1CB0F6)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Ver Retroalimentación",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Button(
            onClick = onVolverACursos,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58CC02)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            )
        ) {
            Text(
                "CONTINUAR",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ConfettiAnimacionDuolingo() {
    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat() * 1200,
                y = -Random.nextFloat() * 800,
                color = listOf(
                    Color(0xFF58CC02),
                    Color(0xFF1CB0F6),
                    Color(0xFFFF9600),
                    Color(0xFFFFC800),
                    Color(0xFFCE82FF),
                    Color(0xFFFF4B4B)
                ).random(),
                size = Random.nextFloat() * 12 + 6,
                velocityX = Random.nextFloat() * 4 - 2,
                velocityY = Random.nextFloat() * 6 + 3,
                rotation = Random.nextFloat() * 360,
                rotationSpeed = Random.nextFloat() * 8 + 4
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            particle.y += particle.velocityY
            particle.x += particle.velocityX
            particle.rotation += particle.rotationSpeed

            if (particle.y > size.height + 100) {
                particle.y = -100f
                particle.x = Random.nextFloat() * size.width
            }

            if (particle.x < -50) particle.x = size.width + 50
            if (particle.x > size.width + 50) particle.x = -50f

            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}

/**
 * ========================================
 * COMPONENTES ADICIONALES PARA QUIZ
 * ========================================
 */

@Composable
fun IndicadorVidasMejorado(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    modifier: Modifier = Modifier
) {
    val colorVidas = when {
        vidasActuales == 0 -> Color(0xFFFF4B4B)
        vidasActuales <= 2 -> Color(0xFFFFC800)
        else -> Color(0xFFFF4B4B)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = colorVidas,
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        text = "Vidas",
                        fontSize = 14.sp,
                        color = Color(0xFF777777),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$vidasActuales / $vidasMax",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = colorVidas
                    )
                }
            }

            if (vidasActuales < vidasMax && minutosParaProxima > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "+1 en ${minutosParaProxima}m",
                            fontSize = 14.sp,
                            color = Color(0xFF1CB0F6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarraProgresoQuizMejorada(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    modifier: Modifier = Modifier
) {
    val progreso = preguntaActual.toFloat() / totalPreguntas.toFloat()
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progreso"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pregunta $preguntaActual de $totalPreguntas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3C3C3C)
            )

            Text(
                text = "${(progreso * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = colorModo
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = Color(0xFFE5E5E5),
                    shape = RoundedCornerShape(5.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progresoAnimado)
                    .fillMaxHeight()
                    .background(
                        color = colorModo,
                        shape = RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}

@Composable
fun DialogoSinVidasMejorado(
    minutosParaProxima: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF4B4B).copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color(0xFFFF4B4B),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        },
        title = {
            Text(
                "Sin vidas",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color(0xFF3C3C3C)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No tienes vidas disponibles para realizar este quiz.",
                    fontSize = 16.sp,
                    color = Color(0xFF4B4B4B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1CB0F6),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Cómo funcionan las vidas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3C3C3C)
                            )
                        }

                        Text(
                            text = "• Recuperas 1 vida cada 30 minutos\n• Máximo: 5 vidas\n• Próxima vida en: $minutosParaProxima minutos",
                            fontSize = 15.sp,
                            color = Color(0xFF4B4B4B),
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF58CC02).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF58CC02),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Regresa en $minutosParaProxima minutos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF58CC02)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1CB0F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Entendido",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}