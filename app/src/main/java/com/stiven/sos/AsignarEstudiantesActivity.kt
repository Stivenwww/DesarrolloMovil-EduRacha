package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme

class AsignarEstudiantesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val curso = intent.getStringExtra("CURSO") ?: "Curso"

        setContent {
            EduRachaTheme {
                AsignarEstudiantesScreen(
                    curso = curso,
                    onNavigateBack = {
                        val intent = Intent(this, GestionGruposActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                    },
                    onAsignacionExitosa = {
                        val intent = Intent(this, VisualizarGruposActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("CURSO", curso)
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
fun AsignarEstudiantesScreen(
    curso: String,
    onNavigateBack: () -> Unit,
    onAsignacionExitosa: () -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    val todosEstudiantes = remember { Estudiante.obtenerEstudiantesEjemplo() }
    val estudiantesYaAsignados = remember {
        GruposRepository.obtenerEstudiantesPorCurso(curso)
    }
    val estudiantesSeleccionados = remember { mutableStateListOf<Estudiante>() }
    var showDialog by remember { mutableStateOf(false) }

    val estudiantesFiltrados = remember(searchQuery, todosEstudiantes) {
        todosEstudiantes.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) ||
                    it.apellido.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    fun asignarEstudiantes() {
        if (estudiantesSeleccionados.isEmpty()) {
            Toast.makeText(context, "Selecciona al menos un estudiante", Toast.LENGTH_SHORT).show()
        } else {
            GruposRepository.asignarEstudiantes(curso, estudiantesSeleccionados)
            showDialog = true
        }
    }

    Scaffold(
        containerColor = EduRachaColors.Background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = estudiantesSeleccionados.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { asignarEstudiantes() },
                    containerColor = EduRachaColors.Success,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Asignar",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Asignar (${estudiantesSeleccionados.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AsignarEstudiantesHeader(
                curso = curso,
                onNavigateBack = onNavigateBack
            )

            Column(modifier = Modifier.fillMaxSize()) {
                EstadisticasCard(
                    totalEstudiantes = todosEstudiantes.size,
                    seleccionados = estudiantesSeleccionados.size,
                    modifier = Modifier.padding(16.dp)
                )

                SearchBarAsignacion(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = "Lista de Estudiantes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = estudiantesFiltrados,
                        key = { it.id }
                    ) { estudiante ->
                        val yaAsignado = estudiante in estudiantesYaAsignados
                        val isSelected = estudiante in estudiantesSeleccionados

                        EstudianteCheckItem(
                            estudiante = estudiante,
                            isSelected = isSelected,
                            yaAsignado = yaAsignado,
                            onCheckedChange = { checked ->
                                if (checked && !yaAsignado) {
                                    estudiantesSeleccionados.add(estudiante)
                                } else {
                                    estudiantesSeleccionados.remove(estudiante)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        DialogoExito(
            curso = curso,
            cantidadAsignados = estudiantesSeleccionados.size,
            onDismiss = {
                showDialog = false
                onNavigateBack()
            },
            onVisualizar = {
                showDialog = false
                onAsignacionExitosa()
            }
        )
    }
}

@Composable
fun AsignarEstudiantesHeader(curso: String, onNavigateBack: () -> Unit) {
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
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }

                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Asignación", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Asignar Estudiantes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(curso, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun EstadisticasCard(totalEstudiantes: Int, seleccionados: Int, modifier: Modifier = Modifier) {
    val porcentaje = if (totalEstudiantes > 0) (seleccionados.toFloat() / totalEstudiantes.toFloat()) else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.People, null, tint = EduRachaColors.Primary, modifier = Modifier.size(28.dp))
                Text("Selección de Estudiantes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
            }
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = EduRachaColors.Primary.copy(alpha = 0.05f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, null, tint = EduRachaColors.Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(totalEstudiantes.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Primary)
                        Text("Total Estudiantes", fontSize = 11.sp, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                    Divider(modifier = Modifier.width(2.dp).height(80.dp), color = EduRachaColors.SurfaceVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = EduRachaColors.Success, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(seleccionados.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Success)
                        Text("Seleccionados", fontSize = 11.sp, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progreso de selección", fontSize = 12.sp, color = EduRachaColors.TextSecondary)
                    Text("${(porcentaje * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = EduRachaColors.Success,
                    trackColor = EduRachaColors.SurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarAsignacion(searchQuery: String, onSearchQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar estudiante...", color = EduRachaColors.TextSecondary, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EduRachaColors.TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Limpiar", tint = EduRachaColors.TextSecondary)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = EduRachaColors.TextPrimary),
            singleLine = true
        )
    }
}

@Composable
fun EstudianteCheckItem(estudiante: Estudiante, isSelected: Boolean, yaAsignado: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val borderColor = when {
        yaAsignado -> EduRachaColors.Info
        isSelected -> EduRachaColors.Success
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (isSelected || yaAsignado) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(enabled = !yaAsignado, onClick = { onCheckedChange(!isSelected) }),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (yaAsignado) EduRachaColors.Info.copy(alpha = 0.05f) else Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (yaAsignado) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = EduRachaColors.Info.copy(alpha = 0.2f)) {
                    Icon(Icons.Default.Check, "Ya asignado", tint = EduRachaColors.Info, modifier = Modifier.fillMaxSize().padding(8.dp))
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = EduRachaColors.Success, uncheckedColor = EduRachaColors.TextSecondary)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(EduRachaColors.Primary, EduRachaColors.Primary.copy(alpha = 0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Text("${estudiante.nombre.first()}${estudiante.apellido.first()}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${estudiante.nombre} ${estudiante.apellido}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EduRachaColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Email, null, tint = EduRachaColors.TextSecondary, modifier = Modifier.size(14.dp))
                    Text(estudiante.email, fontSize = 12.sp, color = EduRachaColors.TextSecondary)
                }
                if (yaAsignado) {
                    Text("Ya asignado", fontSize = 11.sp, color = EduRachaColors.Info, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Surface(shape = CircleShape, color = getRankingColor(estudiante.posicionRanking)) {
                Text("#${estudiante.posicionRanking}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun DialogoExito(curso: String, cantidadAsignados: Int, onDismiss: () -> Unit, onVisualizar: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Default.CheckCircle, null, tint = EduRachaColors.Success, modifier = Modifier.size(48.dp)) },
        title = { Text("¡Éxito!", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) },
        text = { Text("$cantidadAsignados estudiantes han sido asignados correctamente a '$curso'", textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = onVisualizar, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary)) {
                Text("Visualizar Grupos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", color = EduRachaColors.TextSecondary)
            }
        }
    )
}

fun getRankingColor(posicion: Int): Color {
    return when {
        posicion <= 3 -> EduRachaColors.RankingGold
        posicion <= 10 -> EduRachaColors.RankingSilver
        else -> EduRachaColors.RankingBronze
    }
}