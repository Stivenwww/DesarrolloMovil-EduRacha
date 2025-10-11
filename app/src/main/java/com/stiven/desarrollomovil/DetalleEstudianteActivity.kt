package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import kotlinx.coroutines.delay

// ============================================
// ACTIVITY
// ============================================
class DetalleEstudianteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nombre = intent.getStringExtra("ESTUDIANTE_NOMBRE") ?: "Estudiante"
        val email = intent.getStringExtra("ESTUDIANTE_EMAIL") ?: ""
        val rachaActual = intent.getIntExtra("ESTUDIANTE_RACHA", 0)
        val rachaMejor = intent.getIntExtra("ESTUDIANTE_RACHA_MEJOR", 0)
        val puntos = intent.getIntExtra("ESTUDIANTE_PUNTOS", 0)
        val preguntas = intent.getIntExtra("ESTUDIANTE_PREGUNTAS", 0)
        val correctas = intent.getIntExtra("ESTUDIANTE_CORRECTAS", 0)
        val posicion = intent.getIntExtra("ESTUDIANTE_RANKING", 0)

        setContent {
            EduRachaTheme {
                DetalleEstudianteScreen(
                    nombre = nombre,
                    email = email,
                    rachaActual = rachaActual,
                    rachaMejor = rachaMejor,
                    puntos = puntos,
                    preguntas = preguntas,
                    correctas = correctas,
                    posicion = posicion,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// ============================================
// SCREEN PRINCIPAL
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleEstudianteScreen(
    nombre: String,
    email: String,
    rachaActual: Int,
    rachaMejor: Int,
    puntos: Int,
    preguntas: Int,
    correctas: Int,
    posicion: Int,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val nivel = (puntos / 100) + 1
    val precision = if (preguntas > 0) {
        ((correctas.toDouble() / preguntas.toDouble()) * 100).toInt()
    } else 0
    val puntosParaSiguienteNivel = (nivel * 100) - (puntos % 100)
    val progreso = ((puntos % 100).toFloat() / 100f)

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Premium con Avatar
            DetalleEstudianteHeader(
                nombre = nombre,
                email = email,
                onNavigateBack = onNavigateBack
            )

            // Contenido scrolleable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de Ranking
                RankingCard(posicion = posicion)

                // Título de estadísticas
                Text(
                    text = "Estadísticas Generales",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Grid de estadísticas principales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EstadisticaCard(
                        icon = Icons.Default.Star,
                        valor = puntos.toString(),
                        label = "Puntos",
                        color = EduRachaColors.Accent,
                        modifier = Modifier.weight(1f)
                    )

                    EstadisticaCard(
                        icon = Icons.Default.TrendingUp,
                        valor = nivel.toString(),
                        label = "Nivel",
                        color = EduRachaColors.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EstadisticaCardRacha(
                        rachaActual = rachaActual,
                        rachaMejor = rachaMejor,
                        modifier = Modifier.weight(1f)
                    )

                    EstadisticaCardPrecision(
                        precision = precision,
                        correctas = correctas,
                        preguntas = preguntas,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Card de progreso
                ProgresoNivelCard(
                    nivel = nivel,
                    progreso = progreso,
                    puntosParaSiguienteNivel = puntosParaSiguienteNivel
                )

                // Actividad reciente
                Text(
                    text = "Actividad Reciente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                ActividadRecienteCard()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ============================================
// HEADER PREMIUM CON AVATAR
// ============================================
@Composable
fun DetalleEstudianteHeader(
    nombre: String,
    email: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        // Botón de volver
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        // Contenido centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(100)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nombre.split(" ").take(2).joinToString("") { it.first().toString() },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre
            Text(
                text = nombre,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Email
            Text(
                text = email,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
// Define una clase de datos para que el código sea más limpio y legible
private data class RankingInfo(
    val title: String,    val description: String,
    val color: Color,
    val showTrophy: Boolean
)

// ============================================
// RANKING CARD (CORREGIDO)
// ============================================
@Composable
fun RankingCard(posicion: Int) {
    // CORRECCIÓN: Se usa la nueva data class en lugar de 'Triple'
    val rankingInfo = when (posicion) {
        1 -> RankingInfo("Primer Lugar", "¡Excelente desempeño! 🏆", EduRachaColors.RankingGold, true)
        2 -> RankingInfo("Segundo Lugar", "¡Muy buen trabajo! 🥈", EduRachaColors.RankingSilver, true)
        3 -> RankingInfo("Tercer Lugar", "¡Sigue así! 🥉", EduRachaColors.RankingBronze, true)
        else -> RankingInfo("Posición #$posicion", "Continúa mejorando", EduRachaColors.Primary, false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Posición en Ranking",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Badge de posición
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(rankingInfo.color), // Se usa desde la data class
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = posicion.toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = rankingInfo.title, // Se usa desde la data class
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    Text(
                        text = rankingInfo.description, // Se usa desde la data class
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Trofeo
                if (rankingInfo.showTrophy) { // Se usa desde la data class
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trofeo",
                        tint = rankingInfo.color,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}


// ============================================
// ESTADÍSTICA CARD
// ============================================
@Composable
fun EstadisticaCard(
    icon: ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = valor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================
// ESTADÍSTICA CARD RACHA
// ============================================
@Composable
fun EstadisticaCardRacha(
    rachaActual: Int,
    rachaMejor: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.StreakFire.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = EduRachaColors.StreakFire,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = rachaActual.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Text(
                    text = "Racha Actual",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Mejor: $rachaMejor",
                    fontSize = 10.sp,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================
// ESTADÍSTICA CARD PRECISIÓN
// ============================================
@Composable
fun EstadisticaCardPrecision(
    precision: Int,
    correctas: Int,
    preguntas: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EduRachaColors.Success,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$precision%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                Text(
                    text = "Precisión",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "$correctas/$preguntas",
                    fontSize = 10.sp,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================
// PROGRESO NIVEL CARD
// ============================================
@Composable
fun ProgresoNivelCard(
    nivel: Int,
    progreso: Float,
    puntosParaSiguienteNivel: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Progreso al siguiente nivel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LinearProgressIndicator(
                progress = progreso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EduRachaColors.Primary,
                trackColor = EduRachaColors.SurfaceVariant
            )

            Text(
                text = "$puntosParaSiguienteNivel puntos para nivel ${nivel + 1}",
                fontSize = 14.sp,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

// ============================================
// ACTIVIDAD RECIENTE CARD
// ============================================
@Composable
fun ActividadRecienteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Item 1
            ActividadItem(
                icon = Icons.Default.CheckCircle,
                iconColor = EduRachaColors.Success,
                titulo = "Completó cuestionario de Matemáticas",
                descripcion = "Hace 2 horas • 9/10 correctas"
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = EduRachaColors.SurfaceVariant
            )

            // Item 2
            ActividadItem(
                icon = Icons.Default.Whatshot,
                iconColor = EduRachaColors.StreakFire,
                titulo = "Racha de 15 días consecutivos",
                descripcion = "Hoy • ¡Sigue así!"
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = EduRachaColors.SurfaceVariant
            )

            // Item 3
            ActividadItem(
                icon = Icons.Default.TrendingUp,
                iconColor = EduRachaColors.Accent,
                titulo = "Subió al nivel 5",
                descripcion = "Hace 1 día"
            )
        }
    }
}

@Composable
fun ActividadItem(
    icon: ImageVector,
    iconColor: Color,
    titulo: String,
    descripcion: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )

            Text(
                text = descripcion,
                fontSize = 12.sp,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}