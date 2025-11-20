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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.stiven.sos.ui.theme.EduRachaColors
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random


@Composable
fun VentanaRachaDuolingo(
    diasRacha: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // Animación de entrada con rebote suave
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    /**
     * Secuencia de animación:
     * 1. Hacer visible la ventana
     * 2. Mantenerla 3.5 segundos
     * 3. Cerrarla automáticamente
     */
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(3500)
        visible = false
        delay(300)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            // Efecto de partículas de fuego en el fondo
            if (visible) {
                EfectoParticulasFuego()
            }

            // Tarjeta principal de la racha
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .scale(scale),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF9600).copy(alpha = 0.05f),
                                    Color.White,
                                    Color.White
                                )
                            )
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    /**
                     * ICONO PRINCIPAL DE LLAMA
                     * Con múltiples capas de animación para crear
                     * un efecto visual impactante
                     */
                    LlamaAnimadaGrande()

                    /**
                     * TÍTULO PRINCIPAL
                     * Mensaje motivacional en mayúsculas
                     */
                    Text(
                        text = "RACHA SUBIDA",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9600),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    /**
                     * DESCRIPCIÓN MOTIVACIONAL
                     * Refuerza el logro del usuario
                     */
                    Text(
                        text = "Continúas aprendiendo cada día",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B4B4B),
                        textAlign = TextAlign.Center
                    )

                    /**
                     * CONTADOR DE DÍAS
                     * Elemento más destacado de la ventana
                     * Diseño con gradiente naranja de Duolingo
                     */
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFF9600),
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF9600),
                                            Color(0xFFFFAA00)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 40.dp,
                                    vertical = 20.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icono de llama blanca
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )

                                // Número de días con estilo Duolingo
                                Text(
                                    text = "$diasRacha",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                // Texto "días"
                                Text(
                                    text = "días",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }

                    /**
                     * MENSAJE DE MOTIVACIÓN FINAL
                     * Anima al usuario a continuar su racha
                     */
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¡Sigue así!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3C3C3C)
                        )
                        Text(
                            text = "Cada día cuenta para tu progreso",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF777777),
                            textAlign = TextAlign.Center
                        )
                    }

                    /**
                     * INDICADOR VISUAL DE PROGRESO
                     * Barras que muestran los días de la semana
                     */
                    BarrasProgresoDias(diasRacha = diasRacha)
                }
            }
        }
    }
}



/**
 * ========================================
 * LLAMA ANIMADA GRANDE
 * ========================================
 * Icono principal con múltiples capas de animación
 * - Pulso continuo
 * - Rotación suave
 * - Círculos concéntricos de fondo
 */
@Composable
fun LlamaAnimadaGrande() {
    val infiniteTransition = rememberInfiniteTransition(label = "llama")

    // Animación de escala (pulso)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animación de rotación sutil
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Animación del círculo exterior
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerScale"
    )

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Círculo exterior pulsante (naranja muy claro)
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF9600).copy(alpha = 0.15f),
            modifier = Modifier
                .size(160.dp)
                .scale(outerScale)
        ) {}

        // Círculo medio (naranja claro)
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF9600).copy(alpha = 0.3f),
            modifier = Modifier
                .size(120.dp)
                .scale(scale * 0.8f)
        ) {}

        // Círculo interno (naranja más intenso)
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF9600).copy(alpha = 0.5f),
            modifier = Modifier.size(90.dp)
        ) {}

        // Icono de llama central con animaciones
        Icon(
            Icons.Default.Whatshot,
            contentDescription = null,
            tint = Color(0xFFFF9600),
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .rotate(rotation)
        )
    }
}

/**
 * ========================================
 * BARRAS DE PROGRESO DE DÍAS
 * ========================================
 * Representación visual de los últimos 7 días
 * Muestra qué días están completos en la racha
 */
@Composable
fun BarrasProgresoDias(diasRacha: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Crear 7 barras (una por cada día de la semana)
        for (dia in 1..7) {
            val estaCompleto = dia <= (diasRacha % 7).coerceAtLeast(if (diasRacha >= 7) 7 else diasRacha)
            val altura = if (estaCompleto) {
                // Variar altura para crear efecto visual
                (60 + (dia * 10) % 30).dp
            } else {
                40.dp
            }

            // Animación de altura
            val alturaAnimada by animateFloatAsState(
                targetValue = altura.value,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "altura"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(alturaAnimada.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (estaCompleto)
                            Color(0xFFFF9600)
                        else
                            Color(0xFFE5E5E5)
                    )
            )
        }
    }
}

/**
 * ========================================
 * EFECTO DE PARTÍCULAS DE FUEGO
 * ========================================
 * Partículas naranjas que flotan en el fondo
 * para crear ambiente de celebración
 */
@Composable
fun EfectoParticulasFuego() {
    val particles = remember {
        List(30) {
            ParticleState(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 8 + 4,
                speed = Random.nextFloat() * 0.003f + 0.001f,
                alpha = Random.nextFloat() * 0.6f + 0.2f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val x = size.width * particle.x
            val y = size.height * (particle.y - (time * particle.speed) % 1)

            drawCircle(
                color = Color(0xFFFF9600).copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

data class ParticleState(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

/**
 * ========================================
 * VENTANA DE RACHA PERDIDA
 * ========================================
 * Se muestra cuando el usuario no alcanza el 80%
 * Diseño motivacional que anima a mejorar sin ser negativo
 *
 * PSICOLOGÍA DEL COLOR:
 * - Azul (#1CB0F6): Tranquilidad, aprendizaje, no es alarmante
 * - Evita el rojo para no desmotivar
 */
@Composable
fun VentanaRachaPerdida(
    porcentaje: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(3000)
        visible = false
        delay(300)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .scale(scale),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1CB0F6).copy(alpha = 0.05f),
                                    Color.White
                                )
                            )
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    /**
                     * ICONO DE TENDENCIA
                     * Representa progreso y mejora continua
                     */
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1CB0F6).copy(alpha = 0.15f),
                            modifier = Modifier.size(120.dp)
                        ) {}

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1CB0F6).copy(alpha = 0.25f),
                            modifier = Modifier.size(90.dp)
                        ) {}

                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    /**
                     * TÍTULO MOTIVACIONAL
                     * No usa palabras negativas como "fallaste"
                     */
                    Text(
                        text = "SIGUE MEJORANDO",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1CB0F6),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    /**
                     * MENSAJE PRINCIPAL
                     * Explica de forma positiva qué necesita mejorar
                     */
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Obtuviste $porcentaje%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3C3C3C)
                        )

                        Text(
                            text = "Necesitas 80% o más para subir tu racha",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF777777),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }

                    /**
                     * TARJETA INFORMATIVA
                     * Tips para mejorar en el próximo intento
                     */
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF58CC02).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.EmojiObjects,
                                    contentDescription = null,
                                    tint = Color(0xFF58CC02),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Consejo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3C3C3C)
                                )
                            }

                            Text(
                                text = "Revisa la retroalimentación para aprender de tus errores y mejorar en tu próximo intento",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4B4B4B),
                                lineHeight = 22.sp
                            )
                        }
                    }

                    /**
                     * MENSAJE DE ÁNIMO FINAL
                     */
                    Text(
                        text = "¡La práctica hace al maestro!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1CB0F6),
                        textAlign = TextAlign.Center
                    )
                }
            }
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
            // Icono de corazón vacío con círculo de fondo
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
                color = Color(0xFF3C3C3C),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mensaje principal
                Text(
                    text = "No tienes vidas disponibles para realizar este quiz.",
                    fontSize = 16.sp,
                    color = Color(0xFF4B4B4B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                // Tarjeta informativa sobre el sistema de vidas
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

                // Tarjeta de tiempo de espera (verde para dar sensación positiva)
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
            // Botón de confirmación estilo Duolingo
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1CB0F6)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
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

/**
 * ========================================
 * INDICADOR DE VIDAS DURANTE EL QUIZ
 * ========================================
 * Componente que se muestra en la parte superior
 * durante la ejecución del quiz
 */
@Composable
fun IndicadorVidasMejorado(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    modifier: Modifier = Modifier
) {
    // Color adaptativo según número de vidas
    val colorVidas = when {
        vidasActuales == 0 -> Color(0xFFFF4B4B) // Rojo - sin vidas
        vidasActuales <= 2 -> Color(0xFFFFC800) // Amarillo - pocas vidas
        else -> Color(0xFFFF4B4B) // Rojo normal
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
            // Lado izquierdo: Icono y contador de vidas
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

            // Lado derecho: Timer de regeneración (si aplica)
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
fun AnimacionEstrellaExitosa(
    colorModo: Color,
    onAnimacionCompleta: () -> Unit
) {
    var animacionIniciada by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val sizeScale = if (isTablet) 1.5f else 1f

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
        delay(1000)
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
            modifier = Modifier.size((300 * sizeScale).dp),
            contentAlignment = Alignment.Center
        ) {
            ParticulasBrillantes(
                visible = animacionIniciada,
                color = colorModo,
                sizeScale = sizeScale
            )

            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .size((200 * sizeScale).dp)
                    .scale(escalaEstrella)
                    .rotate(rotacionEstrella)
                    .alpha(alphaEstrella)
            )

            Canvas(
                modifier = Modifier
                    .size((250 * sizeScale).dp)
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
fun AnimacionEstrellaIncorrecta(
    onAnimacionCompleta: () -> Unit
) {
    var animacionIniciada by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth > 600.dp
    val sizeScale = if (isTablet) 1.5f else 1f

    // Animacion de aparicion rapida
    val escalaInicial by animateFloatAsState(
        targetValue = if (animacionIniciada) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escalaInicial"
    )

    // Animacion de rotura - fragmentos se separan
    val separacionFragmentos by animateFloatAsState(
        targetValue = if (animacionIniciada) 150f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "separacion"
    )

    val alphaEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 0f else 1f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 600,
            easing = LinearEasing
        ),
        label = "alpha"
    )

    val rotacionFragmentos by animateFloatAsState(
        targetValue = if (animacionIniciada) 720f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "rotacion"
    )

    LaunchedEffect(Unit) {
        animacionIniciada = true
        delay(1400)
        onAnimacionCompleta()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.4f * alphaEstrella)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size((300 * sizeScale).dp),
            contentAlignment = Alignment.Center
        ) {
            // Particulas de polvo rojo
            ParticulasRotura(
                visible = animacionIniciada,
                sizeScale = sizeScale
            )

            // Circulo de error pulsante
            Canvas(
                modifier = Modifier
                    .size((250 * sizeScale).dp)
                    .alpha(alphaEstrella * 0.7f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFA0606).copy(alpha = 0.6f),
                            Color(0xFFF80303).copy(alpha = 0f)
                        )
                    ),
                    radius = size.minDimension / 2 * escalaInicial
                )
            }

            // Fragmentos de estrella rota
            FragmentosEstrellaRota(
                separacion = separacionFragmentos,
                rotacion = rotacionFragmentos,
                alpha = alphaEstrella,
                escala = escalaInicial,
                sizeScale = sizeScale
            )

            // Estrella central que se rompe
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFF4B4B),
                modifier = Modifier
                    .size((200 * sizeScale).dp)
                    .scale(escalaInicial)
                    .alpha(alphaEstrella * 0.5f)
            )
        }
    }
}

@Composable
fun ParticulasBrillantes(
    visible: Boolean,
    color: Color,
    sizeScale: Float
) {
    val numeroParticulas = 12

    for (i in 0 until numeroParticulas) {
        val angulo = (360f / numeroParticulas) * i

        val offset by animateFloatAsState(
            targetValue = if (visible) 120f * sizeScale else 0f,
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
                .size((24 * sizeScale).dp)
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

@Composable
fun FragmentosEstrellaRota(
    separacion: Float,
    rotacion: Float,
    alpha: Float,
    escala: Float,
    sizeScale: Float
) {
    val numeroFragmentos = 8

    for (i in 0 until numeroFragmentos) {
        val angulo = (360f / numeroFragmentos) * i
        val radianes = Math.toRadians(angulo.toDouble())

        val offsetX = (cos(radianes) * separacion * sizeScale).toFloat()
        val offsetY = (sin(radianes) * separacion * sizeScale).toFloat()

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size((40 * sizeScale).dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFF0303),
                modifier = Modifier
                    .fillMaxSize()
                    .scale(escala * 0.6f)
                    .rotate(rotacion + (angulo * 2))
                    .alpha(alpha)
            )
        }
    }
}

@Composable
fun ParticulasRotura(
    visible: Boolean,
    sizeScale: Float
) {
    val numeroParticulas = 20

    for (i in 0 until numeroParticulas) {
        val angulo = Random.nextFloat() * 360f
        val radianes = Math.toRadians(angulo.toDouble())

        val offset by animateFloatAsState(
            targetValue = if (visible) (100f + Random.nextFloat() * 80f) * sizeScale else 0f,
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
                durationMillis = 600,
                delayMillis = 400,
                easing = LinearEasing
            ),
            label = "alpha_$i"
        )

        val offsetX = (cos(radianes) * offset).toFloat()
        val offsetY = (sin(radianes) * offset).toFloat()

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size((12 * sizeScale).dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFF80404),
                    radius = size.minDimension / 2 * escala,
                    alpha = alpha
                )
            }
        }
    }
}
/**
 * ========================================
 * BARRA DE PROGRESO DEL QUIZ
 * ========================================
 * Muestra la pregunta actual y el progreso general
 */
@Composable
fun BarraProgresoQuizMejorada(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    modifier: Modifier = Modifier
) {
    val progreso = preguntaActual.toFloat() / totalPreguntas.toFloat()

    // Animación suave del progreso
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
        // Fila superior: Texto y porcentaje
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

        // Barra de progreso visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = Color(0xFFE5E5E5),
                    shape = RoundedCornerShape(5.dp)
                )
        ) {
            // Barra de progreso rellena
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