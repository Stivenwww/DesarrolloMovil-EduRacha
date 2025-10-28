package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme

class RankingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                RankingScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ranking",
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
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = EduRachaColors.Accent.copy(alpha = 0.5f)
                )
                Text(
                    text = "¡Próximamente!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Completa quizzes para ganar puntos y competir con otros estudiantes",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                // Tarjeta informativa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RankingFeatureItem(
                            icon = Icons.Default.Star,
                            text = "Gana puntos completando quizzes"
                        )
                        RankingFeatureItem(
                            icon = Icons.Default.LocalFireDepartment,
                            text = "Mantén rachas diarias de estudio"
                        )
                        RankingFeatureItem(
                            icon = Icons.Default.Leaderboard,
                            text = "Compite en el ranking global"
                        )
                        RankingFeatureItem(
                            icon = Icons.Default.EmojiEvents,
                            text = "Desbloquea insignias y recompensas"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EduRachaColors.Accent,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = EduRachaColors.TextPrimary
        )
    }
}