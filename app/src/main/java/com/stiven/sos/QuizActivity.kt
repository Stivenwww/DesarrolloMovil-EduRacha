package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.stiven.sos.services.AnimacionEstrellaIncorrecta
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.BotonJugoso
import com.stiven.sos.ui.theme.PlayColors
import com.stiven.sos.ui.theme.RachiAnimo
import com.stiven.sos.ui.theme.RachiMascota
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class QuizActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()

    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var userId: String
    private lateinit var modo: String

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Quiz"
        modo = intent.getStringExtra("modo") ?: "oficial"

        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        userId = prefs.getString("user_uid", "") ?: ""
        // Precarga los efectos de sonido para que el primero no llegue tarde
        com.stiven.sos.services.GestorSonidos.inicializar(this)

        if (userId.isNotEmpty() && cursoId.isNotEmpty() && temaId.isNotEmpty()) {
            com.stiven.sos.services.NotificacionesHelper.notificarQuizIniciado(
                estudianteId = userId,
                cursoId = cursoId,
                temaId = temaId,
                tituloTema = temaTitulo
            )
        }

        setContent {
            EduRachaTheme {
                com.stiven.sos.services.ConMensajesQuiz(quizViewModel) {
                val uiState by quizViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.quizInterrumpidoPorVidas) {
                    if (uiState.quizInterrumpidoPorVidas) {
                        android.util.Log.e("QuizActivity", "========================================")
                        android.util.Log.e("QuizActivity", "DETECCION CRITICA: Quiz interrumpido por vidas")
                        android.util.Log.e("QuizActivity", "ACCION: Redirigiendo en 2 segundos")
                        android.util.Log.e("QuizActivity", "========================================")

                        kotlinx.coroutines.delay(100000)
                        regresarATemasDelCurso()
                    }
                }

                LaunchedEffect(uiState.sinVidas, uiState.quizActivo) {
                    if (uiState.sinVidas &&
                        uiState.quizActivo != null &&
                        !uiState.quizInterrumpidoPorVidas &&
                        !uiState.finalizando) {

                        android.util.Log.w("QuizActivity", "========================================")
                        android.util.Log.w("QuizActivity", "DETECCION: Usuario sin vidas durante quiz")
                        android.util.Log.w("QuizActivity", "Modo: $modo")
                        android.util.Log.w("QuizActivity", "ACCION: Mostrando dialogo de bloqueo")
                        android.util.Log.w("QuizActivity", "========================================")

                        quizViewModel.mostrarDialogoSinVidas()
                    }
                }

                QuizScreen(
                    cursoId = cursoId,
                    temaId = temaId,
                    temaTitulo = temaTitulo,
                    modo = modo,
                    quizViewModel = quizViewModel,
                    onNavigateToResultado = {
                        val resultado = quizViewModel.uiState.value.resultadoQuiz
                        val quizId = quizViewModel.uiState.value.quizActivo?.quizId

                        val intent = Intent(this, ResultadoQuizActivity::class.java)
                        intent.putExtra("preguntasCorrectas", resultado?.preguntasCorrectas ?: 0)
                        intent.putExtra("preguntasIncorrectas", resultado?.preguntasIncorrectas ?: 0)
                        intent.putExtra("experienciaGanada", resultado?.experienciaGanada ?: 0)
                        intent.putExtra("vidasRestantes", resultado?.vidasRestantes ?: 0)
                        intent.putExtra("bonificacionRapidez", resultado?.bonificaciones?.rapidez ?: 0)
                        intent.putExtra("bonificacionPrimeraVez", resultado?.bonificaciones?.primeraVez ?: 0)
                        intent.putExtra("bonificacionTodoCorrecto", resultado?.bonificaciones?.todoCorrecto ?: 0)
                        intent.putExtra("quizId", quizId ?: "")
                        intent.putExtra("modo", modo)

                        startActivity(intent)
                        finish()
                    },
                    onRegresarATemasDelCurso = {
                        regresarATemasDelCurso()
                    }
                )
                }
            }
        }
    }

    private fun regresarATemasDelCurso() {
        quizViewModel.limpiarQuiz()
        val intent = Intent(this, TemasDelCursoActivity::class.java)
        intent.putExtra("curso_id", cursoId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            android.util.Log.w("QuizActivity", "Usuario intento salir durante el quiz")
        }
    }

    override fun onResume() {
        super.onResume()
        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            android.util.Log.d("QuizActivity", "App regreso con quiz activo")
        }
    }
}

// ============================================================================
// HEADER FIGMA - Vidas, Racha, XP y Progreso con Flecha de Regreso
// ============================================================================

@Composable
fun HeaderQuizFigma(
    vidasActuales: Int,
    vidasMax: Int,
    rachaActual: Int,
    xpActual: Int,
    preguntaActual: Int,
    totalPreguntas: Int,
    onBackClick: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val progreso by animateFloatAsState(
        targetValue = if (totalPreguntas > 0) preguntaActual.toFloat() / totalPreguntas else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progreso"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.stiven.sos.ui.theme.BotonIconoNexo(
                icono = Icons.Default.Close,
                descripcion = "Salir del quiz",
                onClick = onBackClick
            )
            Column(Modifier.weight(1f)) {
                com.stiven.sos.ui.theme.BarraNexo(progreso = progreso, color = p.accion, alto = 14.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pregunta $preguntaActual de $totalPreguntas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textoSuave
                )
            }
            com.stiven.sos.ui.theme.ChipNexo(
                texto = "$vidasActuales",
                color = p.error,
                icono = Icons.Filled.Favorite,
                descripcion = "$vidasActuales de $vidasMax vidas"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.stiven.sos.ui.theme.ChipNexo(
                texto = "$rachaActual",
                color = p.racha,
                icono = Icons.Filled.LocalFireDepartment,
                descripcion = "Racha de $rachaActual días"
            )
            com.stiven.sos.ui.theme.ChipNexo(
                texto = "$xpActual XP",
                color = p.ia,
                icono = Icons.Filled.Bolt,
                descripcion = "$xpActual puntos de experiencia"
            )
        }
    }
}

// ============================================================================
// IDENTIFICADOR GAMIFICADO DE MODO
// ============================================================================

@Composable
fun IdentificadorModoQuiz(
    modo: String,
    modifier: Modifier = Modifier
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val (color, icono, titulo) = when (modo) {
        "practica" -> Triple(p.ia, Icons.Default.Lightbulb, "MODO PRÁCTICA")
        "final" -> Triple(p.racha, Icons.Default.EmojiEvents, "QUIZ FINAL")
        else -> Triple(p.marca, Icons.Default.Quiz, "QUIZ OFICIAL")
    }
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
    }
}

// ============================================================================
// MASCOTA BÚHO ANIMADA (3 Estados: Pensativo, Feliz, Triste)
// ============================================================================

@Composable
fun BuhoMascotaAnimada(
    estado: EstadoBuho,
    mostrarAnimacion: Boolean
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val escala by animateFloatAsState(
        targetValue = if (mostrarAnimacion) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala"
    )
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        if (mostrarAnimacion && estado == EstadoBuho.FELIZ) {
            ConfetiAnimado(isTablet = false)
        }
        com.stiven.sos.ui.theme.RobiNeo(
            tamano = 120.dp,
            expresion = when (estado) {
                EstadoBuho.PENSATIVO -> com.stiven.sos.ui.theme.ExpresionRobi.PIENSA
                EstadoBuho.FELIZ -> com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA
                EstadoBuho.TRISTE -> com.stiven.sos.ui.theme.ExpresionRobi.TRISTE
            },
            modifier = Modifier.scale(escala)
        )
    }
}

enum class EstadoBuho {
    PENSATIVO, FELIZ, TRISTE
}

@Composable
fun ConfetiAnimado(isTablet: Boolean) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val avance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        avance.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val colores = listOf(p.racha, p.marca, p.accion, p.ia, p.racha, p.error, p.marca, p.racha)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val t = avance.value
        val radioMax = size.minDimension * 0.62f
        for (i in colores.indices) {
            val ang = (i * 45.0 - 90.0) * Math.PI / 180.0
            val r = radioMax * t
            val centro = Offset(c.x + (cos(ang) * r).toFloat(), c.y + (sin(ang) * r).toFloat())
            val tam = (if (i % 2 == 0) 9.dp.toPx() else 6.dp.toPx()) * (1f - t * 0.4f)
            drawPath(estrellaQuiz(centro, tam), colores[i].copy(alpha = 1f - t * 0.7f))
        }
    }
}

private fun estrellaQuiz(c: Offset, r: Float): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0 until 10) {
        val radio = if (i % 2 == 0) r else r * 0.45f
        val ang = Math.PI / 5 * i - Math.PI / 2
        val x = c.x + (cos(ang) * radio).toFloat()
        val y = c.y + (sin(ang) * radio).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}



// ============================================================================
// FEEDBACK MESSAGE FIGMA
// ============================================================================

@Composable
fun MensajeFeedbackFigma(esCorrecta: Boolean?, xpGanado: Int, combo: Int = 0) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    AnimatedVisibility(
        visible = esCorrecta != null,
        enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + slideOutVertically()
    ) {
        val color = if (esCorrecta == true) p.accion else p.error
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.16f))
                .border(2.dp, color, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (esCorrecta == true) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (esCorrecta == true) "¡Correcto!" else "Respuesta incorrecta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (esCorrecta == true) p.accionTexto else p.errorTexto
                )
                Text(
                    if (esCorrecta == true) "Sigue así, vas construyendo tu racha."
                    else "Pierdes una vida. Revisa la explicación al final.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textoSuave
                )
            }
            if (esCorrecta == true && combo >= 2) {
                Text(
                    "x$combo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = p.sobreAccion,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.racha)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// PREGUNTA CARD FIGMA - Diseño exacto de Figma
// ============================================================================

@Composable
fun PreguntaCardFigma(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.TarjetaNexo(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        acento = p.marca,
        padding = 18.dp
    ) {
        Text(
            "PREGUNTA $numeroPregunta",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = p.marcaTexto,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            pregunta.texto,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = p.texto,
            lineHeight = 27.sp
        )
    }
}

// ============================================================================
// OPCIÓN RESPUESTA FIGMA - Diseño exacto de Figma
// ============================================================================

@Composable
fun OpcionRespuestaFigma(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    isSelected: Boolean,
    esRespuestaCorrecta: Boolean?,
    esRespuestaIncorrecta: Boolean?,
    enabled: Boolean = true,
    letra: String = "",
    onClick: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val (fondo, borde, sombra, textoColor) = when {
        esRespuestaCorrecta == true -> listOf(p.accion.copy(alpha = 0.16f), p.accion, p.accionSombra, p.accionTexto)
        esRespuestaIncorrecta == true -> listOf(p.error.copy(alpha = 0.16f), p.error, p.errorSombra, p.errorTexto)
        isSelected -> listOf(p.marca.copy(alpha = 0.14f), p.marca, p.marcaSombra, p.marcaTexto)
        else -> listOf(p.superficie, p.borde, p.borde, p.texto)
    }

    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val profundidad = 4.dp
    val hundido by animateDpAsState(
        targetValue = if (presionado && enabled) profundidad else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "opcion"
    )
    // "Salto" al acertar y "temblor" al fallar
    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(esRespuestaIncorrecta) {
        if (esRespuestaIncorrecta == true) {
            for (x in listOf(10f, -10f, 6f, -6f, 0f)) sacudida.animateTo(x, tween(60))
        }
    }
    val salto by animateFloatAsState(
        targetValue = if (esRespuestaCorrecta == true) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "salto"
    )
    val forma = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .graphicsLayer {
                translationX = sacudida.value * density
                scaleX = salto
                scaleY = salto
            }
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = profundidad)
                .clip(forma)
                .background(sombra)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = profundidad)
                .offset(y = hundido)
                .clip(forma)
                .background(p.superficie)
                .background(fondo)
                .border(2.dp, borde, forma)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (letra.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected || esRespuestaCorrecta == true || esRespuestaIncorrecta == true) borde else p.superficieAlt)
                        .border(1.dp, borde, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letra,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected || esRespuestaCorrecta == true || esRespuestaIncorrecta == true) p.sobreMarca else p.textoSuave
                    )
                }
            }
            Text(
                opcion.texto,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textoColor,
                modifier = Modifier.weight(1f),
                lineHeight = 23.sp
            )
            when {
                esRespuestaCorrecta == true -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Respuesta correcta",
                    tint = p.accion,
                    modifier = Modifier.size(28.dp)
                )
                esRespuestaIncorrecta == true -> Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "Respuesta incorrecta",
                    tint = p.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ============================================================================
// BOTÓN SIGUIENTE FIGMA
// ============================================================================

@Composable
fun BotonSiguienteFigma(
    enabled: Boolean,
    esUltimaPregunta: Boolean,
    onClick: () -> Unit
) {
    com.stiven.sos.ui.theme.BotonNexo(
        texto = if (esUltimaPregunta) "Comprobar y finalizar" else "Comprobar",
        onClick = onClick,
        habilitado = enabled,
        tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
        modifier = Modifier.fillMaxWidth()
    )
}

// ============================================================================
// PANTALLA DE PREGUNTA FIGMA
// ============================================================================

@Composable
fun PreguntaScreenFigma(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    temaTitulo: String,
    modo: String,
    sinVidas: Boolean,
    respuestasEstado: Map<Int, Boolean>,
    ultimaRespuestaCorrecta: Boolean?,
    mostrarAnimacionRespuesta: Boolean,
    vidasActuales: Int,
    vidasMax: Int,
    rachaActual: Int,
    xpActual: Int,
    onRespuestaSeleccionada: (Int) -> Unit,
    onAnimacionCompletada: () -> Unit,
    onBackClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    var mostrarExplicacion by remember(pregunta.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Resultado de ESTA pregunta (el servidor lo confirma; null mientras se evalúa).
    // Se usa en lugar de ultimaRespuestaCorrecta, que podía traer el valor de la pregunta anterior.
    val resultadoPregunta: Boolean? = respuestasEstado[numeroPregunta - 1]

    // Estado del búho
    val estadoBuho = when {
        mostrarAnimacionRespuesta && resultadoPregunta == true -> EstadoBuho.FELIZ
        mostrarAnimacionRespuesta && resultadoPregunta == false -> EstadoBuho.TRISTE
        else -> EstadoBuho.PENSATIVO
    }

    LaunchedEffect(sinVidas) {
        if (sinVidas) {
            android.util.Log.w("PreguntaScreen", "Detección: Usuario sin vidas")
            android.util.Log.w("PreguntaScreen", "Cancelando animación y limpiando selección")
            respuestaSeleccionada = null
        }
    }

    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Sonido al conocer el resultado de la respuesta (respeta la preferencia del usuario)
    LaunchedEffect(mostrarAnimacionRespuesta, resultadoPregunta) {
        if (mostrarAnimacionRespuesta && resultadoPregunta != null && !sinVidas) {
            com.stiven.sos.services.GestorSonidos.reproducir(
                context,
                if (resultadoPregunta == true) com.stiven.sos.services.GestorSonidos.Sonido.CORRECTO
                else com.stiven.sos.services.GestorSonidos.Sonido.ERROR
            )
        }
    }

    // Aciertos seguidos hasta esta pregunta (solo visual, no altera la XP)
    val combo = remember(respuestasEstado, numeroPregunta) {
        var n = 0
        var idx = numeroPregunta - 1
        while (idx >= 0 && respuestasEstado[idx] == true) { n++; idx-- }
        n
    }

    com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            HeaderQuizFigma(
                vidasActuales = vidasActuales,
                vidasMax = vidasMax,
                rachaActual = rachaActual,
                xpActual = xpActual,
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IdentificadorModoQuiz(modo = modo)
                    Spacer(Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BuhoMascotaAnimada(
                        estado = estadoBuho,
                        mostrarAnimacion = mostrarAnimacionRespuesta
                    )
                }

                PreguntaCardFigma(
                    pregunta = pregunta,
                    numeroPregunta = numeroPregunta
                )

                val letras = listOf("A", "B", "C", "D", "E", "F")
                pregunta.opciones.forEachIndexed { index, opcion ->
                    // Un único estado por opción: mientras el servidor evalúa se ve solo "seleccionada";
                    // al llegar el resultado se pinta verde o rojo y ya no cambia.
                    val esCorrecta = mostrarExplicacion && respuestaSeleccionada == index && resultadoPregunta == true
                    val esIncorrecta = mostrarExplicacion && respuestaSeleccionada == index && resultadoPregunta == false

                    OpcionRespuestaFigma(
                        opcion = opcion,
                        isSelected = respuestaSeleccionada == index,
                        esRespuestaCorrecta = esCorrecta,
                        esRespuestaIncorrecta = esIncorrecta,
                        enabled = !sinVidas && !mostrarExplicacion,
                        letra = letras.getOrElse(index) { "${index + 1}" },
                        onClick = {
                            if (!sinVidas && !mostrarExplicacion) {
                                respuestaSeleccionada = index
                            } else {
                                android.util.Log.w("PreguntaScreen", "Intento bloqueado: Sin vidas")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // Retroalimentación + botón inferior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.superficie)
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MensajeFeedbackFigma(
                    esCorrecta = if (mostrarAnimacionRespuesta) resultadoPregunta else null,
                    xpGanado = 0,
                    combo = combo
                )
                Box(Modifier.padding(horizontal = 20.dp)) {
                    BotonSiguienteFigma(
                        enabled = respuestaSeleccionada != null && !sinVidas && !mostrarExplicacion,
                        esUltimaPregunta = numeroPregunta >= totalPreguntas,
                        onClick = {
                            if (!sinVidas && respuestaSeleccionada != null) {
                                android.util.Log.d("PreguntaScreen", "Respuesta confirmada: índice $respuestaSeleccionada")
                                mostrarExplicacion = true
                                onRespuestaSeleccionada(respuestaSeleccionada!!)
                            } else {
                                android.util.Log.w("PreguntaScreen", "Click bloqueado: Sin vidas")
                            }
                        }
                    )
                }
            }
        }

        // Capa de bloqueo sin vidas
        if (sinVidas) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                com.stiven.sos.ui.theme.TarjetaNexo(acento = p.error, padding = 28.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        com.stiven.sos.ui.theme.RobiNeo(tamano = 100.dp, expresion = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE)
                        Spacer(Modifier.height(10.dp))
                        Text("Quiz bloqueado", fontSize = 22.sp, fontWeight = FontWeight.Black, color = p.errorTexto)
                        Text("Te quedaste sin vidas", fontSize = 15.sp, color = p.textoSuave, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Avance automático después de mostrar la retroalimentación
    if (mostrarAnimacionRespuesta && !sinVidas) {
        LaunchedEffect(Unit) {
            delay(2000)
            onAnimacionCompletada()
        }
    }
}

// ============================================================================
// QUIZ SCREEN PRINCIPAL
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    cursoId: String,
    temaId: String,
    temaTitulo: String,
    modo: String,
    quizViewModel: QuizViewModel,
    onNavigateToResultado: () -> Unit,
    onRegresarATemasDelCurso: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()

    var yaNavego by remember { mutableStateOf(false) }
    var yaCargoRetroalimentacion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    var tiempoTotalQuiz by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        quizViewModel.iniciarObservadores(cursoId, temaId)
        quizViewModel.iniciarQuiz(cursoId, temaId, modo)
    }

    LaunchedEffect(uiState.quizActivo, uiState.finalizando) {
        if (uiState.quizActivo != null && !uiState.finalizando) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tiempoTotalQuiz++
            }
        }
    }

    LaunchedEffect(uiState.sinVidas, uiState.quizActivo) {
        if (uiState.sinVidas &&
            uiState.quizActivo != null &&
            !uiState.finalizando &&
            !uiState.quizInterrumpidoPorVidas) {

            android.util.Log.w("QuizScreen", "Usuario sin vidas durante quiz")
            android.util.Log.w("QuizScreen", "Modo: $modo")
            android.util.Log.w("QuizScreen", "Mostrando dialogo SIN auto-cierre")

            quizViewModel.mostrarDialogoSinVidas()
        }
    }

    BackHandler(enabled = true) {
        if (uiState.mostrarDialogoSinVidas) {
            android.util.Log.w("QuizScreen", "Back button bloqueado: Dialogo de sin vidas activo")
            return@BackHandler
        }

        if (uiState.finalizando) {
            android.util.Log.w("QuizScreen", "Bloqueada salida durante finalizacion")
        } else if (uiState.quizActivo != null) {
            mostrarDialogoSalir = true
        } else {
            onRegresarATemasDelCurso()
        }
    }

    LaunchedEffect(uiState.resultadoQuiz) {
        if (uiState.resultadoQuiz != null && !yaCargoRetroalimentacion) {
            yaCargoRetroalimentacion = true
            val quizId = uiState.quizActivo?.quizId

            if (quizId != null && (uiState.resultadoQuiz?.preguntasIncorrectas ?: 0) > 0) {
                quizViewModel.obtenerRetroalimentacion(quizId)
                kotlinx.coroutines.delay(500)
            }

            if (!yaNavego) {
                yaNavego = true
                onNavigateToResultado()
            }
        }
    }

    // Todos los diálogos (mantienen funcionalidad original)
    if (uiState.mostrarDialogoPeriodoFinalizado) {
        DialogoPeriodoFinalizadoFigma(
            mensajeError = uiState.mensajeErrorDetallado,
            temaTitulo = temaTitulo,
            onAceptar = {
                quizViewModel.cerrarDialogoPeriodoFinalizado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoErrorGeneral) {
        DialogoErrorGeneralFigma(
            titulo = uiState.tituloError,
            mensaje = uiState.mensajeErrorDetallado,
            onAceptar = {
                quizViewModel.cerrarDialogoErrorGeneral()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoTemaAprobado) {
        DialogoTemaYaAprobadoFigma(
            onContinuar = {
                quizViewModel.forzarInicioQuiz(cursoId, temaId, "practica")
            },
            onCancelar = {
                quizViewModel.cerrarDialogoTemaAprobado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (uiState.mostrarDialogoQuizFinalCompletado) {
        DialogoQuizFinalCompletadoFigma(
            onAceptar = {
                quizViewModel.cerrarDialogoQuizFinalCompletado()
                onRegresarATemasDelCurso()
            }
        )
    }

    if (mostrarDialogoSalir) {
        DialogoAbandonarQuizFigma(
            onConfirmar = {
                mostrarDialogoSalir = false
                onRegresarATemasDelCurso()
            },
            onCancelar = { mostrarDialogoSalir = false }
        )
    }

    if (uiState.mostrarDialogoSinVidas && uiState.quizActivo != null) {
        DialogoSinVidasDuranteQuizFigma(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = {
                android.util.Log.d("QuizScreen", "Usuario cerro dialogo con boton Cerrar")
                android.util.Log.d("QuizScreen", "Marcando quiz como abandonado")

                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            },
            onVolverATemas = {
                android.util.Log.d("QuizScreen", "Usuario presiono Volver a Temas")
                android.util.Log.d("QuizScreen", "Marcando quiz como abandonado")

                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            }
        )
    }

    val nexo = com.stiven.sos.ui.theme.LocalNexo.current
    val colorModo = when (modo) {
        "practica" -> nexo.ia
        "final" -> nexo.racha
        else -> nexo.marca
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.quizActivo == null -> {
                PantallaCargaQuizFigma(colorModo = colorModo)
            }

            uiState.finalizando -> {
                PantallaFinalizandoFigma()
            }

            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    key(uiState.preguntaActual) {
                        PreguntaScreenFigma(
                            pregunta = preguntaActual,
                            numeroPregunta = uiState.preguntaActual + 1,
                            totalPreguntas = quiz.preguntas.size,
                            temaTitulo = temaTitulo,
                            modo = modo,
                            sinVidas = uiState.sinVidas,
                            respuestasEstado = uiState.respuestasEstado,
                            ultimaRespuestaCorrecta = uiState.ultimaRespuestaCorrecta,
                            mostrarAnimacionRespuesta = uiState.mostrarAnimacionRespuesta,
                            vidasActuales = uiState.vidas?.vidasActuales ?: 5,
                            vidasMax = uiState.vidas?.vidasMax ?: 5,
                            rachaActual = uiState.progreso?.rachaDias ?: 0,
                            xpActual = uiState.progreso?.experiencia ?: 0,
                            onRespuestaSeleccionada = { opcionId ->
                                if (!uiState.sinVidas) {
                                    quizViewModel.responderPregunta(
                                        preguntaId = preguntaActual.id,
                                        respuestaSeleccionada = opcionId
                                    )
                                } else {
                                    android.util.Log.w("QuizScreen", "Intento de responder sin vidas - BLOQUEADO")
                                }
                            },
                            onAnimacionCompletada = {
                                quizViewModel.ocultarAnimacionRespuesta()
                                quizViewModel.avanzarSiguientePregunta()

                                if (uiState.preguntaActual + 1 >= quiz.preguntas.size) {
                                    quizViewModel.finalizarQuiz()
                                }
                            },
                            onBackClick = {
                                mostrarDialogoSalir = true
                            }
                        )
                    }
                }
            }
        }
    }
}


// ============================================================================
// DIÁLOGO SIN VIDAS FIGMA
// ============================================================================

@Composable
fun DialogoSinVidasDuranteQuizFigma(
    minutosParaProxima: Int,
    onDismiss: () -> Unit,
    onVolverATemas: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.HeartBroken,
        color = p.error,
        titulo = "Te quedaste sin vidas",
        mensaje = "El quiz se bloqueó porque no tienes vidas disponibles. Vuelve cuando se recarguen.",
        expresionRobi = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE,
        textoPrincipal = "Volver al mapa",
        iconoPrincipal = Icons.Default.Map,
        onPrincipal = onVolverATemas,
        textoSecundario = "Cerrar",
        onSecundario = onDismiss,
        onDismiss = {},
        cerrable = false,
        contenidoExtra = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.marca.copy(alpha = 0.12f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = p.marcaTexto, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Próxima vida en", fontSize = 13.sp, color = p.marcaTexto, fontWeight = FontWeight.Bold)
                    Text("$minutosParaProxima minutos", fontSize = 20.sp, fontWeight = FontWeight.Black, color = p.texto)
                }
            }
        }
    )
}

// ============================================================================
// DIÁLOGO ABANDONAR QUIZ FIGMA
// ============================================================================

@Composable
fun DialogoAbandonarQuizFigma(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.ExitToApp,
        color = p.racha,
        titulo = "¿Salir del quiz?",
        mensaje = "Si sales ahora perderás el progreso de este intento: no se guardan tus respuestas ni ganarás experiencia.",
        expresionRobi = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE,
        textoPrincipal = "Seguir respondiendo",
        tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.ACCION,
        onPrincipal = onCancelar,
        textoSecundario = "Salir de todas formas",
        onSecundario = onConfirmar,
        onDismiss = onCancelar
    )
}

// ============================================================================
// PANTALLAS DE CARGA Y FINALIZANDO
// ============================================================================

@Composable
fun PantallaCargaQuizFigma(colorModo: Color) {
    com.stiven.sos.ui.theme.CargandoNexo(
        titulo = "Preparando tu quiz",
        mensaje = "Robi está eligiendo tus preguntas",
        color = colorModo
    )
}

@Composable
fun PantallaFinalizandoFigma() {
    com.stiven.sos.ui.theme.CargandoNexo(
        titulo = "Finalizando quiz",
        mensaje = "Calculando tu puntuación y recompensas",
        color = com.stiven.sos.ui.theme.LocalNexo.current.accion
    )
}

// ============================================================================
// DIÁLOGOS ADICIONALES
// ============================================================================

@Composable
fun DialogoPeriodoFinalizadoFigma(
    mensajeError: String,
    temaTitulo: String,
    onAceptar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.EventBusy,
        color = p.racha,
        titulo = "Periodo finalizado",
        mensaje = "El periodo de este tema ya terminó. Ya no puedes realizar quizzes para este tema.",
        textoPrincipal = "Volver al mapa",
        onPrincipal = onAceptar,
        onDismiss = onAceptar
    )
}

@Composable
fun DialogoErrorGeneralFigma(
    titulo: String,
    mensaje: String,
    onAceptar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.Error,
        color = p.error,
        titulo = titulo.ifEmpty { "Algo salió mal" },
        mensaje = mensaje,
        textoPrincipal = "Regresar",
        tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.PELIGRO,
        onPrincipal = onAceptar,
        onDismiss = onAceptar
    )
}

@Composable
fun DialogoTemaYaAprobadoFigma(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.CheckCircle,
        color = p.accion,
        titulo = "Isla ya conquistada",
        mensaje = "Ya aprobaste este tema. Puedes seguir practicando en modo práctica.",
        expresionRobi = com.stiven.sos.ui.theme.ExpresionRobi.FELIZ,
        textoPrincipal = "Seguir practicando",
        tipoPrincipal = com.stiven.sos.ui.theme.TipoBoton.ACCION,
        onPrincipal = onContinuar,
        textoSecundario = "Regresar",
        onSecundario = onCancelar,
        onDismiss = onCancelar
    )
}

@Composable
fun DialogoQuizFinalCompletadoFigma(
    onAceptar: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.EmojiEvents,
        color = p.racha,
        titulo = "Quiz final completado",
        mensaje = "Ya completaste el quiz final de este curso.",
        expresionRobi = com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA,
        textoPrincipal = "Entendido",
        onPrincipal = onAceptar,
        onDismiss = onAceptar
    )
}