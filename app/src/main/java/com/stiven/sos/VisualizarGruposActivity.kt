// Archivo: app/src/main/java/com/stiven/desarrollomovil/VisualizarGruposActivity.kt

package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.Tema
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel

class VisualizarGruposActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // El curso seleccionado inicialmente para expandir
        val cursoIdSeleccionado = intent.getStringExtra("CURSO_ID")

        setContent {
            EduRachaTheme {
                VisualizarGruposScreen(
                    cursoIdInicial = cursoIdSeleccionado,
                    cursoViewModel = cursoViewModel,
                    onNavigateBack = {
                        val intent = Intent(this, GestionGruposActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}


@Composable
fun VisualizarGruposScreen(
    cursoIdInicial: String?,
    cursoViewModel: CursoViewModel,
    onNavigateBack: () -> Unit
) {
    //  Observar el uiState unificado ---
    val uiState by cursoViewModel.uiState.collectAsState()
    val cursos = uiState.cursos
    val isLoading = uiState.isLoading
    val error = uiState.error

    // Cargar cursos al iniciar
    LaunchedEffect(Unit) {
        cursoViewModel.obtenerCursos()
    }

    // Estado local para saber qué tarjeta de curso está expandida
    var expandedCursoId by remember { mutableStateOf(cursoIdInicial) }

    val totalCursos = cursos.size
    val totalTemas = cursos.sumOf { it.temas?.size ?: 0 }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Mostrar loading solo si no hay datos en caché
                isLoading && cursos.isEmpty() -> {
                    LoadingVisualizarGrupos()
                }
                // Mostrar error solo si no hay datos en caché
                error != null && cursos.isEmpty() -> {
                    ErrorVisualizarGrupos(
                        mensaje = error,
                        onRetry = { cursoViewModel.obtenerCursos() }
                    )
                }
                // Mostrar contenido
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        VisualizarGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = totalCursos,
                            totalTemas = totalTemas // Se muestra el total de temas en lugar de estudiantes
                        )

                        if (cursos.isEmpty()) {
                            EmptyVisualizarGruposState()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = cursos,
                                    key = { it.id!! }
                                ) { curso ->
                                    GrupoExpandibleCard(
                                        curso = curso,
                                        isExpanded = expandedCursoId == curso.id,
                                        onCardClick = {
                                            expandedCursoId = if (expandedCursoId == curso.id) null else curso.id
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualizarGruposHeader(
    onNavigateBack: () -> Unit,
    totalCursos: Int,
    totalTemas: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Contenido de Cursos", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Temas organizados por curso", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatVisualizarCard(totalCursos, "Cursos", Icons.Outlined.MenuBook, EduRachaColors.Primary, Modifier.weight(1f))
                StatVisualizarCard(totalTemas, "Temas", Icons.Outlined.Bookmark, EduRachaColors.Success, Modifier.weight(1f))
            }
        }
    }
}

//  La tarjeta ahora recibe un objeto Curso completo ---
@Composable
fun GrupoExpandibleCard(
    curso: Curso,
    isExpanded: Boolean,
    onCardClick: () -> Unit
) {
    val temas = curso.temas?.values?.toList() ?: emptyList()
    val transition = updateTransition(targetState = isExpanded, label = "expansion")

    val cardElevation by transition.animateDp(label = "elevation") { if (it) 8.dp else 4.dp }
    val rotationAngle by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono e Información del Curso
                Box(Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f)), Alignment.Center) {
                    Icon(Icons.Outlined.MenuBook, "Curso", tint = EduRachaColors.Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(curso.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = EduRachaColors.Success.copy(alpha = 0.15f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bookmark, null, tint = EduRachaColors.Success, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${temas.size} temas", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = EduRachaColors.Success)
                        }
                    }
                }
                // Botón de expansión
                IconButton(onClick = onCardClick, Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Accent.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.ExpandMore, "Expandir", tint = EduRachaColors.Accent, modifier = Modifier.rotate(rotationAngle))
                }
            }

            // --- CORRECCIÓN 3: La sección expandida ahora muestra la lista de Temas ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                Column {
                    Divider(Modifier.padding(horizontal = 20.dp), color = EduRachaColors.Background)
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Lista de Temas", style = MaterialTheme.typography.titleSmall, color = EduRachaColors.TextSecondary)
                        if (temas.isEmpty()) {
                            Text("No hay temas asignados a este curso.", style = MaterialTheme.typography.bodyMedium, color = EduRachaColors.TextSecondary)
                        } else {
                            temas.forEach { tema ->
                                TemaItem(tema = tema)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- NUEVO COMPOSABLE para mostrar un item de Tema ---
@Composable
fun TemaItem(tema: Tema) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = EduRachaColors.Background.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, EduRachaColors.Border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Bookmark, null, tint = EduRachaColors.Success, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tema.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (tema.contenido.isNotBlank()) {
                    Text(tema.contenido, style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}


// --- El resto de Composables (Loading, Error, Empty) están bien y se mantienen ---
@Composable
fun LoadingVisualizarGrupos() {
    Box(Modifier
        .fillMaxSize()
        .background(EduRachaColors.Background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = EduRachaColors.Primary, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            Text("Cargando cursos...", style = MaterialTheme.typography.bodyLarge, color = EduRachaColors.TextSecondary)
        }
    }
}

@Composable
fun ErrorVisualizarGrupos(mensaje: String, onRetry: () -> Unit) {
    Column(Modifier
        .fillMaxSize()
        .padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = EduRachaColors.Error, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Error al cargar", style = MaterialTheme.typography.headlineSmall, color = EduRachaColors.TextPrimary)
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun StatVisualizarCard(value: Int, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.1f)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Text("$value", style = MaterialTheme.typography.headlineMedium, color = color)
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun EmptyVisualizarGruposState() {
    Column(Modifier
        .fillMaxSize()
        .padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.School, null, tint = EduRachaColors.Primary.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("No hay cursos disponibles", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text("Crea un curso desde el panel del docente para poder visualizarlo aquí.", style = MaterialTheme.typography.bodyMedium, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}
