package com.stiven.sos

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel
import kotlinx.coroutines.delay
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog

class ListaCursosActivity : ComponentActivity() {
    private val cursoViewModel: CursoViewModel by viewModels()
    private val preguntaViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val proposito = intent.getStringExtra("PROPOSITO")
        setContent {
            EduRachaTheme {
                ListaCursosScreen(
                    cursoViewModel = cursoViewModel,
                    preguntaViewModel = preguntaViewModel,
                    proposito = proposito,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val proposito = intent.getStringExtra("PROPOSITO")
        if (proposito == "VALIDAR_PREGUNTAS") {
            preguntaViewModel.cargarPreguntas(
                cursoId = null,
                estado = EstadoPregunta.PENDIENTE_REVISION
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCursosScreen(
    cursoViewModel: CursoViewModel,
    preguntaViewModel: PreguntaViewModel,
    proposito: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val preguntaUiState by preguntaViewModel.uiState.collectAsState()

    val cursos = cursoUiState.cursos
    val isLoading = cursoUiState.isLoading
    val errorMessage = cursoUiState.error
    val successMessage = cursoUiState.operationSuccess

    var searchQuery by remember { mutableStateOf("") }
    var estadoFiltro by remember { mutableStateOf("Todos") }
    var cursoParaDialogoDetalle by remember { mutableStateOf<Curso?>(null) }
    var cursoParaDialogoTemas by remember { mutableStateOf<Curso?>(null) }
    var cursoParaEliminar by remember { mutableStateOf<Curso?>(null) }
    val esModoValidacion = proposito == "VALIDAR_PREGUNTAS"

    LaunchedEffect(Unit) {
        cursoViewModel.obtenerCursos()
        if (esModoValidacion) {
            preguntaViewModel.cargarPreguntas(
                cursoId = null,
                estado = EstadoPregunta.PENDIENTE_REVISION
            )
        }
    }

    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            cursoViewModel.clearError()
        }
        successMessage?.let {
            Toast.makeText(context, "✓ $it", Toast.LENGTH_SHORT).show()
            cursoViewModel.clearSuccessMessage()
        }
    }

    val cursosFiltrados = remember(cursos, searchQuery, estadoFiltro) {
        cursos.filter { curso ->
            val cumpleBusqueda = searchQuery.isBlank() ||
                    curso.titulo.contains(searchQuery, ignoreCase = true) ||
                    curso.codigo.contains(searchQuery, ignoreCase = true)

            val cumpleFiltroEstado = estadoFiltro == "Todos" ||
                    curso.estado.equals(estadoFiltro, ignoreCase = true)

            cumpleBusqueda && cumpleFiltroEstado
        }
    }

    val preguntasPendientes = remember(preguntaUiState.preguntas) {
        preguntaUiState.preguntas.filter {
            it.estado == EstadoPregunta.PENDIENTE_REVISION
        }
    }

    val preguntasPorCurso = remember(preguntasPendientes) {
        preguntasPendientes.groupBy { it.cursoId }
            .mapValues { it.value.size }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esModoValidacion) "Seleccionar Curso para Validar" else "Mis Cursos Creados",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (esModoValidacion && preguntasPendientes.isNotEmpty()) {
                        Surface(
                            color = EduRachaColors.Warning,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${preguntasPendientes.size} pendientes",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EduRachaColors.Primary)
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(if (esModoValidacion) 1f else 0.7f)) {
                    SearchBarMejorada(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )
                }
                if (!esModoValidacion) {
                    Box(modifier = Modifier.weight(0.3f)) {
                        EstadoFilterDropdown(
                            estadoSeleccionado = estadoFiltro,
                            onEstadoChange = { estadoFiltro = it }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && cursos.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = EduRachaColors.Primary
                        )
                    }

                    errorMessage != null && cursos.isEmpty() -> {
                        ErrorMessage(
                            message = errorMessage ?: "Error desconocido",
                            onRetry = { cursoViewModel.obtenerCursos() }
                        )
                    }

                    cursosFiltrados.isEmpty() -> {
                        EmptyCursosState(isSearchActive = searchQuery.isNotBlank() || estadoFiltro != "Todos")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (esModoValidacion) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = EduRachaColors.Accent.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                null,
                                                tint = EduRachaColors.Accent
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                "Selecciona un curso y luego un tema para revisar sus preguntas.",
                                                color = EduRachaColors.TextSecondary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(cursosFiltrados, key = { _, curso -> curso.id!! }) { index, curso ->
                                AnimatedCursoCard(
                                    curso = curso,
                                    index = index,
                                    preguntasPendientes = if (esModoValidacion)
                                        preguntasPorCurso[curso.id] ?: 0
                                    else null,
                                    onClick = {
                                        if (esModoValidacion) {
                                            cursoParaDialogoTemas = curso
                                        } else {
                                            cursoParaDialogoDetalle = curso
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de detalles del curso mejorado
    if (cursoParaDialogoDetalle != null) {
        CursoDetailDialogMejorado(
            curso = cursoParaDialogoDetalle!!,
            onDismiss = { cursoParaDialogoDetalle = null },
            onEdit = { /* Se maneja internamente */ },
            onDelete = {
                cursoParaEliminar = cursoParaDialogoDetalle
                cursoParaDialogoDetalle = null
            }
        )
    }

    // Diálogo de confirmación de eliminación
    if (cursoParaEliminar != null) {
        ConfirmDeleteDialog(
            curso = cursoParaEliminar!!,
            onConfirm = {
                cursoViewModel.eliminarCurso(cursoParaEliminar!!.id!!)
                cursoParaEliminar = null
            },
            onDismiss = { cursoParaEliminar = null }
        )
    }

    // Diálogo para seleccionar tema
    if (cursoParaDialogoTemas != null) {
        SeleccionarTemaDialog(
            curso = cursoParaDialogoTemas!!,
            preguntasPorTema = preguntasPendientes
                .filter { it.cursoId == cursoParaDialogoTemas?.id }
                .groupBy { it.temaId }
                .mapValues { it.value.size },
            onDismiss = { cursoParaDialogoTemas = null },
            onTemaSelected = { cursoId, temaId, temaTitulo ->
                val intent = Intent(context, ValidacionPreguntasActivity::class.java).apply {
                    putExtra("CURSO_ID", cursoId)
                    putExtra("CURSO_TITULO", cursoParaDialogoTemas?.titulo ?: "")
                    putExtra("TEMA_ID", temaId)
                    putExtra("TEMA_TITULO", temaTitulo)
                }
                context.startActivity(intent)
                cursoParaDialogoTemas = null
            }
        )
    }
}

@Composable
fun AnimatedCursoCard(
    curso: Curso,
    index: Int,
    preguntasPendientes: Int? = null,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 70L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(obtenerColorEstado(curso.estado))
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(obtenerColorEstado(curso.estado).copy(alpha = 0.1f))
                            .border(
                                2.dp,
                                obtenerColorEstado(curso.estado).copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = obtenerColorEstado(curso.estado),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = curso.titulo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = curso.codigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = obtenerColorEstado(curso.estado)
                            )
                            Text(" • ", fontSize = 14.sp, color = EduRachaColors.TextSecondary)
                            Text(
                                "${curso.duracionDias} días",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = obtenerColorEstado(curso.estado).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = obtenerIconoEstado(curso.estado),
                                        contentDescription = null,
                                        tint = obtenerColorEstado(curso.estado),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = curso.estado.replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = obtenerColorEstado(curso.estado)
                                    )
                                }
                            }

                            if (preguntasPendientes != null && preguntasPendientes > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EduRachaColors.Warning.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = EduRachaColors.Warning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$preguntasPendientes pendientes",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EduRachaColors.Warning
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Detalles",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CursoDetailDialogMejorado(
    curso: Curso,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var modoEdicion by remember { mutableStateOf(false) }

    if (modoEdicion) {
        EditarCursoDialog(
            curso = curso,
            onDismiss = { modoEdicion = false; onDismiss() },
            onCancel = { modoEdicion = false }
        )
    } else {
        DetallesCursoDialog(
            curso = curso,
            onDismiss = onDismiss,
            onEdit = { modoEdicion = true },
            onDelete = onDelete
        )
    }
}

@Composable
fun DetallesCursoDialog(
    curso: Curso,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header con color de estado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    obtenerColorEstado(curso.estado),
                                    obtenerColorEstado(curso.estado).copy(alpha = 0.7f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            curso.titulo,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Contenido
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Info Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(
                            icon = Icons.Outlined.Tag,
                            label = "Código",
                            value = curso.codigo,
                            color = EduRachaColors.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        InfoCard(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Duración",
                            value = "${curso.duracionDias} días",
                            color = EduRachaColors.Accent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(
                            icon = obtenerIconoEstado(curso.estado),
                            label = "Estado",
                            value = curso.estado.replaceFirstChar { it.uppercase() },
                            color = obtenerColorEstado(curso.estado),
                            modifier = Modifier.weight(1f)
                        )
                        InfoCard(
                            icon = Icons.Outlined.Person,
                            label = "Docente ID",
                            value = curso.docenteId.take(8) + "...",
                            color = EduRachaColors.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Descripción
                    if (curso.descripcion.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Descripción",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = EduRachaColors.Background
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                curso.descripcion,
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Botón de usuarios asignados
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(context, UsuariosAsignadosActivity::class.java).apply {
                                putExtra("CURSO_ID", curso.id)
                                putExtra("CURSO_TITULO", curso.titulo)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Info
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Ver Usuarios Asignados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Botones de acción
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EduRachaColors.Error
                            ),
                            border = BorderStroke(2.dp, EduRachaColors.Error),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EduRachaColors.Primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Editar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditarCursoDialog(
    curso: Curso,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: CursoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var titulo by remember { mutableStateOf(curso.titulo) }
    var descripcion by remember { mutableStateOf(curso.descripcion) }
    var duracionDias by remember { mutableStateOf(curso.duracionDias.toString()) }
    var estado by remember { mutableStateOf(curso.estado) }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = EduRachaColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Editar Curso",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            "Actualiza la información del curso",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Campos de edición
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del curso") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.MenuBook, null, tint = EduRachaColors.Primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EduRachaColors.Primary,
                        focusedLabelColor = EduRachaColors.Primary
                    )
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 4,
                    maxLines = 6,
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, null, tint = EduRachaColors.Primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EduRachaColors.Primary,
                        focusedLabelColor = EduRachaColors.Primary
                    )
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = duracionDias,
                    onValueChange = { duracionDias = it.filter { c -> c.isDigit() } },
                    label = { Text("Duración (días)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.CalendarToday, null, tint = EduRachaColors.Primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EduRachaColors.Primary,
                        focusedLabelColor = EduRachaColors.Primary
                    )
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Estado del curso",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                EstadoSelectorMejorado(
                    estadoSeleccionado = estado,
                    onEstadoSeleccionado = { estado = it }
                )

                Spacer(Modifier.height(28.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, EduRachaColors.TextSecondary.copy(alpha = 0.3f))
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val cursoActualizado = curso.copy(
                                titulo = titulo,
                                descripcion = descripcion,
                                duracionDias = duracionDias.toIntOrNull() ?: curso.duracionDias,
                                estado = estado
                            )
                            viewModel.actualizarCurso(cursoActualizado)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Success
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    curso: Curso,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                "¿Eliminar curso?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Estás a punto de eliminar el curso:",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = EduRachaColors.Error.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            curso.titulo,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Código: ${curso.codigo}",
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Esta acción no se puede deshacer. Se eliminarán todos los datos asociados al curso.",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sí, eliminar curso", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = BorderStroke(2.dp, EduRachaColors.TextSecondary.copy(alpha = 0.3f))
            ) {
                Text("Cancelar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = EduRachaColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EstadoSelectorMejorado(
    estadoSeleccionado: String,
    onEstadoSeleccionado: (String) -> Unit
) {
    val estados = listOf(
        "activo" to Icons.Outlined.CheckCircle,
        "inactivo" to Icons.Outlined.Cancel,
        "borrador" to Icons.Outlined.Edit,
        "archivado" to Icons.Outlined.Archive
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        estados.forEach { (estado, icono) ->
            val isSelected = estado == estadoSeleccionado
            val color = obtenerColorEstado(estado)

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEstadoSeleccionado(estado) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) color.copy(alpha = 0.15f)
                    else EduRachaColors.Background
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) color else EduRachaColors.SurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icono,
                        contentDescription = null,
                        tint = if (isSelected) color else EduRachaColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        estado.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) color else EduRachaColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SeleccionarTemaDialog(
    curso: Curso,
    preguntasPorTema: Map<String, Int>,
    onDismiss: () -> Unit,
    onTemaSelected: (cursoId: String, temaId: String, temaTitulo: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Quiz, null, tint = EduRachaColors.Accent) },
        title = { Text("Seleccionar Tema a Validar", fontWeight = FontWeight.Bold) },
        text = {
            val temas = curso.temas?.entries?.toList() ?: emptyList()

            if (temas.isEmpty()) {
                Text(
                    "Este curso no tiene temas registrados.",
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(temas, key = { it.key }) { (key, tema) ->
                        val temaIdReal = tema.id
                        val temaTitulo = tema.titulo
                        val cantidadPendientes = preguntasPorTema[temaIdReal] ?: 0

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTemaSelected(curso.id!!, temaIdReal, temaTitulo)
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EduRachaColors.SurfaceVariant)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    tema.titulo,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (cantidadPendientes > 0) {
                                    Surface(
                                        color = EduRachaColors.Warning,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$cantidadPendientes",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarMejorada(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por nombre o código...", color = EduRachaColors.TextSecondary.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = EduRachaColors.Primary) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, "Limpiar", tint = EduRachaColors.TextSecondary)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(50),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoFilterDropdown(estadoSeleccionado: String, onEstadoChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val estados = listOf("Todos", "activo", "inactivo", "borrador", "archivado")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = estadoSeleccionado.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtro") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = EduRachaColors.Primary.copy(alpha = 0.3f),
                focusedBorderColor = EduRachaColors.Primary,
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            estados.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.replaceFirstChar { it.uppercase() }) },
                    onClick = { onEstadoChange(item); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            null,
            tint = EduRachaColors.Error,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Oops, algo salió mal",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun EmptyCursosState(isSearchActive: Boolean) {
    val title = if (isSearchActive) "Sin resultados" else "No tienes cursos"
    val subtitle = if (isSearchActive)
        "Intenta con otra búsqueda o filtro."
    else
        "Crea tu primer curso para comenzar a organizar tu contenido."

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearchActive) Icons.Outlined.SearchOff else Icons.Outlined.School,
            null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            subtitle,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun obtenerColorEstado(estado: String): Color {
    return when (estado.lowercase()) {
        "activo" -> EduRachaColors.Success
        "inactivo" -> EduRachaColors.TextSecondary
        "borrador" -> EduRachaColors.Warning
        "archivado" -> EduRachaColors.Info
        else -> EduRachaColors.Primary
    }
}

private fun obtenerIconoEstado(estado: String): ImageVector {
    return when (estado.lowercase()) {
        "activo" -> Icons.Outlined.CheckCircle
        "inactivo" -> Icons.Outlined.Cancel
        "borrador" -> Icons.Outlined.Edit
        "archivado" -> Icons.Outlined.Archive
        else -> Icons.Outlined.Label
    }
}