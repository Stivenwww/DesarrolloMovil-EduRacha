package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.EduRachaCard
import com.stiven.desarrollomovil.ui.theme.components.EduRachaEmptyState
import com.stiven.desarrollomovil.ui.theme.components.EduRachaTopAppBar

// ============================================
// ACTIVITY MEJORADA A JETPACK COMPOSE
// ============================================
class SeleccionarAsignaturaValidacionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                SeleccionarAsignaturaScreen(
                    onNavigateBack = { finish() },
                    onAsignaturaSelected = { asignatura ->
                        abrirValidacionPreguntas(asignatura)
                    }
                )
            }
        }
    }

    private fun abrirValidacionPreguntas(asignatura: String) {
        val intent = Intent(this, ValidacionPreguntasActivity::class.java).apply {
            putExtra("ASIGNATURA", asignatura)
        }
        startActivity(intent)
    }
}

// ============================================
// PANTALLA EN JETPACK COMPOSE
// ============================================
@Composable
fun SeleccionarAsignaturaScreen(
    onNavigateBack: () -> Unit,
    onAsignaturaSelected: (String) -> Unit
) {
    val asignaturasConPendientes = remember {
        PreguntasIARepository.obtenerAsignaturasConPreguntasPendientes()
    }

    Scaffold(
        topBar = {
            EduRachaTopAppBar(
                title = "Validar Preguntas",
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            // 1. Header Card
            item {
                HeaderCard()
            }

            // 2. Sección de Lista
            if (asignaturasConPendientes.isNotEmpty()) {
                item {
                    SectionHeader()
                }

                // 3. Lista de Asignaturas
                items(asignaturasConPendientes, key = { it }) { asignaturaNombre ->
                    val pendientesCount = remember {
                        PreguntasIARepository.contarPreguntasPendientesPorAsignatura(asignaturaNombre)
                    }
                    AsignaturaSeleccionCard(
                        nombreAsignatura = asignaturaNombre,
                        pendientesCount = pendientesCount,
                        onClick = { onAsignaturaSelected(asignaturaNombre) }
                    )
                }
            } else {
                // 4. Estado Vacío
                item {
                    EduRachaEmptyState(
                        icon = Icons.Default.Verified,
                        title = "¡Felicitaciones!",
                        description = "No hay preguntas pendientes de validación en ninguna asignatura.",
                        modifier = Modifier.padding(top = Spacing.extraLarge)
                    )
                }
            }
        }
    }
}

// ============================================
// COMPONENTES DE LA PANTALLA (CORREGIDOS)
// ============================================

@Composable
private fun HeaderCard() {
    // CAMBIO: Se elimina el parámetro `containerColor` y `borderColor` de EduRachaCard
    // Se asume que EduRachaCard ya tiene un color de fondo predeterminado (normalmente blanco o Surface)
    EduRachaCard {
        Column(modifier = Modifier.padding(Spacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // CAMBIO: Se reemplaza el inexistente 'EduRachaIcon' por un componente estándar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.WarningContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Icono de Validación",
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.medium))
                Column {
                    Text(
                        text = "Validación de Preguntas",
                        style = MaterialTheme.typography.titleLarge,
                        color = EduRachaColors.TextPrimary
                    )
                    Text(
                        text = "Revisa preguntas generadas por IA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.medium))

            // CAMBIO: Se reemplaza el inexistente 'EduRachaInfoBox' por un componente estándar
            Surface(
                shape = CustomShapes.Card,
                color = EduRachaColors.WarningContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Información",
                        tint = EduRachaColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = "Selecciona una asignatura para revisar sus preguntas pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Spacing.small)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(EduRachaColors.Warning)
        )
        Spacer(modifier = Modifier.width(Spacing.medium))
        Text(
            text = "ASIGNATURAS CON PREGUNTAS PENDIENTES",
            style = MaterialTheme.typography.labelLarge,
            color = EduRachaColors.Warning,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AsignaturaSeleccionCard(
    nombreAsignatura: String,
    pendientesCount: Int,
    onClick: () -> Unit
) {
    // CAMBIO: Se reemplaza el `EduRachaCompactCard` por una tarjeta personalizada
    // que no depende del parámetro `badgeCount`.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.small),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = EduRachaColors.Primary
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombreAsignatura,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = if (pendientesCount == 1) "1 pregunta pendiente" else "$pendientesCount preguntas pendientes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EduRachaColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Badge para el contador
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                    .clip(CircleShape)
                    .background(EduRachaColors.Warning),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pendientesCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
