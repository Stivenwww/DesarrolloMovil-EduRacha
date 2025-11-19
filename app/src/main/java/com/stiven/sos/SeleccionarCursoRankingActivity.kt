package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import java.text.SimpleDateFormat
import java.util.*

class SeleccionarCursoRankingActivity : ComponentActivity() {
    private val viewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                SeleccionarCursoRankingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onCursoSelected = { curso ->
                        val intent = Intent(this, RankingDetalleActivity::class.java).apply {
                            putExtra("CURSO_ID", curso.id)
                            putExtra("CURSO_TITULO", curso.titulo)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarCursoRankingScreen(
    viewModel: CursoViewModel,
    onNavigateBack: () -> Unit,
    onCursoSelected: (Curso) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = EduRachaColors.Primary,
                                strokeWidth = 4.dp
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Cargando cursos...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            "Obteniendo información de rankings",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                uiState.error != null -> {
                    ErrorSeleccionCursoView(
                        error = uiState.error!!,
                        onRetry = { viewModel.obtenerCursos() },
                        onNavigateBack = onNavigateBack
                    )
                }

                uiState.cursos.isEmpty() -> {
                    SinCursosDisponiblesView(onNavigateBack = onNavigateBack)
                }

                else -> {
                    ListadoCursosRanking(
                        cursos = uiState.cursos,
                        onCursoClick = onCursoSelected,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
fun ListadoCursosRanking(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit,
    onNavigateBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Header con gradiente institucional
        item {
            Box(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Botón de retroceso
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Título principal
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Rankings de Cursos",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Consulta el desempeño por curso",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Badge de fecha
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Encabezado con contador
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EduRachaColors.Primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${cursos.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Cursos disponibles",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            "Selecciona uno para ver su ranking",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.Leaderboard,
                        null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }

        // Lista de cursos
        items(cursos) { curso ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TarjetaCursoRanking(
                    curso = curso,
                    onClick = { onCursoClick(curso) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TarjetaCursoRanking(
    curso: Curso,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono con gradiente
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.Primary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Badge de código
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = curso.codigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = curso.titulo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = EduRachaColors.Background, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Botón de acción
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = EduRachaColors.Success.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Success.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ver Ranking Completo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Success
                        )
                        Text(
                            text = "Consulta el desempeño de estudiantes",
                            fontSize = 11.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        null,
                        tint = EduRachaColors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SinCursosDisponiblesView(onNavigateBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Header con gradiente institucional
        item {
            Box(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Botón de retroceso
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Título principal
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Rankings de Cursos",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Consulta el desempeño por curso",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Badge de fecha
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(60.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = EduRachaColors.Primary
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "No hay cursos disponibles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Aún no se han creado cursos en la plataforma",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(32.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Los cursos aparecerán aquí una vez sean creados por los docentes",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorSeleccionCursoView(error: String, onRetry: () -> Unit, onNavigateBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Header con gradiente institucional
        item {
            Box(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Botón de retroceso
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Título principal
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Rankings de Cursos",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Consulta el desempeño por curso",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Badge de fecha
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(60.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = EduRachaColors.Error
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Error al cargar cursos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Reintentar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}