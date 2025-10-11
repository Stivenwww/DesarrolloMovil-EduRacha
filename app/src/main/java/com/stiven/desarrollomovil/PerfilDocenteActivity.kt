package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.ui.theme.*
import com.stiven.desarrollomovil.ui.theme.components.*
import com.stiven.desarrollomovil.viewmodel.CursoViewModel

class PerfilDocenteActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduRachaTheme {
                PerfilDocenteScreen(
                    viewModel = cursoViewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

// ============================================
// PANTALLA PRINCIPAL DE PERFIL DOCENTE
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilDocenteScreen(
    viewModel: CursoViewModel,
    onBackClick: () -> Unit
) {
    // Obtener datos reales de Firebase
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName ?: "Docente"
    val userEmail = user?.email ?: "correo@uniautonoma.edu.co"

    // Observar datos del ViewModel
    val cursos by viewModel.cursos.collectAsState()

    // Cargar cursos al iniciar
    LaunchedEffect(Unit) {
        viewModel.obtenerCursos()
    }

    // Calcular estadísticas desde la API
    val totalCursos = cursos.size
    val cursosActivos = cursos.count { it.estado == "activo" }
    val cursosBorrador = cursos.count { it.estado == "borrador" }

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

            // Sección de estadísticas de cursos
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding)
                ) {
                    Text(
                        text = "Estadísticas de Cursos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )

                    // Tarjetas de estadísticas en fila
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total de cursos
                        EduRachaCompactStatsCard(
                            title = "Total",
                            value = totalCursos.toString(),
                            icon = Icons.Default.School,
                            iconColor = EduRachaColors.Primary,
                            modifier = Modifier.weight(1f)
                        )

                        // Cursos activos
                        EduRachaCompactStatsCard(
                            title = "Activos",
                            value = cursosActivos.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = EduRachaColors.Success,
                            modifier = Modifier.weight(1f)
                        )

                        // Cursos en borrador
                        EduRachaCompactStatsCard(
                            title = "Borradores",
                            value = cursosBorrador.toString(),
                            icon = Icons.Default.Edit,
                            iconColor = EduRachaColors.Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.large)) }

            // Información del docente
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

            // Resumen de cursos por estado
            if (totalCursos > 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenPadding)
                    ) {
                        Text(
                            text = "Resumen de Cursos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.TextPrimary,
                            modifier = Modifier.padding(bottom = Spacing.medium)
                        )

                        EduRachaCard {
                            Column(
                                modifier = Modifier.padding(Spacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                ResumenCursoRow(
                                    estado = "Activo",
                                    cantidad = cursosActivos,
                                    color = EduRachaColors.Success,
                                    total = totalCursos
                                )
                                ResumenCursoRow(
                                    estado = "Borrador",
                                    cantidad = cursosBorrador,
                                    color = EduRachaColors.Warning,
                                    total = totalCursos
                                )
                                ResumenCursoRow(
                                    estado = "Inactivo",
                                    cantidad = totalCursos - cursosActivos - cursosBorrador,
                                    color = EduRachaColors.TextSecondary,
                                    total = totalCursos
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(Spacing.large)) }
            }

            // Footer institucional
            item {
                FooterInstitucional()
            }
        }
    }
}

// ============================================
// HEADER DEL PERFIL DOCENTE
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
// TARJETA COMPACTA DE ESTADÍSTICAS
// ============================================
@Composable
fun EduRachaCompactStatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CustomShapes.Card,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.small)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EduRachaColors.TextSecondary
            )
        }
    }
}

// ============================================
// FILA DE RESUMEN DE CURSO
// ============================================
@Composable
fun ResumenCursoRow(
    estado: String,
    cantidad: Int,
    color: Color,
    total: Int
) {
    val porcentaje = if (total > 0) (cantidad * 100) / total else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = estado,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    text = "$cantidad cursos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CustomShapes.Badge)
                    .background(color.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(porcentaje / 100f)
                        .fillMaxHeight()
                        .background(color)
                )
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