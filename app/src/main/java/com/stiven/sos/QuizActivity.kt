package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.services.NotificacionesHelper
import com.stiven.sos.services.BarraProgresoQuizMejorada
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var userId: String
    private lateinit var modo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Quiz"
        modo = intent.getStringExtra("modo") ?: "oficial"

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        userId = prefs.getString("user_uid", "") ?: ""

        if (userId.isNotEmpty() && cursoId.isNotEmpty() && temaId.isNotEmpty()) {
            NotificacionesHelper.notificarQuizIniciado(
                estudianteId = userId,
                cursoId = cursoId,
                temaId = temaId,
                tituloTema = temaTitulo
            )
        }

        setContent {
            EduRachaTheme {
                QuizScreen(
                    cursoId = cursoId,
                    temaId = temaId,
                    temaTitulo = temaTitulo,
                    modo = modo,
                    quizViewModel = quizViewModel,
                    onNavigateToResultado = {
                        val resultado = quizViewModel.uiState.value.resultadoQuiz
                        val quizId = quizViewModel.uiState.value.quizActivo?.quizId

                        val intent = Intent(this, ResultadoQuizActivity::class.java)
                        intent.putExtra("preguntasCorrectas", resultado?.preguntasCorrectas ?: 0)
                        intent.putExtra("preguntasIncorrectas", resultado?.preguntasIncorrectas ?: 0)
                        intent.putExtra("experienciaGanada", resultado?.experienciaGanada ?: 0)
                        intent.putExtra("vidasRestantes", resultado?.vidasRestantes ?: 0)
                        intent.putExtra("bonificacionRapidez", resultado?.bonificaciones?.rapidez ?: 0)
                        intent.putExtra("bonificacionPrimeraVez", resultado?.bonificaciones?.primeraVez ?: 0)
                        intent.putExtra("bonificacionTodoCorrecto", resultado?.bonificaciones?.todoCorrecto ?: 0)
                        intent.putExtra("quizId", quizId ?: "")
                        intent.putExtra("modo", modo)

                        startActivity(intent)
                        finish()
                    },
                    onRegresarACursos = {
                        val intent = Intent(this, CursosInscritosActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    cursoId: String,
    temaId: String,
    temaTitulo: String,
    modo: String,
    quizViewModel: QuizViewModel,
    onNavigateToResultado: () -> Unit,
    onRegresarACursos: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    var yaNavego by remember { mutableStateOf(false) }
    var yaCargoRetroalimentacion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        quizViewModel.iniciarObservadores(cursoId, temaId)
        quizViewModel.iniciarQuiz(cursoId, temaId, modo)
    }

    // ✅ VALIDACIÓN: Si se acaban las vidas durante el quiz
    LaunchedEffect(uiState.sinVidas) {
        if (uiState.sinVidas && modo != "practica" && uiState.quizActivo != null) {
            quizViewModel.mostrarDialogoSinVidas()
        }
    }

    BackHandler(enabled = true) {
        if (uiState.finalizando) {
            // No hacer nada
        } else {
            mostrarDialogoSalir = true
        }
    }

    LaunchedEffect(uiState.resultadoQuiz) {
        if (uiState.resultadoQuiz != null && !yaCargoRetroalimentacion) {
            yaCargoRetroalimentacion = true
            val quizId = uiState.quizActivo?.quizId

            if (quizId != null && (uiState.resultadoQuiz?.preguntasIncorrectas ?: 0) > 0) {
                quizViewModel.obtenerRetroalimentacion(quizId)
                delay(500)
            }

            if (!yaNavego) {
                yaNavego = true
                onNavigateToResultado()
            }
        }
    }

    // Diálogo de salir
    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EduRachaColors.Warning.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(60.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "⚠️ ¿Abandonar el quiz?",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        "Si abandonas ahora perderás:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = EduRachaColors.Error.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = EduRachaColors.Error,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    "Tu progreso actual",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (modo == "oficial") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = EduRachaColors.Error,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        "1 vida perdida",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    "Experiencia posible",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoSalir = false
                        onRegresarACursos()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Sí, abandonar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoSalir = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
                ) {
                    Text(
                        "Continuar quiz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        )
    }

    // ✅ DIÁLOGO SIN VIDAS DURANTE EL QUIZ
    if (uiState.mostrarDialogoSinVidas && modo != "practica") {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EduRachaColors.Error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    Icon(
                        Icons.Default.HeartBroken,
                        contentDescription = null,
                        tint = EduRachaColors.Error,
                        modifier = Modifier.size(64.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "💔 ¡Se acabaron las vidas!",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = EduRachaColors.Error,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        text = "Lo sentimos, ya no tienes vidas disponibles para continuar.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF2196F3).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Sistema de recuperación",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                            Text(
                                text = "• Recuperas 1 vida cada 30 minutos\n• Máximo: 5 vidas\n• Próxima vida en: ${uiState.vidas?.minutosParaProximaVida ?: 30} minutos",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextPrimary,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFF8A00),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Regresa en ${uiState.vidas?.minutosParaProximaVida ?: 30} minutos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A00)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        quizViewModel.cerrarDialogoSinVidas()
                        onRegresarACursos()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            "Regresar a Mis Cursos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    val colorModo = when (modo) {
        "practica" -> Color(0xFF00ACC1)
        "final" -> Color(0xFFFF8F00)
        else -> Color(0xFF2196F3) // ✅ AZUL
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            temaTitulo,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = when (modo) {
                                    "practica" -> "Modo Práctica 🎯"
                                    "final" -> "Quiz Final 🏆"
                                    else -> "Modo Oficial ⭐"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorModo
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        when {
            uiState.isLoading && uiState.quizActivo == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colorModo.copy(alpha = 0.12f),
                                    Color.White
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colorModo,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(70.dp)
                        )
                        Text(
                            text = "Preparando quiz...",
                            color = EduRachaColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cargando preguntas",
                            color = EduRachaColors.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            uiState.finalizando -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    EduRachaColors.Success.copy(alpha = 0.12f),
                                    Color.White
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EduRachaColors.Success,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(70.dp)
                        )
                        Text(
                            text = "Finalizando quiz...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Calculando resultados",
                            fontSize = 16.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EduRachaColors.Warning.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Por favor no cierres la app",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.Warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(90.dp),
                            tint = EduRachaColors.Error
                        )
                        Text(
                            text = "Error",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            fontSize = 16.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    key(uiState.preguntaActual) {
                        PreguntaScreen(
                            pregunta = preguntaActual,
                            numeroPregunta = uiState.preguntaActual + 1,
                            totalPreguntas = quiz.preguntas.size,
                            colorModo = colorModo,
                            onRespuestaSeleccionada = { opcionId ->
                                quizViewModel.responderPregunta(
                                    preguntaId = preguntaActual.id,
                                    respuestaSeleccionada = opcionId
                                )

                                if (uiState.preguntaActual + 1 >= quiz.preguntas.size) {
                                    quizViewModel.finalizarQuiz()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ✅ COMPONENTE DE PREGUNTA CON FEEDBACK INMEDIATO
@Composable
fun PreguntaScreen(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    colorModo: Color,
    onRespuestaSeleccionada: (Int) -> Unit
) {
    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    var mostrarFeedback by remember(pregunta.id) { mutableStateOf(false) }
    var botonPresionado by remember(pregunta.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // ✅ SIMULAR RESPUESTA CORRECTA (en producción viene del backend)
    val respuestaCorrecta = 0 // PLACEHOLDER - debería venir de la pregunta

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorModo.copy(alpha = 0.08f),
                        Color.White
                    )
                )
            )
    ) {
        // HEADER FIJO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BarraProgresoQuizMejorada(
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                colorModo = colorModo
            )
        }

        Divider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.2f))

        // CONTENIDO SCROLLEABLE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Tarjeta de pregunta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorModo.copy(alpha = 0.12f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = colorModo,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = "Pregunta $numeroPregunta",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorModo
                        )
                    }

                    Text(
                        text = pregunta.texto,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EduRachaColors.TextPrimary,
                        lineHeight = 26.sp
                    )
                }
            }

            // Opciones con feedback
            pregunta.opciones.forEachIndexed { index, opcion ->
                OpcionCardConFeedback(
                    opcion = opcion,
                    index = index,
                    isSelected = respuestaSeleccionada == index,
                    mostrarFeedback = mostrarFeedback,
                    esCorrecta = index == respuestaCorrecta, // PLACEHOLDER
                    colorModo = colorModo,
                    onClick = {
                        if (!botonPresionado && !mostrarFeedback) {
                            respuestaSeleccionada = index
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ✅ BOTÓN CON DOS ESTADOS
            if (!mostrarFeedback) {
                // BOTÓN: "Confirmar Respuesta"
                Button(
                    onClick = {
                        respuestaSeleccionada?.let {
                            mostrarFeedback = true
                            scope.launch {
                                delay(2000) // Mostrar feedback 2 segundos
                                botonPresionado = true
                                onRespuestaSeleccionada(it)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    enabled = respuestaSeleccionada != null && !botonPresionado,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorModo,
                        disabledContainerColor = EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confirmar Respuesta",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else {
                // BOTÓN: "Siguiente Pregunta" (con feedback visible)
                Button(
                    onClick = { /* Se maneja automáticamente */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorModo,
                        disabledContainerColor = colorModo.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (numeroPregunta < totalPreguntas) "Siguiente..." else "Finalizando...",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ✅ OPCIÓN CON FEEDBACK VISUAL
@Composable
fun OpcionCardConFeedback(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    index: Int,
    isSelected: Boolean,
    mostrarFeedback: Boolean,
    esCorrecta: Boolean,
    colorModo: Color,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        mostrarFeedback && isSelected && esCorrecta -> EduRachaColors.Success.copy(alpha = 0.15f)
        mostrarFeedback && isSelected && !esCorrecta -> EduRachaColors.Error.copy(alpha = 0.15f)
        isSelected -> colorModo.copy(alpha = 0.12f)
        else -> Color.White
    }

    val borderColor = when {
        mostrarFeedback && isSelected && esCorrecta -> EduRachaColors.Success
        mostrarFeedback && isSelected && !esCorrecta -> EduRachaColors.Error
        isSelected -> colorModo
        else -> Color.LightGray.copy(alpha = 0.3f)
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(if (mostrarFeedback && isSelected) 3.dp else 2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = borderColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (mostrarFeedback && isSelected) {
                        Icon(
                            if (esCorrecta) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (esCorrecta) EduRachaColors.Success else EduRachaColors.Error,
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorModo,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Text(
                text = opcion.texto,
                fontSize = 16.sp,
                color = if (mostrarFeedback && isSelected) {
                    if (esCorrecta) EduRachaColors.Success else EduRachaColors.Error
                } else if (isSelected) {
                    colorModo
                } else {
                    EduRachaColors.TextPrimary
                },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                lineHeight = 24.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}