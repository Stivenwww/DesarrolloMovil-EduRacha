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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

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

    val titleSize = (screenWidth * 0.045f).coerceIn(16.dp, 20.dp).value.sp
    val subtitleSize = (screenWidth * 0.03f).coerceIn(11.dp, 14.dp).value.sp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Preguntas Revisadas",
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalAprobadas aprobadas · $totalRechazadas rechazadas",
                            fontSize = subtitleSize,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size((screenWidth * 0.06f).coerceIn(20.dp, 28.dp))
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
                                tint = Color.White,
                                modifier = Modifier.size((screenWidth * 0.06f).coerceIn(20.dp, 28.dp))
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
                val chipPadding = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
                val chipSpacing = (screenWidth * 0.02f).coerceIn(6.dp, 10.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(chipPadding),
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing)
                ) {
                    if (selectedEstado != null) {
                        val estado = selectedEstado!!
                        FilterChip(
                            selected = true,
                            onClick = { selectedEstado = null },
                            label = {
                                Text(
                                    when(estado) {
                                        EstadoPregunta.APROBADA -> " Aprobadas"
                                        EstadoPregunta.RECHAZADA -> " Rechazadas"
                                        else -> estado
                                    },
                                    fontSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro",
                                    modifier = Modifier.size((screenWidth * 0.045f).coerceIn(16.dp, 20.dp))
                                )
                            }
                        )
                    }

                    if (selectedCurso != null) {
                        val cursoId = selectedCurso!!
                        val curso = cursoUiState.cursos.find { it.id == cursoId }

                        // Usamos la misma lógica que en el diálogo para obtener el nombre
                        val nombreMostrado = curso?.let {
                            it.nombre.ifEmpty { it.titulo }
                        } ?: "Curso"

                        FilterChip(
                            selected = true,
                            onClick = { selectedCurso = null },
                            label = {
                                Text(
                                    nombreMostrado, // <-- SOLUCIÓN AQUÍ
                                    fontSize = (screenWidth * 0.032f).coerceIn(12.dp, 14.dp).value.sp
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro",
                                    modifier = Modifier.size((screenWidth * 0.045f).coerceIn(16.dp, 20.dp))
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
                        CircularProgressIndicator(
                            color = EduRachaColors.Primary,
                            modifier = Modifier.size((screenWidth * 0.12f).coerceIn(40.dp, 60.dp))
                        )
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
                    val listPadding = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
                    val listSpacing = (screenWidth * 0.03f).coerceIn(10.dp, 16.dp)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(listPadding),
                        verticalArrangement = Arrangement.spacedBy(listSpacing)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                        Color.Gray.copy(alpha = 0.3f)
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
                                        Color.Gray.copy(alpha = 0.05f),
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
                                                        Color.Gray.copy(alpha = 0.3f)
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