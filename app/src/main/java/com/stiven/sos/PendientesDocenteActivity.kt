package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.stiven.sos.models.PendientesCursoDocente
import com.stiven.sos.models.PendientesDocente
import com.stiven.sos.ui.theme.*

/**
 * Centro de pendientes del docente: cuántas preguntas, explicaciones, temas sin explicación
 * y aspirantes tiene por revisar, en total y por curso. Cada fila lleva a la pantalla donde se resuelve.
 */
class PendientesDocenteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EduRachaTheme { PendientesDocenteScreen(onVolver = { finish() }) } }
    }
}

/** Carga los pendientes y los vuelve a cargar cada vez que la pantalla regresa al frente. */
@Composable
fun rememberPendientesDocente(): Triple<PendientesDocente?, Boolean, String?> {
    var datos by remember { mutableStateOf<PendientesDocente?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    // Debe ser el de compose.ui: el de lifecycle.compose no está disponible con la versión de Compose del
    // proyecto y cerraba la app al entrar al panel docente ("CompositionLocal LocalLifecycleOwner not present").
    val ciclo = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) version++ }
        ciclo.addObserver(obs)
        onDispose { ciclo.removeObserver(obs) }
    }
    LaunchedEffect(version) {
        cargando = true
        try {
            val r = com.stiven.sos.api.ApiClient.apiService.obtenerPendientesDocente()
            if (r.isSuccessful) { datos = r.body(); error = null } else error = com.stiven.sos.api.mensajeDeError(r)
        } catch (e: Exception) {
            error = "No se pudo conectar con el servidor."
        }
        cargando = false
    }
    return Triple(datos, cargando, error)
}

@Composable
fun PendientesDocenteScreen(onVolver: () -> Unit) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val (datos, cargando, error) = rememberPendientesDocente()

    fun abrir(clase: Class<*>, c: PendientesCursoDocente? = null) {
        context.startActivity(Intent(context, clase).apply {
            c?.let { putExtra("CURSO_ID", it.cursoId); putExtra("CURSO_TITULO", it.titulo); putExtra("CURSO_CODIGO", it.codigo) }
        })
    }

    FondoNexo {
        Column(Modifier.fillMaxSize()) {
            EncabezadoNexo(
                titulo = "Pendientes",
                subtitulo = if (datos?.total == 0) "Estás al día" else "Lo que te falta por revisar",
                onVolver = onVolver,
                tono = TonoEncabezado.RACHA,
                acciones = { datos?.let { if (it.total > 0) EtiquetaEncabezadoNexo("${it.total}") } }
            )
            when {
                cargando && datos == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.racha) }
                datos == null -> EstadoVacioNexo(
                    titulo = "No se pudieron cargar", mensaje = error ?: "", expresion = ExpresionRobi.TRISTE
                )
                datos.total == 0 -> EstadoVacioNexo(
                    titulo = "¡Todo al día!",
                    mensaje = "No tienes preguntas, explicaciones ni aspirantes por revisar.",
                    expresion = ExpresionRobi.CELEBRA
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Column(Modifier.widthIn(max = ANCHO_CONTENIDO_NEXO).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TituloSeccionNexo("Por tipo", icono = Icons.Outlined.Category)
                            FilaPendiente(Icons.Outlined.FactCheck, p.ia, "Preguntas por validar", datos.preguntas,
                                "Apruébalas una por una o todas de una vez") { abrir(SeleccionarCursoValidacionActivity::class.java) }
                            FilaPendiente(Icons.Outlined.RateReview, p.racha, "Explicaciones por revisar", datos.explicaciones,
                                "Léelas y apruébalas para habilitar los quizzes") { abrir(SeleccionarCursoExplicacionesActivity::class.java) }
                            FilaPendiente(Icons.Outlined.NoteAdd, p.error, "Temas sin explicación", datos.temasSinExplicacion,
                                "Escríbelas o genéralas con IA") { abrir(SeleccionarCursoExplicacionesActivity::class.java) }
                            FilaPendiente(Icons.Outlined.PersonAdd, p.accion, "Aspirantes a tus cursos", datos.aspirantes,
                                "Acepta o rechaza solicitudes de inscripción") { abrir(GestionGruposActivity::class.java) }
                            Spacer(Modifier.height(4.dp))
                            TituloSeccionNexo("Por curso", icono = Icons.Outlined.MenuBook)
                        }
                    }
                    items(datos.cursos, key = { it.cursoId }) { c ->
                        TarjetaPendientesCurso(c, Modifier.widthIn(max = ANCHO_CONTENIDO_NEXO).fillMaxWidth(),
                            onPreguntas = { abrir(ValidacionPreguntasActivity::class.java, c) },
                            onExplicaciones = { abrir(GestionExplicacionesActivity::class.java, c) },
                            onAspirantes = { abrir(AsignarEstudiantesActivity::class.java, c) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPendiente(icono: ImageVector, color: Color, titulo: String, cantidad: Int, ayuda: String, onClick: () -> Unit) {
    val p = LocalNexo.current
    FilaNexo(
        icono = icono,
        color = if (cantidad > 0) color else p.textoTenue,
        titulo = titulo,
        subtitulo = if (cantidad > 0) ayuda else "Nada pendiente",
        onClick = if (cantidad > 0) onClick else null,
        extremo = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$cantidad", color = if (cantidad > 0) color else p.textoTenue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                if (cantidad > 0) Icon(Icons.Filled.ChevronRight, null, tint = p.textoSuave)
            }
        }
    )
}

@Composable
private fun TarjetaPendientesCurso(
    c: PendientesCursoDocente,
    modifier: Modifier,
    onPreguntas: () -> Unit,
    onExplicaciones: () -> Unit,
    onAspirantes: () -> Unit
) {
    val p = LocalNexo.current
    TarjetaNexo(modifier, acento = p.racha, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Outlined.School, p.marca)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.titulo, color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Código ${c.codigo}", color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Text("${c.total}", color = p.rachaTexto, fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.preguntas > 0) AccesoPendiente("${c.preguntas} preguntas por validar", p.ia, onPreguntas)
            if (c.explicaciones + c.temasSinExplicacion > 0) {
                AccesoPendiente(
                    listOfNotNull(
                        c.explicaciones.takeIf { it > 0 }?.let { "$it explicaciones por revisar" },
                        c.temasSinExplicacion.takeIf { it > 0 }?.let { "$it temas sin explicación" }
                    ).joinToString(" · "),
                    p.racha, onExplicaciones
                )
            }
            if (c.aspirantes > 0) AccesoPendiente("${c.aspirantes} aspirantes esperando", p.accion, onAspirantes)
        }
    }
}

@Composable
private fun AccesoPendiente(texto: String, color: Color, onClick: () -> Unit) {
    FichaNexo(texto, false, color, onClick, Modifier.fillMaxWidth())
}
