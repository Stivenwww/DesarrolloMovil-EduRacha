package com.stiven.sos

import androidx.compose.foundation.clickable
import com.stiven.sos.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import com.stiven.sos.ui.theme.EduRachaColors
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.SolicitudViewModel
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

class CursosDisponiblesActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val solicitudViewModel: SolicitudViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        cursoViewModel.obtenerCursos()
        quizViewModel.cargarCursosInscritos() // Cargar cursos inscritos

        if (userUid.isNotEmpty()) {
            solicitudViewModel.cargarSolicitudesEstudiante(userUid)
        }

        setContent {
            EduRachaTheme {
                CursosDisponiblesScreen(
                    cursoViewModel = cursoViewModel,
                    solicitudViewModel = solicitudViewModel,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// Enum para los filtros
enum class FiltroTipo {
    DISPONIBLES,
    SOLICITADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosDisponiblesScreen(
    cursoViewModel: CursoViewModel,
    solicitudViewModel: SolicitudViewModel,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600

    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()
    val quizUiState by quizViewModel.uiState.collectAsState()

    var showSolicitudDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var mensajeEstudiante by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showMascotaMessage by remember { mutableStateOf(false) }
    var filtroActual by remember { mutableStateOf(FiltroTipo.DISPONIBLES) }

    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val userUid = prefs.getString("user_uid", "") ?: ""
    val userName = prefs.getString("user_name", "") ?: ""
    val userEmail = prefs.getString("user_email", "") ?: ""

    // Obtener códigos de cursos con solicitud pendiente
    val cursosConSolicitud = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .map { it.codigoCurso }
            .toSet()
    }

    // Obtener códigos de cursos donde ya está inscrito
    val cursosInscritos = remember(quizUiState.cursosInscritos) {
        quizUiState.cursosInscritos.map { it.codigo }.toSet()
    }

    LaunchedEffect(solicitudUiState.mensajeExito) {
        solicitudUiState.mensajeExito?.let {
            showSuccessDialog = true
            solicitudViewModel.clearMessages()
        }
    }

    LaunchedEffect(solicitudUiState.error) {
        solicitudUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            solicitudViewModel.clearMessages()
        }
    }

    // FILTRADO COMPLETO
    val cursosFiltrados = remember(cursoUiState.cursos, searchQuery) {
        var filtered = cursoUiState.cursos

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true)
            }
        }

        filtered
    }

    // CURSOS DISPONIBLES: Excluir los que tienen solicitud pendiente Y los que ya está inscrito
    val cursosDisponibles = cursosFiltrados.filterNot {
        cursosConSolicitud.contains(it.codigo) || cursosInscritos.contains(it.codigo)
    }

    // CURSOS SOLICITADOS: Solo los que tienen solicitud pendiente Y no está inscrito
    val cursosSolicitados = cursosFiltrados.filter {
        cursosConSolicitud.contains(it.codigo) && !cursosInscritos.contains(it.codigo)
    }

    // Determinar qué lista mostrar según el filtro
    val cursosAMostrar = when (filtroActual) {
        FiltroTipo.DISPONIBLES -> cursosDisponibles
        FiltroTipo.SOLICITADOS -> cursosSolicitados
    }

    val p = LocalNexo.current

    FondoNexo(conCuadricula = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EncabezadoNexo(
                    titulo = "Explorar cursos",
                    subtitulo = "${cursoUiState.cursos.size} cursos en la plataforma",
                    onVolver = onNavigateBack
                )
            }

            item {
                GloboRobi(
                    titulo = "ROBI",
                    mensaje = if (cursosConSolicitud.isNotEmpty())
                        "Tienes ${cursosConSolicitud.size} ${if (cursosConSolicitud.size == 1) "solicitud" else "solicitudes"} esperando respuesta de tu docente."
                    else "Elige un curso y envía tu solicitud. Tu docente la aprobará.",
                    expresion = ExpresionRobi.FELIZ,
                    tamanoRobi = 64.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                BuscadorNexo(
                    valor = searchQuery,
                    onCambio = { searchQuery = it },
                    placeholder = "Buscar por nombre, código o descripción",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                FiltrosTabs(
                    filtroActual = filtroActual,
                    onFiltroChange = { filtroActual = it },
                    cantidadDisponibles = cursosDisponibles.size,
                    cantidadSolicitados = cursosSolicitados.size
                )
            }

            if (cursosAMostrar.isNotEmpty()) {
                itemsIndexed(cursosAMostrar, key = { _, curso -> curso.codigo }) { index, curso ->
                    val pendiente = cursosConSolicitud.contains(curso.codigo)
                    EntradaEscalonada(index) {
                        TarjetaCursoNexo(
                            titulo = curso.titulo,
                            codigo = curso.codigo,
                            descripcion = curso.descripcion,
                            temas = curso.getTemasLista().sortedBy { it.orden }.map { it.titulo },
                            indice = index,
                            textoAccion = if (pendiente) "Solicitud en revisión" else "Solicitar unirme",
                            iconoAccion = if (pendiente) Icons.Default.HourglassTop else Icons.Default.Send,
                            habilitado = !pendiente,
                            tipoAccion = if (pendiente) TipoBoton.SECUNDARIO else TipoBoton.MARCA,
                            chip = if (pendiente) "PENDIENTE" else null,
                            onAccion = {
                                cursoSeleccionado = curso
                                mensajeEstudiante = ""
                                showSolicitudDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Error al cargar: mensaje claro + reintentar
            val errorCursos = cursoUiState.error
            if (errorCursos != null && cursoUiState.cursos.isEmpty() && !cursoUiState.isLoading) {
                item {
                    EstadoVacioNexo(
                        titulo = "No pudimos cargar los cursos",
                        mensaje = "Revisa tu conexión a internet e inténtalo de nuevo.",
                        expresion = ExpresionRobi.TRISTE,
                        textoAccion = "Reintentar",
                        onAccion = { cursoViewModel.obtenerCursos() }
                    )
                }
            }

            if (cursosAMostrar.isEmpty() && !cursoUiState.isLoading && cursoUiState.error == null) {
                item {
                    EstadoVacioNexo(
                        titulo = when {
                            searchQuery.isNotBlank() -> "Sin resultados"
                            filtroActual == FiltroTipo.SOLICITADOS -> "Sin solicitudes pendientes"
                            else -> "No hay cursos nuevos"
                        },
                        mensaje = when {
                            searchQuery.isNotBlank() -> "Ningún curso coincide con \"$searchQuery\"."
                            filtroActual == FiltroTipo.SOLICITADOS -> "Cuando solicites unirte a un curso aparecerá aquí."
                            else -> "Ya estás en todos los cursos disponibles o aún no hay cursos publicados."
                        }
                    )
                }
            }

            if (cursoUiState.isLoading || quizUiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RobiNeo(tamano = 100.dp, expresion = ExpresionRobi.PIENSA)
                            Spacer(Modifier.height(8.dp))
                            Text("Buscando cursos...", color = p.textoSuave, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showSuccessDialog) {
        DialogoExitoSolicitudFigma(
            onDismiss = { showSuccessDialog = false }
        )
    }

    if (showSolicitudDialog && cursoSeleccionado != null) {
        DialogoSolicitudFigma(
            curso = cursoSeleccionado!!,
            mensaje = mensajeEstudiante,
            onMensajeChange = { mensajeEstudiante = it },
            onDismiss = {
                showSolicitudDialog = false
                cursoSeleccionado = null
                mensajeEstudiante = ""
            },
            onConfirm = {
                solicitudViewModel.crearSolicitud(
                    codigoCurso = cursoSeleccionado!!.codigo,
                    estudianteId = userUid,
                    estudianteNombre = userName,
                    estudianteEmail = userEmail,
                    mensaje = mensajeEstudiante.ifBlank { null }
                )
                showSolicitudDialog = false
                cursoSeleccionado = null
                mensajeEstudiante = ""
            }
        )
    }
}

// ============================================================================
// FILTROS (Disponibles / Solicitados)
// ============================================================================

@Composable
fun FiltrosTabs(
    filtroActual: FiltroTipo,
    onFiltroChange: (FiltroTipo) -> Unit,
    cantidadDisponibles: Int,
    cantidadSolicitados: Int
) {
    val p = LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.superficie)
            .border(1.dp, p.borde, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FiltroTab(
            text = "Disponibles",
            count = cantidadDisponibles,
            isSelected = filtroActual == FiltroTipo.DISPONIBLES,
            onClick = { onFiltroChange(FiltroTipo.DISPONIBLES) },
            color = p.marca,
            modifier = Modifier.weight(1f)
        )
        FiltroTab(
            text = "Solicitados",
            count = cantidadSolicitados,
            isSelected = filtroActual == FiltroTipo.SOLICITADOS,
            onClick = { onFiltroChange(FiltroTipo.SOLICITADOS) },
            color = p.racha,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FiltroTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    val fondo by animateColorAsState(
        targetValue = if (isSelected) color else Color.Transparent,
        animationSpec = tween(250),
        label = "fondoTab"
    )
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) p.sobreAccion else p.textoSuave
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) p.sobreAccion else color,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.4f) else color.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

// ============================================================================
// DIÁLOGO DE SOLICITUD
// ============================================================================

@Composable
fun DialogoSolicitudFigma(
    curso: Curso,
    mensaje: String,
    onMensajeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val p = LocalNexo.current
    var selectedPredefined by remember { mutableStateOf<String?>(null) }
    val mensajesPredefinidos = listOf(
        "Estoy muy interesado en aprender sobre este tema y creo que este curso me ayudará en mi formación académica.",
        "Me gustaría unirme a este curso para complementar mis conocimientos en el área.",
        "Este curso es fundamental para mi carrera y me comprometo a participar activamente."
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = p.superficie,
            border = BorderStroke(1.dp, p.marca.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InsigniaNexo(Icons.Default.Send, p.marca, tamano = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Solicitar unirme", fontSize = 20.sp, fontWeight = FontWeight.Black, color = p.texto)
                        Text("${curso.titulo} · ${curso.codigo}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.marcaTexto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = p.textoSuave)
                    }
                }

                Text("Elige un mensaje para tu docente", fontSize = 14.sp, fontWeight = FontWeight.Black, color = p.textoSuave)
                mensajesPredefinidos.forEach { predefinido ->
                    MensajePredefinidoChip(
                        message = predefinido,
                        isSelected = selectedPredefined == predefinido,
                        onClick = {
                            selectedPredefined = predefinido
                            onMensajeChange(predefinido)
                        }
                    )
                }

                Text("O escribe el tuyo (opcional)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = p.textoSuave)
                OutlinedTextField(
                    value = mensaje,
                    onValueChange = {
                        onMensajeChange(it.take(500))
                        selectedPredefined = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe aquí...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.marca,
                        unfocusedBorderColor = p.borde,
                        focusedTextColor = p.texto,
                        unfocusedTextColor = p.texto,
                        focusedContainerColor = p.superficieAlt,
                        unfocusedContainerColor = p.superficieAlt,
                        cursorColor = p.marca,
                        focusedPlaceholderColor = p.textoTenue,
                        unfocusedPlaceholderColor = p.textoTenue
                    )
                )

                BotonNexo(
                    texto = "Enviar solicitud",
                    onClick = onConfirm,
                    tipo = TipoBoton.MARCA,
                    icono = Icons.Default.Send,
                    modifier = Modifier.fillMaxWidth()
                )
                BotonNexo(
                    texto = "Cancelar",
                    onClick = onDismiss,
                    tipo = TipoBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MensajePredefinidoChip(
    message: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val p = LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) p.marca.copy(alpha = 0.12f) else p.superficieAlt)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) p.marca else p.borde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isSelected) p.marcaTexto else p.textoTenue,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            fontSize = 13.sp,
            color = if (isSelected) p.texto else p.textoSuave,
            lineHeight = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

// ============================================================================
// DIÁLOGO DE ÉXITO
// ============================================================================

@Composable
fun DialogoExitoSolicitudFigma(
    onDismiss: () -> Unit
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.stiven.sos.services.GestorSonidos.reproducir(context, com.stiven.sos.services.GestorSonidos.Sonido.CORRECTO)
        delay(2800)
        onDismiss()
    }
    DialogoNexo(
        icono = Icons.Default.CheckCircle,
        color = p.accion,
        titulo = "¡Solicitud enviada!",
        mensaje = "Tu docente revisará tu solicitud.",
        nota = "Te notificaremos cuando haya una respuesta",
        onDismiss = onDismiss
    )
}
