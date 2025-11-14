package com.stiven.sos.services

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stiven.sos.ui.theme.EduRachaColors
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun CelebracionRachaDialog(
    diasRacha: Int,
    onDismiss: () -> Unit
) {
    // Estado para controlar animacion de entrada
    var visible by remember { mutableStateOf(false) }
    var mostrarConfetti by remember { mutableStateOf(false) }

    // Animacion de escala con efecto bounce
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Efecto que controla el ciclo de vida del dialogo
    LaunchedEffect(Unit) {
        visible = true
        mostrarConfetti = true
        delay(4000) // Mostrar por 4 segundos
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            // Efecto de confetti de fondo
            if (mostrarConfetti) {
                ConfettiEffect()
            }

            // Card principal de la celebracion
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .scale(scale),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF3E0), // Naranja claro
                                    Color.White
                                )
                            )
                        )
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icono de fuego animado
                    IconoFuegoAnimadoMejorado()

                    Text(
                        text = "INCREIBLE",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B35),
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "Subiste tu racha",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    // Tarjeta con contador de racha
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFF6B35).copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(48.dp)
                            )
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "$diasRacha dias",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF6B35)
                                )
                                Text(
                                    text = "seguidos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sigue asi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// Componente de icono de fuego con animacion
@Composable
private fun IconoFuegoAnimadoMejorado() {
    val infiniteTransition = rememberInfiniteTransition(label = "fuego")

    // Animacion de escala pulsante
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animacion de rotacion suave
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Circulo de fondo con gradiente radial
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6B35).copy(alpha = 0.3f),
                                Color(0xFFFF6B35).copy(alpha = 0.05f)
                            )
                        )
                    )
            )
        }

        // Icono de fuego animado
        Icon(
            Icons.Default.Whatshot,
            contentDescription = null,
            tint = Color(0xFFFF6B35),
            modifier = Modifier
                .size(70.dp)
                .scale(scale)
                .rotate(rotation)
        )
    }
}

// ============================================
// EFECTO CONFETTI
// Particulas animadas que caen desde arriba
// ============================================

@Composable
private fun ConfettiEffect() {
    // Generar lista de particulas con propiedades aleatorias
    val confettiPieces = remember {
        List(50) {
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.2f,
                color = listOf(
                    Color(0xFFFF6B35),
                    Color(0xFFFFD93D),
                    Color(0xFF6BCF7F),
                    Color(0xFF4FC3F7),
                    Color(0xFFBA68C8)
                ).random(),
                rotation = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    var time by remember { mutableStateOf(0f) }

    // Actualizar tiempo para animacion continua
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // 60 FPS
            time += 0.016f
        }
    }

    // Dibujar particulas en canvas
    Canvas(modifier = Modifier.fillMaxSize()) {
        confettiPieces.forEach { piece ->
            val yPos = (piece.y + time * piece.speed) % 1.2f
            val xOffset = sin(time * 2f + piece.x * 10f) * 50f

            drawCircle(
                color = piece.color,
                radius = 8.dp.toPx(),
                center = Offset(
                    x = size.width * piece.x + xOffset,
                    y = size.height * yPos
                ),
                alpha = if (yPos > 1f) 1f - (yPos - 1f) * 5f else 1f
            )
        }
    }
}

// Clase de datos para cada particula de confetti
private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val speed: Float
)

// ============================================
// INDICADOR DE VIDAS MEJORADO
// Muestra vidas actuales y tiempo para proxima vida
// ============================================

@Composable
fun IndicadorVidasMejorado(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (vidasActuales == 0)
            EduRachaColors.Error.copy(alpha = 0.08f)
        else
            Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seccion de contador de vidas
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Fondo circular del icono
                    Surface(
                        shape = CircleShape,
                        color = if (vidasActuales == 0)
                            EduRachaColors.Error.copy(alpha = 0.15f)
                        else
                            EduRachaColors.Error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    // Icono de corazon
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (vidasActuales == 0)
                            EduRachaColors.TextSecondary
                        else
                            EduRachaColors.Error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = "Vidas",
                        fontSize = 11.sp,
                        color = EduRachaColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$vidasActuales / $vidasMax",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (vidasActuales == 0)
                            EduRachaColors.Error
                        else
                            EduRachaColors.TextPrimary
                    )
                }
            }

            // Indicador de tiempo para proxima vida
            if (vidasActuales < vidasMax && minutosParaProxima > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EduRachaColors.Info.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = EduRachaColors.Info,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "+1 en ${minutosParaProxima}m",
                            fontSize = 13.sp,
                            color = EduRachaColors.Info,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// BARRA DE PROGRESO CON ESTRELLAS
// Muestra progreso del quiz con indicadores visuales
// ============================================

@Composable
fun BarraProgresoQuizMejorada(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    modifier: Modifier = Modifier
) {
    // Calcular progreso y estrellas a mostrar
    val progreso = preguntaActual.toFloat() / totalPreguntas.toFloat()
    val estrellasMostradas = (progreso * 5).toInt()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fila superior con texto y estrellas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pregunta $preguntaActual de $totalPreguntas",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorModo
            )

            // Indicador de estrellas (5 estrellas total)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { index ->
                    EstrellaProgreso(
                        activa = index < estrellasMostradas,
                        colorModo = colorModo
                    )
                }
            }
        }

        // Barra de progreso horizontal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(colorModo.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colorModo.copy(alpha = 0.8f),
                                colorModo
                            )
                        )
                    )
            )
        }
    }
}

// Componente de estrella individual con animacion
@Composable
private fun EstrellaProgreso(activa: Boolean, colorModo: Color) {
    val scale by animateFloatAsState(
        targetValue = if (activa) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Icon(
        if (activa) Icons.Default.Star else Icons.Default.StarBorder,
        contentDescription = null,
        tint = if (activa) EduRachaColors.Warning else colorModo.copy(alpha = 0.3f),
        modifier = Modifier
            .size(20.dp)
            .scale(scale)
    )
}

// ============================================
// DIALOGO SIN VIDAS MEJORADO
// Se muestra cuando el usuario no tiene vidas disponibles
// ============================================

@Composable
fun DialogoSinVidasMejorado(
    minutosParaProxima: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Fondo circular
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxSize()
                ) {}

                // Icono de corazon vacio
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                "Sin vidas disponibles",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No tienes vidas para realizar este quiz.",
                    fontSize = 15.sp,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                // Card con informacion del sistema de vidas
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Info.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = EduRachaColors.Info,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Sistema de vidas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Info
                            )
                        }

                        Text(
                            text = "1 vida cada 30 minutos\nMaximo: 5 vidas\nProxima vida: $minutosParaProxima min",
                            fontSize = 13.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Card con tiempo de espera
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Success.copy(alpha = 0.1f),
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
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Regresa en $minutosParaProxima minutos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Success
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
                    containerColor = EduRachaColors.Primary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Entendido",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}