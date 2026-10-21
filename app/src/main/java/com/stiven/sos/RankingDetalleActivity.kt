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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.RankingViewModel
import com.stiven.sos.viewmodel.TipoRanking
import kotlin.math.roundToInt

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

        viewModel.cargarRankingCurso(cursoId, TipoRanking.EXPERIENCIA)

        setContent {
            EduRachaTheme {
                PantallaDetalleRanking(
                    cursoId = cursoId,
                    cursoTitulo = cursoTitulo,
                    viewModel = viewModel,
                    alVolverAtras = { finish() }
                )
            }
        }
    }
}

@Composable
fun PantallaDetalleRanking(
    cursoId: String,
    cursoTitulo: String,
    viewModel: RankingViewModel,
    alVolverAtras: () -> Unit
) {
    val estadoUI by viewModel.uiState.collectAsState()
    var pestanaSeleccionada by remember { mutableIntStateOf(0) }
    var estudianteSeleccionado by remember { mutableStateOf<RankingEstudiante?>(null) }
    var mostrarEstadisticas by remember { mutableStateOf(true) }
    // 0 = ranking, 1 = estadísticas del curso
    var vista by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(pestanaSeleccionada) {
        val tipo = when (pestanaSeleccionada) {
            0 -> TipoRanking.EXPERIENCIA
            1 -> TipoRanking.RACHA
            2 -> TipoRanking.VIDAS
            else -> TipoRanking.EXPERIENCIA
        }
        viewModel.cargarRankingCurso(cursoId, tipo)
    }


    EduRachaV2Container {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoRankingMejorado(
                cursoTitulo = cursoTitulo,
                alVolverAtras = alVolverAtras,
                pestanaSeleccionada = pestanaSeleccionada,
                alSeleccionarPestana = { pestanaSeleccionada = it },
                mostrarEstadisticas = mostrarEstadisticas,
                onToggleEstadisticas = { mostrarEstadisticas = !mostrarEstadisticas },
                vista = vista,
                alCambiarVista = { vista = it }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    vista == 1 -> EstadisticasCursoDocente(cursoId = cursoId)

                    estadoUI.isLoading -> {
                        EduRachaV2LoadingState(
                            message = "Cargando ranking...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    estadoUI.error != null -> {
                        EduRachaV2ErrorState(
                            title = "Error al cargar ranking",
                            message = estadoUI.error!!,
                            onRetry = {
                                val tipo = when (pestanaSeleccionada) {
                                    0 -> TipoRanking.EXPERIENCIA
                                    1 -> TipoRanking.RACHA
                                    2 -> TipoRanking.VIDAS
                                    else -> TipoRanking.EXPERIENCIA
                                }
                                viewModel.cargarRankingCurso(cursoId, tipo)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    estadoUI.rankingEstudiantes.isEmpty() -> {
                        EduRachaV2EmptyState(
                            icon = Icons.Outlined.EmojiEvents,
                            iconColor = EduRachaV2Colors.Secondary,
                            title = "Aún no hay ranking",
                            message = "Los estudiantes aparecerán aquí cuando completen actividades del curso",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        ContenidoRankingMejorado(
                            ranking = estadoUI.rankingEstudiantes,
                            usuarioActualId = viewModel.obtenerUsuarioActualId(),
                            tipoRanking = estadoUI.tipoRanking,
                            mostrarEstadisticas = mostrarEstadisticas,
                            onEstudianteClick = { estudiante ->
                                estudianteSeleccionado = estudiante
                            }
                        )
                    }
                }
            }
        }
    }
    // Perfil del estudiante en pantalla completa, encima del ranking
    if (estudianteSeleccionado != null) {
        DialogoPerfilEstudiante(
            estudiante = estudianteSeleccionado!!.let { it.copy(cursoId = it.cursoId ?: cursoId) },
            tipoRanking = estadoUI.tipoRanking,
            onDismiss = { estudianteSeleccionado = null }
        )
    }
}

@Composable
fun EncabezadoRankingMejorado(
    cursoTitulo: String,
    alVolverAtras: () -> Unit,
    pestanaSeleccionada: Int,
    alSeleccionarPestana: (Int) -> Unit,
    mostrarEstadisticas: Boolean,
    onToggleEstadisticas: () -> Unit,
    vista: Int = 0,
    alCambiarVista: (Int) -> Unit = {}
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    Column {
        com.stiven.sos.ui.theme.EncabezadoNexo(
            titulo = cursoTitulo,
            subtitulo = "Ranking y estadísticas",
            onVolver = alVolverAtras,
            tono = com.stiven.sos.ui.theme.TonoEncabezado.RACHA,
            acciones = {
                if (vista == 0) {
                    com.stiven.sos.ui.theme.AccionEncabezadoNexo(
                        if (mostrarEstadisticas) Icons.Outlined.BarChart else Icons.Outlined.ShowChart,
                        if (mostrarEstadisticas) "Ocultar resumen" else "Mostrar resumen",
                        onToggleEstadisticas,
                        com.stiven.sos.ui.theme.TonoEncabezado.RACHA
                    )
                }
            }
        )
        com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.stiven.sos.ui.theme.SegmentadoNexo(
                    opciones = listOf(
                        Triple(0, "Ranking", Icons.Outlined.EmojiEvents),
                        Triple(1, "Estadísticas", Icons.Outlined.Insights)
                    ),
                    seleccion = vista,
                    onCambio = alCambiarVista,
                    colores = { if (it == 0) p.racha else p.marca }
                )
                if (vista == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple(0, "Experiencia", p.racha),
                            Triple(1, "Racha", p.racha),
                            Triple(2, "Vidas", p.error)
                        ).forEach { (i, texto, color) ->
                            com.stiven.sos.ui.theme.FichaNexo(texto, pestanaSeleccionada == i, color, { alSeleccionarPestana(i) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconoTrofeoAnimado() {
    val transicionInfinita = rememberInfiniteTransition(label = "animacionTrofeo")

    val rotacion by transicionInfinita.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotacionTrofeo"
    )

    val escala by transicionInfinita.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaTrofeo"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                rotationZ = rotacion
                scaleX = escala
                scaleY = escala
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun PestanaRanking(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    seleccionada: Boolean,
    alHacerClic: () -> Unit,
    color: Color
) {
    val escala by animateFloatAsState(
        targetValue = if (seleccionada) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaPestana"
    )

    Surface(
        modifier = Modifier
            .scale(escala)
            .clickable(onClick = alHacerClic),
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionada) color.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (seleccionada) color else EduRachaV2Colors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = etiqueta,
                fontSize = 12.sp,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                color = if (seleccionada) color else EduRachaV2Colors.TextSecondary
            )
        }
    }
}

@Composable
fun ContenidoRankingMejorado(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking,
    mostrarEstadisticas: Boolean,
    onEstudianteClick: (RankingEstudiante) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Estadísticas generales (opcional)
        if (mostrarEstadisticas) {
            item {
                EstadisticasAvanzadas(
                    ranking = ranking,
                    tipoRanking = tipoRanking
                )
            }
        }

        // Podio Top 3
        if (ranking.isNotEmpty()) {
            item {
                // Mismo podio Nexo (islas 2.º, 1.º, 3.º) que ve el estudiante
                PodioV2(
                    ranking = ranking,
                    usuarioActualId = usuarioActualId,
                    tipoRanking = tipoRanking
                )
            }
        }

        // Gráficas de mejores
        if (ranking.size >= 3 && mostrarEstadisticas) {
            item {
                GraficasComparativas(
                    ranking = ranking,
                    tipoRanking = tipoRanking
                )
            }
        }

        // Lista de todos los estudiantes (toca uno para ver su perfil)
        if (ranking.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = EduRachaV2Colors.SoftGray
                    )
                    Text(
                        text = "Todos los participantes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.TextSecondary
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = EduRachaV2Colors.SoftGray
                    )
                }
            }

            itemsIndexed(ranking) { indice, estudiante ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = indice * 30
                        )
                    )
                ) {
                    TarjetaEstudianteInteractiva(
                        posicion = indice + 1,
                        estudiante = estudiante,
                        esUsuarioActual = estudiante.id == usuarioActualId,
                        tipoRanking = tipoRanking,
                        onClick = { onEstudianteClick(estudiante) }
                    )
                }
            }
        }
    }
}

@Composable
fun EstadisticasAvanzadas(
    ranking: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    val total = ranking.size
    val promedio = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> if (ranking.isNotEmpty()) ranking.map { it.experiencia }.average() else 0.0
        TipoRanking.RACHA -> if (ranking.isNotEmpty()) ranking.map { it.diasConsecutivos }.average() else 0.0
        TipoRanking.VIDAS -> if (ranking.isNotEmpty()) ranking.map { it.vidas }.average() else 0.0
    }
    val maximo = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.maxOfOrNull { it.experiencia } ?: 0
        TipoRanking.RACHA -> ranking.maxOfOrNull { it.diasConsecutivos } ?: 0
        TipoRanking.VIDAS -> ranking.maxOfOrNull { it.vidas } ?: 0
    }
    val minimo = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.minOfOrNull { it.experiencia } ?: 0
        TipoRanking.RACHA -> ranking.minOfOrNull { it.diasConsecutivos } ?: 0
        TipoRanking.VIDAS -> ranking.minOfOrNull { it.vidas } ?: 0
    }

    // Calcular desviación estándar
    val valores = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> ranking.map { it.experiencia.toDouble() }
        TipoRanking.RACHA -> ranking.map { it.diasConsecutivos.toDouble() }
        TipoRanking.VIDAS -> ranking.map { it.vidas.toDouble() }
    }
    val desviacion = if (valores.isNotEmpty()) {
        val media = valores.average()
        kotlin.math.sqrt(valores.map { (it - media) * (it - media) }.average())
    } else 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EduRachaColors.Surface,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaV2Colors.Primary.copy(alpha = 0.08f),
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduRachaV2Gradients.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Estadísticas Avanzadas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaV2Colors.TextPrimary
                    )
                    Text(
                        text = "Análisis detallado del curso",
                        fontSize = 13.sp,
                        color = EduRachaV2Colors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primera fila de estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ItemEstadisticaCompacta(
                    icono = Icons.Outlined.People,
                    valor = total.toString(),
                    etiqueta = "Total",
                    color = EduRachaV2Colors.Primary,
                    modifier = Modifier.weight(1f)
                )

                ItemEstadisticaCompacta(
                    icono = Icons.Outlined.TrendingUp,
                    valor = String.format("%.1f", promedio),
                    etiqueta = "Promedio",
                    color = EduRachaV2Colors.Accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segunda fila de estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ItemEstadisticaCompacta(
                    icono = Icons.Outlined.ArrowUpward,
                    valor = maximo.toString(),
                    etiqueta = "Máximo",
                    color = EduRachaV2Colors.Success,
                    modifier = Modifier.weight(1f)
                )

                ItemEstadisticaCompacta(
                    icono = Icons.Outlined.ArrowDownward,
                    valor = minimo.toString(),
                    etiqueta = "Mínimo",
                    color = EduRachaV2Colors.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Desviación estándar
            ItemEstadisticaCompacta(
                icono = Icons.Outlined.ShowChart,
                valor = String.format("±%.1f", desviacion),
                etiqueta = "Desviación Estándar",
                color = EduRachaV2Colors.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ItemEstadisticaCompacta(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    valor: String,
    etiqueta: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = valor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = etiqueta,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaV2Colors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun PodioMejorado(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking,
    onEstudianteClick: (RankingEstudiante) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = EduRachaColors.Surface,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaV2Colors.Accent.copy(alpha = 0.12f),
                            Color.White
                        )
                    )
                )
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
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = com.stiven.sos.ui.theme.LocalNexo.current.racha,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = "Podio del Curso",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaV2Colors.TextPrimary
                        )
                        Text(
                            text = "Top ${minOf(3, ranking.size)} estudiantes",
                            fontSize = 14.sp,
                            color = EduRachaV2Colors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (ranking.size >= 2) {
                        ColumnaPodioClickeable(
                            posicion = 2,
                            estudiante = ranking[1],
                            esUsuarioActual = ranking[1].id == usuarioActualId,
                            tipoRanking = tipoRanking,
                            altura = 120.dp,
                            onClick = { onEstudianteClick(ranking[1]) }
                        )
                    }

                    if (ranking.isNotEmpty()) {
                        ColumnaPodioClickeable(
                            posicion = 1,
                            estudiante = ranking[0],
                            esUsuarioActual = ranking[0].id == usuarioActualId,
                            tipoRanking = tipoRanking,
                            altura = 160.dp,
                            onClick = { onEstudianteClick(ranking[0]) }
                        )
                    }

                    if (ranking.size >= 3) {
                        ColumnaPodioClickeable(
                            posicion = 3,
                            estudiante = ranking[2],
                            esUsuarioActual = ranking[2].id == usuarioActualId,
                            tipoRanking = tipoRanking,
                            altura = 100.dp,
                            onClick = { onEstudianteClick(ranking[2]) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnaPodioClickeable(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking,
    altura: Dp,
    onClick: () -> Unit
) {
    val (colorMedalla, emoji) = when (posicion) {
        1 -> Pair(Color(0xFFFFD700), "1")
        2 -> Pair(Color(0xFFC0C0C0), "2")
        3 -> Pair(Color(0xFFCD7F32), "3")
        else -> Pair(EduRachaV2Colors.TextSecondary, "")
    }

    val valor = when (tipoRanking) {
        TipoRanking.EXPERIENCIA -> estudiante.experiencia
        TipoRanking.RACHA -> estudiante.diasConsecutivos
        TipoRanking.VIDAS -> estudiante.vidas
    }

    val transicionInfinita = rememberInfiniteTransition(label = "animacionPodio")
    val escala by transicionInfinita.animateFloat(
        initialValue = 1f,
        targetValue = if (posicion == 1) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaPodio"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(if (posicion == 1) 72.dp else 60.dp)
                .scale(escala)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorMedalla,
                            colorMedalla.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = if (posicion == 1) 32.sp else 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            EduRachaV2Colors.Secondary,
                            EduRachaV2Colors.SecondaryDark
                        )
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = estudiante.nombre.firstOrNull()?.uppercase() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = estudiante.nombre,
            fontSize = 13.sp,
            fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
            color = EduRachaV2Colors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colorMedalla.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, colorMedalla.copy(alpha = 0.3f))
        ) {
            Text(
                text = valor.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colorMedalla,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(altura)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    EduRachaV2Colors.Primary,
                                    EduRachaV2Colors.PrimaryDark
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "#$posicion",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colorMedalla
                )
                if (esUsuarioActual) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaV2Colors.Primary
                    ) {
                        Text(
                            text = "TÚ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GraficasComparativas(
    ranking: List<RankingEstudiante>,
    tipoRanking: TipoRanking
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = EduRachaColors.Surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaV2Colors.Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = EduRachaV2Colors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = when (tipoRanking) {
                        TipoRanking.EXPERIENCIA -> "Top Experiencia"
                        TipoRanking.RACHA -> "Mejores Rachas"
                        TipoRanking.VIDAS -> "Mayor Energía"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaV2Colors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ranking.take(5).forEachIndexed { indice, estudiante ->
                val valor = when (tipoRanking) {
                    TipoRanking.EXPERIENCIA -> estudiante.experiencia
                    TipoRanking.RACHA -> estudiante.diasConsecutivos
                    TipoRanking.VIDAS -> estudiante.vidas
                }
                val maxValor = when (tipoRanking) {
                    TipoRanking.EXPERIENCIA -> ranking.maxOfOrNull { it.experiencia } ?: 1
                    TipoRanking.RACHA -> ranking.maxOfOrNull { it.diasConsecutivos } ?: 1
                    TipoRanking.VIDAS -> ranking.maxOfOrNull { it.vidas } ?: 1
                }

                BarraGraficaMejorada(
                    nombre = estudiante.nombre,
                    valor = valor,
                    maxValor = maxValor,
                    color = when (indice) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFFC0C0C0)
                        2 -> Color(0xFFCD7F32)
                        3 -> EduRachaV2Colors.Primary
                        else -> EduRachaV2Colors.Secondary
                    },
                    posicion = indice + 1
                )

                if (indice < 4) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun BarraGraficaMejorada(
    nombre: String,
    valor: Int,
    maxValor: Int,
    color: Color,
    posicion: Int
) {
    val progreso = if (maxValor > 0) valor.toFloat() / maxValor.toFloat() else 0f
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progresoBarraGrafica"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$posicion",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Text(
                    text = nombre,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaV2Colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = valor.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(EduRachaV2Colors.Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progresoAnimado)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.7f))
                        )
                    )
            )
        }
    }
}

@Composable
fun TarjetaEstudianteInteractiva(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking,
    onClick: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val medalla = when (posicion) { 1 -> Color(0xFFFFC14D); 2 -> Color(0xFFB8C4D6); 3 -> Color(0xFFE0915A); else -> null }
    com.stiven.sos.ui.theme.TarjetaNexo(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        acento = medalla ?: if (esUsuarioActual) p.marca else null,
        padding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background((medalla ?: p.superficieAlt).copy(alpha = if (medalla != null) 0.9f else 1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$posicion", color = if (medalla != null) Color(0xFF1B2433) else p.textoSuave, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            com.stiven.sos.ui.theme.AvatarNexo(estudiante.nombre, p.ia, tamano = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(estudiante.nombre, color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Toca para ver su progreso", color = p.textoTenue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = p.textoSuave)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricaRanking(Icons.Outlined.Bolt, "${estudiante.experiencia}", "XP", p.racha, tipoRanking == TipoRanking.EXPERIENCIA, Modifier.weight(1f))
            MetricaRanking(Icons.Outlined.LocalFireDepartment, "${estudiante.diasConsecutivos}", "Racha", p.racha, tipoRanking == TipoRanking.RACHA, Modifier.weight(1f))
            MetricaRanking(Icons.Outlined.Favorite, "${estudiante.vidas}", "Vidas", p.error, tipoRanking == TipoRanking.VIDAS, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricaRanking(icono: androidx.compose.ui.graphics.vector.ImageVector, valor: String, etiqueta: String, color: Color, destacada: Boolean, modifier: Modifier) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (destacada) color.copy(alpha = 0.16f) else p.superficieAlt)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(valor, color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Text(etiqueta, color = p.textoSuave, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

/**
 * Perfil del estudiante en pantalla completa (antes una ventana emergente con botón cerrar gris).
 * Suma los datos del ranking con los de las estadísticas del curso (promedio, quizzes, temas, actividad).
 */
@Composable
fun DialogoPerfilEstudiante(
    estudiante: RankingEstudiante,
    tipoRanking: TipoRanking,
    onDismiss: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val estVm: EstadisticasCursoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val estUi by estVm.ui.collectAsState()
    LaunchedEffect(estudiante.cursoId) { estudiante.cursoId?.let { estVm.cargar(it) } }
    val detalle = estUi.datos?.estudiantes?.firstOrNull { it.estudianteId == estudiante.id }

    com.stiven.sos.ui.theme.PantallaSuperpuestaNexo(onAtras = onDismiss) {
        com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
            Column(Modifier.fillMaxSize()) {
                com.stiven.sos.ui.theme.EncabezadoNexo(
                    titulo = estudiante.nombre,
                    subtitulo = "Perfil del estudiante",
                    onVolver = onDismiss,
                    tono = com.stiven.sos.ui.theme.TonoEncabezado.RACHA
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.stiven.sos.ui.theme.AvatarNexo(estudiante.nombre, p.ia, tamano = 72.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(estudiante.nombre, color = p.texto, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                    if (detalle?.enRiesgo == true) {
                                        com.stiven.sos.ui.theme.EtiquetaEstadoNexo("Necesita apoyo", p.error)
                                    } else if (detalle != null) {
                                        com.stiven.sos.ui.theme.EtiquetaEstadoNexo("Va bien", p.accion)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Bolt, "${estudiante.experiencia}", "XP", p.racha, Modifier.weight(1f))
                                com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.LocalFireDepartment, "${estudiante.diasConsecutivos}", "Días de racha", p.racha, Modifier.weight(1f))
                                com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Favorite, "${estudiante.vidas}", "Vidas", p.error, Modifier.weight(1f))
                            }
                            when {
                                detalle != null -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Quiz, "${detalle.quizzes}", "Quizzes", p.ia, Modifier.weight(1f))
                                        com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.TrackChanges, "${detalle.promedio}%", "Aciertos", p.marca, Modifier.weight(1f))
                                        com.stiven.sos.ui.theme.StatNexo(Icons.Outlined.Verified, "${detalle.temasAprobados}", "Temas aprob.", p.accion, Modifier.weight(1f))
                                    }
                                    com.stiven.sos.ui.theme.TarjetaNexo(Modifier.fillMaxWidth()) {
                                        Text("Promedio de aciertos", color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                        Spacer(Modifier.height(8.dp))
                                        com.stiven.sos.ui.theme.BarraNexo(detalle.promedio / 100f, color = if (detalle.promedio >= 80) p.accion else p.marca, alto = 12.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(if (detalle.promedio >= 80) "Aprueba con holgura (meta: 80%)." else "Le faltan ${80 - detalle.promedio} puntos para la meta de 80%.",
                                            color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    com.stiven.sos.ui.theme.NotaNexo(
                                        when {
                                            detalle.ultimaActividad == null -> "Todavía no ha presentado quizzes."
                                            detalle.diasSinActividad == 0 -> "Estuvo activo hoy (${detalle.ultimaActividad})."
                                            else -> "Último quiz hace ${detalle.diasSinActividad} días (${detalle.ultimaActividad})."
                                        },
                                        p.marca, Icons.Outlined.Event, titulo = "Actividad"
                                    )
                                    detalle.motivoRiesgo?.let {
                                        com.stiven.sos.ui.theme.NotaNexo(it, p.error, Icons.Outlined.ReportProblem, titulo = "Por qué necesita apoyo")
                                    }
                                }
                                estUi.cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = p.racha)
                                }
                                else -> com.stiven.sos.ui.theme.NotaNexo(
                                    estUi.error ?: "No hay más datos de este estudiante todavía.", p.textoSuave, Icons.Outlined.Info
                                )
                            }
                        }
                    }
                }
                Surface(color = p.superficie, shadowElevation = 8.dp) {
                    com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        com.stiven.sos.ui.theme.BotonNexo("Cerrar", onDismiss, tipo = com.stiven.sos.ui.theme.TipoBoton.MARCA, alto = 52.dp, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun ItemPerfilEstudiante(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valor: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = EduRachaV2Colors.TextPrimary
                )
            }
            Text(
                text = valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}