package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// CORRECCIÓN 1: Añadir la importación que falta para ImageVector
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.*

class PerfilDocenteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                PerfilDocenteScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

// ============================================
// PANTALLA PRINCIPAL DE PERFIL DOCENTE (CORREGIDO)
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilDocenteScreen(onBackClick: () -> Unit) {
    // Obtener datos reales de Firebase
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName ?: "Docente"
    val userEmail = user?.email ?: "correo@uniautonoma.edu.co"
    // ELIMINADO: Ya no se obtiene el teléfono
    // val userPhone = user?.phoneNumber ?: "Sin teléfono"

    val totalAsignaturas = CrearAsignatura.asignaturasGuardadas.size

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.large)
        ) {
            // Header con datos del docente
            item {
                PerfilDocenteHeader(
                    userName = userName,
                    userEmail = userEmail,
                    onBackClick = onBackClick
                )
            }

            item { Spacer(modifier = Modifier.height(Spacing.large)) }

            // Sección de estadísticas simplificada.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding)
                ) {
                    Text(
                        text = "Estadísticas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )

                    // Solo se muestra la tarjeta de asignaturas
                    EduRachaStatsCard(
                        title = "Asignaturas Creadas",
                        value = totalAsignaturas.toString(),
                        icon = Icons.Default.School,
                        iconBackgroundColor = EduRachaColors.Primary,
                        iconTint = EduRachaColors.Primary,
                        modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.large)) }

            // Información del docente (sin cambios)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding)
                ) {
                    Text(
                        text = "Información de la Cuenta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )

                    EduRachaCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            InfoDocenteItem(
                                icon = Icons.Default.Person,
                                label = "Nombre completo",
                                value = userName
                            )
                            EduRachaDivider()
                            InfoDocenteItem(
                                icon = Icons.Default.Email,
                                label = "Correo electrónico",
                                value = userEmail
                            )

                            // --- BLOQUE DEL TELÉFONO ELIMINADO ---
                            // EduRachaDivider()
                            // InfoDocenteItem(
                            //     icon = Icons.Default.Phone,
                            //     label = "Teléfono",
                            //     value = userPhone
                            // )
                            // ------------------------------------

                            EduRachaDivider()
                            InfoDocenteItem(
                                icon = Icons.Default.AccountCircle,
                                label = "Rol",
                                value = "Docente"
                            )
                            EduRachaDivider()
                            InfoDocenteItem(
                                icon = Icons.Default.School,
                                label = "Institución",
                                value = "UNIAUTÓNOMA"
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.large)) }

            // Footer institucional
            item {
                FooterInstitucional()
            }
        }
    }
}

// ============================================
// HEADER DEL PERFIL DOCENTE (CORREGIDO)
// ============================================
@Composable
fun PerfilDocenteHeader(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = Elevation.medium),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.GradientStart,
                            EduRachaColors.GradientEnd
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.screenPadding)
            ) {
                // Botón de volver
                IconButton(
                    onClick = onBackClick,
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

                Spacer(modifier = Modifier.height(Spacing.large))

                // Avatar y datos del docente
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(
                                elevation = Elevation.large,
                                shape = CircleShape
                            )
                            .border(
                                width = 4.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = EduRachaColors.Primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Nombre
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.extraSmall))

                    // Email
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                }
            }
        }
    }
}


// ============================================
// ITEM DE INFORMACIÓN DEL DOCENTE
// ============================================
@Composable
fun InfoDocenteItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = EduRachaColors.PrimaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

// ============================================
// FOOTER INSTITUCIONAL
// ============================================
@Composable
fun FooterInstitucional() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenPadding),
        shape = CustomShapes.Card,
        color = EduRachaColors.PrimaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = "Corporación Universitaria",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Autónoma del Cauca",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "EduRacha - Sistema de Gestión Académica",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = EduRachaColors.TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}


