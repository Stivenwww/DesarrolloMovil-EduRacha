package com.stiven.sos

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.utils.SessionManager
import com.stiven.sos.viewmodel.SolicitudDocenteViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AsignarEstudiantesActivity : ComponentActivity() {

    private val grupoViewModel: GrupoCursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""

        //  Obtener docenteId de SessionManager en lugar del Intent
        // UID de la sesión activa (el guardado podía ser de otra cuenta)
        val docenteId = com.stiven.sos.services.uidSesion(this)

        Log.d("AsignarEstudiantes", """
             Activity iniciada:
            - Curso: $cursoTitulo
            - CursoID: $cursoId
            - DocenteID: $docenteId
        """.trimIndent())

        if (docenteId.isBlank()) {
            com.stiven.sos.ui.theme.Avisos.mostrarAlVolver(this, "No hay sesión activa. Vuelve a iniciar sesión.", com.stiven.sos.ui.theme.TipoAviso.ERROR)
            finish()
            return
        }

        grupoViewModel.iniciar(docenteId, cursoId)

        setContent {
            EduRachaTheme {
                GrupoCursoScreen(
                    cursoTitulo = cursoTitulo,
                    vm = grupoViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// ============================================================================
// Grupo del curso (docente): aspirantes e inscritos en la misma pantalla, sin ventanas emergentes.
// Aceptar es inmediato; rechazar abre un panel dentro de la misma tarjeta para elegir el motivo.
// ============================================================================

data class GrupoCursoUi(
    val cargando: Boolean = true,
    val aspirantes: List<SolicitudCurso> = emptyList(),
    val inscritos: List<com.stiven.sos.models.UsuarioAsignado> = emptyList(),
    val procesando: Set<String> = emptySet(),
    val error: String? = null,
    val mensaje: String? = null
)

class GrupoCursoViewModel : androidx.lifecycle.ViewModel() {
    private val api = com.stiven.sos.api.ApiClient.apiService
    private val _ui = kotlinx.coroutines.flow.MutableStateFlow(GrupoCursoUi())
    val ui: kotlinx.coroutines.flow.StateFlow<GrupoCursoUi> = _ui
    private var docenteId = ""
    private var cursoId = ""

    fun iniciar(docente: String, curso: String) {
        if (docente == docenteId && curso == cursoId && !_ui.value.cargando) return
        docenteId = docente; cursoId = curso
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(cargando = true, error = null)
            try {
                val r1 = api.obtenerSolicitudesDocente(docenteId)
                val aspirantes = if (r1.isSuccessful) r1.body().orEmpty()
                    .filter { it.cursoId == cursoId && it.estado == EstadoSolicitud.PENDIENTE }
                    .sortedBy { it.fechaSolicitud.toLongOrNull() ?: 0L } else emptyList()
                val r2 = api.obtenerEstudiantesPorCurso(cursoId)
                val inscritos = if (r2.isSuccessful) r2.body().orEmpty().sortedBy { it.nombre.lowercase() } else emptyList()
                _ui.value = _ui.value.copy(
                    cargando = false, aspirantes = aspirantes, inscritos = inscritos,
                    error = if (!r1.isSuccessful) com.stiven.sos.api.mensajeDeError(r1) else null
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(cargando = false, error = "No se pudo conectar con el servidor.")
            }
        }
    }

    fun responder(s: SolicitudCurso, aceptar: Boolean, mensaje: String?) {
        val id = s.id ?: return
        if (id in _ui.value.procesando) return
        run {
            viewModelScope.launch {
                _ui.value = _ui.value.copy(procesando = _ui.value.procesando + id)
                try {
                    val r = api.responderSolicitud(id, com.stiven.sos.models.RespuestaSolicitudRequest(aceptar, mensaje?.ifBlank { null }))
                    if (r.isSuccessful) {
                        _ui.value = _ui.value.copy(
                            aspirantes = _ui.value.aspirantes.filter { it.id != id },
                            mensaje = if (aceptar) "${s.estudianteNombre} ya es parte del curso." else "Solicitud de ${s.estudianteNombre} rechazada."
                        )
                        if (aceptar) recargarInscritos()
                    } else {
                        _ui.value = _ui.value.copy(error = com.stiven.sos.api.mensajeDeError(r))
                    }
                } catch (e: Exception) {
                    _ui.value = _ui.value.copy(error = "No se pudo conectar con el servidor.")
                } finally {
                    _ui.value = _ui.value.copy(procesando = _ui.value.procesando - id)
                }
            }
        }
    }

    fun aceptarTodas() {
        _ui.value.aspirantes.forEach { responder(it, true, null) }
    }

    private suspend fun recargarInscritos() {
        runCatching { api.obtenerEstudiantesPorCurso(cursoId) }.getOrNull()?.takeIf { it.isSuccessful }?.body()?.let {
            _ui.value = _ui.value.copy(inscritos = it.sortedBy { e -> e.nombre.lowercase() })
        }
    }

    fun limpiarMensajes() { _ui.value = _ui.value.copy(mensaje = null, error = null) }
}

private val MOTIVOS_RECHAZO = listOf(
    "El curso ya no tiene cupos",
    "No perteneces a este grupo",
    "Usa el código de tu grupo",
    "Escríbeme para revisarlo"
)

private fun fechaLegible(valor: String): String {
    val ms = valor.toLongOrNull() ?: return valor
    return java.text.SimpleDateFormat("d MMM yyyy · HH:mm", java.util.Locale("es", "CO")).format(java.util.Date(ms))
}

@Composable
fun GrupoCursoScreen(
    cursoTitulo: String,
    vm: GrupoCursoViewModel,
    onNavigateBack: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val ui by vm.ui.collectAsState()
    var pestana by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    var busqueda by remember { mutableStateOf("") }
    var confirmarTodas by remember { mutableStateOf(false) }

    LaunchedEffect(ui.mensaje, ui.error) {
        ui.mensaje?.let { com.stiven.sos.ui.theme.Avisos.mostrar(context, it, com.stiven.sos.ui.theme.TipoAviso.EXITO) }
        ui.error?.let { com.stiven.sos.ui.theme.Avisos.mostrar(context, it, com.stiven.sos.ui.theme.TipoAviso.ERROR) }
        if (ui.mensaje != null || ui.error != null) vm.limpiarMensajes()
    }

    com.stiven.sos.ui.theme.FondoNexo {
        Column(Modifier.fillMaxSize()) {
            com.stiven.sos.ui.theme.EncabezadoNexo(
                titulo = cursoTitulo,
                subtitulo = "Grupo del curso",
                onVolver = onNavigateBack,
                tono = com.stiven.sos.ui.theme.TonoEncabezado.IA,
                acciones = {
                    com.stiven.sos.ui.theme.AccionEncabezadoNexo(Icons.Default.Refresh, "Actualizar", { vm.cargar() }, com.stiven.sos.ui.theme.TonoEncabezado.IA)
                }
            )
            com.stiven.sos.ui.theme.ContenidoCentradoNexo(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.stiven.sos.ui.theme.StatNexo(Icons.Default.PersonAdd, "${ui.aspirantes.size}", "Aspirantes", p.racha, Modifier.weight(1f))
                        com.stiven.sos.ui.theme.StatNexo(Icons.Default.Groups, "${ui.inscritos.size}", "Inscritos", p.accion, Modifier.weight(1f))
                    }
                    com.stiven.sos.ui.theme.SegmentadoNexo(
                        opciones = listOf(
                            Triple(0, "Aspirantes", Icons.Default.PersonAdd),
                            Triple(1, "Inscritos", Icons.Default.Groups)
                        ),
                        seleccion = pestana,
                        onCambio = { pestana = it },
                        colores = { if (it == 0) p.racha else p.accion }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.cargando && ui.aspirantes.isEmpty() && ui.inscritos.isEmpty() ->
                        CircularProgressIndicator(color = p.ia, modifier = Modifier.align(Alignment.Center))

                    pestana == 0 && ui.aspirantes.isEmpty() -> com.stiven.sos.ui.theme.EstadoVacioNexo(
                        titulo = "Sin aspirantes",
                        mensaje = "Cuando un estudiante pida unirse con el código del curso, aparecerá aquí.",
                        expresion = com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    pestana == 0 -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (ui.aspirantes.size > 1) {
                            item {
                                com.stiven.sos.ui.theme.BotonNexo(
                                    texto = "Aceptar a los ${ui.aspirantes.size}",
                                    onClick = { confirmarTodas = true },
                                    tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                                    icono = Icons.Default.DoneAll,
                                    habilitado = ui.procesando.isEmpty(),
                                    modifier = Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO).fillMaxWidth()
                                )
                            }
                        }
                        items(ui.aspirantes, key = { it.id ?: it.estudianteId }) { s ->
                            TarjetaAspirante(
                                s = s,
                                procesando = s.id in ui.procesando,
                                onAceptar = { vm.responder(s, true, null) },
                                onRechazar = { motivo -> vm.responder(s, false, motivo) },
                                modifier = Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO).fillMaxWidth()
                            )
                        }
                    }

                    else -> {
                        val lista = ui.inscritos.filter {
                            busqueda.isBlank() || it.nombre.contains(busqueda, true) || it.correo.contains(busqueda, true)
                        }
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Box(Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO)) {
                                    com.stiven.sos.ui.theme.BuscadorNexo(busqueda, { busqueda = it }, "Buscar por nombre o correo")
                                }
                            }
                            if (lista.isEmpty()) item {
                                com.stiven.sos.ui.theme.EstadoVacioNexo(
                                    titulo = if (ui.inscritos.isEmpty()) "Aún no hay inscritos" else "Sin resultados",
                                    mensaje = if (ui.inscritos.isEmpty()) "Acepta aspirantes para formar el grupo." else "Prueba con otro nombre.",
                                    expresion = com.stiven.sos.ui.theme.ExpresionRobi.PIENSA
                                )
                            }
                            items(lista, key = { it.uid.ifBlank { it.correo } }) { e ->
                                com.stiven.sos.ui.theme.FilaNexo(
                                    icono = Icons.Default.Person,
                                    color = if (e.estado.equals("activo", true)) p.accion else p.textoSuave,
                                    titulo = e.nombre.ifBlank { "Estudiante" },
                                    subtitulo = e.correo.ifBlank { null },
                                    modifier = Modifier.widthIn(max = com.stiven.sos.ui.theme.ANCHO_CONTENIDO_NEXO),
                                    extremo = {
                                        com.stiven.sos.ui.theme.EtiquetaEstadoNexo(
                                            e.estado.ifBlank { "activo" },
                                            if (e.estado.isBlank() || e.estado.equals("activo", true)) p.accion else p.textoSuave
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmarTodas) {
        com.stiven.sos.ui.theme.DialogoAccionNexo(
            icono = Icons.Default.DoneAll,
            color = p.accion,
            titulo = "¿Aceptar a todos?",
            mensaje = "${ui.aspirantes.size} estudiantes entrarán al curso y recibirán un aviso de bienvenida.",
            textoPrincipal = "Aceptar a todos",
            tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.ACCION,
            onPrincipal = { confirmarTodas = false; vm.aceptarTodas() },
            textoSecundario = "Cancelar",
            onSecundario = { confirmarTodas = false },
            onDismiss = { confirmarTodas = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaAspirante(
    s: SolicitudCurso,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    var rechazando by remember(s.id) { mutableStateOf(false) }
    var motivo by remember(s.id) { mutableStateOf<String?>(null) }
    var personalizado by remember(s.id) { mutableStateOf("") }

    com.stiven.sos.ui.theme.TarjetaNexo(modifier, acento = if (rechazando) p.error else p.racha, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.stiven.sos.ui.theme.AvatarNexo(s.estudianteNombre.ifBlank { "?" }, p.ia, tamano = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.estudianteNombre.ifBlank { "Estudiante" }, color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (s.estudianteEmail.isNotBlank()) {
                    Text(s.estudianteEmail, color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Text("Solicitó el ${fechaLegible(s.fechaSolicitud)}", color = p.textoTenue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
        s.mensajeEstudiante?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            com.stiven.sos.ui.theme.NotaNexo("\"$it\"", p.marca, Icons.Default.FormatQuote, titulo = "Mensaje del estudiante")
        }
        Spacer(Modifier.height(12.dp))
        if (!rechazando) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Rechazar",
                    onClick = { rechazando = true },
                    tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                    habilitado = !procesando,
                    alto = 48.dp,
                    modifier = Modifier.weight(1f)
                )
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Aceptar",
                    onClick = onAceptar,
                    tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                    icono = Icons.Default.Check,
                    cargando = procesando,
                    alto = 48.dp,
                    modifier = Modifier.weight(1.3f)
                )
            }
        } else {
            // Rechazo dentro de la misma tarjeta (sin ventana emergente)
            Text("¿Por qué no lo aceptas? (opcional)", color = p.texto, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MOTIVOS_RECHAZO.forEach { m ->
                    com.stiven.sos.ui.theme.FichaNexo(m, motivo == m, p.error, { motivo = if (motivo == m) null else m })
                }
            }
            Spacer(Modifier.height(8.dp))
            com.stiven.sos.ui.theme.CampoNexo(
                valor = personalizado,
                onCambio = { personalizado = it; if (it.isNotBlank()) motivo = null },
                etiqueta = "Otro motivo",
                lineasMinimas = 2,
                colorFoco = p.error
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Cancelar",
                    onClick = { rechazando = false; motivo = null; personalizado = "" },
                    tipo = com.stiven.sos.ui.theme.TipoBoton.SECUNDARIO,
                    habilitado = !procesando,
                    alto = 48.dp,
                    modifier = Modifier.weight(1f)
                )
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Rechazar",
                    onClick = { onRechazar(personalizado.ifBlank { motivo }) },
                    tipo = com.stiven.sos.ui.theme.TipoBoton.PELIGRO,
                    cargando = procesando,
                    alto = 48.dp,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}
