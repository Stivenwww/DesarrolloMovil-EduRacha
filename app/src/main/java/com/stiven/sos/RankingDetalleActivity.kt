package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.RankingViewModel
import com.stiven.sos.viewmodel.TipoRanking

class RankingDetalleActivity : ComponentActivity() {
    private val viewModel: RankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Ranking"

        if (cursoId.isEmpty()) {
            finish()
            return
        }

        // Cargar ranking por defecto (experiencia)
        viewModel.cargarRankingCurso(cursoId, TipoRanking.EXPERIENCIA)

        setContent {
            EduRachaTheme {
                RankingDetalleScreen(
                    cursoId = cursoId,
                    cursoTitulo = cursoTitulo,
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetalleScreen(
    cursoId: String,
    cursoTitulo: String,
    viewModel: RankingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // Cambiar ranking cuando se cambia de tab
    LaunchedEffect(selectedTab) {
        val tipo = when (selectedTab) {
            0 -> TipoRanking.EXPERIENCIA
            1 -> TipoRanking.RACHA
            2 -> TipoRanking.VIDAS
            else -> TipoRanking.EXPERIENCIA
        }
        viewModel.cargarRankingCurso(cursoId, tipo)
    }

    Scaffold(
        topBar = {
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
                        Column {
                            Text(
                                cursoTitulo,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Ranking del Curso",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
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

                // Tabs mejorados
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = tabPositions[selectedTab].left)
                                    .width(tabPositions[selectedTab].width)
                                    .height(4.dp)
                                    .padding(horizontal = 24.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.height(64.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Experiencia",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.height(64.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Whatshot,
                                null,
                                tint = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Racha",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.height(64.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                null,
                                tint = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Vidas",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingRankingDetalle()
                }

                uiState.error != null -> {
                    ErrorViewRankingDetalle(
                        error = uiState.error!!,
                        onRetry = {
                            val tipo = when (selectedTab) {
                                0 -> TipoRanking.EXPERIENCIA
                                1 -> TipoRanking.RACHA
                                2 -> TipoRanking.VIDAS
                                else -> TipoRanking.EXPERIENCIA
                            }
                            viewModel.cargarRankingCurso(cursoId, tipo)
                        }
                    )
                }

                uiState.rankingEstudiantes.isEmpty() -> {
                    EmptyRankingDetalleView()
                }

                else -> {
                    RankingCursoDetalleView(
                        ranking = uiState.rankingEstudiantes,
                        usuarioActualId = viewModel.obtenerUsuarioActualId(),
                        tipoRanking = uiState.tipoRanking
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCursoDetalleView(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estadísticas generales
        item {
            EstadisticasGeneralesCard(
                ranking = ranking,
                tipoRanking = tipoRanking
            )
        }

        // Podio (Top 3)
        if (ranking.size >= 3) {
            item {
                PodioCard(
                    ranking = ranking.take(3),
                    usuarioActualId = usuarioActualId,
                    tipoRanking = tipoRanking
                )
            }
        }

        // Separador
        if (ranking.size > 3) {
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
                        text = "Otros Participantes",
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
            itemsIndexed(ranking.drop(3)) { index, estudiante ->
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
                    RankingCardDetalle(
                        posicion = index + 4,
                        estudiante = estudiante,
                        esUsuarioActual = estudiante.id == usuarioActualId,
                        tipoRanking = tipoRanking
                    )
                }
            }
        } else {
            // Si hay menos de 3, mostrar todos normalmente
            itemsIndexed(ranking) { index, estudiante ->
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
                    TopRankingCardDetalle(
                        posicion = index + 1,
                        estudiante = estudiante,
                        esUsuarioActual = estudiante.id == usuarioActualId,
                        tipoRanking = tipoRanking
                    )
                }
            }
        }
    }
}

@Composable
fun EstadisticasGeneralesCard(
    ranking: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    val total = ranking.size
    val promedio = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.map { it.experiencia }.average()
        TipoRanking.RACHA -> ranking.map { it.diasConsecutivos }.average()
        TipoRanking.VIDAS -> ranking.map { it.vidas }.average()
    }
    val maximo = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.maxOfOrNull { it.experiencia } ?: 0
        TipoRanking.RACHA -> ranking.maxOfOrNull { it.diasConsecutivos } ?: 0
        TipoRanking.VIDAS -> ranking.maxOfOrNull { it.vidas } ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary.copy(alpha = 0.15f),
                            EduRachaColors.Primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        text = "Estadísticas del Curso",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Resumen de rendimiento",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItemDetalle(
                    label = "Participantes",
                    value = total.toString(),
                    icon = Icons.Default.People,
                    color = EduRachaColors.Primary
                )

                Divider(
                    modifier = Modifier
                        .height(80.dp)
                        .width(2.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.5f)
                )

                EstadisticaItemDetalle(
                    label = "Promedio",
                    value = String.format("%.1f", promedio),
                    icon = Icons.Default.TrendingUp,
                    color = EduRachaColors.Accent
                )

                Divider(
                    modifier = Modifier
                        .height(80.dp)
                        .width(2.dp),
                    color = EduRachaColors.Border.copy(alpha = 0.5f)
                )

                EstadisticaItemDetalle(
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
fun EstadisticaItemDetalle(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PodioCard(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Secondary.copy(alpha = 0.12f),
                            Color.White
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏆",
                    fontSize = 36.sp
                )
                Column {
                    Text(
                        text = "Podio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Top 3 estudiantes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Podio visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Segundo lugar
                if (ranking.size >= 2) {
                    PodioItem(
                        posicion = 2,
                        estudiante = ranking[1],
                        esUsuarioActual = ranking[1].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 100.dp
                    )
                }

                // Primer lugar
                if (ranking.isNotEmpty()) {
                    PodioItem(
                        posicion = 1,
                        estudiante = ranking[0],
                        esUsuarioActual = ranking[0].id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        height = 130.dp
                    )
                }

                // Tercer lugar
                if (ranking.size >= 3) {
                    PodioItem(
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
fun PodioItem(
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(100.dp)
    ) {
        // Medalla
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorMedalla,
                            colorMedalla.copy(alpha = 0.8f)
                        )
                    )
                )
                .then(
                    if (esUsuarioActual) {
                        Modifier.scale(1.15f)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (posicion) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> ""
                },
                fontSize = 34.sp
            )
        }

        // Nombre
        Text(
            text = estudiante.nombre,
            fontSize = 13.sp,
            fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        // Valor con fondo
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colorMedalla.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, colorMedalla.copy(alpha = 0.3f))
        ) {
            Text(
                text = valor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorMedalla,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Pedestal mejorado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "#$posicion",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorMedalla
                )
                if (esUsuarioActual) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EduRachaColors.Primary
                    ) {
                        Text(
                            text = "TÚ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopRankingCardDetalle(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking
) {
    val colorMedalla = when (posicion) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.Accent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (esUsuarioActual) 10.dp else 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaColors.PrimaryContainer
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colorMedalla.copy(alpha = 0.15f),
                            colorMedalla.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colorMedalla,
                                colorMedalla.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (posicion) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> posicion.toString()
                    },
                    fontSize = if (posicion <= 3) 32.sp else 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = estudiante.nombre,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (tipoRanking) {
                        TipoRanking.EXPERIENCIA -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EduRachaColors.Accent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EduRachaColors.Accent.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = EduRachaColors.Accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${estudiante.experiencia} XP",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.Accent
                                    )
                                }
                            }
                        }
                        TipoRanking.RACHA -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EduRachaColors.StreakFire.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EduRachaColors.StreakFire.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = EduRachaColors.StreakFire,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${estudiante.diasConsecutivos} días",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.StreakFire
                                    )
                                }
                            }
                        }
                        TipoRanking.VIDAS -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EduRachaColors.Error.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EduRachaColors.Error.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = EduRachaColors.Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${estudiante.vidas} ❤️",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.Error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (esUsuarioActual) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EduRachaColors.Primary
                ) {
                    Text(
                        text = "Tú",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCardDetalle(
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (esUsuarioActual) 6.dp else 3.dp)
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
                    text = posicion.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Accent
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = estudiante.nombre,
                    fontSize = 15.sp,
                    fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))

                val valorMostrado = when (tipoRanking) {
                    TipoRanking.EXPERIENCIA -> "${estudiante.experiencia} XP"
                    TipoRanking.RACHA -> "${estudiante.diasConsecutivos} días"
                    TipoRanking.VIDAS -> "${estudiante.vidas} ❤️"
                }

                Text(
                    text = valorMostrado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.Accent
                )
            }

            if (esUsuarioActual) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Primary
                ) {
                    Text(
                        text = "Tú",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRankingDetalleView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(EduRachaColors.AccentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Leaderboard,
                contentDescription = null,
                modifier = Modifier.size(70.dp),
                tint = EduRachaColors.Accent
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Aún no hay datos de ranking",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Los estudiantes deben completar quizzes para aparecer en el ranking",
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ErrorViewRankingDetalle(error: String, onRetry: () -> Unit) {
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
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = EduRachaColors.Error
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Error al cargar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = error,
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))
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
fun LoadingRankingDetalle() {
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
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation),
                tint = EduRachaColors.Primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Cargando ranking...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary
            )
        }
    }
}