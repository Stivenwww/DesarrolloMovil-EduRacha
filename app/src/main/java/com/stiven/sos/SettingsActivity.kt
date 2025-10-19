package com.stiven.sos

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.ui.theme.EduRachaColors
import com.stiven.sos.ui.theme.EduRachaTheme

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

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val userEmail = remember { currentUser?.email ?: "" }
    val displayName = remember { currentUser?.displayName ?: "" }

    val (userName, username) = remember(displayName) {
        if (displayName.contains("(") && displayName.contains(")")) {
            val name = displayName.substringBefore("(").trim()
            val user = displayName.substringAfter("(").substringBefore(")").trim()
            Pair(name, user)
        } else {
            Pair(displayName.ifEmpty { "Usuario" }, "")
        }
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
                username = username,
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
                    SettingsItem(Icons.Outlined.Edit, "Editar perfil", "Actualiza tu información", EduRachaColors.Primary) { showEditProfileDialog = true }
                    SettingsDivider()
                    SettingsItem(Icons.Outlined.Lock, "Cambiar contraseña", "Mantén tu cuenta segura", EduRachaColors.Primary) { showChangePasswordDialog = true }
                }

                SettingsSection(title = "NOTIFICACIONES", iconColor = EduRachaColors.Accent) {
                    SettingsSwitchItem("Notificaciones push", "Recibe alertas importantes", notificationsEnabled) { notificationsEnabled = it }
                    SettingsDivider()
                    SettingsSwitchItem("Sonido", "Alertas con sonido", soundEnabled) { soundEnabled = it }
                }

                SettingsSection(title = "INFORMACIÓN", iconColor = EduRachaColors.Secondary) {
                    SettingsItem(Icons.Outlined.Help, "Ayuda y soporte", "Centro de ayuda EduRacha", EduRachaColors.Secondary) { Toast.makeText(context, "Próximamente", Toast.LENGTH_SHORT).show() }
                    SettingsDivider()
                    SettingsItem(Icons.Outlined.Info, "Acerca de EduRacha", "Versión 1.0.0", EduRachaColors.Secondary) { showAboutDialog = true }
                }

                LogoutButton(onClick = { showLogoutDialog = true })
                SettingsFooter()
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentUsername = username,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, user ->
                // Aquí iría tu lógica para actualizar el perfil en Firebase
                Toast.makeText(context, "Perfil actualizado (simulación)", Toast.LENGTH_SHORT).show()
                showEditProfileDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { _, _ ->
                // Aquí iría tu lógica para cambiar la contraseña en Firebase
                Toast.makeText(context, "Contraseña cambiada (simulación)", Toast.LENGTH_SHORT).show()
                showChangePasswordDialog = false
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


// --- COMPOSABLES REUTILIZABLES ---

// CORRECCIÓN: Se añade la anotación @OptIn para suprimir la advertencia de API experimental.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(userName: String, username: String, userEmail: String, onBackClick: () -> Unit) {
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
                        .background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Avatar", modifier = Modifier.size(40.dp), tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (username.isNotEmpty()) {
                        Text("@$username", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(userEmail, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, iconColor: Color, content: @Composable ColumnScope.() -> Unit) {
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
                .background(color))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center
        ) {
            Icon(icon, title, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
            Text(subtitle, fontSize = 13.sp, color = EduRachaColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Default.ArrowForwardIos, "Ir", tint = EduRachaColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
            Text(subtitle, fontSize = 13.sp, color = EduRachaColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
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
            .height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(EduRachaColors.Error)
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
            .padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Corporación Universitaria", fontSize = 12.sp, color = EduRachaColors.TextSecondary)
        Text("Autónoma del Cauca", fontSize = 14.sp, color = EduRachaColors.Primary, fontWeight = FontWeight.Bold)
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


// --- FUNCIONES DE DIÁLOGO ---

@Composable
fun EditProfileDialog(currentName: String, currentUsername: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var username by remember { mutableStateOf(currentUsername) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nombre de usuario") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, username) }, colors = ButtonDefaults.buttonColors(EduRachaColors.Primary)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = EduRachaColors.TextSecondary) } }
    )
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it; errorMessage = "" }, label = { Text("Contraseña actual") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it; errorMessage = "" }, label = { Text("Nueva contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; errorMessage = "" }, label = { Text("Confirmar contraseña") }, visualTransformation = PasswordVisualTransformation(), isError = errorMessage.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = EduRachaColors.Error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 6) errorMessage = "La contraseña debe tener 6+ caracteres"
                    else if (newPassword != confirmPassword) errorMessage = "Las contraseñas no coinciden"
                    else onConfirm(currentPassword, newPassword)
                },
                colors = ButtonDefaults.buttonColors(EduRachaColors.Primary)
            ) { Text("Cambiar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = EduRachaColors.TextSecondary) } }
    )
}

@Composable
fun LogoutConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Logout, null, tint = EduRachaColors.Error, modifier = Modifier.size(48.dp)) },
        title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("¿Estás seguro de que deseas cerrar sesión?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(EduRachaColors.Error), modifier = Modifier.fillMaxWidth()) { Text("CERRAR SESIÓN") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.School, null, tint = EduRachaColors.Primary, modifier = Modifier.size(48.dp)) },
        title = { Text("EduRacha", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Versión 1.0.0", color = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(16.dp))
                Text("Corporación Universitaria Autónoma del Cauca", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = EduRachaColors.Primary)
            }
        },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(EduRachaColors.Primary)) { Text("Entendido") } }
    )
}
