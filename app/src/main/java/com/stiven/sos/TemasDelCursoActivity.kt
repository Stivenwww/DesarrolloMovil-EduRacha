package com.stiven.sos
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Tema
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

class TemasDelCursoActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var cursoNombre: String
    private var temas: List<Tema> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        cursoNombre = intent.getStringExtra("curso_nombre") ?: "Curso"

        temas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("curso_temas", Tema::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("curso_temas") ?: emptyList()
        }

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_curso_id", cursoId)
            commit()
        }

        setContent {
            EduRachaTheme {
                // Variable de estado para forzar recomposicion
                var refreshKey by remember { mutableStateOf(0) }

                // Iniciar observadores con el refreshKey
                LaunchedEffect(cursoId, refreshKey) {
                    quizViewModel.iniciarObservadores(cursoId)
                    quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        quizViewModel.detenerObservadores()
                    }
                }

                // Forzar refresh cuando regresa del quiz
                LaunchedEffect(Unit) {
                    // Esperar un poco y refrescar
                    delay(300)
                    refreshKey++
                }

                TemasDelCursoScreen(
                    cursoNombre = cursoNombre,
                    cursoId = cursoId,
                    temas = temas,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onTemaClick = { tema ->
                        prefs.edit().apply {
                            putString("last_tema_id", tema.id)
                            commit()
                        }

                        val vidas = quizViewModel.uiState.value.vidas?.vidasActuales ?: 0
                        if (vidas == 0) {
                            quizViewModel.mostrarDialogoSinVidas()
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
        // CRITICO: Reiniciar observadores y recargar datos al volver
        quizViewModel.iniciarObservadores(cursoId)
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }

    override fun onRestart() {
        super.onRestart()
        // CRITICO: Tambien refrescar al hacer restart
        quizViewModel.iniciarObservadores(cursoId)
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }
}
// Pantalla principal que muestra los temas del curso
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
    // Obtener el estado de la UI desde el ViewModel
    val uiState by quizViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Variables de estado para el Quiz Final
    var quizFinalAprobado by remember { mutableStateOf(false) }
    var quizFinalPorcentaje by remember { mutableStateOf(0) }
    var cargandoQuizFinal by remember { mutableStateOf(true) }

    // CARGAR ESTADO DEL QUIZ FINAL DESDE FIREBASE
    LaunchedEffect(cursoId) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", android.content.Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        Log.d("TemasDelCurso", "========================================")
        Log.d("TemasDelCurso", "VERIFICANDO ESTADO DEL QUIZ FINAL")
        Log.d("TemasDelCurso", "UserUid: $userUid")
        Log.d("TemasDelCurso", "CursoId: $cursoId")
        Log.d("TemasDelCurso", "========================================")

        if (userUid.isNotEmpty() && cursoId.isNotEmpty()) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()

                // Traer TODOS los quizzes y filtrar en cliente (sin índice)
                val quizzesRef = database.getReference("quizzes")

                Log.d("TemasDelCurso", "Consultando todos los quizzes...")

                val snapshot = quizzesRef.get().await()

                if (snapshot.exists()) {
                    Log.d("TemasDelCurso", "Total quizzes en DB: ${snapshot.childrenCount}")

                    var quizFinalEncontrado = false
                    var quizzesDelEstudiante = 0

                    // Iterar sobre TODOS los quizzes
                    for (quizSnapshot in snapshot.children) {
                        val quizId = quizSnapshot.key ?: continue
                        val estudianteId = quizSnapshot.child("estudianteId").getValue(String::class.java)

                        // Filtrar en cliente por estudianteId
                        if (estudianteId == userUid) {
                            quizzesDelEstudiante++

                            val modo = quizSnapshot.child("modo").getValue(String::class.java)
                            val quizCursoId = quizSnapshot.child("cursoId").getValue(String::class.java)
                            val estado = quizSnapshot.child("estado").getValue(String::class.java)

                            Log.d("TemasDelCurso", "Quiz del estudiante: $quizId")
                            Log.d("TemasDelCurso", "  - modo: $modo")
                            Log.d("TemasDelCurso", "  - cursoId: $quizCursoId")
                            Log.d("TemasDelCurso", "  - estado: $estado")

                            // Verificar si es el quiz final del curso actual
                            if (modo == "final" && quizCursoId == cursoId) {
                                quizFinalEncontrado = true

                                val correctas = quizSnapshot.child("preguntasCorrectas").getValue(Int::class.java) ?: 0
                                val incorrectas = quizSnapshot.child("preguntasIncorrectas").getValue(Int::class.java) ?: 0
                                val totalPreguntas = correctas + incorrectas

                                Log.d("TemasDelCurso", "========================================")
                                Log.d("TemasDelCurso", "✅ QUIZ FINAL ENCONTRADO!")
                                Log.d("TemasDelCurso", "Quiz ID: $quizId")
                                Log.d("TemasDelCurso", "Estado: $estado")
                                Log.d("TemasDelCurso", "Correctas: $correctas")
                                Log.d("TemasDelCurso", "Incorrectas: $incorrectas")
                                Log.d("TemasDelCurso", "Total: $totalPreguntas")

                                // Solo calcular si está finalizado y tiene respuestas
                                if (estado == "finalizado" && totalPreguntas > 0) {
                                    val porcentaje = (correctas * 100) / totalPreguntas
                                    quizFinalPorcentaje = porcentaje
                                    quizFinalAprobado = (porcentaje >= 80)

                                    Log.d("TemasDelCurso", "Porcentaje: $porcentaje%")
                                    Log.d("TemasDelCurso", "Aprobado (>=80%): $quizFinalAprobado")

                                    if (quizFinalAprobado) {
                                        Log.d("TemasDelCurso", "🔒 BLOQUEANDO ACCESO AL QUIZ FINAL")
                                    } else {
                                        Log.d("TemasDelCurso", "⚠️ Quiz completado pero no aprobado (<80%)")
                                    }
                                } else {
                                    Log.d("TemasDelCurso", "⚠️ Quiz iniciado pero no finalizado")
                                    quizFinalAprobado = false
                                    quizFinalPorcentaje = 0
                                }

                                Log.d("TemasDelCurso", "========================================")

                                // Salir del loop
                                break
                            }
                        }
                    }

                    Log.d("TemasDelCurso", "Quizzes del estudiante: $quizzesDelEstudiante")

                    if (!quizFinalEncontrado) {
                        Log.d("TemasDelCurso", "✅ No se encontró quiz final - DISPONIBLE")
                        quizFinalAprobado = false
                        quizFinalPorcentaje = 0
                    }
                } else {
                    Log.d("TemasDelCurso", "❌ No hay quizzes en la base de datos")
                    quizFinalAprobado = false
                    quizFinalPorcentaje = 0
                }
            } catch (e: Exception) {
                Log.e("TemasDelCurso", "========================================")
                Log.e("TemasDelCurso", "❌ ERROR al cargar Quiz Final")
                Log.e("TemasDelCurso", "Mensaje: ${e.message}")
                Log.e("TemasDelCurso", "========================================")
                e.printStackTrace()

                // En caso de error, BLOQUEAR por seguridad
                quizFinalAprobado = false
                quizFinalPorcentaje = 0
            } finally {
                cargandoQuizFinal = false
            }
        } else {
            Log.e("TemasDelCurso", "❌ UserUid o CursoId vacío")
            cargandoQuizFinal = false
        }
    }

    // Mostrar diálogo si no hay vidas
    if (uiState.mostrarDialogoSinVidas) {
        DialogoSinVidasMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo decorativo superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary,
                            EduRachaColors.Primary.copy(alpha = 0.95f),
                            EduRachaColors.PrimaryLight
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior con información del curso
            TopBarPremiumCompacto(
                cursoNombre = cursoNombre,
                temasCompletados = 0,
                totalTemas = temas.size,
                onNavigateBack = onNavigateBack
            )

            // Lista de contenido con temas y quiz final
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card con indicador de vidas
                item {
                    CardVidasIndividual(
                        vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                        vidasMax = uiState.vidas?.vidasMax ?: 5,
                        minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0
                    )
                }

                // Cards de estadísticas (XP y Racha)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardStatIndividual(
                            icon = Icons.Default.Star,
                            valor = "${uiState.progreso?.experiencia ?: 0}",
                            label = "XP Total",
                            color = Color(0xFFFFC107),
                            modifier = Modifier.weight(1f)
                        )
                        CardStatIndividual(
                            icon = Icons.Default.Whatshot,
                            valor = "${uiState.progreso?.rachaDias ?: 0}",
                            label = "Días",
                            color = Color(0xFFFF6B35),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Manejo de lista vacía de temas
                if (temas.isEmpty()) {
                    item { EmptyStateTemasModerno() }
                } else {
                    // TEMAS NORMALES DEL CURSO
                    itemsIndexed(temas.sortedBy { it.orden }) { index, tema ->
                        TemaCardInteractivaDuolingo(
                            tema = tema,
                            numero = index + 1,
                            totalTemas = temas.size,
                            onClick = { onTemaClick(tema) },
                            sinVidas = uiState.vidas?.vidasActuales == 0
                        )
                    }

                    // QUIZ FINAL - Solo mostrar si todos los temas están aprobados
                    if (uiState.todosTemasAprobados) {
                        item {
                            Spacer(Modifier.height(8.dp))

                            // Mostrar card del Quiz Final con validación de estado
                            QuizFinalCardEpico(
                                onClick = {
                                    // VALIDACIONES ANTES DE PERMITIR ACCESO
                                    when {
                                        // 1. Si ya está aprobado con 80% o más, bloquear
                                        quizFinalAprobado -> {
                                            Log.w("TemasDelCurso", "========================================")
                                            Log.w("TemasDelCurso", "ACCESO DENEGADO AL QUIZ FINAL")
                                            Log.w("TemasDelCurso", "Razón: Ya aprobado con $quizFinalPorcentaje%")
                                            Log.w("TemasDelCurso", "========================================")
                                            // Opcional: Mostrar un Toast o SnackBar informando al usuario
                                            android.widget.Toast.makeText(
                                                context,
                                                "Ya aprobaste el Quiz Final con $quizFinalPorcentaje%",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        // 2. Si no hay vidas, mostrar diálogo
                                        uiState.vidas?.vidasActuales == 0 -> {
                                            quizViewModel.mostrarDialogoSinVidas()
                                        }
                                        // 3. Si pasa todas las validaciones, permitir acceso
                                        else -> {
                                            Log.d("TemasDelCurso", "Acceso permitido al Quiz Final")
                                            onQuizFinalClick()
                                        }
                                    }
                                },
                                sinVidas = uiState.vidas?.vidasActuales == 0,
                                yaCompletado = quizFinalAprobado,
                                porcentajeObtenido = quizFinalPorcentaje
                            )
                        }
                    }
                }
            }
        }
    }
}
// Data class para el estado de cada tema
data class EstadoTema(
    val aprobado: Boolean = false,
    val porcentaje: Int = 0,
    val enCooldown: Boolean = false,
    val horasRestantes: Int = 0,
    val minutosRestantes: Int = 0
)


//  TOPBAR COMPACTO Y PROPORCIONADO
@Composable
fun TopBarPremiumCompacto(
    cursoNombre: String,
    temasCompletados: Int,
    totalTemas: Int,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Header superior con botón back y badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Back con glassmorphism
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Badges superiores
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassBadgeCompacto(
                    icon = Icons.Default.EmojiEvents,
                    text = "$temasCompletados/$totalTemas"
                )
                GlassBadgeCompacto(
                    icon = Icons.Default.School,
                    text = "Curso"
                )
            }
        }

        // Título del curso más compacto
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = cursoNombre,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 34.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Aprende jugando",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Progreso",
                            fontSize = 12.sp,
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
fun GlassBadgeCompacto(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.25f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

// 💳 CARD DE VIDAS INDIVIDUAL
@Composable
fun CardVidasIndividual(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int
) {
    val colorVida = Color(0xFFFF1744)
    val colorCritico = Color(0xFFD50000)
    val colorActual = if (vidasActuales <= 1) colorCritico else colorVida

    val scale = remember { Animatable(1f) }
    val heartbeat = rememberInfiniteTransition(label = "heartbeat")

    val pulse by heartbeat.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(vidasActuales) {
        scale.animateTo(1.12f, tween(150))
        scale.animateTo(1f, tween(150))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Corazón compacto
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorActual.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(52.dp)
                            .scale(if (vidasActuales > 0) pulse else 1f)
                    ) {}

                    Surface(
                        shape = CircleShape,
                        color = colorActual.copy(alpha = 0.25f),
                        modifier = Modifier.size(40.dp)
                    ) {}

                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = colorActual,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(scale.value)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ENERGÍA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$vidasActuales",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = colorActual
                        )
                        Text(
                            text = "/ $vidasMax",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Barra de vidas compacta
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(vidasMax) { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < vidasActuales) colorActual
                                        else Color.LightGray.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            // Timer compacto
            if (vidasActuales < vidasMax && minutosParaProxima > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1976D2).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$minutosParaProxima",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "min",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}

// 📊 CARD DE STAT INDIVIDUAL (XP o Racha)
@Composable
fun CardStatIndividual(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(valor) {
        scale.animateTo(1.05f, tween(100))
        scale.animateTo(1f, tween(100))
    }

    Card(
        modifier = modifier.scale(scale.value),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = valor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextSecondary
                )
            }
        }
    }
}


@Composable
fun TemaCardInteractivaDuolingo(
    tema: Tema,
    numero: Int,
    totalTemas: Int,
    onClick: () -> Unit,
    sinVidas: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 12f,
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = !sinVidas) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sinVidas) Color.White.copy(alpha = 0.6f) else Color.White
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (sinVidas) {
                                listOf(Color.LightGray, Color.Gray)
                            } else {
                                listOf(
                                    Color(0xFF1976D2),
                                    Color(0xFF2196F3),
                                    Color(0xFF42A5F5)
                                )
                            }
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                NumeroTema3D(
                    numero = numero,
                    sinVidas = sinVidas
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (sinVidas)
                            Color.LightGray.copy(alpha = 0.3f)
                        else
                            Color(0xFF64B5F6).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (sinVidas) Color.Gray else Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lección $numero de $totalTemas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sinVidas) Color.Gray else Color(0xFF1976D2)
                            )
                        }
                    }

                    Text(
                        text = tema.titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (sinVidas) EduRachaColors.TextSecondary else EduRachaColors.TextPrimary,
                        lineHeight = 26.sp
                    )

                    if (tema.descripcion.isNotBlank()) {
                        Text(
                            text = tema.descripcion,
                            fontSize = 15.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconoAccionInteractivo(sinVidas = sinVidas)
            }
        }
    }
}
@Composable
fun NumeroTema3D(numero: Int, sinVidas: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "numero_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .offset(y = if (!sinVidas) offsetY.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sombra
        Surface(
            shape = CircleShape,
            color = if (sinVidas)
                Color.LightGray.copy(alpha = 0.2f)
            else
                Color(0xFF1976D2).copy(alpha = 0.12f),
            modifier = Modifier.size(84.dp)
        ) {}

        // Círculo medio con gradiente
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (sinVidas) {
                            listOf(Color.LightGray, Color.Gray)
                        } else {
                            listOf(
                                Color(0xFF2196F3),
                                Color(0xFF1976D2)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$numero",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun IconoAccionInteractivo(sinVidas: Boolean) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!sinVidas) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Círculo de fondo giratorio (solo si no está sin vidas)
        if (!sinVidas) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1976D2).copy(alpha = 0.1f),
                modifier = Modifier
                    .size(56.dp)
                    .rotate(rotation.value)
            ) {}
        }

        Surface(
            shape = CircleShape,
            color = if (sinVidas)
                Color(0xFFFF1744).copy(alpha = 0.15f)
            else
                Color(0xFF1976D2).copy(alpha = 0.15f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (sinVidas) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (sinVidas) Color(0xFFFF1744) else Color(0xFF1976D2),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
// Composable para mostrar la tarjeta del Quiz Final
// Incluye parámetros para controlar si está bloqueado por falta de vidas,
// si ya fue completado, y el porcentaje obtenido anteriormente
@Composable
fun QuizFinalCardEpico(
    onClick: () -> Unit,
    sinVidas: Boolean,
    yaCompletado: Boolean = false,  // Indica si el quiz ya fue aprobado con 80% o más
    porcentajeObtenido: Int = 0      // Porcentaje obtenido en el último intento
) {
    // Transición infinita para animaciones continuas
    val infiniteTransition = rememberInfiniteTransition(label = "quiz_final")

    // Animación de escala que hace que la tarjeta "respire"
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animación de rotación sutil
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Tarjeta principal del Quiz Final
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (!sinVidas && !yaCompletado) scale else 1f)  // Solo anima si está disponible
            .clickable(
                enabled = !sinVidas && !yaCompletado,  // Deshabilitado si no hay vidas o ya completó
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (sinVidas || yaCompletado) 2.dp else 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (sinVidas || yaCompletado) {
                            // Colores desaturados cuando está bloqueado
                            listOf(Color.LightGray, Color.White)
                        } else {
                            // Gradiente vibrante cuando está disponible
                            listOf(
                                EduRachaColors.Secondary,
                                EduRachaColors.SecondaryLight,
                                EduRachaColors.Warning
                            )
                        }
                    )
                )
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Trofeo animado en la parte superior
                TrofeoEpicoAnimado(
                    rotation = if (!sinVidas && !yaCompletado) rotation else 0f,
                    sinVidas = sinVidas || yaCompletado
                )

                // Título del Quiz Final
                Text(
                    text = if (yaCompletado) "QUIZ COMPLETADO" else "QUIZ FINAL",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = if (sinVidas || yaCompletado) Color.Gray else Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                // Descripción del estado actual
                Text(
                    text = when {
                        yaCompletado -> "Felicidades! Ya aprobaste con $porcentajeObtenido%\nNo puedes repetir el quiz final"
                        sinVidas -> "Necesitas energía disponible"
                        else -> "Completaste todos los temas!\nDemuestra tu conocimiento"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sinVidas || yaCompletado) Color.Gray else Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // Botón y recompensas (solo si está disponible)
                if (!sinVidas && !yaCompletado) {
                    // Botón para comenzar el examen
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EduRachaColors.Secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Comenzar Examen Final",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EduRachaColors.Secondary
                            )
                        }
                    }

                    // Chips de recompensas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecompensaChip(
                            icon = Icons.Default.Star,
                            texto = "+500 XP",
                            color = EduRachaColors.Warning,
                            modifier = Modifier.weight(1f)
                        )
                        RecompensaChip(
                            icon = Icons.Default.EmojiEvents,
                            texto = "Certificado",
                            color = EduRachaColors.RankingGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (yaCompletado) {
                    // Mostrar badge de completado
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Aprobado con $porcentajeObtenido%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TrofeoEpicoAnimado(rotation: Float, sinVidas: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "trofeo_epico")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (sinVidas) Color.Gray.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .size(120.dp)
                .scale(if (!sinVidas) scale else 1f)
        ) {}

        Surface(
            shape = CircleShape,
            color = if (sinVidas) Color.Gray.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(90.dp)
        ) {}

        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = if (sinVidas) Color.Gray else Color.White,
            modifier = Modifier
                .size(56.dp)
                .rotate(if (!sinVidas) rotation else 0f)
        )
    }
}

@Composable
fun RecompensaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.9f)
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
                text = texto,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun EmptyStateTemasModerno() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "empty")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )

            Box(
                modifier = Modifier.offset(y = offsetY.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Primary.copy(alpha = 0.08f),
                    modifier = Modifier.size(140.dp)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(110.dp)
                ) {}
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = EduRachaColors.Primary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "Aún no hay temas",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "El docente está preparando contenido educativo de calidad para ti",
                fontSize = 16.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EduRachaColors.Info.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = EduRachaColors.Info,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Mantén tu racha activa mientras tanto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduRachaColors.Info
                    )
                }
            }
        }
    }
}