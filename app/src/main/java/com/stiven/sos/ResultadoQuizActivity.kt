package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.draw.clip
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.stiven.sos.services.CelebracionRachaDialog
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

class ResultadoQuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private var preguntasCorrectas: Int = 0
    private var preguntasIncorrectas: Int = 0
    private var experienciaGanada: Int = 0
    private var vidasRestantes: Int = 0
    private var bonificacionRapidez: Int = 0
    private var bonificacionPrimeraVez: Int = 0
    private var bonificacionTodoCorrecto: Int = 0
    private var quizId: String = ""
    private lateinit var modo: String
    private lateinit var cursoId: String
    private lateinit var temaId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preguntasCorrectas = intent.getIntExtra("preguntasCorrectas", 0)
        preguntasIncorrectas = intent.getIntExtra("preguntasIncorrectas", 0)
        experienciaGanada = intent.getIntExtra("experienciaGanada", 0)
        vidasRestantes = intent.getIntExtra("vidasRestantes", 0)
        bonificacionRapidez = intent.getIntExtra("bonificacionRapidez", 0)
        bonificacionPrimeraVez = intent.getIntExtra("bonificacionPrimeraVez", 0)
        bonificacionTodoCorrecto = intent.getIntExtra("bonificacionTodoCorrecto", 0)
        quizId = intent.getStringExtra("quizId") ?: ""
        modo = intent.getStringExtra("modo") ?: "oficial"

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        cursoId = prefs.getString("last_curso_id", "") ?: ""
        temaId = prefs.getString("last_tema_id", "") ?: ""

        setContent {
            EduRachaTheme {
                ResultadoQuizScreen(
                    preguntasCorrectas = preguntasCorrectas,
                    preguntasIncorrectas = preguntasIncorrectas,
                    experienciaGanada = experienciaGanada,
                    vidasRestantes = vidasRestantes,
                    bonificacionRapidez = bonificacionRapidez,
                    bonificacionPrimeraVez = bonificacionPrimeraVez,
                    bonificacionTodoCorrecto = bonificacionTodoCorrecto,
                    quizId = quizId,
                    modo = modo,
                    cursoId = cursoId,
                    temaId = temaId,
                    quizViewModel = quizViewModel,
                    onVerRetroalimentacion = {
                        val intent = Intent(this, RetroalimentacionActivity::class.java)
                        intent.putExtra("quizId", quizId)
                        startActivity(intent)
                    },
                    onVolverACursos = {
                        val intent = Intent(this, CursosInscritosActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    },
                    onIniciarPractica = {
                        val intent = Intent(this, ExplicacionTemaActivity::class.java)
                        intent.putExtra("curso_id", cursoId)
                        intent.putExtra("tema_id", temaId)
                        intent.putExtra("tema_titulo", "Tema")
                        intent.putExtra("tema_explicacion", "Explicación")
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
fun ResultadoQuizScreen(
    preguntasCorrectas: Int,
    preguntasIncorrectas: Int,
    experienciaGanada: Int,
    vidasRestantes: Int,
    bonificacionRapidez: Int,
    bonificacionPrimeraVez: Int,
    bonificacionTodoCorrecto: Int,
    quizId: String,
    modo: String,
    cursoId: String,
    temaId: String,
    quizViewModel: QuizViewModel,
    onVerRetroalimentacion: () -> Unit,
    onVolverACursos: () -> Unit,
    onIniciarPractica: () -> Unit
) {
    val totalPreguntas = preguntasCorrectas + preguntasIncorrectas
    val porcentaje = if (totalPreguntas > 0) (preguntasCorrectas * 100) / totalPreguntas else 0
    val aprobo = porcentaje >= 80

    // SOLO UNA celebración de racha
    var mostrarCelebracionRacha by remember { mutableStateOf(false) }
    var diasRacha by remember { mutableStateOf(0) }

    val uiState by quizViewModel.uiState.collectAsState()

    // Prevenir el botón de atrás durante las celebraciones
    BackHandler(enabled = mostrarCelebracionRacha) {
        // No hacer nada durante la celebración
    }

    LaunchedEffect(Unit) {
        if (cursoId.isNotEmpty()) {
            quizViewModel.iniciarObservadores(cursoId, temaId)
        }
    }

    LaunchedEffect(uiState.progreso) {
        uiState.progreso?.let {
            diasRacha = it.rachaDias
        }
    }

    //  MOSTRAR CELEBRACIÓN DE RACHA SOLO SI MODO OFICIAL Y RACHA SUBIÓ
    LaunchedEffect(uiState.mostrarCelebracionRacha) {
        if (uiState.mostrarCelebracionRacha && modo == "oficial" && aprobo) {
            delay(1000) // Pequeña pausa
            mostrarCelebracionRacha = true
        }
    }

    //  UNA SOLA celebración de racha
    if (mostrarCelebracionRacha && diasRacha > 0) {
        CelebracionRachaDialog(
            diasRacha = diasRacha,
            onDismiss = {
                mostrarCelebracionRacha = false
                quizViewModel.cerrarCelebracionRacha()
            }
        )
    }

    val colorResultado = when {
        porcentaje >= 90 -> EduRachaColors.Success
        porcentaje >= 80 -> EduRachaColors.Warning
        porcentaje >= 70 -> Color(0xFFFF8F00)
        else -> EduRachaColors.Error
    }

    val mensajeResultado = when {
        porcentaje == 100 -> " ¡PERFECTO!"
        porcentaje >= 90 -> " ¡EXCELENTE!"
        porcentaje >= 80 -> " ¡MUY BIEN!"
        porcentaje >= 70 -> "¡BIEN!"
        porcentaje >= 60 -> " SIGUE ESTUDIANDO"
        else -> " SIGUE PRACTICANDO"
    }

    val mensajeMotivacional = when {
        porcentaje == 100 -> "¡Dominas este tema completamente!"
        porcentaje >= 90 -> "¡Tienes un excelente dominio del tema!"
        porcentaje >= 80 -> "¡Gran trabajo! Sigues mejorando."
        porcentaje >= 70 -> "Buen trabajo. Revisa algunos conceptos."
        porcentaje >= 60 -> "Estás progresando. Practica un poco más."
        else -> "No te rindas. La práctica hace al maestro."
    }

    // ✅Mensaje específico según modo
    val mensajeModo = when (modo) {
        "practica" -> "Modo Práctica - Ganas XP y pierdes vidas, NO cuenta para racha"
        "oficial" -> if (aprobo) "¡Aprobaste el quiz oficial! Cuenta para tu racha 🔥" else "Quiz oficial completado"
        "final" -> "Quiz Final del Curso"
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Resultados del Quiz",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            when (modo) {
                                "practica" -> "Modo Práctica 🎯"
                                "final" -> "Quiz Final 🏆"
                                else -> "Modo Oficial ⭐"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResultado
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorResultado.copy(alpha = 0.08f),
                            Color.White
                        )
                    )
                ),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header principal
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colorResultado.copy(alpha = 0.05f),
                                        Color.White
                                    )
                                )
                            )
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        IconoResultadoAnimado(
                            aprobo = aprobo,
                            porcentaje = porcentaje,
                            colorResultado = colorResultado
                        )

                        Text(
                            text = mensajeResultado,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = colorResultado,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = colorResultado.copy(alpha = 0.12f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 36.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$porcentaje%",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorResultado
                                )
                                Text(
                                    text = "de precisión",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = mensajeMotivacional,
                            fontSize = 15.sp,
                            color = EduRachaColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        // ✅ Indicador de modo
                        if (mensajeModo.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EduRachaColors.Info.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = EduRachaColors.Info,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = mensajeModo,
                                        fontSize = 13.sp,
                                        color = EduRachaColors.Info,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colorResultado.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ContadorRespuestas(
                                cantidad = preguntasCorrectas,
                                total = totalPreguntas,
                                tipo = "Correctas",
                                color = EduRachaColors.Success,
                                icono = Icons.Default.CheckCircle
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                            )

                            ContadorRespuestas(
                                cantidad = preguntasIncorrectas,
                                total = totalPreguntas,
                                tipo = "Incorrectas",
                                color = EduRachaColors.Error,
                                icono = Icons.Default.Cancel
                            )
                        }
                    }
                }
            }

            // Estadísticas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorResultado.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = colorResultado,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Estadísticas",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BarraEstadistica(
                                label = "Correctas",
                                valor = preguntasCorrectas,
                                total = totalPreguntas,
                                color = EduRachaColors.Success
                            )
                            BarraEstadistica(
                                label = "Incorrectas",
                                valor = preguntasIncorrectas,
                                total = totalPreguntas,
                                color = EduRachaColors.Error
                            )
                        }

                        Divider(color = Color.LightGray.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    text = "Vidas restantes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EduRachaColors.Error.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$vidasRestantes / 5",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EduRachaColors.Error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recompensas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EduRachaColors.Warning.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = EduRachaColors.Warning,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Recompensas",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EduRachaColors.Warning.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(22.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = EduRachaColors.Warning,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Experiencia Total",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EduRachaColors.TextSecondary
                                        )
                                        Text(
                                            text = "+$experienciaGanada XP",
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Black,
                                            color = EduRachaColors.Warning
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        if (bonificacionRapidez > 0 || bonificacionPrimeraVez > 0 || bonificacionTodoCorrecto > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "✨ Bonificaciones Especiales",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )

                                if (bonificacionRapidez > 0) {
                                    BonificacionCard(
                                        icono = Icons.Default.Speed,
                                        titulo = "Rapidez",
                                        emoji = "⚡",
                                        puntos = bonificacionRapidez,
                                        color = EduRachaColors.Info
                                    )
                                }
                                if (bonificacionPrimeraVez > 0) {
                                    BonificacionCard(
                                        icono = Icons.Default.Celebration,
                                        titulo = "Primera vez",
                                        emoji = "🎊",
                                        puntos = bonificacionPrimeraVez,
                                        color = Color(0xFFAB47BC)
                                    )
                                }
                                if (bonificacionTodoCorrecto > 0) {
                                    BonificacionCard(
                                        icono = Icons.Default.WorkspacePremium,
                                        titulo = "Perfecto",
                                        emoji = "🏆",
                                        puntos = bonificacionTodoCorrecto,
                                        color = EduRachaColors.Warning
                                    )
                                }
                            }
                        }

                        // ✅ Racha SOLO si modo oficial Y aprobó
                        if (modo == "oficial" && aprobo && diasRacha > 0) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFF6B35).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B35),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Racha de estudio",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EduRachaColors.TextSecondary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = "$diasRacha",
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF6B35)
                                            )
                                            Text(
                                                text = "días seguidos 🔥",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EduRachaColors.TextPrimary,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Modo práctica desbloqueado (solo si es oficial Y aprobó)
            if (aprobo && modo == "oficial") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EduRachaColors.Info.copy(alpha = 0.08f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EduRachaColors.Info.copy(alpha = 0.18f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = EduRachaColors.Info,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "¡Desbloqueado!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EduRachaColors.TextSecondary
                                    )
                                    Text(
                                        text = "🎯 Modo Práctica",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EduRachaColors.Info
                                    )
                                }
                            }

                            Text(
                                text = "Aprobaste el tema. Ahora puedes seguir practicando. Ganas XP y pierdes vidas, pero NO cuenta para racha.",
                                fontSize = 15.sp,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = onIniciarPractica,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EduRachaColors.Info
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        "Seguir Practicando",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botones de acción
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ Retroalimentación solo si modo NO es práctica
                    if (preguntasIncorrectas > 0 && modo != "practica") {
                        OutlinedButton(
                            onClick = onVerRetroalimentacion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                2.5.dp,
                                EduRachaColors.Warning
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EduRachaColors.Warning
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        "Ver Retroalimentación",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$preguntasIncorrectas ${if (preguntasIncorrectas == 1) "pregunta" else "preguntas"} para revisar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onVolverACursos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp
                        )
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
                                "Volver a Mis Cursos",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ============================================
// 🎨 COMPONENTES AUXILIARES
// ============================================

@Composable
private fun IconoResultadoAnimado(
    aprobo: Boolean,
    porcentaje: Int,
    colorResultado: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "resultado")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = colorResultado.copy(alpha = 0.12f),
            modifier = Modifier
                .size(130.dp)
                .scale(scale)
        ) {}

        Surface(
            shape = CircleShape,
            color = colorResultado.copy(alpha = 0.25f),
            modifier = Modifier.size(95.dp)
        ) {}

        val icono = when {
            porcentaje == 100 -> Icons.Default.WorkspacePremium
            aprobo -> Icons.Default.EmojiEvents
            porcentaje >= 60 -> Icons.Default.Psychology
            else -> Icons.Default.AutoStories
        }

        Icon(
            icono,
            contentDescription = null,
            tint = colorResultado,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
private fun ContadorRespuestas(
    cantidad: Int,
    total: Int,
    tipo: String,
    color: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "$cantidad",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = tipo,
            fontSize = 13.sp,
            color = EduRachaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BarraEstadistica(
    label: String,
    valor: Int,
    total: Int,
    color: Color
) {
    val progreso = if (total > 0) valor.toFloat() / total.toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "$valor de $total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.8f),
                                color
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun BonificacionCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    emoji: String,
    puntos: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icono,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = titulo,
                        fontSize = 15.sp,
                        color = EduRachaColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = emoji,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "+$puntos XP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}