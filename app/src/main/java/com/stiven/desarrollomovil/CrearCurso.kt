package com.stiven.desarrollomovil

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import com.stiven.desarrollomovil.models.Curso

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.appdistribution.gradle.ApiService
import com.stiven.desarrollomovil.models.Tema
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Object para mantener los cursos guardados
object CrearCursoObject {
    val cursosGuardados = mutableListOf<Curso>()
}

class CrearCursoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                CrearCursoScreen(
                    onNavigateBack = { finish() },
                    onCursoCreado = { curso ->
                        CrearCursoObject.cursosGuardados.add(curso)
                        mostrarDialogoExito(curso)
                    }
                )
            }
        }
    }

    private fun mostrarDialogoExito(curso: Curso) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¡Curso creado!")
            .setMessage("${curso.titulo} se ha guardado exitosamente.\n\nTotal: ${CrearCursoObject.cursosGuardados.size} cursos")
            .setPositiveButton("Ver lista") { _, _ ->
                startActivity(Intent(this, ListaCursosActivity::class.java))
                finish()
            }
            .setNegativeButton("Crear otro") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Volver") { _, _ -> finish() }
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCursoScreen(
    onNavigateBack: () -> Unit,
    onCursoCreado: (Curso) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var titulo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var docenteId by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("activo") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfNombre by remember { mutableStateOf("") }

    var tituloError by remember { mutableStateOf(false) }
    var codigoError by remember { mutableStateOf(false) }
    var descripcionError by remember { mutableStateOf(false) }
    var docenteIdError by remember { mutableStateOf(false) }
    var duracionError by remember { mutableStateOf(false) }

    val imagenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imagenUri = it
                Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                pdfUri = it
                pdfNombre = obtenerNombreArchivo(context, it)
                Toast.makeText(context, "PDF adjuntado: $pdfNombre", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun validarYGuardar(
        context: Context,
        titulo: String,
        codigo: String,
        descripcion: String,
        docenteId: String,
        duracionDias: String,
        pdfUri: Uri?,
        estado: String,
        onCursoCreado: (Curso) -> Unit
    ) {
        var tituloError = titulo.isBlank()
        var codigoError = codigo.isBlank()
        var descripcionError = descripcion.isBlank()
        var docenteIdError = docenteId.isBlank()
        var duracionError = duracionDias.isBlank() || duracionDias.toIntOrNull() == null || duracionDias.toInt() < 1

        if (tituloError || codigoError || descripcionError || docenteIdError || duracionError) {
            Toast.makeText(context, "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        val temasMap = if (pdfUri != null) {
            mapOf(
                "tema_1" to Tema(
                    id = "tema_1",
                    titulo = "Material del curso",
                    contenido = descripcion,
                    archivoUrl = pdfUri.toString(),
                    tipo = "pdf",
                    fechaCreacion = System.currentTimeMillis().toString()
                )
            )
        } else null

        val curso = Curso(
            id = null,
            titulo = titulo.trim(),
            codigo = codigo.trim().uppercase(),
            descripcion = descripcion.trim(),
            docenteId = docenteId.trim(),
            duracionDias = duracionDias.toInt(),
            temas = temasMap,
            estado = estado,
            fechaCreacion = System.currentTimeMillis().toString()
        )

        // 🚀 Llamada real al backend con corrutinas
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = com.stiven.desarrollomovil.api.ApiClient.apiService.crearCurso(curso)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Curso creado correctamente", Toast.LENGTH_SHORT).show()
                        onCursoCreado(curso)
                    } else {
                        Toast.makeText(context, "Error al crear el curso: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No se pudo conectar al servidor", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CrearCursoHeader(
                onNavigateBack = onNavigateBack,
                totalCursos = CrearCursoObject.cursosGuardados.size
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ImagenCursoCard(
                    imagenUri = imagenUri,
                    nombreCurso = titulo.ifEmpty { "Nuevo curso" },
                    onImagenClick = { imagenLauncher.launch("image/*") }
                )

                SectionHeader("INFORMACIÓN BÁSICA", Icons.Default.Info, EduRachaColors.Primary)

                InformacionBasicaCursoCard(
                    titulo = titulo,
                    onTituloChange = { titulo = it; tituloError = false },
                    tituloError = tituloError,
                    codigo = codigo,
                    onCodigoChange = { codigo = it; codigoError = false },
                    codigoError = codigoError,
                    docenteId = docenteId,
                    onDocenteIdChange = { docenteId = it; docenteIdError = false },
                    docenteIdError = docenteIdError,
                    duracionDias = duracionDias,
                    onDuracionChange = { duracionDias = it; duracionError = false },
                    duracionError = duracionError,
                    estado = estado,
                    onEstadoChange = { estado = it },
                    estadoError = false
                )

                SectionHeader("DESCRIPCIÓN DEL CURSO", Icons.Default.Description, EduRachaColors.Secondary)

                DescripcionCursoCard(
                    descripcion = descripcion,
                    onDescripcionChange = { descripcion = it; descripcionError = false },
                    descripcionError = descripcionError,
                    pdfNombre = pdfNombre,
                    onAgregarPdf = { pdfLauncher.launch("application/pdf") },
                    onEliminarPdf = { pdfUri = null; pdfNombre = "" }
                )

                BotonCrearCurso(onClick = { validarYGuardar(
                    context = context,
                    titulo = titulo,
                    codigo = codigo,
                    descripcion = descripcion,
                    docenteId = docenteId,
                    duracionDias = duracionDias,
                    pdfUri = pdfUri,
                    estado = estado,
                    onCursoCreado = { nuevoCurso ->
                        CrearCursoObject.cursosGuardados.add(nuevoCurso)
                    }
                )
                 })

                FooterInfo()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun CrearCursoHeader(onNavigateBack: () -> Unit, totalCursos: Int) {
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

                if (totalCursos > 0) {
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
                                Icons.Default.School,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "$totalCursos",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Crear Curso",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                Text(
                    "Configura un nuevo curso",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ImagenCursoCard(imagenUri: Uri?, nombreCurso: String, onImagenClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Primary.copy(alpha = 0.1f))
                    .border(2.dp, EduRachaColors.Primary, CircleShape)
                    .clickable(onClick = onImagenClick),
                contentAlignment = Alignment.Center
            ) {
                if (imagenUri != null) {
                    AsyncImage(
                        model = imagenUri,
                        contentDescription = "Imagen del curso",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, "Agregar imagen", tint = EduRachaColors.Primary, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(nombreCurso, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Primary, textAlign = TextAlign.Center)
            Text("Toca el círculo para agregar una imagen", fontSize = 13.sp, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(4.dp).height(24.dp).background(color, RoundedCornerShape(2.dp)))
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
    }
}

@Composable
fun InformacionBasicaCursoCard(
    titulo: String, onTituloChange: (String) -> Unit, tituloError: Boolean,
    codigo: String, onCodigoChange: (String) -> Unit, codigoError: Boolean,
    docenteId: String, onDocenteIdChange: (String) -> Unit, docenteIdError: Boolean,
    duracionDias: String, onDuracionChange: (String) -> Unit, duracionError: Boolean,
    estado: String, onEstadoChange: (String) -> Unit, estadoError: Boolean
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CustomTextField(titulo, onTituloChange, "Título del curso", Icons.Default.Edit, tituloError, "El título es requerido")
            CustomTextField(codigo, onCodigoChange, "Código del curso", Icons.Default.Badge, codigoError, "El código es requerido")
            CustomTextField(docenteId, onDocenteIdChange, "ID del docente", Icons.Default.Person, docenteIdError, "El ID del docente es requerido")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(duracionDias, onDuracionChange, "Duración (días)", Icons.Default.CalendarToday, duracionError, "Requerido", Modifier.weight(1f))
                EstadoDropdown(estado, onEstadoChange, estadoError, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector,
    isError: Boolean = false, errorMessage: String = "", modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            value, onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, tint = if (isError) EduRachaColors.Error else EduRachaColors.Primary) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Primary,
                focusedLabelColor = EduRachaColors.Primary,
                cursorColor = EduRachaColors.Primary,
                errorBorderColor = EduRachaColors.Error,
                errorLabelColor = EduRachaColors.Error
            ),
            singleLine = true
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(errorMessage, color = EduRachaColors.Error, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoDropdown(
    estado: String, onEstadoChange: (String) -> Unit, isError: Boolean, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val estados = listOf("activo", "inactivo", "borrador", "archivado")
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            estado, {},
            readOnly = true, label = { Text("Estado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = { Icon(Icons.Default.ToggleOn, null, tint = if (isError) EduRachaColors.Error else EduRachaColors.Accent) },
            isError = isError,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Accent,
                focusedLabelColor = EduRachaColors.Accent,
                errorBorderColor = EduRachaColors.Error,
                errorLabelColor = EduRachaColors.Error
            )
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            estados.forEach { item ->
                DropdownMenuItem({ Text(item) }, onClick = { onEstadoChange(item); expanded = false })
            }
        }
    }
}

@Composable
fun DescripcionCursoCard(
    descripcion: String, onDescripcionChange: (String) -> Unit, descripcionError: Boolean,
    pdfNombre: String, onAgregarPdf: () -> Unit, onEliminarPdf: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = EduRachaColors.Secondary.copy(alpha = 0.1f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = EduRachaColors.Secondary, modifier = Modifier.size(20.dp))
                    Text("Describe los contenidos y objetivos del curso", fontSize = 13.sp, color = EduRachaColors.TextSecondary)
                }
            }
            OutlinedTextField(
                descripcion, { if (it.length <= 1000) onDescripcionChange(it) },
                label = { Text("Descripción del curso") }, isError = descripcionError,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EduRachaColors.Secondary,
                    focusedLabelColor = EduRachaColors.Secondary,
                    cursorColor = EduRachaColors.Secondary
                ),
                supportingText = { Text("${descripcion.length}/1000 caracteres") }
            )
            if (pdfNombre.isEmpty()) {
                OutlinedButton(onAgregarPdf, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = EduRachaColors.Secondary)) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adjuntar material PDF (opcional)")
                }
            } else {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = EduRachaColors.Success.copy(alpha = 0.1f), border = ButtonDefaults.outlinedButtonBorder) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = EduRachaColors.Success, modifier = Modifier.size(24.dp))
                            Text(pdfNombre, fontSize = 14.sp, color = EduRachaColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton({ onEliminarPdf() }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Eliminar PDF", tint = EduRachaColors.Error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BotonCrearCurso(onClick: () -> Unit) {
    Button(
        onClick, Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EduRachaColors.Primary),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text("Crear Curso", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FooterInfo() {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = EduRachaColors.Primary.copy(alpha = 0.05f)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.School, null, tint = EduRachaColors.Primary, modifier = Modifier.size(32.dp))
            Text("EduRacha UNIAUTÓNOMA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.Primary, textAlign = TextAlign.Center)
            Text("Sistema de gestión académica gamificado", fontSize = 12.sp, color = EduRachaColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

fun obtenerNombreArchivo(context: android.content.Context, uri: Uri): String {
    var nombre = "archivo.pdf"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                nombre = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {
        nombre = uri.lastPathSegment ?: nombre
    }
    return nombre
}