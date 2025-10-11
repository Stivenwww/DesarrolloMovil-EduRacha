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
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import kotlinx.coroutines.delay

// ============================================
// ACTIVITY
// ============================================
class GestionGruposActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        setContent {
            EduRachaTheme {
                GestionGruposScreen(
                    onNavigateBack = {
                        val intent = Intent(this, PanelDocenteActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                    },
                    onCursoClick = { curso ->
                        val intent = Intent(this, AsignarEstudiantesActivity::class.java)
                        intent.putExtra("CURSO_TITULO", curso.titulo)
                        intent.putExtra("CURSO_CODIGO", curso.codigo)
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

// ============================================
// SCREEN PRINCIPAL
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionGruposScreen(
    onNavigateBack: () -> Unit,
    onCursoClick: (Curso) -> Unit,
    onCrearCurso: () -> Unit
) {
    // CORRECCIÓN: Usar CrearCursoObject para acceder a la lista de cursos guardados
    val cursos by remember(CrearCursoObject.cursosGuardados.size) {
        mutableStateOf(CrearCursoObject.cursosGuardados.toList())
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GestionGruposHeader(onNavigateBack = onNavigateBack)

            if (cursos.isEmpty()) {
                EmptyStateGrupos(onCrearCurso = onCrearCurso)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    EstadisticasGruposCard(
                        totalCursos = cursos.size,
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
                            key = { it.codigo } // Usar código único
                        ) { curso ->
                            CursoGrupoCard(
                                curso = curso,
                                onClick = { onCursoClick(curso) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// HEADER
// ============================================
@Composable
fun GestionGruposHeader(onNavigateBack: () -> Unit) {
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
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gestión de Grupos",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Organiza estudiantes por curso",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ============================================
// ESTADÍSTICAS CARD
// ============================================
@Composable
fun EstadisticasGruposCard(
    totalCursos: Int,
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
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(48.dp)
            )

            StatItemGrupos(
                icon = Icons.Default.School,
                value = totalCursos.toString(),
                label = "Total Cursos",
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

// ============================================
// SECTION HEADER
// ============================================
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

// ============================================
// CURSO CARD
// ============================================
@Composable
fun CursoGrupoCard(
    curso: Curso,
    onClick: () -> Unit
) {
    // Contar estudiantes asignados (si tienes un repositorio)
    val estudiantesAsignados = remember(curso.codigo) {
        // TODO: Implementar conteo real desde tu repositorio
        // GruposRepository.obtenerEstudiantesPorCurso(curso.codigo).size
        0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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

            // Información del curso
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = curso.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Código del curso
                Text(
                    text = "Código: ${curso.codigo}",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Duración y estado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${curso.duracionDias} días",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }

                    // Estado
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (curso.estado.lowercase()) {
                            "activo" -> EduRachaColors.Success.copy(alpha = 0.15f)
                            "inactivo" -> EduRachaColors.Error.copy(alpha = 0.15f)
                            else -> EduRachaColors.TextSecondary.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = curso.estado.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (curso.estado.lowercase()) {
                                "activo" -> EduRachaColors.Success
                                "inactivo" -> EduRachaColors.Error
                                else -> EduRachaColors.TextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Estudiantes asignados
                if (estudiantesAsignados > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$estudiantesAsignados estudiantes",
                            fontSize = 12.sp,
                            color = EduRachaColors.Accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Icono de navegación
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver más",
                tint = EduRachaColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// EMPTY STATE
// ============================================
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