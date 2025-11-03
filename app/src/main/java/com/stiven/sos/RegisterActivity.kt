package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.RegistroRequest
import com.stiven.sos.models.UserPreferences
import com.stiven.sos.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager.getInstance(this)
        userType = intent.getStringExtra("user_type") ?: "student"

        setContent {
            EduRachaTheme {
                RegisterScreen(
                    userType = userType,
                    auth = auth,
                    context = this@RegisterActivity,
                    sessionManager = sessionManager,
                    onNavigateToLogin = {
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        intent.putExtra("user_type", userType)
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToMain = { goToMain() }
                )
            }
        }
    }

    private fun goToMain() {
        val intent = if (userType == "teacher") {
            Intent(this, PanelDocenteActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.putExtra("user_type", userType)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    userType: String,
    auth: FirebaseAuth,
    context: android.content.Context,
    sessionManager: SessionManager,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val scrollState = rememberScrollState()

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val primaryColor = Color(0xFF0D47A1)
    val secondaryColor = Color(0xFF0D47A1)
    val backgroundColor = Color(0xFFF5F5F5)

    fun showError(title: String, message: String) {
        errorTitle = title
        errorMessage = message
        showErrorDialog = true
    }

    fun validateInputs(): Boolean {
        when {
            fullName.trim().isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa tu nombre completo")
                return false
            }
            fullName.trim().length < 3 -> {
                showError("Nombre muy corto", "El nombre debe tener al menos 3 caracteres")
                return false
            }
            username.trim().isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa un nombre de usuario")
                return false
            }
            username.trim().length < 3 -> {
                showError("Usuario muy corto", "El nombre de usuario debe tener al menos 3 caracteres")
                return false
            }
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                showError("Usuario inválido", "Solo se permiten letras, números y guiones bajos")
                return false
            }
            email.trim().isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa tu correo electrónico")
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Correo inválido", "Por favor ingresa un correo electrónico válido")
                return false
            }
            password.isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa una contraseña")
                return false
            }
            password.length < 6 -> {
                showError("Contraseña muy corta", "La contraseña debe tener al menos 6 caracteres")
                return false
            }
            confirmPassword.isEmpty() -> {
                showError("Campo requerido", "Por favor confirma tu contraseña")
                return false
            }
            password != confirmPassword -> {
                showError("Contraseñas diferentes", "Las contraseñas no coinciden. Verifica e intenta nuevamente")
                return false
            }
        }
        return true
    }

    // 🔥 FUNCIÓN: Obtiene el rol desde Firebase Auth (igual que en Login)
    suspend fun obtenerRolDeFirebaseAuth(userId: String): String? {
        return try {
            val user = auth.currentUser
            if (user != null && user.uid == userId) {
                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims
                val rol = claims["rol"] as? String
                Log.d("RegisterActivity", "📊 Rol obtenido de Firebase Auth: $rol")
                rol
            } else {
                Log.e("RegisterActivity", "❌ Usuario no coincide o es null")
                null
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "❌ Error al obtener rol: ${e.message}")
            null
        }
    }

    // 🔥 FUNCIÓN: Guarda datos en SharedPreferences (igual que en Login)
    fun guardarDatosEnPreferences(
        userId: String,
        rol: String,
        nombre: String,
        correo: String,
        apodo: String
    ) {
        val prefs = context.getSharedPreferences("EduRachaPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_rol", rol)
            putString("user_id", userId)
            putString("user_name", nombre)
            putString("user_email", correo)
            putString("user_nickname", apodo)
            apply()
        }
        Log.d("RegisterActivity", "✅ Datos guardados en SharedPreferences")
        Log.d("RegisterActivity", "   UID: $userId")
        Log.d("RegisterActivity", "   Rol: $rol")
        Log.d("RegisterActivity", "   Nombre: $nombre")
    }

    val title = if (userType == "teacher") "Crear Cuenta de Docente" else "Crear Cuenta de Estudiante"
    val buttonText = if (userType == "teacher") "CREAR CUENTA DOCENTE" else "CREAR CUENTA ESTUDIANTIL"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Logo institucional",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sistema Institucional",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Plataforma de Gestión Educativa",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        letterSpacing = 0.2.sp
                    )

                    Text(
                        text = "Completa todos los campos para acceder a la plataforma",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre completo") },
                        placeholder = { Text("Ingresa tu nombre y apellidos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        placeholder = { Text("usuario123") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("ejemplo@correo.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = primaryColor
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña") },
                        placeholder = { Text("Repite tu contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = primaryColor
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (validateInputs()) {
                                isLoading = true

                                val rol = if (userType == "teacher") "docente" else "estudiante"

                                Log.d("RegisterActivity", "=== INICIANDO REGISTRO ===")
                                Log.d("RegisterActivity", "Nombre: ${fullName.trim()}")
                                Log.d("RegisterActivity", "Correo: ${email.trim()}")
                                Log.d("RegisterActivity", "Rol esperado: $rol")

                                // 🔥 PASO 1: Registrar en tu backend
                                val registroRequest = RegistroRequest(
                                    nombreCompleto = fullName.trim(),
                                    apodo = username.trim(),
                                    correo = email.trim(),
                                    contrasena = password,
                                    rol = rol
                                )

                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        // Llamada al backend
                                        val response = withContext(Dispatchers.IO) {
                                            ApiClient.apiService.registrarUsuario(registroRequest)
                                        }

                                        if (response.isSuccessful && response.body() != null) {
                                            Log.d("RegisterActivity", "✓ Backend: Registro exitoso")

                                            val responseBody = response.body()!!
                                            val uid = responseBody["uid"] as? String ?: ""

                                            // 🔥 PASO 2: Iniciar sesión en Firebase Auth para obtener el token con customClaims
                                            Log.d("RegisterActivity", "🔐 Autenticando en Firebase...")

                                            auth.signInWithEmailAndPassword(email.trim(), password)
                                                .addOnSuccessListener { authResult ->
                                                    val user = authResult.user
                                                    if (user != null) {
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            try {
                                                                // Esperar un poco para que Firebase actualice los claims
                                                                kotlinx.coroutines.delay(1000)

                                                                // Obtener el rol desde Firebase Auth
                                                                val rolReal = obtenerRolDeFirebaseAuth(user.uid) ?: rol

                                                                Log.d("RegisterActivity", "✓ Rol obtenido: $rolReal")

                                                                // Guardar en todas las ubicaciones
                                                                guardarDatosEnPreferences(
                                                                    user.uid,
                                                                    rolReal,
                                                                    fullName.trim(),
                                                                    email.trim(),
                                                                    username.trim()
                                                                )

                                                                sessionManager.saveUserSession(
                                                                    userId = user.uid,
                                                                    userName = fullName.trim(),
                                                                    userEmail = email.trim(),
                                                                    userRol = rolReal
                                                                )

                                                                UserPreferences.saveUserData(
                                                                    context = context,
                                                                    uid = user.uid,
                                                                    nombreCompleto = fullName.trim(),
                                                                    apodo = username.trim(),
                                                                    correo = email.trim(),
                                                                    rol = rolReal
                                                                )

                                                                Toast.makeText(
                                                                    context,
                                                                    "¡Registro exitoso! Bienvenido ${fullName.trim()}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()

                                                                isLoading = false
                                                                onNavigateToMain()

                                                            } catch (e: Exception) {
                                                                Log.e("RegisterActivity", "❌ Error al obtener rol: ${e.message}")
                                                                isLoading = false
                                                                auth.signOut()
                                                                showError("Error", "No se pudo completar el registro: ${e.message}")
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    isLoading = false
                                                    Log.e("RegisterActivity", "❌ Error en Firebase Auth: ${exception.message}")
                                                    showError(
                                                        "Error de autenticación",
                                                        "No se pudo iniciar sesión después del registro. Por favor intenta iniciar sesión manualmente."
                                                    )
                                                }

                                        } else {
                                            // Manejo de errores del backend
                                            isLoading = false
                                            Log.e("RegisterActivity", "❌ Error del backend: ${response.code()}")

                                            val errorTitle: String
                                            val errorMsg: String

                                            when (response.code()) {
                                                400 -> {
                                                    val errorBody = response.errorBody()?.string()
                                                    when {
                                                        errorBody?.contains("EMAIL_EXISTS", ignoreCase = true) == true -> {
                                                            errorTitle = "Correo ya registrado"
                                                            errorMsg = "Este correo electrónico ya está en uso.\n\nPor favor usa otro correo o inicia sesión si ya tienes una cuenta."
                                                        }
                                                        errorBody?.contains("INVALID_EMAIL", ignoreCase = true) == true -> {
                                                            errorTitle = "Correo inválido"
                                                            errorMsg = "El formato del correo electrónico no es válido."
                                                        }
                                                        errorBody?.contains("WEAK_PASSWORD", ignoreCase = true) == true -> {
                                                            errorTitle = "Contraseña débil"
                                                            errorMsg = "La contraseña debe tener al menos 6 caracteres."
                                                        }
                                                        else -> {
                                                            errorTitle = "Datos inválidos"
                                                            errorMsg = "Por favor verifica que todos los campos estén correctos."
                                                        }
                                                    }
                                                }
                                                409 -> {
                                                    errorTitle = "Usuario existente"
                                                    errorMsg = "Ya existe una cuenta con este correo o nombre de usuario.\n\nIntenta con datos diferentes o inicia sesión."
                                                }
                                                500 -> {
                                                    errorTitle = "Error del servidor"
                                                    errorMsg = "Ocurrió un error en el servidor.\n\nPor favor intenta nuevamente más tarde."
                                                }
                                                503 -> {
                                                    errorTitle = "Servicio no disponible"
                                                    errorMsg = "El servidor no está disponible en este momento.\n\nIntenta más tarde."
                                                }
                                                else -> {
                                                    errorTitle = "Error de registro"
                                                    errorMsg = "Error ${response.code()}: ${response.message()}"
                                                }
                                            }

                                            showError(errorTitle, errorMsg)
                                        }

                                    } catch (e: Exception) {
                                        Log.e("RegisterActivity", "❌ Excepción: ${e.message}", e)
                                        isLoading = false

                                        val errorMsg = when {
                                            e.message?.contains("timeout", ignoreCase = true) == true ->
                                                "Tiempo de espera agotado.\n\nVerifica tu conexión a internet e intenta nuevamente."
                                            e.message?.contains("unable to resolve host", ignoreCase = true) == true ->
                                                "No se pudo conectar al servidor.\n\nVerifica tu conexión a internet."
                                            else ->
                                                "No se pudo conectar con el servidor.\n\nVerifica tu conexión a internet e intenta más tarde."
                                        }
                                        showError("Error de conexión", errorMsg)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D47A1),
                            disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = buttonText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .background(Color(0xFFE0E0E0))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿Ya tienes una cuenta?",
                    fontSize = 15.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0D47A1))
                ) {
                    Text(
                        text = "INICIAR SESIÓN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF0D47A1)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF0D47A1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showErrorDialog) {
        ErrorDialog(
            title = errorTitle,
            message = errorMessage,
            onDismiss = { showErrorDialog = false }
        )
    }
}