package com.stiven.sos

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.FirebaseDatabase
import com.stiven.sos.models.Tema
import com.stiven.sos.services.DialogoSinVidasMejorado
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.ui.theme.EstadoNodo
import com.stiven.sos.ui.theme.GloboFlotante
import com.stiven.sos.ui.theme.NodoRuta
import com.stiven.sos.ui.theme.PlayColors
import com.stiven.sos.ui.theme.TarjetaRachi
import com.stiven.sos.ui.theme.FondoFormasFlotantes
import com.stiven.sos.ui.theme.EduRachaV2Colors
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.tasks.await

class TemasDelCursoActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var cursoId: String
    private lateinit var cursoNombre: String
    private var temas: List<Tema> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarDatosCurso()

        if (savedInstanceState == null) {
            quizViewModel.iniciarObservadores(cursoId)
            quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
        }

        setContent {
            EduRachaTheme {
                TemasDelCursoScreenFinal(
                    cursoNombre = cursoNombre,
                    cursoId = cursoId,
                    temas = temas,
                    quizViewModel = quizViewModel,
                    onNavigateBack = {
                        quizViewModel.detenerObservadores()
                        finish()
                    },
                    onTemaClick = { tema ->
                        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
                        prefs.edit().putString("last_tema_id", tema.id).apply()

                        val vidas = quizViewModel.uiState.value.vidas?.vidasActuales ?: 0
                        if (vidas == 0) {
                            quizViewModel.mostrarDialogoSinVidas()
                        } else {
                            val intent = Intent(this, ExplicacionTemaActivity::class.java).apply {
                                putExtra("curso_id", cursoId)
                                putExtra("tema_id", tema.id)
                                putExtra("tema_titulo", tema.titulo)
                                putExtra("tema_explicacion", tema.explicacion)
                            }
                            startActivity(intent)
                        }
                    },
                    onQuizFinalClick = {
                        val intent = Intent(this, QuizActivity::class.java).apply {
                            putExtra("curso_id", cursoId)
                            putExtra("tema_id", "quiz_final")
                            putExtra("tema_titulo", "Quiz Final - $cursoNombre")
                            putExtra("modo", "final")
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun cargarDatosCurso() {
        cursoId = intent.getStringExtra("curso_id") ?: ""
        cursoNombre = intent.getStringExtra("curso_nombre") ?: "Curso"
        temas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("curso_temas", Tema::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("curso_temas") ?: emptyList()
        }
        val prefs = getSharedPreferences("EduRachaUserPrefs", MODE_PRIVATE)
        prefs.edit().putString("last_curso_id", cursoId).apply()
    }

    override fun onResume() {
        super.onResume()
        quizViewModel.verificarTodosTemasAprobados(cursoId, temas.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            quizViewModel.detenerObservadores()
        }
    }
}

// ============================================================================
// PANTALLA PRINCIPAL
// ============================================================================

@Composable
fun TemasDelCursoScreenFinal(
    cursoNombre: String,
    cursoId: String,
    temas: List<Tema>,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onTemaClick: (Tema) -> Unit,
    onQuizFinalClick: () -> Unit
) {
    val uiState by quizViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var quizFinalAprobado by remember { mutableStateOf(false) }
    var temasAprobados by remember { mutableStateOf(setOf<String>()) }

    var mostrarDialogoVidas by remember { mutableStateOf(false) }
    var mostrarDialogoRacha by remember { mutableStateOf(false) }
    var mostrarDialogoXP by remember { mutableStateOf(false) }

    // Cargar estado de temas aprobados (calculado en el servidor)
    LaunchedEffect(cursoId) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        val userUid = prefs.getString("user_uid", "") ?: ""

        if (userUid.isNotEmpty() && cursoId.isNotEmpty()) {
            val respuesta = try {
                com.stiven.sos.api.ApiClient.apiService.obtenerTemasAprobados(cursoId)
            } catch (e: Exception) {
                Log.e("TemasDelCurso", "Error consultando progreso: ${e.message}")
                null
            }
            val cuerpo = respuesta?.body()
            if (respuesta != null && respuesta.isSuccessful && cuerpo != null) {
                temasAprobados = cuerpo.temasAprobados.toSet()
                quizFinalAprobado = cuerpo.quizFinalAprobado
            } else if (respuesta?.code() == 404) {
                // Respaldo temporal mientras se despliega el backend nuevo (endpoint aún no existe)
            try {
                val database = FirebaseDatabase.getInstance()
                val snapshot = database.getReference("quizzes").orderByChild("estudianteId").equalTo(userUid).get().await()

                val aprobados = mutableSetOf<String>()
                var finalAprobado = false

                if (snapshot.exists()) {
                    for (quizSnapshot in snapshot.children) {
                        val estudianteId = quizSnapshot.child("estudianteId").getValue(String::class.java)
                        if (estudianteId == userUid) {
                            val quizCursoId = quizSnapshot.child("cursoId").getValue(String::class.java)
                            val estado = quizSnapshot.child("estado").getValue(String::class.java)
                            val modo = quizSnapshot.child("modo").getValue(String::class.java)
                            val temaId = quizSnapshot.child("temaId").getValue(String::class.java)

                            if (quizCursoId == cursoId && estado == "finalizado") {
                                val correctas = quizSnapshot.child("preguntasCorrectas").getValue(Int::class.java) ?: 0
                                val incorrectas = quizSnapshot.child("preguntasIncorrectas").getValue(Int::class.java) ?: 0
                                val total = correctas + incorrectas

                                if (total > 0) {
                                    val porcentaje = (correctas * 100) / total
                                    if (porcentaje >= 80) {
                                        if (modo == "final") {
                                            finalAprobado = true
                                        } else if (temaId != null) {
                                            aprobados.add(temaId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                temasAprobados = aprobados
                quizFinalAprobado = finalAprobado
            } catch (e: Exception) {
                Log.e("TemasDelCurso", "Error: ${e.message}")
            }
            }
        }
    }

    if (uiState.mostrarDialogoSinVidas) {
        DialogoSinVidasMejorado(
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 30,
            onDismiss = { quizViewModel.cerrarDialogoSinVidas() }
        )
    }

    // Diálogos compactos
    if (mostrarDialogoVidas) {
        DialogoVidasCompacto(
            vidasActuales = uiState.vidas?.vidasActuales ?: 5,
            vidasMax = uiState.vidas?.vidasMax ?: 5,
            minutosParaProxima = uiState.vidas?.minutosParaProximaVida ?: 0,
            onDismiss = { mostrarDialogoVidas = false }
        )
    }

    if (mostrarDialogoRacha) {
        DialogoRachaCompacto(
            rachaDias = uiState.progreso?.rachaDias ?: 0,
            onDismiss = { mostrarDialogoRacha = false }
        )
    }

    if (mostrarDialogoXP) {
        DialogoXPCompacto(
            xpActual = uiState.progreso?.experiencia ?: 0,
            onDismiss = { mostrarDialogoXP = false }
        )
    }

    val p = com.stiven.sos.ui.theme.LocalNexo.current
    val temasOrdenados = remember(temas) { temas.sortedBy { it.orden } }
    val indiceActual = temasOrdenados.indexOfFirst { !temasAprobados.contains(it.id) }
    val progresoCurso = if (temas.isEmpty()) 0f else temasAprobados.count { id -> temas.any { it.id == id } }.toFloat() / temas.size

    com.stiven.sos.ui.theme.FondoNexo(conCuadricula = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                com.stiven.sos.ui.theme.EncabezadoNexo(
                    titulo = cursoNombre,
                    subtitulo = "Mapa de islas",
                    onVolver = onNavigateBack
                )
            }

            // Estadísticas (vidas, racha, XP) — tocables para ver detalle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.stiven.sos.ui.theme.StatNexo(
                        icono = Icons.Default.Favorite,
                        valor = "${uiState.vidas?.vidasActuales ?: 5}/${uiState.vidas?.vidasMax ?: 5}",
                        etiqueta = "Vidas",
                        color = p.error,
                        onClick = { mostrarDialogoVidas = true },
                        modifier = Modifier.weight(1f)
                    )
                    com.stiven.sos.ui.theme.StatNexo(
                        icono = Icons.Default.LocalFireDepartment,
                        valor = "${uiState.progreso?.rachaDias ?: 0}",
                        etiqueta = "Racha",
                        color = p.racha,
                        onClick = { mostrarDialogoRacha = true },
                        modifier = Modifier.weight(1f)
                    )
                    com.stiven.sos.ui.theme.StatNexo(
                        icono = Icons.Default.Bolt,
                        valor = "${uiState.progreso?.experiencia ?: 0}",
                        etiqueta = "XP total",
                        color = p.ia,
                        onClick = { mostrarDialogoXP = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Progreso del curso + Robi
            item {
                com.stiven.sos.ui.theme.TarjetaNexo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    acento = p.marca
                ) {
                    com.stiven.sos.ui.theme.GloboRobi(
                        titulo = "ROBI",
                        mensaje = when {
                            temas.isEmpty() -> "Este curso aún no tiene temas. Vuelve pronto."
                            indiceActual == -1 -> "Conquistaste todas las islas. Es hora del quiz final."
                            indiceActual == 0 && temasAprobados.isEmpty() -> "Toca la primera isla para empezar tu ruta."
                            else -> "Vas muy bien. Tu siguiente isla: ${temasOrdenados.getOrNull(indiceActual)?.titulo ?: ""}"
                        },
                        expresion = if (indiceActual == -1 && temas.isNotEmpty()) com.stiven.sos.ui.theme.ExpresionRobi.CELEBRA else com.stiven.sos.ui.theme.ExpresionRobi.FELIZ,
                        tamanoRobi = 68.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Progreso del curso",
                            color = p.textoSuave,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${temasAprobados.count { id -> temas.any { it.id == id } }}/${temas.size} islas",
                            color = p.accionTexto,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    com.stiven.sos.ui.theme.BarraNexo(progreso = progresoCurso, color = p.accion)
                }
            }

            item {
                com.stiven.sos.ui.theme.TituloSeccionNexo(
                    texto = "Tu ruta",
                    icono = Icons.Default.Map,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // MAPA DE ISLAS (misma navegación: onTemaClick)
            itemsIndexed(temasOrdenados) { index, tema ->
                val aprobado = temasAprobados.contains(tema.id)
                val estado = when {
                    aprobado -> com.stiven.sos.ui.theme.EstadoHex.COMPLETADO
                    index == indiceActual -> com.stiven.sos.ui.theme.EstadoHex.ACTUAL
                    else -> com.stiven.sos.ui.theme.EstadoHex.DISPONIBLE
                }
                val tipoIsla = when (estado) {
                    com.stiven.sos.ui.theme.EstadoHex.COMPLETADO -> com.stiven.sos.ui.theme.TipoIsla.CONQUISTADA
                    com.stiven.sos.ui.theme.EstadoHex.ACTUAL -> com.stiven.sos.ui.theme.TipoIsla.ACTIVA
                    else -> com.stiven.sos.ui.theme.TipoIsla.BLOQUEADA
                }
                // Zigzag suave: 0, +1, 0, -1 ...
                val patron = listOf(0f, 1f, 0f, -1f)
                val desplazamiento = (patron[index % patron.size] * 64).dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.offset(x = desplazamiento),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (estado == com.stiven.sos.ui.theme.EstadoHex.ACTUAL) {
                            Text(
                                if (index == 0) "EMPIEZA AQUÍ" else "SIGUE AQUÍ",
                                color = p.sobreAccion,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(p.accion)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.size(width = 150.dp, height = 132.dp)) {
                            com.stiven.sos.ui.theme.IslaFlotante(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = 150.dp, height = 70.dp),
                                tipo = tipoIsla,
                                fase = index % 3
                            )
                            com.stiven.sos.ui.theme.HexNodo(
                                estado = estado,
                                descripcion = "Isla ${index + 1}: ${tema.titulo}. " +
                                    if (aprobado) "Conquistada, toca para repasar" else "Toca para comenzar",
                                onClick = { onTemaClick(tema) },
                                tamano = 76.dp,
                                etiqueta = "${index + 1}"
                            )
                        }
                        Text(
                            tema.titulo,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = p.texto,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                        Text(
                            if (aprobado) "Conquistada · repasar" else if (estado == com.stiven.sos.ui.theme.EstadoHex.ACTUAL) "Siguiente isla" else "Disponible",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aprobado) p.rachaTexto else p.textoSuave
                        )
                    }
                }
            }

            // QUIZ FINAL
            if (temasAprobados.size == temas.size && temas.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    QuizFinalCardFigma(
                        onClick = {
                            if (quizFinalAprobado) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Ya aprobaste el Quiz Final",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onQuizFinalClick()
                            }
                        },
                        yaAprobado = quizFinalAprobado,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            if (temas.isEmpty()) {
                item {
                    com.stiven.sos.ui.theme.EstadoVacioNexo(
                        titulo = "Sin islas todavía",
                        mensaje = "Tu docente aún no ha publicado temas en este curso."
                    )
                }
            }
        }
    }
}

// ============================================================================
// QUIZ FINAL CARD
// ============================================================================

@Composable
fun QuizFinalCardFigma(
    onClick: () -> Unit,
    yaAprobado: Boolean,
    modifier: Modifier = Modifier
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.TarjetaNexo(
        modifier = modifier.fillMaxWidth(),
        acento = if (yaAprobado) p.racha else p.ia,
        padding = 20.dp
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            com.stiven.sos.ui.theme.InsigniaNexo(
                icono = Icons.Default.EmojiEvents,
                color = if (yaAprobado) p.racha else p.ia,
                tamano = 72.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (yaAprobado) "Quiz final completado" else "Isla final: el gran quiz",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = p.texto,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (yaAprobado) "Completaste este curso. Buen trabajo."
                else "Demuestra todo lo que aprendiste para completar el curso.",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.textoSuave,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (!yaAprobado) {
                Spacer(Modifier.height(16.dp))
                com.stiven.sos.ui.theme.BotonNexo(
                    texto = "Comenzar quiz final",
                    onClick = onClick,
                    tipo = com.stiven.sos.ui.theme.TipoBoton.IA,
                    icono = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// DIÁLOGOS COMPACTOS
// ============================================================================

@Composable
fun DialogoVidasCompacto(
    vidasActuales: Int,
    vidasMax: Int,
    minutosParaProxima: Int,
    onDismiss: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoNexo(
        icono = Icons.Default.Favorite,
        color = p.error,
        titulo = "Vidas",
        valor = "$vidasActuales",
        unidad = "/ $vidasMax",
        mensaje = "Se pierde 1 vida al responder mal.",
        nota = if (vidasActuales < vidasMax) "Se recupera 1 vida cada 30 minutos" else null,
        onDismiss = onDismiss
    )
}

@Composable
fun DialogoRachaCompacto(
    rachaDias: Int,
    onDismiss: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoNexo(
        icono = Icons.Default.LocalFireDepartment,
        color = p.racha,
        titulo = "Racha",
        valor = "$rachaDias",
        unidad = if (rachaDias == 1) "día" else "días",
        mensaje = when {
            rachaDias == 0 -> "Comienza tu racha hoy completando una isla."
            rachaDias < 7 -> "Excelente inicio. No la dejes caer."
            rachaDias < 30 -> "Increíble dedicación."
            else -> "Eres una leyenda del curso."
        },
        textoBoton = "Continuar",
        onDismiss = onDismiss
    )
}

@Composable
fun DialogoXPCompacto(
    xpActual: Int,
    onDismiss: () -> Unit
) {
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoNexo(
        icono = Icons.Default.Bolt,
        color = p.ia,
        titulo = "Experiencia",
        valor = "$xpActual",
        unidad = "XP",
        mensaje = "Completa temas y quizzes para ganar más experiencia.",
        textoBoton = "Continuar",
        onDismiss = onDismiss
    )
}