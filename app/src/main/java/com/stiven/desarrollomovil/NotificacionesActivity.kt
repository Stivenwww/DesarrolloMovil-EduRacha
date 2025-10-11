package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.EduRachaEmptyState
import com.stiven.desarrollomovil.ui.theme.components.EduRachaTopAppBar

// Data class y Enum (Sin cambios)
data class Notificacion(
    val id: Int,
    val tipo: TipoNotificacion,
    val titulo: String,
    val descripcion: String,
    val tiempo: String,
    val leida: Boolean = false
)

enum class TipoNotificacion {
    ESTUDIANTE, TAREA, PREGUNTA_VALIDADA, SISTEMA, LOGRO, RECORDATORIO
}

class NotificacionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Usando el tema centralizado de EduRacha
            EduRachaTheme {
                NotificacionesScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(onNavigateBack: () -> Unit) {
    var notificaciones by remember { mutableStateOf(obtenerNotificaciones()) }
    var mostrarSoloNoLeidas by remember { mutableStateOf(false) }

    val notificacionesFiltradas = if (mostrarSoloNoLeidas) {
        notificaciones.filter { !it.leida }
    } else {
        notificaciones
    }
    val notificacionesNoLeidasCount = notificaciones.count { !it.leida }

    Scaffold(
        topBar = {
            // Usando el TopAppBar del sistema de diseño
            EduRachaTopAppBar(
                title = "Notificaciones",
                onNavigationClick = onNavigateBack,
                actions = {
                    if (notificacionesNoLeidasCount > 0) {
                        TextButton(onClick = { notificaciones = notificaciones.map { it.copy(leida = true) } }) {
                            Text("Leer todo", color = Color.White)
                        }
                    }
                }
            )
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.large)
        ) {
            // Header Card
            item {
                HeaderInfoCard(notificacionesNoLeidasCount = notificacionesNoLeidasCount)
            }

            // Filtro de notificaciones
            item {
                FilterSection(
                    mostrarSoloNoLeidas = mostrarSoloNoLeidas,
                    onFilterChanged = { mostrarSoloNoLeidas = it }
                )
            }

            // Lista de notificaciones
            if (notificacionesFiltradas.isEmpty()) {
                item {
                    // Usando el EmptyState del sistema de diseño
                    EduRachaEmptyState(
                        modifier = Modifier.padding(top = 50.dp),
                        icon = Icons.Default.NotificationsOff,
                        title = "Todo en orden",
                        description = "No tienes notificaciones por ahora."
                    )
                }
            } else {
                items(notificacionesFiltradas, key = { it.id }) { notificacion ->
                    NotificationItemCard(
                        modifier = Modifier.padding(horizontal = Spacing.screenPadding),
                        notificacion = notificacion,
                        onClick = {
                            notificaciones = notificaciones.map {
                                if (it.id == notificacion.id) it.copy(leida = true) else it
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }
        }
    }
}

// --- COMPONENTES MEJORADOS Y REFACTORIZADOS ---

@Composable
private fun HeaderInfoCard(notificacionesNoLeidasCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.screenPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EduRachaColors.Surface, CustomShapes.Card)
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Centro de Notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = "Mantente al día con tus actividades",
                    style = MaterialTheme.typography.bodySmall,
                    color = EduRachaColors.TextSecondary
                )
            }
            if (notificacionesNoLeidasCount > 0) {
                Spacer(modifier = Modifier.width(Spacing.medium))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.AccentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificacionesNoLeidasCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Accent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(mostrarSoloNoLeidas: Boolean, onFilterChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenPadding, vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (mostrarSoloNoLeidas) "No Leídas" else "Todas las Notificaciones",
            style = MaterialTheme.typography.titleSmall,
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
private fun NotificationItemCard(
    modifier: Modifier = Modifier,
    notificacion: Notificacion,
    onClick: () -> Unit
) {
    val containerColor = if (notificacion.leida) EduRachaColors.SurfaceVariant else EduRachaColors.Surface
    val iconColor = obtenerColorIcono(notificacion.tipo)
    val iconBgColor = iconColor.copy(alpha = 0.1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notificacion.leida) 0.dp else Elevation.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = obtenerIcono(notificacion.tipo),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificacion.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = notificacion.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduRachaColors.TextSecondary
                )
                Text(
                    text = notificacion.tiempo,
                    style = MaterialTheme.typography.labelSmall,
                    color = EduRachaColors.TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = Spacing.extraSmall)
                )
            }
            if (!notificacion.leida) {
                Spacer(modifier = Modifier.width(Spacing.medium))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Primary)
                )
            }
        }
    }
}


// --- FUNCIONES AUXILIARES (ahora usando EduRachaColors) ---

private fun obtenerIcono(tipo: TipoNotificacion): ImageVector {
    return when (tipo) {
        TipoNotificacion.ESTUDIANTE -> Icons.Outlined.Person
        TipoNotificacion.TAREA -> Icons.Outlined.Assignment
        TipoNotificacion.PREGUNTA_VALIDADA -> Icons.Outlined.CheckCircle
        TipoNotificacion.SISTEMA -> Icons.Outlined.Info
        TipoNotificacion.LOGRO -> Icons.Outlined.EmojiEvents
        TipoNotificacion.RECORDATORIO -> Icons.Outlined.NotificationImportant
    }
}

private fun obtenerColorIcono(tipo: TipoNotificacion): Color {
    return when (tipo) {
        TipoNotificacion.ESTUDIANTE -> EduRachaColors.Primary
        TipoNotificacion.TAREA -> EduRachaColors.Warning
        TipoNotificacion.PREGUNTA_VALIDADA -> EduRachaColors.Success
        TipoNotificacion.SISTEMA -> EduRachaColors.Info
        TipoNotificacion.LOGRO -> EduRachaColors.Secondary
        TipoNotificacion.RECORDATORIO -> EduRachaColors.Error
    }
}

private fun obtenerNotificaciones(): List<Notificacion> {
    return listOf(
        Notificacion(1, TipoNotificacion.ESTUDIANTE, "Nuevo estudiante inscrito", "Ana García se ha inscrito en Matemáticas I.", "Hace 10 minutos"),
        Notificacion(2, TipoNotificacion.TAREA, "Tarea próxima a vencer", "La entrega del proyecto final vence mañana.", "Hace 1 hora"),
        Notificacion(3, TipoNotificacion.PREGUNTA_VALIDADA, "Preguntas validadas", "5 preguntas han sido aprobadas para el banco.", "Hace 3 horas"),
        Notificacion(4, TipoNotificacion.SISTEMA, "Actualización del sistema", "EduRacha se ha actualizado a la versión 2.1.", "Hace 2 días", true),
        Notificacion(5, TipoNotificacion.LOGRO, "¡Felicitaciones!", "Has alcanzado 100 estudiantes activos.", "Hace 3 días", true),
        Notificacion(6, TipoNotificacion.RECORDATORIO, "Recordatorio: Actualizar plan", "No olvides actualizar el plan de aula del semestre.", "Hace 5 días", true)
    )
}
