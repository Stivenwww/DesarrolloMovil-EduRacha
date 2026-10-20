package com.stiven.sos

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.SolicitudViewModel
import java.text.SimpleDateFormat
import java.util.*

class MisSolicitudesActivity : ComponentActivity() {
    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        solicitudViewModel.cargarSolicitudesEstudiante(userUid)

        setContent {
            EduRachaTheme {
                MisSolicitudesScreenV2(
                    solicitudViewModel = solicitudViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

fun formatearFecha(fecha: String): String {
    return try {
        val timestamp = fecha.toLongOrNull() ?: return fecha
        val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        fecha
    }
}

fun formatearHora(fecha: String): String {
    return try {
        val timestamp = fecha.toLongOrNull() ?: return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale("es", "CO"))
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun MisSolicitudesScreenV2(
    solicitudViewModel: SolicitudViewModel,
    onNavigateBack: () -> Unit
) {
    val p = LocalNexo.current
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()
    var filtroSeleccionado by remember { mutableStateOf<EstadoSolicitud?>(null) }
    var textoBusqueda by remember { mutableStateOf("") }

    val solicitudesFiltradas = remember(solicitudUiState.solicitudes, filtroSeleccionado, textoBusqueda) {
        var resultado = solicitudUiState.solicitudes
        if (filtroSeleccionado != null) {
            resultado = resultado.filter { it.estado == filtroSeleccionado }
        }
        if (textoBusqueda.isNotBlank()) {
            resultado = resultado.filter { solicitud ->
                solicitud.codigoCurso.contains(textoBusqueda, ignoreCase = true) ||
                        solicitud.nombreCurso.contains(textoBusqueda, ignoreCase = true) ||
                        solicitud.mensajeEstudiante?.contains(textoBusqueda, ignoreCase = true) == true
            }
        }
        resultado
    }

    FondoNexo(conCuadricula = false) {
        when {
            solicitudUiState.isLoading -> CargandoNexo(
                titulo = "Cargando solicitudes",
                mensaje = "Revisando el estado de tus solicitudes"
            )

            solicitudUiState.solicitudes.isEmpty() -> Column(Modifier.fillMaxSize()) {
                EncabezadoNexo(titulo = "Mis solicitudes", onVolver = onNavigateBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EstadoVacioNexo(
                        titulo = "Aún no has enviado solicitudes",
                        mensaje = "Explora los cursos disponibles y solicita unirte a uno."
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    EncabezadoNexo(
                        titulo = "Mis solicitudes",
                        subtitulo = "${solicitudUiState.solicitudes.size} en total",
                        onVolver = onNavigateBack
                    )
                }
                item {
                    BuscadorNexo(
                        valor = textoBusqueda,
                        onCambio = { textoBusqueda = it },
                        placeholder = "Buscar por curso o código",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple(EstadoSolicitud.PENDIENTE, "En revisión", Icons.Outlined.HourglassTop),
                            Triple(EstadoSolicitud.ACEPTADA, "Aceptadas", Icons.Outlined.CheckCircle),
                            Triple(EstadoSolicitud.RECHAZADA, "Rechazadas", Icons.Outlined.Cancel)
                        ).forEach { (estado, etiqueta, icono) ->
                            val activo = filtroSeleccionado == estado
                            val color = colorEstadoSolicitud(estado)
                            StatNexo(
                                icono = icono,
                                valor = "${solicitudUiState.solicitudes.count { it.estado == estado }}",
                                etiqueta = etiqueta,
                                color = color,
                                onClick = { filtroSeleccionado = if (activo) null else estado },
                                modifier = Modifier
                                    .weight(1f)
                                    .border(if (activo) 2.dp else 0.dp, if (activo) color else Color.Transparent, RoundedCornerShape(18.dp))
                            )
                        }
                    }
                }
                if (solicitudesFiltradas.isEmpty()) {
                    item {
                        EstadoVacioNexo(
                            titulo = "Sin resultados",
                            mensaje = if (textoBusqueda.isNotBlank())
                                "No hay solicitudes que coincidan con \"$textoBusqueda\"."
                            else "No hay solicitudes con este estado. Toca el filtro otra vez para ver todas."
                        )
                    }
                } else {
                    itemsIndexed(solicitudesFiltradas) { index, solicitud ->
                        EntradaEscalonada(index) {
                            SolicitudCardV2(solicitud)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun colorEstadoSolicitud(estado: EstadoSolicitud): Color {
    val p = LocalNexo.current
    return when (estado) {
        EstadoSolicitud.PENDIENTE -> p.racha
        EstadoSolicitud.ACEPTADA -> p.accion
        EstadoSolicitud.RECHAZADA -> p.error
    }
}

@Composable
fun SolicitudCardV2(solicitud: SolicitudCurso) {
    val p = LocalNexo.current
    val color = colorEstadoSolicitud(solicitud.estado)
    val (texto, icono) = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> "En revisión" to Icons.Outlined.HourglassTop
        EstadoSolicitud.ACEPTADA -> "Aceptada" to Icons.Outlined.CheckCircle
        EstadoSolicitud.RECHAZADA -> "Rechazada" to Icons.Outlined.Cancel
    }
    TarjetaNexo(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        acento = color
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Outlined.MenuBook, color, tamano = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    solicitud.codigoCurso.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = p.textoTenue,
                    letterSpacing = 1.sp
                )
                Text(
                    solicitud.nombreCurso.ifBlank { "Curso ${solicitud.codigoCurso}" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = p.texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ChipNexo(texto = texto, color = color, icono = icono)
        }
        Spacer(Modifier.height(12.dp))
        InfoRowV2(
            icon = Icons.Outlined.Event,
            label = "Enviada",
            value = formatearFecha(solicitud.fechaSolicitud),
            subValue = formatearHora(solicitud.fechaSolicitud),
            color = p.marca
        )
        solicitud.mensajeEstudiante?.takeIf { it.isNotBlank() }?.let { mensaje ->
            Spacer(Modifier.height(10.dp))
            MensajeBoxV2(
                icon = Icons.Outlined.Person,
                titulo = "Tu mensaje",
                mensaje = mensaje,
                color = p.marca
            )
        }
        if (solicitud.fechaRespuesta != null) {
            Spacer(Modifier.height(10.dp))
            InfoRowV2(
                icon = Icons.Outlined.Schedule,
                label = "Respondida",
                value = formatearFecha(solicitud.fechaRespuesta),
                subValue = formatearHora(solicitud.fechaRespuesta),
                color = color
            )
            solicitud.mensajeDocente?.takeIf { it.isNotBlank() }?.let { mensaje ->
                Spacer(Modifier.height(10.dp))
                MensajeBoxV2(
                    icon = Icons.Outlined.School,
                    titulo = "Respuesta del docente",
                    mensaje = mensaje,
                    color = color
                )
            }
        }
    }
}

@Composable
fun InfoRowV2(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color
) {
    val p = LocalNexo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.superficieAlt)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InsigniaNexo(icon, color, tamano = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = p.textoSuave)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.texto)
        }
        if (subValue.isNotBlank()) {
            Text(subValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.textoSuave)
        }
    }
}

@Composable
fun MensajeBoxV2(
    icon: ImageVector,
    titulo: String,
    mensaje: String,
    color: Color
) {
    val p = LocalNexo.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Black, color = p.texto)
        }
        Spacer(Modifier.height(6.dp))
        Text(mensaje, fontSize = 14.sp, color = p.texto, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
