package com.stiven.sos

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.models.Pregunta
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreguntasRevisadasScreen(
    preguntasViewModel: PreguntaViewModel,
    cursoViewModel: CursoViewModel,
    onBack: () -> Unit
) {
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()
    val cursoUiState by cursoViewModel.uiState.collectAsState()

    // Estados para filtros
    var selectedEstado by remember { mutableStateOf<String?>(null) }
    var selectedCurso by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedPregunta by remember { mutableStateOf<Pregunta?>(null) }

    // Filtrar preguntas
    val preguntasFiltradas = remember(
        preguntasUiState.preguntas,
        selectedEstado,
        selectedCurso
    ) {
        preguntasUiState.preguntas.filter { pregunta ->
            val cumpleEstado = selectedEstado == null || pregunta.estado == selectedEstado
            val cumpleCurso = selectedCurso == null || pregunta.cursoId == selectedCurso
            cumpleEstado && cumpleCurso &&
                    (pregunta.estado == EstadoPregunta.APROBADA || pregunta.estado == EstadoPregunta.RECHAZADA)
        }
    }

    // Contar estadísticas
    val totalAprobadas = preguntasFiltradas.count { it.estado == EstadoPregunta.APROBADA }
    val totalRechazadas = preguntasFiltradas.count { it.estado == EstadoPregunta.RECHAZADA }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Preguntas Revisadas")
                        Text(
                            text = "$totalAprobadas aprobadas · $totalRechazadas rechazadas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (selectedEstado != null || selectedCurso != null)
                                EduRachaColors.Warning else Color.Transparent
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = Color.White
                            )
                        }
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
        ) {
            // Chips de filtro activo
            if (selectedEstado != null || selectedCurso != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedEstado != null) {
                        val estado = selectedEstado!!
                        FilterChip(
                            selected = true,
                            onClick = { selectedEstado = null },
                            label = {
                                Text(
                                    when(estado) {
                                        EstadoPregunta.APROBADA -> "✅ Aprobadas"
                                        EstadoPregunta.RECHAZADA -> "❌ Rechazadas"
                                        else -> estado
                                    }
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    if (selectedCurso != null) {
                        val cursoId = selectedCurso!!
                        val curso = cursoUiState.cursos.find { it.id == cursoId }
                        FilterChip(
                            selected = true,
                            onClick = { selectedCurso = null },
                            label = { Text(curso?.nombre ?: "Curso") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Lista de preguntas
            when {
                preguntasUiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EduRachaColors.Primary)
                    }
                }

                preguntasFiltradas.isEmpty() -> {
                    EmptyState(
                        hasFilters = selectedEstado != null || selectedCurso != null,
                        onClearFilters = {
                            selectedEstado = null
                            selectedCurso = null
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(preguntasFiltradas) { pregunta ->
                            PreguntaCard(
                                pregunta = pregunta,
                                cursoNombre = cursoUiState.cursos
                                    .find { it.id == pregunta.cursoId }?.nombre,
                                onClick = { selectedPregunta = pregunta }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de filtros
    if (showFilterDialog) {
        FilterDialog(
            cursos = cursoUiState.cursos,
            selectedEstado = selectedEstado,
            selectedCurso = selectedCurso,
            onEstadoSelected = { selectedEstado = it },
            onCursoSelected = { selectedCurso = it },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Diálogo de detalle
    if (selectedPregunta != null) {
        val pregunta = selectedPregunta!!
        PreguntaDetailDialog(
            pregunta = pregunta,
            cursoNombre = cursoUiState.cursos
                .find { it.id == pregunta.cursoId }?.nombre,
            onDismiss = { selectedPregunta = null }
        )
    }
}

@Composable
fun PreguntaCard(
    pregunta: Pregunta,
    cursoNombre: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con estado y curso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (pregunta.estado == EstadoPregunta.APROBADA)
                        EduRachaColors.Success.copy(alpha = 0.15f)
                    else
                        EduRachaColors.Error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (pregunta.estado == EstadoPregunta.APROBADA)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
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
                            fontSize = 12.sp,
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
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = cursoNombre,
                            fontSize = 11.sp,
                            color = EduRachaColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Texto de la pregunta
            Text(
                text = pregunta.texto,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Info adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PreguntaInfoChip(
                    icon = Icons.Outlined.ListAlt,
                    text = "${pregunta.opciones.size} opciones"
                )

                if (pregunta.dificultad != null) {
                    PreguntaInfoChip(
                        icon = Icons.Outlined.Speed,
                        text = pregunta.dificultad.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    )
                }

                if (pregunta.fuente.isNotEmpty()) {
                    PreguntaInfoChip(
                        icon = if (pregunta.fuente == "ia") Icons.Outlined.AutoAwesome else Icons.Outlined.Person,
                        text = if (pregunta.fuente == "ia") "IA" else pregunta.fuente.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    )
                }
            }

            // Notas de revisión (si existen)
            if (pregunta.notasRevision != null && pregunta.notasRevision.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = EduRachaColors.Warning
                        )
                        Text(
                            text = pregunta.notasRevision,
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreguntaInfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = EduRachaColors.TextSecondary
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun EmptyState(hasFilters: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasFilters) Icons.Outlined.FilterAltOff else Icons.Outlined.QuestionAnswer,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFilters)
                "No hay preguntas con estos filtros"
            else
                "No hay preguntas revisadas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilters)
                "Intenta cambiar los filtros"
            else
                "Las preguntas aprobadas o rechazadas aparecerán aquí",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary.copy(alpha = 0.7f)
        )

        if (hasFilters) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                )
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar filtros")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar preguntas") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filtro por estado
                Text(
                    text = "Estado",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEstado == EstadoPregunta.APROBADA,
                        onClick = {
                            onEstadoSelected(
                                if (selectedEstado == EstadoPregunta.APROBADA) null
                                else EstadoPregunta.APROBADA
                            )
                        },
                        label = { Text("✅ Aprobadas") }
                    )

                    FilterChip(
                        selected = selectedEstado == EstadoPregunta.RECHAZADA,
                        onClick = {
                            onEstadoSelected(
                                if (selectedEstado == EstadoPregunta.RECHAZADA) null
                                else EstadoPregunta.RECHAZADA
                            )
                        },
                        label = { Text("❌ Rechazadas") }
                    )
                }

                // Filtro por curso
                if (cursos.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Curso",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        cursos.take(5).forEach { curso ->
                            FilterChip(
                                selected = selectedCurso == curso.id,
                                onClick = {
                                    onCursoSelected(
                                        if (selectedCurso == curso.id) null else curso.id
                                    )
                                },
                                label = { Text(curso.nombre) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun PreguntaDetailDialog(
    pregunta: Pregunta,
    cursoNombre: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detalle de la pregunta")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (pregunta.estado == EstadoPregunta.APROBADA)
                        EduRachaColors.Success.copy(alpha = 0.15f)
                    else
                        EduRachaColors.Error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (pregunta.estado == EstadoPregunta.APROBADA) "✅ Aprobada" else "❌ Rechazada",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pregunta.estado == EstadoPregunta.APROBADA)
                            EduRachaColors.Success
                        else
                            EduRachaColors.Error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información del curso
                if (cursoNombre != null) {
                    DetailSection(
                        title = "Curso",
                        content = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = EduRachaColors.Primary
                                    )
                                    Text(
                                        text = cursoNombre,
                                        fontSize = 14.sp,
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
                    content = {
                        Text(
                            text = pregunta.texto,
                            fontSize = 15.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                )

                // Opciones
                DetailSection(
                    title = "Opciones de respuesta",
                    content = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pregunta.opciones.forEach { opcion ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (opcion.esCorrecta)
                                        EduRachaColors.Success.copy(alpha = 0.1f)
                                    else
                                        Color.Gray.copy(alpha = 0.05f),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = if (opcion.esCorrecta)
                                        BorderStroke(2.dp, EduRachaColors.Success)
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (opcion.esCorrecta)
                                                        EduRachaColors.Success
                                                    else
                                                        Color.Gray.copy(alpha = 0.3f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (opcion.esCorrecta) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Correcta",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.White
                                                )
                                            } else {
                                                Text(
                                                    text = "${opcion.id + 1}",
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = opcion.texto,
                                            fontSize = 14.sp,
                                            color = if (opcion.esCorrecta)
                                                EduRachaColors.Success
                                            else
                                                EduRachaColors.TextPrimary,
                                            fontWeight = if (opcion.esCorrecta)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
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
                        content = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = EduRachaColors.Primary
                                    )
                                    Text(
                                        text = pregunta.explicacionCorrecta,
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    )
                }

                // Metadatos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pregunta.dificultad != null) {
                        MetadataChip(
                            icon = Icons.Outlined.Speed,
                            label = "Dificultad",
                            value = pregunta.dificultad.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }
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
                        }
                    )
                }

                // Notas de revisión
                if (pregunta.notasRevision != null && pregunta.notasRevision.isNotEmpty()) {
                    DetailSection(
                        title = "Notas de revisión",
                        content = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduRachaColors.Warning.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = EduRachaColors.Warning
                                    )
                                    Text(
                                        text = pregunta.notasRevision,
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = 20.sp
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pregunta.revisadoPor != null) {
                            DetailInfoRow(
                                icon = Icons.Outlined.Person,
                                label = "Revisado por",
                                value = pregunta.revisadoPor
                            )
                        }
                        if (pregunta.fechaRevision != null) {
                            DetailInfoRow(
                                icon = Icons.Outlined.CalendarMonth,
                                label = "Fecha de revisión",
                                value = pregunta.fechaRevision
                            )
                        }
                    }
                }

                // Información de creación
                HorizontalDivider()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pregunta.creadoPor.isNotEmpty()) {
                        DetailInfoRow(
                            icon = Icons.Outlined.PersonAdd,
                            label = "Creado por",
                            value = pregunta.creadoPor
                        )
                    }
                    if (pregunta.fechaCreacion.isNotEmpty()) {
                        DetailInfoRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Fecha de creación",
                            value = pregunta.fechaCreacion
                        )
                    }
                    if (pregunta.modificada) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduRachaColors.Warning.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = EduRachaColors.Warning
                                )
                                Text(
                                    text = "Esta pregunta ha sido modificada",
                                    fontSize = 12.sp,
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
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
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
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = EduRachaColors.Primary.copy(alpha = 0.08f),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = EduRachaColors.Primary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = EduRachaColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = 12.sp,
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
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = EduRachaColors.TextSecondary
        )
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = EduRachaColors.TextPrimary
        )
    }
}