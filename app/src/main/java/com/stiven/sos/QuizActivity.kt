package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel

class QuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Quiz"

        // Iniciar el quiz automáticamente
        quizViewModel.iniciarQuiz(cursoId, temaId)

        setContent {
            EduRachaTheme {
                QuizScreen(
                    temaTitulo = temaTitulo,
                    quizViewModel = quizViewModel,
                    onNavigateToResultado = {
                        val intent = Intent(this, ResultadoQuizActivity::class.java)
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
    temaTitulo: String,
    quizViewModel: QuizViewModel,
    onNavigateToResultado: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    // Variable para controlar si ya se navegó a resultados
    var yaNavego by remember { mutableStateOf(false) }

    // Navegar automáticamente a resultados cuando se finaliza
    LaunchedEffect(uiState.resultadoQuiz) {
        if (uiState.resultadoQuiz != null && !yaNavego) {
            yaNavego = true
            onNavigateToResultado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            temaTitulo,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Pregunta ${uiState.preguntaActual + 1} de ${uiState.quizActivo?.preguntas?.size ?: 0}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = EduRachaColors.Primary)
                        Text(
                            text = "Cargando quiz...",
                            color = EduRachaColors.TextSecondary
                        )
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = EduRachaColors.Error
                        )
                        Text(
                            text = "Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }

            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    PreguntaScreen(
                        pregunta = preguntaActual,
                        numeroPregunta = uiState.preguntaActual + 1,
                        totalPreguntas = quiz.preguntas.size,
                        onRespuestaSeleccionada = { opcionId ->
                            // Registrar la respuesta
                            quizViewModel.responderPregunta(
                                preguntaId = preguntaActual.id,
                                respuestaSeleccionada = opcionId
                            )

                            // Si es la última pregunta, finalizar el quiz
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

@Composable
fun PreguntaScreen(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    onRespuestaSeleccionada: (Int) -> Unit
) {
    var respuestaSeleccionada by remember { mutableStateOf<Int?>(null) }
    var botonPresionado by remember { mutableStateOf(false) } // ← Nueva variable

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Indicador de progreso
        LinearProgressIndicator(
            progress = numeroPregunta.toFloat() / totalPreguntas.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = EduRachaColors.Primary,
            trackColor = EduRachaColors.Primary.copy(alpha = 0.2f)
        )

        Spacer(Modifier.height(8.dp))

        // Texto de la pregunta
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Icons.Default.Quiz,
                        contentDescription = null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Pregunta $numeroPregunta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = pregunta.texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    lineHeight = 26.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Opciones
        pregunta.opciones.forEachIndexed { index, opcion ->
            OpcionCard(
                opcion = opcion,
                index = index,
                isSelected = respuestaSeleccionada == index,
                onClick = {
                    if (!botonPresionado) { // ← Solo permitir si no se presionó el botón
                        respuestaSeleccionada = index
                    }
                }
            )
        }

        Spacer(Modifier.weight(1f))

        // Botón de confirmar
        Button(
            onClick = {
                respuestaSeleccionada?.let {
                    if (!botonPresionado) { // ← Evitar doble clic
                        botonPresionado = true
                        onRespuestaSeleccionada(it)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = respuestaSeleccionada != null && !botonPresionado, // ← Deshabilitar después del clic
            colors = ButtonDefaults.buttonColors(
                containerColor = EduRachaColors.Primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (botonPresionado) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = if (numeroPregunta < totalPreguntas) "Siguiente" else "Finalizar Quiz",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OpcionCard(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                EduRachaColors.Primary.copy(alpha = 0.15f)
            else
                Color.White
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, EduRachaColors.Primary)
        else
            null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = EduRachaColors.Primary,
                    unselectedColor = EduRachaColors.TextSecondary
                )
            )

            Text(
                text = opcion.texto,
                fontSize = 16.sp,
                color = if (isSelected) EduRachaColors.Primary else EduRachaColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}