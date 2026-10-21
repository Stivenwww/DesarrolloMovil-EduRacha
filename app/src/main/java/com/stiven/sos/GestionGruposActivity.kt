package com.stiven.sos

import androidx.compose.foundation.border
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.Curso
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.CursoViewModel
import com.stiven.sos.viewmodel.SolicitudViewModel
import kotlinx.coroutines.delay

class GestionGruposActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()
    private val solicitudViewModel: SolicitudViewModel by viewModels()

    private var docenteSesion = ""

    // Al volver de un grupo se actualizan los aspirantes pendientes
    override fun onRestart() {
        super.onRestart()
        if (docenteSesion.isNotEmpty()) solicitudViewModel.cargarSolicitudesDocente(docenteSesion)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar datos
        val docenteId = com.stiven.sos.services.uidSesion(this)
        docenteSesion = docenteId

        cursoViewModel.obtenerCursos()
        if (docenteId.isNotEmpty()) {
            solicitudViewModel.cargarSolicitudesDocente(docenteId)
        }

        setContent {
            EduRachaTheme {
                GestionGruposScreen(
                    cursoViewModel = cursoViewModel,
                    solicitudViewModel = solicitudViewModel,
                    docenteId = docenteId,
                    onNavigateBack = { finish() },
                    onCursoClick = { curso ->
                        val intent = Intent(this, AsignarEstudiantesActivity::class.java)
                        intent.putExtra("CURSO_TITULO", curso.titulo)
                        intent.putExtra("CURSO_CODIGO", curso.codigo)
                        intent.putExtra("CURSO_ID", curso.id)
                        startActivity(intent)
                    },
                    onCrearCurso = {
                        startActivity(Intent(this, CrearCursoActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun GestionGruposScreen(
    cursoViewModel: CursoViewModel,
    solicitudViewModel: SolicitudViewModel,
    docenteId: String = "",
    onNavigateBack: () -> Unit,
    onCursoClick: (Curso) -> Unit,
    onCrearCurso: () -> Unit
) {
    val cursoUiState by cursoViewModel.uiState.collectAsState()
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    // Solo los cursos del docente que inició sesión
    val cursos = remember(cursoUiState.cursos, docenteId) {
        cursoUiState.cursos.filter { docenteId.isBlank() || it.docenteId == docenteId }
    }
    val isLoading = cursoUiState.isLoading
    val errorMessage = cursoUiState.error

    // Contar solicitudes pendientes por curso
    val solicitudesPorCurso = remember(solicitudUiState.solicitudes) {
        solicitudUiState.solicitudes
            .filter { it.estado == EstadoSolicitud.PENDIENTE }
            .groupBy { it.cursoId }
            .mapValues { it.value.size }
    }

    LaunchedEffect(Unit) {
        cursoViewModel.obtenerCursos()
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && cursos.isEmpty() -> {
                    com.stiven.sos.ui.theme.CargandoNexo(
                        titulo = "Cargando tus grupos",
                        mensaje = "Revisando cursos y aspirantes"
                    )
                }

                errorMessage != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = 0
                        )
                        ErrorStateGrupos(
                            errorMessage = errorMessage,
                            onRetry = { cursoViewModel.obtenerCursos() }
                        )
                    }
                }

                cursos.isEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = 0
                        )
                        EmptyStateGrupos(onCrearCurso = onCrearCurso)
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GestionGruposHeader(
                            onNavigateBack = onNavigateBack,
                            totalCursos = cursos.size
                        )

                        EstadisticasGruposCard(
                            totalCursos = cursos.size,
                            totalTemas = cursos.sumOf { it.temas?.size ?: 0 },
                            cursosActivos = cursos.count { it.estado.equals("activo", ignoreCase = true) },
                            totalSolicitudes = solicitudesPorCurso.values.sum(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        SectionHeaderGrupos(
                            title = "Tus cursos",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = cursos,
                                key = { it.id ?: it.codigo }
                            ) { curso ->
                                CursoGrupoCard(
                                    curso = curso,
                                    solicitudesPendientes = solicitudesPorCurso[curso.id] ?: 0,
                                    onClick = { onCursoClick(curso) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateGrupos(errorMessage: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        com.stiven.sos.ui.theme.EstadoVacioNexo(
            titulo = "No pudimos cargar tus cursos",
            mensaje = errorMessage,
            expresion = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE,
            textoAccion = "Reintentar",
            onAccion = onRetry
        )
    }
}

@Composable
fun GestionGruposHeader(
    onNavigateBack: () -> Unit,
    totalCursos: Int
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.EncabezadoNexo(
        titulo = "Gestión de grupos",
        subtitulo = "Aspirantes y estudiantes",
        onVolver = onNavigateBack,
        acciones = {
            if (totalCursos > 0) {
                com.stiven.sos.ui.theme.ChipNexo(
                    texto = "$totalCursos",
                    color = nx.iaTexto,
                    icono = Icons.Default.School,
                    descripcion = "$totalCursos cursos"
                )
            }
        }
    )
}

@Composable
fun EstadisticasGruposCard(
    totalCursos: Int,
    totalTemas: Int,
    cursosActivos: Int,
    totalSolicitudes: Int,
    modifier: Modifier = Modifier
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        com.stiven.sos.ui.theme.StatNexo(
            icono = Icons.Default.School,
            valor = "$totalCursos",
            etiqueta = "Cursos",
            color = nx.marca,
            modifier = Modifier.weight(1f)
        )
        com.stiven.sos.ui.theme.StatNexo(
            icono = Icons.Default.PersonAdd,
            valor = "$totalSolicitudes",
            etiqueta = "Aspirantes",
            color = nx.racha,
            modifier = Modifier.weight(1f)
        )
        com.stiven.sos.ui.theme.StatNexo(
            icono = Icons.Default.CheckCircle,
            valor = "$cursosActivos",
            etiqueta = "Activos",
            color = nx.accion,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItemGrupos(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SectionHeaderGrupos(
    title: String,
    modifier: Modifier = Modifier
) {
    com.stiven.sos.ui.theme.TituloSeccionNexo(
        texto = title,
        icono = Icons.Default.Groups,
        modifier = modifier
    )
}

@Composable
fun CursoGrupoCard(
    curso: Curso,
    solicitudesPendientes: Int,
    onClick: () -> Unit
) {
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    val cantidadTemas = curso.temas?.size ?: 0
    val activo = curso.estado.equals("activo", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(nx.superficie)
            .border(1.dp, if (solicitudesPendientes > 0) nx.racha.copy(alpha = 0.6f) else nx.borde, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.stiven.sos.ui.theme.InsigniaNexo(Icons.Default.School, nx.marca, tamano = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = curso.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = nx.texto,
                    maxLines = 2
                )
                Text(
                    text = "Código: ${curso.codigo}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = nx.textoSuave
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver grupo de ${curso.titulo}",
                tint = nx.textoSuave
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.stiven.sos.ui.theme.ChipNexo(
                texto = if (activo) "Activo" else curso.estado.ifBlank { "Sin estado" }.replaceFirstChar { it.uppercase() },
                color = if (activo) nx.accionTexto else nx.textoSuave,
                icono = if (activo) Icons.Default.CheckCircle else Icons.Default.PauseCircle
            )
            com.stiven.sos.ui.theme.ChipNexo(
                texto = "$cantidadTemas ${if (cantidadTemas == 1) "tema" else "temas"}",
                color = nx.iaTexto,
                icono = Icons.Default.MenuBook
            )
        }
        if (solicitudesPendientes > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(nx.racha.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = nx.rachaTexto, modifier = Modifier.size(20.dp))
                Text(
                    text = "$solicitudesPendientes aspirante${if (solicitudesPendientes > 1) "s" else ""} esperando aprobación",
                    fontSize = 13.sp,
                    color = nx.rachaTexto,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun EmptyStateGrupos(onCrearCurso: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        com.stiven.sos.ui.theme.EstadoVacioNexo(
            titulo = "Aún no tienes cursos",
            mensaje = "Crea un curso primero para poder gestionar grupos de estudiantes.",
            textoAccion = "Crear curso",
            onAccion = onCrearCurso
        )
    }
}