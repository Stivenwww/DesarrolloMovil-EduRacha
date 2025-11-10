package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.RankingEstudiante
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.RankingViewModel

class RankingDetalleActivity : ComponentActivity() {
    private val viewModel: RankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Ranking"

        if (cursoId.isEmpty()) {
            finish()
            return
        }

        viewModel.cargarRankingCurso(cursoId)

        setContent {
            EduRachaTheme {
                RankingDetalleScreen(
                    cursoTitulo = cursoTitulo,
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetalleScreen(
    cursoTitulo: String,
    viewModel: RankingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        cursoTitulo,
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
                    containerColor = EduRachaColors.Accent
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EduRachaColors.Accent
                    )
                }

                uiState.error != null -> {
                    ErrorViewRankingDetalle(
                        error = uiState.error!!,
                        onRetry = { /* El ranking ya fue cargado */ }
                    )
                }

                uiState.rankingEstudiantes.isEmpty() -> {
                    EmptyRankingDetalleView()
                }

                else -> {
                    RankingCursoDetalleView(
                        ranking = uiState.rankingEstudiantes,
                        usuarioActualId = viewModel.obtenerUsuarioActualId()
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCursoDetalleView(
    ranking: List<RankingEstudiante>,
    usuarioActualId: String?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "🏆 Top 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(ranking.take(3)) { index, estudiante ->
            TopRankingCardDetalle(
                posicion = index + 1,
                estudiante = estudiante,
                esUsuarioActual = estudiante.id == usuarioActualId
            )
        }

        if (ranking.size > 3) {
            item {
                Text(
                    text = "Otros participantes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(ranking.drop(3)) { index, estudiante ->
                RankingCardDetalle(
                    posicion = index + 4,
                    estudiante = estudiante,
                    esUsuarioActual = estudiante.id == usuarioActualId
                )
            }
        }
    }
}

@Composable
fun TopRankingCardDetalle(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean
) {
    val colorMedalla = when (posicion) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> EduRachaColors.TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (esUsuarioActual) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaColors.Primary.copy(alpha = 0.1f)
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(colorMedalla.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (posicion) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> posicion.toString()
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = estudiante.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${estudiante.experiencia} XP",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${estudiante.diasConsecutivos} días",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }

            if (esUsuarioActual) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Primary
                ) {
                    Text(
                        text = "Tú",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCardDetalle(
    posicion: Int,
    estudiante: RankingEstudiante,
    esUsuarioActual: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esUsuarioActual)
                EduRachaColors.Primary.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = posicion.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = estudiante.nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${estudiante.experiencia} XP • ${estudiante.diasConsecutivos} días",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            if (esUsuarioActual) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EduRachaColors.Primary
                ) {
                    Text(
                        text = "Tú",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}



@Composable
fun EmptyRankingDetalleView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Leaderboard,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.Accent.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Aún no hay datos de ranking",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Los estudiantes deben completar quizzes para aparecer en el ranking",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
fun ErrorViewRankingDetalle(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = EduRachaColors.Error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            text = error,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}