// ui/theme/EduRachaPlay.kt
// Componentes visuales animados para la experiencia del ESTUDIANTE.
// Solo capa de presentación: no contienen lógica de negocio ni acceden a datos.
// Identidad propia de EduRacha (mascota "Rachi", una llamita que representa la racha).
package com.stiven.sos.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// PALETA "PLAY" (derivada de EduRachaColors)
// ============================================================================

object PlayColors {
    private val p: NexoPaleta get() = TemaApp.paleta
    val Indigo: Color get() = p.marca
    val IndigoSombra: Color get() = p.marcaSombra
    val IndigoSuave: Color get() = p.bloqueado
    val IndigoSuaveSombra: Color get() = p.bloqueadoSombra
    val Sol: Color get() = p.racha
    val SolSombra: Color get() = p.rachaSombra
    val Llama: Color get() = p.racha
    val LlamaSombra: Color get() = p.rachaSombra
    val Menta: Color get() = p.accion
    val MentaSombra: Color get() = p.accionSombra
    val Coral: Color get() = p.error
    val CoralSombra: Color get() = p.errorSombra
    val Cielo: Color get() = p.marca
    val CieloSombra: Color get() = p.marcaSombra
    val Tinta: Color get() = p.texto
    val Fondo: Color get() = p.fondo
}

/** Fondo con degradado azul eléctrico → celeste usado en encabezados del estudiante. */
val GradienteEncabezado: Brush
    get() = Brush.linearGradient(listOf(TemaApp.paleta.marcaSombra, TemaApp.paleta.marca, TemaApp.paleta.ia))

// ============================================================================
// MASCOTA "RACHI" — llamita animada dibujada con Canvas (original de EduRacha)
// ============================================================================

enum class RachiAnimo { FELIZ, PENSANDO, CELEBRANDO, TRISTE }

@Composable
fun RachiMascota(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animo: RachiAnimo = RachiAnimo.FELIZ,
    descripcion: String = "Robi, tu compañero de estudio con IA"
) {
    // La mascota oficial ahora es Robi Neo (diseño Nexo). Se mantiene esta firma
    // para no tocar todas las pantallas que ya la usan.
    RobiNeo(
        modifier = modifier,
        tamano = size * 0.82f,
        expresion = when (animo) {
            RachiAnimo.FELIZ -> ExpresionRobi.NORMAL
            RachiAnimo.PENSANDO -> ExpresionRobi.PIENSA
            RachiAnimo.CELEBRANDO -> ExpresionRobi.CELEBRA
            RachiAnimo.TRISTE -> ExpresionRobi.TRISTE
        },
        descripcion = descripcion
    )
}

// ============================================================================
// BOTÓN "JUGOSO" CON PROFUNDIDAD 3D (se hunde al presionar)
// ============================================================================

@Composable
fun BotonJugoso(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PlayColors.Indigo,
    colorSombra: Color = PlayColors.IndigoSombra,
    colorTexto: Color = Color.White,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    alto: Dp = 56.dp,
    mayusculas: Boolean = true
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val profundidad = 5.dp
    val hundido by animateDpAsState(
        targetValue = if (presionado && habilitado) profundidad else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "hundido"
    )
    val base = if (habilitado) color else Color(0xFFDCE3EF)
    val sombra = if (habilitado) colorSombra else Color(0xFFC3CCDC)
    val textoColor = if (habilitado) colorTexto else Color(0xFF94A0B8)
    val forma = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(alto + profundidad)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .align(Alignment.BottomCenter)
                .clip(forma)
                .background(sombra)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .offset(y = hundido)
                .clip(forma)
                .background(base),
            contentAlignment = Alignment.Center
        ) {
            // Brillo superior sutil
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (habilitado) 0.22f else 0f))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, tint = textoColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = if (mayusculas) texto.uppercase() else texto,
                    color = textoColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================================
// CHIP DE ESTADÍSTICA (vidas, racha, XP) — rebota cuando cambia el valor
// ============================================================================

@Composable
fun ChipEstadistica(
    emoji: String,
    valor: String,
    color: Color,
    etiquetaAccesible: String,
    modifier: Modifier = Modifier,
    fondo: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    val rebote = remember { Animatable(1f) }
    LaunchedEffect(valor) {
        rebote.snapTo(0.8f)
        rebote.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val base = modifier
        .scale(rebote.value)
        .clip(RoundedCornerShape(50))
        .background(fondo)
        .border(2.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
        .semantics { contentDescription = etiquetaAccesible }
    Row(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(valor, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ============================================================================
// BARRA DE PROGRESO GRUESA CON BRILLO
// ============================================================================

@Composable
fun BarraProgresoJugosa(
    progreso: Float,
    modifier: Modifier = Modifier,
    color: Color = PlayColors.Menta,
    fondo: Color = Color(0xFFE1E8F3),
    alto: Dp = 16.dp
) {
    val animado by animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "progreso"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(CircleShape)
            .background(fondo)
    ) {
        if (animado > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth(animado)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 8.dp, end = 8.dp, top = 3.dp)
                        .fillMaxWidth()
                        .height(alto * 0.25f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}

// ============================================================================
// NODO DE LA RUTA DE APRENDIZAJE
// ============================================================================

enum class EstadoNodo { COMPLETADO, ACTUAL, DISPONIBLE }

@Composable
fun NodoRuta(
    numero: Int,
    estado: EstadoNodo,
    onClick: () -> Unit,
    descripcion: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 76.dp
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val profundidad = 7.dp
    val hundido by animateDpAsState(
        targetValue = if (presionado) profundidad else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "nodo"
    )
    val (cara, sombra) = when (estado) {
        EstadoNodo.COMPLETADO -> PlayColors.Sol to PlayColors.SolSombra
        EstadoNodo.ACTUAL -> PlayColors.Indigo to PlayColors.IndigoSombra
        EstadoNodo.DISPONIBLE -> PlayColors.IndigoSuave to PlayColors.IndigoSuaveSombra
    }

    val t = rememberInfiniteTransition(label = "pulso")
    val pulso by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing)),
        label = "anillo"
    )

    Box(
        modifier = modifier.size(tamano + 28.dp),
        contentAlignment = Alignment.Center
    ) {
        if (estado == EstadoNodo.ACTUAL) {
            Canvas(Modifier.size(tamano + 28.dp)) {
                val r = size.minDimension / 2
                drawCircle(
                    color = PlayColors.Indigo.copy(alpha = (1f - pulso) * 0.35f),
                    radius = r * (0.72f + 0.28f * pulso),
                    center = Offset(size.width / 2, size.height / 2 + 3.dp.toPx())
                )
                drawCircle(
                    color = PlayColors.Indigo.copy(alpha = 0.18f),
                    radius = r * 0.92f,
                    center = Offset(size.width / 2, size.height / 2 + 3.dp.toPx()),
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }
        Box(
            Modifier
                .size(width = tamano, height = tamano + profundidad)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = descripcion }
        ) {
            Box(
                Modifier
                    .size(tamano)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(sombra)
            )
            Box(
                Modifier
                    .size(tamano)
                    .offset(y = hundido)
                    .clip(CircleShape)
                    .background(cara),
                contentAlignment = Alignment.Center
            ) {
                // reflejo
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = tamano * 0.12f)
                        .size(width = tamano * 0.42f, height = tamano * 0.12f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f))
                )
                when (estado) {
                    EstadoNodo.COMPLETADO -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(tamano * 0.5f))
                    EstadoNodo.ACTUAL -> Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(tamano * 0.5f))
                    EstadoNodo.DISPONIBLE -> Text(
                        "$numero", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/** Globo de diálogo flotante (p. ej. "¡Empieza aquí!") que rebota suavemente. */
@Composable
fun GloboFlotante(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    colorTexto: Color = PlayColors.Indigo
) {
    val t = rememberInfiniteTransition(label = "globo")
    val y by t.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "y"
    )
    Column(
        modifier = modifier.graphicsLayer { translationY = y * density },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(color)
                .border(2.dp, Color(0xFFE1E8F3), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(texto.uppercase(), color = colorTexto, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
        }
        Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(p, color)
        }
    }
}

// ============================================================================
// FONDO CON FORMAS FLOTANTES (decorativo, liviano)
// ============================================================================

@Composable
fun FondoFormasFlotantes(
    modifier: Modifier = Modifier,
    colores: List<Color> = listOf(
        Color(0xFF2F6BFF).copy(alpha = 0.07f),
        Color(0xFFFFC531).copy(alpha = 0.10f),
        Color(0xFF14C8D6).copy(alpha = 0.08f),
        Color(0xFFFF5C8A).copy(alpha = 0.07f)
    )
) {
    val t = rememberInfiniteTransition(label = "fondo")
    val fase by t.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "fase"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val puntos = listOf(
            Triple(0.12f, 0.10f, 0.22f), Triple(0.88f, 0.22f, 0.16f),
            Triple(0.18f, 0.55f, 0.12f), Triple(0.82f, 0.70f, 0.20f),
            Triple(0.50f, 0.92f, 0.14f)
        )
        puntos.forEachIndexed { i, (x, y, r) ->
            val desfase = fase + i * 1.3f
            drawCircle(
                color = colores[i % colores.size],
                radius = w * r,
                center = Offset(w * x + cos(desfase) * w * 0.03f, h * y + sin(desfase) * h * 0.02f)
            )
        }
    }
}

// ============================================================================
// CONFETI DE CELEBRACIÓN (se reproduce una vez)
// ============================================================================

private data class Confeti(val x: Float, val vel: Float, val giro: Float, val color: Color, val ancho: Float, val retraso: Float)

@Composable
fun ConfetiCelebracion(modifier: Modifier = Modifier, activo: Boolean = true, cantidad: Int = 60) {
    if (!activo) return
    val piezas = remember(cantidad) {
        val colores = listOf(PlayColors.Indigo, PlayColors.Sol, PlayColors.Menta, PlayColors.Coral, PlayColors.Cielo, Color(0xFF8B5CF6))
        List(cantidad) {
            Confeti(
                x = Random.nextFloat(),
                vel = 0.7f + Random.nextFloat() * 0.6f,
                giro = Random.nextFloat() * 720f - 360f,
                color = colores[it % colores.size],
                ancho = 6f + Random.nextFloat() * 8f,
                retraso = Random.nextFloat() * 0.3f
            )
        }
    }
    val avance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        avance.animateTo(1f, tween(durationMillis = 3200, easing = LinearEasing))
    }
    Canvas(modifier.fillMaxSize()) {
        val p = avance.value
        piezas.forEach { c ->
            val local = ((p - c.retraso) / (1f - c.retraso)).coerceIn(0f, 1f)
            if (local > 0f && local < 1f) {
                val y = -40f + local * c.vel * (size.height + 80f)
                val x = c.x * size.width + sin(local * 10f + c.x * 6f) * 24f
                val alfa = if (local > 0.8f) (1f - local) / 0.2f else 1f
                rotate(degrees = c.giro * local, pivot = Offset(x, y)) {
                    drawRect(
                        color = c.color.copy(alpha = alfa),
                        topLeft = Offset(x - c.ancho / 2, y - c.ancho * 0.35f),
                        size = Size(c.ancho * density / 2f, c.ancho * 0.7f * density / 2f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// TARJETA DE MASCOTA CON MENSAJE (reemplaza el búho genérico)
// ============================================================================

@Composable
fun TarjetaRachi(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    animo: RachiAnimo = RachiAnimo.FELIZ
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GradienteEncabezado)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            RachiMascota(size = 72.dp, animo = animo)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(mensaje, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Start)
        }
    }
}
