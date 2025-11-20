package com.stiven.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.stiven.sos.models.Curso
import com.stiven.sos.models.ReporteEstado
import com.stiven.sos.models.Tema
import com.stiven.sos.models.TipoReporte
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.CustomShapes
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.ReporteViewModel
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : ComponentActivity() {

    private val reporteViewModel: ReporteViewModel by viewModels()
    private val cursoViewModel: CursoViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, " Permiso necesario para guardar reportes", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verificarPermisos()
        cursoViewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                ReportesScreenComplete(
                    reporteViewModel = reporteViewModel,
                    cursoViewModel = cursoViewModel,
                    onBackClick = { finish() },
                    onNavigateToPreview = { cursoId, fecha, temaId, desde, hasta, tipo ->
                        val intent = Intent(this, PreviewReporteActivity::class.java).apply {
                            putExtra("CURSO_ID", cursoId)
                            putExtra("FECHA", fecha)
                            putExtra("TEMA_ID", temaId)
                            putExtra("DESDE", desde)
                            putExtra("HASTA", hasta)
                            putExtra("TIPO_REPORTE", tipo.name)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun verificarPermisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

// Clase para manejar diferentes tamaños de pantalla
data class ResponsiveConfig(
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val screenWidth: Int,
    val horizontalPadding: Int,
    val verticalSpacing: Int,
    val cardSpacing: Int,
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val titleFontSize: androidx.compose.ui.unit.TextUnit
)

@Composable
fun rememberResponsiveConfig(): ResponsiveConfig {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return ResponsiveConfig(
        isCompact = screenWidth < 600,
        isMedium = screenWidth in 600..839,
        isExpanded = screenWidth >= 840,
        screenWidth = screenWidth,
        horizontalPadding = when {
            screenWidth < 360 -> 12
            screenWidth < 600 -> 16
            screenWidth < 840 -> 24
            else -> 32
        },
        verticalSpacing = when {
            screenWidth < 360 -> 12
            screenWidth < 600 -> 16
            else -> 20
        },
        cardSpacing = when {
            screenWidth < 360 -> 8
            screenWidth < 600 -> 12
            else -> 16
        },
        fontSize = when {
            screenWidth < 360 -> 12.sp
            screenWidth < 600 -> 13.sp
            else -> 14.sp
        },
        titleFontSize = when {
            screenWidth < 360 -> 18.sp
            screenWidth < 600 -> 20.sp
            else -> 22.sp
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreenComplete(
    reporteViewModel: ReporteViewModel,
    cursoViewModel: CursoViewModel,
    onBackClick: () -> Unit,
    onNavigateToPreview: (String, String?, String?, String?, String?, TipoReporte) -> Unit
) {
    val context = LocalContext.current
    val responsiveConfig = rememberResponsiveConfig()
    val cursoUiState by cursoViewModel.uiState.collectAsState()

    val estadoDiario by reporteViewModel.estadoReporteDiario.collectAsState()
    val estadoTema by reporteViewModel.estadoReporteTema.collectAsState()
    val estadoGeneral by reporteViewModel.estadoReporteGeneral.collectAsState()
    val estadoRango by reporteViewModel.estadoReporteRango.collectAsState()

    var cursoSeleccionado by remember { mutableStateOf<String?>(null) }
    var temaSeleccionado by remember { mutableStateOf<String?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(obtenerFechaAyer()) }
    var fechaDesde by remember { mutableStateOf(obtenerFechaHaceDias(7)) }
    var fechaHasta by remember { mutableStateOf(obtenerFechaAyer()) }

    LaunchedEffect(estadoDiario, estadoTema, estadoGeneral, estadoRango) {
        listOf(estadoDiario, estadoTema, estadoGeneral, estadoRango).forEach { estado ->
            when (estado) {
                is ReporteEstado.Exito -> {
                    Toast.makeText(context, " Reporte guardado: ${estado.nombreArchivo}", Toast.LENGTH_LONG).show()
                    when (estado) {
                        estadoDiario -> reporteViewModel.resetearEstadoDiario()
                        estadoTema -> reporteViewModel.resetearEstadoTema()
                        estadoGeneral -> reporteViewModel.resetearEstadoGeneral()
                        estadoRango -> reporteViewModel.resetearEstadoRango()
                    }
                }
                is ReporteEstado.Error -> {
                    Toast.makeText(context, " Error: ${estado.mensaje}", Toast.LENGTH_LONG).show()
                    when (estado) {
                        estadoDiario -> reporteViewModel.resetearEstadoDiario()
                        estadoTema -> reporteViewModel.resetearEstadoTema()
                        estadoGeneral -> reporteViewModel.resetearEstadoGeneral()
                        estadoRango -> reporteViewModel.resetearEstadoRango()
                    }
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Description,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(if (responsiveConfig.isCompact) 24.dp else 28.dp)
                        )
                        Spacer(Modifier.width(if (responsiveConfig.isCompact) 8.dp else 12.dp))
                        Column {
                            Text(
                                "Reportes Excel",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (responsiveConfig.isCompact) 16.sp else 20.sp
                            )
                            if (!responsiveConfig.isCompact || responsiveConfig.screenWidth > 360) {
                                Text(
                                    "Genera y descarga reportes",
                                    fontSize = if (responsiveConfig.isCompact) 11.sp else 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EduRachaColors.Background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = responsiveConfig.horizontalPadding.dp)
                .padding(vertical = responsiveConfig.verticalSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(responsiveConfig.verticalSpacing.dp)
        ) {

            // Stats Cards adaptables
            if (responsiveConfig.isExpanded) {
                // En pantallas grandes: 4 columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)
                ) {
                    StatCardMini(
                        label = "Cursos",
                        value = "${cursoUiState.cursos.size}",
                        icon = Icons.Outlined.MenuBook,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.weight(1f),
                        config = responsiveConfig
                    )
                    StatCardMini(
                        label = "Tipos",
                        value = "4",
                        icon = Icons.Outlined.Assessment,
                        color = EduRachaColors.Secondary,
                        modifier = Modifier.weight(1f),
                        config = responsiveConfig
                    )
                }
            } else {
                // En pantallas pequeñas/medianas: 2 columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)
                ) {
                    StatCardMini(
                        label = "Cursos",
                        value = "${cursoUiState.cursos.size}",
                        icon = Icons.Outlined.MenuBook,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.weight(1f),
                        config = responsiveConfig
                    )
                    StatCardMini(
                        label = "Tipos",
                        value = "4",
                        icon = Icons.Outlined.Assessment,
                        color = EduRachaColors.Secondary,
                        modifier = Modifier.weight(1f),
                        config = responsiveConfig
                    )
                }
            }

            ReporteCardCompleta(
                titulo = " Reporte Diario",
                descripcion = "Actividad diaria de estudiantes",
                color = EduRachaColors.Primary,
                badge = "Popular",
                config = responsiveConfig
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it },
                        config = responsiveConfig
                    )

                    FechaSelectorPremium(
                        fecha = fechaSeleccionada,
                        onFechaChange = { fechaSeleccionada = it },
                        config = responsiveConfig
                    )

                    // Botones adaptativos según el tamaño de pantalla
                    if (responsiveConfig.isCompact && responsiveConfig.screenWidth < 360) {
                        // Pantallas muy pequeñas: botones en columna
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, fechaSeleccionada, null, null, null, TipoReporte.DIARIO)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteDiario(it, fechaSeleccionada)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Pantallas normales: botones en fila
                        Row(horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, fechaSeleccionada, null, null, null, TipoReporte.DIARIO)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = responsiveConfig.fontSize)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteDiario(it, fechaSeleccionada)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = responsiveConfig.fontSize)
                            }
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = " Reporte por Tema",
                descripcion = "Desempeño en tema específico",
                color = EduRachaColors.Secondary,
                badge = "Detallado",
                config = responsiveConfig
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = {
                            cursoSeleccionado = it
                            temaSeleccionado = null
                        },
                        config = responsiveConfig
                    )

                    val temasDelCurso = remember(cursoSeleccionado) {
                        cursoUiState.cursos
                            .firstOrNull { it.id == cursoSeleccionado }
                            ?.temas
                            ?.values
                            ?.toList()
                            ?: emptyList()
                    }

                    TemaDropdownPremium(
                        temas = temasDelCurso,
                        temaSeleccionado = temaSeleccionado,
                        onTemaSelected = { temaSeleccionado = it },
                        enabled = cursoSeleccionado != null,
                        config = responsiveConfig
                    )

                    if (responsiveConfig.isCompact && responsiveConfig.screenWidth < 360) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val curso = cursoSeleccionado
                                    val tema = temaSeleccionado
                                    if (curso != null && tema != null) {
                                        onNavigateToPreview(curso, null, tema, null, null, TipoReporte.TEMA)
                                    }
                                },
                                enabled = cursoSeleccionado != null && temaSeleccionado != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val curso = cursoSeleccionado
                                    val tema = temaSeleccionado
                                    if (curso != null && tema != null) {
                                        reporteViewModel.descargarReporteTema(curso, tema)
                                    }
                                },
                                enabled = cursoSeleccionado != null && temaSeleccionado != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Secondary)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val curso = cursoSeleccionado
                                    val tema = temaSeleccionado
                                    if (curso != null && tema != null) {
                                        onNavigateToPreview(curso, null, tema, null, null, TipoReporte.TEMA)
                                    }
                                },
                                enabled = cursoSeleccionado != null && temaSeleccionado != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = responsiveConfig.fontSize)
                            }

                            Button(
                                onClick = {
                                    val curso = cursoSeleccionado
                                    val tema = temaSeleccionado
                                    if (curso != null && tema != null) {
                                        reporteViewModel.descargarReporteTema(curso, tema)
                                    }
                                },
                                enabled = cursoSeleccionado != null && temaSeleccionado != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Secondary)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = responsiveConfig.fontSize)
                            }
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = " Reporte General",
                descripcion = "Estadísticas completas del curso",
                color = EduRachaColors.Accent,
                badge = "Completo",
                config = responsiveConfig
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it },
                        config = responsiveConfig
                    )

                    if (responsiveConfig.isCompact && responsiveConfig.screenWidth < 360) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, null, null, null, null, TipoReporte.GENERAL)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteGeneral(it)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Accent)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, null, null, null, null, TipoReporte.GENERAL)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = responsiveConfig.fontSize)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteGeneral(it)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Accent)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = responsiveConfig.fontSize)
                            }
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = " Reporte por Rango",
                descripcion = "Actividad entre dos fechas",
                color = EduRachaColors.Success,
                badge = "Histórico",
                config = responsiveConfig
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it },
                        config = responsiveConfig
                    )

                    // Selectores de fecha adaptativos
                    if (responsiveConfig.isCompact && responsiveConfig.screenWidth < 400) {
                        // Pantallas pequeñas: fechas en columna
                        Column(verticalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            FechaSelectorPremium(
                                fecha = fechaDesde,
                                onFechaChange = { fechaDesde = it },
                                label = "Desde",
                                config = responsiveConfig
                            )
                            FechaSelectorPremium(
                                fecha = fechaHasta,
                                onFechaChange = { fechaHasta = it },
                                label = "Hasta",
                                config = responsiveConfig
                            )
                        }
                    } else {
                        // Pantallas normales: fechas en fila
                        Row(horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            FechaSelectorPremium(
                                fecha = fechaDesde,
                                onFechaChange = { fechaDesde = it },
                                label = "Desde",
                                modifier = Modifier.weight(1f),
                                config = responsiveConfig
                            )
                            FechaSelectorPremium(
                                fecha = fechaHasta,
                                onFechaChange = { fechaHasta = it },
                                label = "Hasta",
                                modifier = Modifier.weight(1f),
                                config = responsiveConfig
                            )
                        }
                    }

                    if (responsiveConfig.isCompact && responsiveConfig.screenWidth < 360) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, null, null, fechaDesde, fechaHasta, TipoReporte.RANGO)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteRango(it, fechaDesde, fechaHasta)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(responsiveConfig.cardSpacing.dp)) {
                            OutlinedButton(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        onNavigateToPreview(it, null, null, fechaDesde, fechaHasta, TipoReporte.RANGO)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vista Previa", fontSize = responsiveConfig.fontSize)
                            }

                            Button(
                                onClick = {
                                    cursoSeleccionado?.let {
                                        reporteViewModel.descargarReporteRango(it, fechaDesde, fechaHasta)
                                    }
                                },
                                enabled = cursoSeleccionado != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar", fontSize = responsiveConfig.fontSize)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CustomShapes.Card,
                colors = CardDefaults.cardColors(containerColor = EduRachaColors.InfoContainer)
            ) {
                Row(
                    modifier = Modifier.padding(responsiveConfig.horizontalPadding.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = EduRachaColors.Info,
                        modifier = Modifier.size(if (responsiveConfig.isCompact) 32.dp else 40.dp)
                    )
                    Spacer(Modifier.width(if (responsiveConfig.isCompact) 12.dp else 16.dp))
                    Column {
                        Text(
                            " Información",
                            fontSize = if (responsiveConfig.isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Info
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Los reportes se guardan en Downloads. Usa Vista Previa para verificar.",
                            fontSize = responsiveConfig.fontSize,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(responsiveConfig.verticalSpacing.dp))
        }
    }
}

@Composable
fun ReporteCardCompleta(
    titulo: String,
    descripcion: String,
    color: Color,
    badge: String? = null,
    config: ResponsiveConfig,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (config.isCompact) 4.dp else 6.dp, CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color)
                    .padding(
                        horizontal = config.horizontalPadding.dp,
                        vertical = if (config.isCompact) 16.dp else 20.dp
                    )
            ) {
                if (config.isCompact && config.screenWidth < 360) {
                    // Pantallas muy pequeñas: layout vertical
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = titulo,
                                fontSize = config.titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = descripcion,
                                fontSize = config.fontSize,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Pantallas normales: layout horizontal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = titulo,
                                fontSize = config.titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = descripcion,
                                fontSize = config.fontSize,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = if (config.isCompact) 11.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        horizontal = if (config.isCompact) 10.dp else 12.dp,
                                        vertical = if (config.isCompact) 5.dp else 6.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(
                    horizontal = config.horizontalPadding.dp,
                    vertical = if (config.isCompact) 16.dp else 20.dp
                ),
                content = content
            )
        }
    }
}

@Composable
fun StatCardMini(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    config: ResponsiveConfig,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(if (config.isCompact) 3.dp else 4.dp, CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (config.isCompact) 12.dp else 16.dp,
                    vertical = if (config.isCompact) 12.dp else 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (config.isCompact) 42.dp else 50.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = color,
                    modifier = Modifier.size(if (config.isCompact) 24.dp else 28.dp)
                )
            }
            Spacer(Modifier.height(if (config.isCompact) 8.dp else 12.dp))
            Text(
                text = value,
                fontSize = if (config.isCompact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = if (config.isCompact) 12.sp else 13.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursoDropdownPremium(
    cursos: List<Curso>,
    cursoSeleccionado: String?,
    onCursoSelected: (String) -> Unit,
    config: ResponsiveConfig,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val cursoActual = cursos.firstOrNull { it.id == cursoSeleccionado }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = cursoActual?.titulo ?: "Seleccionar curso",
            onValueChange = {},
            readOnly = true,
            label = { Text("Curso", fontSize = config.fontSize) },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = config.fontSize),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cursos.forEach { curso ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                curso.titulo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = config.fontSize
                            )
                            Text(
                                "${curso.temas?.size ?: 0} temas",
                                fontSize = if (config.isCompact) 11.sp else 12.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    },
                    onClick = {
                        curso.id?.let { onCursoSelected(it) }
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemaDropdownPremium(
    temas: List<Tema>,
    temaSeleccionado: String?,
    onTemaSelected: (String) -> Unit,
    enabled: Boolean = true,
    config: ResponsiveConfig,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val temaActual = temas.firstOrNull { it.id == temaSeleccionado }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = temaActual?.titulo ?: "Seleccionar tema",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Tema", fontSize = config.fontSize) },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border,
                disabledBorderColor = EduRachaColors.BorderLight
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = config.fontSize),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            temas.forEach { tema ->
                DropdownMenuItem(
                    text = {
                        Text(
                            tema.titulo,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = config.fontSize
                        )
                    },
                    onClick = {
                        tema.id?.let { onTemaSelected(it) }
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FechaSelectorPremium(
    fecha: String,
    onFechaChange: (String) -> Unit,
    label: String = "Fecha",
    config: ResponsiveConfig,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = fecha,
        onValueChange = onFechaChange,
        label = { Text(label, fontSize = config.fontSize) },
        leadingIcon = {
            Icon(
                Icons.Default.CalendarToday,
                null,
                modifier = Modifier.size(if (config.isCompact) 18.dp else 20.dp)
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EduRachaColors.Primary,
            unfocusedBorderColor = EduRachaColors.Border
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = config.fontSize),
        modifier = modifier.fillMaxWidth()
    )
}

fun obtenerFechaAyer(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

fun obtenerFechaHaceDias(dias: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -dias)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}