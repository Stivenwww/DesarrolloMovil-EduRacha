package com.stiven.sos

import com.stiven.sos.ui.theme.avisar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.*
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.PreguntaViewModel

class ValidacionPreguntasActivity : ComponentActivity() {

    private val viewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val temaId = intent.getStringExtra("TEMA_ID") ?: ""
        val temaTitulo = intent.getStringExtra("TEMA_TITULO") ?: temaId

        setContent {
            EduRachaTheme {
                ValidacionPreguntasScreen(
                    cursoTitulo = cursoTitulo,
                    cursoId = cursoId,
                    temaId = temaId,
                    temaTitulo = temaTitulo,
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun ValidacionPreguntasScreen(
    cursoTitulo: String,
    cursoId: String,
    temaId: String,
    temaTitulo: String,
    viewModel: PreguntaViewModel,
    onNavigateBack: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // "" = todos los temas del curso
    var temaSel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(temaId) }
    var titulosTemas by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var confirmarTodas by remember { mutableStateOf(false) }

    fun recargar() { if (cursoId.isNotEmpty()) viewModel.cargarPreguntas(cursoId = cursoId, estado = EstadoPregunta.PENDIENTE_REVISION) }

    LaunchedEffect(cursoId) {
        recargar()
        // Nombres de los temas para las tarjetas de selección
        runCatching { com.stiven.sos.api.ApiClient.apiService.obtenerTemasPorCurso(cursoId, null) }.getOrNull()
            ?.takeIf { it.isSuccessful }?.body()?.let { lista -> titulosTemas = lista.associate { it.id to it.titulo } }
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.avisar(it, com.stiven.sos.ui.theme.TipoAviso.ERROR)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.avisar(it, com.stiven.sos.ui.theme.TipoAviso.EXITO)
            viewModel.clearMessages()
        }
    }

    val pendientesCurso = remember(uiState.preguntas) { uiState.preguntas.filter { it.estado == EstadoPregunta.PENDIENTE_REVISION } }
    val porTema = remember(pendientesCurso) { pendientesCurso.groupingBy { it.temaId }.eachCount() }
    val preguntasFiltradas = remember(pendientesCurso, temaSel) {
        if (temaSel.isBlank()) pendientesCurso else pendientesCurso.filter { it.temaId == temaSel }
    }
    val nombreTemaSel = if (temaSel.isBlank()) null else (titulosTemas[temaSel] ?: temaTitulo.takeIf { temaSel == temaId } ?: "Tema")

    Scaffold(
        snackbarHost = { com.stiven.sos.ui.theme.AvisosNexoHost(snackbarHostState) },
        topBar = {
            ValidacionTopAppBar(
                cursoTitulo = cursoTitulo,
                temaTitulo = nombreTemaSel ?: "Todos los temas",
                pendingCount = preguntasFiltradas.size,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = p.fondo
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjetas de tema (seleccionar qué validar)
            item {
                Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.stiven.sos.ui.theme.TituloSeccionNexo("Elige el tema", icono = Icons.Default.Topic, color = p.ia)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            TarjetaTemaValidacion("Todos los temas", pendientesCurso.size, temaSel.isBlank()) { temaSel = "" }
                        }
                        val ids = (porTema.keys + titulosTemas.keys).distinct().sortedByDescending { porTema[it] ?: 0 }
                        items(ids) { id ->
                            TarjetaTemaValidacion(titulosTemas[id] ?: "Tema", porTema[id] ?: 0, temaSel == id) { temaSel = id }
                        }
                    }
                }
            }

            // Aprobar todas
            if (preguntasFiltradas.isNotEmpty()) {
                item {
                    com.stiven.sos.ui.theme.TarjetaNexo(Modifier.widthIn(max = 720.dp).fillMaxWidth(), acento = p.accion) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.stiven.sos.ui.theme.InsigniaNexo(Icons.Default.DoneAll, p.accion)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${preguntasFiltradas.size} por revisar", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text(
                                    "Puedes revisarlas una por una o aprobarlas todas" + (nombreTemaSel?.let { " de \"$it\"" } ?: " del curso") + ".",
                                    color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        com.stiven.sos.ui.theme.BotonNexo(
                            texto = "Aprobar todas (${preguntasFiltradas.size})",
                            onClick = { confirmarTodas = true },
                            tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                            icono = Icons.Default.DoneAll,
                            cargando = uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            when {
                uiState.isLoading && preguntasFiltradas.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.ia)
                    }
                }
                preguntasFiltradas.isEmpty() -> item {
                    com.stiven.sos.ui.theme.EstadoVacioNexo(
                        titulo = "¡Todo validado!",
                        mensaje = if (temaSel.isBlank()) "No hay preguntas pendientes en este curso." else "No hay preguntas pendientes en este tema.",
                        expresion = com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA,
                        textoAccion = if (temaSel.isNotBlank() && pendientesCurso.isNotEmpty()) "Ver todos los temas" else null,
                        onAccion = { temaSel = "" }
                    )
                }
                else -> items(preguntasFiltradas, key = { it.id ?: it.texto }) { pregunta ->
                    Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                        PreguntaCard(
                            pregunta = pregunta,
                            onAprobar = {
                                viewModel.actualizarEstadoPregunta(
                                    id = pregunta.id ?: "",
                                    estado = EstadoPregunta.APROBADA,
                                    notas = "Aprobada por el docente",
                                    onSuccess = { recargar() }
                                )
                            },
                            onRechazar = { motivo ->
                                viewModel.actualizarEstadoPregunta(
                                    id = pregunta.id ?: "",
                                    estado = EstadoPregunta.RECHAZADA,
                                    notas = motivo,
                                    onSuccess = { recargar() }
                                )
                            },
                            onEditar = { preguntaEditada ->
                                viewModel.actualizarPregunta(
                                    id = preguntaEditada.id ?: "",
                                    pregunta = preguntaEditada,
                                    onSuccess = { recargar() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmarTodas) {
        com.stiven.sos.ui.theme.DialogoAccionNexo(
            icono = Icons.Default.DoneAll,
            color = p.accion,
            titulo = "¿Aprobar ${preguntasFiltradas.size} preguntas?",
            mensaje = "Quedarán disponibles para los quizzes " +
                (nombreTemaSel?.let { "del tema \"$it\"" } ?: "de todo el curso") + ". Después puedes editarlas o desactivarlas en Preguntas revisadas.",
            textoPrincipal = "Sí, aprobar todas",
            tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.ACCION,
            iconoPrincipal = Icons.Default.DoneAll,
            onPrincipal = {
                confirmarTodas = false
                viewModel.aprobarPendientes(cursoId, temaSel.ifBlank { null }) { recargar() }
            },
            textoSecundario = "Revisar una por una",
            onSecundario = { confirmarTodas = false },
            onDismiss = { confirmarTodas = false }
        )
    }
}

/** Tarjeta para elegir el tema a validar: nombre, cuántas faltan y si está seleccionado. */
@Composable
private fun TarjetaTemaValidacion(titulo: String, pendientes: Int, seleccionado: Boolean, onClick: () -> Unit) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val color = if (pendientes > 0) p.ia else p.accion
    Column(
        Modifier
            .width(168.dp)
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (seleccionado) color.copy(alpha = 0.14f) else p.superficie)
            .border(if (seleccionado) 2.dp else 1.dp, if (seleccionado) color else p.borde, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (pendientes > 0) Icons.Default.PendingActions else Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.weight(1f))
            if (seleccionado) Icon(Icons.Default.RadioButtonChecked, "Seleccionado", tint = color, modifier = Modifier.size(18.dp))
        }
        Text(titulo, color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(
            if (pendientes == 0) "Al día" else "$pendientes por revisar",
            color = if (pendientes > 0) p.iaTexto else p.accionTexto, fontWeight = FontWeight.Black, fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidacionTopAppBar(
    cursoTitulo: String,
    temaTitulo: String,
    pendingCount: Int,
    onNavigateBack: () -> Unit
) {
    com.stiven.sos.ui.theme.EncabezadoNexo(
        titulo = "Validación de preguntas",
        subtitulo = if (temaTitulo.isNotBlank()) "$cursoTitulo · $temaTitulo" else cursoTitulo,
        onVolver = onNavigateBack,
        tono = com.stiven.sos.ui.theme.TonoEncabezado.IA,
        acciones = {
            com.stiven.sos.ui.theme.EtiquetaEncabezadoNexo("$pendingCount", Icons.Default.PendingActions)
        }
    )
}




@Composable
fun PreguntaCard(
    pregunta: Pregunta,
    onAprobar: () -> Unit,
    onRechazar: (String) -> Unit,
    onEditar: (Pregunta) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Tamaños responsivos para el card
    val cardPadding = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
    val cardCornerRadius = (screenWidth * 0.05f).coerceIn(16.dp, 24.dp)
    val badgeCornerRadius = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    val titleSize = (screenWidth * 0.042f).coerceIn(15.dp, 19.dp).value.sp
    val bodySize = (screenWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp
    val badgeSize = (screenWidth * 0.028f).coerceIn(10.dp, 12.dp).value.sp

    val iconSize = (screenWidth * 0.045f).coerceIn(16.dp, 20.dp)
    val buttonIconSize = (screenWidth * 0.045f).coerceIn(16.dp, 20.dp)
    val spacing = (screenWidth * 0.03f).coerceIn(10.dp, 16.dp)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(cardPadding)) {
            // Header con ID y Dificultad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))) {
                    val idPregunta = pregunta.id?.takeLast(4) ?: "N/A"
                    Surface(
                        color = EduRachaColors.Primary.copy(0.15f),
                        shape = RoundedCornerShape(badgeCornerRadius)
                    ) {
                        Text(
                            "ID: $idPregunta",
                            Modifier.padding(
                                horizontal = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp),
                                vertical = (screenWidth * 0.01f).coerceIn(3.dp, 5.dp)
                            ),
                            color = EduRachaColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = badgeSize
                        )
                    }

                    val dificultad = pregunta.dificultad?.lowercase() ?: "media"
                    val colorDificultad = when (dificultad) {
                        "facil", "bajo" -> EduRachaColors.Success
                        "media", "medio" -> EduRachaColors.Warning
                        "dificil", "alto" -> EduRachaColors.Error
                        else -> EduRachaColors.TextSecondary
                    }
                    Surface(
                        color = colorDificultad.copy(0.15f),
                        shape = RoundedCornerShape(badgeCornerRadius)
                    ) {
                        Text(
                            dificultad.replaceFirstChar { it.uppercase() },
                            Modifier.padding(
                                horizontal = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp),
                                vertical = (screenWidth * 0.01f).coerceIn(3.dp, 5.dp)
                            ),
                            color = colorDificultad,
                            fontWeight = FontWeight.Bold,
                            fontSize = badgeSize,
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expandir",
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            Spacer(Modifier.height(spacing))

            // Texto de la pregunta
            Text(
                pregunta.texto,
                fontSize = titleSize,
                fontWeight = FontWeight.SemiBold,
                lineHeight = titleSize * 1.3f
            )
            Spacer(Modifier.height(spacing * 0.75f))

            // Opciones
            pregunta.opciones.forEachIndexed { index, opcion ->
                OpcionItem(('A' + index).toString(), opcion.texto, opcion.esCorrecta)
                if (index < pregunta.opciones.size - 1) Spacer(Modifier.height((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
            }

            // Mostrar explicación
            Spacer(Modifier.height(spacing))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(spacing * 0.75f))

            if (!pregunta.explicacionCorrecta.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardCornerRadius * 0.6f),
                    color = EduRachaColors.Primary.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing),
                        verticalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize),
                                tint = EduRachaColors.Primary
                            )
                            Text(
                                "Explicación de la respuesta *",
                                fontSize = bodySize * 0.93f,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Primary
                            )
                        }
                        Text(
                            text = pregunta.explicacionCorrecta,
                            fontSize = bodySize,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = bodySize * 1.4f
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cardCornerRadius * 0.6f),
                    color = EduRachaColors.Error.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        EduRachaColors.Error
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(spacing),
                        horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = EduRachaColors.Error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                " Explicación obligatoria faltante",
                                fontSize = bodySize * 0.93f,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Error
                            )
                            Text(
                                "Debes agregar una explicación antes de aprobar",
                                fontSize = bodySize * 0.86f,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Sección Expandible (Metadatos)
            if (expanded) {
                Spacer(Modifier.height(spacing))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(spacing))

                Text(
                    "Información adicional",
                    fontSize = bodySize,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))

                InfoRow(
                    Icons.Default.AutoAwesome,
                    "Generado por",
                    pregunta.metadatosIA?.generadoPor ?: "No especificado"
                )
                Spacer(Modifier.height((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                val fuente = pregunta.fuente
                if(fuente.isNotBlank()) {
                    InfoRow(Icons.Default.Bookmark, "Fuente", fuente)
                    Spacer(Modifier.height((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                }
                InfoRow(Icons.Default.Person, "Creado por", pregunta.creadoPor)
                Spacer(Modifier.height((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                InfoRow(Icons.Default.CalendarMonth, "Fecha", pregunta.fechaCreacion)
            }

            // Botones de Acción
            Spacer(Modifier.height(spacing * 1.25f))

            // Hacer responsive los botones basado en el ancho de pantalla
            if (screenWidth < 400.dp) {
                // En pantallas pequeñas, apilar los botones
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                ) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                        Text("Editar", fontSize = bodySize)
                    }
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EduRachaColors.Error
                        )
                    ) {
                        Icon(Icons.Default.ThumbDown, contentDescription = "Rechazar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                        Text("Rechazar", fontSize = bodySize)
                    }
                    Button(
                        onClick = onAprobar,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !pregunta.explicacionCorrecta.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Success
                        )
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Aprobar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                        Text("Aprobar", fontSize = bodySize)
                    }
                }
            } else {
                // En pantallas normales/grandes, mostrar en fila
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((screenWidth * 0.02f).coerceIn(6.dp, 10.dp))
                ) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.015f).coerceIn(4.dp, 8.dp)))
                        Text("Editar", fontSize = bodySize * 0.93f)
                    }
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EduRachaColors.Error
                        )
                    ) {
                        Icon(Icons.Default.ThumbDown, contentDescription = "Rechazar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.015f).coerceIn(4.dp, 8.dp)))
                        Text("Rechazar", fontSize = bodySize * 0.93f)
                    }
                    Button(
                        onClick = onAprobar,
                        modifier = Modifier.weight(1f),
                        enabled = !pregunta.explicacionCorrecta.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Success
                        )
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Aprobar", modifier = Modifier.size(buttonIconSize))
                        Spacer(Modifier.width((screenWidth * 0.015f).coerceIn(4.dp, 8.dp)))
                        Text("Aprobar", fontSize = bodySize * 0.93f)
                    }
                }
            }
        }
    }

    // Diálogo de Edición
    if (showEditDialog) {
        var textoEditado by remember { mutableStateOf(pregunta.texto) }
        var explicacionEditada by remember { mutableStateOf(pregunta.explicacionCorrecta ?: "") }
        val opcionesOriginales = pregunta.opciones
        var opcionesEditadas by remember {
            mutableStateOf(opcionesOriginales.map { it.texto })
        }
        var respuestaCorrectaIndex by remember {
            mutableStateOf(opcionesOriginales.indexOfFirst { it.esCorrecta })
        }

        val dialogConfig = LocalConfiguration.current
        val dialogWidth = dialogConfig.screenWidthDp.dp
        val dialogTitleSize = (dialogWidth * 0.05f).coerceIn(18.dp, 24.dp).value.sp
        val dialogBodySize = (dialogWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp
        val dialogLabelSize = (dialogWidth * 0.035f).coerceIn(12.dp, 15.dp).value.sp

        val dialogIconSize = (dialogWidth * 0.05f).coerceIn(18.dp, 24.dp)
        val dialogPadding = (dialogWidth * 0.03f).coerceIn(10.dp, 16.dp)


        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Editar pregunta",
                    fontWeight = FontWeight.Bold,
                    fontSize = dialogTitleSize
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dialogPadding)
                ) {
                    // Texto de la pregunta
                    Text(
                        "Pregunta",
                        fontSize = dialogLabelSize,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    OutlinedTextField(
                        value = textoEditado,
                        onValueChange = { textoEditado = it },
                        label = { Text("Texto de la pregunta", fontSize = dialogBodySize * 0.9f) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape((dialogWidth * 0.03f).coerceIn(10.dp, 14.dp)),
                        textStyle = LocalTextStyle.current.copy(fontSize = dialogBodySize)
                    )

                    HorizontalDivider()

                    // Opciones
                    Text(
                        "Opciones de respuesta",
                        fontSize = dialogLabelSize,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    opcionesEditadas.forEachIndexed { index, textoOpcion ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape((dialogWidth * 0.03f).coerceIn(10.dp, 14.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (respuestaCorrectaIndex == index)
                                    EduRachaColors.Success.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding((dialogWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((dialogWidth * 0.02f).coerceIn(6.dp, 10.dp))
                            ) {
                                RadioButton(
                                    selected = respuestaCorrectaIndex == index,
                                    onClick = { respuestaCorrectaIndex = index },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EduRachaColors.Success
                                    )
                                )
                                OutlinedTextField(
                                    value = textoOpcion,
                                    onValueChange = {
                                        opcionesEditadas = opcionesEditadas.toMutableList()
                                            .also { list -> list[index] = it }
                                    },
                                    label = { Text("Opción ${'A' + index}", fontSize = dialogBodySize * 0.9f) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape((dialogWidth * 0.02f).coerceIn(6.dp, 10.dp)),
                                    textStyle = LocalTextStyle.current.copy(fontSize = dialogBodySize)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Campo para editar explicación
                    Text(
                        "Explicación (Obligatoria) *",
                        fontSize = dialogLabelSize,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                    OutlinedTextField(
                        value = explicacionEditada,
                        onValueChange = { explicacionEditada = it },
                        label = { Text("¿Por qué esta es la respuesta correcta?", fontSize = dialogBodySize * 0.9f) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape((dialogWidth * 0.03f).coerceIn(10.dp, 14.dp)),
                        isError = explicacionEditada.isBlank(),
                        supportingText = {
                            if (explicacionEditada.isBlank()) {
                                Text(
                                    "La explicación es obligatoria",
                                    color = EduRachaColors.Error,
                                    fontSize = dialogBodySize * 0.85f
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = if (explicacionEditada.isBlank())
                                    EduRachaColors.Error
                                else
                                    EduRachaColors.Primary,
                                modifier = Modifier.size(dialogIconSize)
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = dialogBodySize)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val preguntaEditada = pregunta.copy(
                            texto = textoEditado,
                            opciones = opcionesEditadas.mapIndexed { index, textoOpcion ->
                                Opcion(
                                    id = opcionesOriginales.getOrNull(index)?.id ?: index,
                                    texto = textoOpcion,
                                    esCorrecta = index == respuestaCorrectaIndex
                                )
                            },
                            explicacionCorrecta = explicacionEditada.takeIf { it.isNotBlank() },
                            modificada = true
                        )
                        onEditar(preguntaEditada)
                        showEditDialog = false
                    },
                    enabled = explicacionEditada.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    )
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size((dialogWidth * 0.045f).coerceIn(16.dp, 20.dp)))
                    Spacer(Modifier.width((dialogWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                    Text("Guardar Cambios", fontSize = dialogBodySize)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", fontSize = dialogBodySize)
                }
            },
            shape = RoundedCornerShape((dialogWidth * 0.05f).coerceIn(16.dp, 24.dp))
        )
    }

    // Diálogo de Rechazo
    if (showRejectDialog) {
        var motivo by remember { mutableStateOf("") }

        val dialogConfig = LocalConfiguration.current
        val dialogWidth = dialogConfig.screenWidthDp.dp
        val dialogTitleSize = (dialogWidth * 0.045f).coerceIn(16.dp, 20.dp).value.sp
        val dialogBodySize = (dialogWidth * 0.035f).coerceIn(13.dp, 16.dp).value.sp

        val dialogIconSize = (dialogWidth * 0.05f).coerceIn(18.dp, 24.dp)
        val dialogPadding = (dialogWidth * 0.03f).coerceIn(10.dp, 16.dp)


        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Text(
                    "Rechazar pregunta",
                    fontWeight = FontWeight.Bold,
                    fontSize = dialogTitleSize
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(dialogPadding)) {
                    Text(
                        "Indica el motivo por el cual rechazas esta pregunta:",
                        fontSize = dialogBodySize,
                        color = EduRachaColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo del rechazo", fontSize = dialogBodySize * 0.9f) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape((dialogWidth * 0.03f).coerceIn(10.dp, 14.dp)),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Comment,
                                contentDescription = null,
                                tint = EduRachaColors.Error,
                                modifier = Modifier.size(dialogIconSize)
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = dialogBodySize)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivo.isNotBlank()) {
                            onRechazar(motivo)
                            showRejectDialog = false
                        }
                    },
                    enabled = motivo.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    )
                ) {
                    Icon(Icons.Default.ThumbDown, null, Modifier.size((dialogWidth * 0.045f).coerceIn(16.dp, 20.dp)))
                    Spacer(Modifier.width((dialogWidth * 0.02f).coerceIn(6.dp, 10.dp)))
                    Text("Confirmar Rechazo", fontSize = dialogBodySize)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) {
                    Text("Cancelar", fontSize = dialogBodySize)
                }
            },
            shape = RoundedCornerShape((dialogWidth * 0.05f).coerceIn(16.dp, 24.dp))
        )
    }
}

@Composable
fun OpcionItem(letra: String, texto: String, esCorrecta: Boolean) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val itemPadding = (screenWidth * 0.03f).coerceIn(10.dp, 14.dp)
    val cornerRadius = (screenWidth * 0.03f).coerceIn(10.dp, 14.dp)
    val circleSize = (screenWidth * 0.07f).coerceIn(24.dp, 32.dp)
    val iconSize = (screenWidth * 0.05f).coerceIn(18.dp, 24.dp)
    val textSize = (screenWidth * 0.038f).coerceIn(14.dp, 17.dp).value.sp

    val backgroundColor = if (esCorrecta) {
        EduRachaColors.Success.copy(0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (esCorrecta) EduRachaColors.Success else Color.Transparent,
                RoundedCornerShape(cornerRadius)
            )
            .background(backgroundColor, RoundedCornerShape(cornerRadius))
            .padding(horizontal = itemPadding, vertical = itemPadding * 0.83f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(
                    if (esCorrecta) EduRachaColors.Success else MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letra,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = textSize * 0.9f
            )
        }
        Spacer(Modifier.width(itemPadding))
        Text(
            texto,
            fontSize = textSize,
            lineHeight = textSize * 1.3f,
            modifier = Modifier.weight(1f)
        )
        if (esCorrecta) {
            Spacer(Modifier.width(itemPadding * 0.5f))
            Icon(
                Icons.Default.CheckCircle,
                "Correcta",
                modifier = Modifier.size(iconSize),
                tint = EduRachaColors.Success
            )
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val iconSize = (screenWidth * 0.04f).coerceIn(14.dp, 18.dp)
    val labelSize = (screenWidth * 0.035f).coerceIn(13.dp, 15.dp).value.sp
    val valueSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
    val spacing = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Icon(
            icon,
            null,
            Modifier.size(iconSize),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$label:",
            fontSize = labelSize,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            fontSize = valueSize,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}