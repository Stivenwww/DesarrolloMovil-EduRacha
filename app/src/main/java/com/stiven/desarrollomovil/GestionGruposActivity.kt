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
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import kotlinx.coroutines.delay

// ============================================
// ACTIVITY
// ============================================
class GestionGruposActivity : ComponentActivity() {

    // onResume() ya no es estrictamente necesario si el estado se maneja bien en Compose.
    // Lo mantenemos por ahora para asegurar una actualización forzada al volver.
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
                    onAsignaturaClick = { asignatura ->
                        val intent = Intent(this, AsignarEstudiantesActivity::class.java)
                        intent.putExtra("ASIGNATURA", asignatura)
                        startActivity(intent)
                    },
                    onCrearAsignatura = {
                        startActivity(Intent(this, CrearAsignatura::class.java))
                    }
                )
            }
        }
    }
}

// ============================================
// SCREEN PRINCIPAL (MIGRADO Y CORREGIDO)
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionGruposScreen(
    onNavigateBack: () -> Unit,
    onAsignaturaClick: (String) -> Unit,
    onCrearAsignatura: () -> Unit
) {
    // CORRECCIÓN CLAVE:
    // 1. Se usa el 'object CrearAsignatura' para acceder a la lista.
    // 2. Se observa el tamaño de la lista para que la UI se recomponga si cambia.
    val asignaturas by remember(CrearAsignatura.asignaturasGuardadas.size) {
        mutableStateOf(CrearAsignatura.asignaturasGuardadas.toList())
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

            // Lógica para mostrar la lista o el estado vacío, igual que en el código XML original.
            if (asignaturas.isEmpty()) {
                EmptyStateGrupos(onCrearAsignatura = onCrearAsignatura)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // La tarjeta de estadísticas ahora solo muestra el total de asignaturas.
                    EstadisticasGruposCard(
                        totalAsignaturas = asignaturas.size,
                        modifier = Modifier.padding(20.dp)
                    )

                    SectionHeaderGrupos(
                        title = "ASIGNATURAS DISPONIBLES",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )

                    // El antiguo RecyclerView ahora es una LazyColumn.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // CORRECCIÓN: Se usa el constructor de PaddingValues que especifica los 4 lados.
                        contentPadding = PaddingValues(
                            start = 20.dp,        end = 20.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // El adapter se reemplaza por el bloque 'items'.
                        items(
                            items = asignaturas,
                            key = { it.id } // Usar un ID único para cada elemento.
                        ) { asignatura ->
                            AsignaturaGrupoCard(
                                asignatura = asignatura,
                                onClick = { onAsignaturaClick(asignatura.nombre) }
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
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)) {
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
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Gestión de Grupos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Organiza estudiantes por asignatura",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ============================================
// ESTADÍSTICAS CARD (SIMPLIFICADO)
// ============================================
@Composable
fun EstadisticasGruposCard(
    totalAsignaturas: Int,
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
                value = totalAsignaturas.toString(),
                label = "Total Asignaturas",
                color = EduRachaColors.Success
            )
        }
    }
}

@Composable
fun StatItemGrupos(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
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
fun SectionHeaderGrupos(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier
            .width(4.dp)
            .height(24.dp)
            .background(EduRachaColors.Success, RoundedCornerShape(2.dp)))
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
// ASIGNATURA CARD (ADAPTADO DEL ADAPTER)
// ============================================
@Composable
fun AsignaturaGrupoCard(
    asignatura: Asignatura,
    onClick: () -> Unit
) {
    // Lógica para contar estudiantes, igual que en tu Adapter.
    // Se usa 'remember' con la clave de la asignatura para que se calcule una sola vez por item.
    val estudiantesAsignados = remember(asignatura.nombre) {
        GruposRepository.obtenerEstudiantesPorAsignatura(asignatura.nombre).size
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Text(text = asignatura.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = EduRachaColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$estudiantesAsignados estudiantes",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
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
fun EmptyStateGrupos(onCrearAsignatura: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(100); isVisible = true }
        AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
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
            text = "No hay asignaturas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Crea una asignatura primero para poder gestionar grupos de estudiantes",
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCrearAsignatura,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Crear Asignatura", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

