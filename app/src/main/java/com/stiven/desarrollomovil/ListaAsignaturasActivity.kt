package com.stiven.desarrollomovil

import android.net.Uri
import coil.compose.AsyncImage
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

class ListaAsignaturasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Uniautonoma.Primary)) {
                ListaAsignaturasScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaAsignaturasScreen(
    onNavigateBack: () -> Unit
) {
    val asignaturas = remember { CrearAsignatura.asignaturasGuardadas }
    var selectedAsignatura by remember { mutableStateOf<Asignatura?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val asignaturasFiltradas = remember(searchQuery, selectedFilter) {
        asignaturas.filter { asignatura ->
            val matchesSearch = asignatura.nombre.contains(searchQuery, ignoreCase = true) ||
                    asignatura.codigo.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || asignatura.modalidad == selectedFilter
            matchesSearch && matchesFilter
        }
    }

    val modalidades = remember {
        asignaturas.map { it.modalidad }.distinct()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Uniautonoma.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con gradiente y estadísticas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Uniautonoma.Primary,
                                Uniautonoma.PrimaryLight
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
                                text = "Mis Asignaturas",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (asignaturas.isNotEmpty()) {
                                Text(
                                    text = "${asignaturas.size} ${if (asignaturas.size == 1) "asignatura" else "asignaturas"}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        // Badge con número total
                        if (asignaturas.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${asignaturas.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Barra de búsqueda
                    if (asignaturas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            placeholder = {
                                Text(
                                    "Buscar asignaturas...",
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

            // Filtros por modalidad
            if (asignaturas.isNotEmpty() && modalidades.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Uniautonoma.Primary,
                            selectedLabelColor = Color.White
                        )
                    )

                    modalidades.forEach { modalidad ->
                        FilterChip(
                            selected = selectedFilter == modalidad,
                            onClick = {
                                selectedFilter = if (selectedFilter == modalidad) null else modalidad
                            },
                            label = { Text(modalidad) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = obtenerColorModalidad(modalidad),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Contenido principal
            if (asignaturas.isEmpty()) {
                EmptyAsignaturasState()
            } else if (asignaturasFiltradas.isEmpty()) {
                EmptySearchState(onClearSearch = { searchQuery = "" })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = asignaturasFiltradas,
                        key = { _, asignatura -> asignatura.codigo }
                    ) { index, asignatura ->
                        AnimatedAsignaturaCard(
                            asignatura = asignatura,
                            index = index,
                            onClick = { selectedAsignatura = asignatura }
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
    if (selectedAsignatura != null) {
        AsignaturaDetailDialog(
            asignatura = selectedAsignatura!!,
            onDismiss = { selectedAsignatura = null }
        )
    }
}

@Composable
fun AnimatedAsignaturaCard(
    asignatura: Asignatura,
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
                // Header con color de modalidad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(obtenerColorModalidad(asignatura.modalidad))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de asignatura
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(obtenerColorModalidad(asignatura.modalidad).copy(alpha = 0.1f))
                            .border(
                                2.dp,
                                obtenerColorModalidad(asignatura.modalidad).copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (asignatura.imagenUrl.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(asignatura.imagenUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = obtenerIconoAsignatura(asignatura.modalidad),
                                contentDescription = null,
                                tint = obtenerColorModalidad(asignatura.modalidad),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Información
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asignatura.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Uniautonoma.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = asignatura.codigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = obtenerColorModalidad(asignatura.modalidad)
                            )

                            Text(
                                text = " • ",
                                fontSize = 14.sp,
                                color = Uniautonoma.TextSecondary
                            )

                            Text(
                                text = "Sem. ${asignatura.semestre}",
                                fontSize = 14.sp,
                                color = Uniautonoma.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chip de modalidad
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = obtenerColorModalidad(asignatura.modalidad).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = obtenerIconoModalidad(asignatura.modalidad),
                                    contentDescription = null,
                                    tint = obtenerColorModalidad(asignatura.modalidad),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = asignatura.modalidad,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = obtenerColorModalidad(asignatura.modalidad)
                                )
                            }
                        }
                    }

                    // Flecha
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Uniautonoma.TextSecondary
                    )
                }

                // Preview del plan de aula
                if (asignatura.planAula.isNotEmpty()) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Uniautonoma.Background
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
                            tint = Uniautonoma.TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (asignatura.planAula.length > 120) {
                                asignatura.planAula.substring(0, 120) + "..."
                            } else {
                                asignatura.planAula
                            },
                            fontSize = 13.sp,
                            color = Uniautonoma.TextSecondary,
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
fun AsignaturaDetailDialog(
    asignatura: Asignatura,
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
                        imageVector = obtenerIconoAsignatura(asignatura.modalidad),
                        contentDescription = null,
                        tint = obtenerColorModalidad(asignatura.modalidad),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = asignatura.nombre,
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
                    value = asignatura.codigo,
                    icon = Icons.Outlined.Tag,
                    color = Uniautonoma.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Semestre",
                    value = "${asignatura.semestre}°",
                    icon = Icons.Outlined.CalendarMonth,
                    color = Uniautonoma.Accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Modalidad",
                    value = asignatura.modalidad,
                    icon = obtenerIconoModalidad(asignatura.modalidad),
                    color = obtenerColorModalidad(asignatura.modalidad)
                )

                if (asignatura.planAula.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Uniautonoma.Background)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Uniautonoma.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Plan de Aula",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Uniautonoma.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = asignatura.planAula,
                                fontSize = 13.sp,
                                color = Uniautonoma.TextSecondary,
                                lineHeight = 18.sp
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
                    containerColor = obtenerColorModalidad(asignatura.modalidad)
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
                color = Uniautonoma.TextSecondary
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Uniautonoma.TextPrimary
            )
        }
    }
}

@Composable
fun EmptyAsignaturasState() {
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
                .background(Uniautonoma.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = Uniautonoma.Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No tienes asignaturas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Uniautonoma.TextPrimary
        )

        Text(
            text = "Crea tu primera asignatura para comenzar",
            fontSize = 14.sp,
            color = Uniautonoma.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Uniautonoma.Accent.copy(alpha = 0.1f)
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
                    tint = Uniautonoma.Accent,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Las asignaturas te ayudan a organizar tu contenido académico",
                    fontSize = 13.sp,
                    color = Uniautonoma.TextSecondary,
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
            tint = Uniautonoma.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No se encontraron resultados",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Uniautonoma.TextPrimary
        )

        Text(
            text = "Intenta con otros términos de búsqueda",
            fontSize = 14.sp,
            color = Uniautonoma.TextSecondary,
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
fun obtenerColorModalidad(modalidad: String): Color {
    return when (modalidad) {
        "Presencial" -> Uniautonoma.Primary
        "Virtual" -> Uniautonoma.Accent
        "Híbrida" -> Uniautonoma.Secondary
        "Remota" -> Uniautonoma.Success
        else -> Uniautonoma.TextSecondary
    }
}

fun obtenerIconoModalidad(modalidad: String): ImageVector {
    return when (modalidad) {
        "Presencial" -> Icons.Outlined.School
        "Virtual" -> Icons.Outlined.Computer
        "Híbrida" -> Icons.Outlined.DevicesOther
        "Remota" -> Icons.Outlined.Cloud
        else -> Icons.Outlined.Class
    }
}

fun obtenerIconoAsignatura(modalidad: String): ImageVector {
    return when (modalidad) {
        "Presencial" -> Icons.Outlined.MenuBook
        "Virtual" -> Icons.Outlined.Computer
        "Híbrida" -> Icons.Outlined.AutoStories
        "Remota" -> Icons.Outlined.CloudQueue
        else -> Icons.Outlined.Book
    }
}
