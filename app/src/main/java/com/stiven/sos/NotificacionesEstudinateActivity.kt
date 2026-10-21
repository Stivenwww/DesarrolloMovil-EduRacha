package com.stiven.sos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.services.NotificacionModel
import com.stiven.sos.services.ServicioNotificaciones
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificacionesEstudianteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UID de la sesión activa: con el guardado en el celular se escuchaba la bandeja de otra cuenta
        val userId = com.stiven.sos.services.uidSesion(this)

        setContent {
            EduRachaTheme {
                NotificacionesEstudianteScreen(
                    userId = userId,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesEstudianteScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notificaciones by remember { mutableStateOf<List<NotificacionModel>>(emptyList()) }
    var mostrarSoloNoLeidas by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Escuchar notificaciones en tiempo real
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            ServicioNotificaciones.escucharNotificaciones(userId).collect { notifs ->
                notificaciones = notifs
                isLoading = false
            }
        }
    }

    val notificacionesFiltradas = if (mostrarSoloNoLeidas) {
        notificaciones.filter { !it.leido }
    } else {
        notificaciones
    }

    val notificacionesNoLeidasCount = notificaciones.count { !it.leido }

    Scaffold(
        topBar = {
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = "Notificaciones",
                subtitulo = if (notificacionesNoLeidasCount > 0) "$notificacionesNoLeidasCount sin leer" else "Estás al día",
                onVolver = onNavigateBack,
                acciones = {
                    if (notificacionesNoLeidasCount > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        ServicioNotificaciones.marcarTodasComoLeidas(userId)
                                        com.stiven.sos.ui.theme.Avisos.mostrar(context, "Todas las notificaciones marcadas como leídas")
                                    } catch (e: Exception) {
                                        com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error: ${e.message}")
                                    }
                                }
                            }
                        ) {
                            Text("Leer todo", color = EduRachaColors.AccentDark, fontWeight = FontWeight.Black)
                        }
                    }
                }
            )
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EduRachaColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Header Card
                item {
                    HeaderInfoCardEstudiante(notificacionesNoLeidasCount = notificacionesNoLeidasCount)
                }

                // Filtro
                item {
                    FilterSectionEstudiante(
                        mostrarSoloNoLeidas = mostrarSoloNoLeidas,
                        onFilterChanged = { mostrarSoloNoLeidas = it }
                    )
                }

                // Lista de notificaciones
                if (notificacionesFiltradas.isEmpty()) {
                    item {
                        EmptyNotificacionesEstudiante(mostrarSoloNoLeidas)
                    }
                } else {
                    items(notificacionesFiltradas, key = { it.id }) { notificacion ->
                        NotificacionEstudianteCard(
                            notificacion = notificacion,
                            onClick = {
                                scope.launch {
                                    try {
                                        ServicioNotificaciones.marcarComoLeida(userId, notificacion.id)
                                    } catch (e: Exception) {
                                        // Error silencioso
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        ServicioNotificaciones.eliminarNotificacion(userId, notificacion.id)
                                        com.stiven.sos.ui.theme.Avisos.mostrar(context, "Notificación eliminada")
                                    } catch (e: Exception) {
                                        com.stiven.sos.ui.theme.Avisos.mostrar(context, "Error al eliminar")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderInfoCardEstudiante(notificacionesNoLeidasCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EduRachaColors.Surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Centro de Notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = "Mantente al día con tus actividades",
                    style = MaterialTheme.typography.bodySmall,
                    color = EduRachaColors.TextSecondary
                )
            }
            if (notificacionesNoLeidasCount > 0) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificacionesNoLeidasCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSectionEstudiante(
    mostrarSoloNoLeidas: Boolean,
    onFilterChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (mostrarSoloNoLeidas) "No Leídas" else "Todas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        FilterChip(
            selected = mostrarSoloNoLeidas,
            onClick = { onFilterChanged(!mostrarSoloNoLeidas) },
            label = { Text("Solo no leídas") },
            leadingIcon = {
                Icon(
                    imageVector = if (mostrarSoloNoLeidas) Icons.Filled.FilterList else Icons.Outlined.FilterList,
                    contentDescription = "Filtrar"
                )
            }
        )
    }
}

@Composable
private fun NotificacionEstudianteCard(
    notificacion: NotificacionModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (notificacion.leido)
        EduRachaColors.SurfaceVariant
    else
        EduRachaColors.Surface

    val (icon, iconColor) = obtenerIconoYColorEstudiante(notificacion.tipo)
    val iconBgColor = iconColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notificacion.leido) 0.dp else 2.dp
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificacion.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = notificacion.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduRachaColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = formatearFecha(notificacion.fecha),
                    style = MaterialTheme.typography.labelSmall,
                    color = EduRachaColors.TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!notificacion.leido) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Primary)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Eliminar",
                        tint = EduRachaColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificacionesEstudiante(mostrandoFiltro: Boolean) {
    com.stiven.sos.ui.theme.EstadoVacioNexo(
        titulo = if (mostrandoFiltro) "No hay notificaciones sin leer" else "No tienes notificaciones",
        mensaje = if (mostrandoFiltro) "Estás al día con todas tus actividades." else "Aquí aparecerán tus avances, rachas y avisos de tus cursos.",
        expresion = com.stiven.sos.ui.theme.ExpresionRobi.FELIZ
    )
}

private fun obtenerIconoYColorEstudiante(tipo: String): Pair<ImageVector, Color> {
    return when (tipo) {
        "bienvenida" -> Icons.Outlined.Celebration to EduRachaColors.Success
        "quiz_completado" -> Icons.Outlined.CheckCircle to EduRachaColors.Success
        "inscripcion" -> Icons.Outlined.School to EduRachaColors.Primary
        "quiz_disponible" -> Icons.Outlined.Quiz to EduRachaColors.Accent
        "quiz_iniciado" -> Icons.Outlined.PlayCircle to EduRachaColors.Primary
        "racha" -> Icons.Outlined.Whatshot to EduRachaColors.Secondary
        "recordatorio" -> Icons.Outlined.NotificationImportant to EduRachaColors.Warning
        "logro" -> Icons.Outlined.EmojiEvents to EduRachaColors.Secondary
        else -> Icons.Outlined.Info to EduRachaColors.Primary
    }
}

private fun formatearFecha(timestamp: Long): String {
    val ahora = System.currentTimeMillis()
    val diferencia = ahora - timestamp

    return when {
        diferencia < 60_000 -> "Hace un momento"
        diferencia < 3600_000 -> "Hace ${diferencia / 60_000} minutos"
        diferencia < 86400_000 -> "Hace ${diferencia / 3600_000} horas"
        diferencia < 604800_000 -> "Hace ${diferencia / 86400_000} días"
        else -> {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            formato.format(Date(timestamp))
        }
    }
}