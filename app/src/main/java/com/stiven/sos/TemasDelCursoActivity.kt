package com.stiven.sos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Tema
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.services.IndicadorVidasMejorado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class TemasDelCursoActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var cursoNombre: String
    private var temas: List<Tema> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos del intent
        cursoId = intent.getStringExtra("curso_id") ?: ""
        cursoNombre = intent.getStringExtra("curso_nombre") ?: "Curso"

        temas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("curso_temas", Tema::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("curso_temas") ?: emptyList()
        }

        // Guardar curso en preferencias
        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_curso_id", cursoId)
            commit()
        }

        setContent {
            EduRachaTheme {
                LaunchedEffect(cursoId) {
                    quizViewModel.iniciarObservadores(cursoId)
                    quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        quizViewModel.detenerObservadores()
                    }
                }

                TemasDelCursoScreen(
                    cursoNombre = cursoNombre,
                    cursoId = cursoId,
                    temas = temas,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onTemaClick = { tema, yaResolviHoy ->
                        prefs.edit().apply {
                            putString("last_tema_id", tema.id)
                            commit()
                        }

                        val vidas = quizViewModel.uiState.value.vidas?.vidasActuales ?: 0
                        if (vidas == 0) {
                            quizViewModel.mostrarDialogoSinVidas()
                        } else if (yaResolviHoy) {
                            // Mostrar diálogo informativo (ya se maneja en el composable)
                        } else {
                            val intent = Intent(this, ExplicacionTemaActivity::class.java)
                            intent.putExtra("curso_id", cursoId)
                            intent.putExtra("tema_id", tema.id)
                            intent.putExtra("tema_titulo", tema.titulo)
                            intent.putExtra("tema_explicacion", tema.explicacion)
                            startActivity(intent)
                        }
                    },
                    onQuizFinalClick = {
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.putExtra("curso_id", cursoId)
                        intent.putExtra("tema_id", "quiz_final")
                        intent.putExtra("tema_titulo", "Quiz Final - $cursoNombre")
                        intent.putExtra("modo", "final")
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        quizViewModel.iniciarObservadores(cursoId)
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemasDelCursoScreen(
    cursoNombre: String,
    cursoId: String,
    temas: List<Tema>,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onTemaClick: (Tema, Boolean) -> Unit,
    onQuizFinalClick: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    // Estado para verificar qué temas ya fueron resueltos hoy
    var temasResueltosHoy by remember { mutableStateOf<Map<String, TemaEstado>>(emptyMap()) }

    // Cargar estado de temas
    LaunchedEffect(cursoId) {
        temasResueltosHoy = verificarTemasResueltosHoy(cursoId, temas)
    }

    // Diálogo sin vidas
    if (uiState.mostrarDialogoSinVidas) {
        DialogoSinVidasMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            cursoNombre,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Selecciona un tema",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header con estadísticas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Indicador de vidas
                    IndicadorVidasMejorado(
                        vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                        vidasMax = uiState.vidas?.vidasMax ?: 5,
                        minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Barra de progreso
                    BarraProgresoMejorada(
                        temasCompletados = temasResueltosHoy.count { it.value.aprobado },
                        totalTemas = temas.size,
                        xpActual = uiState.progreso?.experiencia ?: 0,
                        rachaActual = uiState.progreso?.rachaDias ?: 0
                    )
                }
            }

            // Lista de temas
            if (temas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EduRachaColors.TextSecondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }
                        Text(
                            text = "No hay temas disponibles",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "El docente aun no ha creado temas para este curso",
                            fontSize = 15.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(temas.sortedBy { it.orden }) { index, tema ->
                        val estadoTema = temasResueltosHoy[tema.id]
                        val yaResolviHoy = estadoTema?.yaResolviHoy ?: false
                        val aprobado = estadoTema?.aprobado ?: false

                        TemaCardMejorada(
                            tema = tema,
                            numero = index + 1,
                            onClick = { onTemaClick(tema, yaResolviHoy) },
                            sinVidas = uiState.vidas?.vidasActuales == 0,
                            yaResolviHoy = yaResolviHoy,
                            aprobado = aprobado,
                            horasRestantes = estadoTema?.horasRestantes ?: 0,
                            minutosRestantes = estadoTema?.minutosRestantes ?: 0
                        )
                    }

                    // Card de quiz final
                    if (uiState.todosTemasAprobados) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            QuizFinalCard(
                                onClick = onQuizFinalClick,
                                sinVidas = uiState.vidas?.vidasActuales == 0
                            )
                        }
                    }
                }
            }
        }
    }
}

// Clase de estado del tema
data class TemaEstado(
    val aprobado: Boolean,
    val yaResolviHoy: Boolean,
    val horasRestantes: Int,
    val minutosRestantes: Int
)

// Función para verificar estado de temas
suspend fun verificarTemasResueltosHoy(cursoId: String, temas: List<Tema>): Map<String, TemaEstado> {
    val resultado = mutableMapOf<String, TemaEstado>()

    try {
        val userUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return resultado
        val database = FirebaseDatabase.getInstance()
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (tema in temas) {
            val ref = database.getReference("usuarios/$userUid/cursos/$cursoId/temasCompletados/${tema.id}")
            val snapshot = ref.get().await()

            if (snapshot.exists()) {
                val ultimaFecha = snapshot.child("ultimaFechaQuiz").getValue(String::class.java)
                val aprobado = snapshot.child("aprobado").getValue(Boolean::class.java) ?: false
                val yaResolviHoy = ultimaFecha == fechaHoy

                // Calcular tiempo restante
                var horasRestantes = 0
                var minutosRestantes = 0

                if (yaResolviHoy) {
                    val calendar = Calendar.getInstance()
                    val horaActual = calendar.timeInMillis

                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    val medianoche = calendar.timeInMillis

                    val diferenciaMilis = medianoche - horaActual
                    horasRestantes = (diferenciaMilis / (1000 * 60 * 60)).toInt()
                    minutosRestantes = ((diferenciaMilis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                }

                resultado[tema.id] = TemaEstado(
                    aprobado = aprobado,
                    yaResolviHoy = yaResolviHoy,
                    horasRestantes = horasRestantes,
                    minutosRestantes = minutosRestantes
                )
            }
        }
    } catch (e: Exception) {
        // Log error
    }

    return resultado
}

// Barra de progreso mejorada
@Composable
fun BarraProgresoMejorada(
    temasCompletados: Int,
    totalTemas: Int,
    xpActual: Int,
    rachaActual: Int
) {
    val progreso = if (totalTemas > 0) temasCompletados.toFloat() / totalTemas.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Título y contador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progreso del Curso",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "$temasCompletados/$totalTemas temas",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.Primary
            )
        }

        // Barra de progreso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EduRachaColors.Primary.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Primary,
                                EduRachaColors.PrimaryLight
                            )
                        )
                    )
            )
        }

        // Stats compactos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChipCompact(
                icon = Icons.Default.Star,
                valor = "$xpActual XP",
                color = EduRachaColors.Warning,
                modifier = Modifier.weight(1f)
            )
            StatChipCompact(
                icon = Icons.Default.Whatshot,
                valor = "$rachaActual dias",
                color = Color(0xFFFF6B35),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatChipCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// Card de tema mejorada con estado
@Composable
fun TemaCardMejorada(
    tema: Tema,
    numero: Int,
    onClick: () -> Unit,
    sinVidas: Boolean,
    yaResolviHoy: Boolean,
    aprobado: Boolean,
    horasRestantes: Int,
    minutosRestantes: Int
) {
    // Determinar si está deshabilitado
    val deshabilitado = sinVidas || yaResolviHoy

    // Estado para mostrar diálogo
    var mostrarDialogoYaResolvi by remember { mutableStateOf(false) }

    // Diálogo informativo
    if (mostrarDialogoYaResolvi) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoYaResolvi = false },
            icon = {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = EduRachaColors.Warning,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Quiz completado hoy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ya resolviste el quiz oficial de este tema hoy.")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Info.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Nuevo quiz disponible en:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Info
                            )
                            Text(
                                text = "$horasRestantes horas y $minutosRestantes minutos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.Info
                            )
                        }
                    }

                    if (aprobado) {
                        Text(
                            text = "Puedes usar el modo practica desde la pantalla de explicacion",
                            fontSize = 13.sp,
                            color = EduRachaColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoYaResolvi = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    )
                ) {
                    Text("Entendido")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !deshabilitado) {
                if (yaResolviHoy) {
                    mostrarDialogoYaResolvi = true
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (deshabilitado) 2.dp else 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (deshabilitado)
                Color.White.copy(alpha = 0.6f)
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (deshabilitado) Modifier
                    else Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                EduRachaColors.Primary.copy(alpha = 0.05f),
                                Color.White
                            )
                        ),
                        shape = RoundedCornerShape(0.dp)
                    )
                )
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número del tema
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (deshabilitado)
                    EduRachaColors.TextSecondary.copy(alpha = 0.2f)
                else
                    EduRachaColors.Primary.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$numero",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (deshabilitado)
                            EduRachaColors.TextSecondary
                        else
                            EduRachaColors.Primary
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            // Información del tema
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tema.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (deshabilitado)
                        EduRachaColors.TextSecondary
                    else
                        EduRachaColors.TextPrimary
                )
                if (tema.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tema.descripcion,
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary,
                        lineHeight = 20.sp
                    )
                }

                // Indicador de estado
                if (yaResolviHoy) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Warning.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Disponible en ${horasRestantes}h ${minutosRestantes}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Warning
                            )
                        }
                    }
                } else if (aprobado) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Success.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Aprobado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Success
                            )
                        }
                    }
                }
            }

            // Icono de estado
            if (sinVidas) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(28.dp)
                )
            } else if (yaResolviHoy) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = EduRachaColors.Warning,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = EduRachaColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// Card de quiz final
@Composable
fun QuizFinalCard(
    onClick: () -> Unit,
    sinVidas: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quiz_final")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (!sinVidas) scale else 1f)
            .clickable(enabled = !sinVidas, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sinVidas) 2.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sinVidas) Color.White.copy(alpha = 0.6f) else Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = if (sinVidas) listOf(Color.LightGray, Color.White) else listOf(
                            EduRachaColors.Warning,
                            EduRachaColors.WarningLight
                        )
                    )
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (sinVidas) Color.Gray.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (sinVidas) Color.Gray else Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Text(
                    text = "Quiz Final del Curso",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (sinVidas) Color.Gray else Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (sinVidas)
                        "Necesitas vidas disponibles"
                    else
                        "Has completado todos los temas. Demuestra todo lo aprendido",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sinVidas) Color.Gray else Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (!sinVidas) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Comenzar Examen Final",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Warning
                            )
                        }
                    }
                }
            }
        }
    }
}