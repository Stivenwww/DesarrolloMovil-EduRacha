package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.EduRachaV2Colors
import com.stiven.sos.ui.theme.EduRachaV2Gradients
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExplicacionTemaActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var temaExplicacion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Tema"
        temaExplicacion = intent.getStringExtra("tema_explicacion") ?: ""

        setContent {
            EduRachaTheme {
                LaunchedEffect(cursoId, temaId) {
                    quizViewModel.iniciarObservadores(cursoId, temaId)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        quizViewModel.detenerObservadores()
                    }
                }

                ExplicacionTemaScreen(
                    temaTitulo = temaTitulo,
                    temaExplicacion = temaExplicacion,
                    cursoId = cursoId,
                    temaId = temaId,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onIniciarQuiz = { modo ->
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.putExtra("curso_id", cursoId)
                        intent.putExtra("tema_id", temaId)
                        intent.putExtra("tema_titulo", temaTitulo)
                        intent.putExtra("modo", modo)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

// ============================================================================
// PANTALLA PRINCIPAL
// ============================================================================

@Composable
fun ExplicacionTemaScreen(
    temaTitulo: String,
    temaExplicacion: String,
    cursoId: String,
    temaId: String,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onIniciarQuiz: (String) -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    // Dividir explicación en secciones
    val secciones = remember(temaExplicacion) {
        temaExplicacion.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var seccionesCompletadas by remember { mutableStateOf(setOf<Int>()) }
    val todasCompletadas = seccionesCompletadas.size == secciones.size

    val aproboQuizOficial = uiState.porcentajeQuizOficial >= 80
    val enCooldown = uiState.yaResolviHoy && aproboQuizOficial

    // IMPORTANTE: Quiz práctica solo se habilita si YA resolvió el oficial HOY
    val yaResolviHoy = uiState.yaResolviHoy

    var seccionActual by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Expresión de la mascota según progreso
    val expresionBuho = when {
        todasCompletadas -> OwlExpression.HAPPY
        seccionesCompletadas.size >= secciones.size / 2 -> OwlExpression.EXCITED
        seccionesCompletadas.isEmpty() -> OwlExpression.CURIOUS
        else -> OwlExpression.FOCUSED
    }

    // Marcar explicación como vista cuando se completan todas las secciones
    LaunchedEffect(todasCompletadas) {
        if (todasCompletadas) {
            quizViewModel.marcarExplicacionVista(temaId) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaV2Colors.Background)
    ) {
        // TODO ES SCROLLABLE INCLUYENDO EL HEADER
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // HEADER SIMPLE CON TÍTULO Y BURBUJAS
            item {
                HeaderConBurbujas(
                    titulo = temaTitulo,
                    cantidadSecciones = secciones.size,
                    onNavigateBack = onNavigateBack
                )
            }

            // INDICADORES DE PROGRESO
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    IndicadoresProgreso(
                        total = secciones.size,
                        seccionActual = seccionActual,
                        seccionesCompletadas = seccionesCompletadas,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )

                    // MASCOTA BÚHO ANIMADA
                    MascotaBuho(
                        expresion = expresionBuho,
                        mensaje = obtenerMensajeBuho(seccionesCompletadas.size, secciones.size, todasCompletadas),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            // TARJETA STORY ACTUAL
            item {
                StoryCard(
                    numero = seccionActual + 1,
                    contenido = secciones[seccionActual],
                    completada = seccionesCompletadas.contains(seccionActual),
                    onCompletar = {
                        seccionesCompletadas = seccionesCompletadas + seccionActual
                        // Transición más rápida (600ms)
                        scope.launch {
                            delay(600)
                            if (seccionActual < secciones.size - 1) {
                                seccionActual++
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // CARDS DE QUIZ (solo si todas completadas)
            if (todasCompletadas) {
                // Card de completado
                item {
                    CardTemaCompletadoModerno(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }

                // Quiz Oficial Card
                item {
                    QuizOficialCard(
                        aproboQuizOficial = aproboQuizOficial,
                        porcentajeObtenido = uiState.porcentajeQuizOficial,
                        enCooldown = enCooldown,
                        horasRestantes = uiState.horasParaNuevoQuiz,
                        minutosRestantes = uiState.minutosParaNuevoQuiz,
                        sinVidas = uiState.sinVidas,
                        onIniciar = { onIniciarQuiz("oficial") },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Quiz Práctica Card - SOLO SI YA RESOLVIÓ HOY
                if (yaResolviHoy && aproboQuizOficial) {
                    item {
                        QuizPracticaCard(
                            sinVidas = uiState.sinVidas,
                            onIniciar = { onIniciarQuiz("practica") },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                // Mensaje de bloqueo
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = EduRachaV2Colors.TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    "Quiz bloqueado",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaV2Colors.TextPrimary
                                )
                                Text(
                                    "Lee todas las secciones para desbloquear",
                                    fontSize = 14.sp,
                                    color = EduRachaV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTONES DE NAVEGACIÓN FIJOS EN LA PARTE INFERIOR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = EduRachaV2Colors.Background,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Anterior
                IconButton(
                    onClick = { if (seccionActual > 0) seccionActual-- },
                    enabled = seccionActual > 0,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (seccionActual > 0) EduRachaV2Colors.Primary
                            else EduRachaV2Colors.SoftGray,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Texto indicador
                Text(
                    "${seccionActual + 1} / ${secciones.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Siguiente
                IconButton(
                    onClick = {
                        if (seccionesCompletadas.contains(seccionActual) && seccionActual < secciones.size - 1) {
                            seccionActual++
                        }
                    },
                    enabled = seccionesCompletadas.contains(seccionActual) && seccionActual < secciones.size - 1,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (seccionesCompletadas.contains(seccionActual) && seccionActual < secciones.size - 1)
                                EduRachaV2Colors.Primary
                            else EduRachaV2Colors.SoftGray,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Siguiente",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// HEADER CON BURBUJAS - COMPACTO
// ============================================================================

@Composable
fun HeaderConBurbujas(
    titulo: String,
    cantidadSecciones: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(EduRachaV2Gradients.Purple)
            .statusBarsPadding()
    ) {
        // Burbujas decorativas animadas
        Box(modifier = Modifier.fillMaxSize()) {
            val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

            val bubble1Offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bubble1"
            )

            // Burbuja grande
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 250.dp, y = (-10).dp)
                    .graphicsLayer { translationY = bubble1Offset }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Burbuja mediana
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 200.dp, y = 60.dp)
                    .graphicsLayer { translationY = -bubble1Offset * 0.7f }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            )

            // Burbuja pequeña
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = 20.dp, y = 30.dp)
                    .graphicsLayer { translationY = bubble1Offset * 0.5f }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón back
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                // Ícono de libro
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Título
                Text(
                    titulo,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 28.sp,
                    maxLines = 2
                )

                Spacer(Modifier.height(10.dp))

                // Badge de secciones
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(6.dp)
                        )
                        Text(
                            "$cantidadSecciones secciones",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// INDICADORES DE PROGRESO
// ============================================================================

@Composable
fun IndicadoresProgreso(
    total: Int,
    seccionActual: Int,
    seccionesCompletadas: Set<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            seccionesCompletadas.contains(index) -> EduRachaV2Colors.Success
                            index == seccionActual -> EduRachaV2Colors.Primary
                            else -> EduRachaV2Colors.SoftGray
                        }
                    )
            )
        }
    }
}

// ============================================================================
// MASCOTA BÚHO
// ============================================================================

enum class OwlExpression {
    CURIOUS, FOCUSED, EXCITED, HAPPY
}

@Composable
fun MascotaBuho(
    expresion: OwlExpression,
    mensaje: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "owl")

    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val blink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                1f at 0
                1f at 2800
                0.3f at 2850
                1f at 2900
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Búho animado
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = bounce.dp)
            ) {
                Text(
                    text = when (expresion) {
                        OwlExpression.CURIOUS -> "🦉"
                        OwlExpression.FOCUSED -> "🦉"
                        OwlExpression.EXCITED -> "🦉"
                        OwlExpression.HAPPY -> "🎉"
                    },
                    fontSize = 60.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleY = blink
                    }
                )
            }

            // Mensaje
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    when (expresion) {
                        OwlExpression.CURIOUS -> "¡Empecemos!"
                        OwlExpression.FOCUSED -> "¡Vas bien!"
                        OwlExpression.EXCITED -> "¡Casi terminas!"
                        OwlExpression.HAPPY -> "¡Excelente!"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary
                )
                Text(
                    mensaje,
                    fontSize = 14.sp,
                    color = EduRachaV2Colors.TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

fun obtenerMensajeBuho(completadas: Int, total: Int, todasCompletadas: Boolean): String {
    return when {
        todasCompletadas -> "¡Has leído todo! Ahora puedes hacer el quiz 🎯"
        completadas == 0 -> "Lee cada sección usando los botones de navegación"
        completadas < total / 2 -> "Continúa leyendo, estás aprendiendo mucho"
        else -> "Solo te faltan ${total - completadas} secciones más"
    }
}

// ============================================================================
// STORY CARD
// ============================================================================

@Composable
fun StoryCard(
    numero: Int,
    contenido: String,
    completada: Boolean,
    onCompletar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduRachaV2Colors.Secondary.copy(0.15f)
                ) {
                    Text(
                        "Parte $numero",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.Secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (completada) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = EduRachaV2Colors.Success,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Contenido - TEXTO NEGRO Y MÁS GRANDE
            Text(
                contenido,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                lineHeight = 28.sp
            )

            Spacer(Modifier.height(24.dp))

            // Botón completar
            if (!completada) {
                Button(
                    onClick = onCompletar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaV2Colors.Success
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 18.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Marcar como leído",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaV2Colors.Success.copy(0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EduRachaV2Colors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Sección completada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaV2Colors.Success
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// CARD DE TEMA COMPLETADO MODERNO
// ============================================================================

@Composable
fun CardTemaCompletadoModerno(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF7096),
                            Color(0xFFFF5277)
                        )
                    )
                )
        ) {
            // Burbujas decorativas
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 250.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 20.dp, y = 100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji animado
                val infiniteTransition = rememberInfiniteTransition(label = "trophy")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Text(
                    "🏆",
                    fontSize = 80.sp,
                    modifier = Modifier
                        .scale(scale)
                        .padding(bottom = 16.dp)
                )

                Text(
                    "¡Tema Completado!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Has leído todas las secciones. ¡Es hora de poner a prueba tus conocimientos!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

// ============================================================================
// CARD QUIZ OFICIAL MEJORADA
// ============================================================================

@Composable
fun QuizOficialCard(
    aproboQuizOficial: Boolean,
    porcentajeObtenido: Int,
    enCooldown: Boolean,
    horasRestantes: Int,
    minutosRestantes: Int,
    sinVidas: Boolean,
    onIniciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val puedeIniciar = !sinVidas && !enCooldown

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B9D),
                            Color(0xFFFF5277)
                        )
                    )
                )
        ) {
            // Burbujas decorativas
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 220.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 280.dp, y = 60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏆", fontSize = 32.sp)
                        }

                        Column {
                            Text(
                                "Quiz Oficial",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "Demuestra tu conocimiento",
                                fontSize = 13.sp,
                                color = Color.White.copy(0.9f)
                            )
                        }
                    }

                    // Badge de estado
                    if (aproboQuizOficial) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(0.3f)
                        ) {
                            Text(
                                "$porcentajeObtenido%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Características
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CaracteristicaQuiz(
                        icono = "⭐",
                        texto = "Afecta tu racha diaria"
                    )
                    CaracteristicaQuiz(
                        icono = "💎",
                        texto = "Gana experiencia y progreso"
                    )
                    if (enCooldown) {
                        CaracteristicaQuiz(
                            icono = "⏰",
                            texto = "Próximo intento: ${horasRestantes}h ${minutosRestantes}m"
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Botón
                Button(
                    onClick = onIniciar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = puedeIniciar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color.White.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Icon(
                        if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (puedeIniciar) Color(0xFFFF5277) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        when {
                            enCooldown -> "En cooldown"
                            sinVidas -> "Sin energía"
                            else -> "Comenzar Quiz"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (puedeIniciar) Color(0xFFFF5277) else Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

// ============================================================================
// CARD QUIZ PRÁCTICA MEJORADA
// ============================================================================

@Composable
fun QuizPracticaCard(
    sinVidas: Boolean,
    onIniciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val puedeIniciar = !sinVidas

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF2563EB)
                        )
                    )
                )
        ) {
            // Burbujas decorativas
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 240.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .offset(x = 20.dp, y = 80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💪", fontSize = 32.sp)
                    }

                    Column {
                        Text(
                            "Modo Práctica",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "Refuerza tu aprendizaje",
                            fontSize = 13.sp,
                            color = Color.White.copy(0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Características
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CaracteristicaQuiz(
                        icono = "🎯",
                        texto = "Practica sin límites"
                    )
                    CaracteristicaQuiz(
                        icono = "💎",
                        texto = "Gana XP sin afectar tu racha"
                    )

                }

                Spacer(Modifier.height(24.dp))

                // Botón
                Button(
                    onClick = onIniciar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = puedeIniciar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color.White.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Icon(
                        if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (puedeIniciar) Color(0xFF2563EB) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (sinVidas) "Sin energía" else "Comenzar Práctica",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (puedeIniciar) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTE AUXILIAR - CARACTERÍSTICA QUIZ
// ============================================================================

@Composable
fun CaracteristicaQuiz(
    icono: String,
    texto: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            icono,
            fontSize = 20.sp
        )
        Text(
            texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(0.95f)
        )
    }
}