package com.stiven.desarrollomovil

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

class ListaCursosActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EduRachaTheme {
                ListaCursosScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCursosScreen(
    onNavigateBack: () -> Unit
) {
    // CORRECCIÓN: Usar CrearCursoObject en lugar de CrearCurso
    val cursos = remember { CrearCursoObject.cursosGuardados }
    var selectedCurso by remember { mutableStateOf<Curso?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val cursosFiltrados = remember(searchQuery, selectedFilter) {
        cursos.filter { curso ->
            val matchesSearch = curso.titulo.contains(searchQuery, ignoreCase = true) ||
                    curso.codigo.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || curso.estado == selectedFilter
            matchesSearch && matchesFilter
        }
    }

    val estados = remember {
        cursos.map { it.estado }.distinct()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Primary,
                                EduRachaColors.PrimaryLight
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
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Mis Cursos",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (cursos.isNotEmpty()) {
                                Text(
                                    text = "${cursos.size} ${if (cursos.size == 1) "curso" else "cursos"}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        // Badge con número total
                        if (cursos.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${cursos.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Barra de búsqueda
                    if (cursos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            placeholder = {
                                Text(
                                    "Buscar cursos...",
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color.White
                            )
                        )
                    }
                }
            }

            // Filtros por estado
            if (cursos.isNotEmpty() && estados.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EduRachaColors.Primary,
                            selectedLabelColor = Color.White
                        )
                    )

                    estados.forEach { estado ->
                        FilterChip(
                            selected = selectedFilter == estado,
                            onClick = {
                                selectedFilter = if (selectedFilter == estado) null else estado
                            },
                            label = { Text(estado.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = obtenerColorEstado(estado),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Contenido principal
            if (cursos.isEmpty()) {
                EmptyCursosState()
            } else if (cursosFiltrados.isEmpty()) {
                EmptySearchState(onClearSearch = { searchQuery = "" })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = cursosFiltrados,
                        key = { _, curso -> curso.id ?: curso.codigo }
                    ) { index, curso ->
                        AnimatedCursoCard(
                            curso = curso,
                            index = index,
                            onClick = { selectedCurso = curso }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Diálogo de detalles
    if (selectedCurso != null) {
        CursoDetailDialog(
            curso = selectedCurso!!,
            onDismiss = { selectedCurso = null }
        )
    }
}

@Composable
fun AnimatedCursoCard(
    curso: Curso,
    index: Int,
    onClick: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header con color de estado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(obtenerColorEstado(curso.estado))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono del curso
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(obtenerColorEstado(curso.estado).copy(alpha = 0.1f))
                            .border(
                                2.dp,
                                obtenerColorEstado(curso.estado).copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = obtenerColorEstado(curso.estado),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Información
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = curso.titulo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = curso.codigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = obtenerColorEstado(curso.estado)
                            )

                            Text(
                                text = " • ",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )

                            Text(
                                text = "${curso.duracionDias} días",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chip de estado
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = obtenerColorEstado(curso.estado).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = obtenerIconoEstado(curso.estado),
                                    contentDescription = null,
                                    tint = obtenerColorEstado(curso.estado),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = curso.estado.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = obtenerColorEstado(curso.estado)
                                )
                            }
                        }
                    }

                    // Flecha
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = EduRachaColors.TextSecondary
                    )
                }

                // Preview de la descripción
                if (curso.descripcion.isNotEmpty()) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = EduRachaColors.Background
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (curso.descripcion.length > 120) {
                                curso.descripcion.substring(0, 120) + "..."
                            } else {
                                curso.descripcion
                            },
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CursoDetailDialog(
    curso: Curso,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = obtenerColorEstado(curso.estado),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = curso.titulo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DetailRow(
                    label = "Código",
                    value = curso.codigo,
                    icon = Icons.Outlined.Tag,
                    color = EduRachaColors.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Duración",
                    value = "${curso.duracionDias} días",
                    icon = Icons.Outlined.CalendarMonth,
                    color = EduRachaColors.Accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Estado",
                    value = curso.estado.replaceFirstChar { it.uppercase() },
                    icon = obtenerIconoEstado(curso.estado),
                    color = obtenerColorEstado(curso.estado)
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "ID Docente",
                    value = curso.docenteId,
                    icon = Icons.Outlined.Person,
                    color = EduRachaColors.Secondary
                )

                if (curso.descripcion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = EduRachaColors.Background)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = EduRachaColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Descripción",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = curso.descripcion,
                                fontSize = 13.sp,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Mostrar temas si existen
                if (curso.temas?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = EduRachaColors.Background)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            tint = EduRachaColors.Info,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Material adjunto",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${curso.temas!!.size} archivo(s)",
                                fontSize = 13.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = obtenerColorEstado(curso.estado)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = EduRachaColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

@Composable
fun EmptyCursosState() {
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
                .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = EduRachaColors.Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No tienes cursos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )

        Text(
            text = "Crea tu primer curso para comenzar",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = EduRachaColors.Accent.copy(alpha = 0.1f)
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
                    tint = EduRachaColors.Accent,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Los cursos te ayudan a organizar tu contenido educativo",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun EmptySearchState(
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No se encontraron resultados",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )

        Text(
            text = "Intenta con otros términos de búsqueda",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onClearSearch) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Limpiar búsqueda")
        }
    }
}

// Funciones auxiliares
fun obtenerColorEstado(estado: String): Color {
    return when (estado.lowercase()) {
        "activo" -> EduRachaColors.Success
        "inactivo" -> EduRachaColors.TextSecondary
        "borrador" -> EduRachaColors.Warning
        "archivado" -> EduRachaColors.Info
        else -> EduRachaColors.Primary
    }
}

fun obtenerIconoEstado(estado: String): ImageVector {
    return when (estado.lowercase()) {
        "activo" -> Icons.Default.CheckCircle
        "inactivo" -> Icons.Default.Cancel
        "borrador" -> Icons.Default.Edit
        "archivado" -> Icons.Default.Archive
        else -> Icons.Default.Circle
    }
}