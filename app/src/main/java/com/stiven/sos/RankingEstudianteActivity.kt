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
            RankingTopBar(
                titulo = if (cursoSeleccionado == null) "🏆 Ranking General" else cursoSeleccionado!!.titulo,
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Background,
                            EduRachaColors.PrimaryContainer.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    LoadingRanking()
                }
                uiState.error != null -> {
                    ErrorRanking(mensaje = uiState.error!!) {
                        viewModel.cargarCursosInscritos()
                    }
                }
                cursoSeleccionado == null -> {
                    ListaCursosInscritos(
                        cursos = uiState.cursosInscritos,
                        onCursoClick = { curso ->
                            cursoSeleccionado = curso
                            viewModel.cargarRankingCurso(curso.id!!, TipoRanking.EXPERIENCIA)
                        }
                    )
                }
                else -> {
                    RankingDetalleCurso(
                        cursoId = cursoSeleccionado!!.id!!,
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
fun RankingTopBar(
    titulo: String,
    mostrarBotonVolver: Boolean,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.PrimaryLight,
                            EduRachaColors.Accent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = titulo,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ListaCursosInscritos(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit
) {
    if (cursos.isEmpty()) {
        EmptyCursosState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        EduRachaColors.Accent.copy(alpha = 0.12f),
                                        EduRachaColors.Secondary.copy(alpha = 0.08f)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎯",
                                        fontSize = 28.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Mis Cursos",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EduRachaColors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toca un curso para ver tu posición en el ranking",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
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
                                                EduRachaColors.Accent
                                            )
                                        )
                                    )
                                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cursos.size.toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            items(cursos) { curso ->
                CursoRankingCard(
                    curso = curso,
                    onClick = { onCursoClick(curso) }
                )
            }
        }
    }
}

@Composable
fun CursoRankingCard(
    curso: Curso,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            EduRachaColors.AccentContainer.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Secondary,
                                    EduRachaColors.Accent
                                )
                            )
                        )
                        .border(
                            3.dp,
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = curso.titulo,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EduRachaColors.AccentContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = EduRachaColors.Accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = curso.codigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Accent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = EduRachaColors.Accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RankingDetalleCurso(
    cursoId: String,
    tipoRankingSeleccionado: TipoRanking,
    rankingEstudiantes: List<RankingEstudiante>,
    usuarioActualId: String?,
    onTipoRankingChange: (TipoRanking) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        RankingEstadisticas(
            rankingEstudiantes = rankingEstudiantes,
            tipoRanking = tipoRankingSeleccionado
        )

        Spacer(modifier = Modifier.height(20.dp))

        RankingTypeSelector(
            tipoSeleccionado = tipoRankingSeleccionado,
            onTipoChange = onTipoRankingChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (rankingEstudiantes.isEmpty()) {
            EmptyRankingState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(rankingEstudiantes) { index, estudiante ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 50
                            )
                        )
                    ) {
                        RankingEstudianteItem(
                            posicion = index + 1,
                            estudiante = estudiante,
                            tipoRanking = tipoRankingSeleccionado,
                            esUsuarioActual = estudiante.id == usuarioActualId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingEstadisticas(
    rankingEstudiantes: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    if (rankingEstudiantes.isEmpty()) return

    val total = rankingEstudiantes.size
    val promedio = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> rankingEstudiantes.map { it.experiencia }.average().toInt()
        TipoRanking.RACHA -> rankingEstudiantes.map { it.diasConsecutivos }.average().toInt()
        TipoRanking.VIDAS -> rankingEstudiantes.map { it.vidas }.average().toInt()
    }
    val maximo = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> rankingEstudiantes.maxOfOrNull { it.experiencia } ?: 0
        TipoRanking.RACHA -> rankingEstudiantes.maxOfOrNull { it.diasConsecutivos } ?: 0
        TipoRanking.VIDAS -> rankingEstudiantes.maxOfOrNull { it.vidas } ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EduRachaColors.Accent.copy(alpha = 0.08f),
                            EduRachaColors.Secondary.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItem(
                    label = "Participantes",
                    value = total.toString(),
                    icon = Icons.Default.People,
                    color = EduRachaColors.Accent
                )

                Divider(
                    modifier = Modifier
                        .height(70.dp)
                        .width(2.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.3f)
                )

                EstadisticaItem(
                    label = "Promedio",
                    value = promedio.toString(),
                    icon = Icons.Default.BarChart,
                    color = EduRachaColors.Secondary
                )

                Divider(
                    modifier = Modifier
                        .height(70.dp)
                        .width(2.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.3f)
                )

                EstadisticaItem(
                    label = "Máximo",
                    value = maximo.toString(),
                    icon = Icons.Default.TrendingUp,
                    color = EduRachaColors.Success
                )
            }
        }
    }
}

@Composable
fun EstadisticaItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun RankingTypeSelector(
    tipoSeleccionado: TipoRanking,
    onTipoChange: (TipoRanking) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankingTypeChip(
            label = "Experiencia",
            icon = Icons.Default.Star,
            isSelected = tipoSeleccionado == TipoRanking.EXPERIENCIA,
            onClick = { onTipoChange(TipoRanking.EXPERIENCIA) },
            modifier = Modifier.weight(1f),
            color = EduRachaColors.Secondary
        )

        RankingTypeChip(
            label = "Racha",
            icon = Icons.Default.Whatshot,
            isSelected = tipoSeleccionado == TipoRanking.RACHA,
            onClick = { onTipoChange(TipoRanking.RACHA) },
            modifier = Modifier.weight(1f),
            color = EduRachaColors.StreakFire
        )

        RankingTypeChip(
            label = "Vidas",
            icon = Icons.Default.Favorite,
            isSelected = tipoSeleccionado == TipoRanking.VIDAS,
            onClick = { onTipoChange(TipoRanking.VIDAS) },
            modifier = Modifier.weight(1f),
            color = EduRachaColors.Error
        )
    }
}

@Composable
fun RankingTypeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 10.dp else 3.dp
        ),
        border = if (!isSelected) BorderStroke(2.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

data class RankingUI(
    val valor: String,
    val unidad: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun RankingEstudianteItem(
    posicion: Int,
    estudiante: RankingEstudiante,
    tipoRanking: TipoRanking,
    esUsuarioActual: Boolean
) {
    val rankingColor = when (posicion) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.Accent
    }

    val info = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> RankingUI(
            valor = estudiante.experiencia.toString(),
            unidad = "XP",
            icon = Icons.Default.Star,
            color = EduRachaColors.Secondary
        )
        TipoRanking.RACHA -> RankingUI(
            valor = estudiante.diasConsecutivos.toString(),
            unidad = "días",
            icon = Icons.Default.Whatshot,
            color = EduRachaColors.StreakFire
        )
        TipoRanking.VIDAS -> RankingUI(
            valor = estudiante.vidas.toString(),
            unidad = "❤️",
            icon = Icons.Default.Favorite,
            color = EduRachaColors.Error
        )
    }

    val isTopThree = posicion <= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isTopThree) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                esUsuarioActual -> EduRachaColors.Accent.copy(alpha = 0.12f)
                isTopThree -> Color.White
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTopThree) 10.dp else 4.dp
        ),
        border = BorderStroke(
            width = if (esUsuarioActual) 3.dp else if (isTopThree) 2.dp else 0.dp,
            color = if (esUsuarioActual) EduRachaColors.Accent else if (isTopThree) rankingColor.copy(alpha = 0.4f) else Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isTopThree) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                rankingColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTopThree) 20.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isTopThree) 60.dp else 52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTopThree) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        rankingColor,
                                        rankingColor.copy(alpha = 0.6f)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(
                                        rankingColor.copy(alpha = 0.2f),
                                        rankingColor.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        )
                        .border(
                            width = if (isTopThree) 3.dp else 0.dp,
                            color = if (isTopThree) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTopThree) {
                        Text(
                            text = when (posicion) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> posicion.toString()
                            },
                            fontSize = 32.sp
                        )
                    } else {
                        Text(
                            text = posicion.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = rankingColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = estudiante.nombre,
                            fontSize = if (isTopThree) 18.sp else 16.sp,
                            fontWeight = if (esUsuarioActual || isTopThree) FontWeight.ExtraBold else FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (esUsuarioActual) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EduRachaColors.Accent
                            ) {
                                Text(
                                    text = "Tú",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (isTopThree && tipoRanking == TipoRanking.EXPERIENCIA) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val maxXP = 1000f
                        val progress = (estudiante.experiencia.toFloat() / maxXP).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(rankingColor.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                rankingColor,
                                                rankingColor.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = info.icon,
                            contentDescription = null,
                            tint = info.color,
                            modifier = Modifier.size(if (isTopThree) 26.dp else 22.dp)
                        )
                        Text(
                            text = info.valor,
                            fontSize = if (isTopThree) 24.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = info.color
                        )
                    }
                    Text(
                        text = info.unidad,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCursosState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EduRachaColors.Accent.copy(alpha = 0.2f),
                                EduRachaColors.AccentContainer
                            )
                        )
                    )
                    .border(4.dp, EduRachaColors.Accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = EduRachaColors.Accent
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "🎓 Sin cursos aún",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Únete a un curso para competir y ver tu posición en el ranking con otros estudiantes",
                fontSize = 16.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyRankingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EduRachaColors.Secondary.copy(alpha = 0.2f),
                                EduRachaColors.AccentContainer
                            )
                        )
                    )
                    .border(4.dp, EduRachaColors.Secondary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = EduRachaColors.Secondary
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "🏆 Ranking vacío",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¡Sé el primero en aparecer! Completa quizzes y actividades para escalar posiciones",
                fontSize = 16.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoadingRanking() {
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
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EduRachaColors.Accent.copy(alpha = 0.2f),
                                EduRachaColors.AccentContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(rotation),
                    tint = EduRachaColors.Accent
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cargando ranking...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Preparando las posiciones",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary
            )
        }
    }
}

@Composable
fun ErrorRanking(
    mensaje: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EduRachaColors.Error.copy(alpha = 0.2f),
                                EduRachaColors.ErrorContainer
                            )
                        )
                    )
                    .border(4.dp, EduRachaColors.Error.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = EduRachaColors.Error
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "😕 Algo salió mal",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                fontSize = 16.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Accent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 180.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Reintentar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}