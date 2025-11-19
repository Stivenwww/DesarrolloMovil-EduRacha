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
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Calcular tamaños responsivos basados en el ancho de la pantalla
    val horizontalPadding = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)
    val logoSize = (screenWidth * 0.28f).coerceIn(90.dp, 180.dp)
    val glowMaxSize = logoSize * 1.1f
    val glowMidSize = logoSize * 0.89f
    val glowInnerSize = logoSize * 0.72f
    val logoCircleSize = logoSize * 0.61f

    // Tamaños de fuente responsivos
    val institutionalFontSize = (screenWidth.value * 0.035f).coerceIn(12f, 16f).sp
    val titleFontSize = (screenWidth.value * 0.10f).coerceIn(28f, 44f).sp
    val subtitleFontSize = (screenWidth.value * 0.038f).coerceIn(13f, 16f).sp
    val descriptionFontSize = (screenWidth.value * 0.035f).coerceIn(12f, 15f).sp
    val buttonFontSize = (screenWidth.value * 0.040f).coerceIn(14f, 17f).sp
    val footerFontSize = (screenWidth.value * 0.032f).coerceIn(11f, 14f).sp

    // Espaciados responsivos
    val topPadding = (screenHeight * 0.05f).coerceIn(40.dp, 80.dp)
    val logoTopSpacing = (screenHeight * 0.05f).coerceIn(20.dp, 50.dp)
    val afterLogoSpacing = (screenHeight * 0.04f).coerceIn(20.dp, 40.dp)
    val buttonHeight = (screenHeight * 0.075f).coerceIn(56.dp, 68.dp)

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
                .height((screenHeight * 0.08f).coerceIn(60.dp, 90.dp))
        ) {
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
                .padding(horizontal = horizontalPadding)
                .padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(logoTopSpacing))

            // Logo circular con brillo animado
            Box(
                modifier = Modifier.size(logoSize),
                contentAlignment = Alignment.Center
            ) {
                // Círculo de brillo externo animado
                Box(
                    modifier = Modifier
                        .size(glowMaxSize * glowScale)
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
                        .size(glowMidSize * glowScale)
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
                        .size(glowInnerSize)
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
                        .size(logoCircleSize)
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

            Spacer(modifier = Modifier.height(afterLogoSpacing))

            // Título institucional minimalista
            Text(
                text = "UNIAUTÓNOMA",
                fontSize = institutionalFontSize,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.PrimaryBlue,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Título principal
            Text(
                text = "EduRacha",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtítulo
            Text(
                text = "Sistema de Gestión Académica",
                fontSize = subtitleFontSize,
                fontWeight = FontWeight.Normal,
                color = AppColors.TextGray
            )

            Spacer(modifier = Modifier.height((screenHeight * 0.025f).coerceIn(12.dp, 24.dp)))

            // Descripción simple
            Text(
                text = "Desbloquea el conocimiento al convertir el estudio en un juego, elevando tus habilidades.",
                fontSize = descriptionFontSize,
                color = AppColors.TextLightGray,
                textAlign = TextAlign.Center,
                lineHeight = descriptionFontSize * 1.4f,
                modifier = Modifier.padding(horizontal = (screenWidth * 0.04f).coerceIn(8.dp, 20.dp))
            )

            Spacer(modifier = Modifier.height((screenHeight * 0.045f).coerceIn(24.dp, 56.dp)))

            // Botón Estudiante
            InstitutionalButton(
                text = "Portal Estudiante",
                icon = Icons.Default.School,
                backgroundColor = AppColors.PrimaryBlue,
                accentColor = AppColors.AccentGold,
                onClick = onStudentClick,
                buttonHeight = buttonHeight,
                fontSize = buttonFontSize,
                iconSize = (buttonFontSize.value * 1.5f).sp
            )

            Spacer(modifier = Modifier.height((screenHeight * 0.02f).coerceIn(12.dp, 20.dp)))

            // Botón Docente
            InstitutionalButton(
                text = "Panel Docente",
                icon = Icons.Default.MenuBook,
                backgroundColor = AppColors.AccentGold,
                textColor = AppColors.White,
                accentColor = AppColors.AccentGold,
                onClick = onTeacherClick,
                outlined = false,
                buttonHeight = buttonHeight,
                fontSize = buttonFontSize,
                iconSize = (buttonFontSize.value * 1.5f).sp
            )

            Spacer(modifier = Modifier.height((screenHeight * 0.035f).coerceIn(20.dp, 40.dp)))

            // Información adicional minimalista
            Text(
                text = "Construyamos tu conocimiento, en equipo.",
                fontSize = footerFontSize,
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
    outlined: Boolean = false,
    buttonHeight: androidx.compose.ui.unit.Dp = 64.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    iconSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(buttonHeight)

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
                    modifier = Modifier.size(iconSize.value.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = fontSize,
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
                    modifier = Modifier.size(iconSize.value.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = fontSize,
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