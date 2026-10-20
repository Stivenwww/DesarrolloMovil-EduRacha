package com.stiven.sos

import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.clickable
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

/** Partícula de confeti (estado mutable solo para la animación). */
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

/** Estrellas según el porcentaje (solo visual): 90%+ = 3, 80%+ = 2, 50%+ = 1. */
private fun estrellasPorPorcentaje(porcentaje: Int): Int = when {
    porcentaje >= 90 -> 3
    porcentaje >= 80 -> 2
    porcentaje >= 50 -> 1
    else -> 0
}

// ============================================================================
// PANTALLA DE RESULTADOS — diseño Nexo
// ============================================================================

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
    val p = LocalNexo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val totalPreguntas = preguntasCorrectas + preguntasIncorrectas
    val porcentaje = if (totalPreguntas > 0) (preguntasCorrectas * 100) / totalPreguntas else 0
    val aprobo = porcentaje >= 80
    val estrellas = estrellasPorPorcentaje(porcentaje)

    var mostrarVentanaRacha by remember { mutableStateOf(false) }
    var mostrarVentanaRachaPerdida by remember { mutableStateOf(false) }
    var mostrarConfetti by remember { mutableStateOf(false) }
    var diasRacha by remember { mutableStateOf(0) }
    var rachaSubida by remember { mutableStateOf(false) }

    val uiState by quizViewModel.uiState.collectAsState()

    val (titulo, subtitulo) = when {
        porcentaje >= 90 -> "¡Excelente trabajo!" to "Dominaste esta isla."
        porcentaje >= 80 -> "¡Isla conquistada!" to "Aprobaste el quiz."
        porcentaje >= 50 -> "Casi lo logras" to "Necesitas 80% para aprobar."
        else -> "Sigue intentándolo" to "Repasa la explicación y vuelve a intentarlo."
    }
    val colorResultado = when {
        porcentaje >= 80 -> p.accion
        porcentaje >= 50 -> p.racha
        else -> p.error
    }

    // Sonido de cierre (respeta la preferencia de sonidos)
    LaunchedEffect(Unit) {
        com.stiven.sos.services.GestorSonidos.reproducir(
            context,
            if (aprobo) com.stiven.sos.services.GestorSonidos.Sonido.FIN
            else com.stiven.sos.services.GestorSonidos.Sonido.AVANCE
        )
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

    FondoNexo(conCuadricula = false) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Héroe: Robi + estrellas + porcentaje
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(190.dp).fillMaxWidth()) {
                        IslaFlotante(
                            modifier = Modifier.size(width = 220.dp, height = 90.dp),
                            tipo = if (aprobo) TipoIsla.CONQUISTADA else TipoIsla.ACTIVA
                        )
                        RobiNeo(
                            tamano = 140.dp,
                            expresion = when {
                                aprobo -> ExpresionRobi.CELEBRA
                                porcentaje >= 50 -> ExpresionRobi.PIENSA
                                else -> ExpresionRobi.TRISTE
                            },
                            modifier = Modifier.padding(bottom = 46.dp)
                        )
                    }
                    EstrellasNexo(ganadas = estrellas, tamano = 48.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        titulo,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = p.texto,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        subtitulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textoSuave,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Fila de métricas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatNexo(
                        icono = Icons.Filled.TrackChanges,
                        valor = "$porcentaje%",
                        etiqueta = "Precisión",
                        color = colorResultado,
                        modifier = Modifier.weight(1f)
                    )
                    StatNexo(
                        icono = Icons.Filled.CheckCircle,
                        valor = "$preguntasCorrectas",
                        etiqueta = "Correctas",
                        color = p.accion,
                        modifier = Modifier.weight(1f)
                    )
                    StatNexo(
                        icono = Icons.Filled.Cancel,
                        valor = "$preguntasIncorrectas",
                        etiqueta = "Incorrectas",
                        color = p.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Experiencia (valores entregados por el servidor)
            item {
                TarjetaExperienciaNexo(
                    experienciaGanada = experienciaGanada,
                    bonificacionRapidez = bonificacionRapidez,
                    bonificacionPrimeraVez = bonificacionPrimeraVez,
                    bonificacionTodoCorrecto = bonificacionTodoCorrecto
                )
            }

            // Racha y vidas
            if (diasRacha > 0 || modo == "oficial") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (diasRacha > 0) {
                            StatNexo(
                                icono = Icons.Filled.LocalFireDepartment,
                                valor = "$diasRacha",
                                etiqueta = if (diasRacha == 1) "día de racha" else "días de racha",
                                color = p.racha,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (modo == "oficial") {
                            StatNexo(
                                icono = Icons.Filled.Favorite,
                                valor = "$vidasRestantes/5",
                                etiqueta = "Vidas",
                                color = p.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Modo práctica desbloqueado
            if (aprobo && modo == "oficial") {
                item {
                    TarjetaNexo(modifier = Modifier.fillMaxWidth(), acento = p.ia) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InsigniaNexo(Icons.Filled.FitnessCenter, p.ia, tamano = 52.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Modo práctica desbloqueado", fontSize = 17.sp, fontWeight = FontWeight.Black, color = p.texto)
                                Text(
                                    "Repasa este tema cuando quieras para dominar todos los conceptos.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.textoSuave
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        BotonNexo(
                            texto = "Seguir practicando",
                            onClick = onIniciarPractica,
                            tipo = TipoBoton.IA,
                            icono = Icons.Filled.PlayArrow,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Acciones
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (preguntasIncorrectas > 0) {
                        BotonNexo(
                            texto = "Ver retroalimentación",
                            onClick = onVerRetroalimentacion,
                            tipo = TipoBoton.MARCA,
                            icono = Icons.Filled.Lightbulb,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BotonNexo(
                        texto = "Continuar",
                        onClick = onVolverACursos,
                        tipo = TipoBoton.ACCION,
                        icono = Icons.Filled.ArrowForward,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (mostrarConfetti) {
            ConfettiAnimacionV2()
        }

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
    }
}

@Composable
fun TarjetaExperienciaNexo(
    experienciaGanada: Int,
    bonificacionRapidez: Int,
    bonificacionPrimeraVez: Int,
    bonificacionTodoCorrecto: Int
) {
    val p = LocalNexo.current
    // Conteo animado del XP recibido del servidor
    val xpAnimado = remember { Animatable(0f) }
    LaunchedEffect(experienciaGanada) {
        xpAnimado.animateTo(experienciaGanada.toFloat(), tween(1200, easing = FastOutSlowInEasing))
    }
    TarjetaNexo(modifier = Modifier.fillMaxWidth(), acento = p.ia) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Filled.Bolt, p.ia, tamano = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("EXPERIENCIA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = p.iaTexto, letterSpacing = 1.sp)
                Text("Ganada en este quiz", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
            Text(
                "+${xpAnimado.value.toInt()}",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = p.iaTexto
            )
        }
        val bonos = listOfNotNull(
            if (bonificacionRapidez > 0) Triple(Icons.Filled.Speed, "Velocidad relámpago", bonificacionRapidez) else null,
            if (bonificacionPrimeraVez > 0) Triple(Icons.Filled.Celebration, "Primera vez", bonificacionPrimeraVez) else null,
            if (bonificacionTodoCorrecto > 0) Triple(Icons.Filled.EmojiEvents, "100% sin errores", bonificacionTodoCorrecto) else null
        )
        if (bonos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Bonificaciones", fontSize = 14.sp, fontWeight = FontWeight.Black, color = p.texto)
            Spacer(Modifier.height(6.dp))
            bonos.forEach { (icono, texto, puntos) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.racha.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icono, contentDescription = null, tint = p.rachaTexto, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.texto, modifier = Modifier.weight(1f))
                    Text("+$puntos XP", fontSize = 14.sp, fontWeight = FontWeight.Black, color = p.rachaTexto)
                }
            }
        }
    }
}

// ============================================================================
// VENTANAS EMERGENTES
// ============================================================================

@Composable
fun VentanaRachaDuolingoV2(
    diasRacha: Int,
    onDismiss: () -> Unit
) {
    val p = LocalNexo.current
    val escala = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        TarjetaNexo(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = escala.value; scaleY = escala.value },
            acento = p.racha,
            padding = 28.dp
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconoLlamaNexo(tamano = 96.dp)
                Spacer(Modifier.height(12.dp))
                Text("¡Racha aumentada!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = p.texto, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$diasRacha ${if (diasRacha == 1) "día" else "días"} seguidos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = p.rachaTexto,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.racha.copy(alpha = 0.15f))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Sigue así para no perderla.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
        }
    }
}

@Composable
fun VentanaRachaPerdidaV2(
    porcentaje: Int,
    onDismiss: () -> Unit
) {
    val p = LocalNexo.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        TarjetaNexo(modifier = Modifier.fillMaxWidth(), acento = p.racha, padding = 28.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RobiNeo(tamano = 110.dp, expresion = ExpresionRobi.PIENSA)
                Spacer(Modifier.height(10.dp))
                Text("¡No te rindas!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = p.texto, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.racha.copy(alpha = 0.12f))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tu puntuación: $porcentaje%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = p.texto)
                    Text("Necesitas 80% para aprobar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
                }
                Spacer(Modifier.height(10.dp))
                Text("Tu racha se mantiene.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Llama dibujada (sin emoji) con latido suave. */
@Composable
fun IconoLlamaNexo(tamano: androidx.compose.ui.unit.Dp = 64.dp) {
    val p = LocalNexo.current
    val t = rememberInfiniteTransition(label = "llama")
    val latido by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "latido"
    )
    Canvas(Modifier.size(tamano).graphicsLayer { scaleX = latido; scaleY = latido }) {
        val w = size.width
        val h = size.height
        fun llama(escala: Float): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
            val cx = w / 2f
            val base = h * 0.95f
            val alto = h * 0.9f * escala
            val ancho = w * 0.42f * escala
            moveTo(cx, base - alto)
            cubicTo(cx + ancho * 0.2f, base - alto * 0.7f, cx + ancho * 1.1f, base - alto * 0.55f, cx + ancho, base - alto * 0.25f)
            cubicTo(cx + ancho * 0.9f, base, cx - ancho * 0.9f, base, cx - ancho, base - alto * 0.25f)
            cubicTo(cx - ancho * 1.05f, base - alto * 0.5f, cx - ancho * 0.4f, base - alto * 0.55f, cx, base - alto)
            close()
        }
        drawPath(llama(1f), p.racha)
        drawPath(llama(0.62f), p.error.copy(alpha = 0.85f))
        drawPath(llama(0.32f), Color.White.copy(alpha = 0.85f))
    }
}

// ============================================================================
// CONFETI
// ============================================================================

@Composable
fun ConfettiAnimacionV2() {
    val p = LocalNexo.current
    val colores = listOf(p.accion, p.marca, p.racha, p.ia, p.error)

    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat() * 1200,
                y = -Random.nextFloat() * 1000,
                color = colores.random(),
                size = Random.nextFloat() * 12 + 6,
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
        @Suppress("UNUSED_VARIABLE")
        val tick = time // fuerza el redibujado en cada cuadro
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

            rotate(particle.rotation, pivot = Offset(particle.x, particle.y)) {
                drawRoundRect(
                    color = particle.color,
                    topLeft = Offset(particle.x - particle.size / 2f, particle.y - particle.size / 4f),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size / 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    alpha = 0.95f
                )
            }
        }
    }
}
