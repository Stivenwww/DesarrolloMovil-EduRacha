package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.models.Tema
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class TemasDelCursoActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var cursoNombre: String
    private var temas: List<Tema> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarDatosCurso()

        if (savedInstanceState == null) {
            Log.d("TemasDelCurso", "Iniciando observadores por primera vez")
            quizViewModel.iniciarObservadores(cursoId)
            quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
        }

        setContent {
            EduRachaTheme {
                DisposableEffect(Unit) {
                    Log.d("TemasDelCurso", "Pantalla montada")
                    onDispose {
                        Log.d("TemasDelCurso", "Pantalla desmontada")
                    }
                }

                TemasDelCursoScreen(
                    cursoNombre = cursoNombre,
                    cursoId = cursoId,
                    temas = temas,
                    quizViewModel = quizViewModel,
                    onNavigateBack = {
                        quizViewModel.detenerObservadores()
                        finish()
                    },
                    onTemaClick = { tema ->
                        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
                        prefs.edit().putString("last_tema_id", tema.id).apply()

                        val vidas = quizViewModel.uiState.value.vidas?.vidasActuales ?: 0
                        if (vidas == 0) {
                            quizViewModel.mostrarDialogoSinVidas()
                        } else {
                            val intent = Intent(this, ExplicacionTemaActivity::class.java).apply {
                                putExtra("curso_id", cursoId)
                                putExtra("tema_id", tema.id)
                                putExtra("tema_titulo", tema.titulo)
                                putExtra("tema_explicacion", tema.explicacion)
                            }
                            startActivity(intent)
                        }
                    },
                    onQuizFinalClick = {
                        val intent = Intent(this, QuizActivity::class.java).apply {
                            putExtra("curso_id", cursoId)
                            putExtra("tema_id", "quiz_final")
                            putExtra("tema_titulo", "Quiz Final - $cursoNombre")
                            putExtra("modo", "final")
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun cargarDatosCurso() {
        cursoId = intent.getStringExtra("curso_id") ?: ""
        cursoNombre = intent.getStringExtra("curso_nombre") ?: "Curso"

        temas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("curso_temas", Tema::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("curso_temas") ?: emptyList()
        }

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        prefs.edit().putString("last_curso_id", cursoId).apply()

        Log.d("TemasDelCurso", "Datos cargados - Curso: $cursoNombre, Temas: ${temas.size}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("TemasDelCurso", "onResume - Refrescando datos")
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Log.d("TemasDelCurso", "Activity finalizando - Deteniendo observadores")
            quizViewModel.detenerObservadores()
        }
    }
}

data class ResponsiveDimens(
    val screenWidth: Dp,
    val isCompact: Boolean,
    val isTablet: Boolean,
    val padding: Dp,
    val cardPadding: Dp,
    val spacing: Dp,
    val iconSize: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    val bodySize: TextUnit,
    val smallSize: TextUnit,
    val cornerRadius: Dp,
    val elevation: Dp
)

@Composable
fun rememberResponsiveDimens(): ResponsiveDimens {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val screenHeight = config.screenHeightDp.dp

    val isCompact = screenWidth < 360.dp || screenHeight < 640.dp
    val isTablet = screenWidth >= 600.dp

    return ResponsiveDimens(
        screenWidth = screenWidth,
        isCompact = isCompact,
        isTablet = isTablet,
        padding = when {
            isTablet -> 32.dp
            isCompact -> 12.dp
            else -> 20.dp
        },
        cardPadding = when {
            isTablet -> 24.dp
            isCompact -> 12.dp
            else -> 16.dp
        },
        spacing = when {
            isTablet -> 16.dp
            isCompact -> 8.dp
            else -> 12.dp
        },
        iconSize = when {
            isTablet -> 28.dp
            isCompact -> 20.dp
            else -> 24.dp
        },
        iconMedium = when {
            isTablet -> 36.dp
            isCompact -> 28.dp
            else -> 32.dp
        },
        iconLarge = when {
            isTablet -> 56.dp
            isCompact -> 40.dp
            else -> 48.dp
        },
        titleSize = when {
            isTablet -> 32.sp
            isCompact -> 20.sp
            else -> 26.sp
        },
        subtitleSize = when {
            isTablet -> 18.sp
            isCompact -> 12.sp
            else -> 15.sp
        },
        bodySize = when {
            isTablet -> 16.sp
            isCompact -> 13.sp
            else -> 15.sp
        },
        smallSize = when {
            isTablet -> 14.sp
            isCompact -> 11.sp
            else -> 13.sp
        },
        cornerRadius = when {
            isTablet -> 24.dp
            isCompact -> 14.dp
            else -> 18.dp
        },
        elevation = when {
            isTablet -> 8.dp
            isCompact -> 3.dp
            else -> 5.dp
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemasDelCursoScreen(
    cursoNombre: String,
    cursoId: String,
    temas: List<Tema>,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onTemaClick: (Tema) -> Unit,
    onQuizFinalClick: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val dimens = rememberResponsiveDimens()

    var quizFinalAprobado by remember { mutableStateOf(false) }
    var quizFinalPorcentaje by remember { mutableStateOf(0) }
    var cargandoQuizFinal by remember { mutableStateOf(true) }

    var mostrarDialogoRacha by remember { mutableStateOf(false) }
    var mostrarDialogoXP by remember { mutableStateOf(false) }
    var mostrarDialogoVidas by remember { mutableStateOf(false) }

    LaunchedEffect(cursoId) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        if (userUid.isNotEmpty() && cursoId.isNotEmpty()) {
            try {
                val database = FirebaseDatabase.getInstance()
                val snapshot = database.getReference("quizzes").get().await()

                if (snapshot.exists()) {
                    for (quizSnapshot in snapshot.children) {
                        val estudianteId = quizSnapshot.child("estudianteId").getValue(String::class.java)
                        if (estudianteId == userUid) {
                            val modo = quizSnapshot.child("modo").getValue(String::class.java)
                            val quizCursoId = quizSnapshot.child("cursoId").getValue(String::class.java)
                            val estado = quizSnapshot.child("estado").getValue(String::class.java)

                            if (modo == "final" && quizCursoId == cursoId && estado == "finalizado") {
                                val correctas = quizSnapshot.child("preguntasCorrectas").getValue(Int::class.java) ?: 0
                                val incorrectas = quizSnapshot.child("preguntasIncorrectas").getValue(Int::class.java) ?: 0
                                val totalPreguntas = correctas + incorrectas

                                if (totalPreguntas > 0) {
                                    val porcentaje = (correctas * 100) / totalPreguntas
                                    quizFinalPorcentaje = porcentaje
                                    quizFinalAprobado = porcentaje >= 80
                                }
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TemasDelCurso", "Error cargando quiz final: ${e.message}")
            } finally {
                cargandoQuizFinal = false
            }
        }
    }

    if (uiState.mostrarDialogoSinVidas) {
        DialogoSinVidasMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
        )
    }

    if (mostrarDialogoRacha) {
        DialogoGamificadoRacha(
            rachaDias = uiState.progreso?.rachaDias ?: 0,
            onDismiss = { mostrarDialogoRacha = false },
            dimens = dimens
        )
    }

    if (mostrarDialogoXP) {
        DialogoGamificadoXP(
            experienciaTotal = uiState.progreso?.experiencia ?: 0,
            onDismiss = { mostrarDialogoXP = false },
            dimens = dimens
        )
    }

    Scaffold(
        containerColor = EduRachaColors.Background,
        topBar = {
            TopBarModerno(
                cursoNombre = cursoNombre,
                onNavigateBack = onNavigateBack,
                dimens = dimens
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = dimens.padding,
                vertical = dimens.spacing
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacing)
        ) {
            item {
                CardVidasCompacta(
                    vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                    vidasMax = uiState.vidas?.vidasMax ?: 5,
                    minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0,
                    dimens = dimens
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacing)
                ) {
                    CardStatCompacta(
                        icon = Icons.Default.Star,
                        valor = "${uiState.progreso?.experiencia ?: 0}",
                        label = "XP Total",
                        color = EduRachaColors.Warning,
                        modifier = Modifier.weight(1f),
                        onClick = { mostrarDialogoXP = true },
                        dimens = dimens
                    )
                    CardStatCompacta(
                        icon = Icons.Default.Whatshot,
                        valor = "${uiState.progreso?.rachaDias ?: 0}",
                        label = "Racha",
                        color = EduRachaColors.Error,
                        modifier = Modifier.weight(1f),
                        onClick = { mostrarDialogoRacha = true },
                        dimens = dimens
                    )
                }
            }

            if (temas.isEmpty()) {
                item { EmptyStateTemas(dimens) }
            } else {
                itemsIndexed(temas.sortedBy { it.orden }) { index, tema ->
                    TemaCardModerna(
                        tema = tema,
                        numero = index + 1,
                        totalTemas = temas.size,
                        onClick = { onTemaClick(tema) },
                        sinVidas = uiState.vidas?.vidasActuales == 0,
                        dimens = dimens
                    )
                }

                if (uiState.todosTemasAprobados) {
                    item {
                        Spacer(Modifier.height(dimens.spacing))
                        QuizFinalCard(
                            onClick = {
                                when {
                                    quizFinalAprobado -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Ya aprobaste el Quiz Final con $quizFinalPorcentaje%",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    uiState.vidas?.vidasActuales == 0 -> {
                                        quizViewModel.mostrarDialogoSinVidas()
                                    }
                                    else -> onQuizFinalClick()
                                }
                            },
                            sinVidas = uiState.vidas?.vidasActuales == 0,
                            yaCompletado = quizFinalAprobado,
                            porcentajeObtenido = quizFinalPorcentaje,
                            dimens = dimens
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoGamificadoRacha(
    rachaDias: Int,
    onDismiss: () -> Unit,
    dimens: ResponsiveDimens
) {
    var mostrarAnimacion by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        mostrarAnimacion = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (mostrarAnimacion) {
                LluviaDeFuego()
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (dimens.isTablet) 0.6f else 0.9f)
                    .wrapContentHeight()
                    .padding(dimens.padding),
                shape = RoundedCornerShape(dimens.cornerRadius * 1.5f),
                color = EduRachaColors.Surface,
                shadowElevation = dimens.elevation * 2
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    EduRachaColors.Error,
                                    EduRachaColors.Error.copy(0.8f),
                                    EduRachaColors.Surface
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.cardPadding * 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimens.spacing * 1.5f)
                    ) {
                        IconoRachaAnimado(dimens)

                        Text(
                            "RACHA DE FUEGO",
                            fontSize = dimens.titleSize,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .size(dimens.iconLarge * 3.5f)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$rachaDias",
                                    fontSize = (dimens.titleSize.value * 2.5f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    "DIAS",
                                    fontSize = dimens.subtitleSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(0.9f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(dimens.cornerRadius))
                                .background(Color.White.copy(0.15f))
                                .padding(dimens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(dimens.spacing)
                        ) {
                            MensajeMotivacional(rachaDias, dimens)

                            Divider(
                                color = Color.White.copy(0.3f),
                                thickness = 1.dp
                            )

                            Text(
                                "Sigue estudiando todos los dias para mantener tu racha activa",
                                fontSize = dimens.bodySize,
                                color = Color.White.copy(0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = dimens.bodySize * 1.5f
                            )
                        }

                        BarraProgresoRacha(rachaDias, dimens)

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.iconLarge * 1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(dimens.cornerRadius)
                        ) {
                            Text(
                                "Continuar",
                                fontSize = dimens.subtitleSize,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.Error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoGamificadoXP(
    experienciaTotal: Int,
    onDismiss: () -> Unit,
    dimens: ResponsiveDimens
) {
    var mostrarAnimacion by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        mostrarAnimacion = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (mostrarAnimacion) {
                LluviaDeEstrellas()
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (dimens.isTablet) 0.6f else 0.9f)
                    .wrapContentHeight()
                    .padding(dimens.padding),
                shape = RoundedCornerShape(dimens.cornerRadius * 1.5f),
                color = EduRachaColors.Surface,
                shadowElevation = dimens.elevation * 2
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    EduRachaColors.Warning,
                                    EduRachaColors.Warning.copy(0.8f),
                                    EduRachaColors.Surface
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.cardPadding * 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimens.spacing * 1.5f)
                    ) {
                        IconoEstrellaAnimado(dimens)

                        Text(
                            "EXPERIENCIA",
                            fontSize = dimens.titleSize,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .size(dimens.iconLarge * 3.5f)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$experienciaTotal",
                                    fontSize = (dimens.titleSize.value * 2.5f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    "XP",
                                    fontSize = dimens.subtitleSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(0.9f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(dimens.cornerRadius))
                                .background(Color.White.copy(0.15f))
                                .padding(dimens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(dimens.spacing)
                        ) {
                            MensajeMotivacionalXP(experienciaTotal, dimens)

                            Divider(
                                color = Color.White.copy(0.3f),
                                thickness = 1.dp
                            )

                            Text(
                                "Completa mas lecciones y quizzes para ganar mas experiencia",
                                fontSize = dimens.bodySize,
                                color = Color.White.copy(0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = dimens.bodySize * 1.5f
                            )
                        }

                        BarraProgresoXP(experienciaTotal, dimens)

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.iconLarge * 1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(dimens.cornerRadius)
                        ) {
                            Text(
                                "Continuar",
                                fontSize = dimens.subtitleSize,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.Warning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LluviaDeEstrellas() {
    val estrellas = remember {
        List(30) {
            EstrellaData(
                offsetX = Random.nextFloat(),
                delay = Random.nextInt(0, 1000),
                duration = Random.nextInt(2000, 4000)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        estrellas.forEach { estrella ->
            EstrellaAnimada(estrella)
        }
    }
}

@Composable
fun LluviaDeFuego() {
    val fueguitos = remember {
        List(30) {
            EstrellaData(
                offsetX = Random.nextFloat(),
                delay = Random.nextInt(0, 1000),
                duration = Random.nextInt(2000, 4000)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        fueguitos.forEach { fueguito ->
            FuegoAnimado(fueguito)
        }
    }
}

data class EstrellaData(
    val offsetX: Float,
    val delay: Int,
    val duration: Int
)

@Composable
fun EstrellaAnimada(data: EstrellaData) {
    val infiniteTransition = rememberInfiniteTransition(label = "star")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(data.duration, delayMillis = data.delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp

    Icon(
        Icons.Default.Star,
        contentDescription = null,
        tint = Color.Yellow,
        modifier = Modifier
            .offset(
                x = (screenWidth * data.offsetX).dp,
                y = offsetY.dp
            )
            .size(24.dp)
            .rotate(rotation)
            .alpha(0.8f)
    )
}

@Composable
fun FuegoAnimado(data: EstrellaData) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(data.duration, delayMillis = data.delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fireY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireRotation"
    )

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp

    Icon(
        Icons.Default.Whatshot,
        contentDescription = null,
        tint = Color(0xFFFF6B35),
        modifier = Modifier
            .offset(
                x = (screenWidth * data.offsetX).dp,
                y = offsetY.dp
            )
            .size(28.dp)
            .rotate(rotation)
            .alpha(0.9f)
    )
}

@Composable
fun IconoRachaAnimado(dimens: ResponsiveDimens) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireRotation"
    )

    Box(
        modifier = Modifier
            .size(dimens.iconLarge * 2.5f)
            .scale(scale)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(0.2f),
            modifier = Modifier.fillMaxSize()
        ) {}
        Icon(
            Icons.Default.Whatshot,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(dimens.iconLarge * 1.8f)
        )
    }
}

@Composable
fun IconoEstrellaAnimado(dimens: ResponsiveDimens) {
    val infiniteTransition = rememberInfiniteTransition(label = "star")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )

    Box(
        modifier = Modifier
            .size(dimens.iconLarge * 2.5f)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(0.2f),
            modifier = Modifier.fillMaxSize()
        ) {}
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(dimens.iconLarge * 1.8f)
                .rotate(rotation)
        )
    }
}

@Composable
fun MensajeMotivacional(rachaDias: Int, dimens: ResponsiveDimens) {
    val mensaje = when {
        rachaDias == 0 -> "Comienza tu racha hoy mismo"
        rachaDias < 3 -> "Buen comienzo, sigue asi"
        rachaDias < 7 -> "Excelente progreso, continua"
        rachaDias < 15 -> "Increible dedicacion"
        rachaDias < 30 -> "Eres imparable"
        rachaDias < 60 -> "Maestro del conocimiento"
        else -> "Leyenda del aprendizaje"
    }

    Text(
        mensaje,
        fontSize = dimens.subtitleSize,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MensajeMotivacionalXP(experiencia: Int, dimens: ResponsiveDimens) {
    val mensaje = when {
        experiencia < 100 -> "Estas comenzando tu viaje"
        experiencia < 500 -> "Buen progreso estudiante"
        experiencia < 1000 -> "Avanzando rapidamente"
        experiencia < 2000 -> "Conocimiento notable"
        experiencia < 5000 -> "Experto en formacion"
        experiencia < 10000 -> "Maestro del saber"
        else -> "Gran maestro legendario"
    }

    Text(
        mensaje,
        fontSize = dimens.subtitleSize,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun BarraProgresoRacha(rachaDias: Int, dimens: ResponsiveDimens) {
    val objetivo = when {
        rachaDias < 7 -> 7
        rachaDias < 15 -> 15
        rachaDias < 30 -> 30
        rachaDias < 60 -> 60
        else -> ((rachaDias / 30) + 1) * 30
    }

    val progreso = (rachaDias.toFloat() / objetivo).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacing * 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Progreso al siguiente nivel",
                fontSize = dimens.smallSize,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.8f)
            )
            Text(
                "$rachaDias / $objetivo dias",
                fontSize = dimens.smallSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.spacing * 1.5f)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, Color.White.copy(0.8f))
                        )
                    )
            )
        }
    }
}

@Composable
fun BarraProgresoXP(experiencia: Int, dimens: ResponsiveDimens) {
    val objetivo = when {
        experiencia < 100 -> 100
        experiencia < 500 -> 500
        experiencia < 1000 -> 1000
        experiencia < 2000 -> 2000
        experiencia < 5000 -> 5000
        experiencia < 10000 -> 10000
        else -> ((experiencia / 5000) + 1) * 5000
    }

    val progreso = (experiencia.toFloat() / objetivo).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacing * 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Progreso al siguiente rango",
                fontSize = dimens.smallSize,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.8f)
            )
            Text(
                "$experiencia / $objetivo XP",
                fontSize = dimens.smallSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.spacing * 1.5f)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, Color.White.copy(0.8f))
                        )
                    )
            )
        }
    }
}

@Composable
fun TopBarModerno(
    cursoNombre: String,
    onNavigateBack: () -> Unit,
    dimens: ResponsiveDimens
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EduRachaColors.Primary,
        shadowElevation = dimens.elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.PrimaryLight
                        )
                    )
                )
                .statusBarsPadding()
                .padding(dimens.padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(dimens.iconLarge)
                        .background(Color.White.copy(0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color.White.copy(0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = dimens.cardPadding,
                            vertical = dimens.spacing * 0.7f
                        ),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(dimens.iconSize * 0.8f)
                        )
                        Text(
                            "Curso",
                            color = Color.White,
                            fontSize = dimens.smallSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacing))

            Text(
                text = cursoNombre,
                fontSize = dimens.titleSize,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = dimens.titleSize * 1.2f
            )

            Spacer(Modifier.height(dimens.spacing * 0.5f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.8f)
            ) {
                Surface(
                    shape = RoundedCornerShape(dimens.cornerRadius * 0.5f),
                    color = Color.White.copy(0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = dimens.spacing,
                            vertical = dimens.spacing * 0.4f
                        ),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.4f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(dimens.iconSize * 0.7f)
                        )
                        Text(
                            "Aprende jugando",
                            fontSize = dimens.smallSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardVidasCompacta(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    dimens: ResponsiveDimens
) {
    val colorVida = EduRachaColors.Error
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        1f, 1.05f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = EduRachaColors.Surface,
        shadowElevation = dimens.elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(dimens.iconLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorVida.copy(0.15f),
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (vidasActuales > 0) scale else 1f)
                    ) {}
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = colorVida,
                        modifier = Modifier.size(dimens.iconMedium * 0.7f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacing * 0.3f)) {
                    Text(
                        "ENERGIA",
                        fontSize = dimens.smallSize * 0.8f,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.3f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "$vidasActuales",
                            fontSize = dimens.titleSize * 0.9f,
                            fontWeight = FontWeight.Black,
                            color = colorVida
                        )
                        Text(
                            "/ $vidasMax",
                            fontSize = dimens.bodySize,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextSecondary,
                            modifier = Modifier.padding(bottom = dimens.spacing * 0.2f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.3f)) {
                        repeat(vidasMax) { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = dimens.spacing, height = 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < vidasActuales) colorVida
                                        else Color.LightGray.copy(0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            if (vidasActuales < vidasMax && minutosParaProxima > 0) {
                Surface(
                    shape = RoundedCornerShape(dimens.cornerRadius * 0.7f),
                    color = EduRachaColors.Info.copy(0.1f)
                ) {
                }
            }
        }
    }
}

@Composable
fun CardStatCompacta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    dimens: ResponsiveDimens
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = EduRachaColors.Surface,
        shadowElevation = if (isPressed) dimens.elevation * 0.5f else dimens.elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            color.copy(0.08f),
                            EduRachaColors.Surface
                        )
                    )
                )
                .padding(dimens.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(0.15f),
                modifier = Modifier.size(dimens.iconLarge * 0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacing * 0.1f)) {
                Text(
                    valor,
                    fontSize = dimens.subtitleSize * 1.3f,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    label,
                    fontSize = dimens.smallSize,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextSecondary
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun TemaCardModerna(
    tema: Tema,
    numero: Int,
    totalTemas: Int,
    onClick: () -> Unit,
    sinVidas: Boolean,
    dimens: ResponsiveDimens
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = !sinVidas) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = if (sinVidas) EduRachaColors.Surface.copy(0.6f) else EduRachaColors.Surface,
        shadowElevation = if (isPressed) dimens.elevation * 0.5f else dimens.elevation
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (sinVidas) {
                                listOf(Color.LightGray, Color.Gray)
                            } else {
                                listOf(EduRachaColors.Primary, EduRachaColors.PrimaryLight)
                            }
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spacing)
            ) {
                NumeroTema(
                    numero = numero,
                    sinVidas = sinVidas,
                    dimens = dimens
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacing * 0.6f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(dimens.cornerRadius * 0.5f),
                        color = if (sinVidas) Color.LightGray.copy(0.3f)
                        else EduRachaColors.Primary.copy(0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = dimens.spacing,
                                vertical = dimens.spacing * 0.4f
                            ),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.4f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (sinVidas) Color.Gray else EduRachaColors.Primary,
                                modifier = Modifier.size(dimens.iconSize * 0.7f)
                            )
                            Text(
                                "Leccion $numero de $totalTemas",
                                fontSize = dimens.smallSize,
                                fontWeight = FontWeight.Bold,
                                color = if (sinVidas) Color.Gray else EduRachaColors.Primary
                            )
                        }
                    }

                    Text(
                        tema.titulo,
                        fontSize = dimens.subtitleSize,
                        fontWeight = FontWeight.Black,
                        color = if (sinVidas) EduRachaColors.TextSecondary else EduRachaColors.TextPrimary,
                        lineHeight = dimens.subtitleSize * 1.3f
                    )

                    if (tema.descripcion.isNotBlank()) {
                        Text(
                            tema.descripcion,
                            fontSize = dimens.bodySize,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = dimens.bodySize * 1.4f
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (sinVidas) EduRachaColors.Error.copy(0.15f)
                    else EduRachaColors.Primary.copy(0.15f),
                    modifier = Modifier.size(dimens.iconLarge)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (sinVidas) Icons.Default.Lock else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (sinVidas) EduRachaColors.Error else EduRachaColors.Primary,
                            modifier = Modifier.size(dimens.iconMedium * 0.6f)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun NumeroTema(
    numero: Int,
    sinVidas: Boolean,
    dimens: ResponsiveDimens
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        0f, -6f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "float"
    )

    Box(
        modifier = Modifier
            .size(dimens.iconLarge * 1.4f)
            .offset(y = if (!sinVidas) offsetY.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (sinVidas) Color.LightGray.copy(0.2f)
            else EduRachaColors.Primary.copy(0.12f),
            modifier = Modifier.fillMaxSize()
        ) {}

        Box(
            modifier = Modifier
                .size(dimens.iconLarge * 1.1f)
                .clip(CircleShape)
                .background(
                    if (sinVidas) Color.Gray
                    else EduRachaColors.Primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$numero",
                fontSize = dimens.subtitleSize * 1.2f,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun QuizFinalCard(
    onClick: () -> Unit,
    sinVidas: Boolean,
    yaCompletado: Boolean,
    porcentajeObtenido: Int,
    dimens: ResponsiveDimens
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quiz")
    val scale by infiniteTransition.animateFloat(
        1f, 1.03f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (!sinVidas && !yaCompletado) scale else 1f)
            .clickable(enabled = !sinVidas && !yaCompletado, onClick = onClick),
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = EduRachaColors.Surface,
        shadowElevation = if (sinVidas || yaCompletado) dimens.elevation * 0.5f else dimens.elevation * 2f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (sinVidas || yaCompletado) {
                            listOf(Color.LightGray, EduRachaColors.Surface)
                        } else {
                            listOf(
                                EduRachaColors.Secondary,
                                EduRachaColors.SecondaryLight
                            )
                        }
                    )
                )
                .padding(dimens.cardPadding * 1.5f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.spacing)
            ) {
                Box(
                    modifier = Modifier.size(dimens.iconLarge * 2f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (sinVidas || yaCompletado) Color.Gray.copy(0.2f)
                        else Color.White.copy(0.3f),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (sinVidas || yaCompletado) Color.Gray else Color.White,
                        modifier = Modifier.size(dimens.iconLarge)
                    )
                }

                Text(
                    if (yaCompletado) "QUIZ COMPLETADO" else "QUIZ FINAL",
                    fontSize = dimens.titleSize * 0.8f,
                    fontWeight = FontWeight.Black,
                    color = if (sinVidas || yaCompletado) Color.Gray else Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    when {
                        yaCompletado -> "Aprobaste con $porcentajeObtenido%"
                        sinVidas -> "Necesitas energia disponible"
                        else -> "Completaste todos los temas"
                    },
                    fontSize = dimens.bodySize,
                    fontWeight = FontWeight.Medium,
                    color = if (sinVidas || yaCompletado) Color.Gray else Color.White.copy(0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = dimens.bodySize * 1.4f
                )

                if (!sinVidas && !yaCompletado) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color.White,
                        shadowElevation = dimens.elevation
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = dimens.cardPadding,
                                vertical = dimens.spacing
                            ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EduRachaColors.Secondary,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                            Spacer(Modifier.width(dimens.spacing * 0.6f))
                            Text(
                                "Comenzar Examen Final",
                                fontSize = dimens.bodySize,
                                fontWeight = FontWeight.ExtraBold,
                                color = EduRachaColors.Secondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RecompensaChip(
                            icon = Icons.Default.Star,
                            texto = "+500 XP",
                            color = EduRachaColors.Warning,
                            modifier = Modifier.weight(1f),
                            dimens = dimens
                        )
                        RecompensaChip(
                            icon = Icons.Default.EmojiEvents,
                            texto = "Certificado",
                            color = EduRachaColors.RankingGold,
                            modifier = Modifier.weight(1f),
                            dimens = dimens
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecompensaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    dimens: ResponsiveDimens
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.cornerRadius * 0.7f),
        color = Color.White.copy(0.9f)
    ) {
        Row(
            modifier = Modifier.padding(dimens.spacing),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacing * 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(dimens.iconSize * 0.8f)
            )
            Text(
                texto,
                fontSize = dimens.smallSize,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun EmptyStateTemas(dimens: ResponsiveDimens) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimens.padding * 2f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spacing * 1.5f)
        ) {
            Box(
                modifier = Modifier.size(dimens.iconLarge * 3f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Primary.copy(0.08f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconLarge * 1.5f),
                    tint = EduRachaColors.Primary.copy(0.6f)
                )
            }

            Text(
                "Aun no hay temas",
                fontSize = dimens.titleSize * 0.8f,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "El docente esta preparando contenido educativo de calidad",
                fontSize = dimens.bodySize,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = dimens.bodySize * 1.5f
            )

            Surface(
                shape = RoundedCornerShape(dimens.cornerRadius),
                color = EduRachaColors.Info.copy(0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(dimens.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = EduRachaColors.Info,
                        modifier = Modifier.size(dimens.iconSize)
                    )
                    Text(
                        "Manten tu racha activa mientras tanto",
                        fontSize = dimens.bodySize,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.Info
                    )
                }
            }
        }
    }
}