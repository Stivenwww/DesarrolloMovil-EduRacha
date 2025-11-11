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

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: ""
        temaExplicacion = intent.getStringExtra("tema_explicacion")
            ?: intent.getStringExtra("tema_contenido")
                    ?: "No hay explicación disponible para este tema."

        // Iniciar observadores
        quizViewModel.iniciarObservadores(cursoId)

        setContent {
            EduRachaTheme {
                ExplicacionTemaScreen(
                    temaTitulo = temaTitulo,
                    temaExplicacion = temaExplicacion,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onIniciarQuiz = { navegarAlQuiz() }
                )
            }
        }
    }

    /**
     * ✅ Función para navegar al quiz - marcar explicación Y navegar
     */
    private fun navegarAlQuiz() {
        quizViewModel.marcarExplicacionVista(temaId) {
            // ✅ Solo navegar SI se marcó correctamente
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("curso_id", cursoId)
            intent.putExtra("tema_id", temaId)
            intent.putExtra("tema_titulo", temaTitulo)
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
    onIniciarQuiz: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // ✅ Variable para prevenir múltiples clics
    var botonPresionado by remember { mutableStateOf(false) }

    // ✅ Reiniciar botón si hay un error
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            botonPresionado = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        temaTitulo,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
                    .padding(16.dp)
            ) {
                // Card de información
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EduRachaColors.Info.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = EduRachaColors.Info,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Lee atentamente la explicación antes de iniciar el quiz",
                            fontSize = 14.sp,
                            color = EduRachaColors.Info,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Contenido de la explicación
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
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = EduRachaColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Contenido del Tema",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Divider(color = EduRachaColors.TextSecondary.copy(alpha = 0.2f))

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = temaExplicacion,
                            fontSize = 16.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Card de estado actual
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Estado Actual",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )

                        // Experiencia
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${uiState.progreso?.experiencia ?: 0} XP",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }

                        // Racha
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${uiState.progreso?.rachaDias ?: 0} días de racha",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }

                        // Vidas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if ((uiState.vidas?.vidasActuales ?: 0) > 0)
                                    EduRachaColors.Error
                                else
                                    EduRachaColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${uiState.vidas?.vidasActuales ?: 5}/${uiState.vidas?.vidasMax ?: 5} vidas disponibles",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary
                                )
                                if (uiState.vidas != null && uiState.vidas!!.vidasActuales < uiState.vidas!!.vidasMax) {
                                    Text(
                                        text = "+1 vida en ${uiState.vidas!!.minutosParaProximaVida} min",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.Info
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }

            // ✅ Botón flotante CORREGIDO
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                val vidasDisponibles = uiState.vidas?.vidasActuales ?: 5

                Button(
                    onClick = {
                        // ✅ Prevenir múltiples clics
                        if (!botonPresionado && !uiState.isLoading) {
                            if (vidasDisponibles > 0) {
                                botonPresionado = true
                                onIniciarQuiz()
                            } else {
                                quizViewModel.mostrarDialogoSinVidas()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vidasDisponibles > 0)
                            EduRachaColors.Primary
                        else
                            EduRachaColors.TextSecondary
                    ),
                    enabled = !uiState.isLoading && !botonPresionado
                ) {
                    if (uiState.isLoading || botonPresionado) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            if (vidasDisponibles > 0) Icons.Default.PlayArrow else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (vidasDisponibles > 0)
                                "Iniciar Quiz"
                            else
                                "Sin Vidas Disponibles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ✅ Diálogo de error
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
                            tint = EduRachaColors.Error
                        )
                    },
                    title = {
                        Text(
                            text = "Error",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(text = uiState.error ?: "Error desconocido")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                quizViewModel.limpiarError()
                                botonPresionado = false
                            }
                        ) {
                            Text("Entendido")
                        }
                    }
                )
            }

            // Diálogo de sin vidas
            if (uiState.mostrarDialogoSinVidas) {
                AlertDialog(
                    onDismissRequest = { quizViewModel.cerrarDialogoSinVidas() },
                    icon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = EduRachaColors.Error
                        )
                    },
                    title = {
                        Text(
                            text = "Sin Vidas Disponibles",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "No tienes vidas disponibles para iniciar un quiz."
                            )
                            if (uiState.vidas != null) {
                                Text(
                                    text = "⏱️ Recuperarás 1 vida en ${uiState.vidas!!.minutosParaProximaVida} minutos",
                                    color = EduRachaColors.Info,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { quizViewModel.cerrarDialogoSinVidas() }
                        ) {
                            Text("Entendido")
                        }
                    }
                )
            }
        }
    }
}