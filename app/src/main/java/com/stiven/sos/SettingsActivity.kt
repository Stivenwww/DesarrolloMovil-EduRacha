package com.stiven.sos
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.models.UserPreferences
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.stiven.sos.api.ApiService
import com.stiven.sos.api.ApiClient
import com.stiven.sos.models.ActualizarPerfilRequest


class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setContent {
            EduRachaTheme {
                SettingsScreen(
                    onBackClick = { finish() },
                    onLogout = { logout() }
                )
            }
        }
    }

    private fun logout() {
        UserPreferences.clearUserData(this)
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userNickname by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }

    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        val prefs = context.getSharedPreferences("EduRachaUserPrefs", Context.MODE_PRIVATE)
        userName = prefs.getString("user_name", null) ?: "Usuario"
        userEmail = prefs.getString("user_email", null) ?: ""
        userNickname = prefs.getString("user_nickname", null) ?: ""
        userRole = prefs.getString("user_role", null) ?: "estudiante"
        userUid = prefs.getString("user_uid", null) ?: ""
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EduRachaColors.Background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                userName = userName,
                username = userNickname,
                userEmail = userEmail,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                SettingsSection(title = "CUENTA", iconColor = EduRachaColors.Primary) {
                    SettingsItem(
                        Icons.Outlined.Edit, "Editar perfil", "Actualiza tu información",
                        EduRachaColors.Primary
                    ) { showEditProfileDialog = true }
                    SettingsDivider()
                    SettingsItem(
                        Icons.Outlined.Lock, "Cambiar contraseña", "Mantén tu cuenta segura",
                        EduRachaColors.Primary
                    ) { showChangePasswordDialog = true }
                }

                SettingsSection(title = "NOTIFICACIONES", iconColor = EduRachaColors.Accent) {
                    SettingsSwitchItem(
                        "Notificaciones push", "Recibe alertas importantes",
                        notificationsEnabled
                    ) { notificationsEnabled = it }
                    SettingsDivider()
                    SettingsSwitchItem(
                        "Sonido", "Alertas con sonido",
                        soundEnabled
                    ) { soundEnabled = it }
                }

                SettingsSection(title = "INFORMACIÓN", iconColor = EduRachaColors.Secondary) {
                    SettingsItem(
                        Icons.Outlined.Help, "Ayuda y soporte", "Centro de ayuda EduRacha",
                        EduRachaColors.Secondary
                    ) { Toast.makeText(context, "Próximamente", Toast.LENGTH_SHORT).show() }
                    SettingsDivider()
                    SettingsItem(
                        Icons.Outlined.Info, "Acerca de EduRacha", "Versión 1.0.0",
                        EduRachaColors.Secondary
                    ) { showAboutDialog = true }
                }

                LogoutButton(onClick = { showLogoutDialog = true })
                SettingsFooter()
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentUsername = userNickname,
            userUid = userUid,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, nickname ->
                showEditProfileDialog = false
                updateUserProfile(context, userUid, name, nickname, userEmail, userRole) {
                    refreshKey++
                    Toast.makeText(context, "✓ Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            userEmail = userEmail,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                showChangePasswordDialog = false
                changePassword(context, currentPassword, newPassword)
            }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}




private fun updateUserProfile(
    context: Context,
    uid: String,
    newName: String,
    newNickname: String,
    email: String,
    role: String,
    onSuccess: () -> Unit
) {
    if (uid.isEmpty()) {
        Toast.makeText(context, "Error: Usuario no identificado", Toast.LENGTH_LONG).show()
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1️⃣ Guarda localmente en SharedPreferences
            withContext(Dispatchers.Main) {
                UserPreferences.saveUserData(
                    context = context,
                    uid = uid,
                    nombreCompleto = newName,
                    apodo = newNickname,
                    correo = email,
                    rol = role
                )
            }

            // 2️⃣ Sincroniza con la API del backend
            val apiService = ApiClient.instance.create(ApiService::class.java)

            val perfilUpdate = ActualizarPerfilRequest(
                nombreCompleto = newName,
                apodo = newNickname,
                correo = email,
                rol = role
            )

            val response = apiService.actualizarPerfil(uid, perfilUpdate)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    onSuccess()
                    Toast.makeText(
                        context,
                        "✓ Perfil actualizado exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Error al sincronizar: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}


private fun changePassword(
    context: Context,
    currentPassword: String,
    newPassword: String
) {
    val user = FirebaseAuth.getInstance().currentUser

    if (user?.email == null) {
        Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_LONG).show()
        return
    }

    val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

    user.reauthenticate(credential).addOnSuccessListener {
        user.updatePassword(newPassword).addOnSuccessListener {
            Toast.makeText(context, "✓ Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error al cambiar contraseña: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
    }
}

//<editor-fold desc="UI Components">
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(
    userName: String,
    username: String,
    userEmail: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        EduRachaColors.Primary,
                        EduRachaColors.Primary.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Column {
            TopAppBar(
                title = { Text("Configuración", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        userName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (username.isNotEmpty()) {
                        Text(
                            "@$username",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        userEmail,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        SectionHeader(title = title, color = iconColor)
        Spacer(Modifier.height(12.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Column { content() }
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, title, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            Icons.Default.ArrowForwardIos,
            "Ir",
            tint = EduRachaColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = EduRachaColors.Primary,
                uncheckedTrackColor = EduRachaColors.SurfaceVariant
            )
        )
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick,
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(EduRachaColors.Error)
    ) {
        Icon(Icons.Outlined.Logout, "Cerrar sesión", Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text("Cerrar Sesión", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsFooter() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Corporación Universitaria",
            fontSize = 12.sp,
            color = EduRachaColors.TextSecondary
        )
        Text(
            "Autónoma del Cauca",
            fontSize = 14.sp,
            color = EduRachaColors.Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        Modifier.padding(horizontal = 20.dp),
        color = EduRachaColors.Background,
        thickness = 2.dp
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentUsername: String,
    userUid: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var username by remember { mutableStateOf(currentUsername) }
    var nameError by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }

    fun validateName(value: String): String {
        return when {
            value.trim().isEmpty() -> "El nombre es obligatorio"
            value.trim().length < 3 -> "Mínimo 3 caracteres"
            else -> ""
        }
    }

    fun validateUsername(value: String): String {
        return when {
            value.trim().isEmpty() -> "El usuario es obligatorio"
            value.trim().length < 3 -> "Mínimo 3 caracteres"
            !value.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Solo letras, números y _"
            else -> ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Editar Perfil",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = ""
                    },
                    label = { Text("Nombre completo") },
                    isError = nameError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, null, tint = EduRachaColors.Primary)
                    },
                    supportingText = {
                        if (nameError.isNotEmpty()) {
                            Text(nameError, color = EduRachaColors.Error)
                        }
                    }
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = ""
                    },
                    label = { Text("Nombre de usuario") },
                    isError = usernameError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.AlternateEmail, null, tint = EduRachaColors.Primary)
                    },
                    supportingText = {
                        if (usernameError.isNotEmpty()) {
                            Text(usernameError, color = EduRachaColors.Error)
                        } else {
                            Text("Solo letras, números y guiones bajos", fontSize = 12.sp)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = validateName(name)
                    usernameError = validateUsername(username)

                    if (nameError.isEmpty() && usernameError.isEmpty()) {
                        onConfirm(name.trim(), username.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(EduRachaColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, "Guardar", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = EduRachaColors.TextSecondary)
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Cambiar Contraseña",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Para: $userEmail",
                    fontSize = 13.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Contraseña actual") },
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null, tint = EduRachaColors.Primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Toggle visibility"
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, null, tint = EduRachaColors.Success)
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Toggle visibility"
                            )
                        }
                    },
                    supportingText = {
                        Text("Mínimo 6 caracteres", fontSize = 11.sp)
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = errorMessage.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.CheckCircle, null, tint = EduRachaColors.Success)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Toggle visibility"
                            )
                        }
                    }
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        errorMessage,
                        color = EduRachaColors.Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> errorMessage = "Ingresa tu contraseña actual"
                        newPassword.length < 6 -> errorMessage = "La contraseña debe tener al menos 6 caracteres"
                        newPassword != confirmPassword -> errorMessage = "Las contraseñas no coinciden"
                        currentPassword == newPassword -> errorMessage = "La nueva contraseña debe ser diferente"
                        else -> onConfirm(currentPassword, newPassword)
                    }
                },
                colors = ButtonDefaults.buttonColors(EduRachaColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, "Cambiar", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cambiar contraseña")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = EduRachaColors.TextSecondary)
            }
        }
    )
}

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Logout,
                null,
                tint = EduRachaColors.Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Cerrar Sesión",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "¿Estás seguro de que deseas cerrar sesión?",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(EduRachaColors.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CERRAR SESIÓN")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.School,
                null,
                tint = EduRachaColors.Primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "EduRacha",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Versión 1.0.0",
                    color = EduRachaColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Corporación Universitaria Autónoma del Cauca",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = EduRachaColors.Primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sistema de Gestión Educativa",
                    fontSize = 12.sp,
                    color = EduRachaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(EduRachaColors.Primary)
            ) {
                Text("Entendido")
            }
        }
    )
}
//</editor-fold>