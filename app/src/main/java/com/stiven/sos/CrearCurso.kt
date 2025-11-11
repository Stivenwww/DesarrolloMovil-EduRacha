package com.stiven.sos

import android.content.Context
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stiven.sos.models.CursoRequest
import com.stiven.sos.models.GenerarExplicacionRequest
import com.stiven.sos.models.TemaRequest
import com.stiven.sos.models.UserPreferences
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
                        Toast.makeText(this, "Curso creado exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
        }
    }
}

// ============================================
// MODELOS DE DATOS
// ============================================

data class TemaTemp(
    val id: String = "tema_${System.currentTimeMillis()}",
    val titulo: String,
    val contenido: String,
    val archivoUrl: String = "",
    val archivoNombre: String = "",
    val tipo: String = "texto",
    val explicacion: String = "",
    val explicacionFuente: String = "",
    val explicacionEstado: String = "pendiente",
    val fechaCreacion: String = obtenerFechaActual(),
    val explicacionUltimaActualizacion: String = obtenerFechaActual()
)

// ============================================
// FUNCIONES AUXILIARES
// ============================================

fun obtenerFechaActual(): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formato.format(Date())
}

private suspend fun simularSubidaPdf(context: Context, uri: Uri): Result<String> {
    return withContext(Dispatchers.IO) {
        delay(1500)
        Result.success("https://example.com/${obtenerNombreArchivo(context, uri)}")
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

// ============================================
// PANTALLA PRINCIPAL
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCursoConTemasScreen(
    onNavigateBack: () -> Unit,
    onCursoCreado: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val docenteId = remember { UserPreferences.getUserUid(context) }

    // 🔍 DEBUG: Verificar el valor del docenteId (se ejecuta solo una vez)
    LaunchedEffect(Unit) {
        Log.d("CREAR_CURSO", "🔍 docenteId obtenido: '$docenteId'")
        if (docenteId.isNullOrBlank()) {
            Log.e("CREAR_CURSO", "❌ ERROR: docenteId es NULL o vacío!")
        }
    }

    // Estados del curso
    var titulo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("activo") }

    // Estados de errores
    var tituloError by remember { mutableStateOf(false) }
    var codigoError by remember { mutableStateOf(false) }
    var descripcionError by remember { mutableStateOf(false) }
    var duracionError by remember { mutableStateOf(false) }

    // Estados de temas
    var temas by remember { mutableStateOf<List<TemaTemp>>(emptyList()) }
    var showAgregarTemaDialog by remember { mutableStateOf(false) }
    var temaParaEditar by remember { mutableStateOf<TemaTemp?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Estados para diálogos informativos
    var showAdvertenciaIADialog by remember { mutableStateOf(false) }
    var showProgresoGeneracionDialog by remember { mutableStateOf(false) }
    var progresoCurrent by remember { mutableStateOf(0) }
    var progresoTotal by remember { mutableStateOf(0) }
    var temaActual by remember { mutableStateOf("") }

    // Fecha actual - calculada una sola vez
    val fechaActual = remember { obtenerFechaActual() }

    // Animación optimizada
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

    // Scaffold principal
    Scaffold(
        //  containerColor movido DENTRO de los paréntesis del Scaffold
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Header con gradiente
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

                    // Vista previa del título
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

                    // Título principal si no hay vista previa
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

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Sección: Información del curso
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
                            label = "Título del curso (Requerido)",
                            icon = Icons.Default.Edit,
                            isError = tituloError,
                            errorMessage = "El título es requerido"
                        )

                        CustomTextField(
                            value = codigo,
                            onValueChange = { codigo = it.uppercase(); codigoError = false },
                            label = "Código del curso (Requerido)",
                            icon = Icons.Default.Badge,
                            isError = codigoError,
                            errorMessage = "El código es requerido"
                        )

                        CustomTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it; descripcionError = false },
                            label = "Descripción del curso (Requerido)",
                            icon = Icons.Default.Description,
                            isError = descripcionError,
                            errorMessage = "La descripción es requerida"
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
                                label = "Duración (días) (Requerido)",
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

                        // Indicador de docente asignado
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = EduRachaColors.Success.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Docente asignado automáticamente",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Text(
                                        "ID: $docenteId",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                }
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = EduRachaColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Sección: Temas del curso
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

                // Lista de temas o mensaje vacío
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
                                "Agrega al menos un tema con explicación para crear el curso",
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
                                    Toast.makeText(context, "Tema eliminado", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Advertencia sobre temas con IA
                val temasConIA = temas.filter { it.explicacionFuente == "ia" }

                if (temasConIA.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EduRachaColors.Accent.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            EduRachaColors.Accent.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    null,
                                    tint = EduRachaColors.Accent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Generación con Inteligencia Artificial",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Text(
                                        "${temasConIA.size} ${if (temasConIA.size == 1) "tema requiere" else "temas requieren"} explicación con IA",
                                        fontSize = 13.sp,
                                        color = EduRachaColors.TextSecondary
                                    )
                                }
                            }

                            Divider(color = EduRachaColors.Accent.copy(alpha = 0.2f))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoItem(
                                        icon = Icons.Default.Schedule,
                                        text = "Tiempo estimado: ${temasConIA.size * 2} minutos",
                                        color = EduRachaColors.Primary
                                    )
                                    InfoItem(
                                        icon = Icons.Default.CloudDone,
                                        text = "El curso se creará inmediatamente",
                                        color = EduRachaColors.Success
                                    )
                                    InfoItem(
                                        icon = Icons.Default.Autorenew,
                                        text = "Si falla, podrás reintentar desde la lista de cursos",
                                        color = EduRachaColors.Accent
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = EduRachaColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        tint = EduRachaColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Recomendación: Puedes cerrar esta pantalla después de crear el curso. Las explicaciones se generarán automáticamente en segundo plano.",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Botón crear curso
                Button(
                    onClick = {
                        // 🔥 1. PRIMERO: Validar docenteId
                        if (docenteId.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                "❌ Error: No se pudo obtener tu ID. Cierra sesión y vuelve a iniciar.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        // 2. Validar campos del formulario
                        tituloError = titulo.isBlank()
                        codigoError = codigo.isBlank()
                        descripcionError = descripcion.isBlank()
                        duracionError = duracionDias.isBlank() || duracionDias.toIntOrNull() == null

                        if (tituloError || codigoError || descripcionError || duracionError) {
                            Toast.makeText(context, "Completa todos los campos obligatorios", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // 3. Validar temas
                        if (temas.isEmpty()) {
                            Toast.makeText(context, "Agrega al menos un tema", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val temasManualesSinExplicacion = temas.filter {
                            it.explicacionFuente == "manual" && it.explicacion.isBlank()
                        }

                        if (temasManualesSinExplicacion.isNotEmpty()) {
                            Toast.makeText(context, "Los temas manuales necesitan explicación", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // 4. Decidir flujo según tipo de temas
                        val temasIA = temas.filter { it.explicacionFuente == "ia" }
                        if (temasIA.isNotEmpty()) {
                            showAdvertenciaIADialog = true
                        } else {
                            // Si no hay temas con IA, crear directamente
                            crearCursoDirectamente(
                                context = context,
                                titulo = titulo,
                                codigo = codigo,
                                descripcion = descripcion,
                                docenteId = docenteId, // ✅ Ya validado como non-null
                                duracionDias = duracionDias,
                                temas = temas,
                                estado = estado,
                                fechaActual = fechaActual,
                                onLoadingChange = { isLoading = it },
                                onCursoCreado = onCursoCreado
                            )
                        }
                    },
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

    // Diálogo de advertencia antes de crear
    if (showAdvertenciaIADialog) {
        AdvertenciaIADialog(
            temasConIA = temas.filter { it.explicacionFuente == "ia" },
            onDismiss = { showAdvertenciaIADialog = false },
            onConfirm = {
                showAdvertenciaIADialog = false

                // 🔥 VALIDACIÓN ADICIONAL del docenteId
                if (docenteId.isNullOrBlank()) {
                    Toast.makeText(
                        context,
                        "❌ Error: ID de docente no encontrado. Cierra sesión y vuelve a iniciar.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@AdvertenciaIADialog
                }

                showProgresoGeneracionDialog = true
                progresoTotal = temas.filter { it.explicacionFuente == "ia" }.size
                progresoCurrent = 0
                temaActual = ""

                crearCursoConIA(
                    context = context,
                    titulo = titulo,
                    codigo = codigo,
                    descripcion = descripcion,
                    docenteId = docenteId, // ✅ Ya validado como non-null
                    duracionDias = duracionDias,
                    temas = temas,
                    estado = estado,
                    fechaActual = fechaActual,
                    onLoadingChange = { isLoading = it },
                    onProgresoChange = { current, total, nombreTema ->
                        progresoCurrent = current
                        progresoTotal = total
                        temaActual = nombreTema
                    },
                    onFinished = {
                        showProgresoGeneracionDialog = false
                        onCursoCreado()
                    }
                )
            }
        )
    }

    // Diálogo de progreso de generación
    if (showProgresoGeneracionDialog) {
        ProgresoGeneracionDialog(
            progresoCurrent = progresoCurrent,
            progresoTotal = progresoTotal,
            temaActual = temaActual
        )
    }

    // Diálogo agregar/editar tema
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
                    Toast.makeText(context, "Tema actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    temas = temas + nuevoTema
                    Toast.makeText(context, "Tema agregado", Toast.LENGTH_SHORT).show()
                }
                showAgregarTemaDialog = false
                temaParaEditar = null
            }
        )
    }
}

// ============================================
// DIÁLOGOS
// ============================================

@Composable
fun AdvertenciaIADialog(
    temasConIA: List<TemaTemp>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Encabezado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EduRachaColors.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Generación con IA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary
                        )
                        Text(
                            "${temasConIA.size} ${if (temasConIA.size == 1) "tema" else "temas"} pendiente(s)",
                            fontSize = 14.sp,
                            color = EduRachaColors.TextSecondary
                        )
                    }
                }

                Divider(color = EduRachaColors.Background, thickness = 1.dp)

                // Información
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "¿Qué va a pasar?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )

                    InfoRow(
                        icon = Icons.Default.CloudDone,
                        text = "El curso se creará inmediatamente",
                        color = EduRachaColors.Success
                    )
                    InfoRow(
                        icon = Icons.Default.Psychology,
                        text = "Se generarán ${temasConIA.size} explicación(es) con IA",
                        color = EduRachaColors.Accent
                    )
                    InfoRow(
                        icon = Icons.Default.Schedule,
                        text = "Tiempo estimado: ${temasConIA.size * 2} minutos",
                        color = EduRachaColors.Primary
                    )
                    InfoRow(
                        icon = Icons.Default.Autorenew,
                        text = "Si falla, se reintentará automáticamente 2 veces",
                        color = EduRachaColors.Primary
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Puedes cerrar esta pantalla durante el proceso",
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EduRachaColors.Accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgresoGeneracionDialog(
    progresoCurrent: Int,
    progresoTotal: Int,
    temaActual: String
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Animación de carga
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = EduRachaColors.Accent,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        Icons.Default.Psychology,
                        null,
                        tint = EduRachaColors.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Generando explicaciones con IA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Progreso: $progresoCurrent de $progresoTotal",
                        fontSize = 14.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }

                // Barra de progreso
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { if (progresoTotal > 0) progresoCurrent.toFloat() / progresoTotal.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EduRachaColors.Accent,
                        trackColor = EduRachaColors.Accent.copy(alpha = 0.2f)
                    )

                    if (temaActual.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = EduRachaColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    null,
                                    tint = EduRachaColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Procesando: $temaActual",
                                    fontSize = 13.sp,
                                    color = EduRachaColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = EduRachaColors.Accent.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = EduRachaColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Este proceso puede tardar hasta 2 minutos por tema. Puedes cerrar esta pantalla de forma segura.",
                            fontSize = 11.sp,
                            color = EduRachaColors.TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

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

    var explicacion by remember { mutableStateOf(temaExistente?.explicacion ?: "") }
    var tipoExplicacion by remember { mutableStateOf(temaExistente?.explicacionFuente?.ifEmpty { "manual" } ?: "manual") }

    var idError by remember { mutableStateOf(false) }
    var tituloError by remember { mutableStateOf(false) }
    var contenidoError by remember { mutableStateOf(false) }
    var explicacionError by remember { mutableStateOf(false) }
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
                            Toast.makeText(context, "PDF cargado correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Error al cargar PDF: ${result.exceptionOrNull()?.message}",
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
                OutlinedTextField(
                    value = temaId,
                    onValueChange = {
                        temaId = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                        idError = false
                    },
                    label = { Text("ID único del Tema (Requerido)") },
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
                            Text("Ej: tema1, intro, fundamentos", fontSize = 11.sp)
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; tituloError = false },
                    label = { Text("Título del Tema (Requerido)") },
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

                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it; contenidoError = false },
                    label = { Text("Contenido del tema (Requerido)") },
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

                Divider(color = EduRachaColors.Background, thickness = 2.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Explicación del Tema (OBLIGATORIA)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { tipoExplicacion = "manual" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipoExplicacion == "manual")
                                EduRachaColors.Primary else EduRachaColors.Background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Manual", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { tipoExplicacion = "ia" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipoExplicacion == "ia")
                                EduRachaColors.Accent else EduRachaColors.Background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("IA", fontSize = 13.sp)
                    }
                }

                if (tipoExplicacion == "manual") {
                    OutlinedTextField(
                        value = explicacion,
                        onValueChange = { explicacion = it; explicacionError = false },
                        label = { Text("Escribe la explicación") },
                        isError = explicacionError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (explicacionError) EduRachaColors.Error else EduRachaColors.Primary,
                            focusedLabelColor = if (explicacionError) EduRachaColors.Error else EduRachaColors.Primary
                        ),
                        supportingText = {
                            if (explicacionError) {
                                Text("La explicación es obligatoria", color = EduRachaColors.Error, fontSize = 12.sp)
                            } else {
                                Text("${explicacion.length}/1000 caracteres", fontSize = 11.sp)
                            }
                        },
                        maxLines = 8
                    )
                }

                if (tipoExplicacion == "ia") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = EduRachaColors.Accent.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            EduRachaColors.Accent.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Psychology,
                                    null,
                                    tint = EduRachaColors.Accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Generación Automática con IA",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduRachaColors.TextPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "La explicación se generará automáticamente después de crear el curso (puede tardar hasta 2 minutos)",
                                        fontSize = 12.sp,
                                        color = EduRachaColors.TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Divider(color = EduRachaColors.Accent.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))

                            ProcesoStep(
                                numero = "1",
                                texto = "El tema se creará con estado 'pendiente'",
                                color = EduRachaColors.Accent
                            )
                            ProcesoStep(
                                numero = "2",
                                texto = "La IA generará la explicación automáticamente",
                                color = EduRachaColors.Accent
                            )
                            ProcesoStep(
                                numero = "3",
                                texto = "Deberás revisar y aprobar antes de generar preguntas",
                                color = EduRachaColors.Primary
                            )
                        }
                    }
                }

                if (explicacion.isBlank() && tipoExplicacion == "manual") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = EduRachaColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = EduRachaColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "La explicación es obligatoria para crear el tema. Los estudiantes la verán antes de iniciar el quiz.",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    tituloError = titulo.isBlank()
                    contenidoError = contenido.isBlank()

                    // Solo validar explicación si es MANUAL
                    explicacionError = if (tipoExplicacion == "manual") {
                        explicacion.isBlank()
                    } else {
                        false
                    }

                    if (temaExistente == null) {
                        idError = temaId.isBlank() || temasExistentes.any { it.id == temaId }
                    }

                    if (!tituloError && !contenidoError && !idError && !explicacionError) {
                        val nuevoTema = TemaTemp(
                            id = temaId.trim(),
                            titulo = titulo.trim(),
                            contenido = contenido.trim(),
                            archivoUrl = archivoUrl.trim(),
                            archivoNombre = archivoNombre.trim(),
                            tipo = if (archivoUrl.isNotEmpty()) "pdf" else "texto",
                            explicacion = if (tipoExplicacion == "manual") explicacion.trim() else "",
                            explicacionFuente = tipoExplicacion,
                            explicacionEstado = "pendiente" // ✅ SIEMPRE PENDIENTE
                        )
                        onConfirm(nuevoTema)
                    } else {
                        if (idError) {
                            Toast.makeText(context, "El ID debe ser único", Toast.LENGTH_SHORT).show()
                        } else if (explicacionError) {
                            Toast.makeText(context, "Debes escribir la explicación", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
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
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f)
    )
}

// ============================================
// COMPONENTES DE UI REUTILIZABLES
// ============================================

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
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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

            if (tema.explicacion.isNotEmpty() || tema.explicacionFuente == "ia") {
                Spacer(Modifier.height(16.dp))
                Divider(color = EduRachaColors.Background)
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Primary.copy(alpha = 0.1f),
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
                                .background(EduRachaColors.Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (tema.explicacionFuente == "ia") Icons.Default.Psychology else Icons.Default.Edit,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = EduRachaColors.Primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Explicación incluida",
                                fontSize = 12.sp,
                                color = EduRachaColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (tema.explicacionFuente == "ia") "Generada con IA" else "Escrita manualmente",
                                fontSize = 15.sp,
                                color = EduRachaColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (tema.explicacionFuente == "ia" && tema.explicacionEstado.isNotEmpty()) {
                                Text(
                                    "Estado: ${tema.explicacionEstado.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 12.sp,
                                    color = when (tema.explicacionEstado) {
                                        "aprobada" -> EduRachaColors.Success
                                        "pendiente" -> EduRachaColors.Accent
                                        "rechazada" -> EduRachaColors.Error
                                        else -> EduRachaColors.TextSecondary
                                    }
                                )
                            }
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            "Explicación incluida",
                            tint = EduRachaColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

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
            label = { Text("Estado (Requerido)") },
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

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = EduRachaColors.TextPrimary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = EduRachaColors.TextPrimary,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProcesoStep(
    numero: String,
    texto: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                numero,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            texto,
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================
// FUNCIONES DE LÓGICA DE NEGOCIO
// ============================================

private fun crearCursoDirectamente(
    context: Context,
    titulo: String,
    codigo: String,
    descripcion: String,
    docenteId: String,
    duracionDias: String,
    temas: List<TemaTemp>,
    estado: String,
    fechaActual: String,
    onLoadingChange: (Boolean) -> Unit,
    onCursoCreado: () -> Unit
) {
    onLoadingChange(true)

    val temasMap = temas.associate { temaTemp ->
        temaTemp.id to TemaRequest(
            id = temaTemp.id,
            titulo = temaTemp.titulo,
            contenido = temaTemp.contenido,
            archivoUrl = temaTemp.archivoUrl.ifEmpty { null },
            tipo = if (temaTemp.archivoUrl.isNotEmpty()) "pdf" else "texto",
            fechaCreacion = fechaActual,
            explicacion = if (temaTemp.explicacionFuente == "manual") temaTemp.explicacion else null,
            explicacionFuente = when {
                temaTemp.explicacionFuente == "manual" -> "docente"
                temaTemp.explicacionFuente == "ia" -> "ia"
                else -> null
            },
            explicacionUltimaActualizacion = if (temaTemp.explicacionFuente == "manual") fechaActual else null,
            explicacionEstado = "pendiente" // ✅ SIEMPRE PENDIENTE
        )
    }

    val cursoParaEnviar = CursoRequest(
        titulo = titulo.trim(),
        codigo = codigo.trim().uppercase(),
        descripcion = descripcion.trim(),
        docenteId = docenteId,
        duracionDias = duracionDias.toInt(),
        temas = temasMap,
        estado = estado,
        fechaCreacion = fechaActual
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = com.stiven.sos.api.ApiClient.apiService.crearCurso(cursoParaEnviar)

            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                if (response.isSuccessful) {
                    Toast.makeText(context, "✅ Curso creado exitosamente", Toast.LENGTH_LONG).show()
                    onCursoCreado()
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun crearCursoConIA(
    context: Context,
    titulo: String,
    codigo: String,
    descripcion: String,
    docenteId: String,
    duracionDias: String,
    temas: List<TemaTemp>,
    estado: String,
    fechaActual: String,
    onLoadingChange: (Boolean) -> Unit,
    onProgresoChange: (Int, Int, String) -> Unit,
    onFinished: () -> Unit
) {
    onLoadingChange(true)

    val temasMap = temas.associate { temaTemp ->
        temaTemp.id to TemaRequest(
            id = temaTemp.id,
            titulo = temaTemp.titulo,
            contenido = temaTemp.contenido,
            archivoUrl = temaTemp.archivoUrl.ifEmpty { null },
            tipo = if (temaTemp.archivoUrl.isNotEmpty()) "pdf" else "texto",
            fechaCreacion = fechaActual,
            explicacion = if (temaTemp.explicacionFuente == "manual") temaTemp.explicacion else null,
            explicacionFuente = when {
                temaTemp.explicacionFuente == "manual" -> "docente"
                temaTemp.explicacionFuente == "ia" -> "ia"
                else -> null
            },
            explicacionUltimaActualizacion = if (temaTemp.explicacionFuente == "manual") fechaActual else null,
            explicacionEstado = "pendiente" // ✅ SIEMPRE PENDIENTE
        )
    }

    val cursoParaEnviar = CursoRequest(
        titulo = titulo.trim(),
        codigo = codigo.trim().uppercase(),
        descripcion = descripcion.trim(),
        docenteId = docenteId,
        duracionDias = duracionDias.toInt(),
        temas = temasMap,
        estado = estado,
        fechaCreacion = fechaActual
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = com.stiven.sos.api.ApiClient.apiService.crearCurso(cursoParaEnviar)

            if (response.isSuccessful && response.body() != null) {
                val cursoId = response.body()!!.id
                val temasConIA = temas.filter { it.explicacionFuente == "ia" }

                if (!cursoId.isNullOrBlank() && temasConIA.isNotEmpty()) {
                    generarExplicacionesConReintentos(
                        context = context,
                        cursoId = cursoId,
                        temasConIA = temasConIA,
                        intentosMaximos = 2,
                        onProgresoChange = onProgresoChange
                    )
                }

                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                    Toast.makeText(context, "✅ Curso creado con explicaciones generadas", Toast.LENGTH_LONG).show()
                    onFinished()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private suspend fun generarExplicacionesConReintentos(
    context: Context,
    cursoId: String,
    temasConIA: List<TemaTemp>,
    intentosMaximos: Int = 2,
    onProgresoChange: (Int, Int, String) -> Unit
) {
    var exitosas = 0
    var fallidas = 0
    var timeouts = 0

    temasConIA.forEachIndexed { index, tema ->
        var intentoActual = 0
        var exito = false

        withContext(Dispatchers.Main) {
            onProgresoChange(index, temasConIA.size, tema.titulo)
        }

        while (intentoActual < intentosMaximos && !exito) {
            intentoActual++

            try {
                val temaId = tema.id
                Log.d("CREAR_CURSO", "🔄 Procesando: $temaId - ${tema.titulo} (intento $intentoActual)")

                withTimeout(120_000L) {
                    val request = GenerarExplicacionRequest(
                        cursoId = cursoId,
                        temaId = temaId,
                        tituloTema = tema.titulo,
                        contenidoTema = tema.contenido
                    )

                    val explicacionResponse = com.stiven.sos.api.ApiClient.apiService.generarExplicacionIA(
                        cursoId = cursoId,
                        temaId = temaId,
                        request = request
                    )

                    when {
                        explicacionResponse.isSuccessful -> {
                            exitosas++
                            exito = true
                            Log.d("CREAR_CURSO", "✅ Explicación generada para $temaId")
                        }
                        else -> {
                            if (intentoActual >= intentosMaximos) {
                                timeouts++
                            } else {
                                delay(5000)
                            }
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                if (intentoActual >= intentosMaximos) {
                    timeouts++
                    Log.e("CREAR_CURSO", "⏱️ Timeout definitivo para ${tema.id}")
                } else {
                    delay(5000)
                }
            } catch (e: Exception) {
                if (intentoActual >= intentosMaximos) {
                    fallidas++
                }
                Log.e("CREAR_CURSO", "❌ Error para ${tema.id}: ${e.message}")
            }
        }

        withContext(Dispatchers.Main) {
            onProgresoChange(index + 1, temasConIA.size, tema.titulo)
        }
    }

    Log.d("CREAR_CURSO", "📊 Exitosas: $exitosas | Timeouts: $timeouts | Fallidas: $fallidas")
}


