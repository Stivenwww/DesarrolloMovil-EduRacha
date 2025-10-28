package com.stiven.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stiven.sos.models.EstadoSolicitud
import com.stiven.sos.models.SolicitudCurso
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.SolicitudDocenteViewModel
import kotlinx.coroutines.delay

class AsignarEstudiantesActivity : ComponentActivity() {

    // ✅ CAMBIO PRINCIPAL: Pasar application al ViewModel
    private val solicitudViewModel: SolicitudDocenteViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SolicitudDocenteViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"
        val cursoCodigo = intent.getStringExtra("CURSO_CODIGO") ?: ""
        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""

        setContent {
            EduRachaTheme {
                AsignarEstudiantesScreen(
                    viewModel = solicitudViewModel,
                    cursoTitulo = cursoTitulo,
                    cursoCodigo = cursoCodigo,
                    cursoId = cursoId,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

// ... El resto del código de Composables sin cambios ...
@Composable
fun AsignarEstudiantesScreen(
    viewModel: SolicitudDocenteViewModel,
    cursoTitulo: String,
    cursoCodigo: String,
    cursoId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitudes = uiState.solicitudes
    val isLoading = uiState.isLoading
    val error = uiState.error
    val mensajeExito = uiState.mensajeExito

    val solicitudesPendientes = solicitudes.filter { it.estado == EstadoSolicitud.PENDIENTE }
    val solicitudesAceptadas = solicitudes.filter { it.estado == EstadoSolicitud.ACEPTADA }
    val solicitudesRechazadas = solicitudes.filter { it.estado == EstadoSolicitud.RECHAZADA }

    var solicitudParaResponder by remember { mutableStateOf<SolicitudCurso?>(null) }
    var mostrarDialogoAceptar by remember { mutableStateOf(false) }
    var mostrarDialogoRechazar by remember { mutableStateOf(false) }

    LaunchedEffect(cursoId) {
        viewModel.cargarSolicitudesPorCurso(cursoId)
    }

    LaunchedEffect(mensajeExito, error) {
        if (mensajeExito != null || error != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = EduRachaColors.Background,
        snackbarHost = {
            if (mensajeExito != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = EduRachaColors.Success
                ) {
                    Text(mensajeExito, color = Color.White)
                }
            }
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = EduRachaColors.Error
                ) {
                    Text(error, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AsignarEstudiantesHeader(
                cursoTitulo = cursoTitulo,
                cursoCodigo = cursoCodigo,
                totalPendientes = solicitudesPendientes.size,
                onNavigateBack = onNavigateBack
            )

            when {
                isLoading && solicitudes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = EduRachaColors.Primary)
                            Text("Cargando solicitudes...", color = EduRachaColors.TextSecondary)
                        }
                    }
                }

                solicitudes.isEmpty() -> {
                    EmptyStateSolicitudes()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (solicitudesPendientes.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "SOLICITUDES PENDIENTES",
                                    count = solicitudesPendientes.size,
                                    color = EduRachaColors.Primary
                                )
                            }

                            items(
                                items = solicitudesPendientes,
                                key = { it.id ?: it.estudianteId }
                            ) { solicitud ->
                                SolicitudCard(
                                    solicitud = solicitud,
                                    onAceptar = {
                                        solicitudParaResponder = solicitud
                                        mostrarDialogoAceptar = true
                                    },
                                    onRechazar = {
                                        solicitudParaResponder = solicitud
                                        mostrarDialogoRechazar = true
                                    },
                                    isProcessing = uiState.solicitudEnProceso == solicitud.id
                                )
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        if (solicitudesAceptadas.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "ACEPTADAS",
                                    count = solicitudesAceptadas.size,
                                    color = EduRachaColors.Success
                                )
                            }

                            items(
                                items = solicitudesAceptadas,
                                key = { it.id ?: it.estudianteId }
                            ) { solicitud ->
                                SolicitudCardReadOnly(
                                    solicitud = solicitud,
                                    estado = EstadoSolicitud.ACEPTADA
                                )
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        if (solicitudesRechazadas.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "RECHAZADAS",
                                    count = solicitudesRechazadas.size,
                                    color = EduRachaColors.Error
                                )
                            }

                            items(
                                items = solicitudesRechazadas,
                                key = { it.id ?: it.estudianteId }
                            ) { solicitud ->
                                SolicitudCardReadOnly(
                                    solicitud = solicitud,
                                    estado = EstadoSolicitud.RECHAZADA
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoAceptar && solicitudParaResponder != null) {
        DialogoResponderSolicitud(
            solicitud = solicitudParaResponder!!,
            esAceptar = true,
            onConfirm = { mensaje ->
                viewModel.aceptarSolicitud(solicitudParaResponder!!.id ?: "", mensaje)
                mostrarDialogoAceptar = false
                solicitudParaResponder = null
            },
            onDismiss = {
                mostrarDialogoAceptar = false
                solicitudParaResponder = null
            }
        )
    }

    if (mostrarDialogoRechazar && solicitudParaResponder != null) {
        DialogoResponderSolicitud(
            solicitud = solicitudParaResponder!!,
            esAceptar = false,
            onConfirm = { mensaje ->
                viewModel.rechazarSolicitud(solicitudParaResponder!!.id ?: "", mensaje)
                mostrarDialogoRechazar = false
                solicitudParaResponder = null
            },
            onDismiss = {
                mostrarDialogoRechazar = false
                solicitudParaResponder = null
            }
        )
    }
}

@Composable
fun AsignarEstudiantesHeader(
    cursoTitulo: String,
    cursoCodigo: String,
    totalPendientes: Int,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                if (totalPendientes > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EduRachaColors.Error
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "$totalPendientes pendiente${if (totalPendientes > 1) "s" else ""}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = cursoTitulo,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = cursoCodigo,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gestiona las solicitudes de estudiantes",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SolicitudCard(
    solicitud: SolicitudCurso,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EduRachaColors.Primary,
                                    EduRachaColors.Secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = solicitud.estudianteNombre.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solicitud.estudianteNombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = solicitud.estudianteEmail,
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            if (!solicitud.mensaje.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Background
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                tint = EduRachaColors.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Mensaje:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRachaColors.Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = solicitud.mensaje,
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = EduRachaColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Solicitud enviada: ${solicitud.fechaSolicitud}",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Error.copy(alpha = 0.1f)
                    ),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = EduRachaColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Rechazar",
                        color = EduRachaColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAceptar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduRachaColors.Success
                    ),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SolicitudCardReadOnly(
    solicitud: SolicitudCurso,
    estado: EstadoSolicitud
) {
    val (backgroundColor, iconColor, estadoTexto, icon) = when (estado) {
        EstadoSolicitud.ACEPTADA -> Quad(
            EduRachaColors.Success.copy(alpha = 0.05f),
            EduRachaColors.Success,
            "Aceptada",
            Icons.Default.CheckCircle
        )
        EstadoSolicitud.RECHAZADA -> Quad(
            EduRachaColors.Error.copy(alpha = 0.05f),
            EduRachaColors.Error,
            "Rechazada",
            Icons.Default.Cancel
        )
        else -> Quad(
            Color.Gray.copy(alpha = 0.05f),
            Color.Gray,
            "Pendiente",
            Icons.Default.HourglassEmpty
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = solicitud.estudianteNombre.firstOrNull()?.uppercase() ?: "?",
                        color = iconColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solicitud.estudianteNombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = solicitud.estudianteEmail,
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = estadoTexto,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
            }

            if (!solicitud.fechaRespuesta.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = EduRachaColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Respondida: ${solicitud.fechaRespuesta}",
                        fontSize = 11.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoResponderSolicitud(
    solicitud: SolicitudCurso,
    esAceptar: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var mensaje by remember {
        mutableStateOf(
            if (esAceptar)
                "¡Bienvenido al curso! Tu solicitud ha sido aceptada."
            else
                "Lo sentimos, tu solicitud no pudo ser aceptada en este momento."
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (esAceptar)
                                    EduRachaColors.Success.copy(alpha = 0.15f)
                                else
                                    EduRachaColors.Error.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (esAceptar) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (esAceptar) EduRachaColors.Success else EduRachaColors.Error,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (esAceptar) "Aceptar Solicitud" else "Rechazar Solicitud",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            text = solicitud.estudianteNombre,
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Mensaje para el estudiante:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mensaje,
                    onValueChange = { mensaje = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe un mensaje...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (esAceptar) EduRachaColors.Success else EduRachaColors.Error,
                        unfocusedBorderColor = EduRachaColors.Background
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EduRachaColors.Background)
                    ) {
                        Text(
                            "Cancelar",
                            color = EduRachaColors.TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onConfirm(mensaje) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esAceptar) EduRachaColors.Success else EduRachaColors.Error
                        )
                    ) {
                        Icon(
                            imageVector = if (esAceptar) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (esAceptar) "Aceptar" else "Rechazar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateSolicitudes() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            isVisible = true
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn() + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAddDisabled,
                    contentDescription = null,
                    tint = EduRachaColors.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sin solicitudes",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "No hay solicitudes pendientes para este curso en este momento",
            fontSize = 15.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
        )
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)