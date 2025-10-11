package com.stiven.desarrollomovil

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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Estados para los datos del usuario
    val userEmail = remember { currentUser?.email ?: "" }
    val displayName = remember { currentUser?.displayName ?: "" }

    // Extraer nombre y username del displayName
    val (userName, username) = remember {
        if (displayName.contains("(") && displayName.contains(")")) {
            val name = displayName.substringBefore("(").trim()
            val user = displayName.substringAfter("(").substringBefore(")").trim()
            Pair(name, user)
        } else {
            Pair(displayName.ifEmpty { "Usuario" }, "")
        }
    }

    // Estados para diálogos
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Estados para switches
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Uniautonoma.Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Uniautonoma.Primary,
                                Uniautonoma.PrimaryLight
                            )
                        )
                    )
            ) {
                Column {
                    // TopAppBar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Configuración",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Perfil de usuario
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (username.isNotEmpty()) {
                                Text(
                                    text = "@$username",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = userEmail,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Contenido con scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                // Sección: CUENTA
                SectionHeader(
                    title = "CUENTA",
                    color = Uniautonoma.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Edit,
                            title = "Editar perfil",
                            subtitle = "Actualiza tu información personal",
                            iconColor = Uniautonoma.Primary,
                            onClick = { showEditProfileDialog = true }
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = Uniautonoma.Background
                        )

                        SettingsItem(
                            icon = Icons.Outlined.Lock,
                            title = "Cambiar contraseña",
                            subtitle = "Mantén tu cuenta segura",
                            iconColor = Uniautonoma.Primary,
                            onClick = { showChangePasswordDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sección: NOTIFICACIONES
                SectionHeader(
                    title = "NOTIFICACIONES",
                    color = Uniautonoma.Accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Notificaciones push",
                            subtitle = "Recibe alertas importantes",
                            iconColor = Uniautonoma.Accent,
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) "Notificaciones activadas" else "Notificaciones desactivadas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Uniautonoma.Background)
                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsSwitchItem(
                            icon = Icons.Outlined.VolumeUp,
                            title = "Sonido",
                            subtitle = "Alertas con sonido",
                            iconColor = Uniautonoma.Secondary,
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) "Sonido activado" else "Sonido desactivado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sección: INFORMACIÓN
                SectionHeader(
                    title = "INFORMACIÓN",
                    color = Uniautonoma.Secondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Help,
                            title = "Ayuda y soporte",
                            subtitle = "Centro de ayuda EduRacha",
                            iconColor = Uniautonoma.Secondary,
                            onClick = {
                                Toast.makeText(context, "Ayuda - Próximamente", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = Uniautonoma.Background
                        )

                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "Acerca de EduRacha",
                            subtitle = "Versión 1.0.0 - UNIAUTÓNOMA",
                            iconColor = Uniautonoma.Secondary,
                            onClick = { showAboutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Botón Cerrar Sesión
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Uniautonoma.Error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cerrar Sesión",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer institucional
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Corporación Universitaria",
                        fontSize = 12.sp,
                        color = Uniautonoma.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Autónoma del Cauca",
                        fontSize = 14.sp,
                        color = Uniautonoma.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    // Diálogos
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentUsername = username,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, user ->
                val newDisplayName = "$name ($user) - ${if (displayName.contains("Docente")) "Docente" else "Estudiante"}"
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build()

                currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                        showEditProfileDialog = false
                        // Recargar la actividad para reflejar cambios
                        (context as? ComponentActivity)?.recreate()
                    } else {
                        Toast.makeText(context, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                    user.reauthenticate(credential).addOnCompleteListener { reauth ->
                        if (reauth.isSuccessful) {
                            user.updatePassword(newPassword).addOnCompleteListener { update ->
                                if (update.isSuccessful) {
                                    Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                                    showChangePasswordDialog = false
                                } else {
                                    Toast.makeText(context, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                Toast.makeText(context, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Uniautonoma.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = "Ir",
            tint = Uniautonoma.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Uniautonoma.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Uniautonoma.TextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var username by remember { mutableStateOf(currentUsername) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Perfil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, username) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Uniautonoma.Primary
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Uniautonoma.TextSecondary)
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cambiar Contraseña",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Nueva contraseña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = errorMessage.isNotEmpty(),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Uniautonoma.Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                            errorMessage = "Todos los campos son obligatorios"
                        }
                        newPassword.length < 6 -> {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden"
                        }
                        else -> {
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Uniautonoma.Primary
                )
            ) {
                Text("Cambiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Uniautonoma.TextSecondary)
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
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                tint = Uniautonoma.Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Cerrar Sesión",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas cerrar sesión?",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Uniautonoma.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Uniautonoma.Error
                )
            ) {
                Text("Cerrar Sesión")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Uniautonoma.TextSecondary)
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
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = Uniautonoma.Primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "EduRacha",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Versión 1.0.0",
                    fontSize = 16.sp,
                    color = Uniautonoma.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Corporación Universitaria\nAutónoma del Cauca",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Uniautonoma.Primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Plataforma de gestión educativa con gamificación integrada.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = Uniautonoma.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Uniautonoma.Primary
                )
            ) {
                Text("Entendido")
            }
        }
    )
}