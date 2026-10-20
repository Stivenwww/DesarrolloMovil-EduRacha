package com.stiven.sos

import com.stiven.sos.ui.theme.EduRachaColors
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
import androidx.compose.ui.graphics.graphicsLayer
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
    val PrimaryBlue: Color get() = EduRachaColors.PrimaryDark
    val DarkBlue: Color get() = EduRachaColors.PrimaryDark
    val AccentGold = Color(0xFFFFC531)
    val LightBlue: Color get() = EduRachaColors.PrimaryDark
    val BackgroundGray: Color get() = EduRachaColors.Background
    val TextGray: Color get() = EduRachaColors.TextSecondary
    val TextLightGray: Color get() = EduRachaColors.TextTertiary
    val White = Color.White
}

// Delegamos en el tema central (colores, tipografía y formas unificados)
@Composable
fun EduRachaTheme(content: @Composable () -> Unit) {
    com.stiven.sos.ui.theme.EduRachaTheme(content = content)
}

@Composable
fun WelcomeScreen(
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val aparicion by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "aparicion"
    )

    com.stiven.sos.ui.theme.FondoNexo {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("edu", fontSize = 26.sp, fontWeight = FontWeight.Black, color = p.texto)
                    Text("racha", fontSize = 26.sp, fontWeight = FontWeight.Black, color = p.marcaTexto)
                }
                Text(
                    "UNIAUTÓNOMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = p.textoSuave,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.superficie)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(300.dp).fillMaxWidth()) {
                com.stiven.sos.ui.theme.IslaFlotante(
                    modifier = Modifier.width(280.dp).height(120.dp),
                    tipo = com.stiven.sos.ui.theme.TipoIsla.ACTIVA
                )
                com.stiven.sos.ui.theme.RobiNeo(
                    tamano = 170.dp,
                    expresion = com.stiven.sos.ui.theme.ExpresionRobi.SALUDA,
                    modifier = Modifier.padding(bottom = 70.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = aparicion
                    translationY = (1f - aparicion) * 40f
                }
            ) {
                Text(
                    "Aprende con Robi",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = p.texto,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Conquista islas de conocimiento, mantén tu racha y sube en la liga de tu curso.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textoSuave,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = aparicion
                        translationY = (1f - aparicion) * 80f
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Soy estudiante",
                    onClick = onStudentClick,
                    icono = Icons.Default.School,
                    tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                    modifier = Modifier.fillMaxWidth()
                )
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Soy docente",
                    onClick = onTeacherClick,
                    icono = Icons.Default.MenuBook,
                    tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(20.dp))
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