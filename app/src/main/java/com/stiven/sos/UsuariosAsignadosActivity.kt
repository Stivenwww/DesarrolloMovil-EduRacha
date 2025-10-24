package com.stiven.sos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.models.UsuarioAsignado
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import com.stiven.sos.viewmodel.UsuarioViewModel
import kotlinx.coroutines.delay

class UsuariosAsignadosActivity : ComponentActivity() {
    private val viewModel: UsuarioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursoId = intent.getStringExtra("CURSO_ID") ?: ""
        val cursoTitulo = intent.getStringExtra("CURSO_TITULO") ?: "Curso"

        setContent {
            EduRachaTheme {
                UsuariosAsignadosScreen(
                    viewModel = viewModel,
                    cursoId = cursoId,
                    cursoTitulo = cursoTitulo,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosAsignadosScreen(
    viewModel: UsuarioViewModel,
    cursoId: String,
    cursoTitulo: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Cargar estudiantes al inicio
    LaunchedEffect(cursoId) {
        viewModel.cargarEstudiantesPorCurso(cursoId, cursoTitulo)
    }

    // Mostrar errores con Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Filtrar estudiantes por búsqueda
    val usuariosFiltrados = remember(uiState.usuarios, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.usuarios
        } else {
            uiState.usuarios.filter { usuario ->
                usuario.nombre.contains(searchQuery, ignoreCase = true) ||
                        usuario.correo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Estudiantes Asignados",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            cursoTitulo,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState.usuarios.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${uiState.usuarios.size} estudiantes",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EduRachaColors.Primary
                )
            )
        },
        containerColor = EduRachaColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Barra de búsqueda
            if (uiState.usuarios.isNotEmpty()) {
                SearchBarEstudiantes(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Contenido principal
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = EduRachaColors.Primary
                        )
                    }

                    uiState.error != null && uiState.usuarios.isEmpty() -> {
                        ErrorEstudiantesView(
                            message = uiState.error ?: "Error desconocido",
                            onRetry = {
                                viewModel.cargarEstudiantesPorCurso(cursoId, cursoTitulo)
                            }
                        )
                    }

                    usuariosFiltrados.isEmpty() -> {
                        EmptyEstudiantesView(
                            isSearchActive = searchQuery.isNotBlank()
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                usuariosFiltrados,
                                key = { _, usuario -> usuario.uid }
                            ) { index, usuario ->
                                AnimatedEstudianteCard(
                                    usuario = usuario,
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEstudianteCard(
    usuario: UsuarioAsignado,
    index: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circular con inicial
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EduRachaColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = obtenerIniciales(usuario.nombre),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.Primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información del estudiante
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = usuario.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduRachaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = EduRachaColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = usuario.correo,
                            fontSize = 13.sp,
                            color = EduRachaColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de estado
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = obtenerColorEstadoUsuario(usuario.estado).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(obtenerColorEstadoUsuario(usuario.estado))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = usuario.estado.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = obtenerColorEstadoUsuario(usuario.estado)
                            )
                        }
                    }
                }

                // Icono de más opciones (placeholder)
                IconButton(onClick = { /* TODO: Opciones */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarEstudiantes(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Buscar por nombre o correo...",
                color = EduRachaColors.TextSecondary.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "Buscar",
                tint = EduRachaColors.Primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        "Limpiar",
                        tint = EduRachaColors.TextSecondary
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(50),
        singleLine = true
    )
}

@Composable
fun ErrorEstudiantesView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            null,
            tint = EduRachaColors.Error,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Error al cargar estudiantes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun EmptyEstudiantesView(isSearchActive: Boolean) {
    val title = if (isSearchActive)
        "Sin resultados"
    else
        "Sin estudiantes asignados"
    val subtitle = if (isSearchActive)
        "Intenta con otra búsqueda."
    else
        "Aún no hay estudiantes inscritos en este curso."

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearchActive) Icons.Outlined.SearchOff else Icons.Outlined.PersonOff,
            null,
            modifier = Modifier.size(100.dp),
            tint = EduRachaColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EduRachaColors.TextPrimary
        )
        Text(
            subtitle,
            fontSize = 14.sp,
            color = EduRachaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Funciones auxiliares
private fun obtenerIniciales(nombre: String): String {
    val palabras = nombre.trim().split(" ")
    return when {
        palabras.size >= 2 -> "${palabras[0].first()}${palabras[1].first()}".uppercase()
        palabras.isNotEmpty() -> palabras[0].take(2).uppercase()
        else -> "?"
    }
}

private fun obtenerColorEstadoUsuario(estado: String): Color {
    return when (estado.lowercase()) {
        "activo" -> EduRachaColors.Success
        "inactivo" -> EduRachaColors.TextSecondary
        "suspendido" -> EduRachaColors.Error
        else -> EduRachaColors.Primary
    }
}