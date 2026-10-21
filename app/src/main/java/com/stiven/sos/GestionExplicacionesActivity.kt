package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stiven.sos.models.Tema
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.AccionExplicacion
import com.stiven.sos.viewmodel.EstadoExplicacion
import com.stiven.sos.viewmodel.ExplicacionesViewModel
import com.stiven.sos.viewmodel.estadoExplicacion
import kotlinx.coroutines.launch

/**
 * Validar explicaciones (docente): CRUD completo de la explicación de cada tema.
 * - Ver todas (con filtro por estado) y leerlas en una pantalla de lectura cómoda, por secciones.
 * - Crear o editar a mano (también cuando la IA falló o el tema no tiene explicación).
 * - Generar con IA o desde un PDF, aprobar, rechazar y eliminar.
 */
class GestionExplicacionesActivity : ComponentActivity() {

    private val vm: ExplicacionesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        if (cursoId.isEmpty()) {
            finish()
            return
        }
        vm.iniciar(cursoId)
        setContent {
            EduRachaTheme {
                GestionExplicacionesScreen(cursoTitulo = cursoTitulo, vm = vm, onNavigateBack = { finish() })
            }
        }
    }
}

private enum class FiltroExplicaciones(val texto: String) { TODAS("Todas"), PENDIENTES("Por revisar"), APROBADAS("Aprobadas"), SIN("Sin explicación") }

@Composable
private fun colorEstado(e: EstadoExplicacion): Color {
    val p = LocalNexo.current
    return when (e) {
        EstadoExplicacion.APROBADA -> p.accion
        EstadoExplicacion.PENDIENTE -> p.racha
        EstadoExplicacion.RECHAZADA -> p.error
        EstadoExplicacion.SIN_EXPLICACION -> p.textoSuave
    }
}

private fun textoEstado(e: EstadoExplicacion) = when (e) {
    EstadoExplicacion.APROBADA -> "Aprobada"
    EstadoExplicacion.PENDIENTE -> "Por revisar"
    EstadoExplicacion.RECHAZADA -> "Rechazada"
    EstadoExplicacion.SIN_EXPLICACION -> "Sin explicación"
}

@Composable
fun GestionExplicacionesScreen(
    cursoTitulo: String,
    vm: ExplicacionesViewModel,
    onNavigateBack: () -> Unit
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()
    var filtro by rememberSaveable { mutableStateOf(FiltroExplicaciones.PENDIENTES) }
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(ui.mensaje) {
        ui.mensaje?.let {
            Avisos.mostrar(context, it, TipoAviso.EXITO)
            vm.limpiarMensaje()
        }
    }

    val conteo = remember(ui.temas) { ui.temas.groupingBy { it.estadoExplicacion() }.eachCount() }
    val pendientes = (conteo[EstadoExplicacion.PENDIENTE] ?: 0) + (conteo[EstadoExplicacion.RECHAZADA] ?: 0)
    val visibles = ui.temas.filter {
        when (filtro) {
            FiltroExplicaciones.TODAS -> true
            FiltroExplicaciones.PENDIENTES -> it.estadoExplicacion() in setOf(EstadoExplicacion.PENDIENTE, EstadoExplicacion.RECHAZADA)
            FiltroExplicaciones.APROBADAS -> it.estadoExplicacion() == EstadoExplicacion.APROBADA
            FiltroExplicaciones.SIN -> it.estadoExplicacion() == EstadoExplicacion.SIN_EXPLICACION
        }
    }

    FondoNexo {
        Column(Modifier.fillMaxSize()) {
            EncabezadoNexo(
                titulo = "Explicaciones",
                subtitulo = cursoTitulo,
                onVolver = onNavigateBack,
                tono = TonoEncabezado.IA,
                acciones = {
                    if (pendientes > 0) EtiquetaEncabezadoNexo("$pendientes por revisar")
                    AccionEncabezadoNexo(Icons.Filled.Refresh, "Actualizar", { vm.cargar() }, TonoEncabezado.IA)
                }
            )

            ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FiltroExplicaciones.values().forEach { f ->
                        val n = when (f) {
                            FiltroExplicaciones.TODAS -> ui.temas.size
                            FiltroExplicaciones.PENDIENTES -> pendientes
                            FiltroExplicaciones.APROBADAS -> conteo[EstadoExplicacion.APROBADA] ?: 0
                            FiltroExplicaciones.SIN -> conteo[EstadoExplicacion.SIN_EXPLICACION] ?: 0
                        }
                        FichaNexo("${f.texto} · $n", filtro == f, p.ia, { filtro = f })
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.cargando && ui.temas.isEmpty() -> CircularProgressIndicator(color = p.ia, modifier = Modifier.align(Alignment.Center))
                    ui.error != null && ui.temas.isEmpty() -> EstadoVacioNexo(
                        titulo = "No se pudieron cargar",
                        mensaje = ui.error ?: "",
                        expresion = ExpresionRobi.TRISTE,
                        textoAccion = "Reintentar",
                        onAccion = { vm.cargar() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    visibles.isEmpty() -> EstadoVacioNexo(
                        titulo = if (filtro == FiltroExplicaciones.PENDIENTES) "¡Todo al día!" else "Nada por aquí",
                        mensaje = when (filtro) {
                            FiltroExplicaciones.PENDIENTES -> "No hay explicaciones por revisar en este curso."
                            FiltroExplicaciones.SIN -> "Todos los temas tienen explicación."
                            else -> "No hay temas en este filtro."
                        },
                        expresion = if (filtro == FiltroExplicaciones.PENDIENTES) ExpresionRobi.CELEBRA else ExpresionRobi.NORMAL,
                        textoAccion = if (filtro != FiltroExplicaciones.TODAS) "Ver todas" else null,
                        onAccion = { filtro = FiltroExplicaciones.TODAS },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(visibles, key = { it.id }) { tema ->
                            TarjetaTemaExplicacion(
                                tema = tema,
                                accion = ui.enCurso[tema.id],
                                onAbrir = { abiertoId = tema.id },
                                onAprobar = { vm.aprobar(tema.id) },
                                modifier = Modifier.widthIn(max = ANCHO_CONTENIDO_NEXO).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    val abierto = ui.temas.firstOrNull { it.id == abiertoId }
    if (abierto != null) {
        DetalleExplicacionPantalla(
            tema = abierto,
            accion = ui.enCurso[abierto.id],
            error = ui.errorAccion?.takeIf { it.first == abierto.id }?.second,
            vm = vm,
            onCerrar = { abiertoId = null; vm.limpiarErrorAccion() }
        )
    }
}

@Composable
private fun TarjetaTemaExplicacion(
    tema: Tema,
    accion: AccionExplicacion?,
    onAbrir: () -> Unit,
    onAprobar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    val estado = tema.estadoExplicacion()
    val color = colorEstado(estado)
    TarjetaNexo(modifier.clickable(role = Role.Button, onClick = onAbrir), acento = color, padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsigniaNexo(
                when (estado) {
                    EstadoExplicacion.APROBADA -> Icons.Filled.Verified
                    EstadoExplicacion.SIN_EXPLICACION -> Icons.Outlined.NoteAdd
                    EstadoExplicacion.RECHAZADA -> Icons.Outlined.ReportGmailerrorred
                    else -> Icons.Outlined.RateReview
                },
                color
            )
            Column(Modifier.weight(1f)) {
                Text(tema.titulo ?: "Tema", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EtiquetaEstadoNexo(textoEstado(estado), color)
                    if (estado != EstadoExplicacion.SIN_EXPLICACION) {
                        EtiquetaEstadoNexo(if (tema.explicacionFuente == "ia") "IA" else "Docente", if (tema.explicacionFuente == "ia") p.ia else p.marca)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tema.explicacion?.takeIf { it.isNotBlank() } ?: "Este tema aún no tiene explicación. Ábrelo para escribirla o generarla con IA.",
            color = if (estado == EstadoExplicacion.SIN_EXPLICACION) p.textoTenue else p.textoSuave,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonNexo(
                texto = if (estado == EstadoExplicacion.SIN_EXPLICACION) "Crear" else "Abrir",
                onClick = onAbrir,
                tipo = TipoBoton.SECUNDARIO,
                icono = if (estado == EstadoExplicacion.SIN_EXPLICACION) Icons.Filled.Add else Icons.Filled.MenuBook,
                alto = 48.dp,
                modifier = Modifier.weight(1f)
            )
            if (estado == EstadoExplicacion.PENDIENTE || estado == EstadoExplicacion.RECHAZADA) {
                BotonNexo(
                    texto = "Aprobar",
                    onClick = onAprobar,
                    tipo = TipoBoton.ACCION,
                    icono = Icons.Filled.Check,
                    cargando = accion == AccionExplicacion.APROBAR,
                    habilitado = accion == null,
                    alto = 48.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// DETALLE: leer, editar y todas las acciones (pantalla completa, no un diálogo amontonado)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetalleExplicacionPantalla(
    tema: Tema,
    accion: AccionExplicacion?,
    error: String?,
    vm: ExplicacionesViewModel,
    onCerrar: () -> Unit
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val estado = tema.estadoExplicacion()
    val ocupado = accion != null
    // Sin explicación se abre directo en edición (crear a mano)
    var editando by rememberSaveable(tema.id) { mutableStateOf(estado == EstadoExplicacion.SIN_EXPLICACION) }
    var borrador by rememberSaveable(tema.id) { mutableStateOf(tema.explicacion ?: "") }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var verContenido by remember { mutableStateOf(false) }

    // Cuando llega una explicación nueva (IA o PDF), se actualiza el borrador
    LaunchedEffect(tema.explicacion) {
        if (!editando || borrador.isBlank()) borrador = tema.explicacion ?: ""
        if (!tema.explicacion.isNullOrBlank() && accion == null && estado != EstadoExplicacion.SIN_EXPLICACION) editando = false
    }

    val selectorPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            com.stiven.sos.utils.ArchivosPdf.prepararParte(context, uri)
                .onSuccess { vm.generarDesdePdf(tema.id, it) }
                .onFailure { vm.errorLocal(tema.id, it.message ?: "No se pudo leer el PDF") }
        }
    }

    PantallaSuperpuestaNexo(onAtras = { if (!ocupado) onCerrar() }) {
        FondoNexo(conCuadricula = false) {
            Column(Modifier.fillMaxSize()) {
                EncabezadoNexo(
                    titulo = tema.titulo ?: "Tema",
                    subtitulo = "Explicación · ${textoEstado(estado)}",
                    onVolver = { if (!ocupado) onCerrar() },
                    tono = TonoEncabezado.IA
                )

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding()) {
                    ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Resumen del estado
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EtiquetaEstadoNexo(textoEstado(estado), colorEstado(estado))
                                if (estado != EstadoExplicacion.SIN_EXPLICACION) {
                                    EtiquetaEstadoNexo(if (tema.explicacionFuente == "ia") "Escrita por IA" else "Escrita por docente",
                                        if (tema.explicacionFuente == "ia") p.ia else p.marca)
                                    val palabras = (tema.explicacion ?: "").split(Regex("\\s+")).count { it.isNotBlank() }
                                    EtiquetaEstadoNexo("$palabras palabras · ${(palabras / 200).coerceAtLeast(1)} min de lectura", p.marca)
                                }
                            }

                            // Acción en curso (IA o PDF)
                            if (accion == AccionExplicacion.GENERAR_IA || accion == AccionExplicacion.GENERAR_PDF) {
                                TarjetaNexo(Modifier.fillMaxWidth(), acento = p.ia) {
                                    GloboRobi(
                                        titulo = "ASISTENTE IA",
                                        mensaje = if (accion == AccionExplicacion.GENERAR_PDF) "Estoy leyendo tu PDF y escribiendo la explicación… puede tardar hasta un minuto."
                                        else "Estoy escribiendo una explicación detallada del tema… puede tardar hasta un minuto.",
                                        expresion = ExpresionRobi.PIENSA,
                                        tamanoRobi = 60.dp
                                    )
                                }
                            }

                            // Error de la última acción + salida manual
                            if (error != null) {
                                NotaNexo(error, p.error, Icons.Outlined.ErrorOutline, titulo = "No se pudo completar")
                                if (!editando) {
                                    BotonNexo("Escribirla manualmente", { editando = true }, tipo = TipoBoton.SECUNDARIO, icono = Icons.Filled.Edit,
                                        modifier = Modifier.fillMaxWidth())
                                }
                            }

                            // Contenido del tema (plegable)
                            TarjetaNexo(Modifier.fillMaxWidth().clickable { verContenido = !verContenido }, padding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Description, null, tint = p.marcaTexto, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Contenido del tema", color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                    Icon(if (verContenido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (verContenido) "Ocultar" else "Mostrar", tint = p.textoSuave)
                                }
                                AnimatedVisibility(verContenido) {
                                    Text(
                                        (tema.contenido ?: "").ifBlank { "Sin contenido" },
                                        color = p.textoSuave, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            if (editando) {
                                TarjetaNexo(Modifier.fillMaxWidth(), acento = p.marca) {
                                    Text(if (estado == EstadoExplicacion.SIN_EXPLICACION) "Escribe la explicación" else "Editar explicación",
                                        color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("Usa una línea terminada en dos puntos para cada sección (por ejemplo \"Ejemplos:\") y guiones para las listas.",
                                        color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(10.dp))
                                    CampoNexo(
                                        valor = borrador,
                                        onCambio = { borrador = it },
                                        etiqueta = "Explicación",
                                        lineasMinimas = 12,
                                        error = borrador.isNotBlank() && borrador.trim().length < 50,
                                        apoyo = if (borrador.isNotBlank() && borrador.trim().length < 50) "Mínimo 50 caracteres" else "${borrador.length} caracteres"
                                    )
                                }
                            } else if (estado != EstadoExplicacion.SIN_EXPLICACION) {
                                TarjetaNexo(Modifier.fillMaxWidth()) {
                                    TextoExplicacionNexo(tema.explicacion ?: "")
                                }
                            }

                            // Más acciones (todas disponibles)
                            if (!editando) {
                                TituloSeccionNexo("Más acciones", icono = Icons.Outlined.Tune)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccionChip("Editar", Icons.Outlined.Edit, p.marca, !ocupado) { borrador = tema.explicacion ?: ""; editando = true }
                                    AccionChip("Regenerar con IA", Icons.Outlined.AutoAwesome, p.ia, !ocupado, accion == AccionExplicacion.GENERAR_IA) { vm.generarConIA(tema) }
                                    AccionChip("Desde un PDF", Icons.Outlined.PictureAsPdf, p.ia, !ocupado, accion == AccionExplicacion.GENERAR_PDF) { selectorPdf.launch("application/pdf") }
                                    if (estado == EstadoExplicacion.PENDIENTE || estado == EstadoExplicacion.APROBADA) {
                                        AccionChip("Rechazar", Icons.Outlined.ThumbDown, p.error, !ocupado, accion == AccionExplicacion.RECHAZAR) { vm.rechazar(tema.id) }
                                    }
                                    if (estado != EstadoExplicacion.SIN_EXPLICACION) {
                                        AccionChip("Eliminar", Icons.Outlined.DeleteOutline, p.error, !ocupado, accion == AccionExplicacion.ELIMINAR) { confirmarEliminar = true }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // Barra de acciones principal (fija abajo, fácil de alcanzar con el pulgar)
                Surface(color = p.superficie, shadowElevation = 8.dp) {
                    ContenidoCentradoNexo(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (editando) {
                            val valido = borrador.trim().length >= 50
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BotonNexo(
                                    texto = if (estado == EstadoExplicacion.SIN_EXPLICACION) "Generar con IA" else "Cancelar",
                                    onClick = {
                                        if (estado == EstadoExplicacion.SIN_EXPLICACION) vm.generarConIA(tema)
                                        else { borrador = tema.explicacion ?: ""; editando = false }
                                    },
                                    tipo = if (estado == EstadoExplicacion.SIN_EXPLICACION) TipoBoton.IA else TipoBoton.SECUNDARIO,
                                    cargando = accion == AccionExplicacion.GENERAR_IA,
                                    habilitado = !ocupado,
                                    alto = 52.dp,
                                    modifier = Modifier.weight(1f)
                                )
                                BotonNexo(
                                    texto = "Guardar",
                                    onClick = { vm.guardar(tema.id, borrador, aprobar = false) },
                                    tipo = TipoBoton.MARCA,
                                    habilitado = valido && !ocupado,
                                    cargando = accion == AccionExplicacion.GUARDAR,
                                    alto = 52.dp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            BotonNexo(
                                texto = "Guardar y aprobar",
                                onClick = { vm.guardar(tema.id, borrador, aprobar = true) },
                                tipo = TipoBoton.ACCION,
                                icono = Icons.Filled.Verified,
                                habilitado = valido && !ocupado,
                                alto = 52.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BotonNexo(
                                    texto = "Editar",
                                    onClick = { borrador = tema.explicacion ?: ""; editando = true },
                                    tipo = TipoBoton.SECUNDARIO,
                                    icono = Icons.Filled.Edit,
                                    habilitado = !ocupado,
                                    alto = 52.dp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (estado != EstadoExplicacion.APROBADA) {
                                    BotonNexo(
                                        texto = "Aprobar",
                                        onClick = { vm.aprobar(tema.id) },
                                        tipo = TipoBoton.ACCION,
                                        icono = Icons.Filled.Check,
                                        cargando = accion == AccionExplicacion.APROBAR,
                                        habilitado = !ocupado,
                                        alto = 52.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    BotonNexo(
                                        texto = "Listo",
                                        onClick = onCerrar,
                                        tipo = TipoBoton.MARCA,
                                        habilitado = !ocupado,
                                        alto = 52.dp,
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

    if (confirmarEliminar) {
        DialogoAccionNexo(
            icono = Icons.Outlined.DeleteOutline,
            color = p.error,
            titulo = "¿Eliminar la explicación?",
            mensaje = "El tema quedará sin explicación y no se podrán generar preguntas con IA hasta que crees y apruebes una nueva.",
            textoPrincipal = "Eliminar",
            tipoPrincipal = TipoBoton.PELIGRO,
            onPrincipal = { confirmarEliminar = false; vm.eliminar(tema.id) },
            textoSecundario = "Cancelar",
            onSecundario = { confirmarEliminar = false },
            onDismiss = { confirmarEliminar = false }
        )
    }
}

@Composable
private fun AccionChip(texto: String, icono: ImageVector, color: Color, habilitado: Boolean, cargando: Boolean = false, onClick: () -> Unit) {
    val p = LocalNexo.current
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.superficie)
            .border(1.dp, color.copy(alpha = if (habilitado) 0.5f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (cargando) CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        else Icon(icono, null, tint = if (habilitado) color else p.textoTenue, modifier = Modifier.size(18.dp))
        Text(texto, color = if (habilitado) p.texto else p.textoTenue, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

/**
 * Muestra la explicación por secciones: una línea corta que termina en ":" es un título,
 * las líneas que empiezan con "- " son viñetas y lo demás son párrafos.
 */
@Composable
fun TextoExplicacionNexo(texto: String) {
    val p = LocalNexo.current
    val bloques = remember(texto) { texto.replace("\r", "").split("\n").map { it.trim() } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bloques.forEach { linea ->
            when {
                linea.isBlank() -> Spacer(Modifier.height(2.dp))
                linea.endsWith(":") && linea.length <= 48 -> Row(
                    Modifier.padding(top = 6.dp).semantics { heading() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(width = 4.dp, height = 18.dp).clip(CircleShape).background(p.ia))
                    Spacer(Modifier.width(8.dp))
                    Text(linea.removeSuffix(":"), color = p.iaTexto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                linea.startsWith("- ") || linea.startsWith("• ") -> Row {
                    Text("•", color = p.ia, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(linea.drop(2), color = p.texto, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
                }
                else -> Text(linea, color = p.texto, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 23.sp)
            }
        }
    }
}
