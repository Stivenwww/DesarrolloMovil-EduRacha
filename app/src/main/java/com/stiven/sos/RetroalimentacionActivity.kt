package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.stiven.sos.models.RetroalimentacionFallosResponse
import com.stiven.sos.repository.QuizRepository
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.launch

class RetroalimentacionActivity : ComponentActivity() {

    private lateinit var quizId: String
    private lateinit var repository: QuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        quizId = intent.getStringExtra("quizId") ?: ""
        repository = QuizRepository(application)

        setContent {
            EduRachaTheme {
                RetroalimentacionScreen(
                    quizId = quizId,
                    repository = repository,
                    onCerrar = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroalimentacionScreen(
    quizId: String,
    repository: QuizRepository,
    onCerrar: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var retroalimentacion by remember { mutableStateOf<RetroalimentacionFallosResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // ✅ Cargar retroalimentación al iniciar
    LaunchedEffect(quizId) {
        scope.launch {
            repository.obtenerRetroalimentacion(quizId).fold(
                onSuccess = { data ->
                    retroalimentacion = data
                    isLoading = false
                },
                onFailure = { e ->
                    error = e.message
                    isLoading = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Retroalimentación",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(
                            Icons.Default.Close,
                            "Cerrar",
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
        when {
            isLoading -> {
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
                            text = "Analizando tus respuestas...",
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }

            error != null -> {
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
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = EduRachaColors.Error
                        )
                        Text(
                            text = "Error al cargar retroalimentación",
                            color = EduRachaColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onCerrar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EduRachaColors.Primary
                            )
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }

            retroalimentacion != null -> {
                if (retroalimentacion!!.totalFallos == 0) {
                    // ✅ Sin errores
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
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = EduRachaColors.Success
                            )
                            Text(
                                text = "¡Perfecto!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                            Text(
                                text = "No tuviste errores en este quiz",
                                fontSize = 16.sp,
                                color = EduRachaColors.TextSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onCerrar,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EduRachaColors.Primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continuar")
                            }
                        }
                    }
                } else {
                    // ❌ Hay errores - Mostrar retroalimentación interactiva
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = EduRachaColors.Warning.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = EduRachaColors.Warning,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Aprende de tus errores",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EduRachaColors.TextPrimary
                                        )
                                        Text(
                                            text = "${retroalimentacion!!.totalFallos} pregunta(s) para revisar",
                                            fontSize = 14.sp,
                                            color = EduRachaColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Preguntas falladas
                        itemsIndexed(retroalimentacion!!.preguntasFalladas) { index, pregunta ->
                            PreguntaFalladaCard(
                                numero = index + 1,
                                pregunta = pregunta
                            )
                        }

                        // Botón de cerrar
                        item {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onCerrar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EduRachaColors.Primary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Entendido",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
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
fun PreguntaFalladaCard(
    numero: Int,
    pregunta: com.stiven.sos.models.RetroalimentacionPregunta
) {
    // Estado para expandir/colapsar
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expandido = !expandido }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header de la pregunta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Error.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$numero",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Error
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pregunta $numero",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Error
                    )
                    Text(
                        text = if (expandido) "Toca para ocultar" else "Toca para ver detalles",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                Icon(
                    if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EduRachaColors.TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Texto de la pregunta
            Text(
                text = pregunta.texto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextPrimary,
                lineHeight = 24.sp
            )

            // Contenido expandible con animación
            AnimatedVisibility(
                visible = expandido,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = EduRachaColors.TextSecondary.copy(alpha = 0.2f))

                    // Tu respuesta (incorrecta)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Error.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = EduRachaColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Tu respuesta:",
                                    fontSize = 12.sp,
                                    color = EduRachaColors.Error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pregunta.respuestaUsuarioTexto,
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Respuesta correcta
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Success.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Respuesta correcta:",
                                    fontSize = 12.sp,
                                    color = EduRachaColors.Success,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pregunta.respuestaCorrectaTexto,
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Explicación
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Info.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = EduRachaColors.Info,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Explicación:",
                                    fontSize = 12.sp,
                                    color = EduRachaColors.Info,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = pregunta.explicacion,
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}