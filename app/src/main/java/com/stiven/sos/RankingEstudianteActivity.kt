package com.stiven.sos

import androidx.compose.material.icons.filled.*
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

    FondoNexo(conCuadricula = false) {
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

// ============================================================================
// LISTA DE CURSOS (elige de qué curso ver la liga)
// ============================================================================

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
    val p = LocalNexo.current
    val acentos = listOf(p.marca, p.ia, p.accion, p.racha, p.error)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            EncabezadoNexo(titulo = "Ligas", subtitulo = "Ranking", onVolver = onNavigateBack)
        }
        item {
            GloboRobi(
                titulo = "ROBI",
                mensaje = "Elige un curso para ver tu posición. Tienes ${cursos.size} ${if (cursos.size == 1) "liga" else "ligas"} activas.",
                expresion = ExpresionRobi.FELIZ,
                tamanoRobi = 72.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        itemsIndexed(cursos) { index, curso ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 70L)
                visible = true
            }
            val aparicion by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(350),
                label = "aparicion"
            )
            FilaNexo(
                icono = Icons.Outlined.Leaderboard,
                color = acentos[index % acentos.size],
                titulo = curso.titulo,
                subtitulo = curso.codigo,
                onClick = { onCursoClick(curso) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .graphicsLayer {
                        alpha = aparicion
                        translationY = (1f - aparicion) * 40f
                    },
                extremo = {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = p.textoSuave)
                }
            )
        }
    }
}

// ============================================================================
// DETALLE DEL RANKING DE UN CURSO
// ============================================================================

private fun valorRanking(estudiante: RankingEstudiante, tipo: TipoRanking): Int = when (tipo) {
    TipoRanking.EXPERIENCIA -> estudiante.experiencia
    TipoRanking.RACHA -> estudiante.diasConsecutivos
    TipoRanking.VIDAS -> estudiante.vidas
}

private fun unidadRanking(tipo: TipoRanking): String = when (tipo) {
    TipoRanking.EXPERIENCIA -> "XP"
    TipoRanking.RACHA -> "días"
    TipoRanking.VIDAS -> "vidas"
}

@Composable
private fun iconoRanking(tipo: TipoRanking): ImageVector = when (tipo) {
    TipoRanking.EXPERIENCIA -> Icons.Outlined.Bolt
    TipoRanking.RACHA -> Icons.Outlined.LocalFireDepartment
    TipoRanking.VIDAS -> Icons.Outlined.FavoriteBorder
}

@Composable
private fun colorRanking(tipo: TipoRanking): Color {
    val p = LocalNexo.current
    return when (tipo) {
        TipoRanking.EXPERIENCIA -> p.ia
        TipoRanking.RACHA -> p.racha
        TipoRanking.VIDAS -> p.error
    }
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
    val p = LocalNexo.current
    val miPosicion = rankingEstudiantes.indexOfFirst { it.id == usuarioActualId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            EncabezadoNexo(titulo = curso.titulo, subtitulo = "Liga · ${curso.codigo}", onVolver = onNavigateBack)
        }

        item {
            TabsRankingV2(
                tipoSeleccionado = tipoRankingSeleccionado,
                onTipoChange = onTipoRankingChange
            )
        }

        when {
            isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RobiNeo(tamano = 100.dp, expresion = ExpresionRobi.PIENSA)
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando la liga...", color = p.textoSuave, fontWeight = FontWeight.Bold)
                    }
                }
            }

            rankingEstudiantes.isEmpty() -> item {
                EstadoVacioNexo(
                    titulo = "Liga vacía",
                    mensaje = "Completa quizzes para aparecer en el ranking y competir con tus compañeros."
                )
            }

            else -> {
                // Podio con islas flotantes (1 a 3 estudiantes)
                item {
                    PodioV2(
                        ranking = rankingEstudiantes.take(3),
                        usuarioActualId = usuarioActualId,
                        tipoRanking = tipoRankingSeleccionado
                    )
                }

                if (miPosicion >= 0) {
                    item {
                        val yo = rankingEstudiantes[miPosicion]
                        TarjetaNexo(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            acento = p.marca
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${miPosicion + 1}", color = p.marcaTexto, fontSize = 26.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Tu posición", color = p.texto, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        when {
                                            miPosicion == 0 -> "Lideras la liga. ¡Defiende el primer lugar!"
                                            else -> {
                                                val diferencia = valorRanking(rankingEstudiantes[miPosicion - 1], tipoRankingSeleccionado) -
                                                        valorRanking(yo, tipoRankingSeleccionado)
                                                "Te faltan ${diferencia.coerceAtLeast(0)} ${unidadRanking(tipoRankingSeleccionado)} para subir un puesto"
                                            }
                                        },
                                        color = p.textoSuave,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (rankingEstudiantes.size > 3) {
                    item { DividerRanking() }
                    itemsIndexed(rankingEstudiantes.drop(3)) { index, estudiante ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 50L)
                            visible = true
                        }
                        val aparicion by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(300),
                            label = "aparicion"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .graphicsLayer {
                                    alpha = aparicion
                                    translationY = (1f - aparicion) * 30f
                                }
                        ) {
                            RankingItemV2(
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
fun TabsRankingV2(
    tipoSeleccionado: TipoRanking,
    onTipoChange: (TipoRanking) -> Unit
) {
    val p = LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.superficie)
            .border(1.dp, p.borde, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            TipoRanking.EXPERIENCIA to "XP",
            TipoRanking.RACHA to "Racha",
            TipoRanking.VIDAS to "Vidas"
        ).forEach { (tipo, texto) ->
            val activo = tipo == tipoSeleccionado
            val color = colorRanking(tipo)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activo) color else Color.Transparent)
                    .clickable { onTipoChange(tipo) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(iconoRanking(tipo), contentDescription = null, tint = if (activo) p.sobreAccion else color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(texto, color = if (activo) p.sobreAccion else p.textoSuave, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Podio en escalones sobre islas flotantes: 2.º a la izquierda, 1.º al centro (más alto), 3.º a la derecha.
 * Funciona con 1, 2 o 3 estudiantes.
 */
@Composable
fun PodioV2(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?,
    tipoRanking: TipoRanking
) {
    val p = LocalNexo.current
    val orden = listOf(1, 0, 2) // posiciones visuales: 2.º, 1.º, 3.º
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        orden.forEach { idx ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                val estudiante = ranking.getOrNull(idx)
                if (estudiante != null) {
                    PosicionPodioV2(
                        estudiante = estudiante,
                        posicion = idx + 1,
                        esUsuarioActual = estudiante.id == usuarioActualId,
                        tipoRanking = tipoRanking
                    )
                }
            }
        }
    }
}

@Composable
fun PosicionPodioV2(
    estudiante: RankingEstudiante,
    posicion: Int,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking
) {
    val p = LocalNexo.current
    val (colorMedalla, alturaEscalon) = when (posicion) {
        1 -> p.racha to 128.dp
        2 -> p.marca to 96.dp
        else -> p.ia to 72.dp
    }
    val subida = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((3 - posicion) * 150L)
        subida.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { translationY = (1f - subida.value) * 120f; alpha = subida.value.coerceIn(0f, 1f) }
    ) {
        if (posicion == 1) {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = "Primer lugar", tint = p.racha, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(2.dp))
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarNexo(
                nombre = estudiante.nombre,
                color = colorMedalla,
                tamano = if (posicion == 1) 64.dp else 54.dp,
                modifier = Modifier.border(3.dp, if (esUsuarioActual) p.accion else p.superficie, CircleShape)
            )
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colorMedalla)
                    .border(2.dp, p.superficie, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$posicion", color = p.sobreAccion, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (esUsuarioActual) "Tú" else estudiante.nombre.split(" ").first(),
            color = if (esUsuarioActual) p.accionTexto else p.texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${valorRanking(estudiante, tipoRanking)} ${unidadRanking(tipoRanking)}",
            color = p.textoSuave,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        // Escalón: isla flotante + bloque
        Box(contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(alturaEscalon)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        Brush.verticalGradient(listOf(colorMedalla.copy(alpha = 0.55f), colorMedalla.copy(alpha = 0.08f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("$posicion", color = p.texto.copy(alpha = 0.85f), fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            IslaFlotante(
                modifier = Modifier.fillMaxWidth().height(34.dp),
                tipo = if (posicion == 1) TipoIsla.CONQUISTADA else TipoIsla.ACTIVA,
                fase = posicion
            )
        }
    }
}

@Composable
fun DividerRanking() {
    val p = LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(p.borde))
        Text(
            "RESTO DE LA LIGA",
            color = p.textoSuave,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Box(Modifier.weight(1f).height(1.dp).background(p.borde))
    }
}

@Composable
fun RankingItemV2(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean,
    tipoRanking: TipoRanking
) {
    val p = LocalNexo.current
    val color = colorRanking(tipoRanking)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (esUsuarioActual) p.marca.copy(alpha = 0.12f) else p.superficie)
            .border(if (esUsuarioActual) 2.dp else 1.dp, if (esUsuarioActual) p.marca else p.borde, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "$posicion",
            color = p.textoSuave,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = TextAlign.Center
        )
        AvatarNexo(nombre = estudiante.nombre, color = if (esUsuarioActual) p.marcaSombra else p.bloqueadoSombra, tamano = 40.dp)
        Text(
            if (esUsuarioActual) "${estudiante.nombre} (tú)" else estudiante.nombre,
            color = p.texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        BadgeValorRanking(
            valor = valorRanking(estudiante, tipoRanking),
            unidad = unidadRanking(tipoRanking),
            icono = iconoRanking(tipoRanking),
            color = color
        )
    }
}

@Composable
fun BadgeValorRanking(
    valor: Int,
    unidad: String,
    icono: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text("$valor", color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(unidad, color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================================================================
// ESTADOS
// ============================================================================

@Composable
fun LoadingEstudianteView() {
    CargandoNexo(titulo = "Cargando tus ligas", mensaje = "Preparando los rankings de tus cursos")
}

@Composable
fun ErrorEstudianteView(
    error: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoNexo(titulo = "Ligas", onVolver = onNavigateBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacioNexo(
                titulo = "No pudimos cargar el ranking",
                mensaje = error,
                expresion = ExpresionRobi.TRISTE,
                textoAccion = "Reintentar",
                onAccion = onRetry
            )
        }
    }
}

@Composable
fun EmptyCursosV2(onNavigateBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoNexo(titulo = "Ligas", onVolver = onNavigateBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacioNexo(
                titulo = "Aún no estás en ninguna liga",
                mensaje = "Únete a un curso para ver el ranking y competir con tus compañeros."
            )
        }
    }
}
