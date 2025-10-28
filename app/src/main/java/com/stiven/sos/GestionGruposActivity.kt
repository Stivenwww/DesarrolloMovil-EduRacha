package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay

class GestionGruposActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar datos
        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val docenteId = prefs.getString("user_uid", "") ?: ""

        cursoViewModel.obtenerCursos()
        if (docenteId.isNotEmpty()) {
            solicitudViewModel.cargarSolicitudesDocente(docenteId)
        }

        setContent {
            EduRachaTheme {
                GestionGruposScreen(
                    cursoViewModel = cursoViewModel,
                    solicitudViewModel = solicitudViewModel,
                    onNavigateBack = {
                        val intent = Intent(this, PanelDocenteActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                    },
                    onCursoClick = { curso ->
                        val intent = Intent(this, AsignarEstudiantesActivity::class.java)
                        intent.putExtra("CURSO_TITULO", curso.titulo)
                        intent.putExtra("CURSO_CODIGO", curso.codigo)
                        intent.putExtra("CURSO_ID", curso.id)
                        startActivity(intent)
                    },
                    onCrearCurso = {
                        startActivity(Intent(this, CrearCursoActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun GestionGruposScreen(
    cursoViewModel: CursoViewModel,
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit,
    onCursoClick: (Curso) -> Unit,
    onCrearCurso: () -> Unit
) {
    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    val cursos = cursoUiState.cursos
    val isLoading = cursoUiState.isLoading
    val errorMessage = cursoUiState.error

    // Contar solicitudes pendientes por curso
    val solicitudesPorCurso = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .groupBy { it.cursoId }
            .mapValues { it.value.size }
    }

    LaunchedEffect(Unit) {
        cursoViewModel.obtenerCursos()
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && cursos.isEmpty() -> {
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

                errorMessage != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = 0
                        )
                        ErrorStateGrupos(
                            errorMessage = errorMessage,
                            onRetry = { cursoViewModel.obtenerCursos() }
                        )
                    }
                }

                cursos.isEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = 0
                        )
                        EmptyStateGrupos(onCrearCurso = onCrearCurso)
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = cursos.size
                        )

                        EstadisticasGruposCard(
                            totalCursos = cursos.size,
                            totalTemas = cursos.sumOf { it.temas?.size ?: 0 },
                            cursosActivos = cursos.count { it.estado.equals("activo", ignoreCase = true) },
                            totalSolicitudes = solicitudesPorCurso.values.sum(),
                            modifier = Modifier.padding(20.dp)
                        )

                        SectionHeaderGrupos(
                            title = "CURSOS DISPONIBLES",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = cursos,
                                key = { it.id ?: it.codigo }
                            ) { curso ->
                                CursoGrupoCard(
                                    curso = curso,
                                    solicitudesPendientes = solicitudesPorCurso[curso.id] ?: 0,
                                    onClick = { onCursoClick(curso) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateGrupos(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = EduRachaColors.Error,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error al cargar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reintentar",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun GestionGruposHeader(
    onNavigateBack: () -> Unit,
    totalCursos: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
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
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                if (totalCursos > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EduRachaColors.Secondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "$totalCursos",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gestión de Grupos",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aspirantes y estudiantes asignados",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun EstadisticasGruposCard(
    totalCursos: Int,
    totalTemas: Int,
    cursosActivos: Int,
    totalSolicitudes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItemGrupos(
                icon = Icons.Default.School,
                value = totalCursos.toString(),
                label = "Cursos",
                color = EduRachaColors.Primary
            )

            Divider(
                modifier = Modifier
                    .height(50.dp)
                    .width(1.dp),
                color = EduRachaColors.Background
            )

            StatItemGrupos(
                icon = Icons.Default.PersonAdd,
                value = totalSolicitudes.toString(),
                label = "Aspirantes",
                color = EduRachaColors.Accent
            )

            Divider(
                modifier = Modifier
                    .height(50.dp)
                    .width(1.dp),
                color = EduRachaColors.Background
            )

            StatItemGrupos(
                icon = Icons.Default.CheckCircle,
                value = cursosActivos.toString(),
                label = "Activos",
                color = EduRachaColors.Success
            )
        }
    }
}

@Composable
fun StatItemGrupos(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SectionHeaderGrupos(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(EduRachaColors.Success, RoundedCornerShape(2.dp))
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Success,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CursoGrupoCard(
    curso: Curso,
    solicitudesPendientes: Int,
    onClick: () -> Unit
) {
    val cantidadTemas = curso.temas?.size ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = curso.titulo,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Código: ${curso.codigo}",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver más",
                    tint = EduRachaColors.TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badge de solicitudes pendientes
            if (solicitudesPendientes > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$solicitudesPendientes aspirante${if (solicitudesPendientes > 1) "s" else ""} esperando aprobación",
                            fontSize = 13.sp,
                            color = EduRachaColors.Accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateGrupos(onCrearCurso: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            isVisible = true
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn() + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = EduRachaColors.Success.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No hay cursos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Crea un curso primero para poder gestionar grupos de estudiantes",
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCrearCurso,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Crear Curso",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}