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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.models.Tema
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.EduRachaV2Colors
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.tasks.await

class TemasDelCursoActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var cursoNombre: String
    private var temas: List<Tema> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarDatosCurso()

        if (savedInstanceState == null) {
            quizViewModel.iniciarObservadores(cursoId)
            quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
        }

        setContent {
            EduRachaTheme {
                TemasDelCursoScreenFinal(
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
    }

    override fun onResume() {
        super.onResume()
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            quizViewModel.detenerObservadores()
        }
    }
}

// ============================================================================
// PANTALLA PRINCIPAL
// ============================================================================

@Composable
fun TemasDelCursoScreenFinal(
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

    var quizFinalAprobado by remember { mutableStateOf(false) }
    var temasAprobados by remember { mutableStateOf(setOf<String>()) }

    var mostrarDialogoVidas by remember { mutableStateOf(false) }
    var mostrarDialogoRacha by remember { mutableStateOf(false) }
    var mostrarDialogoXP by remember { mutableStateOf(false) }

    // Cargar estado de temas aprobados
    LaunchedEffect(cursoId) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        if (userUid.isNotEmpty() && cursoId.isNotEmpty()) {
            try {
                val database = FirebaseDatabase.getInstance()
                val snapshot = database.getReference("quizzes").get().await()

                val aprobados = mutableSetOf<String>()
                var finalAprobado = false

                if (snapshot.exists()) {
                    for (quizSnapshot in snapshot.children) {
                        val estudianteId = quizSnapshot.child("estudianteId").getValue(String::class.java)
                        if (estudianteId == userUid) {
                            val quizCursoId = quizSnapshot.child("cursoId").getValue(String::class.java)
                            val estado = quizSnapshot.child("estado").getValue(String::class.java)
                            val modo = quizSnapshot.child("modo").getValue(String::class.java)
                            val temaId = quizSnapshot.child("temaId").getValue(String::class.java)

                            if (quizCursoId == cursoId && estado == "finalizado") {
                                val correctas = quizSnapshot.child("preguntasCorrectas").getValue(Int::class.java) ?: 0
                                val incorrectas = quizSnapshot.child("preguntasIncorrectas").getValue(Int::class.java) ?: 0
                                val total = correctas + incorrectas

                                if (total > 0) {
                                    val porcentaje = (correctas * 100) / total
                                    if (porcentaje >= 80) {
                                        if (modo == "final") {
                                            finalAprobado = true
                                        } else if (temaId != null) {
                                            aprobados.add(temaId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                temasAprobados = aprobados
                quizFinalAprobado = finalAprobado
            } catch (e: Exception) {
                Log.e("TemasDelCurso", "Error: ${e.message}")
            }
        }
    }

    if (uiState.mostrarDialogoSinVidas) {
        DialogoSinVidasMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
        )
    }

    // Diálogos compactos
    if (mostrarDialogoVidas) {
        DialogoVidasCompacto(
            vidasActuales = uiState.vidas?.vidasActuales ?: 5,
            vidasMax = uiState.vidas?.vidasMax ?: 5,
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0,
            onDismiss = { mostrarDialogoVidas = false }
        )
    }

    if (mostrarDialogoRacha) {
        DialogoRachaCompacto(
            rachaDias = uiState.progreso?.rachaDias ?: 0,
            onDismiss = { mostrarDialogoRacha = false }
        )
    }

    if (mostrarDialogoXP) {
        DialogoXPCompacto(
            xpActual = uiState.progreso?.experiencia ?: 0,
            onDismiss = { mostrarDialogoXP = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // HEADER AZUL CON LIBROS 3D Y BURBUJAS
            item {
                HeaderFigmaConBurbujas(
                    cursoNombre = cursoNombre,
                    onNavigateBack = onNavigateBack
                )
            }

            // 3 CARDS DE ESTADÍSTICAS CON CLICK
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardFigmaInteractive(
                        icon = "❤️",
                        valor = "${uiState.vidas?.vidasActuales ?: 5}/${uiState.vidas?.vidasMax ?: 5}",
                        label = "Vidas",
                        color = EduRachaV2Colors.Pink,
                        onClick = { mostrarDialogoVidas = true },
                        modifier = Modifier.weight(1f)
                    )
                    StatCardFigmaInteractive(
                        icon = "🔥",
                        valor = "${uiState.progreso?.rachaDias ?: 0}",
                        label = "Racha",
                        color = EduRachaV2Colors.Warning,
                        onClick = { mostrarDialogoRacha = true },
                        modifier = Modifier.weight(1f)
                    )
                    StatCardFigmaInteractive(
                        icon = "⭐",
                        valor = "${uiState.progreso?.experiencia ?: 0}",
                        label = "XP Total",
                        color = EduRachaV2Colors.Accent,
                        onClick = { mostrarDialogoXP = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // CARD DE PROGRESO DE XP
            item {
                ProgresoXPCard(
                    xpActual = uiState.progreso?.experiencia ?: 0,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // CARD MORADA CON MASCOTA
            item {
                MascotaCardFigma(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // TÍTULO "Temas del Curso"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = EduRachaV2Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Temas del Curso",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }
            }

            // LISTA DE TEMAS
            itemsIndexed(temas.sortedBy { it.orden }) { index, tema ->
                TemaCardFigmaGrande(
                    tema = tema,
                    numero = index + 1,
                    xpReward = 100 + (index * 50),
                    isAprobado = temasAprobados.contains(tema.id),
                    onClick = { onTemaClick(tema) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // QUIZ FINAL
            if (temasAprobados.size == temas.size && temas.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    QuizFinalCardFigma(
                        onClick = {
                            if (quizFinalAprobado) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Ya aprobaste el Quiz Final",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onQuizFinalClick()
                            }
                        },
                        yaAprobado = quizFinalAprobado,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// HEADER CON BURBUJAS Y LIBROS 3D
// ============================================================================

@Composable
fun HeaderFigmaConBurbujas(
    cursoNombre: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5B8EFF),
                        Color(0xFF4776E6)
                    )
                )
            )
            .statusBarsPadding()
            .padding(bottom = 48.dp)
    ) {
        // BURBUJAS DECORATIVAS
        BurbujasDecorativas()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Libros3DRealistas()

            Spacer(Modifier.height(24.dp))

            Text(
                cursoNombre,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "¡Sigue practicando!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BurbujasDecorativas() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

    val burbuja1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b1"
    )

    val burbuja2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 250.dp, y = 50.dp)
                .graphicsLayer { translationY = burbuja1Y }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-20).dp, y = 100.dp)
                .graphicsLayer { translationY = burbuja2Y }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = 180.dp, y = 150.dp)
                .graphicsLayer { translationY = -burbuja1Y * 0.5f }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
    }
}

@Composable
fun Libros3DRealistas() {
    val infiniteTransition = rememberInfiniteTransition(label = "books")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(65.dp, 80.dp)
                .offset(x = (-20).dp, y = offsetY.dp)
                .rotate(-12f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-4).dp, y = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF5FA552))
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF7BC96F),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .background(Color.White.copy(0.4f))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(0.3f))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(65.dp, 80.dp)
                .offset(y = (offsetY - 10).dp)
                .rotate(3f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-4).dp, y = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD94545))
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF6B6B),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .background(Color.White.copy(0.4f))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(0.3f))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(65.dp, 80.dp)
                .offset(x = 20.dp, y = (offsetY + 5).dp)
                .rotate(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-4).dp, y = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3BA89F))
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF4ECDC4),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .background(Color.White.copy(0.4f))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(0.3f))
                    )
                }
            }
        }
    }
}

// ============================================================================
// STAT CARDS INTERACTIVAS
// ============================================================================

@Composable
fun StatCardFigmaInteractive(
    icon: String,
    valor: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = if (isPressed) 2.dp else 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 32.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                valor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// CARD DE PROGRESO DE XP
// ============================================================================

@Composable
fun ProgresoXPCard(
    xpActual: Int,
    modifier: Modifier = Modifier
) {
    val proximaMeta = when {
        xpActual < 500 -> 500
        xpActual < 1000 -> 1000
        xpActual < 2000 -> 2000
        xpActual < 5000 -> 5000
        else -> ((xpActual / 5000) + 1) * 5000
    }

    val progreso = (xpActual.toFloat() / proximaMeta).coerceIn(0f, 1f)
    val xpFaltante = (proximaMeta - xpActual).coerceAtLeast(0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 28.sp)
                    Text(
                        "Progreso de XP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }

                Text(
                    "$xpActual/$proximaMeta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B6B6B)
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFFFF3CD))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progreso)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD93D),
                                    Color(0xFFFFC864)
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "¡Solo $xpFaltante XP más para alcanzar $proximaMeta XP! 🎉",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// MASCOTA CARD
// ============================================================================

@Composable
fun MascotaCardFigma(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF8B5CF6),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BuhoAnimado()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¡Vas muy bien!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mantén tu racha completando un tema hoy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.9f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun BuhoAnimado() {
    val infiniteTransition = rememberInfiniteTransition(label = "owl")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color.White.copy(0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text("🦉", fontSize = 48.sp)
    }
}

// ============================================================================
// TEMA CARDS
// ============================================================================

@Composable
fun TemaCardFigmaGrande(
    tema: Tema,
    numero: Int,
    xpReward: Int,
    isAprobado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    tema.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D),
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                if (isAprobado) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF7BC96F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Aprobado",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐", fontSize = 18.sp)
                Text(
                    "+$xpReward XP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFC864)
                )
            }

            Spacer(Modifier.height(20.dp))

            BotonRepasarTema(onClick = onClick, isAprobado = isAprobado)
        }
    }
}

@Composable
fun BotonRepasarTema(onClick: () -> Unit, isAprobado: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF8B5CF6),
        shadowElevation = if (isPressed) 2.dp else 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isAprobado) "Repasar tema" else "Comenzar tema",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ============================================================================
// QUIZ FINAL CARD
// ============================================================================

@Composable
fun QuizFinalCardFigma(
    onClick: () -> Unit,
    yaAprobado: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")
    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (yaAprobado) Color(0xFF7BC96F) else Color(0xFFFFD93D),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !yaAprobado, onClick = onClick)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.scale(if (!yaAprobado) trophyScale else 1f)) {
                Text("🏆", fontSize = 64.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                if (yaAprobado) "¡Quiz Final Completado!" else "Quiz Final Disponible",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                if (yaAprobado)
                    "Has completado este curso exitosamente"
                else
                    "Completa el quiz final para certificarte",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(0.95f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (!yaAprobado) {
                Spacer(Modifier.height(24.dp))

                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFFD93D),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Comenzar Examen",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD93D)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIÁLOGOS COMPACTOS
// ============================================================================

@Composable
fun DialogoVidasCompacto(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❤️", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Vidas",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = EduRachaV2Colors.Pink
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "$vidasActuales",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = EduRachaV2Colors.Pink
                    )
                    Text(
                        "/ $vidasMax",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaV2Colors.Pink.copy(0.1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Se pierde 1 vida al responder mal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2D2D2D),
                            textAlign = TextAlign.Center
                        )
                        if (vidasActuales < vidasMax) {
                            Spacer(Modifier.height(8.dp))
                            Divider(color = EduRachaV2Colors.Pink.copy(0.2f))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Se recupera 1 vida cada 30 minutos",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaV2Colors.Pink,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaV2Colors.Pink
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DialogoRachaCompacto(
    rachaDias: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔥", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Racha",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = EduRachaV2Colors.Warning
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "$rachaDias",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = EduRachaV2Colors.Warning
                    )
                    Text(
                        "días",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaV2Colors.Warning.copy(0.1f)
                ) {
                    Text(
                        when {
                            rachaDias == 0 -> "¡Comienza tu racha hoy!"
                            rachaDias < 7 -> "¡Excelente inicio!"
                            rachaDias < 30 -> "¡Increíble dedicación!"
                            else -> "¡Eres una leyenda!"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D2D2D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaV2Colors.Warning
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DialogoXPCompacto(
    xpActual: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⭐", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Experiencia",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = EduRachaV2Colors.Accent
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "$xpActual",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = EduRachaV2Colors.Accent
                    )
                    Text(
                        "XP",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaV2Colors.Accent.copy(0.1f)
                ) {
                    Text(
                        "Completa temas y quizzes para ganar más experiencia",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D2D2D),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaV2Colors.Accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Continuar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}