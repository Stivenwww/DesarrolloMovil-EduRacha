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
import com.stiven.sos.ui.theme.CustomShapes
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val cargando by reporteViewModel.cargandoPreview.collectAsState()
    val errorPreview by reporteViewModel.errorPreview.collectAsState()
    val datosReporte by reporteViewModel.datosPreview.collectAsState()

    val estadoDescarga by when (tipoReporte) {
        TipoReporte.DIARIO -> reporteViewModel.estadoReporteDiario.collectAsState()
        TipoReporte.TEMA -> reporteViewModel.estadoReporteTema.collectAsState()
        TipoReporte.GENERAL -> reporteViewModel.estadoReporteGeneral.collectAsState()
        TipoReporte.RANGO -> reporteViewModel.estadoReporteRango.collectAsState()
    }

    LaunchedEffect(cursoId) {
        when (tipoReporte) {
            TipoReporte.DIARIO -> fecha?.let { reporteViewModel.obtenerDatosPreviewDiario(cursoId, it) }
            TipoReporte.TEMA -> temaId?.let { reporteViewModel.obtenerDatosPreviewTema(cursoId, it) }
            TipoReporte.GENERAL -> reporteViewModel.obtenerDatosPreviewGeneral(cursoId)
            TipoReporte.RANGO -> {
                if (desde != null && hasta != null) {
                    reporteViewModel.obtenerDatosPreviewRango(cursoId, desde, hasta)
                }
            }
        }
    }

    LaunchedEffect(estadoDescarga) {
        when (val estado = estadoDescarga) {
            is ReporteEstado.Exito -> {
                Toast.makeText(context, "✅ Reporte descargado: ${estado.nombreArchivo}", Toast.LENGTH_LONG).show()
                when (tipoReporte) {
                    TipoReporte.DIARIO -> reporteViewModel.resetearEstadoDiario()
                    TipoReporte.TEMA -> reporteViewModel.resetearEstadoTema()
                    TipoReporte.GENERAL -> reporteViewModel.resetearEstadoGeneral()
                    TipoReporte.RANGO -> reporteViewModel.resetearEstadoRango()
                }
            }
            is ReporteEstado.Error -> {
                Toast.makeText(context, "❌ Error: ${estado.mensaje}", Toast.LENGTH_LONG).show()
                when (tipoReporte) {
                    TipoReporte.DIARIO -> reporteViewModel.resetearEstadoDiario()
                    TipoReporte.TEMA -> reporteViewModel.resetearEstadoTema()
                    TipoReporte.GENERAL -> reporteViewModel.resetearEstadoGeneral()
                    TipoReporte.RANGO -> reporteViewModel.resetearEstadoRango()
                }
            }
            else -> {}
        }
    }

    val descargando = estadoDescarga is ReporteEstado.Descargando

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vista Previa", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            when (tipoReporte) {
                                TipoReporte.DIARIO -> "Reporte Diario"
                                TipoReporte.TEMA -> "Reporte por Tema"
                                TipoReporte.GENERAL -> "Reporte General"
                                TipoReporte.RANGO -> "Reporte por Rango"
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!cargando && errorPreview == null && datosReporte != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (tipoReporte) {
                            TipoReporte.DIARIO -> fecha?.let { reporteViewModel.descargarReporteDiario(cursoId, it) }
                            TipoReporte.TEMA -> temaId?.let { reporteViewModel.descargarReporteTema(cursoId, it) }
                            TipoReporte.GENERAL -> reporteViewModel.descargarReporteGeneral(cursoId)
                            TipoReporte.RANGO -> {
                                if (desde != null && hasta != null) {
                                    reporteViewModel.descargarReporteRango(cursoId, desde, hasta)
                                }
                            }
                        }
                    },
                    icon = {
                        if (descargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, "Descargar", tint = Color.White)
                        }
                    },
                    text = {
                        Text(
                            if (descargando) "Descargando..." else "Descargar Excel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = EduRachaColors.Success,
                    modifier = Modifier.shadow(12.dp, CustomShapes.Card)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EduRachaColors.Background)
                .padding(paddingValues)
        ) {
            when {
                cargando -> EstadoCargando()
                errorPreview != null -> EstadoError(
                    mensaje = errorPreview!!,
                    onReintentar = {
                        when (tipoReporte) {
                            TipoReporte.DIARIO -> fecha?.let { reporteViewModel.obtenerDatosPreviewDiario(cursoId, it) }
                            TipoReporte.TEMA -> temaId?.let { reporteViewModel.obtenerDatosPreviewTema(cursoId, it) }
                            TipoReporte.GENERAL -> reporteViewModel.obtenerDatosPreviewGeneral(cursoId)
                            TipoReporte.RANGO -> {
                                if (desde != null && hasta != null) {
                                    reporteViewModel.obtenerDatosPreviewRango(cursoId, desde, hasta)
                                }
                            }
                        }
                    }
                )
                datosReporte != null -> PreviewContentPremium(datos = datosReporte!!)
            }
        }
    }
}

@Composable
fun EstadoCargando() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = EduRachaColors.Primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Cargando datos del reporte...",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Por favor espera",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun EstadoError(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = EduRachaColors.ErrorContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Error al cargar datos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Error
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = mensaje,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onReintentar,
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PreviewContentPremium(datos: DatosReporte) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderInfoCard(datos)

        if (datos.filas.isEmpty()) {
            EmptyStateCardPremium()
        } else {
            DataTableCardPremium(datos)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun HeaderInfoCard(datos: DatosReporte) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.PrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = datos.cursoNombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )

                    Spacer(Modifier.height(8.dp))
                    when (datos.tipoReporte) {
                        TipoReporte.DIARIO -> {
                            datos.fecha?.let {
                                InfoChip(icon = Icons.Outlined.CalendarToday, text = "Fecha: $it")
                            }
                        }
                        TipoReporte.TEMA -> {
                            datos.temaNombre?.let {
                                InfoChip(icon = Icons.Outlined.Topic, text = "Tema: $it")
                            }
                        }
                        TipoReporte.GENERAL -> {
                            InfoChip(icon = Icons.Outlined.Assessment, text = "Reporte General")
                        }
                        TipoReporte.RANGO -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                InfoChip(icon = Icons.Outlined.DateRange, text = "Desde: ${datos.fechaInicio}")
                                InfoChip(icon = Icons.Outlined.DateRange, text = "Hasta: ${datos.fechaFin}")
                            }
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${datos.filas.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Primary
                            )
                            Text(
                                text = "registros",
                                fontSize = 10.sp,
                                color = EduRachaColors.Primary
                            )
                        }
                    }
                }
            }

            if (datos.totalRegistros > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = EduRachaColors.Border)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatsChipPremium(
                        label = "Total estudiantes",
                        value = "${datos.totalRegistros}",
                        icon = Icons.Outlined.Groups
                    )
                    StatsChipPremium(
                        label = "Con actividad",
                        value = "${datos.registrosConDatos}",
                        icon = Icons.Outlined.CheckCircle,
                        color = EduRachaColors.Success
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = EduRachaColors.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, color = EduRachaColors.TextSecondary)
    }
}

@Composable
fun StatsChipPremium(label: String, value: String, icon: ImageVector, color: Color = EduRachaColors.Primary) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Text(text = label, fontSize = 11.sp, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyStateCardPremium() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.WarningContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = EduRachaColors.Warning, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = "Sin Datos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Warning)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No hay datos disponibles para este reporte en el período seleccionado.",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DataTableCardPremium(datos: DatosReporte) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text(
                text = "📋 Datos del Reporte",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f), CustomShapes.CardSmall)
                    .padding(12.dp)
            ) {
                datos.encabezados.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(150.dp).padding(horizontal = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            datos.filas.forEachIndexed { index, fila ->
                Row(
                    modifier = Modifier
                        .background(if (index % 2 == 0) Color.White else EduRachaColors.SurfaceVariant)
                        .border(1.dp, EduRachaColors.Border, CustomShapes.CardSmall)
                        .padding(12.dp)
                ) {
                    datos.encabezados.forEach { header ->
                        Text(
                            text = fila[header] ?: "-",
                            modifier = Modifier.width(150.dp).padding(horizontal = 8.dp),
                            fontSize = 13.sp,
                            color = EduRachaColors.TextPrimary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}