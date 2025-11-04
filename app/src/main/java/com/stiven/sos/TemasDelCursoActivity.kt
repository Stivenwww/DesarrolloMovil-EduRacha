package com.stiven.sos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Tema
import com.stiven.sos.models.VidasResponse
import com.stiven.sos.repository.ProgresoCurso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel

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

        setContent {
            EduRachaTheme {
                // ✅ Iniciar observadores dentro del composable
                LaunchedEffect(cursoId) {
                    quizViewModel.iniciarObservadores(cursoId)
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
                    onTemaClick = { tema ->
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
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ Reiniciar observadores cuando regresamos
        quizViewModel.iniciarObservadores(cursoId)
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
    onTemaClick: (Tema) -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    // ✅ Diálogo cuando no hay vidas
    if (uiState.mostrarDialogoSinVidas) {
        AlertDialog(
            onDismissRequest = { quizViewModel.cerrarDialogoSinVidas() },
            icon = {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Sin vidas disponibles",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No tienes vidas para realizar el quiz.")
                    Text(
                        "Recuperarás 1 vida cada 30 minutos hasta llegar al máximo de 5 vidas.",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                    if (uiState.vidas != null && uiState.vidas!!.minutosParaProximaVida > 0) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduRachaColors.Info.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = EduRachaColors.Info,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Próxima vida en ${uiState.vidas!!.minutosParaProximaVida} minutos",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.Info,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { quizViewModel.cerrarDialogoSinVidas() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    )
                ) {
                    Text("Entendido")
                }
            }
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Selecciona un tema",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
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
        containerColor = EduRachaColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ✅ Barra de estadísticas (vidas, XP, racha)
            EstadisticasHeader(
                vidas = uiState.vidas,
                progreso = uiState.progreso
            )

            if (temas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No hay temas disponibles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "El docente aún no ha creado temas para este curso",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(temas.sortedBy { it.orden }) { index, tema ->
                        TemaCard(
                            tema = tema,
                            numero = index + 1,
                            onClick = { onTemaClick(tema) },
                            sinVidas = uiState.vidas?.vidasActuales == 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticasHeader(
    vidas: VidasResponse?,
    progreso: ProgresoCurso?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fila de vidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (vidas?.vidasActuales == 0)
                            EduRachaColors.TextSecondary
                        else
                            EduRachaColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${vidas?.vidasActuales ?: 0} / ${vidas?.vidasMax ?: 5}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (vidas?.vidasActuales == 0)
                            EduRachaColors.Error
                        else
                            EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Vidas",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                if (vidas != null && vidas.vidasActuales < vidas.vidasMax && vidas.minutosParaProximaVida > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EduRachaColors.Info.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = EduRachaColors.Info,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "+1 en ${vidas.minutosParaProximaVida} min",
                                fontSize = 12.sp,
                                color = EduRachaColors.Info,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Fila de XP y Racha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // XP
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EduRachaColors.Warning.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = EduRachaColors.Warning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "${progreso?.experiencia ?: 0} XP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                            Text(
                                text = "Experiencia",
                                fontSize = 11.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                }

                // Racha
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Success.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EduRachaColors.Success.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "${progreso?.rachaDias ?: 0} días",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                            Text(
                                text = "Racha",
                                fontSize = 11.sp,
                                color = EduRachaColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemaCard(
    tema: Tema,
    numero: Int,
    onClick: () -> Unit,
    sinVidas: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !sinVidas, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sinVidas)
                Color.White.copy(alpha = 0.5f)
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (sinVidas)
                    EduRachaColors.TextSecondary.copy(alpha = 0.3f)
                else
                    EduRachaColors.Primary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$numero",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sinVidas)
                            EduRachaColors.TextSecondary
                        else
                            EduRachaColors.Primary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tema.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sinVidas)
                        EduRachaColors.TextSecondary
                    else
                        EduRachaColors.TextPrimary
                )
                if (tema.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tema.descripcion,
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = EduRachaColors.TextSecondary
            )
        }
    }
}