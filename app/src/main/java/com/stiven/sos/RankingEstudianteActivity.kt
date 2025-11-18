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
import kotlin.math.min

/**
 * Activity principal que muestra el sistema de ranking de estudiantes
 * Mantiene toda la lógica original pero con interfaz completamente rediseñada
 */
class RankingEstudianteActivity : ComponentActivity() {

    private val viewModel: RankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carga los cursos en los que el estudiante está inscrito
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

/**
 * Pantalla principal del ranking
 * Muestra lista de cursos o detalle de ranking según selección
 */
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
                titulo = if (cursoSeleccionado == null) "Ranking General" else cursoSeleccionado!!.titulo,
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
                .background(EduRachaColors.Background)
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

/**
 * Barra superior con gradiente institucional
 * Muestra título y botón de navegación
 */
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
            .shadow(4.dp),
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
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón de retroceso con diseño circular
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.25f),
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

                // Título con icono de trofeo
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = EduRachaColors.Secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Lista de cursos disponibles para ver ranking
 * Diseño de tarjetas con gradientes y espaciado adecuado
 */
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
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Encabezado informativo con contador de cursos
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        EduRachaColors.Primary.copy(alpha = 0.08f),
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
                                    text = "Mis Cursos Inscritos",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selecciona un curso para ver tu posición en el ranking",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )
                            }

                            // Badge con número de cursos
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                EduRachaColors.Secondary,
                                                EduRachaColors.SecondaryLight
                                            )
                                        )
                                    )
                                    .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = cursos.size.toString(),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "cursos",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lista de cursos con diseño de tarjetas mejorado
            items(cursos) { curso ->
                CursoRankingCard(
                    curso = curso,
                    onClick = { onCursoClick(curso) }
                )
            }
        }
    }
}

/**
 * Tarjeta individual de curso con animación de press
 * Diseño limpio con iconografía y código de curso visible
 */
@Composable
fun CursoRankingCard(
    curso: Curso,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono del curso con gradiente
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Información del curso
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = curso.titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    lineHeight = 22.sp
                )

                // Código del curso con estilo badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.AccentContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = curso.codigo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EduRachaColors.Accent
                        )
                    }
                }
            }

            // Flecha indicadora
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = EduRachaColors.Accent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Vista principal del detalle de ranking con podio y lista
 * Organización vertical clara con secciones bien definidas
 */
@Composable
fun RankingDetalleCurso(
    cursoId: String,
    tipoRankingSeleccionado: TipoRanking,
    rankingEstudiantes: List<RankingEstudiante>,
    usuarioActualId: String?,
    onTipoRankingChange: (TipoRanking) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Selector de tipo de ranking
        item {
            RankingTypeSelector(
                tipoSeleccionado = tipoRankingSeleccionado,
                onTipoChange = onTipoRankingChange
            )
        }

        // Estadísticas generales con gráfica
        item {
            RankingEstadisticasConGrafica(
                rankingEstudiantes = rankingEstudiantes,
                tipoRanking = tipoRankingSeleccionado
            )
        }

        if (rankingEstudiantes.isEmpty()) {
            item {
                EmptyRankingState()
            }
        } else {
            // Podio para los 3 primeros lugares
            if (rankingEstudiantes.size >= 3) {
                item {
                    PodioTopTres(
                        estudiantes = rankingEstudiantes.take(3),
                        tipoRanking = tipoRankingSeleccionado,
                        usuarioActualId = usuarioActualId
                    )
                }
            }

            // Lista del resto de estudiantes (después del top 3)
            if (rankingEstudiantes.size > 3) {
                item {
                    Text(
                        text = "Resto del Ranking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                itemsIndexed(rankingEstudiantes.drop(3)) { index, estudiante ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 40
                            )
                        )
                    ) {
                        RankingEstudianteItem(
                            posicion = index + 4,
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

/**
 * Estadísticas con gráfica de barras horizontal
 * Muestra distribución de rendimiento en el curso
 */
@Composable
fun RankingEstadisticasConGrafica(
    rankingEstudiantes: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    if (rankingEstudiantes.isEmpty()) return

    val total = rankingEstudiantes.size
    val valores = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> rankingEstudiantes.map { it.experiencia }
        TipoRanking.RACHA -> rankingEstudiantes.map { it.diasConsecutivos }
        TipoRanking.VIDAS -> rankingEstudiantes.map { it.vidas }
    }

    val promedio = valores.average().toInt()
    val maximo = valores.maxOrNull() ?: 0
    val minimo = valores.minOrNull() ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Título de sección
            Text(
                text = "Estadísticas del Curso",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary
            )

            // Métricas principales en fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricaCompacta(
                    label = "Total",
                    valor = total.toString(),
                    icon = Icons.Default.People,
                    color = EduRachaColors.Primary
                )

                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    thickness = 1.dp,
                    color = EduRachaColors.Border
                )

                MetricaCompacta(
                    label = "Promedio",
                    valor = promedio.toString(),
                    icon = Icons.Default.BarChart,
                    color = EduRachaColors.Secondary
                )

                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    thickness = 1.dp,
                    color = EduRachaColors.Border
                )

                MetricaCompacta(
                    label = "Máximo",
                    valor = maximo.toString(),
                    icon = Icons.Default.TrendingUp,
                    color = EduRachaColors.Success
                )
            }

            Divider(color = EduRachaColors.Border.copy(alpha = 0.5f))

            // Gráfica de distribución
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Distribución de Rendimiento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.TextSecondary
                )

                GraficaDistribucion(
                    valores = valores,
                    tipoRanking = tipoRanking
                )
            }
        }
    }
}

/**
 * Métrica compacta para estadísticas
 * Diseño vertical con icono, valor y etiqueta
 */
@Composable
fun MetricaCompacta(
    label: String,
    valor: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = valor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = EduRachaColors.TextSecondary
        )
    }
}

/**
 * Gráfica de distribución de valores
 * Muestra rangos de rendimiento (bajo, medio, alto) con barras horizontales
 */
@Composable
fun GraficaDistribucion(
    valores: List<Int>,
    tipoRanking: TipoRanking
) {
    if (valores.isEmpty()) return

    val maximo = valores.maxOrNull() ?: 0
    if (maximo == 0) return

    // Dividir en 3 rangos: bajo (0-33%), medio (33-66%), alto (66-100%)
    val rangoAlto = valores.count { it >= maximo * 0.66 }
    val rangoMedio = valores.count { it in (maximo * 0.33).toInt() until (maximo * 0.66).toInt() }
    val rangoBajo = valores.count { it < maximo * 0.33 }
    val total = valores.size

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barra para rendimiento alto
        BarraDistribucion(
            label = "Alto Rendimiento",
            cantidad = rangoAlto,
            total = total,
            color = EduRachaColors.Success
        )

        // Barra para rendimiento medio
        BarraDistribucion(
            label = "Rendimiento Medio",
            cantidad = rangoMedio,
            total = total,
            color = EduRachaColors.Secondary
        )

        // Barra para rendimiento bajo
        BarraDistribucion(
            label = "Necesita Mejorar",
            cantidad = rangoBajo,
            total = total,
            color = EduRachaColors.Warning
        )
    }
}

/**
 * Barra individual de distribución
 * Muestra porcentaje y cantidad con barra de progreso animada
 */
@Composable
fun BarraDistribucion(
    label: String,
    cantidad: Int,
    total: Int,
    color: Color
) {
    val porcentaje = if (total > 0) (cantidad.toFloat() / total * 100).toInt() else 0
    val progress = if (total > 0) cantidad.toFloat() / total else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "progress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Etiqueta y valores
        Column(
            modifier = Modifier.width(120.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "$cantidad estudiantes",
                fontSize = 10.sp,
                color = EduRachaColors.TextSecondary
            )
        }

        // Barra de progreso
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }

        // Porcentaje
        Text(
            text = "$porcentaje%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Podio animado para los 3 primeros lugares
 * Diseño tipo olimpiadas con alturas diferentes
 * Posición central (1°) más alta que laterales (2° y 3°)
 */
@Composable
fun PodioTopTres(
    estudiantes: List<RankingEstudiante>,
    tipoRanking: TipoRanking,
    usuarioActualId: String?
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Título del podio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = EduRachaColors.Secondary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Top 3 del Ranking",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Primary
                )
            }

            // Podio con posiciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Segundo lugar (izquierda)
                if (estudiantes.size >= 2) {
                    PosicionPodio(
                        estudiante = estudiantes[1],
                        posicion = 2,
                        tipoRanking = tipoRanking,
                        esUsuarioActual = estudiantes[1].id == usuarioActualId,
                        alturaPodio = 180.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Primer lugar (centro - más alto)
                PosicionPodio(
                    estudiante = estudiantes[0],
                    posicion = 1,
                    tipoRanking = tipoRanking,
                    esUsuarioActual = estudiantes[0].id == usuarioActualId,
                    alturaPodio = 240.dp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Tercer lugar (derecha)
                if (estudiantes.size >= 3) {
                    PosicionPodio(
                        estudiante = estudiantes[2],
                        posicion = 3,
                        tipoRanking = tipoRanking,
                        esUsuarioActual = estudiantes[2].id == usuarioActualId,
                        alturaPodio = 140.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Posición individual en el podio
 * Animación de aparición desde abajo hacia arriba
 */
@Composable
fun PosicionPodio(
    estudiante: RankingEstudiante,
    posicion: Int,
    tipoRanking: TipoRanking,
    esUsuarioActual: Boolean,
    alturaPodio: Dp,
    modifier: Modifier = Modifier
) {
    // Color según posición
    val colorPosicion = when (posicion) {
        1 -> EduRachaColors.RankingGold
        2 -> EduRachaColors.RankingSilver
        3 -> EduRachaColors.RankingBronze
        else -> EduRachaColors.Accent
    }

    // Valor según tipo de ranking
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
            unidad = "vidas",
            icon = Icons.Default.Favorite,
            color = EduRachaColors.Error
        )
    }

    // Animación de entrada
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Column(
        modifier = modifier.offset(y = offsetY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Avatar del estudiante
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorPosicion,
                            colorPosicion.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    width = if (esUsuarioActual) 3.dp else 2.dp,
                    color = if (esUsuarioActual) EduRachaColors.Accent else Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = estudiante.nombre.first().toString().uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nombre del estudiante
        Text(
            text = estudiante.nombre.split(" ").firstOrNull() ?: estudiante.nombre,
            fontSize = 13.sp,
            fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Badge "Tú" si es el usuario actual
        if (esUsuarioActual) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EduRachaColors.Accent
            ) {
                Text(
                    text = "Tú",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Base del podio con información
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(alturaPodio)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorPosicion.copy(alpha = 0.9f),
                            colorPosicion
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Número de posición
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = posicion.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Valor del ranking
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = info.valor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = info.unidad,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Selector de tipo de ranking (Experiencia, Racha, Vidas)
 * Diseño de chips con animación de selección
 */
@Composable
fun RankingTypeSelector(
    tipoSeleccionado: TipoRanking,
    onTipoChange: (TipoRanking) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Criterio de Ranking",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.Primary
        )

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
}

/**
 * Chip individual para tipo de ranking
 * Cambio visual claro entre seleccionado y no seleccionado
 */
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
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        border = if (!isSelected) BorderStroke(1.5.dp, color.copy(alpha = 0.4f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

/**
 * Data class para información de ranking UI
 */
data class RankingUI(
    val valor: String,
    val unidad: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Item individual de estudiante en el ranking (después del top 3)
 * Diseño compacto con información esencial
 */
@Composable
fun RankingEstudianteItem(
    posicion: Int,
    estudiante: RankingEstudiante,
    tipoRanking: TipoRanking,
    esUsuarioActual: Boolean
) {
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
            unidad = "vidas",
            icon = Icons.Default.Favorite,
            color = EduRachaColors.Error
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaColors.Accent.copy(alpha = 0.08f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (esUsuarioActual)
            BorderStroke(2.dp, EduRachaColors.Accent)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Número de posición
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = posicion.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Accent
                )
            }

            // Información del estudiante
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = estudiante.nombre,
                        fontSize = 15.sp,
                        fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
                        color = EduRachaColors.TextPrimary
                    )

                    if (esUsuarioActual) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduRachaColors.Accent
                        ) {
                            Text(
                                text = "Tú",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Valor del ranking
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = info.color,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = info.valor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = info.color
                    )
                    Text(
                        text = info.unidad,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Estado vacío cuando no hay cursos inscritos
 */
@Composable
fun EmptyCursosState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = EduRachaColors.Accent
                )
            }
            Text(
                text = "Sin cursos inscritos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Únete a un curso para ver tu posición en el ranking y competir con otros estudiantes",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Estado vacío cuando no hay estudiantes en el ranking
 */
@Composable
fun EmptyRankingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = EduRachaColors.Secondary
                )
            }
            Text(
                text = "Ranking vacío",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Completa quizzes y actividades para aparecer en el ranking",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Indicador de carga con animación
 */
@Composable
fun LoadingRanking() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    .background(EduRachaColors.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(rotation),
                    tint = EduRachaColors.Accent
                )
            }

            Text(
                text = "Cargando ranking...",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

/**
 * Estado de error con opción de reintentar
 */
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
            modifier = Modifier.padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = EduRachaColors.Error
                )
            }
            Text(
                text = "Error al cargar",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = mensaje,
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Accent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Reintentar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}