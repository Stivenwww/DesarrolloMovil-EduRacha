
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

// ============================================
// CURSOS INSCRITOS ACTIVITY
// ============================================

class CursosInscritosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                CursosInscritosScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosInscritosScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Cursos",
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
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = EduRachaColors.Primary.copy(alpha = 0.5f)
                )
                Text(
                    text = "No estás inscrito en ningún curso",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Una vez que un docente acepte tu solicitud, los cursos aparecerán aquí",
                    fontSize = 14.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Info.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = EduRachaColors.Info,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Aquí podrás ver tu progreso, rachas y puntos por cada curso",
                            fontSize = 12.sp,
                            color = EduRachaColors.Info,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
