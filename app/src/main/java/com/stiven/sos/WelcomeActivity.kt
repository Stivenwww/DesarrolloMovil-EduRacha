package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                WelcomeScreen(
                    onStudentClick = { navigateToLogin("student") },
                    onTeacherClick = { navigateToLogin("teacher") }
                )
            }
        }
    }

    private fun navigateToLogin(userType: String) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("user_type", userType)
        }
        startActivity(intent)
    }
}

// Colores institucionales de UNIAUTÓNOMA
object AppColors {
    val PrimaryBlue = Color(0xFF003D82)
    val DarkBlue = Color(0xFF002A5C)
    val AccentGold = Color(0xFFFFB300)
    val LightBlue = Color(0xFF1565C0)
    val BackgroundGray = Color(0xFFF5F7FA)
    val TextGray = Color(0xFF666666)
    val TextLightGray = Color(0xFF999999)
    val White = Color.White
}

@Composable
fun EduRachaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.PrimaryBlue,
            secondary = AppColors.AccentGold,
            background = AppColors.White,
            surface = AppColors.White
        ),
        content = content
    )
}

@Composable
fun WelcomeScreen(
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.White)
    ) {
        // Header minimalista con barra institucional
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            // Barra de color institucional sutil en la parte superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(AppColors.PrimaryBlue)
            )
        }

        // Contenedor principal sin elevación excesiva
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo circular con brillo animado más prominente y visible
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Círculo de brillo externo animado muy grande
                Box(
                    modifier = Modifier
                        .size(200.dp * glowScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.AccentGold.copy(alpha = glowAlpha),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.7f),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.4f),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent
                                ),
                                radius = 300f
                            ),
                            shape = CircleShape
                        )
                )
                // Círculo de brillo medio animado
                Box(
                    modifier = Modifier
                        .size(160.dp * glowScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.9f),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.6f),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                ),
                                radius = 200f
                            ),
                            shape = CircleShape
                        )
                )
                // Círculo de brillo interno
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.5f),
                                    AppColors.AccentGold.copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent
                                ),
                                radius = 150f
                            ),
                            shape = CircleShape
                        )
                )
                // Logo circular
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(AppColors.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Logo EduRacha",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Título institucional minimalista
            Text(
                text = "UNIAUTÓNOMA",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.PrimaryBlue,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Título principal
            Text(
                text = "EduRacha",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtítulo
            Text(
                text = "Sistema de Gestión Académica",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = AppColors.TextGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Descripción simple
            Text(
                text = "Desbloquea el conocimiento al convertir el estudio en un juego, elevando tus habilidades.",
                fontSize = 14.sp,
                color = AppColors.TextLightGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Botón Estudiante
            InstitutionalButton(
                text = "Portal Estudiante",
                icon = Icons.Default.School,
                backgroundColor = AppColors.PrimaryBlue,
                accentColor = AppColors.AccentGold,
                onClick = onStudentClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Docente
            InstitutionalButton(
                text = "Panel Docente",
                icon = Icons.Default.MenuBook,
                backgroundColor = AppColors.AccentGold,
                textColor = AppColors.White,
                accentColor = AppColors.AccentGold,
                onClick = onTeacherClick,
                outlined = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Información adicional minimalista
            Text(
                text = "Construyamos tu conocimiento, en equipo.",
                fontSize = 13.sp,
                color = AppColors.TextLightGray,
                textAlign = TextAlign.Center
            )
        }

        // Footer institucional sutil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(AppColors.PrimaryBlue)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AppColors.AccentGold)
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(AppColors.PrimaryBlue)
            )
        }
    }
}


@Composable
fun InstitutionalButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color = AppColors.White,
    accentColor: Color,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(64.dp)

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            ),
            border = BorderStroke(2.dp, accentColor)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    EduRachaTheme {
        WelcomeScreen(
            onStudentClick = {},
            onTeacherClick = {}
        )
    }
}