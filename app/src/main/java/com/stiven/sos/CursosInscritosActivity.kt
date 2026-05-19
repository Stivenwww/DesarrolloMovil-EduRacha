package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

class CursosInscritosActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        quizViewModel.cargarCursosInscritos()

        setContent {
            EduRachaTheme {
                CursosInscritosScreenV2Figma(
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onCursoClick = { curso ->
                        val intent = Intent(this, TemasDelCursoActivity::class.java)
                        intent.putExtra("curso_id", curso.id)
                        intent.putExtra("curso_nombre", curso.titulo)

                        val temasList = ArrayList(curso.getTemasLista())
                        intent.putParcelableArrayListExtra("curso_temas", temasList)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun CursosInscritosScreenV2Figma(
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onCursoClick: (Curso) -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filtrado de cursos
    val cursosFiltrados = remember(uiState.cursosInscritos, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.cursosInscritos
        } else {
            uiState.cursosInscritos.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    // Animación de entrada
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        screenVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F9FC),
                        Color(0xFFE8ECF0)
                    )
                )
            )
    ) {
        // Header con fondo verde estilo Figma
        HeaderGreenFigma(
            totalCursos = uiState.cursosInscritos.size,
            onNavigateBack = onNavigateBack
        )

        // Contenido principal con scroll mejorado
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Buscador (solo si hay cursos)
            if (uiState.cursosInscritos.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = screenVisible,
                        enter = fadeIn(tween(400)) + expandVertically()
                    ) {
                        SearchBarFigma(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it }
                        )
                    }
                }
            }

            // Contenido
            when {
                uiState.isLoading -> {
                    item {
                        LoadingViewFigma()
                    }
                }

                uiState.error != null -> {
                    item {
                        ErrorViewFigma(
                            error = uiState.error ?: "",
                            onRetry = { quizViewModel.cargarCursosInscritos() }
                        )
                    }
                }

                uiState.cursosInscritos.isEmpty() -> {
                    item {
                        EmptyCursosViewFigma()
                    }
                }

                cursosFiltrados.isEmpty() -> {
                    item {
                        EmptySearchViewFigma(searchQuery = searchQuery)
                    }
                }

                else -> {
                    itemsIndexed(
                        items = cursosFiltrados,
                        key = { _, curso -> curso.id ?: curso.codigo }
                    ) { index, curso ->
                        AnimatedCursoCardFigma(
                            curso = curso,
                            onClick = { onCursoClick(curso) },
                            delay = index * 80,
                            index = index
                        )
                    }
                }
            }

            // Espaciado final
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================================
// HEADER GREEN FIGMA
// ============================================================================

@Composable
fun HeaderGreenFigma(
    totalCursos: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF41C77C),
                        Color(0xFF5FD99A)
                    )
                )
            )
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 64.dp, start = 20.dp, end = 20.dp)
    ) {
        // Burbujas decorativas
        Box(
            modifier = Modifier
                .size(96.dp)
                .offset(x = 250.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .size(128.dp)
                .offset(x = 64.dp, y = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Botón de atrás
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Iconos en la parte superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFC864),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Título
            Text(
                text = "Cursos Inscritos",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 40.sp
            )

            Spacer(Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Aquí están tus cursos activos",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// ============================================================================
// BUSCADOR FIGMA
// ============================================================================

@Composable
fun SearchBarFigma(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = Color(0xFF717182),
                modifier = Modifier.size(24.dp)
            )

            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFF1C1C1E),
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Buscar cursos...",
                            fontSize = 16.sp,
                            color = Color(0xFFA0A0AB)
                        )
                    }
                    innerTextField()
                }
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Limpiar",
                        tint = Color(0xFF717182),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// CARD DE CURSO ANIMADO FIGMA
// ============================================================================

@Composable
fun AnimatedCursoCardFigma(
    curso: Curso,
    onClick: () -> Unit,
    delay: Int = 0,
    index: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600)
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX.toPx()
            this.alpha = alpha
        }
    ) {
        CursoCardFigma(
            curso = curso,
            onClick = onClick,
            index = index
        )
    }
}

@Composable
fun CursoCardFigma(
    curso: Curso,
    onClick: () -> Unit,
    index: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 8.dp,
        animationSpec = tween(100)
    )

    // Gradientes predefinidos que rotan según el índice
    val gradientes = listOf(
        listOf(Color(0xFF3C79F5), Color(0xFF6BB9FF)), // Azul
        listOf(Color(0xFF41C77C), Color(0xFFFFC864)), // Verde-Amarillo
        listOf(Color(0xFF9C6BFF), Color(0xFFFF7096)), // Morado-Rosa
        listOf(Color(0xFFFFC864), Color(0xFFFF7096))  // Amarillo-Rosa
    )

    val gradient = Brush.linearGradient(gradientes[index % gradientes.size])

    // Iconos que rotan según el índice
    val iconos = listOf(
        Icons.Outlined.Computer,
        Icons.Outlined.Code,
        Icons.Outlined.Storage,
        Icons.Outlined.Psychology
    )
    val icono = iconos[index % iconos.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(gradient)
            ) {
                // Burbujas decorativas
                AnimatedBubblesBackgroundSmall()

                // Contenido del header - Logo ARRIBA, Título ABAJO
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // LOGO ARRIBA (con badge de estrella decorativa)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Box(
                                modifier = Modifier.padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icono,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Estrella decorativa (como en Figma)
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFC864),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // TÍTULO ABAJO
                    Text(
                        text = curso.titulo,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Descripción
                if (!curso.descripcion.isNullOrBlank()) {
                    Text(
                        text = curso.descripcion ?: "",
                        fontSize = 15.sp,
                        color = Color(0xFF717182),
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // Contenido programático
                val temas = curso.getTemasLista()
                if (temas.isNotEmpty()) {
                    Text(
                        "CONTENIDO PROGRAMÁTICO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A0AB),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        temas.take(4).forEach { tema ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .offset(y = 7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3C79F5))
                                )
                                Text(
                                    tema.titulo,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1C1C1E),
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // Botón de acción
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3C79F5)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Comenzar a practicar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTES AUXILIARES
// ============================================================================

@Composable
private fun AnimatedBubblesBackgroundSmall() {
    val infiniteTransition = rememberInfiniteTransition()

    val bubble1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Burbuja grande
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 220.dp, y = (-10).dp)
                .graphicsLayer {
                    translationY = bubble1Offset
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Burbuja pequeña
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(x = (-5).dp, y = 20.dp)
                .graphicsLayer {
                    translationY = -bubble1Offset * 0.7f
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
    }
}

// ============================================================================
// ESTADOS VACÍOS Y LOADING
// ============================================================================

@Composable
fun LoadingViewFigma() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = Color(0xFF41C77C),
                strokeWidth = 4.dp
            )

            Text(
                text = "Cargando tus cursos...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF717182)
            )
        }
    }
}

@Composable
fun ErrorViewFigma(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE8EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFFF7096)
                )
            }

            Text(
                text = "Algo salió mal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )

            Text(
                text = error,
                fontSize = 14.sp,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7096)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Reintentar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCursosViewFigma() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F4FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.School,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF3C79F5)
                )
            }

            Text(
                text = "No tienes cursos activos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cuando un docente acepte tu solicitud de inscripción, tus cursos aparecerán aquí",
                fontSize = 14.sp,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun EmptySearchViewFigma(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF7F9FC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFA0A0AB)
                )
            }

            Text(
                text = "No se encontraron cursos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )

            Text(
                text = "No hay cursos que coincidan con \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color(0xFF717182),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}