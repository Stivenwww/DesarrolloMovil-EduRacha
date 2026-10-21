package com.stiven.sos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stiven.sos.api.ApiClient
import com.stiven.sos.api.mensajeDeError
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ============================================================================
// Estadísticas del curso (rol docente, dentro de Ranking)
// Datos calculados por el servidor con la información de cada estudiante en la base de datos.
// ============================================================================

data class EstadisticasUi(val cargando: Boolean = false, val datos: EstadisticasCurso? = null, val error: String? = null)

class EstadisticasCursoViewModel : ViewModel() {
    private val _ui = MutableStateFlow(EstadisticasUi())
    val ui: StateFlow<EstadisticasUi> = _ui.asStateFlow()
    private var cursoActual: String? = null

    fun cargar(cursoId: String, forzar: Boolean = false) {
        if (!forzar && cursoActual == cursoId && _ui.value.datos != null) return
        cursoActual = cursoId
        viewModelScope.launch {
            _ui.value = _ui.value.copy(cargando = true, error = null)
            try {
                val r = ApiClient.apiService.obtenerEstadisticasCurso(cursoId)
                _ui.value = if (r.isSuccessful && r.body() != null) EstadisticasUi(datos = r.body())
                else _ui.value.copy(cargando = false, error = mensajeDeError(r))
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(cargando = false, error = "No se pudo conectar con el servidor.")
            }
        }
    }
}

@Composable
fun EstadisticasCursoDocente(cursoId: String, vm: EstadisticasCursoViewModel = viewModel()) {
    val p = LocalNexo.current
    val ui by vm.ui.collectAsState()
    LaunchedEffect(cursoId) { vm.cargar(cursoId) }

    val d = ui.datos
    // 0 = ninguno; 1 actividad, 2 distribución, 3 por tema
    var ampliado by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    when {
        ui.cargando && d == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = p.marca)
        }
        d == null -> EstadoVacioNexo(
            titulo = "No se pudieron calcular",
            mensaje = ui.error ?: "Intenta de nuevo.",
            expresion = ExpresionRobi.TRISTE,
            textoAccion = "Reintentar",
            onAccion = { vm.cargar(cursoId, forzar = true) }
        )
        else -> Box(Modifier.fillMaxSize()) {
          Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResumenKpis(d.resumen)
                    if (d.resumen.quizzesFinalizados == 0) {
                        GloboRobi(
                            titulo = "AÚN SIN DATOS",
                            mensaje = "Cuando tus estudiantes presenten quizzes, aquí verás su actividad, sus puntajes y quién necesita ayuda.",
                            expresion = ExpresionRobi.PIENSA,
                            tamanoRobi = 60.dp
                        )
                    }
                    TarjetaGrafico(
                        "Actividad de los últimos 14 días",
                        "Quizzes finalizados por día. Toca un punto para ver el detalle.",
                        onAmpliar = { ampliado = 1 }
                    ) { GraficoActividad(d.actividadDiaria) }
                    TarjetaGrafico(
                        "¿Cómo les va en los quizzes?",
                        "Cantidad de quizzes por porcentaje de aciertos. Se aprueba con 80% o más.",
                        onAmpliar = { ampliado = 2 }
                    ) { GraficoDistribucion(d.distribucionPuntajes) }
                    if (d.porTema.isNotEmpty()) {
                        TarjetaGrafico(
                            "Promedio de aciertos por tema",
                            "Los temas con barra corta son los que más cuestan.",
                            onAmpliar = { ampliado = 3 }
                        ) { BarrasPorTema(d.porTema) }
                    }
                    EstudiantesEnRiesgo(d.estudiantes.filter { it.enRiesgo })
                    if (d.preguntasDificiles.isNotEmpty()) {
                        TarjetaGrafico(
                            "Preguntas más difíciles",
                            "Porcentaje de respuestas correctas (mínimo 3 respuestas)."
                        ) { PreguntasDificiles(d.preguntasDificiles) }
                    }
                    TablaEstudiantes(d.estudiantes)
                    Text(
                        "Actualizado: ${d.generado.take(16).replace("T", " ")} (hora del servidor)",
                        color = p.textoTenue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
          }
          when (ampliado) {
              1 -> GraficoAmpliado("Actividad diaria", "Quizzes finalizados por día en las últimas dos semanas.", { ampliado = 0 },
                  { GraficoActividad(d.actividadDiaria, alto = 300.dp) },
                  d.actividadDiaria.reversed().map { fechaCorta(it.fecha) to "${it.quizzes} quizzes · ${it.estudiantes} est." })
              2 -> GraficoAmpliado("Distribución de puntajes", "Cuántos quizzes cayeron en cada rango de aciertos.", { ampliado = 0 },
                  { GraficoDistribucion(d.distribucionPuntajes, alto = 320.dp) },
                  d.distribucionPuntajes.map { it.etiqueta to "${it.cantidad} quizzes" })
              3 -> GraficoAmpliado("Promedio por tema", "Promedio de aciertos y aprobación de cada tema.", { ampliado = 0 },
                  { BarrasPorTema(d.porTema) },
                  d.porTema.map { it.titulo to "${it.promedio}% · ${it.tasaAprobacion}% aprobó" })
          }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumenKpis(r: ResumenEstadisticas) {
    val p = LocalNexo.current
    val items = listOf(
        Triple(Icons.Outlined.Groups, "${r.totalEstudiantes}", "Estudiantes") to p.marca,
        Triple(Icons.Outlined.DirectionsRun, "${r.activosUltimos7Dias}", "Activos 7 días") to p.accion,
        Triple(Icons.Outlined.Quiz, "${r.quizzesFinalizados}", "Quizzes") to p.ia,
        Triple(Icons.Outlined.TrackChanges, "${r.promedioAciertos}%", "Aciertos prom.") to p.marca,
        Triple(Icons.Outlined.Verified, "${r.tasaAprobacion}%", "Aprobación") to p.accion,
        Triple(Icons.Outlined.LocalFireDepartment, "${r.rachaPromedio}", "Racha prom.") to p.racha,
        Triple(Icons.Outlined.Bolt, "${r.xpTotal}", "XP total") to p.racha,
        Triple(Icons.Outlined.Timer, "${r.tiempoPromedioSeg} s", "Tiempo prom.") to p.marca,
        Triple(Icons.Outlined.ReportProblem, "${r.estudiantesEnRiesgo}", "Necesitan apoyo") to p.error
    )
    BoxWithConstraints {
        val columnas = when {
            maxWidth >= 600.dp -> 5
            maxWidth >= 380.dp -> 3
            else -> 2
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = columnas
        ) {
            items.forEach { (t, color) ->
                StatNexo(t.first, t.second, t.third, color, Modifier.weight(1f))
            }
            // Relleno para que la última fila no estire sus tarjetas
            repeat((columnas - items.size % columnas) % columnas) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TarjetaGrafico(titulo: String, descripcion: String, onAmpliar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    val p = LocalNexo.current
    TarjetaNexo(Modifier.fillMaxWidth().then(if (onAmpliar != null) Modifier.clickable(onClick = onAmpliar) else Modifier)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(titulo, color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(descripcion, color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            if (onAmpliar != null) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(p.marca.copy(alpha = 0.12f))
                        .clickable(onClick = onAmpliar).semantics { contentDescription = "Ampliar gráfico" },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.OpenInFull, null, tint = p.marcaTexto, modifier = Modifier.size(20.dp)) }
            }
        }
        Spacer(Modifier.height(14.dp))
        contenido()
    }
}

/** Pantalla completa con el gráfico en grande y sus datos en una lista (para leerlos con calma). */
@Composable
private fun GraficoAmpliado(titulo: String, descripcion: String, onCerrar: () -> Unit, grafico: @Composable () -> Unit, datos: List<Pair<String, String>>) {
    val p = LocalNexo.current
    PantallaSuperpuestaNexo(onAtras = onCerrar) {
        FondoNexo(conCuadricula = false) {
            Column(Modifier.fillMaxSize()) {
                EncabezadoNexo(titulo = titulo, subtitulo = "Estadísticas", onVolver = onCerrar, tono = TonoEncabezado.RACHA)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(descripcion, color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            TarjetaNexo(Modifier.fillMaxWidth()) { grafico() }
                            TarjetaNexo(Modifier.fillMaxWidth(), padding = 0.dp) {
                                Text("Datos", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp))
                                datos.forEachIndexed { i, (k, v) ->
                                    Row(
                                        Modifier.fillMaxWidth().background(if (i % 2 == 0) p.superficieAlt else Color.Transparent)
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(k, color = p.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(v, color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fechaCorta(iso: String): String = runCatching {
    val (_, m, d) = iso.split("-")
    "${d.toInt()}/${m.toInt()}"
}.getOrDefault(iso)

/** Línea de un solo color (actividad diaria). Un toque selecciona el día y muestra su valor. */
@Composable
private fun GraficoActividad(puntos: List<PuntoActividad>, alto: androidx.compose.ui.unit.Dp = 160.dp) {
    val p = LocalNexo.current
    if (puntos.isEmpty()) { Text("Sin datos", color = p.textoSuave); return }
    var sel by remember(puntos) { mutableStateOf(puntos.lastIndex) }
    val max = (puntos.maxOf { it.quizzes }).coerceAtLeast(1)
    val elegido = puntos[sel.coerceIn(0, puntos.lastIndex)]
    val total = puntos.sumOf { it.quizzes }

    Row(verticalAlignment = Alignment.Bottom) {
        Text("${elegido.quizzes}", color = p.texto, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(
            "  quizzes el ${fechaCorta(elegido.fecha)} · ${elegido.estudiantes} estudiantes",
            color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
    val grid = p.borde
    val linea = p.marca
    val superficie = p.superficie
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(alto)
            .semantics { contentDescription = "Gráfico de actividad: $total quizzes en ${puntos.size} días, máximo $max en un día" }
            .pointerInput(puntos) {
                detectTapGestures { o ->
                    val paso = size.width / (puntos.size - 1).coerceAtLeast(1)
                    sel = (o.x / paso).toInt().let { if (o.x - it * paso > paso / 2) it + 1 else it }.coerceIn(0, puntos.lastIndex)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val arriba = 8.dp.toPx()
        val util = h - arriba
        val paso = w / (puntos.size - 1).coerceAtLeast(1)
        // Rejilla recesiva (3 líneas)
        for (i in 0..2) {
            val y = arriba + util * i / 2f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        val pts = puntos.mapIndexed { i, pt -> Offset(i * paso, arriba + util * (1f - pt.quizzes / max.toFloat())) }
        val area = Path().apply {
            moveTo(pts.first().x, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, h)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(linea.copy(alpha = 0.22f), linea.copy(alpha = 0f))))
        val trazo = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(trazo, linea, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        // Cursor del día elegido
        val s = pts[sel.coerceIn(0, pts.lastIndex)]
        drawLine(grid, Offset(s.x, arriba), Offset(s.x, h), strokeWidth = 1.5f)
        drawCircle(superficie, 7.dp.toPx(), s)
        drawCircle(linea, 5.dp.toPx(), s)
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth()) {
        Text(fechaCorta(puntos.first().fecha), color = p.textoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(fechaCorta(puntos[puntos.size / 2].fecha), color = p.textoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("Hoy", color = p.textoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Barras verticales (magnitud con un solo color); el rango aprobatorio lleva el color de acción y una etiqueta. */
@Composable
private fun GraficoDistribucion(rangos: List<RangoPuntaje>, alto: androidx.compose.ui.unit.Dp = 170.dp) {
    val p = LocalNexo.current
    if (rangos.isEmpty()) { Text("Sin datos", color = p.textoSuave); return }
    val max = rangos.maxOf { it.cantidad }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(alto),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        rangos.forEachIndexed { i, r ->
            val aprobatorio = i == rangos.lastIndex
            val color = if (aprobatorio) p.accion else p.marca
            Column(
                Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "${r.etiqueta}: ${r.cantidad} quizzes" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text("${r.cantidad}", color = p.texto, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                val fraccion = r.cantidad / max.toFloat()
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight((fraccion * 0.78f).coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(color)
                )
                Spacer(Modifier.height(6.dp))
                Text(r.etiqueta, color = p.textoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(p.accion))
        Text("  Aprobado (80-100%)", color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(p.marca))
        Text("  Por debajo de 80%", color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Barras horizontales: promedio de aciertos por tema, con la tasa de aprobación como texto. */
@Composable
private fun BarrasPorTema(temas: List<EstadisticaTema>) {
    val p = LocalNexo.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        temas.forEach { t ->
            Column(Modifier.semantics(mergeDescendants = true) {
                contentDescription = "${t.titulo}: promedio ${t.promedio}%, aprobación ${t.tasaAprobacion}%, ${t.intentos} intentos"
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.titulo, color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(if (t.intentos == 0) "Sin intentos" else "${t.promedio}%", color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(4.dp))
                BarraNexo(t.promedio / 100f, color = if (t.promedio >= 80) p.accion else p.marca, alto = 10.dp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${t.intentos} intentos · ${t.estudiantes} estudiantes · ${t.tasaAprobacion}% aprobó",
                    color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EstudiantesEnRiesgo(lista: List<EstadisticaEstudiante>) {
    val p = LocalNexo.current
    TarjetaNexo(Modifier.fillMaxWidth(), acento = if (lista.isEmpty()) p.accion else p.error) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(if (lista.isEmpty()) Icons.Outlined.Verified else Icons.Outlined.ReportProblem, if (lista.isEmpty()) p.accion else p.error)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Estudiantes que necesitan apoyo", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    if (lista.isEmpty()) "Nadie está en riesgo por ahora." else "${lista.size} con poca actividad o puntajes bajos",
                    color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (lista.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lista.take(8).forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarNexo(e.nombre, p.error.copy(alpha = 0.85f), tamano = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.nombre, color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(e.motivoRiesgo ?: "", color = p.errorTexto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (lista.size > 8) Text("y ${lista.size - 8} más en la tabla de abajo", color = p.textoSuave, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PreguntasDificiles(lista: List<PreguntaDificil>) {
    val p = LocalNexo.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lista.forEach { q ->
            val pct = if (q.respuestas == 0) 0 else q.aciertos * 100 / q.respuestas
            Column {
                Text(q.texto, color = p.texto, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarraNexo(pct / 100f, color = p.racha, alto = 8.dp, modifier = Modifier.weight(1f))
                    Text("  $pct% · ${q.respuestas} resp.", color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class OrdenTabla(val texto: String) { XP("XP"), PROMEDIO("Aciertos"), QUIZZES("Quizzes"), RACHA("Racha") }

/** Tabla (vista accesible de los datos) con orden configurable. */
@Composable
private fun TablaEstudiantes(lista: List<EstadisticaEstudiante>) {
    val p = LocalNexo.current
    var orden by remember { mutableStateOf(OrdenTabla.XP) }
    val ordenada = when (orden) {
        OrdenTabla.XP -> lista.sortedByDescending { it.experiencia }
        OrdenTabla.PROMEDIO -> lista.sortedByDescending { it.promedio }
        OrdenTabla.QUIZZES -> lista.sortedByDescending { it.quizzes }
        OrdenTabla.RACHA -> lista.sortedByDescending { it.racha }
    }
    TarjetaNexo(Modifier.fillMaxWidth()) {
        Text("Detalle por estudiante", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OrdenTabla.values().forEach { o -> FichaNexo(o.texto, orden == o, p.marca, { orden = o }) }
        }
        Spacer(Modifier.height(10.dp))
        if (lista.isEmpty()) {
            Text("El curso no tiene estudiantes inscritos.", color = p.textoSuave, fontSize = 14.sp)
        }
        ordenada.forEachIndexed { i, e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (i % 2 == 0) p.superficieAlt else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${i + 1}. ${e.nombre}: ${e.experiencia} XP, ${e.promedio}% aciertos, ${e.quizzes} quizzes, racha ${e.racha}, ${e.temasAprobados} temas aprobados"
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${i + 1}", color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.nombre, color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${e.quizzes} quizzes · ${e.temasAprobados} temas aprobados · racha ${e.racha}" +
                            (e.diasSinActividad?.let { if (it > 0) " · hace $it d" else " · hoy" } ?: ""),
                        color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${e.experiencia} XP", color = p.texto, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("${e.promedio}%", color = if (e.promedio >= 80) p.accionTexto else p.textoSuave, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
