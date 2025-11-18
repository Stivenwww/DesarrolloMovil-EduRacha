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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class QuizActivity : ComponentActivity() {

    // ViewModel que maneja toda la logica de negocio del quiz
    private val quizViewModel: QuizViewModel by viewModels()

    // Variables para almacenar los parametros recibidos del Intent
    private lateinit var cursoId: String
    private lateinit var temaId: String
    private lateinit var temaTitulo: String
    private lateinit var userId: String
    private lateinit var modo: String // Valores posibles: oficial, practica, final

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar parametros del Intent enviados desde la pantalla anterior
        cursoId = intent.getStringExtra("curso_id") ?: ""
        temaId = intent.getStringExtra("tema_id") ?: ""
        temaTitulo = intent.getStringExtra("tema_titulo") ?: "Quiz"
        modo = intent.getStringExtra("modo") ?: "oficial"

        // Obtener ID del usuario desde SharedPreferences
        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        userId = prefs.getString("user_uid", "") ?: ""

        // Notificar al sistema que el quiz ha iniciado
        // Esto permite al profesor ver que estudiantes estan activos en tiempo real
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
                // Observar el estado del UI desde el ViewModel
                val uiState by quizViewModel.uiState.collectAsState()

                /**
                 * VALIDACION CRITICA 1: MONITOREO INMEDIATO DE INTERRUPCION POR VIDAS
                 *
                 * Este LaunchedEffect detecta cuando quizInterrumpidoPorVidas se activa
                 * y redirige INMEDIATAMENTE al usuario
                 * Es la primera linea de defensa contra usuarios sin vidas
                 *
                 */
                LaunchedEffect(uiState.quizInterrumpidoPorVidas) {
                    if (uiState.quizInterrumpidoPorVidas) {
                        android.util.Log.e("QuizActivity", "========================================")
                        android.util.Log.e("QuizActivity", "DETECCION CRITICA: Quiz interrumpido por vidas")
                        android.util.Log.e("QuizActivity", "ACCION: Redirigiendo en 2 segundos")
                        android.util.Log.e("QuizActivity", "========================================")

                        // Esperar 2 segundos para que el usuario vea el dialogo
                        kotlinx.coroutines.delay(2000)
                        regresarATemasDelCurso()
                    }
                }

                /**
                 * VALIDACION CRITICA 2: MONITOR ADICIONAL DE VIDAS DURANTE QUIZ ACTIVO
                 *
                 * Este segundo LaunchedEffect es una red de seguridad adicional
                 * Detecta cuando sinVidas se activa DURANTE un quiz activo
                 * y muestra el dialogo de bloqueo inmediatamente
                 */
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

                // Renderizar la pantalla principal del quiz
                QuizScreen(
                    cursoId = cursoId,
                    temaId = temaId,
                    temaTitulo = temaTitulo,
                    modo = modo,
                    quizViewModel = quizViewModel,
                    // Callback para navegar a pantalla de resultados al terminar el quiz
                    onNavigateToResultado = {
                        val resultado = quizViewModel.uiState.value.resultadoQuiz
                        val quizId = quizViewModel.uiState.value.quizActivo?.quizId

                        // Crear Intent con todos los datos del resultado
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
                    // Callback para regresar a temas del curso
                    onRegresarATemasDelCurso = {
                        regresarATemasDelCurso()
                    }
                )
            }
        }
    }

    /**
     * FUNCION HELPER: REGRESAR A TEMAS DEL CURSO
     *
     * Limpia el estado del quiz y navega de vuelta a la pantalla de temas
     * Usa flags para limpiar el stack de navegacion y evitar que el usuario
     * pueda volver atras a un quiz invalidado
     */
    private fun regresarATemasDelCurso() {
        quizViewModel.limpiarQuiz()
        val intent = Intent(this, TemasDelCursoActivity::class.java)
        intent.putExtra("curso_id", cursoId)
        // Flags para limpiar el stack y crear nueva tarea
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * VALIDACION: Detectar cuando el usuario intenta salir de la aplicacion
     * mediante el boton de inicio o recientes del sistema
     * Esto ayuda a prevenir que busquen respuestas en otras apps o navegador
     */
    override fun onPause() {
        super.onPause()

        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            // Registrar intento de salida durante el quiz
            android.util.Log.w("QuizActivity", "Usuario intento salir durante el quiz")
            // NOTA: Aqui podriamos implementar penalizacion adicional si se requiere
        }
    }

    /**
     * VALIDACION: Detectar cuando la app vuelve a primer plano
     * Podria usarse para verificar el estado de las vidas o tiempo transcurrido
     */
    override fun onResume() {
        super.onResume()

        val quizActivo = quizViewModel.uiState.value.quizActivo
        if (quizActivo != null && !quizViewModel.uiState.value.finalizando) {
            android.util.Log.d("QuizActivity", "App regreso con quiz activo")
            // NOTA: Aqui se podria verificar si las vidas cambiaron mientras estuvo en segundo plano
        }
    }
}

/**
 * CARD DE PREGUNTA
 *
 * Muestra la pregunta actual en un diseño destacado y legible
 * Incluye un badge con el numero de pregunta y separador visual
 *
 * PARAMETROS:
 * @param pregunta Objeto con los datos de la pregunta (texto, opciones, etc)
 * @param numeroPregunta Numero de la pregunta actual (1-based index)
 * @param colorModo Color distintivo segun el modo del quiz (oficial, practica, final)
 */
@Composable
fun CardPreguntaMejorada(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    colorModo: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Gradiente sutil desde el color del modo hacia blanco
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorModo.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Badge circular con numero y texto "Pregunta"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circulo con el numero de pregunta
                Surface(
                    shape = CircleShape,
                    color = colorModo.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$numeroPregunta",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorModo
                        )
                    }
                }
                Text(
                    text = "Pregunta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorModo
                )
            }

            // Separador visual
            Divider(
                color = colorModo.copy(alpha = 0.2f),
                thickness = 2.dp
            )

            // Texto de la pregunta con formato legible
            Text(
                text = pregunta.texto,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary,
                lineHeight = 28.sp
            )
        }
    }
}

/**
 * OPCION DE RESPUESTA
 *
 * Card clickeable para cada opcion de respuesta
 * Incluye animaciones de seleccion y estado visual claro
 *
 * PARAMETROS:
 * @param opcion Objeto con los datos de la opcion (texto, id)
 * @param index Indice de la opcion en la lista (usado para identificacion)
 * @param isSelected Si esta opcion esta seleccionada actualmente
 * @param colorModo Color del modo para el estado seleccionado
 * @param enabled Si la opcion esta habilitada (false cuando no hay vidas)
 * @param onClick Funcion a ejecutar al hacer click en la opcion
 */
@Composable
fun OpcionRespuestaMejorada(
    opcion: com.stiven.sos.models.OpcionQuizResponse,
    index: Int,
    isSelected: Boolean,
    colorModo: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Animacion de escala cuando se selecciona (efecto bounce)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Animacion de elevacion cuando se selecciona
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f), // Reducir opacidad si esta deshabilitado
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorModo.copy(alpha = 0.12f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 2.dp,
            color = if (isSelected) colorModo else EduRachaColors.Border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = { if (enabled) onClick() } // Solo responde al click si enabled es true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono de radio button o check
            Surface(
                shape = CircleShape,
                color = if (isSelected) colorModo.copy(alpha = 0.2f) else EduRachaColors.Border.copy(alpha = 0.3f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        // Mostrar check si esta seleccionada
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorModo,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        // Mostrar circulo vacio si no esta seleccionada
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Texto de la opcion
            Text(
                text = opcion.texto,
                fontSize = 17.sp,
                color = if (isSelected) colorModo else EduRachaColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                lineHeight = 25.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * BOTON DE CONFIRMACION
 *
 * Boton para confirmar la respuesta seleccionada y avanzar a la siguiente pregunta
 * Se deshabilita automaticamente cuando no hay respuesta seleccionada o no hay vidas
 *
 * PARAMETROS:
 * @param enabled Si el boton esta habilitado
 * @param colorModo Color del boton
 * @param esUltimaPregunta Cambia el texto a "Finalizar Quiz" si es true
 * @param onClick Accion al hacer click
 * @param modifier Modificadores adicionales opcionales
 */
@Composable
fun BotonConfirmarMejorado(
    enabled: Boolean,
    colorModo: Color,
    esUltimaPregunta: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorModo,
            disabledContainerColor = EduRachaColors.TextSecondary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Texto dinamico segun si es la ultima pregunta
            Text(
                text = if (esUltimaPregunta) "Finalizar Quiz" else "Siguiente",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            // Icono dinamico segun si es la ultima pregunta
            Icon(
                if (esUltimaPregunta) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * DIALOGO DE CONFIRMACION PARA ABANDONAR QUIZ
 *
 * Se muestra cuando el usuario intenta salir durante el quiz
 * Advierte sobre la perdida de vida y progreso
 * Implementa politica de integridad academica
 *
 * PARAMETROS:
 * @param onConfirmar Funcion a ejecutar si confirma el abandono
 * @param onCancelar Funcion a ejecutar si cancela y continua el quiz
 */
@Composable
fun DialogoAbandonarQuizConBloqueo(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de candado en circulo rojo
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = EduRachaColors.Error.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = EduRachaColors.Error,
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Salida bloqueada durante el quiz",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 30.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Explicacion de la politica
                Text(
                    "Por politica de integridad academica, no puedes salir de la aplicacion mientras resuelves un quiz.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Card con advertencias
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EduRachaColors.Warning.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = EduRachaColors.Warning,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Si decides abandonar:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Perderas 1 vida",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Perderas tu progreso actual",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Mensaje motivacional
                Text(
                    "Te recomendamos continuar y dar lo mejor de ti",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EduRachaColors.Primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        },
        // Boton para confirmar abandono (rojo)
        confirmButton = {
            Button(
                onClick = onConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Abandonar de todas formas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        // Boton para continuar quiz (azul)
        dismissButton = {
            Button(
                onClick = onCancelar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Primary
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Continuar quiz",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

/**
 * PANTALLA DE PREGUNTA CON VALIDACIONES
 *
 * Pantalla que muestra una pregunta individual con todas sus opciones
 * Incluye validaciones de vidas y overlay de bloqueo
 *
 * MEJORAS IMPLEMENTADAS:
 * 1. Deteccion inmediata cuando sinVidas cambia a true
 * 2. Cancelacion automatica de animacion si se queda sin vidas
 * 3. Overlay visual de bloqueo cuando no hay vidas
 * 4. Bloqueo de todas las interacciones sin vidas
 * 5. Limpieza automatica de respuesta seleccionada
 *
 * PARAMETROS:
 * @param pregunta Objeto con los datos de la pregunta actual
 * @param numeroPregunta Numero de la pregunta actual (1-based)
 * @param totalPreguntas Total de preguntas en el quiz
 * @param colorModo Color segun el modo del quiz
 * @param temaTitulo Titulo del tema para mostrar en header
 * @param modo Modo del quiz (oficial, practica, final)
 * @param tiempoTotalQuiz Tiempo transcurrido en segundos
 * @param sinVidas Flag que indica si el usuario no tiene vidas
 * @param onRespuestaSeleccionada Callback al seleccionar y confirmar una respuesta
 */
@Composable
fun PreguntaScreen(
    pregunta: com.stiven.sos.models.PreguntaQuizResponse,
    numeroPregunta: Int,
    totalPreguntas: Int,
    colorModo: Color,
    temaTitulo: String,
    modo: String,
    tiempoTotalQuiz: Int,
    sinVidas: Boolean,
    onRespuestaSeleccionada: (Int) -> Unit
) {
    // Estado local para la respuesta seleccionada
    // Se reinicia cada vez que cambia la pregunta (key en remember)
    var respuestaSeleccionada by remember(pregunta.id) { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    var mostrarAnimacionEstrella by remember(pregunta.id) { mutableStateOf(false) }

    /**
     * VALIDACION CRITICA: Deteccion inmediata de perdida de vidas
     *
     * Este LaunchedEffect se ejecuta INMEDIATAMENTE cuando sinVidas cambia
     * Cancela cualquier animacion en curso y limpia la seleccion
     */
    LaunchedEffect(sinVidas) {
        if (sinVidas) {
            android.util.Log.w("PreguntaScreen", "Deteccion: Usuario sin vidas")
            android.util.Log.w("PreguntaScreen", "Cancelando animacion y limpiando seleccion")
            mostrarAnimacionEstrella = false
            respuestaSeleccionada = null
        }
    }

    /**
     * Contenedor principal con overlay de bloqueo cuando no hay vidas
     */
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorModo.copy(alpha = 0.08f),
                            EduRachaColors.Background,
                            Color.White
                        )
                    )
                )
        ) {
            // Header con titulo, modo y temporizador
            HeaderQuizMejorado(
                temaTitulo = temaTitulo,
                modo = modo,
                colorModo = colorModo,
                tiempoTranscurrido = tiempoTotalQuiz
            )

            // Barra de progreso con estrellas
            BarraProgresoConEstrellasIluminadas(
                preguntaActual = numeroPregunta,
                totalPreguntas = totalPreguntas,
                colorModo = colorModo,
                respuestaSeleccionada = respuestaSeleccionada != null
            )

            // Contenido scrolleable con pregunta y opciones
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Card con la pregunta
                CardPreguntaMejorada(
                    pregunta = pregunta,
                    numeroPregunta = numeroPregunta,
                    colorModo = colorModo
                )

                /**
                 * Opciones de respuesta con bloqueo cuando no hay vidas
                 * El parametro enabled controla si la opcion es clickeable
                 */
                pregunta.opciones.forEachIndexed { index, opcion ->
                    OpcionRespuestaMejorada(
                        opcion = opcion,
                        index = index,
                        isSelected = respuestaSeleccionada == index,
                        colorModo = colorModo,
                        enabled = !sinVidas, // BLOQUEAR si no hay vidas
                        onClick = {
                            if (!sinVidas) {
                                respuestaSeleccionada = index
                            } else {
                                android.util.Log.w("PreguntaScreen", "Intento bloqueado: Sin vidas")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))
            }

            // Boton de confirmacion (tambien bloqueado sin vidas)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                BotonConfirmarMejorado(
                    enabled = respuestaSeleccionada != null && !sinVidas, // BLOQUEAR sin vidas
                    colorModo = colorModo,
                    esUltimaPregunta = numeroPregunta >= totalPreguntas,
                    onClick = {
                        // Doble verificacion antes de iniciar animacion
                        if (!sinVidas) {
                            respuestaSeleccionada?.let {
                                mostrarAnimacionEstrella = true
                            }
                        } else {
                            android.util.Log.w("PreguntaScreen", "Click bloqueado: Sin vidas")
                        }
                    },
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        /**
         * OVERLAY DE BLOQUEO VISUAL
         *
         * Se muestra sobre toda la pantalla cuando no hay vidas
         * Bloquea todas las interacciones y muestra mensaje claro
         */
        if (sinVidas) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { }, // Bloquear todos los clics
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Quiz Bloqueado",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF4B4B)
                        )
                        Text(
                            "Te has quedado sin vidas",
                            fontSize = 16.sp,
                            color = EduRachaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    /**
     * Animacion de estrella exitosa
     * Solo se muestra si tiene vidas disponibles
     * Duracion: 500ms para mayor fluidez
     */
    if (mostrarAnimacionEstrella && !sinVidas) {
        AnimacionEstrellaExitosa(
            colorModo = colorModo,
            onAnimacionCompleta = {
                // Verificar vidas antes de procesar la respuesta
                if (!sinVidas) {
                    mostrarAnimacionEstrella = false
                    respuestaSeleccionada?.let { opcionSeleccionada ->
                        onRespuestaSeleccionada(opcionSeleccionada)
                    }
                } else {
                    // Si perdio vidas durante la animacion, cancelar
                    mostrarAnimacionEstrella = false
                    android.util.Log.w("PreguntaScreen", "Animacion cancelada: Vidas agotadas")
                }
            }
        )
    }
}

/**
 * ANIMACION DE ESTRELLA EXITOSA
 *
 * Se muestra cuando el usuario confirma una respuesta
 * Incluye efectos de escala, rotacion y fade out
 * Duracion total: 500ms (reducida para mejor experiencia)
 *
 * PARAMETROS:
 * @param colorModo Color del efecto visual (segun modo del quiz)
 * @param onAnimacionCompleta Callback al terminar la animacion
 */
@Composable
fun AnimacionEstrellaExitosa(
    colorModo: Color,
    onAnimacionCompleta: () -> Unit
) {
    var animacionIniciada by remember { mutableStateOf(false) }

    // Animacion de escala con efecto bounce
    val escalaEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "escalaEstrella"
    )

    // Animacion de rotacion completa (360 grados)
    val rotacionEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 360f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "rotacionEstrella"
    )

    // Animacion de fade out al final
    val alphaEstrella by animateFloatAsState(
        targetValue = if (animacionIniciada) 0f else 1f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 400,
            easing = LinearEasing
        ),
        label = "alphaEstrella"
    )

    /**
     * CAMBIO CRITICO: Duracion reducida de 1000ms a 500ms
     * Mejora la experiencia y evita que el usuario se impaciente
     */
    LaunchedEffect(Unit) {
        animacionIniciada = true
        delay(500) // Duracion total: 500ms
        onAnimacionCompleta()
    }

    // Overlay oscuro con la estrella animada
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.3f * alphaEstrella)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Particulas brillantes alrededor de la estrella
            ParticulasBrillantes(
                visible = animacionIniciada,
                color = colorModo
            )

            // Estrella principal dorada
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .size(200.dp)
                    .scale(escalaEstrella)
                    .rotate(rotacionEstrella)
                    .alpha(alphaEstrella)
            )

            // Resplandor radial de fondo
            Canvas(
                modifier = Modifier
                    .size(250.dp)
                    .alpha(alphaEstrella * 0.6f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.6f),
                            Color(0xFFFFD700).copy(alpha = 0f)
                        )
                    ),
                    radius = size.minDimension / 2 * escalaEstrella
                )
            }
        }
    }
}

/**
 * PARTICULAS BRILLANTES
 *
 * Particulas que rodean la estrella principal y se expanden hacia afuera
 * con efecto bounce
 *
 * PARAMETROS:
 * @param visible Controla si las particulas estan visibles
 * @param color Color de las particulas (no usado actualmente, siempre dorado)
 */
@Composable
fun ParticulasBrillantes(
    visible: Boolean,
    color: Color
) {
    val numeroParticulas = 12

    // Crear 12 particulas distribuidas en circulo (360 grados / 12 = 30 grados cada una)
    for (i in 0 until numeroParticulas) {
        val angulo = (360f / numeroParticulas) * i

        // Animacion de desplazamiento hacia afuera
        val offset by animateFloatAsState(
            targetValue = if (visible) 120f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offset_$i"
        )

        // Animacion de escala
        val escala by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "escala_$i"
        )

        // Animacion de fade out
        val alpha by animateFloatAsState(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 300,
                easing = LinearEasing
            ),
            label = "alpha_$i"
        )

        // Calcular posicion de la particula usando trigonometria
        val offsetX = cos(Math.toRadians(angulo.toDouble())).toFloat() * offset
        val offsetY = sin(Math.toRadians(angulo.toDouble())).toFloat() * offset

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .fillMaxSize()
                    .scale(escala)
                    .alpha(alpha)
            )
        }
    }
}

/**
 * FUNCION HELPER: Formatear tiempo
 *
 * Convierte segundos a formato legible "M:SS"
 *
 * PARAMETROS:
 * @param segundos Tiempo en segundos
 *
 * RETORNA: String formateado como "M:SS" (ejemplo: "5:03", "12:45")
 */
fun formatearTiempo(segundos: Int): String {
    val minutos = segundos / 60
    val segs = segundos % 60
    return String.format("%d:%02d", minutos, segs)
}

/**
 * HEADER DEL QUIZ
 *
 * Muestra informacion clave en la parte superior:
 * - Titulo del tema
 * - Modo del quiz (oficial, practica, final)
 * - Temporizador con animacion pulsante
 *
 * PARAMETROS:
 * @param temaTitulo Titulo del tema actual
 * @param modo Modo del quiz (oficial, practica, final)
 * @param colorModo Color distintivo del modo
 * @param tiempoTranscurrido Tiempo en segundos
 */
@Composable
fun HeaderQuizMejorado(
    temaTitulo: String,
    modo: String,
    colorModo: Color,
    tiempoTranscurrido: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna izquierda: Titulo y badge de modo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = temaTitulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EduRachaColors.TextPrimary,
                    maxLines = 1
                )

                // Badge con el modo del quiz
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorModo.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (modo) {
                            "practica" -> "Modo Practica"
                            "final" -> "Quiz Final"
                            else -> "Modo Oficial"
                        },
                        color = colorModo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Columna derecha: Temporizador con icono pulsante
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconoRelojPulsante()

                Text(
                    text = formatearTiempo(tiempoTranscurrido),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

/**
 * ICONO DE RELOJ PULSANTE
 *
 * Animacion continua de pulso para el icono del temporizador
 * Ayuda a mantener la atencion del usuario en el tiempo
 */
@Composable
fun IconoRelojPulsante() {
    val infiniteTransition = rememberInfiniteTransition(label = "reloj")

    // Animacion de escala con repeticion infinita
    val escala by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    Icon(
        Icons.Default.Timer,
        contentDescription = null,
        tint = Color(0xFF4CAF50),
        modifier = Modifier
            .size(24.dp)
            .scale(escala)
    )
}

/**
 * BARRA DE PROGRESO CON ESTRELLAS ILUMINADAS
 *
 * Muestra el progreso del quiz de forma visual con:
 * - Barra de progreso animada con gradiente
 * - Estrellas que se iluminan al responder cada pregunta
 *
 * IMPORTANTE: Las estrellas NO indican si la respuesta es correcta o incorrecta,
 * solo muestran cuantas preguntas se han respondido
 *
 * PARAMETROS:
 * @param preguntaActual Numero de pregunta actual (1-based)
 * @param totalPreguntas Total de preguntas en el quiz
 * @param colorModo Color del modo para la barra y estrellas
 * @param respuestaSeleccionada Si hay respuesta seleccionada en pregunta actual
 */
@Composable
fun BarraProgresoConEstrellasIluminadas(
    preguntaActual: Int,
    totalPreguntas: Int,
    colorModo: Color,
    respuestaSeleccionada: Boolean
) {
    // Calcular progreso como fraccion (0.0 a 1.0)
    val progreso = (preguntaActual.toFloat() / totalPreguntas.toFloat())

    // Animacion suave del progreso con efecto bounce
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progreso"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra de progreso con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorModo.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progresoAnimado)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorModo,
                                    colorModo.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // Fila de estrellas (una por pregunta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 1..totalPreguntas) {
                    val estrellaCompletada = i <= preguntaActual
                    EstrellaIluminada(
                        completada = estrellaCompletada,
                        estaActiva = i == preguntaActual && respuestaSeleccionada
                    )
                }
            }
        }
    }
}

/**
 * ESTRELLA ILUMINADA
 *
 * Representa una pregunta en la barra de progreso
 * Se ilumina cuando la pregunta esta completada (respondida)
 *
 * NOTA IMPORTANTE: La estrella NO indica si la respuesta es correcta,
 * solo indica que la pregunta fue respondida
 *
 * PARAMETROS:
 * @param completada Si la estrella debe estar iluminada (pregunta respondida)
 * @param estaActiva Si es la estrella activa actual (con brillo pulsante)
 */
@Composable
fun EstrellaIluminada(
    completada: Boolean,
    estaActiva: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "estrella")

    // Animacion de brillo pulsante solo para estrellas activas
    val brillo by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brillo"
    )

    // Animacion de escala cuando se completa
    val escala by animateFloatAsState(
        targetValue = if (completada) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escala"
    )

    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo de brillo para estrellas activas
        if (completada && estaActiva) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * brillo
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }

        // Icono de estrella (llena si completada, contorno si no)
        Icon(
            if (completada) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = if (completada) Color(0xFFFFD700) else EduRachaColors.TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier
                .size(24.dp)
                .scale(escala)
        )
    }
}

/**
 * PANTALLA PRINCIPAL DEL QUIZ
 *
 * Coordina el flujo completo del quiz y maneja todos los dialogos de validacion
 * Es el componente central que orquesta toda la experiencia del quiz
 *
 * FLUJO:
 * 1. Muestra dialogo informativo sobre estrellas (SIEMPRE al inicio)
 * 2. Inicializa el quiz y observadores
 * 3. Muestra preguntas una por una
 * 4. Valida vidas en tiempo real
 * 5. Finaliza y navega a resultados
 *
 * PARAMETROS:
 * @param cursoId ID del curso actual
 * @param temaId ID del tema actual
 * @param temaTitulo Titulo del tema para mostrar en UI
 * @param modo Modo del quiz (oficial, practica, final)
 * @param quizViewModel ViewModel que maneja el estado y logica de negocio
 * @param onNavigateToResultado Callback para navegar a resultados
 * @param onRegresarATemasDelCurso Callback para regresar a temas
 */
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
    // Observar el estado del ViewModel
    val uiState by quizViewModel.uiState.collectAsState()

    // Estados locales para controlar navegacion y dialogos
    var yaNavego by remember { mutableStateOf(false) }
    var yaCargoRetroalimentacion by remember { mutableStateOf(false) }
    var mostrarDialogoSalir by remember { mutableStateOf(false) }


    var mostrarDialogoInfoEstrellas by remember { mutableStateOf(true) }

    // Temporizador global del quiz
    var tiempoTotalQuiz by remember { mutableStateOf(0) }

    /**
     * INICIALIZACION: Solo inicia el quiz despues de cerrar el dialogo de estrellas
     *
     * Este LaunchedEffect se ejecuta cuando mostrarDialogoInfoEstrellas cambia a false
     * Es decir, solo cuando el usuario acepta el dialogo informativo
     */
    LaunchedEffect(mostrarDialogoInfoEstrellas) {
        if (!mostrarDialogoInfoEstrellas) {
            // Solo iniciar el quiz despues de cerrar el dialogo
            quizViewModel.iniciarObservadores(cursoId, temaId)
            quizViewModel.iniciarQuiz(cursoId, temaId, modo)
        }
    }

    /**
     * TEMPORIZADOR: Incrementa cada segundo mientras el quiz este activo
     */
    LaunchedEffect(uiState.quizActivo, uiState.finalizando) {
        if (uiState.quizActivo != null && !uiState.finalizando) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tiempoTotalQuiz++
            }
        }
    }

    /**
     * VALIDACION CRITICA 1: Redireccion inmediata por vidas agotadas
     *
     * para dar tiempo suficiente al usuario de ver el dialogo de sin vidas
     */
    LaunchedEffect(uiState.quizInterrumpidoPorVidas) {
        if (uiState.quizInterrumpidoPorVidas) {
            android.util.Log.e("QuizScreen", "Quiz interrumpido por falta de vidas")
            android.util.Log.e("QuizScreen", "Redirigiendo en 2 segundos")

            // Delay de 2 segundos para que el dialogo sea visible
            kotlinx.coroutines.delay(2000)
            quizViewModel.limpiarQuiz()
            onRegresarATemasDelCurso()
        }
    }

    /**
     * VALIDACION CRITICA 2: Monitor de vidas durante quiz activo
     */
    LaunchedEffect(uiState.sinVidas, uiState.quizActivo) {
        if (uiState.sinVidas &&
            uiState.quizActivo != null &&
            !uiState.finalizando &&
            !uiState.quizInterrumpidoPorVidas) {

            android.util.Log.w("QuizScreen", "Usuario sin vidas durante quiz")
            android.util.Log.w("QuizScreen", "Modo: $modo")

            quizViewModel.mostrarDialogoSinVidas()
        }
    }

    /**
     * BACK HANDLER: Interceptar boton de retroceso del sistema
     * Muestra dialogo de confirmacion si hay quiz activo
     */
    BackHandler(enabled = true) {
        if (uiState.finalizando) {
            android.util.Log.w("QuizScreen", "Bloqueada salida durante finalizacion")
        } else if (uiState.quizActivo != null) {
            mostrarDialogoSalir = true
        } else {
            onRegresarATemasDelCurso()
        }
    }

    /**
     * NAVEGACION A RESULTADOS: Cuando el quiz termina
     */
    LaunchedEffect(uiState.resultadoQuiz) {
        if (uiState.resultadoQuiz != null && !yaCargoRetroalimentacion) {
            yaCargoRetroalimentacion = true
            val quizId = uiState.quizActivo?.quizId

            // Cargar retroalimentacion si hubo errores
            if (quizId != null && (uiState.resultadoQuiz?.preguntasIncorrectas ?: 0) > 0) {
                quizViewModel.obtenerRetroalimentacion(quizId)
                kotlinx.coroutines.delay(500)
            }

            // Navegar a pantalla de resultados
            if (!yaNavego) {
                yaNavego = true
                onNavigateToResultado()
            }
        }
    }

    /**
     * DIALOGOS DE VALIDACION
     * Cada dialogo maneja un caso especifico de error o validacion
     */

    // Dialogo: Periodo finalizado
    if (uiState.mostrarDialogoPeriodoFinalizado) {
        DialogoPeriodoFinalizado(
            mensajeError = uiState.mensajeErrorDetallado,
            temaTitulo = temaTitulo,
            onAceptar = {
                quizViewModel.cerrarDialogoPeriodoFinalizado()
                onRegresarATemasDelCurso()
            }
        )
    }

    // Dialogo: Error general
    if (uiState.mostrarDialogoErrorGeneral) {
        DialogoErrorGeneral(
            titulo = uiState.tituloError,
            mensaje = uiState.mensajeErrorDetallado,
            onAceptar = {
                quizViewModel.cerrarDialogoErrorGeneral()
                onRegresarATemasDelCurso()
            }
        )
    }

    /**
     * DIALOGO DE ESTRELLAS - SIEMPRE se muestra al inicio antes de cargar el quiz
     *
     * IMPORTANTE: Este dialogo explica que las estrellas solo indican progreso,
     * NO si las respuestas son correctas o incorrectas
     */
    if (mostrarDialogoInfoEstrellas) {
        DialogoInfoEstrellas(
            onAceptar = {
                mostrarDialogoInfoEstrellas = false
                // El LaunchedEffect de arriba detectara este cambio e iniciara el quiz
            }
        )
    }

    // Dialogo: Tema ya aprobado (ofrece modo practica)
    if (uiState.mostrarDialogoTemaAprobado) {
        DialogoTemaYaAprobado(
            onContinuar = {
                quizViewModel.forzarInicioQuiz(cursoId, temaId, "practica")
            },
            onCancelar = {
                quizViewModel.cerrarDialogoTemaAprobado()
                onRegresarATemasDelCurso()
            }
        )
    }

    // Dialogo: Quiz Final ya completado
    if (uiState.mostrarDialogoQuizFinalCompletado) {
        DialogoQuizFinalCompletado(
            onAceptar = {
                quizViewModel.cerrarDialogoQuizFinalCompletado()
                onRegresarATemasDelCurso()
            }
        )
    }

    // Dialogo: Confirmar abandono durante quiz
    if (mostrarDialogoSalir) {
        DialogoAbandonarQuizConBloqueo(
            onConfirmar = {
                mostrarDialogoSalir = false
                onRegresarATemasDelCurso()
            },
            onCancelar = { mostrarDialogoSalir = false }
        )
    }

    /**
     * DIALOGO CRITICO: SIN VIDAS DURANTE EL QUIZ - VERSION MEJORADA
     *
     * MEJORAS:
     * 1. Ahora incluye boton para volver a temas del curso
     * 2. Permanece visible por mas tiempo (2 segundos antes de redirigir)
     * 3. Muestra claramente el tiempo para la proxima vida
     */
    if (uiState.mostrarDialogoSinVidas && uiState.quizActivo != null) {
        DialogoSinVidasDuranteQuizMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = {
                android.util.Log.d("QuizScreen", "Usuario cerro dialogo de sin vidas")
                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            },
            onVolverATemas = {
                android.util.Log.d("QuizScreen", "Usuario presiono volver a temas")
                quizViewModel.cerrarDialogoSinVidas()
                quizViewModel.limpiarQuiz()
                onRegresarATemasDelCurso()
            }
        )
    }

    /**
     * Determinar color segun el modo del quiz
     */
    val colorModo = when (modo) {
        "practica" -> Color(0xFF9C27B0) // Morado
        "final" -> Color(0xFFFFB300)    // Amarillo/Dorado
        else -> EduRachaColors.Primary  // Azul (oficial)
    }

    /**
     * CONTENEDOR PRINCIPAL: Muestra la pantalla segun el estado actual
     */
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Pantalla de carga solo si el dialogo de estrellas esta cerrado
            !mostrarDialogoInfoEstrellas && uiState.isLoading && uiState.quizActivo == null -> {
                PantallaCargaQuiz(colorModo = colorModo)
            }

            // Pantalla de finalizacion
            uiState.finalizando -> {
                PantallaFinalizando()
            }

            // Pantalla principal con preguntas
            uiState.quizActivo != null -> {
                val quiz = uiState.quizActivo!!
                val preguntaActual = quiz.preguntas.getOrNull(uiState.preguntaActual)

                if (preguntaActual != null) {
                    // Key fuerza recomposicion al cambiar de pregunta
                    key(uiState.preguntaActual) {
                        PreguntaScreen(
                            pregunta = preguntaActual,
                            numeroPregunta = uiState.preguntaActual + 1,
                            totalPreguntas = quiz.preguntas.size,
                            colorModo = colorModo,
                            temaTitulo = temaTitulo,
                            modo = modo,
                            tiempoTotalQuiz = tiempoTotalQuiz,
                            sinVidas = uiState.sinVidas,
                            onRespuestaSeleccionada = { opcionId ->
                                /**
                                 * VALIDACION CRITICA: NO PERMITIR RESPONDER SIN VIDAS
                                 * Aplica a TODOS los modos
                                 * Doble verificacion antes de procesar respuesta
                                 */
                                if (!uiState.sinVidas) {
                                    quizViewModel.responderPregunta(
                                        preguntaId = preguntaActual.id,
                                        respuestaSeleccionada = opcionId
                                    )

                                    // Si es la ultima pregunta, finalizar
                                    if (uiState.preguntaActual + 1 >= quiz.preguntas.size) {
                                        quizViewModel.finalizarQuiz()
                                    }
                                } else {
                                    android.util.Log.w("QuizScreen", "========================================")
                                    android.util.Log.w("QuizScreen", "Intento de responder sin vidas - BLOQUEADO")
                                    android.util.Log.w("QuizScreen", "Modo: $modo")
                                    android.util.Log.w("QuizScreen", "========================================")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * DIALOGO MEJORADO: Sin vidas durante el quiz
 *
 * VERSION MEJORADA con las siguientes mejoras:
 * 1. Incluye boton principal para "Volver a Temas del Curso"
 * 2. Incluye boton secundario para "Cerrar"
 * 3. Permanece visible por 2 segundos antes de redirigir automaticamente
 * 4. Muestra claramente el tiempo para la proxima vida
 * 5. Diseño mas amigable y claro
 *
 * PARAMETROS:
 * @param minutosParaProxima Minutos hasta regenerar la proxima vida
 * @param onDismiss Callback al cerrar el dialogo con el boton secundario
 * @param onVolverATemas Callback al presionar el boton principal de volver a temas
 */
@Composable
fun DialogoSinVidasDuranteQuizMejorado(
    minutosParaProxima: Int,
    onDismiss: () -> Unit,
    onVolverATemas: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de corazon roto en circulo rojo
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.HeartBroken,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Te quedaste sin vidas",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFFF4B4B),
                lineHeight = 32.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mensaje explicativo
                Text(
                    "El quiz ha sido bloqueado porque te has quedado sin vidas disponibles",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Card con temporizador
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Proxima vida en:",
                                fontSize = 14.sp,
                                color = Color(0xFF1CB0F6),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$minutosParaProxima minutos",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1CB0F6)
                            )
                        }
                    }
                }

                // Mensaje final
                Text(
                    "Vuelve cuando tengas vidas disponibles para continuar aprendiendo",
                    fontSize = 15.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Boton principal: Volver a Temas (azul)
                Button(
                    onClick = onVolverATemas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Volver a Temas del Curso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Boton secundario: Cerrar (outlined)
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Color(0xFFFF4B4B)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "Cerrar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4B4B)
                    )
                }
            }
        }
    )
}

/**
 * PANTALLA DE CARGA DEL QUIZ
 *
 * Se muestra mientras se preparan las preguntas del quiz
 * Incluye animacion circular con icono de escuela
 *
 * PARAMETROS:
 * @param colorModo Color segun el modo del quiz (oficial, practica, final)
 */
@Composable
fun PantallaCargaQuiz(colorModo: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorModo.copy(alpha = 0.15f),
                        EduRachaColors.Background,
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animacion de carga
            LoadingAnimation(colorModo = colorModo)

            Text(
                text = "Preparando tu quiz...",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cargando las mejores preguntas para ti",
                fontSize = 16.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * ANIMACION DE CARGA
 *
 * Circulo giratorio con arco animado y icono pulsante
 *
 * PARAMETROS:
 * @param colorModo Color del arco y el icono central
 */
@Composable
fun LoadingAnimation(colorModo: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Rotacion continua de 360 grados
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Escala pulsante para el icono
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Canvas para dibujar circulo y arco
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            // Circulo de fondo semitransparente
            drawCircle(
                color = colorModo.copy(alpha = 0.2f),
                radius = radius,
                center = Offset(centerX, centerY)
            )

            // Arco animado giratorio (270 grados)
            drawArc(
                color = colorModo,
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                size = size
            )
        }

        // Icono de escuela en el centro con escala pulsante
        Icon(
            Icons.Default.School,
            contentDescription = null,
            tint = colorModo,
            modifier = Modifier
                .size(50.dp)
                .scale(scale)
        )
    }
}

/**
 * PANTALLA DE FINALIZACION
 *
 * Se muestra durante la finalizacion del quiz mientras se calculan resultados
 * Incluye animacion de exito con circulos pulsantes
 */
@Composable
fun PantallaFinalizando() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Success.copy(alpha = 0.15f),
                        EduRachaColors.Background,
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animacion de finalizacion
            FinalizandoAnimation()

            Text(
                text = "Finalizando quiz...",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EduRachaColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Calculando tu puntuacion y recompensas",
                fontSize = 17.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * ANIMACION DE FINALIZACION
 *
 * Circulos concentricos pulsantes con icono de check
 * Indica que el proceso de finalizacion esta en curso
 */
@Composable
fun FinalizandoAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "finalizando")

    // Animacion de escala para el circulo exterior
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Circulo exterior pulsante
        Surface(
            shape = CircleShape,
            color = EduRachaColors.Success.copy(alpha = 0.2f),
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
        ) {}

        // Circulo medio fijo
        Surface(
            shape = CircleShape,
            color = EduRachaColors.Success.copy(alpha = 0.3f),
            modifier = Modifier.size(100.dp)
        ) {}

        // Icono de check en el centro
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EduRachaColors.Success,
            modifier = Modifier.size(60.dp)
        )
    }
}

/**
 * DIALOGO: Periodo finalizado
 *
 * Informa al usuario que el periodo del tema ha terminado
 * y no puede realizar mas quizzes para este tema
 *
 * PARAMETROS:
 * @param mensajeError Mensaje de error del servidor
 * @param temaTitulo Titulo del tema
 * @param onAceptar Callback al aceptar (regresa a temas)
 */
@Composable
fun DialogoPeriodoFinalizado(
    mensajeError: String,
    temaTitulo: String,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de calendario cancelado
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Periodo Finalizado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 32.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "El periodo de este tema ya finalizo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                // Card con informacion del tema
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    "Tema:",
                                    fontSize = 14.sp,
                                    color = EduRachaColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    temaTitulo,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFFF9800).copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Explicacion
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(28.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Que significa esto?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Este tema tiene un periodo de disponibilidad que ya ha terminado",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    "Ya no puedes realizar quizzes para este tema",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                // Sugerencia
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1CB0F6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF1CB0F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Consulta con tu profesor sobre otros temas disponibles",
                            fontSize = 14.sp,
                            color = Color(0xFF1CB0F6),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Mensaje de error tecnico (si existe)
                if (mensajeError.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = EduRachaColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = mensajeError.replace("{\"error\":\"", "")
                                    .replace("\"}", "")
                                    .replace("\\", ""),
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Volver a Temas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

/**
 * DIALOGO: Error general
 *
 * Para otros tipos de errores que no son periodo finalizado
 *
 * PARAMETROS:
 * @param titulo Titulo del error
 * @param mensaje Descripcion del error
 * @param onAceptar Callback al aceptar
 */
@Composable
fun DialogoErrorGeneral(
    titulo: String,
    mensaje: String,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de error
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF4B4B),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = titulo.ifEmpty { "Error" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 30.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF4B4B).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Limpiar mensaje de error de formato JSON
                        Text(
                            text = mensaje.replace("{\"error\":\"", "")
                                .replace("\"}", "")
                                .replace("\\", ""),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = EduRachaColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4B4B)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Regresar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

/**
 * DIALOGO: Tema ya aprobado
 *
 * Aparece cuando el usuario intenta hacer un quiz de un tema que ya aprobo
 * Ofrece la opcion de continuar en modo practica para repasar
 *
 * PARAMETROS:
 * @param onContinuar Callback para continuar en modo practica
 * @param onCancelar Callback para regresar a temas
 */
@Composable
fun DialogoTemaYaAprobado(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de check verde
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Tema ya aprobado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 30.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has aprobado este tema con exito",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Card con beneficios del modo practica
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Si deseas seguir practicando:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Podras hacer el quiz en modo practica",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "No perderas vidas",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Seguiras ganando experiencia",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        // Boton para continuar practicando
        confirmButton = {
            Button(
                onClick = onContinuar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Seguir practicando",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        // Boton para regresar
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    "Regresar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    )
}

/**
 * DIALOGO: Quiz Final ya completado
 *
 * Informa al usuario que ya aprobo el curso con el Quiz Final
 * y no puede volver a hacerlo
 *
 * PARAMETROS:
 * @param onAceptar Callback al aceptar (regresa a temas)
 */
@Composable
fun DialogoQuizFinalCompletado(
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Icono de trofeo dorado
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFB300).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Quiz Final Completado",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 30.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ya has completado exitosamente el Quiz Final de este curso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Card informativo
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Felicidades por tu logro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.TextPrimary
                            )
                        }

                        Text(
                            "El Quiz Final solo puede realizarse una vez por curso. Si deseas mejorar tu conocimiento, puedes practicar en los temas individuales.",
                            fontSize = 15.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Entendido",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}

/**
 * DIALOGO INFORMATIVO: Explicacion sobre las estrellas
 *
 * Se muestra al inicio de CUALQUIER quiz para aclarar el significado de las estrellas
 *
 * IMPORTANTE: Este dialogo es CRITICO para evitar confusion del usuario
 * Las estrellas NO indican si la respuesta es correcta o incorrecta
 * Solo indican cuantas preguntas se han respondido (progreso)
 *
 * PARAMETROS:
 * @param onAceptar Callback al aceptar y comenzar el quiz
 */
@Composable
fun DialogoInfoEstrellas(
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAceptar,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            // Estrella dorada con animacion pulsante
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "estrella_info")
                val escala by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "escala"
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(escala)
                ) {}

                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(70.dp)
                )
            }
        },
        title = {
            Text(
                text = "Informacion Importante",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                color = EduRachaColors.TextPrimary,
                lineHeight = 32.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card principal con la explicacion
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Seccion: Que significan las estrellas
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(32.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Que significan las estrellas?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EduRachaColors.TextPrimary
                                )
                                Text(
                                    "Las estrellas solo indican la cantidad de preguntas que has respondido",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EduRachaColors.TextPrimary,
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFFFD700).copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Seccion: Importante
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1CB0F6),
                                modifier = Modifier.size(32.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Importante:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1CB0F6)
                                )

                                // Punto 1: No indican si es correcto o incorrecto
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = 20.sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Las estrellas NO indican si tu respuesta es correcta o incorrecta",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = 22.sp
                                    )
                                }

                                // Punto 2: Solo muestran progreso
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = 20.sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Solo muestran tu progreso en el quiz",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = 22.sp
                                    )
                                }

                                // Punto 3: Veras resultados al final
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("*", fontSize = 20.sp, color = Color(0xFF1CB0F6))
                                    Text(
                                        "Veras tus respuestas correctas e incorrectas al finalizar el quiz",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EduRachaColors.TextPrimary,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Mensaje motivacional final
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Confia en tus conocimientos y da lo mejor de ti",
                            fontSize = 15.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Boton para comenzar el quiz
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        "Entendido, comenzar quiz",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    )
}