package com.stiven.sos

import com.stiven.sos.ui.theme.EduRachaColors
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
import androidx.compose.ui.draw.clip
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

    val primaryColor = EduRachaColors.PrimaryDark
    val secondaryColor = EduRachaColors.PrimaryDark
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

    // Lógica original del inicio de sesión con correo (sin cambios)
    fun iniciarSesionConCorreo() {
                            if (validateInputs()) {
                                isLoading = true
                                Log.d("LoginActivity", "=== INICIANDO SESIÓN ===")
                                Log.d("LoginActivity", "Portal: $userType")

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
                        }

    val esDocente = userType == "teacher"
    val nx = com.stiven.sos.ui.theme.LocalNexo.current
    val campoColores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = nx.marca,
        unfocusedBorderColor = nx.borde,
        focusedLabelColor = nx.marcaTexto,
        unfocusedLabelColor = nx.textoSuave,
        focusedLeadingIconColor = nx.marcaTexto,
        unfocusedLeadingIconColor = nx.textoSuave,
        focusedTextColor = nx.texto,
        unfocusedTextColor = nx.texto,
        disabledTextColor = nx.textoSuave,
        disabledBorderColor = nx.borde,
        focusedContainerColor = nx.superficieAlt,
        unfocusedContainerColor = nx.superficieAlt,
        disabledContainerColor = nx.superficieAlt,
        cursorColor = nx.marca,
        focusedPlaceholderColor = nx.textoTenue,
        unfocusedPlaceholderColor = nx.textoTenue
    )

    com.stiven.sos.ui.theme.FondoNexo {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                    Text("edu", fontSize = 22.sp, fontWeight = FontWeight.Black, color = nx.texto)
                    Text("racha", fontSize = 22.sp, fontWeight = FontWeight.Black, color = nx.marcaTexto)
                }
                com.stiven.sos.ui.theme.ChipNexo(
                    texto = if (esDocente) "Docente" else "Estudiante",
                    color = if (esDocente) nx.iaTexto else nx.marcaTexto,
                    icono = if (esDocente) Icons.Default.MenuBook else Icons.Default.School
                )
            }

            // Robi sobre su isla
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                com.stiven.sos.ui.theme.IslaFlotante(
                    modifier = Modifier.size(width = 210.dp, height = 84.dp),
                    tipo = com.stiven.sos.ui.theme.TipoIsla.ACTIVA
                )
                com.stiven.sos.ui.theme.RobiNeo(
                    tamano = 128.dp,
                    expresion = if (isLoading) com.stiven.sos.ui.theme.ExpresionRobi.PIENSA
                    else com.stiven.sos.ui.theme.ExpresionRobi.SALUDA,
                    modifier = Modifier.padding(bottom = 46.dp)
                )
            }

            Text(
                text = "¡Hola de nuevo!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = nx.texto
            )
            Text(
                text = if (esDocente) "Entra para gestionar tus cursos" else "Entra para seguir tu racha",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = nx.textoSuave,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Tarjeta del formulario
            com.stiven.sos.ui.theme.TarjetaNexo(
                modifier = Modifier.fillMaxWidth(),
                padding = 20.dp
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = nx.texto
                )
                Text(
                    text = "Ingresa con tu cuenta institucional",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = nx.textoSuave,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = campoColores,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    placeholder = { Text("Mínimo 6 caracteres") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = campoColores,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (!isLoading) iniciarSesionConCorreo() }
                    ),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = nx.textoSuave
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                com.stiven.sos.ui.theme.BotonNexo(
                    texto = if (isLoading) "Ingresando..." else "Iniciar sesión",
                    onClick = { iniciarSesionConCorreo() },
                    habilitado = !isLoading,
                    cargando = isLoading,
                    tipo = com.stiven.sos.ui.theme.TipoBoton.ACCION,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = nx.borde)
                    Text(
                        "o",
                        color = nx.textoTenue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = nx.borde)
                }

                OutlinedButton(
                    onClick = {
                        val signInIntent = googleClient.signInIntent
                        googleLauncher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = nx.superficie,
                        contentColor = nx.texto
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, nx.borde)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_2),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continuar con Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = nx.textoSuave
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "Regístrate",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = nx.marcaTexto
                    )
                }
            }
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
    val p = com.stiven.sos.ui.theme.LocalNexo.current
    com.stiven.sos.ui.theme.DialogoAccionNexo(
        icono = Icons.Default.Error,
        color = p.error,
        titulo = title,
        mensaje = message,
        textoPrincipal = "Entendido",
        onPrincipal = onDismiss,
        onDismiss = onDismiss,
        expresionRobi = com.stiven.sos.ui.theme.ExpresionRobi.TRISTE
    )
}