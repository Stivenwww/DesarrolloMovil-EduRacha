package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme

class DetalleEstudianteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos del estudiante desde el Intent
        val estudianteId = intent.getStringExtra("ESTUDIANTE_ID") ?: ""
        val estudianteNombre = intent.getStringExtra("ESTUDIANTE_NOMBRE") ?: "Estudiante"
        val estudianteEmail = intent.getStringExtra("ESTUDIANTE_EMAIL") ?: ""
        val rachaActual = intent.getIntExtra("ESTUDIANTE_RACHA", 0)
        val rachaMejor = intent.getIntExtra("ESTUDIANTE_RACHA_MEJOR", 0)
        val puntosTotal = intent.getIntExtra("ESTUDIANTE_PUNTOS", 0)
        val preguntasRespondidas = intent.getIntExtra("ESTUDIANTE_PREGUNTAS", 0)
        val preguntasCorrectas = intent.getIntExtra("ESTUDIANTE_CORRECTAS", 0)
        val ranking = intent.getIntExtra("ESTUDIANTE_RANKING", 0)

        setContent {
            EduRachaTheme {
                DetalleEstudianteScreen(
                    nombre = estudianteNombre,
                    email = estudianteEmail,
                    rachaActual = rachaActual,
                    rachaMejor = rachaMejor,
                    puntosTotal = puntosTotal,
                    preguntasRespondidas = preguntasRespondidas,
                    preguntasCorrectas = preguntasCorrectas,
                    ranking = ranking,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun DetalleEstudianteScreen(
    nombre: String,
    email: String,
    rachaActual: Int,
    rachaMejor: Int,
    puntosTotal: Int,
    preguntasRespondidas: Int,
    preguntasCorrectas: Int,
    ranking: Int,
    onNavigateBack: () -> Unit
) {
    val precision = if (preguntasRespondidas > 0) {
        ((preguntasCorrectas.toFloat() / preguntasRespondidas.toFloat()) * 100).toInt()
    } else 0

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con gradiente y perfil
            DetalleHeader(
                nombre = nombre,
                email = email,
                ranking = ranking,
                onNavigateBack = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de Racha
                RachaCard(
                    rachaActual = rachaActual,
                    rachaMejor = rachaMejor
                )

                // Card de Estadísticas Generales
                EstadisticasGeneralesCard(
                    puntosTotal = puntosTotal,
                    precision = precision,
                    preguntasRespondidas = preguntasRespondidas
                )

                // Card de Rendimiento
                RendimientoCard(
                    preguntasCorrectas = preguntasCorrectas,
                    preguntasIncorrectas = preguntasRespondidas - preguntasCorrectas,
                    totalPreguntas = preguntasRespondidas
                )

                // Información adicional
                InfoAdicionalCard()
            }
        }
    }
}

@Composable
fun DetalleHeader(
    nombre: String,
    email: String,
    ranking: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Botón volver
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar y datos del estudiante
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val iniciales = nombre.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull() }
                            .joinToString("")

                        Text(
                            text = iniciales,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary
                        )
                    }

                    // Badge de ranking
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp),
                        shape = CircleShape,
                        color = getRankingColor(ranking),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "#$ranking",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = nombre,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun RachaCard(
    rachaActual: Int,
    rachaMejor: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.StreakFire.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = EduRachaColors.StreakFire,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Racha de Aprendizaje",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = EduRachaColors.Background
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Racha Actual
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (rachaActual > 0)
                        EduRachaColors.StreakFire.copy(alpha = 0.15f)
                    else
                        EduRachaColors.Background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = if (rachaActual > 0) EduRachaColors.StreakFire else EduRachaColors.TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$rachaActual",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rachaActual > 0) EduRachaColors.StreakFire else EduRachaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Días Actual",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Mejor Racha
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$rachaMejor",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mejor Racha",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (rachaActual > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Success.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "¡Racha activa! Sigue así",
                            fontSize = 13.sp,
                            color = EduRachaColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticasGeneralesCard(
    puntosTotal: Int,
    precision: Int,
    preguntasRespondidas: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Estadísticas Generales",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = EduRachaColors.Background
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    icon = Icons.Default.Star,
                    label = "Puntos",
                    value = "$puntosTotal",
                    color = EduRachaColors.Accent,
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    icon = Icons.Default.Percent,
                    label = "Precisión",
                    value = "$precision%",
                    color = EduRachaColors.Success,
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    icon = Icons.Default.QuestionAnswer,
                    label = "Respondidas",
                    value = "$preguntasRespondidas",
                    color = EduRachaColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RendimientoCard(
    preguntasCorrectas: Int,
    preguntasIncorrectas: Int,
    totalPreguntas: Int
) {
    val porcentajeCorrectas = if (totalPreguntas > 0) {
        (preguntasCorrectas.toFloat() / totalPreguntas.toFloat())
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Success.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = EduRachaColors.Success,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Rendimiento",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = EduRachaColors.Background
            )

            // Barra de progreso
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Precisión general",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                    Text(
                        text = "${(porcentajeCorrectas * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Success
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { porcentajeCorrectas },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = EduRachaColors.Success,
                    trackColor = EduRachaColors.Background
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Desglose de respuestas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnswerCard(
                    icon = Icons.Default.CheckCircle,
                    label = "Correctas",
                    value = "$preguntasCorrectas",
                    color = EduRachaColors.Success,
                    modifier = Modifier.weight(1f)
                )

                AnswerCard(
                    icon = Icons.Default.Cancel,
                    label = "Incorrectas",
                    value = "$preguntasIncorrectas",
                    color = EduRachaColors.Error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AnswerCard(
    icon: ImageVector,
    label: String,
    value: String,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun InfoAdicionalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Primary.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Los datos se actualizan en tiempo real según la actividad del estudiante",
                fontSize = 13.sp,
                color = EduRachaColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

