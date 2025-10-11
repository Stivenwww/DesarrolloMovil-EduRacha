package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

class VisualizarGruposActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EduRachaTheme {
                VisualizarGruposScreen(
                    // --- ¡ESTA ES LA LÓGICA CORREGIDA! ---
                    onNavigateBack = {
                        val intent = Intent(this, AsignarEstudiantesActivity::class.java)

                        // Simplemente inicia la actividad. No necesitamos `finish()`.
                        // La actividad actual se pausará y quedará en la pila.
                        startActivity(intent)

                        // La línea "finish()" ha sido eliminada.
                    }
                )
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizarGruposScreen(
    onNavigateBack: () -> Unit
) {
    // Aquí, GruposRepository.obtenerTodasLasAsignaturasConEstudiantes() devuelve un Map<String, List<Estudiante>>
    val grupos = remember { GruposRepository.obtenerTodasLasAsignaturasConEstudiantes() }
    val totalAsignaturas = grupos.size
    val totalEstudiantes = grupos.values.sumOf { it.size }

    var expandedAsignatura by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Uniautonoma.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Uniautonoma.Primary,
                                Uniautonoma.Accent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 24.dp)
                ) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Este onClick ahora ejecutará el Intent que definimos en la Activity.
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver a Gestión de Grupos",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Visualizar Grupos",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }


                    Spacer(modifier = Modifier.height(20.dp))

                    // Card de estadísticas
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Header con icono
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Uniautonoma.Accent.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = Uniautonoma.Accent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Grupos Organizados",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Uniautonoma.TextPrimary
                                    )
                                    Text(
                                        text = "Vista completa de estudiantes",
                                        fontSize = 14.sp,
                                        color = Uniautonoma.TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = Uniautonoma.Background
                            )

                            // Estadísticas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Total Asignaturas
                                StatVisualizarCard(
                                    value = totalAsignaturas,
                                    label = "Asignaturas",
                                    icon = Icons.Outlined.MenuBook,
                                    color = Uniautonoma.Primary,
                                    modifier = Modifier.weight(1f)
                                )

                                // Total Estudiantes
                                StatVisualizarCard(
                                    value = totalEstudiantes,
                                    label = "Estudiantes",
                                    icon = Icons.Outlined.Groups,
                                    color = Uniautonoma.Success,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(Uniautonoma.Accent, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "GRUPOS POR ASIGNATURA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Uniautonoma.Accent,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenido principal
            if (grupos.isEmpty()) {
                EmptyVisualizarGruposState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Usamos .toList() para poder usar forEachIndexed que es más simple aquí
                    grupos.entries.toList().forEachIndexed { index, (asignatura, estudiantes) ->
                        item(key = asignatura) {
                            AnimatedGrupoCard(
                                asignatura = asignatura,
                                estudiantes = estudiantes,
                                index = index,
                                isExpanded = expandedAsignatura == asignatura,
                                onExpandClick = {
                                    expandedAsignatura = if (expandedAsignatura == asignatura) null else asignatura
                                }
                            )
                        }
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
                color = Uniautonoma.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AnimatedGrupoCard(
    asignatura: String,
    estudiantes: List<Estudiante>,
    index: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(400)
        ) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 8.dp else 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header de la asignatura
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Uniautonoma.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Uniautonoma.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Información
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asignatura,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Uniautonoma.TextPrimary,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Uniautonoma.Success.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Groups,
                                    contentDescription = null,
                                    tint = Uniautonoma.Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${estudiantes.size} estudiantes",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Uniautonoma.Success
                                )
                            }
                        }
                    }

                    // Botón expandir
                    IconButton(
                        onClick = onExpandClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Uniautonoma.Accent.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Contraer" else "Expandir",
                            tint = Uniautonoma.Accent
                        )
                    }
                }

                // Lista de estudiantes expandible
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
                            color = Uniautonoma.Background
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
                                color = Uniautonoma.TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            estudiantes.forEachIndexed { estudianteIndex, estudiante ->
                                EstudianteItem(
                                    estudiante = estudiante,
                                    index = estudianteIndex + 1
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
fun EstudianteItem(
    estudiante: Estudiante,
    index: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Uniautonoma.Background.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Uniautonoma.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Uniautonoma.Accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Icono de estudiante
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Uniautonoma.TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Nombre del estudiante
            Text(
                text = "${estudiante.nombre} ${estudiante.apellido}",
                fontSize = 14.sp,
                color = Uniautonoma.TextPrimary,
                modifier = Modifier.weight(1f)
            )
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
        // Icono grande
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Uniautonoma.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = Uniautonoma.Accent.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No hay grupos formados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Uniautonoma.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Asigna estudiantes a las asignaturas para poder visualizar los grupos",
            fontSize = 14.sp,
            color = Uniautonoma.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card informativa
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Uniautonoma.Primary.copy(alpha = 0.1f)
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
                    tint = Uniautonoma.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Los grupos se crean automáticamente al asignar estudiantes desde la gestión de grupos",
                    fontSize = 13.sp,
                    color = Uniautonoma.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
