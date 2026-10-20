// ui/theme/NexoPantallas.kt
// Piezas de pantalla reutilizables del diseño "Nexo": encabezado, diálogos, títulos,
// globo de Robi, estados vacíos. Solo presentación (sin acceso a datos).
package com.stiven.sos.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Botón cuadrado redondeado con icono (volver, ajustes, etc.). Área táctil de 48dp. */
@Composable
fun BotonIconoNexo(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val p = LocalNexo.current
    Box(
        modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.superficie)
            .border(1.dp, p.borde, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, null, tint = color ?: p.texto, modifier = Modifier.size(22.dp))
    }
}

/** Barra superior: volver + título/subtítulo + acciones opcionales a la derecha. */
@Composable
fun EncabezadoNexo(
    titulo: String,
    onVolver: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    val p = LocalNexo.current
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onVolver != null) {
            BotonIconoNexo(Icons.Filled.ArrowBack, "Volver", onVolver)
        }
        Column(Modifier.weight(1f)) {
            if (subtitulo != null) {
                Text(
                    subtitulo.uppercase(),
                    color = p.marcaTexto,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                titulo,
                color = p.texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        acciones()
    }
}

/** Título de sección con icono. */
@Composable
fun TituloSeccionNexo(
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    color: Color? = null
) {
    val p = LocalNexo.current
    Row(
        modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icono != null) Icon(icono, null, tint = color ?: p.marcaTexto, modifier = Modifier.size(20.dp))
        Text(texto, color = p.texto, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

/** Icono dentro de un cuadro redondeado con el color de acento suave. */
@Composable
fun InsigniaNexo(icono: ImageVector, color: Color, modifier: Modifier = Modifier, tamano: Dp = 44.dp) {
    Box(
        modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.32f))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(tamano * 0.55f))
    }
}

/** Robi con un globo de texto (consejos, ánimo, retroalimentación). */
@Composable
fun GloboRobi(
    mensaje: String,
    modifier: Modifier = Modifier,
    expresion: ExpresionRobi = ExpresionRobi.NORMAL,
    tamanoRobi: Dp = 76.dp,
    titulo: String? = null
) {
    val p = LocalNexo.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RobiNeo(tamano = tamanoRobi, expresion = expresion)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = p.superficie,
            border = BorderStroke(1.dp, p.ia.copy(alpha = 0.45f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(Modifier.padding(14.dp)) {
                if (titulo != null) {
                    Text(titulo, color = p.iaTexto, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(2.dp))
                }
                Text(mensaje, color = p.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
            }
        }
    }
}

/** Estado vacío o de error con Robi y una acción opcional. */
@Composable
fun EstadoVacioNexo(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    expresion: ExpresionRobi = ExpresionRobi.PIENSA,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    val p = LocalNexo.current
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RobiNeo(tamano = 120.dp, expresion = expresion)
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = p.texto, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(mensaje, color = p.textoSuave, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 21.sp)
        if (textoAccion != null && onAccion != null) {
            Spacer(Modifier.height(18.dp))
            BotonNexo(texto = textoAccion, onClick = onAccion, tipo = TipoBoton.MARCA, modifier = Modifier.fillMaxWidth(0.8f))
        }
    }
}

/**
 * Diálogo informativo estándar (vidas, racha, XP, avisos).
 * [valor] y [unidad] son opcionales para mostrar una cifra grande.
 */
@Composable
fun DialogoNexo(
    icono: ImageVector,
    color: Color,
    titulo: String,
    mensaje: String,
    onDismiss: () -> Unit,
    valor: String? = null,
    unidad: String? = null,
    nota: String? = null,
    textoBoton: String = "Entendido",
    tipoBoton: TipoBoton = TipoBoton.MARCA
) {
    val p = LocalNexo.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = p.superficie,
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InsigniaNexo(icono, color, tamano = 72.dp)
                Spacer(Modifier.height(14.dp))
                Text(titulo, color = p.texto, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                if (valor != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(valor, color = color, fontSize = 44.sp, fontWeight = FontWeight.Black)
                        if (unidad != null) {
                            Text(unidad, color = p.textoSuave, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color.copy(alpha = 0.10f))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(mensaje, color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        if (nota != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(nota, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                BotonNexo(texto = textoBoton, onClick = onDismiss, tipo = tipoBoton, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Píldora de estadística grande (icono + valor + etiqueta). */
@Composable
fun StatNexo(
    icono: ImageVector,
    valor: String,
    etiqueta: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val base = modifier
        .clip(RoundedCornerShape(18.dp))
        .background(p.superficie)
        .border(1.dp, p.borde, RoundedCornerShape(18.dp))
        .semantics(mergeDescendants = true) { contentDescription = "$etiqueta: $valor" }
    Column(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor, color = p.texto, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(etiqueta, color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Fila de lista/menú con insignia de color, título, subtítulo y chevron opcional. */
@Composable
fun FilaNexo(
    icono: ImageVector,
    color: Color,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onClick: (() -> Unit)? = null,
    extremo: @Composable (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(p.superficie)
        .border(1.dp, p.borde, RoundedCornerShape(18.dp))
    Row(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InsigniaNexo(icono, color)
        Column(Modifier.weight(1f)) {
            Text(titulo, color = p.texto, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) {
                Text(subtitulo, color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (extremo != null) extremo()
    }
}

/** Círculo con iniciales (avatar sin foto). */
@Composable
fun AvatarNexo(nombre: String, color: Color, modifier: Modifier = Modifier, tamano: Dp = 44.dp) {
    val iniciales = nombre.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    Box(
        modifier.size(tamano).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales, color = Color.White, fontWeight = FontWeight.Black, fontSize = (tamano.value * 0.36f).sp)
    }
}

/**
 * Diálogo de decisión con acción principal y secundaria opcional.
 * [contenidoExtra] permite insertar un bloque propio (por ejemplo, un temporizador).
 * Si [cerrable] es false, tocar fuera no lo cierra.
 */
@Composable
fun DialogoAccionNexo(
    icono: ImageVector,
    color: Color,
    titulo: String,
    mensaje: String,
    textoPrincipal: String,
    onPrincipal: () -> Unit,
    onDismiss: () -> Unit,
    tipoPrincipal: TipoBoton = TipoBoton.MARCA,
    iconoPrincipal: ImageVector? = null,
    textoSecundario: String? = null,
    onSecundario: (() -> Unit)? = null,
    cerrable: Boolean = true,
    expresionRobi: ExpresionRobi? = null,
    contenidoExtra: @Composable (() -> Unit)? = null
) {
    val p = LocalNexo.current
    Dialog(
        onDismissRequest = { if (cerrable) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = cerrable,
            dismissOnClickOutside = cerrable
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = p.superficie,
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (expresionRobi != null) {
                    RobiNeo(tamano = 96.dp, expresion = expresionRobi)
                } else {
                    InsigniaNexo(icono, color, tamano = 72.dp)
                }
                Spacer(Modifier.height(14.dp))
                Text(titulo, color = p.texto, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 28.sp)
                Spacer(Modifier.height(8.dp))
                Text(mensaje, color = p.textoSuave, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 21.sp)
                if (contenidoExtra != null) {
                    Spacer(Modifier.height(14.dp))
                    contenidoExtra()
                }
                Spacer(Modifier.height(20.dp))
                BotonNexo(
                    texto = textoPrincipal,
                    onClick = onPrincipal,
                    tipo = tipoPrincipal,
                    icono = iconoPrincipal,
                    modifier = Modifier.fillMaxWidth()
                )
                if (textoSecundario != null && onSecundario != null) {
                    Spacer(Modifier.height(10.dp))
                    BotonNexo(
                        texto = textoSecundario,
                        onClick = onSecundario,
                        tipo = TipoBoton.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Pantalla completa de espera con Robi pensando. */
@Composable
fun CargandoNexo(titulo: String, mensaje: String, color: Color? = null) {
    val p = LocalNexo.current
    FondoNexo {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RobiNeo(tamano = 140.dp, expresion = ExpresionRobi.PIENSA)
            Spacer(Modifier.height(20.dp))
            Text(titulo, color = p.texto, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(mensaje, color = p.textoSuave, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.LinearProgressIndicator(
                color = color ?: p.marca,
                trackColor = p.borde,
                modifier = Modifier.fillMaxWidth(0.6f).height(8.dp).clip(CircleShape)
            )
        }
    }
}

/** Buscador redondeado del diseño Nexo. */
@Composable
fun BuscadorNexo(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.superficie)
            .border(1.dp, p.borde, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = p.textoSuave, modifier = Modifier.size(22.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = p.texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(p.marca),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = placeholder },
            decorationBox = { campo ->
                if (valor.isEmpty()) {
                    Text(placeholder, color = p.textoTenue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                campo()
            }
        )
        if (valor.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Limpiar búsqueda",
                tint = p.textoSuave,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onCambio("") }
            )
        }
    }
}

/** Colores de acento rotativos para tarjetas de curso (identidad por curso). */
@Composable
fun acentoCurso(indice: Int): Pair<Color, Color> {
    val p = LocalNexo.current
    return when (indice % 4) {
        0 -> p.marca to p.marcaSombra
        1 -> p.accion to p.accionSombra
        2 -> p.ia to p.iaSombra
        else -> p.racha to p.rachaSombra
    }
}

/**
 * Tarjeta de curso: cabecera tipo "isla" con el color del curso, temas y acción principal.
 * [chip] muestra un estado opcional (por ejemplo "Pendiente").
 */
@Composable
fun TarjetaCursoNexo(
    titulo: String,
    codigo: String,
    descripcion: String?,
    temas: List<String>,
    indice: Int,
    textoAccion: String,
    iconoAccion: ImageVector,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    tipoAccion: TipoBoton = TipoBoton.ACCION,
    chip: String? = null,
    onClickTarjeta: (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val (color, sombra) = acentoCurso(indice)
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(p.superficie)
        .border(1.dp, p.borde, RoundedCornerShape(22.dp))
    Column(if (onClickTarjeta != null) base.clickable(role = Role.Button, onClick = onClickTarjeta) else base) {
        // Cabecera
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(sombra, color))
                )
        ) {
            IslaFlotante(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 4.dp)
                    .size(width = 110.dp, height = 46.dp),
                tipo = TipoIsla.CONQUISTADA,
                fase = indice % 3
            )
            Column(Modifier.padding(16.dp).fillMaxWidth(0.72f)) {
                Text(
                    codigo.uppercase(),
                    color = p.sobreAccion.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    titulo,
                    color = p.sobreAccion,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chip != null) {
                Text(
                    chip,
                    color = p.sobreAccion,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            if (!descripcion.isNullOrBlank()) {
                Text(
                    descripcion,
                    color = p.textoSuave,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
            }
            if (temas.isNotEmpty()) {
                Text(
                    "${temas.size} ${if (temas.size == 1) "ISLA" else "ISLAS"} DE CONTENIDO",
                    color = p.textoTenue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                temas.take(3).forEachIndexed { i, tema ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(color.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", color = p.texto, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(tema, color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (temas.size > 3) {
                    Text(
                        "+${temas.size - 3} más",
                        color = p.textoSuave,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            BotonNexo(
                texto = textoAccion,
                onClick = onAccion,
                habilitado = habilitado,
                tipo = tipoAccion,
                icono = iconoAccion,
                alto = 50.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Aparición escalonada (deslizar + desvanecer) para elementos de listas. */
@Composable
fun EntradaEscalonada(indice: Int, content: @Composable () -> Unit) {
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((indice.coerceAtMost(8) * 70).toLong())
        visible = true
    }
    val t by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(380),
        label = "entrada"
    )
    Box(Modifier.graphicsLayerCompat(t)) { content() }
}

private fun Modifier.graphicsLayerCompat(t: Float): Modifier =
    this.then(Modifier.graphicsLayer { alpha = t; translationY = (1f - t) * 40f })
