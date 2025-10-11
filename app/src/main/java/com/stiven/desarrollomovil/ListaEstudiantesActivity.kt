// CORRECCIÓN 1: Cambiar el paquete al principal para que la Activity sea encontrada.
package com.stiven.desarrollomovil

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// CORRECCIÓN 2: Importar las clases del tema desde su subpaquete.
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

// ===================================================================
// CORRECCIÓN 3: Añadir la clase Activity que faltaba en este archivo.
// Esta es la clase que PanelDocenteActivity está buscando.
// ===================================================================
class ListaEstudiantesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                // La Activity ahora llama al Composable que está más abajo en este mismo archivo.
                ListaEstudiantesScreen(
                    onNavigateBack = { finish() }, // Cierra esta activity para volver.
                    onEstudianteClick = { estudiante ->
                        Toast.makeText(this, "Clic en: ${estudiante.nombre}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// --- El resto del código es el Composable que ya tenías, ahora trabajando junto a la Activity ---

fun Estudiante.isRachaActiva(): Boolean {
    return this.rachaActual > 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEstudiantesScreen(
    onNavigateBack: () -> Unit,
    onEstudianteClick: (Estudiante) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val estudiantes = remember { Estudiante.obtenerEstudiantesEjemplo().sortedBy { it.posicionRanking } }

    val estudiantesFiltrados = remember(searchQuery, estudiantes) {
        if (searchQuery.isEmpty()) {
            estudiantes
        } else {
            estudiantes.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.apellido.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudiantes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EduRachaColors.Primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EduRachaColors.Background)
                .padding(paddingValues)
        ) {
            SearchBarCard(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier.padding(16.dp)
            )
            StatsCard(
                estudiantes = estudiantes,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (estudiantesFiltrados.isEmpty()) {
                EmptyStateView(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estudiantesFiltrados, key = { it.id }) { estudiante ->
                        EstudianteItem(
                            estudiante = estudiante,
                            onClick = { onEstudianteClick(estudiante) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar estudiante...") },
            leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

@Composable
fun StatsCard(
    estudiantes: List<Estudiante>,
    modifier: Modifier = Modifier
) {
    val total = estudiantes.size
    val activos = estudiantes.count { it.isRachaActiva() }
    val promedio = if (estudiantes.isNotEmpty()) estudiantes.map { it.puntosTotal }.average().toInt() else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(total.toString(), "Total", EduRachaColors.Primary)
            Divider(Modifier.width(1.dp).height(40.dp), color = EduRachaColors.SurfaceVariant)
            StatItem(activos.toString(), "Activos", EduRachaColors.Success)
            Divider(Modifier.width(1.dp).height(40.dp), color = EduRachaColors.SurfaceVariant)
            StatItem(promedio.toString(), "Promedio pts", EduRachaColors.Accent)
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = EduRachaColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun EstudianteItem(estudiante: Estudiante, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(getRankingColor(estudiante.posicionRanking)),
                contentAlignment = Alignment.Center
            ) {
                Text("#${estudiante.posicionRanking}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("${estudiante.nombre} ${estudiante.apellido}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = EduRachaColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(estudiante.email, fontSize = 12.sp, color = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("Racha", "${estudiante.rachaActual} 🔥", if (estudiante.isRachaActiva()) EduRachaColors.StreakFire else EduRachaColors.TextSecondary)
                    StatChip("Puntos", estudiante.puntosTotal.toString(), EduRachaColors.Accent)
                }
            }
            if (estudiante.isRachaActiva()) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(EduRachaColors.Success))
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontSize = 11.sp, color = EduRachaColors.TextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text("No se encontraron estudiantes", color = EduRachaColors.TextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}


