package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

class CursosInscritosActivity : ComponentActivity() {

    private val quizViewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        quizViewModel.cargarCursosInscritos()

        setContent {
            EduRachaTheme {
                CursosInscritosScreenV2Figma(
                    quizViewModel = quizViewModel,
                    onNavigateBack = { finish() },
                    onCursoClick = { curso ->
                        val intent = Intent(this, TemasDelCursoActivity::class.java)
                        intent.putExtra("curso_id", curso.id)
                        intent.putExtra("curso_nombre", curso.titulo)

                        val temasList = ArrayList(curso.getTemasLista())
                        intent.putParcelableArrayListExtra("curso_temas", temasList)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun CursosInscritosScreenV2Figma(
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onCursoClick: (Curso) -> Unit
) {
    val p = LocalNexo.current
    val uiState by quizViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filtrado de cursos (misma regla de siempre)
    val cursosFiltrados = remember(uiState.cursosInscritos, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.cursosInscritos
        } else {
            uiState.cursosInscritos.filter { curso ->
                curso.titulo.contains(searchQuery, ignoreCase = true) ||
                        curso.codigo.contains(searchQuery, ignoreCase = true) ||
                        curso.descripcion?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    FondoNexo(conCuadricula = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EncabezadoNexo(
                    titulo = "Mis cursos",
                    subtitulo = if (uiState.cursosInscritos.isEmpty()) "Tus islas" else "${uiState.cursosInscritos.size} cursos activos",
                    onVolver = onNavigateBack
                )
            }

            if (uiState.cursosInscritos.isNotEmpty()) {
                item {
                    BuscadorNexo(
                        valor = searchQuery,
                        onCambio = { searchQuery = it },
                        placeholder = "Buscar por nombre o código",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            when {
                uiState.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RobiNeo(tamano = 110.dp, expresion = ExpresionRobi.PIENSA)
                            Spacer(Modifier.height(10.dp))
                            Text("Cargando tus cursos...", color = p.textoSuave, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                uiState.error != null -> item {
                    EstadoVacioNexo(
                        titulo = "No pudimos cargar tus cursos",
                        mensaje = uiState.error ?: "",
                        expresion = ExpresionRobi.TRISTE,
                        textoAccion = "Reintentar",
                        onAccion = { quizViewModel.cargarCursosInscritos() }
                    )
                }

                uiState.cursosInscritos.isEmpty() -> item {
                    EstadoVacioNexo(
                        titulo = "Aún no tienes cursos",
                        mensaje = "Pide a tu docente el código del curso y únete desde \"Cursos disponibles\"."
                    )
                }

                cursosFiltrados.isEmpty() -> item {
                    EstadoVacioNexo(
                        titulo = "Sin resultados",
                        mensaje = "No hay cursos que coincidan con \"$searchQuery\"."
                    )
                }

                else -> itemsIndexed(
                    items = cursosFiltrados,
                    key = { _, curso -> curso.id ?: curso.codigo }
                ) { index, curso ->
                    EntradaEscalonada(index) {
                        TarjetaCursoNexo(
                            titulo = curso.titulo,
                            codigo = curso.codigo,
                            descripcion = curso.descripcion,
                            temas = curso.getTemasLista().sortedBy { it.orden }.map { it.titulo },
                            indice = index,
                            textoAccion = "Abrir mapa de islas",
                            iconoAccion = Icons.Outlined.Map,
                            onAccion = { onCursoClick(curso) },
                            onClickTarjeta = { onCursoClick(curso) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
