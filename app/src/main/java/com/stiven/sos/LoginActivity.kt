package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.stiven.sos.models.UserPreferences
import com.stiven.sos.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var sessionManager: SessionManager
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager.getInstance(this)
        userType = intent.getStringExtra("user_type") ?: "student"

        setupGoogleSignIn()

        setContent {
            EduRachaTheme {
                LoginScreen(
                    userType = userType,
                    auth = auth,
                    googleClient = googleClient,
                    context = this@LoginActivity,
                    sessionManager = sessionManager,
                    onNavigateToRegister = {
                        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                        intent.putExtra("user_type", userType)
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToMain = { goToMain() }
                )
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun goToMain() {
        val intent = if (userType == "teacher") {
            Intent(this, PanelDocenteActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("user_type", userType)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    userType: String,
    auth: FirebaseAuth,
    googleClient: GoogleSignInClient,
    context: android.content.Context,
    sessionManager: SessionManager,
    onNavigateToRegister: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
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
            email.trim().isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa tu correo electrónico")
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Correo inválido", "Por favor ingresa un correo electrónico válido")
                return false
            }
            password.isEmpty() -> {
                showError("Campo requerido", "Por favor ingresa tu contraseña")
                return false
            }
            password.length < 6 -> {
                showError("Contraseña muy corta", "La contraseña debe tener al menos 6 caracteres")
                return false
            }
        }
        return true
    }

    //  Obtiene el rol desde customClaims de Firebase Auth
    suspend fun obtenerRolDeFirebaseAuth(userId: String): String? {
        return try {
            val user = auth.currentUser
            if (user != null && user.uid == userId) {
                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims
                val rol = claims["rol"] as? String

                Log.d("LoginActivity", "Rol obtenido de Firebase Auth claims: $rol")
                rol
            } else {
                Log.e("LoginActivity", " Usuario no coincide o es null")
                null
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", " Error al obtener rol de Firebase Auth: ${e.message}")
            null
        }
    }

    //  Guarda el rol en SharedPreferences
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
        Log.d("LoginActivity", "   Datos guardados en SharedPreferences")
        Log.d("LoginActivity", "   UID: $userId")
        Log.d("LoginActivity", "   Rol: $rol")
        Log.d("LoginActivity", "   Nombre: $nombre")
    }

    //  Verifica si el rol coincide con el portal seleccionado
    suspend fun verificarRolUsuario(
        userId: String,
        userName: String,
        userEmail: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val rolEsperado = if (userType == "teacher") "docente" else "estudiante"

        Log.d("LoginActivity", "=== VERIFICACIÓN DE ROL ===")
        Log.d("LoginActivity", "UID: $userId")
        Log.d("LoginActivity", "Portal seleccionado: $userType (esperando: $rolEsperado)")

        val rolGuardado = obtenerRolDeFirebaseAuth(userId)

        if (rolGuardado == null) {
            Log.e("LoginActivity", "✗ No se encontró rol en Firebase Auth")
            onError("Tu cuenta no tiene un rol asignado.\n\n" +
                    "Esto ocurre con cuentas antiguas. Por favor:\n\n" +
                    "1. Cierra esta ventana\n" +
                    "2. Ve a 'Registrarse'\n" +
                    "3. Usa el mismo correo y contraseña\n" +
                    "4. Completa el proceso de registro\n\n" +
                    "Esto actualizará tu cuenta correctamente.")
            return
        }

        Log.d("LoginActivity", "Rol del usuario: $rolGuardado")

        if (rolGuardado == rolEsperado) {
            Log.d("LoginActivity", "✓ Rol correcto, permitiendo acceso")
            onSuccess(rolGuardado)
        } else {
            Log.d("LoginActivity", "✗ Rol incorrecto, bloqueando acceso")
            val mensajeError = if (userType == "teacher") {
                "Esta cuenta está registrada como estudiante.\n\nPor favor usa el portal de estudiantes para iniciar sesión."
            } else {
                "Esta cuenta está registrada como docente.\n\nPor favor usa el portal de docentes para iniciar sesión."
            }
            onError(mensajeError)
        }
    }

    val title = if (userType == "teacher") "Inicio de sesión - Docente" else "Inicio de sesión - Estudiante"

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            isLoading = true
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val nombre = user.displayName ?: account.displayName ?: "Usuario"
                                val correo = user.email ?: ""

                                verificarRolUsuario(
                                    userId = user.uid,
                                    userName = nombre,
                                    userEmail = correo,
                                    onSuccess = { rolReal ->
                                        guardarDatosEnPreferences(user.uid, rolReal, nombre, correo, "")

                                        sessionManager.saveUserSession(
                                            userId = user.uid,
                                            userName = nombre,
                                            userEmail = correo,
                                            userRol = rolReal
                                        )

                                        UserPreferences.saveUserData(
                                            context = context,
                                            uid = user.uid,
                                            nombreCompleto = nombre,
                                            apodo = "",
                                            correo = correo,
                                            rol = rolReal
                                        )

                                        isLoading = false
                                        onNavigateToMain()
                                    },
                                    onError = { mensaje ->
                                        isLoading = false
                                        auth.signOut()
                                        googleClient.signOut()
                                        showError("Acceso denegado", mensaje)
                                    }
                                )
                            } catch (e: Exception) {
                                isLoading = false
                                auth.signOut()
                                googleClient.signOut()
                                showError("Error", "Error al verificar el rol: ${e.message}")
                            }
                        }
                    }
                } else {
                    isLoading = false
                    showError("Error de autenticación", authTask.exception?.localizedMessage ?: "No se pudo iniciar sesión con Google")
                }
            }
        } catch (e: Exception) {
            isLoading = false
            showError("Error de conexión", "No se pudo iniciar sesión con Google. Verifica tu conexión a internet.")
        }
    }

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
                            .padding(bottom = 36.dp),
                        letterSpacing = 0.2.sp
                    )

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

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (validateInputs()) {
                                isLoading = true
                                Log.d("LoginActivity", "=== INICIANDO SESIÓN ===")
                                Log.d("LoginActivity", "Email: $email, Portal: $userType")

                                auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { authResult ->
                                        val user = authResult.user
                                        if (user != null) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                try {
                                                    val nombre = user.displayName ?: "Usuario"
                                                    val correo = user.email ?: ""

                                                    verificarRolUsuario(
                                                        userId = user.uid,
                                                        userName = nombre,
                                                        userEmail = correo,
                                                        onSuccess = { rolReal ->
                                                            guardarDatosEnPreferences(user.uid, rolReal, nombre, correo, "")

                                                            sessionManager.saveUserSession(
                                                                userId = user.uid,
                                                                userName = nombre,
                                                                userEmail = correo,
                                                                userRol = rolReal
                                                            )

                                                            UserPreferences.saveUserData(
                                                                context = context,
                                                                uid = user.uid,
                                                                nombreCompleto = nombre,
                                                                apodo = "",
                                                                correo = correo,
                                                                rol = rolReal
                                                            )

                                                            Log.d("LoginActivity", "✓ Login exitoso con rol: $rolReal")
                                                            isLoading = false
                                                            onNavigateToMain()
                                                        },
                                                        onError = { mensaje ->
                                                            isLoading = false
                                                            auth.signOut()
                                                            showError("Acceso denegado", mensaje)
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    isLoading = false
                                                    auth.signOut()
                                                    showError("Error", "Error al verificar el rol: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        val errorMsg = when {
                                            exception.message?.contains("password", ignoreCase = true) == true ->
                                                "Contraseña incorrecta"
                                            exception.message?.contains("user", ignoreCase = true) == true ||
                                                    exception.message?.contains("email", ignoreCase = true) == true ->
                                                "No existe una cuenta con este correo electrónico"
                                            exception.message?.contains("network", ignoreCase = true) == true ->
                                                "Error de conexión a internet"
                                            exception.message?.contains("too-many-requests", ignoreCase = true) == true ->
                                                "Demasiados intentos. Intenta más tarde"
                                            else -> exception.message ?: "Error desconocido"
                                        }
                                        showError("Error de inicio de sesión", errorMsg)
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
                                text = "INICIAR SESIÓN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            val signInIntent = googleClient.signInIntent
                            googleLauncher.launch(signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF424242)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_2),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Iniciar sesión con Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
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
                    text = "¿No tienes cuenta?",
                    fontSize = 15.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, primaryColor)
                ) {
                    Text(
                        text = "REGISTRARSE AHORA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = primaryColor,
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

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Entendido",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}