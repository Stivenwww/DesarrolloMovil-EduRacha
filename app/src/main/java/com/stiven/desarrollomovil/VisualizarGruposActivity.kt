package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

// NOTA: ASUMO QUE LAS CLASES 'Estudiante', 'GruposRepository', y 'GestionGruposActivity'
// están definidas en otras partes de tu proyecto.

class VisualizarGruposActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoSeleccionado = intent.getStringExtra("CURSO")

        setContent {
            EduRachaTheme {
                VisualizarGruposScreen(
                    cursoInicial = cursoSeleccionado,
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

    // CORRECCIÓN CLAVE: Eliminada la función onResume() que contenía recreate().
    // Esto previene el parpadeo de la pantalla.
}

// =========================================================================================
// COMPOSABLES
// =========================================================================================

@Composable
fun VisualizarGruposScreen(
    cursoInicial: String?,
    onNavigateBack: () -> Unit
) {
    // CORRECCIÓN/MEJORA: Usar 'key' con un valor que cambie cuando los datos del repositorio cambien.
    // Esto asegura que si los datos cambian fuera de esta Activity, la vista se recomponga.
    // Asumo que 'GruposRepository.obtenerTotalEstudiantes()' es una buena clave de cambio.
    val grupos = remember(GruposRepository.obtenerTotalEstudiantes()) {
        GruposRepository.obtenerTodosLosCursosConEstudiantes()
    }

    val totalCursos = grupos.size
    val totalEstudiantes = grupos.values.sumOf { it.size }

    var expandedCurso by remember { mutableStateOf(cursoInicial) }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            VisualizarGruposHeader(
                onNavigateBack = onNavigateBack,
                totalCursos = totalCursos,
                totalEstudiantes = totalEstudiantes
            )

            if (grupos.isEmpty()) {
                EmptyVisualizarGruposState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ordenar cursos alfabéticamente para consistencia
                    items(
                        items = grupos.keys.sorted(),
                        key = { it }
                    ) { curso ->
                        val estudiantes = grupos[curso] ?: emptyList()
                        GrupoExpandibleCard(
                            curso = curso,
                            estudiantes = estudiantes,
                            isExpanded = expandedCurso == curso,
                            onCardClick = {
                                expandedCurso = if (expandedCurso == curso) null else curso
                            }
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

@Composable
fun VisualizarGruposHeader(
    onNavigateBack: () -> Unit,
    totalCursos: Int,
    totalEstudiantes: Int
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
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Visualizar Grupos",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estudiantes organizados por curso",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grupos Organizados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = "Vista completa de estudiantes",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = EduRachaColors.Background
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatVisualizarCard(
                        value = totalCursos,
                        label = "Cursos",
                        icon = Icons.Outlined.MenuBook,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatVisualizarCard(
                        value = totalEstudiantes,
                        label = "Estudiantes",
                        icon = Icons.Outlined.Groups,
                        color = EduRachaColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatVisualizarCard(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    text = "$value",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun GrupoExpandibleCard(
    curso: String,
    estudiantes: List<Estudiante>,
    isExpanded: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = "Curso",
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = curso,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EduRachaColors.Success.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${estudiantes.size} estudiantes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = EduRachaColors.Success
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCardClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Accent.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Contraer" else "Expandir",
                        tint = EduRachaColors.Accent,
                        modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut()
            ) {
                Column {
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = EduRachaColors.Background
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lista de Estudiantes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (estudiantes.isEmpty()) {
                            Text(
                                text = "No hay estudiantes asignados a este curso.",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            estudiantes.forEachIndexed { index, estudiante ->
                                EstudianteItem(
                                    estudiante = estudiante,
                                    index = index + 1
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
fun EstudianteItem(estudiante: Estudiante, index: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = EduRachaColors.Background.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = EduRachaColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${estudiante.nombre} ${estudiante.apellido}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = estudiante.email,
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            Surface(
                shape = CircleShape,
                color = getRankingColor(estudiante.posicionRanking)
            ) {
                Text(
                    "#${estudiante.posicionRanking}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyVisualizarGruposState() {
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
                .background(EduRachaColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = EduRachaColors.Accent.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No hay grupos formados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Asigna estudiantes a los cursos para poder visualizar los grupos",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = EduRachaColors.Primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Los grupos se crean automáticamente al asignar estudiantes desde la gestión de grupos",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

