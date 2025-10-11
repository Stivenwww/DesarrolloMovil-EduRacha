package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class PanelDocenteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La configuración inicial se maneja en onResume para asegurar que siempre esté actualizada
    }

    override fun onResume() {
        super.onResume()
        // Forzar recomposición si los datos cambian al volver a la pantalla.
        // Esto es clave para que los contadores se actualicen.
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Uniautonoma.Primary)) {
                PanelDocenteScreen(
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    // --- CORRECCIÓN DE NAVEGACIÓN ---
                    onNavigateToNotifications = { navigateTo(NotificacionesActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCreateSubject = { navigateTo(CrearAsignatura::class.java) },
                    onNavigateToValidation = { handleValidationClick() },
                    onNavigateToReports = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToGroups = { navigateTo(GestionGruposActivity::class.java) }, // Corregido
                    onNavigateToStudents = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToSubjects = { handleSubjectsClick() }
                )
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }

    private fun navigateToValidationScreen(asignatura: String) {
        val intent = Intent(this, ValidacionPreguntasActivity::class.java).apply {
            putExtra("ASIGNATURA", asignatura)
        }
        startActivity(intent)
    }

    private fun handleValidationClick() {
        val asignaturasConPendientes = PreguntasIARepository.obtenerAsignaturasConPreguntasPendientes()

        when {
            asignaturasConPendientes.isEmpty() -> {
                Toast.makeText(this, "No hay preguntas pendientes de validación", Toast.LENGTH_SHORT).show()
            }
            asignaturasConPendientes.size == 1 -> {
                navigateToValidationScreen(asignaturasConPendientes[0])
            }
            else -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Seleccionar Asignatura")
                    .setItems(asignaturasConPendientes.toTypedArray()) { _, which ->
                        val asignaturaSeleccionada = asignaturasConPendientes[which]
                        navigateToValidationScreen(asignaturaSeleccionada)
                    }
                    .show()
            }
        }
    }

    private fun handleSubjectsClick() {
        val asignaturas = CrearAsignatura.asignaturasGuardadas
        if (asignaturas.isEmpty()) {
            Toast.makeText(this, "No hay asignaturas creadas", Toast.LENGTH_SHORT).show()
        } else {
            navigateTo(ListaAsignaturasActivity::class.java)
        }
    }
}

// Paleta de colores (la mantengo como la tenías)
object Uniautonoma {
    val Primary = Color(0xFF003D82)
    val PrimaryLight = Color(0xFF1565C0)
    val Secondary = Color(0xFFFFB300)
    val SecondaryVariant = Color(0xFFFF8F00)
    val Accent = Color(0xFF42A5F5)
    val AccentLight = Color(0xFF81D4FA)
    val Success = Color(0xFF2E7D32)
    val SuccessGreen = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF6F00)
    val WarningLight = Color(0xFFFFA726)
    val Error = Color(0xFFD32F2F)
    val Background = Color(0xFFF8F9FA)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF757575)
}

@Composable
fun PanelDocenteScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateSubject: () -> Unit,
    onNavigateToValidation: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    val context = LocalContext.current
    val greeting = remember { getGreeting() }

    val user = FirebaseAuth.getInstance().currentUser
    val userName = remember { user?.displayName?.split(" ")?.firstOrNull() ?: "Zulema" }
    val userEmail = remember { user?.email ?: "zulema@gmail.com" }

    // Se obtienen los valores directamente para que se actualicen en cada recomposición.
    val totalAsignaturas = CrearAsignatura.asignaturasGuardadas.size
    // Se suman las preguntas pendientes de TODAS las asignaturas para obtener el total.
    val preguntasPendientes = CrearAsignatura.asignaturasGuardadas.sumOf { asignatura ->
        PreguntasIARepository.obtenerPreguntasPendientes(asignatura.nombre).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Uniautonoma.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, Uniautonoma.Primary, CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(32.dp),
                        tint = Uniautonoma.Primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = greeting, fontSize = 14.sp, color = Uniautonoma.TextPrimary)
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Uniautonoma.TextPrimary
                    )
                    if (userEmail.isNotEmpty()) {
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = Uniautonoma.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                // --- Iconos de Notificaciones y Ajustes con navegación correcta ---
                Row {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Uniautonoma.TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ajustes",
                            tint = Uniautonoma.TextSecondary
                        )
                    }
                }
            }
        }

        // Stats Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactStatCard(
                label = "Asignaturas",
                value = "$totalAsignaturas",
                icon = Icons.Outlined.MenuBook,
                iconColor = Uniautonoma.Primary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSubjects
            )

            CompactStatCard(
                label = "Pendientes",
                value = "$preguntasPendientes",
                icon = Icons.Outlined.HourglassTop,
                iconColor = Uniautonoma.Warning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToValidation
            )

            CompactStatCard(
                label = "Estudiantes",
                value = "124",
                icon = Icons.Outlined.Groups,
                iconColor = Uniautonoma.SuccessGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToStudents
            )
        }

        // Herramientas principales
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Herramientas principales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.TextPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            MainToolCard(
                title = "Crear asignatura",
                description = "Sube tu plan de aula y configura la materia",
                icon = Icons.Outlined.Add,
                backgroundColor = Uniautonoma.Primary,
                onClick = onNavigateToCreateSubject
            )

            Spacer(modifier = Modifier.height(16.dp))

            MainToolCard(
                title = "Validación IA",
                description = "Revisa y aprueba preguntas generadas por IA",
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = Uniautonoma.Accent,
                onClick = onNavigateToValidation
            )

            Spacer(modifier = Modifier.height(16.dp))

            MainToolCard(
                title = "Reportes y Analytics",
                description = "Visualiza estadísticas de participación",
                icon = Icons.Outlined.BarChart,
                backgroundColor = Uniautonoma.Secondary,
                onClick = onNavigateToReports
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card de Gestión de Grupos con navegación correcta ---
            MainToolCard(
                title = "Gestión de Grupos",
                description = "Organiza y asigna estudiantes a grupos",
                icon = Icons.Outlined.GroupAdd,
                backgroundColor = Uniautonoma.SuccessGreen,
                onClick = onNavigateToGroups
            )
        }
    }
}

// --- El resto de los Composables (CompactStatCard, MainToolCard, etc.) permanecen sin cambios ---
@Composable
fun CompactStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(36.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Uniautonoma.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(40.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
            Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = "Ir", tint = Color.White)
        }
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
