package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.CursoViewModel

class SeleccionarCursoRankingActivity : ComponentActivity() {
    private val viewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.obtenerCursos()

        setContent {
            EduRachaTheme {
                SeleccionarCursoRankingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onCursoSelected = { curso ->
                        val intent = Intent(this, RankingDetalleActivity::class.java).apply {
                            putExtra("CURSO_ID", curso.id)
                            putExtra("CURSO_TITULO", curso.titulo)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun SeleccionarCursoRankingScreen(
    viewModel: CursoViewModel,
    onNavigateBack: () -> Unit,
    onCursoSelected: (Curso) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    EduRachaV2Container {
        when {
            uiState.isLoading -> {
                LoadingRankingView()
            }

            uiState.error != null -> {
                ErrorRankingView(
                    error = uiState.error!!,
                    onRetry = { viewModel.obtenerCursos() },
                    onNavigateBack = onNavigateBack
                )
            }

            uiState.cursos.isEmpty() -> {
                EmptyRankingView(onNavigateBack = onNavigateBack)
            }

            else -> {
                ListadoCursosRankingV2(
                    cursos = uiState.cursos,
                    onCursoClick = onCursoSelected,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
fun ListadoCursosRankingV2(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit,
    onNavigateBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header animado con scroll
        item {
            HeaderRankingScrollable(
                cantidadCursos = cursos.size,
                onNavigateBack = onNavigateBack
            )
        }

        // Espaciado
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
                TarjetaCursoRankingV2(
                    curso = curso,
                    posicion = index + 1,
                    onClick = { onCursoClick(curso) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Espaciado final
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeaderRankingScrollable(
    cantidadCursos: Int,
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
        // Burbujas decorativas animadas
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
            // Botón de retroceso animado
            val interactionSource = remember { MutableInteractionSource() }

            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                interactionSource = interactionSource
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Ícono principal animado
            AnimatedTrophyIcon()

            Spacer(Modifier.height(20.dp))

            // Título principal
            Text(
                "Rankings de Cursos",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            // Subtítulo
            Text(
                "Explora el desempeño académico",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            // Contador animado de cursos
            ContadorCursosAnimado(cantidad = cantidadCursos)
        }
    }
}

@Composable
fun AnimatedTrophyIcon() {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
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
fun ContadorCursosAnimado(cantidad: Int) {
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
                    "Cursos disponibles",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Toca uno para ver su ranking",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Outlined.Leaderboard,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TarjetaCursoRankingV2(
    curso: Curso,
    posicion: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val gradienteColor = when (posicion % 4) {
        0 -> EduRachaV2Gradients.Green
        1 -> EduRachaV2Gradients.Purple
        2 -> EduRachaV2Gradients.Yellow
        else -> EduRachaV2Gradients.Orange
    }

    val iconColor = when (posicion % 4) {
        0 -> EduRachaV2Colors.Success
        1 -> EduRachaV2Colors.Secondary
        2 -> EduRachaV2Colors.Accent
        else -> EduRachaV2Colors.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header de la tarjeta con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradienteColor)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Número de posición animado
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#$posicion",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        // Badge de código
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Ícono animado
                    AnimatedCourseIcon(iconColor)
                }
            }

            // Botón de acción
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                color = iconColor.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Leaderboard,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ver Ranking Completo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                        Text(
                            text = "Consulta el desempeño de estudiantes",
                            fontSize = 12.sp,
                            color = EduRachaV2Colors.TextSecondary
                        )
                    }

                    AnimatedArrowIcon(iconColor)
                }
            }
        }
    }
}

@Composable
fun AnimatedCourseIcon(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun AnimatedArrowIcon(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        Icons.Outlined.ArrowForward,
        null,
        tint = color,
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { translationX = offsetX }
    )
}

@Composable
fun LoadingRankingView() {
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
                    .background(EduRachaV2Colors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = EduRachaV2Colors.Primary,
                    strokeWidth = 4.dp
                )
            }

            Text(
                "Cargando rankings...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaV2Colors.TextPrimary
            )

            Text(
                "Preparando la competencia 🏆",
                fontSize = 14.sp,
                color = EduRachaV2Colors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorRankingView(
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
                title = "Error al cargar rankings",
                message = error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyRankingView(onNavigateBack: () -> Unit) {
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
                title = "No hay cursos disponibles",
                message = "Los cursos aparecerán aquí una vez sean creados por los docentes. ¡Mantente atento! 📚",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}