package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.PreguntaViewModel

class SeleccionarCursoValidacionActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val preguntasViewModel: PreguntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                SeleccionarCursoValidacionScreen(
                    cursoViewModel = cursoViewModel,
                    preguntasViewModel = preguntasViewModel,
                    onNavigateBack = { finish() },
                    onCursoSelected = { curso ->
                        abrirValidacionPreguntas(curso)
                    }
                )
            }
        }
    }

    private fun abrirValidacionPreguntas(curso: Curso) {
        val intent = Intent(this, ValidacionPreguntasActivity::class.java).apply {
            putExtra("CURSO_TITULO", curso.titulo)
            putExtra("CURSO_ID", curso.id)
            putExtra("CURSO_CODIGO", curso.codigo)
        }
        startActivity(intent)
    }
}

// ============================================
// PANTALLA EN JETPACK COMPOSE
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarCursoValidacionScreen(
    cursoViewModel: CursoViewModel,
    preguntasViewModel: PreguntaViewModel,
    onNavigateBack: () -> Unit,
    onCursoSelected: (Curso) -> Unit
) {
    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val preguntasUiState by preguntasViewModel.uiState.collectAsState()

    val todosLosCursos = cursoUiState.cursos
    val todasLasPreguntas = preguntasUiState.preguntas
    val isLoading = cursoUiState.isLoading || preguntasUiState.isLoading

    LaunchedEffect(Unit) {
        cursoViewModel.obtenerCursos()
        // Cargar SOLO preguntas con estado "pendiente_revision"
        preguntasViewModel.cargarPreguntas(
            cursoId = null, // Todas las preguntas
            estado = EstadoPregunta.PENDIENTE_REVISION
        )
    }

    // CORRECCIÓN: Filtrar solo las preguntas que realmente están pendientes
    val preguntasPendientes = remember(todasLasPreguntas) {
        todasLasPreguntas.filter { it.estado == EstadoPregunta.PENDIENTE_REVISION }
    }

    // Debug log para verificar
    LaunchedEffect(todasLasPreguntas) {
        android.util.Log.d("SeleccionCurso", "Total preguntas recibidas del API: ${todasLasPreguntas.size}")
        val pendientes = todasLasPreguntas.count { it.estado == EstadoPregunta.PENDIENTE_REVISION }
        android.util.Log.d("SeleccionCurso", "Preguntas realmente pendientes: $pendientes")
        todasLasPreguntas.forEach { pregunta ->
            android.util.Log.d("SeleccionCurso", "  - ID: ${pregunta.id}, Estado: ${pregunta.estado}")
        }
    }

    // Combinar datos: cursos que tienen preguntas pendientes
    val cursosConPreguntasPendientes = remember(todosLosCursos, preguntasPendientes) {
        val idsCursosConPendientes = preguntasPendientes
            .map { it.cursoId }
            .toSet()

        todosLosCursos.filter { curso -> curso.id in idsCursosConPendientes }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Validar Preguntas IA",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Selecciona un curso",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        color = EduRachaColors.Secondary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${preguntasPendientes.size} total",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading && cursosConPreguntasPendientes.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = EduRachaColors.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando cursos...", color = EduRachaColors.TextSecondary)
                    }
                }
                cursosConPreguntasPendientes.isEmpty() -> {
                    EmptyStateValidacion(onNavigateBack = onNavigateBack)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            InfoCard(
                                totalCursos = cursosConPreguntasPendientes.size,
                                totalPendientes = preguntasPendientes.size
                            )
                        }

                        item { SectionHeaderValidacion() }

                        items(cursosConPreguntasPendientes, key = { it.id ?: it.codigo }) { curso ->
                            // Contar preguntas pendientes de este curso específico
                            val pendientesEnEsteCurso = preguntasPendientes.count {
                                it.cursoId == curso.id
                            }

                            CursoValidacionCard(
                                curso = curso,
                                preguntasPendientes = pendientesEnEsteCurso,
                                onClick = { onCursoSelected(curso) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// COMPONENTES DE UI
// ============================================

@Composable
private fun InfoCard(totalCursos: Int, totalPendientes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Warning.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Warning.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = EduRachaColors.Warning,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Preguntas por Validar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    "$totalCursos ${if (totalCursos == 1) "curso" else "cursos"} con preguntas pendientes",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            Surface(
                color = EduRachaColors.Warning,
                shape = CircleShape
            ) {
                Text(
                    text = "$totalPendientes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderValidacion() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(24.dp)
                .background(EduRachaColors.Warning, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "CURSOS CON PREGUNTAS PENDIENTES",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Warning,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CursoValidacionCard(
    curso: Curso,
    preguntasPendientes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    curso.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge de pendientes
                    Surface(
                        color = EduRachaColors.WarningContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "$preguntasPendientes pendientes",
                                fontSize = 12.sp,
                                color = EduRachaColors.Warning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        "• ${curso.codigo}",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun EmptyStateValidacion(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EduRachaColors.Success,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "¡Todo al día!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            "No hay preguntas pendientes de validación.",
            textAlign = TextAlign.Center,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = EduRachaColors.Primary
            )
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Volver")
        }
    }
}