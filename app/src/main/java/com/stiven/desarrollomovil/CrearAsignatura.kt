package com.stiven.desarrollomovil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

// IMPORTANTE: Mantener el companion object para compatibilidad con otras activities
class CrearAsignatura : ComponentActivity() {

    companion object {
        val asignaturasGuardadas = mutableListOf<Asignatura>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = EduRachaColors.Background
                ) {
                    CrearAsignaturaScreen(
                        onNavigateBack = { finish() },
                        onAsignaturaCreada = { asignatura ->
                            asignaturasGuardadas.add(asignatura)
                            mostrarDialogoExito(asignatura)
                        }
                    )
                }
            }
        }
    }

    private fun mostrarDialogoExito(asignatura: Asignatura) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¡Asignatura creada!")
            .setMessage("${asignatura.nombre} se ha guardado exitosamente.\n\nTotal: ${asignaturasGuardadas.size} asignaturas")
            .setPositiveButton("Ver lista") { _, _ ->
                startActivity(Intent(this, ListaAsignaturasActivity::class.java))
                finish()
            }
            .setNegativeButton("Crear otra") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Volver") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearAsignaturaScreen(
    onNavigateBack: () -> Unit,
    onAsignaturaCreada: (Asignatura) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var semestre by remember { mutableStateOf("") }
    var modalidad by remember { mutableStateOf("") }
    var planAula by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfNombre by remember { mutableStateOf("") }

    var nombreError by remember { mutableStateOf(false) }
    var codigoError by remember { mutableStateOf(false) }
    var semestreError by remember { mutableStateOf(false) }
    var modalidadError by remember { mutableStateOf(false) }
    var planAulaError by remember { mutableStateOf(false) }

    // Launcher para imagen
    val imagenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imagenUri = it
                Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Launcher para PDF
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val mimeType = context.contentResolver.getType(it)
                if (mimeType == "application/pdf") {
                    pdfUri = it
                    pdfNombre = obtenerNombreArchivo(context, it)
                    Toast.makeText(context, "PDF adjuntado: $pdfNombre", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Solo se permiten archivos PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    fun validarYGuardar() {
        nombreError = nombre.isBlank()
        codigoError = codigo.isBlank()
        semestreError = semestre.isBlank() || semestre.toIntOrNull() == null || semestre.toInt() !in 1..12
        modalidadError = modalidad.isBlank()
        planAulaError = planAula.isBlank()

        if (nombreError || codigoError || semestreError || modalidadError || planAulaError) {
            Toast.makeText(
                context,
                "Por favor completa todos los campos requeridos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Guardar el PDF como string si existe
        val pdfUrl = pdfUri?.toString() ?: ""

        // Incluir el PDF en el planAula si existe
        val planAulaCompleto = if (pdfUrl.isNotEmpty()) {
            "$planAula\n[PDF: $pdfNombre]($pdfUrl)"
        } else {
            planAula
        }

        val asignatura = Asignatura(
            nombre = nombre.trim(),
            codigo = codigo.trim().uppercase(),
            semestre = semestre.toInt(),
            modalidad = modalidad,
            planAula = planAulaCompleto,
            imagenUrl = imagenUri?.toString() ?: ""
        )

        onAsignaturaCreada(asignatura)

        // Limpiar formulario
        nombre = ""
        codigo = ""
        semestre = ""
        modalidad = ""
        planAula = ""
        imagenUri = null
        pdfUri = null
        pdfNombre = ""
    }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CrearAsignaturaHeader(
                onNavigateBack = onNavigateBack,
                totalAsignaturas = CrearAsignatura.asignaturasGuardadas.size
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ImagenAsignaturaCard(
                    imagenUri = imagenUri,
                    nombreAsignatura = nombre.ifEmpty { "Nueva asignatura" },
                    onImagenClick = { imagenLauncher.launch("image/*") }
                )

                SectionHeader(
                    "INFORMACIÓN BÁSICA",
                    Icons.Default.Info,
                    EduRachaColors.Primary
                )

                InformacionBasicaCard(
                    nombre = nombre,
                    onNombreChange = { nombre = it; nombreError = false },
                    nombreError = nombreError,
                    codigo = codigo,
                    onCodigoChange = { codigo = it.uppercase(); codigoError = false },
                    codigoError = codigoError,
                    semestre = semestre,
                    onSemestreChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) { semestre = it; semestreError = false } },
                    semestreError = semestreError,
                    modalidad = modalidad,
                    onModalidadChange = { modalidad = it; modalidadError = false },
                    modalidadError = modalidadError
                )

                SectionHeader(
                    "PLAN DE AULA",
                    Icons.Default.Description,
                    EduRachaColors.Secondary
                )

                PlanAulaCard(
                    planAula = planAula,
                    onPlanAulaChange = { planAula = it; planAulaError = false },
                    planAulaError = planAulaError,
                    pdfNombre = pdfNombre,
                    onAgregarPdf = { pdfLauncher.launch("application/pdf") },
                    onEliminarPdf = {
                        pdfUri = null
                        pdfNombre = ""
                    }
                )

                BotonCrearAsignatura(onClick = { validarYGuardar() })

                FooterInfo()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun CrearAsignaturaHeader(onNavigateBack: () -> Unit, totalAsignaturas: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
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
                        Icons.Default.ArrowBack,
                        "Volver",
                        tint = Color.White
                    )
                }

                if (totalAsignaturas > 0) {
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
                                "$totalAsignaturas",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Crear Asignatura",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Configura una nueva materia",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ImagenAsignaturaCard(
    imagenUri: Uri?,
    nombreAsignatura: String,
    onImagenClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
                        contentDescription = "Imagen de la asignatura",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        "Agregar imagen",
                        tint = EduRachaColors.Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                nombreAsignatura,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )

            Text(
                "Toca el círculo para agregar una imagen (opcional)",
                fontSize = 13.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
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
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
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
fun InformacionBasicaCard(
    nombre: String, onNombreChange: (String) -> Unit, nombreError: Boolean,
    codigo: String, onCodigoChange: (String) -> Unit, codigoError: Boolean,
    semestre: String, onSemestreChange: (String) -> Unit, semestreError: Boolean,
    modalidad: String, onModalidadChange: (String) -> Unit, modalidadError: Boolean
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CustomTextField(
                nombre,
                onNombreChange,
                "Nombre de la asignatura *",
                Icons.Default.Edit,
                nombreError,
                "El nombre es requerido"
            )

            CustomTextField(
                codigo,
                onCodigoChange,
                "Código de la asignatura *",
                Icons.Default.Badge,
                codigoError,
                "El código es requerido"
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomTextField(
                    semestre,
                    onSemestreChange,
                    "Semestre *",
                    Icons.Default.CalendarToday,
                    semestreError,
                    "Del 1 al 12",
                    Modifier.weight(1f)
                )

                ModalidadDropdown(
                    modalidad,
                    onModalidadChange,
                    modalidadError,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    errorMessage: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    icon,
                    null,
                    tint = if (isError) EduRachaColors.Error else EduRachaColors.Primary
                )
            },
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
            Text(
                errorMessage,
                color = EduRachaColors.Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalidadDropdown(
    modalidad: String,
    onModalidadChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val modalidades = listOf("Presencial", "Virtual", "Híbrida", "Remota")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = modalidad,
            onValueChange = {},
            readOnly = true,
            label = { Text("Modalidad *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = {
                Icon(
                    Icons.Default.School,
                    null,
                    tint = if (isError) EduRachaColors.Error else EduRachaColors.Accent
                )
            },
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EduRachaColors.Accent,
                focusedLabelColor = EduRachaColors.Accent,
                errorBorderColor = EduRachaColors.Error,
                errorLabelColor = EduRachaColors.Error
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modalidades.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onModalidadChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlanAulaCard(
    planAula: String,
    onPlanAulaChange: (String) -> Unit,
    planAulaError: Boolean,
    pdfNombre: String,
    onAgregarPdf: () -> Unit,
    onEliminarPdf: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EduRachaColors.Secondary.copy(alpha = 0.1f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = EduRachaColors.Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Describe los contenidos y objetivos. Puedes adjuntar un PDF adicional.",
                        fontSize = 13.sp,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = planAula,
                onValueChange = { if (it.length <= 500) onPlanAulaChange(it) },
                label = { Text("Plan de aula *") },
                isError = planAulaError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EduRachaColors.Secondary,
                    focusedLabelColor = EduRachaColors.Secondary,
                    cursorColor = EduRachaColors.Secondary,
                    errorBorderColor = EduRachaColors.Error
                ),
                supportingText = { Text("${planAula.length}/500 caracteres") }
            )

            // Sección de PDF
            if (pdfNombre.isEmpty()) {
                OutlinedButton(
                    onClick = onAgregarPdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EduRachaColors.Secondary
                    )
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Adjuntar PDF (opcional)")
                }
            } else {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EduRachaColors.Success.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        EduRachaColors.Success
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                null,
                                tint = EduRachaColors.Success,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                pdfNombre,
                                fontSize = 14.sp,
                                color = EduRachaColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onEliminarPdf,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Eliminar PDF",
                                tint = EduRachaColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BotonCrearAsignatura(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EduRachaColors.Primary
        ),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Crear Asignatura",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FooterInfo() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EduRachaColors.Primary.copy(alpha = 0.05f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.School,
                null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                "EduRacha UNIAUTÓNOMA",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                "Sistema de gestión académica gamificado",
                fontSize = 12.sp,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
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