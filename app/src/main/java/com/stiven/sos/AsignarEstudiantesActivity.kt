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

class AsignarEstudiantesActivity : ComponentActivity() {

    private val solicitudViewModel: SolicitudDocenteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""

        //  Obtener docenteId de SessionManager en lugar del Intent
        val sessionManager = SessionManager.getInstance(applicationContext)
        val docenteId = sessionManager.getUserId() ?: ""

        Log.d("AsignarEstudiantes", """
             Activity iniciada:
            - Curso: $cursoTitulo
            - CursoID: $cursoId
            - DocenteID: $docenteId
        """.trimIndent())

        if (docenteId.isBlank()) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        //  Cargar todas las solicitudes del docente
        solicitudViewModel.cargarSolicitudesDocente(docenteId)

        setContent {
            EduRachaTheme {
                GestionSolicitudesScreen(
                    cursoTitulo = cursoTitulo,
                    cursoId = cursoId,
                    docenteId = docenteId,
                    solicitudViewModel = solicitudViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionSolicitudesScreen(
    cursoTitulo: String,
    cursoId: String,
    docenteId: String,
    solicitudViewModel: SolicitudDocenteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val solicitudUiState by solicitudViewModel.uiState.collectAsState()

    var solicitudSeleccionada by remember { mutableStateOf<SolicitudCurso?>(null) }
    var showRechazarDialog by remember { mutableStateOf(false) }
    var showAceptarDialog by remember { mutableStateOf(false) }
    var mensajeRespuesta by remember { mutableStateOf("") }
    var mensajePredeterminadoSeleccionado by remember { mutableStateOf<String?>(null) }
    var modoPersonalizado by remember { mutableStateOf(false) }

    //  Filtrar solicitudes del curso actual que estén PENDIENTES
    val solicitudesCurso = remember(solicitudUiState.solicitudes, cursoId) {
        val filtradas = solicitudUiState.solicitudes.filter {
            it.cursoId == cursoId && it.estado == EstadoSolicitud.PENDIENTE
        }
        Log.d("GestionSolicitudes", """
             Filtrando solicitudes:
            - Total recibidas: ${solicitudUiState.solicitudes.size}
            - Del curso $cursoId: ${filtradas.size}
        """.trimIndent())
        filtradas
    }

    LaunchedEffect(solicitudUiState.mensajeExito) {
        solicitudUiState.mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            solicitudViewModel.clearMessages()
            //  Recargar solicitudes después de aceptar/rechazar
            solicitudViewModel.cargarSolicitudesDocente(docenteId)
        }
    }

    LaunchedEffect(solicitudUiState.error) {
        solicitudUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            solicitudViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Aspirantes al Curso",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            cursoTitulo,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (solicitudesCurso.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.PendingActions,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${solicitudesCurso.size}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                solicitudUiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EduRachaColors.Primary
                    )
                }

                solicitudesCurso.isEmpty() -> {
                    EmptyAspirantesView()
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = solicitudesCurso,
                            key = { it.id ?: it.estudianteId }
                        ) { solicitud ->
                            SolicitudAspiranteCard(
                                solicitud = solicitud,
                                onAceptarClick = {
                                    solicitudSeleccionada = solicitud
                                    mensajeRespuesta = ""
                                    mensajePredeterminadoSeleccionado = null
                                    modoPersonalizado = false
                                    showAceptarDialog = true
                                },
                                onRechazarClick = {
                                    solicitudSeleccionada = solicitud
                                    mensajeRespuesta = ""
                                    mensajePredeterminadoSeleccionado = null
                                    modoPersonalizado = false
                                    showRechazarDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogos de aceptar y rechazar
    if (showAceptarDialog && solicitudSeleccionada != null) {
        DialogoAceptarSolicitud(
            solicitud = solicitudSeleccionada!!,
            mensajePredeterminadoSeleccionado = mensajePredeterminadoSeleccionado,
            onMensajeSeleccionado = { mensajePredeterminadoSeleccionado = it },
            modoPersonalizado = modoPersonalizado,
            onModoPersonalizadoChange = { modoPersonalizado = it },
            mensajeRespuesta = mensajeRespuesta,
            onMensajeChange = { mensajeRespuesta = it },
            onConfirm = {
                val mensajeFinal = if (modoPersonalizado) {
                    mensajeRespuesta.ifBlank { null }
                } else {
                    mensajePredeterminadoSeleccionado
                }
                solicitudSeleccionada?.id?.let { id ->
                    solicitudViewModel.aceptarSolicitud(id, mensajeFinal)
                }
                showAceptarDialog = false
            },
            onDismiss = {
                showAceptarDialog = false
                solicitudSeleccionada = null
                mensajeRespuesta = ""
                mensajePredeterminadoSeleccionado = null
                modoPersonalizado = false
            }
        )
    }

    if (showRechazarDialog && solicitudSeleccionada != null) {
        DialogoRechazarSolicitud(
            solicitud = solicitudSeleccionada!!,
            mensajePredeterminadoSeleccionado = mensajePredeterminadoSeleccionado,
            onMensajeSeleccionado = { mensajePredeterminadoSeleccionado = it },
            modoPersonalizado = modoPersonalizado,
            onModoPersonalizadoChange = { modoPersonalizado = it },
            mensajeRespuesta = mensajeRespuesta,
            onMensajeChange = { mensajeRespuesta = it },
            onConfirm = {
                val mensajeFinal = if (modoPersonalizado) {
                    mensajeRespuesta.ifBlank { null }
                } else {
                    mensajePredeterminadoSeleccionado
                }
                solicitudSeleccionada?.id?.let { id ->
                    solicitudViewModel.rechazarSolicitud(id, mensajeFinal)
                }
                showRechazarDialog = false
            },
            onDismiss = {
                showRechazarDialog = false
                solicitudSeleccionada = null
                mensajeRespuesta = ""
                mensajePredeterminadoSeleccionado = null
                modoPersonalizado = false
            }
        )
    }
}



@Composable
fun DialogoAceptarSolicitud(
    solicitud: SolicitudCurso,
    mensajePredeterminadoSeleccionado: String?,
    onMensajeSeleccionado: (String?) -> Unit,
    modoPersonalizado: Boolean,
    onModoPersonalizadoChange: (Boolean) -> Unit,
    mensajeRespuesta: String,
    onMensajeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = EduRachaColors.Success,
                modifier = Modifier.size(52.dp)
            )
        },
        title = {
            Text(
                "Aceptar Solicitud",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
        },
        text = {
            DialogoAceptarContent(
                solicitud = solicitud,
                mensajePredeterminadoSeleccionado = mensajePredeterminadoSeleccionado,
                onMensajeSeleccionado = onMensajeSeleccionado,
                modoPersonalizado = modoPersonalizado,
                onModoPersonalizadoChange = onModoPersonalizadoChange,
                mensajeRespuesta = mensajeRespuesta,
                onMensajeChange = onMensajeChange
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Success
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Aceptar Estudiante", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DialogoRechazarSolicitud(
    solicitud: SolicitudCurso,
    mensajePredeterminadoSeleccionado: String?,
    onMensajeSeleccionado: (String?) -> Unit,
    modoPersonalizado: Boolean,
    onModoPersonalizadoChange: (Boolean) -> Unit,
    mensajeRespuesta: String,
    onMensajeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Cancel,
                null,
                tint = EduRachaColors.Error,
                modifier = Modifier.size(52.dp)
            )
        },
        title = {
            Text(
                "Rechazar Solicitud",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
        },
        text = {
            DialogoRechazarContent(
                solicitud = solicitud,
                mensajePredeterminadoSeleccionado = mensajePredeterminadoSeleccionado,
                onMensajeSeleccionado = onMensajeSeleccionado,
                modoPersonalizado = modoPersonalizado,
                onModoPersonalizadoChange = onModoPersonalizadoChange,
                mensajeRespuesta = mensajeRespuesta,
                onMensajeChange = onMensajeChange
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EduRachaColors.Error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Rechazar Solicitud", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DialogoAceptarContent(
    solicitud: SolicitudCurso,
    mensajePredeterminadoSeleccionado: String?,
    onMensajeSeleccionado: (String?) -> Unit,
    modoPersonalizado: Boolean,
    onModoPersonalizadoChange: (Boolean) -> Unit,
    mensajeRespuesta: String,
    onMensajeChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Vas a aceptar a ${solicitud.estudianteNombre} en el curso",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 15.sp,
            color = EduRachaColors.TextPrimary
        )

        val mensajeEst = solicitud.mensajeEstudiante
        if (!mensajeEst.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            MensajeEstudianteCard(mensajeEst)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Mensaje de bienvenida",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (!modoPersonalizado) {
            OpcionesMensajePredeterminado(
                mensajesPredeterminados = listOf(
                    "Bienvenido al curso. Estoy seguro de que te irá muy bien.",
                    "Me alegra tenerte en el curso. Espero que disfrutes el aprendizaje.",
                    "Has sido aceptado. Estoy disponible si tienes alguna duda.",
                    "Bienvenido. Prepárate para una gran experiencia de aprendizaje."
                ),
                mensajeSeleccionado = mensajePredeterminadoSeleccionado,
                onMensajeSeleccionado = onMensajeSeleccionado,
                colorFondo = EduRachaColors.Success,
                onModoPersonalizado = { onModoPersonalizadoChange(true) }
            )
        } else {
            CampoMensajePersonalizado(
                mensajeRespuesta = mensajeRespuesta,
                onMensajeChange = onMensajeChange,
                placeholder = "Ej: Bienvenido al curso...",
                colorBorde = EduRachaColors.Success,
                onVolverPredeterminados = { onModoPersonalizadoChange(false) }
            )
        }
    }
}

@Composable
fun DialogoRechazarContent(
    solicitud: SolicitudCurso,
    mensajePredeterminadoSeleccionado: String?,
    onMensajeSeleccionado: (String?) -> Unit,
    modoPersonalizado: Boolean,
    onModoPersonalizadoChange: (Boolean) -> Unit,
    mensajeRespuesta: String,
    onMensajeChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Vas a rechazar la solicitud de ${solicitud.estudianteNombre}",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 15.sp,
            color = EduRachaColors.TextPrimary
        )

        val mensajeEst = solicitud.mensajeEstudiante
        if (!mensajeEst.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            MensajeEstudianteCard(mensajeEst)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Motivo del rechazo (opcional)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (!modoPersonalizado) {
            OpcionesMensajePredeterminado(
                mensajesPredeterminados = listOf(
                    "Lo siento, el curso ha alcanzado su capacidad máxima.",
                    "En este momento no hay cupos disponibles.",
                    "No cumples con los requisitos previos del curso.",
                    "La solicitud no cumple con los criterios de admisión."
                ),
                mensajeSeleccionado = mensajePredeterminadoSeleccionado,
                onMensajeSeleccionado = onMensajeSeleccionado,
                colorFondo = EduRachaColors.Error,
                onModoPersonalizado = { onModoPersonalizadoChange(true) }
            )
        } else {
            CampoMensajePersonalizado(
                mensajeRespuesta = mensajeRespuesta,
                onMensajeChange = onMensajeChange,
                placeholder = "Ej: El curso está lleno...",
                colorBorde = EduRachaColors.Error,
                onVolverPredeterminados = { onModoPersonalizadoChange(false) }
            )
        }
    }
}

@Composable
fun MensajeEstudianteCard(mensaje: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EduRachaColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, EduRachaColors.Primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Message,
                    null,
                    tint = EduRachaColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Mensaje del estudiante",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.Primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = mensaje,
                fontSize = 13.sp,
                color = EduRachaColors.TextPrimary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun OpcionesMensajePredeterminado(
    mensajesPredeterminados: List<String>,
    mensajeSeleccionado: String?,
    onMensajeSeleccionado: (String?) -> Unit,
    colorFondo: Color,
    onModoPersonalizado: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mensajesPredeterminados.forEach { mensaje ->
            MensajePredeterminadoChip(
                mensaje = mensaje,
                seleccionado = mensajeSeleccionado == mensaje,
                onClick = {
                    onMensajeSeleccionado(if (mensajeSeleccionado == mensaje) null else mensaje)
                },
                colorFondo = colorFondo
            )
        }

        Spacer(Modifier.height(4.dp))

        TextButton(
            onClick = onModoPersonalizado,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Escribir mensaje personalizado")
        }
    }
}

@Composable
fun CampoMensajePersonalizado(
    mensajeRespuesta: String,
    onMensajeChange: (String) -> Unit,
    placeholder: String,
    colorBorde: Color,
    onVolverPredeterminados: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = mensajeRespuesta,
            onValueChange = onMensajeChange,
            label = { Text("Escribe tu mensaje") },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorBorde,
                focusedLabelColor = colorBorde
            )
        )

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onVolverPredeterminados) {
            Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Volver a mensajes predeterminados")
        }
    }
}

@Composable
fun MensajePredeterminadoChip(
    mensaje: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    colorFondo: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) colorFondo.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) colorFondo else EduRachaColors.TextSecondary.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = seleccionado,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorFondo,
                    unselectedColor = EduRachaColors.TextSecondary
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = mensaje,
                fontSize = 13.sp,
                color = if (seleccionado) EduRachaColors.TextPrimary else EduRachaColors.TextSecondary,
                fontWeight = if (seleccionado) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SolicitudAspiranteCard(
    solicitud: SolicitudCurso,
    onAceptarClick: () -> Unit,
    onRechazarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.Primary.copy(alpha = 0.75f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = solicitud.estudianteNombre
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull() }
                            .joinToString("")
                            .uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solicitud.estudianteNombre,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            null,
                            tint = EduRachaColors.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = solicitud.estudianteEmail,
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EduRachaColors.SurfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Solicitó el ${solicitud.fechaSolicitud}",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val mensajeEst = solicitud.mensajeEstudiante
            if (!mensajeEst.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, EduRachaColors.Primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                null,
                                tint = EduRachaColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mensaje del estudiante",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = mensajeEst,
                            fontSize = 13.sp,
                            color = EduRachaColors.TextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRechazarClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EduRachaColors.Error
                    ),
                    border = BorderStroke(1.5.dp, EduRachaColors.Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Rechazar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = onAceptarClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Success
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Aceptar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyAspirantesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(EduRachaColors.SurfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = EduRachaColors.TextSecondary.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "No hay solicitudes pendientes",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cuando los estudiantes soliciten unirse a este curso, aparecerán aquí para que puedas revisar y aprobar sus solicitudes",
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}