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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
            Toast.makeText(this, "⚠️ Permiso necesario para guardar reportes", Toast.LENGTH_LONG).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreenComplete(
    reporteViewModel: ReporteViewModel,
    cursoViewModel: CursoViewModel,
    onBackClick: () -> Unit,
    onNavigateToPreview: (String, String?, String?, String?, String?, TipoReporte) -> Unit
) {
    val context = LocalContext.current
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
                        Icon(Icons.Outlined.Description, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Reportes Excel", fontWeight = FontWeight.Bold)
                            Text("Genera y descarga reportes", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCardMini(
                    label = "Cursos",
                    value = "${cursoUiState.cursos.size}",
                    icon = Icons.Outlined.MenuBook,
                    color = EduRachaColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCardMini(
                    label = "Tipos",
                    value = "4",
                    icon = Icons.Outlined.Assessment,
                    color = EduRachaColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            ReporteCardCompleta(
                titulo = "📅 Reporte Diario",
                descripcion = "Actividad diaria de estudiantes",
                color = EduRachaColors.Primary,
                badge = "Popular"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it }
                    )

                    FechaSelectorPremium(
                        fecha = fechaSeleccionada,
                        onFechaChange = { fechaSeleccionada = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                cursoSeleccionado?.let {
                                    onNavigateToPreview(it, fechaSeleccionada, null, null, null, TipoReporte.DIARIO)
                                }
                            },
                            enabled = cursoSeleccionado != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Visibility, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Vista Previa")
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
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar")
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = "📘 Reporte por Tema",
                descripcion = "Desempeño en tema específico",
                color = EduRachaColors.Secondary,
                badge = "Detallado"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = {
                            cursoSeleccionado = it
                            temaSeleccionado = null
                        }
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
                        enabled = cursoSeleccionado != null
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Icon(Icons.Outlined.Visibility, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Vista Previa")
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
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar")
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = "📊 Reporte General",
                descripcion = "Estadísticas completas del curso",
                color = EduRachaColors.Accent,
                badge = "Completo"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                cursoSeleccionado?.let {
                                    onNavigateToPreview(it, null, null, null, null, TipoReporte.GENERAL)
                                }
                            },
                            enabled = cursoSeleccionado != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Visibility, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Vista Previa")
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
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar")
                        }
                    }
                }
            }

            ReporteCardCompleta(
                titulo = "📆 Reporte por Rango",
                descripcion = "Actividad entre dos fechas",
                color = EduRachaColors.Success,
                badge = "Histórico"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CursoDropdownPremium(
                        cursos = cursoUiState.cursos,
                        cursoSeleccionado = cursoSeleccionado,
                        onCursoSelected = { cursoSeleccionado = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FechaSelectorPremium(
                            fecha = fechaDesde,
                            onFechaChange = { fechaDesde = it },
                            label = "Desde",
                            modifier = Modifier.weight(1f)
                        )
                        FechaSelectorPremium(
                            fecha = fechaHasta,
                            onFechaChange = { fechaHasta = it },
                            label = "Hasta",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                cursoSeleccionado?.let {
                                    onNavigateToPreview(it, null, null, fechaDesde, fechaHasta, TipoReporte.RANGO)
                                }
                            },
                            enabled = cursoSeleccionado != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Visibility, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Vista Previa")
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
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar")
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
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Info, null, tint = EduRachaColors.Info, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("💡 Información", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Info)
                        Spacer(Modifier.height(4.dp))
                        Text("Los reportes se guardan en Downloads. Usa Vista Previa para verificar.", fontSize = 13.sp, color = EduRachaColors.TextSecondary, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ReporteCardCompleta(
    titulo: String,
    descripcion: String,
    color: Color,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(color).padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(text = descripcion, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    }

                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

@Composable
fun StatCardMini(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(4.dp, CustomShapes.Card),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(text = label, fontSize = 13.sp, color = EduRachaColors.TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursoDropdownPremium(cursos: List<Curso>, cursoSeleccionado: String?, onCursoSelected: (String) -> Unit, modifier: Modifier = Modifier) {
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
            label = { Text("Curso") },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cursos.forEach { curso ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(curso.titulo, fontWeight = FontWeight.SemiBold)
                            Text("${curso.temas?.size ?: 0} temas", fontSize = 12.sp, color = EduRachaColors.TextSecondary)
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
fun TemaDropdownPremium(temas: List<Tema>, temaSeleccionado: String?, onTemaSelected: (String) -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
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
            label = { Text("Tema") },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                unfocusedBorderColor = EduRachaColors.Border,
                disabledBorderColor = EduRachaColors.BorderLight
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            temas.forEach { tema ->
                DropdownMenuItem(
                    text = { Text(tema.titulo, fontWeight = FontWeight.SemiBold) },
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
fun FechaSelectorPremium(fecha: String, onFechaChange: (String) -> Unit, label: String = "Fecha", modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = fecha,
        onValueChange = onFechaChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EduRachaColors.Primary,
            unfocusedBorderColor = EduRachaColors.Border
        ),
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