// MainActivity.kt
package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val solicitudViewModel: SolicitudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Trae el tema/sonidos guardados en el perfil (si el usuario cambió de celular)
        com.stiven.sos.services.SincronizadorPreferencias.descargar(this)
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()

        setContent {
            EduRachaTheme {
                MainEstudianteScreen(
                    solicitudViewModel = solicitudViewModel,
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    onNavigateToNotifications = { navigateTo(NotificacionesEstudianteActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCursos = {
                        startActivity(Intent(this, CursosDisponiblesActivity::class.java))
                    },
                    onNavigateToSolicitudes = {
                        startActivity(Intent(this, MisSolicitudesActivity::class.java))
                    },
                    onNavigateToCursosInscritos = {
                        startActivity(Intent(this, CursosInscritosActivity::class.java))
                    },
                    onNavigateToRanking = {
                        startActivity(Intent(this, RankingEstudianteActivity::class.java))
                    }
                )
            }
        }
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_uid", null)

        userId?.let {
            solicitudViewModel.cargarSolicitudesEstudiante(it)
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}

@Composable
fun MainEstudianteScreen(
    solicitudViewModel: SolicitudViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCursos: () -> Unit,
    onNavigateToSolicitudes: () -> Unit,
    onNavigateToCursosInscritos: () -> Unit,
    onNavigateToRanking: () -> Unit
) {
    val context = LocalContext.current

    // Leer datos del usuario
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        fullName = prefs.getString("user_name", null) ?: "Estudiante"
        userEmail = prefs.getString("user_email", null) ?: ""
        userUid = prefs.getString("user_uid", null) ?: ""
    }

    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    // Datos reales (sin valores inventados)
    val solicitudesPendientes = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count {
            it.estado == com.stiven.sos.models.EstadoSolicitud.PENDIENTE
        }
    }

    val solicitudesAceptadas = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes.count {
            it.estado == com.stiven.sos.models.EstadoSolicitud.ACEPTADA
        }
    }


    val userInitial = fullName.firstOrNull()?.uppercase() ?: "E"
    val primerNombre = fullName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Estudiante"

    val p = com.stiven.sos.ui.theme.LocalNexo.current

    com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HeaderInicioEstudiante(
                primerNombre = primerNombre,
                userEmail = userEmail,
                userInitial = userInitial,
                hasNotificationBadge = com.stiven.sos.services.rememberNotificacionesNoLeidas() > 0 || solicitudesPendientes > 0,
                onNotificationClick = onNavigateToNotifications,
                onSettingsClick = onNavigateToSettings,
                onInfoClick = onNavigateToProfile
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Héroe: Robi en su isla + acción principal
                EntradaAnimada(retraso = 80) {
                    com.stiven.sos.ui.theme.TarjetaNexo(
                        modifier = Modifier.fillMaxWidth(),
                        acento = p.marca,
                        padding = 18.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "TU RUTA DE HOY",
                                    color = p.marcaTexto,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (solicitudesAceptadas > 0) "Sigue conquistando islas"
                                    else "Únete a tu primer curso",
                                    color = p.texto,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 25.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Un poquito cada día mantiene viva tu racha.",
                                    color = p.textoSuave,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(width = 110.dp, height = 120.dp)) {
                                com.stiven.sos.ui.theme.IslaFlotante(
                                    modifier = Modifier.size(width = 110.dp, height = 46.dp),
                                    tipo = com.stiven.sos.ui.theme.TipoIsla.ACTIVA
                                )
                                com.stiven.sos.ui.theme.RobiNeo(
                                    tamano = 84.dp,
                                    expresion = com.stiven.sos.ui.theme.ExpresionRobi.SALUDA,
                                    modifier = Modifier.padding(bottom = 22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        com.stiven.sos.ui.theme.BotonNexo(
                            texto = if (solicitudesAceptadas > 0) "Continuar aprendiendo" else "Explorar cursos",
                            onClick = if (solicitudesAceptadas > 0) onNavigateToCursosInscritos else onNavigateToCursos,
                            icono = Icons.Outlined.PlayArrow,
                            tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Estadísticas reales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaStatInicio(
                        icono = Icons.Outlined.MenuBook,
                        valor = solicitudesAceptadas,
                        etiqueta = "Cursos inscritos",
                        color = p.accion,
                        retraso = 150,
                        onClick = onNavigateToCursosInscritos,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaStatInicio(
                        icono = Icons.Outlined.HourglassTop,
                        valor = solicitudesPendientes,
                        etiqueta = "Pendientes",
                        color = p.racha,
                        retraso = 250,
                        onClick = onNavigateToSolicitudes,
                        modifier = Modifier.weight(1f)
                    )
                }

                com.stiven.sos.ui.theme.TituloSeccionNexo(
                    texto = "Explora",
                    icono = Icons.Outlined.Explore,
                    modifier = Modifier.padding(top = 4.dp)
                )

                EntradaAnimada(retraso = 220) {
                    TarjetaModuloJugosa(
                        titulo = "Cursos disponibles",
                        subtitulo = "Explora y únete a nuevos cursos",
                        icono = Icons.Outlined.Explore,
                        color = p.marca,
                        colorSombra = p.marcaSombra,
                        onClick = onNavigateToCursos
                    )
                }
                EntradaAnimada(retraso = 290) {
                    TarjetaModuloJugosa(
                        titulo = "Mis cursos",
                        subtitulo = "Tu mapa de islas por curso",
                        icono = Icons.Outlined.Map,
                        color = p.accion,
                        colorSombra = p.accionSombra,
                        onClick = onNavigateToCursosInscritos
                    )
                }
                EntradaAnimada(retraso = 360) {
                    TarjetaModuloJugosa(
                        titulo = "Mis solicitudes",
                        subtitulo = if (solicitudesPendientes > 0)
                            "Tienes $solicitudesPendientes por revisar"
                        else "Revisa el estado de tus solicitudes",
                        icono = Icons.Outlined.Assignment,
                        color = p.ia,
                        colorSombra = p.iaSombra,
                        onClick = onNavigateToSolicitudes
                    )
                }
                EntradaAnimada(retraso = 430) {
                    TarjetaModuloJugosa(
                        titulo = "Ligas",
                        subtitulo = "Compite sanamente con tu grupo",
                        icono = Icons.Outlined.Leaderboard,
                        color = p.racha,
                        colorSombra = p.rachaSombra,
                        onClick = onNavigateToRanking
                    )
                }

                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}

// ============================================================================
// COMPONENTES PRIVADOS DE LA PANTALLA DE INICIO
// ============================================================================

private fun saludoSegunHora(): String {
    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hora) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
private fun HeaderInicioEstudiante(
    primerNombre: String,
    userEmail: String,
    userInitial: String,
    hasNotificationBadge: Boolean,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    // Mismo encabezado que el resto de la app (tarjeta de color, antetítulo y título), sin botón volver.
    com.stiven.sos.ui.theme.EncabezadoNexo(
        titulo = primerNombre.ifBlank { "Estudiante" },
        subtitulo = "${saludoSegunHora()} · EduRacha",
        onVolver = null,
        tono = com.stiven.sos.ui.theme.TonoEncabezado.MARCA,
        acciones = {
            Box {
                com.stiven.sos.ui.theme.AccionEncabezadoNexo(Icons.Outlined.Notifications, "Notificaciones", onNotificationClick)
                if (hasNotificationBadge) {
                    val p = com.stiven.sos.ui.theme.LocalNexo.current
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(p.error)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
            com.stiven.sos.ui.theme.AccionEncabezadoNexo(Icons.Outlined.Settings, "Configuración", onSettingsClick)
            com.stiven.sos.ui.theme.AccionEncabezadoNexo(Icons.Outlined.Person, "Mi perfil", onInfoClick)
        }
    )
}

@Composable
private fun BotonIconoHeader(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    conBadge: Boolean = false
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icono, contentDescription = descripcion, tint = p.texto, modifier = Modifier.size(24.dp))
        }
        if (conBadge) {
            val t = rememberInfiniteTransition(label = "badge")
            val pulso by t.animateFloat(
                initialValue = 1f, targetValue = 1.35f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "pulso"
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(10.dp)
                    .graphicsLayer { scaleX = pulso; scaleY = pulso }
                    .clip(CircleShape)
                    .background(p.error)
                    .border(1.5.dp, p.fondo, CircleShape)
            )
        }
    }
}

@Composable
private fun EntradaAnimada(retraso: Int, contenido: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(retraso.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) { it / 3 }
    ) {
        contenido()
    }
}

@Composable
private fun TarjetaStatInicio(
    icono: ImageVector,
    valor: Int,
    etiqueta: String,
    color: Color,
    retraso: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    // Contador animado desde 0 hasta el valor real
    val animado = remember { Animatable(0f) }
    LaunchedEffect(valor) {
        delay(retraso.toLong())
        animado.animateTo(valor.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "esc")

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .clip(RoundedCornerShape(18.dp))
            .background(p.superficie)
            .border(1.dp, p.borde, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$etiqueta: $valor" }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.stiven.sos.ui.theme.InsigniaNexo(icono, color, tamano = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("${animado.value.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = p.texto)
            Text(etiqueta, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.textoSuave, maxLines = 1)
        }
    }
}

@Composable
private fun TarjetaModuloJugosa(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    color: Color,
    colorSombra: Color,
    onClick: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val profundidad = 4.dp
    val hundido by animateDpAsState(
        targetValue = if (presionado) profundidad else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "hundido"
    )
    val t = rememberInfiniteTransition(label = "icono")
    val balanceo by t.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "balanceo"
    )
    val forma = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = profundidad)
                .clip(forma)
                .background(p.borde)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = profundidad)
                .offset(y = hundido)
                .clip(forma)
                .background(p.superficie)
                .border(1.dp, p.borde, forma)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { rotationZ = balanceo }
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(colorSombra)
                )
                Icon(icono, contentDescription = null, tint = p.sobreAccion, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.Black, color = p.texto)
                Spacer(Modifier.height(2.dp))
                Text(subtitulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
