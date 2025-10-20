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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.RegistroRequest
import com.stiven.sos.models.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        setContent {
            EduRachaTheme {
                RegisterScreen(
                    userType = userType,
                    auth = auth,
                    context = this@RegisterActivity,
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
        // ✅ Limpiar todas las actividades anteriores del stack
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

    var fullNameError by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF0D47A1)
    val secondaryColor = Color(0xFF0D47A1)
    val backgroundColor = Color(0xFFF5F5F5)
    val errorColor = Color(0xFFD32F2F)

    fun validateFullName(name: String): String {
        return when {
            name.trim().isEmpty() -> "El nombre completo es obligatorio"
            name.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres"
            else -> ""
        }
    }

    fun validateUsername(user: String): String {
        return when {
            user.trim().isEmpty() -> "El nombre de usuario es obligatorio"
            user.trim().length < 3 -> "El nombre de usuario debe tener al menos 3 caracteres"
            !user.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Solo letras, números y guiones bajos"
            else -> ""
        }
    }

    fun validateEmail(mail: String): String {
        return when {
            mail.trim().isEmpty() -> "El correo electrónico es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> "Ingresa un correo válido"
            else -> ""
        }
    }

    fun validatePassword(pass: String): String {
        return when {
            pass.isEmpty() -> "La contraseña es obligatoria"
            pass.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> ""
        }
    }

    fun validateConfirmPassword(pass: String, confirm: String): String {
        return when {
            confirm.isEmpty() -> "Confirma tu contraseña"
            pass != confirm -> "Las contraseñas no coinciden"
            else -> ""
        }
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
                        onValueChange = {
                            fullName = it
                            fullNameError = ""
                        },
                        label = { Text("Nombre completo") },
                        placeholder = { Text("Ingresa tu nombre y apellidos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = fullNameError.isNotEmpty(),
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (fullNameError.isNotEmpty()) errorColor else primaryColor
                            )
                        }
                    )
                    if (fullNameError.isNotEmpty()) {
                        Text(
                            text = fullNameError,
                            color = errorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Ingresa tu nombre y apellidos completos",
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = ""
                        },
                        label = { Text("Nombre de usuario") },
                        placeholder = { Text("usuario123") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = usernameError.isNotEmpty(),
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
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (usernameError.isNotEmpty()) errorColor else primaryColor
                            )
                        }
                    )
                    if (usernameError.isNotEmpty()) {
                        Text(
                            text = usernameError,
                            color = errorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Solo letras, números y guiones bajos",
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                    } else {
                        Text(
                            text = "Debe ser un correo válido (ej: usuario@dominio.com)",
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                    } else {
                        Text(
                            text = "Mínimo 6 caracteres, incluye números y letras",
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = ""
                        },
                        label = { Text("Confirmar contraseña") },
                        placeholder = { Text("Repite tu contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = confirmPasswordError.isNotEmpty(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                tint = if (confirmPasswordError.isNotEmpty()) errorColor else primaryColor
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
                    if (confirmPasswordError.isNotEmpty()) {
                        Text(
                            text = confirmPasswordError,
                            color = errorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Debe coincidir con la contraseña anterior",
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            fullNameError = validateFullName(fullName)
                            usernameError = validateUsername(username)
                            emailError = validateEmail(email)
                            passwordError = validatePassword(password)
                            confirmPasswordError = validateConfirmPassword(password, confirmPassword)

                            if (fullNameError.isEmpty() && usernameError.isEmpty() &&
                                emailError.isEmpty() && passwordError.isEmpty() &&
                                confirmPasswordError.isEmpty()) {

                                isLoading = true

                                val rol = if (userType == "teacher") "docente" else "estudiante"

                                val registroRequest = RegistroRequest(
                                    nombreCompleto = fullName.trim(),
                                    apodo = username.trim(),
                                    correo = email.trim(),
                                    contrasena = password,
                                    rol = rol
                                )

                                Log.d("RegisterActivity", "=== INICIANDO REGISTRO ===")
                                Log.d("RegisterActivity", "Nombre completo a enviar: ${registroRequest.nombreCompleto}")
                                Log.d("RegisterActivity", "Correo a enviar: ${registroRequest.correo}")
                                Log.d("RegisterActivity", "Rol a enviar: ${registroRequest.rol}")

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        Log.d("RegisterActivity", "Enviando petición al servidor...")
                                        val response = ApiClient.apiService.registrarUsuario(registroRequest)

                                        Log.d("RegisterActivity", "=== RESPUESTA RECIBIDA ===")
                                        Log.d("RegisterActivity", "Código de respuesta: ${response.code()}")

                                        withContext(Dispatchers.Main) {
                                            if (response.isSuccessful) {
                                                Log.d("RegisterActivity", "Respuesta exitosa")

                                                if (response.body() != null) {
                                                    val responseBody = response.body()!!

                                                    val uid = try { responseBody["uid"] as? String ?: "" } catch (e: Exception) { "" }
                                                    val nombre = try { responseBody["nombreCompleto"] as? String ?: fullName.trim() } catch (e: Exception) { fullName.trim() }
                                                    val apodo = try { responseBody["apodo"] as? String ?: username.trim() } catch (e: Exception) { username.trim() }
                                                    val correo = try { responseBody["correo"] as? String ?: email.trim() } catch (e: Exception) { email.trim() }
                                                    val rolRespuesta = try { responseBody["rol"] as? String ?: rol } catch (e: Exception) { rol }

                                                    Log.d("RegisterActivity", "UID recibido: $uid")
                                                    Log.d("RegisterActivity", "Nombre recibido: $nombre")
                                                    Log.d("RegisterActivity", "Correo recibido: $correo")

                                                    // 🔑 PRIMERO: Limpiar datos anteriores
                                                    Log.d("RegisterActivity", "=== LIMPIANDO DATOS ANTERIORES ===")
                                                    UserPreferences.clearUserData(context)

                                                    // 🔑 SEGUNDO: GUARDAR EN SHAREDPREFERENCES CON commit() (SÍNCRONO)
                                                    Log.d("RegisterActivity", "=== GUARDANDO NUEVOS DATOS EN SHAREDPREFERENCES ===")
                                                    UserPreferences.saveUserData(
                                                        context = context,
                                                        uid = uid,
                                                        nombreCompleto = nombre,
                                                        apodo = apodo,
                                                        correo = correo,
                                                        rol = rolRespuesta
                                                    )

                                                    Log.d("RegisterActivity", "✓ Datos guardados en SharedPreferences")

                                                    // Verificar que se guardó correctamente
                                                    Log.d("RegisterActivity", "Verificación post-guardado: ${UserPreferences.getUserName(context)}")

                                                    Toast.makeText(
                                                        context,
                                                        "¡Registro exitoso! Bienvenido $nombre",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    isLoading = false
                                                    // ✅ Navegar inmediatamente (sin delay)
                                                    onNavigateToMain()
                                                } else {
                                                    Log.e("RegisterActivity", "Body es nulo")
                                                    isLoading = false
                                                    Toast.makeText(context, "Error: respuesta vacía del servidor", Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                Log.e("RegisterActivity", "Respuesta no exitosa: ${response.code()}")
                                                isLoading = false

                                                val errorMsg = when (response.code()) {
                                                    400 -> "Datos inválidos. Verifica la información"
                                                    409 -> "Este correo ya está registrado"
                                                    500 -> "Error en el servidor. Intenta más tarde"
                                                    else -> "Error ${response.code()}: ${response.message()}"
                                                }
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterActivity", "Error: ${e.message}", e)
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
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
}