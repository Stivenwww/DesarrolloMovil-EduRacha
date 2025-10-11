package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

// ============================================
// ACTIVITY PRINCIPAL
// ============================================
class ListaEstudiantesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                ListaEstudiantesScreen(
                    onNavigateBack = { finish() },
                    onEstudianteClick = { estudiante ->
                        abrirDetalleEstudiante(estudiante)
                    }
                )
            }
        }
    }

    private fun abrirDetalleEstudiante(estudiante: Estudiante) {
        val intent = Intent(this, DetalleEstudianteActivity::class.java).apply {
            // CORRECCIÓN: Se añade el nombre del extra a cada putExtra
            putExtra("ESTUDIANTE_ID", estudiante.id)
            putExtra("ESTUDIANTE_NOMBRE", "${estudiante.nombre} ${estudiante.apellido}")
            putExtra("ESTUDIANTE_EMAIL", estudiante.email)
            putExtra("ESTUDIANTE_RACHA", estudiante.rachaActual)
            putExtra("ESTUDIANTE_RACHA_MEJOR", estudiante.rachaMejor)
            putExtra("ESTUDIANTE_PUNTOS", estudiante.puntosTotal)
            putExtra("ESTUDIANTE_PREGUNTAS", estudiante.preguntasRespondidas)
            putExtra("ESTUDIANTE_CORRECTAS", estudiante.preguntasCorrectas)
            putExtra("ESTUDIANTE_RANKING", estudiante.posicionRanking)
        }
        startActivity(intent)
    }
}

// ============================================
// SCREEN COMPOSABLE - REDISEÑADA
// ============================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ListaEstudiantesScreen(
    onNavigateBack: () -> Unit,
    onEstudianteClick: (Estudiante) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortType.RANKING) }

    val estudiantes = remember { Estudiante.obtenerEstudiantesEjemplo() }

    // CORRECCIÓN: Lógica de filtrado y ordenación corregida para que devuelva el valor.
    val estudiantesFiltrados = remember(searchQuery, sortBy, estudiantes) {
        val filtered = if (searchQuery.isBlank()) {
            estudiantes
        } else {
            estudiantes.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.apellido.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
        }

        // Aplicar ordenamiento y devolver la lista resultante
        when (sortBy) {
            SortType.RANKING -> filtered.sortedBy { it.posicionRanking }
            SortType.POINTS -> filtered.sortedByDescending { it.puntosTotal }
            SortType.STREAK -> filtered.sortedByDescending { it.rachaActual }
            SortType.NAME -> filtered.sortedBy { "${it.nombre} ${it.apellido}" }
        }
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PremiumHeader(
                title = "Estudiantes",
                totalCount = estudiantes.size,
                onNavigateBack = onNavigateBack
            )

            // Contenedor principal para la lista o el estado vacío
            AnimatedContent(
                targetState = estudiantesFiltrados.isEmpty(),
                label = "content_animation"
            ) { isEmpty ->
                if (isEmpty) {
                    PremiumEmptyState(
                        searchQuery = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PremiumSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                sortBy = sortBy,
                                onSortChange = { sortBy = it },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        items(
                            items = estudiantesFiltrados,
                            key = { it.id }
                        ) { estudiante ->
                            PremiumEstudianteCard(
                                estudiante = estudiante,
                                onClick = { onEstudianteClick(estudiante) },
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// HEADER PREMIUM CON GRADIENTE
// ============================================
@Composable
fun PremiumHeader(
    title: String,
    totalCount: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
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

            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EduRachaColors.Secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "$totalCount",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Gestiona tu comunidad educativa",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================
// SEARCH BAR PREMIUM
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: SortType,
    onSortChange: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = EduRachaColors.TextPrimary
                ),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Box(modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar por nombre o email...",
                                    color = EduRachaColors.TextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = EduRachaColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            )
        }

        Box {
            Card(
                modifier = Modifier
                    .size(50.dp)
                    .clickable { showSortMenu = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (sortBy != SortType.RANKING) EduRachaColors.Secondary else Color.White
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = if (sortBy != SortType.RANKING) Color.White else EduRachaColors.Primary
                    )
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                // CORRECCIÓN: Se usa .entries para iterar sobre el enum de forma segura
                SortType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (sortBy == type) EduRachaColors.Primary else EduRachaColors.TextSecondary
                                )
                                Text(
                                    text = type.label,
                                    fontWeight = if (sortBy == type) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onSortChange(type)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}

// ============================================
// CARD DE ESTUDIANTE PREMIUM
// ============================================
@Composable
fun PremiumEstudianteCard(
    estudiante: Estudiante,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, onClickLabel = "Ver detalles"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con Ranking Badge
            Box {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    getRankingColor(estudiante.posicionRanking),
                                    getRankingColor(estudiante.posicionRanking).copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // CORRECCIÓN: Se usa firstOrNull para evitar errores si el nombre está vacío
                        text = "${estudiante.nombre.firstOrNull()?.uppercaseChar() ?: ""}${estudiante.apellido.firstOrNull()?.uppercaseChar() ?: ""}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    shape = CircleShape,
                    color = getRankingColor(estudiante.posicionRanking),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "#${estudiante.posicionRanking}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información Principal
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${estudiante.nombre} ${estudiante.apellido}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = EduRachaColors.TextSecondary
                    )
                    Text(
                        text = estudiante.email,
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumStatChip(
                        icon = Icons.Default.Whatshot,
                        value = "${estudiante.rachaActual}",
                        // CORRECCIÓN: Se simplifica la lógica
                        color = if (estudiante.rachaActual > 0) EduRachaColors.StreakFire else EduRachaColors.TextSecondary
                    )
                    PremiumStatChip(
                        icon = Icons.Default.Star,
                        value = "${estudiante.puntosTotal}",
                        color = EduRachaColors.Accent
                    )
                    PremiumStatChip(
                        icon = Icons.Default.CheckCircle,
                        value = "${estudiante.preguntasCorrectas}",
                        color = EduRachaColors.Success
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = EduRachaColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PremiumStatChip(
    icon: ImageVector,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ============================================
// EMPTY STATE PREMIUM
// ============================================
@Composable
fun PremiumEmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }

        AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searchQuery.isBlank()) Icons.Default.People else Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = EduRachaColors.Primary.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (searchQuery.isBlank()) "No hay estudiantes" else "Sin resultados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank()) "Aún no hay estudiantes registrados en el sistema"
            else "No encontramos estudiantes con \"$searchQuery\"",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ============================================
// ENUMS Y HELPERS (AÑADIDOS)
// ============================================
enum class SortType(val label: String, val icon: ImageVector) {
    RANKING("Por Ranking", Icons.Default.EmojiEvents),
    POINTS("Por Puntos", Icons.Default.Star),
    STREAK("Por Racha", Icons.Default.Whatshot),
    NAME("Por Nombre", Icons.Default.SortByAlpha)
}

fun getRankingColor(posicion: Int): Color {
    return when (posicion) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.Primary
    }
}
