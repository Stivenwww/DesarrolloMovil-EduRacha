package com.stiven.desarrollomovil

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.models.CursoRequest
import com.stiven.desarrollomovil.models.Tema
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CrearCursoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                CrearCursoConTemasScreen(
                    onNavigateBack = { finish() },
                    onCursoCreado = {
                        Toast.makeText(this, "✅ Curso creado exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
        }
    }
}

// Clase TemaTemp para el UI
data class TemaTemp(
    val id: String,
    val titulo: String,
    val contenido: String,
    val archivoUrl: String,
    val archivoNombre: String,
    val tipo: String = "pdf"
)

// Función para obtener fecha en formato ISO 8601 (YYYY-MM-DD)
fun obtenerFechaActual(): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formato.format(Date())
}

// Simulación de subida de PDF (reemplaza con tu lógica real)
private suspend fun simularSubidaPdf(context: Context, uri: Uri): Result<String> {
    return withContext(Dispatchers.IO) {
        delay(1500)
        // AQUÍ debes implementar la subida real a tu servidor/storage
        Result.success("https://example.com/${obtenerNombreArchivo(context, uri)}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCursoConTemasScreen(
    onNavigateBack: () -> Unit,
    onCursoCreado: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Estados del curso
    var titulo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var docenteId by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("activo") }

    // Estados de validación
    var tituloError by remember { mutableStateOf(false) }
    var codigoError by remember { mutableStateOf(false) }
    var descripcionError by remember { mutableStateOf(false) }
    var docenteIdError by remember { mutableStateOf(false) }
    var duracionError by remember { mutableStateOf(false) }

    // Lista de temas
    var temas by remember { mutableStateOf<List<TemaTemp>>(emptyList()) }
    var showAgregarTemaDialog by remember { mutableStateOf(false) }
    var temaParaEditar by remember { mutableStateOf<TemaTemp?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Fecha actual en tiempo real
    var fechaActual by remember { mutableStateOf(obtenerFechaActual()) }

    // Actualizar fecha cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 60 segundos
            fechaActual = obtenerFechaActual()
        }
    }

    // Animación del libro
    val infiniteTransition = rememberInfiniteTransition(label = "bookAnimation")
    val bookRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bookRotation"
    )

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
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
                            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                        }

                        // Badge con cantidad de temas
                        if (temas.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = EduRachaColors.Secondary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkAdded,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${temas.size} ${if (temas.size == 1) "tema" else "temas"}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Vista previa del curso
                    AnimatedVisibility(
                        visible = titulo.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = EduRachaColors.Secondary,
                                modifier = Modifier
                                    .size(40.dp)
                                    .rotate(bookRotation)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Vista Previa",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    titulo,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (codigo.isNotEmpty()) {
                                    Text(
                                        "Código: $codigo",
                                        color = EduRachaColors.Secondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (titulo.isEmpty()) {
                        Text(
                            "Crear Curso Completo",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Configura el curso y agrega temas",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    fechaActual,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECCIÓN 1: INFORMACIÓN DEL CURSO
                SectionHeader("INFORMACIÓN DEL CURSO", Icons.Default.Info, EduRachaColors.Primary)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CustomTextField(
                            value = titulo,
                            onValueChange = { titulo = it; tituloError = false },
                            label = "Título del curso *",
                            icon = Icons.Default.Edit,
                            isError = tituloError,
                            errorMessage = "El título es requerido"
                        )

                        CustomTextField(
                            value = codigo,
                            onValueChange = { codigo = it.uppercase(); codigoError = false },
                            label = "Código del curso *",
                            icon = Icons.Default.Badge,
                            isError = codigoError,
                            errorMessage = "El código es requerido"
                        )

                        CustomTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it; descripcionError = false },
                            label = "Descripción del curso *",
                            icon = Icons.Default.Description,
                            isError = descripcionError,
                            errorMessage = "La descripción es requerida"
                        )

                        CustomTextField(
                            value = docenteId,
                            onValueChange = { docenteId = it; docenteIdError = false },
                            label = "ID del docente *",
                            icon = Icons.Default.Person,
                            isError = docenteIdError,
                            errorMessage = "El ID del docente es requerido"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CustomTextField(
                                value = duracionDias,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() } && it.length <= 4) {
                                        duracionDias = it
                                        duracionError = false
                                    }
                                },
                                label = "Duración (días) *",
                                icon = Icons.Default.CalendarToday,
                                isError = duracionError,
                                errorMessage = "Requerido",
                                modifier = Modifier.weight(1f)
                            )

                            EstadoDropdown(
                                estado = estado,
                                onEstadoChange = { estado = it },
                                isError = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // SECCIÓN 2: TEMAS DEL CURSO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("TEMAS DEL CURSO", Icons.Default.MenuBook, EduRachaColors.Success)

                    Button(
                        onClick = { showAgregarTemaDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nuevo Tema", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (temas.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Accent.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                null,
                                modifier = Modifier.size(80.dp),
                                tint = EduRachaColors.Accent
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No hay temas agregados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EduRachaColors.TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Agrega al menos un tema con contenido para crear el curso",
                                fontSize = 14.sp,
                                color = EduRachaColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        temas.forEachIndexed { index, tema ->
                            TemaItemCardMejorado(
                                tema = tema,
                                index = index + 1,
                                totalTemas = temas.size,
                                onEditar = { temaParaEditar = tema },
                                onEliminar = {
                                    temas = temas.filter { it.id != tema.id }
                                    Toast.makeText(context, "✓ Tema eliminado", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // BOTÓN CREAR CURSO
                Button(
                    onClick = {
                        // Validaciones (esta parte ya estaba bien)
                        tituloError = titulo.isBlank()
                        codigoError = codigo.isBlank()
                        descripcionError = descripcion.isBlank()
                        docenteIdError = docenteId.isBlank()
                        duracionError = duracionDias.isBlank() || duracionDias.toIntOrNull() == null

                        if (tituloError || codigoError || descripcionError || docenteIdError || duracionError) {
                            Toast.makeText(context, "⚠️ Completa todos los campos obligatorios", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (temas.isEmpty()) {
                            Toast.makeText(context, "⚠️ Agrega al menos un tema", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // --- INICIO DE LA CORRECCIÓN ---

                        // 1. Crear el Mapa de temas como lo espera tu API.
                        val temasMap = temas.associate { temaTemp ->
                            temaTemp.id to Tema(
                                id = temaTemp.id,
                                titulo = temaTemp.titulo,
                                contenido = temaTemp.contenido,
                                archivoUrl = temaTemp.archivoUrl.ifEmpty { "" },
                                tipo = if (temaTemp.archivoUrl.isNotEmpty()) "pdf" else "texto",
                                fechaCreacion = fechaActual
                            )
                        }

// 2. Crear un objeto 'CursoRequest', que es lo que la función 'crearCurso' de tu API espera.
                        val cursoParaEnviar = com.stiven.desarrollomovil.models.CursoRequest(
                            titulo = titulo.trim(),
                            codigo = codigo.trim().uppercase(),
                            descripcion = descripcion.trim(),
                            docenteId = docenteId.trim(),
                            duracionDias = duracionDias.toInt(),
                            temas = temasMap,
                            estado = estado,
                            fechaCreacion = fechaActual
                        )

                        // --- FIN DE LA CORRECCIÓN ---


                        isLoading = true
                        Log.d("CREAR_CURSO", "Enviando curso: ${com.google.gson.Gson().toJson(cursoParaEnviar)}")

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // 3. Llamar a la API con el objeto 'cursoParaEnviar' que tiene el tipo correcto.
                                val response = com.stiven.desarrollomovil.api.ApiClient.apiService.crearCurso(cursoParaEnviar)

                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    if (response.isSuccessful) {
                                        Log.d("CREAR_CURSO", "Curso creado exitosamente")
                                        onCursoCreado()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("CREAR_CURSO", "Error ${response.code()}: $errorBody")
                                        Toast.makeText(
                                            context,
                                            "❌ Error ${response.code()}: Verifica los datos o la conexión",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    Log.e("CREAR_CURSO", "Excepción: ${e.message}", e)
                                    Toast.makeText(
                                        context,
                                        "❌ Error de conexión: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    // ... (el resto de las propiedades del botón se quedan igual)
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {


                if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Creando curso...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Crear Curso Completo", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Diálogo para agregar/editar tema
    if (showAgregarTemaDialog || temaParaEditar != null) {
        AgregarTemaDialog(
            temaExistente = temaParaEditar,
            temasExistentes = temas,
            fechaActual = fechaActual,
            onDismiss = {
                showAgregarTemaDialog = false
                temaParaEditar = null
            },
            onConfirm = { nuevoTema ->
                if (temaParaEditar != null) {
                    temas = temas.map { if (it.id == temaParaEditar!!.id) nuevoTema else it }
                    Toast.makeText(context, "✓ Tema actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    temas = temas + nuevoTema
                    Toast.makeText(context, "✓ Tema agregado", Toast.LENGTH_SHORT).show()
                }
                showAgregarTemaDialog = false
                temaParaEditar = null
            }
        )
    }
}

// Card de tema MEJORADO - MÁS AMPLIO Y VISUAL
@Composable
fun TemaItemCardMejorado(
    tema: TemaTemp,
    index: Int,
    totalTemas: Int,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp), // Más alto
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp) // Más padding
        ) {
            // Header del tema
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Número del tema con gradiente
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EduRachaColors.Success,
                                    EduRachaColors.Success.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$index",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "/$totalTemas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                // Información del tema
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = EduRachaColors.Success.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "ID: ${tema.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tema.titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Botones de acción
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Accent.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Editar",
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Error.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Eliminar",
                            tint = EduRachaColors.Error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = EduRachaColors.Background)
            Spacer(Modifier.height(16.dp))

            // Contenido
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Description,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = EduRachaColors.Secondary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    tema.contenido.ifBlank { "(Sin contenido)" },
                    fontSize = 15.sp,
                    color = if (tema.contenido.isBlank()) EduRachaColors.TextSecondary.copy(alpha = 0.5f) else EduRachaColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // PDF adjunto
            if (tema.archivoUrl.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(color = EduRachaColors.Background)
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Error.copy(alpha = 0.1f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EduRachaColors.Error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = EduRachaColors.Error
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "PDF Adjunto",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                tema.archivoNombre,
                                fontSize = 15.sp,
                                color = EduRachaColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            "PDF incluido",
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// Diálogo mejorado para agregar/editar tema
@Composable
fun AgregarTemaDialog(
    temaExistente: TemaTemp? = null,
    temasExistentes: List<TemaTemp> = emptyList(),
    fechaActual: String,
    onDismiss: () -> Unit,
    onConfirm: (TemaTemp) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var temaId by remember { mutableStateOf(temaExistente?.id ?: "") }
    var titulo by remember { mutableStateOf(temaExistente?.titulo ?: "") }
    var contenido by remember { mutableStateOf(temaExistente?.contenido ?: "") }
    var archivoUrl by remember { mutableStateOf(temaExistente?.archivoUrl ?: "") }
    var archivoNombre by remember { mutableStateOf(temaExistente?.archivoNombre ?: "") }

    var idError by remember { mutableStateOf(false) }
    var tituloError by remember { mutableStateOf(false) }
    var contenidoError by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { pdfUri ->
                isUploading = true
                coroutineScope.launch {
                    val result = simularSubidaPdf(context, pdfUri)
                    withContext(Dispatchers.Main) {
                        isUploading = false
                        if (result.isSuccess) {
                            archivoUrl = result.getOrThrow()
                            archivoNombre = obtenerNombreArchivo(context, pdfUri)
                            Toast.makeText(context, "✅ PDF cargado correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "❌ Error al cargar PDF: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (temaExistente == null) "Agregar Nuevo Tema" else "Editar Tema",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = EduRachaColors.Primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fecha: $fechaActual",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo ID
                OutlinedTextField(
                    value = temaId,
                    onValueChange = {
                        temaId = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                        idError = false
                    },
                    label = { Text("ID único del Tema *") },
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    isError = idError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    readOnly = temaExistente != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (idError) EduRachaColors.Error else EduRachaColors.Primary,
                        focusedLabelColor = if (idError) EduRachaColors.Error else EduRachaColors.Primary
                    ),
                    supportingText = {
                        if (idError) {
                            Text("ID requerido y único", color = EduRachaColors.Error, fontSize = 12.sp)
                        } else if (temaExistente != null) {
                            Text("ID no editable", fontSize = 11.sp)
                        } else {
                            Text("Ej: t1, tema1, intro", fontSize = 11.sp)
                        }
                    },
                    singleLine = true
                )

                // Campo Título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; tituloError = false },
                    label = { Text("Título del Tema *") },
                    leadingIcon = { Icon(Icons.Default.Title, null) },
                    isError = tituloError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (tituloError) EduRachaColors.Error else EduRachaColors.Primary,
                        focusedLabelColor = if (tituloError) EduRachaColors.Error else EduRachaColors.Primary
                    ),
                    supportingText = {
                        if (tituloError) {
                            Text("El título es obligatorio", color = EduRachaColors.Error, fontSize = 12.sp)
                        }
                    },
                    singleLine = true
                )

                // Campo Contenido
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it; contenidoError = false },
                    label = { Text("Contenido del tema *") },
                    isError = contenidoError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (contenidoError) EduRachaColors.Error else EduRachaColors.Secondary,
                        focusedLabelColor = if (contenidoError) EduRachaColors.Error else EduRachaColors.Secondary
                    ),
                    supportingText = {
                        if (contenidoError) {
                            Text("El contenido es obligatorio", color = EduRachaColors.Error, fontSize = 12.sp)
                        } else {
                            Text("${contenido.length}/500 caracteres", fontSize = 11.sp)
                        }
                    },
                    maxLines = 6
                )

                Divider(color = EduRachaColors.Background, thickness = 2.dp)

                // Sección de PDF
                Text(
                    "Archivo PDF (Opcional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )

                if (archivoUrl.isEmpty()) {
                    Button(
                        onClick = { pdfLauncher.launch("application/pdf") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Accent,
                            disabledContainerColor = EduRachaColors.Background
                        ),
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Subiendo PDF...", color = Color.White)
                        } else {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Seleccionar PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { archivoUrl = ""; archivoNombre = "" },
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Success.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            EduRachaColors.Success.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "PDF Adjunto",
                                    fontSize = 11.sp,
                                    color = EduRachaColors.TextSecondary
                                )
                                Text(
                                    archivoNombre.ifEmpty { "Archivo PDF" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRachaColors.Success,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { archivoUrl = ""; archivoNombre = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    "Eliminar",
                                    tint = EduRachaColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (archivoUrl.isEmpty()) {
                    Text(
                        "💡 El PDF es opcional. Puedes agregarlo después.",
                        fontSize = 12.sp,
                        color = EduRachaColors.TextSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validaciones
                    tituloError = titulo.isBlank()
                    contenidoError = contenido.isBlank()

                    if (temaExistente == null) {
                        idError = temaId.isBlank() || temasExistentes.any { it.id == temaId }
                    }

                    if (!tituloError && !contenidoError && !idError) {
                        val nuevoTema = TemaTemp(
                            id = temaId.trim(),
                            titulo = titulo.trim(),
                            contenido = contenido.trim(),
                            archivoUrl = archivoUrl.trim(),
                            archivoNombre = archivoNombre.trim(),
                            tipo = "pdf"
                        )
                        onConfirm(nuevoTema)
                    } else {
                        if (idError) {
                            Toast.makeText(
                                context,
                                "⚠️ El ID debe ser único y no estar vacío",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (tituloError || contenidoError) {
                            Toast.makeText(
                                context,
                                "⚠️ Completa todos los campos obligatorios",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Success),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (temaExistente == null) Icons.Default.Add else Icons.Default.Save,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (temaExistente == null) "Agregar Tema" else "Guardar Cambios",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// Componentes auxiliares
@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean,
    errorMessage: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = if (isError) EduRachaColors.Error else EduRachaColors.Primary
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) EduRachaColors.Error else EduRachaColors.Primary,
            focusedLabelColor = if (isError) EduRachaColors.Error else EduRachaColors.Primary,
            cursorColor = EduRachaColors.Primary,
            errorBorderColor = EduRachaColors.Error,
            errorCursorColor = EduRachaColors.Error,
            errorLeadingIconColor = EduRachaColors.Error
        ),
        singleLine = true,
        supportingText = {
            if (isError) {
                Text(errorMessage, color = EduRachaColors.Error, fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoDropdown(
    estado: String,
    onEstadoChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val estados = listOf("activo", "inactivo", "borrador", "archivado")
    val selectedText = estado.replaceFirstChar { it.uppercase() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Estado *") },
            leadingIcon = {
                Icon(
                    Icons.Default.ToggleOn,
                    null,
                    tint = if (isError) EduRachaColors.Error else EduRachaColors.Accent
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) EduRachaColors.Error else EduRachaColors.Accent,
                focusedLabelColor = if (isError) EduRachaColors.Error else EduRachaColors.Accent
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            estados.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opcion.replaceFirstChar { it.uppercase() },
                            fontWeight = if (opcion == estado) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onEstadoChange(opcion)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = {
                        if (opcion == estado) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun obtenerNombreArchivo(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            uri.lastPathSegment ?: "archivo.pdf"
        }
    } ?: uri.lastPathSegment ?: "archivo.pdf"
}