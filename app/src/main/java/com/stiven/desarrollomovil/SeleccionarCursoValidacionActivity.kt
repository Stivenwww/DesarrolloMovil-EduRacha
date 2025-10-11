package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.models.Curso
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

// ============================================
// ACTIVITY PARA SELECCIONAR CURSO
// ============================================
class SeleccionarCursoValidacionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                SeleccionarCursoValidacionScreen(
                    onNavigateBack = { finish() },
                    onCursoSelected = { cursoTitulo ->
                        abrirValidacionPreguntas(cursoTitulo)
                    }
                )
            }
        }
    }

    private fun abrirValidacionPreguntas(cursoTitulo: String) {
        val intent = Intent(this, ValidacionPreguntasActivity::class.java).apply {
            putExtra("CURSO_TITULO", cursoTitulo)
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
    onNavigateBack: () -> Unit,
    onCursoSelected: (String) -> Unit
) {
    // Obtener cursos con preguntas pendientes
    val cursosConPendientes = remember {
        PreguntasIARepository.obtenerCursosConPreguntasPendientes()
    }

    // Calcular estadísticas por curso
    val estadisticasPorCurso = remember(cursosConPendientes) {
        cursosConPendientes.associateWith { curso ->
            val pendientes = PreguntasIARepository.obtenerPreguntasPendientes(curso.titulo).size
            val total = PreguntasIARepository.obtenerTodasLasPreguntas(curso.titulo).size
            Pair(pendientes, total)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp),
                color = EduRachaColors.Primary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.Primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Validar Preguntas IA",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Selecciona un curso",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            Surface(
                                color = EduRachaColors.Secondary,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${estadisticasPorCurso.values.sumOf { it.first }} total",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cursosConPendientes.isEmpty()) {
                EmptyStateValidacion(onNavigateBack = onNavigateBack)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header informativo
                    item {
                        InfoCard(
                            totalCursos = cursosConPendientes.size,
                            totalPendientes = estadisticasPorCurso.values.sumOf { it.first }
                        )
                    }

                    // Título de sección
                    item {
                        SectionHeaderValidacion()
                    }

                    // Lista de cursos
                    items(cursosConPendientes, key = { it.codigo }) { curso ->
                        val (pendientes, total) = estadisticasPorCurso[curso] ?: Pair(0, 0)

                        CursoValidacionCard(
                            curso = curso,
                            preguntasPendientes = pendientes,
                            preguntasTotales = total,
                            onClick = { onCursoSelected(curso.titulo) }
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

// ============================================
// CARD INFORMATIVA
// ============================================
@Composable
private fun InfoCard(
    totalCursos: Int,
    totalPendientes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = EduRachaColors.Warning.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Preguntas por Validar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "$totalCursos ${if (totalCursos == 1) "curso" else "cursos"} con preguntas pendientes",
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

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selecciona un curso para revisar sus preguntas generadas por IA",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ============================================
// HEADER DE SECCIÓN
// ============================================
@Composable
private fun SectionHeaderValidacion() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(EduRachaColors.Warning, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "CURSOS CON PREGUNTAS PENDIENTES",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Warning,
            letterSpacing = 1.sp
        )
    }
}

// ============================================
// CARD DE CURSO
// ============================================
@Composable
fun CursoValidacionCard(
    curso: Curso,
    preguntasPendientes: Int,
    preguntasTotales: Int,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del curso
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EduRachaColors.Primary.copy(alpha = 0.2f),
                                EduRachaColors.Primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del curso
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = curso.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge de pendientes
                    Surface(
                        color = EduRachaColors.Warning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$preguntasPendientes pendientes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = EduRachaColors.Warning
                            )
                        }
                    }

                    // Total de preguntas
                    Text(
                        text = "de $preguntasTotales total",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                // Barra de progreso
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (preguntasTotales > 0) {
                        (preguntasTotales - preguntasPendientes).toFloat() / preguntasTotales
                    } else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EduRachaColors.Success,
                    trackColor = EduRachaColors.Background
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Flecha indicadora
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir a validación",
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ============================================
// EMPTY STATE
// ============================================
@Composable
fun EmptyStateValidacion(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(EduRachaColors.Success.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EduRachaColors.Success,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Todo al día!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )

        Text(
            text = "No hay preguntas pendientes de validación en ningún curso",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = EduRachaColors.Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text("Volver", fontWeight = FontWeight.Bold)
        }
    }
}