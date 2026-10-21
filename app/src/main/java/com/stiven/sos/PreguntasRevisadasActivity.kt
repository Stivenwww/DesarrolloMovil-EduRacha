package com.stiven.sos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.models.Pregunta
import com.stiven.sos.models.DificultadPregunta
import com.stiven.sos.models.FuentePregunta
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import java.util.Locale

class PreguntasRevisadasActivity : ComponentActivity() {

    private val preguntasViewModel: PreguntaViewModel by viewModels()
    private val cursoViewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar todas las preguntas (sin filtro de estado inicial)
        preguntasViewModel.cargarPreguntas()
        cursoViewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                PreguntasRevisadasScreen(
                    preguntasViewModel = preguntasViewModel,
                    cursoViewModel = cursoViewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreguntasRevisadasScreen(
    preguntasViewModel: PreguntaViewModel,
    cursoViewModel: CursoViewModel,
    onBack: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()
    val cursoUiState by cursoViewModel.uiState.collectAsState()

    // Filtros (se conservan al girar la pantalla)
    var estadoSel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var cursoSel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var temaSel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var dificultadSel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var busqueda by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var detalleId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(preguntasUiState.successMessage, preguntasUiState.error) {
        preguntasUiState.successMessage?.let { com.stiven.sos.ui.theme.Avisos.mostrar(context, it, com.stiven.sos.ui.theme.TipoAviso.EXITO) }
        preguntasUiState.error?.let { com.stiven.sos.ui.theme.Avisos.mostrar(context, it, com.stiven.sos.ui.theme.TipoAviso.ERROR) }
        if (preguntasUiState.successMessage != null || preguntasUiState.error != null) preguntasViewModel.clearMessages()
    }

    val uid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val misCursos = remember(cursoUiState.cursos) { cursoUiState.cursos.filter { uid == null || it.docenteId == uid } }
    val idsMisCursos = remember(misCursos) { misCursos.mapNotNull { it.id }.toSet() }
    val revisadas = remember(preguntasUiState.preguntas, idsMisCursos) {
        preguntasUiState.preguntas.filter {
            (it.estado == EstadoPregunta.APROBADA || it.estado == EstadoPregunta.RECHAZADA) &&
                (idsMisCursos.isEmpty() || it.cursoId in idsMisCursos)
        }
    }
    val cursoActual = misCursos.firstOrNull { it.id == cursoSel }
    val temasCurso = remember(cursoActual) { cursoActual?.getTemasLista().orEmpty() }
    val filtradas = remember(revisadas, estadoSel, cursoSel, temaSel, dificultadSel, busqueda) {
        revisadas.filter { q ->
            (estadoSel == null || q.estado == estadoSel) &&
                (cursoSel == null || q.cursoId == cursoSel) &&
                (temaSel == null || q.temaId == temaSel) &&
                (dificultadSel == null || q.dificultad == dificultadSel) &&
                (busqueda.isBlank() || q.texto.contains(busqueda, true))
        }.sortedByDescending { it.fechaRevision ?: it.fechaCreacion }
    }
    val hayFiltros = estadoSel != null || cursoSel != null || dificultadSel != null || busqueda.isNotBlank()
    val aprobadas = revisadas.count { it.estado == EstadoPregunta.APROBADA }
    val rechazadas = revisadas.count { it.estado == EstadoPregunta.RECHAZADA }

    com.stiven.sos.ui.theme.FondoNexo {
        Column(Modifier.fillMaxSize()) {
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = "Preguntas revisadas",
                subtitulo = "Banco de preguntas",
                onVolver = onBack,
                tono = com.stiven.sos.ui.theme.TonoEncabezado.IA,
                acciones = {
                    com.stiven.sos.ui.theme.AccionEncabezadoNexo(Icons.Default.Refresh, "Actualizar",
                        { preguntasViewModel.cargarPreguntas() }, com.stiven.sos.ui.theme.TonoEncabezado.IA)
                }
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Filtros visibles y a un toque (antes estaban escondidos en una ventana emergente)
                item {
                    Column(Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.stiven.sos.ui.theme.StatNexo(Icons.Default.CheckCircle, "$aprobadas", "Aprobadas", p.accion, Modifier.weight(1f),
                                onClick = { estadoSel = if (estadoSel == EstadoPregunta.APROBADA) null else EstadoPregunta.APROBADA })
                            com.stiven.sos.ui.theme.StatNexo(Icons.Default.Cancel, "$rechazadas", "Rechazadas", p.error, Modifier.weight(1f),
                                onClick = { estadoSel = if (estadoSel == EstadoPregunta.RECHAZADA) null else EstadoPregunta.RECHAZADA })
                        }
                        com.stiven.sos.ui.theme.BuscadorNexo(busqueda, { busqueda = it }, "Buscar en el texto de la pregunta")
                        com.stiven.sos.ui.theme.SegmentadoNexo(
                            opciones = listOf(
                                Triple("todas", "Todas", Icons.Default.List),
                                Triple(EstadoPregunta.APROBADA, "Aprobadas", Icons.Default.Check),
                                Triple(EstadoPregunta.RECHAZADA, "Rechazadas", Icons.Default.Close)
                            ),
                            seleccion = estadoSel ?: "todas",
                            onCambio = { estadoSel = if (it == "todas") null else it },
                            colores = { when (it) { EstadoPregunta.APROBADA -> p.accion; EstadoPregunta.RECHAZADA -> p.error; else -> p.ia } }
                        )
                        Text("Curso", color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.stiven.sos.ui.theme.FichaNexo("Todos", cursoSel == null, p.marca, { cursoSel = null; temaSel = null })
                            misCursos.forEach { c ->
                                val n = revisadas.count { it.cursoId == c.id }
                                if (n > 0) com.stiven.sos.ui.theme.FichaNexo("${c.titulo} · $n", cursoSel == c.id, p.marca, { cursoSel = c.id; temaSel = null })
                            }
                        }
                        if (cursoSel != null && temasCurso.isNotEmpty()) {
                            Text("Tema", color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.stiven.sos.ui.theme.FichaNexo("Todos", temaSel == null, p.ia, { temaSel = null })
                                temasCurso.forEach { t ->
                                    val n = revisadas.count { it.temaId == t.id }
                                    if (n > 0) com.stiven.sos.ui.theme.FichaNexo("${t.titulo} · $n", temaSel == t.id, p.ia, { temaSel = t.id })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dificultad", color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            listOf(DificultadPregunta.FACIL to "Fácil", DificultadPregunta.MEDIO to "Media", DificultadPregunta.DIFICIL to "Difícil").forEach { (v, t) ->
                                com.stiven.sos.ui.theme.FichaNexo(t, dificultadSel == v, p.racha, { dificultadSel = if (dificultadSel == v) null else v })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${filtradas.size} resultados", color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (hayFiltros) {
                                com.stiven.sos.ui.theme.TextoAccionEncabezadoNexo("Limpiar filtros") {
                                    estadoSel = null; cursoSel = null; temaSel = null; dificultadSel = null; busqueda = ""
                                }
                            }
                        }
                    }
                }

                when {
                    preguntasUiState.isLoading && revisadas.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.ia) }
                    }
                    filtradas.isEmpty() -> item {
                        com.stiven.sos.ui.theme.EstadoVacioNexo(
                            titulo = if (hayFiltros) "Sin resultados" else "Aún no hay preguntas revisadas",
                            mensaje = if (hayFiltros) "Prueba con otros filtros." else "Cuando apruebes o rechaces preguntas aparecerán aquí.",
                            expresion = com.stiven.sos.ui.theme.ExpresionRobi.PIENSA,
                            textoAccion = if (hayFiltros) "Limpiar filtros" else null,
                            onAccion = { estadoSel = null; cursoSel = null; temaSel = null; dificultadSel = null; busqueda = "" }
                        )
                    }
                    else -> items(filtradas, key = { it.id ?: it.texto }) { pregunta ->
                        Box(Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO).fillMaxWidth()) {
                            PreguntaCard(
                                pregunta = pregunta,
                                cursoNombre = misCursos.find { it.id == pregunta.cursoId }?.titulo,
                                onClick = { detalleId = pregunta.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detalle en su propia pantalla completa (antes era una ventana emergente)
    val detalle = revisadas.firstOrNull { it.id == detalleId } ?: preguntasUiState.preguntas.firstOrNull { it.id == detalleId }
    if (detalleId != null && detalle != null) {
        val curso = misCursos.find { it.id == detalle.cursoId }
        com.stiven.sos.ui.theme.PantallaSuperpuestaNexo(onAtras = { detalleId = null }) {
            DetallePreguntaRevisadaPantalla(
                pregunta = detalle,
                cursoNombre = curso?.titulo,
                temaNombre = curso?.temas?.get(detalle.temaId)?.titulo ?: curso?.getTemasLista()?.firstOrNull { it.id == detalle.temaId }?.titulo,
                cargando = preguntasUiState.isLoading,
                onVolver = { detalleId = null },
                onCambiarEstado = { estado, nota -> preguntasViewModel.actualizarEstadoPregunta(detalle.id ?: "", estado, nota) },
                onEliminar = { preguntasViewModel.eliminarPregunta(detalle.id ?: "") { detalleId = null } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetallePreguntaRevisadaPantalla(
    pregunta: Pregunta,
    cursoNombre: String?,
    temaNombre: String?,
    cargando: Boolean,
    onVolver: () -> Unit,
    onCambiarEstado: (String, String?) -> Unit,
    onEliminar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val aprobada = pregunta.estado == EstadoPregunta.APROBADA
    val colorEstado = if (aprobada) p.accion else p.error
    var confirmarEliminar by remember { mutableStateOf(false) }
    val letras = listOf("A", "B", "C", "D", "E", "F")

    com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
        Column(Modifier.fillMaxSize()) {
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = "Detalle de la pregunta",
                subtitulo = listOfNotNull(cursoNombre, temaNombre).joinToString(" · ").ifBlank { "Pregunta revisada" },
                onVolver = onVolver,
                tono = com.stiven.sos.ui.theme.TonoEncabezado.IA
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.stiven.sos.ui.theme.EtiquetaEstadoNexo(if (aprobada) "Aprobada" else "Rechazada", colorEstado)
                            com.stiven.sos.ui.theme.EtiquetaEstadoNexo(
                                when (pregunta.dificultad) { DificultadPregunta.FACIL -> "Fácil"; DificultadPregunta.DIFICIL -> "Difícil"; else -> "Media" }, p.racha)
                            com.stiven.sos.ui.theme.EtiquetaEstadoNexo(if (pregunta.fuente == FuentePregunta.IA) "Creada con IA" else "Creada por docente",
                                if (pregunta.fuente == FuentePregunta.IA) p.ia else p.marca)
                            if (pregunta.modificada) com.stiven.sos.ui.theme.EtiquetaEstadoNexo("Editada", p.textoSuave)
                        }
                        com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth(), acento = colorEstado) {
                            Text(pregunta.texto, color = p.texto, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 25.sp)
                        }
                        com.stiven.sos.ui.theme.TituloSeccionNexo("Opciones", icono = Icons.Default.List)
                        pregunta.opciones.forEachIndexed { i, o ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (o.esCorrecta) p.accion.copy(alpha = 0.14f) else p.superficie)
                                    .border(if (o.esCorrecta) 2.dp else 1.dp, if (o.esCorrecta) p.accion else p.borde, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(if (o.esCorrecta) p.accion else p.superficieAlt), contentAlignment = Alignment.Center) {
                                    if (o.esCorrecta) Icon(Icons.Default.Check, null, tint = p.sobreAccion, modifier = Modifier.size(18.dp))
                                    else Text(letras.getOrElse(i) { "${i + 1}" }, color = p.textoSuave, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(o.texto, color = p.texto, fontWeight = if (o.esCorrecta) FontWeight.Black else FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                if (o.esCorrecta) Text("Correcta", color = p.accionTexto, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                        pregunta.explicacionCorrecta?.takeIf { it.isNotBlank() }?.let {
                            com.stiven.sos.ui.theme.NotaNexo(it, p.racha, Icons.Default.Lightbulb, titulo = "¿Por qué es la correcta?")
                        }
                        pregunta.notasRevision?.takeIf { it.isNotBlank() }?.let {
                            com.stiven.sos.ui.theme.NotaNexo(it, colorEstado, Icons.Default.RateReview, titulo = "Nota de la revisión")
                        }
                        com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth()) {
                            Text("Información", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            listOfNotNull(
                                "Curso" to (cursoNombre ?: "—"),
                                "Tema" to (temaNombre ?: "—"),
                                "Creada por" to pregunta.creadoPor.ifBlank { "—" },
                                "Creada el" to pregunta.fechaCreacion.take(10).ifBlank { "—" },
                                pregunta.revisadoPor?.let { "Revisada por" to it },
                                pregunta.fechaRevision?.let { "Revisada el" to it.take(10) }
                            ).forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(k, color = p.textoSuave, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                                    Text(v, color = p.texto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Surface(color = p.superficie, shadowElevation = 8.dp) {
                com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.stiven.sos.ui.theme.BotonNexo("Eliminar", { confirmarEliminar = true }, tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                            icono = Icons.Default.Delete, habilitado = !cargando, alto = 52.dp, modifier = Modifier.weight(1f))
                        if (aprobada) {
                            com.stiven.sos.ui.theme.BotonNexo("Rechazar", { onCambiarEstado(EstadoPregunta.RECHAZADA, "Retirada por el docente") },
                                tipo = com.stiven.sos.ui.theme.TipoBoton.PELIGRO, icono = Icons.Default.Close, cargando = cargando, alto = 52.dp, modifier = Modifier.weight(1.2f))
                        } else {
                            com.stiven.sos.ui.theme.BotonNexo("Aprobar", { onCambiarEstado(EstadoPregunta.APROBADA, "Aprobada por el docente") },
                                tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION, icono = Icons.Default.Check, cargando = cargando, alto = 52.dp, modifier = Modifier.weight(1.2f))
                        }
                    }
                }
            }
        }
    }
    if (confirmarEliminar) {
        com.stiven.sos.ui.theme.DialogoAccionNexo(
            icono = Icons.Default.DeleteOutline, color = p.error,
            titulo = "¿Eliminar la pregunta?",
            mensaje = "Se borrará del banco y no volverá a aparecer en los quizzes. Esta acción no se puede deshacer.",
            textoPrincipal = "Eliminar", tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.PELIGRO,
            onPrincipal = { confirmarEliminar = false; onEliminar() },
            textoSecundario = "Cancelar", onSecundario = { confirmarEliminar = false },
            onDismiss = { confirmarEliminar = false }
        )
    }
}

@Composable
fun PreguntaCard(
    pregunta: Pregunta,
    cursoNombre: String?,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val cardPadding = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
    val cardRadius = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
    val badgeRadius = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    val badgeTextSize = (screenWidth * 0.03f).coerceIn(11.dp, 13.dp).value.sp
    val cursoTextSize = (screenWidth * 0.028f).coerceIn(10.dp, 12.dp).value.sp
    val preguntaTextSize = (screenWidth * 0.038f).coerceIn(14.dp, 17.dp).value.sp
    val infoTextSize = (screenWidth * 0.028f).coerceIn(10.dp, 12.dp).value.sp
    val notasTextSize = (screenWidth * 0.03f).coerceIn(11.dp, 13.dp).value.sp

    val iconSize = (screenWidth * 0.04f).coerceIn(14.dp, 18.dp)
    val spacing = (screenWidth * 0.03f).coerceIn(10.dp, 16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding)
        ) {
            // Header con estado y curso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(badgeRadius),
                    color = if (pregunta.estado == EstadoPregunta.APROBADA)
                        EduRachaColors.Success.copy(alpha = 0.15f)
                    else
                        EduRachaColors.Error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = (screenWidth * 0.025f).coerceIn(8.dp, 12.dp),
                            vertical = (screenWidth * 0.015f).coerceIn(4.dp, 8.dp)
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.01f).coerceIn(3.dp, 6.dp))
                    ) {
                        Icon(
                            imageVector = if (pregunta.estado == EstadoPregunta.APROBADA)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = if (pregunta.estado == EstadoPregunta.APROBADA)
                                EduRachaColors.Success
                            else
                                EduRachaColors.Error
                        )
                        Text(
                            text = if (pregunta.estado == EstadoPregunta.APROBADA)
                                "Aprobada"
                            else
                                "Rechazada",
                            fontSize = badgeTextSize,
                            fontWeight = FontWeight.Bold,
                            color = if (pregunta.estado == EstadoPregunta.APROBADA)
                                EduRachaColors.Success
                            else
                                EduRachaColors.Error
                        )
                    }
                }

                if (cursoNombre != null) {
                    Surface(
                        shape = RoundedCornerShape(badgeRadius),
                        color = EduRachaColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = cursoNombre,
                            fontSize = cursoTextSize,
                            color = EduRachaColors.Primary,
                            modifier = Modifier.padding(
                                horizontal = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp),
                                vertical = (screenWidth * 0.01f).coerceIn(3.dp, 6.dp)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing))

            // Texto de la pregunta
            Text(
                text = pregunta.texto,
                fontSize = preguntaTextSize,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = preguntaTextSize * 1.3f
            )

            Spacer(modifier = Modifier.height(spacing))

            // Info adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.04f).coerceIn(12.dp, 20.dp))
            ) {
                PreguntaInfoChip(
                    icon = Icons.Outlined.ListAlt,
                    text = "${pregunta.opciones.size} opciones",
                    textSize = infoTextSize,
                    iconSize = (screenWidth * 0.035f).coerceIn(12.dp, 16.dp)
                )

                if (pregunta.dificultad != null) {
                    PreguntaInfoChip(
                        icon = Icons.Outlined.Speed,
                        text = pregunta.dificultad.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        textSize = infoTextSize,
                        iconSize = (screenWidth * 0.035f).coerceIn(12.dp, 16.dp)
                    )
                }

                if (pregunta.fuente.isNotEmpty()) {
                    PreguntaInfoChip(
                        icon = if (pregunta.fuente == "ia") Icons.Outlined.AutoAwesome else Icons.Outlined.Person,
                        text = if (pregunta.fuente == "ia") "IA" else pregunta.fuente.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        textSize = infoTextSize,
                        iconSize = (screenWidth * 0.035f).coerceIn(12.dp, 16.dp)
                    )
                }
            }

            // Notas de revisión (si existen)
            if (pregunta.notasRevision != null && pregunta.notasRevision.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing * 0.67f))
                Surface(
                    shape = RoundedCornerShape(badgeRadius),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                        horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                    ) {
                        Icon(
                            Icons.Outlined.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = EduRachaColors.Warning
                        )
                        Text(
                            text = pregunta.notasRevision,
                            fontSize = notasTextSize,
                            color = EduRachaColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = notasTextSize * 1.3f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreguntaInfoChip(
    icon: ImageVector,
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = EduRachaColors.TextSecondary
        )
        Text(
            text = text,
            fontSize = textSize,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun EmptyState(hasFilters: Boolean, onClearFilters: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val iconSize = (screenWidth * 0.2f).coerceIn(60.dp, 100.dp)
    val titleSize = (screenWidth * 0.045f).coerceIn(16.dp, 22.dp).value.sp
    val bodySize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp
    val buttonTextSize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp

    val padding = (screenWidth * 0.08f).coerceIn(24.dp, 40.dp)
    val spacing = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasFilters) Icons.Outlined.FilterAltOff else Icons.Outlined.QuestionAnswer,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = if (hasFilters)
                "No hay preguntas con estos filtros"
            else
                "No hay preguntas revisadas",
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(spacing * 0.5f))
        Text(
            text = if (hasFilters)
                "Intenta cambiar los filtros"
            else
                "Las preguntas aprobadas o rechazadas aparecerán aquí",
            fontSize = bodySize,
            color = EduRachaColors.TextSecondary.copy(alpha = 0.7f)
        )

        if (hasFilters) {
            Spacer(modifier = Modifier.height(spacing))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                )
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size((screenWidth * 0.05f).coerceIn(18.dp, 24.dp))
                )
                Spacer(modifier = Modifier.width((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                Text("Limpiar filtros", fontSize = buttonTextSize)
            }
        }
    }
}

@Composable
fun FilterDialog(
    cursos: List<com.stiven.sos.models.Curso>,
    selectedEstado: String?,
    selectedCurso: String?,
    onEstadoSelected: (String?) -> Unit,
    onCursoSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val titleSize = (screenWidth * 0.05f).coerceIn(18.dp, 24.dp).value.sp
    val sectionTitleSize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp
    val chipTextSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
    val buttonTextSize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp

    val dialogPadding = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
    val spacing = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    // Log para debug
    Log.d("FilterDialog", "Cursos recibidos: ${cursos.size}")
    cursos.forEach { curso ->
        Log.d("FilterDialog", "Curso: id=${curso.id}, nombre=${curso.nombre}, titulo=${curso.titulo}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Filtrar preguntas",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dialogPadding)
            ) {
                // Filtro por estado
                Text(
                    text = "Estado",
                    fontSize = sectionTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    FilterChip(
                        selected = selectedEstado == EstadoPregunta.APROBADA,
                        onClick = {
                            onEstadoSelected(
                                if (selectedEstado == EstadoPregunta.APROBADA) null
                                else EstadoPregunta.APROBADA
                            )
                        },
                        label = {
                            Text(
                                " Aprobadas",
                                fontSize = chipTextSize
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedEstado == EstadoPregunta.RECHAZADA,
                        onClick = {
                            onEstadoSelected(
                                if (selectedEstado == EstadoPregunta.RECHAZADA) null
                                else EstadoPregunta.RECHAZADA
                            )
                        },
                        label = {
                            Text(
                                " Rechazadas",
                                fontSize = chipTextSize
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Filtro por curso
                if (cursos.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Curso (${cursos.size} disponibles)",
                        fontSize = sectionTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        cursos.forEach { curso ->
                            val nombreCurso = curso.nombre.ifEmpty { curso.titulo }
                            Log.d("FilterDialog", "Renderizando curso: $nombreCurso")

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCursoSelected(
                                            if (selectedCurso == curso.id) null else curso.id
                                        )
                                    },
                                shape = RoundedCornerShape((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                color = if (selectedCurso == curso.id)
                                    EduRachaColors.Primary.copy(alpha = 0.2f)
                                else
                                    Color.White,
                                border = BorderStroke(
                                    width = if (selectedCurso == curso.id) 2.dp else 1.dp,
                                    color = if (selectedCurso == curso.id)
                                        EduRachaColors.Primary
                                    else
                                        EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = (screenWidth * 0.04f).coerceIn(12.dp, 18.dp),
                                            vertical = (screenWidth * 0.03f).coerceIn(10.dp, 14.dp)
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = nombreCurso,
                                        fontSize = chipTextSize,
                                        color = if (selectedCurso == curso.id)
                                            EduRachaColors.Primary
                                        else
                                            Color.Black,
                                        fontWeight = if (selectedCurso == curso.id)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selectedCurso == curso.id) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionado",
                                            tint = EduRachaColors.Primary,
                                            modifier = Modifier.size((screenWidth * 0.05f).coerceIn(18.dp, 22.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    HorizontalDivider()
                    Text(
                        text = "Curso",
                        fontSize = sectionTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "No hay cursos disponibles",
                        fontSize = chipTextSize,
                        color = EduRachaColors.TextSecondary,
                        modifier = Modifier.padding(vertical = spacing)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cerrar",
                    fontSize = buttonTextSize
                )
            }
        },
        shape = RoundedCornerShape((screenWidth * 0.05f).coerceIn(16.dp, 24.dp))
    )
}

@Composable
fun PreguntaDetailDialog(
    pregunta: Pregunta,
    cursoNombre: String?,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val titleSize = (screenWidth * 0.045f).coerceIn(16.dp, 20.dp).value.sp
    val badgeTextSize = (screenWidth * 0.03f).coerceIn(11.dp, 13.dp).value.sp
    val sectionTitleSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
    val bodyTextSize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp
    val smallTextSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
    val buttonTextSize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp

    val iconSize = (screenWidth * 0.04f).coerceIn(14.dp, 18.dp)
    val mediumIconSize = (screenWidth * 0.05f).coerceIn(18.dp, 24.dp)
    val smallIconSize = (screenWidth * 0.035f).coerceIn(12.dp, 16.dp)

    val radius = (screenWidth * 0.03f).coerceIn(10.dp, 14.dp)
    val spacing = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Detalle de la pregunta",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(radius * 0.67f),
                    color = if (pregunta.estado == EstadoPregunta.APROBADA)
                        EduRachaColors.Success.copy(alpha = 0.15f)
                    else
                        EduRachaColors.Error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (pregunta.estado == EstadoPregunta.APROBADA) " Aprobada" else " Rechazada",
                        fontSize = badgeTextSize,
                        fontWeight = FontWeight.Bold,
                        color = if (pregunta.estado == EstadoPregunta.APROBADA)
                            EduRachaColors.Success
                        else
                            EduRachaColors.Error,
                        modifier = Modifier.padding(
                            horizontal = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp),
                            vertical = (screenWidth * 0.01f).coerceIn(3.dp, 6.dp)
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Información del curso
                if (cursoNombre != null) {
                    DetailSection(
                        title = "Curso",
                        titleSize = sectionTitleSize,
                        content = {
                            Surface(
                                shape = RoundedCornerShape(radius),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                    horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize),
                                        tint = EduRachaColors.Primary
                                    )
                                    Text(
                                        text = cursoNombre,
                                        fontSize = bodyTextSize,
                                        color = EduRachaColors.Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    )
                }

                // Texto de la pregunta
                DetailSection(
                    title = "Pregunta",
                    titleSize = sectionTitleSize,
                    content = {
                        Text(
                            text = pregunta.texto,
                            fontSize = bodyTextSize,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = bodyTextSize * 1.4f
                        )
                    }
                )

                // Opciones
                DetailSection(
                    title = "Opciones de respuesta",
                    titleSize = sectionTitleSize,
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                        ) {
                            pregunta.opciones.forEach { opcion ->
                                Surface(
                                    shape = RoundedCornerShape(radius),
                                    color = if (opcion.esCorrecta)
                                        EduRachaColors.Success.copy(alpha = 0.1f)
                                    else
                                        EduRachaColors.TextSecondary.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = if (opcion.esCorrecta)
                                        BorderStroke(2.dp, EduRachaColors.Success)
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding((screenWidth * 0.03f).coerceIn(10.dp, 16.dp)),
                                        horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.03f).coerceIn(10.dp, 16.dp)),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((screenWidth * 0.06f).coerceIn(20.dp, 28.dp))
                                                .clip(CircleShape)
                                                .background(
                                                    if (opcion.esCorrecta)
                                                        EduRachaColors.Success
                                                    else
                                                        EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (opcion.esCorrecta) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Correcta",
                                                    modifier = Modifier.size((screenWidth * 0.04f).coerceIn(14.dp, 18.dp)),
                                                    tint = Color.White
                                                )
                                            } else {
                                                Text(
                                                    text = "${opcion.id + 1}",
                                                    fontSize = smallTextSize,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = opcion.texto,
                                            fontSize = bodyTextSize,
                                            color = if (opcion.esCorrecta)
                                                EduRachaColors.Success
                                            else
                                                EduRachaColors.TextPrimary,
                                            fontWeight = if (opcion.esCorrecta)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal,
                                            lineHeight = bodyTextSize * 1.3f,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

                // Explicación de la respuesta correcta (si existe)
                if (pregunta.explicacionCorrecta != null && pregunta.explicacionCorrecta.isNotEmpty()) {
                    DetailSection(
                        title = "Explicación de la respuesta",
                        titleSize = sectionTitleSize,
                        content = {
                            Surface(
                                shape = RoundedCornerShape(radius),
                                color = EduRachaColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding((screenWidth * 0.03f).coerceIn(10.dp, 16.dp)),
                                    horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(mediumIconSize),
                                        tint = EduRachaColors.Primary
                                    )
                                    Text(
                                        text = pregunta.explicacionCorrecta,
                                        fontSize = smallTextSize,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = smallTextSize * 1.5f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    )
                }

                // Metadatos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                ) {
                    if (pregunta.dificultad != null) {
                        MetadataChip(
                            icon = Icons.Outlined.Speed,
                            label = "Dificultad",
                            value = pregunta.dificultad.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            labelSize = (screenWidth * 0.025f).coerceIn(9.dp, 11.dp).value.sp,
                            valueSize = smallTextSize,
                            iconSize = iconSize,
                            padding = (screenWidth * 0.025f).coerceIn(8.dp, 12.dp),
                            radius = radius
                        )
                    }

                    MetadataChip(
                        icon = if (pregunta.fuente == "ia") Icons.Outlined.AutoAwesome else Icons.Outlined.Person,
                        label = "Fuente",
                        value = when(pregunta.fuente) {
                            "ia" -> "IA"
                            "docente" -> "Docente"
                            "importada" -> "Importada"
                            else -> pregunta.fuente
                        },
                        labelSize = (screenWidth * 0.025f).coerceIn(9.dp, 11.dp).value.sp,
                        valueSize = smallTextSize,
                        iconSize = iconSize,
                        padding = (screenWidth * 0.025f).coerceIn(8.dp, 12.dp),
                        radius = radius
                    )
                }

                // Notas de revisión
                if (pregunta.notasRevision != null && pregunta.notasRevision.isNotEmpty()) {
                    DetailSection(
                        title = "Notas de revisión",
                        titleSize = sectionTitleSize,
                        content = {
                            Surface(
                                shape = RoundedCornerShape(radius),
                                color = EduRachaColors.Warning.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding((screenWidth * 0.03f).coerceIn(10.dp, 16.dp)),
                                    horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                                ) {
                                    Icon(
                                        Icons.Outlined.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(mediumIconSize),
                                        tint = EduRachaColors.Warning
                                    )
                                    Text(
                                        text = pregunta.notasRevision,
                                        fontSize = smallTextSize,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = smallTextSize * 1.5f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    )
                }

                // Información de revisión
                if (pregunta.revisadoPor != null || pregunta.fechaRevision != null) {
                    HorizontalDivider()
                    Column(
                        verticalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                    ) {
                        if (pregunta.revisadoPor != null) {
                            DetailInfoRow(
                                icon = Icons.Outlined.Person,
                                label = "Revisado por",
                                value = pregunta.revisadoPor,
                                iconSize = smallIconSize,
                                textSize = smallTextSize
                            )
                        }
                        if (pregunta.fechaRevision != null) {
                            DetailInfoRow(
                                icon = Icons.Outlined.CalendarMonth,
                                label = "Fecha de revisión",
                                value = pregunta.fechaRevision,
                                iconSize = smallIconSize,
                                textSize = smallTextSize
                            )
                        }
                    }
                }

                // Información de creación
                HorizontalDivider()
                Column(
                    verticalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                ) {
                    if (pregunta.creadoPor.isNotEmpty()) {
                        DetailInfoRow(
                            icon = Icons.Outlined.PersonAdd,
                            label = "Creado por",
                            value = pregunta.creadoPor,
                            iconSize = smallIconSize,
                            textSize = smallTextSize
                        )
                    }
                    if (pregunta.fechaCreacion.isNotEmpty()) {
                        DetailInfoRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Fecha de creación",
                            value = pregunta.fechaCreacion,
                            iconSize = smallIconSize,
                            textSize = smallTextSize
                        )
                    }
                    if (pregunta.modificada) {
                        Surface(
                            shape = RoundedCornerShape(radius),
                            color = EduRachaColors.Warning.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = EduRachaColors.Warning
                                )
                                Text(
                                    text = "Esta pregunta ha sido modificada",
                                    fontSize = smallTextSize,
                                    color = EduRachaColors.Warning,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                )
            ) {
                Text(
                    "Cerrar",
                    fontSize = buttonTextSize
                )
            }
        },
        shape = RoundedCornerShape((screenWidth * 0.05f).coerceIn(16.dp, 24.dp))
    )
}

@Composable
fun DetailSection(
    title: String,
    titleSize: androidx.compose.ui.unit.TextUnit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val spacing = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            text = title,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Primary
        )
        content()
    }
}

@Composable
fun RowScope.MetadataChip(
    icon: ImageVector,
    label: String,
    value: String,
    labelSize: androidx.compose.ui.unit.TextUnit,
    valueSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
    padding: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp
) {
    Surface(
        shape = RoundedCornerShape(radius),
        color = EduRachaColors.Primary.copy(alpha = 0.08f),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = EduRachaColors.Primary
            )
            Text(
                text = label,
                fontSize = labelSize,
                color = EduRachaColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

@Composable
fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconSize: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val spacing = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = EduRachaColors.TextSecondary
        )
        Text(
            text = "$label:",
            fontSize = textSize,
            color = EduRachaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = textSize,
            color = EduRachaColors.TextPrimary
        )
    }
}