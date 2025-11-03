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
import androidx.compose.ui.text.style.TextAlign
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
        // Obtener explicación, si no existe usar contenido
        temaExplicacion = intent.getStringExtra("tema_explicacion")
            ?: intent.getStringExtra("tema_contenido")
                    ?: "No hay explicación disponible para este tema."

        // Cargar info del tema
        quizViewModel.obtenerTemaInfo(cursoId, temaId)

        setContent {
            EduRachaTheme {
                ExplicacionTemaScreen(
                    temaTitulo = temaTitulo,
                    temaExplicacion = temaExplicacion,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onIniciarQuiz = {
                        // Marcar como visto y navegar al quiz
                        quizViewModel.marcarExplicacionVista(temaId) {
                            val intent = Intent(this, QuizActivity::class.java)
                            intent.putExtra("curso_id", cursoId)
                            intent.putExtra("tema_id", temaId)
                            intent.putExtra("tema_titulo", temaTitulo)
                            startActivity(intent)
                            finish()
                        }
                    }
                )
            }
        }
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

                // Información del quiz
                uiState.temaInfo?.let { info ->
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
                                text = "Información del Quiz",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${info.preguntasDisponibles} preguntas disponibles",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = EduRachaColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${info.vidasActuales} vidas disponibles",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp)) // Espacio para el botón flotante
            }

            // Botón flotante para iniciar quiz
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Button(
                    onClick = onIniciarQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Iniciar Quiz",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mostrar error si existe
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Aquí podrías mostrar un Snackbar
                }
            }
        }
    }
}