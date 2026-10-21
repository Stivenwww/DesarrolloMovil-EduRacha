// ui/theme/NexoComponentes.kt
// Componentes visuales del diseño "Nexo". Solo presentación: no acceden a datos.
package com.stiven.sos.ui.theme

import androidx.compose.ui.graphics.compositeOver

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
// ROBI NEO — mascota animada (dibujada en Canvas, sin imágenes)
// ============================================================================

enum class ExpresionRobi { NORMAL, FELIZ, PIENSA, CELEBRA, TRISTE, SALUDA }

@Composable
fun RobiNeo(
    modifier: Modifier = Modifier,
    tamano: Dp = 120.dp,
    expresion: ExpresionRobi = ExpresionRobi.NORMAL,
    descripcion: String = "Robi, tu compañero de estudio con IA"
) {
    val p = LocalNexo.current
    val t = rememberInfiniteTransition(label = "robi")
    val flote by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (expresion == ExpresionRobi.CELEBRA) 520 else 1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "flote"
    )
    val parpadeo by t.animateFloat(
        initialValue = 1f, targetValue = 0.99f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3800
                0.1f at 3950
                1f at 4100
            }
        ), label = "parpadeo"
    )
    val pulso by t.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulso"
    )
    val mano by t.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mano"
    )

    val ojo = if (p.esOscuro) Color(0xFF3AF2FF) else Color(0xFF5CF2E0)
    val estrella = p.racha

    Canvas(
        modifier = modifier
            .size(width = tamano, height = tamano * (170f / 140f))
            .semantics { contentDescription = descripcion }
    ) {
        val s = size.width / 140f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        fun z(w: Float, h: Float) = Size(w * s, h * s)
        fun r(v: Float) = CornerRadius(v * s, v * s)
        val amplitud = if (expresion == ExpresionRobi.CELEBRA) 14f else 9f
        val dy = -flote * amplitud * s

        // Sombra en el piso
        val escalaSombra = 1f - flote * 0.25f
        drawOval(
            color = p.marca.copy(alpha = 0.35f * escalaSombra),
            topLeft = o(70f - 26f * escalaSombra, 160f),
            size = z(52f * escalaSombra, 7f)
        )

        translate(top = dy) {
            // Propulsor
            val jet = Path().apply {
                moveTo(58f * s, 140f * s); lineTo(82f * s, 140f * s); lineTo(76f * s, 160f * s); lineTo(64f * s, 160f * s); close()
            }
            drawPath(jet, p.marca.copy(alpha = 0.35f * pulso))
            drawOval(p.marca.copy(alpha = pulso), topLeft = o(56f, 136f), size = z(28f, 8f))

            // Antena
            drawCircle(p.marca.copy(alpha = 0.25f * pulso), radius = 12f * s, center = o(70f, 10f))
            drawLine(p.robotSombra, o(70f, 22f), o(70f, 12f), strokeWidth = 4f * s, cap = StrokeCap.Round)
            drawCircle(if (expresion == ExpresionRobi.CELEBRA) estrella else p.marca, radius = 6f * s, center = o(70f, 10f))

            // Audífonos
            drawRoundRect(p.ia, topLeft = o(14f, 42f), size = z(14f, 28f), cornerRadius = r(7f))
            drawRoundRect(p.marca.copy(alpha = pulso), topLeft = o(19f, 48f), size = z(4f, 16f), cornerRadius = r(2f))
            drawRoundRect(p.ia, topLeft = o(112f, 42f), size = z(14f, 28f), cornerRadius = r(7f))
            drawRoundRect(p.marca.copy(alpha = pulso), topLeft = o(117f, 48f), size = z(4f, 16f), cornerRadius = r(2f))

            // Cabeza
            drawRoundRect(p.robotSombra, topLeft = o(24f, 22f), size = z(92f, 72f), cornerRadius = r(34f))
            drawRoundRect(p.robotCuerpo, topLeft = o(24f, 20f), size = z(92f, 68f), cornerRadius = r(34f))
            if (p.robotContorno.alpha > 0f) {
                drawRoundRect(p.robotContorno, topLeft = o(24f, 20f), size = z(92f, 68f), cornerRadius = r(34f), style = Stroke(3f * s))
            }
            drawOval(Color.White.copy(alpha = 0.8f), topLeft = o(34f, 26f), size = z(24f, 8f))

            // Visor
            drawRoundRect(p.visor, topLeft = o(33f, 34f), size = z(74f, 42f), cornerRadius = r(21f))
            val brillo = Path().apply { moveTo(40f * s, 42f * s); quadraticBezierTo(50f * s, 36f * s, 64f * s, 37f * s) }
            drawPath(brillo, Color.White.copy(alpha = 0.25f), style = Stroke(3f * s, cap = StrokeCap.Round))

            dibujarCara(expresion, s, ojo, estrella, parpadeo)

            // Mejillas
            drawCircle(Color(0xFFFF9DC6), radius = 4f * s, center = o(38f, 82f))
            drawCircle(Color(0xFFFF9DC6), radius = 4f * s, center = o(102f, 82f))

            // Cuerpo
            drawRoundRect(p.robotSombra, topLeft = o(44f, 98f), size = z(52f, 40f), cornerRadius = r(20f))
            drawRoundRect(p.robotCuerpo, topLeft = o(44f, 96f), size = z(52f, 38f), cornerRadius = r(19f))
            if (p.robotContorno.alpha > 0f) {
                drawRoundRect(p.robotContorno, topLeft = o(44f, 96f), size = z(52f, 38f), cornerRadius = r(19f), style = Stroke(3f * s))
            }
            drawRoundRect(p.ia, topLeft = o(54f, 104f), size = z(32f, 18f), cornerRadius = r(9f))
            drawCircle(p.marca.copy(alpha = pulso), radius = 5f * s, center = o(70f, 113f))

            // Manos flotantes
            fun dibujarMano(c: Offset) {
                drawCircle(p.robotCuerpo, radius = 9f * s, center = c)
                if (p.robotContorno.alpha > 0f) drawCircle(p.robotContorno, radius = 9f * s, center = c, style = Stroke(3f * s))
                drawCircle(p.ia, radius = 3f * s, center = c)
            }
            when (expresion) {
                ExpresionRobi.CELEBRA -> {
                    dibujarMano(o(18f, 82f + mano * 4f))
                    dibujarMano(o(122f, 82f - mano * 4f))
                }
                ExpresionRobi.SALUDA -> {
                    dibujarMano(o(30f, 114f))
                    rotate(degrees = mano * 22f, pivot = o(104f, 104f)) { dibujarMano(o(120f, 86f)) }
                }
                ExpresionRobi.TRISTE -> {
                    dibujarMano(o(32f, 120f)); dibujarMano(o(108f, 120f))
                }
                else -> {
                    dibujarMano(o(30f, 112f + mano * 3f))
                    dibujarMano(o(110f, 112f - mano * 3f))
                }
            }
        }
    }
}

private fun DrawScope.dibujarCara(e: ExpresionRobi, s: Float, ojo: Color, estrella: Color, parpadeo: Float) {
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val trazo = Stroke(width = 4.5f * s, cap = StrokeCap.Round)
    when (e) {
        ExpresionRobi.FELIZ -> {
            arco(o(44f, 58f), o(52f, 47f), o(60f, 58f), ojo, trazo)
            arco(o(80f, 58f), o(88f, 47f), o(96f, 58f), ojo, trazo)
            bocaLlena(s, ojo)
        }
        ExpresionRobi.CELEBRA -> {
            drawPath(estrellaPath(o(52f, 53f), 9f * s), estrella)
            drawPath(estrellaPath(o(88f, 53f), 9f * s), estrella)
            bocaLlena(s, ojo)
        }
        ExpresionRobi.PIENSA -> {
            drawRoundRect(ojo, topLeft = o(50f, 42f), size = Size(13f * s, 17f * s), cornerRadius = CornerRadius(6.5f * s))
            drawRoundRect(ojo, topLeft = o(85f, 42f), size = Size(13f * s, 17f * s), cornerRadius = CornerRadius(6.5f * s))
            drawCircle(ojo, radius = 3f * s, center = o(73f, 68f), style = Stroke(2.5f * s))
        }
        ExpresionRobi.TRISTE -> {
            val azul = Color(0xFF8FB8FF)
            drawRoundRect(azul, topLeft = o(46f, 52f), size = Size(13f * s, 9f * s), cornerRadius = CornerRadius(4.5f * s))
            drawRoundRect(azul, topLeft = o(81f, 52f), size = Size(13f * s, 9f * s), cornerRadius = CornerRadius(4.5f * s))
            arco(o(62f, 72f), o(70f, 66f), o(78f, 72f), azul, Stroke(3f * s, cap = StrokeCap.Round))
        }
        else -> {
            val alto = 17f * parpadeo
            val y = 54.5f - alto / 2f
            drawRoundRect(ojo, topLeft = o(46f, y), size = Size(13f * s, alto * s), cornerRadius = CornerRadius(6.5f * s))
            drawRoundRect(ojo, topLeft = o(81f, y), size = Size(13f * s, alto * s), cornerRadius = CornerRadius(6.5f * s))
            if (parpadeo > 0.5f) {
                drawCircle(Color.White, radius = 2f * s, center = o(55f, 50f))
                drawCircle(Color.White, radius = 2f * s, center = o(90f, 50f))
            }
            arco(o(62f, 68f), o(70f, 73f), o(78f, 68f), ojo, Stroke(3f * s, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.arco(a: Offset, c: Offset, b: Offset, color: Color, trazo: Stroke) {
    val path = Path().apply { moveTo(a.x, a.y); quadraticBezierTo(c.x, c.y, b.x, b.y) }
    drawPath(path, color, style = trazo)
}

private fun DrawScope.bocaLlena(s: Float, color: Color) {
    val path = Path().apply {
        moveTo(60f * s, 64f * s); quadraticBezierTo(70f * s, 77f * s, 80f * s, 64f * s); close()
    }
    drawPath(path, color)
}

private fun estrellaPath(centro: Offset, radio: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radio else radio * 0.45f
        val ang = -PI / 2 + i * PI / 5
        val x = centro.x + (cos(ang) * r).toFloat()
        val y = centro.y + (sin(ang) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ============================================================================
// FONDO NEXO — degradado, estrellas (oscuro) o manchas de color (claro), cuadrícula
// ============================================================================

@Composable
fun FondoNexo(
    modifier: Modifier = Modifier,
    conCuadricula: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p = LocalNexo.current
    Box(modifier.fillMaxSize().background(p.fondo)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    colors = listOf(p.fondoCentro, p.fondo),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = maxOf(w, h) * 0.7f
                )
            )
            if (p.esOscuro) {
                val rnd = Random(7)
                repeat(26) {
                    drawCircle(
                        Color.White.copy(alpha = 0.25f + rnd.nextFloat() * 0.5f),
                        radius = 1f + rnd.nextFloat() * 1.6f,
                        center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.7f)
                    )
                }
            } else {
                val c = p.decoracion
                drawCircle(c[0].copy(alpha = 0.12f), radius = w * 0.34f, center = Offset(w * 0.08f, h * 0.16f))
                drawCircle(c[1].copy(alpha = 0.45f), radius = w * 0.30f, center = Offset(w * 0.95f, h * 0.10f))
                drawCircle(c[2].copy(alpha = 0.10f), radius = w * 0.28f, center = Offset(w * 0.88f, h * 0.78f))
            }
            if (conCuadricula) {
                val color = p.marca.copy(alpha = 0.10f)
                val horizonte = h * 0.82f
                for (i in 0..3) {
                    val y = horizonte + (h - horizonte) * (i / 3f) * (i / 3f)
                    drawLine(color, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }
                for (i in -3..3) {
                    drawLine(color, Offset(w / 2f, horizonte), Offset(w / 2f + i * w * 0.4f, h), strokeWidth = 1.5f)
                }
            }
        }
        content()
    }
}

// ============================================================================
// ISLA FLOTANTE
// ============================================================================

enum class TipoIsla { ACTIVA, CONQUISTADA, BLOQUEADA }

@Composable
fun IslaFlotante(
    modifier: Modifier = Modifier,
    tipo: TipoIsla = TipoIsla.ACTIVA,
    fase: Int = 0
) {
    val p = LocalNexo.current
    val t = rememberInfiniteTransition(label = "isla")
    val y by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600 + fase * 400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "y"
    )
    val (pasto, pastoSombra) = when (tipo) {
        TipoIsla.ACTIVA -> p.accion to p.accionSombra
        TipoIsla.CONQUISTADA -> p.racha to p.rachaSombra
        TipoIsla.BLOQUEADA -> p.bloqueado to p.bloqueadoSombra
    }
    Canvas(modifier.graphicsLayer { translationY = -y * 6.dp.toPx() }) {
        val w = size.width
        val h = size.height
        val arriba = h * 0.28f
        // roca inferior
        val roca = Path().apply {
            moveTo(0f, arriba)
            cubicTo(w * 0.08f, h * 0.7f, w * 0.35f, h * 0.95f, w * 0.5f, h)
            cubicTo(w * 0.65f, h * 0.95f, w * 0.92f, h * 0.7f, w, arriba)
            close()
        }
        drawPath(roca, Brush.verticalGradient(listOf(p.islaRoca, p.islaRoca.copy(alpha = 0f)), startY = arriba, endY = h))
        if (tipo != TipoIsla.BLOQUEADA) {
            val cristal = Path().apply { moveTo(w * 0.42f, h * 0.45f); lineTo(w * 0.46f, h * 0.72f); lineTo(w * 0.5f, h * 0.44f); close() }
            drawPath(cristal, p.marca.copy(alpha = 0.85f))
            val cristal2 = Path().apply { moveTo(w * 0.6f, h * 0.42f); lineTo(w * 0.63f, h * 0.62f); lineTo(w * 0.67f, h * 0.41f); close() }
            drawPath(cristal2, p.ia.copy(alpha = 0.9f))
        }
        // superficie
        drawOval(pastoSombra, topLeft = Offset(0f, arriba - h * 0.16f), size = Size(w, h * 0.34f))
        drawOval(pasto, topLeft = Offset(0f, arriba - h * 0.2f), size = Size(w, h * 0.32f))
        drawOval(Color.White.copy(alpha = 0.25f), topLeft = Offset(w * 0.12f, arriba - h * 0.16f), size = Size(w * 0.5f, h * 0.08f))
        if (tipo == TipoIsla.ACTIVA) {
            drawOval(
                p.marca.copy(alpha = 0.35f),
                topLeft = Offset(-w * 0.06f, arriba - h * 0.24f),
                size = Size(w * 1.12f, h * 0.42f),
                style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 14f), y * 28f))
            )
        }
    }
}

// ============================================================================
// NODO HEXAGONAL DE LECCIÓN
// ============================================================================

enum class EstadoHex { COMPLETADO, ACTUAL, DISPONIBLE, BLOQUEADO }

@Composable
fun HexNodo(
    estado: EstadoHex,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tamano: Dp = 72.dp,
    etiqueta: String? = null
) {
    val p = LocalNexo.current
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val hundido by animateDpAsState(if (presionado) 4.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "hex")
    val t = rememberInfiniteTransition(label = "onda")
    val onda by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "onda")

    val (cara, sombra) = when (estado) {
        EstadoHex.COMPLETADO -> p.racha to p.rachaSombra
        EstadoHex.ACTUAL -> p.marca to p.marcaSombra
        EstadoHex.DISPONIBLE -> p.ia to p.iaSombra
        EstadoHex.BLOQUEADO -> p.bloqueado to p.bloqueadoSombra
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(tamano * 1.3f)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    enabled = estado != EstadoHex.BLOQUEADO,
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = descripcion },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(tamano * 1.3f)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f / 1.3f
                if (estado == EstadoHex.ACTUAL) {
                    drawPath(hexPath(c, r * (1f + 0.3f * onda)), p.marca.copy(alpha = 1f - onda), style = Stroke(3.dp.toPx()))
                }
                drawPath(hexPath(c.copy(y = c.y + 5.dp.toPx()), r), sombra)
                drawPath(hexPath(c.copy(y = c.y + hundido.toPx()), r), cara)
                if (estado == EstadoHex.ACTUAL) {
                    drawPath(hexPath(c.copy(y = c.y + hundido.toPx()), r * 0.68f), p.fondo)
                }
            }
            val icono: ImageVector? = when (estado) {
                EstadoHex.COMPLETADO -> Icons.Filled.Check
                EstadoHex.ACTUAL -> Icons.Filled.PlayArrow
                EstadoHex.BLOQUEADO -> Icons.Filled.Lock
                EstadoHex.DISPONIBLE -> Icons.Filled.PlayArrow
            }
            val tinte = when (estado) {
                EstadoHex.ACTUAL -> p.marca
                EstadoHex.BLOQUEADO -> p.textoTenue
                EstadoHex.COMPLETADO -> p.sobreMarca
                EstadoHex.DISPONIBLE -> Color.White
            }
            if (icono != null) {
                Icon(icono, null, tint = tinte, modifier = Modifier.offset(y = hundido).size(tamano * 0.42f))
            }
        }
        if (etiqueta != null) {
            val fondoEtiqueta = if (estado == EstadoHex.ACTUAL) p.marca else p.superficie
            val textoEtiqueta = if (estado == EstadoHex.ACTUAL) p.sobreMarca else p.textoSuave
            Text(
                etiqueta.uppercase(),
                color = textoEtiqueta,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(fondoEtiqueta)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun hexPath(c: Offset, r: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val ang = -PI / 2 + i * PI / 3
        val x = c.x + (cos(ang) * r).toFloat()
        val y = c.y + (sin(ang) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ============================================================================
// BOTÓN NEXO (3D, se hunde al presionar)
// ============================================================================

enum class TipoBoton { ACCION, MARCA, IA, PELIGRO, SECUNDARIO }

@Composable
fun BotonNexo(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipo: TipoBoton = TipoBoton.ACCION,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    alto: Dp = 56.dp
) {
    val p = LocalNexo.current
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val activo = habilitado && !cargando
    val profundidad = 5.dp
    val hundido by animateDpAsState(
        if (presionado && activo) profundidad else 0.dp,
        spring(stiffness = Spring.StiffnessHigh), label = "boton"
    )
    val (cara, sombra, textoColor) = when (tipo) {
        TipoBoton.ACCION -> Triple(p.accion, p.accionSombra, p.sobreAccion)
        TipoBoton.MARCA -> Triple(p.marca, p.marcaSombra, p.sobreMarca)
        TipoBoton.IA -> Triple(p.ia, p.iaSombra, Color.White)
        TipoBoton.PELIGRO -> Triple(p.error, p.errorSombra, Color.White)
        // Secundario sin borde gris: fondo suave del color de marca
        TipoBoton.SECUNDARIO -> Triple(p.marca.copy(alpha = if (p.esOscuro) 0.18f else 0.12f).compositeOver(p.superficie), p.marca.copy(alpha = 0.30f).compositeOver(p.superficie), p.marcaTexto)
    }
    val caraFinal = if (habilitado) cara else p.borde
    val sombraFinal = if (habilitado) sombra else p.borde
    val textoFinal = if (habilitado) textoColor else p.textoTenue
    val forma = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(alto + profundidad)
            .clickable(interactionSource = interaccion, indication = null, enabled = activo, role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(alto).align(Alignment.BottomCenter).clip(forma).background(sombraFinal))
        Box(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .offset(y = hundido)
                .clip(forma)
                .background(caraFinal)
                ,
            contentAlignment = Alignment.Center
        ) {
            if (cargando) {
                CircularProgressIndicator(color = textoFinal, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (icono != null) {
                        Icon(icono, null, tint = textoFinal, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        texto.uppercase(),
                        color = textoFinal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// TARJETA, CHIP, BARRA, ESTRELLAS
// ============================================================================

@Composable
fun TarjetaNexo(
    modifier: Modifier = Modifier,
    acento: Color? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = LocalNexo.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = p.superficie,
        border = BorderStroke(1.dp, acento?.copy(alpha = 0.55f) ?: p.borde),
        shadowElevation = if (p.esOscuro) 0.dp else 2.dp
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun ChipNexo(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    descripcion: String? = null,
    onClick: (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val base = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(p.superficie)
        .border(1.dp, p.borde, RoundedCornerShape(12.dp))
        .then(if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier)
    Row(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icono != null) Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
        Text(texto, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun BarraNexo(
    progreso: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalNexo.current.accion,
    alto: Dp = 10.dp
) {
    val p = LocalNexo.current
    Box(modifier.fillMaxWidth().height(alto).clip(CircleShape).background(p.borde)) {
        val v = progreso.coerceIn(0f, 1f)
        if (v > 0.001f) {
            Box(Modifier.fillMaxWidth(v).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

/** Estrellas que aparecen una por una con efecto "pop". [ganadas] entre 0 y 3. */
@Composable
fun EstrellasNexo(ganadas: Int, modifier: Modifier = Modifier, tamano: Dp = 56.dp) {
    val p = LocalNexo.current
    Row(
        modifier.semantics { contentDescription = "$ganadas de 3 estrellas" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until 3) {
            val escala = remember { Animatable(0f) }
            LaunchedEffect(ganadas) {
                escala.snapTo(0f)
                kotlinx.coroutines.delay(150L + i * 220L)
                escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            val grande = i == 1
            val color = if (i < ganadas) p.racha else p.borde
            Canvas(
                Modifier
                    .size(if (grande) tamano * 1.3f else tamano)
                    .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            ) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawPath(estrellaPath(c.copy(y = c.y + 2.dp.toPx()), size.minDimension / 2f), if (i < ganadas) p.rachaSombra else p.borde)
                drawPath(estrellaPath(c, size.minDimension / 2f), color)
            }
        }
    }
}

// ============================================================================
// SELECTOR DE TEMA (Sistema / Claro / Oscuro)
// ============================================================================

@Composable
fun SelectorTemaNexo(modifier: Modifier = Modifier) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val opciones = listOf(
        Triple(PreferenciaTema.SISTEMA, "Sistema", Icons.Filled.PhoneAndroid),
        Triple(PreferenciaTema.CLARO, "Claro", Icons.Filled.LightMode),
        Triple(PreferenciaTema.OSCURO, "Oscuro", Icons.Filled.DarkMode)
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.superficieAlt)
            .border(1.dp, p.borde, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opciones.forEach { (valor, texto, icono) ->
            val activo = TemaApp.preferencia == valor
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activo) p.marca else Color.Transparent)
                    .clickable(role = Role.RadioButton) { TemaApp.cambiarTema(context, valor) }
                    .semantics { contentDescription = "Tema $texto" + if (activo) ", seleccionado" else "" },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, null, tint = if (activo) p.sobreMarca else p.textoSuave, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(texto, color = if (activo) p.sobreMarca else p.textoSuave, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
