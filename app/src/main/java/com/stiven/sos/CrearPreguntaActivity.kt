package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import com.stiven.sos.viewmodel.TemaViewModel
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * Crear preguntas (docente).
 * 1. Elige curso y tema.  2. Elige cómo crearlas: escribirlas o generarlas con IA (hasta 30, en lotes de 10).
 * La pantalla nunca se cierra al guardar: las preguntas creadas quedan listadas abajo para editarlas o eliminarlas,
 * y desde ahí se puede ir directo a validarlas.
 * El PDF ya no va aquí: se usa al crear los temas del curso para que la IA escriba la explicación.
 */
class CrearPreguntaActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val temaViewModel: TemaViewModel by viewModels()
    private val preguntaViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) cursoViewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                CrearPreguntaScreen(
                    cursoViewModel = cursoViewModel,
                    temaViewModel = temaViewModel,
                    preguntaViewModel = preguntaViewModel,
                    onBack = { finish() },
                    onValidar = { curso, tema ->
                        startActivity(Intent(this, ValidacionPreguntasActivity::class.java).apply {
                            putExtra("CURSO_ID", curso.id)
                            putExtra("CURSO_TITULO", curso.titulo)
                            putExtra("TEMA_ID", tema.id)
                            putExtra("TEMA_TITULO", tema.titulo)
                        })
                    }
                )
            }
        }
    }
}

enum class ModoCreacion {
    MANUAL, IA
}

data class OpcionEditable(
    val id: Int,
    val texto: String,
    val esCorrecta: Boolean
)

/** Pregunta creada en esta sesión del formulario (para revisarla, editarla o eliminarla sin salir). */
data class PreguntaSesion(
    val id: String,
    val texto: String,
    val opciones: List<OpcionEditable>,
    val dificultad: String,
    val explicacion: String,
    val deIA: Boolean = false
)

private fun opcionesVacias() = listOf(
    OpcionEditable(0, "", false),
    OpcionEditable(1, "", false),
    OpcionEditable(2, "", false)
)

/** Lista de problemas del formulario manual, en el orden en que el docente los ve. */
fun validarPreguntaManual(texto: String, opciones: List<OpcionEditable>, explicacion: String): List<String> {
    val errores = mutableListOf<String>()
    if (texto.isBlank()) errores += "Escribe el texto de la pregunta."
    if (opciones.size < 2) errores += "Agrega al menos dos opciones."
    if (opciones.any { it.texto.isBlank() }) errores += "Todas las opciones deben tener texto."
    val repetidas = opciones.map { it.texto.trim().lowercase() }.filter { it.isNotEmpty() }
    if (repetidas.size != repetidas.toSet().size) errores += "Hay opciones repetidas."
    if (opciones.count { it.esCorrecta } != 1) errores += "Marca exactamente una respuesta correcta."
    if (explicacion.isBlank()) errores += "Explica por qué la respuesta es correcta."
    return errores
}

/** Gson puede dejar en null campos que Kotlin declara no nulos; esto evita cierres por NullPointerException. */
private fun seguro(texto: String?): String = texto ?: ""

/** Convierte una pregunta del servidor al registro de la sesión. */
private fun Pregunta.aSesion(deIA: Boolean): PreguntaSesion? {
    val idSeguro = id ?: return null
    return PreguntaSesion(
        id = idSeguro,
        texto = seguro(texto),
        opciones = (opciones as List<Opcion>?).orEmpty().mapIndexed { i, o -> OpcionEditable(i, seguro(o.texto), o.esCorrecta) },
        dificultad = dificultad ?: DificultadPregunta.MEDIO,
        explicacion = explicacionCorrecta ?: "",
        deIA = deIA
    )
}

private val LETRAS = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun CrearPreguntaScreen(
    cursoViewModel: CursoViewModel,
    temaViewModel: TemaViewModel,
    preguntaViewModel: PreguntaViewModel,
    onBack: () -> Unit,
    onValidar: (Curso, Tema) -> Unit
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val cursoUi by cursoViewModel.uiState.collectAsState()
    val temaUi by temaViewModel.uiState.collectAsState()
    val preguntaUi by preguntaViewModel.uiState.collectAsState()
    val progresoIA by preguntaViewModel.progresoIA.collectAsState()

    // Selección (se conserva al girar la pantalla)
    var cursoId by rememberSaveable { mutableStateOf<String?>(null) }
    var temaId by rememberSaveable { mutableStateOf<String?>(null) }
    var modo by rememberSaveable { mutableStateOf(ModoCreacion.MANUAL) }
    var hoja by rememberSaveable { mutableStateOf(0) } // 0 ninguna, 1 cursos, 2 temas

    val uidDocente = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val cursos = remember(cursoUi.cursos, uidDocente) {
        cursoUi.cursos.filter { it.docenteId == uidDocente }.ifEmpty { cursoUi.cursos }
    }
    val curso = cursos.firstOrNull { it.id == cursoId }
    val tema = temaUi.temas.firstOrNull { it.id == temaId }

    // Formulario manual
    var textoPregunta by rememberSaveable { mutableStateOf("") }
    var dificultad by rememberSaveable { mutableStateOf(DificultadPregunta.MEDIO) }
    var opciones by remember { mutableStateOf(opcionesVacias()) }
    var explicacion by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf<List<String>>(emptyList()) }
    var editandoId by rememberSaveable { mutableStateOf<String?>(null) }
    var porEliminar by remember { mutableStateOf<PreguntaSesion?>(null) }

    // IA
    var cantidad by rememberSaveable { mutableStateOf(10) }

    val sesion = preguntaViewModel.preguntasSesion
    val generando = progresoIA != null

    fun limpiarFormulario() {
        textoPregunta = ""
        opciones = opcionesVacias()
        explicacion = ""
        errores = emptyList()
        editandoId = null
    }

    LaunchedEffect(cursoId) {
        cursoId?.let { temaViewModel.cargarTemasPorCurso(it, "todos") }
    }

    // Avisos con el diseño Nexo (no cierran la pantalla)
    LaunchedEffect(preguntaUi.successMessage) {
        preguntaUi.successMessage?.let {
            Avisos.mostrar(context, it, TipoAviso.EXITO)
            preguntaViewModel.clearMessages()
        }
    }

    FondoNexo {
        Column(Modifier.fillMaxSize()) {
            EncabezadoNexo(
                titulo = "Crear preguntas",
                subtitulo = "Banco de preguntas",
                onVolver = onBack,
                tono = TonoEncabezado.IA,
                acciones = { if (sesion.isNotEmpty()) EtiquetaEncabezadoNexo("${sesion.size} nuevas", Icons.Filled.Quiz) }
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // ---------- Paso 1: curso y tema ----------
                        PasoNexo(1, "Elige el curso y el tema", color = p.ia, completo = tema != null)
                        SelectorNexo(
                            etiqueta = "Curso",
                            valor = curso?.titulo,
                            ayuda = if (cursoUi.isLoading) "Cargando cursos…" else "Toca para elegir un curso",
                            icono = Icons.Outlined.MenuBook,
                            color = p.marca,
                            onClick = { hoja = 1 }
                        )
                        if (curso != null) {
                            SelectorNexo(
                                etiqueta = "Tema",
                                valor = tema?.titulo,
                                ayuda = if (temaUi.isLoading) "Cargando temas…" else "Toca para elegir un tema",
                                icono = Icons.Outlined.Topic,
                                color = p.ia,
                                onClick = { hoja = 2 },
                                habilitado = !temaUi.isLoading,
                                extremo = tema?.let { t -> { EstadoExplicacionChip(t) } }
                            )
                        }

                        if (curso != null && tema != null) {
                            // ---------- Paso 2: modo ----------
                            PasoNexo(2, "¿Cómo quieres crearlas?", color = p.ia)
                            SegmentadoNexo(
                                opciones = listOf(
                                    Triple(ModoCreacion.MANUAL, "Escribirlas", Icons.Outlined.Edit),
                                    Triple(ModoCreacion.IA, "Con IA", Icons.Outlined.AutoAwesome)
                                ),
                                seleccion = modo,
                                onCambio = { if (!generando) modo = it },
                                colores = { if (it == ModoCreacion.IA) p.ia else p.marca }
                            )

                            // ---------- Paso 3 ----------
                            when (modo) {
                                ModoCreacion.MANUAL -> FormularioManualNexo(
                                    texto = textoPregunta,
                                    onTexto = { textoPregunta = it },
                                    dificultad = dificultad,
                                    onDificultad = { dificultad = it },
                                    opciones = opciones,
                                    onOpciones = { opciones = it },
                                    explicacion = explicacion,
                                    onExplicacion = { explicacion = it },
                                    errores = errores,
                                    editando = editandoId != null,
                                    guardando = preguntaUi.isLoading,
                                    onCancelarEdicion = { limpiarFormulario() },
                                    onGuardar = {
                                        errores = validarPreguntaManual(textoPregunta, opciones, explicacion)
                                        if (errores.isEmpty()) {
                                            val registroBase = PreguntaSesion(
                                                id = editandoId ?: "",
                                                texto = textoPregunta.trim(),
                                                opciones = opciones.map { it.copy(texto = it.texto.trim()) },
                                                dificultad = dificultad,
                                                explicacion = explicacion.trim(),
                                                deIA = sesion.firstOrNull { it.id == editandoId }?.deIA ?: false
                                            )
                                            guardarPregunta(context, preguntaViewModel, curso, tema, registroBase, editandoId) { id ->
                                                val registro = registroBase.copy(id = id)
                                                val pos = sesion.indexOfFirst { it.id == id }
                                                if (pos >= 0) sesion[pos] = registro else sesion.add(0, registro)
                                                limpiarFormulario()
                                            }
                                        }
                                    }
                                )
                                ModoCreacion.IA -> GeneradorIANexo(
                                    tema = tema,
                                    cantidad = cantidad,
                                    onCantidad = { cantidad = it },
                                    progreso = progresoIA,
                                    onGenerar = {
                                        val cId = curso.id ?: return@GeneradorIANexo
                                        val textoTema = seguro(tema.titulo) +
                                            seguro(tema.contenido).let { if (it.isNotBlank()) "\n\n" + it.take(1500) else "" }
                                        preguntaViewModel.generarPreguntasPorLotes(
                                            cursoId = cId,
                                            temaId = tema.id,
                                            temaTexto = textoTema,
                                            total = cantidad,
                                            onLote = { nuevas ->
                                                nuevas.mapNotNull { it.aSesion(deIA = true) }.forEach { sesion.add(0, it) }
                                            },
                                            onFin = { _, _ -> }
                                        )
                                    },
                                    onDetener = { preguntaViewModel.detenerGeneracion() }
                                )
                            }

                            // Error (por ejemplo, el tema no tiene explicación aprobada)
                            preguntaUi.error?.let { error ->
                                NotaNexo(error, p.error, Icons.Outlined.ErrorOutline, titulo = "No se pudo completar")
                                if (error.contains("explicación aprobada", ignoreCase = true)) {
                                    GenerarExplicacionTemaCard(curso = curso, tema = tema)
                                }
                            }

                            // ---------- Creadas en esta sesión ----------
                            if (sesion.isNotEmpty()) {
                                PreguntasDeLaSesion(
                                    preguntas = sesion,
                                    editandoId = editandoId,
                                    onEditar = { pq ->
                                        modo = ModoCreacion.MANUAL
                                        editandoId = pq.id
                                        textoPregunta = pq.texto
                                        opciones = pq.opciones.ifEmpty { opcionesVacias() }
                                        dificultad = pq.dificultad
                                        explicacion = pq.explicacion
                                        errores = emptyList()
                                    },
                                    onEliminar = { porEliminar = it },
                                    onValidar = { onValidar(curso, tema) }
                                )
                            }
                        } else if (curso == null && !cursoUi.isLoading && cursoUi.error != null) {
                            EstadoVacioNexo(
                                titulo = "Sin conexión",
                                mensaje = cursoUi.error ?: "No se pudieron cargar tus cursos.",
                                expresion = ExpresionRobi.TRISTE,
                                textoAccion = "Reintentar",
                                onAccion = { cursoViewModel.obtenerCursos() }
                            )
                        } else if (curso == null) {
                            GloboRobi(
                                titulo = "ASISTENTE IA",
                                mensaje = "Elige un curso y un tema. Luego puedes escribir tus preguntas o pedirme que genere hasta ${PreguntaViewModel.MAX_PREGUNTAS_IA} de una vez.",
                                expresion = ExpresionRobi.SALUDA,
                                tamanoRobi = 64.dp
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // ---------- Hojas de selección ----------
    if (hoja == 1) {
        HojaSeleccionNexo(
            titulo = "Elige el curso",
            opciones = cursos.mapNotNull { c ->
                c.id?.let { OpcionHojaNexo(it, seguro(c.titulo), if (seguro(c.codigo).isNotBlank()) "Código: ${c.codigo}" else null) }
            },
            onElegir = { id ->
                if (id != cursoId) {
                    cursoId = id
                    temaId = null
                    sesion.clear()
                    limpiarFormulario()
                    preguntaViewModel.clearMessages()
                }
                hoja = 0
            },
            onCerrar = { hoja = 0 },
            icono = Icons.Outlined.MenuBook,
            color = p.marca,
            cargando = cursoUi.isLoading && cursos.isEmpty(),
            vacio = cursoUi.error ?: "Aún no tienes cursos. Crea uno desde el panel docente.",
            seleccionado = cursoId
        )
    }
    if (hoja == 2 && curso != null) {
        HojaSeleccionNexo(
            titulo = "Elige el tema",
            opciones = temaUi.temas.filter { it.id.isNotBlank() }.map { t ->
                val (texto, color) = estadoExplicacion(t)
                OpcionHojaNexo(t.id, seguro(t.titulo), null, texto, color)
            },
            onElegir = { id ->
                if (id != temaId) {
                    temaId = id
                    limpiarFormulario()
                    preguntaViewModel.clearMessages()
                }
                hoja = 0
            },
            onCerrar = { hoja = 0 },
            icono = Icons.Outlined.Topic,
            color = p.ia,
            cargando = temaUi.isLoading,
            vacio = "Este curso todavía no tiene temas.",
            seleccionado = temaId
        )
    }

    porEliminar?.let { pq ->
        DialogoAccionNexo(
            icono = Icons.Outlined.DeleteOutline,
            color = p.error,
            titulo = "¿Eliminar esta pregunta?",
            mensaje = "\"${pq.texto.take(90)}\" se borrará del banco. Esta acción no se puede deshacer.",
            textoPrincipal = "Eliminar",
            tipoPrincipal = TipoBoton.PELIGRO,
            onPrincipal = {
                preguntaViewModel.eliminarPregunta(pq.id) {
                    sesion.removeAll { it.id == pq.id }
                    if (editandoId == pq.id) limpiarFormulario()
                }
                porEliminar = null
            },
            textoSecundario = "Cancelar",
            onSecundario = { porEliminar = null },
            onDismiss = { porEliminar = null }
        )
    }
}

/** Texto y color del estado de la explicación del tema (las preguntas con IA necesitan una aprobada). */
@Composable
private fun estadoExplicacion(t: Tema): Pair<String, Color> {
    val p = LocalNexo.current
    return when {
        t.explicacion.isNullOrBlank() -> "Sin explicación" to p.error
        t.explicacionEstado == "aprobada" -> "Aprobada" to p.accion
        else -> "Pendiente" to p.racha
    }
}

@Composable
private fun EstadoExplicacionChip(t: Tema) {
    val (texto, color) = estadoExplicacion(t)
    EtiquetaEstadoNexo(texto, color)
}

// ============================================================================
// FORMULARIO MANUAL
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioManualNexo(
    texto: String,
    onTexto: (String) -> Unit,
    dificultad: String,
    onDificultad: (String) -> Unit,
    opciones: List<OpcionEditable>,
    onOpciones: (List<OpcionEditable>) -> Unit,
    explicacion: String,
    onExplicacion: (String) -> Unit,
    errores: List<String>,
    editando: Boolean,
    guardando: Boolean,
    onCancelarEdicion: () -> Unit,
    onGuardar: () -> Unit
) {
    val p = LocalNexo.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.animateContentSize()) {
        PasoNexo(3, if (editando) "Editando la pregunta" else "Escribe la pregunta", color = p.marca)

        TarjetaNexo(Modifier.fillMaxWidth(), acento = if (editando) p.racha else null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EtiquetaEstadoNexo("Editando", p.racha)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Cancelar",
                            color = p.textoSuave,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onCancelarEdicion).padding(8.dp)
                        )
                    }
                }
                CampoNexo(
                    valor = texto,
                    onCambio = onTexto,
                    etiqueta = "Pregunta",
                    lineasMinimas = 3,
                    error = errores.isNotEmpty() && texto.isBlank(),
                    apoyo = "${texto.length} caracteres"
                )
                Text("Dificultad", color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple(DificultadPregunta.FACIL, "Fácil", p.accion),
                        Triple(DificultadPregunta.MEDIO, "Media", p.racha),
                        Triple(DificultadPregunta.DIFICIL, "Difícil", p.error)
                    ).forEach { (valor, etiqueta, color) ->
                        FichaNexo(etiqueta, dificultad == valor, color, { onDificultad(valor) }, Modifier.weight(1f))
                    }
                }
            }
        }

        TarjetaNexo(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Opciones de respuesta", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Toca el círculo de la opción correcta", color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (opciones.size < LETRAS.size) {
                    AccionPequenaNexo(Icons.Filled.Add, "Agregar opción", p.accion) {
                        onOpciones(opciones + OpcionEditable(opciones.size, "", false))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                opciones.forEachIndexed { index, opcion ->
                    FilaOpcionNexo(
                        letra = LETRAS.getOrElse(index) { "${index + 1}" },
                        opcion = opcion,
                        onTexto = { nuevo -> onOpciones(opciones.mapIndexed { i, o -> if (i == index) o.copy(texto = nuevo) else o }) },
                        onCorrecta = { onOpciones(opciones.mapIndexed { i, o -> o.copy(esCorrecta = i == index) }) },
                        onEliminar = if (opciones.size > 2) {
                            { onOpciones(opciones.filterIndexed { i, _ -> i != index }.mapIndexed { n, o -> o.copy(id = n) }) }
                        } else null
                    )
                }
            }
        }

        TarjetaNexo(Modifier.fillMaxWidth()) {
            Text("¿Por qué es la correcta?", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(
                "Los estudiantes la verán como retroalimentación después de responder.",
                color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            CampoNexo(
                valor = explicacion,
                onCambio = onExplicacion,
                etiqueta = "Explicación de la respuesta",
                lineasMinimas = 3,
                icono = Icons.Outlined.Lightbulb,
                error = errores.isNotEmpty() && explicacion.isBlank(),
                colorFoco = p.racha
            )
        }

        AnimatedVisibility(errores.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            NotaNexo(
                errores.joinToString("\n") { "• $it" },
                p.error,
                Icons.Outlined.ErrorOutline,
                titulo = "Revisa antes de guardar"
            )
        }

        BotonNexo(
            texto = if (editando) "Guardar cambios" else "Guardar y crear otra",
            onClick = onGuardar,
            tipo = TipoBoton.ACCION,
            icono = if (editando) Icons.Filled.Save else Icons.Filled.Add,
            cargando = guardando,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccionPequenaNexo(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) { Icon(icono, null, tint = color) }
}

@Composable
private fun FilaOpcionNexo(
    letra: String,
    opcion: OpcionEditable,
    onTexto: (String) -> Unit,
    onCorrecta: () -> Unit,
    onEliminar: (() -> Unit)?
) {
    val p = LocalNexo.current
    val color = if (opcion.esCorrecta) p.accion else p.marca
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (opcion.esCorrecta) p.accion else p.superficieAlt)
                .border(2.dp, if (opcion.esCorrecta) p.accion else p.borde, CircleShape)
                .clickable(role = Role.RadioButton, onClick = onCorrecta)
                .semantics {
                    contentDescription = "Marcar opción $letra como correcta"
                    stateDescription = if (opcion.esCorrecta) "Correcta" else "No marcada"
                },
            contentAlignment = Alignment.Center
        ) {
            if (opcion.esCorrecta) Icon(Icons.Filled.Check, null, tint = p.sobreAccion)
            else Text(letra, color = p.textoSuave, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        CampoNexo(
            valor = opcion.texto,
            onCambio = onTexto,
            etiqueta = if (opcion.esCorrecta) "Opción $letra · correcta" else "Opción $letra",
            modifier = Modifier.weight(1f),
            colorFoco = color,
            lineasMaximas = 3
        )
        if (onEliminar != null) {
            IconButton(onClick = onEliminar, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, "Quitar opción $letra", tint = p.textoSuave)
            }
        }
    }
}

// ============================================================================
// GENERADOR CON IA (hasta 30, por lotes)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneradorIANexo(
    tema: Tema,
    cantidad: Int,
    onCantidad: (Int) -> Unit,
    progreso: PreguntaViewModel.ProgresoIA?,
    onGenerar: () -> Unit,
    onDetener: () -> Unit
) {
    val p = LocalNexo.current
    val max = PreguntaViewModel.MAX_PREGUNTAS_IA
    val lotes = PreguntaViewModel.lotesPara(cantidad)
    val segundos = PreguntaViewModel.segundosEstimados(cantidad)
    val sinExplicacion = tema.explicacionEstado != "aprobada" || tema.explicacion.isNullOrBlank()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PasoNexo(3, "Genera preguntas con IA", color = p.ia)

        if (progreso != null) {
            TarjetaNexo(Modifier.fillMaxWidth(), acento = p.ia) {
                GloboRobi(
                    titulo = "LOTE ${progreso.lote} DE ${progreso.lotes}",
                    mensaje = progreso.mensaje,
                    expresion = if (progreso.esperando) ExpresionRobi.NORMAL else ExpresionRobi.PIENSA,
                    tamanoRobi = 60.dp
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${progreso.hechas}", color = p.iaTexto, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(" de ${progreso.total} preguntas", color = p.textoSuave, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(6.dp))
                BarraNexo(progreso.hechas / progreso.total.coerceAtLeast(1).toFloat(), color = p.ia, alto = 12.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Puedes quedarte en esta pantalla: cada lote se guarda apenas termina y aparece abajo.",
                    color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                BotonNexo("Detener", onDetener, tipo = TipoBoton.SECUNDARIO, icono = Icons.Filled.Stop, modifier = Modifier.fillMaxWidth())
            }
            return@Column
        }

        if (sinExplicacion) {
            NotaNexo(
                "La IA crea las preguntas a partir de la explicación aprobada del tema. Apruébala primero en Explicaciones (o genérala abajo).",
                p.racha, Icons.Outlined.Info, titulo = "Este tema aún no tiene explicación aprobada"
            )
        }

        TarjetaNexo(Modifier.fillMaxWidth(), acento = p.ia) {
            Text("¿Cuántas preguntas?", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccionPequenaNexo(Icons.Filled.Remove, "Menos preguntas", p.ia) { onCantidad((cantidad - 1).coerceAtLeast(1)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$cantidad", color = p.iaTexto, fontSize = 40.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.semantics { contentDescription = "$cantidad preguntas" })
                    Text("de $max como máximo", color = p.textoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                AccionPequenaNexo(Icons.Filled.Add, "Más preguntas", p.ia) { onCantidad((cantidad + 1).coerceAtMost(max)) }
            }
            Slider(
                value = cantidad.toFloat(),
                onValueChange = { onCantidad(it.toInt().coerceIn(1, max)) },
                valueRange = 1f..max.toFloat(),
                steps = max - 2,
                colors = SliderDefaults.colors(thumbColor = p.ia, activeTrackColor = p.ia, inactiveTrackColor = p.borde,
                    activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 30).forEach { n -> FichaNexo("$n", cantidad == n, p.ia, { onCantidad(n) }) }
            }
            Spacer(Modifier.height(12.dp))
            NotaNexo(
                if (lotes.size == 1) "Se genera en un solo lote. Tiempo aproximado: ${segundos} s."
                else "Se generan en ${lotes.size} lotes (${lotes.joinToString(" + ")}) con una pausa corta entre cada uno, " +
                    "para no superar el límite de la IA. Tiempo aproximado: ${if (segundos >= 60) "${segundos / 60} min ${segundos % 60} s" else "$segundos s"}.",
                p.marca, Icons.Outlined.Timer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No se repiten preguntas que el tema ya tenga. Todas quedan pendientes de revisión.",
                color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        }

        BotonNexo(
            texto = "Generar $cantidad preguntas",
            onClick = onGenerar,
            tipo = TipoBoton.IA,
            icono = Icons.Filled.AutoAwesome,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// PREGUNTAS DE LA SESIÓN
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreguntasDeLaSesion(
    preguntas: List<PreguntaSesion>,
    editandoId: String?,
    onEditar: (PreguntaSesion) -> Unit,
    onEliminar: (PreguntaSesion) -> Unit,
    onValidar: () -> Unit
) {
    val p = LocalNexo.current
    var expandida by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TituloSeccionNexo("Creadas ahora (${preguntas.size})", Modifier.weight(1f), Icons.Outlined.Checklist, p.accion)
        }
        Text("Quedan pendientes de revisión. Toca una para ver sus opciones.", color = p.textoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        preguntas.forEachIndexed { i, pq ->
            val abierta = expandida == pq.id
            TarjetaNexo(
                Modifier.fillMaxWidth().clickable { expandida = if (abierta) null else pq.id },
                acento = if (pq.id == editandoId) p.racha else null,
                padding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background((if (pq.deIA) p.ia else p.marca).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${preguntas.size - i}", color = if (pq.deIA) p.iaTexto else p.marcaTexto, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(pq.texto, color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp,
                            maxLines = if (abierta) 8 else 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            EtiquetaEstadoNexo(if (pq.deIA) "IA" else "Manual", if (pq.deIA) p.ia else p.marca)
                            EtiquetaEstadoNexo(
                                when (pq.dificultad) { DificultadPregunta.FACIL -> "Fácil"; DificultadPregunta.DIFICIL -> "Difícil"; else -> "Media" },
                                when (pq.dificultad) { DificultadPregunta.FACIL -> p.accion; DificultadPregunta.DIFICIL -> p.error; else -> p.racha }
                            )
                        }
                    }
                }
                AnimatedVisibility(abierta) {
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pq.opciones.forEachIndexed { k, o ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(LETRAS.getOrElse(k) { "${k + 1}" }, color = if (o.esCorrecta) p.accionTexto else p.textoSuave,
                                    fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(o.texto, color = if (o.esCorrecta) p.accionTexto else p.texto, fontSize = 14.sp,
                                    fontWeight = if (o.esCorrecta) FontWeight.Black else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (o.esCorrecta) Icon(Icons.Filled.CheckCircle, "Correcta", tint = p.accion, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (pq.explicacion.isNotBlank()) {
                            NotaNexo(pq.explicacion, p.racha, Icons.Outlined.Lightbulb)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FichaNexo("Editar", false, p.marca, { onEditar(pq) }, Modifier.weight(1f))
                    FichaNexo("Eliminar", false, p.error, { onEliminar(pq) }, Modifier.weight(1f))
                }
            }
        }
        BotonNexo(
            texto = "Revisar y aprobar",
            onClick = onValidar,
            tipo = TipoBoton.MARCA,
            icono = Icons.Filled.FactCheck,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// FUNCIONES
// ============================================================================

private fun guardarPregunta(
    context: Context,
    preguntaViewModel: PreguntaViewModel,
    curso: Curso,
    tema: Tema,
    datos: PreguntaSesion,
    idEdicion: String?,
    onGuardada: (String) -> Unit
) {
    val cursoId = curso.id ?: run {
        Avisos.mostrar(context, "El curso no tiene un identificador válido.", TipoAviso.ERROR)
        return
    }
    if (tema.id.isBlank()) {
        Avisos.mostrar(context, "El tema no tiene un identificador válido.", TipoAviso.ERROR)
        return
    }
    val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
    val docenteNombre = prefs.getString("user_name", null) ?: "Docente"

    val pregunta = Pregunta(
        id = null,
        cursoId = cursoId,
        temaId = tema.id,
        texto = datos.texto,
        opciones = datos.opciones.mapIndexed { i, o -> Opcion(id = i, texto = o.texto, esCorrecta = o.esCorrecta) },
        fuente = if (datos.deIA) FuentePregunta.IA else FuentePregunta.DOCENTE,
        estado = EstadoPregunta.PENDIENTE_REVISION,
        dificultad = datos.dificultad,
        creadoPor = docenteNombre,
        fechaCreacion = Instant.now().toString(),
        metadatosIA = null,
        revisadoPor = null,
        fechaRevision = null,
        notasRevision = null,
        modificada = idEdicion != null,
        explicacionCorrecta = datos.explicacion
    )
    preguntaViewModel.guardarPreguntaManual(pregunta, idEdicion, onGuardada)
}

/**
 * Atajo cuando el tema no tiene explicación aprobada: genera la explicación con IA
 * (queda "pendiente") para que el docente la apruebe en Explicaciones y luego genere preguntas.
 */
@Composable
private fun GenerarExplicacionTemaCard(curso: Curso, tema: Tema) {
    val p = LocalNexo.current
    val scope = rememberCoroutineScope()
    // 0 = sin iniciar, 1 = generando, 2 = lista, 3 = error
    var estado by remember(tema.id) { mutableStateOf(0) }
    var mensaje by remember(tema.id) { mutableStateOf("") }

    TarjetaNexo(modifier = Modifier.fillMaxWidth(), acento = p.ia) {
        GloboRobi(
            titulo = "ASISTENTE IA",
            mensaje = when (estado) {
                1 -> "Estoy escribiendo la explicación de \"${tema.titulo}\"... puede tardar hasta un minuto."
                2 -> "Listo. Ve a Panel docente > Explicaciones, apruébala y vuelve aquí para generar las preguntas."
                3 -> mensaje
                else -> "Las preguntas se basan en la explicación aprobada del tema. ¿Genero la explicación de \"${tema.titulo}\" ahora?"
            },
            expresion = when (estado) {
                1 -> ExpresionRobi.PIENSA
                2 -> ExpresionRobi.CELEBRA
                3 -> ExpresionRobi.TRISTE
                else -> ExpresionRobi.FELIZ
            },
            tamanoRobi = 60.dp
        )
        if (estado != 2) {
            Spacer(Modifier.height(12.dp))
            BotonNexo(
                texto = if (estado == 3) "Reintentar" else "Generar explicación con IA",
                onClick = {
                    val cursoId = curso.id ?: return@BotonNexo
                    estado = 1
                    scope.launch {
                        try {
                            val respuesta = com.stiven.sos.api.ApiClient.apiService.generarExplicacionIA(
                                cursoId = cursoId,
                                temaId = tema.id,
                                request = GenerarExplicacionRequest(
                                    cursoId = cursoId,
                                    temaId = tema.id,
                                    tituloTema = seguro(tema.titulo),
                                    contenidoTema = seguro(tema.contenido).ifBlank { null }
                                )
                            )
                            if (respuesta.isSuccessful) {
                                estado = 2
                            } else {
                                mensaje = com.stiven.sos.api.mensajeDeError(respuesta)
                                estado = 3
                            }
                        } catch (e: Exception) {
                            mensaje = "No pude conectar con el servidor: ${e.message ?: "error desconocido"}"
                            estado = 3
                        }
                    }
                },
                habilitado = estado != 1,
                cargando = estado == 1,
                tipo = TipoBoton.IA,
                icono = Icons.Filled.AutoAwesome,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
