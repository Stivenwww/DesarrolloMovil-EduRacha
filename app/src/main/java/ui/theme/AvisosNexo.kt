// ui/theme/AvisosNexo.kt
// Avisos de la app con el diseño Nexo: "se creó el usuario", "pregunta guardada", errores, etc.
// Reemplazan los Toast grises del sistema y el snackbar de Material en todas las pantallas.
package com.stiven.sos.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class TipoAviso { EXITO, ERROR, ADVERTENCIA, INFO }

/** Un aviso en pantalla. [titulo] es opcional: si falta se usa uno según el tipo. */
data class Aviso(
    val mensaje: String,
    val tipo: TipoAviso = TipoAviso.INFO,
    val titulo: String? = null,
    val duracionMs: Long = 3800,
    val id: Long = System.nanoTime()
)

/** Deduce el tipo de un mensaje de texto (para los avisos que antes eran Toast). */
fun tipoDeMensaje(mensaje: String): TipoAviso {
    val m = mensaje.lowercase()
    return when {
        listOf("error", "no se pudo", "no pudo", "falló", "fallo", "inválid", "no válid", "sin conexión", "denegad", "no tienes permiso")
            .any { it in m } -> TipoAviso.ERROR
        listOf("correctamente", "creado", "creada", "guardad", "eliminad", "actualizad", "aprobad", "enviad", "exitosa", "exitosamente", "generad", "listo", "descargad", "asignad", "aceptad")
            .any { it in m } -> TipoAviso.EXITO
        listOf("atención", "debes", "primero", "selecciona", "completa", "advertencia", "falta", "ya ")
            .any { it in m } -> TipoAviso.ADVERTENCIA
        else -> TipoAviso.INFO
    }
}

private fun tituloPorDefecto(tipo: TipoAviso) = when (tipo) {
    TipoAviso.EXITO -> "¡Listo!"
    TipoAviso.ERROR -> "Algo salió mal"
    TipoAviso.ADVERTENCIA -> "Atención"
    TipoAviso.INFO -> "Aviso"
}

@Composable
private fun estiloAviso(tipo: TipoAviso): Pair<Color, ImageVector> {
    val p = LocalNexo.current
    return when (tipo) {
        TipoAviso.EXITO -> (if (p.esOscuro) p.accion else p.accionTexto) to Icons.Filled.CheckCircle
        TipoAviso.ERROR -> (if (p.esOscuro) p.error else p.errorTexto) to Icons.Filled.ErrorOutline
        TipoAviso.ADVERTENCIA -> (if (p.esOscuro) p.racha else p.rachaTexto) to Icons.Filled.WarningAmber
        TipoAviso.INFO -> (if (p.esOscuro) p.marca else p.marcaTexto) to Icons.Filled.Info
    }
}

/**
 * Tarjeta de aviso Nexo: insignia con icono, título, mensaje, botón cerrar y una barra
 * que muestra el tiempo que le queda en pantalla.
 */
@Composable
fun TarjetaAvisoNexo(
    aviso: Aviso,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    etiquetaAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    val p = LocalNexo.current
    val (color, icono) = estiloAviso(aviso.tipo)
    var iniciado by remember(aviso.id) { mutableStateOf(false) }
    LaunchedEffect(aviso.id) { iniciado = true }
    val restante by animateFloatAsState(
        targetValue = if (iniciado) 0f else 1f,
        animationSpec = tween(aviso.duracionMs.toInt()),
        label = "restante"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = p.superficie,
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.55f)),
        shadowElevation = 10.dp,
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column {
            Row(
                Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsigniaNexo(icono, color, tamano = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        aviso.titulo ?: tituloPorDefecto(aviso.tipo),
                        color = color,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        aviso.mensaje,
                        color = p.texto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp
                    )
                }
                if (etiquetaAccion != null && onAccion != null) {
                    Text(
                        etiquetaAccion.uppercase(),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAccion)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCerrar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar aviso", tint = p.textoSuave, modifier = Modifier.size(20.dp))
                }
            }
            // Tiempo restante en pantalla
            Box(Modifier.fillMaxWidth().height(4.dp).background(color.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(restante.coerceIn(0f, 1f)).fillMaxHeight().background(color.copy(alpha = 0.7f)))
            }
        }
    }
}

/** Pila de avisos de una pantalla (estado observable). */
class PilaAvisos {
    val avisos = mutableStateListOf<Aviso>()
    fun mostrar(aviso: Aviso) {
        avisos.removeAll { it.mensaje == aviso.mensaje }   // sin duplicados seguidos
        avisos.add(aviso)
        while (avisos.size > 3) avisos.removeAt(0)
    }
    fun cerrar(id: Long) { avisos.removeAll { it.id == id } }
}

/** Dibuja los avisos arriba de la pantalla, bajo la barra de estado, con entrada y salida animadas. */
@Composable
fun CapaAvisosNexo(pila: PilaAvisos, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pila.avisos.forEach { aviso ->
            key(aviso.id) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(aviso.id) {
                    visible = true
                    delay(aviso.duracionMs)
                    visible = false
                    delay(300)
                    pila.cerrar(aviso.id)
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it } + fadeIn(),
                    exit = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250))
                ) {
                    TarjetaAvisoNexo(aviso, onCerrar = { pila.cerrar(aviso.id) })
                }
            }
        }
    }
}

/**
 * Avisos desde cualquier parte del código (reemplazo de Toast):
 *   Avisos.mostrar(context, "Pregunta guardada")
 * Se dibujan encima de la pantalla actual con el diseño Nexo. Si el contexto no es una
 * pantalla (por ejemplo, un servicio), se usa un Toast normal.
 */
object Avisos {
    private const val ETIQUETA = "eduracha_capa_avisos"

    fun mostrar(context: Context, mensaje: String, tipo: TipoAviso = tipoDeMensaje(mensaje), titulo: String? = null) {
        if (mensaje.isBlank()) return
        val actividad = context.buscarActividad()
        val contenedor = actividad?.findViewById<ViewGroup>(android.R.id.content)
        if (actividad == null || contenedor == null || actividad.isFinishing) {
            android.widget.Toast.makeText(context.applicationContext, mensaje, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val duracion = if (mensaje.length > 90 || tipo == TipoAviso.ERROR) 5200L else 3800L
        pilaDe(actividad, contenedor).mostrar(Aviso(mensaje, tipo, titulo, duracion))
    }

    private var pendiente: Aviso? = null
    private var origen: java.lang.ref.WeakReference<Activity>? = null
    private var escuchando = false

    /**
     * Para avisos justo antes de cerrar la pantalla (por ejemplo "Curso creado" + finish()):
     * el aviso aparece en la pantalla a la que se vuelve.
     */
    fun mostrarAlVolver(context: Context, mensaje: String, tipo: TipoAviso = tipoDeMensaje(mensaje)) {
        val actividad = context.buscarActividad()
        if (actividad == null) { mostrar(context, mensaje, tipo); return }
        pendiente = Aviso(mensaje, tipo, duracionMs = 4200)
        origen = java.lang.ref.WeakReference(actividad)
        if (!escuchando) {
            escuchando = true
            actividad.application.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(a: Activity) {
                    val aviso = pendiente ?: return
                    if (a === origen?.get() && !a.isFinishing) return
                    pendiente = null
                    a.findViewById<ViewGroup>(android.R.id.content)?.let { pilaDe(a, it).mostrar(aviso) }
                }
                override fun onActivityCreated(a: Activity, b: android.os.Bundle?) {}
                override fun onActivityStarted(a: Activity) {}
                override fun onActivityPaused(a: Activity) {}
                override fun onActivityStopped(a: Activity) {}
                override fun onActivitySaveInstanceState(a: Activity, b: android.os.Bundle) {}
                override fun onActivityDestroyed(a: Activity) {}
            })
        }
    }

    private fun pilaDe(actividad: Activity, contenedor: ViewGroup): PilaAvisos {
        (contenedor.findViewWithTag<ComposeView>(ETIQUETA)?.getTag(ETIQUETA.hashCode()) as? PilaAvisos)?.let { return it }
        val pila = PilaAvisos()
        val vista = ComposeView(actividad).apply {
            tag = ETIQUETA
            setTag(ETIQUETA.hashCode(), pila)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                EduRachaTheme {
                    // Solo ocupa lo que miden los avisos: no bloquea los toques del resto de la pantalla
                    CapaAvisosNexo(pila)
                }
            }
        }
        contenedor.addView(
            vista,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.TOP
            }
        )
        vista.elevation = 1000f
        return pila
    }

    private tailrec fun Context.buscarActividad(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.buscarActividad()
        else -> null
    }
}

/** Visuales de snackbar con tipo, para las pantallas que usan SnackbarHostState. */
class AvisoSnackbar(
    override val message: String,
    val tipo: TipoAviso = tipoDeMensaje(message),
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

/** Atajo: hostState.avisar("Curso creado", TipoAviso.EXITO) */
suspend fun SnackbarHostState.avisar(mensaje: String, tipo: TipoAviso = tipoDeMensaje(mensaje)) {
    showSnackbar(AvisoSnackbar(mensaje, tipo))
}

/**
 * Avisos inferiores con el estilo Nexo (reemplaza el snackbar gris de Material).
 * Usa la misma tarjeta que los avisos superiores.
 */
@Composable
fun AvisosNexoHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)
    ) { datos ->
        val tipo = (datos.visuals as? AvisoSnackbar)?.tipo ?: tipoDeMensaje(datos.visuals.message)
        val duracion = when (datos.visuals.duration) {
            SnackbarDuration.Short -> 4000L
            SnackbarDuration.Long -> 10000L
            SnackbarDuration.Indefinite -> 60000L
        }
        val aviso = remember(datos) { Aviso(datos.visuals.message, tipo, duracionMs = duracion) }
        TarjetaAvisoNexo(
            aviso = aviso,
            onCerrar = { datos.dismiss() },
            etiquetaAccion = datos.visuals.actionLabel,
            onAccion = { datos.performAction() }
        )
    }
}
