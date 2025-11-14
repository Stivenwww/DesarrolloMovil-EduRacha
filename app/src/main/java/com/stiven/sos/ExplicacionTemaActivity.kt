package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.services.IndicadorVidasMejorado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel

class ExplicacionTemaActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var temaExplicacion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos del intent
        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: ""
        temaExplicacion = intent.getStringExtra("tema_explicacion")
            ?: intent.getStringExtra("tema_contenido")
                    ?: "No hay explicacion disponible para este tema."

        // Iniciar observadores de progreso y vidas
        quizViewModel.iniciarObservadores(cursoId, temaId)

        setContent {
            EduRachaTheme {
                ExplicacionTemaScreen(
                    temaTitulo = temaTitulo,
                    temaExplicacion = temaExplicacion,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onIniciarQuiz = { modo -> navegarAlQuiz(modo) }
                )
            }
        }
    }

    // Navegar al quiz después de marcar la explicación como vista
    private fun navegarAlQuiz(modo: String) {
        quizViewModel.marcarExplicacionVista(temaId) {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("curso_id", cursoId)
            intent.putExtra("tema_id", temaId)
            intent.putExtra("tema_titulo", temaTitulo)
            intent.putExtra("modo", modo)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        quizViewModel.detenerObservadores()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplicacionTemaScreen(
    temaTitulo: String,
    temaExplicacion: String,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onIniciarQuiz: (String) -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var botonPresionado by remember { mutableStateOf(false) }
    var mostrarDialogoModo by remember { mutableStateOf(false) }
    var mostrarDialogoYaResolvi by remember { mutableStateOf(false) }

    // Resetear botón si hay error
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            botonPresionado = false
        }
    }

    // Dialogo cuando ya resolvió el quiz oficial hoy
    if (mostrarDialogoYaResolvi) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoYaResolvi = false },
            icon = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = EduRachaColors.Warning,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Quiz oficial completado",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Ya resolviste el quiz oficial de hoy.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Información del tiempo restante
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EduRachaColors.Info.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EduRachaColors.Info,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Nuevo quiz disponible en:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Info
                                )
                            }
                            Text(
                                text = "${uiState.horasParaNuevoQuiz} horas y ${uiState.minutosParaNuevoQuiz} minutos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = EduRachaColors.Info
                            )
                        }
                    }

                    // Opción de práctica si ya aprobó
                    if (uiState.temaAprobado) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EduRachaColors.Success.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Puedes usar el modo practica mientras esperas",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.temaAprobado) {
                    Button(
                        onClick = {
                            mostrarDialogoYaResolvi = false
                            botonPresionado = true
                            onIniciarQuiz("practica")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Info
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Ir a Modo Practica",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoYaResolvi = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Entendido",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Dialogo de selección de modo (solo si tema aprobado)
    if (mostrarDialogoModo && uiState.temaAprobado) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoModo = false },
            icon = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = EduRachaColors.Primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Elige el Modo",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Ya aprobaste este tema. Como deseas continuar?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Modo Práctica
                    Button(
                        onClick = {
                            mostrarDialogoModo = false
                            botonPresionado = true
                            onIniciarQuiz("practica")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Info
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Modo Practica",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "Gana XP, pierdes vidas",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Botón Modo Oficial
                    OutlinedButton(
                        onClick = {
                            mostrarDialogoModo = false
                            // Verificar si ya resolvió hoy
                            if (uiState.yaResolviHoy) {
                                mostrarDialogoYaResolvi = true
                            } else {
                                botonPresionado = true
                                onIniciarQuiz("oficial")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            EduRachaColors.Warning
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = EduRachaColors.Warning
                            )
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Modo Oficial",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = EduRachaColors.Warning
                                )
                                Text(
                                    "Mejora tu porcentaje",
                                    fontSize = 13.sp,
                                    color = EduRachaColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoModo = false }) {
                    Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        temaTitulo,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
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
        containerColor = EduRachaColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Indicador de vidas
                IndicadorVidasMejorado(
                    vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                    vidasMax = uiState.vidas?.vidasMax ?: 5,
                    minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Card de instrucciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EduRachaColors.Primary.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = EduRachaColors.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Lee atentamente la explicacion antes de iniciar el quiz",
                            fontSize = 17.sp,
                            color = EduRachaColors.Primary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Card de contenido
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = EduRachaColors.Primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Contenido del Tema",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Divider(
                            color = EduRachaColors.TextSecondary.copy(alpha = 0.2f),
                            thickness = 2.dp
                        )

                        Spacer(Modifier.height(28.dp))

                        Text(
                            text = temaExplicacion,
                            fontSize = 20.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 34.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Card de estadísticas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Tu Progreso Actual",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EduRachaColors.TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Card de Experiencia
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = EduRachaColors.Warning.copy(alpha = 0.12f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = EduRachaColors.Warning,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "${uiState.progreso?.experiencia ?: 0}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EduRachaColors.Warning
                                    )
                                    Text(
                                        text = "XP",
                                        fontSize = 14.sp,
                                        color = EduRachaColors.TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Card de Racha
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFF6B35).copy(alpha = 0.12f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B35),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "${uiState.progreso?.rachaDias ?: 0}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF6B35)
                                    )
                                    Text(
                                        text = "Dias",
                                        fontSize = 14.sp,
                                        color = EduRachaColors.TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Indicador de tema aprobado
                        if (uiState.temaAprobado) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EduRachaColors.Success.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EduRachaColors.Success,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Tema Aprobado",
                                            fontSize = 18.sp,
                                            color = EduRachaColors.Success,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Elige entre practicar o mejorar tu porcentaje",
                                            fontSize = 15.sp,
                                            color = EduRachaColors.TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(140.dp))
            }

            // Botón flotante para iniciar quiz
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp),
                shadowElevation = 14.dp,
                shape = RoundedCornerShape(22.dp)
            ) {
                val vidasDisponibles = uiState.vidas?.vidasActuales ?: 5

                Button(
                    onClick = {
                        if (!botonPresionado && !uiState.isLoading) {
                            // Sin vidas disponibles
                            if (vidasDisponibles == 0) {
                                quizViewModel.mostrarDialogoSinVidas()
                            }
                            // Ya resolvió hoy en modo oficial
                            else if (uiState.yaResolviHoy && !uiState.temaAprobado) {
                                mostrarDialogoYaResolvi = true
                            }
                            // Tema aprobado: mostrar opciones
                            else if (uiState.temaAprobado) {
                                mostrarDialogoModo = true
                            }
                            // Primera vez: solo modo oficial
                            else {
                                botonPresionado = true
                                onIniciarQuiz("oficial")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vidasDisponibles > 0)
                            EduRachaColors.Primary
                        else
                            EduRachaColors.TextSecondary
                    ),
                    enabled = !uiState.isLoading && !botonPresionado,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    if (uiState.isLoading || botonPresionado) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            if (vidasDisponibles > 0) Icons.Default.PlayArrow else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(18.dp))
                        Text(
                            text = when {
                                vidasDisponibles == 0 -> "Sin Vidas Disponibles"
                                uiState.temaAprobado -> "Elegir Modo de Quiz"
                                else -> "Iniciar Quiz Oficial"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Diálogo de error
            if (uiState.error != null) {
                AlertDialog(
                    onDismissRequest = {
                        quizViewModel.limpiarError()
                        botonPresionado = false
                    },
                    icon = {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = EduRachaColors.Error,
                            modifier = Modifier.size(52.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Error",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    },
                    text = {
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            fontSize = 17.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                quizViewModel.limpiarError()
                                botonPresionado = false
                            }
                        ) {
                            Text("Entendido", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Diálogo sin vidas
            if (uiState.mostrarDialogoSinVidas) {
                DialogoSinVidasMejorado(
                    minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
                    onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
                )
            }
        }
    }
}