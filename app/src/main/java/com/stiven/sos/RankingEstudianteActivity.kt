package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
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

    Scaffold(
        containerColor = EduRachaColors.Background,
        topBar = {
            TopBarEstudiante(
                titulo = if (cursoSeleccionado == null) "Mis Rankings" else cursoSeleccionado!!.titulo,
                mostrarBotonVolver = cursoSeleccionado != null,
                onNavigateBack = {
                    if (cursoSeleccionado != null) {
                        cursoSeleccionado = null
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingRankingEstudiante()
                uiState.error != null -> ErrorRankingEstudiante(mensaje = uiState.error!!) {
                    viewModel.cargarCursosInscritos()
                }
                cursoSeleccionado == null -> {
                    ListaCursosEstudiante(
                        cursos = uiState.cursosInscritos,
                        onCursoClick = { curso ->
                            cursoSeleccionado = curso
                            viewModel.cargarRankingCurso(curso.id!!, TipoRanking.EXPERIENCIA)
                        }
                    )
                }
                else -> {
                    RankingDetalleCursoEstudiante(
                        cursoId = cursoSeleccionado!!.id!!,
                        cursoTitulo = cursoSeleccionado!!.titulo,
                        tipoRankingSeleccionado = tipoRankingSeleccionado,
                        rankingEstudiantes = uiState.rankingEstudiantes,
                        usuarioActualId = viewModel.obtenerUsuarioActualId(),
                        onTipoRankingChange = { tipo ->
                            tipoRankingSeleccionado = tipo
                            viewModel.cargarRankingCurso(cursoSeleccionado!!.id!!, tipo)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarEstudiante(
    titulo: String,
    mostrarBotonVolver: Boolean,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    EduRachaColors.Primary,
                    EduRachaColors.PrimaryLight
                )
            )
        )
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = EduRachaColors.Secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            titulo,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Volver",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ListaCursosEstudiante(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit
) {
    if (cursos.isEmpty()) {
        EmptyCursosEstudiante()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        EduRachaColors.Primary.copy(alpha = 0.12f),
                                        EduRachaColors.Accent.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Mis Cursos",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Selecciona un curso para ver el ranking",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                EduRachaColors.Secondary,
                                                EduRachaColors.SecondaryLight
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        cursos.size.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "cursos",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(cursos) { curso ->
                CursoCardEstudiante(curso = curso, onClick = { onCursoClick(curso) })
            }
        }
    }
}

@Composable
fun CursoCardEstudiante(curso: Curso, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                EduRachaColors.Accent,
                                EduRachaColors.AccentLight
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    curso.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.AccentContainer
                ) {
                    Text(
                        curso.codigo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EduRachaColors.Accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = EduRachaColors.Accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RankingDetalleCursoEstudiante(
    cursoId: String,
    cursoTitulo: String,
    tipoRankingSeleccionado: TipoRanking,
    rankingEstudiantes: List<RankingEstudiante>,
    usuarioActualId: String?,
    onTipoRankingChange: (TipoRanking) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
    ) {
        // Tabs de tipo de ranking
        TabsRankingEstudiante(
            tipoSeleccionado = tipoRankingSeleccionado,
            onTipoChange = onTipoRankingChange
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (rankingEstudiantes.isEmpty()) {
                item { EmptyRankingEstudiante() }
            } else {
                // Estadísticas generales
                item {
                    EstadisticasGeneralesEstudiante(
                        ranking = rankingEstudiantes,
                        tipoRanking = tipoRankingSeleccionado
                    )
                }

                // Podio (Top 3)
                if (rankingEstudiantes.size >= 3) {
                    item {
                        PodioEstudiante(
                            ranking = rankingEstudiantes.take(3),
                            usuarioActualId = usuarioActualId,
                            tipoRanking = tipoRankingSeleccionado
                        )
                    }
                }

                // Separador
                if (rankingEstudiantes.size > 3) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = EduRachaColors.Border
                            )
                            Text(
                                "Otros Participantes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EduRachaColors.TextSecondary
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = EduRachaColors.Border
                            )
                        }
                    }

                    // Resto del ranking
                    itemsIndexed(rankingEstudiantes.drop(3)) { index, estudiante ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(300, delayMillis = index * 50)
                            )
                        ) {
                            RankingItemEstudiante(
                                posicion = index + 4,
                                estudiante = estudiante,
                                esUsuarioActual = estudiante.id == usuarioActualId,
                                tipoRanking = tipoRankingSeleccionado
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabsRankingEstudiante(
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabChipEstudiante(
                label = "Experiencia",
                icon = Icons.Default.Star,
                isSelected = tipoSeleccionado == TipoRanking.EXPERIENCIA,
                onClick = { onTipoChange(TipoRanking.EXPERIENCIA) },
                color = EduRachaColors.Secondary,
                modifier = Modifier.weight(1f)
            )
            TabChipEstudiante(
                label = "Racha",
                icon = Icons.Default.Whatshot,
                isSelected = tipoSeleccionado == TipoRanking.RACHA,
                onClick = { onTipoChange(TipoRanking.RACHA) },
                color = EduRachaColors.StreakFire,
                modifier = Modifier.weight(1f)
            )
            TabChipEstudiante(
                label = "Vidas",
                icon = Icons.Default.Favorite,
                isSelected = tipoSeleccionado == TipoRanking.VIDAS,
                onClick = { onTipoChange(TipoRanking.VIDAS) },
                color = EduRachaColors.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TabChipEstudiante(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 3.dp else 1.dp
        ),
        border = if (!isSelected) BorderStroke(1.5.dp, color.copy(alpha = 0.3f)) else null,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(22.dp)
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

@Composable
fun EstadisticasGeneralesEstudiante(
    ranking: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    val total = ranking.size
    val valores = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.map { it.experiencia }
        TipoRanking.RACHA -> ranking.map { it.diasConsecutivos }
        TipoRanking.VIDAS -> ranking.map { it.vidas }
    }
    val promedio = valores.average()
    val maximo = valores.maxOrNull() ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary.copy(alpha = 0.12f),
                            Color.White
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.PrimaryLight
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Estadísticas del Curso",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        "Resumen general",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItemEstudiante(
                    label = "Participantes",
                    value = total.toString(),
                    icon = Icons.Default.People,
                    color = EduRachaColors.Primary
                )
                Divider(
                    modifier = Modifier
                        .height(70.dp)
                        .width(1.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.5f)
                )
                EstadisticaItemEstudiante(
                    label = "Promedio",
                    value = String.format("%.1f", promedio),
                    icon = Icons.Default.TrendingUp,
                    color = EduRachaColors.Accent
                )
                Divider(
                    modifier = Modifier
                        .height(70.dp)
                        .width(1.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.5f)
                )
                EstadisticaItemEstudiante(
                    label = "Máximo",
                    value = maximo.toString(),
                    icon = Icons.Default.EmojiEvents,
                    color = EduRachaColors.Success
                )
            }
        }
    }
}

@Composable
fun EstadisticaItemEstudiante(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 11.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PodioEstudiante(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Secondary.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🏆", fontSize = 32.sp)
                Column {
                    Text(
                        "Podio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        "Top 3 estudiantes",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Segundo lugar
                if (ranking.size >= 2) {
                    PosicionPodioEstudiante(
                        posicion = 2,
                        estudiante = ranking[1],
                        esUsuarioActual = ranking[1].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 100.dp
                    )
                }

                // Primer lugar
                PosicionPodioEstudiante(
                    posicion = 1,
                    estudiante = ranking[0],
                    esUsuarioActual = ranking[0].id == usuarioActualId,
                    tipoRanking = tipoRanking,
                    height = 130.dp
                )

                // Tercer lugar
                if (ranking.size >= 3) {
                    PosicionPodioEstudiante(
                        posicion = 3,
                        estudiante = ranking[2],
                        esUsuarioActual = ranking[2].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 80.dp
                    )
                }
            }
        }
    }
}

@Composable
fun PosicionPodioEstudiante(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking,
    height: Dp
) {
    val colorMedalla = when (posicion) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.TextSecondary
    }

    val valor = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> estudiante.experiencia.toString()
        TipoRanking.RACHA -> estudiante.diasConsecutivos.toString()
        TipoRanking.VIDAS -> estudiante.vidas.toString()
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(100.dp).offset(y = offsetY)
    ) {
        // Medalla
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorMedalla, colorMedalla.copy(alpha = 0.8f))
                    )
                )
                .then(if (esUsuarioActual) Modifier.scale(1.1f) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (posicion) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> ""
                },
                fontSize = 30.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            estudiante.nombre.split(" ").firstOrNull() ?: estudiante.nombre,
            fontSize = 12.sp,
            fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        if (esUsuarioActual) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EduRachaColors.Primary
            ) {
                Text(
                    "TÚ",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colorMedalla.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, colorMedalla.copy(alpha = 0.3f))
        ) {
            Text(
                valor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorMedalla,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pedestal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorMedalla.copy(alpha = 0.5f),
                            colorMedalla.copy(alpha = 0.25f)
                        )
                    )
                )
                .then(
                    if (esUsuarioActual) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    EduRachaColors.Primary.copy(alpha = 0.8f),
                                    EduRachaColors.Primary.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$posicion",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorMedalla
            )
        }
    }
}

@Composable
fun RankingItemEstudiante(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaColors.PrimaryContainer
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (esUsuarioActual) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    posicion.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Accent
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        estudiante.nombre,
                        fontSize = 15.sp,
                        fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.Medium,
                        color = EduRachaColors.TextPrimary
                    )
                    if (esUsuarioActual) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduRachaColors.Primary
                        ) {
                            Text(
                                "Tú",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (tipoRanking) {
                        TipoRanking.EXPERIENCIA -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Accent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EduRachaColors.Accent.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = EduRachaColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${estudiante.experiencia} XP",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.Accent
                                    )
                                }
                            }
                        }
                        TipoRanking.RACHA -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.StreakFire.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EduRachaColors.StreakFire.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Whatshot,
                                        null,
                                        tint = EduRachaColors.StreakFire,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${estudiante.diasConsecutivos} días",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.StreakFire
                                    )
                                }
                            }
                        }
                        TipoRanking.VIDAS -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Error.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EduRachaColors.Error.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        null,
                                        tint = EduRachaColors.Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${estudiante.vidas} ❤️",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.Error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCursosEstudiante() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(EduRachaColors.AccentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                null,
                modifier = Modifier.size(65.dp),
                tint = EduRachaColors.Accent
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Sin cursos inscritos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Únete a un curso para ver el ranking y competir con otros estudiantes",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun EmptyRankingEstudiante() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(EduRachaColors.AccentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Leaderboard,
                null,
                modifier = Modifier.size(65.dp),
                tint = EduRachaColors.Accent
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Ranking vacío",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Los estudiantes deben completar quizzes para aparecer en el ranking",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun ErrorRankingEstudiante(mensaje: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(EduRachaColors.ErrorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Error,
                null,
                modifier = Modifier.size(60.dp),
                tint = EduRachaColors.Error
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Error al cargar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            mensaje,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = EduRachaColors.Primary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(
                "Reintentar",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LoadingRankingEstudiante() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Icon(
                Icons.Default.EmojiEvents,
                null,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation),
                tint = EduRachaColors.Primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Cargando ranking...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary
            )
        }
    }
}