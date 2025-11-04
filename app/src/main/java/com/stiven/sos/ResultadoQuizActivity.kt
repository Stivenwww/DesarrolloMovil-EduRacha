package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.BonificacionesResponse
import com.stiven.sos.models.FinalizarQuizResponse
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme

class ResultadoQuizActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preguntasCorrectas = intent.getIntExtra("preguntasCorrectas", 0)
        val preguntasIncorrectas = intent.getIntExtra("preguntasIncorrectas", 0)
        val experienciaGanada = intent.getIntExtra("experienciaGanada", 0)
        val vidasRestantes = intent.getIntExtra("vidasRestantes", 0)
        val bonificacionRapidez = intent.getIntExtra("bonificacionRapidez", 0)
        val bonificacionPrimeraVez = intent.getIntExtra("bonificacionPrimeraVez", 0)
        val bonificacionTodoCorrecto = intent.getIntExtra("bonificacionTodoCorrecto", 0)
        val quizId = intent.getStringExtra("quizId") ?: ""

        val resultado = FinalizarQuizResponse(
            preguntasCorrectas = preguntasCorrectas,
            preguntasIncorrectas = preguntasIncorrectas,
            experienciaGanada = experienciaGanada,
            vidasRestantes = vidasRestantes,
            bonificaciones = BonificacionesResponse(
                rapidez = bonificacionRapidez,
                primeraVez = bonificacionPrimeraVez,
                todoCorrecto = bonificacionTodoCorrecto
            )
        )

        setContent {
            EduRachaTheme {
                ResultadoQuizScreen(
                    resultado = resultado,
                    quizId = quizId,
                    onVolverACursos = { finish() },
                    onReintentarQuiz = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoQuizScreen(
    resultado: FinalizarQuizResponse,
    quizId: String,
    onVolverACursos: () -> Unit,
    onReintentarQuiz: () -> Unit
) {
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Resultado del Quiz",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                val totalPreguntas = resultado.preguntasCorrectas + resultado.preguntasIncorrectas
                val porcentaje = if (totalPreguntas > 0) {
                    (resultado.preguntasCorrectas * 100) / totalPreguntas
                } else 0

                val (icono, color, mensaje) = when {
                    porcentaje >= 90 -> Triple(Icons.Default.EmojiEvents, EduRachaColors.Success, "¡Excelente!")
                    porcentaje >= 70 -> Triple(Icons.Default.ThumbUp, EduRachaColors.Info, "¡Bien hecho!")
                    else -> Triple(Icons.Default.SentimentDissatisfied, EduRachaColors.Warning, "Sigue practicando")
                }

                val scale by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "icon_scale"
                )

                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icono,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Text(
                    text = mensaje,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Text(
                    text = "$porcentaje% de respuestas correctas",
                    fontSize = 16.sp,
                    color = EduRachaColors.TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                // Card de estadísticas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Resultados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            EstadisticaItem(
                                icono = Icons.Default.CheckCircle,
                                color = EduRachaColors.Success,
                                valor = resultado.preguntasCorrectas.toString(),
                                label = "Correctas"
                            )
                            EstadisticaItem(
                                icono = Icons.Default.Cancel,
                                color = EduRachaColors.Error,
                                valor = resultado.preguntasIncorrectas.toString(),
                                label = "Incorrectas"
                            )
                            EstadisticaItem(
                                icono = Icons.Default.Star,
                                color = EduRachaColors.Warning,
                                valor = resultado.experienciaGanada.toString(),
                                label = "XP"
                            )
                        }
                    }
                }

                // Card de bonificaciones
                if (resultado.bonificaciones.rapidez > 0 ||
                    resultado.bonificaciones.primeraVez > 0 ||
                    resultado.bonificaciones.todoCorrecto > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EduRachaColors.Warning.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning
                                )
                                Text(
                                    text = "Bonificaciones",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                            }

                            if (resultado.bonificaciones.rapidez > 0) {
                                BonificacionItem(
                                    texto = "Rapidez",
                                    xp = resultado.bonificaciones.rapidez
                                )
                            }
                            if (resultado.bonificaciones.primeraVez > 0) {
                                BonificacionItem(
                                    texto = "Primera vez",
                                    xp = resultado.bonificaciones.primeraVez
                                )
                            }
                            if (resultado.bonificaciones.todoCorrecto > 0) {
                                BonificacionItem(
                                    texto = "Todo correcto",
                                    xp = resultado.bonificaciones.todoCorrecto
                                )
                            }
                        }
                    }
                }

                // Card de vidas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (resultado.vidasRestantes > 0)
                                EduRachaColors.Error
                            else
                                EduRachaColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Vidas restantes",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                            Text(
                                text = "${resultado.vidasRestantes}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    resultado.vidasRestantes == 0 -> EduRachaColors.Error
                                    resultado.vidasRestantes <= 2 -> EduRachaColors.Warning
                                    else -> EduRachaColors.TextPrimary
                                }
                            )
                        }

                        if (resultado.vidasRestantes == 0) {
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Error.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Sin vidas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // ✅ Botones de acción CORREGIDOS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ Botón de retroalimentación - SIEMPRE visible
                    if (quizId.isNotEmpty()) {
                        Button(
                            onClick = {
                                val intent = Intent(context, RetroalimentacionActivity::class.java)
                                intent.putExtra("quizId", quizId)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (resultado.preguntasIncorrectas > 0)
                                    EduRachaColors.Warning
                                else
                                    EduRachaColors.Success
                            )
                        ) {
                            Icon(
                                if (resultado.preguntasIncorrectas > 0)
                                    Icons.Default.Lightbulb
                                else
                                    Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (resultado.preguntasIncorrectas > 0)
                                    "Ver Errores (${resultado.preguntasIncorrectas})"
                                else
                                    "Quiz Perfecto ✓",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onVolverACursos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Volver a Cursos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (resultado.vidasRestantes > 0) {
                        OutlinedButton(
                            onClick = onReintentarQuiz,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Reintentar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = EduRachaColors.Info.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EduRachaColors.Info
                                )
                                Text(
                                    text = "Espera 30 minutos para recuperar una vida",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.Info
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaItem(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    valor: String,
    label: String
) {
    var targetValue by remember { mutableStateOf(0) }
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "counter"
    )

    LaunchedEffect(valor) {
        targetValue = valor.toIntOrNull() ?: 0
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = animatedValue.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary
        )
    }
}

@Composable
fun BonificacionItem(texto: String, xp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            color = EduRachaColors.TextPrimary
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = EduRachaColors.Warning.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+$xp",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Warning
                )
                Text(
                    text = "XP",
                    fontSize = 12.sp,
                    color = EduRachaColors.Warning
                )
            }
        }
    }
}