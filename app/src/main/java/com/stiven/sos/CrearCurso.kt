package com.stiven.sos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CrearCursoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                CrearCursoConTemasScreen(
                    onNavigateBack = { finish() },
                    onCursoCreado = {
                        com.stiven.sos.ui.theme.Avisos.mostrarAlVolver(this, "Curso creado exitosamente", com.stiven.sos.ui.theme.TipoAviso.EXITO)
                        finish()
                    }
                )
            }
        }
    }
}

// ============================================
// MODELOS DE DATOS
// ============================================

data class TemaTemp(
    val id: String = "tema_${System.currentTimeMillis()}",
    val titulo: String,
    val contenido: String,
    val archivoUrl: String = "",
    val archivoNombre: String = "",
    val tipo: String = "texto",
    val explicacion: String = "",
    val explicacionFuente: String = "",
    val explicacionEstado: String = "pendiente",
    val fechaCreacion: String = obtenerFechaActual(),
    val explicacionUltimaActualizacion: String = obtenerFechaActual(),
    val programacion: ProgramacionTema? = null,
    /** Texto extraído del PDF del tema (solo para que la IA escriba la explicación; no se guarda en el curso). */
    val textoPdf: String = "",
    val paginasPdf: Int = 0
)

/** El tema pide explicación con IA y todavía no la tiene (se generará al crear el curso). */
fun necesitaExplicacionIA(t: TemaTemp) = t.explicacionFuente == "ia" && t.explicacion.isBlank()

// ============================================
// FUNCIONES AUXILIARES
// ============================================

fun obtenerFechaActual(): String {
    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date())
}

fun obtenerTimestampActual(): Long {
    return System.currentTimeMillis()
}

fun calcularFechaFin(fechaInicio: Long, duracionDias: Int): Long {
    return fechaInicio + TimeUnit.DAYS.toMillis(duracionDias.toLong())
}

/**
 * Lee el PDF en el servidor y devuelve su texto real.
 * Si el servidor no puede leerlo (escaneado, protegido, dañado) devuelve el motivo como error.
 */
private suspend fun leerPdfEnServidor(context: Context, uri: Uri): Result<com.stiven.sos.models.ResultadoPdf> {
    val parte = com.stiven.sos.utils.ArchivosPdf.prepararParte(context, uri).getOrElse { return Result.failure(it) }
    return try {
        val resp = com.stiven.sos.api.ApiClient.apiService.extraerPdf(parte)
        val cuerpo = resp.body()
        if (resp.isSuccessful && cuerpo != null && cuerpo.error == null) Result.success(cuerpo)
        else Result.failure(Exception(cuerpo?.error ?: com.stiven.sos.api.mensajeDeError(resp)))
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor para leer el PDF."))
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            uri.lastPathSegment ?: "archivo.pdf"
        }
    } ?: uri.lastPathSegment ?: "archivo.pdf"
}

fun generarProgramacionAutomatica(
    temas: List<TemaTemp>,
    fechaInicio: Long,
    fechaFin: Long
): ProgramacionCurso? {
    if (temas.isEmpty() || fechaInicio == 0L || fechaFin == 0L) return null

    val duracionTotal = fechaFin - fechaInicio
    val diasPorTema = TimeUnit.MILLISECONDS.toDays(duracionTotal).toInt() / temas.size

    if (diasPorTema <= 0) return null

    val temasOrdenados = mutableListOf<String>()
    val distribucionTemporal = mutableMapOf<String, RangoTema>()

    var fechaActual = fechaInicio

    temas.forEachIndexed { index, tema ->
        temasOrdenados.add(tema.id)

        val fechaFinalTema = if (index == temas.size - 1) {
            fechaFin
        } else {
            fechaActual + TimeUnit.DAYS.toMillis(diasPorTema.toLong())
        }

        distribucionTemporal[tema.id] = RangoTema(
            temaId = tema.id,
            titulo = tema.titulo,
            fechaInicio = fechaActual,
            fechaFin = fechaFinalTema,
            quizzesRequeridos = diasPorTema,
            diasAsignados = diasPorTema
        )

        fechaActual = fechaFinalTema
    }

    return ProgramacionCurso(
        temasOrdenados = temasOrdenados,
        distribucionTemporal = distribucionTemporal
    )
}

// ============================================
// PANTALLA PRINCIPAL
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCursoConTemasScreen(
    onNavigateBack: () -> Unit,
    onCursoCreado: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val docenteId = remember { UserPreferences.getUserUid(context) }

    LaunchedEffect(Unit) {
        Log.d("CREAR_CURSO", " docenteId obtenido: '$docenteId'")
        if (docenteId.isNullOrBlank()) {
            Log.e("CREAR_CURSO", "ERROR: docenteId es NULL o vacío!")
        }
    }

    // Estados del curso
    var titulo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("activo") }

    // Estados de fechas
    var fechaInicio by remember { mutableStateOf(obtenerTimestampActual()) }
    var fechaFin by remember { mutableStateOf(0L) }

    // Estados de errores
    var tituloError by remember { mutableStateOf(false) }
    var codigoError by remember { mutableStateOf(false) }
    var descripcionError by remember { mutableStateOf(false) }
    var duracionError by remember { mutableStateOf(false) }

    // Estados de temas
    var temas by remember { mutableStateOf<List<TemaTemp>>(emptyList()) }
    var showAgregarTemaDialog by remember { mutableStateOf(false) }
    var temaParaEditar by remember { mutableStateOf<TemaTemp?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Estados para diálogos informativos
    var showAdvertenciaIADialog by remember { mutableStateOf(false) }
    var showProgresoGeneracionDialog by remember { mutableStateOf(false) }
    var progresoCurrent by remember { mutableStateOf(0) }
    var progresoTotal by remember { mutableStateOf(0) }
    var temaActual by remember { mutableStateOf("") }

    val fechaActual = remember { obtenerFechaActual() }

    // Calcular fecha fin cuando cambia la duración
    LaunchedEffect(duracionDias) {
        val dias = duracionDias.toIntOrNull()
        if (dias != null && dias > 0) {
            fechaFin = calcularFechaFin(fechaInicio, dias)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bookAnimation")
    val bookRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bookRotation"
    )

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Encabezado Nexo + vista previa del curso
            val nx = com.stiven.sos.ui.theme.LocalNexo.current
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = "Crear curso",
                subtitulo = "Configura el curso y sus temas",
                onVolver = onNavigateBack,
                acciones = {
                    if (temas.isNotEmpty()) {
                        com.stiven.sos.ui.theme.ChipNexo(
                            texto = "${temas.size} ${if (temas.size == 1) "tema" else "temas"}",
                            color = nx.iaTexto,
                            icono = Icons.Default.BookmarkAdded
                        )
                    }
                }
            )
            com.stiven.sos.ui.theme.TarjetaNexo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                acento = nx.ia
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.rotate(bookRotation)) {
                        com.stiven.sos.ui.theme.InsigniaNexo(Icons.Default.MenuBook, nx.ia, tamano = 52.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "VISTA PREVIA · ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                            color = nx.iaTexto,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            titulo.ifEmpty { "Tu nuevo curso" },
                            color = if (titulo.isEmpty()) nx.textoTenue else nx.texto,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (codigo.isNotEmpty()) {
                            Text(
                                "Código: $codigo",
                                color = nx.marcaTexto,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SectionHeader("INFORMACIÓN DEL CURSO", Icons.Default.Info, EduRachaColors.Primary)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(EduRachaColors.Surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CustomTextField(
                            value = titulo,
                            onValueChange = { titulo = it; tituloError = false },
                            label = "Título del curso (Requerido)",
                            icon = Icons.Default.Edit,
                            isError = tituloError,
                            errorMessage = "El título es requerido"
                        )

                        CustomTextField(
                            value = codigo,
                            onValueChange = { codigo = it.uppercase(); codigoError = false },
                            label = "Código del curso (Requerido)",
                            icon = Icons.Default.Badge,
                            isError = codigoError,
                            errorMessage = "El código es requerido"
                        )

                        CustomTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it; descripcionError = false },
                            label = "Descripción del curso (Requerido)",
                            icon = Icons.Default.Description,
                            isError = descripcionError,
                            errorMessage = "La descripción es requerida"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CustomTextField(
                                value = duracionDias,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() } && it.length <= 4) {
                                        duracionDias = it
                                        duracionError = false
                                    }
                                },
                                label = "Duración (días) (Requerido)",
                                icon = Icons.Default.CalendarToday,
                                isError = duracionError,
                                errorMessage = "Requerido",
                                modifier = Modifier.weight(1f)
                            )

                            EstadoDropdown(
                                estado = estado,
                                onEstadoChange = { estado = it },
                                isError = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // MOSTRAR FECHAS CALCULADAS
                        if (duracionDias.isNotEmpty() && duracionDias.toIntOrNull() != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            null,
                                            tint = EduRachaColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Programación del Curso",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EduRachaColors.TextPrimary
                                        )
                                    }

                                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    Text(
                                        "Inicio: ${sdf.format(Date(fechaInicio))}",
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                    if (fechaFin != 0L) {
                                        Text(
                                            "Fin: ${sdf.format(Date(fechaFin))}",
                                            fontSize = 13.sp,
                                            color = EduRachaColors.TextSecondary
                                        )
                                        Text(
                                            "Duración: ${duracionDias} días",
                                            fontSize = 13.sp,
                                            color = EduRachaColors.Success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = EduRachaColors.Success.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Docente asignado automáticamente",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Text(
                                        "ID: $docenteId",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                }
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("TEMAS DEL CURSO", Icons.Default.MenuBook, EduRachaColors.Success)

                    Button(
                        onClick = { showAgregarTemaDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nuevo Tema", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (temas.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Accent.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                null,
                                modifier = Modifier.size(80.dp),
                                tint = EduRachaColors.Accent
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No hay temas agregados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EduRachaColors.TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Agrega al menos un tema con explicación para crear el curso",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        temas.forEachIndexed { index, tema ->
                            TemaItemCardMejorado(
                                tema = tema,
                                index = index + 1,
                                totalTemas = temas.size,
                                onEditar = { temaParaEditar = tema },
                                onEliminar = {
                                    temas = temas.filter { it.id != tema.id }
                                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Tema eliminado")
                                }
                            )
                        }
                    }
                }

                val temasConIA = temas.filter { necesitaExplicacionIA(it) }

                if (temasConIA.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EduRachaColors.Accent.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            EduRachaColors.Accent.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    null,
                                    tint = EduRachaColors.Accent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Generación con Inteligencia Artificial",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Text(
                                        "${temasConIA.size} ${if (temasConIA.size == 1) "tema requiere" else "temas requieren"} explicación con IA",
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                }
                            }

                            Divider(color = EduRachaColors.Accent.copy(alpha = 0.2f))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = EduRachaColors.Surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoItem(
                                        icon = Icons.Default.Schedule,
                                        text = "Tiempo estimado: ${temasConIA.size * 2} minutos",
                                        color = EduRachaColors.Primary
                                    )
                                    InfoItem(
                                        icon = Icons.Default.CloudDone,
                                        text = "El curso se creará inmediatamente",
                                        color = EduRachaColors.Success
                                    )
                                    InfoItem(
                                        icon = Icons.Default.Autorenew,
                                        text = "Si falla, podrás reintentar desde la lista de cursos",
                                        color = EduRachaColors.Accent
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        tint = EduRachaColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Recomendación: Puedes cerrar esta pantalla después de crear el curso. Las explicaciones se generarán automáticamente en segundo plano.",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        if (docenteId.isNullOrBlank()) {
                            com.stiven.sos.ui.theme.Avisos.mostrar(context, " Error: No se pudo obtener tu ID. Cierra sesión y vuelve a iniciar.")
                            return@Button
                        }

                        tituloError = titulo.isBlank()
                        codigoError = codigo.isBlank()
                        descripcionError = descripcion.isBlank()
                        duracionError = duracionDias.isBlank() || duracionDias.toIntOrNull() == null

                        if (tituloError || codigoError || descripcionError || duracionError) {
                            com.stiven.sos.ui.theme.Avisos.mostrar(context, "Completa todos los campos obligatorios")
                            return@Button
                        }

                        if (temas.isEmpty()) {
                            com.stiven.sos.ui.theme.Avisos.mostrar(context, "Agrega al menos un tema")
                            return@Button
                        }

                        val temasManualesSinExplicacion = temas.filter {
                            it.explicacionFuente == "manual" && it.explicacion.isBlank()
                        }

                        if (temasManualesSinExplicacion.isNotEmpty()) {
                            com.stiven.sos.ui.theme.Avisos.mostrar(context, "Los temas manuales necesitan explicación")
                            return@Button
                        }

                        val programacion = generarProgramacionAutomatica(temas, fechaInicio, fechaFin)

                        val temasIA = temas.filter { necesitaExplicacionIA(it) }
                        if (temasIA.isNotEmpty()) {
                            showAdvertenciaIADialog = true
                        } else {
                            crearCursoDirectamente(
                                context = context,
                                titulo = titulo,
                                codigo = codigo,
                                descripcion = descripcion,
                                docenteId = docenteId,
                                duracionDias = duracionDias,
                                fechaInicio = fechaInicio,
                                fechaFin = fechaFin,
                                temas = temas,
                                programacion = programacion,
                                estado = estado,
                                fechaActual = fechaActual,
                                onLoadingChange = { isLoading = it },
                                onCursoCreado = onCursoCreado
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Creando curso...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Crear Curso Completo", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showAdvertenciaIADialog) {
        AdvertenciaIADialog(
            temasConIA = temas.filter { necesitaExplicacionIA(it) },
            onDismiss = { showAdvertenciaIADialog = false },
            onConfirm = {
                showAdvertenciaIADialog = false

                if (docenteId.isNullOrBlank()) {
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, " Error: ID de docente no encontrado. Cierra sesión y vuelve a iniciar.")
                    return@AdvertenciaIADialog
                }

                showProgresoGeneracionDialog = true
                progresoTotal = temas.filter { necesitaExplicacionIA(it) }.size
                progresoCurrent = 0
                temaActual = ""

                val programacion = generarProgramacionAutomatica(temas, fechaInicio, fechaFin)

                crearCursoConIA(
                    context = context,
                    titulo = titulo,
                    codigo = codigo,
                    descripcion = descripcion,
                    docenteId = docenteId,
                    duracionDias = duracionDias,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    temas = temas,
                    programacion = programacion,
                    estado = estado,
                    fechaActual = fechaActual,
                    onLoadingChange = { isLoading = it },
                    onProgresoChange = { current, total, nombreTema ->
                        progresoCurrent = current
                        progresoTotal = total
                        temaActual = nombreTema
                    },
                    onFinished = {
                        showProgresoGeneracionDialog = false
                        onCursoCreado()
                    }
                )
            }
        )
    }

    if (showProgresoGeneracionDialog) {
        ProgresoGeneracionDialog(
            progresoCurrent = progresoCurrent,
            progresoTotal = progresoTotal,
            temaActual = temaActual
        )
    }

    if (showAgregarTemaDialog || temaParaEditar != null) {
        AgregarTemaDialog(
            temaExistente = temaParaEditar,
            temasExistentes = temas,
            fechaActual = fechaActual,
            nombreCurso = titulo,
            onDismiss = {
                showAgregarTemaDialog = false
                temaParaEditar = null
            },
            onConfirm = { nuevoTema ->
                if (temaParaEditar != null) {
                    temas = temas.map { if (it.id == temaParaEditar!!.id) nuevoTema else it }
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Tema actualizado")
                } else {
                    temas = temas + nuevoTema
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Tema agregado")
                }
                showAgregarTemaDialog = false
                temaParaEditar = null
            }
        )
    }
}

// ============================================
// DIÁLOGOS
// ============================================

@Composable
fun AdvertenciaIADialog(
    temasConIA: List<TemaTemp>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Generación con IA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            "${temasConIA.size} ${if (temasConIA.size == 1) "tema" else "temas"} pendiente(s)",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }

                Divider(color = EduRachaColors.Background, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "¿Qué va a pasar?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    InfoRow(
                        icon = Icons.Default.CloudDone,
                        text = "El curso se creará inmediatamente",
                        color = EduRachaColors.Success
                    )
                    InfoRow(
                        icon = Icons.Default.Psychology,
                        text = "Se generarán ${temasConIA.size} explicación(es) con IA",
                        color = EduRachaColors.Accent
                    )
                    InfoRow(
                        icon = Icons.Default.Schedule,
                        text = "Tiempo estimado: ${temasConIA.size * 2} minutos",
                        color = EduRachaColors.Primary
                    )
                    InfoRow(
                        icon = Icons.Default.Autorenew,
                        text = "Si falla, se reintentará automáticamente 2 veces",
                        color = EduRachaColors.Primary
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Puedes cerrar esta pantalla durante el proceso",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgresoGeneracionDialog(
    progresoCurrent: Int,
    progresoTotal: Int,
    temaActual: String
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = EduRachaColors.Accent,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        Icons.Default.Psychology,
                        null,
                        tint = EduRachaColors.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Generando explicaciones con IA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Progreso: $progresoCurrent de $progresoTotal",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { if (progresoTotal > 0) progresoCurrent.toFloat() / progresoTotal.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EduRachaColors.Accent,
                        trackColor = EduRachaColors.Accent.copy(alpha = 0.2f)
                    )

                    if (temaActual.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = EduRachaColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Procesando: $temaActual",
                                    fontSize = 13.sp,
                                    color = EduRachaColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Este proceso puede tardar hasta 2 minutos por tema. Puedes cerrar esta pantalla de forma segura.",
                            fontSize = 11.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/** ID legible a partir del título ("Introducción a Kotlin" -> "introduccion_a_kotlin"), único entre [usados]. */
fun idDesdeTitulo(titulo: String, usados: Collection<String>): String {
    val base = java.text.Normalizer.normalize(titulo.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(40)
        .ifBlank { "tema" }
    var id = base
    var n = 2
    while (id in usados) id = "${base}_${n++}"
    return id
}

/**
 * Agregar o editar un tema (pantalla completa, diseño Nexo).
 * El PDF va aquí: el servidor lee su texto y la IA escribe la explicación del tema con ese contenido.
 * Si la IA falla, la explicación se puede escribir a mano en el mismo campo.
 */
@Composable
fun AgregarTemaDialog(
    temaExistente: TemaTemp? = null,
    temasExistentes: List<TemaTemp> = emptyList(),
    fechaActual: String,
    nombreCurso: String = "",
    onDismiss: () -> Unit,
    onConfirm: (TemaTemp) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = com.stiven.sos.ui.theme.LocalNexo.current

    var titulo by remember { mutableStateOf(temaExistente?.titulo ?: "") }
    var temaId by remember { mutableStateOf(temaExistente?.id ?: "") }
    var idManual by remember { mutableStateOf(temaExistente != null) }
    var contenido by remember { mutableStateOf(temaExistente?.contenido ?: "") }
    var archivoUrl by remember { mutableStateOf(temaExistente?.archivoUrl ?: "") }
    var archivoNombre by remember { mutableStateOf(temaExistente?.archivoNombre ?: "") }
    var textoPdf by remember { mutableStateOf(temaExistente?.textoPdf ?: "") }
    var paginasPdf by remember { mutableStateOf(temaExistente?.paginasPdf ?: 0) }
    var leyendoPdf by remember { mutableStateOf(false) }
    var errorPdf by remember { mutableStateOf<String?>(null) }

    // "ia" o "manual"
    var modoExplicacion by remember { mutableStateOf(temaExistente?.explicacionFuente?.ifBlank { null } ?: "ia") }
    var explicacion by remember { mutableStateOf(temaExistente?.explicacion ?: "") }
    var generando by remember { mutableStateOf(false) }
    var errorIA by remember { mutableStateOf<String?>(null) }
    var intentoGuardar by remember { mutableStateOf(false) }

    val usados = temasExistentes.filter { it.id != temaExistente?.id }.map { it.id }
    val idFinal = if (idManual) temaId.trim() else idDesdeTitulo(titulo, usados)
    val idRepetido = temaExistente == null && idFinal in usados
    val tituloError = intentoGuardar && titulo.isBlank()
    val contenidoError = intentoGuardar && contenido.isBlank() && textoPdf.isBlank()
    val explicacionError = intentoGuardar && modoExplicacion == "manual" && explicacion.trim().length < 50

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val pdfUri = uri ?: return@rememberLauncherForActivityResult
        leyendoPdf = true
        errorPdf = null
        scope.launch {
            val result = leerPdfEnServidor(context, pdfUri)
            leyendoPdf = false
            result.onSuccess { pdf ->
                archivoNombre = pdf.nombre.ifBlank { obtenerNombreArchivo(context, pdfUri) }
                archivoUrl = "pdf://$archivoNombre"
                textoPdf = pdf.texto
                paginasPdf = pdf.paginas
                modoExplicacion = "ia"
                val aviso = if (pdf.truncado) " Por su extensión se usará la primera parte." else ""
                com.stiven.sos.ui.theme.Avisos.mostrar(
                    context, "PDF leído: ${pdf.paginas} páginas. Ya puedes generar la explicación.$aviso",
                    com.stiven.sos.ui.theme.TipoAviso.EXITO
                )
            }.onFailure { e ->
                errorPdf = e.message ?: "No se pudo leer el PDF"
            }
        }
    }

    fun generarExplicacion() {
        if (titulo.isBlank()) {
            intentoGuardar = true
            errorIA = "Escribe primero el título del tema."
            return
        }
        generando = true
        errorIA = null
        scope.launch {
            try {
                val datos = buildMap {
                    put("titulo", titulo.trim())
                    if (contenido.isNotBlank()) put("contenido", contenido.trim())
                    if (textoPdf.isNotBlank()) put("textoDocumento", textoPdf)
                    if (nombreCurso.isNotBlank()) put("curso", nombreCurso.trim())
                }
                val resp = com.stiven.sos.api.ApiClient.apiService.generarExplicacionBorrador(datos)
                val texto = resp.body()?.get("explicacion")
                if (resp.isSuccessful && !texto.isNullOrBlank()) {
                    explicacion = texto
                } else {
                    errorIA = com.stiven.sos.api.mensajeDeError(resp)
                }
            } catch (e: Exception) {
                errorIA = "No se pudo conectar con el servidor. Puedes escribir la explicación tú mismo."
            } finally {
                generando = false
            }
        }
    }

    com.stiven.sos.ui.theme.PantallaSuperpuestaNexo(onAtras = { if (!generando && !leyendoPdf) onDismiss() }) {
        com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
            Column(Modifier.fillMaxSize()) {
                com.stiven.sos.ui.theme.EncabezadoNexo(
                    titulo = if (temaExistente == null) "Nuevo tema" else "Editar tema",
                    subtitulo = nombreCurso.ifBlank { "Crear curso" },
                    onVolver = { if (!generando && !leyendoPdf) onDismiss() },
                    tono = com.stiven.sos.ui.theme.TonoEncabezado.MARCA
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            // 1. Datos del tema
                            com.stiven.sos.ui.theme.PasoNexo(1, "Datos del tema", completo = titulo.isNotBlank() && !contenidoError)
                            com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    com.stiven.sos.ui.theme.CampoNexo(
                                        valor = titulo,
                                        onCambio = { titulo = it },
                                        etiqueta = "Título del tema",
                                        icono = Icons.Default.Title,
                                        unaLinea = true,
                                        error = tituloError,
                                        apoyo = if (tituloError) "El título es obligatorio" else null
                                    )
                                    com.stiven.sos.ui.theme.CampoNexo(
                                        valor = if (idManual) temaId else idFinal,
                                        onCambio = {
                                            idManual = true
                                            temaId = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                                        },
                                        etiqueta = "Identificador",
                                        icono = Icons.Default.Key,
                                        unaLinea = true,
                                        habilitado = temaExistente == null,
                                        error = idRepetido || (intentoGuardar && idFinal.isBlank()),
                                        apoyo = when {
                                            temaExistente != null -> "No se puede cambiar"
                                            idRepetido -> "Ya existe otro tema con este identificador"
                                            else -> "Se crea solo a partir del título; puedes cambiarlo"
                                        }
                                    )
                                    com.stiven.sos.ui.theme.CampoNexo(
                                        valor = contenido,
                                        onCambio = { contenido = it },
                                        etiqueta = "¿Qué se aprende en este tema?",
                                        lineasMinimas = 3,
                                        lineasMaximas = 8,
                                        error = contenidoError,
                                        apoyo = if (contenidoError) "Describe el tema o adjunta un PDF" else "Resumen corto para los estudiantes y para la IA"
                                    )
                                }
                            }

                            // 2. PDF
                            com.stiven.sos.ui.theme.PasoNexo(2, "Material en PDF (opcional)", completo = textoPdf.isNotBlank(),
                                detalle = "La IA escribirá la explicación con el contenido real del documento")
                            if (textoPdf.isNotBlank() || archivoUrl.isNotBlank()) {
                                com.stiven.sos.ui.theme.FilaNexo(
                                    icono = Icons.Default.PictureAsPdf,
                                    color = p.error,
                                    titulo = archivoNombre.ifBlank { "Documento PDF" },
                                    subtitulo = if (textoPdf.isNotBlank()) "$paginasPdf páginas · ${textoPdf.length} caracteres leídos" else "Adjunto (vuelve a elegirlo para usarlo con la IA)",
                                    extremo = {
                                        IconButton(onClick = { archivoUrl = ""; archivoNombre = ""; textoPdf = ""; paginasPdf = 0 }) {
                                            Icon(Icons.Default.Close, "Quitar PDF", tint = p.textoSuave)
                                        }
                                    }
                                )
                            } else {
                                com.stiven.sos.ui.theme.BotonNexo(
                                    texto = if (leyendoPdf) "Leyendo PDF…" else "Elegir PDF",
                                    onClick = { pdfLauncher.launch("application/pdf") },
                                    tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                                    icono = Icons.Default.UploadFile,
                                    cargando = leyendoPdf,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            errorPdf?.let {
                                com.stiven.sos.ui.theme.NotaNexo(it, p.error, Icons.Default.ErrorOutline, titulo = "No se pudo leer el PDF")
                            }

                            // 3. Explicación
                            com.stiven.sos.ui.theme.PasoNexo(3, "Explicación del tema", color = p.ia, completo = explicacion.isNotBlank(),
                                detalle = "Los estudiantes la leen antes del quiz. Queda pendiente hasta que la apruebes.")
                            com.stiven.sos.ui.theme.SegmentadoNexo(
                                opciones = listOf(
                                    Triple("ia", "Con IA", Icons.Default.AutoAwesome),
                                    Triple("manual", "Escribirla", Icons.Default.Edit)
                                ),
                                seleccion = modoExplicacion,
                                onCambio = { modoExplicacion = it },
                                colores = { if (it == "ia") p.ia else p.marca }
                            )

                            if (modoExplicacion == "ia") {
                                com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth(), acento = p.ia) {
                                    com.stiven.sos.ui.theme.GloboRobi(
                                        titulo = "ASISTENTE IA",
                                        mensaje = when {
                                            generando -> "Estoy escribiendo la explicación… puede tardar hasta un minuto."
                                            explicacion.isNotBlank() -> "Listo. Revísala abajo: puedes corregir lo que quieras antes de guardar."
                                            textoPdf.isNotBlank() -> "Escribiré una explicación detallada usando solo lo que dice tu PDF."
                                            else -> "Escribiré una explicación detallada con el título y la descripción del tema. Si no la generas ahora, la escribo al crear el curso."
                                        },
                                        expresion = when {
                                            generando -> com.stiven.sos.ui.theme.ExpresionRobi.PIENSA
                                            explicacion.isNotBlank() -> com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA
                                            else -> com.stiven.sos.ui.theme.ExpresionRobi.FELIZ
                                        },
                                        tamanoRobi = 60.dp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    com.stiven.sos.ui.theme.BotonNexo(
                                        texto = if (explicacion.isBlank()) "Generar explicación ahora" else "Volver a generar",
                                        onClick = { generarExplicacion() },
                                        tipo = com.stiven.sos.ui.theme.TipoBoton.IA,
                                        icono = Icons.Default.AutoAwesome,
                                        cargando = generando,
                                        habilitado = !leyendoPdf,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                errorIA?.let {
                                    com.stiven.sos.ui.theme.NotaNexo(it, p.error, Icons.Default.ErrorOutline, titulo = "La IA no pudo escribirla")
                                    com.stiven.sos.ui.theme.BotonNexo(
                                        texto = "Escribirla yo",
                                        onClick = { modoExplicacion = "manual"; errorIA = null },
                                        tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                                        icono = Icons.Default.Edit,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (modoExplicacion == "manual" || explicacion.isNotBlank()) {
                                com.stiven.sos.ui.theme.CampoNexo(
                                    valor = explicacion,
                                    onCambio = { explicacion = it },
                                    etiqueta = if (modoExplicacion == "ia") "Explicación generada (editable)" else "Escribe la explicación",
                                    lineasMinimas = 8,
                                    lineasMaximas = 18,
                                    error = explicacionError,
                                    colorFoco = if (modoExplicacion == "ia") p.ia else p.marca,
                                    apoyo = if (explicacionError) "Escribe al menos 50 caracteres" else "${explicacion.length} caracteres"
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // Barra de acciones fija abajo
                Surface(color = p.superficie, shadowElevation = 8.dp) {
                    com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.stiven.sos.ui.theme.BotonNexo(
                                texto = "Cancelar",
                                onClick = onDismiss,
                                tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                                habilitado = !generando && !leyendoPdf,
                                modifier = Modifier.weight(1f)
                            )
                            com.stiven.sos.ui.theme.BotonNexo(
                                texto = if (temaExistente == null) "Agregar" else "Guardar",
                                onClick = {
                                    intentoGuardar = true
                                    val faltaContenido = contenido.isBlank() && textoPdf.isBlank()
                                    val faltaExplicacion = modoExplicacion == "manual" && explicacion.trim().length < 50
                                    if (titulo.isBlank() || faltaContenido || faltaExplicacion || idRepetido || idFinal.isBlank()) {
                                        com.stiven.sos.ui.theme.Avisos.mostrar(
                                            context, "Revisa los campos marcados en rojo.", com.stiven.sos.ui.theme.TipoAviso.ADVERTENCIA
                                        )
                                        return@BotonNexo
                                    }
                                    onConfirm(
                                        TemaTemp(
                                            id = idFinal,
                                            titulo = titulo.trim(),
                                            contenido = contenido.trim().ifBlank { "Material del documento \"$archivoNombre\"" },
                                            archivoUrl = archivoUrl.trim(),
                                            archivoNombre = archivoNombre.trim(),
                                            tipo = if (archivoUrl.isNotEmpty()) "pdf" else "texto",
                                            explicacion = explicacion.trim(),
                                            explicacionFuente = modoExplicacion,
                                            explicacionEstado = "pendiente",
                                            textoPdf = textoPdf,
                                            paginasPdf = paginasPdf,
                                            programacion = temaExistente?.programacion
                                        )
                                    )
                                },
                                tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                                icono = if (temaExistente == null) Icons.Default.Add else Icons.Default.Save,
                                habilitado = !generando && !leyendoPdf,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// COMPONENTES DE UI REUTILIZABLES
// ============================================

@Composable
fun TemaItemCardMejorado(
    tema: TemaTemp,
    index: Int,
    totalTemas: Int,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EduRachaColors.Success,
                                    EduRachaColors.Success.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$index",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "/$totalTemas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = EduRachaColors.Success.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "ID: ${tema.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tema.titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Accent.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Editar",
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Error.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Eliminar",
                            tint = EduRachaColors.Error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = EduRachaColors.Background)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Description,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = EduRachaColors.Secondary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    tema.contenido.ifBlank { "(Sin contenido)" },
                    fontSize = 15.sp,
                    color = if (tema.contenido.isBlank()) EduRachaColors.TextSecondary.copy(alpha = 0.5f) else EduRachaColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            if (tema.explicacion.isNotEmpty() || tema.explicacionFuente == "ia") {
                Spacer(Modifier.height(16.dp))
                Divider(color = EduRachaColors.Background)
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EduRachaColors.Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (tema.explicacionFuente == "ia") Icons.Default.Psychology else Icons.Default.Edit,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = EduRachaColors.Primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Explicación incluida",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (tema.explicacionFuente == "ia") "Generada con IA" else "Escrita manualmente",
                                fontSize = 15.sp,
                                color = EduRachaColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (tema.explicacionFuente == "ia" && tema.explicacionEstado.isNotEmpty()) {
                                Text(
                                    "Estado: ${tema.explicacionEstado.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 12.sp,
                                    color = when (tema.explicacionEstado) {
                                        "aprobada" -> EduRachaColors.Success
                                        "pendiente" -> EduRachaColors.Accent
                                        "rechazada" -> EduRachaColors.Error
                                        else -> EduRachaColors.TextSecondary
                                    }
                                )
                            }
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            "Explicación incluida",
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (tema.archivoUrl.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(color = EduRachaColors.Background)
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Error.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EduRachaColors.Error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = EduRachaColors.Error
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "PDF Adjunto",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                tema.archivoNombre,
                                fontSize = 15.sp,
                                color = EduRachaColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            "PDF incluido",
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = EduRachaColors.TextPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean,
    errorMessage: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = if (isError) EduRachaColors.Error else EduRachaColors.Primary
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) EduRachaColors.Error else EduRachaColors.Primary,
            focusedLabelColor = if (isError) EduRachaColors.Error else EduRachaColors.Primary,
            cursorColor = EduRachaColors.Primary,
            errorBorderColor = EduRachaColors.Error,
            errorCursorColor = EduRachaColors.Error,
            errorLeadingIconColor = EduRachaColors.Error
        ),
        singleLine = true,
        supportingText = {
            if (isError) {
                Text(errorMessage, color = EduRachaColors.Error, fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoDropdown(
    estado: String,
    onEstadoChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val estados = listOf("activo", "inactivo", "borrador", "archivado")
    val selectedText = estado.replaceFirstChar { it.uppercase() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Estado (Requerido)") },
            leadingIcon = {
                Icon(
                    Icons.Default.ToggleOn,
                    null,
                    tint = if (isError) EduRachaColors.Error else EduRachaColors.Accent
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) EduRachaColors.Error else EduRachaColors.Accent,
                focusedLabelColor = if (isError) EduRachaColors.Error else EduRachaColors.Accent
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            estados.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opcion.replaceFirstChar { it.uppercase() },
                            fontWeight = if (opcion == estado) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onEstadoChange(opcion)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = {
                        if (opcion == estado) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = EduRachaColors.TextPrimary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = EduRachaColors.TextPrimary,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProcesoStep(
    numero: String,
    texto: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                numero,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            texto,
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================
// FUNCIONES DE LÓGICA DE NEGOCIO
// ============================================

private fun crearCursoDirectamente(
    context: Context,
    titulo: String,
    codigo: String,
    descripcion: String,
    docenteId: String,
    duracionDias: String,
    fechaInicio: Long,
    fechaFin: Long,
    temas: List<TemaTemp>,
    programacion: ProgramacionCurso?,
    estado: String,
    fechaActual: String,
    onLoadingChange: (Boolean) -> Unit,
    onCursoCreado: () -> Unit
) {
    onLoadingChange(true)

    val temasMap = temas.associate { temaTemp ->
        temaTemp.id to TemaRequest(
            id = temaTemp.id,
            titulo = temaTemp.titulo,
            contenido = temaTemp.contenido,
            archivoUrl = temaTemp.archivoUrl.ifEmpty { null },
            tipo = if (temaTemp.archivoUrl.isNotEmpty()) "pdf" else "texto",
            fechaCreacion = fechaActual,
            explicacion = temaTemp.explicacion.trim().ifBlank { null },
            explicacionFuente = when {
                temaTemp.explicacionFuente == "manual" -> "docente"
                temaTemp.explicacionFuente == "ia" -> "ia"
                else -> null
            },
            explicacionUltimaActualizacion = if (temaTemp.explicacion.isNotBlank()) fechaActual else null,
            explicacionEstado = "pendiente",
            programacion = temaTemp.programacion
        )
    }

    val cursoParaEnviar = CursoRequest(
        titulo = titulo.trim(),
        codigo = codigo.trim().uppercase(),
        descripcion = descripcion.trim(),
        docenteId = docenteId,
        duracionDias = duracionDias.toInt(),
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        temas = temasMap,
        programacion = programacion,
        estado = estado,
        fechaCreacion = fechaActual
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = com.stiven.sos.api.ApiClient.apiService.crearCurso(cursoParaEnviar)

            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                if (response.isSuccessful) {
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Curso creado exitosamente")
                    onCursoCreado()
                } else {
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error: ${e.message}")
            }
        }
    }
}

private fun crearCursoConIA(
    context: Context,
    titulo: String,
    codigo: String,
    descripcion: String,
    docenteId: String,
    duracionDias: String,
    fechaInicio: Long,
    fechaFin: Long,
    temas: List<TemaTemp>,
    programacion: ProgramacionCurso?,
    estado: String,
    fechaActual: String,
    onLoadingChange: (Boolean) -> Unit,
    onProgresoChange: (Int, Int, String) -> Unit,
    onFinished: () -> Unit
) {
    onLoadingChange(true)

    val temasMap = temas.associate { temaTemp ->
        temaTemp.id to TemaRequest(
            id = temaTemp.id,
            titulo = temaTemp.titulo,
            contenido = temaTemp.contenido,
            archivoUrl = temaTemp.archivoUrl.ifEmpty { null },
            tipo = if (temaTemp.archivoUrl.isNotEmpty()) "pdf" else "texto",
            fechaCreacion = fechaActual,
            explicacion = temaTemp.explicacion.trim().ifBlank { null },
            explicacionFuente = when {
                temaTemp.explicacionFuente == "manual" -> "docente"
                temaTemp.explicacionFuente == "ia" -> "ia"
                else -> null
            },
            explicacionUltimaActualizacion = if (temaTemp.explicacion.isNotBlank()) fechaActual else null,
            explicacionEstado = "pendiente",
            programacion = temaTemp.programacion
        )
    }

    val cursoParaEnviar = CursoRequest(
        titulo = titulo.trim(),
        codigo = codigo.trim().uppercase(),
        descripcion = descripcion.trim(),
        docenteId = docenteId,
        duracionDias = duracionDias.toInt(),
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        temas = temasMap,
        programacion = programacion,
        estado = estado,
        fechaCreacion = fechaActual
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = com.stiven.sos.api.ApiClient.apiService.crearCurso(cursoParaEnviar)

            if (response.isSuccessful && response.body() != null) {
                val cursoId = response.body()!!.id
                val temasConIA = temas.filter { necesitaExplicacionIA(it) }

                if (!cursoId.isNullOrBlank() && temasConIA.isNotEmpty()) {
                    generarExplicacionesConReintentos(
                        context = context,
                        cursoId = cursoId,
                        temasConIA = temasConIA,
                        intentosMaximos = 2,
                        onProgresoChange = onProgresoChange
                    )
                }

                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Curso creado con explicaciones generadas")
                    onFinished()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                    com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error: ${e.message}")
            }
        }
    }
}

private suspend fun generarExplicacionesConReintentos(
    context: Context,
    cursoId: String,
    temasConIA: List<TemaTemp>,
    intentosMaximos: Int = 2,
    onProgresoChange: (Int, Int, String) -> Unit
) {
    var exitosas = 0
    var fallidas = 0
    var timeouts = 0

    temasConIA.forEachIndexed { index, tema ->
        var intentoActual = 0
        var exito = false

        withContext(Dispatchers.Main) {
            onProgresoChange(index, temasConIA.size, tema.titulo)
        }

        while (intentoActual < intentosMaximos && !exito) {
            intentoActual++

            try {
                val temaId = tema.id
                Log.d("CREAR_CURSO", "Procesando: $temaId - ${tema.titulo} (intento $intentoActual)")

                withTimeout(120_000L) {
                    val request = GenerarExplicacionRequest(
                        cursoId = cursoId,
                        temaId = temaId,
                        tituloTema = tema.titulo,
                        // Si el tema trae PDF, la IA escribe la explicación con el texto del documento
                        contenidoTema = tema.textoPdf.ifBlank { tema.contenido }
                    )

                    val explicacionResponse = com.stiven.sos.api.ApiClient.apiService.generarExplicacionIA(
                        cursoId = cursoId,
                        temaId = temaId,
                        request = request
                    )

                    when {
                        explicacionResponse.isSuccessful -> {
                            exitosas++
                            exito = true
                            Log.d("CREAR_CURSO", " Explicación generada para $temaId")
                        }
                        else -> {
                            if (intentoActual >= intentosMaximos) {
                                timeouts++
                            } else {
                                delay(5000)
                            }
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                if (intentoActual >= intentosMaximos) {
                    timeouts++
                    Log.e("CREAR_CURSO", " Timeout definitivo para ${tema.id}")
                } else {
                    delay(5000)
                }
            } catch (e: Exception) {
                if (intentoActual >= intentosMaximos) {
                    fallidas++
                }
                Log.e("CREAR_CURSO", " Error para ${tema.id}: ${e.message}")
            }
        }

        withContext(Dispatchers.Main) {
            onProgresoChange(index + 1, temasConIA.size, tema.titulo)
        }
    }

    Log.d("CREAR_CURSO", " Exitosas: $exitosas | Timeouts: $timeouts | Fallidas: $fallidas")
}