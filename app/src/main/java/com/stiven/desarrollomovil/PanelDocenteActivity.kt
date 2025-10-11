package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme
import java.util.*

class PanelDocenteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setContent {
            EduRachaTheme {
                PanelDocenteScreen(
                    onNavigateToProfile = { navigateTo(PerfilDocenteActivity::class.java) },
                    onNavigateToNotifications = { navigateTo(NotificacionesActivity::class.java) },
                    onNavigateToSettings = { navigateTo(SettingsActivity::class.java) },
                    onNavigateToCreateCourse = { navigateTo(CrearCursoActivity::class.java) },
                    onNavigateToValidation = { handleValidationClick() },
                    onNavigateToReports = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToGroups = { navigateTo(GestionGruposActivity::class.java) },
                    onNavigateToStudents = { navigateTo(ListaEstudiantesActivity::class.java) },
                    onNavigateToCourses = { handleCoursesClick() }
                )
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }

    private fun navigateToValidationScreen(cursoTitulo: String) {
        val intent = Intent(this, ValidacionPreguntasActivity::class.java).apply {
            putExtra("CURSO_TITULO", cursoTitulo)
        }
        startActivity(intent)
    }

    private fun handleValidationClick() {
        // Obtener cursos con preguntas pendientes
        val cursosConPendientes = CrearCursoObject.cursosGuardados
            .filter { curso ->
                PreguntasIARepository.obtenerPreguntasPendientes(curso.titulo).isNotEmpty()
            }
            .map { it.titulo }

        when {
            cursosConPendientes.isEmpty() -> {
                Toast.makeText(this, "No hay preguntas pendientes de validación", Toast.LENGTH_SHORT).show()
            }
            cursosConPendientes.size == 1 -> {
                navigateToValidationScreen(cursosConPendientes[0])
            }
            else -> {
                val items = cursosConPendientes.toTypedArray()
                MaterialAlertDialogBuilder(this)
                    .setTitle("Seleccionar Curso")
                    .setItems(items) { dialog, which ->
                        val cursoSeleccionado = cursosConPendientes[which]
                        navigateToValidationScreen(cursoSeleccionado)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun handleCoursesClick() {
        val cursos = CrearCursoObject.cursosGuardados
        if (cursos.isEmpty()) {
            Toast.makeText(this, "No hay cursos creados aún", Toast.LENGTH_SHORT).show()
        } else {
            navigateTo(ListaCursosActivity::class.java)
        }
    }
}

@Composable
fun PanelDocenteScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCourse: () -> Unit,
    onNavigateToValidation: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToCourses: () -> Unit
) {
    val context = LocalContext.current
    val greeting = remember { getGreeting() }
    val user = FirebaseAuth.getInstance().currentUser
    val userName = remember { user?.displayName?.split(" ")?.firstOrNull() ?: "Docente" }
    val userEmail = remember { user?.email ?: "docente@uniautonoma.edu.co" }

    // Obtener datos actualizados de cursos
    val totalCursos = CrearCursoObject.cursosGuardados.size

    // Sumar todas las preguntas pendientes de todos los cursos
    val preguntasPendientes = CrearCursoObject.cursosGuardados.sumOf { curso ->
        PreguntasIARepository.obtenerPreguntasPendientes(curso.titulo).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EduRachaColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header con datos del usuario
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
                        .border(2.dp, EduRachaColors.Primary, CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(32.dp),
                        tint = EduRachaColors.Primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = greeting, fontSize = 14.sp, color = EduRachaColors.TextPrimary)
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary
                    )
                    if (userEmail.isNotEmpty()) {
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = EduRachaColors.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = EduRachaColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ajustes",
                            tint = EduRachaColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Tarjetas de estadísticas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactStatCard(
                label = "Cursos",
                value = "$totalCursos",
                icon = Icons.Outlined.MenuBook,
                iconColor = EduRachaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCourses
            )
            CompactStatCard(
                label = "Pendientes",
                value = "$preguntasPendientes",
                icon = Icons.Outlined.HourglassTop,
                iconColor = EduRachaColors.Warning,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToValidation
            )
            CompactStatCard(
                label = "Estudiantes",
                value = "124",
                icon = Icons.Outlined.Groups,
                iconColor = EduRachaColors.Success,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToStudents
            )
        }

        // Herramientas principales
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Herramientas principales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MainToolCard(
                title = "Crear Curso",
                description = "Sube tu plan de aula y configura el curso",
                icon = Icons.Outlined.Add,
                backgroundColor = EduRachaColors.Primary,
                onClick = onNavigateToCreateCourse
            )
            MainToolCard(
                title = "Validación IA",
                description = "Revisa y aprueba preguntas generadas por IA",
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = EduRachaColors.Accent,
                onClick = onNavigateToValidation
            )
            MainToolCard(
                title = "Reportes y Analytics",
                description = "Visualiza estadísticas de participación",
                icon = Icons.Outlined.BarChart,
                backgroundColor = EduRachaColors.Secondary,
                onClick = onNavigateToReports
            )
            MainToolCard(
                title = "Gestión de Grupos",
                description = "Organiza y asigna estudiantes a grupos",
                icon = Icons.Outlined.GroupAdd,
                backgroundColor = EduRachaColors.Success,
                onClick = onNavigateToGroups
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

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
                color = EduRachaColors.TextSecondary,
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = "Ir",
                tint = Color.White
            )
        }
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 0..11 -> "Buenos días"
        hour in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}