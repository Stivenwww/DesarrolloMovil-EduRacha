// ui/theme/NexoFormularios.kt
// Piezas de formulario del diseño Nexo: campos, selectores en hoja inferior, pasos, segmentos
// y el contenedor de ancho máximo que hace las pantallas responsive (celular, plegable y tablet).
package com.stiven.sos.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ancho máximo del contenido: en tablet o celular horizontal el formulario no se estira de borde a borde. */
val ANCHO_CONTENIDO_NEXO: Dp = 720.dp

/** Centra el contenido y limita su ancho a [ANCHO_CONTENIDO_NEXO]. */
@Composable
fun ContenidoCentradoNexo(
    modifier: Modifier = Modifier,
    anchoMaximo: Dp = ANCHO_CONTENIDO_NEXO,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = anchoMaximo).fillMaxWidth(), content = content)
    }
}

/** Paso numerado de un flujo ("1 · Elige el tema"). */
@Composable
fun PasoNexo(numero: Int, titulo: String, modifier: Modifier = Modifier, color: Color? = null, completo: Boolean = false, detalle: String? = null) {
    val p = LocalNexo.current
    val c = color ?: p.marca
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (completo) p.accion else c),
            contentAlignment = Alignment.Center
        ) {
            if (completo) Icon(Icons.Filled.Check, null, tint = p.sobreAccion, modifier = Modifier.size(18.dp))
            else Text("$numero", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = p.texto, fontSize = 17.sp, fontWeight = FontWeight.Black)
            if (detalle != null) Text(detalle, color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Campo de texto con los colores Nexo (borde de marca al enfocar, error en coral). */
@Composable
fun CampoNexo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    lineasMinimas: Int = 1,
    lineasMaximas: Int = Int.MAX_VALUE,
    error: Boolean = false,
    apoyo: String? = null,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    colorFoco: Color? = null,
    unaLinea: Boolean = false
) {
    val p = LocalNexo.current
    val foco = colorFoco ?: p.marca
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(),
        minLines = if (unaLinea) 1 else lineasMinimas,
        maxLines = if (unaLinea) 1 else lineasMaximas,
        singleLine = unaLinea,
        isError = error,
        enabled = habilitado,
        leadingIcon = icono?.let { { Icon(it, null, tint = if (error) p.error else foco) } },
        supportingText = apoyo?.let { { Text(it, color = if (error) p.errorTexto else p.textoSuave) } },
        shape = RoundedCornerShape(16.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = p.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = foco,
            unfocusedBorderColor = p.borde,
            focusedLabelColor = foco,
            unfocusedLabelColor = p.textoSuave,
            cursorColor = foco,
            focusedContainerColor = p.superficie,
            unfocusedContainerColor = p.superficie,
            disabledContainerColor = p.superficieAlt,
            errorBorderColor = p.error,
            errorLabelColor = p.errorTexto,
            errorContainerColor = p.superficie,
            focusedTextColor = p.texto,
            unfocusedTextColor = p.texto
        )
    )
}

/** Fila que abre un selector: muestra la etiqueta y el valor elegido (o el texto de ayuda). */
@Composable
fun SelectorNexo(
    etiqueta: String,
    valor: String?,
    ayuda: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    extremo: @Composable (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val elegido = valor != null
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.superficie)
            .border(if (elegido) 1.5.dp else 1.dp, if (elegido) color.copy(alpha = 0.7f) else p.borde, RoundedCornerShape(18.dp))
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$etiqueta: ${valor ?: ayuda}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InsigniaNexo(icono, if (habilitado) color else p.textoTenue)
        Column(Modifier.weight(1f)) {
            Text(etiqueta.uppercase(), color = p.textoSuave, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(
                valor ?: ayuda,
                color = if (elegido) p.texto else p.textoTenue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (extremo != null) extremo()
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = p.textoSuave)
    }
}

/** Una opción de la hoja de selección. */
data class OpcionHojaNexo<T>(val valor: T, val titulo: String, val subtitulo: String? = null, val etiqueta: String? = null, val colorEtiqueta: Color? = null)

/** Hoja inferior para elegir un elemento de una lista (cursos, temas…). Más cómoda en el celular que un diálogo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HojaSeleccionNexo(
    titulo: String,
    opciones: List<OpcionHojaNexo<T>>,
    onElegir: (T) -> Unit,
    onCerrar: () -> Unit,
    icono: ImageVector,
    color: Color,
    vacio: String = "No hay elementos para mostrar.",
    cargando: Boolean = false,
    seleccionado: T? = null
) {
    val p = LocalNexo.current
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estado,
        containerColor = p.fondo,
        contentColor = p.texto,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        ContenidoCentradoNexo {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsigniaNexo(icono, color, tamano = 40.dp)
                    Text(titulo, color = p.texto, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.semantics { heading() })
                }
                Spacer(Modifier.height(12.dp))
                when {
                    cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = color)
                    }
                    opciones.isEmpty() -> Text(
                        vacio, color = p.textoSuave, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(opciones) { o ->
                            val activo = o.valor == seleccionado
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (activo) color.copy(alpha = 0.12f) else p.superficie)
                                    .border(1.dp, if (activo) color else p.borde, RoundedCornerShape(16.dp))
                                    .clickable(role = Role.Button) { onElegir(o.valor) }
                                    .semantics { selected = activo }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(o.titulo, color = p.texto, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (o.subtitulo != null) {
                                        Text(o.subtitulo, color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (o.etiqueta != null) EtiquetaEstadoNexo(o.etiqueta, o.colorEtiqueta ?: color)
                                if (activo) Icon(Icons.Filled.Check, null, tint = color)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Etiqueta pequeña de estado ("Aprobada", "Pendiente", "Sin explicación"). */
@Composable
fun EtiquetaEstadoNexo(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Control segmentado (dos o tres opciones excluyentes) con el estilo Nexo. */
@Composable
fun <T> SegmentadoNexo(
    opciones: List<Triple<T, String, ImageVector>>,
    seleccion: T?,
    onCambio: (T) -> Unit,
    modifier: Modifier = Modifier,
    colores: ((T) -> Color)? = null
) {
    val p = LocalNexo.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.superficieAlt)
            .border(1.dp, p.borde, RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opciones.forEach { (valor, texto, icono) ->
            val activo = valor == seleccion
            val c = colores?.invoke(valor) ?: p.marca
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (activo) c else Color.Transparent)
                    .clickable(role = Role.Tab) { onCambio(valor) }
                    .semantics { selected = activo }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icono, null, tint = if (activo) Color.White else p.textoSuave, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(texto, color = if (activo) Color.White else p.textoSuave, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

/** Ficha seleccionable pequeña (dificultad, cantidad rápida…). */
@Composable
fun FichaNexo(texto: String, activa: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalNexo.current
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (activa) color.copy(alpha = 0.16f) else p.superficie)
            .border(if (activa) 1.5.dp else 1.dp, if (activa) color else p.borde, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = activa }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = if (activa) color else p.textoSuave, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** Caja de nota dentro de una tarjeta (información, advertencia o error). */
@Composable
fun NotaNexo(texto: String, color: Color, icono: ImageVector, modifier: Modifier = Modifier, titulo: String? = null) {
    val p = LocalNexo.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.30f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            if (titulo != null) Text(titulo, color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(texto, color = p.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
        }
    }
}

/**
 * Pantalla completa encima de la actual (detalle, formulario largo). A diferencia de un Dialog,
 * vive en la misma ventana: los avisos Nexo se ven encima y el botón atrás la cierra.
 */
@Composable
fun PantallaSuperpuestaNexo(onAtras: () -> Unit, content: @Composable () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onAtras)
    Box(
        Modifier
            .fillMaxSize()
            // Absorbe los toques para que no lleguen a la pantalla de abajo
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) { content() }
}
