package com.stiven.sos

import com.stiven.sos.ui.theme.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.EduRachaV2Colors
import com.stiven.sos.ui.theme.EduRachaV2Gradients
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExplicacionTemaActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var temaExplicacion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Tema"
        temaExplicacion = intent.getStringExtra("tema_explicacion") ?: ""

        setContent {
            EduRachaTheme {
                com.stiven.sos.services.ConMensajesQuiz(quizViewModel) {
                LaunchedEffect(cursoId, temaId) {
                    quizViewModel.iniciarObservadores(cursoId, temaId)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        quizViewModel.detenerObservadores()
                    }
                }

                ExplicacionTemaScreen(
                    temaTitulo = temaTitulo,
                    temaExplicacion = temaExplicacion,
                    cursoId = cursoId,
                    temaId = temaId,
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onIniciarQuiz = { modo ->
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.putExtra("curso_id", cursoId)
                        intent.putExtra("tema_id", temaId)
                        intent.putExtra("tema_titulo", temaTitulo)
                        intent.putExtra("modo", modo)
                        startActivity(intent)
                        finish()
                    }
                )
                }
            }
        }
    }
}

// ============================================================================
// PANTALLA PRINCIPAL — Explicación interactiva (diseño Nexo)
// Cada sección es una tarjeta: el estudiante revela las ideas una a una y
// responde un mini reto de "completa la idea" antes de avanzar.
// ============================================================================

@Composable
fun ExplicacionTemaScreen(
    temaTitulo: String,
    temaExplicacion: String,
    cursoId: String,
    temaId: String,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onIniciarQuiz: (String) -> Unit
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val uiState by quizViewModel.uiState.collectAsState()

    // Dividir explicación en secciones (misma regla de siempre: párrafos separados por línea en blanco)
    val secciones = remember(temaExplicacion) {
        temaExplicacion.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
            .map { limpiarMarkdown(it) }
            .filter { it.isNotEmpty() }
    }

    var seccionesCompletadas by remember { mutableStateOf(setOf<Int>()) }
    val todasCompletadas = seccionesCompletadas.size == secciones.size

    // Regla: la explicación es obligatoria solo la primera vez. Si ya leyó esta misma versión,
    // queda como repaso opcional; si el docente la cambió, se vuelve a pedir. null = consultando.
    var yaLeida by remember { mutableStateOf<Boolean?>(null) }
    var explicacionCambio by remember { mutableStateOf(false) }
    LaunchedEffect(cursoId, temaId) {
        try {
            val r = com.stiven.sos.api.ApiClient.apiService.estadoExplicacion(cursoId, temaId)
            val cuerpo = if (r.isSuccessful) r.body() else null
            yaLeida = cuerpo?.get("vista") == true
            explicacionCambio = cuerpo?.get("cambio") == true
        } catch (e: Exception) {
            yaLeida = false
        }
    }
    val repasoOpcional = yaLeida == true && !todasCompletadas

    val aproboQuizOficial = uiState.porcentajeQuizOficial >= 80
    val enCooldown = uiState.yaResolviHoy && aproboQuizOficial

    // IMPORTANTE: Quiz práctica solo se habilita si YA resolvió el oficial HOY
    val yaResolviHoy = uiState.yaResolviHoy

    var seccionActual by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Lector de voz (opcional, sin permisos)
    val lector = rememberLectorVoz()

    // Marcar explicación como vista cuando se completan todas las secciones
    LaunchedEffect(todasCompletadas) {
        if (todasCompletadas) {
            quizViewModel.marcarExplicacionVista(temaId, cursoId) {}
        }
    }

    LaunchedEffect(Unit) {
        com.stiven.sos.services.GestorSonidos.inicializar(context)
    }

    FondoNexo(conCuadricula = false) {
        Column(Modifier.fillMaxSize()) {
            EncabezadoNexo(
                titulo = temaTitulo,
                subtitulo = "Explicación interactiva",
                onVolver = {
                    lector.detener()
                    onNavigateBack()
                },
                acciones = {
                    if (secciones.isNotEmpty() && lector.disponible) {
                        BotonIconoNexo(
                            icono = if (lector.hablando) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                            descripcion = if (lector.hablando) "Detener lectura" else "Escuchar esta tarjeta",
                            color = p.iaTexto,
                            onClick = {
                                if (lector.hablando) lector.detener()
                                else lector.leer(secciones.getOrNull(seccionActual) ?: "")
                            }
                        )
                    }
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (repasoOpcional) {
                    item {
                        FilaNexo(
                            icono = Icons.Default.CheckCircle,
                            color = p.accionTexto,
                            titulo = "Ya leíste esta explicación",
                            subtitulo = "Repasarla es opcional. Puedes ir directo al quiz o revisar las tarjetas abajo.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        QuizOficialCard(
                            aproboQuizOficial = aproboQuizOficial,
                            porcentajeObtenido = uiState.porcentajeQuizOficial,
                            enCooldown = enCooldown,
                            horasRestantes = uiState.horasParaNuevoQuiz,
                            minutosRestantes = uiState.minutosParaNuevoQuiz,
                            sinVidas = uiState.sinVidas,
                            onIniciar = {
                                lector.detener()
                                onIniciarQuiz("oficial")
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (yaResolviHoy && aproboQuizOficial) {
                        item {
                            QuizPracticaCard(
                                sinVidas = uiState.sinVidas,
                                onIniciar = {
                                    lector.detener()
                                    onIniciarQuiz("practica")
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (secciones.isEmpty()) {
                    item {
                        EstadoVacioNexo(
                            titulo = "Sin explicación",
                            mensaje = "Tu docente aún no ha publicado la explicación de este tema. Puedes ir directo al quiz."
                        )
                    }
                } else {
                    item {
                        IndicadoresProgreso(
                            total = secciones.size,
                            seccionActual = seccionActual,
                            seccionesCompletadas = seccionesCompletadas,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        GloboRobi(
                            titulo = "ROBI",
                            mensaje = obtenerMensajeBuho(seccionesCompletadas.size, secciones.size, todasCompletadas),
                            expresion = when {
                                todasCompletadas -> ExpresionRobi.CELEBRA
                                seccionesCompletadas.isEmpty() -> ExpresionRobi.SALUDA
                                else -> ExpresionRobi.FELIZ
                            },
                            tamanoRobi = 64.dp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    item {
                        key(seccionActual) {
                            StoryCard(
                                numero = seccionActual + 1,
                                total = secciones.size,
                                contenido = secciones[seccionActual.coerceIn(0, secciones.size - 1)],
                                todas = secciones,
                                completada = seccionesCompletadas.contains(seccionActual),
                                onCompletar = {
                                    seccionesCompletadas = seccionesCompletadas + seccionActual
                                    com.stiven.sos.services.GestorSonidos.reproducir(
                                        context, com.stiven.sos.services.GestorSonidos.Sonido.AVANCE
                                    )
                                    scope.launch {
                                        delay(700)
                                        if (seccionActual < secciones.size - 1) {
                                            lector.detener()
                                            seccionActual++
                                        } else {
                                            listState.animateScrollToItem(3)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (todasCompletadas) {
                    item {
                        CardTemaCompletadoModerno(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        QuizOficialCard(
                            aproboQuizOficial = aproboQuizOficial,
                            porcentajeObtenido = uiState.porcentajeQuizOficial,
                            enCooldown = enCooldown,
                            horasRestantes = uiState.horasParaNuevoQuiz,
                            minutosRestantes = uiState.minutosParaNuevoQuiz,
                            sinVidas = uiState.sinVidas,
                            onIniciar = {
                                lector.detener()
                                onIniciarQuiz("oficial")
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (yaResolviHoy && aproboQuizOficial) {
                        item {
                            QuizPracticaCard(
                                sinVidas = uiState.sinVidas,
                                onIniciar = {
                                    lector.detener()
                                    onIniciarQuiz("practica")
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else if (!repasoOpcional) {
                    item {
                        FilaNexo(
                            icono = Icons.Default.Lock,
                            color = p.textoSuave,
                            titulo = if (explicacionCambio) "Explicación actualizada" else "Quiz bloqueado",
                            subtitulo = if (explicacionCambio)
                                "Tu docente cambió esta explicación. Léela de nuevo para desbloquear el quiz."
                            else "Completa las ${secciones.size} tarjetas para desbloquearlo",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Navegación inferior entre tarjetas
            if (secciones.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.superficie)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val puedeAvanzar = (seccionesCompletadas.contains(seccionActual) || yaLeida == true) &&
                        seccionActual < secciones.size - 1
                    BotonNexo(
                        texto = "Anterior",
                        onClick = {
                            if (seccionActual > 0) {
                                lector.detener()
                                seccionActual--
                            }
                        },
                        habilitado = seccionActual > 0,
                        tipo = TipoBoton.SECUNDARIO,
                        icono = Icons.Default.ArrowBack,
                        alto = 50.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${seccionActual + 1}/${secciones.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = p.texto
                    )
                    BotonNexo(
                        texto = "Siguiente",
                        onClick = {
                            if (puedeAvanzar) {
                                lector.detener()
                                seccionActual++
                            }
                        },
                        habilitado = puedeAvanzar,
                        tipo = TipoBoton.MARCA,
                        icono = Icons.Default.ArrowForward,
                        alto = 50.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// PROGRESO SEGMENTADO
// ============================================================================

@Composable
fun IndicadoresProgreso(
    total: Int,
    seccionActual: Int,
    seccionesCompletadas: Set<Int>,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${seccionesCompletadas.size} de $total tarjetas completadas" },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { i ->
            val color by animateColorAsState(
                targetValue = when {
                    seccionesCompletadas.contains(i) -> p.accion
                    i == seccionActual -> p.marca
                    else -> p.borde
                },
                label = "segmento"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(if (i == seccionActual) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

fun obtenerMensajeBuho(completadas: Int, total: Int, todasCompletadas: Boolean): String {
    return when {
        todasCompletadas -> "¡Leíste todo! Ya puedes hacer el quiz."
        completadas == 0 -> "Revela cada idea y responde el mini reto para avanzar."
        completadas >= total / 2 -> "Vas por la mitad. Sigue así."
        else -> "Buen comienzo. Te faltan ${total - completadas} tarjetas."
    }
}

// ============================================================================
// MINI RETO "COMPLETA LA IDEA" (se genera localmente a partir del texto)
// ============================================================================

data class RetoCompletar(val frase: String, val respuesta: String, val opciones: List<String>)

private val PALABRAS_COMUNES = setOf(
    "además", "también", "cuando", "porque", "entonces", "mientras", "algunos", "algunas", "otros", "otras",
    "puede", "pueden", "tiene", "tienen", "sobre", "entre", "desde", "hasta", "donde", "cuales", "cualquier",
    "siempre", "nuestro", "nuestra", "ejemplo", "través", "durante", "aunque", "después", "antes", "mismo",
    "misma", "manera", "forma", "parte", "estos", "estas", "aquellos", "aquellas", "ningún", "ninguna",
    "muchos", "muchas", "cuanto", "debemos", "hacemos", "importante", "siguiente", "general", "ahora"
)

private fun palabrasClave(texto: String): List<String> =
    Regex("\\p{L}{6,}").findAll(texto).map { it.value }
        .filter { it.lowercase() !in PALABRAS_COMUNES }
        .toList()

fun generarReto(seccion: String, todas: List<String>, semilla: Int): RetoCompletar? {
    val frases = seccion.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length > 25 }
    if (frases.isEmpty()) return null
    val rnd = kotlin.random.Random(semilla * 31 + seccion.length)
    val candidatas = frases.flatMap { f -> palabrasClave(f).map { f to it } }
    if (candidatas.isEmpty()) return null
    // Preferir palabras largas (suelen ser los conceptos)
    val top = candidatas.sortedByDescending { it.second.length }.take(4)
    val (frase, respuesta) = top[rnd.nextInt(top.size)]
    val distractores = todas.flatMap { palabrasClave(it) }
        .map { it.lowercase() }
        .distinct()
        .filter { it != respuesta.lowercase() }
        .shuffled(rnd)
        .take(2)
    if (distractores.size < 2) return null
    val opciones = (distractores + respuesta.lowercase()).shuffled(rnd)
    val oculta = frase.replaceFirst(respuesta, "_____")
    return RetoCompletar(oculta, respuesta.lowercase(), opciones)
}

// ============================================================================
// TARJETA INTERACTIVA
// ============================================================================

@Composable
fun StoryCard(
    numero: Int,
    total: Int,
    contenido: String,
    todas: List<String>,
    completada: Boolean,
    onCompletar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val ideas = remember(contenido) {
        contenido.split(Regex("(?<=[.!?])\\s+|\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf(contenido) }
    }
    val reto = remember(contenido) { generarReto(contenido, todas, numero) }
    var reveladas by remember { mutableStateOf(if (completada) ideas.size else 1) }
    var elegida by remember { mutableStateOf<String?>(null) }
    var acerto by remember { mutableStateOf(completada) }
    val sacudida = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val todoRevelado = reveladas >= ideas.size

    TarjetaNexo(
        modifier = modifier.fillMaxWidth(),
        acento = if (completada) p.accion else p.marca,
        padding = 18.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TARJETA $numero DE $total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = p.marcaTexto,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
            )
            if (completada) {
                ChipNexo(texto = "Completada", color = p.accionTexto, icono = Icons.Default.CheckCircle)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Ideas reveladas una por una
        ideas.take(reveladas).forEachIndexed { i, idea ->
            var visible by remember { mutableStateOf(completada || i == 0) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350)) + expandVertically(tween(350))
            ) {
                Row(Modifier.padding(vertical = 5.dp)) {
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i == reveladas - 1 && !todoRevelado) p.marca else p.ia)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        idea,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.texto,
                        lineHeight = 25.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            completada -> Unit

            !todoRevelado -> {
                BotonNexo(
                    texto = "Revelar siguiente idea (${reveladas}/${ideas.size})",
                    onClick = { reveladas++ },
                    tipo = TipoBoton.MARCA,
                    icono = Icons.Default.AutoAwesome,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            reto != null && !acerto -> {
                // Mini reto antes de avanzar
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = sacudida.value * density }
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.ia.copy(alpha = 0.10f))
                        .border(1.dp, p.ia.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = p.iaTexto, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RETO DE ROBI · COMPLETA LA IDEA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = p.iaTexto, letterSpacing = 0.8.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(reto.frase, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = p.texto, lineHeight = 23.sp)
                    Spacer(Modifier.height(10.dp))
                    reto.opciones.forEach { opcion ->
                        val esElegida = elegida == opcion
                        val colorBorde = when {
                            esElegida && opcion == reto.respuesta -> p.accion
                            esElegida -> p.error
                            else -> p.borde
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.superficie)
                                .border(2.dp, colorBorde, RoundedCornerShape(12.dp))
                                .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                                    elegida = opcion
                                    if (opcion == reto.respuesta) {
                                        acerto = true
                                        com.stiven.sos.services.GestorSonidos.reproducir(context, com.stiven.sos.services.GestorSonidos.Sonido.CORRECTO)
                                        onCompletar()
                                    } else {
                                        com.stiven.sos.services.GestorSonidos.reproducir(context, com.stiven.sos.services.GestorSonidos.Sonido.ERROR)
                                        scope.launch {
                                            for (x in listOf(10f, -10f, 6f, -6f, 0f)) sacudida.animateTo(x, tween(55))
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(opcion, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = p.texto)
                        }
                    }
                    if (elegida != null && elegida != reto.respuesta) {
                        Spacer(Modifier.height(4.dp))
                        Text("Casi. Vuelve a leer la tarjeta e inténtalo otra vez.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.errorTexto)
                    }
                }
            }

            else -> {
                BotonNexo(
                    texto = "Lo entendí",
                    onClick = onCompletar,
                    tipo = TipoBoton.ACCION,
                    icono = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// TARJETAS FINALES
// ============================================================================

@Composable
fun CardTemaCompletadoModerno(modifier: Modifier = Modifier) {
    val p = LocalNexo.current
    TarjetaNexo(modifier = modifier.fillMaxWidth(), acento = p.accion) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Default.EmojiEvents, p.racha, tamano = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("¡Explicación completada!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = p.texto)
                Text("Ahora demuestra lo que aprendiste.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
        }
    }
}

@Composable
fun QuizOficialCard(
    aproboQuizOficial: Boolean,
    porcentajeObtenido: Int,
    enCooldown: Boolean,
    horasRestantes: Int,
    minutosRestantes: Int,
    sinVidas: Boolean,
    onIniciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    val puedeIniciar = !sinVidas && !enCooldown
    TarjetaNexo(modifier = modifier.fillMaxWidth(), acento = p.marca) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Default.Quiz, p.marca, tamano = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Quiz oficial", fontSize = 18.sp, fontWeight = FontWeight.Black, color = p.texto)
                Text("Demuestra tu conocimiento", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
            if (aproboQuizOficial) {
                ChipNexo(texto = "$porcentajeObtenido%", color = p.accionTexto, icono = Icons.Default.CheckCircle)
            }
        }
        Spacer(Modifier.height(12.dp))
        CaracteristicaQuiz(icono = Icons.Default.LocalFireDepartment, color = p.racha, texto = "Cuenta para tu racha diaria")
        CaracteristicaQuiz(icono = Icons.Default.Bolt, color = p.ia, texto = "Gana experiencia y progreso")
        if (enCooldown) {
            CaracteristicaQuiz(icono = Icons.Default.Schedule, color = p.marca, texto = "Próximo intento: ${horasRestantes}h ${minutosRestantes}m")
        }
        Spacer(Modifier.height(14.dp))
        BotonNexo(
            texto = when {
                enCooldown -> "Disponible más tarde"
                sinVidas -> "Sin vidas"
                else -> "Comenzar quiz"
            },
            onClick = onIniciar,
            habilitado = puedeIniciar,
            tipo = TipoBoton.ACCION,
            icono = if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun QuizPracticaCard(
    sinVidas: Boolean,
    onIniciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexo.current
    val puedeIniciar = !sinVidas
    TarjetaNexo(modifier = modifier.fillMaxWidth(), acento = p.ia) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(Icons.Default.FitnessCenter, p.ia, tamano = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Modo práctica", fontSize = 18.sp, fontWeight = FontWeight.Black, color = p.texto)
                Text("Refuerza tu aprendizaje", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.textoSuave)
            }
        }
        Spacer(Modifier.height(12.dp))
        CaracteristicaQuiz(icono = Icons.Default.AllInclusive, color = p.marca, texto = "Practica sin límites")
        CaracteristicaQuiz(icono = Icons.Default.Bolt, color = p.ia, texto = "Gana XP sin afectar tu racha")
        Spacer(Modifier.height(14.dp))
        BotonNexo(
            texto = if (sinVidas) "Sin vidas" else "Comenzar práctica",
            onClick = onIniciar,
            habilitado = puedeIniciar,
            tipo = TipoBoton.IA,
            icono = if (puedeIniciar) Icons.Default.PlayArrow else Icons.Default.Lock,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CaracteristicaQuiz(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    texto: String
) {
    val p = LocalNexo.current
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.texto)
    }
}

// ============================================================================
// LECTOR DE VOZ (TextToSpeech del sistema)
// ============================================================================

class LectorVoz {
    var disponible by mutableStateOf(false)
    var hablando by mutableStateOf(false)
    internal var tts: android.speech.tts.TextToSpeech? = null

    fun leer(texto: String) {
        val motor = tts ?: return
        if (!disponible || texto.isBlank()) return
        hablando = true
        motor.speak(texto, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "explicacion")
    }

    fun detener() {
        tts?.stop()
        hablando = false
    }
}

@Composable
fun rememberLectorVoz(): LectorVoz {
    val context = LocalContext.current
    val lector = remember { LectorVoz() }
    DisposableEffect(Unit) {
        var motor: android.speech.tts.TextToSpeech? = null
        motor = android.speech.tts.TextToSpeech(context.applicationContext) { estado ->
            if (estado == android.speech.tts.TextToSpeech.SUCCESS) {
                val r = motor?.setLanguage(java.util.Locale("es", "ES"))
                lector.disponible = r != android.speech.tts.TextToSpeech.LANG_MISSING_DATA &&
                        r != android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
                motor?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { lector.hablando = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { lector.hablando = false }
                })
            }
        }
        lector.tts = motor
        onDispose {
            motor?.stop()
            motor?.shutdown()
            lector.tts = null
        }
    }
    return lector
}

/** Quita marcas de Markdown que a veces devuelve la IA (negritas, títulos, viñetas, código). */
fun limpiarMarkdown(texto: String): String =
    texto.lines().joinToString("\n") { linea ->
        linea.trim()
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^[-*+]\\s+"), "• ")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
    }.trim()
