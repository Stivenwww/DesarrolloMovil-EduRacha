package com.stiven.sos.services


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.viewmodel.TipoMensaje
import kotlinx.coroutines.delay

@Composable
fun MensajeFlotante(
    mensaje: String,
    tipo: TipoMensaje,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-ocultar después de 3 segundos
    LaunchedEffect(visible) {
        if (visible) {
            delay(3000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = obtenerColorFondo(tipo),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, obtenerColorIcono(tipo).copy(alpha = 0.6f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = obtenerIcono(tipo),
                    contentDescription = null,
                    tint = obtenerColorIcono(tipo),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = mensaje,
                    fontSize = 14.sp,
                    color = obtenerColorTexto(tipo),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = obtenerColorIcono(tipo),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun obtenerColorFondo(tipo: TipoMensaje): Color = com.stiven.sos.ui.theme.LocalNexo.current.superficie

@Composable
private fun obtenerColorTexto(tipo: TipoMensaje): Color = com.stiven.sos.ui.theme.LocalNexo.current.texto

@Composable
private fun obtenerColorIcono(tipo: TipoMensaje): Color {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    return when (tipo) {
        TipoMensaje.EXITO -> p.accionTexto
        TipoMensaje.ERROR -> p.errorTexto
        TipoMensaje.INFO -> p.marcaTexto
        TipoMensaje.ADVERTENCIA -> p.rachaTexto
    }
}

@Composable
private fun obtenerIcono(tipo: TipoMensaje): androidx.compose.ui.graphics.vector.ImageVector {
    return when (tipo) {
        TipoMensaje.EXITO -> Icons.Default.CheckCircle
        TipoMensaje.ERROR -> Icons.Default.Error
        TipoMensaje.INFO -> Icons.Default.Info
        TipoMensaje.ADVERTENCIA -> Icons.Default.Warning
    }
}

/**
 * Muestra arriba de la pantalla (bajo la barra de estado) los mensajes del QuizViewModel.
 * Antes esos mensajes ("No tienes vidas disponibles", errores al iniciar) no se mostraban en ninguna pantalla.
 */
@Composable
fun ConMensajesQuiz(
    quizViewModel: com.stiven.sos.viewmodel.QuizViewModel,
    contenido: @Composable () -> Unit
) {
    val estado by quizViewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize()) {
        contenido()
        MensajeFlotante(
            mensaje = estado.mensajeFlotante ?: "",
            tipo = estado.tipoMensaje,
            visible = estado.mostrarMensajeFlotante && !estado.mensajeFlotante.isNullOrBlank(),
            onDismiss = { quizViewModel.ocultarMensaje() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .widthIn(max = 560.dp)
        )
    }
}
