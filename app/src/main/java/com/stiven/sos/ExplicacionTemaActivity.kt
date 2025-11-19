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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
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
    var explicacionCompletada by remember { mutableStateOf(false) }

    val aproboQuizOficial = uiState.porcentajeQuizOficial >= 80
    val enCooldown = uiState.yaResolviHoy && aproboQuizOficial

    val puedeHacerQuizOficial = explicacionCompletada && !uiState.sinVidas && (!aproboQuizOficial || !enCooldown)
    val puedeHacerPractica = aproboQuizOficial && !uiState.sinVidas

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.Primary.copy(alpha = 0.9f),
                            EduRachaColors.Background
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBarExplicacion(
                titulo = temaTitulo,
                onNavigateBack = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (aproboQuizOficial && enCooldown) {
                    AlertaCooldownActivo(
                        horas = uiState.horasParaNuevoQuiz,
                        minutos = uiState.minutosParaNuevoQuiz,
                        porcentajeObtenido = uiState.porcentajeQuizOficial
                    )
                }

                if (uiState.porcentajeQuizOficial > 0 && !aproboQuizOficial) {
                    AlertaIntentarNuevamente(
                        porcentajeObtenido = uiState.porcentajeQuizOficial
                    )
                }

                // NUEVA IMPLEMENTACIÓN: Lectura por Etapas
                ExplicacionPorEtapas(
                    explicacion = temaExplicacion,
                    onCompletada = {
                        explicacionCompletada = true
                        quizViewModel.marcarExplicacionVista(temaId) {}
                    }
                )

                CardQuizOficial(
                    puedeIniciar = puedeHacerQuizOficial,
                    explicacionVista = explicacionCompletada,
                    sinVidas = uiState.sinVidas,
                    aproboQuizOficial = aproboQuizOficial,
                    porcentajeObtenido = uiState.porcentajeQuizOficial,
                    enCooldown = enCooldown,
                    horasRestantes = uiState.horasParaNuevoQuiz,
                    minutosRestantes = uiState.minutosParaNuevoQuiz,
                    onIniciar = { onIniciarQuiz("oficial") }
                )

                if (aproboQuizOficial) {
                    CardModoPractica(
                        sinVidas = uiState.sinVidas,
                        onIniciar = { onIniciarQuiz("practica") }
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// NUEVA FUNCIÓN: Sistema de lectura por etapas
@Composable
fun ExplicacionPorEtapas(
    explicacion: String,
    onCompletada: () -> Unit
) {
    // Dividir la explicación en párrafos
    val parrafos = remember(explicacion) {
        explicacion.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var etapaActual by remember { mutableStateOf(0) }
    var todasLasEtapasVistas by remember { mutableStateOf(false) }

    val progress = (etapaActual + 1).toFloat() / parrafos.size.toFloat()

    // Animación de entrada de cards
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(todasLasEtapasVistas) {
        if (todasLasEtapasVistas) {
            onCompletada()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header con progreso
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
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1976D2).copy(alpha = 0.15f),
                            modifier = Modifier.size(50.dp)
                        ) {}
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1976D2).copy(alpha = 0.25f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Contenido del Tema",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = if (todasLasEtapasVistas) "¡Completado!" else "Lee con atención",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (todasLasEtapasVistas) Color(0xFF4CAF50) else EduRachaColors.TextSecondary
                        )
                    }
                }

                // Badge de progreso
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1976D2).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (todasLasEtapasVistas) Icons.Default.CheckCircle else Icons.Default.Article,
                            contentDescription = null,
                            tint = if (todasLasEtapasVistas) Color(0xFF4CAF50) else Color(0xFF1976D2),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${etapaActual + 1}/${parrafos.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (todasLasEtapasVistas) Color(0xFF4CAF50) else Color(0xFF1976D2)
                        )
                    }
                }
            }

            // Barra de progreso
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (todasLasEtapasVistas) Color(0xFF4CAF50) else Color(0xFF1976D2),
                    trackColor = Color(0xFFE0E0E0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    parrafos.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        index < etapaActual -> Color(0xFF4CAF50)
                                        index == etapaActual -> Color(0xFF1976D2)
                                        else -> Color(0xFFE0E0E0)
                                    }
                                )
                        )
                    }
                }
            }

            // Contenido de la etapa actual con animación
            AnimatedContent(
                targetState = etapaActual,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) +
                            slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { it }
                            ) togetherWith
                            fadeOut(animationSpec = tween(300)) +
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { -it }
                            )
                },
                label = "etapa_content"
            ) { etapa ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F9FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1976D2).copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${etapa + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                            Text(
                                text = "Párrafo ${etapa + 1} de ${parrafos.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }

                        Text(
                            text = parrafos[etapa],
                            fontSize = 16.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón "Anterior" (solo si no es la primera etapa)
                if (etapaActual > 0) {
                    OutlinedButton(
                        onClick = { etapaActual-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1976D2)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Anterior",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botón principal (Siguiente/Completar)
                Button(
                    onClick = {
                        if (etapaActual < parrafos.size - 1) {
                            etapaActual++
                        } else {
                            todasLasEtapasVistas = true
                        }
                    },
                    modifier = Modifier.weight(if (etapaActual > 0) 1f else 1f),
                    enabled = !todasLasEtapasVistas,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (etapaActual == parrafos.size - 1)
                            Color(0xFF4CAF50) else Color(0xFF1976D2),
                        disabledContainerColor = Color(0xFF81C784)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when {
                                todasLasEtapasVistas -> Icons.Default.CheckCircle
                                etapaActual == parrafos.size - 1 -> Icons.Default.Check
                                else -> Icons.Default.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            when {
                                todasLasEtapasVistas -> "✓ Completado"
                                etapaActual == parrafos.size - 1 -> "Completar Lectura"
                                else -> "Siguiente"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info adicional
            if (!todasLasEtapasVistas) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Debes leer todo el contenido para desbloquear el quiz",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Mensaje de éxito
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "¡Excelente! Has completado toda la explicación",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Mantén las demás funciones como TopBarExplicacion, AlertaCooldownActivo, etc.
// (Las dejo sin cambios por brevedad, pero puedes copiarlas de tu código original)

@Composable
fun TopBarExplicacion(
    titulo: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Aprende",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = titulo,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            lineHeight = 32.sp
        )
    }
}

// NUEVA ALERTA: Cooldown activo con contador
@Composable
fun AlertaCooldownActivo(horas: Int, minutos: Int, porcentajeObtenido: Int) {
    // Animación de pulso para el ícono del reloj
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // Azul claro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2196F3).copy(alpha = 0.2f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Quiz aprobado!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Próximo quiz en: ${horas}h ${minutos}m",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AlertaIntentarNuevamente(porcentajeObtenido: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Último intento: $porcentajeObtenido%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Te falta ${80 - porcentajeObtenido}% para aprobar. ¡Sigue intentando!",
                    fontSize = 13.sp,
                    color = Color(0xFF6D4C41),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// CARD EXPLICACIÓN
@Composable
fun CardExplicacionMejorada(
    explicacion: String,
    explicacionVista: Boolean,
    onMarcarLeida: () -> Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(explicacionVista) {
        if (explicacionVista) {
            scale.animateTo(1.05f, tween(200))
            scale.animateTo(1f, tween(200))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header con icono
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1976D2).copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1976D2).copy(alpha = 0.25f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Contenido del Tema",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Lee con atención",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            // Divider decorativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1976D2).copy(alpha = 0.3f),
                                Color(0xFF64B5F6).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Contenido de la explicación con mejor formato
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Text(
                    text = explicacion,
                    fontSize = 16.sp,
                    color = EduRachaColors.TextPrimary,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(20.dp)
                )
            }

            // Botón de marcar como leída
            Button(
                onClick = onMarcarLeida,
                modifier = Modifier.fillMaxWidth(),
                enabled = !explicacionVista,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFF81C784)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (explicacionVista) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        if (explicacionVista) "✓ Explicación leída" else "Marcar como leída",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info adicional
            if (!explicacionVista) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2196F3).copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Debes leer el contenido para desbloquear el quiz",
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CardQuizOficial(
    puedeIniciar: Boolean,
    explicacionVista: Boolean,
    sinVidas: Boolean,
    aproboQuizOficial: Boolean,
    porcentajeObtenido: Int,
    enCooldown: Boolean,
    horasRestantes: Int,
    minutosRestantes: Int,
    onIniciar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (puedeIniciar) Color.White else Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (puedeIniciar) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (puedeIniciar) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1976D2).copy(alpha = 0.08f),
                                Color.White
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.LightGray.copy(alpha = 0.1f),
                                Color.White
                            )
                        )
                    }
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono principal
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (puedeIniciar)
                        Color(0xFF1976D2).copy(alpha = 0.15f)
                    else
                        Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.size(80.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = if (puedeIniciar)
                        Color(0xFF1976D2).copy(alpha = 0.25f)
                    else
                        Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                enCooldown -> Icons.Default.Schedule
                                aproboQuizOficial && !enCooldown -> Icons.Default.School // Puede volver a hacer el quiz
                                sinVidas || !explicacionVista -> Icons.Default.Lock
                                else -> Icons.Default.School
                            },
                            contentDescription = null,
                            tint = if (puedeIniciar) Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Text(
                text = "Quiz Oficial",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (puedeIniciar) Color(0xFF1976D2) else Color.Gray,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    enCooldown -> "Racha completada. Vuelve en ${horasRestantes}h ${minutosRestantes}m"
                    aproboQuizOficial && !enCooldown -> "¡Completaste la racha! Puedes volver a intentar mejorar"
                    sinVidas -> "Necesitas energía disponible para iniciar"
                    !explicacionVista -> "Primero lee la explicación del tema"
                    porcentajeObtenido > 0 -> "Último intento: $porcentajeObtenido%. ¡Sigue intentando!"
                    else -> "Resuelve el quiz para ganar XP y mantener tu racha"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (puedeIniciar) EduRachaColors.TextSecondary else Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            // Mostrar contador de tiempo si está en cooldown
            if (enCooldown) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2196F3).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${horasRestantes}h ${minutosRestantes}m restantes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Button(
                onClick = onIniciar,
                modifier = Modifier.fillMaxWidth(),
                enabled = puedeIniciar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        when {
                            enCooldown -> "Esperando cooldown"
                            aproboQuizOficial && !enCooldown -> "Mejorar Puntuación"
                            puedeIniciar -> if (porcentajeObtenido > 0 && porcentajeObtenido < 80) "Mejorar Puntuación" else "Iniciar Quiz Oficial"
                            else -> "Bloqueado"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rewards
            if (puedeIniciar && !enCooldown) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RewardBadge(
                        icon = Icons.Default.Star,
                        text = "+50 XP",
                        color = Color(0xFFFFC107),
                        modifier = Modifier.weight(1f)
                    )
                    RewardBadge(
                        icon = Icons.Default.Whatshot,
                        text = if (aproboQuizOficial) "Mejora tu score" else "Mantén racha",
                        color = Color(0xFFE02127),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

//  CARD MODO PRÁCTICA - SIMPLIFICADA
@Composable
fun CardModoPractica(
    sinVidas: Boolean,
    onIniciar: () -> Unit
) {
    val puedeIniciar = !sinVidas

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (puedeIniciar) Color.White else Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (puedeIniciar) 6.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (puedeIniciar) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFDA582F).copy(alpha = 0.08f),
                                Color.White
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.LightGray.copy(alpha = 0.1f),
                                Color.White
                            )
                        )
                    }
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (puedeIniciar)
                        Color(0xFFDA582F).copy(alpha = 0.15f)
                    else
                        Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = if (puedeIniciar)
                        Color(0xFFDA582F).copy(alpha = 0.25f)
                    else
                        Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (puedeIniciar) Icons.Default.FitnessCenter else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (puedeIniciar) Color(0xFFDA582F) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                text = "🏋️ Modo Práctica",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (puedeIniciar) Color(0xFFDA582F) else Color.Gray,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (sinVidas)
                    "Necesitas energía disponible para iniciar"
                else
                    "Practica sin límites. Gana XP pero no afecta tu racha",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (puedeIniciar) EduRachaColors.TextSecondary else Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Button(
                onClick = onIniciar,
                modifier = Modifier.fillMaxWidth(),
                enabled = puedeIniciar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        if (puedeIniciar) "Iniciar Práctica" else "Bloqueado",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RewardBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}