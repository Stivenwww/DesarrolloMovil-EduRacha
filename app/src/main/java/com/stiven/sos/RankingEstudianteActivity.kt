package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.RankingViewModel
import com.stiven.sos.viewmodel.TipoRanking

class RankingEstudianteActivity : ComponentActivity() {
    private val viewModel: RankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.cargarCursosInscritos()

        setContent {
            EduRachaTheme {
                RankingEstudianteScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun RankingEstudianteScreen(
    viewModel: RankingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var tipoRankingSeleccionado by remember { mutableStateOf(TipoRanking.EXPERIENCIA) }

    EduRachaV2Container {
        when {
            uiState.isLoading && cursoSeleccionado == null -> {
                LoadingEstudianteView()
            }

            uiState.error != null && cursoSeleccionado == null -> {
                ErrorEstudianteView(
                    error = uiState.error!!,
                    onRetry = { viewModel.cargarCursosInscritos() },
                    onNavigateBack = onNavigateBack
                )
            }

            cursoSeleccionado == null -> {
                ListaCursosEstudianteV2(
                    cursos = uiState.cursosInscritos,
                    onCursoClick = { curso ->
                        cursoSeleccionado = curso
                        viewModel.cargarRankingCurso(curso.id!!, TipoRanking.EXPERIENCIA)
                    },
                    onNavigateBack = onNavigateBack
                )
            }

            else -> {
                RankingDetalleCursoV2(
                    curso = cursoSeleccionado!!,
                    tipoRankingSeleccionado = tipoRankingSeleccionado,
                    rankingEstudiantes = uiState.rankingEstudiantes,
                    usuarioActualId = viewModel.obtenerUsuarioActualId(),
                    isLoading = uiState.isLoading,
                    onTipoRankingChange = { tipo ->
                        tipoRankingSeleccionado = tipo
                        viewModel.cargarRankingCurso(cursoSeleccionado!!.id!!, tipo)
                    },
                    onNavigateBack = { cursoSeleccionado = null }
                )
            }
        }
    }
}

@Composable
fun ListaCursosEstudianteV2(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit,
    onNavigateBack: () -> Unit
) {
    if (cursos.isEmpty()) {
        EmptyCursosV2(onNavigateBack)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header con gradiente
        item {
            HeaderCursosEstudiante(
                cantidadCursos = cursos.size,
                onNavigateBack = onNavigateBack
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
        }

        // Tarjetas de curso con animación escalonada
        items(items = cursos) { curso ->
            val index = cursos.indexOf(curso)
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 80L)
                visible = true
            }

            val offsetY by animateFloatAsState(
                targetValue = if (visible) 0f else 50f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(400)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        this.alpha = alpha
                    }
            ) {
                TarjetaCursoEstudianteV2(
                    curso = curso,
                    index = index,
                    onClick = { onCursoClick(curso) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeaderCursosEstudiante(
    cantidadCursos: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                EduRachaV2Gradients.Purple,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        AnimatedBubblesDecoration(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            color = Color.White
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedTrophyIconEstudiante()

            Spacer(Modifier.height(20.dp))

            Text(
                "Mis Rankings",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Compite y sigue tu progreso",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            ContadorCursosEstudiante(cantidad = cantidadCursos)
        }
    }
}

@Composable
fun AnimatedTrophyIconEstudiante() {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ContadorCursosEstudiante(cantidad: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                var displayCount by remember { mutableStateOf(0) }

                LaunchedEffect(cantidad) {
                    animate(
                        initialValue = 0f,
                        targetValue = cantidad.toFloat(),
                        animationSpec = tween(1200, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        displayCount = value.toInt()
                    }
                }

                Text(
                    "$displayCount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Column {
                Text(
                    "Cursos inscritos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Selecciona uno para ver ranking",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Outlined.School,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TarjetaCursoEstudianteV2(
    curso: Curso,
    index: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val gradienteColor = when (index % 4) {
        0 -> EduRachaV2Gradients.Blue
        1 -> EduRachaV2Gradients.Green
        2 -> EduRachaV2Gradients.Orange
        else -> EduRachaV2Gradients.Pink
    }

    val iconColor = when (index % 4) {
        0 -> EduRachaV2Colors.Primary
        1 -> EduRachaV2Colors.Success
        2 -> EduRachaV2Colors.Warning
        else -> EduRachaV2Colors.Pink
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradienteColor)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedCourseIconEstudiante(iconColor)

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = curso.codigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = curso.titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedArrowIconEstudiante()
        }
    }
}

@Composable
fun AnimatedCourseIconEstudiante(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun AnimatedArrowIconEstudiante() {
    val infiniteTransition = rememberInfiniteTransition()

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        Icons.Outlined.ChevronRight,
        null,
        tint = Color.White,
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer { translationX = offsetX }
    )
}

@Composable
fun RankingDetalleCursoV2(
    curso: Curso,
    tipoRankingSeleccionado: TipoRanking,
    rankingEstudiantes: List<RankingEstudiante>,
    usuarioActualId: String?,
    isLoading: Boolean,
    onTipoRankingChange: (TipoRanking) -> Unit,
    onNavigateBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header del detalle
        item {
            HeaderDetalleRanking(
                curso = curso,
                onNavigateBack = onNavigateBack
            )
        }

        // Tabs de tipo de ranking
        item {
            TabsRankingV2(
                tipoSeleccionado = tipoRankingSeleccionado,
                onTipoChange = onTipoRankingChange
            )
        }

        if (isLoading) {
            item {
                EduRachaV2LoadingState(
                    message = "Cargando ranking...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (rankingEstudiantes.isEmpty()) {
            item {
                EduRachaV2EmptyState(
                    icon = Icons.Outlined.Leaderboard,
                    iconColor = EduRachaV2Colors.Secondary,
                    title = "Ranking vacío",
                    message = "Completa quizzes para aparecer en el ranking y competir con tus compañeros 🎯",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                Spacer(Modifier.height(20.dp))
            }

            // Estadísticas
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    EstadisticasRankingV2(
                        ranking = rankingEstudiantes,
                        tipoRanking = tipoRankingSeleccionado
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Podio
            if (rankingEstudiantes.size >= 3) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PodioV2(
                            ranking = rankingEstudiantes.take(3),
                            usuarioActualId = usuarioActualId,
                            tipoRanking = tipoRankingSeleccionado
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Resto del ranking
            if (rankingEstudiantes.size > 3) {
                item {
                    DividerRanking()
                }

                itemsIndexed(rankingEstudiantes.drop(3)) { index, estudiante ->
                    val posicion = index + 4
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 60L)
                        visible = true
                    }

                    val offsetY by animateFloatAsState(
                        targetValue = if (visible) 0f else 30f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )

                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(300)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .graphicsLayer {
                                translationY = offsetY
                                this.alpha = alpha
                            }
                    ) {
                        RankingItemV2(
                            posicion = posicion,
                            estudiante = estudiante,
                            esUsuarioActual = estudiante.id == usuarioActualId,
                            tipoRanking = tipoRankingSeleccionado
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HeaderDetalleRanking(
    curso: Curso,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                EduRachaV2Gradients.Blue,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Text(
                    text = curso.codigo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                curso.titulo,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Tabla de clasificación",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TabsRankingV2(
    tipoSeleccionado: TipoRanking,
    onTipoChange: (TipoRanking) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabChipV2(
                label = "XP",
                icon = Icons.Outlined.Star,
                isSelected = tipoSeleccionado == TipoRanking.EXPERIENCIA,
                onClick = { onTipoChange(TipoRanking.EXPERIENCIA) },
                gradient = EduRachaV2Gradients.Yellow,
                color = EduRachaV2Colors.Accent,
                modifier = Modifier.weight(1f)
            )
            TabChipV2(
                label = "Racha",
                icon = Icons.Outlined.Whatshot,
                isSelected = tipoSeleccionado == TipoRanking.RACHA,
                onClick = { onTipoChange(TipoRanking.RACHA) },
                gradient = EduRachaV2Gradients.Orange,
                color = EduRachaV2Colors.Warning,
                modifier = Modifier.weight(1f)
            )
            TabChipV2(
                label = "Vidas",
                icon = Icons.Outlined.Favorite,
                isSelected = tipoSeleccionado == TipoRanking.VIDAS,
                onClick = { onTipoChange(TipoRanking.VIDAS) },
                gradient = EduRachaV2Gradients.Pink,
                color = EduRachaV2Colors.Pink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TabChipV2(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    gradient: Brush,
    color: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) gradient else Brush.linearGradient(listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.1f))))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) Color.White else color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else color
                )
            }
        }
    }
}

@Composable
fun EstadisticasRankingV2(
    ranking: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    val valores = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.map { it.experiencia }
        TipoRanking.RACHA -> ranking.map { it.diasConsecutivos }
        TipoRanking.VIDAS -> ranking.map { it.vidas }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Analytics,
                        null,
                        tint = EduRachaV2Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Estadísticas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.TextPrimary
                    )
                    Text(
                        "Resumen del curso",
                        fontSize = 13.sp,
                        color = EduRachaV2Colors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EduRachaV2StatCardCompact(
                    icon = Icons.Outlined.People,
                    valor = ranking.size.toString(),
                    label = "Estudiantes",
                    color = EduRachaV2Colors.Primary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                EduRachaV2StatCardCompact(
                    icon = Icons.Outlined.TrendingUp,
                    valor = String.format("%.1f", valores.average()),
                    label = "Promedio",
                    color = EduRachaV2Colors.Success,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                EduRachaV2StatCardCompact(
                    icon = Icons.Outlined.EmojiEvents,
                    valor = (valores.maxOrNull() ?: 0).toString(),
                    label = "Máximo",
                    color = EduRachaV2Colors.Accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PodioV2(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🏆", fontSize = 36.sp)
                Column {
                    Text(
                        "Podio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.TextPrimary
                    )
                    Text(
                        "Top 3 mejores estudiantes",
                        fontSize = 13.sp,
                        color = EduRachaV2Colors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Segundo lugar
                if (ranking.size >= 2) {
                    PosicionPodioV2(
                        posicion = 2,
                        estudiante = ranking[1],
                        esUsuarioActual = ranking[1].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 110.dp
                    )
                }

                // Primer lugar
                PosicionPodioV2(
                    posicion = 1,
                    estudiante = ranking[0],
                    esUsuarioActual = ranking[0].id == usuarioActualId,
                    tipoRanking = tipoRanking,
                    height = 140.dp
                )

                // Tercer lugar
                if (ranking.size >= 3) {
                    PosicionPodioV2(
                        posicion = 3,
                        estudiante = ranking[2],
                        esUsuarioActual = ranking[2].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 90.dp
                    )
                }
            }
        }
    }
}

@Composable
fun PosicionPodioV2(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking,
    height: Dp
) {
    val colorMedalla = when (posicion) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }

    val valor = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> estudiante.experiencia.toString()
        TipoRanking.RACHA -> estudiante.diasConsecutivos.toString()
        TipoRanking.VIDAS -> estudiante.vidas.toString()
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((posicion * 150L))
        isVisible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(100.dp).offset(y = offsetY)
    ) {
        // Medalla animada
        MedallaAnimada(
            tipo = when (posicion) {
                1 -> TipoMedalla.ORO
                2 -> TipoMedalla.PLATA
                else -> TipoMedalla.BRONCE
            },
            modifier = Modifier.size(if (esUsuarioActual) 72.dp else 64.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            estudiante.nombre.split(" ").firstOrNull() ?: estudiante.nombre,
            fontSize = 13.sp,
            fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
            color = EduRachaV2Colors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (esUsuarioActual) {
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduRachaV2Colors.Primary
            ) {
                Text(
                    "TÚ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colorMedalla.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, colorMedalla.copy(alpha = 0.4f))
        ) {
            Text(
                valor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colorMedalla.copy(red = colorMedalla.red * 0.7f, green = colorMedalla.green * 0.7f, blue = colorMedalla.blue * 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Pedestal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorMedalla.copy(alpha = 0.4f),
                            colorMedalla.copy(alpha = 0.2f)
                        )
                    )
                )
                .then(
                    if (esUsuarioActual) {
                        Modifier.border(
                            width = 2.dp,
                            color = EduRachaV2Colors.Primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$posicion",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = colorMedalla.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DividerRanking() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = EduRachaV2Colors.SoftGray,
            thickness = 1.dp
        )
        Text(
            "Clasificación General",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaV2Colors.TextSecondary
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = EduRachaV2Colors.SoftGray,
            thickness = 1.dp
        )
    }
}

@Composable
fun RankingItemV2(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaV2Colors.Primary.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (esUsuarioActual) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (esUsuarioActual)
                            EduRachaV2Gradients.Blue
                        else
                            Brush.linearGradient(
                                listOf(
                                    EduRachaV2Colors.TextSecondary.copy(alpha = 0.2f),
                                    EduRachaV2Colors.TextSecondary.copy(alpha = 0.1f)
                                )
                            )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    posicion.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (esUsuarioActual) Color.White else EduRachaV2Colors.TextSecondary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        estudiante.nombre,
                        fontSize = 16.sp,
                        fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
                        color = EduRachaV2Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (esUsuarioActual) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduRachaV2Colors.Primary
                        ) {
                            Text(
                                "Tú",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                BadgeValorRanking(
                    tipoRanking = tipoRanking,
                    estudiante = estudiante
                )
            }
        }
    }
}

@Composable
fun BadgeValorRanking(
    tipoRanking: TipoRanking,
    estudiante: RankingEstudiante
) {
    val (icon, valor, color) = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> Triple(
            Icons.Outlined.Star,
            "${estudiante.experiencia} XP",
            EduRachaV2Colors.Accent
        )
        TipoRanking.RACHA -> Triple(
            Icons.Outlined.Whatshot,
            "${estudiante.diasConsecutivos} días",
            EduRachaV2Colors.Warning
        )
        TipoRanking.VIDAS -> Triple(
            Icons.Outlined.Favorite,
            "${estudiante.vidas} ❤️",
            EduRachaV2Colors.Pink
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun LoadingEstudianteView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EduRachaV2Colors.Secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = EduRachaV2Colors.Secondary,
                    strokeWidth = 4.dp
                )
            }

            Text(
                "Cargando cursos...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaV2Colors.TextPrimary
            )

            Text(
                "Preparando tus rankings 🎯",
                fontSize = 14.sp,
                color = EduRachaV2Colors.TextSecondary
            )
        }
    }
}

@Composable
fun ErrorEstudianteView(
    error: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        EduRachaV2Gradients.Pink,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            EduRachaV2ErrorState(
                title = "Error al cargar",
                message = error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyCursosV2(onNavigateBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        EduRachaV2Gradients.Purple,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            EduRachaV2EmptyState(
                icon = Icons.Outlined.School,
                iconColor = EduRachaV2Colors.Secondary,
                title = "Sin cursos inscritos",
                message = "Únete a un curso para ver el ranking y competir con otros estudiantes. ¡La competencia te espera! 🚀",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}