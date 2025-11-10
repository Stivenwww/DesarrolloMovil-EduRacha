package com.stiven.sos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stiven.sos.models.EstadoPregunta
import com.stiven.sos.ui.theme.*
import com.stiven.sos.viewmodel.CursoViewModel

class PerfilDocenteActivity : ComponentActivity() {

    private val cursoViewModel: CursoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
        //  Establecer contenido en onResume para recargar datos cada vez
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilDocenteScreen(
    viewModel: CursoViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current


    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var userNickname by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)

        userName = prefs.getString("user_name", null) ?: "Docente"
        userEmail = prefs.getString("user_email", null) ?: "correo@uniautonoma.edu.co"
        userRole = prefs.getString("user_role", null) ?: "docente"
        userNickname = prefs.getString("user_nickname", null) ?: ""

        android.util.Log.d("PerfilDocente", "=== DATOS CARGADOS EN PERFIL ===")
        android.util.Log.d("PerfilDocente", "Nombre: $userName")
        android.util.Log.d("PerfilDocente", "Email: $userEmail")
        android.util.Log.d("PerfilDocente", "Rol: $userRole")
        android.util.Log.d("PerfilDocente", "Nickname: $userNickname")
    }

    // Se observa el uiState unificado del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val cursos = uiState.cursos

    // Cargar cursos al iniciar
    LaunchedEffect(Unit) {
        viewModel.obtenerCursos()
    }

    // Calcular estadísticas desde la lista de cursos obtenida
    val totalCursos = cursos.size
    val cursosActivos = cursos.count { it.estado.equals("activo", ignoreCase = true) }
    val cursosBorrador = cursos.count { it.estado.equals("borrador", ignoreCase = true) }
    val cursosInactivos = cursos.count { it.estado.equals("inactivo", ignoreCase = true) || it.estado.equals("archivado", ignoreCase = true) }

    Scaffold(
        containerColor = EduRachaColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header con datos del docente
            item {
                PerfilDocenteHeader(
                    userName = userName,
                    userEmail = userEmail,
                    onBackClick = onBackClick
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Sección de estadísticas de cursos
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Estadísticas de Cursos",
                        style = MaterialTheme.typography.titleLarge,
                        color = EduRachaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Tarjetas de estadísticas en fila
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EduRachaCompactStatsCard(
                            title = "Total",
                            value = totalCursos.toString(),
                            icon = Icons.Default.School,
                            iconColor = EduRachaColors.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        EduRachaCompactStatsCard(
                            title = "Activos",
                            value = cursosActivos.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = EduRachaColors.Success,
                            modifier = Modifier.weight(1f)
                        )
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

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Información del docente
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Información de la Cuenta",
                        style = MaterialTheme.typography.titleLarge,
                        color = EduRachaColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoDocenteItem(Icons.Default.Person, "Nombre completo", userName)
                            Divider(color = EduRachaColors.Border)
                            InfoDocenteItem(Icons.Default.Email, "Correo electrónico", userEmail)
                            Divider(color = EduRachaColors.Border)
                            InfoDocenteItem(Icons.Default.AccountCircle, "Rol", if (userRole == "docente") "Docente" else "Estudiante")

                            if (userNickname.isNotEmpty()) {
                                Divider(color = EduRachaColors.Border)
                                InfoDocenteItem(Icons.Default.Badge, "Nombre de usuario", userNickname)
                            }

                            Divider(color = EduRachaColors.Border)
                            InfoDocenteItem(Icons.Default.School, "Institución", "UNIAUTÓNOMA")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Resumen de cursos por estado
            if (totalCursos > 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Resumen de Cursos",
                            style = MaterialTheme.typography.titleLarge,
                            color = EduRachaColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ResumenCursoRow("Activo", cursosActivos, EduRachaColors.Success, totalCursos)
                                ResumenCursoRow("Borrador", cursosBorrador, EduRachaColors.Warning, totalCursos)
                                ResumenCursoRow("Inactivos/Archivados", cursosInactivos, EduRachaColors.TextSecondary, totalCursos)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Footer institucional
            item {
                FooterInstitucional()
            }
        }
    }
}

@Composable
fun PerfilDocenteHeader(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EduRachaColors.Primary.copy(0.9f),
                            EduRachaColors.Primary
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Botón de volver
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar y datos del docente
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape)
                            .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = EduRachaColors.Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, color = iconColor)
            Text(title, style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
        }
    }
}

@Composable
fun ResumenCursoRow(
    estado: String,
    cantidad: Int,
    color: Color,
    total: Int
) {
    val porcentaje = if (total > 0) (cantidad.toFloat() / total.toFloat()) else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    estado,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = EduRachaColors.TextPrimary
                )
                Text(
                    "$cantidad cursos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun InfoDocenteItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(EduRachaColors.PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EduRachaColors.Primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = EduRachaColors.TextPrimary
            )
        }
    }
}

@Composable
fun FooterInstitucional() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = EduRachaColors.PrimaryContainer
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.School, null, tint = EduRachaColors.Primary, modifier = Modifier.size(40.dp))
            Text(
                "Corporación Universitaria",
                style = MaterialTheme.typography.bodyMedium,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                "Autónoma del Cauca",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                "EduRacha - Sistema de Gestión Académica",
                style = MaterialTheme.typography.labelSmall,
                color = EduRachaColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}