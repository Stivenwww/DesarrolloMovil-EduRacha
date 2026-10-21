package com.stiven.sos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.DatosReporte
import com.stiven.sos.models.ReporteEstado
import com.stiven.sos.models.TipoReporte
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.ReporteViewModel

class PreviewReporteActivity : ComponentActivity() {

    private val reporteViewModel: ReporteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val fecha = intent.getStringExtra("FECHA")
        val temaId = intent.getStringExtra("TEMA_ID")
        val desde = intent.getStringExtra("DESDE")
        val hasta = intent.getStringExtra("HASTA")
        val tipoReporteStr = intent.getStringExtra("TIPO_REPORTE") ?: TipoReporte.DIARIO.name
        val tipoReporte = try {
            TipoReporte.valueOf(tipoReporteStr)
        } catch (e: Exception) {
            TipoReporte.DIARIO
        }

        setContent {
            EduRachaTheme {
                PreviewReporteScreenPremium(
                    cursoId = cursoId,
                    fecha = fecha,
                    temaId = temaId,
                    desde = desde,
                    hasta = hasta,
                    tipoReporte = tipoReporte,
                    reporteViewModel = reporteViewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/** Columna que se grafica en el Excel y en la vista previa (la misma en los dos). */
private fun columnaGrafico(tipo: TipoReporte) = when (tipo) {
    TipoReporte.DIARIO -> "Experiencia Ganada" to "XP ganada en el día"
    TipoReporte.TEMA -> "Mejor Puntaje" to "Mejor puntaje por estudiante"
    TipoReporte.GENERAL -> "Experiencia Total" to "XP total por estudiante"
    TipoReporte.RANGO -> "Experiencia" to "XP ganada en el rango"
}

private fun tituloReporte(tipo: TipoReporte) = when (tipo) {
    TipoReporte.DIARIO -> "Reporte diario"
    TipoReporte.TEMA -> "Reporte por tema"
    TipoReporte.GENERAL -> "Reporte general del curso"
    TipoReporte.RANGO -> "Reporte por rango de fechas"
}

/** Etiquetas cortas y claras para las columnas (las mismas del Excel). */
private fun etiquetaColumna(c: String) = when (c) {
    "Quizzes Realizados", "Total Quizzes", "Quizzes en Rango" -> "Quizzes"
    "Promedio Correctas", "Promedio General" -> "Aciertos prom."
    "Experiencia Ganada" -> "XP ganada"
    "Experiencia Total" -> "XP total"
    "Experiencia" -> "XP"
    "Temas Completados" -> "Temas"
    "Promedio Tiempo (seg)", "Tiempo Promedio (seg)" -> "Tiempo (s)"
    "Mejor Puntaje" -> "Mejor puntaje"
    "Nombre" -> "Estudiante"
    else -> c
}

private fun numero(v: String?): Double = v?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

@Composable
fun PreviewReporteScreenPremium(
    cursoId: String,
    fecha: String?,
    temaId: String?,
    desde: String?,
    hasta: String?,
    tipoReporte: TipoReporte,
    reporteViewModel: ReporteViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val p = com.stiven.sos.ui.theme.LocalNexo.current

    val cargando by reporteViewModel.cargandoPreview.collectAsState()
    val errorPreview by reporteViewModel.errorPreview.collectAsState()
    val datosReporte by reporteViewModel.datosPreview.collectAsState()
    val estadoDescarga by when (tipoReporte) {
        TipoReporte.DIARIO -> reporteViewModel.estadoReporteDiario.collectAsState()
        TipoReporte.TEMA -> reporteViewModel.estadoReporteTema.collectAsState()
        TipoReporte.GENERAL -> reporteViewModel.estadoReporteGeneral.collectAsState()
        TipoReporte.RANGO -> reporteViewModel.estadoReporteRango.collectAsState()
    }
    // Último archivo descargado en esta pantalla (para abrirlo o compartirlo)
    var archivo by remember { mutableStateOf<ReporteEstado.Exito?>(null) }

    fun cargar() {
        when (tipoReporte) {
            TipoReporte.DIARIO -> fecha?.let { reporteViewModel.obtenerDatosPreviewDiario(cursoId, it) }
            TipoReporte.TEMA -> temaId?.let { reporteViewModel.obtenerDatosPreviewTema(cursoId, it) }
            TipoReporte.GENERAL -> reporteViewModel.obtenerDatosPreviewGeneral(cursoId)
            TipoReporte.RANGO -> if (desde != null && hasta != null) reporteViewModel.obtenerDatosPreviewRango(cursoId, desde, hasta)
        }
    }
    fun descargar() {
        when (tipoReporte) {
            TipoReporte.DIARIO -> fecha?.let { reporteViewModel.descargarReporteDiario(cursoId, it) }
            TipoReporte.TEMA -> temaId?.let { reporteViewModel.descargarReporteTema(cursoId, it) }
            TipoReporte.GENERAL -> reporteViewModel.descargarReporteGeneral(cursoId)
            TipoReporte.RANGO -> if (desde != null && hasta != null) reporteViewModel.descargarReporteRango(cursoId, desde, hasta)
        }
    }
    fun resetear() = when (tipoReporte) {
        TipoReporte.DIARIO -> reporteViewModel.resetearEstadoDiario()
        TipoReporte.TEMA -> reporteViewModel.resetearEstadoTema()
        TipoReporte.GENERAL -> reporteViewModel.resetearEstadoGeneral()
        TipoReporte.RANGO -> reporteViewModel.resetearEstadoRango()
    }

    LaunchedEffect(cursoId) { cargar() }
    LaunchedEffect(estadoDescarga) {
        when (val e = estadoDescarga) {
            is ReporteEstado.Exito -> {
                archivo = e
                com.stiven.sos.ui.theme.Avisos.mostrar(context, "Excel guardado en Descargas: ${e.nombreArchivo}", com.stiven.sos.ui.theme.TipoAviso.EXITO, "Reporte descargado")
                resetear()
            }
            is ReporteEstado.Error -> {
                com.stiven.sos.ui.theme.Avisos.mostrar(context, e.mensaje, com.stiven.sos.ui.theme.TipoAviso.ERROR, "No se pudo descargar")
                resetear()
            }
            else -> {}
        }
    }
    val descargando = estadoDescarga is ReporteEstado.Descargando

    com.stiven.sos.ui.theme.FondoNexo {
        Column(Modifier.fillMaxSize()) {
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = "Vista previa",
                subtitulo = tituloReporte(tipoReporte),
                onVolver = onBackClick,
                tono = com.stiven.sos.ui.theme.TonoEncabezado.ACCION
            )
            Box(Modifier.weight(1f)) {
                when {
                    cargando -> com.stiven.sos.ui.theme.CargandoNexo("Preparando el reporte", "Estoy reuniendo los datos de cada estudiante…", p.accion)
                    errorPreview != null -> com.stiven.sos.ui.theme.EstadoVacioNexo(
                        titulo = "No se pudo cargar",
                        mensaje = errorPreview ?: "",
                        expresion = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE,
                        textoAccion = "Reintentar",
                        onAccion = { cargar() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    datosReporte != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                            HojaReporteNexo(datosReporte!!)
                        }
                    }
                }
            }
            // Barra de acciones
            if (datosReporte != null && !cargando) {
                Surface(color = p.superficie, shadowElevation = 8.dp) {
                    com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.stiven.sos.ui.theme.BotonNexo(
                                texto = if (archivo == null) "Descargar Excel" else "Descargar de nuevo",
                                onClick = { descargar() },
                                tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                                icono = Icons.Filled.Download,
                                cargando = descargando,
                                modifier = Modifier.fillMaxWidth()
                            )
                            archivo?.let { a ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    com.stiven.sos.ui.theme.BotonNexo(
                                        texto = "Abrir",
                                        onClick = { abrirArchivo(context, a, compartir = false) },
                                        tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                                        icono = Icons.Outlined.FileOpen,
                                        alto = 48.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    com.stiven.sos.ui.theme.BotonNexo(
                                        texto = "Compartir",
                                        onClick = { abrirArchivo(context, a, compartir = true) },
                                        tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                                        icono = Icons.Outlined.Share,
                                        alto = 48.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Abre el Excel descargado con la app que el docente tenga (Excel, Sheets, WPS…) o lo comparte. */
private fun abrirArchivo(context: android.content.Context, a: ReporteEstado.Exito, compartir: Boolean) {
    val uri = a.uri?.let { android.net.Uri.parse(it) }
    if (uri == null || uri.scheme != "content") {
        com.stiven.sos.ui.theme.Avisos.mostrar(context, "El archivo está en la carpeta Descargas: ${a.nombreArchivo}", com.stiven.sos.ui.theme.TipoAviso.INFO)
        return
    }
    val tipo = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    val intent = if (compartir) {
        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, a.nombreArchivo)
        }.let { android.content.Intent.createChooser(it, "Compartir reporte") }
    } else {
        android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, tipo)
    }
    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        com.stiven.sos.ui.theme.Avisos.mostrar(
            context, "No hay una app para abrir Excel. El archivo está en Descargas: ${a.nombreArchivo}", com.stiven.sos.ui.theme.TipoAviso.ADVERTENCIA
        )
    }
}

/**
 * La vista previa replica el Excel que se descarga: franja de marca, datos del curso,
 * indicadores, tabla con estados de color y el mismo gráfico de barras.
 */
@Composable
fun HojaReporteNexo(datos: DatosReporte) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val (colGrafico, tituloGrafico) = columnaGrafico(datos.tipoReporte)
    val detalle = when (datos.tipoReporte) {
        TipoReporte.DIARIO -> "Fecha: ${datos.fecha ?: "—"}"
        TipoReporte.TEMA -> "Tema: ${datos.temaNombre ?: datos.temaId ?: "—"}"
        TipoReporte.GENERAL -> "Todo el historial del curso"
        TipoReporte.RANGO -> "Del ${datos.fechaInicio ?: "—"} al ${datos.fechaFin ?: "—"}"
    }
    val sumaGrafico = datos.filas.sumOf { numero(it[colGrafico]) }.toInt()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Franja de marca + título (como las primeras filas del Excel)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = p.superficie, shadowElevation = if (p.esOscuro) 0.dp else 2.dp) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(p.ia).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EduRacha", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("  ·  Aprende en racha", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(Modifier.fillMaxWidth().background(p.marcaSombra).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(tituloReporte(datos.tipoReporte), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text(
                    "Curso: ${datos.cursoNombre}  ·  $detalle",
                    color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Indicadores
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Groups, "${datos.totalRegistros}", "Estudiantes", p.marca, Modifier.weight(1f))
            com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.DirectionsRun, "${datos.registrosConDatos}", "Con actividad", p.accion, Modifier.weight(1f))
            com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Bolt, "$sumaGrafico", etiquetaColumna(colGrafico), p.racha, Modifier.weight(1f))
        }

        // Tabla
        com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth(), padding = 0.dp) {
            Text(
                "Detalle por estudiante (${datos.filas.size})",
                color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)
            )
            if (datos.filas.isEmpty()) {
                Text(
                    "Todavía no hay actividad registrada para este reporte.",
                    color = p.textoSuave, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                TablaReporte(datos)
            }
        }

        // Gráfico (mismo que el Excel)
        if (datos.filas.isNotEmpty()) {
            com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth()) {
                Text(tituloGrafico, color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("También va como gráfico dentro del Excel.", color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                val max = datos.filas.maxOf { numero(it[colGrafico]) }.coerceAtLeast(1.0)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    datos.filas.take(15).forEach { f ->
                        val v = numero(f[colGrafico])
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f["Nombre"] ?: "", color = p.texto, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.width(110.dp))
                            com.stiven.sos.ui.theme.BarraNexo((v / max).toFloat(), color = p.marca, alto = 12.dp, modifier = Modifier.weight(1f))
                            Text(
                                if (v % 1.0 == 0.0) "${v.toInt()}" else "%.1f".format(v),
                                color = p.texto, fontSize = 13.sp, fontWeight = FontWeight.Black,
                                textAlign = TextAlign.End, modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                    if (datos.filas.size > 15) Text("Se muestran los 15 primeros; el Excel incluye a todos.", color = p.textoSuave, fontSize = 12.sp)
                }
            }
        }
        Text(
            "Generado por EduRacha · Corporación Universitaria Autónoma del Cauca",
            color = p.textoTenue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TablaReporte(datos: DatosReporte) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val columnas = datos.encabezados
    val ancho = { c: String -> if (c == "Nombre") 170.dp else if (c == "Estado") 120.dp else 104.dp }
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        // Encabezado de color (como en el Excel)
        Row(Modifier.background(p.marcaSombra)) {
            Text("#", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp,
                modifier = Modifier.width(40.dp).padding(vertical = 10.dp), textAlign = TextAlign.Center)
            columnas.forEach { c ->
                Text(etiquetaColumna(c), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1,
                    modifier = Modifier.width(ancho(c)).padding(horizontal = 8.dp, vertical = 10.dp),
                    textAlign = if (c == "Nombre") TextAlign.Start else TextAlign.Center)
            }
        }
        datos.filas.forEachIndexed { i, f ->
            Row(
                Modifier.background(if (i % 2 == 1) p.superficieAlt else p.superficie),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${i + 1}", color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 13.sp,
                    modifier = Modifier.width(40.dp).padding(vertical = 10.dp), textAlign = TextAlign.Center)
                columnas.forEach { c ->
                    val v = f[c] ?: ""
                    if (c == "Estado") {
                        Box(Modifier.width(ancho(c)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            val color = when {
                                v.contains("aprob", true) || v.contains("activo", true) || v.contains("complet", true) -> p.accion
                                v.contains("progreso", true) || v.contains("pendiente", true) -> p.racha
                                else -> p.textoSuave
                            }
                            com.stiven.sos.ui.theme.EtiquetaEstadoNexo(v, color)
                        }
                    } else {
                        Text(v, color = p.texto, fontSize = 13.sp, fontWeight = if (c == "Nombre") FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.width(ancho(c)).padding(horizontal = 8.dp, vertical = 10.dp),
                            textAlign = if (c == "Nombre") TextAlign.Start else TextAlign.Center)
                    }
                }
            }
        }
    }
}
