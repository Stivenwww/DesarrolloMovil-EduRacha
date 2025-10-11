package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
// Importa el tema centralizado

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        setupGoogleSignIn()

        setContent {
            // 1. APLICA TU TEMA PERSONALIZADO AQUÍ
            EduRachaTheme {
                LoginScreen(
                    userType = userType,
                    auth = auth,
                    googleClient = googleClient,
                    onNavigateToRegister = {
                        val intent = Intent(this, RegisterActivity::class.java)
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
    onNavigateToRegister: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF0D47A1)
    val secondaryColor = Color(0xFF0D47A1)
    val backgroundColor = Color(0xFFF5F5F5)
    val errorColor = Color(0xFFD32F2F)


    // Funciones de validación
    fun validateEmail(email: String): String {
        return when {
            email.trim().isEmpty() -> "El correo electrónico es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Ingresa un correo electrónico válido"
            else -> ""
        }
    }

    fun validatePassword(password: String): String {
        return when {
            password.isEmpty() -> "La contraseña es obligatoria"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> ""
        }
    }

    // Títulos según tipo de usuario
    val title = if (userType == "teacher") "Inicio de sesión - Docente" else "Inicio de sesión - Estudiante"

    // Launcher para Google Sign In
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    val currentDisplayName = user?.displayName ?: account.displayName ?: "Usuario"

                    if (!currentDisplayName.contains("Docente") && !currentDisplayName.contains("Estudiante")) {
                        val newDisplayName = "$currentDisplayName - ${if (userType == "teacher") "Docente" else "Estudiante"}"
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newDisplayName)
                            .build()
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            onNavigateToMain()
                        }
                    } else {
                        onNavigateToMain()
                    }
                } else {
                    Toast.makeText(context, "Error con Google: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
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
            // Header institucional con gradiente
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
                            imageVector = Icons.Default.School, // MANTENIENDO EL ICONO
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

            // Card del formulario
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
                        color = Color(0xFF212121), // MANTENIENDO TU COLOR ORIGINAL
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 36.dp),
                        letterSpacing = 0.2.sp
                    )

                    // Campo Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = ""
                        },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("ejemplo@correo.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = emailError.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            errorBorderColor = errorColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (emailError.isNotEmpty()) errorColor else primaryColor
                            )
                        }
                    )
                    if (emailError.isNotEmpty()) {
                        Text(
                            text = emailError,
                            color = errorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = ""
                        },
                        label = { Text("Contraseña") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = passwordError.isNotEmpty(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLabelColor = primaryColor,
                            errorBorderColor = errorColor,
                            disabledBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (passwordError.isNotEmpty()) errorColor else primaryColor
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
                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = errorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Login principal
                    Button(
                        onClick = {
                            emailError = validateEmail(email)
                            passwordError = validatePassword(password)

                            if (emailError.isEmpty() && passwordError.isEmpty()) {
                                isLoading = true
                                auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "¡Bienvenido! Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                            onNavigateToMain()
                                        } else {
                                            val errorMessage = when {
                                                task.exception?.message?.contains("password") == true -> "Contraseña incorrecta"
                                                task.exception?.message?.contains("user") == true -> "No existe una cuenta con este correo"
                                                task.exception?.message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
                                                else -> "Error: ${task.exception?.message}"
                                            }
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D47A1), // MANTENIENDO TU COLOR ORIGINAL
                            disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
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

                    // Botón Google
                    OutlinedButton(
                        onClick = {
                            val signInIntent = googleClient.signInIntent
                            googleLauncher.launch(signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White, // MANTENIENDO TU COLOR ORIGINAL
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
                            color = Color(0xFF424242) // MANTENIENDO TU COLOR ORIGINAL
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

            // Sección de registro
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    fontSize = 15.sp,
                    color = Color(0xFF757575), // MANTENIENDO TU COLOR ORIGINAL
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White // MANTENIENDO TU COLOR ORIGINAL
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
}
